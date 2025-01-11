
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.*;

public class EdgeServer {

	private static thrift.generated.EdgeServer.Processor<EdgeServerHandler> processor;

	public static void main(String[] args){

		EdgeServerHandler handler;
		try {
			handler = new EdgeServerHandler();
			processor = new thrift.generated.EdgeServer.Processor<>(handler);
			SimpleServer(processor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
		
	}

	private static void SimpleServer(thrift.generated.EdgeServer.Processor<EdgeServerHandler> processor) {
		try {
			TServerTransport transport = new TServerSocket(9090);

			TThreadPoolServer.Args args = new TThreadPoolServer.Args(transport);
			args.transportFactory(new TTransportFactory());
			args.protocolFactory(new TBinaryProtocol.Factory());
			args.processor(processor);
			args.executorService(new ThreadPoolExecutor(512, 65536, 6001, TimeUnit.SECONDS,
					new SynchronousQueue<>()));

			TServer server = new TThreadPoolServer(args);
			System.out.println("Edge Server Listening...");
			server.serve();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}
		
}

