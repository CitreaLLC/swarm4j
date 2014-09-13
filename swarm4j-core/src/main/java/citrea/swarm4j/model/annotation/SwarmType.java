package citrea.swarm4j.model.annotation;

import java.lang.annotation.*;

/**
 * Instances of classes marked by @SwarmModel annotation will be synchronized
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 09/11/13
 *         Time: 22:46
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SwarmType {

    /**
     * @return name of Class as spec token string (by default: the real target class name)
     */
    String value() default "";
}
