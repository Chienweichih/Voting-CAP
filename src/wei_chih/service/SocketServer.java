package wei_chih.service;

import service.handler.ConnectionHandler;
import wei_chih.service.handler.twostep.VotingHandler;
import wei_chih.utility.Utils;

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
                        
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_1).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_2).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_3).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_4).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_5).start();
        
        new SocketServer(SyncServer.class, Config.VOTING_SYNC_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
