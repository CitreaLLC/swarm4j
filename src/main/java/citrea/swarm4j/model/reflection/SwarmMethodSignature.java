package citrea.swarm4j.model.reflection;

import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.08.2014
 *         Time: 16:20
 */
public enum SwarmMethodSignature {

    NONE(),
    SPEC(Spec.class),
    SPEC_VALUE(Spec.class, JSONValue.class),
    SPEC_VALUE_SOURCE(Spec.class, JSONValue.class, OpRecipient.class),
    VALUE(JSONValue.class),
    VALUE_SOURCE(JSONValue.class, OpRecipient.class),
    SPEC_SOURCE(Spec.class, OpRecipient.class),
    SOURCE(OpRecipient.class);

    private final Class<?>[] paramTypes;

    private SwarmMethodSignature(Class<?> ... paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public static SwarmMethodSignature detect(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        SwarmMethodSignature res = null;
        for (SwarmMethodSignature sig : SwarmMethodSignature.values()) {
            if (Arrays.deepEquals(paramTypes, sig.getParamTypes())) {
                res = sig;
                break;
            }
        }
        return res;
    }
}
