package citrea.swarm4j.model.reflection;

import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.OperationMeta;
import citrea.swarm4j.model.spec.Spec;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.08.2014
 *         Time: 16:04
 */
public class ReflectionOpMeta implements OperationMeta {

    private Method method;
    private SwarmOperationKind kind;
    private SwarmMethodSignature signature;

    ReflectionOpMeta(Method method, SwarmOperationKind kind, SwarmMethodSignature signature) {
        this.method = method;
        this.kind = kind;
        this.signature = signature;
    }

    @Override
    public void invoke(Syncable object, Spec spec, JSONValue value, OpRecipient source) throws SwarmMethodInvocationException {
        try {
            switch (this.signature) {
                case NONE:
                    this.method.invoke(object);
                    break;
                case SPEC:
                    this.method.invoke(object, spec);
                    break;
                case SPEC_VALUE:
                    this.method.invoke(object, spec, value);
                    break;
                case SPEC_VALUE_SOURCE:
                    this.method.invoke(object, spec, value, source);
                    break;
                case SPEC_SOURCE:
                    this.method.invoke(object, spec, source);
                    break;
                case VALUE:
                    this.method.invoke(object, value);
                    break;
                case VALUE_SOURCE:
                    this.method.invoke(object, value, source);
                    break;
                case SOURCE:
                    this.method.invoke(object, source);
                    break;
                default:
                    throw new SwarmMethodInvocationException("Unsupported method signature: " + this.signature);
            }
        } catch (IllegalAccessException e) {
            throw new SwarmMethodInvocationException("Method invocation error: " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable ex = e.getTargetException();
            throw new SwarmMethodInvocationException("Method invocation error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public SwarmOperationKind getKind() {
        return kind;
    }

}
