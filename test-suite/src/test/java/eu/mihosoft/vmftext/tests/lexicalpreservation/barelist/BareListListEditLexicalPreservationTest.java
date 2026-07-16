/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.barelist;

import eu.mihosoft.vmftext.tests.lexicalpreservation.barelist.parser.BareListModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.barelist.unparser.BareListModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Lexical preservation for bare delimited lists {@code item (',' item)* EOF}
 * ({@code trivia.size() == 2N+1}).
 */
public class BareListListEditLexicalPreservationTest {

    private final BareListModelParser parser = new BareListModelParser();
    private final BareListModelUnparser unparser = new BareListModelUnparser();

    @Test
    public void uneditedRoundTripExact() {
        String source = "1 ,  2,\n 3";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "1 ,  2,\n 3";
        BareListModel model = parser.parse(source);
        int triviaBefore = model.getRoot().getLexicalInfo().getTriviaPieces().size();

        model.getRoot().getItems().set(1, 99);

        Assert.assertEquals(triviaBefore,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
        Assert.assertEquals("1 ,  99,\n 3", unparser.unparse(model));
    }

    @Test
    public void removeMiddlePreservesSiblingWhitespace() {
        String source = "1 ,  2,\n 3";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().remove(1);

        assertTriviaSize(model, 2);
        Assert.assertEquals("1,\n 3", unparser.unparse(model));
    }

    @Test
    public void removeFirstKeepsFormerSecondLeadingTrivia() {
        String source = "1 ,  2,\n 3";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().remove(0);

        assertTriviaSize(model, 2);
        Assert.assertEquals("  2,\n 3", unparser.unparse(model));
    }

    @Test
    public void addAtEndInsertsCommaAndSpace() {
        String source = "1 ,  2";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().add(3);

        assertTriviaSize(model, 3);
        Assert.assertEquals("1 ,  2, 3", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroPreservesFormerHead() {
        String source = "1 ,  2";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().add(0, 0);

        assertTriviaSize(model, 3);
        Assert.assertEquals("0, 1 ,  2", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExact() {
        String source = "1 ,  2";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().add(0, 0);
        Assert.assertEquals("0, 1 ,  2", unparser.unparse(model));

        model.getRoot().getItems().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
        assertTriviaSize(model, 2);
    }

    @Test
    public void addThenRemoveMiddleRoundTrips() {
        String source = "1 ,  2,\n 3";
        BareListModel model = parser.parse(source);

        model.getRoot().getItems().add(1, 8);
        Assert.assertEquals("1, 8 ,  2,\n 3", unparser.unparse(model));

        model.getRoot().getItems().remove(1);
        Assert.assertEquals(source, unparser.unparse(model));
        assertTriviaSize(model, 3);
    }

    private static void assertTriviaSize(BareListModel model, int nValues) {
        // N values + (N-1) commas + EOF + pad == 2N+1
        Assert.assertEquals(2 * nValues + 1,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
    }
}
