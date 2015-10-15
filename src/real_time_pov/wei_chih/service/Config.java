package real_time_pov.wei_chih.service;

import java.io.File;

/**
 *
 * @author chienweichih
 */
public interface Config extends service.Config {
    public final String SYNC_HOSTNAME = "localhost";
    
    public final int VOTING_SERVICE_PORT_1 = 3011;
    public final int VOTING_SERVICE_PORT_2 = 3012;
    public final int VOTING_SERVICE_PORT_3 = 3013;
    public final int VOTING_SERVICE_PORT_4 = 3014;
    public final int VOTING_SERVICE_PORT_5 = 3015;
    
    public final int VOTING_SYNC_PORT = 3016;
    
    public final String DATA_DIR_PATH = ".." + File.separator + "Accounts" + File.separator + "Account A";
    
    public final String TEST_FILE_NAME =    File.separator + "folder1" + File.separator + "small_1.txt";
//                                            File.separator + "folder3" + File.separator + "2011.rmvb";
//
//                                            File.separator + "testing result" + 
//                                            File.separator + "DeadLock1" + 
//                                            File.separator + "DeadLock" + 
//                                            File.separator + "DeadLock(0).txt";

//                                            File.separator + "My courses" +
//                                            File.separator + "System Software" +
//                                            File.separator + "Slice from NCU" +
//                                            File.separator + "chap_01.pps";
    
    public final String EMPTY_STRING = " ";
    public final String DOWNLOAD_FAIL = "download fail";
    public final String UPLOAD_FAIL = "upload fail";
    public final String AUDIT_FAIL = "audit fail";
    public final String OP_TYPE_MISMATCH = "operation type mismatch";
    public final String WRONG_OP = "wrong op";
}