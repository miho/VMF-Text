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

import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rewrites list-labeled parser wildcards ({@code label+=.}) so ANTLR code
 * generation produces valid {@code List&lt;Token&gt;}-style labeled fields.
 *
 * <p>ANTLR 4.13.x generates broken Java for {@code label+=.}: it declares a
 * singular {@code Token label} and then calls {@code label.add(...)} (issue #8).
 * Non-list labels ({@code label=.}) are fine and left unchanged.</p>
 *
 * <p>The rewrite replaces {@code label+=.} with
 * {@code label+=vmftextAnyToken} and injects a synthetic parser rule:</p>
 *
 * <pre>{@code
 * vmftextAnyToken : . ;
 * }</pre>
 *
 * <p>That preserves parser-wildcard semantics (any token except EOF) while going
 * through ANTLR's working rule-reference list-label path. VMF-Text maps the
 * synthetic rule back to a token-typed {@code String}/{@code List&lt;String&gt;}
 * property so the public API matches the original label.</p>
 */
final class LabeledDotRewriter {

    /**
     * Synthetic parser rule used as a stand-in for list-labeled {@code .}.
     * Must be a valid ANTLR parser rule name (lowercase start, no leading '_').
     */
    static final String ANY_TOKEN_RULE = "vmftextAnyToken";

    private LabeledDotRewriter() {
        throw new AssertionError("Don't instantiate me!");
    }

    static File rewrite(File grammar) throws IOException {
        try (FileInputStream codeStream = new FileInputStream(grammar)) {
            CharStream input = CharStreams.fromStream(codeStream);

            ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

            ParserRuleContext tree = parser.grammarSpec();
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

            AtomicBoolean rewritten = new AtomicBoolean(false);
            AtomicInteger insertBeforeTokenIndex = new AtomicInteger(-1);
            AtomicBoolean ruleAlreadyPresent = new AtomicBoolean(false);

            ParseTreeWalker.DEFAULT.walk(new ANTLRv4ParserBaseListener() {
                @Override
                public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
                    if (ctx.RULE_REF() != null
                            && ANY_TOKEN_RULE.equals(ctx.RULE_REF().getText())) {
                        ruleAlreadyPresent.set(true);
                    }
                }

                @Override
                public void enterLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {
                    if (insertBeforeTokenIndex.get() < 0) {
                        insertBeforeTokenIndex.set(ctx.getStart().getTokenIndex());
                    }
                }

                @Override
                public void enterElement(ANTLRv4Parser.ElementContext ctx) {
                    if (!ParseTreeUtil.isLabeledElement(ctx)
                            || !ParseTreeUtil.isListAssignment(ctx)
                            || !ParseTreeUtil.isDotWildcard(ctx)) {
                        return;
                    }

                    ANTLRv4Parser.AtomContext atom = ctx.labeledElement().atom();
                    TerminalNode dot = atom != null ? atom.DOT() : null;
                    if (dot == null) {
                        // Fallback: rewrite the trailing '.' character token.
                        Token stop = atom.getStop();
                        if (stop != null && ".".equals(stop.getText())) {
                            rewriter.replace(stop, ANY_TOKEN_RULE);
                            rewritten.set(true);
                        }
                        return;
                    }

                    rewriter.replace(dot.getSymbol(), ANY_TOKEN_RULE);
                    rewritten.set(true);
                }
            }, tree);

            if (!rewritten.get()) {
                return grammar;
            }

            if (!ruleAlreadyPresent.get()) {
                String injection = "\n" + ANY_TOKEN_RULE + " : . ;\n";
                int idx = insertBeforeTokenIndex.get();
                if (idx >= 0) {
                    rewriter.insertBefore(idx, injection);
                } else {
                    // No lexer rules — append before EOF.
                    rewriter.insertAfter(tokens.size() - 1, injection);
                }
            }

            Logger.debug("------------------------------------------------------");
            Logger.debug("Labeled-dot rewrite: replaced list-labeled '.' with '"
                    + ANY_TOKEN_RULE + "' (ANTLR issue: +=. codegen)");
            Logger.debug("------------------------------------------------------");

            Path dir = Files.createTempDirectory("vmf-text-labeled-dot");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));
            return grammarOut;
        }
    }
}
