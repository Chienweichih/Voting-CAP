/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package voting_pov.utility;

import java.io.File;

/**
 *
 * @author Chienweichih
 */
public class Test {
    public static void main(String[] args) {
        String path = "attestations" + File.separator + "service-provider" + File.separator;
        Utils.zipDir(new File(path + "new"), new File(path + "TEST"));
    }
}
