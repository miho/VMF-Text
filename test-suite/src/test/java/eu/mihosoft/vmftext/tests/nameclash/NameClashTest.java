package eu.mihosoft.vmftext.tests.nameclash;

import eu.mihosoft.vmftext.tests.nameclash.parser.NameClashModelParser;
import eu.mihosoft.vmftext.tests.nameclash.parser.NameClashTriviaSupport;
import eu.mihosoft.vmftext.tests.nameclash.unparser.NameClashModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * #73: when a parser rule and a token share a name, the auto-labeled properties say
 * what they hold — {@code labelNode} for the rule, {@code labelToken} for the token —
 * and that is the API code works with: getters, setters, builders and reflection.
 */
public class NameClashTest {

    private static boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods()).map(Method::getName).anyMatch(name::equals);
    }

    @Test
    public void ruleReferenceIsNodeTokenReferenceIsToken() throws Exception {
        Assert.assertEquals(Label.class, StmtAlt1.class.getMethod("getLabelNode").getReturnType());
        Assert.assertEquals(String.class, LabelAlt1.class.getMethod("getLabelToken").getReturnType());
        Assert.assertEquals(String.class, LabelAlt2.class.getMethod("getLabelToken").getReturnType());
        // a keyword in front still gives the keyword-context name
        Assert.assertEquals(Label.class, StmtAlt2.class.getMethod("getGotoLabel").getReturnType());
        // no numbered fallback names
        Assert.assertFalse(hasMethod(StmtAlt1.class, "getLabel2"));
        Assert.assertFalse(hasMethod(LabelAlt1.class, "getLabel2"));
    }

    @Test
    public void reflectionTellsWhichPropertyHoldsTheToken() {
        LabelAlt1 label = LabelAlt1.newBuilder().withLabelToken("x").build();
        eu.mihosoft.vmf.runtime.core.Property p = label.vmf().reflect().propertyByName("labelToken").get();
        Assert.assertEquals("Label", NameClashTriviaSupport.tokenOf(p));
        Assert.assertEquals("x", p.get());
    }

    @Test
    public void parsedTextRoundTripsAndReadsWithTheNames() {
        String src = "a: 1; goto b; $c: 2;\n";
        NameClashModel model = new NameClashModelParser().parse(src);
        Assert.assertEquals(src, new NameClashModelUnparser().unparse(model));
        StmtAlt1 first = (StmtAlt1) model.getRoot().getStmts().get(0);
        Assert.assertEquals("a", ((LabelAlt1) first.getLabelNode()).getLabelToken());
        StmtAlt2 second = (StmtAlt2) model.getRoot().getStmts().get(1);
        Assert.assertEquals("b", ((LabelAlt1) second.getGotoLabel()).getLabelToken());
        ((LabelAlt1) second.getGotoLabel()).setLabelToken("again");
        Assert.assertEquals("a: 1; goto again; $c: 2;\n", new NameClashModelUnparser().unparse(model));
    }

    @Test
    public void modelBuiltWithTheNamesUnparsesAndReparses() {
        StmtAlt1 mark = StmtAlt1.newBuilder()
                .withLabelNode(LabelAlt2.newBuilder().withLabelToken("top").build())
                .withNum("7")
                .build();
        StmtAlt2 jump = StmtAlt2.newBuilder()
                .withGotoLabel(LabelAlt1.newBuilder().withLabelToken("top").build())
                .build();
        NameClashModel model = NameClashModel.newBuilder()
                .withRoot(Unit.newBuilder().withStmts(mark, jump).build()).build();
        String out = new NameClashModelUnparser().unparse(model);
        Assert.assertEquals(out, "$top:7;gototop;", out.replaceAll("\\s+", ""));
        NameClashModel back = new NameClashModelParser().parse(out);
        StmtAlt1 mark2 = (StmtAlt1) back.getRoot().getStmts().get(0);
        Assert.assertEquals("top", ((LabelAlt2) mark2.getLabelNode()).getLabelToken());
        Assert.assertEquals("7", mark2.getNum());
        Assert.assertEquals("top", ((LabelAlt1) ((StmtAlt2) back.getRoot().getStmts().get(1)).getGotoLabel()).getLabelToken());
    }
}
