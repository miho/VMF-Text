/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.arraylang;

import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Original lexer spellings for type-mapped tokens must survive unparse when
 * the semantic value is unchanged (e.g. {@code 1} stays {@code 1}, not
 * {@code 1.0}).
 */
public class ArrayLangOriginalLexemePreservationTest {

    private final ArrayLangModelParser parser = new ArrayLangModelParser();
    private final ArrayLangModelUnparser unparser = new ArrayLangModelUnparser();

    @Test
    public void uneditedIntegerFormRoundTripsExactly() {
        String source = "(1, 2)";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void mixedSpellingsRoundTripExactly() {
        String source = "(1, 2.0, .5, 3.)";
        Assert.assertEquals(source, unparser.unparse(parser.parse(source)));
    }

    @Test
    public void siblingEditKeepsUnchangedLexemes() {
        String source = "(1, 2.0, 3)";
        ArrayLangModel model = parser.parse(source);
        model.getRoot().getValues().set(1, 9.0);
        Assert.assertEquals("(1, 9.0, 3)", unparser.unparse(model));
    }

    @Test
    public void editedValueUsesConverterText() {
        String source = "(1, 2)";
        ArrayLangModel model = parser.parse(source);
        model.getRoot().getValues().set(0, 1.5);
        Assert.assertEquals("(1.5, 2)", unparser.unparse(model));
    }
}
