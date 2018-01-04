package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeMappingsLookup implements DelegatedBehavior<TypeMappings>{
    private TypeMappings caller;

    @Override
    public void setCaller(TypeMappings caller) {
        this.caller = caller;
    }


    /**
     * Returns the specified mapping by rule name.
     * @param containerRuleName the name of the rule that contains the parameter which shall be converted
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the specified mapping
     */
    public Optional<Mapping> mappingByRuleName(String containerRuleName, String paramRuleName) {

        return caller.getTypeMappings().stream().filter(tm->tm.getApplyToNames().isEmpty()
                || tm.getApplyToNames().contains(containerRuleName)).
                flatMap(tm->tm.getEntries().stream()).filter(mE-> Objects.equals(mE.getRuleName(), paramRuleName)).findFirst();
    }

    /**
     * Indicates whether the specified mapping exists.
     * @param containerRuleName the name of the rule that contains the parameter which shall be converted
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return {@code true} if the specified mapping exists; {@code false} otherwise
     */
    public boolean mappingByRuleNameExists(String containerRuleName, String paramRuleName) {

        return caller.getTypeMappings().stream().filter(tm->tm.getApplyToNames().isEmpty()
                || tm.getApplyToNames().contains(containerRuleName)).
                flatMap(tm->tm.getEntries().stream()).filter(mE-> Objects.equals(mE.getRuleName(), paramRuleName)).
                findFirst().isPresent();
    }

    /**
     * Returns all mappings that are applied to the specified rule
     * @param containerRuleName the name of the rule
     * @return all mappings that are applied to the specified rule
     */
    public VList<Mapping> mappingsByRuleName(String containerRuleName) {
        return VList.newInstance(caller.getTypeMappings().stream().filter(tm->tm.getApplyToNames().isEmpty()
                || tm.getApplyToNames().contains(containerRuleName)).
                flatMap(tm->tm.getEntries().stream()).distinct().collect(Collectors.toList()));
    }

    /**
     * Returns the target type name of the specified mapping.
     *
     * @param containerRuleName the name of the rule that contains the parameter which shall be converted
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the target type name or {@code String} if the specified mapping does not exist
     */
    public String targetTypeNameOfMapping(String containerRuleName, String paramRuleName) {
        return mappingByRuleName(containerRuleName, paramRuleName).map(tm->tm.getTypeName()).orElse("String");
    }

    /**
     * Returns the conversion code of the specified mapping.
     *
     * @param containerRuleName the name of the rule that contains the parameter which shall be converted
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the conversion code of the specified mapping; returns default conversion code to {@code String} if the specified mapping does not exist
     */
    public String conversionCodeOfMappingStringToType(String containerRuleName, String paramRuleName) {
        return mappingByRuleName(containerRuleName, paramRuleName).map(tm->tm.getStringToTypeCode()).orElse("entry.getText()");
    }

    /**
     * Returns the conversion code of the specified mapping.
     *
     * @param containerRuleName the name of the rule that contains the parameter which shall be converted
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the conversion code of the specified mapping; returns default conversion code to {@code String} if the specified mapping does not exist
     */
    public String conversionCodeOfMappingTypeToString(String containerRuleName, String paramRuleName) {
        return mappingByRuleName(containerRuleName, paramRuleName).map(tm->tm.getTypeToStringCode()).orElse("entry.toString()");
    }

}
