package py3trial;

import py3trial.parser.Python3ModelParser;
import py3trial.parser.Python3TriviaSupport;
import py3trial.unparser.Formatter;
import py3trial.unparser.Python3ModelUnparser;

/**
 * Build experiment (#60): blocks built from scratch. A Python block is
 * {@code NEWLINE INDENT stmt+ DEDENT}, so a programmatic block carries the
 * three tokens as properties. {@code Python3TriviaSupport.fillIndentationTokens}
 * sets them by nesting depth (continuing the indentation of a parsed block the
 * new one sits in), and {@code Formatter.newIndentationTokenFormatter()} places
 * the statements that follow a NEWLINE at that depth. Parsed text stays exact.
 */
public class Build {
    public static void main(String[] args) {
        String nl = String.valueOf((char) 10);
        Python3Model target = new Python3ModelParser().parse(
                "x = 1" + nl + "class A:" + nl + "    y = 2" + nl);

        // a function built from scratch, appended to the module (depth 0)
        Funcdef f = funcdef("f", pass(), pass());
        target.getRoot().getFileInputChoices().add(FileInputChoiceAlt2.newBuilder().withStmt(compound(f)).build());

        // a statement and a nested function appended to the parsed class body (depth 1)
        Classdef a = target.getRoot().vmf().content().stream(Classdef.class).findFirst().get();
        BlockAlt2 body = (BlockAlt2) a.getBlock();
        body.getStmts().add(pass());
        body.getStmts().add(compound(funcdef("g", pass())));

        System.out.println("before filling (tokens unset): the unparser rejects the model, a block without its NEWLINE is not a block");
        try {
            System.out.println(show(new Python3ModelUnparser().unparse(target)));
        } catch (RuntimeException ex) {
            System.out.println("    " + ex.getMessage());
        }

        int filled = Python3TriviaSupport.fillIndentationTokens(target.getRoot(), "    ");
        System.out.println("fillIndentationTokens set " + filled + " token values; INDENT of f's body = '"
                + ((BlockAlt2) f.getBlock()).getIndent() + "', of g's body = '" + ((BlockAlt2) funcdefNamed(body, "g").getBlock()).getIndent() + "'");

        Python3ModelUnparser u = new Python3ModelUnparser();
        u.setFormatter(Formatter.newIndentationTokenFormatter());
        String out = u.unparse(target);
        System.out.println("with the indentation-token formatter:");
        System.out.println(show(out));
        System.out.println("programmatic ranges: " + u.getFormatter().programmaticRanges().size());

        String again = new Python3ModelUnparser().unparse(new Python3ModelParser().parse(out));
        System.out.println("re-parsed output unparses byte-exact: " + out.equals(again));
        System.out.println("idempotent (second fill sets nothing): " + (Python3TriviaSupport.fillIndentationTokens(target.getRoot(), "\t") == 0));
    }

    /** {@code def name(): stmts...} */
    static Funcdef funcdef(String name, Stmt... stmts) {
        return Funcdef.newBuilder()
                .withDefName(NameAlt1.newBuilder().withName(name).build())
                .withDefParameters(Parameters.newBuilder().build())
                .withBlock(BlockAlt2.newBuilder().withStmts(stmts).build())
                .build();
    }

    /** {@code pass} as a statement: stmt → simple_stmts → simple_stmt → pass_stmt */
    static Stmt pass() {
        Simple_stmt simple = Simple_stmt.newBuilder()
                .withSimpleStmtChoice(SimpleStmtChoiceAlt3.newBuilder().withPassStmt(Pass_stmt.newBuilder().build()).build())
                .build();
        return StmtAlt1.newBuilder().withSimpleStmts(Simple_stmts.newBuilder().withSimpleStmt(simple).build()).build();
    }

    static Stmt compound(Funcdef f) {
        return StmtAlt2.newBuilder().withCompoundStmt(Compound_stmtAlt6.newBuilder().withFuncdef(f).build()).build();
    }

    static Funcdef funcdefNamed(BlockAlt2 block, String name) {
        return block.vmf().content().stream(Funcdef.class)
                .filter(fd -> name.equals(((NameAlt1) fd.getDefName()).getName())).findFirst().get();
    }

    static String show(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split(String.valueOf((char) 10), -1)) {
            sb.append("    |").append(line.replace("\t", "<TAB>")).append("|").append((char) 10);
        }
        return sb.toString();
    }
}
