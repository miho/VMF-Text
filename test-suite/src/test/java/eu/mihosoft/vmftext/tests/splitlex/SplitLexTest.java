package eu.mihosoft.vmftext.tests.splitlex;

import eu.mihosoft.vmftext.tests.splitlex.parser.SplitLexModelParser;
import eu.mihosoft.vmftext.tests.splitlex.unparser.SplitLexModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** #39: a split grammar (SplitLexLexer.g4 + SplitLexParser.g4 with tokenVocab) generates one model. */
public class SplitLexTest {

    private static final String SRC = "def a = 1\ndef bb = 22\n";

    @Test
    public void modelIsNamedAfterTheParserGrammarWithoutSuffix() {
        SplitLexModel model = new SplitLexModelParser().parse(SRC);
        Assert.assertEquals(2, model.getRoot().getItems().size());
        Assert.assertEquals("bb", model.getRoot().getItems().get(1).getName());
        Assert.assertEquals("22", model.getRoot().getItems().get(1).getValue());
    }

    @Test
    public void roundTripIsExact() {
        Assert.assertEquals(SRC, new SplitLexModelUnparser().unparse(new SplitLexModelParser().parse(SRC)));
        String spaced = "def  a=1\ndef bb =   22\n";
        Assert.assertEquals(spaced, new SplitLexModelUnparser().unparse(new SplitLexModelParser().parse(spaced)));
    }

    @Test
    public void programmaticModelUnparsesWithTheLexerLiterals() {
        Item item = Item.newBuilder().withName("x").withValue("7").withNl("\n").build();
        SplitLexModel model = SplitLexModel.newBuilder().withRoot(Unit.newBuilder().withItems(item).build()).build();
        String out = new SplitLexModelUnparser().unparse(model);
        Assert.assertEquals(out, "defx=7\n", out.replaceAll("[ \\t]+", ""));
    }
}
