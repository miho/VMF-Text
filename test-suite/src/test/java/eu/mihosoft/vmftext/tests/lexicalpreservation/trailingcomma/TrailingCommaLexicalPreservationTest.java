/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.trailingcomma;

import eu.mihosoft.vmftext.tests.lexicalpreservation.trailingcomma.parser.TrailingCommaModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.trailingcomma.unparser.TrailingCommaModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Optional trailing {@code ','?} — size math must accept present and absent forms.
 */
public class TrailingCommaLexicalPreservationTest {

    private final TrailingCommaModelParser parser = new TrailingCommaModelParser();
    private final TrailingCommaModelUnparser unparser = new TrailingCommaModelUnparser();

    @Test
    public void hintRecordsOptionalTrailingCount() {
        TrailingCommaModel model = parser.parse("(1, 2)");
        ListShapeHint hint = model.getRoot().getLexicalInfo().getListShapeHints().get(0);
        Assert.assertEquals("values", hint.getPropertyName());
        Assert.assertEquals(1, hint.getOptionalTrailingCount());
        Assert.assertEquals(1, hint.getSeparatorCount());
    }

    @Test
    public void withoutTrailingRoundTripExact() {
        String source = "(1,  2,\n 3)";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void withTrailingRoundTripExact() {
        String source = "(1,  2,\n 3,)";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void removeMiddleWithoutTrailingPreservesNeighbors() {
        String source = "(1,  2,\n 3)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().remove(1);
        Assert.assertEquals("(1,\n 3)", unparser.unparse(model));
    }

    @Test
    public void removeMiddleWithTrailingKeepsTrailing() {
        String source = "(1,  2,\n 3,)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().remove(1);
        Assert.assertEquals("(1,\n 3,)", unparser.unparse(model));
    }

    @Test
    public void appendWithoutTrailingPreservesHead() {
        String source = "(1,  2)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().add(9);
        Assert.assertEquals("(1,  2, 9)", unparser.unparse(model));
    }

    @Test
    public void appendWithTrailingKeepsTrailing() {
        String source = "(1,  2,)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().add(9);
        Assert.assertEquals("(1,  2, 9,)", unparser.unparse(model));
    }

    @Test
    public void removeToOneWithTrailingKeepsTrailing() {
        // Grammar is one-or-more; emptying cannot unparse. Leave a singleton.
        String source = "(1,  2,)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().remove(1);
        Assert.assertEquals("(1,)", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroWithTrailingThenRemoveRestores() {
        String source = "(1,  2,)";
        TrailingCommaModel model = parser.parse(source);
        model.getRoot().getValues().add(0, 9);
        Assert.assertEquals("(9, 1,  2,)", unparser.unparse(model));
        model.getRoot().getValues().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
