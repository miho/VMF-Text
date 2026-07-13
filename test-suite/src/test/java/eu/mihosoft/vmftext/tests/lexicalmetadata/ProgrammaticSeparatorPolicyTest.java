package eu.mihosoft.vmftext.tests.lexicalmetadata;

import eu.mihosoft.vmftext.tests.json.Json;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.NumberValue;
import eu.mihosoft.vmftext.tests.json.Obj;
import eu.mihosoft.vmftext.tests.json.ObjectValue;
import eu.mihosoft.vmftext.tests.json.Pair;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.Formatter;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class ProgrammaticSeparatorPolicyTest {

    private static JSONModel newProgrammaticObjectModel() {
        JSONModel model = JSONModel.newInstance();

        Obj obj = Obj.newInstance();
        Pair pair = Pair.newInstance();
        pair.setKey("key");
        NumberValue number = NumberValue.newInstance();
        number.setValue(1.0);
        pair.setValue(number);
        obj.getPairs().add(pair);

        ObjectValue objectValue = ObjectValue.newInstance();
        objectValue.setValue(obj);

        Json root = Json.newInstance();
        root.setValue(objectValue);
        model.setRoot(root);
        return model;
    }

    @Test
    public void defaultPolicyKeepsProgrammaticOutputParseable() {
        String output = new JSONModelUnparser().unparse(newProgrammaticObjectModel());
        Assert.assertEquals("{\"key\":1.0}", output.replaceAll("\\s+", ""));
    }

    @Test
    public void customPolicyCanSuppressProgrammaticSeparators() {
        Formatter.ProgrammaticSeparatorPolicy noSeparators = new Formatter.ProgrammaticSeparatorPolicy() {
            @Override
            public String separatorBefore(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                           Formatter.RuleInfo ruleInfo) {
                return "";
            }

            @Override
            public String separatorBeforeEdited(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                                Formatter.RuleInfo ruleInfo) {
                return "";
            }

            @Override
            public String separatorBeforeClosingDelimiter(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                                          Formatter.RuleInfo ruleInfo) {
                return "";
            }
        };

        JSONModelUnparser unparser = new JSONModelUnparser();
        unparser.setFormatter(Formatter.newDefaultFormatter(noSeparators));
        String output = unparser.unparse(newProgrammaticObjectModel());

        Assert.assertFalse("custom policy should avoid default single-space separators",
                output.contains(" "));
    }

    @Test
    public void parsedModelsStayExactWithCustomPolicy() {
        String source = "{ \"a\" : 1.0 }";
        JSONModel model = new JSONModelParser().parse(source);

        JSONModelUnparser unparser = new JSONModelUnparser();
        unparser.setFormatter(Formatter.newDefaultFormatter(new Formatter.ProgrammaticSeparatorPolicy() {
            @Override
            public String separatorBefore(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                          Formatter.RuleInfo ruleInfo) {
                return "SHOULD-NOT-APPEAR";
            }

            @Override
            public String separatorBeforeEdited(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                                Formatter.RuleInfo ruleInfo) {
                return "SHOULD-NOT-APPEAR";
            }

            @Override
            public String separatorBeforeClosingDelimiter(eu.mihosoft.vmftext.tests.json.CodeElement parent,
                                                          Formatter.RuleInfo ruleInfo) {
                return "SHOULD-NOT-APPEAR";
            }
        }));

        Assert.assertEquals(source, unparser.unparse(model));
    }
}
