package eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules;

import eu.mihosoft.vmftext.tests.arraylang.ArrayLangModel;
import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.LexicalPreservationTest;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules.parser.MultipleParserRulesModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules.parser.MultipleParserRulesParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules.unparser.MultipleParserRulesModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

public class MultipleParserRulesTest {

    @Test
    public void test() {
        // the code to reproduce
        String code = "" +
                " 2  +   3\n45 +  12";

        MultipleParserRulesModelParser.IgnoredPiecesOfTextListener.setDebugOutputEnabled(false);

        MultipleParserRulesModel model = new MultipleParserRulesModelParser().parse(code);

        // unparse the model to 'newCode'
        MultipleParserRulesModelUnparser unparser = new MultipleParserRulesModelUnparser();
        unparser.setFormatter(new MultipleParserRulesFormatter(model.getRoot()));
        String newCode = unparser.unparse(model);

        // parse the new code to another model instance
        MultipleParserRulesModel modelAfterUnparsing = new MultipleParserRulesModelParser().parse(newCode);

        // check whether the models are equal
        Assert.assertEquals(model, modelAfterUnparsing);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code, newCode);
    }

    static class MultipleParserRulesFormatter extends eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules.unparser.BaseFormatter {

        public MultipleParserRulesFormatter(eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules.CodeElement e) {
            //
        }

        private void inc(CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",++value);
        }
        private void dec(CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",value--);
        }

        private int getCounter(CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            return value;
        }

        private List<String> getHiddenText(CodeElement e) {
            return (List<String>)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:ignored-pieces-of-text");
        }

        @Override
        public void pre(MultipleParserRulesModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {

            if(ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE) {

                CodeElement e = ruleInfo.getParentObject();

                if(!getHiddenText(e).isEmpty()) {
                    int pos = getCounter(e);
                    String ws = getHiddenText(e).get(pos);
                    w.append(ws);
                    inc(e);
                } else {
                    w.append(" ");
                }
            }

        }

        public void post(MultipleParserRulesModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w ) {

        }

    }
}
