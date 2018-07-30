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
package eu.mihosoft.vmftext.tests.preventmultioccurrences;

import eu.mihosoft.vmf.runtime.core.VIterator;
import eu.mihosoft.vmftext.tests.expressionlang.ExpressionLangModel;
import eu.mihosoft.vmftext.tests.expressionlang.NumberExpr;
import eu.mihosoft.vmftext.tests.expressionlang.PlusMinusOpExpr;
import eu.mihosoft.vmftext.tests.expressionlang.Prog;
import org.junit.Assert;

import java.util.stream.Collectors;

public class Test {
    @org.junit.Test
    public void preventMultipleOccurrencesOfInstanceTest() {

        ExpressionLangModel model = ExpressionLangModel.newBuilder().build();

        PlusMinusOpExpr operator = PlusMinusOpExpr.newBuilder().
                withLeft(NumberExpr.newBuilder().withValue(2.0).build()).
                withRight(NumberExpr.newBuilder().withValue(3.0).build()).build();

        model.setRoot(Prog.newBuilder().withExpression(operator).build());

        boolean multipleOccurrences1 = model.vmf().content().stream(VIterator.IterationStrategy.UNIQUE_PROPERTY)
                .collect(Collectors.groupingBy(System::identityHashCode, Collectors.counting())).
                        values().stream().filter(n->n>1).count()>0;

        operator.setLeft(operator);

        boolean multipleOccurrences2 = model.vmf().content().stream(VIterator.IterationStrategy.UNIQUE_PROPERTY)
                .collect(Collectors.groupingBy(System::identityHashCode, Collectors.counting())).
                        values().stream().filter(n->n>1).count()>0;

        Assert.assertTrue("The model does not contain multiple occurrences of the same instance", !multipleOccurrences1);

        Assert.assertTrue("The model does contain multiple occurrences of the same instance", multipleOccurrences2);

    }
}
