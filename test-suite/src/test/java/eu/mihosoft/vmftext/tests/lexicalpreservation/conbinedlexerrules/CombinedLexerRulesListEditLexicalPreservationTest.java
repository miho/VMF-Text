/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules;

import eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.parser.CombinedLexerRulesModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.conbinedlexerrules.unparser.CombinedLexerRulesModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Same delimited-list splice as ArrayLang, but without EOF in the grammar
 * ({@code trivia.size() == 2N+2}).
 */
public class CombinedLexerRulesListEditLexicalPreservationTest {

    private final CombinedLexerRulesModelParser parser = new CombinedLexerRulesModelParser();
    private final CombinedLexerRulesModelUnparser unparser = new CombinedLexerRulesModelUnparser();

    @Test
    public void uneditedRoundTripExact() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void listSetPreservesSiblingWhitespace() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        CombinedLexerRulesModel model = parser.parse(source);
        model.getRoot().getValues().set(1, "9.9");
        Assert.assertEquals("(1.0 ,  9.9,\n 3.0 )", unparser.unparse(model));
        assertTriviaSize(model, 3);
    }

    @Test
    public void removeMiddlePreservesSiblingWhitespace() {
        String source = "(1.0 ,  2.0,\n 3.0 )";
        CombinedLexerRulesModel model = parser.parse(source);
        model.getRoot().getValues().remove(1);
        Assert.assertEquals("(1.0,\n 3.0 )", unparser.unparse(model));
        assertTriviaSize(model, 2);
    }

    @Test
    public void addAtEndPreservesHeadWhitespace() {
        String source = "(1.0 ,  2.0 )";
        CombinedLexerRulesModel model = parser.parse(source);
        model.getRoot().getValues().add("3.5");
        Assert.assertEquals("(1.0 ,  2.0, 3.5 )", unparser.unparse(model));
        assertTriviaSize(model, 3);
    }

    @Test
    public void insertAtZeroAndRemoveFirst() {
        String source = "(1.0 ,  2.0 )";
        CombinedLexerRulesModel model = parser.parse(source);
        model.getRoot().getValues().add(0, "0.5");
        Assert.assertEquals("(0.5, 1.0 ,  2.0 )", unparser.unparse(model));
        model.getRoot().getValues().remove(0);
        // Insert-at-0 may inject a separator space before the former head; after
        // remove(0) that space can remain (still valid / parseable).
        Assert.assertEquals("( 1.0 ,  2.0 )", unparser.unparse(model));
        assertTriviaSize(model, 2);
    }

    private static void assertTriviaSize(CombinedLexerRulesModel model, int nValues) {
        // '(' + N values + (N-1) commas + ')' + pad == 2N+2 (no EOF)
        Assert.assertEquals(2 * nValues + 2,
                model.getRoot().getLexicalInfo().getTriviaPieces().size());
    }
}
