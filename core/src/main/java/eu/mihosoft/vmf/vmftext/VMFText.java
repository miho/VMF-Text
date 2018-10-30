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


import eu.mihosoft.vmf.VMF;
import eu.mihosoft.vmf.core.io.*;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.unparser.UPRuleUtil;
import groovy.lang.GroovyClassLoader;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VMFText {

    private VMFText() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static final String CTX_PARSED_OPTIONAL_CODE = "{$ctx.__vmf_text__parsed_optional = true;} ";
    public static final String CTX_ADD_OPTIONAL_STATE_CODE_COMPLEX_CASE = "{$ctx.__vmf_text__optionalSymbols.add($ctx.__vmf_text__parsed_optional);$ctx.__vmf_text__parsed_optional=false;}";
    public static final String CTX_ADD_OPTIONAL_STATE_CODE = "{$ctx.__vmf_text__optionalSymbols.add(true);}";
    public static final String CTX_RULE_LOCALS_CODE = " locals [List<Boolean> __vmf_text__optionalSymbols = new ArrayList<Boolean>(), boolean __vmf_text__parsed_optional = false]";
    public static final String CTX_APPEND_RULE_LOCALS_CODE = ", List<Boolean> __vmf_text__optionalSymbols = new ArrayList<Boolean>(), boolean __vmf_text__parsed_optional = false";

    private static class GrammarAndUnparser {
        UnparserModel unparserModel;
        GrammarModel model;
    }

    // the classloader used for compiling the generated model code.
    private static ClassLoader compileClassLoader;

    /**
     * Defines the classloader used for compiling the generated model code.
     * @param l classloader to set
     */
    public static void setCompileClassLoader(ClassLoader l) {
        compileClassLoader = l;
    }

    /**
     * Returns the classloader used for compiling the generated model code.
     * @return the classloader used for compiling the generated model code
     */
    public static ClassLoader getCompileClassLoader() {
        return compileClassLoader;
    }

    public static void generate(File grammar, String packageName, File outputDir) {
        generate(grammar, packageName, new FileResourceSet(outputDir));
    }

    public static void generate(File grammar, String packageName, File outputDir, File modelOutputDir) {
        generate(grammar, packageName, new FileResourceSet(outputDir),new FileResourceSet(modelOutputDir));
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir) {
        generate(grammar, packageName, outputDir,null);
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir, ResourceSet modelOutputDir) {


        // rewrite grammar
        try {
            grammar = rewriteGrammar(grammar);
            System.out.println("FILE: " + grammar);
        } catch (IOException e) {
            e.printStackTrace();
        }


        AntlrTool.setOutput(outputDir);

        AntlrTool.main(
                new String[]{
                        grammar.getAbsolutePath(),
                        "-listener",
                        "-package", packageName+".parser",
//                "-lib",
//                "srcPath",
                        "-o", "" // packageName.replace('.','/')
                }
        );

        try {
            GrammarAndUnparser conversionResult = convertGrammarToModel(grammar);

            GrammarModel model = conversionResult.model;

            model.setPackageName(packageName);

            // generate model classes for src output
            ModelGenerator generator = new ModelGenerator();
            if(modelOutputDir!=null) {
                generator.generateModel(model, modelOutputDir);
            }

            // generate model delegates
            generator.generateModelDelegates(model, outputDir);

            // generate parser
            generator.generateModelParser(model, outputDir);

            // generate model unparser
            UnparserModel unparserModel = conversionResult.unparserModel;
            generator.generateModelUnparser(model, unparserModel, grammar, outputDir);

            // generate model classes for in-memory compilation
            MemoryResourceSet modelGenCode = new MemoryResourceSet();
            generator.generateModel(model, modelGenCode);

            generateModelCode(outputDir, modelGenCode);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateModelCode(ResourceSet outputDir, MemoryResourceSet modelGenCode) throws Exception {

        List<String> classNames = new ArrayList<>();

        GroovyClassLoader gcl;

        if(getCompileClassLoader()==null) {
            gcl = new GroovyClassLoader();
        } else {
            gcl = new GroovyClassLoader(getCompileClassLoader());
        }

        String modelDefCode = "";

        for (Map.Entry<String, MemoryResource> entry : modelGenCode.getMemSet().entrySet()) {
            modelDefCode += entry.getValue().asString()+"\n";
            classNames.addAll(ModelDefUtil.getNamesOfDefinedInterfaces(entry.getValue().asString()));
        }

        try {
            gcl.parseClass(modelDefCode);
        } catch(Exception ex) {
            ex.printStackTrace(System.err);
            return;
        }

        System.out.println("------------------------------------------------------");
        System.out.println("Generated Model Classes:");
        System.out.println("------------------------------------------------------");

        classNames.forEach(clsN-> System.out.println("-> type: " + clsN));

        Class[] classes = classNames.stream().map(clsN -> {
            try {
                return gcl.loadClass(clsN);
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
            }
            return null;
        }).collect(Collectors.toList()).toArray(new Class[classNames.size()]);

        VMF.generate(outputDir, classes);
    }

    private static File rewriteGrammar(File grammar) throws IOException {
        InputStream codeStream = new FileInputStream(grammar);
        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();
        ParseTreeWalker walker = new ParseTreeWalker();

        GrammarToRuleMatcherListener matchListenr = new GrammarToRuleMatcherListener(tokens);
        //GrammarToRuleMatcherListener.setDebug(true);

        walker.walk(matchListenr, tree);

        //Path dir = new File("/Users/miho/tmp").toPath();
        Path dir = Files.createTempDirectory("vmf-text");

        File grammarOut = new File(dir.toFile(),grammar.getName());

        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

        UnparserModel model = matchListenr.getModel();

        model.vmf().content().stream(UPRule.class).forEach(r -> {

            // add optional symbol list to rule definition
            // - if locals is present, we append to the locals definition
            // - otherwise, we add the 'locals [..]' definition to the rule def
            if(r.getTokenIndexLOCALS()<0) {
                String str = CTX_RULE_LOCALS_CODE;
                rewriter.insertBefore(r.getTokenIndexCOLON(), str);
            } else {
                String str = CTX_APPEND_RULE_LOCALS_CODE;
                rewriter.insertAfter(r.getTokenIndexLOCALS(), str);
            }

        });

        model.vmf().content().stream(UPElement.class).forEach(upElement -> {

            boolean parentIsBlockSet = false;

            if(upElement.getParentAlt().getParentRule() instanceof SubRule) {
                if(UPRuleUtil.isBlockSet((UPElement) upElement.getParentAlt().getParentRule())) {
                    parentIsBlockSet = true;
                }
            }

            if(UPRuleUtil.isEffectivelyOptional(upElement) && !parentIsBlockSet) {

                boolean optionalEBNF = upElement.ebnfZeroMany() || upElement.ebnfOptional();

                if(optionalEBNF) {
                    // put everything inside a sub-rule since otherwise we can't add an action between
                    // the optional element and the ebnf suffix (* or ?)
                    //
                    // example:
                    //
                    // ';'?    -> (';' {action:true} )? {action:record_symbol(true or false)}
                    //
                    rewriter.insertBefore(upElement.getTokenIndexStart(),"(");
                    rewriter.insertAfter(upElement.getTokenIndexStop()-1,
                            CTX_PARSED_OPTIONAL_CODE+")");
                    rewriter.insertAfter(upElement.getTokenIndexStop(),
                            CTX_ADD_OPTIONAL_STATE_CODE_COMPLEX_CASE);
                } else {
                    // just add the action since no ebnf suffix is present
                    rewriter.insertAfter(upElement.getTokenIndexStop(),
                            CTX_ADD_OPTIONAL_STATE_CODE);
                }

            }
        });

        Files.write(grammarOut.toPath(), rewriter.getText().getBytes("UTF-8"));

        return grammarOut;
    }

    private static GrammarAndUnparser convertGrammarToModel(File grammar) throws IOException {

        InputStream codeStream = new FileInputStream(grammar);
        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();
        ParseTreeWalker walker = new ParseTreeWalker();

        List<String> comments = GrammarMetaInformationUtil.extractVMFTextCommentsFromCode(new FileInputStream(grammar));

        System.out.println("\n------------------------------------------------------");
        System.out.println("Meta-Info:");
        System.out.println("------------------------------------------------------");

        TypeMappings typeMappings = TypeMappings.newInstance();

        for(String s : comments) {
            System.out.println(s);
            GrammarMetaInformationUtil.getTypeMapping(typeMappings, s);
        }

        GrammarToModelListener grammarToModelListener =
                new GrammarToModelListener(typeMappings);

        walker.walk(grammarToModelListener, tree);

        GrammarModel model = grammarToModelListener.getModel();

        System.out.println("\n------------------------------------------------------");
        System.out.println("Custom-Model-Definitions:");
        System.out.println("------------------------------------------------------");

        for(String s : comments) {
            GrammarMetaInformationUtil.getCustomAnnotations(s, model);
        }

        System.out.println("\n------------------------------------------------------");
        System.out.println("Grammar Matcher:");
        System.out.println("------------------------------------------------------");

        GrammarToRuleMatcherListener matchListenr = new GrammarToRuleMatcherListener(tokens);

        walker.walk(matchListenr, tree);

        GrammarAndUnparser grammarAndUnparser = new GrammarAndUnparser();
        grammarAndUnparser.model = model;
        grammarAndUnparser.unparserModel = matchListenr.getModel();

        System.out.println("-> unparser model generated.");

        return grammarAndUnparser;
    }



    static class AntlrTool extends org.antlr.v4.Tool {

        private static ResourceSet output;
        private static final List<Resource> openedResources = new ArrayList<>();


        public AntlrTool(String[] args) {
            super(args);
        }

        public static void setOutput(ResourceSet output) {
            AntlrTool.output = output;
        }

        public ResourceSet getOutput() {
            return output;
        }

        @Override
        public void exit(int e) {
            for(Resource res : openedResources) {
                try {
                    res.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }

        public Writer getOutputFileWriter(Grammar g, String fileName) throws IOException {

//            if (outputDirectory == null) {
//                return new StringWriter();
//            }

            // output directory is a function of where the grammar file lives
            // for subdir/T.g4, you get subdir here.  Well, depends on -o etc...
//            File outputDir = getOutputDirectory(g.fileName);
//            File outputFile = new File(outputDir, fileName);

//            if (!outputDir.exists()) {
//                outputDir.mkdirs();
//            }

            String url = genPackage.replace('.','/')+
                    "/"+fileName;

            Resource res = getOutput().open(url);
            openedResources.add(res);
            return res.open();
        }

        public static void main(String[] args) {
            AntlrTool antlr = new AntlrTool(args);
            if ( args.length == 0 ) { antlr.help(); antlr.exit(0); }

            try {
                antlr.processGrammarsOnCommandLine();
            }
            finally {
                if ( antlr.log ) {
                    try {
                        String logname = antlr.logMgr.save();
                        System.out.println("wrote "+logname);
                    }
                    catch (IOException ioe) {
                        antlr.errMgr.toolError(ErrorType.INTERNAL_ERROR, ioe);
                    }
                }
            }
            if ( antlr.return_dont_exit ) return;

            if (antlr.errMgr.getNumErrors() > 0) {
                antlr.exit(1);
            }
            antlr.exit(0);
        }
    }
}
