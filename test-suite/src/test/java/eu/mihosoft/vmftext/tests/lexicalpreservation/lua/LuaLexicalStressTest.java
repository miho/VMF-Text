package eu.mihosoft.vmftext.tests.lexicalpreservation.lua;

import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import eu.mihosoft.vmftext.tests.lua.LuaModel;
import eu.mihosoft.vmftext.tests.lua.LuaSourceBundle;
import eu.mihosoft.vmftext.tests.lua.parser.LuaModelParser;
import eu.mihosoft.vmftext.tests.lua.unparser.LuaModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LuaLexicalStressTest {

    private final LuaModelParser parser = new LuaModelParser();
    private final LuaModelUnparser unparser = new LuaModelUnparser();

    @Test
    public void exactRoundTripForCommentsAndWhitespace() {
        String source = "\tlocal y = 1 + 2\n" +
                "-- trailing comment\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForFunctionAndTableConstructor() {
        String source = "function f(a, b)\n" +
                "  local t = { a, b, name = \"vmf\" }\n" +
                "  return t\n" +
                "end\n";

        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForExistingLuaSample() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get("test-code/lua/test-code-1.lua")));
        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresLuaExactly() {
        String source = "-- bundle\nlocal value = 42\n";
        LuaModel model = parser.parse(source);
        LuaSourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, unparser.unparse(parser.restoreFromSourceBundle(bundle)));
    }

    @Test
    public void appendedStatementSourceRemainsParseable() {
        String changedSource = "local value = 1\nvalue = value + 1\n";
        LuaModel model = parser.parse(changedSource);
        String unparsed = unparser.unparse(model);
        parser.parse(unparsed);
    }
}
