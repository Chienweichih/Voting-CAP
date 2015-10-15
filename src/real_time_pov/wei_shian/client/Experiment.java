package real_time_pov.wei_shian.client;

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import message.Operation;
import message.OperationType;
import utility.Utils;
import real_time_pov.wei_shian.service.Config;

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

        System.out.println("\nWeiShain");
        System.out.println(Config.DATA_DIR_PATH);
        
        List<Operation> ops = new ArrayList<>();
        ops.add(new Operation(OperationType.DOWNLOAD,
                              Config.TEST_FILE_NAME,
                              Config.EMPTY_STRING));
        for (int i = 0;i < 4;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
        
        ops = new ArrayList<>();
        ops.add(new Operation(OperationType.UPLOAD,
                Config.TEST_FILE_NAME,
                Utils.digest(new File(Config.DATA_DIR_PATH + Config.TEST_FILE_NAME))));
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(ops, runTimes);
        }
    }
}
