package eu.mihosoft.vmftext.tests.commalift;

import eu.mihosoft.vmftext.tests.commalift.parser.CommaLiftModelParser;
import eu.mihosoft.vmftext.tests.commalift.unparser.CommaLiftModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/** #64: a repeated block with a repeated literal is lifted into per-item sub classes. */
public class CommaLiftTest {

    private static String roundTrip(String src) {
        return new CommaLiftModelUnparser().unparse(new CommaLiftModelParser().parse(src));
    }

    @Test
    public void everyElementKeepsItsCommas() {
        String[] inputs = {
                "[]", "[1]", "[1, 2, 3]", "[1,,2,3]", "[,1]", "[1,]", "[,,]",
                "[ 1 ,  2 ,,, 3 , ]", "[...1, 2]", "[1,2] [3] [,]",
        };
        for (String src : inputs) {
            Assert.assertEquals(src, roundTrip(src));
        }
    }

    @Test
    public void eachIterationIsOneItemWithItsOwnCommas() {
        Elements elements = new CommaLiftModelParser().parse("[1,,2,3]").getRoot().getArrays().get(0).getElements();
        Assert.assertEquals("1", elements.getElement().getNum());
        Assert.assertEquals(2, elements.getElementsItems().size());
        Assert.assertEquals(2, elements.getElementsItems().get(0).getCommas().size());
        Assert.assertEquals("2", elements.getElementsItems().get(0).getElement().getNum());
        Assert.assertEquals(1, elements.getElementsItems().get(1).getCommas().size());
        Assert.assertEquals("3", elements.getElementsItems().get(1).getElement().getNum());
    }

    @Test
    public void anAppendedItemIsPrintedWithItsComma() {
        CommaLiftModel model = new CommaLiftModelParser().parse("[1, 2]");
        Elements elements = model.getRoot().getArrays().get(0).getElements();
        elements.getElementsItems().add(ElementsItem.newBuilder().withCommas(",")
                .withElement(Element.newBuilder().withNum("3").build()).build());
        String out = new CommaLiftModelUnparser().unparse(model);
        Assert.assertEquals(out, "[1,2,3]", out.replaceAll("\\s+", ""));
    }
}
