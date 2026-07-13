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
package eu.mihosoft.vmftext.tests.dotAsTerminal;

import eu.mihosoft.vmftext.tests.dotAsTerminal.parser.DotAsTerminalModelParser;
import eu.mihosoft.vmftext.tests.dotAsTerminal.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.dotAsTerminal.unparser.DotAsTerminalModelUnparser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Full coverage for labeled parser wildcards ({@code label=.} / {@code label+=.}),
 * issue #8.
 */
public class DotAsTerminalTest {

    private final DotAsTerminalModelParser parser = new DotAsTerminalModelParser();
    private final DotAsTerminalModelUnparser unparser;

    public DotAsTerminalTest() {
        unparser = new DotAsTerminalModelUnparser();
        unparser.setFormatter(new BaseFormatter());
    }

    @Test
    public void labeledDotSingle_parsesTokenText() {
        LabeledDotSingle model = parser.parseLabeledDotSingle("hello");

        Assert.assertEquals("hello", model.getValue());
    }

    @Test
    public void labeledDotSingle_roundTrips() {
        String code = "42";
        LabeledDotSingle model = parser.parseLabeledDotSingle(code);

        Assert.assertEquals("42", model.getValue());
        Assert.assertEquals("42", parser.parseLabeledDotSingle(unparser.unparse(model)).getValue());
    }

    @Test
    public void labeledDotSingle_programmaticUnparse() {
        LabeledDotSingle model = LabeledDotSingle.newInstance();
        model.setValue("xyz");

        Assert.assertEquals("xyz", parser.parseLabeledDotSingle(unparser.unparse(model)).getValue());
    }

    @Test
    public void labeledDotList_collectsTokenTexts() {
        LabeledDotList model = parser.parseLabeledDotList("a 1 !");

        Assert.assertEquals(Arrays.asList("a", "1", "!"), model.getItems());
    }

    @Test
    public void labeledDotList_roundTrips() {
        String code = "hello 123 ;";
        LabeledDotList model = parser.parseLabeledDotList(code);

        Assert.assertEquals(Arrays.asList("hello", "123", ";"), model.getItems());
        assertSameItems(model.getItems(),
                parser.parseLabeledDotList(unparser.unparse(model)).getItems());
    }

    @Test
    public void labeledDotList_programmaticUnparse() {
        LabeledDotList model = LabeledDotList.newInstance();
        model.getItems().add("foo");
        model.getItems().add("99");
        model.getItems().add("?");

        assertSameItems(model.getItems(),
                parser.parseLabeledDotList(unparser.unparse(model)).getItems());
    }

    @Test
    public void textToExpand_splitsWordsAndIgnored() {
        // words take WORD tokens; everything else lands in ignored via +=.
        TextToExpand model = parser.parseTextToExpand("hi <## 7 ##>");

        Assert.assertEquals(Arrays.asList("hi"), model.getWords());
        Assert.assertEquals(Arrays.asList("<##", "7", "##>"), model.getIgnored());
    }

    @Test
    public void textToExpand_roundTripsWordsOnly() {
        String code = "alpha beta gamma";
        TextToExpand model = parser.parseTextToExpand(code);

        Assert.assertEquals(Arrays.asList("alpha", "beta", "gamma"), model.getWords());
        Assert.assertTrue(model.getIgnored().isEmpty());

        TextToExpand again = parser.parseTextToExpand(unparser.unparse(model));
        assertSameItems(model.getWords(), again.getWords());
        Assert.assertTrue(again.getIgnored().isEmpty());
    }

    @Test
    public void textToExpand_mixedParseSplitsLists() {
        // Mixed word/ignored alternatives are split into separate list
        // properties. Round-trip of interleaved order is not asserted here:
        // the unparser cannot reconstruct original alternation sequence from
        // two independent lists alone.
        String code = "alpha <## beta ##> gamma";
        TextToExpand model = parser.parseTextToExpand(code);

        Assert.assertEquals(Arrays.asList("alpha", "beta", "gamma"), model.getWords());
        Assert.assertEquals(Arrays.asList("<##", "##>"), model.getIgnored());
    }

    @Test
    public void textToExpand_onlyIgnoredTokens() {
        TextToExpand model = parser.parseTextToExpand("12 !!");

        Assert.assertTrue(model.getWords().isEmpty());
        Assert.assertEquals(Arrays.asList("12", "!", "!"), model.getIgnored());

        TextToExpand again = parser.parseTextToExpand(unparser.unparse(model));
        Assert.assertTrue(again.getWords().isEmpty());
        assertSameItems(model.getIgnored(), again.getIgnored());
    }

    @Test
    public void anyLexerRuleWorkaround_stillWorks() {
        // ANY is a catch-all lexer rule (single character). Use a character that
        // no other lexer rule claims so the ANY path is exercised. Regression for
        // the pre-issue-#8 workaround.
        MainRuleWorking model = parser.parseMainRuleWorking("@");

        Assert.assertEquals("@", model.getMyLabel());
        Assert.assertEquals("@", parser.parseMainRuleWorking(unparser.unparse(model)).getMyLabel());
    }

    private static void assertSameItems(List<String> expected, List<String> actual) {
        Assert.assertEquals(expected, actual);
    }
}
