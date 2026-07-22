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
import eu.mihosoft.vmf.core.VMFEquals;
import eu.mihosoft.vmf.core.VMFModel;
import eu.mihosoft.vmf.core.io.*;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.target.AntlrTargetOptionalStateProvider;
import eu.mihosoft.vmf.vmftext.grammar.target.JavaAntlrOptionalStateProvider;
import eu.mihosoft.vmf.vmftext.grammar.unparser.UPRuleUtil;
import groovy.lang.GroovyClassLoader;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.mdkt.compiler.InMemoryJavaCompiler;
import org.tinylog.Logger;

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

    /** @deprecated use {@link JavaAntlrOptionalStateProvider#PARSED_OPTIONAL_FLAG_ACTION} */
    @Deprecated
    public static final String CTX_PARSED_OPTIONAL_CODE =
            JavaAntlrOptionalStateProvider.PARSED_OPTIONAL_FLAG_ACTION;
    /** @deprecated use {@link JavaAntlrOptionalStateProvider#RULE_LOCALS_DECLARATION} */
    @Deprecated
    public static final String CTX_RULE_LOCALS_CODE =
            JavaAntlrOptionalStateProvider.RULE_LOCALS_DECLARATION;
    /** @deprecated use {@link JavaAntlrOptionalStateProvider#APPEND_RULE_LOCALS_DECLARATION} */
    @Deprecated
    public static final String CTX_APPEND_RULE_LOCALS_CODE =
            JavaAntlrOptionalStateProvider.APPEND_RULE_LOCALS_DECLARATION;

    private static final AntlrTargetOptionalStateProvider OPTIONAL_STATE_PROVIDER =
            JavaAntlrOptionalStateProvider.INSTANCE;

    static String ctxAddOptionalStateComplexCaseCode(String grammarElementPath) {
        return OPTIONAL_STATE_PROVIDER.buildComplexOptionalStateAction(grammarElementPath);
    }

    static String ctxAddOptionalStateCode(String grammarElementPath) {
        return OPTIONAL_STATE_PROVIDER.buildOptionalStateAction(grammarElementPath);
    }

    public static String stripInjectedActions(String grammarText) {
        return OPTIONAL_STATE_PROVIDER.stripInjectedActions(grammarText);
    }

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
        generate(grammar, packageName, new FileResourceSet(outputDir), GenerationOptions.defaults());
    }

    public static void generate(File grammar, String packageName, File outputDir, GenerationOptions options) {
        generate(grammar, packageName, new FileResourceSet(outputDir), options);
    }

    public static void generate(File grammar, String packageName, File outputDir, File modelOutputDir) {
        generate(grammar, packageName, new FileResourceSet(outputDir),new FileResourceSet(modelOutputDir), GenerationOptions.defaults());
    }

    public static void generate(File grammar, String packageName, File outputDir, File modelOutputDir, GenerationOptions options) {
        generate(grammar, packageName, new FileResourceSet(outputDir),new FileResourceSet(modelOutputDir), options);
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir) {
        generate(grammar, packageName, outputDir,null, GenerationOptions.defaults());
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir, GenerationOptions options) {
        generate(grammar, packageName, outputDir,null, options);
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir, ResourceSet modelOutputDir) {
        generate(grammar, packageName, outputDir, modelOutputDir, GenerationOptions.defaults());
    }

    public static void generate(File grammar, String packageName, ResourceSet outputDir, ResourceSet modelOutputDir,
                                GenerationOptions options) {

        if(options == null) {
            options = GenerationOptions.defaults();
        }

        final File grammarInput = grammar;

        try {
            // Apply optional auto-labeling before all other grammar rewrites. Metadata
            // inside VMF-Text comments can enable auto-labeling per grammar, while the
            // GenerationOptions flag can enable it globally (e.g. from Gradle).
            List<String> comments = GrammarMetaInformationUtil.extractVMFTextCommentsFromCode(new FileInputStream(grammar));
            if(options.isAutoLabel() || GrammarMetaInformationUtil.isAutoLabelEnabled(comments)) {
                grammar = AutoLabeler.rewrite(grammar, options.isEmitAutoLabelReport());
            }

            // Rewrite list-labeled wildcards (label+=.) before optional wrapping / AntlrTool
            // so ANTLR emits valid List-backed labels (issue #8).
            grammar = LabeledDotRewriter.rewrite(grammar);

            // rewrite grammar (wrap optionals + inject optional-state actions)
            grammar = rewriteGrammar(grammar);
            Logger.debug("Rewritten grammar file: {}", grammar);

            AntlrTool.setOutput(outputDir);

            AntlrTool.main(
                    new String[]{
                            grammar.getAbsolutePath(),
                            "-listener",
                            "-package", packageName+".parser",
                            "-o", ""
                    }
            );

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

            // generate source bundle persistence helper
            generator.generateSourceBundle(model, outputDir);

            // generate model unparser
            UnparserModel unparserModel = conversionResult.unparserModel;

            // generate parser (needs unparser model for list-shape hints)
            generator.generateModelParser(model, unparserModel, outputDir);

            // generate trivia-splice support class (extracted from the converter template)
            generator.generateTriviaSupport(model, unparserModel, outputDir);

            generator.generateModelUnparser(model, unparserModel, grammar, outputDir);

            // generate model classes for in-memory compilation
            MemoryResourceSet modelGenCode = new MemoryResourceSet();
            generator.generateModel(model, modelGenCode);

            generateModelCode(outputDir, modelGenCode);

        } catch (VMFTextGenerationException e) {
            // already a clear generation failure — do not double-wrap
            throw e;
        } catch (Exception e) {
            // Fail loudly instead of printing a stack trace and continuing with
            // missing/partial output (the previous behavior produced a "green"
            // build that only broke later at javac time on the generated sources).
            throw new VMFTextGenerationException(
                    "VMF-Text generation failed for grammar '" + grammarInput + "': " + e.getMessage(), e);
        }
    }

    private static void generateModelCode(ResourceSet outputDir, MemoryResourceSet modelGenCode) throws Exception {

        List<String> classNames = new ArrayList<>();


        String modelDefCode = "";

        for (Map.Entry<String, MemoryResource> entry : modelGenCode.getMemSet().entrySet()) {

            modelDefCode += entry.getValue().asString() + "\n";
            classNames.addAll(ModelDefUtil.getNamesOfDefinedInterfaces(entry.getValue().asString()));
        }


        GroovyClassLoader gcl;
        if(getCompileClassLoader()==null) {
            gcl = new GroovyClassLoader();
        } else {
            gcl = new GroovyClassLoader(getCompileClassLoader());
        }

        try {
            gcl.parseClass(modelDefCode);
        } catch(Exception ex) {
            throw new VMFTextGenerationException(
                    "Failed to compile the generated VMF model definitions", ex);
        }

        Logger.debug("------------------------------------------------------");
        Logger.debug("Generated Model Classes:");
        Logger.debug("------------------------------------------------------");

        classNames.forEach(clsN -> Logger.debug("-> type: " + clsN));

        Class[] classes = classNames.stream().map(clsN -> {
            try {
                return gcl.loadClass(clsN);
            } catch (ClassNotFoundException e) {
                throw new VMFTextGenerationException(
                        "Generated model class not found: " + clsN, e);
            }
        }).collect(Collectors.toList()).toArray(new Class[classNames.size()]);

        VMF.generate(outputDir, classes);
    }

    private static File rewriteGrammar(File grammar) throws IOException {

        // The unparser code generator derives grammar-element paths from a
        // model of the REWRITTEN grammar. Wrapping bare EBNF-optional
        // elements in sub-rules changes that structure, so recording paths
        // computed on the original grammar would not match the paths the
        // generated unparser queries. Rewriting therefore happens in two
        // passes:
        //
        //   pass A: insert only the structural wrappers, e.g.
        //           ';'?  ->  ( ';' {flag} )?
        //           (the inner flag action doubles as the wrapper marker);
        //   pass B: reparse, compute element paths on the final structure
        //           and inject the state-recording actions with those paths.
        //
        // Injected actions do not create model elements, so the pass-B model
        // is structurally identical to the model the unparser generator
        // later builds from the final grammar.
        Path dir = Files.createTempDirectory("vmf-text");

        File wrapped = rewriteGrammarWrapOptionals(grammar, dir);

        return rewriteGrammarInjectStateActions(wrapped, dir, grammar.getName());
    }

    private static File rewriteGrammarWrapOptionals(File grammar, Path dir) throws IOException {

        InputStream codeStream = new FileInputStream(grammar);
        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();
        ParseTreeWalker walker = new ParseTreeWalker();

        GrammarToRuleMatcherListener matchListenr = new GrammarToRuleMatcherListener(tokens);

        walker.walk(matchListenr, tree);

        File grammarOut = new File(dir.toFile(),grammar.getName()+".wrapped");

        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

        UnparserModel model = matchListenr.getModel();

        model.vmf().content().stream(UPElement.class).forEach(upElement -> {

            boolean parentIsBlockSet = false;

            // Optional sub-rules are represented by the optionality of their
            // contained terminals/lexer symbols. Recording an additional state
            // for the sub-rule itself desynchronizes formatter consumption when
            // the sub-rule contains optional named/list properties that do not
            // render a terminal for the empty case, e.g. ('(' names+=ID* ')')?.
            if(upElement instanceof SubRule) {
                return;
            }

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
                    // ';'?    -> (';' {action:flag=true} )?
                    //
                    rewriter.insertBefore(upElement.getTokenIndexStart(),"(");
                    rewriter.insertAfter(upElement.getTokenIndexStop()-1,
                            OPTIONAL_STATE_PROVIDER.getParsedOptionalFlagAction()+")");
                }
            }
        });

        Files.write(grammarOut.toPath(), rewriter.getText().getBytes("UTF-8"));

        return grammarOut;
    }

    private static File rewriteGrammarInjectStateActions(File grammar, Path dir, String grammarFileName)
            throws IOException {

        InputStream codeStream = new FileInputStream(grammar);
        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();
        ParseTreeWalker walker = new ParseTreeWalker();

        GrammarToRuleMatcherListener matchListenr = new GrammarToRuleMatcherListener(tokens);

        walker.walk(matchListenr, tree);

        File grammarOut = new File(dir.toFile(),grammarFileName);

        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

        UnparserModel model = matchListenr.getModel();

        model.vmf().content().stream(UPRule.class).forEach(r -> {

            // add optional state list to rule definition
            // - if locals is present, we append to the locals definition
            // - otherwise, we add the 'locals [..]' definition to the rule def
            if(r.getTokenIndexLOCALS()<0) {
                String str = OPTIONAL_STATE_PROVIDER.getRuleLocalsDeclaration();
                rewriter.insertBefore(r.getTokenIndexCOLON(), str);
            } else {
                String str = OPTIONAL_STATE_PROVIDER.getAppendRuleLocalsDeclaration();
                rewriter.insertAfter(r.getTokenIndexLOCALS(), str);
            }

        });

        model.vmf().content().stream(UPElement.class).forEach(upElement -> {

            if(upElement instanceof SubRule) {
                UPElement wrappedElement = injectedWrapperContent((SubRule) upElement);
                if(wrappedElement!=null) {
                    // pass-A wrapper: record explicit presence/absence keyed
                    // by the path of the wrapped element — the same path the
                    // generated unparser queries at consumption time
                    rewriter.insertAfter(upElement.getTokenIndexStop(),
                            ctxAddOptionalStateComplexCaseCode(UPRuleUtil.getPath(wrappedElement)));
                }
                return;
            }

            if(isInsideInjectedWrapper(upElement)) {
                // covered by the wrapper's flag/record mechanism above
                return;
            }

            boolean parentIsBlockSet = false;

            if(upElement.getParentAlt().getParentRule() instanceof SubRule) {
                if(UPRuleUtil.isBlockSet((UPElement) upElement.getParentAlt().getParentRule())) {
                    parentIsBlockSet = true;
                }
            }

            if(UPRuleUtil.isEffectivelyOptional(upElement) && !parentIsBlockSet) {

                if(upElement.ebnfZeroMany() || upElement.ebnfOptional()) {
                    // bare EBNF optionals were wrapped in pass A; an action
                    // inserted after 'x?' would fire even when x is absent,
                    // so never record here
                    return;
                }

                // effectively optional by alternative structure: the action
                // executes only when the containing alternative is taken, so
                // absence is represented by the missing entry for this path
                rewriter.insertAfter(upElement.getTokenIndexStop(),
                        ctxAddOptionalStateCode(UPRuleUtil.getPath(upElement)));
            }
        });

        Files.write(grammarOut.toPath(), rewriter.getText().getBytes("UTF-8"));

        return grammarOut;
    }

    /**
     * Returns the single element wrapped by a pass-A injected wrapper, or
     * {@code null} if the given sub-rule is not such a wrapper. Injected
     * wrappers are EBNF-optional sub-rules that carry the parsed-optional
     * flag action and contain exactly one non-sub-rule element.
     */
    private static UPElement injectedWrapperContent(SubRule sr) {

        if(!(sr instanceof UPElement)) {
            return null;
        }

        UPElement srElement = (UPElement) sr;

        if(!(srElement.ebnfOptional() || srElement.ebnfZeroMany())) {
            return null;
        }

        if(srElement.getText()==null || !srElement.getText().contains("__vmf_text__parsed_optional")) {
            return null;
        }

        if(sr.getAlternatives().size()!=1) {
            return null;
        }

        eu.mihosoft.vmf.vmftext.grammar.AlternativeBase alt = sr.getAlternatives().get(0);

        if(alt.getElements().size()!=1) {
            return null;
        }

        UPElement inner = alt.getElements().get(0);

        if(inner instanceof SubRule) {
            return null;
        }

        if(inner.getText()!=null && inner.getText().contains("__vmf_text__parsed_optional")) {
            return null;
        }

        return inner;
    }

    private static boolean isInsideInjectedWrapper(UPElement e) {

        if(e.getParentAlt()==null) {
            return false;
        }

        if(!(e.getParentAlt().getParentRule() instanceof SubRule)) {
            return false;
        }

        return injectedWrapperContent((SubRule) e.getParentAlt().getParentRule()) == e;
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

        Logger.debug("\n------------------------------------------------------");
        Logger.debug("Meta-Info:");
        Logger.debug("------------------------------------------------------");

        TypeMappings typeMappings = TypeMappings.newInstance();

        for(String s : comments) {
            Logger.debug(s);
            GrammarMetaInformationUtil.getTypeMapping(typeMappings, s);
        }

        GrammarToModelListener grammarToModelListener =
                new GrammarToModelListener(typeMappings);

        walker.walk(grammarToModelListener, tree);

        GrammarModel model = grammarToModelListener.getModel();

        Logger.debug("\n------------------------------------------------------");
        Logger.debug("Custom-Model-Definitions:");
        Logger.debug("------------------------------------------------------");

        for(String s : comments) {
            GrammarMetaInformationUtil.getCustomAnnotations(s, model);
        }

        Logger.debug("\n------------------------------------------------------");
        Logger.debug("Grammar Matcher:");
        Logger.debug("------------------------------------------------------");

        GrammarToRuleMatcherListener matchListenr = new GrammarToRuleMatcherListener(tokens);

        walker.walk(matchListenr, tree);

        GrammarAndUnparser grammarAndUnparser = new GrammarAndUnparser();
        grammarAndUnparser.model = model;
        grammarAndUnparser.unparserModel = matchListenr.getModel();

        Logger.debug("-> unparser model generated.");

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
                    Logger.error(ex, "Failed to close generated resource");
                }
            }
            openedResources.clear();
            if(e != 0) {
                throw new VMFTextGenerationException(
                        "ANTLR reported " + errMgr.getNumErrors()
                                + " grammar error(s); code generation aborted");
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
                        Logger.debug("wrote "+logname);
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
