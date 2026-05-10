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

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vmf.runtime.core.Property;
import eu.mihosoft.vmf.runtime.core.VObject;
import eu.mihosoft.vmftext.tests.expressionlang.ExpressionLangModel;
import eu.mihosoft.vmftext.tests.expressionlang.NumberExpr;
import eu.mihosoft.vmftext.tests.expressionlang.PlusMinusOpExpr;
import eu.mihosoft.vmftext.tests.expressionlang.Prog;
import org.junit.Assert;

import java.util.IdentityHashMap;
import java.util.Map;

public class Test {
    @org.junit.Test
    public void preventMultipleOccurrencesOfInstanceTest() {

        ExpressionLangModel model = ExpressionLangModel.newBuilder().build();

        PlusMinusOpExpr operator = PlusMinusOpExpr.newBuilder().
                withLeft(NumberExpr.newBuilder().withValue(2.0).build()).
                withRight(NumberExpr.newBuilder().withValue(3.0).build()).build();

        model.setRoot(Prog.newBuilder().withExpression(operator).build());

        boolean multipleOccurrences1 = containsMultipleOccurrencesExcludingParents(model);

        operator.setLeft(operator);

        boolean multipleOccurrences2 = containsMultipleOccurrencesExcludingParents(model);

        Assert.assertTrue("The model does not contain multiple occurrences of the same instance", !multipleOccurrences1);

        Assert.assertTrue("The model does contain multiple occurrences of the same instance", multipleOccurrences2);

    }

    private boolean containsMultipleOccurrencesExcludingParents(VObject root) {
        Map<VObject, Integer> occurrences = new IdentityHashMap<>();
        collectOccurrences(root, occurrences, new IdentityHashMap<>());

        return occurrences.values().stream().anyMatch(n -> n > 1);
    }

    @SuppressWarnings("unchecked")
    private void collectOccurrences(
            VObject current,
            Map<VObject, Integer> occurrences,
            Map<VObject, Boolean> activePath) {

        if(current == null) {
            return;
        }

        occurrences.put(current, occurrences.getOrDefault(current, 0) + 1);

        // We have counted the repeated reference. Stop here to avoid infinite
        // recursion on self-referential/cyclic graphs.
        if(activePath.containsKey(current)) {
            return;
        }

        activePath.put(current, Boolean.TRUE);

        for(Property property : current.vmf().reflect().properties()) {
            if("parent".equals(property.getName())) {
                continue;
            }

            if(property.getType().isListType()) {
                Object value = property.get();
                if(value instanceof VList) {
                    for(Object element : (VList<Object>) value) {
                        if(element instanceof VObject) {
                            collectOccurrences((VObject) element, occurrences, activePath);
                        }
                    }
                }
            } else if(property.getType().isModelType()) {
                Object value = property.get();
                if(value instanceof VObject) {
                    collectOccurrences((VObject) value, occurrences, activePath);
                }
            }
        }

        activePath.remove(current);
    }
}
