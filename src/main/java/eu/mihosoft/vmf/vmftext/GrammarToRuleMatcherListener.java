package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseVisitor;
import org.antlr.v4.runtime.TokenStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

class GrammarToRuleMatcherVisitor extends ANTLRv4ParserBaseVisitor {
    @Override
    public Object visitAlternative(ANTLRv4Parser.AlternativeContext ctx) {


        return super.visitAlternative(ctx);
    }
}

class GrammarToRuleMatcherListener extends ANTLRv4ParserBaseListener {

    private TokenStream stream;

    private Stack<String> currentRuleNames = new Stack<>();
    private Stack<ANTLRv4Parser.AlternativeContext> insideAlternative = new Stack<>();

    private boolean currentAltIsLabeled = false;

    private final Map<String, Integer> rulesAltNames = new HashMap<>();


    public GrammarToRuleMatcherListener(TokenStream stream) {
        this.stream = stream;
    }

    private int newRuleAltIndex(String ruleName) {
        Integer id = rulesAltNames.get(ruleName);

        if (id == null) {
            id = 0;

        } else {
            id++;
        }

        rulesAltNames.put(ruleName, id);

        return id;
    }

    @Override
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        if(ctx.identifier()==null) return;
        System.out.println("!!!LABELED: " + ctx.identifier().getText());
        super.enterLabeledAlt(ctx);
    }

    @Override
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {

        final String parentRuleName = currentRuleNames.peek();

        int ruleAltIndex = newRuleAltIndex(parentRuleName);

        System.out.print("vmf_matcher_rule_" + parentRuleName + "_alt_" + ruleAltIndex + " : ");

        currentRuleNames.push(parentRuleName + "_alt_" + ruleAltIndex);

        System.out.print(stream.getText(ctx.getSourceInterval()));

        System.out.println();

        super.enterAlternative(ctx);
    }

    @Override
    public void enterElement(ANTLRv4Parser.ElementContext ctx) {

        System.out.println(">>> "+stream.getText(ctx.getSourceInterval()));

        if (!ParseTreeUtil.isLabeledElement(ctx)) {
            super.enterElement(ctx);
            return;
        }

        System.out.println("!!!!!ENTER----: " + ctx.getText());

        String labelName = ctx.labeledElement().identifier().getText();

        final String parentRuleName = currentRuleNames.peek();

        String ruleName = parentRuleName + "_" + labelName;

        int ruleAltIndex = newRuleAltIndex(ruleName);

        String nextCurrentRule = ruleName + "_occ_" + ruleAltIndex;

        currentRuleNames.push(nextCurrentRule);

        System.out.print("vmf_validator_rule_" + nextCurrentRule + ": ");
        System.out.println(stream.getText(ctx.getSourceInterval()));

        super.enterElement(ctx);
    }

    @Override
    public void exitElement(ANTLRv4Parser.ElementContext ctx) {

        if (!ParseTreeUtil.isLabeledElement(ctx)) {
            super.exitElement(ctx);
            return;
        }

        currentRuleNames.pop();

        System.out.println("!!!!!EXIT----: " + ctx.getText());


        super.exitElement(ctx);
    }

    @Override
    public void exitAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        //       System.out.println(" < "+insideAlternative.pop().getText());

        currentRuleNames.pop();

        super.exitAlternative(ctx);
    }

    @Override
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {

        String ruleName = ctx.RULE_REF().getText();
        System.out.println("> entering rule '" + ruleName + "'");
        currentRuleNames.push(ruleName);
        super.enterParserRuleSpec(ctx);
    }

    @Override
    public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        System.out.println("< exiting rule '" + currentRuleNames.pop() + "'");
        super.exitParserRuleSpec(ctx);
    }

}
