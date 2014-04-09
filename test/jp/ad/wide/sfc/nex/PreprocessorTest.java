/**
 * Preprocessor Test
 */
package jp.ad.wide.sfc.nex;

import java.io.*;
import javax.xml.stream.*;

import com.siemens.ct.exi.EXIFactory;

import static org.junit.Assert.assertEquals;
import org.junit.*;

public class PreprocessorTest {
    void subtestSinglePreprocess(String xmpp_logfilename, String xep0322_logfilename, EXIFactory efactory_maybenull) throws Exception {
		PipedInputStream xmpp_in = new PipedInputStream();
		PipedOutputStream xmpp_pipeout = new PipedOutputStream(xmpp_in);
		Preprocessor pre = new Preprocessor(new InputStreamReader(xmpp_in));
		pre.suppressXMLDeclaration(); // for testing 
		ByteArrayOutputStream xep0322_out = new ByteArrayOutputStream();
		Thread readerThread = null;
		Thread processorThread = null;
		processorThread = pre.mkProcessorThread();
		processorThread.start();
		readerThread = pre.mkReaderThread(xep0322_out, efactory_maybenull);
		readerThread.start();

		FileInputStream f_xmpp_in = new FileInputStream(xmpp_logfilename);
	
		int c;
		while (true) {
			c = f_xmpp_in.read();
			if (c < 0) break;
			xmpp_pipeout.write(c);
		}
	
		f_xmpp_in.close();
		xmpp_pipeout.close();

		processorThread.join();
		System.out.println("## process finished.");
		readerThread.join();
	
		if (efactory_maybenull == null){
			// result must be plain XML
			System.out.println("## PreprocessorTest.RESULT[[[\n"+xep0322_out.toString()+"\n]]]");

			XMLInputFactory xif = XMLInputFactory.newInstance();
			XMLStreamReader xsrc = xif.createXMLStreamReader(new FileInputStream(xep0322_logfilename));
			XMLStreamReader xresult = xif.createXMLStreamReader(new ByteArrayInputStream(("<answer>"+xep0322_out.toString()+"</answer>").getBytes("UTF-8")));
			XMLTester.assert_tags_identical(xsrc, xresult);
		} else {
			byte[] exibytes = xep0322_out.toString().getBytes();
			byte[] answerbytes = new byte[1024]; 
			Assert.assertTrue(exibytes.length <= answerbytes.length);

			FileOutputStream logfile = new FileOutputStream("PreprocessorTest_log.exi");
			logfile.write(exibytes);
			logfile.close();
			FileInputStream answerfile = new FileInputStream(xep0322_logfilename);
			int answerlen = answerfile.read(answerbytes);
			Assert.assertEquals(answerlen, exibytes.length);
			for (int i = 0; i < answerlen; i++){
				Assert.assertEquals(exibytes[i], answerbytes[i]);
			}
		}
		return;
    }

    @Test
    public void testXML() throws Exception{
		subtestSinglePreprocess("sample/01-2-sample.xml", "sample/01-2-sample-result.xml", null);
    }
	
    @Test
	@Ignore
    public void testEXI() throws Exception {
		final String schema_location = "sample/schema_localized/00_canonical_example01.xsd";
		//EXIFactory ef = EXIUtil.prepare_EXIFactory(schema_location, false, false, null);
		EXIFactory ef = EXIUtil.prepare_EXIFactory(schema_location);
		subtestSinglePreprocess("sample/01-2-sample.xml", "sample/01-2-sample-exi/N-SIs.exi", ef);
    }
}

	
	
/**
 *  Local Variables:
 *  tab-width: 4
 *  End:
 */
	
