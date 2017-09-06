package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

public class TypeNameDelegate implements DelegatedBehavior<Type>{
    Type caller;

    @Override
    public void setCaller(Type caller) {
        this.caller = caller;
    }

    public String nameWithUpper() {
        return StringUtil.firstToUpper(caller.getName());
    }

    public String asModelTypeName() {
        return (caller.getPackageName().isEmpty()?"":caller.getPackageName()+".")
                + nameWithUpper()
                + (caller.isArrayType()?"[]":"");
    }

    public String asJavaTypeNameNoCollections() {
        return (caller.getPackageName().isEmpty()?"":caller.getPackageName()+".")
                + nameWithUpper();
    }
}
