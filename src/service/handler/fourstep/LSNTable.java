package service.handler.fourstep;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Scott
 */
public class LSNTable {
    private final ConcurrentHashMap<String, Integer> table;
    
    public LSNTable() {
        table = new ConcurrentHashMap<>();
    }
    
    private Integer getLSNorAdd(String id) {
        Integer lsn;
        
        if (table.containsKey(id)) {
            lsn = table.get(id);
        } else {
            lsn = 1;
            
            table.put(id, lsn);
        }
        
        return lsn;
    }
    
    public boolean isMatched(String id, Integer lsn) {
        return Objects.equals(getLSNorAdd(id), lsn);
    }
    
    public void increment(String id) {
        table.put(id, getLSNorAdd(id) + 1);
    }
}
