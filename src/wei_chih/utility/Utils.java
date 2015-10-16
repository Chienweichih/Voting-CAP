package wei_chih.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import wei_chih.service.Config;

/**
 *
 * @author chienweichih
 */
public class Utils extends utility.Utils {
    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(Utils.class.getName());
    }
    
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
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    public static String read(String fname) {
        String digest = null;
        
        try (FileReader fr = new FileReader(fname);
             BufferedReader br = new BufferedReader(fr)) {
            digest = br.readLine();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return digest;
    }
    
    public static void Serialize(File dest, Object ackStr) {
        try (FileOutputStream fout = new FileOutputStream(dest);
             ObjectOutputStream oos = new ObjectOutputStream(fout)) {   
            oos.writeObject(ackStr);
            oos.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public static <T> T Deserialize(String src) {
        T obj = null;
        try (FileInputStream fin = new FileInputStream(src);
             ObjectInputStream ois = new ObjectInputStream(fin)) {
            obj = (T) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return obj;
    }
}
