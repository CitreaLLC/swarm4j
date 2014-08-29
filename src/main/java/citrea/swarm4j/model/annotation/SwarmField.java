package citrea.swarm4j.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every change to a field marked with @SwarmField generates a version
 * that propagates to other replicas in real time.
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 09/11/13
 *         Time: 22:46
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SwarmField {

    //TODO allow specifying (JSONValue <-> FieldType convertor)
}
