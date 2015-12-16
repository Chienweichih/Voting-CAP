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
        dataDirPath = "";
    }
 
    public SocketServerNonPOV(Class<? extends ConnectionHandler> handler, int port) {
        super(handler, port);
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("NEED ONE ARGUMENT");
            return;
        }
        dataDirPath = Utils.getDataDirPath(args[0]);
                
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        new SocketServerNonPOV(NonPOVHandler.class, Config.SERVICE_PORT[0]).start();
        
        System.out.println("Ready to go!");
    }
}
