package eu.mihosoft.vmftext.tests.optblock;

import eu.mihosoft.vmftext.tests.optblock.parser.OptBlockModelParser;
import eu.mihosoft.vmftext.tests.optblock.unparser.OptBlockModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** #52: an optional block whose only named element is optional stays state-driven. */
public class OptBlockTest {

    private static String roundTrip(String src) {
        return new OptBlockModelUnparser().unparse(new OptBlockModelParser().parse(src));
    }

    @Test
    public void absentBlockIsNotEmitted() {
        Assert.assertEquals("class A: x;", roundTrip("class A: x;"));
    }

    @Test
    public void presentButEmptyBlockIsKept() {
        Assert.assertEquals("class A(): x;", roundTrip("class A(): x;"));
    }

    @Test
    public void presentBlockWithContentIsExact() {
        Assert.assertEquals("class A(B, C): x;", roundTrip("class A(B, C): x;"));
        Assert.assertEquals("class A( B ,C ) : x ;", roundTrip("class A( B ,C ) : x ;"));
    }

    @Test
    public void trailingCommaBlockIsStateDriven() {
        Assert.assertEquals("class A(B, C): x;", roundTrip("class A(B, C): x;"));
        Assert.assertEquals("class A(B, C,): x;", roundTrip("class A(B, C,): x;"));
        Assert.assertEquals("class A(B, *R): x;", roundTrip("class A(B, *R): x;"));
        Assert.assertEquals("class A(B,): x;", roundTrip("class A(B,): x;"));
    }

    @Test
    public void programmaticModelReparsesToTheSameStructure() {
        Decl decl = Decl.newBuilder().withName("A").withBody("x").build();
        OptBlockModel model = OptBlockModel.newBuilder().withRoot(Unit.newBuilder().withDecls(decl).build()).build();
        String out = new OptBlockModelUnparser().unparse(model);
        OptBlockModel back = new OptBlockModelParser().parse(out);
        Assert.assertEquals(out, "A", back.getRoot().getDecls().get(0).getName());
        Assert.assertNull(out, back.getRoot().getDecls().get(0).getBases());
    }

    /** #60: literal-only sugar (';'?) is left out of an element built from scratch; a block with a property is not. */
    @Test
    public void programmaticModelLeavesLiteralOnlySugarOut() {
        Decl plain = Decl.newBuilder().withName("A").withBody("x").build();
        Decl withBases = Decl.newBuilder().withName("B").withBody("y")
                .withBases(IdList.newBuilder().withIds("C", "D").build()).build();
        OptBlockModel model = OptBlockModel.newBuilder().withRoot(Unit.newBuilder().withDecls(plain, withBases).build()).build();
        String out = new OptBlockModelUnparser().unparse(model);
        Assert.assertEquals(out, "class A : x class B ( C , D ) : y", out.replaceAll("\\s+", " ").trim());
        Assert.assertEquals("class A: x", roundTrip("class A: x"));
        Assert.assertEquals("class A: x;", roundTrip("class A: x;"));
    }
}
