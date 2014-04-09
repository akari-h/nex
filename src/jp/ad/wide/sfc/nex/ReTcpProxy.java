/**
 * 
 */
package jp.ad.wide.sfc.nex;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author akari-h
 *
 */
class ReProxyWorker extends Thread {
	Socket client_socket;
	SocketAddress connect_to;
	ReProxyWorker(Socket client_socket, SocketAddress connect_to){
		this.client_socket = client_socket;
		this.connect_to = connect_to;
	}
	public void run(){
		try {
			Socket proxy_socket = new Socket();
			proxy_socket.connect(connect_to);

			// must be XML
			Reader client_inReader = new InputStreamReader(client_socket.getInputStream());
			Writer client_outWriter = new OutputStreamWriter(client_socket.getOutputStream());
		
			// may be EXI
			InputStream proxy_inStream = proxy_socket.getInputStream();
			OutputStream proxy_outStream = proxy_socket.getOutputStream();

			
			Preprocessor prep = new Preprocessor(client_inReader);
			Thread readerThread = prep.mkReaderThread(proxy_outStream);
			Thread procThread = prep.mkProcessorThread();
			readerThread.start();
			procThread.start();
			
			Postprocessor postp = new Postprocessor(proxy_inStream, client_outWriter);
			
			//postp.process_documents();
			postp.process_documents();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}	

public class ReTcpProxy {
	static final int DEFAULT_LISTEN_PORT = 7891;
	int listen_port;
	String connect_address;
	int connect_port;

	public ReTcpProxy(int listen_port, String connect_address, int connect_port){
		this.listen_port = listen_port;
		this.connect_address = connect_address;
		this.connect_port = connect_port;
	}
	void do_service() throws IOException {
		ServerSocket srvsock = new ServerSocket(listen_port);
		
		Socket clientsock = srvsock.accept();
		while (clientsock != null){
			System.err.println("connect: " + clientsock.getRemoteSocketAddress());

			SocketAddress connect_to = new InetSocketAddress(connect_address, connect_port);
		
			ReProxyWorker w = new ReProxyWorker(clientsock, connect_to);
			w.start();
			
			clientsock = srvsock.accept();
		}	
	}

	public static void main(String[] args){
		try {
			int tlisten_port = ReTcpProxy.DEFAULT_LISTEN_PORT;
			String tconnect_address = "localhost";
			int tconnect_port = 7;
			String env_connect_port = System.getenv("NEX_REV_CONNECT_PORT");
			String env_connect_address = System.getenv("NEX_REV_CONNECT_ADDRESS");
			String env_listen_port = System.getenv("NEX_REV_LISTEN_PORT");
			if (env_connect_address != null){
				tconnect_address = env_connect_address;
			}
			if (env_connect_port != null){
				tconnect_port = Integer.parseInt(env_connect_port);
			}
			if (env_listen_port != null){
			    tlisten_port = Integer.parseInt(env_listen_port);
			}
			ReTcpProxy instance = new ReTcpProxy(tlisten_port, tconnect_address, tconnect_port);
			System.err.println("ReTcpProxy("+tlisten_port+", "+tconnect_address+":"+tconnect_port+") initialized.");
			instance.do_service();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}