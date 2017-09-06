package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

public class GetRootClassDelegate implements DelegatedBehavior<GrammarModel> {
    private GrammarModel caller;

    @Override
    public void setCaller(GrammarModel caller) {
        this.caller = caller;
    }

    public boolean hasRootClass() {
        return !caller.getRuleClasses().isEmpty();
    }

    public RuleClass rootClass() {
        if(hasRootClass()) {
            return caller.getRuleClasses().get(0);
        } else {
            return null;
        }
    }
}
