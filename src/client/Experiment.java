package client;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import message.Operation;
import message.OperationType;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class Experiment {
    public static void main(String[] args) throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        KeyPair clientKeyPair = service.KeyPair.CLIENT.getKeypair();
        KeyPair spKeyPair = service.KeyPair.SERVICE_PROVIDER.getKeypair();
        
        Utils.cleanAllAttestations();
        
        Map<String, Client> clients = new HashMap<>();
        
        clients.put("non-POV", new NonPOVClient(clientKeyPair, spKeyPair));
        clients.put("CSN", new CSNClient(clientKeyPair, spKeyPair));
        clients.put("ChainHash", new ChainHashClient(clientKeyPair, spKeyPair));
        clients.put("C&L", new ChainHashAndLSNClient("id", clientKeyPair, spKeyPair));
        clients.put("DoubleHash", new DoubleChainHashClient("id", clientKeyPair, spKeyPair));
        
        int runTimes = 10;
        
        List<Operation> ops = new ArrayList<>();
        
        service.File[] files = new service.File[] { service.File.TEN_MB,
                                                    service.File.TEN_MB_2,
                                                    service.File.TEN_MB_3,
                                                    service.File.TEN_MB_4 };
        
        for (service.File file : files) {
//            ops.add(new Operation(OperationType.DOWNLOAD, file.getName(), ""));
            ops.add(new Operation(OperationType.UPLOAD,
                    file.getName(),
                    Utils.readDigest(file.getPath())));
        }
        
        for (Map.Entry<String, Client> client : clients.entrySet()) {
            classLoader.loadClass(client.getValue().getClass().getName());
            
            System.out.println("\n" + client.getKey());
            
            client.getValue().run(ops, runTimes);
        }
    }
}
