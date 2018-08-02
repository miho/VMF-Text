package eu.mihosoft.vmftext.tests.lexicalpreservation;

import eu.mihosoft.vmftext.tests.arraylang.ArrayLangModel;
import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class LexicalPreservationTest {
    @Test
    public void lexicalPreservationTestJava8() {

        // the code to reproduce
        String code = "" +
                "package my.testpackage;\n" +
                "\n" +
                "public  class MyClass {\n" +
                "  public static void main(String[] args ){\n" +
                "    System.out.println( \"Hello from VMF-Text\");\n" +
                "  }"+
                "}";

        // parse the code to a model instance
        Java8Model model = new Java8ModelParser().parse(code);

        // unparse the model to 'newCode'
        String newCode = new Java8ModelUnparser().unparse(model);

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
                "(1 ,2 , 3, 4\n,5,\n6,7,8, 0 )";

        // parse the code to a model instance
        ArrayLangModel model = new ArrayLangModelParser().parse(code);

        // unparse the model to 'newCode'
        String newCode = new ArrayLangModelUnparser().unparse(model);

        // parse the new code to another model instance
        ArrayLangModel modelAfterUnparsing = new ArrayLangModelParser().parse(newCode);

        // check whether the models are equal
        Assert.assertEquals(model, modelAfterUnparsing);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code, newCode);
    }
}
