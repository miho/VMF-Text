/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmftext.tests.expressionlang;

import eu.mihosoft.vmftext.tests.expressionlang.parser.ExpressionLangModelParser;
import eu.mihosoft.vmftext.tests.expressionlang.unparser.ExpressionLangModelUnparser;
import org.junit.Assert;

import java.io.IOException;

public class Test {
    @org.junit.Test
    public void testExpressionLangInterpreter() {
        ExpressionLangModelParser parser = new ExpressionLangModelParser();
        ExpressionLangModel model = parser.parse("2.5+3*(14-27.218)/2.3");

        GrammarInterpreter interpreter = new GrammarInterpreter();
        Double value = interpreter.execute(model);

        Assert.assertEquals(-14.740869565217391, value, 1e-12);
    }

    @org.junit.Test
    public void testExpressionLangParseUnparseTest() {
        ExpressionLangModelParser parser = new ExpressionLangModelParser();
        ExpressionLangModel model = parser.parse("2.5+3*(14-27.218)/2.3");

        ExpressionLangModelUnparser unparser = new ExpressionLangModelUnparser();
        ExpressionLangModel modelup = parser.parse(unparser.unparse(model));

        Assert.assertEquals(model, modelup);

        GrammarInterpreter interpreter = new GrammarInterpreter();
        Double valueExpected = interpreter.execute(model);

        Double valueUP = interpreter.execute(model);

        Assert.assertEquals(valueExpected, valueUP, 1e-12);
        Assert.assertEquals(model, modelup);
    }

    @org.junit.Test
    public void testParsePerRule() {
        ExpressionLangModelParser parser = new ExpressionLangModelParser();

        NumberExpr numberExpr = parser.parseNumberExpr("2.5");

        Assert.assertEquals(2.5, numberExpr.getValue(), 1e-12);

        PlusMinusOpExpr op = parser.parsePlusMinusOpExpr("2 + 3");

        Assert.assertEquals("+", op.getOperator());

        Assert.assertTrue("Left expr. should be a number expr.", op.getLeft() instanceof NumberExpr);
        Assert.assertEquals(2, ((NumberExpr)op.getLeft()).getValue(), 1e-12);

        Assert.assertTrue("Right expr. should be a number expr.", op.getRight() instanceof NumberExpr);
        Assert.assertEquals(3, ((NumberExpr)op.getRight()).getValue(), 1e-12);

        Expr expr = parser.parseExpr("(1.3+4)");

        Assert.assertTrue("Expected paranexpr., got " + expr.getClass(), expr instanceof ParanExpr);

        expr = parser.parseExpr("4.3");

        Assert.assertTrue("Expected numberexpr., got " + expr.getClass(), expr instanceof NumberExpr);
    }
}
