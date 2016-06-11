package service;

import java.io.File;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import utility.Utils;

/**
 * 
 * @author Scott
 */
public class KeyManager {
    private static KeyManager KEY_MANAGER;
    
    private final Map<String, KeyPair> keyStore;
    
    private KeyManager() {
        keyStore = new HashMap<>();
        
        File keyPairDir = new File(Config.KEYPAIR_DIR_PATH);
        
        for (File keyPairFile : keyPairDir.listFiles()) {
            String keyId = keyPairFile.getName();
            keyId = keyId.substring(0, keyId.indexOf('.'));
            
            KeyPair keyPair = Utils.readKeyPair(GenerateKeyPath(keyId));
            
            keyStore.put(keyId, keyPair);
        }
        
        for (Key key : Key.values()) {
            if (!keyStore.containsKey(key.getKeyId())) {
                createKeyPair(key);
            }
        }
    }
    
    public static KeyManager getInstance() {
        if (KEY_MANAGER == null) {
            KEY_MANAGER = new KeyManager();
        }
        
        return KEY_MANAGER;
    }
    
    public KeyPair createKeyPair(Key key) {
        KeyPair keyPair = Utils.randomGenerateKeyPair();
        String path = GenerateKeyPath(key.getKeyId());
        
        Utils.writeKeyPair(path, keyPair);
        keyStore.put(key.getKeyId(), keyPair);
        
        return keyPair;
    }
    
    public KeyPair getKeyPair(Key key) {
        return keyStore.get(key.getKeyId());
    }
    
    public PublicKey getPublicKey(Key key) {
        return getKeyPair(key).getPublic();
    }
    
    public static String GenerateKeyPath(String keyId) {
        return String.format("%s/%s.keypair", Config.KEYPAIR_DIR_PATH, keyId);
    }
}
