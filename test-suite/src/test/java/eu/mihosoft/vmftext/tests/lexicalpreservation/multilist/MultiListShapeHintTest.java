/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.multilist;

import eu.mihosoft.vmftext.tests.lexicalpreservation.multilist.parser.MultiListModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multilist.unparser.MultiListModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Two primitive delimited lists on one rule: codegen {@link ListShapeHint}s
 * let each list splice independently.
 *
 * <p>Grammar: {@code ids+=ID (',' ids+=ID)* ';' nums+=INT (',' nums+=INT)* EOF}
 * — without hints, a trivia-size heuristic cannot tell where {@code ids} ends
 * and {@code nums} begins after the {@code ';'} separator.
 */
public class MultiListShapeHintTest {

    private final MultiListModelParser parser = new MultiListModelParser();
    private final MultiListModelUnparser unparser = new MultiListModelUnparser();

    @Test
    public void parsedModelCarriesHintsForBothLists() {
        MultiListModel model = parser.parse("a, b ; 1, 2");
        Assert.assertNotNull(model.getRoot().getLexicalInfo());
        Assert.assertEquals(2, model.getRoot().getLexicalInfo().getListShapeHints().size());

        ListShapeHint ids = model.getRoot().getLexicalInfo().getListShapeHints().get(0);
        ListShapeHint nums = model.getRoot().getLexicalInfo().getListShapeHints().get(1);

        Assert.assertEquals("ids", ids.getPropertyName());
        Assert.assertEquals("nums", nums.getPropertyName());
        Assert.assertEquals(0, ids.getOrderIndex());
        Assert.assertEquals(1, nums.getOrderIndex());
        Assert.assertFalse(ids.isModelTyped());
        Assert.assertFalse(nums.isModelTyped());

        // Bare lists: no bracket opener; trailing ';' belongs to the first list's suffix
        Assert.assertEquals(0, ids.getPrefixCount());
        Assert.assertTrue("ids should own the ';' trailer, got suffix=" + ids.getSuffixCount(),
                ids.getSuffixCount() >= 1);
        Assert.assertEquals(0, nums.getPrefixCount());
    }

    @Test
    public void uneditedRoundTripExact() {
        String source = "a ,  b; 1,\n 2";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void editIdsKeepsNumsWhitespace() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        int triviaBefore = model.getRoot().getLexicalInfo().getTriviaPieces().size();

        model.getRoot().getIds().set(1, "z");

        Assert.assertEquals(triviaBefore,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
        Assert.assertEquals("a ,  z; 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void editNumsKeepsIdsWhitespace() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);

        model.getRoot().getNums().set(0, 9);

        Assert.assertEquals("a ,  b; 9,\n 2", unparser.unparse(model));
    }

    @Test
    public void removeFromIdsPreservesNums() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getIds().remove(0);
        Assert.assertEquals("  b; 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void removeFromNumsPreservesIds() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getNums().remove(0);
        Assert.assertEquals("a ,  b;\n 2", unparser.unparse(model));
    }

    @Test
    public void appendToNumsPreservesIds() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getNums().add(9);
        Assert.assertEquals("a ,  b; 1,\n 2, 9", unparser.unparse(model));
    }

    @Test
    public void appendToIdsPreservesNums() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getIds().add("c");
        Assert.assertEquals("a ,  b, c; 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroOnIdsPreservesNums() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getIds().add(0, "z");
        Assert.assertEquals("z, a ,  b; 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroOnNumsPreservesIds() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getNums().add(0, 0);
        // Insert-at-0 does not invent a space after ';'; former head keeps its
        // leading trivia after the new comma (same policy as bare-list heads).
        Assert.assertEquals("a ,  b;0, 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroThenRemoveRestoresExactOnIds() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);

        model.getRoot().getIds().add(0, "z");
        Assert.assertEquals("z, a ,  b; 1,\n 2", unparser.unparse(model));

        model.getRoot().getIds().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }

    @Test
    public void removeLastIdKeepsNumsAndSemicolonShape() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getIds().remove(1);
        Assert.assertEquals("a; 1,\n 2", unparser.unparse(model));
    }

    @Test
    public void removeLastNumKeepsIds() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);
        model.getRoot().getNums().remove(1);
        Assert.assertEquals("a ,  b; 1", unparser.unparse(model));
    }

    @Test
    public void sequentialEditsOnBothListsStayIndependent() {
        String source = "a ,  b; 1,\n 2";
        MultiListModel model = parser.parse(source);

        model.getRoot().getIds().set(0, "aa");
        model.getRoot().getNums().remove(0);
        model.getRoot().getIds().add("c");
        model.getRoot().getNums().add(0, 7);

        Assert.assertEquals("aa ,  b, c;7,\n 2", unparser.unparse(model));
    }
}
