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
package eu.mihosoft.vmftext.tests.containment;

import eu.mihosoft.vmf.runtime.core.VIterator;
import eu.mihosoft.vmftext.tests.containment.parser.ContainmentModelParser;
import eu.mihosoft.vmftext.tests.containment.unparser.ContainmentModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The generated model is a proper tree: grammar children are containment
 * references with {@code CodeElement.parent} as opposite. Consequences that
 * editors rely on: structural {@code equals}/{@code hashCode} over the
 * subtree, a setter that replaces a subtree by an equal-looking one in one
 * step, a parent that VMF maintains when children move, and a traversal that
 * stays within the subtree it starts from.
 */
public class ContainmentModelTest {

    private static ContainmentModelParser parser() {
        ContainmentModelParser parser = new ContainmentModelParser();
        parser.setConsoleOutputDisabled(true);
        return parser;
    }

    private static ContainmentModel parse(String code) {
        return parser().parse(code);
    }

    private static String unparse(ContainmentModel model) {
        return new ContainmentModelUnparser().unparse(model);
    }

    @Test
    public void childrenAreContainedAndParentIsMaintainedByVMF() {
        ContainmentModel model = parse("f(a, scale=1);");
        Call call = model.getRoot().getCalls().get(0);
        Argument scale = call.getArgs().get(1);
        Expr one = scale.getValue();

        Assert.assertSame(model.getRoot(), call.getParent());
        Assert.assertSame(call, scale.getParent());
        Assert.assertSame(scale, one.getParent());
        Assert.assertSame(one, one.getLeft().getParent());
        Assert.assertSame(model.getRoot(), one.getLeft().root());
        Assert.assertEquals(5, one.getLeft().pathToRoot().size());

        List<CodeElement> subtree = one.vmf().content().
                stream(CodeElement.class, VIterator.IterationStrategy.CONTAINMENT_TREE).
                collect(Collectors.toList());
        Assert.assertEquals(2, subtree.size());
        Assert.assertSame(one, subtree.get(0));
        Assert.assertSame(one.getLeft(), subtree.get(1));
    }

    @Test
    public void equalsAndHashCodeAreStructuralOverTheSubtree() {
        Expr one = parser().parseExpr("1");
        Expr anotherOne = parser().parseExpr("1");
        Expr four = parser().parseExpr("4");

        Assert.assertNotSame(one, anotherOne);
        Assert.assertEquals(one, anotherOne);
        Assert.assertEquals(one.hashCode(), anotherOne.hashCode());
        Assert.assertNotEquals(one, four);

        // lexical metadata does not take part: equality is semantic
        Assert.assertEquals(parse("f(a, scale=1);"), parse("f( a,scale = 1 );"));
        Assert.assertNotEquals(parse("f(a, scale=1);"), parse("f(a, scale=4);"));
        Assert.assertNotEquals(parse("f(a, scale=1);"), parse("f(a, scale=1+1);"));
    }

    @Test
    public void settingAnotherSubtreeTakesEffectInOneStep() {
        ContainmentModel model = parse("f(a, scale=1);");
        Argument scale = model.getRoot().getCalls().get(0).getArgs().get(1);
        Expr four = parser().parseExpr("4");

        scale.setValue(four);

        Assert.assertSame(four, scale.getValue());
        Assert.assertSame(scale, four.getParent());
        Assert.assertEquals("f(a, scale=4);", unparse(model));
    }

    @Test
    public void settingAnEqualSubtreeStillReplacesTheObject() {
        ContainmentModel model = parse("f(a, scale=1);");
        Argument scale = model.getRoot().getCalls().get(0).getArgs().get(1);
        Expr previous = scale.getValue();
        Expr anotherOne = parser().parseExpr("1");
        Assert.assertEquals(previous, anotherOne);

        scale.setValue(anotherOne);

        Assert.assertSame(anotherOne, scale.getValue());
        Assert.assertSame(scale, anotherOne.getParent());
        Assert.assertNull(previous.getParent());
        Assert.assertEquals("f(a, scale=1);", unparse(model));
    }

    @Test
    public void movingAChildReleasesItFromThePreviousParent() {
        ContainmentModel model = parse("f(a, scale=1);\ng(b);\n");
        Call f = model.getRoot().getCalls().get(0);
        Call g = model.getRoot().getCalls().get(1);
        Argument scale = f.getArgs().get(1);

        g.getArgs().add(scale);

        Assert.assertEquals(1, f.getArgs().size());
        Assert.assertEquals(2, g.getArgs().size());
        Assert.assertSame(scale, g.getArgs().get(1));
        Assert.assertSame(g, scale.getParent());
        Assert.assertEquals(parse("f(a); g(b, scale=1);"), parse(unparse(model)));

        Argument a = f.getArgs().get(0);
        g.getArgs().set(0, a);

        Assert.assertTrue(f.getArgs().isEmpty());
        Assert.assertSame(g, a.getParent());
        Assert.assertEquals(parse("f(); g(a, scale=1);"), parse(unparse(model)));
    }

    @Test
    public void movingOneOfTwoEqualSiblingsRemovesThatOne() {
        ContainmentModel model = parse("f(1, 1);");
        Call f = model.getRoot().getCalls().get(0);
        Argument first = f.getArgs().get(0);
        Argument second = f.getArgs().get(1);
        Assert.assertEquals(first, second);
        Call g = parse("g();").getRoot().getCalls().get(0);

        g.getArgs().add(second);

        Assert.assertEquals(1, f.getArgs().size());
        Assert.assertSame(first, f.getArgs().get(0));
        Assert.assertSame(f, first.getParent());
        Assert.assertSame(g, second.getParent());
    }

    @Test
    public void traversalStaysWithinTheSubtree() {
        ContainmentModel model = parse("f(a, scale=1+2);\ng(b);\n");
        Expr sum = model.getRoot().getCalls().get(0).getArgs().get(1).getValue();

        List<CodeElement> containmentTree = sum.vmf().content().
                stream(CodeElement.class, VIterator.IterationStrategy.CONTAINMENT_TREE).
                collect(Collectors.toList());
        Assert.assertEquals(3, containmentTree.size());
        Assert.assertSame(sum, containmentTree.get(0));
        Assert.assertSame(sum.getLeft(), containmentTree.get(1));
        Assert.assertSame(sum.getRight(), containmentTree.get(2));

        // the default traversal must not escape through 'parent'
        List<CodeElement> defaultTraversal = sum.vmf().content().
                stream(CodeElement.class).collect(Collectors.toList());
        Assert.assertEquals(3, defaultTraversal.size());
        Assert.assertTrue(defaultTraversal.stream().noneMatch(e -> e instanceof Call || e instanceof Program));

        Assert.assertEquals(2, model.getRoot().vmf().content().stream(Call.class).count());
        Assert.assertEquals(4, model.getRoot().vmf().content().stream(Term.class).count());
    }

    @Test
    public void deepCopyCopiesTheSubtree() {
        ContainmentModel model = parse("f(a, scale=1);");
        Call call = model.getRoot().getCalls().get(0);

        Call copy = call.clone();

        Assert.assertEquals(call, copy);
        Assert.assertNotSame(call.getArgs().get(0), copy.getArgs().get(0));
        Assert.assertSame(copy, copy.getArgs().get(0).getParent());
        Assert.assertSame(call, call.getArgs().get(0).getParent());
        Assert.assertNull(copy.getParent());
        Assert.assertEquals(call.asString(), copy.asString());
    }
}
