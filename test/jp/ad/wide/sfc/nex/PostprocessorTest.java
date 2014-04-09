/**
 * Postprocessor Test
 */
package jp.ad.wide.sfc.nex;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import com.siemens.ct.exi.EXIFactory;

import static org.junit.Assert.assertEquals;
import org.junit.*;

public class PostprocessorTest {

    void testCommonCase(String[] filenames, String path, String answer_filename, EXIFactory efactory) throws Exception {
	/**
	 * [file] | files_pipe_out = post_xep0322_in | [Postprocessor] | post_xmpp_out
	 */

	PipedOutputStream files_pipe_out = new PipedOutputStream();
	PipedInputStream post_xep0322_in = new PipedInputStream(files_pipe_out);
	StringWriter post_xmpp_writer = new StringWriter();

	Postprocessor post = null;
	if (efactory != null){
	    post = new Postprocessor(post_xep0322_in, post_xmpp_writer, efactory);
	} else {
	    post = new Postprocessor(post_xep0322_in, post_xmpp_writer);
	}
	post.setDebug(false);

	PostprocessorTask post_task = new PostprocessorTask(post);
	Thread postTask = new Thread(post_task);
	postTask.start();


	for (String filename : filenames){
	    System.out.println("file: "+filename);
	    FileInputStream f_in = new FileInputStream(path+filename);
	    int c = 0;
	    while ((c = f_in.read()) >= 0){
		files_pipe_out.write(c);
	    }
	}

	files_pipe_out.close();

	System.out.println("postTask.join-->");
	postTask.join();
	System.out.println("postTask.join-->joined.");

	System.out.println("##RESULT[[[\n"+post_xmpp_writer.toString()+"\n]]]");

	XMLInputFactory xif = XMLInputFactory.newInstance();
	XMLStreamReader xsrc = xif.createXMLStreamReader(new FileInputStream(answer_filename));
	XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(post_xmpp_writer.toString().getBytes("UTF-8")));

	XMLTester.assert_tags_identical(xsrc, xresult);

