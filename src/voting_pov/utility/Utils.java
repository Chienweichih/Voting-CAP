package voting_pov.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    
    public static void cleanClientAttestations() {
        for (String path : new String[] { Config.ATTESTATION_DIR_PATH + File.separator + "client",
                                          Config.DOWNLOADS_DIR_PATH }) {
            File dir = new File(path);

            clearDirectory(dir);
        }
    }
    
    public static void createRequiredFiles() {
        File dir;
        for (String path : new String[] { Config.DOWNLOADS_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH + File.separator + "client",
                                          Config.ATTESTATION_DIR_PATH + File.separator + "service-provider",
                                          Config.ATTESTATION_DIR_PATH + File.separator + "service-provider" +
                                                                        File.separator + "old",
                                          Config.ATTESTATION_DIR_PATH + File.separator + "service-provider" +
                                                                        File.separator + "new",
                                          Config.DATA_DIR_PATH,
                                          Config.KEYPAIR_DIR_PATH }) {
            dir = new File(path);
            
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        
        copyFolder(new File(Config.SRC_DIR_PATH), new File(Config.DATA_DIR_PATH));
        
        for (service.KeyPair kp : service.KeyPair.values()) {
            File keyFile = new File(kp.getPath());
            
            if (!keyFile.exists()) {
                try {
                    keyFile.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            
                try (FileOutputStream fos = new FileOutputStream(keyFile);
                     ObjectOutputStream out = new ObjectOutputStream(fos)) {
                    out.writeObject(randomGenerateKeyPair());
                } catch (IOException ex) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
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
 
    public static void zipDir(File src, File dest) {
        try (FileOutputStream fout = new FileOutputStream(dest);
             BufferedOutputStream bout = new BufferedOutputStream(fout);
             ZipOutputStream zout = new ZipOutputStream(bout)) {
            zipSubDirectory("", src, zout);
            zout.close();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void zipSubDirectory(String parentPath, File src, ZipOutputStream zout) {
        byte[] buffer = new byte[BUF_SIZE];
        zout.setLevel(0);
        for (File file : src.listFiles()) {
            if (file.isDirectory()) {
                String path = parentPath + file.getName() + File.separator;
                try {
                    zout.putNextEntry(new ZipEntry(path));
                    zipSubDirectory(path, file, zout);
                    zout.closeEntry();
                } catch (IOException ex) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try (FileInputStream fin = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fin)) {
                    zout.putNextEntry(new ZipEntry(parentPath + file.getName()));
                    int length;
                    while ((length = bis.read(buffer)) > 0) {
                        zout.write(buffer, 0, length);
                    }
                    
                    zout.closeEntry();
                    fin.close();
                } catch (IOException ex) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
     
    public static void unZip (String dest, String src) {
        try (FileInputStream fis = new FileInputStream(src);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zIn = new ZipInputStream(bis)) {
            File folder = new File(dest);
            if(!folder.exists()){
                folder.mkdir();
            }
            
            byte[] buffer = new byte[BUF_SIZE];
            ZipEntry zipEntry = zIn.getNextEntry();
            while (zipEntry != null) {
                String fName = zipEntry.getName();
                File newFile = new File(dest + File.separator + fName);
                
                if (zipEntry.isDirectory()) {
                    File newDir = newFile.getAbsoluteFile();
                    if (!newDir.exists()) {
                        newDir.mkdirs();
                    }
                } else {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
                    
                    int len;
                    while ((len = zIn.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }

                    bos.close();   
                }
                
                zIn.closeEntry();
                zipEntry = zIn.getNextEntry();
            }
            zIn.closeEntry();
            zIn.close();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
