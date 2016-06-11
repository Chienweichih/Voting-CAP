package service;

/**
 *
 * @author Scott
 */
public enum Key {
    CLIENT ("client"),
    SERVICE_PROVIDER ("service_provider");
    
    private final String keyName;
    
    private Key(String keyName) {
        this.keyName = keyName;
    }
    
    public String getKeyId() {
        return keyName;
    }
}
    
