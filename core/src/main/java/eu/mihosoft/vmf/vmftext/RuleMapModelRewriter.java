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
package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.vmf.vmftext.grammar.Property;
import eu.mihosoft.vmf.vmftext.grammar.RuleClass;
import eu.mihosoft.vmf.vmftext.grammar.RuleMapEntry;
import eu.mihosoft.vmf.vmftext.grammar.RuleMappings;
import eu.mihosoft.vmf.vmftext.grammar.Type;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Model-level rewriter for parser-rule maps / model rewriting (issue #1).
 *
 * <p>After the {@link GrammarModel} has been built, this pass redirects every
 * grammar-derived, rule-typed property whose type matches a {@code RuleMap}
 * source to the mapped target model type. This flattens a wrapper rule at its
 * reference sites (e.g. {@code Program.expressions : Expression[]} becomes
 * {@code NumberLiteral[]}); the parse/unparse conversion expressions that
 * actually bridge the two types are emitted by the code generator.</p>
 *
 * <p>Source/target names in the DSL are the generated model type names
 * (PascalCase), whereas {@link RuleClass#getName()} keeps the verbatim grammar
 * spelling (lower-camel); the two are reconciled by capitalizing the first
 * character.</p>
 *
 * <p>Matching supports two cases:</p>
 * <ol>
 *   <li>a property typed directly by the source type; and</li>
 *   <li>a property typed by a base rule whose <em>single</em> concrete subclass
 *       is the source (the common single-labeled-alternative case).</li>
 * </ol>
 * Polymorphic base types with more than one subclass are left untouched because
 * redirecting them would be ambiguous.
 */
final class RuleMapModelRewriter {

    private RuleMapModelRewriter() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Applies all rule maps declared on the model in place. No-op if the model
     * declares no rule maps.
     */
    static void apply(GrammarModel model) {

        RuleMappings ruleMappings = model.getRuleMappings();

        if (ruleMappings == null || ruleMappings.getRuleMappings().isEmpty()) {
            return;
        }

        // generated model type name (PascalCase) -> rule class
        Map<String, RuleClass> byTypeName = new HashMap<>();
        for (RuleClass rc : model.getRuleClasses()) {
            byTypeName.put(cap(rc.getName()), rc);
        }

        for (RuleClass cls : model.getRuleClasses()) {
            for (Property p : cls.getProperties()) {

                Type t = p.getType();

                if (t == null || !t.isRuleType()) {
                    continue;
                }

                Optional<RuleMapEntry> entryOpt =
                        resolveEntry(ruleMappings, byTypeName, cls.getName(), cap(t.getName()));

                if (!entryOpt.isPresent()) {
                    continue;
                }

                RuleMapEntry entry = entryOpt.get();
                RuleClass targetCls = byTypeName.get(entry.getTargetName());

                if (targetCls == null) {
                    throw new VMFTextGenerationException(
                            "RuleMap target type '" + entry.getTargetName()
                                    + "' is not a known model type (source '"
                                    + entry.getSourceName() + "').");
                }

                Logger.debug("RuleMap: redirecting " + cls.getName() + "." + p.getName()
                        + " from '" + cap(t.getName()) + "' to '" + entry.getTargetName() + "'");

                p.setType(Type.newBuilder().
                        withRuleType(true).
                        withArrayType(t.isArrayType()).
                        withPackageName("").
                        withName(targetCls.getName()).
                        build());

                // carry the bridge into the code generator (parse + unparse)
                p.setRuleMapSourceTypeName(entry.getSourceName());
                p.setRuleMapSourceToTargetCode(entry.getSourceToTargetCode());
                p.setRuleMapTargetToSourceCode(entry.getTargetToSourceCode());
            }
        }
    }

    /**
     * Resolves the rule-map entry that applies to a property of the given type
     * in the given container rule: either the type is the mapped source, or the
     * type is a base rule whose single concrete subclass is the mapped source.
     */
    private static Optional<RuleMapEntry> resolveEntry(RuleMappings ruleMappings,
                                                       Map<String, RuleClass> byTypeName,
                                                       String containerRuleName,
                                                       String propertyTypeName) {

        Optional<RuleMapEntry> direct =
                ruleMappings.mappingBySourceName(containerRuleName, propertyTypeName);

        if (direct.isPresent()) {
            return direct;
        }

        RuleClass base = byTypeName.get(propertyTypeName);

        if (base != null && base.getChildClasses().size() == 1) {
            RuleClass onlyChild = base.getChildClasses().get(0);
            return ruleMappings.mappingBySourceName(containerRuleName, cap(onlyChild.getName()));
        }

        return Optional.empty();
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
