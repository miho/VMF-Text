package eu.mihosoft.vmftext.tests.lexicalpreservation.multipleparserrules;

import eu.mihosoft.vmf.runtime.core.Change.ChangeType;
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

    @Test
    public void testModelChanges() {
        // the code to reproduce
        String code = "" +
                " 2  +   3\n45 +  12";

        MultipleParserRulesModelParser.IgnoredPiecesOfTextListener.setDebugOutputEnabled(false);

        MultipleParserRulesModel model = new MultipleParserRulesModelParser().parse(code);

        // change the model
        String additionalRuleCode = "101+ 202";
        Rule1 additionalRule = new MultipleParserRulesModelParser().parseRule1(additionalRuleCode);
        model.getRoot().getRules().add(additionalRule);

        // unparse the model to 'newCode'
        MultipleParserRulesModelUnparser unparser = new MultipleParserRulesModelUnparser();
       
        String newCode = unparser.unparse(model);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // now we finally check whether the new code is equal to the original code
        Assert.assertEquals(code+" " + additionalRuleCode, newCode);
    }
}