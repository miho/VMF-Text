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
 * Regression test for the "fail loudly" behavior: VMF-Text code generation must
 * throw {@link VMFTextGenerationException} for a broken grammar instead of the
 * old behavior of printing a stack trace and returning normally with missing or
 * partial generated output. The Gradle plugin turns this exception into a
 * {@code GradleException} so the build aborts with a clear message.
 */
public class GenerationFailureTest {

    @Test
    public void brokenGrammarFailsGenerationLoudly() throws Exception {
        File tmp = Files.createTempDirectory("vmf-text-broken").toFile();
        tmp.deleteOnExit();

        // Syntactically valid ANTLR, but 'missingRule' is undefined, so the
        // ANTLR tool reports an error. Generation used to swallow this (print a
        // stack trace and continue); it must now abort with an exception.
        File grammar = new File(tmp, "Broken.g4");
        Files.write(grammar.toPath(),
                ("grammar Broken;\n"
                        + "root : missingRule EOF ;\n").getBytes(StandardCharsets.UTF_8));

        File out = new File(tmp, "out");
        Assert.assertTrue(out.mkdirs());

        try {
            VMFText.generate(grammar, "broken.pkg", out);
            Assert.fail("expected VMFTextGenerationException for a grammar with an undefined rule");
        } catch (VMFTextGenerationException expected) {
            // success: generation failed loudly instead of silently producing
            // broken / partial output.
        }
    }
}
