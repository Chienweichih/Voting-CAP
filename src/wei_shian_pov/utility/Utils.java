package wei_shian_pov.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import voting_pov.service.Config;

/**
 *
 * @author chienweichih
 */
public class Utils extends utility.Utils {
    
    public static String digest(byte[] message) {
        return digest(message, Config.DIGEST_ALGORITHM);
    }
    
    /**
     * Digest a byte[] with a specific algorithm.
     * @param message
     * @param algorithm
     * @return the digest value
     */
    public static String digest(byte[] message, String algorithm) {
        String result = null;
        
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            
            result = Hex2Str(digest.digest(message));
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    public static String read(String fname) {
        String digest = null;
        
        try (FileReader fr = new FileReader(fname);
             BufferedReader br = new BufferedReader(fr)) {
            digest = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return digest;
    }
}
