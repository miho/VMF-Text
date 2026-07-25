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
package eu.mihosoft.vmftext.tests.rulemap;

import eu.mihosoft.vmftext.tests.rulemap.parser.RuleMapModelParser;
import eu.mihosoft.vmftext.tests.rulemap.unparser.RuleMapModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * End-to-end tests for parser-rule maps / model rewriting (issue #1). The
 * {@code RuleMap} in {@code RuleMap.g4} flattens the single-alternative
 * {@code expression} wrapper so {@code Program.expressions} is a list of
 * {@code NumberLiteral} directly, while parse/unparse bridge the two types.
 */
public class RuleMapTest {

    /** The flattened model exposes the target type directly (compile-level proof). */
    @Test
    public void modelIsFlattenedToTarget() {
        RuleMapModelParser parser = new RuleMapModelParser();
        RuleMapModel model = parser.parse("1;2;3;");
        Program program = model.getRoot();

        Assert.assertEquals(3, program.getExpressions().size());
        // element type is NumberLiteral (not Expression/ValueExpression) - this
        // assignment only compiles if the wrapper was flattened.
        NumberLiteral first = program.getExpressions().get(0);
        Assert.assertNotNull(first);
    }

    /** Parsed model unparses byte-exact (reconstruction + lexical preservation). */
    @Test
    public void roundTripIsByteExact() {
        RuleMapModelParser parser = new RuleMapModelParser();
        RuleMapModelUnparser unparser = new RuleMapModelUnparser();

        String code = "1 ; 2.5 ; 3 ;";
        RuleMapModel model = parser.parse(code);
        String out = unparser.unparse(model);

        Assert.assertEquals(code, out);
    }
}
