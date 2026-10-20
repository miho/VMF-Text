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
package eu.mihosoft.vmf.vmftext.grammar.antlr4;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * Python-trial regression: a {@code tokens {}} block (or another prequel
 * construct) before the first parser rule must not leave the grammar lexer in
 * "lexer rule" state, where the first rule's {@code locals [...]} block would
 * be lexed as a character set ("mismatched input '[...]' expecting
 * BEGIN_ARGUMENT").
 */
public class LexerAdaptorPrequelTest {

    private static ANTLRv4Parser.GrammarSpecContext parse(String grammar) {
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(CharStreams.fromString(grammar));
        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));
        ANTLRv4Parser.GrammarSpecContext spec = parser.grammarSpec();
        Assert.assertEquals(grammar, 0, parser.getNumberOfSyntaxErrors());
        return spec;
    }

    private static ANTLRv4Parser.ParserRuleSpecContext firstParserRule(ANTLRv4Parser.GrammarSpecContext spec) {
        return spec.rules().ruleSpec(0).parserRuleSpec();
    }

    @Test
    public void tokensBlockBeforeTheFirstRuleKeepsLocalsAnArgumentBlock() {
        ANTLRv4Parser.GrammarSpecContext spec = parse(
                "grammar T;\n"
                        + "tokens { INDENT, DEDENT }\n"
                        + "root locals [int depth = 0] : A INDENT B DEDENT EOF ;\n"
                        + "A : 'a' ;\n"
                        + "B : [b-c] ;\n");
        Assert.assertNotNull(firstParserRule(spec).localsSpec());
    }

    @Test
    public void namedActionsAndOptionsBeforeTheFirstRuleAreTransparent() {
        ANTLRv4Parser.GrammarSpecContext spec = parse(
                "grammar T;\n"
                        + "options { superClass = Base; }\n"
                        + "tokens { X }\n"
                        + "channels { HIDDEN_STUFF }\n"
                        + "@parser::members { int n = 0; }\n"
                        + "@lexer::members { boolean f() { return true; } }\n"
                        + "root locals [int depth = 0] : A EOF ;\n"
                        + "A : [a-z]+ ;\n");
        Assert.assertNotNull(firstParserRule(spec).localsSpec());
    }

    @Test
    public void characterSetsInLexerRulesStillWork() {
        ANTLRv4Parser.GrammarSpecContext spec = parse(
                "grammar T;\n"
                        + "root : A EOF ;\n"
                        + "A : [a-z] [0-9]* ;\n");
        Assert.assertEquals(2, spec.rules().ruleSpec().size());
        Assert.assertNotNull(spec.rules().ruleSpec(1).lexerRuleSpec());
    }
}
