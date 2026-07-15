/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.java8;

import eu.mihosoft.vmftext.tests.java8.MethodDeclaration;
import eu.mihosoft.vmftext.tests.java8.Java8Model;
import eu.mihosoft.vmftext.tests.java8.StringLiteral;
import eu.mihosoft.vmftext.tests.java8.parser.Java8ModelParser;
import eu.mihosoft.vmftext.tests.java8.unparser.Java8ModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * In-place property rewrites on nested CodeElements keep sibling bytes
 * (and keep that element's trivia footprint).
 */
public class Java8InPlaceEditLexicalPreservationTest {

    private final Java8ModelParser parser = new Java8ModelParser();
    private final Java8ModelUnparser unparser = new Java8ModelUnparser();

    @Test
    public void methodRenameAndStringReplacePreserveSurroundingBytes() {
        String source = ""
                + "package demo.hello ;\n"
                + "\n"
                + "/* class comment */\n"
                + "public  class Greeter {\n"
                + "\n"
                + "  // oddly spaced\n"
                + "  public static void greet( String[] args ){\n"
                + "    System.out.println( \"hello\" );   // trailing\n"
                + "  }\n"
                + "\n"
                + "}\n";

        Java8Model model = parser.parse(source);

        model.vmf().content().stream(MethodDeclaration.class)
                .filter(m -> "greet".equals(m.getMethodName()))
                .forEach(m -> m.setMethodName("sayHello"));

        model.vmf().content().stream(StringLiteral.class)
                .filter(lit -> "\"hello\"".equals(lit.getStringValue()))
                .forEach(lit -> lit.setStringValue("\"hello, world\""));

        String expected = source
                .replace("greet", "sayHello")
                .replace("\"hello\"", "\"hello, world\"");
        Assert.assertEquals(expected, unparser.unparse(model));
    }
}
