package service;

import static service.Config.DATA_DIR_PATH;

/**
 *
 * @author Scott
 */
public enum File {
    ONE_KB ("1KB", 1024),
    HUNDRED_KB ("100KB", 100 * 1024),
    ONE_MB ("1MB", 1024 * 1024),
    TEN_MB ("10MB", 10 * 1024 * 1024),
    HUNDRED_MB ("100MB", 100 * 1024 * 1024);

    private final String path;
    private final long size;

    private File(String fname, long fsize) {
        this.path = fname;
        this.size = fsize;
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
}