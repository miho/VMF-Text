package eu.mihosoft.vmftext.tests.autolabelpartial;

import eu.mihosoft.vmftext.tests.autolabelpartial.parser.AutoLabelPartialModelParser;
import eu.mihosoft.vmftext.tests.autolabelpartial.unparser.AutoLabelPartialModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that auto-labeling mixes consistently with partial manual labeling:
 * manual labels are kept, partially labeled alternatives are completed so the
 * grammar stays valid, and generated element names never collide with manual
 * ones.
 */
public class AutoLabelPartialTest {

    @Test
    public void manualAlternativeLabelIsKept() {
        AutoLabelPartialModelParser parser = new AutoLabelPartialModelParser();
        Element named = parser.parseElement("foo");
        Assert.assertTrue("manual '# NamedElement' label is preserved",
                named instanceof NamedElement);
        Assert.assertEquals("foo", ((NamedElement) named).getIdentifier());
    }

    @Test
    public void missingAlternativeLabelIsCompleted() {
        AutoLabelPartialModelParser parser = new AutoLabelPartialModelParser();
        Element intElement = parser.parseElement("42");
        Assert.assertTrue("the unlabeled alternative is labeled automatically",
                intElement instanceof ElementAlt2);
        Assert.assertEquals("42", ((ElementAlt2) intElement).getIntValue());
    }

    @Test
    public void generatedNameDoesNotCollideWithManualLabel() {
        AutoLabelPartialModelParser parser = new AutoLabelPartialModelParser();
        Pair pair = parser.parsePair("(7 : bar)");

        // manual label 'identifier' wins for the INT
        Assert.assertEquals("7", pair.getIdentifier());
        // auto label for the unlabeled IDENTIFIER is disambiguated
        Assert.assertEquals("bar", pair.getIdentifier2());
    }

    @Test
    public void mixedLabelingRoundTrips() {
        String source = "foo 42 (7 : bar)";

        AutoLabelPartialModelParser parser = new AutoLabelPartialModelParser();
        AutoLabelPartialModel model = parser.parse(source);

        Assert.assertEquals(3, model.getRoot().getEntryNodes().size());

        AutoLabelPartialModelUnparser unparser = new AutoLabelPartialModelUnparser();
        String unparsed = unparser.unparse(model);

        Assert.assertEquals(source, unparsed);
        Assert.assertEquals(model, parser.parse(unparsed));
    }
}
