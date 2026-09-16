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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Unit tests for {@link AutoLabeler}: plain lower-camel names (no {@code Node}
 * suffix), keyword-context disambiguation, and optional report-file output.
 */
public class AutoLabelerTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void keywordContextDisambiguatesDuplicateAndDropsNodeSuffix() throws Exception {
        String grammar = ""
                + "grammar KeywordCtx;\n"
                + "\n"
                + "ifStatement\n"
                + "    : 'if' '(' expression ')' statement ('else' statement)?\n"
                + "    ;\n"
                + "\n"
                + "statement\n"
                + "    : IDENTIFIER ';'\n"
                + "    ;\n"
                + "\n"
                + "expression\n"
                + "    : IDENTIFIER\n"
                + "    ;\n"
                + "\n"
                + "IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";

        File grammarFile = tmp.newFile("KeywordCtx.g4");
        Files.write(grammarFile.toPath(), grammar.getBytes(StandardCharsets.UTF_8));

        File reportFile = new File(tmp.getRoot(), "reports/autolabel-report.txt");
        File rewritten = AutoLabeler.rewrite(grammarFile, true, reportFile);
        String text = new String(Files.readAllBytes(rewritten.toPath()), StandardCharsets.UTF_8);

        Assert.assertTrue("keyword context should produce elseStatement=",
                text.contains("elseStatement=") || text.contains("elseStatement"));
        Assert.assertFalse("parser-rule refs must not receive a Node suffix",
                text.contains("statementNode") || text.contains("StatementNode"));
        Assert.assertTrue("then-branch uses PascalCase Statement= to avoid ANTLR error 69",
                text.contains("Statement="));
        Assert.assertFalse("plain lower-camel statement= must not be used when it collides with the rule",
                text.matches("(?s).*[^a-zA-Z]statement=statement.*"));

        Assert.assertTrue("report file should be written", reportFile.isFile());
        String report = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertTrue(report.contains("KeywordCtx.g4"));
        Assert.assertTrue(report.contains("elseStatement") || report.contains("statement"));
        Assert.assertTrue(report.contains("explicit ANTLR labels always win"));
    }
}
