// helper class for Postprocessor

package jp.ad.wide.sfc.nex;

import java.io.*;
import javax.xml.stream.*;

import com.siemens.ct.exi.EXIFactory;

import static org.junit.Assert.assertEquals;
import org.junit.*;


class PostprocessorTask implements Runnable {
    Postprocessor post;
    public PostprocessorTask(Postprocessor post){
	this.post = post;
    }

    public void run(){
	try{
	    post.process_documents();
	}catch (Exception e){
	    e.printStackTrace();
	}
    }
}
