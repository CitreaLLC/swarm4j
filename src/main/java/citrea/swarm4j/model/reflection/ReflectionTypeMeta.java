package citrea.swarm4j.model.reflection;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.annotation.SwarmField;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.FieldMeta;
import citrea.swarm4j.model.meta.OperationMeta;
import citrea.swarm4j.model.meta.TypeMeta;
import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 20:18
 */
public class ReflectionTypeMeta implements TypeMeta {

    private Class<? extends Syncable> type;
    private SpecToken typeToken;
    private Map<String, OperationMeta> operations = new HashMap<String, OperationMeta>();
    private Map<String, FieldMeta> fields = new HashMap<String, FieldMeta>();

    public ReflectionTypeMeta(Class<? extends Syncable> type) throws SwarmException {
        this.type = type;
        this.typeToken = new SpecToken(SpecQuant.TYPE, type.getSimpleName());
        detectSyncOperations();
        detectSyncFields();
    }

    private void detectSyncOperations() throws SwarmException {
        for (Method method : this.type.getMethods()) {
            SwarmOperation methodMeta = null;
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof SwarmOperation) {
                    methodMeta = (SwarmOperation) annotation;
                    break;
                }
            }
            if (methodMeta == null) {
                continue;
            }

            SwarmMethodSignature signature = SwarmMethodSignature.detect(method);
            if (signature == null) {
                throw new SwarmException("Unsupported signature of '" + method.getName() + "' method");
            }

            // TODO validate method name Syncable.RE_METHOD_NAME

            SwarmOperationKind kind = methodMeta.kind();
            operations.put(method.getName(), new ReflectionOpMeta(method, kind, signature));
        }
    }

    private void detectSyncFields() {
        for (Field field : this.type.getFields()) {
            SwarmField fieldMeta = null;
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof SwarmField) {
                    fieldMeta = (SwarmField) annotation;
                    break;
                }
            }

            if (fieldMeta == null) {
                continue;
            }

            fields.put(field.getName(), new FieldWrapper(field));
        }

    }

    @Override
    public Class<? extends Syncable> getType() {
        return type;
    }

    @Override
    public SpecToken getTypeToken() {
        return typeToken;
    }

    @Override
    public Syncable newInstance(SpecToken id, Host host) throws SwarmException {
        try {
            Constructor<? extends Syncable> constructor = type.getConstructor(SpecToken.class, Host.class);
            return constructor.newInstance(id, host);
        } catch (NoSuchMethodException e) {
            throw new SwarmException("Suitable constructor not found: " + typeToken.toString() + id.toString(), e);
        } catch (InvocationTargetException e) {
            throw new SwarmMethodInvocationException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new SwarmMethodInvocationException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new SwarmMethodInvocationException(e.getMessage(), e);
        }
    }

    @Override
    public FieldMeta getFieldMeta(String fieldName) {
        return fields.get(fieldName);
    }

    @Override
    public OperationMeta getOperationMeta(SpecToken op) {
        return getOperationMeta(op.getBody());
    }

    @Override
    public OperationMeta getOperationMeta(String opName) {
        return operations.get(opName);
    }

    @Override
    public Collection<FieldMeta> getAllFields() {
        return fields.values();
    }
}
