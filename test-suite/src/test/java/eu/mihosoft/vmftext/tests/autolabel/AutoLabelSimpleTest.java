package eu.mihosoft.vmftext.tests.autolabel;

import eu.mihosoft.vmftext.tests.autolabel.parser.AutoLabelSimpleModelParser;
import eu.mihosoft.vmftext.tests.autolabel.unparser.AutoLabelSimpleModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class AutoLabelSimpleTest {

    @Test
    public void grammarMetadataEnablesAutoLabelsForUnlabeledGrammar() {
        String source = "x = 1;\ny = x;";

        AutoLabelSimpleModelParser parser = new AutoLabelSimpleModelParser();
        AutoLabelSimpleModel model = parser.parse(source);

        Assert.assertEquals(2, model.getRoot().getStatementNodes().size());

        Statement first = model.getRoot().getStatementNodes().get(0);
        Assert.assertEquals("x", first.getIdentifier());
        Assert.assertTrue("Unlabeled alternatives become typed sub classes.",
                first.getValueNode() instanceof ValueAlt1);
        Assert.assertEquals("1", ((ValueAlt1) first.getValueNode()).getIntValue());

        Statement second = model.getRoot().getStatementNodes().get(1);
        Assert.assertEquals("y", second.getIdentifier());
        Assert.assertTrue("Unlabeled alternatives become typed sub classes.",
                second.getValueNode() instanceof ValueAlt2);
        Assert.assertEquals("x", ((ValueAlt2) second.getValueNode()).getIdentifier());

        String unparsed = new AutoLabelSimpleModelUnparser().unparse(model);
        Assert.assertEquals(source, unparsed);
        Assert.assertEquals(model, parser.parse(unparsed));
    }

    @Test
    public void sourceBundleWorksForAutoLabeledGrammar() {
        String source = "answer = 42;";

        AutoLabelSimpleModelParser parser = new AutoLabelSimpleModelParser();
        AutoLabelSimpleModel model = parser.parse(source);
        AutoLabelSimpleSourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, new AutoLabelSimpleModelUnparser().unparse(parser.restoreFromSourceBundle(bundle)));
    }
}
