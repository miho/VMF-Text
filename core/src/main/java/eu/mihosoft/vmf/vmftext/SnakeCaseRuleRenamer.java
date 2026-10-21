/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
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
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Renames snake_case parser rules to camelCase in the grammar VMF-Text
 * processes, so the model classes, the ANTLR contexts and the unparsers share
 * one spelling (#63): {@code file_input} becomes {@code fileInput} and its
 * model class {@code FileInput}, where the class used to be {@code File_input}
 * next to camelCase properties such as {@code getSimpleStmts()}. Only names
 * with an underscore between word characters change; {@code subscript_}
 * keeps its trailing underscore. A rule whose camelCase name is a rule
 * already keeps its name (so do two snake_case rules that would map to the
 * same name). Rule definitions and rule references are rewritten; rule names
 * inside actions and predicates are not. The lexer grammar of a split pair
 * is not touched. Metadata that names rules — {@code Root(rule=...)} and the
 * apply-to lists of type and rule maps — accepts either spelling, see
 * {@link #rename(String)}.
 */
final class SnakeCaseRuleRenamer {

    private static final Pattern SNAKE = Pattern.compile("[a-z][A-Za-z0-9]*(?:_[A-Za-z0-9]+)+");

    private SnakeCaseRuleRenamer() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * The spelling a parser rule has after this pass: camelCase for a
     * snake_case name, the name itself otherwise.
     */
    static String rename(String ruleName) {
        if(ruleName == null || !SNAKE.matcher(ruleName).matches()) {
            return ruleName;
        }
        return AutoLabeler.camelCase(ruleName);
    }

    static File rewrite(File grammar) throws IOException {
        try(FileInputStream codeStream = new FileInputStream(grammar)) {
            CharStream input = CharStreams.fromStream(codeStream);
            ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
            ParserRuleContext tree = parser.grammarSpec();

            Set<String> ruleNames = new LinkedHashSet<>();
            ParseTreeWalker.DEFAULT.walk(new ANTLRv4ParserBaseListener() {
                @Override
                public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
                    if(ctx.RULE_REF() != null) {
                        ruleNames.add(ctx.RULE_REF().getText());
                    }
                }
            }, tree);

            Map<String, String> renames = new LinkedHashMap<>();
            Set<String> taken = new LinkedHashSet<>(ruleNames);
            for(String name : ruleNames) {
                String renamed = rename(name);
                if(renamed.equals(name)) {
                    continue;
                }
                if(taken.contains(renamed)) {
                    Logger.warn("VMF-Text: parser rule '" + name + "' keeps its name, '" + renamed + "' exists already");
                    continue;
                }
                taken.add(renamed);
                renames.put(name, renamed);
            }
            if(renames.isEmpty()) {
                return grammar;
            }

            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            ParseTreeWalker.DEFAULT.walk(new ANTLRv4ParserBaseListener() {
                @Override
                public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
                    if(ctx.RULE_REF() != null && renames.containsKey(ctx.RULE_REF().getText())) {
                        rewriter.replace(ctx.RULE_REF().getSymbol(), renames.get(ctx.RULE_REF().getText()));
                    }
                }

                @Override
                public void enterRuleref(ANTLRv4Parser.RulerefContext ctx) {
                    if(ctx.RULE_REF() != null && renames.containsKey(ctx.RULE_REF().getText())) {
                        rewriter.replace(ctx.RULE_REF().getSymbol(), renames.get(ctx.RULE_REF().getText()));
                    }
                }
            }, tree);

            Logger.debug("------------------------------------------------------");
            Logger.debug("Rule names: " + renames.size() + " snake_case parser rule(s) renamed to camelCase: " + renames);
            Logger.debug("------------------------------------------------------");

            Path dir = Files.createTempDirectory("vmf-text-rule-names");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));
            return grammarOut;
        }
    }
}
