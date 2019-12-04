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

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import eu.mihosoft.vmf.core.io.FileResourceSet;

import eu.mihosoft.vmf.core.io.MemoryResourceSet;
import eu.mihosoft.vmf.core.io.ResourceSet;
import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;

import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.mihosoft.ext.velocity.legacy.exception.ResourceNotFoundException;
import eu.mihosoft.ext.velocity.legacy.runtime.resource.loader.ClasspathResourceLoader;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        ResourceSet outRes = new FileResourceSet(new File("build/tmp/src-gen"));
        ResourceSet outResModel = new FileResourceSet(new File("build/tmp/src-modeldef"));

        VMFText.generate(
                new File("src/main/resources/eu/mihosoft/vmf/vmftext/antlr/RuleMatcher.g4"),
                "me.p12345678",
                outRes,outResModel);

//        emitGrammarBindingCode(model);
    }

    private static void processGrammarModel(GrammarModel model) {
        ModelGenerator generator = new ModelGenerator();
        FileResourceSet fileset = new FileResourceSet(new File("build/tmp"));
        generator.generateModel(model, fileset);
    }

    static String firstToUpper (String name) {
        return name.substring(0,1).toUpperCase()+name.substring(1);
    }
}

class VMFResourceLoader extends ClasspathResourceLoader {

    /**
     * Get an InputStream so that the Runtime can build a template with it.
     *
     * @param name name of template to get
     * @return InputStream containing the template
     * @throws ResourceNotFoundException if template not found in classpath.
     */
    public InputStream getResourceStream(String name)
            throws ResourceNotFoundException {
        InputStream input = Main.class.getResourceAsStream(name);

        return input;
    }

}



