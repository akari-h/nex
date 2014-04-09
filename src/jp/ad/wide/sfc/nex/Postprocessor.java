// -*- coding: utf-8 -*-
/**
 * 
 */
package jp.ad.wide.sfc.nex;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.SocketException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import com.siemens.ct.exi.EXIFactory;


/**
 * XEP0322 to plain XMPP
 *
 * @author akari-h
 *
 */

public class Postprocessor{
	Writer out_writer;
    DocumentStream in_docstream;
	
	final String L0TAG_START = "streamStart";
	final String L0TAG_END = "streamEnd";
	StringBuffer elementBuffer = new StringBuffer();
	XMLStreamReader xmlr;
	String localName = null;
	String levelOneStr = null;
	boolean enterElement, endFlag, xmlnsModFlag;
	int nuNum = 0;
	int level;
	String my_name = null;

	boolean debug = false;

	HashMap<String,String> nsMap = new HashMap<String,String>(); // uri -> prefix

	void debug(String msg){
		if (this.debug){
			System.err.println(this.my_name+": "+msg);
		}
	}
	public void setDebug(boolean f){
		this.debug = f;
		// ad-hoc EXI debugging routine...
		if (this.in_docstream instanceof EXIDocumentStream){
			((EXIDocumentStream)this.in_docstream).setDebug(f);
		}
	}
	public void setDebug(){
		this.setDebug(true);
	}
	public void unsetDebug(){
		this.setDebug(false);
	}

	private void initialize(Writer out_writer){
	    this.out_writer = out_writer;
		this.localName = null;
		this.levelOneStr = null;
		this.enterElement = false;
		this.endFlag = false;
		this.xmlnsModFlag = true;
		this.level = 0;
		this.xmlr = null;
		this.my_name = "Postprocessor";
		return;
	}

    /**
     * @arg in_stream : stream of documents (Reader), the input document MUST be delimited by &lt;?xml ... &gt; line
     * 
     */
		
	public Postprocessor(InputStream in_stream, Writer out_writer, EXIFactory efactory) throws XMLStreamException{
		initialize(out_writer);
		if (efactory != null){
			this.in_docstream = new EXIDocumentStream(in_stream, efactory);
		} else {
			this.in_docstream = new SimpleDocumentStreamImpl(new InputStreamReader(in_stream));
		}
	}
	public Postprocessor(InputStream in_stream, Writer out_writer) throws XMLStreamException {
		this(in_stream, out_writer, null);
	}

	public void setNickname(String newName){
		this.my_name = newName;
	}
	
	// just for testing
	public static void main(String[] args) throws Exception {
		System.err.println("Postprocessor: takes XEP0322 input and returns XMPP output");
	    Postprocessor instance = new Postprocessor(System.in, new OutputStreamWriter(System.out));
		instance.process_documents();
	}
	
