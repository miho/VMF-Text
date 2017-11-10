package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vcollections.VListChange;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import vjavax.observer.collection.CollectionChangeEvent;
import vjavax.observer.collection.CollectionChangeListener;

import java.util.ArrayList;
import java.util.Objects;

public class CheckRulesDelegate implements DelegatedBehavior<GrammarModel> {
    GrammarModel caller;

    @Override
    public void setCaller(GrammarModel caller) {
        this.caller = caller;
    }

    public void onGrammarModelInstantiated() {
        caller.getRuleClasses().addChangeListener(new CollectionChangeListener<RuleClass, VList<RuleClass>, VListChange<RuleClass>>() {
            @Override
            public void onChange(CollectionChangeEvent<RuleClass, VList<RuleClass>, VListChange<RuleClass>> evt) {

                // remove duplicate properties
                for(RuleClass p1 : evt.added().elements()) {
                    for(RuleClass p2 : new ArrayList<>(evt.source())) {
                        if(p1!=p2 && Objects.equals(p1.getName(),p2.getName())) {
                            evt.source().remove(p1);
                            System.err.println("ERROR: duplicate rule classes '"+p1.getName()+"'!");
                        }
                    }
                }
            }
        });
    }

}
