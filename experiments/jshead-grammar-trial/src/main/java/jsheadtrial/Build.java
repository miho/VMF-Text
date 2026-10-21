package jsheadtrial;

import eu.mihosoft.vmf.runtime.core.Property;
import jsheadtrial.parser.JavaScriptModelParser;
import jsheadtrial.parser.JavaScriptTriviaSupport;
import jsheadtrial.unparser.JavaScriptModelUnparser;

/**
 * #73: the names auto-labeling gives a rule/token name clash (parser rule
 * {@code identifier}, token {@code Identifier}), seen from code that works
 * with the model: reading and editing a parsed program, building one from
 * scratch with the builders, and VMF reflection.
 */
public class Build {
    public static void main(String[] args) {
        String nl = String.valueOf((char) 10);

        // 1) read and edit a parsed model
        JavaScriptModel parsed = new JavaScriptModelParser().parse("outer: for (;;) { continue outer; }" + nl);
        ContinueStatement cont = parsed.getRoot().vmf().content().stream(ContinueStatement.class).findFirst().get();
        LabelledStatement labelled = parsed.getRoot().vmf().content().stream(LabelledStatement.class).findFirst().get();
        IdentifierAlt1 target = (IdentifierAlt1) cont.getIdentifierNode();  // the rule reference: an Identifier node
        System.out.println("continue target: " + target.getIdentifierToken()); // the token reference: its text
        target.setIdentifierToken("again");
        ((IdentifierAlt1) labelled.getIdentifierNode()).setIdentifierToken("again");
        System.out.println("edited: " + show(new JavaScriptModelUnparser().unparse(parsed)));

        // 2) build `stop: break stop;` from scratch
        BreakStatement brk = BreakStatement.newBuilder()
                .withIdentifierNode(IdentifierAlt1.newBuilder().withIdentifierToken("stop").build())
                .withBreakEos(EosAlt1.newBuilder().build())
                .build();
        LabelledStatement stop = LabelledStatement.newBuilder()
                .withIdentifierNode(IdentifierAlt1.newBuilder().withIdentifierToken("stop").build())
                .withStatement(StatementAlt12.newBuilder().withBreakStatement(brk).build())
                .build();
        JavaScriptModel built = JavaScriptModel.newBuilder().withRoot(Program.newBuilder()
                .withSourceElements(SourceElements.newBuilder()
                        .withSourceElements(SourceElement.newBuilder()
                                .withStatement(StatementAlt16.newBuilder().withLabelledStatement(stop).build())
                                .build())
                        .build())
                .build()).build();
        String out = new JavaScriptModelUnparser().unparse(built);
        System.out.println("built:  " + show(out));
        String again = new JavaScriptModelUnparser().unparse(new JavaScriptModelParser().parse(out));
        System.out.println("re-parsed output unparses byte-exact: " + out.equals(again));

        // 3) reflection: which property holds which token
        for (Object o : new Object[]{cont, target}) {
            System.out.println(o.getClass().getInterfaces()[0].getSimpleName() + ":");
            for (Property p : ((eu.mihosoft.vmf.runtime.core.VObject) o).vmf().reflect().properties()) {
                if (p.getName().equals("parent") || p.getName().equals("payload") || p.getName().equals("codeRange")
                        || p.getName().equals("lexicalInfo") || p.getName().equals("ruleMapShellLexicalInfo")) {
                    continue;
                }
                String token = JavaScriptTriviaSupport.tokenOf(p);
                System.out.println("  " + p.getName() + " : " + p.getType().getName().replace("jsheadtrial.", "")
                        + (token == null ? "" : "   (token " + token + ")"));
            }
        }
    }

    static String show(String s) {
        return "\"" + s.replace(String.valueOf((char) 10), "<NL>") + "\"";
    }
}
