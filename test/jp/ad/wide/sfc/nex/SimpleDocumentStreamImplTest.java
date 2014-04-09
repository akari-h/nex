/**
 * SimpleDocumentStreamImpl Test
 */

package jp.ad.wide.sfc.nex;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;


import static org.junit.Assert.assertEquals;
import org.junit.*;

public class SimpleDocumentStreamImplTest {
    @Test
    public void testSimple() throws Exception {
	String input_string = "<?xml version='1.0'?>\n<a><b/></a>\n<?xml version='1.0'?>\n<a><c/></a>\n";
	DocumentStream ds = new SimpleDocumentStreamImpl(new StringReader(input_string));
	Assert.assertEquals("<a><b/></a>", ds.nextDocument());
	Assert.assertEquals("<a><c/></a>", ds.nextDocument());
	try{
	    ds.nextDocument();
	    Assert.fail();
	} catch (DocumentStreamEnd dse){
	    // pass
	}
    }
}