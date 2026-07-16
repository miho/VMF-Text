/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.pipelist;

import eu.mihosoft.vmftext.tests.lexicalpreservation.pipelist.parser.PipeListModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.pipelist.unparser.PipeListModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Bare delimited list with a non-comma separator ({@code '|'}). Trivia splice
 * is separator-agnostic — only the one-terminal-between-items rhythm matters.
 */
public class PipeListLexicalPreservationTest {

    private final PipeListModelParser parser = new PipeListModelParser();
    private final PipeListModelUnparser unparser = new PipeListModelUnparser();

    @Test
    public void uneditedRoundTripExact() {
        String source = "a |  b|\n c";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "a |  b|\n c";
        PipeListModel model = parser.parse(source);
        model.getRoot().getItems().set(1, "z");
        Assert.assertEquals("a |  z|\n c", unparser.unparse(model));
    }

    @Test
    public void removeMiddlePreservesNeighbors() {
        String source = "a |  b|\n c";
        PipeListModel model = parser.parse(source);
        model.getRoot().getItems().remove(1);
        Assert.assertEquals("a|\n c", unparser.unparse(model));
    }

    @Test
    public void appendPreservesHead() {
        String source = "a |  b";
        PipeListModel model = parser.parse(source);
        model.getRoot().getItems().add("c");
        // Unparser emits '|' from the grammar; splice only supplies trivia slots.
        Assert.assertEquals("a |  b| c", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExact() {
        String source = "a |  b";
        PipeListModel model = parser.parse(source);
        model.getRoot().getItems().add(0, "z");
        Assert.assertEquals("z| a |  b", unparser.unparse(model));
        model.getRoot().getItems().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
