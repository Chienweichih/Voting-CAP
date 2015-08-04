package voting_pov.utility;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    
    public static void copyFolder(File src, File dest) {
        if (src.isDirectory()) {
            //if directory not exists, create it
            if(!dest.exists()) {
               dest.mkdir();
               System.out.println("Directory copied from " 
                          + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
               //construct the src and dest file structure
               File srcFile = new File(src, file);
               File destFile = new File(dest, file);
               //recursive copy
               copyFolder(srcFile,destFile);
            }
        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            try {
                Utils.send(new DataOutputStream(new FileOutputStream(dest)), src);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("File copied from " + src + " to " + dest);
        }
    }
}
