package eu.mihosoft.vmftext.tests.labeledopt;

import eu.mihosoft.vmftext.tests.labeledopt.parser.LabeledOptModelParser;
import eu.mihosoft.vmftext.tests.labeledopt.unparser.LabeledOptModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** #53: optional literals inside a second or later labeled alternative keep their recorded state. */
public class LabeledOptTest {

    private static String roundTrip(String src) {
        return new LabeledOptModelUnparser().unparse(new LabeledOptModelParser().parse(src));
    }

    @Test
    public void optionalLiteralInSecondAlternativeIsKept() {
        Assert.assertEquals("[a,];", roundTrip("[a,];"));
        Assert.assertEquals("[a];", roundTrip("[a];"));
        Assert.assertEquals("[ a , ] ;", roundTrip("[ a , ] ;"));
    }

    @Test
    public void optionalLiteralInThirdAlternativeIsKept() {
        Assert.assertEquals("{a;}", roundTrip("{a;}"));
        Assert.assertEquals("{a}", roundTrip("{a}"));
        Assert.assertEquals("{a=b;}", roundTrip("{a=b;}"));
        Assert.assertEquals("{a=b}", roundTrip("{a=b}"));
    }

    @Test
    public void firstAlternativeAndMixedFilesStayExact() {
        String src = "a;\n[b,];\n{c;}\n[d];\n";
        Assert.assertEquals(src, roundTrip(src));
        LabeledOptModel model = new LabeledOptModelParser().parse(src);
        Assert.assertEquals(4, model.getRoot().getItems().size());
        Assert.assertTrue(model.getRoot().getItems().get(1) instanceof Bracketed);
    }
}
