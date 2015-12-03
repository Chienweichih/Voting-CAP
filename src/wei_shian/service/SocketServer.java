package wei_shian.service;

import service.handler.ConnectionHandler;
import wei_shian.service.handler.twostep.WeiShianHandler;
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
                        
        new SocketServer(WeiShianHandler.class, Config.WEI_SHIAN_SERVICE_PORT).start();
        new SocketServer(SyncServer.class, Config.WEI_SHIAN_SYNC_PORT).start();
        
        System.out.println("Ready to go!");
    }
}
