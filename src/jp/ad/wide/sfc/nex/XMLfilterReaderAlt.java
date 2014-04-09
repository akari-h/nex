package jp.ad.wide.sfc.nex;

import java.io.FilterReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

// alternative implementation of XML filter reader
public class XMLfilterReaderAlt extends FilterReader{
    boolean debug = false; // to use, call setDebug
    char[] buffer = new char[16];
    int buffer_next_write = 0;
    int buffer_next_read = 0;
    static final int STATE_OPEN = 0; // read buffer then stream
    static final int STATE_VIRTUALEOF = 1; // wait reset --> OPEN
    static final int STATE_REALEOF = 2; // -> terminate
    static final int STATE_CLOSED = 3; // manually closed -(reset)->OPEN
    static final int STATE_OPEN_QUOTED = 16; // currently in single quote (no special handling);
    int quoted_char = 0;
    
    int state = STATE_OPEN;

    final String target = "<?xml ";
    final String my_name = "XMLfilterReader";

    public static XMLfilterReaderAlt newInstance(Reader in) throws IOException{
	if (! in.markSupported()){
	    in = new BufferedReader(in);
	}
	return new XMLfilterReaderAlt(in);
    }
    public XMLfilterReaderAlt(Reader in) throws IOException {
	super(in);
	// push single char to ignore <? at the beginning
	int r = in.read();
	if (r < 0) {
	    this.state = STATE_REALEOF;
	} else {
	    buffer[buffer_next_write++] = (char)r;
	}
    }
    public XMLfilterReaderAlt(Reader in, boolean debug) throws IOException {
	this(in);
	this.setDebug(debug);
    }
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
    public boolean isRealEOF(){
	return (this.state == STATE_REALEOF);
    }

    boolean buffer_ready(){
	return (buffer_next_read < buffer_next_write);
    }
    int read1_from_buffer(){
	int nextc = buffer[buffer_next_read];
	buffer_next_read++;
	return nextc;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
	debugPrint("read from stream (bulk)");

	if (this.state == STATE_REALEOF && !buffer_ready()){
	    return -1;
	}

	int read_count = 0;
	while (read_count < len){
	    int r = this.read();
	    if (r < 0){
		return read_count;
	    }
	    cbuf[off+read_count] = (char)r;
	    read_count++;
	}
	debugPrint("read count = "+read_count);
	return read_count;
    }

    @Override
    public int read() throws IOException {
	debugPrint("read from stream (single)");
	switch (this.state){
	case STATE_OPEN:
	    debugPrint("STATE_OPEN");
	    if (buffer_ready()){
		return read1_from_buffer();
	    } else if (buffer_next_read == buffer_next_write){
		buffer_next_read = buffer_next_write = 0; // all written. reset.
		int c1 = super.read();
		debugPrint("read "+(char)c1);
		if (c1 < 0){
		    debugPrint("->STATE_REALEOF(OPEN c1)");
		    this.state = STATE_REALEOF;
		    return -1;
		} else if (c1 == '"' || c1 == '\''){
		    debugPrint("->STATE_OPEN_QUOTED("+c1+")");
		    this.quoted_char = c1;
		    this.state = STATE_OPEN_QUOTED;
		    return c1;
		} else if (c1 != '<'){
		    debugPrint("->STATE_OPEN(no &lt;)");
		    return c1;
		}		    

		// c1 is '<'. state -> VIRTUALEOF if next chars are '?xml '
		this.buffer[buffer_next_write++] = (char)c1;
		int buffer_next_write_mark = buffer_next_write;
		super.mark(target.length());

		int read_left = target.length()-1; // offset by 1 for initial '<' (e.g. c1)
		while (read_left > 0){
		    int read_len = super.read(this.buffer, 1, read_left); 
		    if (read_len < 0){
			debugPrint("(OPEN short read)");
			break;
		    }
		    buffer_next_write += read_len;
		    read_left -= read_len;
		}
		String s = new String(this.buffer, 0, target.length());
		if (s.equals(target)){
		    debugPrint("->STATE_VIRTUALEOF");
		    this.state = STATE_VIRTUALEOF;
		    return -1; // next start from buffer
		}
		// drop the buffer and reset
		super.reset();
		buffer_next_write = buffer_next_write_mark;
		// anyway buffer should be ready
		debugPrint("->STATE_OPEN(end)");
		return read1_from_buffer();
	    } 
	    throw new Error("oops -- internal error(should not happen).");
	case STATE_OPEN_QUOTED:
	    debugPrint("STATE_OPEN_QUOTED");
	    if (buffer_ready()){
		return read1_from_buffer();
	    }
	    // else
	    
	    int c1 = super.read();
	    if (c1 < 0){
		debugPrint("->STATE_REALEOF(QUOTED c1)");
		this.state = STATE_REALEOF;
	    } else if (c1 == '\\'){
		int c2 = super.read();
		if (c2 < 0){
		    debugPrint("->STATE_REALEOF(QUOTED c2)");
		    this.state = STATE_REALEOF;
		} else {
		    // move c2 to buffer to 'ignore \' quote'
		    this.buffer[buffer_next_write++] = (char)c2;
		}
	    } else if (c1 == this.quoted_char){
		this.state = STATE_OPEN;
	    }
	    return c1;
	case STATE_REALEOF:
	    debugPrint("STATE_REALEOF");
	    if (buffer_ready()){
		// state is closed but something to read is there.
		return read1_from_buffer();
	    }
	    // else
	    return -1;
	case STATE_VIRTUALEOF:
	    debugPrint("STATE_VIRTUALEOF");
	    return -1;
	case STATE_CLOSED:
	    debugPrint("STATE_CLOSED");
	    return -1;
	default:
	    throw new Error("oops -- unknown state "+this.state);
	}
    }
    @Override
    public void close() throws IOException {
	// virtual close.
	this.state = STATE_CLOSED;
    }

    public void prepareNextStream() throws IOException {
	switch (this.state){
	case STATE_VIRTUALEOF:
	    this.state = STATE_OPEN;
	    return;
	default:
	    // ignored
	}
    }

    

}