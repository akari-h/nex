/**
 * EXI Document Stream
 */
package jp.ad.wide.sfc.nex;


import com.siemens.ct.exi.EXIFactory;
import jp.ad.wide.xep0322.CountingInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Split Streamlined EXI documents and provide it as *decoded* DocumentStream
 */

public class EXIDocumentStream implements DocumentStream {
    InputStream exi_instream;
    EXIFactory efactory;
    boolean debug = false;
    public EXIDocumentStream(InputStream exi_instream, EXIFactory efactory){
	this.exi_instream = new CountingInputStream(new BufferedInputStream(exi_instream));
	this.efactory = efactory;
    }
    public void setDebug(boolean flag){
	this.debug = flag;
    }
    // stops at $EXI or some byte with distinguish bit is true
    void find_next_head() throws DocumentStreamEnd, IOException {
	int c = 0;
	while (true){
	    this.exi_instream.mark(1);
	    if ((c = this.exi_instream.read()) < 0){
		throw new DocumentStreamEnd();
	    }
	    if (c == '$' || c > 127){
		this.exi_instream.reset();
		return;
	    }
	}
    }
    public String nextDocument() throws InterruptedException, IOException, DocumentStreamEnd {
	// check EOF -- why there are no convenient way to check it?
	find_next_head();
	
	ByteArrayOutputStream xml_stream = new ByteArrayOutputStream();
	EXIUtil.decode_stream(xml_stream, this.exi_instream, this.efactory);
	
	String s = xml_stream.toString();
	if (this.debug){
	    System.out.println("EXIDocumentStream received [[[\n"+s+"\n]]]");
	}
	return s;
    }
    public void close() throws IOException {
	this.exi_instream.close();
    }
}