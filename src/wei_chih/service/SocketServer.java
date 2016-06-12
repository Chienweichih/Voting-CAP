package wei_chih.service;

import service.handler.ConnectionHandler;
import wei_chih.service.handler.NonCAPHandler;
import wei_chih.service.handler.wei_chih.WeiChihHandler;
import wei_chih.service.handler.wei_shian.WeiShianHandler;
import wei_chih.service.handler.wei_chih.WeiChihSyncHandler;
import wei_chih.service.handler.wei_shian.WeiShianSyncHandler;
import wei_chih.utility.Utils;

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

        new SocketServer(NonCAPHandler.class, Config.SERVICE_PORT[Config.SERVICE_NUM + 1]).start();

        for (int p : WeiChihSyncHandler.SERVER_PORTS) {
            new SocketServer(WeiChihHandler.class, p).start();
        }

        new SocketServer(WeiChihSyncHandler.class, WeiChihSyncHandler.SYNC_PORT).start();

        new SocketServer(WeiShianHandler.class, Config.WEI_SHIAN_SERVICE_PORT).start();
        new SocketServer(WeiShianSyncHandler.class, Config.WEI_SHIAN_SYNC_PORT).start();

        System.out.println("Ready to go!");
    }
}
