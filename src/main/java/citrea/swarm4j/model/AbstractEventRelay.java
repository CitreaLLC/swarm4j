package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
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
    private List<EventRecipient> listeners = new ArrayList<EventRecipient>();

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

    public void deliver(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {

        checkACL(spec, value, source);

        validate(spec, value, source);

        CHILD child = getChild(spec);
        if (child == null) {
            child = createNewChild(spec, value);
            if (child != null) {
                addChild(child.getId(), child);
            }
        }

        if (child != null) {
            child.deliver(action, spec, value, source);
            return;
        }

        if (!(this instanceof EventRecipient)) { // can't accept set/on/off
            throw new SwarmNoChildException(spec);
        }

        EventRecipient me = (EventRecipient) this;

        switch (action) {
            case on:
            case reOn:
                me.on(action, spec, value, source);
                break;

            case off:
            case reOff:
                me.off(action, spec, source);
                break;

            case set:
            default:
                me.set(spec, value, source);
        }
    }

    protected void addListener(EventRecipient listener) {
        this.listeners.add(listener);
    }

    public void removeListener(EventRecipient listener) {
        this.listeners.remove(listener);
    }

    protected void checkACL(Spec spec, JSONString value, EventRecipient listener) throws SwarmSecurityException {
        //do nothing by default
    }

    protected void validate(Spec spec, JSONValue value, EventRecipient source) throws SwarmValidationException {
        //do nothing by default
    }

    public void emit(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {
        for (EventRecipient l : this.listeners) {
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

    protected static class OnceRecipientWrapper implements EventRecipient {
        private final EventRecipient self;
        private final Spec initialSpec;
        private final EventRecipient innerListener;

        public OnceRecipientWrapper(EventRecipient self, Spec spec, EventRecipient listener) throws SwarmException {
            this.self = self;
            this.initialSpec = spec;
            this.innerListener = listener;
        }

        @Override
        public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
            throw new SwarmUnsupportedActionException(action);
        }

        @Override
        public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
            throw new SwarmUnsupportedActionException(action);
        }

        @Override
        public void set(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {
            innerListener.set(spec, value, listener);
            self.off(Action.off, initialSpec, innerListener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            final EventRecipient oListener;
            if (o instanceof OnceRecipientWrapper) {
                oListener = ((OnceRecipientWrapper) o).innerListener;
            } else if (o instanceof EventRecipient) {
                oListener = ((EventRecipient) o);
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
