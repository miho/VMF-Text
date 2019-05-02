package eu.mihosoft.vmftext.tests.codelelementdelegate;

import eu.mihosoft.vmftext.tests.java8.*;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import org.junit.Assert;
import org.junit.Test;

public class CodeElementTest {

    @Test
    public void parentAndRootTest() {

        Java8ModelParser parser = new Java8ModelParser();

        String code = "" +
                "class MyClass {\n" +
                "  public void myMethod() {\n" +
                "    for( int i = 0; i < 10; i++) {\n" +
                "      System.out.println(\"Hello\");\n" +
                "    }\n" +
                "  }\n" +
                "}";

        ClassDeclaration classDeclaration = parser.parseClassDeclaration(code);

        ForStmnt forStmnt = classDeclaration.vmf().content().stream(ForStmnt.class).findFirst().get();

        StringLiteral str = forStmnt.vmf().content().stream(StringLiteral.class).findFirst().get();

        Assert.assertTrue("parent of string literal must be a literalExpr",
                LiteralExpr.class.isAssignableFrom(str.getParent().getClass()));

        System.out.println("Path to root:");
        str.pathToRoot().forEach(e-> {
            System.out.println("  -> " + e);
        });

        Assert.assertTrue("root of string literal must be a classDecl",
                ClassDeclaration.class.isAssignableFrom(str.root().getClass()));
    }

//    @Test
//    public void getPayloadTest() {
//
//        Java8ModelParser parser = new Java8ModelParser();
//
//        String code = "" +
//                "class MyClass {\n" +
//                "  public void myMethod() {\n" +
//                "    for( int i = 0; i < 10; i++) {\n" +
//                "      System.put.println(\"Hello\");\n" +
//                "    }\n" +
//                "  }\n" +
//                "}";
//
//        ClassDeclaration classDeclaration = parser.parseClassDeclaration(code);
//
//        ForStmnt forStmnt = classDeclaration.vmf().content().stream(ForStmnt.class).findFirst().get();
//
//        Payload payload = Payload.newInstance();
//        forStmnt.setPayload(payload);
//        payload.payloadSet("test", 2);
//
//        StringLiteral str = forStmnt.vmf().content().stream(StringLiteral.class).findFirst().get();
//
//        //Assert.assertEquals(payload,str.payload());
//        //Assert.assertEquals(payload.payloadGet("test"), str.payload().payloadGet("test"));
//    }

}
