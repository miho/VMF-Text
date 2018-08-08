package eu.mihosoft.vmftext.tests.lexicalpreservation;

import eu.mihosoft.vmftext.tests.arraylang.ArrayLangModel;
import eu.mihosoft.vmftext.tests.arraylang.CodeElement;
import eu.mihosoft.vmftext.tests.arraylang.Payload;
import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.arraylang.unparser.Formatter;
import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class LexicalPreservationTest {

    @Test
    public void hiddenTextTest() {
        String code = " ( 1,2,  3,4,5\n,6,7,8,9) ";

        ArrayLangModelParser parser = new ArrayLangModelParser();

        ArrayLangModel model = parser.parse(code);

        Payload payload =  model.getRoot().payload();

        Assert.assertNotNull(payload);

        Object payloadObj = model.getRoot().payload().payloadGet("vmf-text:hidden-text");
        Assert.assertNotNull(payloadObj);

        List<String> hiddenChars = (List<String>) model.getRoot().payload().payloadGet("vmf-text:hidden-text");

        Assert.assertEquals(hiddenChars.size(), 20);

        List<String> hiddenCharsNonEmpty = hiddenChars.stream().filter(s->!s.isEmpty()).collect(Collectors.toList());

        Assert.assertEquals(hiddenCharsNonEmpty.size(), 5);
        Assert.assertEquals(hiddenCharsNonEmpty.get(0), " ");
        Assert.assertEquals(hiddenCharsNonEmpty.get(1), " ");
        Assert.assertEquals(hiddenCharsNonEmpty.get(2), "  ");
        Assert.assertEquals(hiddenCharsNonEmpty.get(3), "\n");
        Assert.assertEquals(hiddenCharsNonEmpty.get(4), " ");

    }

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

    static class DefaultArrayLangFormatter extends BaseFormatter {

        private List<String> hiddenText;

        public DefaultArrayLangFormatter(CodeElement e) {
            this.hiddenText = (List<String>) e.payload().payloadGet("vmf-text:hidden-text");
            setIntState("pos", -1);
        }

        @Override
        public void pre(ArrayLangModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {
            if(getIntState("pos") == -1 &&
                    (ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE)) {
                setIntState("pos",0);

                String ws = hiddenText.get(getIntState("pos"));

                //System.out.print("{"+ws+"}" + "["+ruleInfo.getRuleText()+"]");
                w.append(ws);

                setIntState("pos",1);
            }
        }

        public void post(ArrayLangModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w ) {
            if(ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE) {
                String ws = hiddenText.get(getIntState("pos"));
                //System.out.print("["+ruleInfo.getRuleText()+"]{"+ws+"}");
                w.append(hiddenText.get(getIntState("pos")));
                setIntState("pos",getIntState("pos")+1);
            }
        }

    }

    static class DefaultJava8Formatter extends eu.mihosoft.vmftext.tests.java8.unparser.BaseFormatter {

        private List<String> hiddenText;

        public DefaultJava8Formatter(eu.mihosoft.vmftext.tests.java8.CodeElement e) {
            this.hiddenText = (List<String>) e.payload().payloadGet("vmf-text:hidden-text");
            setIntState("pos", -1);
        }

        @Override
        public void pre(Java8ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {
            if(getIntState("pos") == -1 &&
                    (ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE)) {
                setIntState("pos",0);

                String ws = hiddenText.get(getIntState("pos"));

                //System.out.print("{"+ws+"}" + "["+ruleInfo.getRuleText()+"]");
                w.append(ws);

                setIntState("pos",1);
            }
        }

        public void post(Java8ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w ) {
            if(ruleInfo.getRuleType()==RuleType.TERMINAL || ruleInfo.getRuleType()==RuleType.LEXER_RULE) {
                String ws = hiddenText.get(getIntState("pos"));
                //System.out.print("["+ruleInfo.getRuleText()+"]{"+ws+"}");
                w.append(hiddenText.get(getIntState("pos")));
                setIntState("pos",getIntState("pos")+1);
            }
        }

    }
}


