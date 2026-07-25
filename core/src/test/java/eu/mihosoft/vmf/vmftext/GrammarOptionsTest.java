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
 * Regression tests for grammar {@code options { ... }} handling, in particular
 * the {@code superClass} option (issue #14).
 *
 * <p>These drive the {@link GrammarToModelListener} directly with the bundled
 * ANTLRv4 meta-grammar, mirroring the model-conversion path in
 * {@code VMFText.convertGrammarToModel}, so they exercise option capture without
 * running the full code-generation pipeline.</p>
 */
public class GrammarOptionsTest {

    /** Lex/parse the given grammar text and walk the model listener. */
    private static GrammarModel convert(String grammarText) {
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(CharStreams.fromString(grammarText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
        ParserRuleContext tree = parser.grammarSpec();

        GrammarToModelListener listener = new GrammarToModelListener(TypeMappings.newInstance());
        new ParseTreeWalker().walk(listener, tree);
        return listener.getModel();
    }

    /** A plain grammar-level {@code superClass} option must be captured. */
    @Test
    public void grammarLevelSuperClassIsCaptured() {
        GrammarModel model = convert(
                "grammar OptTest;\n"
                        + "options { superClass = MyBase; }\n"
                        + "root : A B EOF ;\n"
                        + "A : 'a' ;\n"
                        + "B : 'b' ;\n");

        Assert.assertNotNull("options should be captured", model.getOptions());
        Assert.assertEquals("MyBase", model.getOptions().getSuperClassName());
    }

    /**
     * A grammar-level {@code superClass} must survive a rule-level
     * {@code options { ... }} block that is walked afterwards. Before the fix
     * for issue #14, {@code enterOptionsSpec} re-created the {@code Options}
     * object on every options block and silently dropped the {@code superClass}.
     */
    @Test
    public void superClassSurvivesRuleLevelOptions() {
        GrammarModel model = convert(
                "grammar OptTest;\n"
                        + "options { superClass = MyBase; }\n"
                        + "root options { greedy = false; }\n"
                        + "  : A B EOF\n"
                        + "  ;\n"
                        + "A : 'a' ;\n"
                        + "B : 'b' ;\n");

        Assert.assertNotNull("options should still be present", model.getOptions());
        Assert.assertEquals("rule-level options must not clobber grammar-level superClass",
                "MyBase", model.getOptions().getSuperClassName());
    }
}
