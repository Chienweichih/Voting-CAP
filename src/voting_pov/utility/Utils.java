package voting_pov.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    
    public static void zipDir (String sourceDir, String targetFilePath) {
        try (FileOutputStream fos = new FileOutputStream(targetFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (File tmp: new File(sourceDir).listFiles()) {
                checkFileType(tmp, zos, tmp.getName());
            }
            
            zos.finish();
            zos.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
    * recursive check File's attr.
    * @param file
    * @param zos
    * @param fileName
    * @throws Exception
    */
    private static void checkFileType (File file, ZipOutputStream zos, String fileName) {
        if (file.isDirectory()) {
            for (File tmp: file.listFiles()) {
                checkFileType(tmp, zos, fileName +"/"+ tmp.getName());
            }
        } else {
            addZipFile(zos, fileName);
        }
    }

    /**
    * add File to Zip
    * @param file
    * @param zos
    * @param fileName
    * @throws Exception
    */
    private static void addZipFile (ZipOutputStream zos, String fileName) {
        try (FileInputStream fis = new FileInputStream(fileName);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.setLevel(0);
            
            byte[] b = new byte[1024];
            int n;
            while((n = bis.read(b)) > 0) {
                zos.write(b, 0, n);
            }
            
            zos.closeEntry();
            bis.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
