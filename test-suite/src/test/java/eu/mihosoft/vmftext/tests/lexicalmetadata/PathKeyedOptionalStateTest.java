package eu.mihosoft.vmftext.tests.lexicalmetadata;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.CodeElement;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.NestedUnnamedModel;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.OptionalState;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.parser.NestedUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.unparser.NestedUnnamedModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

public class PathKeyedOptionalStateTest {

    @Test
    public void parsedModelContainsPathKeyedOptionalStates() {
        String source = "r1 (), r2 (test123), r3 (), r5 (a b c)";
        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(source);

        long stateCount = model.getRoot().vmf().content().stream(CodeElement.class)
                .filter(e -> e.getLexicalInfo() != null)
                .flatMap(e -> e.getLexicalInfo().getOptionalStates().stream())
                .peek(s -> Assert.assertTrue(
                        "grammar element path expected, got: " + s.getGrammarElementPath(),
                        s.getGrammarElementPath().startsWith("/")))
                .count();

        Assert.assertTrue("expected recorded optional states", stateCount > 0);

        // the legacy positional view must stay derivable from the keyed states
        model.getRoot().vmf().content().stream(CodeElement.class)
                .filter(e -> e.getLexicalInfo() != null)
                .forEach(e -> Assert.assertEquals(
                        e.getLexicalInfo().getOptionalStates().stream()
                                .map(OptionalState::isPresent)
                                .collect(Collectors.toList()),
                        new java.util.ArrayList<>(e.getLexicalInfo().getOptionalSymbols())));
    }

    @Test
    public void pathKeyedStatesAreAuthoritativeOverPositional() {
        String[] sources = {
                "r1, r2",
                "r1 (), r2",
                "r1, r2 (test123), r3 (), r4, r5 ()",
                "r1 (), r2 (test123), r3 (abc), r4 (def), r5 (a b c)"
        };
        for (String source : sources) {
            NestedUnnamedModel model = new NestedUnnamedModelParser().parse(source);

            // corrupt every legacy positional carrier; only the path-keyed
            // typed states remain intact
            model.getRoot().vmf().content().stream(CodeElement.class).forEach(e -> {
                if (e.getLexicalInfo() != null) {
                    e.getLexicalInfo().getOptionalSymbols().clear();
                }
                if (e.getPayload() instanceof Map) {
                    ((Map<?, ?>) e.getPayload()).keySet()
                            .removeIf("vmf-text:optionalSymbols"::equals);
                }
            });

            Assert.assertEquals(source, new NestedUnnamedModelUnparser().unparse(model));
        }
    }

    @Test
    public void pathKeyedRoundTripSurvivesWithoutLexicalPayloadEntries() {
        String source = "r1 (), r2 (test123), r3 (), r4 (def), r5 (a b)";
        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(source);

        model.getRoot().vmf().content().stream(CodeElement.class)
                .map(CodeElement::getPayload)
                .filter(p -> p instanceof Map)
                .map(p -> (Map<?, ?>) p)
                .forEach(p -> p.keySet().removeIf(k ->
                        k instanceof String && ((String) k).startsWith("vmf-text:")));

        Assert.assertEquals(source, new NestedUnnamedModelUnparser().unparse(model));
    }
}
