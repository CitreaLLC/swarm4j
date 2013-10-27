package citrea.swarm4j.spec;

import java.util.HashMap;
import java.util.Map;

/**
 * Possible quants in specifier
 *
 * @see Spec
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 21:51
 */
public enum SpecQuant {
    TYPE('/'),
    MEMBER('.'),
    ID('#'),
    VERSION('!');

    private static final Map<Character, SpecQuant> code2item;
    public static final String allCodes;

    static {
        code2item = new HashMap<Character, SpecQuant>(4);
        code2item.put('/', TYPE);
        code2item.put('.', MEMBER);
        code2item.put('#', ID);
        code2item.put('!', VERSION);

        String codes = "";
        for (SpecQuant q : SpecQuant.values()) {
            codes += q.toString();
        }
        allCodes = codes;
    }

    public final Character code;

    private SpecQuant(Character code) {
        this.code = code;
    }

    public static SpecQuant forCode(Character q) {
        return code2item.get(q);
    }

    public static SpecQuant forCode(String q) {
        return code2item.get(q.charAt(0));
    }
}
