package eu.mihosoft.vmf.vmftext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.Type;
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

        InputStream codeStream = Main.class.getResourceAsStream(
                "antlr/GrammarVMF2.g4");

        CharStream input = CharStreams.fromStream(codeStream);

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

        ParserRuleContext tree = parser.grammarSpec();

        ParseTreeWalker walker = new ParseTreeWalker();

        GrammarToModelListener grammarToModelListener =
                new GrammarToModelListener();

        walker.walk(grammarToModelListener, tree);

        walker.walk(new GrammarToRuleMatcherListener(tokens), tree);

        GrammarModel model = grammarToModelListener.getModel();

        processGrammarModel(model);

//        emitGrammarBindingCode(model);
    }

    private static void processGrammarModel(GrammarModel model) {

        ModelGenerator generator = new ModelGenerator();
        generator.generate(model);
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

class GrammarToModelListener extends ANTLRv4ParserBaseListener {

    private GrammarModel model = GrammarModel.newInstance();

    private RuleClass currentRule;

    private final Map<String, RuleClass> ruleClassesByName
            = new HashMap<>();

    private final List<InitRulePropertiesTask> initPropertyTasks
            = new ArrayList<>();
    private RuleClass superClassrule;

    public GrammarToModelListener() {

        // make sure each rule class is added to the map for lookup
        model.getRuleClasses().addChangeListener((c) -> {
            c.added().elements().forEach(rc -> {
                ruleClassesByName.put(rc.getName(), rc);
            });
        });
    }

    static Property elementToProperty(Map<String, RuleClass> rules, ANTLRv4Parser.ElementContext e) {

        if (e.labeledElement() == null || e.labeledElement().identifier() == null) {
            throw new IllegalArgumentException("Cannot convert unlabeled element to property.");
        }

        boolean hasEBNF = e.ebnfSuffix() !=null;

        // an element is a list type if it is assigned via '+=' and/or if the elements ebnf suffix is '*' or '+'
        boolean isListType = e.labeledElement().PLUS_ASSIGN()!=null
                || (hasEBNF && (e.ebnfSuffix().PLUS()!=null || e.ebnfSuffix().STAR()!=null));

        Property property = Property.newInstance();
        property.setName(e.labeledElement().identifier().getText());

        if (ParseTreeUtil.isParserRule(e)) {
            Type t = typeFromRuleClass(
                       rules.get(ParseTreeUtil.getElementText(e)),isListType);

            property.setType(t);

        } else if (ParseTreeUtil.isLexerRule(e)) {
            // TODO map types to rules
            property.setType(Type.newBuilder().
                    withArrayType(isListType).
                    withPackageName("java.lang").
                    withName("String").build());
        } else {
            property.setType(Type.newBuilder().
                    withArrayType(isListType).
                    withPackageName("java.lang").
                    withName("String").build());
        }

        return property;
    }

    static Type typeFromRuleClass(RuleClass ruleClass, boolean isListType) {

        return Type.newBuilder().
                withRuleType(true).
                withArrayType(isListType).
                withPackageName("").
                withName(ruleClass.getName())
                .build();
    }

    @Override
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {

        System.out.println("------------------------------------------------------");
        System.out.println("ParserRule: " + ctx.RULE_REF().getText());
        System.out.println("------------------------------------------------------");

        currentRule = RuleClass.newInstance();
        currentRule.setName(ctx.RULE_REF().getText());
        // first rule is root
        currentRule.setRoot(model.getRuleClasses().isEmpty());
        model.getRuleClasses().add(currentRule);
        superClassrule = currentRule;

        super.exitParserRuleSpec(ctx);
    }


    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        if (ctx.identifier() != null) {
            currentRule = RuleClass.newInstance();
            model.getRuleClasses().add(currentRule);
            currentRule.setName(ctx.identifier().getText());
            if (superClassrule != null) {
                currentRule.setSuperClass(superClassrule);
            }
        }

        super.enterLabeledAlt(ctx);
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        InitRulePropertiesTask task = new InitRulePropertiesTask(
                this.ruleClassesByName, currentRule, ctx.element());
        this.initPropertyTasks.add(task);

        super.enterAlternative(ctx);
    }

    @Override
    public void enterGrammarSpec(ANTLRv4Parser.GrammarSpecContext ctx) {
        System.out.println("------------------------------------------------------");
        System.out.println("Enter Grammar '" + ctx.identifier().getText() + "'");
        System.out.println("------------------------------------------------------");

        model.setGrammarName(ctx.identifier().getText());

        super.enterGrammarSpec(ctx);
    }

    @Override
    public void exitGrammarSpec(ANTLRv4Parser.GrammarSpecContext ctx) {
        System.out.println("------------------------------------------------------");
        System.out.println("Exit Grammar '" + ctx.identifier().getText() + "'");
        System.out.println("------------------------------------------------------");

        initPropertyTasks.forEach(t -> t.run());

        super.exitGrammarSpec(ctx);
    }

    public GrammarModel getModel() {
        return model;
    }

    static class InitRulePropertiesTask {
        private final Map<String, RuleClass> rules;
        private final RuleClass cls;
        private final List<ANTLRv4Parser.ElementContext> elements;

        public InitRulePropertiesTask(Map<String, RuleClass> rules, RuleClass cls, List<ANTLRv4Parser.ElementContext> elements) {
            this.rules = rules;
            this.cls = cls;
            this.elements = elements;
        }

        void run() {
            cls.getProperties().addAll(
                    elements.stream().filter(e -> e.labeledElement() != null).
                            filter(e -> e.labeledElement().identifier() != null).
                            map(e -> GrammarToModelListener.elementToProperty(rules, e)).
                            collect(Collectors.toList())
            );
        }
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



