/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.json;

import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Model-typed delimited list splice for JSON arrays:
 * parent trivia holds {@code '['}, commas, {@code ']'} only (size {@code N+1}).
 */
public class JsonArrayListEditLexicalPreservationTest {

    private final JSONModelParser parser = new JSONModelParser();
    private final JSONModelUnparser unparser = new JSONModelUnparser();

    @Test
    public void uneditedArrayRoundTripExact() {
        String source = "[1.0 ,  2.0,\n 3.0]";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void numberValueSetPreservesSiblingBytes() {
        String source = "[1.0 ,  2.0,\n 3.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        ((NumberValue) array.getValues().get(1)).setValue(9.0);
        Assert.assertEquals("[1.0 ,  9.0,\n 3.0]", unparser.unparse(model));
    }

    @Test
    public void removeMiddleKeepsNeighborCommaTrivia() {
        String source = "[1.0 ,  2.0,\n 3.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        array.getValues().remove(1);
        Assert.assertEquals("[1.0,\n 3.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 2);
    }

    @Test
    public void removeFirstKeepsLaterWhitespace() {
        String source = "[1.0 ,  2.0,\n 3.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        array.getValues().remove(0);
        Assert.assertEquals("[  2.0,\n 3.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 2);
    }

    @Test
    public void removeLastPreservesHead() {
        String source = "[1.0 ,  2.0,\n 3.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        array.getValues().remove(2);
        Assert.assertEquals("[1.0 ,  2.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 2);
    }

    @Test
    public void appendNumberPreservesHeadWhitespace() {
        String source = "[1.0 ,  2.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        NumberValue n = NumberValue.newInstance();
        n.setValue(3.0);
        array.getValues().add(n);
        Assert.assertEquals("[1.0 ,  2.0, 3.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 3);
    }

    @Test
    public void removeToEmptyUnparsesBrackets() {
        String source = "[1.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        array.getValues().remove(0);
        Assert.assertTrue(array.getValues().isEmpty());
        Assert.assertEquals("[]", unparser.unparse(model));
        Assert.assertEquals(2, array.getLexicalInfo().getTriviaPieces().size());
    }

    @Test
    public void emptyToOneElement() {
        String source = "[]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        NumberValue n = NumberValue.newInstance();
        n.setValue(1.0);
        array.getValues().add(n);
        Assert.assertEquals("[1.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 1);
    }

    @Test
    public void insertAtZeroPreservesLaterWhitespace() {
        String source = "[1.0 ,  2.0]";
        JSONModel model = parser.parse(source);
        Array array = onlyArray(model);
        NumberValue n = NumberValue.newInstance();
        n.setValue(0.0);
        array.getValues().add(0, n);
        Assert.assertEquals("[0.0, 1.0 ,  2.0]", unparser.unparse(model));
        assertParentTriviaSize(array, 3);
    }

    @Test
    public void objectPairsRemoveMiddleKeepsNeighbors() {
        String source = "{\"a\": 1.0 ,  \"b\": 2.0,\n \"c\": 3.0}";
        JSONModel model = parser.parse(source);
        Obj obj = onlyObj(model);
        obj.getPairs().remove(1);
        Assert.assertEquals("{\"a\": 1.0,\n \"c\": 3.0}", unparser.unparse(model));
    }

    @Test
    public void objectPairsAppendPreservesHead() {
        String source = "{\"a\": 1.0 ,  \"b\": 2.0}";
        JSONModel model = parser.parse(source);
        Obj obj = onlyObj(model);
        Pair p = Pair.newInstance();
        p.setKey("c");
        NumberValue n = NumberValue.newInstance();
        n.setValue(3.0);
        p.setValue(n);
        obj.getPairs().add(p);
        Assert.assertEquals("{\"a\": 1.0 ,  \"b\": 2.0, \"c\": 3.0}", unparser.unparse(model));
    }

    @Test
    public void nestedArrayEditKeepsOuterBytes() {
        String source = "[1.0, [2.0 ,  3.0], 4.0]";
        JSONModel model = parser.parse(source);
        Array outer = onlyArray(model);
        Array inner = (Array) outer.getValues().get(1);
        ((NumberValue) inner.getValues().get(0)).setValue(9.0);
        Assert.assertEquals("[1.0, [9.0 ,  3.0], 4.0]", unparser.unparse(model));
    }

    private static Array onlyArray(JSONModel model) {
        return model.vmf().content().stream(Array.class).findFirst()
                .orElseThrow(() -> new AssertionError("no Array"));
    }

    private static Obj onlyObj(JSONModel model) {
        return model.vmf().content().stream(Obj.class).findFirst()
                .orElseThrow(() -> new AssertionError("no Obj"));
    }

    private static void assertParentTriviaSize(Array array, int nValues) {
        Assert.assertEquals(nValues + 1, array.getLexicalInfo().getTriviaPieces().size());
    }
}
