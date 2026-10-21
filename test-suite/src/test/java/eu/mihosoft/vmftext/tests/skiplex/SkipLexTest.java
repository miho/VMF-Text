package eu.mihosoft.vmftext.tests.skiplex;

import eu.mihosoft.vmftext.tests.skiplex.parser.SkipLexModelParser;
import eu.mihosoft.vmftext.tests.skiplex.unparser.SkipLexModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * #61: trivia comes from character positions, so text the lexer skips
 * ({@code -> skip}) round-trips like hidden-channel text.
 */
public class SkipLexTest {

    private static String roundTrip(String src) {
        return new SkipLexModelUnparser().unparse(new SkipLexModelParser().parse(src));
    }

    @Test
    public void skippedWhitespaceAndCommentsRoundTrip() {
        String[] inputs = {
                "a = 1;\n",
                "a=1;b=2;",
                "  // leading comment\na = 1 + b; // trailing\n\n/* block */ c=(2-a) ;   \n// end",
                "x\t=\t( ( y ) ) ;\r\n",
                "",
                "   ",
                "// only a comment\n",
        };
        for (String src : inputs) {
            Assert.assertEquals(src, roundTrip(src));
        }
    }

    @Test
    public void skippedAndHiddenTextAlternatingInOneGapKeepTheirOrder() {
        String src = "a /* hidden */ = // skipped\n /* hidden again */ 1 ;";
        Assert.assertEquals(src, roundTrip(src));
    }

    @Test
    public void triviaPieceHoldsTheSkippedText() {
        SkipLexModel model = new SkipLexModelParser().parse("a = 1; // one\nb = 2;");
        Stmt b = model.getRoot().getStmts().get(1);
        Assert.assertEquals(" // one\n", b.getLexicalInfo().getTriviaPieces().get(0).getText());
        Assert.assertEquals("COMMENT", b.getLexicalInfo().getTriviaPieces().get(0).getKind());
    }

    @Test
    public void inPlaceValueEditKeepsTheSkippedComments() {
        SkipLexModel model = new SkipLexModelParser().parse("a = 1; // one\nb = 2; // two\n");
        NumTerm two = (NumTerm) model.getRoot().getStmts().get(1).getValue().getTerms().get(0);
        two.setNum("3");
        Assert.assertEquals("a = 1; // one\nb = 3; // two\n", new SkipLexModelUnparser().unparse(model));
    }
}
