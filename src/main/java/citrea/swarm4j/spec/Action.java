package citrea.swarm4j.spec;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 12:26
 */
public enum Action {
    on, reOn, off, reOff, set;

    public static final String QUANT = "*";

    private static Set<String> names = new HashSet<String>(Action.values().length);

    static {
        for (Action a: Action.values()) {
            names.add(a.name());
        }
    }

    public static Action byName(String code) {
        return names.contains(code) ? Action.valueOf(code) : Action.set;
    }

    public String asToken() {
        return (Action.set == this) ? "" : QUANT + name();
    }
}
