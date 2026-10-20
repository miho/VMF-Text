package eu.mihosoft.vmftext.tests.blockliftmanual;

import eu.mihosoft.vmftext.tests.blockliftmanual.parser.BlockLiftManualModelParser;
import eu.mihosoft.vmftext.tests.blockliftmanual.unparser.BlockLiftManualModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** Issue #37: literal-only nested alternative blocks in hand-labeled grammars. */
public class BlockLiftManualTest {

    private static final String SRC = "int[] a << 1;\nint [ ] b >>> 2;\nint c >> 3;\n";

    @Test
    public void literalOnlyBlocksRoundTripExactly() {
        BlockLiftManualModel model = new BlockLiftManualModelParser().parse(SRC);
        Assert.assertEquals(SRC, new BlockLiftManualModelUnparser().unparse(model));
    }

    @Test
    public void liftedBlocksAreTypedPerAlternative() {
        java.util.List<Stmt> stmts = new BlockLiftManualModelParser().parse(SRC).getRoot().getStmts();
        Assert.assertEquals(3, stmts.size());
        Assert.assertTrue(stmts.get(0).getStmtChoice() instanceof StmtChoiceAlt2);   // '[]'
        Assert.assertTrue(stmts.get(1).getStmtChoice() instanceof StmtChoiceAlt1);   // '[' ']'
        Assert.assertNull(stmts.get(2).getStmtChoice());                             // absent (optional block)
        Assert.assertTrue(stmts.get(0).getStmtChoice2() instanceof StmtChoice2Alt1); // '<' '<'
        Assert.assertTrue(stmts.get(1).getStmtChoice2() instanceof StmtChoice2Alt2); // '>' '>' '>'
        Assert.assertTrue(stmts.get(2).getStmtChoice2() instanceof StmtChoice2Alt3); // '>' '>'
    }

    @Test
    public void choosingAnAlternativeProgrammaticallyUnparses() {
        BlockLiftManualModel model = new BlockLiftManualModelParser().parse("int c >> 3;\n");
        Stmt stmt = model.getRoot().getStmts().get(0);
        stmt.setStmtChoice(StmtChoiceAlt2.newInstance());   // add '[]'
        stmt.setStmtChoice2(StmtChoice2Alt1.newInstance()); // '>>' -> '<<'
        String out = new BlockLiftManualModelUnparser().unparse(model);
        Assert.assertEquals(out, "int[]c<<3;", out.replaceAll("\\s+", ""));
        Stmt reparsed = new BlockLiftManualModelParser().parse(out).getRoot().getStmts().get(0);
        Assert.assertTrue(reparsed.getStmtChoice() instanceof StmtChoiceAlt2);
        Assert.assertTrue(reparsed.getStmtChoice2() instanceof StmtChoice2Alt1);
    }
}
