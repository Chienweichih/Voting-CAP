package voting_pov.client;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import client.Client;
import java.io.File;
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
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.cleanAllAttestations();
        
        Map<String, Client> clients = new LinkedHashMap<>();
        clients.put("Voting", new VotingClient(clientKeyPair, spKeyPair));
        
        int runTimes = 100;
        
        List<Operation> ops = new ArrayList<>();
        
        service.File[] files = new service.File[] { service.File.HUNDRED_KB };
        
        for (service.File file : files) {
            ops.add(new Operation(OperationType.DOWNLOAD, "voting_pov/utility/MerkleTree.java", Config.EMPTY_STRING));
//            ops.add(new Operation(OperationType.UPLOAD,
//                    file.getName(),
//                    Utils.readDigest(file.getPath())));
        }
        
        for (Map.Entry<String, Client> client : clients.entrySet()) {
            classLoader.loadClass(client.getValue().getClass().getName());
            
            System.out.println("\n" + client.getKey());
            
            client.getValue().run(ops, runTimes);
        }
        
        long time = System.currentTimeMillis();
        Utils.zipDir(new File(Config.DATA_DIR_PATH), new File(Config.ATTESTATION_DIR_PATH + "/test.zip"));
        Utils.unZip(Config.ATTESTATION_DIR_PATH + "/test", Config.ATTESTATION_DIR_PATH + "/test.zip");
        System.out.println(System.currentTimeMillis() - time);
    }
}