	return;
    }

    @Test
    public void testXML() throws Exception{
	final String[] filenames = {
	    "010-base+muc-restart.xml",
	    "020-base+muc-iq-bind.xml",
	    "021-base+muc-iq-roster.xml",
	    "030-base+muc-presence.xml",
	    "041-base+muc-message.xml",
	    "050-base+muc-muc-iq.xml",
	    "051-base+muc-muc-message.xml",
	    "090-base+muc-logout.xml",
	    "099-base+muc-end.xml"
	};
	final String path = "sample/exi/";

	this.testCommonCase(filenames, path, "sample/postproc-answer.xml", null);
    }

    @Test
    public void testEXI() throws Exception {
	final String[] filenames = {
	    "010-base+muc-restart-N-SIs.exi",
	    "020-base+muc-iq-bind-N-SIs.exi",
	    "021-base+muc-iq-roster-N-SIs.exi",
	    "030-base+muc-presence-N-SIs.exi",
	    "041-base+muc-message-N-SIs.exi",
	    "050-base+muc-muc-iq-N-SIs.exi",
	    "051-base+muc-muc-message-N-SIs.exi",
	    "090-base+muc-logout-N-SIs.exi",
	    "099-base+muc-end-N-SIs.exi"
	};
	final String path = "sample/exi/";
	final String schema_location = "sample/schema_localized/00_canonical_example01.xsd";
	EXIFactory ef = EXIUtil.prepare_EXIFactory(schema_location);
	
	this.testCommonCase(filenames, path, "sample/postproc-answer.xml", ef);
    }


    public String testByString(String input_xepstr) throws Exception {
	InputStream xep_in = new ByteArrayInputStream(input_xepstr.getBytes());
	StringWriter result_out = new StringWriter();
	Postprocessor post = new Postprocessor(xep_in, result_out);

	post.setDebug(false);
	post.process_documents();

	return result_out.toString();
    }

    // Handling of default namespace
    @Test
    public void testDefaultNamespace() throws Exception {
	final String input_xepstr = "<?xml version='1.0' encoding='UTF-8'?><ns5:streamStart xmlns:ns5='http://jabber.org/protocol/compress/exi' xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns:ns8='jabber:client' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'> <ns5:xmlns namespace='http://etherx.jabber.org/streams' prefix='stream'/><ns5:xmlns namespace='jabber:client' prefix=''/> <ns5:xmlns namespace='http://www.w3.org/XML/1998/namespace' prefix='xml'/></ns5:streamStart>";
	final String answer_xmpp = "<stream:stream xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:stream='http://etherx.jabber.org/streams' xmlns:ns5='http://jabber.org/protocol/compress/exi' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns='jabber:client' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'>";

	String result_xmpp = testByString(input_xepstr);

	String answer_first_element = XMLUtil.extractElementishFromString(answer_xmpp);
	Range result_first_element_range = XMLUtil.findElementishFromString(result_xmpp);
	String result_first_element = null;
	while (result_first_element_range != null){
	    System.out.println("testDefaultNamespace: searching for result first element");
	    result_first_element = XMLUtil.extractFromString(result_xmpp, result_first_element_range);
	    System.out.println("testDefaultNamespace: current result_first_element:"+result_first_element);
	    // skip processing instruction (e.g. <?xml ...?>)
	    if (! result_first_element.startsWith("<?")){
		break;

	    }
	    result_first_element_range = XMLUtil.findElementishFromString(result_xmpp, result_first_element_range.end());
	}
	
	HashMap<String, String> aMap = XMLUtil.extractAttributes(answer_first_element);
	HashMap<String, String> rMap = XMLUtil.extractAttributes(result_first_element);

	System.out.println("testDefaultNamespace: result_first_element:" + result_first_element);
	System.out.println("testDefaultNamespace: rMap.keySet(): " + rMap.keySet().toString());
	Assert.assertTrue(rMap.containsKey("xmlns"));
	Assert.assertTrue(aMap.containsKey("xmlns"));
	Assert.assertEquals(rMap.get("xmlns"), aMap.get("xmlns"));
	
	return;
    }

    // Handling of comment (XMPP does not allow comments but it should not)
    @Test
    @Ignore
    public void testComment() throws Exception {
	final String input_xepstr = "<?xml version='1.0' encoding='UTF-8'?><!-- test --><ns5:streamStart xmlns:ns5='http://jabber.org/protocol/compress/exi' xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns:ns8='jabber:client' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'> <!-- test --> <ns5:xmlns namespace='http://etherx.jabber.org/streams' prefix='stream'/><ns5:xmlns namespace='jabber:client' prefix=''/> <ns5:xmlns namespace='http://www.w3.org/XML/1998/namespace' prefix='xml'/></ns5:streamStart>";
	final String answer_xmpp = "<stream:stream xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:stream='http://etherx.jabber.org/streams' xmlns:ns5='http://jabber.org/protocol/compress/exi' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns='jabber:client' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'>";

	String result_xmpp = testByString(input_xepstr);

	System.out.println("######### testComment ->[[[\n"+result_xmpp+"\n]]]");

	XMLInputFactory xif = XMLInputFactory.newInstance();
	XMLStreamReader xanswer = xif.createXMLStreamReader(new ByteArrayInputStream(answer_xmpp.toString().getBytes("UTF-8")));
	XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(result_xmpp.toString().getBytes("UTF-8")));

	XMLTester.assert_tags_identical(xanswer, xresult);
    }
    

    // Handling of intermediate subelement namespace
    // result SHOULD have /stream:stream/{jabber:client}iq/{jabber:iq:auth}query
    @Test
    public void testSubelementNamespace() throws Exception {
	final String[] input_xepstrs = {
	    "<?xml version='1.0' encoding='UTF-8'?><ns5:streamStart xmlns:ns5='http://jabber.org/protocol/compress/exi' xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns:ns8='jabber:client' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'><ns5:xmlns namespace='http://etherx.jabber.org/streams' prefix='stream'/><ns5:xmlns namespace='jabber:client' prefix=''/><ns5:xmlns namespace='http://www.w3.org/XML/1998/namespace' prefix='xml'/></ns5:streamStart>\n", 
	    "<?xml version='1.0' encoding='UTF-8'?><ns8:iq xmlns:ns8='jabber:client' id='hoi-1' type='set' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:ns3='http://www.w3.org/2001/XMLSchema' xmlns:ns4='http://etherx.jabber.org/streams' xmlns:ns5='http://jabber.org/protocol/compress/exi' xmlns:ns6='http://jabber.org/protocol/muc' xmlns:ns7='http://jabber.org/protocol/muc#owner' xmlns:ns9='jabber:server' xmlns:ns10='jabber:x:data' xmlns:ns11='urn:ietf:params:xml:ns:xmpp-sasl' xmlns:ns12='urn:ietf:params:xml:ns:xmpp-stanzas' xmlns:ns13='urn:ietf:params:xml:ns:xmpp-streams' xmlns:ns14='urn:ietf:params:xml:ns:xmpp-tls' xmlns:ns15='urn:xmpp:exi:cs'><ns16:query xmlns:ns16='jabber:iq:auth'><ns16:username>test1</ns16:username><ns16:password>pass1</ns16:password><ns16:resource>msg.py-test</ns16:resource></ns16:query></ns8:iq>\n",
	    "<?xml version='1.0'?><exi:streamEnd xmlns:exi='http://jabber.org/protocol/compress/exi' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' />\n"};

	final String answer = "<?xml version='1.0' encoding='UTF-8'?><stream:stream xml:lang='en' to='jb.gohan.to' version='1.0' xmlns:stream='http://etherx.jabber.org/streams' xmlns='jabber:client'><iq id='hoi-1' type='set' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'><query xmlns='jabber:iq:auth'><username>test1</username><password>pass1</password><resource>msg.py-test</resource></query></iq></stream:stream>";

	System.out.println("############### testSubelementNamespace");
	PipedInputStream xep_in = new PipedInputStream();
	PipedOutputStream testxep_out = new PipedOutputStream(xep_in);
	StringWriter result_out = new StringWriter();

	Postprocessor post = new Postprocessor(xep_in, result_out);
	post.setDebug(false);

	PostprocessorTask ptask = new PostprocessorTask(post);
	Thread postThread = new Thread(ptask);
	postThread.start();

	for (String xepstr: input_xepstrs){
	    byte[] b = xepstr.getBytes();
	    testxep_out.write(b, 0, b.length);
	}
	
	testxep_out.flush();
	testxep_out.close();
	postThread.join();
	
	System.out.println("PostprocessorTest.testSubelementNamespace: Result[[[\n"+result_out.toString()+"\n]]]");

	XMLInputFactory xif = XMLInputFactory.newInstance();
	XMLStreamReader xanswer = xif.createXMLStreamReader(new ByteArrayInputStream(answer.getBytes("UTF-8")));
	XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(result_out.toString().getBytes("UTF-8")));
	
	XMLTester.assert_tags_identical(xanswer, xresult);

	System.out.println("############### testSubelementNamespace end");

	return;
    }
}


