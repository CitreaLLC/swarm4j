package citrea.swarm4j.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 20.08.2014
 *         Time: 10:47
 */
public class ChainedIterators<E> implements Iterator<E> {

    private Iterator<? extends E>[] iterators;
    private int current = 0;

    public ChainedIterators(Iterator<? extends E>... iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        while (current < iterators.length) {
            if (iterators[current].hasNext()) return true;
            current++;
        }
        return false;
    }

    @Override
    public E next() {
        if (current >= iterators.length) return null;
        return iterators[current].next();
    }

    @Override
    public void remove() {
        if (current >= iterators.length) return;
        iterators[current].remove();
    }
}
