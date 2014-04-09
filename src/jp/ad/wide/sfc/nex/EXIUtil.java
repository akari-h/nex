/**
 * Utility functions for EXI handling with EXIficient
 */

package jp.ad.wide.sfc.nex;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.siemens.ct.exi.helpers.DefaultEXIFactory;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.FidelityOptions;
import com.siemens.ct.exi.GrammarFactory;
import com.siemens.ct.exi.grammars.Grammars;
import com.siemens.ct.exi.EncodingOptions;
import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.api.sax.EXISource;
import com.siemens.ct.exi.exceptions.EXIException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;


public class EXIUtil {
    public static EXIFactory prepare_EXIFactory(String schemaLocation, boolean strict, boolean include_header_opt, String schemaId) throws EXIException {
	EXIFactory efactory = DefaultEXIFactory.newInstance();
	if (strict){
	    FidelityOptions fo = FidelityOptions.createStrict();
	    efactory.setFidelityOptions(fo);
	}
	// always bitpacked 
	efactory.setCodingMode(CodingMode.BIT_PACKED);
	GrammarFactory gf = GrammarFactory.newInstance();
	Grammars g = gf.createGrammars(schemaLocation);
	
	EncodingOptions eo = EncodingOptions.createDefault();
	if (schemaId != null){
	    g.setSchemaId(schemaId);
	    eo.setOption(EncodingOptions.INCLUDE_SCHEMA_ID);
	}
	if (include_header_opt){
	    eo.setOption(EncodingOptions.INCLUDE_OPTIONS);
	}
	efactory.setEncodingOptions(eo);
	efactory.setGrammars(g);

	return efactory;
    }
    public static EXIFactory prepare_EXIFactory(String schemaLocation) throws EXIException {
	return EXIUtil.prepare_EXIFactory(schemaLocation, true, false, null);
    }

    public static void encode_stream(OutputStream exi_output_stream, InputStream xml_input_stream, EXIFactory efactory) throws IOException {
	try {
	    EXIResult exiresult = new EXIResult(efactory);
	    exiresult.setOutputStream(exi_output_stream);
	    XMLReader xmlreader = XMLReaderFactory.createXMLReader();
	    xmlreader.setContentHandler(exiresult.getHandler());
	    xmlreader.parse(new InputSource(xml_input_stream));
	} catch (EXIException exie){
	    throw new IOException("EXIException", exie);
	} catch (SAXException saxe){
	    throw new IOException("SAXException", saxe);
	}
	return;
    }
    public static void decode_stream(OutputStream xml_output_stream, InputStream exi_input_stream, EXIFactory efactory) throws IOException {
	try {
	    EXISource exisource = new EXISource(efactory);
	    XMLReader exireader = exisource.getXMLReader();
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();
	    SAXSource saxsource = new SAXSource(new InputSource(exi_input_stream));
	    saxsource.setXMLReader(exireader);
	    transformer.transform(saxsource, new StreamResult(xml_output_stream));
	} catch (EXIException exie){
	    throw new IOException("EXIException", exie);
	} catch (TransformerException tfe){
	    throw new IOException("TransformerException", tfe);
	}
	return;
    }
				     
}
