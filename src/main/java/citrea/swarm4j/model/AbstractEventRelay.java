package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;
import org.json.JSONString;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 00:50
 */
public abstract class AbstractEventRelay<CHILD extends AbstractEventRelay> {
    protected Swarm swarm;
    private Spec spec;
    private SpecQuant childKey;
    private Map<SpecToken, CHILD> children = new HashMap<SpecToken, CHILD>();
    private List<SwarmEventListener> listeners = new ArrayList<SwarmEventListener>();

    protected AbstractEventRelay(Swarm swarm, Spec spec, SpecQuant childKey) {
        this.swarm = swarm;
        this.spec = spec;
        this.childKey = childKey;
    }

    protected AbstractEventRelay(Swarm swarm, Spec spec) {
        this.swarm = swarm;
        this.spec = spec;
        this.childKey = null;
    }

    public Spec getSpec() {
        return spec;
    }

    public SpecToken getId() {
        return spec.getLastToken();
    }

    public CHILD getChild(SpecToken key) {
        return (key == null) ? null : children.get(key);
    }

    public CHILD getChild(Spec spec) {
        return (childKey == null) ? null : getChild(spec.getToken(childKey));
    }

    protected void addChild(SpecToken key, CHILD child) {
        this.children.put(key, child);
    }

    public void deliver(Spec spec, JSONValue value, SwarmEventListener source) throws SwarmException {

        checkACL(spec, value, source);

        validate(spec, value, source);

        CHILD child = getChild(spec);
        if (child == null) {
            child = createNewChild(spec, value);
        }

        if (child != null) {
            child.deliver(spec, value, source);
        }

        if (!(this instanceof SwarmEventListener)) { // can't accept set/on/off
            throw new SwarmNoChildException(spec);
        }

        final SpecToken member = spec.getMember();
        if (SpecToken.on.equals(member) || SpecToken.reOn.equals(member)) {

            if (!(this instanceof SubscribeEventListener)) {
                throw new SwarmNoChildException(spec);
            }

            ((SubscribeEventListener) this).on(spec, value, source);

        } else if (SpecToken.off.equals(member)) {

            if (!(this instanceof SubscribeEventListener)) {
                throw new SwarmNoChildException(spec);
            }

            ((SubscribeEventListener) this).off(spec, source);

        } else {

            ((SwarmEventListener) this).set(spec, value, source);

        }
    }

    protected void addListener(SwarmEventListener listener) {
        this.listeners.add(listener);
    }

    protected void checkACL(Spec spec, JSONString value, SwarmEventListener listener) throws SwarmSecurityException {
        //do nothing by default
    }

    protected void validate(Spec spec, JSONValue value, SwarmEventListener source) throws SwarmValidationException {
        //do nothing by default
    }

    public void emit(Spec spec, JSONValue value, SwarmEventListener listener) throws SwarmException {
        for (SwarmEventListener l : this.listeners) {
            l.set(spec, value, listener);
        }
        //TODO add reactions / or don't ?
    }

    protected abstract CHILD createNewChild(Spec spec, JSONValue value) throws SwarmException;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEventRelay)) return false;

        AbstractEventRelay that = (AbstractEventRelay) o;

        return spec.equals(that.spec);
    }

    @Override
    public int hashCode() {
        return spec.hashCode();
    }

    public Set<SpecToken> getChildrenKeys() {
        return children.keySet();
    }

    protected static class OnceListenerWrapper implements SwarmEventListener {
        private final SubscribeEventListener self;
        private final Spec initialSpec;
        private final SwarmEventListener innerListener;

        public OnceListenerWrapper(SubscribeEventListener self, Spec spec, SwarmEventListener listener) throws SwarmException {
            this.self = self;
            this.initialSpec = spec;
            this.innerListener = listener;
        }

        @Override
        public void set(Spec spec, JSONValue value, SwarmEventListener listener) throws SwarmException {
            innerListener.set(spec, value, listener);
            self.off(initialSpec, innerListener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            final SwarmEventListener oListener;
            if (o instanceof AbstractEventRelay.OnceListenerWrapper) {
                oListener = ((AbstractEventRelay.OnceListenerWrapper) o).innerListener;
            } else if (o instanceof SwarmEventListener) {
                oListener = ((SwarmEventListener) o);
            } else {
                return false;
            }

            return innerListener.equals(oListener);
        }

        @Override
        public int hashCode() {
            return self.hashCode() * 31 + innerListener.hashCode();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "spec=" + spec +
                '}';
    }
}
