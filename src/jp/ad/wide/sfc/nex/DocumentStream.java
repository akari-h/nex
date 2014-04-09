/**
 * 
 */
package jp.ad.wide.sfc.nex;

import java.io.IOException;

/**
 * @author akari-h
 *
 */
public interface DocumentStream {

    // throws DocumentStremaEnd when it reaches end of stream (or whatever alike)
    public String nextDocument() throws InterruptedException, IOException, DocumentStreamEnd;
    public void close() throws IOException;
}
