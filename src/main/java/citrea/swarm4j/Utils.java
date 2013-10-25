package citrea.swarm4j;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 25/10/13
 *         Time: 19:30
 */
@Component
public class Utils {

    public String generateRandomId(int bytesNum) {
        Base32 b32 = new Base32();
        return new String(b32.decode(SecureRandom.getSeed(bytesNum)));
    }
}
