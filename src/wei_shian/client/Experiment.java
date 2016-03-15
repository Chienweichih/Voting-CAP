package wei_shian.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Operation;
import message.OperationType;
import wei_chih.utility.Utils;
import wei_shian.service.Config;

/**
 *
 * @author chienweichih
 */
public class Experiment {
    protected static String dataDirPath;
    
    static {
        dataDirPath = "";
    }
    
    public static void main(String[] args) {
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 1;
        
        dataDirPath = Utils.getDataDirPath(args[0]);

        List<Operation> downloadOPs = new ArrayList<>();
        List<Operation> uploadOPs = new ArrayList<>();
        
        try {
            for(String fileName : Utils.randomPickupFiles(dataDirPath, runTimes)) {
                String digest = Utils.digest(new File(dataDirPath + fileName));
                
                downloadOPs.add(new Operation(OperationType.DOWNLOAD,
                                              fileName,
                                              digest));
                uploadOPs.add(new Operation(OperationType.UPLOAD,
                                            fileName,
                                            digest));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("\n === WeiShainPOV ===");
        System.out.println("Data Path: " + dataDirPath);
        System.out.println("Host: " + Config.SERVICE_HOSTNAME + ":" + Config.WEI_SHIAN_SERVICE_PORT);
        System.out.println("Sync. Host: " + Config.SERVICE_HOSTNAME + ":" + Config.WEI_SHIAN_SYNC_PORT);

        for (int i = 0; i < 2; ++i) {
            System.out.println("\nUPLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }

        for (int i = 0; i < 2; ++i) {
            System.out.println("\nDOWNLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
    }
}
