package ezvcard.io;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.scribe.ScribeIndex;
import ezvcard.io.scribe.VCardPropertyScribe;
import ezvcard.parameter.AddressType;
import ezvcard.property.Address;
import ezvcard.property.Label;
import ezvcard.property.ProductId;
import ezvcard.property.RawProperty;
import ezvcard.property.VCardProperty;

/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 The views and conclusions contained in the software and documentation are those
 of the authors and should not be interpreted as representing official policies, 
 either expressed or implied, of the FreeBSD Project.
 */

/**
 * Parent class for all vCard writer classes.
 * @author Michael Angstadt
 */
public abstract class AbstractVCardWriter {
	protected ScribeIndex index = new ScribeIndex();
	protected boolean addProdId = true;
	protected boolean versionStrict = true;

	/**
	 * Gets whether or not a "PRODID" property will be added to each vCard,
	 * saying that the vCard was generated by this library. For 2.1 vCards, the
	 * extended property "X-PRODID" will be added, since "PRODID" is not
	 * supported by that version.
	 * @return true if the property will be added, false if not (defaults to
	 * true)
	 */
	public boolean isAddProdId() {
		return addProdId;
	}

	/**
	 * Sets whether or not to add a "PRODID" property to each vCard, saying that
	 * the vCard was generated by this library. For 2.1 vCards, the extended
	 * property "X-PRODID" will be added, since "PRODID" is not supported by
	 * that version.
	 * @param addProdId true to add this property, false not to (defaults to
	 * true)
	 */
	public void setAddProdId(boolean addProdId) {
		this.addProdId = addProdId;
	}

	/**
	 * Gets whether properties that do not support the target version will be
	 * excluded from the written vCard.
	 * @return true to exclude properties that do not support the target
	 * version, false to include them anyway (defaults to true)
	 */
	public boolean isVersionStrict() {
		return versionStrict;
	}

	/**
	 * Sets whether properties that do not support the target version will be
	 * excluded from the written vCard.
	 * @param versionStrict true to exclude properties that do not support the
	 * target version, false to include them anyway (defaults to true)
	 */
	public void setVersionStrict(boolean versionStrict) {
		this.versionStrict = versionStrict;
	}

	/**
	 * <p>
	 * Registers a property scribe. This is the same as calling:
	 * </p>
	 * <p>
	 * {@code getScribeIndex().register(scribe)}
	 * </p>
	 * @param scribe the scribe to register
	 */
	public void registerScribe(VCardPropertyScribe<? extends VCardProperty> scribe) {
		index.register(scribe);
	}

	/**
	 * Gets the scribe index.
	 * @return the scribe index
	 */
	public ScribeIndex getScribeIndex() {
		return index;
	}

	/**
	 * Sets the scribe index.
	 * @param index the scribe index
	 */
	public void setScribeIndex(ScribeIndex index) {
		this.index = index;
	}

	/**
	 * Determines which properties need to be written.
	 * @param vcard the vCard to write
	 * @param targetVersion the target vCard version
	 * @return the properties to write
	 * @throws IllegalArgumentException if a scribe hasn't been registered for a
	 * custom property class (see: {@link #registerScribe registerScribe})
	 */
	protected List<VCardProperty> prepare(VCard vcard, VCardVersion targetVersion) {
		return prepare(vcard, targetVersion, addProdId);
	}

	/**
	 * Determines which properties need to be written.
	 * @param vcard the vCard to write
	 * @param targetVersion the target vCard version
	 * @param addProdId whether or not to add a PRODID property
	 * @return the properties to write
	 * @throws IllegalArgumentException if a scribe hasn't been registered for a
	 * custom property class (see: {@link #registerScribe, registerScribe})
	 */
	protected List<VCardProperty> prepare(VCard vcard, VCardVersion targetVersion, boolean addProdId) {
		List<VCardProperty> propertiesToAdd = new ArrayList<VCardProperty>();
		Set<Class<? extends VCardProperty>> unregistered = new HashSet<Class<? extends VCardProperty>>();
		for (VCardProperty property : vcard) {
			if (addProdId && property instanceof ProductId) {
				//do not add the PRODID in the vCard if "addProdId" is true
				continue;
			}

			if (versionStrict && !property.getSupportedVersions().contains(targetVersion)) {
				//do not add the property to the vCard if it is not supported by the target version
				continue;
			}

			//check for scribe
			if (!index.hasPropertyScribe(property)) {
				unregistered.add(property.getClass());
				continue;
			}

			propertiesToAdd.add(property);

			//add LABEL types for each ADR type if the target version is 2.1 or 3.0
			if (property instanceof Address && (targetVersion == VCardVersion.V2_1 || targetVersion == VCardVersion.V3_0)) {
				Address adr = (Address) property;
				String labelStr = adr.getLabel();
				if (labelStr == null) {
					continue;
				}

				Label label = new Label(labelStr);
				for (AddressType adrType : adr.getTypes()) {
					label.addType(adrType);
				}
				propertiesToAdd.add(label);
			}
		}

		if (!unregistered.isEmpty()) {
			//all code that calls this method needs to throw an exception, so an exception is thrown here instead of returning some value
			throw new IllegalArgumentException("No scribes were found the following property classes: " + unregistered);
		}

		//add a PRODID property, saying the vCard was generated by this library
		if (addProdId) {
			VCardProperty property;
			if (targetVersion == VCardVersion.V2_1) {
				property = new RawProperty("X-PRODID", "ez-vcard " + Ezvcard.VERSION);
			} else {
				property = new ProductId("ez-vcard " + Ezvcard.VERSION);
			}
			propertiesToAdd.add(property);
		}

		return propertiesToAdd;
	}
}
