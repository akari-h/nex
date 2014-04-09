/**
 * Loopback test for Preprocessor and Postprocessor
 */

package jp.ad.wide.sfc.nex;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import com.siemens.ct.exi.EXIFactory;

import static org.junit.Assert.assertEquals;
import org.junit.*;


public class LoopTest{

	byte[] testCommonCase(String[] filenames, String path, EXIFactory efactory) throws Exception {
		/**
		 * [file] | files_pipe_out = post_xep0322_in | [Postprocessor] 
		 * | post_xmpp_out = pre_xmpp_in | [Preprocessor] | pre_xep0322_out
		 */


		PipedOutputStream files_pipe_out = new PipedOutputStream();
		PipedInputStream post_xep0322_in = new PipedInputStream(files_pipe_out);

		PipedOutputStream post_xmpp_out = new PipedOutputStream();
		PipedInputStream pre_xmpp_in = new PipedInputStream(post_xmpp_out);

		Postprocessor post = null;
		if (efactory != null){
			post = new Postprocessor(post_xep0322_in, new OutputStreamWriter(post_xmpp_out), efactory);
		}else{
			post = new Postprocessor(post_xep0322_in, new OutputStreamWriter(post_xmpp_out));
		}
		post.setDebug(false);
		PostprocessorTask post_task = new PostprocessorTask(post);
		Thread postTask = new Thread(post_task);
		postTask.start();

		ByteArrayOutputStream pre_xep0322_out = new ByteArrayOutputStream();

		Preprocessor pre = new Preprocessor(new InputStreamReader(pre_xmpp_in));
		pre.setDebug(false);
		pre.suppressXMLDeclaration(true);
		Thread readerThread = null;
		Thread preprocThread = null;
		readerThread = pre.mkReaderThread(pre_xep0322_out, efactory);
		readerThread.start();
		preprocThread = pre.mkProcessorThread();
		preprocThread.start();
		
		for (String filename : filenames){
			System.out.println("file: "+filename);
			FileInputStream f_in = new FileInputStream(path+filename);
			int c = 0;
			while ((c = f_in.read()) >= 0){
				files_pipe_out.write(c);
			}
		}
		System.out.println("postTask.join()");
		postTask.join();
		System.out.println("ok.");
		readerThread.join();

		return pre_xep0322_out.toByteArray();
	}

	// ad-hoc test to check intermediate preprocessor output
	public void runPostProcessor(String[] filenames, String path, String fileoutputname, EXIFactory efactory) throws Exception {
		/**
		 * [file] | files_pipe_out = post_xep0322_in | [Postprocessor] | File output
		 */

		PipedOutputStream files_pipe_out = new PipedOutputStream();
		PipedInputStream post_xep0322_in = new PipedInputStream(files_pipe_out);

		FileOutputStream post_xmpp_out = new FileOutputStream(fileoutputname);

		Postprocessor post = null;
		if (efactory != null){
			post = new Postprocessor(post_xep0322_in, new OutputStreamWriter(post_xmpp_out), efactory);
		}else{
			post = new Postprocessor(post_xep0322_in, new OutputStreamWriter(post_xmpp_out));
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

		postTask.join();
		
	}

    @Test
	public void runPostproc() throws Exception{
		/*
		  input: sample/exi/*.xml
		  Will be given to Postprocessor as XEP-0322 XML (or EXI?)
		  Postprocessor's output (XMPP) are connected to Prepropcessor
		  Preprocessor's output (XEP-0322 XML or EXI) are given back and checked if 
		  it's identical or not.
		 */

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
		final String output = "looptest-postproc-output.xml";

		this.runPostProcessor(filenames, path, output, null);
	}

	// TODO: result check not yet working
    @Test
	public void testLoopback() throws Exception{
		/*
		  input: sample/exi/*.xml
		  Will be given to Postprocessor as XEP-0322 XML (or EXI?)
		  Postprocessor's output (XMPP) are connected to Prepropcessor
		  Preprocessor's output (XEP-0322 XML or EXI) are given back and checked if 
		  it's identical or not.
		 */

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
		final String answer = "sample/looptest-answer.xml";

		byte[] result_bytes = this.testCommonCase(filenames, path, null);
		String result = new String(result_bytes);

		System.out.println("## RESULT[[[\n"+result+"\n]]]");
			
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLStreamReader xsrc = xif.createXMLStreamReader(new FileInputStream(answer));
		XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(("<answer>"+result+"</answer>").getBytes("UTF-8")));
		XMLTester.assert_tags_identical(xsrc, xresult);

		return;
    }
	
	@Test
	@Ignore
	public void testLoopbackEXI() throws Exception {
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
		final String[] answers = {
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
		final String schema_location = "sample/schema_localized/00_canonical_example01.xsd";

		EXIFactory ef = EXIUtil.prepare_EXIFactory(schema_location);

		Assert.assertEquals(filenames.length, answers.length);

		byte[] result_bytes = this.testCommonCase(filenames, path, ef);

		XMLInputFactory xif = XMLInputFactory.newInstance();
		
		InputStream exis_in = new ByteArrayInputStream(result_bytes);
		EXIDocumentStream eds = new EXIDocumentStream(exis_in, ef);
		ArrayList<String> results_0322_xml = new ArrayList<String>();
		try {
			while (true){
				results_0322_xml.add(eds.nextDocument());
			}
		} catch (DocumentStreamEnd dse){
		}
		Assert.assertEquals(answers.length, results_0322_xml.size());
		for (int i = 0; i < answers.length; i++){
			String filename = answers[i];
			String result_str = results_0322_xml.get(i);
			XMLStreamReader xanswer = xif.createXMLStreamReader(new FileInputStream(path+filename));
			XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(result_str.getBytes("UTF-8")));
				
			XMLTester.assert_tags_identical(xanswer, xresult);
		}
	}
}

/**
 *  Local Variables:
 *  tab-width: 4
 *  End:
 */

