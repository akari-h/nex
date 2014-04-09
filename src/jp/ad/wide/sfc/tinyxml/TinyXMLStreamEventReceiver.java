package jp.ad.wide.sfc.tinyxml;

import java.util.Map;

public interface TinyXMLStreamEventReceiver {
    /**
     * @param elementStartString whole string: "&lt;element attr="value"&gt;"
     * @param attributes dictionary-like attribute list just for convenience
     */
    public void elementStart(String elementStartString, Map attributes) throws TinyXMLException;
    /**
     * @param characters for non-tagged characters
     */
    public void characters(String characters) throws TinyXMLException;
    /**
     * @param elementEndString while string "&lt;/element&gt;"
     */
    public void elementEnd(String elementEndString) throws TinyXMLException;
    /**
     * @param elementString whole string: "&lt;element attr="value" /&gt;"
     * @param attributes dictionary-like attribute list just for convenience
     */
    public void element(String elementString, Map attributes) throws TinyXMLException;
}
