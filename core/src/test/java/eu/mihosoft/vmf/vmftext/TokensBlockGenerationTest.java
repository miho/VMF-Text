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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Python-trial regression through the full pipeline: a {@code tokens {}} block
 * and named actions before the first parser rule used to leave the bundled
 * grammar lexer in "lexer rule" state, so the injected {@code locals [...]} of
 * the first rule was read as a character set and generation failed with
 * {@code mismatched input '[...]' expecting BEGIN_ARGUMENT}.
 */
public class TokensBlockGenerationTest {

    @Test
    public void tokensBlockAndNamedActionsBeforeTheFirstRuleGenerate() throws Exception {
        File tmp = Files.createTempDirectory("vmf-text-tokens").toFile();
        tmp.deleteOnExit();

        File grammar = new File(tmp, "Tok.g4");
        Files.write(grammar.toPath(),
                ("grammar Tok;\n"
                        + "tokens { INDENT, DEDENT }\n"
                        + "@parser::members { boolean always() { return true; } }\n"
                        + "@lexer::members { int depth = 0; }\n"
                        + "root : items+=item* EOF ;\n"
                        + "item : name=ID NEWLINE ;\n"
                        + "ID : [a-z]+ ;\n"
                        + "NEWLINE : '\\n' ;\n"
                        + "WS : [ \\t]+ -> channel(HIDDEN) ;\n").getBytes(StandardCharsets.UTF_8));

        File out = new File(tmp, "out");
        Assert.assertTrue(out.mkdirs());

        VMFText.generate(grammar, "tok.pkg", out); // must not throw

        Assert.assertTrue("generated parser missing",
                new File(out, "tok/pkg/parser/TokModelParser.java").isFile());
    }
}
