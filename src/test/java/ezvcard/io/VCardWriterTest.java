package ezvcard.io;

import static ezvcard.util.TestUtils.assertWarnings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.List;

import org.junit.Test;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.parameters.AddressTypeParameter;
import ezvcard.types.AddressType;
import ezvcard.types.AgentType;
import ezvcard.types.FormattedNameType;
import ezvcard.types.KindType;
import ezvcard.types.LabelType;
import ezvcard.types.MemberType;
import ezvcard.types.NoteType;
import ezvcard.types.ProdIdType;
import ezvcard.types.StructuredNameType;

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
 * @author Michael Angstadt
 */
public class VCardWriterTest {
	/**
	 * Tests to make sure it contains the BEGIN, VERSION, and END types.
	 */
	@Test
	public void generalStructure() throws Exception {
		VCard vcard = new VCard();
		FormattedNameType fn = new FormattedNameType("John Doe");
		vcard.setFormattedName(fn);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(actual, expected);
	}

	@Test
	public void check_supported_versions() throws Exception {
		VCard vcard = new VCard();
		vcard.setFormattedName("John Doe");
		StructuredNameType n = new StructuredNameType();
		n.setGiven("John");
		n.setFamily("Doe");
		vcard.setStructuredName(n);
		vcard.setMailer("Thunderbird");

		//all properties support the version
		{
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V3_0);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertTrue(warnings.isEmpty());

			VCard parsedVCard = Ezvcard.parse(sw.toString()).first();
			assertEquals("Thunderbird", parsedVCard.getMailer().getValue());
		}

