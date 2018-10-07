package eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules;

import eu.mihosoft.vmftext.tests.arraylang.ArrayLangModel;
import eu.mihosoft.vmftext.tests.arraylang.CodeElement;
//import eu.mihosoft.vmftext.tests.arraylang.Payload;
import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.parser.CombinedLexerRulesModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.unparser.CombinedLexerRulesModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LexicalPreservationTest {

//    @Test
//    public void hiddenTextTest() {
//        String code = " ( 1,2,  3,4,5\n,6,7,8,9) ";
//
//        ArrayLangModelParser parser = new ArrayLangModelParser();
//
//        ArrayLangModel model = parser.parse(code);
//
//        //Payload payload =  model.getRoot().payload();
//
//        //Assert.assertNotNull(payload);
//
//        //Object payloadObj = model.getRoot().payload().payloadGet("vmf-text:hidden-text");
//        //Assert.assertNotNull(payloadObj);
//
//        // List<String> hiddenChars = (List<String>) model.getRoot().payload().payloadGet("vmf-text:hidden-text");
//
//        // there are 20 relevant characters + 1 additional empty string
//        // (to prevent 'index out of bounds' exceptions ) in case the
//        // grammar doesn't end with EOF in which case we won't be able to parse the
//        // WS/hidden chars from the end of the rule to the end of the file.
//        //Assert.assertEquals(21, hiddenChars.size());
//
//        //List<String> hiddenCharsNonEmpty = hiddenChars.stream().filter(s->!s.isEmpty()).collect(Collectors.toList());
////
////        Assert.assertEquals(hiddenCharsNonEmpty.size(), 5);
////        Assert.assertEquals(hiddenCharsNonEmpty.get(0), " ");
////        Assert.assertEquals(hiddenCharsNonEmpty.get(1), " ");
////        Assert.assertEquals(hiddenCharsNonEmpty.get(2), "  ");
////        Assert.assertEquals(hiddenCharsNonEmpty.get(3), "\n");
////        Assert.assertEquals(hiddenCharsNonEmpty.get(4), " ");
//
//    }

    @Test
    public void lexicalPreservationTestJava8() {

        // the code to reproduce
        String code = "" +
                "package my.testpackage.abc ;\n" +
                "\n" +
                "public  class MyClass {\n" +
                "  public static void main(String[] args ){\n" +
                "    System.out.println( \"Hello from VMF-Text\");\n" +
                "  }\n"+
                "}";

        // parse the code to a model instance
        Java8Model model = new Java8ModelParser().parse(code);

        // unparse the model to 'newCode'
        Java8ModelUnparser unparser = new Java8ModelUnparser();
        unparser.setFormatter(new DefaultJava8Formatter(model.getRoot()));
        String newCode = unparser.unparse(model);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // parse the new code to another model instance
        Java8Model modelAfterUnparsing = new Java8ModelParser().parse(newCode);

        // check whether the models are equal
        Assert.assertEquals(model, modelAfterUnparsing);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code, newCode);
    }

    @Test
    public void lexicalPreservationTestArrayLang() {

        // the code to reproduce (INCLUDING EOF (TODO 7.10.2018))
//        String code = "" +
//                " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 ) ";

        // the code to reproduce
        String code = "" +
                " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 )";

        // parse the code to a model instance
        ArrayLangModel model = new ArrayLangModelParser().parse(code);

        // unparse the model to 'newCode'
        ArrayLangModelUnparser unparser = new ArrayLangModelUnparser();
        unparser.setFormatter(new DefaultArrayLangFormatter(model.getRoot()));
        String newCode = unparser.unparse(model);

        // parse the new code to another model instance
        ArrayLangModel modelAfterUnparsing = new ArrayLangModelParser().parse(newCode);

        // check whether the models are equal
        Assert.assertEquals(model, modelAfterUnparsing);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code, newCode);
    }

    @Test
    public void lexicalPreservationTestCombinedLexerRules() {

        // the code to reproduce
        String code = "" +
                " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 )";

        // parse the code to a model instance
        CombinedLexerRulesModel model = new CombinedLexerRulesModelParser().parse(code);

        // unparse the model to 'newCode'
        CombinedLexerRulesModelUnparser unparser = new CombinedLexerRulesModelUnparser();
        unparser.setFormatter(new DefaultCombinedLexerRulesFormatter(model.getRoot()));
        String newCode = unparser.unparse(model);

        // parse the new code to another model instance
        CombinedLexerRulesModel modelAfterUnparsing = new CombinedLexerRulesModelParser().parse(newCode);

        // check whether the models are equal
        Assert.assertEquals(model, modelAfterUnparsing);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code, newCode);
    }

    static class DefaultArrayLangFormatter extends BaseFormatter {

        public DefaultArrayLangFormatter(CodeElement e) {
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

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",--value);
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
        public void pre(ArrayLangModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {
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
    }

    static class DefaultJava8Formatter extends eu.mihosoft.vmftext.tests.java8.unparser.BaseFormatter {

        public DefaultJava8Formatter(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            //
        }

        private void inc(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",++value);
        }
        private void dec(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",--value);
        }

        private int getCounter(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            return value;
        }

        private List<String> getHiddenText(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            return (List<String>)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:ignored-pieces-of-text");
        }

        @Override
        public void pre(Java8ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {

            if(ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE) {
                eu.mihosoft.vmftext.tests.java8.CodeElement e = ruleInfo.getParentObject();

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

        public void post(Java8ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w ) {

        }

    }

    static class DefaultCombinedLexerRulesFormatter extends eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.unparser.BaseFormatter {

        public DefaultCombinedLexerRulesFormatter(eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e) {
            //
        }

        private void inc(eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",++value);
        }
        private void dec(eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            ((Map<String,Object>)e.getPayload()).put("vmf-text:formatter:counter",--value);
        }

        private int getCounter(eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e) {
            Integer value = (Integer)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:formatter:counter");

            if(value==null) {
                value = 0;
            }

            return value;
        }

        private List<String> getHiddenText(eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e) {
            return (List<String>)
                    ((Map<String,Object>)e.getPayload()).get("vmf-text:ignored-pieces-of-text");
        }

        @Override
        public void pre(CombinedLexerRulesModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {

            if(ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE) {
                eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.CodeElement e = ruleInfo.getParentObject();

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
    }
}
