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
package eu.mihosoft.vmf.vmftext.grammar.unparser;

import org.junit.Assert;
import org.junit.Test;

/** #48: the unparser check rules must not contain actions or semantic predicates. */
public class StripActionsAndPredicatesTest {

    @Test
    public void predicateAtTheLeftEdgeIsRemoved() {
        Assert.assertEquals("value=annotationVal",
                UPRuleUtil.stripActionsAndPredicates("{ this.IsNotIdentifierAssign() }? value=annotationVal"));
    }

    @Test
    public void predicateOnlyAlternativeBecomesEmpty() {
        Assert.assertEquals("", UPRuleUtil.stripActionsAndPredicates("{lineTerminatorAhead()}?"));
    }

    @Test
    public void actionInTheMiddleCollapsesToOneSpace() {
        Assert.assertEquals("a b", UPRuleUtil.stripActionsAndPredicates("a { System.out.println(\"x\"); } b"));
    }

    @Test
    public void nestedBracesAndBracesInStringsAreHandled() {
        Assert.assertEquals("x y",
                UPRuleUtil.stripActionsAndPredicates("x { if (a) { s = \"}\"; c = '{'; } } y"));
    }

    @Test
    public void braceLiteralsOfTheGrammarStay() {
        Assert.assertEquals("'{' statements+=statement* '}'",
                UPRuleUtil.stripActionsAndPredicates("'{' statements+=statement* '}'"));
    }

    @Test
    public void subRuleTextKeepsItsEbnfSuffix() {
        Assert.assertEquals("(identifier=Identifier)?",
                UPRuleUtil.stripActionsAndPredicates("({!here(LineTerminator)}? identifier=Identifier)?"));
    }

    @Test
    public void textWithoutBracesIsUnchanged() {
        Assert.assertSame("a b c", UPRuleUtil.stripActionsAndPredicates("a b c"));
        Assert.assertNull(UPRuleUtil.stripActionsAndPredicates(null));
    }
}
