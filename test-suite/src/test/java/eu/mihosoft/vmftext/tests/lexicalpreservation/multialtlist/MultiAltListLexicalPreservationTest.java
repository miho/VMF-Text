/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.multialtlist;

import eu.mihosoft.vmftext.tests.lexicalpreservation.multialtlist.parser.MultiAltListModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multialtlist.unparser.MultiAltListModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * List shapes on non-first alternatives must still receive {@code ListShapeHint}s
 * and splice without clearing trivia.
 */
public class MultiAltListLexicalPreservationTest {

    private final MultiAltListModelParser parser = new MultiAltListModelParser();
    private final MultiAltListModelUnparser unparser = new MultiAltListModelUnparser();

    @Test
    public void nonFirstAltReceivesParenHint() {
        MultiAltListModel model = parser.parse("nums (1, 2, 3)");
        Assert.assertFalse(model.getRoot().getLexicalInfo().getListShapeHints().isEmpty());
        boolean foundNums = false;
        boolean foundIds = false;
        for (ListShapeHint h : model.getRoot().getLexicalInfo().getListShapeHints()) {
            if ("nums".equals(h.getPropertyName())
                    && h.getPrefixCount() == 2 // 'nums' '('
                    && h.getAlternativeIndex() == 1) {
                foundNums = true;
            }
            if ("ids".equals(h.getPropertyName()) && h.getAlternativeIndex() == 2) {
                foundIds = true;
            }
        }
        Assert.assertTrue("expected nums hint from alt 1", foundNums);
        Assert.assertTrue("expected ids hint from alt 2 (even when unused)", foundIds);
    }

    @Test
    public void numsAltRemoveMiddlePreservesNeighbors() {
        String source = "nums (1,  2,\n 3)";
        MultiAltListModel model = parser.parse(source);
        model.getRoot().getNums().remove(1);
        Assert.assertEquals("nums (1,\n 3)", unparser.unparse(model));
    }

    @Test
    public void idsAltRoundTripAndSplice() {
        String source = "ids a,  b,\nc";
        MultiAltListModel model = parser.parse(source);
        Assert.assertEquals(source, unparser.unparse(model));
        model.getRoot().getIds().remove(1);
        Assert.assertEquals("ids a,\nc", unparser.unparse(model));
    }

    @Test
    public void idsAltInsertAtZeroThenRemoveRestores() {
        String source = "ids a,  b";
        MultiAltListModel model = parser.parse(source);
        model.getRoot().getIds().add(0, "z");
        Assert.assertEquals("ids z, a,  b", unparser.unparse(model));
        model.getRoot().getIds().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
