package eu.mihosoft.vmftext.tests.lexicalmetadata;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.CodeElement;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.NestedUnnamedModel;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.OptionalState;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.parser.NestedUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.unparser.NestedUnnamedModelUnparser;
import org.junit.Assert;
import org.junit.Test;

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

        // Every recorded state carries a non-negative occurrence index.
        model.getRoot().vmf().content().stream(CodeElement.class)
                .filter(e -> e.getLexicalInfo() != null)
                .flatMap(e -> e.getLexicalInfo().getOptionalStates().stream())
                .forEach(s -> Assert.assertTrue(s.getOccurrenceIndex() >= 0));
    }

    @Test
    public void pathKeyedRoundTripIsExact() {
        String[] sources = {
                "r1, r2",
                "r1 (), r2",
                "r1, r2 (test123), r3 (), r4, r5 ()",
                "r1 (), r2 (test123), r3 (abc), r4 (def), r5 (a b c)"
        };
        NestedUnnamedModelUnparser unparser = new NestedUnnamedModelUnparser();
        NestedUnnamedModelParser parser = new NestedUnnamedModelParser();
        for (String source : sources) {
            Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
        }
    }

    @Test
    public void flippingPresentFalseMakesOptionalGroupDisappear() {
        String source = "r1 (), r2 (test123)";
        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(source);
        CodeElement withGroup = model.getRoot().vmf().content().stream(CodeElement.class)
                .filter(e -> e.getLexicalInfo() != null
                        && e.getLexicalInfo().getOptionalStates().stream()
                        .anyMatch(OptionalState::isPresent))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no present optional"));

        for (OptionalState s : withGroup.getLexicalInfo().getOptionalStates()) {
            s.setPresent(false);
        }

        String out = new NestedUnnamedModelUnparser().unparse(model);
        Assert.assertFalse(out.contains("()"));
    }
}
