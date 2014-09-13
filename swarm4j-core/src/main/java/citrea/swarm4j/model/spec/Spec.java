package citrea.swarm4j.model.spec;

import citrea.swarm4j.model.value.JSONValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specifier "/type#id.member!version"
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 15:02
 */
public class Spec implements Comparable<Spec> {

    public static final String RS_Q_TOK_EXT = ("([$])(=(?:\\+=)?)")
            .replaceAll("\\$", SpecQuant.allCodes)
            .replaceAll("=", SpecToken.RS_TOK);
    public static final Pattern RE_Q_TOK_EXT = Pattern.compile(RS_Q_TOK_EXT);

    public static final String BASE64 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~";
    public static final String RS_BASE64 = "[0-9A-Za-z_~]";
    public static final Pattern RE_BASE64 = Pattern.compile(RS_BASE64, Pattern.MULTILINE);

    private SpecToken[] tokens;

    public Spec(SpecToken... tokens) {
        int j = 0;
        for (int i = 0, l = tokens.length; i < l; i++) {
            if (tokens[i] != null) {
                if (j != i) {
                    tokens[j] = tokens[i];
                }
                j++;
            }
        }
        if (j < tokens.length) {
            tokens = Arrays.copyOf(tokens, j);
        }
        this.tokens = tokens;
    }

    public Spec(Spec copy) {
        this(Arrays.copyOf(copy.tokens, copy.tokens.length));
    }

    public Spec(String specAsString) {
        this(Spec.parse(specAsString));
    }

    public Spec addToken(SpecToken token) {
        SpecToken[] newTokens = Arrays.copyOf(tokens, tokens.length + 1);
        newTokens[tokens.length] = token;
        return new Spec(newTokens);
    }

    public Spec addToken(String token) {
        SpecToken[] tokensToAdd = new Spec(token).tokens;
        SpecToken[] newTokens = new SpecToken[tokens.length + tokensToAdd.length];
        System.arraycopy(tokens, 0, newTokens, 0, tokens.length);
        System.arraycopy(tokensToAdd, 0, newTokens, tokens.length, tokensToAdd.length);
        return new Spec(newTokens);
    }

    public Spec overrideToken(SpecToken overrideWith) {
        final SpecQuant q = overrideWith.getQuant();
        SpecToken[] newTokens = Arrays.copyOf(tokens, tokens.length);
        boolean found = false;
        for (int i = 0, l = newTokens.length; i < l; i++) {
            SpecToken token = newTokens[i];
            if (token.getQuant() == q) {
                newTokens[i] = overrideWith;
                found = true;
                break;
            }
        }
        if (!found) {
            newTokens = Arrays.copyOf(newTokens, newTokens.length + 1);
            newTokens[newTokens.length - 1] = overrideWith;
        }
        return new Spec(newTokens);
    }

    public Spec overrideToken(String token) {
        return overrideToken(new SpecToken(token));
    }

    public Spec sort() {
        SpecToken[] newTokens = Arrays.copyOf(tokens, tokens.length);
        Arrays.sort(newTokens, SpecToken.ORDER_BY_QUANT);
        return new Spec(newTokens);
    }

    public boolean isEmpty() {
        return tokens.length == 0;
    }

    public int getTokensCount() {
        return tokens.length;
    }

    public SpecToken getToken(int idx) {
        return (tokens.length >= idx + 1) ? tokens[idx] : null;
    }

    public SpecToken getToken(SpecQuant quant) {
        for (SpecToken token : tokens) {
            if (token.getQuant() == quant) {
                return token;
            }
        }
        return null;
    }

    public Iterator<SpecToken> getTokenIterator(SpecQuant quant) {
        return new TokenIterator(tokens, quant);
    }

    public Spec getTypeId() {
        return new Spec(new SpecToken[] { getType(), getId() });
    }

    public Spec getVersionOp() {
        return new Spec(new SpecToken[] { getVersion(), getOp() });
    }

    public SpecToken getType() {
        return getToken(SpecQuant.TYPE);
    }

    public SpecToken getId() {
        return getToken(SpecQuant.ID);
    }

    public SpecToken getVersion() {
        return getToken(SpecQuant.VERSION);
    }

    public SpecToken getOp() {
        return getToken(SpecQuant.OP);
    }


    public SpecPattern getPattern() {
        switch (tokens.length) {
            case 4:
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].getQuant() != SpecQuant.byOrder(i)) {
                        return SpecPattern.UNKNOWN;
                    }
                }
                return SpecPattern.FULL;
            case 2:
                if (tokens[0].getQuant() == SpecQuant.TYPE &&
                        tokens[1].getQuant() == SpecQuant.ID) {
                    return SpecPattern.TYPE_ID;
                } else if (tokens[0].getQuant() == SpecQuant.VERSION &&
                        tokens[1].getQuant() == SpecQuant.OP) {
                    return SpecPattern.VERSION_OP;
                } else {
                    return SpecPattern.UNKNOWN;
                }
            default:
                return SpecPattern.UNKNOWN;
        }
    }

    public boolean fits(Spec specFilter) {
        for (SpecToken tok : specFilter.tokens) {
            boolean found = false;
            for (SpecToken token : this.tokens) {
                if (tok.equals(token)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (SpecToken token : tokens) {
            res.append(token.toString());
        }
        return res.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof String) {
            return o.equals(this.toString());
        }
        if (getClass() != o.getClass()) return false;

        Spec spec = (Spec) o;

        return Arrays.equals(this.tokens, spec.tokens);
    }

    @Override
    public int hashCode() {
        int len = this.tokens.length;
        int result = 0;
        for (SpecQuant q : SpecQuant.values()) {
            int idx = q.ordinal();
            result *= 31;
            if (idx < len) {
                result += this.tokens[idx].hashCode();
            }
        }
        return result;
    }

    @Override
    public int compareTo(Spec spec) {
        return spec == null ? 1 : this.toString().compareTo(spec.toString());
    }

    public static SpecToken[] parse(String specAsString) {
        if (specAsString == null) {
            return new SpecToken[0];
        }

        Matcher matcher = RE_Q_TOK_EXT.matcher(specAsString);
        List<SpecToken> tokensList = new ArrayList<SpecToken>(4);
        while (matcher.find()) {
            tokensList.add(new SpecToken(matcher.group(0)));
        }

        return tokensList.toArray(new SpecToken[tokensList.size()]);
    }

    public static Comparator<Spec> ORDER_NATURAL = new Comparator<Spec>() {

        @Override
        public int compare(Spec left, Spec right) {
            if (left == null) {
                return right == null ? 0 : -1;
            } else {
                if (right == null) {
                    return 1;
                }
                return left.compareTo(right);
            }
        }
    };
}
