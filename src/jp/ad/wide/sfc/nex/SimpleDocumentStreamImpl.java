package jp.ad.wide.sfc.nex;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.PushbackReader;


/**
 * make a DocumentStream class from a stream of document with 
 * &lt;?xml ... ?&gt; line as a hint.
 */

// a stupid implementation of DocumentStream
public class SimpleDocumentStreamImpl implements DocumentStream {
    PushbackReader in_reader;
    StringBuffer document_buf = new StringBuffer();
    StringBuffer token_buf = new StringBuffer();
    
    public SimpleDocumentStreamImpl(Reader in_reader){
	this.in_reader = new PushbackReader(in_reader);
    }
    
    int readUnlessNextChar(char target) throws IOException {
	int c;
	while ((c = in_reader.read()) >= 0){
	    if (c == target){
		in_reader.unread(c);
		break;
	    }
	    token_buf.append((char)c);
	}
	if (c < 0 && token_buf.length() == 0){
	    return -1;
	}

	return 0;
    }
    int readUptoNextChar(char target) throws IOException {
	int c;
	while ((c = in_reader.read()) >= 0){
	    token_buf.append((char)c);
	    if (c == target){
		break;
	    }
	}
	if (c < 0 && token_buf.length() == 0){
	    return -1;
	}

	return 0;
    }
    int readToNextTagStart() throws IOException {
	return readUnlessNextChar('<');
    }
    int readToNextTagEnd() throws IOException {
	return readUptoNextChar('>');
    }
    // this is stupid. FIXME with proper handling of quotes, escaped quotes, comments, etc.
    String readNextToken() throws IOException {
	if (token_buf.length() == 0){
	    if (readToNextTagStart() < 0){
		return null; // EOF
	    }
	}
	if (token_buf.length() == 0){
	    if (readToNextTagEnd() < 0){
		return null;
	    }
	}
	String r = token_buf.toString();
	//System.err.println("## readNextToken -> "+r);
	if ((r.startsWith("<?")) == false && r.matches("[ \n]+") == false) {
		document_buf.append(r);
	}
	token_buf = new StringBuffer();
	return r;
    }

    String nameOf(String tag){
	int start_idx = 1;
	if (tag.charAt(start_idx) == '/'){
	    start_idx = 2;
	}
	int end_idx = tag.indexOf(' ');
	if (end_idx == -1){
	    end_idx = tag.length() - 1; 
	    if (tag.charAt(end_idx) == '/'){
		end_idx --;
	    }
	}
	return tag.substring(start_idx, end_idx);
    }
    String flushDocument(){
	//System.err.println("##flushDocument");
	String rdoc = document_buf.toString();
	document_buf = new StringBuffer();
	return rdoc;
    }
    public String nextDocument() throws InterruptedException, IOException, DocumentStreamEnd {
	String root_open = null;
	//System.err.println("## before root element");
	while (root_open == null){
	    String token = readNextToken();
	    if (token == null){
		throw new DocumentStreamEnd(); // end of file
	    }
	    if (token.startsWith("<") && !token.startsWith("<?")) {
		// this must be the root element
		if (token.endsWith("/>")){
		    	
		    return flushDocument();
		}
		// else
		root_open = nameOf(token);
	    }
	}
	//System.err.println("## until the root element ("+root_open+") close");
	String lastCloseTagName = "";
	while (! lastCloseTagName.equals(root_open)){
	    String token = readNextToken();
	    if (token == null){
		throw new DocumentStreamEnd(); // end of file??? 
	    }
	    if (token.startsWith("</")){
		lastCloseTagName = nameOf(token);
	    }
	}
	return flushDocument();
    }
    public void close() throws IOException {
	this.in_reader.close();
    }
    

    public static void main(String[] args) {
	String sample = "<?xml version='1.0'?>\n<a><b/></a>\n<?xml version='1.0'?>\n<a><c/></a>\n";

	try{
	    DocumentStream ds = new SimpleDocumentStreamImpl(new InputStreamReader(System.in));
	    System.out.println("first is:\n"+ds.nextDocument());
	    System.out.println("next is:\n"+ds.nextDocument());
	} catch (Exception e){
	    e.printStackTrace();
	}
	return;
    }
}