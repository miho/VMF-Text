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

import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.RuleMappings;
import eu.mihosoft.vmf.vmftext.grammar.TypeMappings;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Assert;
import org.junit.Test;

/**
 * Model-level tests for the parser-rule-map redirect pass (issue #1,
 * {@link RuleMapModelRewriter}). A property typed by a single-alternative
 * wrapper rule is flattened to the mapped target model type.
 */
public class RuleMapRedirectTest {

    // expression is a single-alternative wrapper (# valueExpression) over
    // numberLiteral; program.expr references it as a top-level labeled element.
    private static final String GRAMMAR =
            "grammar TM;\n"
                    + "program: expr=expression EOF;\n"
                    + "expression: value=numberLiteral # valueExpression;\n"
                    + "numberLiteral: value=INT # intLiteral | value=DOUBLE # doubleLiteral;\n"
                    + "INT: [0-9]+;\n"
                    + "DOUBLE: [0-9]+ '.' [0-9]*;\n"
                    + "WS: [ \\t\\r\\n]+ -> channel(HIDDEN);\n";

    private static final String RULE_MAP =
            "RuleMap() {\n"
                    + "  (first: ValueExpression -> second: NumberLiteral) = {\n"
                    + "      'first.getValue()',\n"
                    + "      'ValueExpression.newBuilder().withValue(second).build()'\n"
                    + "  }\n"
                    + "}\n";

    private static GrammarModel buildModel(String grammar) {
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(CharStreams.fromString(grammar));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
        ParserRuleContext tree = parser.grammarSpec();

        GrammarToModelListener listener = new GrammarToModelListener(TypeMappings.newInstance());
        new ParseTreeWalker().walk(listener, tree);
        return listener.getModel();
    }

    private static RuleClass ruleClass(GrammarModel m, String name) {
        return m.getRuleClasses().stream().filter(c -> name.equals(c.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("no rule class '" + name + "'"));
    }

    private static Property property(RuleClass cls, String name) {
        return cls.getProperties().stream().filter(p -> name.equals(p.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("no property '" + name + "'"));
    }

    @Test
    public void singleAltWrapperIsRedirectedToTarget() {
        GrammarModel model = buildModel(GRAMMAR);

        // before: program.expr is the wrapper type 'expression'
        Property before = property(ruleClass(model, "program"), "expr");
        Assert.assertTrue(before.getType().isRuleType());
        Assert.assertFalse(before.getType().isArrayType());
        Assert.assertEquals("expression", before.getType().getName());

        RuleMappings mappings = RuleMappings.newInstance();
        GrammarMetaInformationUtil.getRuleMapping(mappings, RULE_MAP);
        model.setRuleMappings(mappings);

        RuleMapModelRewriter.apply(model);

        // after: flattened to the mapped target 'numberLiteral'
        Property after = property(ruleClass(model, "program"), "expr");
        Assert.assertTrue("still a rule type", after.getType().isRuleType());
        Assert.assertFalse("still scalar", after.getType().isArrayType());
        Assert.assertEquals("redirected to NumberLiteral", "numberLiteral",
                after.getType().getName());
    }

    @Test
    public void withoutRuleMapTypeIsUnchanged() {
        GrammarModel model = buildModel(GRAMMAR);
        model.setRuleMappings(RuleMappings.newInstance()); // no maps declared

        RuleMapModelRewriter.apply(model);

        Property after = property(ruleClass(model, "program"), "expr");
        Assert.assertEquals("expression", after.getType().getName());
    }
}
