package eu.mihosoft.vmftext.tests.splitlex.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

/** #39: a lexer base class as grammars-v4 lexers use (superClass option of the lexer grammar). */
public abstract class SplitLexLexerBase extends Lexer {

    private int tokensEmitted;

    protected SplitLexLexerBase(CharStream input) {
        super(input);
    }

    @Override
    public org.antlr.v4.runtime.Token nextToken() {
        tokensEmitted++;
        return super.nextToken();
    }

    public int getTokensEmitted() {
        return tokensEmitted;
    }
}
