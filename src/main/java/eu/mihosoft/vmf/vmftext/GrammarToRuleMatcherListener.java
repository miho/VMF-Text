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

class GrammarToRuleMatcherVisitor extends ANTLRv4ParserBaseVisitor {
    @Override
    public Object visitAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        return super.visitAlternative(ctx);
    }
}

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

    private UPRule getCurrentRule() {

        if(model.getRules().isEmpty()) {
            throw new RuntimeException("Cannot access current rule (does not exist)");
        }

        return model.getRules().get(model.getRules().size()-1);
    }

    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {

        System.out.println("entering alt (l): " + ctx.getText());

        currentAlt = ctx;

        UPRule currentRule = getCurrentRule();
        AlternativeBase alt;

        if(ctx.identifier()==null) {
            alt = Alternative.newBuilder().
                    withText(stream.getText(ctx.getSourceInterval())).build();
        } else {
            System.out.println(" -> labeled: " + ctx.identifier().getText());
            alt = LabeledAlternative.newBuilder().
                    withName(ctx.identifier().getText()).
                    withText(stream.getText(ctx.getSourceInterval())).build();
        }
        currentRule.getAlternatives().add(alt);

        System.out.println(" -> alt id: " + alt.getId());

        currentAlts.push(alt);

        super.enterLabeledAlt(ctx);
    }

    @Override
    public void enterBlock(ANTLRv4Parser.BlockContext ctx) {

        // the block might not have identical source interval because of potential labels
        // that's why we check for existing currentElement which will be added if we are in the
        // "element-might-be-a-block" case
        if(currentElement == null) return; // already covered via enterElement(ctx)

        System.out.println(">>>  E-BLOCK:     "+stream.getText(ctx.getSourceInterval()));

        String elementText = stream.getText(ctx.getSourceInterval());

        // remove the current element since the element is actually a block
        // which will be readded as subrule element below
        //
        // NOTE: this case actually only occurs for unnamed blocks
        currentElement.getParentAlt().getElements().remove(currentElement);

        UPSubRuleElement subRule = UPSubRuleElement.newBuilder().
                withText(elementText).
                build();

        AlternativeBase currentAlt = currentAlts.peek();
        currentAlt.getElements().add(subRule);
        currentRules.push(subRule);

        System.out.println(" -> subrule id: " + subRule.getId());

        super.enterBlock(ctx);
    }

    @Override
    public void exitBlock(ANTLRv4Parser.BlockContext ctx) {
        currentRules.pop();

        super.exitBlock(ctx);
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        // do not enter alt if we already processed as labeled alt
        if(currentAlt!=null&&currentAlt.alternative()==ctx) return;

        System.out.println("entering alt (*): " + ctx.getText());

        UPRule currentRule = getCurrentRule();

        AlternativeBase alt = Alternative.newBuilder().
                withText(stream.getText(ctx.getSourceInterval())).build();

        currentRule.getAlternatives().add(alt);

        System.out.println(" -> alt id: " + alt.getId());

        currentAlts.push(alt);

        super.enterAlternative(ctx);
    }

    @Override
    public void exitAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        AlternativeBase a = currentAlts.pop();

        System.out.println("exiting alt:      " + ctx.getText());

        super.exitAlternative(ctx);
    }

    @Override
    public void enterElement(ANTLRv4Parser.ElementContext ctx) {

        AlternativeBase currentAlt = currentAlts.peek();
        String elementText = stream.getText(ctx.getSourceInterval());

        if (ParseTreeUtil.isLabeledElement(ctx)) {
            String propertyName = ctx.labeledElement().identifier().getText();

            if(ctx.labeledElement().block()!=null) {

                System.out.println(">>> LE-BLOCK:     "+elementText);

                UPNamedSubRuleElement subRule = UPNamedSubRuleElement.newBuilder().
                        withText(elementText).
                        withName(propertyName).
                        build();

                currentAlt.getElements().add(subRule);

                currentRules.push(subRule);

                // if sub-rule is a block-set we need to manually add alternatives


                System.out.println(" -> subrule id: " + subRule.getId());
            } else {

                System.out.println(">>> LE:           " +elementText);

                currentAlt.getElements().add(UPNamedElement.newBuilder().
                        withName(propertyName).
                        withText(stream.getText(ctx.getSourceInterval())).
                        build());
            }

        } else {

            if(ParseTreeUtil.isBlockElement(ctx)) {

                System.out.println(">>>  E-BLOCK:     "+stream.getText(ctx.getSourceInterval()));

                UPSubRuleElement subRule = UPSubRuleElement.newBuilder().
                        withText(elementText).
                        build();

                currentAlt.getElements().add(subRule);

                System.out.println(" -> subrule id: " + subRule.getId());

                currentRules.push(subRule);
            } else {

                System.out.println(">>>  E:           " + stream.getText(ctx.getSourceInterval()));

                // we set the current element since we might hit a subrule element
                // which will be readded via enterBlock(ctx)

                currentElement = UPElement.newBuilder().
                        withText(stream.getText(ctx.getSourceInterval())).build();

                currentAlt.getElements().add(currentElement);

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
        System.out.println("> entering rule:  '" + ruleName + "'");
        currentRuleNames.push(ruleName);

        UPRule rule = UPRule.newBuilder().withName(ruleName).build();

        currentRules.push(rule);
        model.getRules().add(rule);

        System.out.println(" -> rule id: " + rule.getId());

        super.enterParserRuleSpec(ctx);
    }

    @Override
    public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        System.out.println("< exiting rule:   '" + ((UPRule)currentRules.pop()).getName() + "'");

        super.exitParserRuleSpec(ctx);
    }

}
