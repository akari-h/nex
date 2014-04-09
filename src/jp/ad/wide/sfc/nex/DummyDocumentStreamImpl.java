package jp.ad.wide.sfc.nex;
import java.io.IOException;

public class DummyDocumentStreamImpl implements DocumentStream {
    String[] dummy_document = {"<?xml version='1.0'?><exi:streamStart from='im.example.com' to='juliet@im.example.com' id='++TR84Sm6A3hnt3Q065SnAbbk3Y=' version='1.0' lang='en' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' xmlns:exi='http://jabber.org/protocol/compress/exi'><exi:xmlns prefix='exi' namespace='http://jabber.org/protocol/compress/exi' /><exi:xmlns prefix='' namespace='jabber:client' /><exi:xmlns prefix='stream' namespace='http://etherx.jabber.org/streams' /></exi:streamStart>", "<?xml version='1.0'?><features xmlns:exi='http://jabber.org/protocol/compress/exi' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'> <starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'>  <required>  </required> </starttls></features>", "<?xml version='1.0'?><exi:streamEnd xmlns:exi='http://jabber.org/protocol/compress/exi' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'/>"};
    int docCount = 0;
	

    public String nextDocument() throws InterruptedException, IOException {
	if (docCount < 0){
	    throw new IOException();
	}
	String doc = dummy_document[docCount%dummy_document.length];
	docCount ++;
	return doc;
    }
    public void close() throws IOException{
	docCount = -1;
    }

    public static void main(String[] args){
	try {
	    DummyDocumentStreamImpl dummy = new DummyDocumentStreamImpl();
	    System.out.println(dummy.nextDocument());
	    System.out.println(dummy.nextDocument());
	    System.out.println(dummy.nextDocument());
	    System.out.println(dummy.nextDocument());
	}catch(Exception e){
	    e.printStackTrace();
	}
    }
}

