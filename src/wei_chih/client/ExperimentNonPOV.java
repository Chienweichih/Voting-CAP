package wei_chih.client;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import message.Operation;
import message.OperationType;
import wei_chih.service.Config;
import wei_chih.service.SocketServer;
import wei_chih.service.SyncServer;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class ExperimentNonPOV {
    public static void main(String[] args) throws ClassNotFoundException {
        String testFileName = Config.DATA_TESTFILE;
        
        if (args.length == 1) {
            switch (args[0].charAt(args[0].length() - 1)) {
                case 'A':
                    SocketServer.dataDirPath = Config.DATA_A_PATH;
                    testFileName = Config.DATA_A_TESTFILE;
                    break;
                case 'B':
                    SocketServer.dataDirPath = Config.DATA_B_PATH;
                    testFileName = Config.DATA_B_TESTFILE;
                    break;
                case 'C':
                    SocketServer.dataDirPath = Config.DATA_C_PATH;
                    testFileName = Config.DATA_C_TESTFILE;
                    break;
                case 'D':
                    SocketServer.dataDirPath = Config.DATA_D_PATH;
                    testFileName = Config.DATA_D_TESTFILE;
                    break;
                default:
            }
        }
        
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 100;
        
        System.out.println("\nNonPOV");
        System.out.println(SocketServer.dataDirPath);
        System.out.print(Config.SERVICE_HOSTNAME);
        for (int p : SyncServer.SERVER_PORTS) {
            System.out.print(" " + p);
        }
        System.out.println(" " + SyncServer.SYNC_PORT);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                              testFileName,
                              Utils.digest(new File(SocketServer.dataDirPath + testFileName))));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              testFileName,
                              Config.EMPTY_STRING));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
