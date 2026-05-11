package eu.mihosoft.vmftext.tests.lexicalpreservation.miniclang;

import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import eu.mihosoft.vmftext.tests.miniclang.MiniClangModel;
import eu.mihosoft.vmftext.tests.miniclang.MiniClangSourceBundle;
import eu.mihosoft.vmftext.tests.miniclang.parser.MiniClangModelParser;
import eu.mihosoft.vmftext.tests.miniclang.unparser.MiniClangModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MiniClangLexicalStressTest {

    private final MiniClangModelParser parser = new MiniClangModelParser();
    private final MiniClangModelUnparser unparser = new MiniClangModelUnparser();

    @Test
    public void exactRoundTripForIncludesDefinesCommentsAndStatements() {
        String source = "#include <stdio.h>\n" +
                "#define SIZE 2\n" +
                "int main() {\n" +
                "// keep this persistent comment\n" +
                "int x = 1;\n" +
                "printf(\"%d\", x);\n" +
                "}\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForExistingMiniClangSample() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get("test-code/transpose-blocking.c")));
        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresMiniClangExactly() {
        String source = "int main() {\n" +
                "/* hidden block */\n" +
                "int x = 1;\n" +
                "}\n";

        MiniClangModel model = parser.parse(source);
        MiniClangSourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, unparser.unparse(parser.restoreFromSourceBundle(bundle)));
    }

    @Test
    public void appendedStatementSourceRemainsParseable() {
        String changedSource = "int main() {\n" +
                "int x = 1;\n" +
                "x = x + 1;\n" +
                "}\n";

        MiniClangModel model = parser.parse(changedSource);
        parser.parse(unparser.unparse(model));
    }
}
