package eu.mihosoft.vmftext.tests.lexicalpreservation.java8;

import eu.mihosoft.vmftext.tests.java8.ClassDeclaration;
import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.Java8SourceBundle;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Java8LexicalStressTest {

    private final Java8ModelParser parser = new Java8ModelParser();
    private final Java8ModelUnparser unparser = new Java8ModelUnparser();

    @Test
    public void exactRoundTripWithCommentsNestedClassGenericsAnnotationsAndLambda() {
        String source = "" +
                "package demo.lexical;\n" +
                "\n" +
                "/* import comment */\n" +
                "import java.util.function.Function;\n" +
                "\n" +
                "// class comment\n" +
                "@Deprecated\n" +
                "public class Stress<T> {\n" +
                "\t/* nested */ static class Nested { }\n" +
                "\tpublic T id(T value) { return value; }\n" +
                "\tpublic void lambdas(){\n" +
                "\t\tFunction<String,String> f = s -> s.trim();\n" +
                "\t}\n" +
                "}\n" +
                "/* trailing eof */\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void exactRoundTripWithMixedTabsSpacesAndCrLf() {
        String source = "package demo.tabs;\r\n" +
                "public\tclass Tabs {\r\n" +
                "\tpublic static void main(String[] args ){\r\n" +
                "\t\tSystem.out.println( \"x\" );\r\n" +
                "\t}\r\n" +
                "}\r\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForExistingComplexSample() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get("test-code/Java8ComplexCode01.java")));
        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresFocusedSampleExactly() {
        String source = "package demo.bundle;\npublic class BundleStress { }\n/* keep */\n";
        Java8Model model = parser.parse(source);
        Java8SourceBundle bundle = parser.toSourceBundle(model, source);
        Java8Model restored = parser.restoreFromSourceBundle(bundle);

        Assert.assertEquals(source, unparser.unparse(restored));
    }

    @Test
    public void mutationRemainsParseableAfterPrimitiveEdit() {
        String source = "package demo.mutation;\npublic class Before { }\n";
        Java8Model model = parser.parse(source);

        model.vmf().content().stream(ClassDeclaration.class).findFirst().
                ifPresent(classDeclaration -> classDeclaration.setClassName("After"));

        String changed = unparser.unparse(model);
        Assert.assertTrue(changed.contains("After"));
        parser.parse(changed);
    }
}
