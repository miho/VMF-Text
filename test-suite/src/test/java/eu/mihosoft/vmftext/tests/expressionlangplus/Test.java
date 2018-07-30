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
package eu.mihosoft.vmftext.tests.expressionlangplus;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vmftext.tests.expressionlang.ExpressionLangModel;
import eu.mihosoft.vmftext.tests.expressionlang.parser.ExpressionLangModelParser;
import eu.mihosoft.vmftext.tests.expressionlang.unparser.ExpressionLangModelUnparser;
import eu.mihosoft.vmftext.tests.expressionlangplus.parser.ExpressionLangPlusModelParser;
import eu.mihosoft.vmftext.tests.expressionlangplus.unparser.ExpressionLangPlusModelUnparser;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Map;

public class Test {
    @org.junit.Test
    public void testExpressionLangInterpreter() {
        ExpressionLangPlusModelParser parser = new ExpressionLangPlusModelParser();
        ExpressionLangPlusModel model = parser.parse(
                   "a = 2.5+3.0*(14.0-27.218)/2.3;" +
                        "b = {1.2,2.3,3.4};" +
                        "c = cos(0.6) + sqrt(16.0);" +
                        "bRes = b[0]+b[1]");

        // interpret model
        GrammarInterpreter interpreter = new GrammarInterpreter();
        Map<String,Expr> results = interpreter.execute(model);

        Assert.assertEquals(4, results.size());

        checkResults(results);
    }

    private void checkResults(Map<String,Expr> results) {
        Expr aExpr = results.get("a");
        Expr bExpr = results.get("b");

        Expr cExpr = results.get("c");
        Expr bResExpr = results.get("bRes");

        Assert.assertTrue(aExpr instanceof NumberExpr);
        Assert.assertTrue(bExpr instanceof ArrayExpr);
        Assert.assertTrue(cExpr instanceof NumberExpr);
        Assert.assertTrue(bResExpr instanceof NumberExpr);
        Assert.assertEquals(-14.740869565217391, ((NumberExpr)aExpr).getValue(), 1e-12);
        Assert.assertEquals(ArrayExpr.newBuilder().
                withArrayValue(Array.newBuilder().
                withElements(VList.newInstance(Arrays.asList(1.2,2.3,3.4))).
                        build()).
                build(),
                bExpr
        );
        Assert.assertEquals(4.825335614909679, ((NumberExpr)cExpr).getValue(), 1e-12);
        Assert.assertEquals(3.5, ((NumberExpr)bResExpr).getValue(), 1e-12);
    }

    @org.junit.Test
    public void testExpressionLangParseUnparseTest() {
        ExpressionLangPlusModelParser parser = new ExpressionLangPlusModelParser();
        ExpressionLangPlusModel model = parser.parse(
                "a = 2.5+3.0*(14.0-27.218)/2.3;" +
                        "b = {1.2,2.3,3.4};" +
                        "c = cos(0.6) + sqrt(16.0);" +
                        "bRes = b[0]+b[1]");

        // interpret model
        GrammarInterpreter interpreter = new GrammarInterpreter();
        Map<String,Expr> results = interpreter.execute(model);
        checkResults(results);

        // unparse the model and parse model again from unparsed code
        ExpressionLangPlusModelUnparser unparser = new ExpressionLangPlusModelUnparser();
        ExpressionLangPlusModel modelup = parser.parse(unparser.unparse(model));

        // interpret model from unparsed code
        GrammarInterpreter interpreterup = new GrammarInterpreter();
        Map<String,Expr> resultsup = interpreterup.execute(modelup);
        checkResults(resultsup);

        Assert.assertEquals(model,modelup);
    }
}
