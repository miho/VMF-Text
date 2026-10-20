package eu.mihosoft.vmftext.tests.rootrule;

import eu.mihosoft.vmftext.tests.rootrule.parser.RootRuleModelParser;
import eu.mihosoft.vmftext.tests.rootrule.unparser.RootRuleModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** C-trial finding 4: the model root can be declared with Root(rule=...). */
public class RootRuleTest {

    @Test
    public void declaredRootRuleWins() {
        RootRuleModel model = new RootRuleModelParser().parse("a, b");
        ItemList root = model.getRoot(); // compiles only if itemList is the root type
        Assert.assertEquals(2, root.getItems().size());
        Assert.assertEquals("b", root.getItems().get(1).getName());
    }

    @Test
    public void roundTripIsExact() {
        String src = "a ,  b,c";
        RootRuleModel model = new RootRuleModelParser().parse(src);
        Assert.assertEquals(src, new RootRuleModelUnparser().unparse(model));
    }
}
