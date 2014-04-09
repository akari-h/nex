package jp.ad.wide.sfc.nex;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;


// this removes <?xml processing instructions
public class XMLfilterReader extends FilterReader{
	boolean debug = true;
	String my_name = "XMLfilterReader";
	public XMLfilterReader(Reader in) {
		super(in);
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
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int read = super.read(cbuf, off, 2048);
		this.debugPrint(" len("+len+")");
		int xmlElementSize = 0;
if(read == -1) {
			return -1;
		}
			
		this.debugPrint(" int pos = off - 1");
		int pos = off - 1;
		int markPos = 0;
		for(int readPos = off; readPos < off + read; readPos++) {
			this.debugPrint(" pos("+pos+")"+ "off("+off+")"+"read("+read+")");
			
			this.debugPrint(" this readPos("+readPos+")"+" is " + cbuf[readPos]);
			if(cbuf[readPos] == '<') {
				if(cbuf[readPos + 1] == '?') {
					markPos = readPos;
					this.debugPrint(" markPos :" + markPos + "\n");
				}
			}

			if(cbuf[readPos] == '?') {
				if(cbuf[readPos + 1] == '>') {
					
					xmlElementSize = readPos - markPos + 2; // '+1' -> '>'
					
					this.debugPrint(" readPos("+ readPos + ") - markPos(" + markPos + ")" + "\n");
					this.debugPrint(" xmlElementSize :" + xmlElementSize + "\n");
					
					this.debugPrint(" deleteString is ");
					for (int i = markPos; i < xmlElementSize; i++) {
						this.debugPrint("1:"+cbuf[i]);
						cbuf[i] = ' '; //delete xmlElement <?xml --?>
					}
					
					for (int i = markPos; i < xmlElementSize; i++) {
						this.debugPrint("2:"+cbuf[i]);
					}
				}
			}
			
			pos++;
		}
		
		int a = pos-off+1;
		this.debugPrint("pos-off+1 = " + a);
		
		return pos - off + 1;
	}
}
