package service;

import java.security.KeyPair;
import utility.Utils;

/**
 *
 * @author Scott
 */
public interface Config {
    public String SERVICE_HOSTNAME = "localhost";
    public int NONPOV_SERVICE_PORT = 3000;
    public int CSN_SERVICE_PORT = 3001;
    public int CHAINHASH_SERVICE_PORT = 3002;
    public int CHAINHASH_LSN_SERVICE_PORT = 3003;
    public int DOUBLECHAINHASH_SERVICE_PORT = 3004;
    
    public String DEFAULT_CHAINHASH = "default";
    
    public String DATA_DIR_PATH = "data";
    public String ATTESTATION_DIR_PATH = "attestations";
    public String DOWNLOADS_DIR_PATH = "downloads";
    public String KEYPAIR_DIR_PATH = "keypairs";
    
    public String DIGEST_ALGORITHM = "SHA-1";
    
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
    
    public enum FileSize {
        ONE_KB ("1KB", 1024),
        HUNDRED_KB ("100KB", 100 * 1024),
        ONE_MB ("1MB", 1024 * 1024),
        TEN_MB ("10MB", 10 * 1024 * 1024),
        HUNDRED_MB ("100MB", 100 * 1024 * 1024);
        
        private final String path;
        private final long size;
        
        private FileSize(String fname, long fsize) {
            this.path = fname;
            this.size = fsize;
        }
        
        public String getName() {
            return String.format("%s.bin", path);
        }
        
        public String getPath() {
            return String.format("%s/%s", DATA_DIR_PATH, getName());
        }
        
        public long getSize() {
            return size;
        }
    }
    
    public FileSize FILE = FileSize.ONE_MB;
    public int NUM_RUNS = 100;
}
