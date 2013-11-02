package citrea.swarm4j.spec;

import java.util.Arrays;
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
public class Spec {

    public static final String RS_Q_TOK_EXT = ("([$])(=(?:\\+=)?)")
            .replaceAll("\\$", SpecQuant.allCodes)
            .replaceAll("=", SpecToken.RS_TOK);

    public static final String BASE64 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~";

    private SpecToken[] tokens;

    public Spec (Spec copy) {
        this.tokens = Arrays.copyOf(copy.tokens, copy.tokens.length);
    }

    private Spec (SpecToken[] tokens) {
        this.tokens = tokens;
    }

    public Spec (SpecToken type, SpecToken id) {
        this.tokens = new SpecToken[] { type, id };
    }

    public Spec (SpecToken type, SpecToken id, SpecToken member) {
        this.tokens = new SpecToken[] { type, id, member };
    }

    public Spec (SpecToken type, SpecToken id, SpecToken member, SpecToken version) {
        this.tokens = new SpecToken[] { type, id, member, version };
    }

    public Spec (String specAsString) {

        if (specAsString == null) {
            this.tokens = new SpecToken[0];
        }

        Pattern pattern = Pattern.compile(RS_Q_TOK_EXT);
        Matcher matcher = pattern.matcher(specAsString);
        int tokensCount = 0;
        int last = -2;
        tokens = new SpecToken[4];
        while (matcher.find()) {
            SpecQuant quant = SpecQuant.byCode(matcher.group(1));
            int order = quant.ordinal();
            String token = matcher.group(2);
            tokens[order] = new SpecToken(token);
            if (last < order) {
                last = order;
            }
            tokensCount++;
        }
        if (tokensCount != last + 1) {
            throw new IllegalArgumentException("Malformed specifier: missing tokens");
        }
        tokens = Arrays.copyOf(tokens, tokensCount);
    }

    public Spec buildParent() {
        int len = this.tokens.length;
        Spec ret;
        if (len >= 1) {
            ret = new Spec(Arrays.copyOf(this.tokens, this.tokens.length - 1));
        } else {
            ret = new Spec((String) null);
        }
        return ret;
    }

    public Spec overrideToken(SpecQuant q, SpecToken id) {
        SpecToken[] newTokens;
        int len = this.tokens.length;
        int order = q.ordinal();
        if (order <= len - 1) { // remove token
            if (id != null) {
                newTokens = Arrays.copyOf(this.tokens, Math.min(order + 1, len));
                newTokens[order] = id;
            } else {
                newTokens = Arrays.copyOf(this.tokens, Math.min(order, len));
            }
        } else if (len == order + 1) { // replace token
            newTokens = Arrays.copyOf(this.tokens, len);
            newTokens[order] = id;
        } else if (len == order) { // add token
            newTokens = Arrays.copyOf(this.tokens, len + 1);
            newTokens[len] = id;
        } else { // incorrect
            throw new IllegalArgumentException("trying to produce malformed specifier");
        }
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
        return getToken(quant.ordinal());
    }

    public SpecToken getLastToken() {
        int len = tokens.length;
        return len > 0 ? tokens[len - 1] : null;
    }

    public SpecToken getType() {
        return getToken(SpecQuant.TYPE);
    }

    public SpecToken getId() {
        return getToken(SpecQuant.ID);
    }

    public SpecToken getMember() {
        return getToken(SpecQuant.MEMBER);
    }

    public SpecToken getVersion() {
        return getToken(SpecQuant.VERSION);
    }

    public String getVersionAsStr() {
        SpecToken version = getVersion();
        return version == null ? null : version.toString();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            SpecToken token = tokens[i];
            res.append(token.withQuant(SpecQuant.byOrder(i)));
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

}
