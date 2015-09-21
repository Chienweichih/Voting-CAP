package voting_pov.client;

import java.security.KeyPair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import message.Operation;
import message.OperationType;
import voting_pov.service.Config;
import voting_pov.utility.Utils;

/**
 *
 * @author chienweichih
 */
public class Experiment {
    public static void main(String[] args) throws ClassNotFoundException {
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.cleanAllAttestations();
        
        final int runTimes = 100;
        final String testFileName = //File.separator + "folder1" + File.separator + "small_1.txt";
//                                    File.separator + "folder3" + File.separator + "2011.rmvb";
                
                                    File.separator + "testing result" + 
                                    File.separator + "DeadLock1" + 
                                    File.separator + "DeadLock" + 
                                    File.separator + "DeadLock(0).txt";
//
//                                    File.separator + "My courses" +
//                                    File.separator + "System Software" +
//                                    File.separator + "Slice from NCU" +
//                                    File.separator + "chap_01.pps";

        System.out.println("\nVoting");
        System.out.println(Config.DATA_DIR_PATH);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              testFileName,
                              Config.EMPTY_STRING));
        for (int i = 0;i < 4;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                testFileName,
                Utils.digest(new File(Config.DATA_DIR_PATH + testFileName))));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
