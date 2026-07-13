package eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.parser.NestedUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.unparser.NestedUnnamedModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestUnamedOptionalsComplex {

    @Test
    public void testNestedUnnamedOptionals() {
        String code = "r1 (), r2 (test123)";

        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(code);

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

    @Test
    public void testNestedUnnamedOptionalsPresenceMatrix() {
        assertRoundTrip("r1, r2");
        assertRoundTrip("r1 (), r2");
        assertRoundTrip("r1, r2 (test123), r3 (), r4, r5 ()");
        assertRoundTrip("r1 (), r2 (test123), r3 (abc), r4 (def), r5 (a b c)");
    }

    private void assertRoundTrip(String code) {
        NestedUnnamedModel model = new NestedUnnamedModelParser().parse(code);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream prev = System.err;
        System.setErr(ps);

        String newCode = new NestedUnnamedModelUnparser().unparse(model);

        String errors = baos.toString();

        System.out.flush();
        System.setErr(prev);

        Assert.assertTrue("No error must be reported.\n -> Output:\n" + errors, errors.isEmpty());
        Assert.assertEquals(code, newCode);
    }

}
