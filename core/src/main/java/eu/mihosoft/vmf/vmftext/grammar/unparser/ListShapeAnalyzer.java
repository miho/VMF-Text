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

import eu.mihosoft.vmf.vmftext.grammar.AlternativeBase;
import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.UPElement;
import eu.mihosoft.vmf.vmftext.grammar.UPNamedElement;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UPSubRuleElement;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Derives {@code ListShapeHint} specs from the unparser model so generated
 * converters can splice trivia without guessing from {@code trivia.size()}.
 *
 * <p>Each hint carries prefix/suffix terminal counts plus
 * {@code separatorCount} (terminals between consecutive items), covering
 * {@code ','}, multi-token separators like {@code ',' 'and'}, and separator-less
 * {@code item+} lists ({@code separatorCount == 0}).
 */
public final class ListShapeAnalyzer {

    public static final class HintSpec {
        private final String ruleName;
        private final String propertyName;
        private final String kind;
        private final int prefixCount;
        private final int suffixCount;
        private final int separatorCount;
        private final int orderIndex;
        private final boolean modelTyped;

        public HintSpec(String ruleName, String propertyName, String kind,
                        int prefixCount, int suffixCount, int separatorCount,
                        int orderIndex, boolean modelTyped) {
            this.ruleName = ruleName;
            this.propertyName = propertyName;
            this.kind = kind;
            this.prefixCount = prefixCount;
            this.suffixCount = suffixCount;
            this.separatorCount = separatorCount;
            this.orderIndex = orderIndex;
            this.modelTyped = modelTyped;
        }

        public String getRuleName() { return ruleName; }
        public String getPropertyName() { return propertyName; }
        public String getKind() { return kind; }
        public int getPrefixCount() { return prefixCount; }
        public int getSuffixCount() { return suffixCount; }
        public int getSeparatorCount() { return separatorCount; }
        public int getOrderIndex() { return orderIndex; }
        public boolean getModelTyped() { return modelTyped; }
        public boolean isModelTyped() { return modelTyped; }

        /** Capitalized rule class name for generated {@code instanceof} checks. */
        public String getRuleClassName() {
            if (ruleName == null || ruleName.isEmpty()) {
                return "CodeElement";
            }
            return Character.toUpperCase(ruleName.charAt(0)) + ruleName.substring(1);
        }
    }

    private ListShapeAnalyzer() {
    }

    public static List<HintSpec> analyze(GrammarModel grammarModel, UnparserModel unparserModel) {
        List<HintSpec> out = new ArrayList<>();
        if (grammarModel == null || unparserModel == null) {
            return out;
        }
        Map<String, Boolean> modelTypedByRuleProp = indexModelTypedListProps(grammarModel);

        for (UPRule rule : unparserModel.getRules()) {
            if (rule.getAlternatives() == null || rule.getAlternatives().isEmpty()) {
                continue;
            }
            // First alt is the shape oracle (delimited form / primary alt).
            AlternativeBase alt = rule.getAlternatives().get(0);
            List<FlatTerminal> flat = flatten(alt);
            Map<String, ListSegment> segments = segmentLists(flat);
            List<String> order = new ArrayList<>(segments.keySet());
            int orderIndex = 0;
            for (String prop : order) {
                ListSegment seg = segments.get(prop);
                boolean modelTyped = lookupModelTyped(modelTypedByRuleProp, rule.getName(), prop);
                String kind = modelTyped ? "MODEL_DELIMITED" : "PRIMITIVE_DELIMITED";
                int suffix = seg.suffixCount;
                // Parse-time empty pad slot is appended for primitive lists
                // (interleaved value slots). Model-typed parents match
                // bracket/comma terminals only (e.g. JSON size N+1).
                if (!modelTyped && orderIndex == order.size() - 1) {
                    suffix += 1;
                } else if (modelTyped && orderIndex == order.size() - 1
                        && seg.prefixCount == 0 && seg.separatorCount > 0) {
                    // Bare model-typed: parent is commas + pad.
                    suffix += 1;
                }
                out.add(new HintSpec(
                        rule.getName(),
                        prop,
                        kind,
                        seg.prefixCount,
                        suffix,
                        seg.separatorCount,
                        orderIndex++,
                        modelTyped));
            }
        }
        return out;
    }

    private static boolean lookupModelTyped(
            Map<String, Boolean> map, String ruleName, String prop) {
        String lowerKey = ruleName.toLowerCase(Locale.ROOT) + "#" + prop;
        if (map.containsKey(lowerKey)) {
            return Boolean.TRUE.equals(map.get(lowerKey));
        }
        return Boolean.TRUE.equals(map.get(ruleName + "#" + prop));
    }