		//one property does not support the version
		{
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V4_0);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);

			//property not written to vCard
			VCard parsedVCard = Ezvcard.parse(sw.toString()).first();
			assertNull(parsedVCard.getMailer());
		}
	}

	@Test
	public void required_properties_21() throws Exception {
		VCardVersion version = VCardVersion.V2_1;

		//without N
		{
			VCard vcard = new VCard();
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);
		}

		//with N
		{
			VCard vcard = new VCard();
			StructuredNameType n = new StructuredNameType();
			n.setFamily("Joe");
			n.setGiven("John");
			vcard.setStructuredName(n);

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertTrue(warnings.isEmpty());
		}
	}

	@Test
	public void required_properties_30() throws Exception {
		VCardVersion version = VCardVersion.V3_0;

		//without N or FN
		{
			VCard vcard = new VCard();
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(2, warnings);
		}

		//with N
		{
			VCard vcard = new VCard();
			StructuredNameType n = new StructuredNameType();
			n.setFamily("Joe");
			n.setGiven("John");
			vcard.setStructuredName(n);

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);
		}

		//with FN
		{
			VCard vcard = new VCard();
			vcard.setFormattedName("John Doe");

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);
		}

		//with both
		{
			VCard vcard = new VCard();
			StructuredNameType n = new StructuredNameType();
			n.setFamily("Joe");
			n.setGiven("John");
			vcard.setStructuredName(n);
			vcard.setFormattedName("John Doe");

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertTrue(warnings.isEmpty());
		}
	}

	@Test
	public void required_properties_40() throws Exception {
		VCardVersion version = VCardVersion.V4_0;

		//without FN
		{
			VCard vcard = new VCard();
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);
		}

		//with FN
		{
			VCard vcard = new VCard();
			vcard.setFormattedName("John Doe");

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, version);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertTrue(warnings.isEmpty());
		}
	}

	@Test
	public void setAddProdId() throws Exception {
		VCard vcard = new VCard();
		FormattedNameType fn = new FormattedNameType("John Doe");
		vcard.setFormattedName(fn);

		//with X-PRODID (2.1)
		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nX-PRODID:"));

		//with PRODID (3.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nPRODID:"));

		//with PRODID (4.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V4_0);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nPRODID:"));

		//with X-PRODID (2.1)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(true);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nX-PRODID:"));

		//with PRODID (3.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.setAddProdId(true);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nPRODID:"));

		//with PRODID (4.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V4_0);
		vcw.setAddProdId(true);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nPRODID:"));

		//without X-PRODID (2.1)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertFalse(sw.toString().contains("\r\nX-PRODID:"));

		//without PRODID (3.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertFalse(sw.toString().contains("\r\nPRODID:"));

		//without PRODID (4.0)
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V4_0);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertFalse(sw.toString().contains("\r\nPRODID:"));
	}

	@Test
	public void setAddProdId_overwrites_existing_prodId() throws Exception {
		VCard vcard = new VCard();

		FormattedNameType fn = new FormattedNameType("John Doe");
		vcard.setFormattedName(fn);

		ProdIdType prodId = new ProdIdType("Acme Co.");
		vcard.setProdId(prodId);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertTrue(sw.toString().contains("\r\nPRODID:Acme Co."));

		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.setAddProdId(true);
		vcw.write(vcard);
		assertFalse(sw.toString().contains("\r\nPRODID:Acme Co."));
	}

	/**
	 * The TYPE sub types for 2.1 vCards should look like this:
	 * <p>
	 * <code>ADR;WORK;DOM:</code>
	 * </p>
	 * 
	 * The TYPE sub types for 3.0 vCards should look like this:
	 * <p>
	 * <code>ADR;TYPE=work,dom:</code>
	 * </p>
	 */
	@Test
	public void typeParameter() throws Exception {
		VCard vcard = new VCard();

		//one type
		AddressType adr = new AddressType();
		adr.addType(AddressTypeParameter.WORK);
		vcard.addAddress(adr);

		//two types
		adr = new AddressType();
		adr.addType(AddressTypeParameter.WORK);
		adr.addType(AddressTypeParameter.DOM);
		vcard.addAddress(adr);

		//three types
		adr = new AddressType();
		adr.addType(AddressTypeParameter.WORK);
		adr.addType(AddressTypeParameter.DOM);
		adr.addType(AddressTypeParameter.PARCEL);
		vcard.addAddress(adr);

		//2.1
		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"ADR;WORK:;;;;;;\r\n" +
		"ADR;WORK;DOM:;;;;;;\r\n" +
		"ADR;WORK;DOM;PARCEL:;;;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);

		//3.0
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		actual = sw.toString();

		//@formatter:off
		expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"ADR;TYPE=work:;;;;;;\r\n" +
		"ADR;TYPE=work,dom:;;;;;;\r\n" +
		"ADR;TYPE=work,dom,parcel:;;;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);
	}

	/**
	 * Test to make sure it marshals sub types correctly.
	 */
	@Test
	public void subTypes() throws Exception {
		VCard vcard = new VCard();

		//single-valued subtype
		AddressType adr = new AddressType();
		adr.getSubTypes().put("X-DOORMAN", "true");
		vcard.addAddress(adr);

		//single- and multi-valued subtype
		adr = new AddressType();
		adr.getSubTypes().put("X-DOORMAN", "true");
		adr.getSubTypes().put("LANGUAGE", "FR");
		adr.getSubTypes().put("LANGUAGE", "es");
		vcard.addAddress(adr);

		//2.1
		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"ADR;X-DOORMAN=true:;;;;;;\r\n" +
		"ADR;X-DOORMAN=true;LANGUAGE=FR;LANGUAGE=es:;;;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);

		//3.0
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V3_0, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		actual = sw.toString();

		//@formatter:off
		expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"ADR;X-DOORMAN=true:;;;;;;\r\n" +
		"ADR;X-DOORMAN=true;LANGUAGE=FR,es:;;;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);

		//4.0
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V4_0, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		actual = sw.toString();

		//@formatter:off
		expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:4.0\r\n" +
		"ADR;X-DOORMAN=true:;;;;;;\r\n" +
		"ADR;X-DOORMAN=true;LANGUAGE=FR,es:;;;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);
	}

	@Test
	public void parameter_values_handling_special_chars() throws Exception {
		VCard vcard = new VCard();
		NoteType note = vcard.addNote("test");
		note.getSubTypes().put("X-TEST", "^�\\,;:=[]\"\t\n" + ((char) 28));
		note.getSubTypes().put("X-TEST", "normal");

		//2.1 without caret escaping
		{
			VCardVersion version = VCardVersion.V2_1;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no N property" warning
			assertEquals(2, writer.getWarnings().size());

			String actual = sw.toString();

			//removes , : = [ ] FS
			//replaces \ with \\
			//replaces ; with \;
			//replaces newline with space
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" +
			"NOTE;X-TEST=^�\\\\\\;\"\t ;X-TEST=normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}

		//2.1 with caret escaping (ignored)
		{
			VCardVersion version = VCardVersion.V2_1;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setCaretEncodingEnabled(true);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no N property" warning
			assertEquals(2, writer.getWarnings().size());

			String actual = sw.toString();

			//removes , : = [ ] FS
			//replaces \ with \\
			//replaces ; with \;
			//replaces newline with space
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" +
			"NOTE;X-TEST=^�\\\\\\;\"\t ;X-TEST=normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}

		//3.0 without caret escaping
		{
			VCardVersion version = VCardVersion.V3_0;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no N property" and "no FN property" warnings
			assertEquals(3, writer.getWarnings().size());

			String actual = sw.toString();

			//removes FS
			//replaces \ with \\
			//replaces newline with space
			//replaces " with '
			//surrounds in double quotes, since it contains , ; or :
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:3.0\r\n" +
			"NOTE;X-TEST=\"^�\\\\,;:=[]'\t \",normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}

		//3.0 with caret escaping (same as 4.0)
		{
			VCardVersion version = VCardVersion.V3_0;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setCaretEncodingEnabled(true);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no N property" and "no FN property" warnings
			assertEquals(3, writer.getWarnings().size());

			String actual = sw.toString();

			//removes FS
			//replaces ^ with ^^
			//replaces newline with ^n
			//replaces " with ^'
			//surrounds in double quotes, since it contains , ; or :
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:3.0\r\n" +
			"NOTE;X-TEST=\"^^�\\,;:=[]^'\t^n\",normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}

		//4.0 without caret escaping
		{
			VCardVersion version = VCardVersion.V4_0;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no FN property" warning
			assertEquals(2, writer.getWarnings().size());

			String actual = sw.toString();

			//removes FS
			//replaces \ with \\
			//replaces newline with \n
			//replaces " with '
			//surrounds in double quotes, since it contains , ; or :
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:4.0\r\n" +
			"NOTE;X-TEST=\"^�\\\\,;:=[]'\t\\n\",normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}

		//4.0 with caret escaping
		{
			VCardVersion version = VCardVersion.V4_0;
			StringWriter sw = new StringWriter();
			VCardWriter writer = new VCardWriter(sw, version);
			writer.setCaretEncodingEnabled(true);
			writer.setAddProdId(false);
			writer.write(vcard);

			//should generate 1 warning for removal of invalid chars, plus the "no FN property" warning
			assertEquals(2, writer.getWarnings().size());

			String actual = sw.toString();

			//removes FS
			//replaces ^ with ^^
			//replaces newline with ^n
			//replaces " with ^'
			//surrounds in double quotes, since it contains , ; or :
			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:4.0\r\n" +
			"NOTE;X-TEST=\"^^�\\,;:=[]^'\t^n\",normal:test\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(expected, actual);
		}
	}

	/**
	 * Test to make sure the folding scheme functionality works.
	 */
	@Test
	public void foldingScheme() throws Exception {
		VCard vcard = new VCard();
		NoteType note = new NoteType("The vCard MIME Directory Profile also provides support for representing other important information about the person associated with the directory entry. For instance, the date of birth of the person; an audio clip describing the pronunciation of the name associated with the directory entry, or some other application of the digital sound; longitude and latitude geo-positioning information related to the person associated with the directory entry; date and time that the directory information was last updated; annotations often written on a business card; Uniform Resource Locators (URL) for a website; public key information.");
		vcard.addNote(note);
		FoldingScheme fs = new FoldingScheme(50, "  ");

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1, fs, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"NOTE:The vCard MIME Directory Profile also provide\r\n" +
		"  s support for representing other important infor\r\n" +
		"  mation about the person associated with the dire\r\n" +
		"  ctory entry. For instance\\, the date of birth of \r\n" +
		"  the person\\; an audio clip describing the pronun\r\n" +
		"  ciation of the name associated with the director\r\n" +
		"  y entry\\, or some other application of the digit\r\n" +
		"  al sound\\; longitude and latitude geo-positionin\r\n" +
		"  g information related to the person associated w\r\n" +
		"  ith the directory entry\\; date and time that the \r\n" +
		"  directory information was last updated\\; annotat\r\n" +
		"  ions often written on a business card\\; Uniform \r\n" +
		"  Resource Locators (URL) for a website\\; public k\r\n" +
		"  ey information.\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(actual, expected);
	}

	/**
	 * Test to make sure it uses whatever newline string you want it to.
	 */
	@Test
	public void newline() throws Exception {
		VCard vcard = new VCard();
		FormattedNameType fn = new FormattedNameType("John Doe");
		vcard.setFormattedName(fn);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1, FoldingScheme.MIME_DIR, "*");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD*" +
		"VERSION:2.1*" +
		"FN:John Doe*" +
		"END:VCARD*";
		//@formatter:on

		assertEquals(actual, expected);
	}

	/**
	 * Tests to make sure it marshals groups correctly.
	 */
	@Test
	public void groups() throws Exception {
		VCard vcard = new VCard();
		FormattedNameType fn = new FormattedNameType("John Doe");
		fn.setGroup("group1");
		vcard.setFormattedName(fn);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"group1.FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(actual, expected);
	}

	/**
	 * Tests types with nested vCards (i.e AGENT type) in version 2.1.
	 */
	@Test
	public void nestedVCard() throws Exception {
		VCard vcard = new VCard();

		FormattedNameType fn = new FormattedNameType("Michael Angstadt");
		vcard.setFormattedName(fn);

		VCard agentVcard = new VCard();
		agentVcard.setFormattedName(new FormattedNameType("Agent 007"));
		agentVcard.getNotes().add(new NoteType("Make sure that it properly folds long lines which are part of a nested AGENT type in a version 2.1 vCard."));
		AgentType agent = new AgentType(agentVcard);
		vcard.setAgent(agent);

		//i herd u liek AGENTs...
		VCard secondAgentVCard = new VCard();
		secondAgentVCard.setFormattedName(new FormattedNameType("Agent 009"));
		secondAgentVCard.getNotes().add(new NoteType("Make sure that it ALSO properly folds THIS long line because it's part of an AGENT that's inside of an AGENT."));
		AgentType secondAgent = new AgentType(secondAgentVCard);
		agentVcard.setAgent(secondAgent);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertEquals(3, vcw.getWarnings().size()); //each vCard is missing the N property, which is required for 2.1
		String actual = sw.toString();

		//FIXME this test may fail on other machines because Class.getDeclaredFields() returns the fields in no particular order
		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"FN:Michael Angstadt\r\n" +
		"AGENT:\r\n" + //nested types should not have X-GENERATOR
			"BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" +
			"FN:Agent 007\r\n" +
			"AGENT:\r\n" +
				"BEGIN:VCARD\r\n" +
				"VERSION:2.1\r\n" +
				"FN:Agent 009\r\n" +
				"NOTE:Make sure that it ALSO properly folds THIS long line because it's part \r\n" +
				" of an AGENT that's inside of an AGENT.\r\n" +
				"END:VCARD\r\n" +
			"NOTE:Make sure that it properly folds long lines which are part of a nested \r\n" +
			" AGENT type in a version 2.1 vCard.\r\n" +
			"END:VCARD\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);
	}

	/**
	 * Tests types with embedded vCards (i.e AGENT type) in version 3.0.
	 */
	@Test
	public void embeddedVCard() throws Exception {
		VCard vcard = new VCard();

		FormattedNameType fn = new FormattedNameType("Michael Angstadt");
		vcard.setFormattedName(fn);

		VCard agentVcard = new VCard();
		agentVcard.setFormattedName(new FormattedNameType("Agent 007"));
		NoteType note = new NoteType("Bonne soir�e, 007.");
		note.setLanguage("fr");
		agentVcard.getNotes().add(note);
		AgentType agent = new AgentType(agentVcard);
		vcard.setAgent(agent);

		//i herd u liek AGENTs...
		VCard secondAgentVCard = new VCard();
		secondAgentVCard.setFormattedName(new FormattedNameType("Agent 009"));
		note = new NoteType("Good evening, 009.");
		note.setLanguage("en");
		secondAgentVCard.getNotes().add(note);
		AgentType secondAgent = new AgentType(secondAgentVCard);
		agentVcard.setAgent(secondAgent);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V3_0, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		assertEquals(3, vcw.getWarnings().size()); //each vCard is missing the N property, which is required for 3.0
		String actual = sw.toString();

		//FIXME this test may fail on other machines because Class.getDeclaredFields() returns the fields in no particular order
		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"FN:Michael Angstadt\r\n" +
		"AGENT:" + //nested types should not have X-GENERATOR
			"BEGIN:VCARD\\n" +
			"VERSION:3.0\\n" +
			"FN:Agent 007\\n" +
			"AGENT:" +
				"BEGIN:VCARD\\\\n" +
				"VERSION:3.0\\\\n" +
				"FN:Agent 009\\\\n" +
				"NOTE\\\\\\;LANGUAGE=en:Good evening\\\\\\\\\\\\\\, 009.\\\\n" +
				"END:VCARD\\\\n\\n" +
			"NOTE\\;LANGUAGE=fr:Bonne soir�e\\\\\\, 007.\\n" +
			"END:VCARD\\n\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);
	}

	/**
	 * Test to make sure it marshals LABELs correctly.
	 */
	@Test
	public void labels() throws Exception {
		VCard vcard = new VCard();

		//address with label
		AddressType adr = new AddressType();
		adr.setStreetAddress("123 Main St.");
		adr.setLocality("Austin");
		adr.setRegion("TX");
		adr.setPostalCode("12345");
		adr.setLabel("123 Main St.\r\nAustin, TX 12345");
		adr.addType(AddressTypeParameter.HOME);
		vcard.addAddress(adr);

		//address without label
		adr = new AddressType();
		adr.setStreetAddress("222 Broadway");
		adr.setLocality("New York");
		adr.setRegion("NY");
		adr.setPostalCode("99999");
		adr.addType(AddressTypeParameter.WORK);
		vcard.addAddress(adr);

		//orphaned label
		LabelType label = new LabelType("22 Spruce Ln.\r\nChicago, IL 11111");
		label.addType(AddressTypeParameter.PARCEL);
		vcard.addOrphanedLabel(label);

		//3.0
		//LABEL types should be used
		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V3_0, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"ADR;TYPE=home:;;123 Main St.;Austin;TX;12345;\r\n" +
		"LABEL;TYPE=home:123 Main St.\\nAustin\\, TX 12345\r\n" +
		"ADR;TYPE=work:;;222 Broadway;New York;NY;99999;\r\n" +
		"LABEL;TYPE=parcel:22 Spruce Ln.\\nChicago\\, IL 11111\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);

		//4.0
		//LABEL parameters should be used
		sw = new StringWriter();
		vcw = new VCardWriter(sw, VCardVersion.V4_0, null, "\r\n");
		vcw.setAddProdId(false);
		vcw.write(vcard);
		actual = sw.toString();

		//@formatter:off
		expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:4.0\r\n" +
		"ADR;LABEL=\"123 Main St.\\nAustin, TX 12345\";TYPE=home:;;123 Main St.;Austin;TX;12345;\r\n" +
		"ADR;TYPE=work:;;222 Broadway;New York;NY;99999;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(expected, actual);
	}

	/**
	 * If the type's marshal method throws a {@link SkipMeException}, then a
	 * warning should be added to the warnings list and the type object should
	 * NOT be marshalled.
	 */
	@Test
	public void skipMeException() throws Exception {
		VCard vcard = new VCard();

		//add N property so a warning isn't generated (2.1 requires N to be present)
		StructuredNameType n = new StructuredNameType();
		vcard.setStructuredName(n);

		LuckyNumType num = new LuckyNumType();
		num.luckyNum = 24;
		vcard.addExtendedType(num);

		//should be skipped
		num = new LuckyNumType();
		num.luckyNum = 13;
		vcard.addExtendedType(num);

		StringWriter sw = new StringWriter();
		VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
		vcw.setAddProdId(false);
		vcw.write(vcard);

		assertEquals(vcw.getWarnings().toString(), 1, vcw.getWarnings().size());

		String actual = sw.toString();

		//@formatter:off
		String expected =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"N:;;;;\r\n" +
		"X-LUCKY-NUM:24\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		assertEquals(actual, expected);
	}

	/**
	 * If newline characters exist in a property value in 2.1, then that
	 * property value should be "quoted-printable" encoded. The escape sequence
	 * "\n" should ONLY be used for 3.0 and 4.0. See the "Delimiters" subsection
	 * in section 2 of the 2.1 specs.
	 */
	@Test
	public void newlines_in_property_values() throws Exception {
		VCard vcard = new VCard();
		vcard.addNote("One\r\nTwo");
		vcard.addNote("Three Four");

		//2.1
		{
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V2_1);
			vcw.setAddProdId(false);
			vcw.write(vcard);

			String actual = sw.toString();

			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" +
			"NOTE;ENCODING=quoted-printable:One=0D=0ATwo\r\n" +
			"NOTE:Three Four\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(actual, expected);
		}

		//3.0
		{
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V3_0);
			vcw.setAddProdId(false);
			vcw.write(vcard);

			String actual = sw.toString();

			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:3.0\r\n" +
			"NOTE:One\\nTwo\r\n" +
			"NOTE:Three Four\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(actual, expected);
		}

		//4.0
		{
			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V4_0);
			vcw.setAddProdId(false);
			vcw.write(vcard);

			String actual = sw.toString();

			//@formatter:off
			String expected =
			"BEGIN:VCARD\r\n" +
			"VERSION:4.0\r\n" +
			"NOTE:One\\nTwo\r\n" +
			"NOTE:Three Four\r\n" +
			"END:VCARD\r\n";
			//@formatter:on

			assertEquals(actual, expected);
		}
	}

	@Test
	public void kind_and_member_combination() throws Exception {
		VCard vcard = new VCard();
		vcard.setFormattedName("John Doe");
		vcard.addMember(new MemberType("http://uri.com"));

		//correct KIND
		{
			vcard.setKind(KindType.group());

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V4_0);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertTrue(warnings.isEmpty());

			VCard parsedVCard = Ezvcard.parse(sw.toString()).first();
			assertEquals("group", parsedVCard.getKind().getValue());
			assertEquals(1, parsedVCard.getMembers().size());
			assertEquals("http://uri.com", parsedVCard.getMembers().get(0).getUri());
		}

		//wrong KIND
		{
			vcard.setKind(KindType.individual());

			StringWriter sw = new StringWriter();
			VCardWriter vcw = new VCardWriter(sw, VCardVersion.V4_0);
			vcw.write(vcard);

			List<String> warnings = vcw.getWarnings();
			assertWarnings(1, warnings);

			VCard parsedVCard = Ezvcard.parse(sw.toString()).first();
			assertEquals("individual", parsedVCard.getKind().getValue());
			assertTrue(parsedVCard.getMembers().isEmpty());
		}
	}
}
