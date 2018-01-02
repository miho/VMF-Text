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
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

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



