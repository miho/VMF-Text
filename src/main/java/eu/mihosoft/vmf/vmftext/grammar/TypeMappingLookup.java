package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.Objects;
import java.util.Optional;

public class TypeMappingLookup implements DelegatedBehavior<TypeMapping>{
    private TypeMapping caller;

    @Override
    public void setCaller(TypeMapping caller) {
        this.caller = caller;
    }

    /**
     * Returns the specified mapping by rule name.
     *
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the specified mapping
     */
    public Optional<Mapping> mappingByRuleName(String paramRuleName) {
        return caller.getEntries().stream().filter(mE-> Objects.equals(mE.getRuleName(), paramRuleName)).findFirst();
    }
}
