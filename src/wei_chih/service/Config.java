package wei_chih.service;

import java.io.File;

/**
 *
 * @author chienweichih
 */
public interface Config extends service.Config {
    public String SERVICE_HOSTNAME = "140.122.184.105";
    public String SYNC_HOSTNAME = SERVICE_HOSTNAME;
    
    public int SERVICE_NUM = 3;
    public int SERVICE_PORT[] = {3004, 3003, 3002, 3001, 3000};
    
    public String DATA_A_PATH = ".." + File.separator + "Accounts" + File.separator + "Account A";
    public String DATA_B_PATH = ".." + File.separator + "Accounts" + File.separator + "Account B";
    public String DATA_C_PATH = ".." + File.separator + "Accounts" + File.separator + "Account C";
    public String DATA_D_PATH = ".." + File.separator + "Accounts" + File.separator + "Account D";    
    
    public String DATA_A_TESTFILE = File.separator + "E5435D8B01DACDA29CE5F9402CB203ECFDD2E6F6" + File.separator + "54C38E3E2508FFFF92D4F99E8A8EF2C64B1AF16F" + File.separator + "B0C05125F7E2FD6ABE48C47F336399D71986B1D7";
    public String DATA_B_TESTFILE = File.separator + "0F2185683D96A4934D563A5E4966C9FD09EB7320" + File.separator + "2B84F621C0FD4BA8BD514C5C43AB9A897C8C014E" + File.separator + "8BF95EA372568C7A254BA9FADA5F6F1701EBC5B5";
    public String DATA_C_TESTFILE = File.separator + "63DDCC70EEDB647AB89E1AA41C52054FA0C006C2" + File.separator + "1AB37BB1D15ADDB2B20D70CCAECC09D2E6B0DD9A" + File.separator + "3BC27AF61D4D129E5F4C8EA30ADFCBFE1B322333";
    public String DATA_D_TESTFILE = DATA_C_TESTFILE;
    
    public String EMPTY_STRING = " ";
    public String DOWNLOAD_FAIL = "download fail";
    public String UPLOAD_FAIL = "upload fail";
    public String AUDIT_FAIL = "audit fail";
    public String OP_TYPE_MISMATCH = "operation type mismatch";
    public String WRONG_OP = "wrong op";
}