    private static Map<String, Boolean> indexModelTypedListProps(GrammarModel grammarModel) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (RuleClass rc : grammarModel.getRuleClasses()) {
            for (Property p : rc.getProperties()) {
                if (p.getType() != null && p.getType().isArrayType()) {
                    boolean model = p.getType().isRuleType();
                    map.put(rc.getName().toLowerCase(Locale.ROOT) + "#" + p.getName(), model);
                    map.put(rc.getName() + "#" + p.getName(), model);
                }
            }
        }
        return map;
    }

    private static final class FlatTerminal {
        final String listProp; // null if not a list item carrier
        final boolean terminal;
        final String text;

        FlatTerminal(String listProp, boolean terminal, String text) {
            this.listProp = listProp;
            this.terminal = terminal;
            this.text = text;
        }
    }

    private static final class ListSegment {
        int prefixCount;
        int suffixCount;
        /** Terminals between consecutive item occurrences ({@code 0} for {@code item+}). */
        int separatorCount = 1;
        int firstItem;
        int lastItem;
        int itemOccurrences;
    }

    private static List<FlatTerminal> flatten(AlternativeBase alt) {
        List<FlatTerminal> out = new ArrayList<>();
        if (alt.getElements() == null) {
            return out;
        }
        for (UPElement el : alt.getElements()) {
            flattenElement(el, out);
        }
        return out;
    }

    private static void flattenElement(UPElement el, List<FlatTerminal> out) {
        if (el == null || el.isAction()) {
            return;
        }
        if (el.namedElement() && el.isListType()) {
            String name = ((UPNamedElement) el).getName();
            out.add(new FlatTerminal(name, false, null));
            return;
        }
        if (el.unnamedSubRuleElement() && el instanceof UPSubRuleElement) {
            UPSubRuleElement sub = (UPSubRuleElement) el;
            if (sub.getAlternatives() != null) {
                for (AlternativeBase subAlt : sub.getAlternatives()) {
                    if (subAlt.getElements() == null) {
                        continue;
                    }
                    for (UPElement child : subAlt.getElements()) {
                        flattenElement(child, out);
                    }
                }
            }
            return;
        }
        if (el.isTerminal() || el.isLexerRule()) {
            out.add(new FlatTerminal(null, true, el.getText()));
        }
    }

    private static boolean isOpenerTerminal(FlatTerminal t) {
        if (t == null || !t.terminal || t.text == null) {
            return false;
        }
        String s = stripQuotes(t.text.trim());
        return "(".equals(s) || "[".equals(s) || "{".equals(s);
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 3
                && ((s.startsWith("'") && s.endsWith("'"))
                || (s.startsWith("\"") && s.endsWith("\"")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static Map<String, ListSegment> segmentLists(List<FlatTerminal> flat) {
        Map<String, ListSegment> segments = new LinkedHashMap<>();
        for (int i = 0; i < flat.size(); i++) {
            FlatTerminal t = flat.get(i);
            if (t.listProp == null) {
                continue;
            }
            ListSegment seg = segments.get(t.listProp);
            if (seg == null) {
                seg = new ListSegment();
                seg.firstItem = i;
                seg.lastItem = i;
                seg.itemOccurrences = 1;
                segments.put(t.listProp, seg);
            } else {
                seg.lastItem = i;
                seg.itemOccurrences++;
            }
        }

        // Separator terminals between consecutive occurrences of the same prop.
        for (ListSegment seg : segments.values()) {
            if (seg.itemOccurrences <= 1) {
                // Single flat occurrence → item+ / item* (no structural separator).
                seg.separatorCount = 0;
                continue;
            }
            int terminals = 0;
            for (int i = seg.firstItem + 1; i < seg.lastItem; i++) {
                if (flat.get(i).terminal) {
                    terminals++;
                }
            }
            int gaps = seg.itemOccurrences - 1;
            seg.separatorCount = gaps > 0 ? terminals / gaps : 0;
        }

        List<String> order = new ArrayList<>(segments.keySet());
        int[] openerPrefixForNext = new int[order.size()];
        for (int s = 0; s + 1 < order.size(); s++) {
            ListSegment cur = segments.get(order.get(s));
            ListSegment next = segments.get(order.get(s + 1));
            int openers = 0;
            for (int i = next.firstItem - 1; i > cur.lastItem; i--) {
                FlatTerminal t = flat.get(i);
                if (!t.terminal) {
                    break;
                }
                if (isOpenerTerminal(t)) {
                    openers++;
                } else {
                    break;
                }
            }
            openerPrefixForNext[s + 1] = openers;
        }

        for (int s = 0; s < order.size(); s++) {
            ListSegment seg = segments.get(order.get(s));
            int nextStart = (s + 1 < order.size())
                    ? segments.get(order.get(s + 1)).firstItem
                    : flat.size();
            int openersForNext = (s + 1 < order.size())
                    ? openerPrefixForNext[s + 1]
                    : 0;
            if (s == 0) {
                int prefix = 0;
                for (int i = 0; i < seg.firstItem; i++) {
                    if (flat.get(i).terminal) {
                        prefix++;
                    }
                }
                seg.prefixCount = prefix;
            } else {
                seg.prefixCount = openerPrefixForNext[s];
            }
            int suffix = 0;
            int suffixEnd = nextStart - openersForNext;
            for (int i = seg.lastItem + 1; i < suffixEnd; i++) {
                if (flat.get(i).terminal) {
                    suffix++;
                }
            }
            seg.suffixCount = suffix;
        }
        return segments;
    }

    public static Optional<HintSpec> find(
            List<HintSpec> hints, String ruleName, String propertyName) {
        for (HintSpec h : hints) {
            if (h.ruleName.equalsIgnoreCase(ruleName)
                    && h.propertyName.equals(propertyName)) {
                return Optional.of(h);
            }
        }
        return Optional.empty();
    }

    /** Primitive parent+value trivia size for {@code n} items. */
    public static int primitiveLocalSize(int n, int prefix, int suffix, int sepCount) {
        if (n < 0) {
            return -1;
        }
        if (n == 0) {
            return prefix + suffix;
        }
        return prefix + n + (n - 1) * Math.max(0, sepCount) + suffix;
    }

    /** Model-typed parent-only trivia size for {@code n} items. */
    public static int modelLocalSize(int n, int prefix, int suffix, int sepCount) {
        if (n < 0) {
            return -1;
        }
        if (n == 0) {
            return prefix + suffix;
        }
        return prefix + (n - 1) * Math.max(0, sepCount) + suffix;
    }
}
