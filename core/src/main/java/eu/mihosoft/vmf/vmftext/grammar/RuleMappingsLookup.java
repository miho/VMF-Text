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
package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.Objects;
import java.util.Optional;

/**
 * Lookup delegate for parser-rule maps / model rewriting (issue #1).
 *
 * <p>Resolves the {@link RuleMapEntry} that applies to a given source model
 * type at a given container rule, honoring the optional {@code applyTo}
 * scoping of each {@link RuleMapping} block (empty {@code applyTo} = global).</p>
 */
public class RuleMappingsLookup implements DelegatedBehavior<RuleMappings> {

    private RuleMappings caller;

    @Override
    public void setCaller(RuleMappings caller) {
        this.caller = caller;
    }

    /**
     * Returns the rule-map entry whose source type matches {@code sourceName}
     * and whose block applies to {@code containerRuleName}, if any.
     *
     * @param containerRuleName the rule that contains the property to redirect
     * @param sourceName        the source model type name of the mapping
     * @return the matching entry, or empty
     */
    public Optional<RuleMapEntry> mappingBySourceName(String containerRuleName, String sourceName) {
        return caller.getRuleMappings().stream().
                filter(rm -> rm.getApplyToNames().isEmpty()
                        || rm.getApplyToNames().contains(containerRuleName)).
                flatMap(rm -> rm.getEntries().stream()).
                filter(e -> Objects.equals(e.getSourceName(), sourceName)).
                findFirst();
    }

    /**
     * Indicates whether a rule-map entry applies to the given source type and
     * container rule.
     */
    public boolean mappingBySourceNameExists(String containerRuleName, String sourceName) {
        return mappingBySourceName(containerRuleName, sourceName).isPresent();
    }
}
