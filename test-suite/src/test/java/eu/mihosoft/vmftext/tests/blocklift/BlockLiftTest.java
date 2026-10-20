package eu.mihosoft.vmftext.tests.blocklift;

import eu.mihosoft.vmftext.tests.blocklift.parser.BlockLiftModelParser;
import eu.mihosoft.vmftext.tests.blocklift.unparser.BlockLiftModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** C-trial finding 2: keyword-only alternatives inside a block keep their choice. */
public class BlockLiftTest {

    private static final String SRC = "return 1;\nbreak;\nreturn;\ncontinue;\ngoto x;\necho 1  2;\nwith a->b;\nwith a . b;\n";

    @Test
    public void nestedBlockAlternativesRoundTripExactly() {
        BlockLiftModel model = new BlockLiftModelParser().parse(SRC);
        Assert.assertEquals(SRC, new BlockLiftModelUnparser().unparse(model));
    }

    @Test
    public void liftedBlockIsTypedPerAlternative() {
        BlockLiftModel model = new BlockLiftModelParser().parse(SRC);
        java.util.List<Stmt> stmts = model.getRoot().getStmts();
        Assert.assertEquals(8, stmts.size());
        Assert.assertTrue(stmts.get(0).getStmtChoice() instanceof StmtChoiceAlt4); // return 1
        Assert.assertNotNull(((StmtChoiceAlt4) stmts.get(0).getStmtChoice()).getReturnExpr());
        Assert.assertTrue(stmts.get(1).getStmtChoice() instanceof StmtChoiceAlt3); // break
        Assert.assertTrue(stmts.get(2).getStmtChoice() instanceof StmtChoiceAlt4); // return
        Assert.assertNull(((StmtChoiceAlt4) stmts.get(2).getStmtChoice()).getReturnExpr());
        Assert.assertTrue(stmts.get(3).getStmtChoice() instanceof StmtChoiceAlt2); // continue
        Assert.assertTrue(stmts.get(4).getStmtChoice() instanceof StmtChoiceAlt1); // goto x
        Assert.assertEquals("x", ((StmtChoiceAlt1) stmts.get(4).getStmtChoice()).getId());
        Assert.assertTrue(stmts.get(5).getStmtChoice() instanceof StmtChoiceAlt5); // echo 1 2
        Assert.assertEquals(2, ((StmtChoiceAlt5) stmts.get(5).getStmtChoice()).getIntValues().size());
        // a non-repeated literal choice ('.' | '->') is captured as a plain String property
        Assert.assertTrue(stmts.get(6).getStmtChoice() instanceof StmtChoiceAlt6); // with a->b
        Assert.assertEquals("->", ((StmtChoiceAlt6) stmts.get(6).getStmtChoice()).getOperator());
        Assert.assertEquals(".", ((StmtChoiceAlt6) stmts.get(7).getStmtChoice()).getOperator());
    }

    @Test
    public void replacingTheChoiceUnparsesTheNewAlternative() {
        BlockLiftModel model = new BlockLiftModelParser().parse("return 1;\nbreak;\n");
        model.getRoot().getStmts().get(1).setStmtChoice(StmtChoiceAlt2.newInstance()); // break -> continue
        String out = new BlockLiftModelUnparser().unparse(model);
        BlockLiftModel reparsed = new BlockLiftModelParser().parse(out);
        Assert.assertTrue(reparsed.getRoot().getStmts().get(1).getStmtChoice() instanceof StmtChoiceAlt2);
        Assert.assertTrue(out, out.contains("continue;"));
        Assert.assertTrue(out, out.startsWith("return 1;"));
    }
}
