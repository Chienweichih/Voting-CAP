package wei_shian_pov.service;

import java.io.File;

/**
 *
 * @author chienweichih
 */
public interface Config extends voting_pov.service.Config {
    public String SYNC_HOSTNAME = "localhost";
    
    public int WEI_SHIAN_SERVICE_PORT = 3021;
    public int WEI_SHIAN_SYNC_PORT = 3022;
    
    public String DATA_DIR_PATH = ".." + File.separator + "Accounts" + File.separator + "Account B";
}