package utility;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import service.Config;
import service.Config.FileSize;

public class Utils {
    private static final int BUF_SIZE = 8192;
    private static final String HEX = "0123456789ABCDEF";
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    
    /**
     * Converts byte array into hexadecimal string.
     */
    public static String Hex2Str(byte[] bytes) {
        String str = "";
        
        for (byte b: bytes) {
            int v = b & 0xFF;

            str += HEX.charAt(v >> 4) + "" + HEX.charAt(v & 0xF);
        }
        
        return str;
    }
    
    /**
     * Converts hexadecimal string into byte array.
     */
    public static byte[] Str2Hex(String str) {
        byte[] bytes = new byte[str.length() / 2];
        
        byte b;
        
        for (int i = 0; i < str.length(); i += 2) {
            b = (byte) (HEX.indexOf(str.charAt(i)) << 4);
            b += (byte) (HEX.indexOf(str.charAt(i + 1)));
            
            bytes[i / 2] = b;
        }
        
        return bytes;
    }
    
    /**
     * Sends data through the DataOutputStream by converting into UTF-8 bytes.
     */
    public static void send(DataOutputStream out, String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            
            out.writeInt(bytes.length);
            out.write(bytes);
            
            out.flush();
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Reads the file and sends it through the DataOutputStream.
     */
    public static void send(DataOutputStream out, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[BUF_SIZE];
            int n;
                        
            out.writeLong(file.length());
            
            while ((n = fis.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            
            out.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Receives a UTF-8 string from DataInputStream
     */
    public static String receive(DataInputStream in) {
        String data = "";
        
        try {
            byte[] bytes = new byte[in.readInt()];
            
            in.readFully(bytes);
            
            data = new String(bytes, "UTF-8");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return data;
    }
    
    /**
     * Receives a file from DataInputStream.
     */
    public static void receive(DataInputStream in, File file) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[BUF_SIZE];
            int n;
            
            for (long leftBytes = in.readLong(); leftBytes > 0; leftBytes -= n) {
                if (leftBytes > BUF_SIZE) {
                    n = in.read(buf);
                } else {
                    n = (int) leftBytes;
                    
                    in.readFully(buf, 0, n);
                }
                
                fos.write(buf, 0, n);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    public static String digest(File file) {
        return digest(file, Config.DIGEST_ALGORITHM);
    }
    
    /**
     * Digest a file with a specific algorithm.
     * @return the digest value
     */
    public static String digest(File file, String algorithm) {
        String result = null;
        
        try (InputStream fis = new FileInputStream(file)) {
            int n;
            byte[] buffer = new byte[BUF_SIZE];
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            while ((n = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, n);
            }
            
            result = Hex2Str((digest.digest()));
        } catch (NoSuchAlgorithmException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
    public static String digest(String message) {
        return digest(message, Config.DIGEST_ALGORITHM);
    }
    
    /**
     * Digest a string with a specific algorithm.
     * @return the digest value
     */
    public static String digest(String message, String algorithm) {
        String result = null;
        
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            
            result = Hex2Str(digest.digest(message.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    /**
     * Read the digest value from file $fname.digest.
     * @return the pre-saved digest value in $fname.digest
     */
    public static String readDigest(String fname) {
        String digest = null;
        
        try (FileReader fr = new FileReader(fname + ".digest");
             BufferedReader br = new BufferedReader(fr)) {
            digest = br.readLine();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return digest;
    }
    
    /**
     * Digest the file and write its digest value to corresponding file.
     */
    public static void writeDigest(String fname) {
        writeDigest(fname, digest(new File(fname)));
    }
    
    /**
     * Write the digest value to specific file.
     */
    public static void writeDigest(String fname, String digest) {
        write(new File(fname + ".digest"), digest);
    }
    
    /**
     * Write string to specific file.
     */
    public static void write(File file, String str) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.append(str);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Append string to specific file.
     */
    public static void append(File file, String str) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.append(str);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Append string to specific file, then digest it.
     */
    public static void appendAndDigest(File file, String str) {
        append(file, str);
        
        writeDigest(file.getPath());
    }
    
    /**
     * Generates a KeyPair randomly.
     */
    public static KeyPair randomGenerateKeyPair() {
        KeyPair keyPair = null;
        
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512, new SecureRandom());
            keyPair = keyGen.generateKeyPair();
        } catch (SecurityException | NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return keyPair;
    }
    
    /**
     * Reads KeyPair from specific file.
     */
    public static KeyPair readKeyPair(String fname) {
        File file = new File(fname);
        
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            return (KeyPair) in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    /**
     * Delete all of attestation files.
     */
    public static void cleanAllAttestations() {
        for (String path : new String[] { Config.ATTESTATION_DIR_PATH,
                                          Config.DOWNLOADS_DIR_PATH }) {
            File dir = new File(path);

            clearDirectory(dir);
        }
    }
    
    public static void clearDirectory(File folder) {
        if (folder.isFile()) {
            folder.delete();
        } else {
            try {
                for (File file : folder.listFiles()) {
                    clearDirectory(file);
                }
            } catch (NullPointerException e) {
            }
        }
    }
    
    public static void createRequiredFiles() {
        File dir;
        for (String path : new String[] { Config.DOWNLOADS_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH,
                                          Config.ATTESTATION_DIR_PATH + "/client",
                                          Config.ATTESTATION_DIR_PATH + "/service-provider",
                                          Config.DATA_DIR_PATH,
                                          Config.KEYPAIR_DIR_PATH }) {
            dir = new File(path);
            
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        
        dir = new File(String.format("%s/client", Config.ATTESTATION_DIR_PATH));
        dir.mkdirs();
        
        Random r = new Random();
        
        for (FileSize fs : FileSize.values()) {
            File file = new File(fs.getPath());
            
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                
                byte[] buf = new byte[1024];
                long size = 0;
                
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    do {
                        r.nextBytes(buf);
                        fos.write(buf);
                        
                        size += buf.length;
                    } while (size < fs.getSize());
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            
            writeDigest(fs.getPath());
        }
        
        for (Config.KeyPair kp : Config.KeyPair.values()) {
            File keyFile = new File(kp.getPath());
            
            if (!keyFile.exists()) {
                try {
                    keyFile.createNewFile();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            
                try (FileOutputStream fos = new FileOutputStream(keyFile);
                     ObjectOutputStream out = new ObjectOutputStream(fos)) {
                    out.writeObject(randomGenerateKeyPair());
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
