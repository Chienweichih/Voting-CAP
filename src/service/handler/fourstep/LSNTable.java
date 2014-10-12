package service.handler.fourstep;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Scott
 */
public class LSNTable {
    private final ConcurrentHashMap<String, AtomicInteger> table;
    
    public LSNTable() {
        table = new ConcurrentHashMap<>();
    }
    
    private AtomicInteger getLSNorAdd(String id) {
        AtomicInteger lsn;
        
        if (table.containsKey(id)) {
            lsn = table.get(id);
        } else {
            lsn = new AtomicInteger(1);
            
            table.put(id, lsn);
        }
        
        return lsn;
    }
    
    public boolean isMatched(String id, Integer lsn) {
        return getLSNorAdd(id).get() == lsn;
    }
    
    public void increment(String id) {
        getLSNorAdd(id).incrementAndGet();
    }
}
