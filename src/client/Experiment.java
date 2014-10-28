package client;

/**
 *
 * @author Scott
 */
public class Experiment {
    public static void main(String[] args) {
        System.out.println("Testing...");
        
        NonPOVClient.main(args);
        
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
