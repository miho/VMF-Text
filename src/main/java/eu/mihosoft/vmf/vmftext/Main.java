package eu.mihosoft.vmf.vmftext;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        ResourceSet outRes = new FileResourceSet(new File("build/tmp"));

        VMFText.generate(
                new File("src/main/resources/eu/mihosoft/vmf/vmftext/antlr/GrammarVMF2.g4"),
                "me.p12345678",
                outRes);

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

class GrammarToRuleMatcherListener extends ANTLRv4ParserBaseListener {

    private TokenStream stream;

    private String currentRuleName;
    private ANTLRv4Parser.AlternativeContext insideAlternative = null;

    private boolean currentAltIsLabeled = false;

    private final Map<String, Integer> rulesAltNames = new HashMap<>();


    public GrammarToRuleMatcherListener(TokenStream stream) {
        this.stream = stream;
    }

    private int newRuleAltIndex(String ruleName) {
        Integer id = rulesAltNames.get(ruleName);

        if(id==null) {
            id = 0;

        } else {
            id++;
        }

        rulesAltNames.put(ruleName, id);

        return id;
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        if(insideAlternative==null) {
            insideAlternative = ctx;

            int ruleAltIndex = newRuleAltIndex(currentRuleName);

            System.out.print(currentRuleName + "_alt_rule_" + ruleAltIndex + " : ");

            System.out.print(stream.getText(ctx.getSourceInterval()));

            System.out.println();

            ctx.element().stream().
                    filter(e -> ParseTreeUtil.isLabeledElement(e)).filter(e -> ParseTreeUtil.isLexerRule(e)||ParseTreeUtil.isRuleBlock(e)).
                    forEach(e -> {
                        System.out.print(currentRuleName + "_alt_" + ruleAltIndex + "_prop_rule_" + e.labeledElement().identifier().getText() + " : ");

                        System.out.println(stream.getText(e.getSourceInterval()));
            });

            System.out.println();
        }

        super.enterAlternative(ctx);
    }

    @Override
    public void exitAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        if(insideAlternative == ctx){
            insideAlternative = null;
        }

        super.exitAlternative(ctx);
    }

    @Override
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        currentRuleName = ctx.RULE_REF().getText();
        super.enterParserRuleSpec(ctx);
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



