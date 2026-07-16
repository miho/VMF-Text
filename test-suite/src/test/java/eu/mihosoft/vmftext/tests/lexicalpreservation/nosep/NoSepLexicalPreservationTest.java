/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.nosep;

import eu.mihosoft.vmftext.tests.lexicalpreservation.nosep.parser.NoSepModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.nosep.unparser.NoSepModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Separator-less {@code item+} — {@code separatorCount == 0}.
 */
public class NoSepLexicalPreservationTest {

    private final NoSepModelParser parser = new NoSepModelParser();
    private final NoSepModelUnparser unparser = new NoSepModelUnparser();

    @Test
    public void hintRecordsSeparatorCountZero() {
        NoSepModel model = parser.parse("a b c");
        ListShapeHint hint = model.getRoot().getLexicalInfo().getListShapeHints().get(0);
        Assert.assertEquals("items", hint.getPropertyName());
        Assert.assertEquals(0, hint.getSeparatorCount());
    }

    @Test
    public void uneditedRoundTripExact() {
        String source = "a  b\n c";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "a  b\n c";
        NoSepModel model = parser.parse(source);
        model.getRoot().getItems().set(1, "z");
        Assert.assertEquals("a  z\n c", unparser.unparse(model));
    }

    @Test
    public void removeMiddlePreservesNeighbors() {
        String source = "a  b\n c";
        NoSepModel model = parser.parse(source);
        model.getRoot().getItems().remove(1);
        Assert.assertEquals("a\n c", unparser.unparse(model));
    }

    @Test
    public void appendInsertsSpaceBeforeNewItem() {
        String source = "a  b";
        NoSepModel model = parser.parse(source);
        model.getRoot().getItems().add("c");
        Assert.assertEquals("a  b c", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExact() {
        String source = "a  b";
        NoSepModel model = parser.parse(source);
        model.getRoot().getItems().add(0, "z");
        String after = unparser.unparse(model);
        Assert.assertTrue(after, after.startsWith("z"));
        Assert.assertTrue(after.contains("a"));
        model.getRoot().getItems().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
