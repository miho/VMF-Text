package eu.mihosoft.vmf.vmftext;


import eu.mihosoft.vmf.VMF;
import eu.mihosoft.vmf.core.io.*;
import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import groovy.lang.GroovyClassLoader;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VMFText {

    private VMFText() {
        throw new AssertionError("Don't instantiate me!");
    }

    //    public static void generate(String grammars, ResourceSet outputDir) {
//
//    }

    public static void generate(File grammar, String packageName, File outputDir) {
        generate(grammar, packageName, new FileResourceSet(outputDir));
    }


    public static void generate(File grammar, String packageName, ResourceSet outputDir) {

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
            GrammarModel model = convertGrammarToModel(grammar);

            model.setPackageName(packageName);

            // generate model classes for src output
            ModelGenerator generator = new ModelGenerator();
            generator.generateModel(model, outputDir);
            generator.generateModelConverter(model, outputDir);

            // generate model classes for in-memory compilation
            MemoryResourceSet modelGenCode = new MemoryResourceSet();
            generator.generateModel(model, modelGenCode);

            generateModelCode(outputDir, modelGenCode);

            System.exit(0);

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

        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance().ignoreWarnings();
        for (Map.Entry<String, MemoryResource> entry : modelGenCode.getMemSet().entrySet()) {
            // convert /path/to/File.java to pkg.File
            compiler.addSource(entry.getKey().replace('/','.').substring(0,entry.getKey().length()-5),
                    entry.getValue().asString());
            classNames.addAll(ModelDefUtil.getNamesOfDefinedInterfaces(entry.getValue().asString()));
        }

        try {
            compiler.compileAll();
        } catch(Exception ex) {
            ex.printStackTrace(System.out);
        }

        System.out.println("------------------------------------------------------");
        System.out.println("Generated Model Classes:");
        System.out.println("------------------------------------------------------");

        classNames.forEach(clsN-> System.out.println("-> type: " + clsN));

        Class[] classes = classNames.stream().map(clsN -> {
            try {
                return compiler.getClassloader().loadClass(clsN);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()).toArray(new Class[classNames.size()]);

        VMF.generate(outputDir, classes);
    }

    private static GrammarModel convertGrammarToModel(File grammar) throws IOException {
        InputStream codeStream = new FileInputStream(grammar);
        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();

        ParseTreeWalker walker = new ParseTreeWalker();

        GrammarToModelListener grammarToModelListener =
                new GrammarToModelListener();

        walker.walk(grammarToModelListener, tree);
        return grammarToModelListener.getModel();
    }



    private static class AntlrTool extends org.antlr.v4.Tool {

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
            File outputDir = getOutputDirectory(g.fileName);
            File outputFile = new File(outputDir, fileName);

//            if (!outputDir.exists()) {
//                outputDir.mkdirs();
//            }

            String url = genPackage.replace('.','/')+
                    "/"+outputFile.getAbsolutePath();

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
