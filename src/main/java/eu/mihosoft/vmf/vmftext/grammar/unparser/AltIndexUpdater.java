package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.AlternativeBase;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UPRuleBase;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;

public class AltIndexUpdater implements DelegatedBehavior<UPRuleBase>{
    private UPRuleBase caller;

    @Override
    public void setCaller(UPRuleBase caller) {
        this.caller = caller;
    }

    public void onUPRuleBaseInstantiated() {
        caller.getAlternatives().addChangeListener(evt -> {

            // update id of every alternative that has been added to the list
            for(int i = 0; i < evt.added().indices().length;i++) {
                AlternativeBase a =evt.added().elements().get(i);
                a.setId(evt.added().indices()[i]);
            }

        });
    }
}
