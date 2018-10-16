package eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.parser.NestedUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.unparser.NestedUnnamedModelUnparser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class TestUnamedOptionalsComlex {

    @Test
    public void testNestedUnnamedOptionals() {
        String code = "r1 (), r2 (test123)";

        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(code);

        model.vmf().content().stream(RuleWithOptionals2.class).forEach(r -> {
            ParseTreeListener l = new ParseTreeListener() {
                @Override
                public void visitTerminal(TerminalNode terminalNode) {
                    System.out.println("TERMINAL: "+terminalNode.getSymbol().getType());
                }

                @Override
                public void visitErrorNode(ErrorNode errorNode) {

                }

                @Override
                public void enterEveryRule(ParserRuleContext parserRuleContext) {
                    System.out.println("rule: #children" + parserRuleContext.getChildCount());

                    for (int i = 0; i < parserRuleContext.getChildCount(); i++) {
                        System.out.println(" -> child: " + parserRuleContext.getChild(i));
                    }

                    System.out.println(" -> alt-number: "+parserRuleContext.getAltNumber());
                    System.out.println(" -> rule-index: "+parserRuleContext.getRuleIndex());
                }

                @Override
                public void exitEveryRule(ParserRuleContext parserRuleContext) {

                }
            };

            ParserRuleContext ctx =
                    (ParserRuleContext) ((Map<String,Object>)r.getPayload()).get("vmf-text:antlr4-rule-ctx");
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(l,ctx);
        });

        // store system.err as string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream prev = System.err;
        System.setErr(ps);

        String newCode = new NestedUnnamedModelUnparser().unparse(model);

        String errors = baos.toString();

        Assert.assertTrue("No error must be reported.\n -> Output:\n" + errors, errors.isEmpty());
        Assert.assertEquals(code, newCode);

        // stop recording system.err
        System.out.flush();
        System.setErr(prev);
    }

}
