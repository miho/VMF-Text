package eu.mihosoft.vmftext.tests.indent;

import eu.mihosoft.vmftext.tests.indent.parser.IndentModelParser;
import eu.mihosoft.vmftext.tests.indent.parser.IndentTriviaSupport;
import eu.mihosoft.vmftext.tests.indent.unparser.Formatter;
import eu.mihosoft.vmftext.tests.indent.unparser.IndentModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Issue #60: indentation-structured languages. NEWLINE / INDENT / DEDENT are
 * tokens, so a block built from scratch gets its token values from
 * {@code fillIndentationTokens} and the statements inside it are placed by
 * {@code Formatter.newIndentationTokenFormatter()}; parsed text stays exact.
 */
public class IndentTest {

    private static final String SRC = "x = 1\nif x:\n    f(x, 2)\n    if y:\n        pass\n    return x\nelse:\n    pass\ng()\n";

    private static NameExpr name(String n) {
        return NameExpr.newBuilder().withName(n).build();
    }

    private static NumExpr num(String n) {
        return NumExpr.newBuilder().withNum(n).build();
    }

    private static AssignStmt assign(String target, Expr value) {
        return AssignStmt.newBuilder().withTarget(target).withValue(value).build();
    }

    private static CallStmt call(String callee, Expr... args) {
        return CallStmt.newBuilder().withCallee(callee).withArgs(args).build();
    }

    private static ReturnStmt ret(Expr value) {
        return ReturnStmt.newBuilder().withValue(value).build();
    }

    private static PassStmt pass() {
        return PassStmt.newBuilder().build();
    }

    private static Block block(Stmt... stmts) {
        return Block.newBuilder().withStmts(stmts).build();
    }

    private static IfStmt ifStmt(Expr cond, Block body) {
        return IfStmt.newBuilder().withCond(cond).withBody(body).build();
    }

    private static IndentModel model(Stmt... stmts) {
        return IndentModel.newBuilder().withRoot(Unit.newBuilder().withStmts(stmts).build()).build();
    }

    private static String unparse(IndentModel model) {
        IndentModelUnparser unparser = new IndentModelUnparser();
        unparser.setFormatter(Formatter.newIndentationTokenFormatter());
        return unparser.unparse(model);
    }

    @Test
    public void parsedTextRoundTripsExactly() {
        Assert.assertEquals(SRC, new IndentModelUnparser().unparse(new IndentModelParser().parse(SRC)));
        String withTrivia = "x = 1\n\n# comment\nif x:\n    pass  # trailing\n\n    return 1\n  \ny = 2";
        Assert.assertEquals(withTrivia, new IndentModelUnparser().unparse(new IndentModelParser().parse(withTrivia)));
        IndentModel parsed = new IndentModelParser().parse(SRC);
        IfStmt ifs = (IfStmt) parsed.getRoot().getStmts().get(1);
        Assert.assertEquals("\n", ifs.getBody().getNl());
        Assert.assertEquals("    ", ifs.getBody().getIndent());
        Assert.assertEquals("", ifs.getBody().getDedent());
        Assert.assertEquals("        ", ((IfStmt) ifs.getBody().getStmts().get(1)).getBody().getIndent());
    }

    @Test
    public void tokenPropertiesAreAnnotatedWithTheirTokenName() {
        Block block = block(pass());
        java.util.Map<String, String> tokens = new java.util.TreeMap<>();
        for (eu.mihosoft.vmf.runtime.core.Property p : block.vmf().reflect().properties()) {
            String token = IndentTriviaSupport.tokenOf(p);
            if (token != null) {
                tokens.put(p.getName(), token);
            }
        }
        Assert.assertEquals("{dedent=DEDENT, indent=INDENT, nl=NEWLINE}", tokens.toString());
        Assert.assertEquals("vmf-text:token", IndentTriviaSupport.TOKEN_ANNOTATION_KEY);
        Assert.assertNull(IndentTriviaSupport.tokenOf(block.vmf().reflect().propertyByName("stmts").get()));
    }

