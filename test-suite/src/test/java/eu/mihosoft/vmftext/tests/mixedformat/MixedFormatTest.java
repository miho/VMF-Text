package eu.mihosoft.vmftext.tests.mixedformat;

import eu.mihosoft.vmftext.tests.mixedformat.parser.MixedFormatModelParser;
import eu.mihosoft.vmftext.tests.mixedformat.unparser.Formatter;
import eu.mihosoft.vmftext.tests.mixedformat.unparser.MixedFormatModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/** Issue #40: mixed-mode formatting — parsed bytes exact, programmatic parts indented, ranges reported. */
public class MixedFormatTest {

    private static final String SRC = "class A {\n  void a() {\n    x();\n  }\n}\n";

    private static Stmt call(String name) {
        return Stmt.newBuilder().withCall(name).build();
    }

    private static Method method(String name, Stmt... stmts) {
        return Method.newBuilder().withName(name).withStmts(stmts).build();
    }

    @Test
    public void programmaticModelIsBlockIndented() {
        ClassDecl cls = ClassDecl.newBuilder().withName("A").withMethods(method("run", call("go"))).build();
        MixedFormatModel model = MixedFormatModel.newBuilder().withRoot(Unit.newBuilder().withClasses(cls).build()).build();
        MixedFormatModelUnparser unparser = new MixedFormatModelUnparser();
        unparser.setFormatter(Formatter.newBlockIndentFormatter());
        Assert.assertEquals("class A {\n  void run() {\n    go();\n  }\n}", unparser.unparse(model));
    }

    @Test
    public void defaultPolicyIsUnchanged() {
        ClassDecl cls = ClassDecl.newBuilder().withName("A").withMethods(method("run", call("go"))).build();
        MixedFormatModel model = MixedFormatModel.newBuilder().withRoot(Unit.newBuilder().withClasses(cls).build()).build();
        String out = new MixedFormatModelUnparser().unparse(model);
        Assert.assertEquals(out, "classA{voidrun(){go();}}", out.replaceAll("\\s+", ""));
    }

    @Test
    public void insertedSubtreeIsIndentedAndParsedBytesStayExact() {
        MixedFormatModel model = new MixedFormatModelParser().parse(SRC);
        model.getRoot().getClasses().get(0).getMethods().add(method("b", call("y")));
        MixedFormatModelUnparser unparser = new MixedFormatModelUnparser();
        unparser.setFormatter(Formatter.newBlockIndentFormatter());
        String out = unparser.unparse(model);
        Assert.assertEquals("class A {\n  void a() {\n    x();\n  }\n  void b() {\n    y();\n  }\n}\n", out);
    }

    @Test
    public void programmaticRangesCoverExactlyTheInsertedText() {
        MixedFormatModel model = new MixedFormatModelParser().parse(SRC);
        Method inserted = method("b", call("y"));
        model.getRoot().getClasses().get(0).getMethods().add(inserted);
        MixedFormatModelUnparser unparser = new MixedFormatModelUnparser();
        unparser.setFormatter(Formatter.newBlockIndentFormatter());
        String out = unparser.unparse(model);
        List<Formatter.OutputRange> ranges = unparser.getFormatter().programmaticRanges();
        Assert.assertEquals(ranges.toString(), 2, ranges.size()); // the method and its statement, outermost first
        Formatter.OutputRange m = ranges.get(0);
        Assert.assertSame(inserted, m.getElement());
        Assert.assertEquals("void b() {\n    y();\n  }", out.substring(m.getStart(), m.getEnd()));
        Assert.assertEquals(5, m.getStartLine());
        Assert.assertEquals(7, m.getEndLine());
        Formatter.OutputRange s = ranges.get(1);
        Assert.assertEquals("y();", out.substring(s.getStart(), s.getEnd()));
        Assert.assertEquals(6, s.getStartLine());
    }

    @Test
    public void unchangedParsedModelHasNoRangesAndStaysExact() {
        MixedFormatModel model = new MixedFormatModelParser().parse(SRC);
        MixedFormatModelUnparser unparser = new MixedFormatModelUnparser();
        unparser.setFormatter(Formatter.newBlockIndentFormatter());
        Assert.assertEquals(SRC, unparser.unparse(model));
        Assert.assertTrue(unparser.getFormatter().programmaticRanges().isEmpty());
    }
}
