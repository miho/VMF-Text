package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.UPElement;

public class EbnfTypeDelegate implements DelegatedBehavior<UPElement>{
    private UPElement caller;

    @Override
    public void setCaller(UPElement caller) {
        this.caller = caller;
    }

    // TODO *? non greedy (19.12.2017)

    public boolean ebnfOneMany() {
        return caller.getText().endsWith("+");
    }
    public boolean ebnfZeroMany(){
        return caller.getText().endsWith("*");
    }
    public boolean ebnfOne(){
        return !caller.getText().endsWith("+")
                &&!caller.getText().endsWith("*");
    }
    public boolean ebnfOptional(){
        return caller.getText().endsWith("?");
    }
}