    @Test
    public void programmaticModelIsIndentedByNestingDepth() {
        IfStmt inner = ifStmt(name("y"), block(pass()));
        IfStmt outer = IfStmt.newBuilder().withCond(name("x"))
                .withBody(block(call("f", name("x"), num("2")), inner, ret(name("x"))))
                .withElseBody(block(pass())).build();
        IndentModel model = model(assign("x", num("1")), outer, call("g"));
        int filled = IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "    ");
        // 6 statement NEWLINEs + 3 tokens for each of the 3 blocks
        Assert.assertEquals(15, filled);
        Assert.assertEquals("    ", outer.getBody().getIndent());
        Assert.assertEquals("        ", inner.getBody().getIndent());
        Assert.assertEquals("", inner.getBody().getDedent());
        Assert.assertEquals("\n", inner.getBody().getNl());
        Assert.assertEquals(SRC, unparse(model));
        // idempotent: nothing left to fill, output unchanged
        Assert.assertEquals(0, IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "\t"));
        Assert.assertEquals(SRC, unparse(model));
        Assert.assertEquals(SRC, new IndentModelUnparser().unparse(new IndentModelParser().parse(SRC)));
    }

    @Test
    public void indentUnitAndLineBreakAreConfigurable() {
        IndentModel model = model(ifStmt(name("x"), block(pass(), ifStmt(name("y"), block(ret(num("1")))))));
        IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "\r\n", "\t", "NEWLINE", "INDENT", "DEDENT");
        Assert.assertEquals("if x:\r\n\tpass\r\n\tif y:\r\n\t\treturn 1\r\n", unparse(model));
    }

    @Test
    public void statementsAppendedToAParsedBlockTakeItsIndentation() {
        IndentModel model = new IndentModelParser().parse("if x:\n  pass\n");
        Block body = ((IfStmt) model.getRoot().getStmts().get(0)).getBody();
        body.getStmts().add(ret(name("x")));
        body.getStmts().add(ifStmt(name("y"), block(call("f", num("1")))));
        // 2 statement NEWLINEs + the 3 tokens of the new block
        Assert.assertEquals(5, IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "  "));
        // the new block continues the parsed two-space indentation
        Assert.assertEquals("    ", ((IfStmt) body.getStmts().get(2)).getBody().getIndent());
        IndentModelUnparser unparser = new IndentModelUnparser();
        unparser.setFormatter(Formatter.newIndentationTokenFormatter());
        Assert.assertEquals("if x:\n  pass\n  return x\n  if y:\n    f(1)\n", unparser.unparse(model));
        Assert.assertFalse(unparser.getFormatter().programmaticRanges().isEmpty());
    }

    @Test
    public void statementAppendedToAnUnterminatedFileStartsOnANewLine() {
        IndentModel model = new IndentModelParser().parse("x = 1");
        Assert.assertEquals("", ((AssignStmt) model.getRoot().getStmts().get(0)).getNl()); // the virtual NEWLINE
        model.getRoot().getStmts().add(assign("y", num("2")));
        IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "    ");
        Assert.assertEquals("x = 1\ny = 2\n", unparse(model));
    }

    @Test
    public void programmaticBlockInsertedAtTopLevelOfAParsedFile() {
        IndentModel model = new IndentModelParser().parse("x = 1\nif x:\n    pass\n");
        model.getRoot().getStmts().add(1, ifStmt(name("y"), block(ret(name("y")))));
        IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "    ");
        Assert.assertEquals("x = 1\nif y:\n    return y\nif x:\n    pass\n", unparse(model));
    }

    @Test
    public void explicitTokenValuesAreKept() {
        Block body = Block.newBuilder().withIndent("\t").withStmts(pass()).build();
        IndentModel model = model(ifStmt(name("x"), body));
        Assert.assertEquals(3, IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "    "));
        Assert.assertEquals("\t", body.getIndent());
        Assert.assertEquals("if x:\n\tpass\n", unparse(model));
    }

    @Test
    public void unchangedParsedModelHasNoRangesAndStaysExact() {
        IndentModel model = new IndentModelParser().parse(SRC);
        Assert.assertEquals(0, IndentTriviaSupport.fillIndentationTokens(model.getRoot(), "    "));
        IndentModelUnparser unparser = new IndentModelUnparser();
        unparser.setFormatter(Formatter.newIndentationTokenFormatter());
        Assert.assertEquals(SRC, unparser.unparse(model));
        Assert.assertTrue(unparser.getFormatter().programmaticRanges().isEmpty());
    }
}
