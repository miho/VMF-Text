/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex;

import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.parser.NestedUnnamedModelParser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.unnamedoptionalscomplex.unparser.NestedUnnamedModelUnparser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Optional null↔value edits must update optional presence so unparse emits or
 * drops the surrounding optional group correctly.
 */
public class OptionalNullValueLexicalPreservationTest {

    private final NestedUnnamedModelParser parser = new NestedUnnamedModelParser();
    private final NestedUnnamedModelUnparser unparser = new NestedUnnamedModelUnparser();

    @Test
    public void nullToValueOnR2EmitsParensAndName() {
        NestedUnnamedModel model = parser.parse("r1, r2");
        RuleWithOptionals2 r2 = model.getRoot().getChildren2().get(0);
        Assert.assertNull(r2.getName());

        r2.setName("x");

        Assert.assertEquals("r1, r2 (x)", unparser.unparse(model));
    }

    @Test
    public void valueToNullOnR2DropsOptionalGroup() {
        NestedUnnamedModel model = parser.parse("r1, r2 (test123)");
        RuleWithOptionals2 r2 = model.getRoot().getChildren2().get(0);

        r2.setName(null);

        Assert.assertEquals("r1, r2", unparser.unparse(model));
    }

    @Test
    public void nullToValueOnR4EmitsGroup() {
        NestedUnnamedModel model = parser.parse("r1, r2, r3 (), r4");
        RuleWithOptionals4 r4 = model.getRoot().getChildren4().get(0);
        Assert.assertNull(r4.getName());

        r4.setName("x");

        Assert.assertEquals("r1, r2, r3 (), r4 (x)", unparser.unparse(model));
    }

    @Test
    public void setNameInsideEmptyOptionalGroupPreservesParens() {
        NestedUnnamedModel model = parser.parse("r1, r2, r3 (), r4 ()");
        RuleWithOptionals4 r4 = model.getRoot().getChildren4().get(0);
        Assert.assertNull(r4.getName());

        r4.setName("x");

        Assert.assertEquals("r1, r2, r3 (), r4 (x)", unparser.unparse(model));
    }
}
