/**
 * Postprocessor Test
 */
package jp.ad.wide.sfc.nex;

import java.io.*;
import javax.xml.stream.*;

import com.siemens.ct.exi.EXIFactory;

import org.junit.*;

public class XMLfilterReaderTest {
    void assertFilterStreamOK(Reader testSource, String[] answers) throws Exception{
	XMLfilterReaderAlt xfr = new XMLfilterReaderAlt(testSource);
	xfr.setDebug(false);
	int count = 0;
	Assert.assertFalse(xfr.isRealEOF());
	while (!xfr.isRealEOF()){
	    char[] rbuf = new char[1024];
	    int rbuf_read = xfr.read(rbuf, 0, rbuf.length);
	    if (rbuf_read < 0){
		System.out.println("rbuf_read returned "+rbuf_read);
		break;
	    }
	    
	    String s = new String(rbuf, 0, rbuf_read);

	    System.out.println("assertFilterStreamOK got   :["+s+"]");
	    System.out.println("assertFilterStreamOK expect:["+answers[count]+"]");
	    Assert.assertEquals(answers[count], s);
	    count++;
	    xfr.prepareNextStream();
	}
	Assert.assertEquals(answers.length, count);
    }

    @Test
    public void testSingleXML() throws Exception {
	String[] answers = {"<?xml version='1.0'?><a><b>hoge?</b></a>"};
	StringReader testSource = new StringReader(answers[0]);
	assertFilterStreamOK(testSource, answers);
    }

    @Test
    public void testMultiXML() throws Exception {
	String[] answers = {"<?xml version='1.0'?><a><b>hoge?</b></a>\n", "<?xml version='1.0'?><c><d>hoge?</d></c>\n"};
	StringReader testSource = new StringReader(answers[0]+answers[1]);
	assertFilterStreamOK(testSource, answers);
    }

    @Test
    public void testMultiQuotedXML() throws Exception {
	String[] answers = {"<?xml version='1.0'?><a><b>hoge?</b></a>\n", "<?xml version='1.0'?><c y='<?xml ?>'><d x='hoi'>hoge?</d></c>\n"};
	StringReader testSource = new StringReader(answers[0]+answers[1]);
	assertFilterStreamOK(testSource, answers);
    }
    
}


