package voting_pov.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chienweichih
 */
public class MerkleTree {
    public static String create (String srcPath,
                                 String destPath,
                                 boolean isFirst) throws IOException {
        File srcRoot = new File(srcPath);
        File destRoot = new File(destPath);
        
        if (isFirst) {
            if (!srcRoot.exists()) {
                throw new FileNotFoundException("File Directory Not Found!!");
            }

            if (!destRoot.exists()) {
                destRoot.mkdir();
            }
            Utils.clearDirectory(destRoot);
        }
        
        String folderDigest = "";
        for (File file : sortedFiles(srcRoot.listFiles())) {
            String digest;
            String fname = destRoot.getCanonicalPath() + File.separator + file.getName();

            if (file.isDirectory()) {
                new File(fname).mkdir();
                digest = create(file.getPath(), fname, false);
            } else {
                digest = Utils.digest(file);
            }

            Utils.writeDigest(fname, digest);
            folderDigest += digest;
        }
        folderDigest = Utils.digest(Utils.Str2Hex(folderDigest));
        
        if (isFirst) {
            Utils.writeDigest(destPath, folderDigest);
        }
        
        return folderDigest;
    }
    
    public static boolean update (String dataHome,
                                  String hashHome,
                                  String pathUnderHome,
                                  File file) {
        String absPath = hashHome + pathUnderHome;
        String digest = null;
        
        if (file.isDirectory()) {
            try {
                digest = create(dataHome + pathUnderHome, absPath, true);
            } catch (IOException ex) {
                Logger.getLogger(MerkleTree.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            digest = Utils.digest(file);
        }
        
        return update(hashHome, adjustLastChar(absPath) + ".digest", digest);
    }
    
    public static boolean update (String hashHome,
                                  String absPath,
                                  String digest) {
        if (absPath.equals(adjustLastChar(hashHome) + ".digest")) {
            return Utils.readDigest(hashHome).equals(digest);
        }
        
        Utils.writeDigest(removeSuffix(absPath), digest);
        
        File parent = new File(absPath).getParentFile();
        while (parent.exists()) {
            String folderDigest = "";
            for (File file : sortedFiles(parent.listFiles())) {
                if (file.isDirectory()) {
                    continue;
                }
                String fname = removeSuffix(file.getPath());
                folderDigest += Utils.readDigest(fname);
            }

            folderDigest = Utils.digest(Utils.Str2Hex(folderDigest));
            Utils.writeDigest(parent.getPath(), folderDigest);

            File hashRoot = new File(hashHome);
            try {
                if (parent.getCanonicalPath().equals(hashRoot.getCanonicalPath())) {
                    return true;
                }
            } catch (IOException ex) {
                Logger.getLogger(MerkleTree.class.getName()).log(Level.SEVERE, null, ex);
            }
            parent = parent.getParentFile();
        }
        
        return false;
    }

    public static boolean delete (String hashHome,
                                  String pathUnderHome) throws FileNotFoundException {
        File hashFile = new File(pathUnderHome);
        if (!hashFile.exists()) {
            throw new FileNotFoundException("File Path Not Found!!");
        }
        
        String updateFromHere = hashFile.getParent();
        if (hashFile.isDirectory()) {
            new File(adjustLastChar(pathUnderHome) + ".digest").delete();
            Utils.clearDirectory(hashFile);
        }
        hashFile.delete();

        File[] sibling = new File(updateFromHere).listFiles();
        if (sibling != null && sibling.length > 0) {
            updateFromHere = sibling[0].getPath();
        }    
        if (new File(updateFromHere).isDirectory()) {
            updateFromHere = adjustLastChar(updateFromHere) + ".digest";
        }
        return update(hashHome, updateFromHere, Utils.readDigest(removeSuffix(updateFromHere)));
    }
    
    public static void copy (String src, String dest) {
        File destFolder = new File(dest);
        if (!destFolder.exists()) {
            destFolder.mkdir();
        }
        Utils.clearDirectory(destFolder);

        File srcFolder = new File(src);
        Utils.copyFolder(srcFolder, destFolder);
        
        File srcRoothash = new File(adjustLastChar(src) + ".digest");
        File destRoothash = new File(adjustLastChar(dest) + ".digest");
        Utils.copyFolder(srcRoothash, destRoothash);
    }

    private static List<File> sortedFiles(File[] unSortedFiles) {
        List<File> files = Arrays.asList(unSortedFiles);
        Collections.sort(files, (File lhs, File rhs) -> {
            String lhsName = lhs.getName();
            String rhsName = rhs.getName();
            
            if (lhsName.endsWith(".digest")) {
                lhsName = removeSuffix(lhsName);
            }
            if (rhsName.endsWith(".digest")) {
                rhsName = removeSuffix(rhsName);
            }
            
            return lhsName.compareTo(rhsName);
        });
        return files;
    }
    
    private static String removeSuffix (String s) {
        return s.substring(0, s.lastIndexOf(".digest"));
    }
    
    private static String adjustLastChar (String s) {
        int lastIndex = s.length() - 1;
        if (s.charAt(lastIndex) == File.pathSeparatorChar) {
            s = s.substring(0, lastIndex);
        }
        return s;
    }

    private static void print (String hashHome) {
        File hashRoot = new File(hashHome);
        for (File file : sortedFiles(hashRoot.listFiles())) {
            if (file.isDirectory()) {
                continue;
            }
            String digest = Utils.readDigest( removeSuffix(file.getPath()) );
            System.out.println(file.getName() + " : " + digest);
        }
        System.out.println("roothash : " + Utils.readDigest(hashHome));
    }

    private static void unitTest() throws IOException {
        String dataHome = "TEST_DATA";
        String hashHome = "TEST_DATA_HASH";
        
        
        long time = System.currentTimeMillis();
        create(dataHome, hashHome, true);
        time = System.currentTimeMillis() - time;
        System.out.println("Create cost : " + time + "ms");
        print(hashHome);
        
        /**
         * update a file's hash
         */
        String pathUnderHome = File.separator + "src" +
                               File.separator + "summer" +
                               File.separator + "ChainHashing" +
                               File.separator + "Base64codc.java";
        File updateFile = new File(dataHome + pathUnderHome);        
        time = System.currentTimeMillis();
        if (update(dataHome, hashHome, pathUnderHome, updateFile)) {
            System.out.println("Update OK!");
        } else {
            System.out.println("Update Fail.");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("Update cost : " + time + "ms");
        print(hashHome);
        
        String parentPath = File.separator + "src" +
                            File.separator + "summer" +
                            File.separator + "ChainHashing" + File.separator;
        File parent = new File(dataHome + parentPath);
        time = System.currentTimeMillis();
        if (update(dataHome, hashHome, parentPath, parent)) {
            System.out.println("Update Folder OK!");
        } else {
            System.out.println("Update Folder Fail.");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("Update Folder cost : " + time + "ms");
        print(hashHome);

        /**
         * delete a file's hash
         */
        time = System.currentTimeMillis();
        if (delete(hashHome, hashHome + pathUnderHome + ".digest")) {
            System.out.println("Delete OK!");
        } else {
            System.out.println("Delete Fail.");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("Delete cost : " + time + "ms");
        print(hashHome);
    }

    public static void main(String[] args) {
        try {
            unitTest();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
