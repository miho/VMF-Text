package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.vmftext.grammar.*;

import java.util.List;
import java.util.function.Predicate;

/**
 * TODO 17.10.2018
 */
public final class UPRuleUtil {
    private UPRuleUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Returns the path of the specified element, e.g., {@code /r0/a2/sr4/a1/e7}. The path
     * specifies the location of the element in the grammar tree.
     * @param e element
     * @return path of the specified element
     */
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

    /**
     * Determines whether a specified element is effectively optional (see issue github issue #6).
     * @param e element to check
     * @return {@code true} if the specified element is effectively optional; {@code false} otherwise
     */
    public static boolean isEffectivelyOptional(UPElement e) {

        // if e is a named element it is not optional
        boolean effectivelyOptionalNamed = e instanceof WithName;
        if(effectivelyOptionalNamed) return false;

        // ebnf suffix indicates e is optional
        // if true then we can skip further tests since e is definitely effectively optional
        boolean effectivelyOptionalViaEbnf = e.ebnfOptional() || e.ebnfZeroMany();
        if(effectivelyOptionalViaEbnf) return true;

        // indicates whether the elements in the specified alt are all unnamed
        final Predicate<AlternativeBase> onlyUnnamedElementsInAltPred = (a)->a.getElements().stream().filter(
                ne->ne instanceof UPNamedElement
                        || ne instanceof UPNamedSubRuleElement).count()==0;

        // indicates whether at least one sibling alt has only unnamed elements
        final Predicate<AlternativeBase> atLeastOneSiblingAltWithoutNamedElementsPred =
                (alt)-> alt.getParentRule().getAlternatives().
                stream().filter(a->a!=alt).filter(onlyUnnamedElementsInAltPred).count()
                        > 0;

        // e is not directly optional but could be effectively optional because of the parent
        // sub-rule being optional
        boolean onlyUnnamedSiblings = onlyUnnamedElementsInAltPred.test(e.getParentAlt());

        // if we didn't find named siblings and the parent rule is optional then
        // we can be sure that e is effectively optional
        if(onlyUnnamedSiblings && e.getParentAlt().getParentRule() instanceof UPSubRuleElement
                && (((UPSubRuleElement)e.getParentAlt().getParentRule()).ebnfOptional()
                || ((UPSubRuleElement)e.getParentAlt().getParentRule()).ebnfZeroMany())) {
            return true;
        }

        boolean atLeastOneSiblingAltWithoutNamedElements =
                atLeastOneSiblingAltWithoutNamedElementsPred.test(e.getParentAlt());

        // if we didn't find named siblings and the at least one sibling alt consists completely of unnamed
        // elements e is effectively optional even if the parent rule is not optional
        //
        // example:
        //
        //   ( '!' | '?' )
        //
        if(onlyUnnamedSiblings && atLeastOneSiblingAltWithoutNamedElements) {
            return true;
        }

        // now we check parent sub-rules for optionality
        UPRuleBase r = e.getParentAlt().getParentRule();
        while(r != null) {

            if(r instanceof UPSubRuleElement) {
                UPSubRuleElement sre= (UPSubRuleElement) r;
                boolean opt = sre.ebnfOptional() || sre.ebnfZeroMany();

                onlyUnnamedSiblings = onlyUnnamedElementsInAltPred.test(sre.getParentAlt());

                // if the subrule element is optional we need to check the siblings in this alt whether they
                // make the current alt non optional, e.g., named element
                if(opt) {

                    // if we didn't find named siblings we can be sure that e is effectively optional
                    if(onlyUnnamedSiblings) {
                        return true;
                    }
                } else {
                    atLeastOneSiblingAltWithoutNamedElements =
                            atLeastOneSiblingAltWithoutNamedElementsPred.test(sre.getParentAlt());
                    // if we didn't find named siblings and the at least one sibling alt consists completely of unnamed
                    // elements e is effectively optional even if the parent rule is not optional
                    //
                    // example:
                    //
                    //   ( '!' | '?' )
                    //
                    if(onlyUnnamedSiblings && atLeastOneSiblingAltWithoutNamedElements) {
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
