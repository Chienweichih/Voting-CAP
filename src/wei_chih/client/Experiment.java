package wei_chih.client;

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
import wei_chih.service.Config;

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
        System.arraycopy(Config.SERVICE_PORT, 0, SERVER_PORTS, 0, Config.SERVICE_NUM);
        
        SYNC_PORT = Config.SERVICE_PORT[Config.SERVICE_NUM];
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
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("\n === NonPOV ===");
        System.out.println("Data Path: " + dataDirPath);
        System.out.println("Host: " + Config.SERVICE_HOSTNAME + ":" + Config.SERVICE_PORT[Config.SERVICE_NUM + 1]);

        for (int i = 0; i < 2; ++i) {
            System.out.println("\nUPLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }
        
        for (int i = 0; i < 2; ++i) {
            System.out.println("\nDOWNLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("\n === VotingPOV ===");
        System.out.println("Data Path: " + dataDirPath);
        System.out.print("Host: " + Config.SERVICE_HOSTNAME);
        for (int p : SERVER_PORTS) {
            System.out.print(" :" + p);
        }
        System.out.println("\nSync. Host: " + Config.SERVICE_HOSTNAME + ":" + SYNC_PORT);
        
        for (int i = 0; i < 2; ++i) {
            System.out.println("\nUPLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }
        
        for (int i = 0; i < 2; ++i) {
            System.out.println("\nDOWNLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
    }
}
