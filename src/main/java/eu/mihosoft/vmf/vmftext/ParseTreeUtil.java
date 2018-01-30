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

        if(e.getText().contains("~")) {
            System.out.println("e: " + e.getText());
            System.out.println(" -> e.atom: " + e.atom());
            if(e.atom()!=null) {
                System.out.println("   -> text: " + e.atom().getText());
            }
            System.out.println(" -> e.lbe : " + e.labeledElement());
            if(e.labeledElement()!=null) {
                System.out.println("   -> text: " + e.labeledElement().getText());

                // System.out.println("   -> text: " +  e.labeledElement().atom().getText());

            }
        }

        if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
            String atomText = e.labeledElement().atom().getText();

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

            if(atomText.startsWith("~'")) return true;
        }


        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().terminal() != null &&
                e.labeledElement().atom().terminal().TOKEN_REF() == null;
    }

    public static String getElementText(ANTLRv4Parser.ElementContext e) {
        if(isParserRule(e)) {
            return e.labeledElement().atom().ruleref().getText();
        } else if(isLexerRule(e)) {
            return e.labeledElement().atom().terminal().TOKEN_REF().getText();
        } else if(isStringLiteral(e)) {
            return e.labeledElement().atom().terminal().STRING_LITERAL().getText();
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

    public static CodeLocation tokenToCodeLocationStop(Token t) {
        return CodeLocation.newBuilder().
                withIndex(t.getStopIndex()).
                withCharPosInLine(t.getCharPositionInLine()).withLine(t.getLine()).build();
    }

    public static CodeRange ctxToCodeRange(ParserRuleContext ctx) {
        return CodeRange.newBuilder().withStart(tokenToCodeLocationStart(ctx.start)).
                withStop(tokenToCodeLocationStop(ctx.stop)).build();
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
