package jp.ad.wide.sfc.nex;

import java.util.*;
import javax.xml.stream.*;
import javax.xml.namespace.*;
import org.junit.*;

// some utility classes for testing
public class XMLTester {

    static int nextTag(XMLStreamReader x) throws XMLStreamException {
	while (x.hasNext()){
	    switch (x.getEventType()){
	    case XMLStreamConstants.START_ELEMENT:
	    case XMLStreamConstants.ATTRIBUTE:
	    case XMLStreamConstants.END_ELEMENT:
		return x.getEventType();
	    default:
		x.next();
	    }
	}
	return -1;
    }
    static void assertEqualAttributeSet(XMLStreamReader a, XMLStreamReader b) throws Exception {
	Assert.assertEquals(a.getAttributeCount(), b.getAttributeCount());

	HashMap<QName, String> attrA = new HashMap<QName, String>();
	for (int i = 0; i < a.getAttributeCount(); i++){
	    attrA.put(a.getAttributeName(i), a.getAttributeValue(i));
	}
	for (int i = 0; i < b.getAttributeCount(); i++){
	    Assert.assertTrue(attrA.containsKey(b.getAttributeName(i)));
	    Assert.assertEquals(attrA.get(b.getAttributeName(i)), b.getAttributeValue(i));
	}
    }
    public static boolean assert_tags_identical(XMLStreamReader a, XMLStreamReader b) throws Exception{
	while (a.hasNext()){
	    switch (a.getEventType()){
	    case XMLStreamConstants.START_ELEMENT:
	    case XMLStreamConstants.ATTRIBUTE:
		//System.out.println("(SE|AT)("+a.getLocalName()+"),("+b.getLocalName()+")");
		Assert.assertEquals(a.getEventType(), b.getEventType());
		Assert.assertEquals(a.getLocalName(), b.getLocalName());
		
		assertEqualAttributeSet(a, b);
		a.next();
		b.next();
		break;
	    case XMLStreamConstants.END_ELEMENT:
		//System.out.println("(EE)");
    		Assert.assertEquals(a.getEventType(), b.getEventType());
		Assert.assertEquals(a.getLocalName(), b.getLocalName());
		a.next();
		b.next();
		break;
	    default:
		//System.out.println("(other)");
		// all other events ignored.
		break;
	    }
	    nextTag(a);
	    nextTag(b);
	}
	return true;
    }


    public static boolean is_attrmaps_identical(HashMap<String, String> attr1, HashMap<String, String> attr2){
	Set<String> keys1 = attr1.keySet();
	Set<String> keys2 = attr2.keySet();
	if (keys1.size() != keys2.size()) return false;
	if (! keys1.containsAll(keys2)) return false;
	for (String k: keys1){
	    if (! attr1.get(k).equals(attr2.get(k))) return false;
	}
	return true;
    }

    @Test
    public void attrMapCompTest(){
	HashMap<String, String> base = XMLUtil.extractAttributes("<foo a='1' b='2' c='3'>");
	HashMap<String, String> true1b = XMLUtil.extractAttributes("<foo a=\"1\" c=\"3\" b=\"2\">");
	HashMap<String, String> false1b = XMLUtil.extractAttributes("<foo a='2' b='2' c='3'>");
	HashMap<String, String> false2b = XMLUtil.extractAttributes("<foo a='1' b='2' c='3' d='4'>");
	HashMap<String, String> false3b = XMLUtil.extractAttributes("<foo a='1' b='2'>");
	
	Assert.assertTrue(is_attrmaps_identical(base, true1b));
	Assert.assertFalse(is_attrmaps_identical(base, false1b));
	Assert.assertFalse(is_attrmaps_identical(base, false2b));
	Assert.assertFalse(is_attrmaps_identical(base, false3b));
	return;
    }
}