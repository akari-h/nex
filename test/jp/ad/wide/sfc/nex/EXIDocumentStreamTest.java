package jp.ad.wide.sfc.nex;


import com.siemens.ct.exi.*;
import java.io.*;
import javax.xml.stream.*;
import org.junit.*;


public class EXIDocumentStreamTest {
    

    public void concatenatedStreamTestCommon(byte[] prepend) throws Exception {
	final String catfilename = "sample/exi-concatenated.exi";
	final String schema_location = "sample/schema_localized/00_canonical_example01.xsd";
	final String[] answers = {
	    "sample/exi/001-base-start.xml",
	    "sample/exi/002-base-setup.xml",
	    "sample/exi/010-base+muc-restart.xml",
	    "sample/exi/020-base+muc-iq-bind.xml",
	    "sample/exi/021-base+muc-iq-roster.xml",
	    "sample/exi/030-base+muc-presence.xml",
	    "sample/exi/041-base+muc-message.xml",
	    "sample/exi/050-base+muc-muc-iq.xml",
	    "sample/exi/051-base+muc-muc-message.xml",
	    "sample/exi/090-base+muc-logout.xml",
	    "sample/exi/099-base+muc-end.xml",
	    "sample/exi/100-hdr+base+muc-restart.xml"
	};


	InputStream testinputstream = null;
	if (prepend != null){
	    File f = new File(catfilename);
	    long flen = f.length();
	    if (flen > 65535){
		throw new Error("file size too large");
	    }
	    byte b[] = new byte[(int)flen+prepend.length];
	    System.arraycopy(prepend, 0, b, 0, prepend.length);
	    FileInputStream fs = new FileInputStream(f);
	    fs.read(b, prepend.length, (int)flen);
	    fs.close();
	    testinputstream = new ByteArrayInputStream(b);
	} else {
	    testinputstream = new FileInputStream(catfilename);
	}

	EXIFactory ef = EXIUtil.prepare_EXIFactory(schema_location);
	EXIDocumentStream eds = new EXIDocumentStream(testinputstream, ef);
	XMLInputFactory xif = XMLInputFactory.newInstance();
	int doccount = 0;
	try{
	    while (doccount < answers.length){
		String d = eds.nextDocument();
		System.out.println("[[[\n"+d+"\n]]]");

		XMLStreamReader xsrc = xif.createXMLStreamReader(new FileInputStream(answers[doccount]));
		XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(d.getBytes("UTF-8")));
		
		doccount ++;
	    }
	    eds.nextDocument(); // should raise DocumentStreamEnd
	    Assert.fail("should not happen");
	} catch (DocumentStreamEnd e){
	    System.out.println("document stream ended with "+doccount+" documents.");
	}
	Assert.assertEquals(doccount, 12);
	return;
    }

    @Test
    public void concatenatedStreamTest() throws Exception {
	concatenatedStreamTestCommon(null);
    }

    @Test
    public void garbedgeTest() throws Exception {
	byte[] b = new byte[4];
	b[0]=0;
	b[1]=0;
	b[2]=0;
	b[3]=0;
	concatenatedStreamTestCommon(b);
    }
}
