package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

public class NameDelegate implements DelegatedBehavior<WithName>{
    WithName caller;

    @Override
    public void setCaller(WithName caller) {
        this.caller = caller;
    }

    public String nameWithUpper() {
        return StringUtil.firstToUpper(caller.getName());
    }

    public String nameWithLower() {
        return StringUtil.firstToLower(caller.getName());
    }
}
