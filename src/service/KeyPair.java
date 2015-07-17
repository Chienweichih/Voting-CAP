package service;

import static service.Config.KEYPAIR_DIR_PATH;
import utility.Utils;

/**
 *
 * @author Scott
 */
public enum KeyPair {
    CLIENT ("client"),
    SERVICE_PROVIDER ("service_provider");

    private final String keyName;

    private KeyPair(String keyName) {
        this.keyName = keyName;
    }

    public String getPath() {
        return String.format("%s/%s.keypair", KEYPAIR_DIR_PATH, keyName);
    }

    public java.security.KeyPair getKeypair() {
        return Utils.readKeyPair(getPath());
    }
}
    
