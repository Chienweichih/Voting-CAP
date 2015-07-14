package message;

import java.util.LinkedHashMap;

/**
 *
 * @author Scott
 */
public class Operation {
    private final OperationType type;
    private final String path;
    private final String message;
    private final LinkedHashMap<String, String> map;
    
    public Operation(OperationType type, String path, String msg) {
        this.type = type;
        this.path = path.indexOf('\\') != -1 ? path.replace('\\', '/') : path;
        this.message = msg;
        
        map = new LinkedHashMap<>();
        map.put("name", "operation");
        map.put("type", type.toString());
        map.put("path", path);
        map.put("message", msg);
    }
    
    public OperationType getType() {
        return type;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LinkedHashMap<String, String> toMap() {
        return map;
    }
}
