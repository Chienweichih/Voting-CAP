package wei_chih.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

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
     *
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

    public static String getDataDirPath(String args, String pathPrefix) {
        String dataDirPath = Config.EMPTY_STRING;
        try {
            switch (args.charAt(args.length() - 1)) {
                case 'A':
                case 'a':
                    dataDirPath = new File(pathPrefix + "Account A").getCanonicalPath();
                    break;
                case 'B':
                case 'b':
                    dataDirPath = new File(pathPrefix + "Account B").getCanonicalPath();
                    break;
                case 'C':
                case 'c':
                    dataDirPath = new File(pathPrefix + "Account C").getCanonicalPath();
                    break;
                case 'D':
                case 'd':
                    dataDirPath = new File(pathPrefix + "Account D").getCanonicalPath();
                    break;
                default:
                    throw new java.lang.IllegalArgumentException();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        return dataDirPath;
    }

    public static String[] randomPickupFiles(String folderPath, int number) throws FileNotFoundException {
        if (new File(folderPath).exists() == false) {
            throw new java.io.FileNotFoundException();
        }

        String[] fileNames = new String[number];
        int[] index = new int[]{0, number / 4, number / 2, number * 3 / 4};

        while (index[0] + index[1] + index[2] + index[3] < number * 5 / 2) {
            File filePointer = new File(folderPath);
            while (filePointer.isDirectory()) {
                String[] fileList = filePointer.list();
                int childNum = fileList.length;

                if (childNum <= 0) {
                    filePointer = filePointer.getParentFile();
                } else {
                    int randomNum = ThreadLocalRandom.current().nextInt(0, childNum);
                    filePointer = new File(filePointer.getAbsoluteFile() + File.separator + fileList[randomNum]);
                }
            }

            long fileSize = filePointer.length() / 10000;
            int indexNum = 0;
            while (fileSize > 0) {
                fileSize /= 10;
                indexNum++;
            }

            if (indexNum < 4 && index[indexNum] < number * (indexNum + 1) / 4) {
                fileNames[index[indexNum]] = subPath(filePointer.getAbsolutePath());
                index[indexNum]++;
            }
        }

        return fileNames;
    }

    private static String subPath(String path) {
        int indexOfHead = path.indexOf("Accounts");
        if (indexOfHead == -1) {
            return path;
        }
        return path.substring(indexOfHead + "Accounts/Account A".length());
    }

    public static void printExperimentResult(double[] results) {
        int runTimes = results.length;
        double[] avgTime = new double[4];
        for (int i = 0; i < 4; ++i) {
            double sum = 0.0;
            for (int j = 0; j < runTimes / 4; ++j) {
                sum += results[i * 10 + j];
            }
            avgTime[i] = sum * 4 / runTimes;
            System.out.printf("Average Time for File Size under %10.0f Bytes : %f s\n", 10000 * (Math.pow(10, i)), avgTime[i]);
        }

        // for easily copy data
        for (int i = 0; i < 4; ++i) {
            System.out.println(avgTime[i]);
        }
    }
}
