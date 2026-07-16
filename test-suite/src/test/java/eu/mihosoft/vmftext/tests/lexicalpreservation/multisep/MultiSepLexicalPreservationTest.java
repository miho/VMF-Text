/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.multisep;

import eu.mihosoft.vmftext.tests.lexicalpreservation.multisep.parser.MultiSepModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multisep.unparser.MultiSepModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Multi-token separators ({@code ',' 'and'}) — {@code separatorCount == 2}.
 */
public class MultiSepLexicalPreservationTest {

    private final MultiSepModelParser parser = new MultiSepModelParser();
    private final MultiSepModelUnparser unparser = new MultiSepModelUnparser();

    @Test
    public void hintRecordsSeparatorCountTwo() {
        MultiSepModel model = parser.parse("a , and b");
        ListShapeHint hint = model.getRoot().getLexicalInfo().getListShapeHints().get(0);
        Assert.assertEquals("items", hint.getPropertyName());
        Assert.assertEquals(2, hint.getSeparatorCount());
    }

    @Test
    public void uneditedRoundTripExact() {
        String source = "a , and  b,\nand c";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "a , and  b,\nand c";
        MultiSepModel model = parser.parse(source);
        model.getRoot().getItems().set(1, "z");
        Assert.assertEquals("a , and  z,\nand c", unparser.unparse(model));
    }

    @Test
    public void removeMiddlePreservesNeighbors() {
        String source = "a , and  b,\nand c";
        MultiSepModel model = parser.parse(source);
        model.getRoot().getItems().remove(1);
        Assert.assertEquals("a,\nand c", unparser.unparse(model));
    }

    @Test
    public void appendPreservesHead() {
        String source = "a , and  b";
        MultiSepModel model = parser.parse(source);
        model.getRoot().getItems().add("c");
        Assert.assertEquals("a , and  b, and c", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExact() {
        String source = "a , and  b";
        MultiSepModel model = parser.parse(source);
        model.getRoot().getItems().add(0, "z");
        Assert.assertEquals("z, and a , and  b", unparser.unparse(model));
        model.getRoot().getItems().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
