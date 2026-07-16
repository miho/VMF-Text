/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.multilistparen;

import eu.mihosoft.vmftext.tests.lexicalpreservation.multilistparen.parser.MultiListParenModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.multilistparen.unparser.MultiListParenModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Multi-list with an opener after a sibling trailer:
 * {@code ids… ';' '(' nums… ')'}. Codegen hints assign {@code '('} to
 * {@code nums.prefixCount}.
 */
public class MultiListParenShapeHintTest {

    private final MultiListParenModelParser parser = new MultiListParenModelParser();
    private final MultiListParenModelUnparser unparser = new MultiListParenModelUnparser();

    @Test
    public void hintsAssignOpenerToSecondList() {
        MultiListParenModel model = parser.parse("a, b ; (1, 2)");
        Assert.assertEquals(2, model.getRoot().getLexicalInfo().getListShapeHints().size());
        ListShapeHint ids = model.getRoot().getLexicalInfo().getListShapeHints().get(0);
        ListShapeHint nums = model.getRoot().getLexicalInfo().getListShapeHints().get(1);
        Assert.assertEquals("ids", ids.getPropertyName());
        Assert.assertEquals("nums", nums.getPropertyName());
        Assert.assertEquals(0, ids.getPrefixCount());
        Assert.assertTrue("ids owns ';'", ids.getSuffixCount() >= 1);
        Assert.assertEquals("nums owns '('", 1, nums.getPrefixCount());
        Assert.assertTrue(nums.getSuffixCount() >= 1);
    }

    @Test
    public void uneditedRoundTripExact() {
        String source = "a ,  b; (1,\n 2)";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void editNumsKeepsIdsAndParens() {
        String source = "a ,  b; (1,\n 2)";
        MultiListParenModel model = parser.parse(source);
        model.getRoot().getNums().set(0, 9);
        Assert.assertEquals("a ,  b; (9,\n 2)", unparser.unparse(model));
    }

    @Test
    public void removeFromNumsPreservesIds() {
        String source = "a ,  b; (1,\n 2)";
        MultiListParenModel model = parser.parse(source);
        model.getRoot().getNums().remove(0);
        Assert.assertEquals("a ,  b; (\n 2)", unparser.unparse(model));
    }

    @Test
    public void removeFromIdsPreservesNums() {
        String source = "a ,  b; (1,\n 2)";
        MultiListParenModel model = parser.parse(source);
        model.getRoot().getIds().remove(0);
        Assert.assertEquals("  b; (1,\n 2)", unparser.unparse(model));
    }

    @Test
    public void insertAtZeroOnNumsKeepsParenSpacing() {
        String source = "a ,  b; (1,\n 2)";
        MultiListParenModel model = parser.parse(source);
        model.getRoot().getNums().add(0, 0);
        Assert.assertEquals("a ,  b; (0, 1,\n 2)", unparser.unparse(model));
        model.getRoot().getNums().remove(0);
        Assert.assertEquals(source, unparser.unparse(model));
    }
}
