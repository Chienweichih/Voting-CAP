package voting_pov.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final int BUF_SIZE = 8192;
    
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
    
    public static void copyFolder(File src, File dest) {
        if (src.isDirectory()) {
            if(!dest.exists()) {
               dest.mkdir();
            }

            for (String file : src.list()) {
               File srcFile = new File(src, file);
               File destFile = new File(dest, file);
               
               copyFolder(srcFile, destFile);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(dest);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 FileInputStream fis = new FileInputStream(src);
                 BufferedInputStream bin = new BufferedInputStream(fis)) {
                byte[] buf = new byte[BUF_SIZE];
                int n;

                while ((n = bin.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }

                bos.flush();
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
