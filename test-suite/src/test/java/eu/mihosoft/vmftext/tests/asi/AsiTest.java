package eu.mihosoft.vmftext.tests.asi;

import eu.mihosoft.vmftext.tests.asi.parser.AsiModelParser;
import eu.mihosoft.vmftext.tests.asi.unparser.AsiModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * JS-trial regressions: semantic predicates survive the grammar rewrites and a
 * nested rule that matches EOF keeps the hidden text before the end of input.
 */
public class AsiTest {

    private static String roundTrip(String src) {
        return new AsiModelUnparser().unparse(new AsiModelParser().parse(src));
    }

    @Test
    public void semicolonTerminatedStatementsAreExact() {
        Assert.assertEquals("x = 1;\ny = 2;\n", roundTrip("x = 1;\ny = 2;\n"));
    }

    @Test
    public void lineTerminatorEndsAStatement() {
        AsiModel model = new AsiModelParser().parse("x = 1\ny = 2\n");
        Assert.assertEquals(2, model.getRoot().getStatements().size());
        Assert.assertTrue(model.getRoot().getStatements().get(0).getEnd() instanceof EosLine);
        Assert.assertTrue(model.getRoot().getStatements().get(1).getEnd() instanceof EosEof);
    }

    @Test
    public void predicateIsEnforcedNotOptional() {
        try {
            new AsiModelParser().parse("x = 1 y = 2");
            Assert.fail("a statement without ';' or a line terminator must not parse");
        } catch (AsiModelParser.SyntaxErrorException expected) {
            // {lineTerminatorAhead()}? rejected the input
        }
    }

    @Test
    public void nestedEofKeepsTheTrailingHiddenText() {
        Assert.assertEquals("x = 1\ny = 2\n", roundTrip("x = 1\ny = 2\n"));
        Assert.assertEquals("x = 1\n\n", roundTrip("x = 1\n\n"));
        Assert.assertEquals("x = 1  ", roundTrip("x = 1  "));
        Assert.assertEquals("x = 1", roundTrip("x = 1"));
    }

    /**
     * #48: the generated check rules validate the shape of a fragment; a copied
     * predicate such as {@code lineTerminatorAhead()} would be evaluated against
     * a fragment without surroundings and fail.
     */
    @Test
    public void checkRulesCarryNoPredicates() {
        Assert.assertTrue(eu.mihosoft.vmftext.tests.asi.unparser.EosLineUnparser.matchEosLineAlt0(""));
        Assert.assertTrue(eu.mihosoft.vmftext.tests.asi.unparser.StatementUnparser.matchStatementAlt0("x = 1"));
    }

    @Test
    public void programmaticEofRendersNothing() {
        Statement s = Statement.newBuilder().withName("x").withValue("1").withEnd(EosSemi.newInstance()).build();
        AsiModel model = AsiModel.newBuilder().withRoot(Program.newBuilder().withStatements(s).build()).build();
        String out = new AsiModelUnparser().unparse(model);
        Assert.assertEquals(out, "x=1;", out.replaceAll("\\s+", ""));
        Assert.assertFalse(out, Character.isWhitespace(out.charAt(out.length() - 1)));
    }
}
