package eu.mihosoft.vmftext.tests.matchcorrectaltlistelements;

import eu.mihosoft.vmftext.tests.matchcorrectaltlistelements.parser.MatchCorrectAltListElementsModelParser;
import eu.mihosoft.vmftext.tests.matchcorrectaltlistelements.unparser.MatchCorrectAltListElementsModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class MatchCorrectAltListElementsTest {

    @Test
    public void testRule1() {
        MatchCorrectAltListElementsModelParser parser = new MatchCorrectAltListElementsModelParser();
        MatchCorrectAltListElementsModel model = MatchCorrectAltListElementsModel.newInstance();
        MatchCorrectAltListElementsModelUnparser unparser = new MatchCorrectAltListElementsModelUnparser();
        Root root = Root.newInstance();
        model.setRoot(root);

        Rule1 rule1 = Rule1.newInstance();
        rule1.getElements().add("[");
        rule1.getElements().add("]");

        root.setRuleOne(rule1);

        String code = unparser.unparse(model);
        System.out.println("1) code: " + code);
        Assert.assertEquals(" [ ]", code);



        rule1 = Rule1.newInstance();
        rule1.getElements().add("[]");
        root.setRuleOne(rule1);

        code = unparser.unparse(model);
        System.out.println("2) code: " + code);
        Assert.assertEquals(" []", code);



    }

    @Test
    public void testRule2() {
        MatchCorrectAltListElementsModelParser parser = new MatchCorrectAltListElementsModelParser();
        MatchCorrectAltListElementsModel model = MatchCorrectAltListElementsModel.newInstance();
        MatchCorrectAltListElementsModelUnparser unparser = new MatchCorrectAltListElementsModelUnparser();
        Root root = Root.newInstance();
        model.setRoot(root);

        Rule2 rule2 = Rule2.newInstance();
        rule2.getElements().add("[");
        rule2.getElements().add("]");

        root.setRuleTwo(rule2);

        String code = unparser.unparse(model);
        System.out.println("1) code: " + code);
        Assert.assertEquals(" [ ]", code);


        rule2 = Rule2.newInstance();
        rule2.getElements().add("[]");
        root.setRuleTwo(rule2);

        code = unparser.unparse(model);
        System.out.println("2) code: " + code);
        Assert.assertEquals(" []", code);



    }

}
