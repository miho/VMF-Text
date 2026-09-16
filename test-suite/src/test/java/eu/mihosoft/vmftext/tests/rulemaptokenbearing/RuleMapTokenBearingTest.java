/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.mihosoft.vmftext.tests.rulemaptokenbearing;

import eu.mihosoft.vmftext.tests.rulemaptokenbearing.parser.RuleMapTokenBearingModelParser;
import eu.mihosoft.vmftext.tests.rulemaptokenbearing.unparser.RuleMapTokenBearingModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * End-to-end tests for RuleMap token-bearing wrappers (issue #32). The wrapper
 * {@code expr: '(' value=numberLiteral ')'} contributes its own terminals;
 * shell LexicalInfo on the flattened {@code NumberLiteral} restores them on
 * unparse for byte-exact round-trip.
 */
public class RuleMapTokenBearingTest {

    /** The flattened model exposes the target type directly (compile-level proof). */
    @Test
    public void modelIsFlattenedToTarget() {
        RuleMapTokenBearingModelParser parser = new RuleMapTokenBearingModelParser();
        RuleMapTokenBearingModel model = parser.parse("(1);");
        Program program = model.getRoot();

        Assert.assertEquals(1, program.getExpressions().size());
        NumberLiteral first = program.getExpressions().get(0);
        Assert.assertNotNull(first);
        Assert.assertNotNull(first.getRuleMapShellLexicalInfo());
    }

    /** Parsed token-bearing wrappers unparse byte-exact via shell LexicalInfo. */
    @Test
    public void roundTripIsByteExact() {
        RuleMapTokenBearingModelParser parser = new RuleMapTokenBearingModelParser();
        RuleMapTokenBearingModelUnparser unparser = new RuleMapTokenBearingModelUnparser();

        String[] codes = {
                "( 1 ) ; (  2.5\n) ;",
                "(/*c*/1) ;",
                "(\t1 /*c*/) ;"
        };

        for (String code : codes) {
            RuleMapTokenBearingModel model = parser.parse(code);
            String out = unparser.unparse(model);
            Assert.assertEquals(code, out);
        }
    }

    /**
     * Programmatic targets without shell LexicalInfo still unparse to valid
     * text (conservative separators; not necessarily byte-exact).
     */
    @Test
    public void programmaticTargetUnparsesWithoutShell() {
        IntLiteral lit = IntLiteral.newBuilder().withValue("1").build();
        Assert.assertNull(lit.getRuleMapShellLexicalInfo());

        Program program = Program.newBuilder().build();
        program.getExpressions().add(lit);

        RuleMapTokenBearingModelUnparser unparser = new RuleMapTokenBearingModelUnparser();
        String out = unparser.unparse(RuleMapTokenBearingModel.newBuilder().withRoot(program).build());

        Assert.assertNotNull(out);
        Assert.assertFalse(out.isEmpty());
        // Re-parse must succeed and yield the same semantic value.
        RuleMapTokenBearingModelParser parser = new RuleMapTokenBearingModelParser();
        RuleMapTokenBearingModel round = parser.parse(out);
        Assert.assertEquals(1, round.getRoot().getExpressions().size());
        NumberLiteral parsed = round.getRoot().getExpressions().get(0);
        Assert.assertTrue(parsed instanceof IntLiteral);
        Assert.assertEquals("1", ((IntLiteral) parsed).getValue());
    }
}
