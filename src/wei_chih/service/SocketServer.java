package wei_chih.service;

import service.handler.ConnectionHandler;
import wei_chih.service.handler.twostep.NonPOVHandler;
import wei_chih.service.handler.twostep.VotingHandler;
import wei_chih.utility.Utils;
import wei_chih.service.handler.twostep.WeiShianHandler;

/**
 *
 * @author chienweichih
 */
public class SocketServer extends service.SocketServer {
    public static String dataDirPath;
    
    static {
        dataDirPath = "";
    }
 
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("NEED ONE ARGUMENT");
            return;
        }
        dataDirPath = Utils.getDataDirPath(args[0], Config.SERVER_ACCOUNT_PATH);
                
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        new SocketServer(NonPOVHandler.class, Config.SERVICE_PORT[Config.SERVICE_NUM + 1]).start();
        
        for (int p : VotingSyncServer.SERVER_PORTS) {
            new SocketServer(VotingHandler.class, p).start();
        }
        
        new SocketServer(VotingSyncServer.class, VotingSyncServer.SYNC_PORT).start();
        
        new SocketServer(WeiShianHandler.class, Config.WEI_SHIAN_SERVICE_PORT).start();
        new SocketServer(WeiShianSyncServer.class, Config.WEI_SHIAN_SYNC_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
