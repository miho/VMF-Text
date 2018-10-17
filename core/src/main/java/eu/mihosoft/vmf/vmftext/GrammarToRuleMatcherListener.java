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

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vcollections.VListChange;
import eu.mihosoft.vmf.runtime.core.Change;
import eu.mihosoft.vmf.runtime.core.ChangeListener;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseVisitor;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;
import vjavax.observer.collection.CollectionChangeEvent;
import vjavax.observer.collection.CollectionChangeListener;

import java.util.*;

class GrammarToRuleMatcherListener extends ANTLRv4ParserBaseListener {

    private TokenStream stream;

    private Deque<String> currentRuleNames = new ArrayDeque<>();
    private UPElement currentElement = null;
    private ANTLRv4Parser.LabeledAltContext currentAlt = null;
    private Deque<UPRuleBase> currentRules = new ArrayDeque<>();
    private Deque<AlternativeBase> currentAlts = new ArrayDeque<>();

    private final UnparserModel model = UnparserModel.newInstance();


    public GrammarToRuleMatcherListener(TokenStream stream) {
        this.stream = stream;
    }

    public UnparserModel getModel() {
        return model;
    }

    private UPRuleBase getCurrentRule() {
        return currentRules.peek();
    }

    private static boolean debug = false;

    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {

        if(debug)
        System.out.println("entering alt (l): " + ctx.getText());

        currentAlt = ctx;

        UPRuleBase currentRule = getCurrentRule();
        AlternativeBase alt;

        if(ctx.identifier()==null) {
            alt = Alternative.newBuilder().
                    withText(stream.getText(ctx.getSourceInterval())).build();
        } else {
            if(debug)
            System.out.println(" -> labeled: " + ctx.identifier().getText());
            alt = LabeledAlternative.newBuilder().
                    withName(ctx.identifier().getText()).
                    withText(stream.getText(ctx.getSourceInterval())).build();
        }
        currentRule.getAlternatives().add(alt);

        if(debug)
        System.out.println(" -> alt id: " + alt.getAltId());

        currentAlts.push(alt);

        super.enterLabeledAlt(ctx);
    }

//    TODO test whether endsWith('(') in enterElement() is really enough for detecting sub-rules 21.12.2017
//    @Override
//    public void enterBlock(ANTLRv4Parser.BlockContext ctx) {
//
//        // the block might not have identical source interval because of potential labels
//        // that's why we check for existing currentElement which will be added if we are in the
//        // "element-might-be-a-block" case
//        if(currentElement == null) return; // already covered via enterElement(ctx)
//
//        System.out.println(">>>  E-BLOCK:     "+stream.getText(ctx.getSourceInterval()));
//
//        String elementText = stream.getText(ctx.getSourceInterval());
//
//        // remove the current element since the element is actually a block
//        // which will be readded as sub-rule element below
//        //
//        // NOTE: this case actually only occurs for unnamed blocks
//        currentElement.getParentAlt().getElements().remove(currentElement);
//
//        UPSubRuleElement subRule = UPSubRuleElement.newBuilder().
//                withText(elementText).
//                build();
//
//        AlternativeBase currentAlt = currentAlts.peek();
//        currentAlt.getElements().add(subRule);
//        currentRules.push(subRule);
//
//        System.out.println(" -> sub-rule id: " + subRule.getId());
//
//        super.enterBlock(ctx);
//    }

    @Override
    public void exitBlock(ANTLRv4Parser.BlockContext ctx) {
        currentRules.pop();

        super.exitBlock(ctx);
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        // do not enter alt if we already processed as labeled alt
        if(currentAlt!=null&&currentAlt.alternative()==ctx) return;

        if(debug)
        System.out.println("entering alt (*): " + ctx.getText());

        UPRuleBase currentRule = getCurrentRule();

        AlternativeBase alt = Alternative.newBuilder().
                withText(stream.getText(ctx.getSourceInterval())).build();

        currentRule.getAlternatives().add(alt);

        if(debug)
        System.out.println(" -> alt id: " + alt.getAltId());

        currentAlts.push(alt);

        super.enterAlternative(ctx);
    }

    @Override
    public void exitAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        AlternativeBase a = currentAlts.pop();

        if(debug)
        System.out.println("exiting alt:      " + ctx.getText());

