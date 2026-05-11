package eu.mihosoft.vmftext.tests.lexicalpreservation.json;

import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.JSONSourceBundle;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import org.junit.Assert;
import org.junit.Test;

public class JSONLexicalStressTest {

    private final JSONModelParser parser = new JSONModelParser();
    private final JSONModelUnparser unparser = new JSONModelUnparser();

    @Test
    public void exactRoundTripForNestedObjectsArraysAndWhitespace() {
        String source = "{\n" +
                "\t\"version\" : 1.0,\n" +
                "\t\"data\" : {\n" +
                "\t\t\"items\" : [ \"a\" , true , false , null , { \"n\" : 2.0 } ]\n" +
                "\t}\n" +
                "}";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForCompactJson() {
        String source = "{\"a\":1.0,\"b\":[true,false,null]}";
        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresJsonExactly() {
        String source = "{ \"a\" : [ 1.0 , 2.0 , 3.0 ] }";
        JSONModel model = parser.parse(source);
        JSONSourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, unparser.unparse(parser.restoreFromSourceBundle(bundle)));
    }

    @Test
    public void mutationByAppendingParsedPairRemainsParseable() {
        String source = "{ \"a\" : 1.0 }";
        JSONModel changed = parser.parse("{ \"a\" : 1.0, \"b\" : 2.0 }");

        String changedSource = unparser.unparse(changed);
        Assert.assertTrue(changedSource.contains("\"b\""));
        parser.parse(changedSource);
    }
}
