package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.Type;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;

import java.util.*;
import java.util.stream.Collectors;

class GrammarToModelListener extends ANTLRv4ParserBaseListener {

    private GrammarModel model = GrammarModel.newInstance();

    private RuleClass currentRule;

    private final Map<String, RuleClass> ruleClassesByName
            = new HashMap<>();

    private final List<InitRulePropertiesTask> initPropertyTasks
            = new ArrayList<>();
    private RuleClass superClassRule;

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

        // an element is a list type if it is assigned via '+='
        // TODO is this correct (currently disabled) and/or if the elements ebnf suffix is '*' or '+'
        boolean isListType = e.labeledElement().PLUS_ASSIGN()!=null
                //|| (hasEBNF && (e.ebnfSuffix().PLUS()!=null || e.ebnfSuffix().STAR()!=null))
        ;

        Property property = Property.newInstance();
        property.setName(e.labeledElement().identifier().getText());

        property.setCodeRange(ParseTreeUtil.ctxToCodeRange(e));

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

        String ruleName = ctx.RULE_REF().getText();

        System.out.println("------------------------------------------------------");
        System.out.println("ParserRule: " + ruleName);
        System.out.println("------------------------------------------------------");

        Optional<RuleClass> currentRuleOpt = model.getRuleClasses().stream().
                filter(rc->Objects.equals(rc.getName(),ruleName)).findAny();

        if(currentRuleOpt.isPresent()) {
            currentRule = currentRuleOpt.get();
            System.out.println("  -> [UPDATE] merging with existing rule '"+ruleName+"'");
        } else {
            currentRule = RuleClass.newBuilder().withName(ruleName).build();
        }
        // first rule is root
        currentRule.setRoot(model.getRuleClasses().isEmpty());
        currentRule.setCodeRange(ParseTreeUtil.ctxToCodeRange(ctx));
        model.getRuleClasses().add(currentRule);
        superClassRule = currentRule;

        super.exitParserRuleSpec(ctx);
    }


    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        if (ctx.identifier() != null) {

            String ruleName = ctx.identifier().getText();

            System.out.println("-> labeled-alt-rule: " + ruleName);

            Optional<RuleClass> currentRuleOpt = model.getRuleClasses().stream().
                    filter(rc->Objects.equals(rc.getName(),ruleName)).findAny();

            if(currentRuleOpt.isPresent()) {
                currentRule = currentRuleOpt.get();
                System.out.println("  -> [UPDATE] merging with existing rule '"+ruleName+"'");
            } else {
                currentRule = RuleClass.newBuilder().withName(ruleName).build();
            }

            model.getRuleClasses().add(currentRule);

            currentRule.setCodeRange(ParseTreeUtil.ctxToCodeRange(ctx));

            if (superClassRule != null) {
                System.out.println("  -> setting superRuleCls: " + superClassRule.nameWithLower());
                currentRule.setSuperClass(superClassRule);
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
