package wei_chih.service;

import service.handler.ConnectionHandler;
import wei_chih.service.handler.twostep.NonPOVHandler;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class SocketServerNonPOV extends service.SocketServer {
    public static String dataDirPath;
    
    static {
        dataDirPath = Config.DATA_DIR_PATH;
    }
 
    public SocketServerNonPOV(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
    }
    
    public static void main(String[] args) {
        if (args.length == 1) {
            switch (args[0].charAt(args[0].length() - 1)) {
                case 'A':
                    dataDirPath = Config.DATA_A_PATH;
                    break;
                case 'B':
                    dataDirPath = Config.DATA_B_PATH;
                    break;
                case 'C':
                    dataDirPath = Config.DATA_C_PATH;
                    break;
                case 'D':
                    dataDirPath = Config.DATA_D_PATH;
                    break;
                default:
                    return;
            }            
        }
                
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        for (int p : SyncServer.SERVER_PORTS) {
            new SocketServer(NonPOVHandler.class, p).start();
        }
        
        new SocketServer(SyncServer.class, SyncServer.SYNC_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
