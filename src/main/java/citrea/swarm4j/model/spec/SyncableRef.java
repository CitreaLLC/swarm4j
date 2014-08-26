package citrea.swarm4j.model.spec;

import citrea.swarm4j.model.Syncable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 22:32
 */
public class SyncableRef extends Spec {

    private Syncable object;

    public SyncableRef(Syncable object) {
        super(object.getTypeId());
        this.object = object;
    }

    public Syncable getObject() {
        return object;
    }
}
