package citrea.swarm4j.util;

import org.apache.commons.codec.binary.Base64;
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
        Base64 base64 = new Base64();
        return base64.encodeAsString(SecureRandom.getSeed(bytesNum)).replaceAll("=+$", "");
    }
}
