package eu.mihosoft.vmftext.tests.lexicalpreservation.minijava;

import eu.mihosoft.vmftext.tests.minijava.CodeElement;
import eu.mihosoft.vmftext.tests.minijava.MiniJavaModel;
import eu.mihosoft.vmftext.tests.minijava.parser.MiniJavaModelParser;
import eu.mihosoft.vmftext.tests.minijava.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.minijava.unparser.MiniJavaModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;

public class MiniJavaLPTest {
    @Test
    public void unparseTest1() {
        String code ="" +
        "class Main {\n"+
        "    public static void main( String[ ] arguments ) {\n"+
        "        System.out.println( 2+3 );\n"+
        "    }\n"+
        "}\n";

        MiniJavaModel model = new MiniJavaModelParser().parse(code);

        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();

//        unparser.setFormatter(new BaseFormatter(){
//            @Override
//            public void pre(MiniJavaModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {
//                super.pre(unparser, ruleInfo, w);
//                w.append(" ");
//
//                System.out.println("-> "+ruleInfo.getGrammarText() + "   ->   " + ruleInfo.isOptional());
//            }
//
//            @Override
//            public void done(CodeElement e, boolean success, PrintWriter w) {
//                System.out.println("DONE: " + e.getClass().getSimpleName());
//            }
//        });

        String newCode = unparser.unparse(model);

        System.out.println("CODE:");
        System.out.println(newCode);

        Assert.assertEquals(code,newCode);

    }
}
