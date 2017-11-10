package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vcollections.VListChange;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import vjavax.observer.collection.CollectionChangeEvent;
import vjavax.observer.collection.CollectionChangeListener;

import java.util.ArrayList;
import java.util.Objects;

public class CheckPropertiesDelegate  implements DelegatedBehavior<RuleClass> {
    RuleClass caller;

    @Override
    public void setCaller(RuleClass caller) {
        this.caller = caller;
    }

    public void onRuleClassInstantiated() {
        caller.getProperties().addChangeListener(new CollectionChangeListener<Property, VList<Property>, VListChange<Property>>() {
            @Override
            public void onChange(CollectionChangeEvent<Property, VList<Property>, VListChange<Property>> evt) {

                // remove duplicate properties
                for(Property p1 : evt.added().elements()) {
                    for(Property p2 : new ArrayList<>(evt.source())) {
                        if(p1!=p2 && Objects.equals(p1.getName(),p2.getName())) {
                            evt.source().remove(p1);
                        }
                    }
                }
            }
        });
    }

}
