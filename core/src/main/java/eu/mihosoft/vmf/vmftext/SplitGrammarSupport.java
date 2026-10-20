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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Split grammars (#39): a {@code parser grammar X} with
 * {@code options { tokenVocab = L; }} and the {@code lexer grammar L} next to
 * it are processed as one unit. The parser grammar is the input; the lexer
 * grammar is generated into the same package and supplies the lexer rules.
 */
public final class SplitGrammarSupport {

    private static final Pattern HEADER = Pattern.compile("(?m)^\\s*(lexer|parser)?\\s*grammar\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;");
    private static final Pattern TOKEN_VOCAB = Pattern.compile("tokenVocab\\s*=\\s*([A-Za-z_][A-Za-z0-9_]*)");

    private SplitGrammarSupport() {
        throw new AssertionError("no instances");
    }

    private static String stripComments(String text) {
        return text.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }

    /** @return {@code true} if the file declares a {@code lexer grammar} */
    public static boolean isLexerGrammar(File grammar) {
        try {
            Matcher m = HEADER.matcher(stripComments(new String(Files.readAllBytes(grammar.toPath()), StandardCharsets.UTF_8)));
            return m.find() && "lexer".equals(m.group(1));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @return the lexer grammar a {@code parser grammar} refers to via
     * {@code tokenVocab} (looked up next to the parser grammar), or
     * {@code null} for a combined grammar
     */
    public static File lexerGrammarOf(File grammar) {
        String text;
        try {
            text = stripComments(new String(Files.readAllBytes(grammar.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
        Matcher header = HEADER.matcher(text);
        if(!header.find()) {
            return null;
        }
        if("lexer".equals(header.group(1))) {
            throw new VMFTextGenerationException("'" + grammar + "' is a lexer grammar; it is generated"
                    + " through the tokenVocab option of its parser grammar and cannot be processed on its own");
        }
        if(!"parser".equals(header.group(1))) {
            return null;
        }
        Matcher vocab = TOKEN_VOCAB.matcher(text);
        if(!vocab.find()) {
            throw new VMFTextGenerationException("parser grammar '" + grammar + "' has no tokenVocab option;"
                    + " VMF-Text needs the lexer grammar next to it");
        }
        File lexer = new File(grammar.getAbsoluteFile().getParentFile(), vocab.group(1) + ".g4");
        if(!lexer.isFile()) {
            throw new VMFTextGenerationException("tokenVocab '" + vocab.group(1) + "' of '" + grammar
                    + "': expected the lexer grammar at " + lexer);
        }
        return lexer;
    }

    /**
     * Makes the lexer grammar available for the parser grammar's ANTLR runs:
     * generates {@code <Lexer>.tokens} with the plain ANTLR tool and copies it
     * together with the lexer grammar next to the (rewritten) parser grammar.
     *
     * @return the folder to pass as ANTLR's {@code -lib} directory
     */
    public static Path prepareLexer(File lexerGrammar, File rewrittenParserGrammar) throws IOException {
        Path lib = rewrittenParserGrammar.getAbsoluteFile().getParentFile().toPath();
        Files.copy(lexerGrammar.toPath(), lib.resolve(lexerGrammar.getName()), StandardCopyOption.REPLACE_EXISTING);
        Path scratch = Files.createTempDirectory("vmf-text-lexer");
        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{
                lib.resolve(lexerGrammar.getName()).toString(), "-o", scratch.toString()});
        tool.processGrammarsOnCommandLine();
        if(tool.errMgr.getNumErrors() > 0) {
            throw new VMFTextGenerationException("ANTLR reported " + tool.errMgr.getNumErrors()
                    + " error(s) in lexer grammar '" + lexerGrammar + "'");
        }
        String tokensFile = lexerGrammar.getName().replaceAll("\\.g4$", "") + ".tokens";
        Path tokens = scratch.resolve(tokensFile);
        if(!Files.isRegularFile(tokens)) {
            throw new VMFTextGenerationException("ANTLR did not produce " + tokens + " for '" + lexerGrammar + "'");
        }
        Files.copy(tokens, lib.resolve(tokensFile), StandardCopyOption.REPLACE_EXISTING);
        return lib;
    }
}
