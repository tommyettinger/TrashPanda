package trash.panda;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Tommy Ettinger on 8/21/2016.
 */
public class ScavengingTest {
    @Test
    public void testEncodeSample() {
        for (String name : new String[]{"/sample.json", "/sample.java"}) {
            InputStream is = ScavengingTest.class.getResourceAsStream(name);
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String inp = s.hasNext() ? s.next() : "";
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(inp);
            System.out.println("\n");
            String scrambled = Scavenging.scramble(inp, 0xBEEFBEEFBEEFBEEFL), unscrambled;
            System.out.println(scrambled);
            System.out.println("\n");
            unscrambled = Scavenging.unscramble(scrambled, 0xBEEFBEEFBEEFBEEFL);
            System.out.println(unscrambled);
            System.out.println("\n");
            Assert.assertEquals(inp, unscrambled);
            scrambled = Scavenging.scramble(inp, 0L);
            System.out.println(scrambled);
            System.out.println("\n");
            unscrambled = Scavenging.unscramble(scrambled, 0L);
            System.out.println(unscrambled);
            System.out.println("\n");
            Assert.assertEquals(inp, unscrambled);
        }
    }
}
