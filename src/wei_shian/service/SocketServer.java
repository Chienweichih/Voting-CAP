package wei_shian.service;

import service.handler.ConnectionHandler;
import utility.Utils;
import wei_shian.service.handler.twostep.WeiShianHandler;

/**
 *
 * @author chienweichih
 */
public class SocketServer extends service.SocketServer {
    public static String dataDirPath;
    
    static {
        dataDirPath = Config.DATA_DIR_PATH;
    }
    
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
    }
    
    public static void main(String[] args) {
        if (args.length == 1) {
            dataDirPath = args[0];
        }
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
                        
        new SocketServer(WeiShianHandler.class, Config.WEI_SHIAN_SERVICE_PORT).start();
        new SocketServer(SyncServer.class, Config.WEI_SHIAN_SYNC_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
