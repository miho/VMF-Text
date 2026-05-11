package eu.mihosoft.vmftext.tests.sourcebundle;

import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.Java8SourceBundle;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.JSONSourceBundle;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class SourceBundleTest {

    @Test
    public void java8SourceBundleRestoresExactSourceWhenSemanticsMatch() {
        String source = "" +
                "package demo.bundle ;\n" +
                "\n" +
                "// source-bundle keeps this comment\n" +
                "public  class DemoBundle {\n" +
                "\tpublic static void main(String[] args ){\n" +
                "\t\tSystem.out.println( \"bundle\" );\n" +
                "\t}\n" +
                "}\n" +
                "/* eof */\n";

        Java8ModelParser parser = new Java8ModelParser();
        Java8Model model = parser.parse(source);

        Java8SourceBundle bundle = parser.toSourceBundle(model, source);
        Java8Model restored = parser.restoreFromSourceBundle(bundle);

        Assert.assertEquals(source, new Java8ModelUnparser().unparse(restored));
        Assert.assertEquals(Java8SourceBundle.GRAMMAR_NAME, bundle.getGrammarName());
        Assert.assertEquals(Java8SourceBundle.checksum(source), bundle.getChecksum());
    }

    @Test
    public void jsonSourceBundleRestoresExactSourceWhenSemanticsMatch() {
        String source = "{\n" +
                "  \"version\" : 1.0,\n" +
                "  \"data\" : [ true , false , null ]\n" +
                "}";

        JSONModelParser parser = new JSONModelParser();
        JSONModel model = parser.parse(source);

        JSONSourceBundle bundle = parser.toSourceBundle(model, source);
        JSONModel restored = parser.restoreFromSourceBundle(bundle);

        Assert.assertEquals(source, new JSONModelUnparser().unparse(restored));
    }

    @Test
    public void sourceBundleFallsBackToStoredModelWhenSourceAndModelDiverge() {
        String source = "package demo;\npublic class Demo { }\n";
        String changedSource = "package demo;\npublic class DemoChanged { }\n";

        Java8ModelParser parser = new Java8ModelParser();
        Java8Model changedModel = parser.parse(changedSource);

        Java8SourceBundle bundle = parser.toSourceBundle(changedModel, source);
        Java8Model restored = parser.restoreFromSourceBundle(bundle);

        Assert.assertEquals(changedModel, restored);

        String restoredSource = new Java8ModelUnparser().unparse(restored);
        Assert.assertTrue("Restore must fall back to the changed stored model.",
                restoredSource.contains("DemoChanged"));
        Assert.assertFalse("Fallback output must not be the stale bundled source.",
                source.equals(restoredSource));
        Assert.assertEquals(restored, parser.parse(restoredSource));
    }

    @Test
    public void sourceBundleFallsBackToStoredModelWhenSourceIsCorrupted() {
        String validSource = "{ \"version\" : 1.0 }";
        String corruptedSource = "{ \"version\" : ";

        JSONModelParser parser = new JSONModelParser();
        JSONModel storedModel = parser.parse(validSource);

        JSONSourceBundle bundle = parser.toSourceBundle(storedModel, corruptedSource);
        JSONModel restored = parser.restoreFromSourceBundle(bundle);

        Assert.assertEquals(storedModel, restored);

        String restoredSource = new JSONModelUnparser().unparse(restored);
        Assert.assertEquals(restored, parser.parse(restoredSource));
    }
}
