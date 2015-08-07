package voting_pov.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import static utility.Utils.randomGenerateKeyPair;

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
 
    public static void zipDir(File dir, File zipFile) {
        try (FileOutputStream fout = new FileOutputStream(zipFile);
             BufferedOutputStream bout = new BufferedOutputStream(fout);
             ZipOutputStream zout = new ZipOutputStream(bout)) {
            zipSubDirectory("", dir, zout);
            zout.close();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) {
        byte[] buffer = new byte[4096];
        zout.setLevel(0);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                String path = basePath + file.getName() + "/";
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
                    zout.putNextEntry(new ZipEntry(basePath + file.getName()));
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
     
    public static void unZip (String outputPath, String fileName) {
        try (FileInputStream fis = new FileInputStream(fileName);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zIn = new ZipInputStream(bis)) {
            File folder = new File(outputPath);
            if(!folder.exists()){
                folder.mkdir();
            }
            
            ZipEntry zipEntry = zIn.getNextEntry();
            while (zipEntry != null) {
                String fName = zipEntry.getName();
                File newFile = new File(outputPath + File.separator + fName);
                
                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
                
                byte[] b1 = new byte[1024];
                int len;
                while ((len = zIn.read(b1)) > 0) {
                    bos.write(b1, 0, len);
                }

                bos.close();   
                zipEntry = zIn.getNextEntry();
            }
            zIn.closeEntry();
            zIn.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void createRequiredFiles() {
        File dir;
        for (String path : new String[] { Config.DOWNLOADS_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH + "/client",
                                          Config.ATTESTATION_DIR_PATH + "/service-provider",
                                          Config.ATTESTATION_DIR_PATH + "/service-provider/old",
                                          Config.ATTESTATION_DIR_PATH + "/service-provider/new",
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
}
