package citrea.swarm4j.model.spec;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 20:54
 */
public class TokenIterator implements Iterator<SpecToken> {

    private SpecToken[] tokens;
    private SpecQuant quant;
    private int index = 0;

    public TokenIterator(SpecToken[] tokens, SpecQuant quant) {
        this.tokens = tokens;
        this.quant = quant;
    }

    @Override
    public boolean hasNext() {
        while (index < tokens.length && this.quant != tokens[index].getQuant()) {
            index++;
        }
        return (index < tokens.length);
    }

    @Override
    public SpecToken next() {
        return index < tokens.length ? tokens[index++] : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("tokens removal is not supported");
    }
}
