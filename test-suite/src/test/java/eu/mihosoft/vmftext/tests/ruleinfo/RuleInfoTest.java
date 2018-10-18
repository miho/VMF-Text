package eu.mihosoft.vmftext.tests.ruleinfo;


import eu.mihosoft.vmftext.tests.expressionlang.unparser.ExpressionLangModelUnparser;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import eu.mihosoft.vmftext.tests.ruleinfo.parser.RuleInfoModelParser;
import eu.mihosoft.vmftext.tests.ruleinfo.unparser.RuleInfoModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RuleInfoTest {
    @Test
    public void ruleInfoPathTest() {

        String code = "{\"key\":\"abc\"}";

        JSONModel model = new JSONModelParser().parse(code);

        JSONModelUnparser unparser = new JSONModelUnparser();

        String[] expectedInfoStrings = {
                "<NONE> ->  /r1/a0/e0 -> {",
                "STRING ->  /r2/a0/e0 -> \"key\"",
                "<NONE> ->  /r2/a0/e1 -> :",
                "STRING ->  /r4/a0/e0 -> \"abc\"",
                "<NONE> ->  /r1/a0/e3 -> }"
        };

        List<String> infoStrings = new ArrayList<>(expectedInfoStrings.length);

        unparser.setFormatter(new BaseFormatter(){
            @Override
            public void render(JSONModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w, String s) {
                super.render(unparser,ruleInfo,w,s+" ");
                String infoString = ruleInfo.getRuleName().orElse("<NONE>")+" ->  "+ruleInfo.getRulePath()+" -> "+ruleInfo.getRuleText();
                System.out.println(infoString);
                infoStrings.add(infoString);
            }
        });

        unparser.unparse(model);

        Assert.assertArrayEquals(expectedInfoStrings, infoStrings.toArray(new String[infoStrings.size()]));
    }

    @Test
    public void testEffectivelyOptional(){
        String code = "[  ]";
        RuleInfoModel model = new RuleInfoModelParser().parse(code);

        RuleInfoModelUnparser unparser = new RuleInfoModelUnparser();

        String[] expectedInfoStrings = {
                "<NONE> -> true",
                "<NONE> -> true"
        };

        List<String> infoStrings = new ArrayList<>(expectedInfoStrings.length);

        unparser.setFormatter(new eu.mihosoft.vmftext.tests.ruleinfo.unparser.BaseFormatter(){
            @Override
            public void render(RuleInfoModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w, String s) {
                super.render(unparser,ruleInfo,w,s+" ");
                String infoString = ruleInfo.getRuleName().orElse("<NONE>")+" -> " + ruleInfo.isOptional();
                System.out.println(infoString);
                infoStrings.add(infoString);
            }
        });

        unparser.unparse(model);

        Assert.assertArrayEquals(expectedInfoStrings, infoStrings.toArray(new String[infoStrings.size()]));

    }
}
