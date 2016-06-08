package wei_chih.service;

/**
 *
 * @author chienweichih
 */
public interface Config extends service.Config {
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public String SERVICE_HOSTNAME = "localhost";//"140.122.184.105";
    public String SYNC_HOSTNAME = SERVICE_HOSTNAME;
    
    public int SERVICE_NUM = 3;
    public int SERVICE_PORT[] = {3000, 3001, 3002, 3003, 3004,
                                 3005, 3006, 3007, 3008, 3009,
                                 3010, 3011, 3012, 3013, 3014,
                                 3015, 3016, 3017, 3018,
                                 3019, 3020};
    
    public int WEI_SHIAN_SERVICE_PORT = SERVICE_PORT[SERVICE_PORT.length - 1];
    public int WEI_SHIAN_SYNC_PORT = SERVICE_PORT[SERVICE_PORT.length - 2];
    
    public String CLIENT_ACCOUNT_PATH = "../Accounts/";//"/media/wcc/Data/Accounts/";
    public String SERVER_ACCOUNT_PATH = CLIENT_ACCOUNT_PATH;//"../Accounts/";

    public String EMPTY_STRING = " ";
    public String DOWNLOAD_FAIL = "download fail";
    public String UPLOAD_FAIL = "upload fail";
    public String AUDIT_FAIL = "audit fail";
    public String OP_TYPE_MISMATCH = "operation type mismatch";
    public String WRONG_OP = "wrong op";
    
    public String PATH_SEPARATOR = "/";
}