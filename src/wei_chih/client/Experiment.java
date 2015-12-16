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
public class Experiment {
    protected static String dataDirPath;
    
    protected static final int[] SERVER_PORTS;
    protected static final int SYNC_PORT;
    
    static {
        dataDirPath = "";
        
        SERVER_PORTS = new int[Config.SERVICE_NUM];
        for (int i = 0; i < Config.SERVICE_NUM; ++i) {
            SERVER_PORTS[i] = Config.SERVICE_PORT[i];
        }
        
        SYNC_PORT = Config.SERVICE_PORT[Config.SERVICE_NUM];
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

        System.out.println("\nVoting");
        System.out.println(dataDirPath);
        System.out.print(Config.SERVICE_HOSTNAME);
        for (int p : SERVER_PORTS) {
            System.out.print(" " + p);
        }
        System.out.println(" " + SYNC_PORT);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                              testFileName[1],
                              Utils.digest(new File(dataDirPath + testFileName[1]))));
        for (int i = 0;i < 4;++i) {
            System.out.println("\nUPLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              testFileName[1],
                              Config.EMPTY_STRING));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
