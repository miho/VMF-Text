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
import java.util.*;
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
        // rule to add to change the model
        String additionalRuleCode1 = "101+ 202";

        // perform the test
        createAddAdditionalRuleTest(code, additionalRuleCode1);

        // now we try with leading spaces to check whether they will be preserved
        String additionalRuleCode2 = "  101+ 202";
        createAddAdditionalRuleTest(code, additionalRuleCode2);
    }

    private void createAddAdditionalRuleTest(String code, String additionalRuleCode) {

        // enable debug output to check hidden text
        MultipleParserRulesModelParser.IgnoredPiecesOfTextListener.setDebugOutputEnabled(true);

        // parse the model
        MultipleParserRulesModel model = new MultipleParserRulesModelParser().parse(code);

        // additional rule to add
        Rule1 additionalRule = new MultipleParserRulesModelParser().parseRule1(additionalRuleCode);

        // now we actually add it
        model.getRoot().getRules().add(additionalRule);

        // unparse the model to 'newCode'
        MultipleParserRulesModelUnparser unparser = new MultipleParserRulesModelUnparser();

        String newCode = unparser.unparse(model);

        System.out.println("\nUNPARSED: ");
        System.out.println(newCode);

        // now we finally check whether the new code is equal to the original code
        String ws = "";

        // if the additionalRuleCode code does not start with a ws then VMF-Text needs to
        // insert one to prevent accidental merges for grammar rules that need a ws between them
        // to be correctly parsed
        // TODO 17.10.2018 what about other ws like \t or \n ? Maybe the ANTLR4 grammar needs to tell VMFText which neutral ws char to use for rule separation
        if(!additionalRuleCode.startsWith(" ")) {
            ws = " ";
        }
        Assert.assertEquals(code+ws+additionalRuleCode, newCode);
    }

}