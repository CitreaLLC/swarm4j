package citrea.swarm4j.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods marked as @SwarmMethod(kind=SwarmMethodKind.Logged)
 * being invoked will be executed on every replica
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 09/11/13
 *         Time: 22:49
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SwarmOperation {

    /**
     * @return kind of method calls synchronization
     */
    SwarmOperationKind kind() default SwarmOperationKind.Logged;

    /**
     * @return method aliases (by default: the real method name)
     */
    String[] aliases() default {};
}
