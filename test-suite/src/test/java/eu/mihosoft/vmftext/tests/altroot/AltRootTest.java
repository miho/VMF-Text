package eu.mihosoft.vmftext.tests.altroot;

import eu.mihosoft.vmftext.tests.altroot.parser.AltRootModelParser;
import eu.mihosoft.vmftext.tests.altroot.unparser.AltRootModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** C-trial finding 4b: a root rule with labeled alternatives must unparse. */
public class AltRootTest {

    @Test
    public void rootWithAlternativesRoundTrips() {
        for(String src : new String[]{"42", "( 1 )", "((  7) )"}) {
            AltRootModel model = new AltRootModelParser().parse(src);
            Assert.assertEquals(src, new AltRootModelUnparser().unparse(model));
        }
    }

    @Test
    public void rootIsTypedSubClass() {
        AltRootModel model = new AltRootModelParser().parse("( 1 )");
        Assert.assertTrue(model.getRoot() instanceof ExprAlt2);
        Assert.assertTrue(((ExprAlt2) model.getRoot()).getExpr() instanceof ExprAlt1);
        Assert.assertTrue(new AltRootModelParser().parse("5").getRoot() instanceof ExprAlt1);
    }
}
