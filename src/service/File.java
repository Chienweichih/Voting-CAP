package service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static service.Config.DATA_DIR_PATH;

/**
 *
 * @author Scott
 */
public enum File {
    ONE_KB ("1KB", 1024),
    ONE_KB_2 ("1KB_2", 1024),
    ONE_KB_3 ("1KB_3", 1024),
    ONE_KB_4 ("1KB_4", 1024),
    HUNDRED_KB ("100KB", 100 * 1024),
    HUNDRED_KB_2 ("100KB_2", 100 * 1024),
    HUNDRED_KB_3 ("100KB_3", 100 * 1024),
    HUNDRED_KB_4 ("100KB_4", 100 * 1024),
    ONE_MB ("1MB", 1024 * 1024),
    ONE_MB_2 ("1MB_2", 1024 * 1024),
    ONE_MB_3 ("1MB_3", 1024 * 1024),
    ONE_MB_4 ("1MB_4", 1024 * 1024),
    TEN_MB ("10MB", 10 * 1024 * 1024),
    TEN_MB_2 ("10MB_2", 10 * 1024 * 1024),
    TEN_MB_3 ("10MB_3", 10 * 1024 * 1024),
    TEN_MB_4 ("10MB_4", 10 * 1024 * 1024),
    HUNDRED_MB ("100MB", 100 * 1024 * 1024),
    HUNDRED_MB_2 ("100MB_2", 100 * 1024 * 1024),
    HUNDRED_MB_3 ("100MB_3", 100 * 1024 * 1024),
    HUNDRED_MB_4 ("100MB_4", 100 * 1024 * 1024),
    UNKNOWN ("UNKNOWN", 0);

    private final String path;
    private final long size;
    private final ReentrantReadWriteLock lock;

    private File(String fname, long fsize) {
        this.path = fname;
        this.size = fsize;
        this.lock = new ReentrantReadWriteLock();
    }

    public String getName() {
        return String.format("%s.bin", path);
    }

    public String getPath() {
        return String.format("%s/%s", DATA_DIR_PATH, getName());
    }

    public long getSize() {
        return size;
    }
    
    public ReentrantReadWriteLock getLock() {
        return lock;
    }
    
    public Lock getReadLock() {
        return lock.readLock();
    }
    
    public Lock getWriteLock() {
        return lock.writeLock();
    }
    
    public static File parse(String s) {
        for (File f : File.values()) {
            if (f.getName().compareTo(s) == 0) {
                return f;
            }
        }
        
        return UNKNOWN;
    }
}