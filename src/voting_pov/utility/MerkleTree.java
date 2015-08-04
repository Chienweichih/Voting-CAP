package voting_pov.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chienweichih
 */
public class MerkleTree {
    public static String create (String dirRootPath,
                                 String hashRootPath,
                                 boolean isFirst) throws IOException {
        File dirRoot = new File(dirRootPath);
        File hashRoot = new File(hashRootPath);
        
        if (isFirst) {
            if (!dirRoot.exists()) {
                throw new FileNotFoundException("File Directory Not Found!!");
            }

            if (!hashRoot.exists()) {
                hashRoot.mkdir();
            }
            Utils.clearDirectory(hashRoot);
        }
        
        String folderDigest = "";
        for (File file : sortedFiles(dirRoot.listFiles())) {
            String digest;
            String fname = hashRoot.getCanonicalPath() + "/" + file.getName();

            if (file.isDirectory()) {
                new File(fname).mkdir();
                digest = create(file.getPath(), fname, false);
            } else {
                digest = Utils.digest(file);
            }

            Utils.writeDigest(fname, digest);
            folderDigest = folderDigest.concat(digest);
        }
        folderDigest = Utils.digest(Utils.Str2Hex(folderDigest));
        
        if (isFirst) {
            Utils.writeDigest(hashRootPath, folderDigest);
        }
        
        return folderDigest;
    }
    
    public static boolean update (String dirRootPath,
                                  String hashRootPath,
                                  String filePathUnderDir,
                                  File updateFile) throws IOException {
        String updateDigestPath = hashRootPath + filePathUnderDir;
        String hashValue;
        if (updateFile.isDirectory()) {
            hashValue = create(dirRootPath + filePathUnderDir, updateDigestPath, true);
        } else {
            hashValue = Utils.digest(updateFile);
        }
        
        return update(hashRootPath, adjustLastChar(updateDigestPath) + ".digest", hashValue);
    }
    
    public static boolean update (String hashRootPath,
                                  String digestPath,
                                  String digest) throws IOException {
        Utils.writeDigest(removeSuffix(digestPath), digest);
        
        File parent = new File(digestPath).getParentFile();
        while (parent.exists()) {
            String folderDigest = "";
            for (File file : sortedFiles(parent.listFiles())) {
                if (file.isDirectory()) {
                    continue;
                }
                String fname = removeSuffix(file.getPath());
                folderDigest = folderDigest.concat(Utils.readDigest(fname));
            }

            folderDigest = Utils.digest(Utils.Str2Hex(folderDigest));
            Utils.writeDigest(parent.getPath(), folderDigest);

            File hashRoot = new File(hashRootPath);
            if (parent.getCanonicalPath().equals(hashRoot.getCanonicalPath())) {
                return true;
            }
            parent = parent.getParentFile();
        }
        
        return false;
    }

    public static boolean delete (String hashRootPath,
                                  String digestPath) throws FileNotFoundException, IOException {
        File hashFile = new File(digestPath);
        if (!hashFile.exists()) {
            throw new FileNotFoundException("File Path Not Found!!");
        }
        
        String updateFromHere = hashFile.getParent();
        if (hashFile.isDirectory()) {
            new File(adjustLastChar(digestPath) + ".digest").delete();
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
        return update(hashRootPath, updateFromHere, Utils.digest(updateFromHere));
    }
    
    public static void copy (String oldHashPath,
                             String newHashPath) throws IOException {
        File newHashRoot = new File(newHashPath);
        if (!newHashRoot.exists()) {
            newHashRoot.mkdir();
        }
        Utils.clearDirectory(newHashRoot);

        File oldHashRoot = new File(oldHashPath);
        Utils.copyFolder(oldHashRoot, newHashRoot);
    }

    private static List<File> sortedFiles(File[] unSortedFiles) {
        List<File> files = Arrays.asList(unSortedFiles);
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {

                String lhsName = lhs.getName();
                String rhsName = rhs.getName();

                if (lhsName.endsWith(".digest")) {
                    lhsName = lhsName.substring(0, lhsName.lastIndexOf(".digest"));
                }

                if (rhsName.endsWith(".digest")) {
                    rhsName = rhsName.substring(0, rhsName.lastIndexOf(".digest"));
                }

                return lhsName.compareTo(rhsName);
            }
        });
        return files;
    }
    
    private static String removeSuffix (String s) {
        return s.substring(0, s.lastIndexOf(".digest"));
    }
    
    private static String adjustLastChar (String s) {
        int lastIndex = s.length() - 1;
        if (s.charAt(lastIndex) == '/') {
            s = s.substring(0, lastIndex);
        }
        return s;
    }

    private static void print (String hashRootPath) {
        File fileDir = new File(hashRootPath);
        for (File file : sortedFiles(fileDir.listFiles())) {
            if (file.isDirectory()) {
                continue;
            }
            String digest = Utils.readDigest( removeSuffix(file.getPath()) );
            System.out.println(file.getName() + " : " + digest);
        }
        System.out.println("roothash : " + Utils.readDigest(hashRootPath));
    }

    private static void unitTest() throws IOException {
        String dirRootPath = "TEST_DATA";
        String hashRootPath = "TEST_DATA_HASH";
        
        create(dirRootPath, hashRootPath, true);
        print(hashRootPath);
        
        /**
         * update a file's hash
         */
        String filePathUnderDir = "/src/summer/ChainHashing/Base64codc.java";
        File updateFile = new File(dirRootPath + filePathUnderDir);        
        if (update(dirRootPath, hashRootPath, filePathUnderDir, updateFile)) {
            System.out.println("Update OK!");
        } else {
            System.out.println("Update Fail.");
        }
        print(hashRootPath);
        
        String parentPath = "/src/summer/ChainHashing/";
        File parent = new File(dirRootPath + parentPath);
        if (update(dirRootPath, hashRootPath, parentPath, parent)) {
            System.out.println("Batch Update OK!");
        } else {
            System.out.println("Batch Update Fail.");
        }
        print(hashRootPath);

        /**
         * delete a file's hash
         */
        if (delete(hashRootPath, hashRootPath + filePathUnderDir + ".digest")) {
            System.out.println("Delete OK!");
        } else {
            System.out.println("Delete Fail.");
        }
        print(hashRootPath);
    }

    public static void main(String[] args) {
        long time = System.currentTimeMillis();

        try {
            unitTest();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

        time = System.currentTimeMillis() - time;
        System.out.println(time);
    }
}
