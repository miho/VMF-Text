package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;
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

    private final TypeMappings typeMappings;

    public GrammarToModelListener(TypeMappings typeMappings) {

        this.typeMappings = typeMappings;

        model.setTypeMappings(this.typeMappings);

        // make sure each rule class is added to the map for lookup
        model.getRuleClasses().addChangeListener((c) -> {
            c.added().elements().forEach(rc -> {
                ruleClassesByName.put(rc.getName(), rc);
            });
        });
    }

    static Property elementToProperty(Map<String, RuleClass> rules, String ruleName, TypeMappings mappings, ANTLRv4Parser.ElementContext e) {

        if (e.labeledElement() == null || e.labeledElement().identifier() == null) {
            throw new IllegalArgumentException("Cannot convert unlabeled element to property.");
        }

        System.out.println("> generating properties for rule '" + ruleName + "':");

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
            // map types to rules

            String lexerRuleName = ParseTreeUtil.getElementText(e);

            System.out.println(" -> entering lexer rule '" + lexerRuleName+"':");


            Optional<TypeMapping> map = mappings.getTypeMappings().stream().
                    filter(m->m.getApplyToNames().contains(ruleName)
                            || m.getApplyToNames().isEmpty()).findFirst();

            if(map.isPresent()) {

                System.out.println("   -> type map is present");

                Optional<Mapping> tm = map.get().mappingByRuleName(lexerRuleName);

                if(tm.isPresent()) {

                    String fullTypeName = tm.get().getTypeName();

                    System.out.println("   -> replacing '" + lexerRuleName+"' with '" + fullTypeName+"'.");

                    String packageName = TypeUtil.getPackageNameFromFullClassName(fullTypeName);
                    String shortTypeName = TypeUtil.getShortNameFromFullClassName(fullTypeName);

                    System.out.println("name: " + packageName + ", "+ shortTypeName);

                    property.setType(Type.newBuilder().
                            withArrayType(isListType).
                            withPackageName(packageName).
                            withName(shortTypeName).
                            withAntlrRuleName(lexerRuleName).
                            build());
                } else {

                    System.out.println("   -> no replacement found for rule '"
                            + lexerRuleName+"'. Using String conversion.");

                    property.setType(Type.newBuilder().
                            withArrayType(isListType).
                            withPackageName("java.lang").
                            withName("String").
                            withAntlrRuleName(lexerRuleName).
                            build());
                }
            } else {
                System.out.println("   -> no type map found for rule '"
                        + lexerRuleName+"'. Using String conversion.");

                property.setType(Type.newBuilder().
                        withArrayType(isListType).
                        withPackageName("java.lang").
                        withName("String").
                        withAntlrRuleName(lexerRuleName).
                        build());
            }
        } else {
            System.out.println("   -> no rule. Using String conversion.");

            property.setType(Type.newBuilder().
                    withArrayType(isListType).
                    withPackageName("java.lang").
                    withName("String").
                    withAntlrRuleName("").
                    build());
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
                this.ruleClassesByName, currentRule, typeMappings, ctx.element());
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
        private final TypeMappings typeMappings;

        public InitRulePropertiesTask(Map<String, RuleClass> rules, RuleClass cls, TypeMappings typeMappings,
                                      List<ANTLRv4Parser.ElementContext> elements) {
            this.rules = rules;
            this.cls = cls;
            this.elements = elements;
            this.typeMappings = typeMappings;
        }

        void run() {
            cls.getProperties().addAll(
                    elements.stream().filter(e -> e.labeledElement() != null).
                            filter(e -> e.labeledElement().identifier() != null).
                            map(e -> GrammarToModelListener.elementToProperty(rules, cls.nameWithUpper(), typeMappings, e)).
                            collect(Collectors.toList())
            );
        }
    }
}
