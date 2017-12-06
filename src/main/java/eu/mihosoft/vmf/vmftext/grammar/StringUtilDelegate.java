package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vcollections.VListChange;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import vjavax.observer.collection.CollectionChangeEvent;
import vjavax.observer.collection.CollectionChangeListener;

import java.util.ArrayList;
import java.util.Objects;

public class StringUtilDelegate implements DelegatedBehavior<RuleClass> {
    RuleClass caller;

    @Override
    public void setCaller(RuleClass caller) {
        this.caller = caller;
    }

    public String superInterfacesString() {
        return String.join(", ", caller.getSuperInterfaces());
    }

}
