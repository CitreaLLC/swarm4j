package citrea.swarm4j.model.hash;

import citrea.swarm4j.model.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03.09.2014
 *         Time: 23:34
 */
public interface HashFunction {

    int calc(String value);
}
