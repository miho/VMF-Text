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
package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.RuleMapEntry;
import eu.mihosoft.vmf.vmftext.grammar.RuleMapping;
import eu.mihosoft.vmf.vmftext.grammar.RuleMappings;
import org.junit.Assert;
import org.junit.Test;

/**
 * Foundation tests for parser-rule maps / model rewriting (issue #1): the
 * {@code RuleMap() { ... }} DSL is parsed into the {@link RuleMappings} model,
 * including source/target type names and both conversion expressions.
 */
public class RuleMappingTest {

    @Test
    public void ruleMapIsParsed() {
        RuleMappings mappings = RuleMappings.newInstance();

        // matches the prototype in test-suite .../typemappings/TypeMappings.g4
        String code =
                "RuleMap() {\n"
                        + "  (first: ValueExpression -> second: NumberLiteral) = {\n"
                        + "      'first.getValue()',\n"
                        + "      'ValueExpression.newBuilder().withValue(second).build()'\n"
                        + "  }\n"
                        + "}\n";

        GrammarMetaInformationUtil.getRuleMapping(mappings, code);

        Assert.assertEquals("one RuleMap block", 1, mappings.getRuleMappings().size());

        RuleMapping block = mappings.getRuleMappings().get(0);
        Assert.assertTrue("global (no applyTo)", block.getApplyToNames().isEmpty());
        Assert.assertEquals("one entry", 1, block.getEntries().size());

        RuleMapEntry e = block.getEntries().get(0);
        Assert.assertEquals("ValueExpression", e.getSourceName());
        Assert.assertEquals("NumberLiteral", e.getTargetName());
        Assert.assertEquals("first.getValue()", e.getSourceToTargetCode());
        Assert.assertEquals("ValueExpression.newBuilder().withValue(second).build()",
                e.getTargetToSourceCode());
    }

    @Test
    public void ruleMapCoexistsWithTypeMapAndScoping() {
        RuleMappings mappings = RuleMappings.newInstance();

        // a TypeMap and a scoped RuleMap in the same comment block; getRuleMapping
        // must ignore the TypeMap and honor applyTo scoping.
        String code =
                "TypeMap() {\n"
                        + "  (rule: INT -> type: java.lang.Integer) = {\n"
                        + "      toType:   'java.lang.Integer.parseInt(entry.getText())',\n"
                        + "      toString: 'entry.toString()'\n"
                        + "  }\n"
                        + "}\n"
                        + "RuleMap(program) {\n"
                        + "  (first: ValueExpression -> second: NumberLiteral) = {\n"
                        + "      'first.getValue()',\n"
                        + "      'ValueExpression.newBuilder().withValue(second).build()'\n"
                        + "  }\n"
                        + "}\n";

        GrammarMetaInformationUtil.getRuleMapping(mappings, code);

        Assert.assertEquals(1, mappings.getRuleMappings().size());
        RuleMapping block = mappings.getRuleMappings().get(0);
        Assert.assertEquals(1, block.getApplyToNames().size());
        Assert.assertEquals("program", block.getApplyToNames().get(0));
        Assert.assertTrue(mappings.mappingBySourceNameExists("program", "ValueExpression"));
        Assert.assertFalse(mappings.mappingBySourceNameExists("other", "ValueExpression"));
    }
}
