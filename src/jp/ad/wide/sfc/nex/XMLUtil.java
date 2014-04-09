package jp.ad.wide.sfc.nex;

import java.util.Map;
import java.util.HashMap;

public class XMLUtil {
    public static Range findElementishFromString(String xml_ish){
	return findElementishFromString(xml_ish, 0);
    }
    public static Range findElementishFromString(String xml_ish, int offset){
	int s = xml_ish.indexOf('<', offset);
	int e = xml_ish.indexOf('>', s)+1;
	if (s < 0 || e < 0){
	    return null;
	}
	return new Range(s, e);
    }
    public static String extractFromString(String xml_ish, Range element_range){
	return xml_ish.substring(element_range.start(), element_range.end());
    }
    public static String extractElementishFromString(String xml_ish){
	return extractFromString(xml_ish, findElementishFromString(xml_ish));
    }
    // extract key=value map from string "<tag key="value" key="value" >"
    public static HashMap<String, String> extractAttributes(String element_ish){
	String content = element_ish.substring(1, element_ish.length()-1); // remove <>
	String[] kvPairs = content.split(" ");
	HashMap<String, String> kvMap = new HashMap<String, String>();
	for (int i = 1; i < kvPairs.length; i++){ // start from 1 to skip element name
	    String[] kv = kvPairs[i].split("=", 2);
	    if (kv[1].charAt(kv[1].length()-1) == '"' || kv[1].charAt(kv[1].length()-1) == '\''){
		kv[1] = kv[1].substring(1, kv[1].length()-1); // trim quotes
	    }
	    kvMap.put(kv[0], kv[1]);
	}
	return kvMap;
    }
}