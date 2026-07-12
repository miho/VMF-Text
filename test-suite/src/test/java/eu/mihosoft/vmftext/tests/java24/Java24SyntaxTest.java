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
package eu.mihosoft.vmftext.tests.java24;

import eu.mihosoft.vmftext.tests.java24.parser.Java24ModelParser;
import eu.mihosoft.vmftext.tests.java24.unparser.Java24ModelUnparser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Java24SyntaxTest {

    @Test
    public void parsesRecordsSealedTypesPatternsSwitchAndTextBlocks() {
        String source = "" +
                "package demo;\n" +
                "sealed interface Shape permits Circle, Box { }\n" +
                "record Circle(double radius) implements Shape { }\n" +
                "non-sealed class Box implements Shape { }\n" +
                "class Demo {\n" +
                "    static String describe(Shape shape) {\n" +
                "        return switch (shape) {\n" +
                "            case Circle(double radius) when radius > 0 -> \"\"\"\n" +
                "                    circle\n" +
                "                    \"\"\";\n" +
                "            case Box box -> \"box\";\n" +
                "        };\n" +
                "    }\n" +
                "}\n";

        Java24Model model = assertExactRoundTrip(source);

        Assert.assertEquals(1, model.vmf().content().stream(RecordDeclaration.class).count());
        Assert.assertEquals(2, model.vmf().content().stream(SwitchRule.class).count());
        Assert.assertEquals(1, model.vmf().content().stream(TextBlockLiteral.class).count());
    }

    @Test
    public void parsesModuleDeclarationsAndModuleImports() {
        String moduleSource = "" +
                "open module demo.app {\n" +
                "    requires transitive java.logging;\n" +
                "    exports demo.api;\n" +
                "    uses demo.api.Service;\n" +
                "    provides demo.api.Service with demo.internal.ServiceImpl;\n" +
                "}\n";
        Java24Model module = assertExactRoundTrip(moduleSource);
        Assert.assertNotNull(module.getRoot().getModularUnit());

        String importSource = "" +
                "import module java.base;\n" +
                "import java.util.List;\n" +
                "class Imports { List<String> names; }\n";
        Java24Model imports = assertExactRoundTrip(importSource);
        Assert.assertEquals(1,
                imports.vmf().content().stream(ModuleImportDeclaration.class).count());
    }

    @Test
    public void parsesCompactSourceFilesAndFlexibleConstructors() {
        String compactSource = "" +
                "import module java.base;\n" +
                "void main() {\n" +
                "    println(twice(21));\n" +
                "}\n" +
                "int twice(int value) {\n" +
                "    return value * 2;\n" +
                "}\n";
        Java24Model compact = assertExactRoundTrip(compactSource);
        Assert.assertNotNull(compact.getRoot().getCompactUnit());

        String constructorSource = "" +
                "class Positive extends Number {\n" +
                "    Positive(int value) {\n" +
                "        if (value <= 0) throw new IllegalArgumentException();\n" +
                "        super();\n" +
                "    }\n" +
                "}\n";
        assertExactRoundTrip(constructorSource);
    }

    @Test
    public void parsesModernizedJava24Fixture() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get("test-code/Java24RungeKutta.java")),
                StandardCharsets.UTF_8);

        Java24Model model = assertExactRoundTrip(source);

        Assert.assertTrue(model.vmf().content().stream(TextBlockLiteral.class).count() >= 3);
    }

    private Java24Model assertExactRoundTrip(String source) {
        Java24ModelParser parser = new Java24ModelParser();
        List<String> errors = new ArrayList<>();
        parser.getErrorListeners().add(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                errors.add(line + ":" + charPositionInLine + " " + msg);
            }
        });

        Java24Model model = parser.parse(source);
        Assert.assertTrue("Syntax errors: " + errors, errors.isEmpty());
        Assert.assertEquals(source, new Java24ModelUnparser().unparse(model));
        return model;
    }
}
