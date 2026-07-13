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
 */
package eu.mihosoft.vmf.vmftext.grammar.target;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
 * Java-target implementation of optional-presence recording via embedded ANTLR
 * actions and reflective read-back from generated parser rule contexts.
 */
public final class JavaAntlrOptionalStateProvider implements AntlrTargetOptionalStateProvider {

    public static final JavaAntlrOptionalStateProvider INSTANCE = new JavaAntlrOptionalStateProvider();

    public static final String PARSED_OPTIONAL_FLAG_ACTION =
            "{$ctx.__vmf_text__parsed_optional = true;} ";
    public static final String RULE_LOCALS_DECLARATION =
            " locals [List<String> __vmf_text__optionalStates = new ArrayList<String>(), boolean __vmf_text__parsed_optional = false]";
    public static final String APPEND_RULE_LOCALS_DECLARATION =
            ", List<String> __vmf_text__optionalStates = new ArrayList<String>(), boolean __vmf_text__parsed_optional = false";
    public static final String OPTIONAL_STATES_FIELD = "__vmf_text__optionalStates";

    private JavaAntlrOptionalStateProvider() {
    }

    @Override
    public String getParsedOptionalFlagAction() {
        return PARSED_OPTIONAL_FLAG_ACTION;
    }

    @Override
    public String getRuleLocalsDeclaration() {
        return RULE_LOCALS_DECLARATION;
    }

    @Override
    public String getAppendRuleLocalsDeclaration() {
        return APPEND_RULE_LOCALS_DECLARATION;
    }

    @Override
    public String buildComplexOptionalStateAction(String grammarElementPath) {
        return "{$ctx." + OPTIONAL_STATES_FIELD + ".add(\""
                + grammarElementPath + "=\"+$ctx.__vmf_text__parsed_optional);$ctx.__vmf_text__parsed_optional=false;}";
    }

    @Override
    public String buildOptionalStateAction(String grammarElementPath) {
        return "{$ctx." + OPTIONAL_STATES_FIELD + ".add(\"" + grammarElementPath + "=true\");}";
    }

    @Override
    public String stripInjectedActions(String grammarText) {
        return grammarText
                .replace(RULE_LOCALS_DECLARATION, "")
                .replace(APPEND_RULE_LOCALS_DECLARATION, "")
                .replaceAll("\\{\\$ctx\\.__vmf_text__[^}]*\\}[ ]?", "");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> readOptionalStateEntries(ParserRuleContext ctx) {
        try {
            java.lang.reflect.Field field = ctx.getClass().getField(OPTIONAL_STATES_FIELD);
            Object value = field.get(ctx);
            return (List<String>) value;
        } catch (Exception ex) {
            throw new RuntimeException("Cannot read optional-state list from parser context "
                    + ctx.getClass().getSimpleName(), ex);
        }
    }
}
