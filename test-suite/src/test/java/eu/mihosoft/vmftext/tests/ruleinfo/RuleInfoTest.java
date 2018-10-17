package eu.mihosoft.vmftext.tests.ruleinfo;


import eu.mihosoft.vmftext.tests.expressionlang.unparser.ExpressionLangModelUnparser;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Test;

import java.io.PrintWriter;

public class RuleInfoTest {
    @Test
    public void ruleInfoPathTest() {

        String code = "{\"key\":\"abc\"}";

        JSONModel model = new JSONModelParser().parse(code);

        JSONModelUnparser unparser = new JSONModelUnparser();
        unparser.setFormatter(new BaseFormatter(){
            @Override
            public void render(JSONModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w, String s) {
                super.render(unparser,ruleInfo,w,s+" ");
                System.out.println(ruleInfo.getRuleName().orElse("<NONE>")+" ->  "+ruleInfo.getRulePath()+" -> "+ruleInfo.getRuleText());
            }
        });

        unparser.unparse(model);
    }
}
