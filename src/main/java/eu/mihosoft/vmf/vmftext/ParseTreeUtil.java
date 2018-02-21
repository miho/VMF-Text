package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.CodeLocation;
import eu.mihosoft.vmf.vmftext.grammar.CodeRange;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class ParseTreeUtil {

    private ParseTreeUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static boolean isRuleBlock(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement().atom()== null && e.labeledElement().block()!=null;
    }

    public static boolean isParserRule(ANTLRv4Parser.ElementContext e) {

        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().ruleref() != null;
    }

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

    public static String getElementText(ANTLRv4Parser.ElementContext e) {
        if(isParserRule(e)) {
            return e.labeledElement().atom().ruleref().getText();
        } else if(isLexerRule(e)) {

            // if we use not operator we need to access text differently
            if(e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            } else {
                // default case
                return e.labeledElement().atom().terminal().TOKEN_REF().getText();
            }
        } else if(isStringLiteral(e)) {
            // if we use not operator we need to access text differently
            if(e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            } else {
                // default case
                return e.labeledElement().atom().terminal().STRING_LITERAL().getText();
            }
        }

        return null;
    }

    public static boolean isLabeledElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement() != null && e.labeledElement().identifier() != null;
    }

    public static CodeLocation tokenToCodeLocationStart(Token t) {
        return CodeLocation.newBuilder().
                withIndex(t.getStartIndex()).
                withCharPosInLine(t.getCharPositionInLine()).withLine(t.getLine()).build();
    }

    public static CodeLocation tokenToCodeLocationStop(Token t, ParserRuleContext ctx) {
        int startIndex = t.getStartIndex();
        int stopIndex = t.getStopIndex();
        int diff = stopIndex - startIndex;

        return CodeLocation.newBuilder().
                withIndex(stopIndex).
                withCharPosInLine(t.getCharPositionInLine()+diff).
                withLine(/*TODO 14.02.2018 does not work for multi-line-tokens*/t.getLine()).build();
    }

    public static CodeRange ctxToCodeRange(ParserRuleContext ctx) {
        // consider for stop line: https://stackoverflow.com/a/17487805
        CodeLocation start = tokenToCodeLocationStart(ctx.start);
        CodeLocation stop = tokenToCodeLocationStart(ctx.stop);
        return CodeRange.newBuilder().withStart(start).
                withStop(tokenToCodeLocationStop(ctx.stop,ctx))
                .withLength(stop.getIndex()-start.getIndex()+1/*+1 because of inclusive vs. exclusive*/).build();
    }

    public static boolean isLabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return isLabeledElement(e) && e.labeledElement().block()!=null;
    }

    public static boolean isBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && e.labeledElement().block()!=null;
    }

    public static boolean isUnlabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && !isLabeledElement(e) && e.labeledElement().block()!=null;
    }
}
