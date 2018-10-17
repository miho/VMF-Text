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

    public static boolean isEffectivelyOptional(UPElement e) {

        // if e is a named element it is not optional
        boolean effectivelyOptionalUnnamed = e instanceof WithName;
        if(!effectivelyOptionalUnnamed) return false;

        // ebnf suffix indicates e is optional
        // if true then we can skip further tests since e is definitely effectively optional
        boolean effectivelyOptionalViaEbnf = e.ebnfOptional() || e.ebnfZeroMany();
        if(effectivelyOptionalViaEbnf) return true;

        // e is not directly optional but could be effectively optional because of the parent
        // sub-rule being optional

        boolean namedSiblings =
                e.getParentAlt().getElements().stream().filter(
                        ne->ne instanceof UPNamedElement
                                || ne instanceof UPNamedSubRuleElement).count()>0;

        // if we didn't find named siblings we can b e sure that e is effectively optional
        if(!namedSiblings) {
            return true;
        }

        // now we check parent sub-rules for optionality
        UPRuleBase r = e.getParentAlt().getParentRule();
        while(r != null) {

            if(r instanceof UPSubRuleElement) {
                UPSubRuleElement sre= (UPSubRuleElement) r;
                boolean opt = sre.ebnfOptional() || sre.ebnfZeroMany();

                // if the subrule element is optional we need to check the siblings in this alt whether they
                // make the current alt non optional, e.g., named element
                if(opt) {
                    namedSiblings =
                            sre.getParentAlt().getElements().stream().filter(
                                    ne->ne instanceof UPNamedElement
                                    || ne instanceof UPNamedSubRuleElement).count()>0;

                    // if we didn't find named siblings we can b e sure that e is effectively optional
                    if(!namedSiblings) {
                        return true;
                    }
                }

                r = sre.getParentAlt().getParentRule();
            } else {
                return false;
            }
        }

        return false;
    }
}
