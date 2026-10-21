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

    /**
     * #64 (grammars-v4 JavaScript): a reference to the parser rule {@code identifier} cannot be
     * labeled {@code identifier} (ANTLR error 69, rule name) nor {@code Identifier} when a token of
     * that name exists (error 70). With a preceding keyword the keyword-context name is used, and a
     * reference to the token itself is labeled without conflict; ANTLR accepts the result.
     */
    @Test
    public void labelAvoidsTokenNamesAsWellAsRuleNames() throws Exception {
        String grammar = ""
                + "grammar TokenClash;\n"
                + "\n"
                + "unit : 'let' identifier '=' Identifier ';' EOF ;\n"
                + "\n"
                + "identifier : Identifier ;\n"
                + "\n"
                + "Identifier : [a-z]+ ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";

        File dir = tmp.newFolder("tokenclash");
        File grammarFile = new File(dir, "TokenClash.g4");
        Files.write(grammarFile.toPath(), grammar.getBytes(StandardCharsets.UTF_8));

        File rewritten = AutoLabeler.rewrite(grammarFile, false, null);
        String text = new String(Files.readAllBytes(rewritten.toPath()), StandardCharsets.UTF_8);

        // neither the rule name nor the token name is used as a label
        Assert.assertFalse(text, text.matches("(?s).*[^A-Za-z]Identifier=.*"));
        Assert.assertFalse(text, text.matches("(?s).*[^A-Za-z]identifier=.*"));
        // #73: the token reference says what it holds; the keyword-context name still wins for the rule
        Assert.assertTrue(text, text.contains("identifierToken=Identifier"));
        Assert.assertTrue(text, text.contains("letIdentifier=identifier"));

        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{rewritten.getAbsolutePath(), "-o", dir.getAbsolutePath()});
        tool.processGrammarsOnCommandLine();
        Assert.assertEquals("ANTLR accepts the labels: " + text, 0, tool.errMgr.getNumErrors());
    }

    /**
     * #73: without keyword context a reference to the rule is {@code identifierNode} (a second one in
     * the same type {@code identifierNode2}) and a reference to the token {@code identifierToken}.
     */
    @Test
    public void ruleAndTokenReferencesAreNodeAndTokenWhenRuleAndTokenShareTheName() throws Exception {
        String grammar = ""
                + "grammar TokenClash2;\n"
                + "\n"
                + "unit : identifier ':' identifier EOF ;\n"
                + "\n"
                + "identifier : Identifier ;\n"
                + "\n"
                + "Identifier : [a-z]+ ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";

        File dir = tmp.newFolder("tokenclash2");
        File grammarFile = new File(dir, "TokenClash2.g4");
        Files.write(grammarFile.toPath(), grammar.getBytes(StandardCharsets.UTF_8));

        File rewritten = AutoLabeler.rewrite(grammarFile, false, null);
        String text = new String(Files.readAllBytes(rewritten.toPath()), StandardCharsets.UTF_8);

        Assert.assertTrue(text, text.contains("identifierNode=identifier"));
        Assert.assertTrue(text, text.contains("identifierNode2=identifier"));
        Assert.assertTrue(text, text.contains("identifierToken=Identifier"));

        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{rewritten.getAbsolutePath(), "-o", dir.getAbsolutePath()});
        tool.processGrammarsOnCommandLine();
        Assert.assertEquals("ANTLR accepts the labels: " + text, 0, tool.errMgr.getNumErrors());
    }

    /**
     * #73: a rule that repeats an alternative label (grammars-v4 JavaScript's three
     * {@code # NewExpression} alternatives) generates one type per label. Names are counted per
     * label, so unrelated alternatives keep {@code left} / {@code right} instead of numbering
     * across the rule ({@code AdditiveExpression.getSingleExpression15()} before).
     */
    @Test
    public void repeatedAlternativeLabelCountsNamesPerLabel() throws Exception {
        String grammar = ""
                + "grammar RepeatedLabel;\n"
                + "\n"
                + "expr\n"
                + "    : expr '+' expr      # Add\n"
                + "    | expr '*' expr      # Mul\n"
                + "    | 'new' expr         # New\n"
                + "    | 'new' expr '(' ')' # New\n"
                + "    | ID                 # Ref\n"
                + "    ;\n"
                + "\n"
                + "ID : [a-z]+ ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";

        File dir = tmp.newFolder("repeatedlabel");
        File grammarFile = new File(dir, "RepeatedLabel.g4");
        Files.write(grammarFile.toPath(), grammar.getBytes(StandardCharsets.UTF_8));

        File rewritten = AutoLabeler.rewrite(grammarFile, false, null);
        String text = new String(Files.readAllBytes(rewritten.toPath()), StandardCharsets.UTF_8);

        Assert.assertTrue(text, text.contains("left=expr '+' right=expr"));
        Assert.assertTrue(text, text.contains("left=expr '*' right=expr"));
        // the two New alternatives are one type: their names stay unique between them
        Assert.assertEquals(text, 1, text.split("newExpr=expr", -1).length - 1);
        Assert.assertTrue(text, text.contains("'new' newExpr=expr"));

        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{rewritten.getAbsolutePath(), "-o", dir.getAbsolutePath()});
        tool.processGrammarsOnCommandLine();
        Assert.assertEquals("ANTLR accepts the labels: " + text, 0, tool.errMgr.getNumErrors());
    }

    /** #73: when the suffixed name is a rule of its own, the numeric suffix is the fallback. */
    @Test
    public void clashFallsBackToANumberWhenTheSuffixedNameIsTaken() throws Exception {
        String grammar = ""
                + "grammar TokenClash3;\n"
                + "\n"
                + "unit : identifier ':' identifierNode EOF ;\n"
                + "\n"
                + "identifier : Identifier ;\n"
                + "\n"
                + "identifierNode : '#' Identifier ;\n"
                + "\n"
                + "Identifier : [a-z]+ ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";

        File dir = tmp.newFolder("tokenclash3");
        File grammarFile = new File(dir, "TokenClash3.g4");
        Files.write(grammarFile.toPath(), grammar.getBytes(StandardCharsets.UTF_8));

        File rewritten = AutoLabeler.rewrite(grammarFile, false, null);
        String text = new String(Files.readAllBytes(rewritten.toPath()), StandardCharsets.UTF_8);

        Assert.assertTrue(text, text.contains("identifier2=identifier"));
        Assert.assertFalse(text, text.contains("identifierNode=identifier "));

        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{rewritten.getAbsolutePath(), "-o", dir.getAbsolutePath()});
        tool.processGrammarsOnCommandLine();
        Assert.assertEquals("ANTLR accepts the labels: " + text, 0, tool.errMgr.getNumErrors());
    }
}
