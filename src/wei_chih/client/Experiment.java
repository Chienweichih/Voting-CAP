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
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 100;
        
        dataDirPath = Utils.getDataDirPath(args[0]);
        if (dataDirPath.equals(Config.EMPTY_STRING)) {
            System.err.println("ARGUMENT ERROR");
            return;
        }
        
        List<Operation> downloadOPs = new ArrayList<>();
        List<Operation> uploadOPs = new ArrayList<>();
        
        try {
            for(String fileName : Utils.randomPickupFiles(dataDirPath, runTimes)) {
                downloadOPs.add(new Operation(OperationType.DOWNLOAD,
                                      fileName,
                                      Utils.digest(new File(fileName))));
                uploadOPs.add(new Operation(OperationType.UPLOAD,
                                      fileName,
                                      Utils.digest(new File(fileName))));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("\nNonPOV");
        System.out.println(dataDirPath);
        System.out.println(Config.SERVICE_HOSTNAME + " " + Config.SERVICE_PORT[0]);
        
        for (int i = 0;i < 3;++i) {
            System.out.println("\nUPLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }
        
        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new NonPOVClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
        
        ///////////////////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("\nVoting");
        System.out.println(dataDirPath);
        System.out.print(Config.SERVICE_HOSTNAME);
        for (int p : SERVER_PORTS) {
            System.out.print(" " + p);
        }
        System.out.println(" " + SYNC_PORT);
        
        for (int i = 0;i < 4;++i) {
            System.out.println("\nUPLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(uploadOPs, runTimes);
        }
        
        for (int i = 0;i < 3;++i) {
            System.out.println("\nDOWNLOAD " + i);
            new VotingClient(clientKeyPair, spKeyPair).run(downloadOPs, runTimes);
        }
    }
}
