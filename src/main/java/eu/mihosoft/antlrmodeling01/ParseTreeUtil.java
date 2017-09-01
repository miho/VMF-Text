package eu.mihosoft.antlrmodeling01;

public class ParseTreeUtil {

    private ParseTreeUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static boolean isParserRule(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement().atom().ruleref() != null;
    }

    public static boolean isLexerRule(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement().atom().terminal() != null &&
                e.labeledElement().atom().terminal().TOKEN_REF() != null;
    }

    public static boolean isStringLiteral(ANTLRv4Parser.ElementContext e) {
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
}
