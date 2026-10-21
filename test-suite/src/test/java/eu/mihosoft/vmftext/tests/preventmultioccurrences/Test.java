/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
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

/**
 * Grammar children are containment references: an element is contained in
 * exactly one place. Setting it into another slot (or adding it to another
 * list) moves it there and releases it from its previous parent, so the model
 * never holds the same instance twice. (This test used to document the
 * opposite, before the model became a containment tree.)
 */
public class Test {
    @org.junit.Test
    public void preventMultipleOccurrencesOfInstanceTest() {

        ExpressionLangModel model = ExpressionLangModel.newBuilder().build();

        NumberExpr two = NumberExpr.newBuilder().withValue(2.0).build();
        NumberExpr three = NumberExpr.newBuilder().withValue(3.0).build();

        PlusMinusOpExpr operator = PlusMinusOpExpr.newBuilder().
                withLeft(two).
                withRight(three).build();

        model.setRoot(Prog.newBuilder().withExpression(operator).build());

        Assert.assertFalse("The model does not contain multiple occurrences of the same instance",
                containsMultipleOccurrencesExcludingParents(model));
        Assert.assertSame(operator, two.getParent());
        Assert.assertSame(operator, three.getParent());

        // setting an element into another slot moves it
        operator.setRight(two);

        Assert.assertFalse("The model never contains multiple occurrences of the same instance",
                containsMultipleOccurrencesExcludingParents(model));
        Assert.assertNull("the previous slot is released", operator.getLeft());
        Assert.assertSame(two, operator.getRight());
        Assert.assertSame(operator, two.getParent());
        Assert.assertNull("the replaced element is released", three.getParent());

        // the same holds for the root of the model
        ExpressionLangModel otherModel = ExpressionLangModel.newBuilder().build();
        Prog prog = model.getRoot();

        otherModel.setRoot(prog);

        Assert.assertNull(model.getRoot());
        Assert.assertSame(prog, otherModel.getRoot());
        Assert.assertFalse(containsMultipleOccurrencesExcludingParents(otherModel));
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
