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

    public Optional<Mapping> getMappingByRuleName(String name) {
        return caller.getEntries().stream().filter(mE-> Objects.equals(mE.getRuleName(), name)).findFirst();
    }
}
