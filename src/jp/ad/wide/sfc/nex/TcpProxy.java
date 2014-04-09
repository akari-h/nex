/**
 * 
 */
package jp.ad.wide.sfc.nex;

import java.io.BufferedReader;
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

import com.siemens.ct.exi.EXIFactory;

import gnu.getopt.Getopt;

/**
 * @author akari-h
 *
 */

class ProxyWorker extends Thread {
	Socket client_socket;
	SocketAddress connect_to;
	boolean verbose = false;
	EXIFactory efactory_or_null = null;
	ProxyWorker(Socket client_socket, SocketAddress connect_to, EXIFactory efactory_or_null){
		this.client_socket = client_socket;
		this.connect_to = connect_to;
		this.efactory_or_null = efactory_or_null;
	}
	void setVerbose(boolean f){
		this.verbose = f;
	}
	public void run(){
		try {
			Socket proxy_socket = new Socket();
			proxy_socket.connect(connect_to);
			
			// could be EXI, uses bare Stream of bytes
			InputStream client_inStream = client_socket.getInputStream();
			OutputStream client_outStream = client_socket.getOutputStream();
		
			// must be XML, uses Reader/Writer instead of Streams
			Reader proxy_inReader = new InputStreamReader(proxy_socket.getInputStream());
			Writer proxy_outReader = new OutputStreamWriter(proxy_socket.getOutputStream());

			Postprocessor postp = new Postprocessor(client_inStream, proxy_outReader, efactory_or_null);
			Preprocessor prep = new Preprocessor(proxy_inReader);
			prep.setDebug(this.verbose);
			postp.setDebug(this.verbose);

			Thread readerThread = prep.mkReaderThread(client_outStream, efactory_or_null);
			Thread procThread = prep.mkProcessorThread();
			readerThread.start();
			procThread.start();
			
			postp.process_documents();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
	
public class TcpProxy {
	static final int DEFAULT_LISTEN_PORT = 7890;
	static final int DEFAULT_CONNECT_PORT = 7;
	static final String DEFAULT_CONNECT_ADDR = "localhost";
	int listen_port;
	String connect_address;
	int connect_port;
	boolean verbose = false;
	EXIFactory efactory_or_null = null;

	public TcpProxy(int listen_port, String connect_address, int connect_port, EXIFactory efactory_or_null){
		this.listen_port = listen_port;
		this.connect_address = connect_address;
		this.connect_port = connect_port;
		this.efactory_or_null = efactory_or_null;
	}
	void setVerbose(boolean f){
		this.verbose = f;
	}
	void do_service() throws IOException {
		ServerSocket srvsock = new ServerSocket(listen_port);
		
		Socket clientsock = srvsock.accept();
		while (clientsock != null){
			System.err.println("connect: " + clientsock.getRemoteSocketAddress());

			SocketAddress connect_to = new InetSocketAddress(connect_address, connect_port);
		
			ProxyWorker w = new ProxyWorker(clientsock, connect_to, efactory_or_null);
			w.setVerbose(this.verbose);
			w.start();
			
			clientsock = srvsock.accept();
		}	
	}

	public static void print_help_exit(String mesg){
		if (mesg != null){
			System.err.println("Error: "+mesg);
		}
		System.err.println("");
		System.err.println("NEX: TcpProxy for XEP-0322");
		System.err.println("");
		System.err.println("  -a [addr]   Address to connect (default: "+TcpProxy.DEFAULT_CONNECT_ADDR+")");
		System.err.println("  -p [port]   Port to connect    (default: "+TcpProxy.DEFAULT_CONNECT_PORT+")");
		System.err.println("  -l [port]   Port to listen     (default: "+TcpProxy.DEFAULT_LISTEN_PORT+")");
		System.err.println("  -e          enable EXI mode    (default: disabled)");
		System.err.println("  -s [schema] schema to use      (indicates EXI mode, default: unspecified)");
		System.err.println("  -N          EXI non-strict     (default: strict)");
		System.err.println("  -d          debug (verbose)    (default: disabled)");
		System.err.println("  -h          this help");
		System.exit(1);
	}
	public static void main(String[] args){
		try {
			// defaults
			int tlisten_port = TcpProxy.DEFAULT_LISTEN_PORT;
			String tconnect_address = TcpProxy.DEFAULT_CONNECT_ADDR;
			int tconnect_port = TcpProxy.DEFAULT_CONNECT_PORT;
			boolean verbose_arg = false;
			boolean exi_use = false;
			boolean exi_strict = true;
			String exi_schema_file = null;

			// can be overrided by environment
			String env_connect_port = System.getenv("NEX_FWD_CONNECT_PORT");
			String env_connect_address = System.getenv("NEX_FWD_CONNECT_ADDRESS");
			String env_listen_port = System.getenv("NEX_FWD_LISTEN_PORT");

			// can be overrided by args
			Getopt options = new Getopt("TcpProxy", args, "a:dehl:p:s:N");
			int c;
			while ((c = options.getopt()) != -1){
				switch (c){
				case 'a': // addr to connect
					env_connect_address = options.getOptarg();
					if (env_connect_address == null){
						TcpProxy.print_help_exit("-a requires address to connect");
					}
					break;
				case 'p': // port to connect
					env_connect_port = options.getOptarg();
					if (env_connect_port == null){
						TcpProxy.print_help_exit("-p requires port number to connect");
					}
					break;
				case 'l': // port to listen
					env_listen_port = options.getOptarg();
					if (env_listen_port == null){
						TcpProxy.print_help_exit("-l requires port number to listen");
					}
					break;
				case 'e': // force use of EXI
					exi_use = true;
					break;
				case 's': // schema to use
					exi_use = true;
					exi_schema_file = options.getOptarg();
					if (exi_schema_file == null){
						TcpProxy.print_help_exit("-s requires a schema file");
					}
					break;
				case 'N': // non-strict mode
					exi_use = true;
					exi_strict = false;
					break;
				case 'd':
					verbose_arg = true;
					break;
				case 'h':
					TcpProxy.print_help_exit(null);
					break;
				default:
					TcpProxy.print_help_exit("unknown option "+(char)c);
					break;
				}
			}
			
			if (env_connect_address != null){
				tconnect_address = env_connect_address;
			}
			if (env_connect_port != null){
				tconnect_port = Integer.parseInt(env_connect_port);
			}
			if (env_listen_port != null){
				tlisten_port = Integer.parseInt(env_listen_port);
			}

			EXIFactory efactory = null;
			if (exi_use){
				if (exi_schema_file == null){
					TcpProxy.print_help_exit("Schemaless mode is not supported (yet). Please specify some schema.");
				}
				efactory = EXIUtil.prepare_EXIFactory(exi_schema_file, exi_strict, false, null);
			}
			TcpProxy instance = new TcpProxy(tlisten_port, tconnect_address, tconnect_port, efactory);
			instance.setVerbose(verbose_arg);
			String progname = "TcpProxy";
			if (exi_use){
				progname = "EXIProxy";
			}
			System.err.println(progname+"("+tlisten_port+", "+tconnect_address+":"+tconnect_port+") initialized.");
			instance.do_service();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
/**
 *  Local Variables:
 *  tab-width: 4
 *  End:
 */
