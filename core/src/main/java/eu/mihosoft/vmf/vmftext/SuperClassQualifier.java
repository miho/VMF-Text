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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Qualifies an unqualified parser {@code superClass} option with the parser
 * package: {@code options { superClass = JavaParserBase; }} becomes
 * {@code superClass = <package>.parser.JavaParserBase;} in the grammar
 * VMF-Text hands to ANTLR (#62). The base class used to be needed twice —
 * in {@code <package>.parser} for the model parser and in
 * {@code <package>.unparser.antlr4}, where the unparser's check grammar and
 * its own copy of the rewritten grammar are generated with the same option.
 * Now one copy in the parser package serves all three. A value that is
 * qualified already is left alone; lexer grammars are not touched (the
 * lexer of a split pair is generated once, into the parser package).
 */
final class SuperClassQualifier {

    private SuperClassQualifier() {
        throw new AssertionError("Don't instantiate me!");
    }

    static File rewrite(File grammar, String packageName) throws IOException {
        if(packageName == null || packageName.isEmpty()) {
            return grammar;
        }
        try(FileInputStream codeStream = new FileInputStream(grammar)) {
            CharStream input = CharStreams.fromStream(codeStream);
            ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
            ParserRuleContext tree = parser.grammarSpec();
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            AtomicBoolean rewritten = new AtomicBoolean(false);

            ParseTreeWalker.DEFAULT.walk(new ANTLRv4ParserBaseListener() {
                @Override
                public void enterOption(ANTLRv4Parser.OptionContext ctx) {
                    if(ctx.identifier() == null || ctx.optionValue() == null
                            || !"superClass".equals(ctx.identifier().getText())) {
                        return;
                    }
                    String value = ctx.optionValue().getText();
                    if(value.indexOf('.') >= 0 || value.startsWith("'") || value.startsWith("\"")) {
                        return; // qualified by the grammar author
                    }
                    // as a string literal: 'parser' is an ANTLR keyword, so the package
                    // cannot be written as a dotted identifier; ANTLR unquotes option values
                    rewriter.replace(ctx.optionValue().start, ctx.optionValue().stop,
                            "'" + packageName + ".parser." + value + "'");
                    rewritten.set(true);
                }
            }, tree);

            if(!rewritten.get()) {
                return grammar;
            }

            Logger.debug("------------------------------------------------------");
            Logger.debug("superClass option qualified with package '" + packageName + ".parser'");
            Logger.debug("------------------------------------------------------");

            Path dir = Files.createTempDirectory("vmf-text-superclass");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));
            return grammarOut;
        }
    }
}
