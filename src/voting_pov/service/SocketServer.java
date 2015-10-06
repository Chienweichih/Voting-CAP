package voting_pov.service;

import service.handler.ConnectionHandler;
import voting_pov.service.handler.twostep.VotingHandler;
import voting_pov.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class SocketServer extends service.SocketServer {
 
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
    }
    
    public static void main(String[] args) {
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
