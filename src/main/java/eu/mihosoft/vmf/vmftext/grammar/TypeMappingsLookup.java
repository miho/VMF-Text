package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.Objects;
import java.util.Optional;

public class TypeMappingsLookup implements DelegatedBehavior<TypeMappings>{
    private TypeMappings caller;

    @Override
    public void setCaller(TypeMappings caller) {
        this.caller = caller;
    }


    public Optional<Mapping> mappingByRuleName(String containerRuleName, String name) {

        return caller.getTypeMappings().stream().filter(tm->tm.getApplyToNames().contains(containerRuleName)).
                flatMap(tm->tm.getEntries().stream()).filter(mE-> Objects.equals(mE.getRuleName(), name)).findFirst();
    }

    public String targetTypeNameOfMapping(String containerRuleName, String name) {
        return mappingByRuleName(containerRuleName, name).map(tm->tm.getTypeName()).orElse("String");
    }

    public String conversionCodeOfMapping(String containerRuleName, String name) {
        return mappingByRuleName(containerRuleName, name).map(tm->tm.getMappingCode()).orElse("entry");
    }

}
