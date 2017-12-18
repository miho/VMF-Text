package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.UPElement;
import eu.mihosoft.vmf.vmftext.grammar.UPNamedElement;
import eu.mihosoft.vmf.vmftext.grammar.UPNamedSubRuleElement;
import eu.mihosoft.vmf.vmftext.grammar.UPSubRuleElement;

public class ElementTypeChecker implements DelegatedBehavior<UPElement>{
    private UPElement caller;

    @Override
    public void setCaller(UPElement caller) {
        this.caller = caller;
    }

    public boolean namedElement() {
        return caller instanceof UPNamedElement;
    }

    public boolean namedSubRuleElement() {
        return caller instanceof UPNamedSubRuleElement;
    }

    public boolean unnamedSubRuleElement() {
        return caller instanceof UPSubRuleElement;
    }
}
