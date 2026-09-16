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

        Assert.assertEquals(2, model.getRoot().getStatements().size());

        Statement first = model.getRoot().getStatements().get(0);
        Assert.assertEquals("x", first.getIdentifier());
        Assert.assertTrue("Unlabeled alternatives become typed sub classes.",
                first.getValue() instanceof ValueAlt1);
        Assert.assertEquals("1", ((ValueAlt1) first.getValue()).getIntValue());

        Statement second = model.getRoot().getStatements().get(1);
        Assert.assertEquals("y", second.getIdentifier());
        Assert.assertTrue("Unlabeled alternatives become typed sub classes.",
                second.getValue() instanceof ValueAlt2);
        Assert.assertEquals("x", ((ValueAlt2) second.getValue()).getIdentifier());

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
