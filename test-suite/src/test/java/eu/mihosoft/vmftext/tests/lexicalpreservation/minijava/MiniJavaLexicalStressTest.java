package eu.mihosoft.vmftext.tests.lexicalpreservation.minijava;

import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import eu.mihosoft.vmftext.tests.minijava.MainClass;
import eu.mihosoft.vmftext.tests.minijava.MiniJavaModel;
import eu.mihosoft.vmftext.tests.minijava.MiniJavaSourceBundle;
import eu.mihosoft.vmftext.tests.minijava.parser.MiniJavaModelParser;
import eu.mihosoft.vmftext.tests.minijava.unparser.MiniJavaModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class MiniJavaLexicalStressTest {

    private final MiniJavaModelParser parser = new MiniJavaModelParser();
    private final MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();

    @Test
    public void exactRoundTripWithLineAndMultilineComments() {
        String source = "" +
                "/* lead */\n" +
                "class Main {\n" +
                "    public static void main( String[ ] arguments ) {\n" +
                "        // print expression\n" +
                "        System.out.println( 2+3 );\n" +
                "    }\n" +
                "}\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void exactRoundTripWithCrLfAndTabs() {
        String source = "class Main {\r\n" +
                "\tpublic static void main( String[ ] args ) {\r\n" +
                "\t\tSystem.out.println( 1 );\r\n" +
                "\t}\r\n" +
                "}\r\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresMiniJavaExactly() {
        String source = "class Main {\n" +
                "    public static void main( String[ ] args ) {\n" +
                "        System.out.println( 7 );\n" +
                "    }\n" +
                "}\n";

        MiniJavaModel model = parser.parse(source);
        MiniJavaSourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, unparser.unparse(parser.restoreFromSourceBundle(bundle)));
    }

    @Test
    public void mutationRemainsParseableAfterMainClassRename() {
        String source = "class Main {\n" +
                "    public static void main( String[ ] args ) {\n" +
                "        System.out.println( 1 );\n" +
                "    }\n" +
                "}\n";

        MiniJavaModel model = parser.parse(source);
        model.vmf().content().stream(MainClass.class).findFirst().
                ifPresent(mainClass -> mainClass.setName("RenamedMain"));

        String changed = unparser.unparse(model);
        Assert.assertTrue(changed.contains("RenamedMain"));
        parser.parse(changed);
    }
}
