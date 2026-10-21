package eu.mihosoft.vmftext.tests.naming;

import eu.mihosoft.vmftext.tests.naming.parser.NamingModelParser;
import eu.mihosoft.vmftext.tests.naming.unparser.NamingModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

/** #47: keyword tokens, left/right operands, camelCase for snake_case rule names. */
public class NamingTest {

    private static String roundTrip(String src) {
        return new NamingModelUnparser().unparse(new NamingModelParser().parse(src));
    }

    private static boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods()).map(Method::getName).anyMatch(name::equals);
    }

    @Test
    public void keywordTokensAreNotProperties() {
        Assert.assertFalse("Var is a keyword, not a value", hasMethod(SimpleStmt.class, "getVar"));
        Assert.assertFalse(hasMethod(Cond.class, "getIfValue"));
        Assert.assertFalse(hasMethod(Cond.class, "getElseValue"));
        Assert.assertTrue(hasMethod(SimpleStmt.class, "getId"));
    }

    @Test
    public void binaryOperandsAreLeftAndRight() {
        NamingModel model = new NamingModelParser().parse("var x = a + 1\n");
        SimpleStmt stmt = model.getRoot().getSimpleStmts().get(0);
        SumAlt1 sum = (SumAlt1) ((SimpleStmtChoiceAlt1) stmt.getSimpleStmtChoice()).getSum();
        Assert.assertTrue(sum.getLeft() instanceof TermAlt1);
        Assert.assertTrue(sum.getRight() instanceof TermAlt2);
    }

    @Test
    public void keywordContextNamesTheSecondOperand() {
        NamingModel model = new NamingModelParser().parse("var y = if a else b\n");
        Cond cond = ((SimpleStmtChoiceAlt2) model.getRoot().getSimpleStmts().get(0).getSimpleStmtChoice()).getCond();
        Assert.assertNotNull(cond.getIfSum());
        Assert.assertNotNull(cond.getElseSum());
    }

    /** #47: camelCase properties; #63: the rule's own class is camelCase too (file_input → FileInput). */
    @Test
    public void snakeCaseRulesGiveCamelCaseClassesAndProperties() {
        Assert.assertTrue(hasMethod(FileInput.class, "getSimpleStmts"));
        Assert.assertTrue(hasMethod(SimpleStmt.class, "getSimpleStmtChoice"));
        Assert.assertEquals("FileInput", new NamingModelParser().parse("var z = 1\n").getRoot().vmf().reflect().type().getName()
                .replaceAll(".*\\.", ""));
        Assert.assertEquals("fileInput", eu.mihosoft.vmftext.tests.naming.parser.NamingParser.ruleNames[0]);
    }

    @Test
    public void roundTripsAreExact() {
        for (String src : new String[]{"var x = a + 1\n", "var y = if a else b\n", "var z = 1\n", "var a=b+c\nvar d = 2\n"}) {
            Assert.assertEquals(src, roundTrip(src));
        }
    }
}
