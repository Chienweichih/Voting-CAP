package voting_pov.service;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import service.handler.ConnectionHandler;
import voting_pov.utility.Utils;
import voting_pov.service.handler.twostep.VotingHandler;
import voting_pov.utility.MerkleTree;

/**
 *
 * @author chienweichih
 */
public class SocketServer extends service.SocketServer {
 
    public SocketServer(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
        
        try {
            MerkleTree.create(Config.DATA_DIR_PATH,
                              VotingHandler.NEW_HASH_PATH,
                              true);
            
            String attestationPath = Config.ATTESTATION_DIR_PATH + File.separator + "service-provider";
            MerkleTree.copy(attestationPath + File.separator + "new" + File.separator + "data_HASH",
                            attestationPath + File.separator + "old" + File.separator + "data_HASH");
        } catch (IOException ex) {
            Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
                        
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_1).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_2).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_3).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_4).start();
        new SocketServer(VotingHandler.class, Config.VOTING_SERVICE_PORT_5).start();
        
        System.out.println("Ready to go!");
    }
}
