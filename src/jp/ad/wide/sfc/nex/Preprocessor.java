package jp.ad.wide.sfc.nex;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.NamespaceContext;

import com.siemens.ct.exi.EXIFactory;

/** 
 * External reader thread for output
 */
class ReaderThread extends Thread {
	Preprocessor processor;
	OutputStream out_stream;
	EXIFactory efactory;
	boolean debugflag = false;
	/* if efactory is null, it does plain XML output. */
	ReaderThread(Preprocessor processor, OutputStream out_stream, EXIFactory efactory) {
		this.processor = processor;
		this.out_stream = out_stream;
		this.efactory = efactory;
	}
	void setDebug(boolean f){
		this.debugflag = f;
	}
	void debuglog(String msg){
		if (this.debugflag){
			System.err.println(msg);
		}
	}
	public void run() {
		try{
			while (true){
				debuglog("## ReaderThread: nextDocument();");
				String document = processor.nextDocument();
				if (document == null){
					break;
				}

				debuglog("## ReaderThread::sending XEP0322: [["+document+"]]");
				if (efactory != null){
					ByteArrayInputStream docstream = new ByteArrayInputStream(document.getBytes());
					EXIUtil.encode_stream(out_stream, docstream, efactory);
				} else {
					out_stream.write(document.getBytes());
				}
				out_stream.flush();
			}
			out_stream.flush();
			out_stream.close();
		}catch (InterruptedException ie){
			// ignore
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}

/**
 * Internal processor thread just for Preprocessor.process_all()
 */
class ProcessorThread extends Thread {
	Preprocessor processor;
	ProcessorThread(Preprocessor processor){
		this.processor = processor;
	}
	public void run() {
		try{
			this.processor.process_all();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}


/**
 * Plain XMPP to XEP-0322
 */


public class Preprocessor implements DocumentStream {
	
	boolean debug = false; // to dump the debug log, please call preprocessor.setDebug()
	final String L0TAG = "stream";
	StringBuffer elementBuffer = new StringBuffer();
	Reader in_stream;
	XMLStreamReader xmlr;
	int level, endBuffer, attr;	
	String levelOneStr, localName;
	boolean endFlag, levelModFlag;
	boolean no_more_document = false; //indicates no more document from the documentstream
	boolean suppress_xml_declaration = false;

	String my_name = null;
	
	BlockingQueue<String> docQueue = new LinkedBlockingQueue<String>();
	HashMap<String,String> nsMap = new HashMap<String,String>();


	void debugPrint(String msg){
		if (this.debug){
			System.err.println(this.my_name+": "+msg);
		}
	}
	public void setDebug(boolean f){
		this.debug = f;
	}
	public void setDebug(){
		this.setDebug(true);
	}
	public void unsetDebug(){
		this.setDebug(false);
	}
	
	public Preprocessor(Reader in_stream) throws XMLStreamException {
		this.level = 0;
		this.levelModFlag = false;
		this.levelOneStr = null;
		this.localName = null;
		this.endFlag = new Boolean(false);
		this.in_stream = in_stream;
		this.my_name = "Preprocessor";
		this.debugPrint("created.");
	}
	public Preprocessor(Reader in_stream, String nickname) throws XMLStreamException {
		this(in_stream);
		this.my_name = nickname;
	}

	void suppressXMLDeclaration(boolean f){
		this.suppress_xml_declaration = f;
	}
	void suppressXMLDeclaration(){
		this.suppressXMLDeclaration(true);
	}
	
	public static void main(String[] args) throws Exception {	
		System.err.println("Preprocessor: takes XMPP input and returns XEP0322 output");
		//--console input version--
		Preprocessor instance = new Preprocessor(new InputStreamReader(System.in));
		//instance.setDebug(true);
		Thread rt = instance.mkReaderThread(System.out);
		Thread pt = instance.mkProcessorThread();
		rt.start();
		pt.start();
		pt.join();
		rt.join();
	}

	public Thread mkReaderThread(OutputStream out_stream){
		ReaderThread rt = new ReaderThread(this, out_stream, null);
		rt.setDebug(this.debug);
		return (Thread) rt;

	}
	public Thread mkReaderThread(OutputStream out_stream, EXIFactory efactory){
		return (Thread) new ReaderThread(this, out_stream, efactory);
	}
	public Thread mkProcessorThread(){
		return (Thread) new ProcessorThread(this);
	}

	public void process_all() throws XMLStreamException, IOException, InterruptedException {
		
		this.debugPrint("process_all() start");
		XMLInputFactory xmlif = XMLInputFactory.newInstance(); 
		this.debugPrint("createXMLStreamReader");
		XMLfilterReader xfr = new XMLfilterReader(in_stream);
		xfr.setDebug(this.debug);
		this.xmlr = xmlif.createXMLStreamReader(xfr);
		this.debugPrint("createedXMLStreamReader");
		while (this.endFlag == false) {
			if (this.levelModFlag == false) {
				if (this.xmlr.hasNext()){
					this.debugPrint("xmlr.next");
					this.xmlr.next();
				} else {
					break;
				}
			}
			switch(this.level) {
			case 0:
				this.debugPrint("processEventZero");
				this.endFlag = processEventZero(xmlr);
				break;
			case 1:
				this.debugPrint("processEventOne");
				this.endFlag = processEventOne(xmlr);
				break;			
			case 2:
				this.debugPrint("processEventTwo");
				this.endFlag = processEventTwo(xmlr);
				break;			
			}
		}
		xmlr.close();
		docQueue.add(""); // indicates EOF.
		this.debugPrint("process_all() end");
	}
	
	private void push_stream() throws IOException {
		if (! suppress_xml_declaration){
			elementBuffer.insert(0, "<?xml version='1.0'?>");
		}
		this.debugPrint("push_stream(["+elementBuffer.toString()+"])");
		docQueue.add(elementBuffer.toString());
		elementBuffer = new StringBuffer();
		return;
	}
	private void document_turnover() throws IOException, InterruptedException, XMLStreamException {
		this.debugPrint("--------entered new input stream reader---------\n");

		xmlr.close();

		this.debugPrint("### This.level = 1");
		this.level = 1;
		this.levelOneStr = null;

		return;
	}
	
	public String nextDocument() throws InterruptedException {
		if ( this.no_more_document ){
			return null;
		}

		String retDoc = docQueue.take();
		if (retDoc.length() == 0){
			// virtual end of file
			this.no_more_document = true;
			this.debugPrint("no more document.");
			return null;
		}
		return retDoc;
	}
	public void close() throws IOException {
		this.no_more_document = true;
		this.in_stream.close();
	}
	
	private boolean processEventZero(XMLStreamReader xmlr) throws IOException, XMLStreamException {
		
		switch(xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
  			
			this.localName = xmlr.getLocalName();
			
  			if (localName.equals(L0TAG)) {
  				elementBuffer.append("<exi:streamStart");
  				outputAttributes(xmlr);
  				mapNamespaces(xmlr);
  				outputNamespaces(xmlr);
				elementBuffer.append(" xmlns:exi=\"http://jabber.org/protocol/compress/exi\"");
  				elementBuffer.append(">");  				
  				outputNamespaceList(xmlr);
  				elementBuffer.append("</exi:streamStart>");
  				
  				push_stream();
  				
   				this.level++;
   			} else if (localName.equals("xml")) { 
				// FIXME: This is not acceptable hack.
   				push_stream();
     		}else {
   				this.debugPrint("--ERROR: given localname is "+localName+", expected "+L0TAG+"--");
   			}
  			
			break;
		case XMLStreamConstants.END_ELEMENT:
			// do end element processing
			this.localName = xmlr.getLocalName();
			if (localName.equals(L0TAG)) {
				elementBuffer.append("<exi:streamEnd");
				outputNamespaceListLevelOne(xmlr);
				elementBuffer.append(" />");
				nsMap.clear();
				push_stream();
				this.endFlag = true;
				return this.endFlag;
			}
			break;
		case XMLStreamConstants.CHARACTERS:
			// do characters processing
			break;
		case XMLStreamConstants.COMMENT:
			// do comment processing
			break;
		case XMLStreamConstants.START_DOCUMENT:
			// do start processing
			break;
		case XMLStreamConstants.END_DOCUMENT:
			// do end processing
			break;
		default:
			//ignore?
			break;
		}
		return false; //if end of stream return true
	}
	
	private boolean processEventOne(XMLStreamReader xmlr) throws IOException, XMLStreamException, InterruptedException {
		switch(xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
			// do start element processing
			localName = xmlr.getLocalName();
			if (localName.equals(L0TAG)) {
				elementBuffer = new StringBuffer();
				processEventZero(xmlr);
				document_turnover();
			}else if (this.levelOneStr == null) {
				//elementBuffer.append("<"+localName+">");
				
  				elementBuffer.append("<");
				outputName(xmlr);
  				outputAttributes(xmlr);
  				outputNamespaces(xmlr);
  				outputNamespaceListLevelOne(xmlr);
  				elementBuffer.append(">");
  				
				this.levelOneStr = localName;
				this.level++;
			}			
			break;
		case XMLStreamConstants.END_ELEMENT:
			// from processEventTwo
			// do end element processing
			this.localName = xmlr.getLocalName();
			if (localName.equals(L0TAG)) {
				level--;
				this.levelModFlag = true;
			}else{
				push_stream();
				this.levelOneStr = null;
				this.levelModFlag = false;
			}
			break;
		case XMLStreamConstants.CHARACTERS:
			// do characters processing
			break;
		case XMLStreamConstants.COMMENT:
			// do comment processing
			break;
		case XMLStreamConstants.START_DOCUMENT:
			// do start processing
			break;
		case XMLStreamConstants.END_DOCUMENT:
			// do end processing
			break;
		default:
			//ignore?
			break;
		}
		return false; //if end of stream return true		
	}
	private boolean processEventTwo(XMLStreamReader xmlr) throws IOException, XMLStreamException, InterruptedException {

		switch(xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
			// do start element processing
			this.localName = xmlr.getLocalName();
			if (localName.equals(L0TAG)) {
				elementBuffer = new StringBuffer();
				processEventZero(xmlr);
				document_turnover();
			} else {
				elementBuffer.append("<");
				outputName(xmlr);
  				outputAttributes(xmlr);
  				outputNamespaces(xmlr);
  				elementBuffer.append(">");
			}
			break;
		case XMLStreamConstants.END_ELEMENT:
			// do end element processing
			localName = xmlr.getLocalName();
			elementBuffer.append("</");
			outputName(xmlr);
			elementBuffer.append(">");
			this.levelModFlag = false;
			if (localName.equals(levelOneStr)) {
				this.level--;
				this.levelModFlag = true;
			}
			break;
		case XMLStreamConstants.CHARACTERS:
			// do characters processing
   			int start = xmlr.getTextStart();
   			int length = xmlr.getTextLength();
   			elementBuffer.append(new String(xmlr.getTextCharacters(), start, length));
   			break;
		case XMLStreamConstants.COMMENT:
			// do comment processing
			break;
		case XMLStreamConstants.START_DOCUMENT:
			// do start processing
			break;
		case XMLStreamConstants.END_DOCUMENT:
			// do end processing
			break;
		default:
			//ignore?
			break;
		}
		return false; //if end of stream return true	
	}
	
	public void outputName(XMLStreamReader xmlr){
	    if(xmlr.hasName()){
	      String prefix = xmlr.getPrefix();
	      String uri = xmlr.getNamespaceURI();
	      String localName = xmlr.getLocalName();
	      
	      String firstPrefix = xmlr.getPrefix();
	      if (!(firstPrefix.equals(""))) elementBuffer.append(firstPrefix + ":");
		  elementBuffer.append(localName);
	    }
	}
	public void outputAttributes(XMLStreamReader xmlr){
		for (int i=0; i < xmlr.getAttributeCount(); i++) {
		    String prefix = xmlr.getAttributePrefix(i);
		    String namespace = xmlr.getAttributeNamespace(i);
		    String localName = xmlr.getAttributeLocalName(i);
		    String value = xmlr.getAttributeValue(i);

		    elementBuffer.append(" ");

			if (namespace != null && (prefix == null || prefix.length() == 0)){
				NamespaceContext nc = xmlr.getNamespaceContext();
				prefix = nc.getPrefix(namespace);
			}
			if (prefix != null && prefix.length() > 0){
				elementBuffer.append(prefix+":");
			}
		    elementBuffer.append(localName+"='"+value+"'");
		}
	}
	/**
	 * Supply xmlns attributes for each stanzas
	 */
	public void outputNamespaces(XMLStreamReader xmlr){
	    for (int index=0; index < xmlr.getNamespaceCount(); index++) {
			String prefix = xmlr.getNamespacePrefix(index);
			String uri = xmlr.getNamespaceURI(index);
		
			if (prefix == null){
				elementBuffer.append(" xmlns=\""+ uri + "\"" );
			} else if (prefix.equals("exi")){
				// skip exi to avoid duplicated entry
			}else{
				elementBuffer.append(" xmlns:"+prefix+"=\""+ uri +"\"");
			}

	    }
	}
	public void outputNamespaceListLevelOne(XMLStreamReader xmlr){	
		for(Iterator<Map.Entry<String,String>> it = nsMap.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<String, String> entry = it.next();
			String uri = entry.getKey();
			String prefix = entry.getValue();
							
			if (prefix == null) {
				if (this.localName.equals(L0TAG)){
					elementBuffer.append(" xmlns=\""+ uri + "\"");
				}
			} else {	
				elementBuffer.append(" xmlns:"+prefix+"=\""+ uri +"\"");
			}
		} 
	}
	// load namespace information from XML and record it in nsMap
	public void mapNamespaces(XMLStreamReader xmlr){
	    for (int index=0; index < xmlr.getNamespaceCount(); index++) {
			String prefix = xmlr.getNamespacePrefix(index);
			String uri = xmlr.getNamespaceURI(index);

			nsMap.put(uri, prefix);
			nsMap.put("http://jabber.org/protocol/compress/exi", "exi");
	    }
	}
	public void outputNamespaceList(XMLStreamReader xmlr){	
		for(Iterator<Map.Entry<String,String>> it = nsMap.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<String, String> entry = it.next();
			String uri = entry.getKey();
			String prefix = entry.getValue();
							
			elementBuffer.append(" ");
			if (prefix == null) {
				elementBuffer.append("<exi:xmlns prefix=\"\" namespace=\""+uri+"\" />");
			} else {	
				elementBuffer.append("<exi:xmlns prefix=\""+prefix+"\" namespace=\""+uri+"\" />");
			}
		} 
	}
}

/**
 *  Local Variables:
 *  tab-width: 4
 *  End:
 */
