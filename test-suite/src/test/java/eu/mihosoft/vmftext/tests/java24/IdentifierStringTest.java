package eu.mihosoft.vmftext.tests.java24;

import eu.mihosoft.vmftext.tests.java24.parser.Java24ModelParser;
import eu.mihosoft.vmftext.tests.java24.unparser.Java24ModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import org.junit.Assert;
import org.junit.Test;

public class IdentifierStringTest {

    private final Java24ModelParser parser = new Java24ModelParser();
    private final Java24ModelUnparser unparser = new Java24ModelUnparser();

    @Test
    public void packageNameAsStringJoinsIdentifierTexts() {
        // module, record and open are contextual keywords matched by
        // non-IDENTIFIER alternatives of the identifier rule
        Java24Model model = parser.parse("package module.record.open;\nclass A { }\n");

        Assert.assertEquals("module.record.open",
                model.getRoot().getNormalUnit().getPackageDecl().packageNameAsString());
    }

    @Test
    public void defPackageNameFromStringSurvivesUnparseReparse() {
        Java24Model model = parser.parse("package before.pkg;\nclass A { }\n");
        model.getRoot().getNormalUnit().getPackageDecl().defPackageNameFromString("after.pkg.v2");

        Assert.assertEquals("after.pkg.v2",
                model.getRoot().getNormalUnit().getPackageDecl().packageNameAsString());

        Java24Model reparsed = parser.parse(unparser.unparse(model));
        Assert.assertEquals("after.pkg.v2",
                reparsed.getRoot().getNormalUnit().getPackageDecl().packageNameAsString());
    }

    @Test
    public void exactRoundTripPreservesCommentsAndWhitespace() {
        String source = "" +
                "package demo.lexical; // trailing comment\n" +
                "\n" +
                "/* keep me */\n" +
                "class Stress {\n" +
                "}\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }
}
