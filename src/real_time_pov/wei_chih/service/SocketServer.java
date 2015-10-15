package real_time_pov.wei_chih.service;

import service.handler.ConnectionHandler;
import real_time_pov.wei_chih.service.handler.twostep.VotingHandler;
import real_time_pov.wei_chih.utility.Utils;

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
