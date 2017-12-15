package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.AlternativeBase;
import eu.mihosoft.vmf.vmftext.grammar.SubRule;
import eu.mihosoft.vmf.vmftext.grammar.UPElement;
import eu.mihosoft.vmf.vmftext.grammar.UPRuleBase;

public class SubRuleIndexUpdater implements DelegatedBehavior<AlternativeBase>{
    private AlternativeBase caller;

    @Override
    public void setCaller(AlternativeBase caller) {
        this.caller = caller;
    }

    public void onAlternativeBaseInstantiated() {
        caller.getElements().addChangeListener(evt -> {

            if(!evt.wasAdded()) {
                return;
            }

            // update id of every sub-rule that has been added to the list
            for(int i = 0; i < evt.added().indices().length;i++) {

                UPElement e = evt.added().elements().get(i);

                if (e instanceof SubRule) {
                    // count number of elements of type sub-rule from 0-"our index"
                    int index = (int) evt.source().subList(0, evt.added().indices()[i]).stream().
                            filter(el -> el instanceof SubRule).count();
                    ((SubRule) e).setId(index);
                }
            }
        });
    }
}
