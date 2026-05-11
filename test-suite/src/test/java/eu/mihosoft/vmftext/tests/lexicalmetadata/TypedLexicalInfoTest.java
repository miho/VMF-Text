package eu.mihosoft.vmftext.tests.lexicalmetadata;

import eu.mihosoft.vmftext.tests.json.CodeElement;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.LexicalInfo;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class TypedLexicalInfoTest {

    @Test
    public void parsedModelContainsTypedLexicalInfo() {
        String source = "{ \"a\" : [ 1.0 , 2.0 ] }";
        JSONModel model = new JSONModelParser().parse(source);

        CodeElement root = model.getRoot();
        Assert.assertNotNull(root.getLexicalInfo());
        Assert.assertNotNull(root.getLexicalInfo().getIgnoredPiecesOfText());
        Assert.assertNotNull(root.getLexicalInfo().getOptionalSymbols());
        Assert.assertNotNull(root.getLexicalInfo().getOriginalRange());
        Assert.assertTrue(root.getLexicalInfo().getGrammarElementPath().endsWith("Context"));
    }

    @Test
    public void unparserCanUseTypedLexicalInfoWithoutLexicalPayloadEntries() {
        String source = "{ \"a\" : [ 1.0 , 2.0 ] }";
        JSONModel model = new JSONModelParser().parse(source);

        model.getRoot().vmf().content().stream(CodeElement.class).
                forEach(element -> element.setPayload(new HashMap<String,Object>()));

        Assert.assertEquals(source, new JSONModelUnparser().unparse(model));
    }

    @Test
    public void semanticEqualityIgnoresTypedLexicalInfo() {
        String source = "{ \"a\" : 1.0 }";
        JSONModel first = new JSONModelParser().parse(source);
        JSONModel second = new JSONModelParser().parse(source);

        LexicalInfo lexicalInfo = first.getRoot().getLexicalInfo();
        lexicalInfo.getIgnoredPiecesOfText().clear();
        lexicalInfo.getIgnoredPiecesOfText().add("/* changed only lexical metadata */");

        Assert.assertEquals(first, second);
    }
}
