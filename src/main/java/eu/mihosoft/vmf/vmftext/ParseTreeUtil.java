package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.CodeLocation;
import eu.mihosoft.vmf.vmftext.grammar.CodeRange;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/**
 * Utility class for ANTLR4 parse trees. The utility methods provided by this class are
 * mostly useful for determining the type of specified element contexts.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class ParseTreeUtil {

    private ParseTreeUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Indicates whether the specified element context is a rule block.
     * @param e the element context to check
     * @return {@code true} if the specified context is a rule block; {@code false} otherwise
     */
    public static boolean isRuleBlock(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement().atom()== null && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is a parser rule.
     * @param e the element context to check
     * @return {@code true} if the specified celement ontext is a parser rule; {@code false} otherwise
     */
    public static boolean isParserRule(ANTLRv4Parser.ElementContext e) {

        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().ruleref() != null;
    }

    /**
     * Indicates whether the specified element context is a lexer rule.
     * @param e the element context to check
     * @return {@code true} if the specified element context is a lexer rule; {@code false} otherwise
     */
    public static boolean isLexerRule(ANTLRv4Parser.ElementContext e) {

        if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
            String atomText = e.labeledElement().atom().getText();

            // detect lexer rules with not operator
            if(atomText.length()> 1
                    && atomText.startsWith("~")
                    && Character.isUpperCase(atomText.codePointAt(1)))
                return true;
        }



        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().terminal() != null &&
                e.labeledElement().atom().terminal().TOKEN_REF() != null;
    }

    /**
     * Indicates whether the specified element context is a string literal.
     * @param e the element context to check
     * @return {@code true} if the specified element context is a string literal; {@code false} otherwise
     */
    public static boolean isStringLiteral(ANTLRv4Parser.ElementContext e) {

        if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
            String atomText = e.labeledElement().atom().getText();

            // detect literals with not operator
            if(atomText.startsWith("~'")) return true;
        }


        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().terminal() != null &&
                e.labeledElement().atom().terminal().TOKEN_REF() == null;
    }

    /**
     * Indicates whether the specified element context is negated, i.e. {@code ~[ABC]}
     * @param e the element context to check
     * @return {@code true} if the specified element context is negated; {@code false} otherwise
     */
    public static boolean isNegated(ANTLRv4Parser.ElementContext e) {

        if(isParserRule(e)) {
            // parser rules can't be negated
            return false;
        } else if(e.atom()!=null) {
            return e.atom().notSet()!=null;
        } else if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
            return e.labeledElement().atom().notSet()!=null;
        }

        return false;
    }

    /**
     * Returns the text of the specified element context.
     * @param e element context
     * @return the text of the specified element context
     */
    public static String getElementText(ANTLRv4Parser.ElementContext e) {
        if(isParserRule(e)) {
            return e.labeledElement().atom().ruleref().getText();
        } else if(isLexerRule(e)) {

            if(e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            }
            /*else {
                // default case
                return e.labeledElement().atom().terminal().TOKEN_REF().getText();
            }*/
        } else if(isStringLiteral(e)) {

            if(e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            }
            /*else {
                // default case
                return e.labeledElement().atom().terminal().STRING_LITERAL().getText();
            }*/
        }

        return null;
    }

    /**
     * Indicates whether the specified element context is labelled.
     * @param e the element context to check
     * @return {@code true} if the specified element context is labeled; {@code false} otherwise
     */
    public static boolean isLabeledElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement() != null && e.labeledElement().identifier() != null;
    }

    /**
     * Returns the start location of the specified token.
     * @param t token
     * @return the start location of the specified token
     */
    public static CodeLocation tokenToCodeLocationStart(Token t) {
        return CodeLocation.newBuilder().
                withIndex(t.getStartIndex()).
                withCharPosInLine(t.getCharPositionInLine()).withLine(t.getLine()).build();
    }

    /**
     * Returns the stop/end location of the specified token.
     * @param t token
     * @param ctx parser rule context the token belongs to
     * @return the stop/end location of the specified token
     */
    public static CodeLocation tokenToCodeLocationStop(Token t, ParserRuleContext ctx) {
        int startIndex = t.getStartIndex();
        int stopIndex = t.getStopIndex();
        int diff = stopIndex - startIndex;

        return CodeLocation.newBuilder().
                withIndex(stopIndex).
                withCharPosInLine(t.getCharPositionInLine()+diff).
                withLine(/*TODO 14.02.2018 does not work for multi-line-tokens*/t.getLine()).build();
    }

    /**
     * Returns the code range of the specified context.
     * @param ctx the context to check
     * @return the code range of the specified context
     */
    public static CodeRange ctxToCodeRange(ParserRuleContext ctx) {
        // consider for stop line: https://stackoverflow.com/a/17487805
        CodeLocation start = tokenToCodeLocationStart(ctx.start);
        CodeLocation stop = tokenToCodeLocationStart(ctx.stop);
        return CodeRange.newBuilder().withStart(start).
                withStop(tokenToCodeLocationStop(ctx.stop,ctx))
                .withLength(stop.getIndex()-start.getIndex()+1/*+1 because of inclusive vs. exclusive*/).build();
    }

    /**
     * Indicates whether the specified element context is a labeled block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is a labeled block element; {@code false} otherwise
     */
    public static boolean isLabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return isLabeledElement(e) && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is a block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is a block element; {@code false} otherwise
     */
    public static boolean isBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is an unlabeled block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is an unlabeled block element; {@code false} otherwise
     */
    public static boolean isUnlabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && !isLabeledElement(e) && e.labeledElement().block()!=null;
    }
}
