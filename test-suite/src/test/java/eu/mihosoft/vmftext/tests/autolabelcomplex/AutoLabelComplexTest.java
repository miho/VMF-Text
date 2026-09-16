package eu.mihosoft.vmftext.tests.autolabelcomplex;

import eu.mihosoft.vmftext.tests.autolabelcomplex.parser.AutoLabelComplexModelParser;
import eu.mihosoft.vmftext.tests.autolabelcomplex.unparser.AutoLabelComplexModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that auto-labeling produces a usable, typed VMF API for a complex
 * unlabeled grammar (multiple alternatives, nested repeated blocks, optionals
 * and self references) and that parsed models round-trip.
 */
public class AutoLabelComplexTest {

    private static final String SOURCE = "x = 1 + 2 + 3;\n"
            + "if (x) { y = a * b * c; } else y = 0;\n";

    @Test
    public void unlabeledAlternativesBecomeTypedSubClasses() {
        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(SOURCE);

        Program program = model.getRoot();
        Assert.assertEquals(2, program.getStatements().size());

        // first statement: assignment 'x = 1 + 2 + 3;'
        Statement first = program.getStatements().get(0);
        Assert.assertTrue("assignment alternative -> StatementAlt1",
                first instanceof StatementAlt1);
        Assignment assignment = ((StatementAlt1) first).getAssignment();
        Assert.assertEquals("x", assignment.getIdentifier());

        // second statement: if/else -> StatementAlt2
        Statement second = program.getStatements().get(1);
        Assert.assertTrue("if alternative -> StatementAlt2",
                second instanceof StatementAlt2);
        IfStatement ifStatement = ((StatementAlt2) second).getIfStatement();

        // then-branch is a block statement, else-branch an assignment statement
        Assert.assertTrue("then-branch is a block statement",
                ifStatement.getStatement() instanceof StatementAlt3);
        Assert.assertTrue("else-branch is an assignment statement",
                ifStatement.getElseStatement() instanceof StatementAlt1);
    }

    @Test
    public void repeatedElementsInBlocksBecomeLists() {
        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(SOURCE);

        // '1 + 2 + 3' -> one leading term + two repeated terms in the list
        Assignment assignment = ((StatementAlt1) model.getRoot().getStatements().get(0))
                .getAssignment();
        Expression expression = assignment.getExpression();
        Assert.assertNotNull("leading term is captured", expression.getTerm());
        Assert.assertEquals("repeated terms are collected into a list",
                2, expression.getTerms().size());

        // the leading term '1' resolves to a FactorAlt1 (INT) via factor
        Factor leadingFactor = expression.getTerm().getFactor();
        Assert.assertTrue("INT factor -> FactorAlt1", leadingFactor instanceof FactorAlt1);
        Assert.assertEquals("1", ((FactorAlt1) leadingFactor).getIntValue());

        // 'a * b * c' inside the block -> one leading factor + two in the list
        IfStatement ifStatement = ((StatementAlt2) model.getRoot().getStatements().get(1))
                .getIfStatement();
        Block block = ((StatementAlt3) ifStatement.getStatement()).getBlock();
        Assignment inner = ((StatementAlt1) block.getStatements().get(0)).getAssignment();
        Term term = inner.getExpression().getTerm();
        Assert.assertNotNull("leading factor is captured", term.getFactor());
        Assert.assertEquals("repeated factors are collected into a list",
                2, term.getFactors().size());
    }

    @Test
    public void mixedOperatorsRoundTripInOrder() {
        String source = "x = 1 + 2 - 3;\n";

        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(source);

        Expression expression = ((StatementAlt1) model.getRoot().getStatements().get(0))
                .getAssignment().getExpression();
        Assert.assertEquals("both operators are captured", 2, expression.getOperators().size());
        Assert.assertEquals("+", expression.getOperators().get(0));
        Assert.assertEquals("-", expression.getOperators().get(1));

        String unparsed = new AutoLabelComplexModelUnparser().unparse(model);
        Assert.assertEquals("mixed operators unparse in their original positions",
                source, unparsed);
        Assert.assertEquals(model, parser.parse(unparsed));
    }

    @Test
    public void altTypedRulesNumberElementNamesPerAlternative() {
        // FactorAlt3 ('(' expression ')') and FactorAlt4 ('[' ... ']') are
        // separate types, so each starts its name numbering fresh: the leading
        // expression of FactorAlt4 is 'expression', not 'expression2'.
        String source = "x = [1, 2];\n";

        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(source);

        Factor factor = ((StatementAlt1) model.getRoot().getStatements().get(0))
                .getAssignment().getExpression().getTerm().getFactor();
        Assert.assertTrue("array factor -> FactorAlt4", factor instanceof FactorAlt4);
        FactorAlt4 arrayFactor = (FactorAlt4) factor;

        Assert.assertEquals("1", ((FactorAlt1) arrayFactor.getExpression()
                .getTerm().getFactor()).getIntValue());
        Assert.assertEquals("repeated expressions are collected into a list",
                1, arrayFactor.getExpressions().size());
        Assert.assertEquals("the ',' separator is captured",
                1, arrayFactor.getSymbols().size());

        String unparsed = new AutoLabelComplexModelUnparser().unparse(model);
        Assert.assertEquals(source, unparsed);
        Assert.assertEquals(model, parser.parse(unparsed));
    }

    @Test
    public void multiTokenBlocksAreCapturedElementWise() {
        // ('[' ']')* is not a token set, so it must not receive a single block
        // label (ANTLR rejects that with error 130); instead each literal is
        // captured as its own ordered list property.
        String source = "d[][];\n";

        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(source);

        Statement statement = model.getRoot().getStatements().get(0);
        Assert.assertTrue("dims alternative -> StatementAlt4",
                statement instanceof StatementAlt4);
        Dims dims = ((StatementAlt4) statement).getDims();

        Assert.assertEquals("d", dims.getIdentifier());
        Assert.assertEquals("each '[' is captured in order",
                2, dims.getSymbols().size());
        Assert.assertEquals("each ']' is captured in order",
                2, dims.getSymbols2().size());

        String unparsed = new AutoLabelComplexModelUnparser().unparse(model);
        Assert.assertEquals("repeated bracket pairs round-trip", source, unparsed);
        Assert.assertEquals(model, parser.parse(unparsed));
    }

    @Test
    public void parsedModelRoundTrips() {
        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(SOURCE);

        AutoLabelComplexModelUnparser unparser = new AutoLabelComplexModelUnparser();
        String unparsed = unparser.unparse(model);

        Assert.assertEquals("parsed model unparses to the exact source", SOURCE, unparsed);
        Assert.assertEquals("re-parsing the unparsed source yields an equal model",
                model, parser.parse(unparsed));
    }

    @Test
    public void sourceBundleRoundTrips() {
        AutoLabelComplexModelParser parser = new AutoLabelComplexModelParser();
        AutoLabelComplexModel model = parser.parse(SOURCE);

        AutoLabelComplexSourceBundle bundle = parser.toSourceBundle(model, SOURCE);

        Assert.assertEquals(SOURCE, new AutoLabelComplexModelUnparser()
                .unparse(parser.restoreFromSourceBundle(bundle)));
    }
}
