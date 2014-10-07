package client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import service.Config;
import utility.Utils;

/**
 *
 * @author Scott
 */
public class DigestData {
    public static void main(String[] args) {
        File dataDir = new File(Config.DATA_DIR_PATH);
        
        for (File file : dataDir.listFiles()) {
            String fname = dataDir.getName() + "/" + file.getName() + ".digest";
            String digest = Utils.digest(file, Config.DIGEST_ALGORITHM);
            
            try (FileWriter fw = new FileWriter(fname)) {
                fw.write(digest);
            } catch (IOException ex) {
                Logger.getLogger(DigestData.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println(fname + ": " + digest);
        }
    }
}
