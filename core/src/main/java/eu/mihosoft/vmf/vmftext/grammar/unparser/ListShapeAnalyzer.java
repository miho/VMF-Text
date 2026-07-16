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

import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;
import eu.mihosoft.vmf.vmftext.grammar.UPElement;
import eu.mihosoft.vmf.vmftext.grammar.UPNamedElement;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UPSubRuleElement;
import eu.mihosoft.vmf.vmftext.grammar.AlternativeBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Derives {@code ListShapeHint} specs from the unparser model so generated
 * converters can splice trivia without guessing from {@code trivia.size()}.
 */
public final class ListShapeAnalyzer {

    public static final class HintSpec {
        private final String ruleName;
        private final String propertyName;
        private final String kind;
        private final int prefixCount;
        private final int suffixCount;
        private final int orderIndex;
        private final boolean modelTyped;

        public HintSpec(String ruleName, String propertyName, String kind,
                        int prefixCount, int suffixCount, int orderIndex,
                        boolean modelTyped) {
            this.ruleName = ruleName;
            this.propertyName = propertyName;
            this.kind = kind;
            this.prefixCount = prefixCount;
            this.suffixCount = suffixCount;
            this.orderIndex = orderIndex;
            this.modelTyped = modelTyped;
        }

        public String getRuleName() { return ruleName; }
        public String getPropertyName() { return propertyName; }
        public String getKind() { return kind; }
        public int getPrefixCount() { return prefixCount; }
        public int getSuffixCount() { return suffixCount; }
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
            // Use the first alternative as the shape oracle (typical list rules
            // share one structural alt for the delimited form).
            AlternativeBase alt = rule.getAlternatives().get(0);
            List<FlatTerminal> flat = flatten(alt);
            Map<String, ListSegment> segments = segmentLists(flat);
            int order = 0;
            for (Map.Entry<String, ListSegment> e : segments.entrySet()) {
                String prop = e.getKey();
                ListSegment seg = e.getValue();
                String key = rule.getName().toLowerCase(Locale.ROOT) + "#" + prop;
                boolean modelTyped = Boolean.TRUE.equals(modelTypedByRuleProp.get(key));
                // Also try exact rule name key
                if (!modelTypedByRuleProp.containsKey(key)) {
                    modelTyped = Boolean.TRUE.equals(
                            modelTypedByRuleProp.get(rule.getName() + "#" + prop));
                }
                String kind = modelTyped ? "MODEL_DELIMITED" : "PRIMITIVE_DELIMITED";
                out.add(new HintSpec(
                        rule.getName(),
                        prop,
                        kind,
                        seg.prefixCount,
                        seg.suffixCount,
                        order++,
                        modelTyped));
            }
        }
        return out;
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

        FlatTerminal(String listProp, boolean terminal) {
            this.listProp = listProp;
            this.terminal = terminal;
        }
    }

    private static final class ListSegment {
        int prefixCount;
        int suffixCount;
        int firstItem;
        int lastItem;
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
            out.add(new FlatTerminal(name, false));
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
            // EOF counts as a suffix terminal for footprint purposes
            out.add(new FlatTerminal(null, true));
        }
    }

    private static Map<String, ListSegment> segmentLists(List<FlatTerminal> flat) {
        Map<String, ListSegment> segments = new LinkedHashMap<>();
        // Find each property's first/last item indices
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
                segments.put(t.listProp, seg);
            } else {
                seg.lastItem = i;
            }
        }
        List<String> order = new ArrayList<>(segments.keySet());
        for (int s = 0; s < order.size(); s++) {
            String prop = order.get(s);
            ListSegment seg = segments.get(prop);
            int nextStart = (s + 1 < order.size())
                    ? segments.get(order.get(s + 1)).firstItem
                    : flat.size();
            if (s == 0) {
                int prefix = 0;
                for (int i = 0; i < seg.firstItem; i++) {
                    if (flat.get(i).terminal) {
                        prefix++;
                    }
                }
                seg.prefixCount = prefix;
            } else {
                // Terminals between lists belong to the previous list's suffix
                // only (e.g. ';' in ids…; nums…). Do not double-count them as
                // this list's prefix. Openers glued to this list are uncommon;
                // extend here if a real grammar needs '(' after a sibling list.
                seg.prefixCount = 0;
            }
            int suffix = 0;
            for (int i = seg.lastItem + 1; i < nextStart; i++) {
                if (flat.get(i).terminal) {
                    suffix++;
                }
            }
            // Parse-time pad slot (empty string) is appended after the last
            // terminal of the rule — count it on the final list segment.
            if (s == order.size() - 1) {
                suffix += 1;
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
}
