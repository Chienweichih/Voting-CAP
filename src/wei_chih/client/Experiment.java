package wei_chih.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.Client;
import message.Operation;
import message.OperationType;
import service.Key;
import service.KeyManager;
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
    
    public static void main(String[] args) throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        
        KeyManager keyManager = KeyManager.getInstance();
        KeyPair clientKeyPair = keyManager.getKeyPair(Key.CLIENT);
        KeyPair spKeyPair = keyManager.getKeyPair(Key.SERVICE_PROVIDER);
        
        Utils.createRequiredFiles();
        Utils.cleanAllAttestations();
        
        final int runTimes = 40;
        
        dataDirPath = Utils.getDataDirPath(args[0], Config.CLIENT_ACCOUNT_PATH);

        // prepare ramdom operations
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

        // print machine detail
        System.out.println("Data Path: " + dataDirPath);
        System.out.println("Service Host: " + Config.SERVICE_HOSTNAME);
        System.out.print("Voting-POV Port:");
        for (int p : SERVER_PORTS) {
            System.out.print(" " + p);
        }
        System.out.println("\nVoting-POV Sync. Port: " + SYNC_PORT);
        System.out.println("WeiShian-POV Port: " + Config.WEI_SHIAN_SERVICE_PORT);
        System.out.println("WeiShian-POV Sync. Port: " + Config.WEI_SHIAN_SYNC_PORT);
        System.out.println("Non-POV Port: " + Config.SERVICE_PORT[Config.SERVICE_NUM + 1]);
        
        // add clients do-list
        Map<String, Client> clients = new LinkedHashMap<>();
        clients.put("Voting-POV-Upload", new VotingClient(clientKeyPair, spKeyPair));
        clients.put("Voting-POV-Download", new VotingClient(clientKeyPair, spKeyPair));
        clients.put("Non-POV-Upload", new NonPOVClient(clientKeyPair, spKeyPair));
        clients.put("Non-POV-Download", new NonPOVClient(clientKeyPair, spKeyPair));
        clients.put("WeiShian-POV-Upload", new WeiShianClient(clientKeyPair, spKeyPair));
        clients.put("WeiShian-POV-Download", new WeiShianClient(clientKeyPair, spKeyPair));
                
        // go!
        for (Map.Entry<String, Client> client : clients.entrySet()) {
            classLoader.loadClass(client.getValue().getClass().getName());
            
            System.out.println("\n" + client.getKey());
            
            if (client.getKey().endsWith("Upload")) {
                client.getValue().run(uploadOPs, runTimes);
            } else {
                client.getValue().run(downloadOPs, runTimes);
            }
        }
    }
}
