package client;

import utility.Utils;

/**
 *
 * @author Scott
 */
public class Experiment {
    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println("Loading...");
        
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        
        classLoader.loadClass(NonPOVClient.class.getName());
        classLoader.loadClass(CSNClient.class.getName());
        classLoader.loadClass(ChainHashClient.class.getName());
        classLoader.loadClass(ChainHashAndLSNClient.class.getName());
        classLoader.loadClass(DoubleChainHashClient.class.getName());
        
        Utils.cleanAllAttestations();
        
        System.out.println("\nStart...");
        
        System.out.println("\nnon-POV scheme");
        
        NonPOVClient.main(args);
        
        System.out.println("\nCSN scheme");
        
        CSNClient.main(args);
        
        System.out.println("\nChainHash scheme");
        
        ChainHashClient.main(args);
        
        System.out.println("\nC&L scheme");
        
        ChainHashAndLSNClient.main(args);
        
        System.out.println("\nDoubleHash scheme");
        
        DoubleChainHashClient.main(args);
    }
}
