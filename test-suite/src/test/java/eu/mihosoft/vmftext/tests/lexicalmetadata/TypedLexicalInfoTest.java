package eu.mihosoft.vmftext.tests.lexicalmetadata;

import eu.mihosoft.vmftext.tests.json.CodeElement;
import eu.mihosoft.vmftext.tests.json.JSONModel;
import eu.mihosoft.vmftext.tests.json.LexicalInfo;
import eu.mihosoft.vmftext.tests.json.TriviaPiece;
import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TypedLexicalInfoTest {

    @Test
    public void parsedModelContainsTypedLexicalInfo() {
        String source = "{ \"a\" : [ 1.0 , 2.0 ] }";
        JSONModel model = new JSONModelParser().parse(source);

        CodeElement root = model.getRoot();
        Assert.assertNotNull(root.getLexicalInfo());
        Assert.assertNotNull(root.getLexicalInfo().getTriviaPieces());
        Assert.assertFalse(root.getLexicalInfo().getTriviaPieces().isEmpty());
        Assert.assertNotNull(root.getLexicalInfo().getTriviaPieces().get(0).getText());
        Assert.assertNotNull(root.getLexicalInfo().getTriviaPieces().get(0).getKind());
        Assert.assertNotNull(root.getLexicalInfo().getOptionalSymbols());
        Assert.assertNotNull(root.getLexicalInfo().getOriginalRange());
        Assert.assertTrue(root.getLexicalInfo().getGrammarElementPath().endsWith("Context"));
    }

    @Test
    public void freshlyParsedModelsDoNotWriteLexicalPayloadEntries() {
        String source = "{ \"a\" : [ 1.0 , 2.0 ] }";
        JSONModel model = new JSONModelParser().parse(source);

        model.getRoot().vmf().content().stream(CodeElement.class).forEach(e -> {
            Object payload = e.getPayload();
            if (payload instanceof Map) {
                ((Map<?, ?>) payload).keySet().forEach(key ->
                        Assert.assertFalse(
                                "unexpected lexical payload entry: " + key,
                                key instanceof String && ((String) key).startsWith("vmf-text:")));
            }
        });
    }

    @Test
    public void unparserCanUseTypedLexicalInfoWithoutLexicalPayloadEntries() {
        String source = "{ \"a\" : [ 1.0 , 2.0 ] }";
        JSONModel model = new JSONModelParser().parse(source);

        model.getRoot().vmf().content().stream(CodeElement.class).
                map(CodeElement::getPayload).
                filter(payload -> payload instanceof Map).
                map(payload -> (Map<?,?>) payload).
                forEach(payload -> payload.keySet().removeIf(key ->
                        key instanceof String && ((String) key).startsWith("vmf-text:")));

        Assert.assertEquals(source, new JSONModelUnparser().unparse(model));
    }

    @Test
    public void semanticEqualityIgnoresTypedLexicalInfo() {
        String source = "{ \"a\" : 1.0 }";
        JSONModel first = new JSONModelParser().parse(source);
        JSONModel second = new JSONModelParser().parse(source);

        LexicalInfo lexicalInfo = first.getRoot().getLexicalInfo();
        lexicalInfo.getTriviaPieces().clear();
        lexicalInfo.getTriviaPieces().add(TriviaPiece.newBuilder().
                withText("/* changed only lexical metadata */").
                withKind("COMMENT").
                build());

        Assert.assertEquals(first, second);
    }
}
