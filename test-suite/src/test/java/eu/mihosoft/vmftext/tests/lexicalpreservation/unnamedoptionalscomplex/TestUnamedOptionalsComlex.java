package eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionals;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionals.parser.SimpleUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionals.unparser.SimpleUnnamedModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestUnamedOptionals {

    @Test
    public void testSimpleUnnamedOptionals() {
        String code = "name = test123 name  = test456";

        SimpleUnnamedModel model = new SimpleUnnamedModelParser().parse(code);

        // store system.err as string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream prev = System.err;
        System.setErr(ps);

        String newCode = new SimpleUnnamedModelUnparser().unparse(model);

        String errors = baos.toString();

        Assert.assertTrue("No error must be reported.\n -> Output:\n" + errors, errors.isEmpty());
        Assert.assertEquals(code, newCode);

        // stop recording system.err
        System.out.flush();
        System.setErr(prev);
    }

}
