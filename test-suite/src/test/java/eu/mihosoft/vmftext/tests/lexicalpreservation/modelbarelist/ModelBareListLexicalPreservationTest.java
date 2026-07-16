/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.modelbarelist;

import eu.mihosoft.vmftext.tests.lexicalpreservation.modelbarelist.parser.ModelBareListModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.modelbarelist.unparser.ModelBareListModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Bare model-typed list {@code item (',' item)*} — no brackets. Structural
 * edits should keep sibling leading trivia when the parent comma footprint is
 * recognized (via {@code MODEL_DELIMITED} hint or heuristic).
 */
public class ModelBareListLexicalPreservationTest {

    private final ModelBareListModelParser parser = new ModelBareListModelParser();
    private final ModelBareListModelUnparser unparser = new ModelBareListModelUnparser();

    @Test
    public void uneditedRoundTripExact() {
        String source = "a ,  b,\n c";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void renameKeepsSiblingWhitespace() {
        String source = "a ,  b,\n c";
        ModelBareListModel model = parser.parse(source);
        model.getRoot().getItems().get(1).setName("z");
        Assert.assertEquals("a ,  z,\n c", unparser.unparse(model));
    }

    @Test
    public void removeMiddleKeepsNeighbors() {
        String source = "a ,  b,\n c";
        ModelBareListModel model = parser.parse(source);
        model.getRoot().getItems().remove(1);
        // Removing the middle item drops its coupling separator group; the
        // neighbor's leading trivia (`\n `) remains on the child.
        Assert.assertEquals("a,\n c", unparser.unparse(model));
    }

    @Test
    public void appendKeepsHead() {
        String source = "a ,  b";
        ModelBareListModel model = parser.parse(source);
        Item n = Item.newInstance();
        n.setName("c");
        model.getRoot().getItems().add(n);
        Assert.assertEquals("a ,  b, c", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExact() {
        String source = "a ,  b";
        ModelBareListModel model = parser.parse(source);
        Item z = Item.newInstance();
        z.setName("z");
        model.getRoot().getItems().add(0, z);
        String after = unparser.unparse(model);
        Assert.assertTrue(after, after.contains("z") && after.contains("a"));
        model.getRoot().getItems().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
