package eu.mihosoft.vmftext.tests.listgrow;

import eu.mihosoft.vmftext.tests.listgrow.parser.ListGrowModelParser;
import eu.mihosoft.vmftext.tests.listgrow.unparser.ListGrowModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Growing a comma-separated list whose shape the splice does not recognize
 * must still emit every separator and never glue tokens; the result must
 * re-parse strictly.
 */
public class ListGrowTest {

    private static String unparse(ListGrowModel model) {
        return new ListGrowModelUnparser().unparse(model);
    }

    private static String squeeze(String s) {
        return s.replaceAll("\\s+", "");
    }

    @Test
    public void appendToOneElementList() {
        ListGrowModel model = new ListGrowModelParser().parse("(int a)");
        model.getRoot().getParams().add(Param.newBuilder().withType("int").withName("b").build());
        String out = unparse(model);
        Assert.assertEquals(out, "(inta,intb)", squeeze(out));
        Assert.assertEquals(2, new ListGrowModelParser().parse(out).getRoot().getParams().size());
    }

    @Test
    public void appendToTwoElementList() {
        ListGrowModel model = new ListGrowModelParser().parse("( int a , int b )");
        model.getRoot().getParams().add(Param.newBuilder().withType("int").withName("c").build());
        String out = unparse(model);
        Assert.assertEquals(out, "(inta,intb,intc)", squeeze(out));
        Assert.assertEquals(3, new ListGrowModelParser().parse(out).getRoot().getParams().size());
    }

    @Test
    public void appendToEmptyList() {
        ListGrowModel model = new ListGrowModelParser().parse("()");
        model.getRoot().getParams().add(Param.newBuilder().withType("int").withName("a").build());
        String out = unparse(model);
        Assert.assertEquals(out, "(inta)", squeeze(out));
        Assert.assertEquals(1, new ListGrowModelParser().parse(out).getRoot().getParams().size());
    }

    @Test
    public void keywordsNeverGlueToIdentifiers() {
        ListGrowModel model = new ListGrowModelParser().parse("(int a)");
        model.getRoot().getParams().add(Param.newBuilder().withType("int").withName("b").build());
        String out = unparse(model);
        Assert.assertFalse(out, out.contains("aint"));
        Assert.assertFalse(out, out.contains("bint"));
    }

    @Test
    public void unchangedListStaysExact() {
        String src = "( int a ,int b, int c  )";
        Assert.assertEquals(src, unparse(new ListGrowModelParser().parse(src)));
    }
}