	void process_documents() throws InterruptedException, IOException, XMLStreamException {
		this.debug("---DEBUG: PostProcessor.process_documents---");
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try{
			String tmpdoc = this.in_docstream.nextDocument();
			Reader current_docreader = new StringReader(tmpdoc);
			this.xmlr = xmlif.createXMLStreamReader(current_docreader);
			while (this.endFlag == false && xmlr != null && xmlr.hasNext()) {
				xmlr.next(); 
			
				switch(xmlr.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					this.debug("---START_ELEMENT---");
					this.localName = xmlr.getLocalName();
					this.debug("this.localName="+this.localName);
					if (this.localName.equals(L0TAG_START) || this.localName.equals("xmlns")){
						this.endFlag |= processEventStart();
					} else if (this.localName.equals(L0TAG_END)) {
						this.endFlag |= processEventEnd();
					} else {
						this.endFlag |= processEventDefault();
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					this.debug("---END_ELEMENT---");
					this.localName = xmlr.getLocalName();
					if (this.localName.equals(L0TAG_START) || this.localName.equals("xmlns")){
						this.endFlag |= processEventStart();
					} else {
						this.endFlag |= processEventDefault();
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					this.debug("---CHAR_ELEMENT---");
					this.endFlag |= processEventDefault();
					break;
				}
				this.debug("## process_document: current buffer state ->["+elementBuffer.toString()+"]");// TODO: should be removed after debugging
			}
		} catch (DocumentStreamEnd dse){
			// pass
		} catch (SocketException se){
			// somehow, communication broken.
			if (this.debug) se.printStackTrace();
			xmlr = null;
			try {
				this.in_docstream.close();
			} catch (Exception e){
				if (this.debug) e.printStackTrace();
			}
			try {
				this.out_writer.close();
			} catch (Exception e){
				if (this.debug) e.printStackTrace();
			}
		}
		this.debug("---finish!!---\n");
		if (xmlr != null){
			xmlr.close();
		}

	}

	// write the built XML chunk.
	private void push_stream() throws IOException {
		String docStr = elementBuffer.toString();
		this.debug("## push_stream elementBuffer=["+docStr+"]");
		out_writer.write(elementBuffer.toString());
		out_writer.flush();
		elementBuffer = new StringBuffer();
		return;
	}
    private void document_turnover() throws IOException, InterruptedException, XMLStreamException {
		push_stream();
		this.debug("--------entered new input stream reader---------\n");
		xmlr.close();
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		try{
			String nextDocument = this.in_docstream.nextDocument();
			this.debug("## nextDocument["+nextDocument+"]");
			this.xmlr = xmlif.createXMLStreamReader(new StringReader(nextDocument));
		} catch (DocumentStreamEnd e){
			endFlag = true;
			this.xmlr = null;
		}
		return;
	}
	

	void initNsMap(){
		nsMap.clear();
		// initialize with common prefix (to be used at the very first element)
		for (int i = 0; i < XMLConstants.NS_PREFIXES.length; i++){
			String prefix = XMLConstants.NS_PREFIXES[i];
			String uri = XMLConstants.NS_URIS[i];
			nsMap.put(uri, prefix);
		}
		return;
	}
	private boolean processEventStart() throws IOException, XMLStreamException, InterruptedException {
		switch(xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
			this.debug("## processEventStart START_ELEMENT");
			this.localName = xmlr.getLocalName();
			if (this.localName.equals(L0TAG_START)) {
				this.initNsMap();
				elementBuffer.append("<?xml version='1.0' encoding='UTF-8'?>");
				elementBuffer.append("<stream:stream");
				outputAttributes();
				//check Namespace
				outputNamespacesEventStart();
				elementBuffer.append(">");
			} else {
				this.debug("###mapNamespaces");
				mapNamespaces();
			}
			break;
		case XMLStreamConstants.END_ELEMENT:
			this.debug("## processEventStart END_ELEMENT");
			this.localName = xmlr.getLocalName();
			if (this.localName.equals(L0TAG_START)){
			    // document turnover;
				this.debug("## processEventStart docuemnt turnover");
			    this.document_turnover();
			}
			break;
		default:
			this.debug("## processEventStart unhandled event "+xmlr.getEventType());
			break;
		}
		this.debug("## processEventStart return");
		return false;
	}
	private boolean processEventEnd() throws IOException, XMLStreamException {
		elementBuffer.append("</stream:stream>");
		push_stream();
		return true;
	}
	private boolean processEventDefault() throws IOException, XMLStreamException, InterruptedException {
		switch(xmlr.getEventType()) {
		case XMLStreamConstants.START_ELEMENT:
			this.debug("processEventDefault: START_ELEMENT");
			if (this.levelOneStr == null) {
				this.levelOneStr = xmlr.getLocalName();
			}
			elementBuffer.append("<");
			outputName();
			outputAttributes();
			outputNamespacesDefault();
			elementBuffer.append(">");
			break;
		case XMLStreamConstants.END_ELEMENT:
			this.debug("processEventDefault: END_ELEMENT");
			elementBuffer.append("</");
			outputName();
			elementBuffer.append(">");
			this.debug("EE: this.levelOneStr="+this.levelOneStr);
			if (xmlr.getLocalName().equals(this.levelOneStr)) {
			    this.debug("## processEventDefault -- turnover by match:"+ this.levelOneStr);
			    this.document_turnover();
			    this.levelOneStr = null;
			}
			break;
		case XMLStreamConstants.CHARACTERS:
			this.debug("processEventDefault: CHARACTERS");
			int start = xmlr.getTextStart();
   			int length = xmlr.getTextLength();
   			elementBuffer.append(new String(xmlr.getTextCharacters(), start, length));
			break;
		default:
			this.debug("processEventDefault: ignored event "+xmlr.getEventType());
			break;
		}
		return false;
	}
	// write localname part with prefix translation
	void outputName(){
	    if(xmlr.hasName()){
	      String uri = xmlr.getNamespaceURI();
	      String localName = xmlr.getLocalName();
	      String firstPrefix = xmlr.getPrefix();

	      // if there is prefix check it with nsMap and output
	      if (!(firstPrefix.equals(""))) {
			  this.debug("outputName: searching for corresponding output prefix for uri "+uri+" of "+localName);
			  
			  String mapped_prefix = nsMap.get(uri);
			  if (mapped_prefix != null){
				  if (mapped_prefix.length() > 0){
					if (mapped_prefix.length() > 0){
						elementBuffer.append(mapped_prefix + ":");
					}
				  }
			  } else {
				  elementBuffer.append(firstPrefix+":");
			  }
	      }
	      // localName always on output
		  elementBuffer.append(localName);
	    }
	}
	// add namespace attribute texts to elementBuffer
	void outputAttributes(){
		for (int index=0; index < xmlr.getAttributeCount(); index++) {
			String prefix = xmlr.getAttributePrefix(index);
		    String namespace = xmlr.getAttributeNamespace(index);
		    String localName = xmlr.getAttributeLocalName(index);
		    String value = xmlr.getAttributeValue(index);
			this.debug("outputAttributes; prefix="+prefix);
			this.debug("outputAttributes; namespace="+namespace);
			this.debug("outputAttributes; localName="+localName);
			this.debug("outputAttributes; value="+value);

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
	 * supply xmlns attributes on stream:stream
	 */
	void outputNamespacesEventStart(){
		elementBuffer.append(" xmlns:stream='http://etherx.jabber.org/streams' xmlns='jabber:client'");
	    for (int index=0; index < xmlr.getNamespaceCount(); index++) {
			String prefix = xmlr.getNamespacePrefix(index);
			String uri = xmlr.getNamespaceURI(index);
		
			/* xmlns:stream and the default namespace are ignored because already added */
			if (prefix == null || prefix.length() == 0){
				continue;
			}else if (!prefix.equals("stream") ){ 
				continue;
			}
			elementBuffer.append(" xmlns:"+prefix+"=\""+ uri +"\"");
		}
	}
	void outputNamespacesDefault(){
	    for (int index=0; index < xmlr.getNamespaceCount(); index++) {
			String prefix = xmlr.getNamespacePrefix(index);
			String uri = xmlr.getNamespaceURI(index);
			boolean default_flag = false;

			if (! nsMap.containsKey(uri)){
				if (prefix == null || prefix.length() == 0) {
					elementBuffer.append(" xmlns=\""+ uri + "\"");
				}else {
					elementBuffer.append(" xmlns:"+prefix+"=\""+ uri +"\"");
				}	
			}
	    }
	}
	void mapNamespaces() {
		String prefix = null;
		String uri = null;

		for (int i = 0 ; i < xmlr.getAttributeCount(); i++){
			QName k = xmlr.getAttributeName(i);
			if (k.toString().equals("prefix")){
				prefix = xmlr.getAttributeValue(i);
			} else if (k.toString().equals("namespace")){
				uri = xmlr.getAttributeValue(i);
			} else {
				this.debug("mapNamespaces(): ignored "+k.toString());
			}
		}
		this.debug("prefix="+prefix+" : uri="+uri);
		
		if (prefix == null || prefix.length() == 0) { prefix = ""; xmlnsModFlag=true; }
		
		nsMap.put(uri, prefix);
	}
}

/**
 *  Local Variables:
 *  tab-width: 4
 *  End:
 */
