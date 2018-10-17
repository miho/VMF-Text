package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.vmftext.grammar.*;

import java.util.List;

/**
 * TODO 17.10.2018
 */
public final class UPRuleUtil {
    private UPRuleUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static String getPath(UPElement e) {
        if(e == null) return "";

        if(e instanceof UPRuleBase) {
            return getPath((UPRuleBase) e);
        } else {
            return getPath(e.getParentAlt())+"/e"+e.getElementId();
        }
    }

    private static String getPath(AlternativeBase a) {

        if(a == null) return "";

        return getPath(a.getParentRule()) + "/a"+a.getAltId();
    }

    private static String getPath(UPRuleBase r) {

        if(r == null) return "";

        if(r instanceof UPSubRuleElement) {
            UPSubRuleElement sre = (UPSubRuleElement) r;
            return getPath(sre.getParentAlt()) + "/sr"+sre.getRuleId();
        } else if(r instanceof UPNamedSubRuleElement) {
            UPNamedSubRuleElement sre = (UPNamedSubRuleElement) r;
            return getPath(sre.getParentAlt()) + "/sr"+sre.getRuleId();
        } else {
            return "/r" + r.getRuleId();
        }
    }
}
