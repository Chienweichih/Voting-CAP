package service;

/**
 *
 * @author Scott
 */
public interface Config {
    public String SERVICE_HOSTNAME = "localhost";
    public int SERVICE_PORT = 3000;
    
    public String DEFAULT_CHAINHASH = "default";
    
    public String DATA_DIR_PATH = "data";
    
    public String DIGEST_ALGORITHM = "SHA-1";
}
