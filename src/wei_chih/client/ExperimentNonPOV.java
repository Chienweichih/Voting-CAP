package wei_chih.client;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import message.Operation;
import message.OperationType;
import wei_chih.service.Config;
import wei_chih.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class ExperimentNonPOV {
    protected static String dataDirPath;
    
    static {
        dataDirPath = "";
    }
    
    public static void main(String[] args) throws ClassNotFoundException {
        String[] testFileName = Utils.getTestFileName(args);
        dataDirPath = testFileName[0];
        if (dataDirPath.equals(Config.EMPTY_STRING)) {
            System.err.println("ARGUMENT ERROR");
            return;
        }
                
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 100;
        
        System.out.println("\nNonPOV");
        System.out.println(dataDirPath);
        System.out.print(Config.SERVICE_HOSTNAME);
        
        System.out.print(" " + Config.SERVICE_PORT[0]);
        System.out.println(" " + Experiment.SYNC_PORT);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                              testFileName[1],
                              Utils.digest(new File(dataDirPath + testFileName[1]))));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              testFileName[1],
                              Config.EMPTY_STRING));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
