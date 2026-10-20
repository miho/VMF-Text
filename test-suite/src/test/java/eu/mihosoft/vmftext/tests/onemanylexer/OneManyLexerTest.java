package eu.mihosoft.vmftext.tests.onemanylexer;

import eu.mihosoft.vmftext.tests.onemanylexer.parser.OneManyLexerModelParser;
import eu.mihosoft.vmftext.tests.onemanylexer.unparser.OneManyLexerModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** C-trial finding 1: {@code X+} lists of lexer rules must unparse. */
public class OneManyLexerTest {

    private static final String SRC = "a b  c;\n\"x\" \"y\";\n";

    @Test
    public void plusListsOfLexerRulesRoundTripExactly() {
        OneManyLexerModel model = new OneManyLexerModelParser().parse(SRC);
        Assert.assertEquals(2, model.getRoot().getParts().size());
        Assert.assertEquals(3, model.getRoot().getParts().get(0).getWords().size());
        Assert.assertEquals(2, model.getRoot().getParts().get(1).getStrings().size());
        Assert.assertEquals(SRC, new OneManyLexerModelUnparser().unparse(model));
    }

    @Test
    public void singleElementPlusListUnparses() {
        OneManyLexerModel model = new OneManyLexerModelParser().parse("\"only\";");
        Assert.assertEquals("\"only\";", new OneManyLexerModelUnparser().unparse(model));
    }

    @Test
    public void inPlaceSetKeepsSiblingWhitespace() {
        OneManyLexerModel model = new OneManyLexerModelParser().parse(SRC);
        model.getRoot().getParts().get(0).getWords().set(1, "zz");
        Assert.assertEquals("a zz  c;\n\"x\" \"y\";\n", new OneManyLexerModelUnparser().unparse(model));
    }

    @Test
    public void programmaticPlusListUnparsesWithConservativeSeparators() {
        Part part = Part.newBuilder().withWords("a", "b").build();
        Prog prog = Prog.newBuilder().withParts(part).build();
        OneManyLexerModel model = OneManyLexerModel.newBuilder().withRoot(prog).build();
        String out = new OneManyLexerModelUnparser().unparse(model);
        Assert.assertEquals("a b ;", out.trim().replaceAll("\\s+", " "));
    }
}
