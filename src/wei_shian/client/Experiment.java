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
    
    public static void main(String[] args) throws ClassNotFoundException {
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 1;
        
        dataDirPath = Utils.getDataDirPath(args[0]);
        if (dataDirPath.equals(Config.EMPTY_STRING)) {
            System.err.println("ARGUMENT ERROR");
            return;
        }
        
        List<Operation> downloadOPs = new ArrayList<>();
        List<Operation> uploadOPs = new ArrayList<>();
        
        try {
            for(String fileName : Utils.randomPickupFiles(dataDirPath, runTimes)) {
                String digest = Utils.digest(new File(fileName));
                
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
        
        System.out.println("\nWeiShain");
        System.out.println(dataDirPath);
        System.out.print(Config.SERVICE_HOSTNAME);
        System.out.println(" " + Config.WEI_SHIAN_SERVICE_PORT + " " + Config.WEI_SHIAN_SYNC_PORT);

        for (int i = 0;i < 4;++i) {
            System.out.println("\nUPLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }

        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new WeiShianClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
    }
}
