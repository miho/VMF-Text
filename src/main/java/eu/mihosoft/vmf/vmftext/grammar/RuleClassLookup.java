package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.Objects;
import java.util.Optional;

public class RuleClassLookup implements DelegatedBehavior<GrammarModel> {
    private GrammarModel caller;

    @Override
    public void setCaller(GrammarModel caller) {
        this.caller = caller;
    }

    public Optional<RuleClass> ruleClassByName(String name) {
        return caller.getRuleClasses().stream().filter(rCls-> Objects.equals(rCls.nameWithUpper(),name)).findFirst();
    }

    public Optional<Property> propertyByName(String rClsName, String propName) {
        Optional<RuleClass> ruleClass = ruleClassByName(rClsName);

        if(!ruleClass.isPresent()) {
            throw new RuntimeException("RuleClass '" + rClsName + "' not found!");
        }

        return ruleClass.get().getProperties().stream().
                filter(prop->Objects.equals(prop.nameWithLower(), propName)).findFirst();
    }
}
