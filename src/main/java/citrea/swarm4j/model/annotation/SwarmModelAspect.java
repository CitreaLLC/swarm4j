package citrea.swarm4j.model.annotation;

import citrea.swarm4j.model.callback.OpRecipient;
import org.aspectj.lang.Aspects;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 09/11/13
 *         Time: 23:15
 */
@Aspect
public class SwarmModelAspect extends Aspects {
    private static final Logger logger = LoggerFactory.getLogger(SwarmModelAspect.class);

    //@Pointcut("@annotation(citrea.swarm4j.model.annotation.SwarmModel)")
    public void ofSwarmModel() {}

    public void initOfSwarmModel() {}

    //@Pointcut("execution(* @citrea.swarm4j.model.annotation.SwarmMethod *.*(..)) && ofSwarmModel()")
    public void swarmMethodsCalling() {}

    //@Pointcut("deliver(@citrea.swarm4j.model.annotation.SwarmField * *) && ofSwarmModel()")
    public void swarmFieldChanging() {}

    @Before("within(citrea.swarm4j.model.SampleModel+)")
    public void beforeModelInit(JoinPoint jp) throws Throwable {
        logger.debug("beforeModelInit {}", jp.toLongString());
    }

    //@DeclareParents(value = "citrea.swarm4j.model.SampleModel", defaultImpl = EventRelay.class)
    public OpRecipient opRecipient;


    //@Before("swarmFieldChanging()")
    public void beforeFieldChanging(Object that) {
        logger.debug("beforeFieldChanging {}", that);
    }
}
