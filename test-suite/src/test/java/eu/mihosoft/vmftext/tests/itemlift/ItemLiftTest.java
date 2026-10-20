package eu.mihosoft.vmftext.tests.itemlift;

import eu.mihosoft.vmftext.tests.itemlift.parser.ItemLiftModelParser;
import eu.mihosoft.vmftext.tests.itemlift.unparser.ItemLiftModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** #54: a repeated block with an optional property is lifted into per-item sub classes. */
public class ItemLiftTest {

    private static String roundTrip(String src) {
        return new ItemLiftModelUnparser().unparse(new ItemLiftModelParser().parse(src));
    }

    @Test
    public void defaultsStayWithTheirParameter() {
        Assert.assertEquals("def f(a, b=1, c);", roundTrip("def f(a, b=1, c);"));
        Assert.assertEquals("def f(a=1, b, c=2);", roundTrip("def f(a=1, b, c=2);"));
        Assert.assertEquals("def f(self, sock, protocol, waiter=0, extra=1);",
                roundTrip("def f(self, sock, protocol, waiter=0, extra=1);"));
    }

    @Test
    public void everyIterationIsOneTypedItem() {
        ItemLiftModel model = new ItemLiftModelParser().parse("def f(a, b=1, c);");
        Params params = model.getRoot().getDecls().get(0).getParams();
        Assert.assertEquals(2, params.getParamsItems().size());
        Assert.assertNull(params.getNum());
        Assert.assertEquals("1", params.getParamsItems().get(0).getNum());
        Assert.assertNull(params.getParamsItems().get(1).getNum());
    }

    @Test
    public void defaultCanBeAddedToAnItem() {
        ItemLiftModel model = new ItemLiftModelParser().parse("def f(a, b, c);");
        Params params = model.getRoot().getDecls().get(0).getParams();
        params.getParamsItems().get(1).setNum("7");
        String out = new ItemLiftModelUnparser().unparse(model);
        Assert.assertEquals(out, "deff(a,b,c=7);", out.replaceAll("\\s+", ""));
        Assert.assertEquals("7", new ItemLiftModelParser().parse(out).getRoot().getDecls().get(0)
                .getParams().getParamsItems().get(1).getNum());
    }
}
