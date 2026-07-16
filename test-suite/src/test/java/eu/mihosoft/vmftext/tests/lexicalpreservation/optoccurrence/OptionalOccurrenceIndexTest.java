/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.optoccurrence;

import eu.mihosoft.vmftext.tests.lexicalpreservation.optoccurrence.parser.OptOccurrenceModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.optoccurrence.unparser.OptOccurrenceModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link OptionalState#getOccurrenceIndex()} pins each repeated optional path
 * so mixed presence cannot be reassigned across siblings.
 *
 * <p>Grammar: {@code item: 'x' ('(' name=ID ')')? ;} — several items share the
 * same optional grammar paths; each present occurrence must keep a distinct
 * index so the formatter can address them exactly.
 */
public class OptionalOccurrenceIndexTest {

    private final OptOccurrenceModelParser parser = new OptOccurrenceModelParser();
    private final OptOccurrenceModelUnparser unparser = new OptOccurrenceModelUnparser();

    @Test
    public void occurrenceIndicesAreAssignedPerPath() {
        // first item has group, second lacks it — same optional paths, different indices
        OptOccurrenceModel model = parser.parse("x (a), x");
        Item first = model.getRoot().getItems().get(0);
        Item second = model.getRoot().getItems().get(1);

        Assert.assertFalse(first.getLexicalInfo().getOptionalStates().isEmpty());
        List<Integer> firstIdx = first.getLexicalInfo().getOptionalStates().stream()
                .map(OptionalState::getOccurrenceIndex)
                .collect(Collectors.toList());
        Assert.assertTrue("expected non-negative occurrence indices, got " + firstIdx,
                firstIdx.stream().allMatch(i -> i >= 0));

        Assert.assertNull(second.getName());
        Assert.assertEquals("x (a), x", unparser.unparse(model));
    }

    @Test
    public void presentStatesShareSameOccurrenceIndexWithinItem() {
        OptOccurrenceModel model = parser.parse("x (keep)");
        Item item = model.getRoot().getItems().get(0);
        List<OptionalState> states = item.getLexicalInfo().getOptionalStates();
        Assert.assertFalse(states.isEmpty());

        // All terminals of one optional group on one item share one occurrence index
        int idx = states.get(0).getOccurrenceIndex();
        Assert.assertTrue(idx >= 0);
        for (OptionalState s : states) {
            Assert.assertEquals(idx, s.getOccurrenceIndex());
            Assert.assertTrue(s.getPresent());
            Assert.assertNotNull(s.getGrammarElementPath());
            Assert.assertFalse(s.getGrammarElementPath().isEmpty());
        }
    }

    @Test
    public void siblingItemsGetDistinctOccurrenceIndicesForSamePaths() {
        OptOccurrenceModel model = parser.parse("x (a), x (b)");
        Item first = model.getRoot().getItems().get(0);
        Item second = model.getRoot().getItems().get(1);

        Map<String, Integer> firstByPath = indexByPath(first);
        Map<String, Integer> secondByPath = indexByPath(second);
        Assert.assertFalse(firstByPath.isEmpty());
        Assert.assertEquals(firstByPath.keySet(), secondByPath.keySet());

        for (String path : firstByPath.keySet()) {
            Assert.assertNotEquals(
                    "same path on siblings must not share occurrenceIndex: " + path,
                    firstByPath.get(path), secondByPath.get(path));
        }
    }

    @Test
    public void mixedPresenceRoundTrips() {
        String source = "x, x (a), x (b), x";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void mixedPresenceKeepsOddWhitespace() {
        String source = "x  ,  x (a),\nx (b) , x";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void setNameOnAbsentSiblingDoesNotStealEarlierPresence() {
        OptOccurrenceModel model = parser.parse("x (keep), x");
        Item second = model.getRoot().getItems().get(1);
        second.setName("new");
        Assert.assertEquals("x (keep), x (new)", unparser.unparse(model));
    }

    @Test
    public void setNameOnMiddleAbsentKeepsNeighbors() {
        OptOccurrenceModel model = parser.parse("x (a), x, x (c)");
        model.getRoot().getItems().get(1).setName("b");
        Assert.assertEquals("x (a), x (b), x (c)", unparser.unparse(model));
    }

    @Test
    public void clearNameOnPresentSiblingLeavesOthersIntact() {
        OptOccurrenceModel model = parser.parse("x (a), x (b)");
        model.getRoot().getItems().get(0).setName(null);
        Assert.assertEquals("x, x (b)", unparser.unparse(model));
    }

    @Test
    public void clearThenRestoreDoesNotCorruptSibling() {
        String source = "x (a), x (b)";
        OptOccurrenceModel model = parser.parse(source);

        model.getRoot().getItems().get(0).setName(null);
        Assert.assertEquals("x, x (b)", unparser.unparse(model));

        model.getRoot().getItems().get(0).setName("a");
        Assert.assertEquals("x (a), x (b)", unparser.unparse(model));
    }

    @Test
    public void renamePresentValueKeepsGroupAndSiblingWhitespace() {
        String source = "x (old) , x (keep)";
        OptOccurrenceModel model = parser.parse(source);
        model.getRoot().getItems().get(0).setName("new");
        Assert.assertEquals("x (new) , x (keep)", unparser.unparse(model));
    }

    private static Map<String, Integer> indexByPath(Item item) {
        return item.getLexicalInfo().getOptionalStates().stream()
                .collect(Collectors.toMap(
                        OptionalState::getGrammarElementPath,
                        OptionalState::getOccurrenceIndex,
                        (a, b) -> {
                            Assert.assertEquals(a, b);
                            return a;
                        }));
    }
}
