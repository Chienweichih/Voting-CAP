package wei_shian.client;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import message.Operation;
import message.OperationType;
import utility.Utils;
import wei_shian.service.Config;
import wei_shian.service.SocketServer;

/**
 *
 * @author chienweichih
 */
public class Experiment {
    public static void main(String[] args) throws ClassNotFoundException {
        String testFileName = Config.TEST_FILE_NAME;
        if (args.length == 2) {
            SocketServer.dataDirPath = args[0];
            testFileName = args[1];
        }
        
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.cleanAllAttestations();
        
        final int runTimes = 100;

        System.out.println("\nWeiShain");
        System.out.println(SocketServer.dataDirPath);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              testFileName,
                              Config.EMPTY_STRING));
        for (int i = 0;i < 4;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                testFileName,
                Utils.digest(new File(SocketServer.dataDirPath + testFileName))));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
