/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.arraylang;

import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Lexical preservation for ArrayLang list mutations: in-place set, structural
 * add/remove with surgical trivia splicing for the
 * {@code '(' item (',' item)* ')'} shape.
 */
public class ArrayLangListEditLexicalPreservationTest {

    private final ArrayLangModelParser parser = new ArrayLangModelParser();
    private final ArrayLangModelUnparser unparser = new ArrayLangModelUnparser();

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        ArrayLangModel model = parser.parse(source);
        int triviaBefore = model.getRoot().getLexicalInfo().getTriviaPieces().size();

        model.getRoot().getValues().set(1, 99.0);

        Assert.assertEquals(triviaBefore,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
        Assert.assertEquals("(1.0 ,  99.0,\n 3.0 )", unparser.unparse(model));
    }

    @Test
    public void removeMiddlePreservesSiblingWhitespace() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().remove(1);

        assertTriviaSize(model, 2);
        Assert.assertEquals("(1.0,\n 3.0 )", unparser.unparse(model));
    }

    @Test
    public void removeFirstKeepsFormerSecondLeadingTrivia() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().remove(0);

        assertTriviaSize(model, 2);
        Assert.assertEquals("(  2.0,\n 3.0 )", unparser.unparse(model));
    }

    @Test
    public void removeLastPreservesHeadWhitespace() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().remove(2);

        assertTriviaSize(model, 2);
        Assert.assertEquals("(1.0 ,  2.0 )", unparser.unparse(model));
    }

    @Test
    public void addAtEndInsertsCommaAndSpace() {
        String source = "(1.0 ,  2.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().add(3.0);

        assertTriviaSize(model, 3);
        Assert.assertEquals("(1.0 ,  2.0, 3.0 )", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroPreservesFormerHead() {
        String source = "(1.0 ,  2.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().add(0, 0.5);

        assertTriviaSize(model, 3);
        Assert.assertEquals("(0.5, 1.0 ,  2.0 )", unparser.unparse(model));
    }

    @Test
    public void insertInMiddleKeepsExistingCommaTrivia() {
        String source = "(1.0 ,  2.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().add(1, 9.0);

        assertTriviaSize(model, 3);
        Assert.assertEquals("(1.0, 9.0 ,  2.0 )", unparser.unparse(model));
    }

    @Test
    public void removeOnlyElementClearsTrivia() {
        String source = "(1.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().remove(0);

        Assert.assertTrue(model.getRoot().getLexicalInfo().getTriviaPieces().isEmpty());
    }

    @Test
    public void addThenRemoveRoundTripsPreservedShape() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        ArrayLangModel model = parser.parse(source);

        model.getRoot().getValues().add(1, 8.0);
        Assert.assertEquals("(1.0, 8.0 ,  2.0,\n 3.0 )", unparser.unparse(model));

        model.getRoot().getValues().remove(1);
        Assert.assertEquals(source, unparser.unparse(model));
        assertTriviaSize(model, 3);
    }

    @Test
    public void uneditedRoundTripStillExact() {
        String source = " (1.0 ,2.0 , 3.0, 4.0\n,5.0,\n6.0,7.0,8.0, 0.0 )";
        ArrayLangModel model = parser.parse(source);
        Assert.assertEquals(source, unparser.unparse(model));
    }

    private static void assertTriviaSize(ArrayLangModel model, int nValues) {
        // '(' + N values + (N-1) commas + ')' + EOF + pad == 2N+3
        Assert.assertEquals(2 * nValues + 3,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
    }
}
