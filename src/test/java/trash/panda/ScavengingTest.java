package trash.panda;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Tommy Ettinger on 8/21/2016.
 */
public class ScavengingTest {
    @Test
    public void testEncodeSample()
    {
        InputStream is = ScavengingTest.class.getResourceAsStream("/sample.json");
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String inp = s.hasNext() ? s.next() : "";
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(inp);
        System.out.println("\n");
        String scrambled = Scavenging.scramble(inp, 0xBEEFBEEFBEEFBEEFL);
        System.out.println(scrambled);
        System.out.println("\n");
        System.out.println(Scavenging.unscramble(scrambled, 0xBEEFBEEFBEEFBEEFL));
        System.out.println("\n");
        scrambled = Scavenging.scramble(inp, 0L);
        System.out.println(scrambled);
        System.out.println("\n");
        System.out.println(Scavenging.unscramble(scrambled, 0L));
        System.out.println("\n");
    }
}
