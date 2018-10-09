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

        String code = "" +
                " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 )";

        // parse the code to a model instance
        ArrayLangModel model = new ArrayLangModelParser().parse(code);

        // unparse the model to 'newCode'
        ArrayLangModelUnparser unparser = new ArrayLangModelUnparser();
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
    public void lexicalPreservationTestArrayLangEOF() {

        // the code to reproduce
        String code = "" +
                " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 ) ";

        // parse the code to a model instance
        ArrayLangModel model = new ArrayLangModelParser().parse(code);

        // unparse the model to 'newCode'
        ArrayLangModelUnparser unparser = new ArrayLangModelUnparser();
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
}
