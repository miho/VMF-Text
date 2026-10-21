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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** #63: snake_case parser rules become camelCase; #62: an unqualified parser superClass is qualified. */
public class RuleNameAndSuperClassRewriteTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private File grammarFile(String name, String text) throws Exception {
        File f = tmp.newFile(name);
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void renameFunctionOnlyTouchesUnderscoresBetweenWords() {
        Assert.assertEquals("fileInput", SnakeCaseRuleRenamer.rename("file_input"));
        Assert.assertEquals("aBC", SnakeCaseRuleRenamer.rename("a_b_c"));
        Assert.assertEquals("subscript_", SnakeCaseRuleRenamer.rename("subscript_"));
        Assert.assertEquals("statement", SnakeCaseRuleRenamer.rename("statement"));
        Assert.assertEquals("simpleStmt", SnakeCaseRuleRenamer.rename("simpleStmt"));
        Assert.assertNull(SnakeCaseRuleRenamer.rename(null));
    }

    @Test
    public void definitionsAndReferencesAreRenamedActionsAreNot() throws Exception {
        String grammar = ""
                + "grammar Snake;\n"
                + "file_input : simple_stmt* EOF ;\n"
                + "simple_stmt : name=ID '=' value=ID ';' {System.out.println($simple_stmt.text);} ;\n"
                + "subscript_ : ID ;\n"
                + "ID : [a-z]+ ;\n"
                + "WS : [ \\t\\r\\n]+ -> channel(HIDDEN) ;\n";
        File rewritten = SnakeCaseRuleRenamer.rewrite(grammarFile("Snake.g4", grammar));
        String text = read(rewritten);
        Assert.assertTrue(text, text.contains("fileInput : simpleStmt* EOF ;"));
        Assert.assertTrue(text, text.contains("simpleStmt : name=ID"));
        Assert.assertTrue("the action keeps its text", text.contains("$simple_stmt.text"));
        Assert.assertTrue("a trailing underscore stays", text.contains("subscript_ : ID ;"));
        Assert.assertEquals("the file name is the grammar name", "Snake.g4", rewritten.getName());
    }

    @Test
    public void nothingToRenameReturnsTheInputAndCollisionsKeepTheName() throws Exception {
        File plain = grammarFile("Plain.g4", "grammar Plain;\nunit : ID ;\nID : [a-z]+ ;\n");
        Assert.assertSame(plain, SnakeCaseRuleRenamer.rewrite(plain));
        String grammar = ""
                + "grammar Clash;\n"
                + "unit : file_input fileInput ;\n"
                + "file_input : ID ;\n"
                + "fileInput : ID ;\n"
                + "ID : [a-z]+ ;\n";
        File clash = grammarFile("Clash.g4", grammar);
        Assert.assertSame("the camelCase name exists: nothing changes", clash, SnakeCaseRuleRenamer.rewrite(clash));
    }

    @Test
    public void unqualifiedSuperClassIsQualifiedWithTheParserPackage() throws Exception {
        String grammar = ""
                + "grammar Based;\n"
                + "options { superClass = MyBase; tokenVocab = Other; }\n"
                + "unit : ID ;\n"
                + "ID : [a-z]+ ;\n";
        File rewritten = SuperClassQualifier.rewrite(grammarFile("Based.g4", grammar), "my.pkg");
        String text = read(rewritten);
        // a string literal: 'parser' is an ANTLR keyword and cannot be part of a dotted identifier
        Assert.assertTrue(text, text.contains("superClass = 'my.pkg.parser.MyBase';"));
        Assert.assertTrue("other options are untouched", text.contains("tokenVocab = Other;"));

        File qualified = grammarFile("Qualified.g4", "grammar Qualified;\noptions { superClass = a.b.Base; }\nunit : ID ;\nID : [a-z]+ ;\n");
        Assert.assertSame(qualified, SuperClassQualifier.rewrite(qualified, "my.pkg"));
        File none = grammarFile("None.g4", "grammar None;\nunit : ID ;\nID : [a-z]+ ;\n");
        Assert.assertSame(none, SuperClassQualifier.rewrite(none, "my.pkg"));
        Assert.assertSame(none, SuperClassQualifier.rewrite(none, ""));
    }

    /** ANTLR itself must accept the quoted, qualified value and extend that class. */
    @Test
    public void antlrAcceptsTheQuotedQualifiedSuperClass() throws Exception {
        File dir = tmp.newFolder("antlr");
        File grammar = new File(dir, "Quoted.g4");
        Files.write(grammar.toPath(), ("grammar Quoted;\n"
                + "options { superClass = 'my.pkg.parser.MyBase'; }\n"
                + "unit : ID ;\n"
                + "ID : [a-z]+ ;\n").getBytes(StandardCharsets.UTF_8));
        org.antlr.v4.Tool tool = new org.antlr.v4.Tool(new String[]{grammar.getAbsolutePath(), "-o", dir.getAbsolutePath()});
        tool.processGrammarsOnCommandLine();
        Assert.assertEquals("no ANTLR errors", 0, tool.errMgr.getNumErrors());
        String parser = read(new File(dir, "QuotedParser.java"));
        Assert.assertTrue(parser, parser.contains("extends my.pkg.parser.MyBase"));
    }
}
