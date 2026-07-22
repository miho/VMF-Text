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

import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;

import org.tinylog.Logger;

import java.util.*;
import java.util.stream.Collectors;

class GrammarToModelListener extends ANTLRv4ParserBaseListener {

    private final GrammarModel model = GrammarModel.newInstance();

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

        Logger.debug("> generating properties for rule '" + ruleName + "':");

        boolean hasEBNF = e.ebnfSuffix() !=null;

        // an element is a list type if it is assigned via '+='
        // TODO is this correct (currently disabled) and/or if the elements ebnf suffix is '*' or '+'
        boolean isListType = e.labeledElement().PLUS_ASSIGN()!=null
                //|| (hasEBNF && (e.ebnfSuffix().PLUS()!=null || e.ebnfSuffix().STAR()!=null))
        ;

        Property property = Property.newInstance();
        property.setName(e.labeledElement().identifier().getText());

        property.setCodeRange(ParseTreeUtil.ctxToCodeRange(e));

        // Synthetic stand-in for list-labeled '.' (issue #8 / LabeledDotRewriter):
        // expose as a token-typed String property, not a nested rule class.
        if (ParseTreeUtil.isWildcardTokenProxy(e) || ParseTreeUtil.isDotWildcard(e)) {
            Logger.debug("   -> labeled wildcard / any-token proxy. Using String conversion.");

            property.setType(Type.newBuilder().
                    withArrayType(isListType).
                    withPackageName("java.lang").
                    withName("String").
                    withAntlrRuleName("").
                    build());

        } else if (ParseTreeUtil.isParserRule(e)) {
            Type t = typeFromRuleClass(
                       rules.get(ParseTreeUtil.getElementText(e)),isListType);

            property.setType(t);

        } else if (ParseTreeUtil.isLexerRule(e)) {
            // map types to rules

            String lexerRuleName = ParseTreeUtil.getElementText(e);

            Logger.debug(" -> entering lexer rule '" + lexerRuleName+"':");


            Optional<TypeMapping> map = mappings.getTypeMappings().stream().
                    filter(m->m.getApplyToNames().contains(ruleName)
                            || m.getApplyToNames().isEmpty()).findFirst();

            if(map.isPresent()) {

                Logger.debug("   -> type map is present");

                Optional<Mapping> tm = map.get().mappingByRuleName(lexerRuleName);

                if(tm.isPresent()) {

                    String fullTypeName = tm.get().getTypeName();

                    Logger.debug("   -> replacing '" + lexerRuleName+"' with '" + fullTypeName+"'.");

                    String packageName = TypeUtil.getPackageNameFromFullClassName(fullTypeName);
                    String shortTypeName = TypeUtil.getShortNameFromFullClassName(fullTypeName);

                    Logger.debug("name: " + packageName + ", "+ shortTypeName);

                    property.setType(Type.newBuilder().
                            withArrayType(isListType).
                            withPackageName(packageName).
                            withName(shortTypeName).
                            withAntlrRuleName(lexerRuleName).
                            build());


                    String defaultValueString = tm.get().getDefaultValueCode();

                    property.getAnnotations().add(PropertyAnnotation.newBuilder().
                            withText("@eu.mihosoft.vmf.core.DefaultValue(\"" + defaultValueString + "\")").build());

                } else {

                    Logger.debug("   -> no replacement found for rule '"
                            + lexerRuleName+"'. Using String conversion.");

                    property.setType(Type.newBuilder().
                            withArrayType(isListType).
                            withPackageName("java.lang").
                            withName("String").
                            withAntlrRuleName(lexerRuleName).
                            build());
                }
            } else {
                Logger.debug("   -> no type map found for rule '"
                        + lexerRuleName+"'. Using String conversion.");

                property.setType(Type.newBuilder().
                        withArrayType(isListType).
                        withPackageName("java.lang").
                        withName("String").
                        withAntlrRuleName(lexerRuleName).
                        build());
            }
        } else {
            // String literals map to token-typed String properties.
            Logger.debug("   -> no rule. Using String conversion.");

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
                withName(ruleClass.getName()).
                build();
    }

