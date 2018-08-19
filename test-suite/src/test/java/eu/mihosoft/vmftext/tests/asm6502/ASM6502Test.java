package eu.mihosoft.vmftext.tests.asm6502;

import eu.mihosoft.vmftext.tests.asm6502.parser.ASM6502ModelParser;
import eu.mihosoft.vmftext.tests.asm6502.unparser.ASM6502ModelUnparser;

import eu.mihosoft.vmftext.tests.asm6502.unparser.Formatter;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;

public class ASM6502Test {
    @Test
    public void parseUnparseTest() {
        createParseUnparseTest(new File("test-code/asm6502/combsort.txt"), false);
    }

    public void createParseUnparseTest(File file, boolean compareCode) {
        try {
            createParseUnparseTest(new String(Files.readAllBytes(file.toPath())),compareCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createParseUnparseTest(String code, boolean compareCode) {

        // parse the code
        ASM6502ModelParser parser = new ASM6502ModelParser();
        ASM6502Test.ErrorListener errorListener = new ASM6502Test.ErrorListener();
        parser.getErrorListeners().add(errorListener);
        ASM6502Model model = parser.parse(code);

        Assert.assertTrue("Code has to parse without errors.", !errorListener.hasErrors());
        Assert.assertTrue("Model must not be empty", model.getRoot()!=null);

        long count = model.getRoot().vmf().content().stream(CodeElement.class).count();
        Assert.assertTrue("Model must not be empty: #elements="+count, count > 3);

        // unparse the model
        ASM6502ModelUnparser unparser = new ASM6502ModelUnparser();
        unparser.setFormatter(new Formatter() {
            public void post(ASM6502ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w ) {
                if(!ruleInfo.getRuleText().startsWith(";")) {
                    w.append(" ");
                }
            }
        });
        String newCode = unparser.unparse(model);

        System.out.println(newCode);

        // ensure the code is equal to the unparsed code
        if(compareCode) {
            Assert.assertEquals(code, newCode);
        }

        // parse the unparsed code and
        ASM6502Model newModel = parser.parse(newCode);

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
