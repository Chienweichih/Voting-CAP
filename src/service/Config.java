package service;

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
    
    public String DOWNLOADS_DIR_PATH = "downloads";
    
    public String DIGEST_ALGORITHM = "SHA-1";
    
    public String FNAME = "1M.txt";
    public int NUM_RUNS = 100;
}
