package eu.mihosoft.vmftext.tests.arraylang;

import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import org.junit.Assert;
import org.junit.Test;

/** C-trial finding 3: parse() must not silently return an error-recovered model. */
public class SyntaxErrorTest {

    @Test(expected = ArrayLangModelParser.SyntaxErrorException.class)
    public void parseFailsOnSyntaxErrorsByDefault() {
        new ArrayLangModelParser().parse("(1.0, 2.0");
    }

    @Test
    public void exceptionReportsEveryError() {
        try {
            new ArrayLangModelParser().parse("(1.0, 2.0");
            Assert.fail("expected SyntaxErrorException");
        } catch(ArrayLangModelParser.SyntaxErrorException ex) {
            Assert.assertFalse(ex.getErrors().isEmpty());
            Assert.assertTrue(ex.getErrors().get(0), ex.getErrors().get(0).startsWith("line 1:"));
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("syntax error"));
        }
    }

    @Test
    public void optOutReturnsTheRecoveredModel() {
        ArrayLangModelParser parser = new ArrayLangModelParser();
        parser.setFailOnSyntaxError(false);
        ArrayLangModel model = parser.parse("(1.0, 2.0");
        Assert.assertNotNull(model.getRoot());
        Assert.assertEquals(2, model.getRoot().getValues().size());
    }

    @Test
    public void validInputStillParses() {
        Assert.assertEquals(3, new ArrayLangModelParser().parse("(1.0, 2.0, 3.0)").getRoot().getValues().size());
    }
}
