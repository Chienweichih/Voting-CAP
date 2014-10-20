package service.handler.fourstep;

/**
 *
 * @author Scott
 */
public class DoubleHashingChainTable extends HashingChainTable {
    public final static String MAIN_CHAIN = "__MAIN_CHAIN__";
    
    public void chain(String hash) {
        super.chain(MAIN_CHAIN, hash);
    }
    
    @Override
    public void chain(String id, String hash) {
        super.chain(id, hash);
    }
    
    public String getLastChainHashOfAll() {
        return super.getLastChainHash(MAIN_CHAIN);
    }
}