    @Override
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {

        String ruleName = ctx.RULE_REF().getText();

        Logger.debug("------------------------------------------------------");
        Logger.debug("ParserRule: " + ruleName);
        Logger.debug("------------------------------------------------------");

        // Hide the synthetic any-token stand-in from the public model (issue #8).
        if (ParseTreeUtil.isWildcardTokenProxyRule(ruleName)) {
            Logger.debug("  -> [SKIP] synthetic labeled-dot stand-in");
            currentRule = null;
            super.enterParserRuleSpec(ctx);
            return;
        }

        Optional<RuleClass> currentRuleOpt = model.getRuleClasses().stream().
                filter(rc->Objects.equals(rc.getName(),ruleName)).findAny();

        if(currentRuleOpt.isPresent()) {
            currentRule = currentRuleOpt.get();
            Logger.debug("  -> [UPDATE] merging with existing rule '"+ruleName+"'");
        } else {
            currentRule = RuleClass.newBuilder().withName(ruleName).build();
        }

        // first rule is root
        currentRule.setRoot(model.getRuleClasses().isEmpty());
        currentRule.setCodeRange(ParseTreeUtil.ctxToCodeRange(ctx));
        model.getRuleClasses().add(currentRule);
        superClassRule = currentRule;

        super.enterParserRuleSpec(ctx);
    }


    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        if (ctx.identifier() != null) {

            String ruleName = ctx.identifier().getText();

            Logger.debug("-> labeled-alt-rule: " + ruleName);

            Optional<RuleClass> currentRuleOpt = model.getRuleClasses().stream().
                    filter(rc->Objects.equals(rc.getName(),ruleName)).findAny();

            if(currentRuleOpt.isPresent()) {
                currentRule = currentRuleOpt.get();
                Logger.debug("  -> [UPDATE] merging with existing rule '"+ruleName+"'");
            } else {
                currentRule = RuleClass.newBuilder().withName(ruleName).build();
            }

            model.getRuleClasses().add(currentRule);

            currentRule.setCodeRange(ParseTreeUtil.ctxToCodeRange(ctx));

            if (superClassRule != null) {
                Logger.debug("  -> setting superRuleCls: " + superClassRule.nameWithLower());
                currentRule.setSuperClass(superClassRule);
            }
        }

        super.enterLabeledAlt(ctx);
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        if (currentRule == null) {
            super.enterAlternative(ctx);
            return;
        }

        InitRulePropertiesTask task = new InitRulePropertiesTask(
                this.ruleClassesByName, currentRule, typeMappings, ctx.element());
        this.initPropertyTasks.add(task);

        super.enterAlternative(ctx);
    }

    @Override
    public void enterGrammarSpec(ANTLRv4Parser.GrammarSpecContext ctx) {
        Logger.debug("------------------------------------------------------");
        Logger.debug("Enter Grammar '" + ctx.identifier().getText() + "'");
        Logger.debug("------------------------------------------------------");

        model.setGrammarName(ctx.identifier().getText());

        super.enterGrammarSpec(ctx);
    }

    @Override
    public void enterOptionsSpec(ANTLRv4Parser.OptionsSpecContext ctx) {
        Logger.debug("------------------------------------------------------");
        Logger.debug("Enter OptionsSpec");
        Logger.debug("------------------------------------------------------");

        model.setOptions(Options.newBuilder().build());

        super.enterOptionsSpec(ctx);
    }

    @Override
    public void enterOption(ANTLRv4Parser.OptionContext ctx) {
        Logger.debug("------------------------------------------------------");
        Logger.debug("Enter Option");
        Logger.debug("------------------------------------------------------");

        var optionsName = ctx.identifier().getText();

        if("superClass".equals(optionsName)) {
            var superClassName = ctx.optionValue().getText();
            Logger.debug(" -> setting superClass: " + superClassName);
            model.getOptions().setSuperClassName(superClassName);
        } else {
            Logger.debug(" -> ignoring option: " + optionsName);
        }
    }

    @Override
    public void exitGrammarSpec(ANTLRv4Parser.GrammarSpecContext ctx) {
        Logger.debug("------------------------------------------------------");
        Logger.debug("Exit Grammar '" + ctx.identifier().getText() + "'");
        Logger.debug("------------------------------------------------------");

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

            List<Property> properties = elements.stream().filter(e -> e.labeledElement() != null).
                filter(e -> e.labeledElement().identifier() != null).
                map(e -> GrammarToModelListener.elementToProperty(
                        rules, cls.nameWithUpper(), typeMappings, e)).
                collect(Collectors.toList());

             // filter duplicate properties
            List<Property> props = new ArrayList<>(properties);
            for(Property p1 : props) {
                for(Property p2 : props) {
                    if(p1!=p2 && Objects.equals(p1.getName(),p2.getName())) {
                        properties.remove(p1);
                    }
                }
            }

            // filter duplicate properties
            props = new ArrayList<>(properties);
            for(Property p1 : props) {
                for(Property p2 : cls.getProperties()) {
                    if(p1!=p2 && Objects.equals(p1.getName(),p2.getName())) {
                        properties.remove(p1);
                    }
                }
            }

            cls.getProperties().addAll(
                properties
            );
        }
    }
}
