package eu.mihosoft.vmftext.tests.lua;

import eu.mihosoft.vmftext.tests.lua.parser.LuaModelParser;
import eu.mihosoft.vmftext.tests.lua.unparser.LuaModelUnparser;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;

public class LuaTest {
    @Test
    public void parseUnparseTest() throws IOException {

        System.out.println(">> simple expression:");

        createParseUnparseTest("a = 2 + 3", true);
        createParseUnparseTest("a = 2 + 3 ", true);

        System.out.println(">> large code sample 1:");
        createParseUnparseTest(new String(Files.readAllBytes(Paths.get("test-code/lua/test-code-1.lua"))),
                   false);
        System.out.println(">> large code sample 2:");
        createParseUnparseTest(new String(Files.readAllBytes(Paths.get("test-code/lua/test-code-2.lua"))),
                false);

    }

    public void createParseUnparseTest(String code, boolean compareCode) {

        // parse the code
        LuaModelParser parser = new LuaModelParser();
        ErrorListener errorListener = new ErrorListener();
        parser.getErrorListeners().add(errorListener);
        LuaModel model = parser.parse(code);

        Assert.assertTrue("Code has to parse without errors.", !errorListener.hasErrors());

        // unparse the model
        LuaModelUnparser unparser = new LuaModelUnparser();
        String newCode = unparser.unparse(model);

        // ensure the code is equal to the unparsed code
        if(compareCode) {
            Assert.assertEquals(code, newCode);
        }

        // parse the unparsed code and
        LuaModel newModel = parser.parse(newCode);

        // compare the original
        // model and the model parsed from unparsed code
        Assert.assertEquals(model, newModel);
    }

    static class ErrorListener implements ANTLRErrorListener {
        private boolean errors;
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            this.errors = true;
        }

        @Override
        public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {

        }

        @Override
        public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {

        }

        @Override
        public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {

        }

        public boolean hasErrors() {
            return errors;
        }
    }


}
