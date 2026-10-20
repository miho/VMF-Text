package py3trial.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

/** grammars-v4 python/python3/Java/Python3ParserBase.java: both predicates are constant. */
public abstract class Python3ParserBase extends Parser {

    protected Python3ParserBase(TokenStream input) {
        super(input);
    }

    public boolean CannotBePlusMinus() {
        return true;
    }

    public boolean CannotBeDotLpEq() {
        return true;
    }
}