        super.exitAlternative(ctx);
    }

    @Override
    public void enterElement(ANTLRv4Parser.ElementContext ctx) {

        AlternativeBase currentAlt = currentAlts.peek();
        String elementText = stream.getText(ctx.getSourceInterval());

        if (ParseTreeUtil.isLabeledElement(ctx)) {
            String propertyName = ctx.labeledElement().identifier().getText();

            boolean listType = ctx.labeledElement().PLUS_ASSIGN()!=null;

            if(ctx.labeledElement().block()!=null) {

                if(debug)
                System.out.println(">>> LE-BLOCK:     "+elementText);

                UPNamedSubRuleElement subRule = UPNamedSubRuleElement.newBuilder().
                        withText(elementText).
                        withName(propertyName).
                        withListType(listType).
                        withParserRule(ParseTreeUtil.isParserRule(ctx)).
                        withLexerRule(ParseTreeUtil.isLexerRule(ctx)).
                        withTerminal(ParseTreeUtil.isStringLiteral(ctx)).
                        withNegated(ParseTreeUtil.isNegated(ctx)).
                        build();

                if(subRule.isParserRule()||subRule.isLexerRule()) {
                    subRule.setRuleName(StringUtil.firstToUpper(ParseTreeUtil.getElementText(ctx)));
                }

                currentAlt.getElements().add(subRule);

                currentRules.push(subRule);

                // if sub-rule is a block-set we need to manually add alternatives
                if(debug)
                System.out.println(" -> sub-rule id: " + subRule.getRuleId());
            } else {
                if(debug)
                System.out.println(">>> LE:           " +elementText);

                UPNamedElement namedElement = UPNamedElement.newBuilder().
                        withName(propertyName).
                        withText(stream.getText(ctx.getSourceInterval())).
                        withListType(listType).
                        withParserRule(ParseTreeUtil.isParserRule(ctx)).
                        withLexerRule(ParseTreeUtil.isLexerRule(ctx)).
                        withTerminal(ParseTreeUtil.isStringLiteral(ctx)).
                        withNegated(ParseTreeUtil.isNegated(ctx)).
                        build();

                if(namedElement.isParserRule()||namedElement.isLexerRule()) {
                    namedElement.setRuleName(StringUtil.firstToUpper(ParseTreeUtil.getElementText(ctx)));
                }

                currentAlt.getElements().add(namedElement);

            }

        } else {

            if(ParseTreeUtil.isBlockElement(ctx)) {

                if(debug)
                System.out.println(">>>  E-BLOCK:     "+stream.getText(ctx.getSourceInterval()));

                UPSubRuleElement subRule = UPSubRuleElement.newBuilder().
                        withText(elementText).
                        withParserRule(ParseTreeUtil.isParserRule(ctx)).
                        withLexerRule(ParseTreeUtil.isLexerRule(ctx)).
                        withTerminal(ParseTreeUtil.isStringLiteral(ctx)).
                        withNegated(ParseTreeUtil.isNegated(ctx)).
                        build();

                if(subRule.isParserRule()||subRule.isLexerRule()) {
                    subRule.setRuleName(StringUtil.firstToUpper(ParseTreeUtil.getElementText(ctx)));
                }

                currentAlt.getElements().add(subRule);

                if(debug)
                System.out.println(" -> sub-rule id: " + subRule.getRuleId());

                currentRules.push(subRule);
            } else {



                // we set the current element since we might hit a sub-rule element
                // which will be readded via enterBlock(ctx)

                if(elementText.startsWith("(")) {
                    if(debug)
                    System.out.println(">>>  E-BLOCK(*):  " + stream.getText(ctx.getSourceInterval()));
                    UPSubRuleElement subRule = UPSubRuleElement.newBuilder().
                            withText(elementText).
                            withParserRule(ParseTreeUtil.isParserRule(ctx)).
                            withLexerRule(ParseTreeUtil.isLexerRule(ctx)).
                            withTerminal(ParseTreeUtil.isStringLiteral(ctx)).
                            withNegated(ParseTreeUtil.isNegated(ctx)).
                            build();

                    if(subRule.isParserRule()||subRule.isLexerRule()) {
                        subRule.setRuleName(StringUtil.firstToUpper(ParseTreeUtil.getElementText(ctx)));
                    }

                    currentAlt.getElements().add(subRule);

                    if(debug)
                    System.out.println(" -> sub-rule id: " + subRule.getRuleId());

                    currentRules.push(subRule);
                } else {
                    if(debug)
                    System.out.println(">>>  E:           " + stream.getText(ctx.getSourceInterval()));

                    currentElement = UPElement.newBuilder().
                                withText(stream.getText(ctx.getSourceInterval())).
                                withParserRule(ParseTreeUtil.isParserRule(ctx)).
                                withLexerRule(ParseTreeUtil.isLexerRule(ctx)).
                                withTerminal(ParseTreeUtil.isStringLiteral(ctx)).
                                withNegated(ParseTreeUtil.isNegated(ctx)).
                                build();

                    if(currentElement.isParserRule()||currentElement.isLexerRule()) {
                        currentElement.setRuleName(StringUtil.firstToUpper(ParseTreeUtil.getElementText(ctx)));
                    }

                    currentAlt.getElements().add(currentElement);
                }

            }

        }

        super.enterElement(ctx);
    }


    @Override
    public void exitElement(ANTLRv4Parser.ElementContext ctx) {

        currentElement = null;

        super.exitElement(ctx);
    }

    @Override
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {

        String ruleName = ctx.RULE_REF().getText();
        if(debug)
        System.out.println("> entering rule:  '" + ruleName + "'");
        currentRuleNames.push(ruleName);

        UPRule rule = UPRule.newBuilder().withName(ruleName).build();

        currentRules.push(rule);
        model.getRules().add(rule);
        if(debug)
        System.out.println(" -> rule id: " + rule.getRuleId());

        super.enterParserRuleSpec(ctx);
    }

    @Override
    public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        if(debug)
        System.out.println("< exiting rule:   '" + ((UPRule)currentRules.pop()).getName() + "'");

        super.exitParserRuleSpec(ctx);
    }

    @Override
    public void enterLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {

        if(debug)
        System.out.println("> entering lexer rule:   '" + ctx.TOKEN_REF().getText() + "'");

        UPLexerRule lr = UPLexerRule.newBuilder().
                withName(ctx.TOKEN_REF().getText()).
                withText(stream.getText(ctx.lexerRuleBlock())).build();

        model.getLexerRules().add(lr);

        super.enterLexerRuleSpec(ctx);
    }

    @Override
    public void exitLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {

        if(debug)
            System.out.println("< exiting lexer rule:   '" + ctx.TOKEN_REF().getText() + "'");

        super.exitLexerRuleSpec(ctx);
    }
}
