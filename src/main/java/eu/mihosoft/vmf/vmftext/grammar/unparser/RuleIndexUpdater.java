package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;

public class RuleIndexUpdater implements DelegatedBehavior<UnparserModel>{
    private UnparserModel caller;

    @Override
    public void setCaller(UnparserModel caller) {
        this.caller = caller;
    }

    public void onUnparserModelInstantiated() {
        caller.getRules().addChangeListener(evt -> {

            // update id of every rule that has been added to the list
            for(int i = 0; i < evt.added().indices().length;i++) {
                UPRule r =evt.added().elements().get(i);
                r.setId(evt.added().indices()[i]);
            }

        });
    }
}
