package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.vmftext.grammar.*;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    /**
     * Optionality as the generated formatter should see it. Same as
     * {@link #isEffectivelyOptional(UPElement)} except that an unnamed
     * terminal / lexer element whose optionality comes only from a
     * <em>property-driven repetition</em> is reported as NOT optional: the
     * unparser enters each iteration of e.g. {@code (',' item)*} exactly when
     * there is an item to render, so the separator is present by construction.
     * Recorded optional-presence states cannot cover iterations that did not
     * exist when the input was parsed (appending to a one-element list used to
     * drop the new comma). Elements under a {@code ?} and literal-only
     * repetitions (no property to drive the iteration count) stay state-driven.
     */
    public static boolean isOptionalForFormatter(UPElement e) {
        if(!isEffectivelyOptional(e)) {
            return false;
        }
        if(e instanceof SubRule || e.ebnfOptional() || e.ebnfZeroMany()) {
            return true; // a block, or an element with its own ?/* suffix: state-driven
        }
        AlternativeBase alt = e.getParentAlt();
        while(alt != null && alt.getParentRule() instanceof UPElement) {
            if(!altContainsNamedElement(alt)) {
                // the terminal sits in a literal-only alternative: the unparser
                // cannot choose it by a property (it emits the literals of every
                // alternative and the recorded states filter), so stay
                // state-driven
                return true;
            }
            UPElement block = (UPElement) alt.getParentRule();
            if(block.ebnfOptional()) {
                return true; // nearest suffix is '?': a real optional
            }
            if(block.ebnfZeroMany() || block.ebnfOneMany()) {
                // nearest suffix is a repetition whose iterations and alternative
                // choice are driven by a property: present by construction
                return false;
            }
            alt = block.getParentAlt(); // plain grouping block: keep walking up
        }
        return true;
    }

    private static boolean altContainsNamedElement(AlternativeBase a) {
        for(UPElement el : a.getElements()) {
            if(el instanceof WithName) {
                return true;
            }
            if(el instanceof SubRule) {
                for(AlternativeBase inner : ((UPRuleBase) el).getAlternatives()) {
                    if(altContainsNamedElement(inner)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isEffectivelyOptional(UPElement e) {

        // indicates whether the elements in the specified alt are all unnamed
        final Predicate<AlternativeBase> onlyUnnamedElementsInAltPred = (a)->a.getElements().stream().filter(
                ne->ne instanceof UPNamedElement
                        || ne instanceof UPNamedSubRuleElement).count()==0;

        // indicates whether at least one sibling alt has only unnamed elements
        final Predicate<AlternativeBase> atLeastOneSiblingAltWithoutNamedElementsPred =
                (alt)-> alt.getParentRule().getAlternatives().
                        stream().filter(a->a!=alt).filter(onlyUnnamedElementsInAltPred).count()
                        > 0;

        // if e is a named element it is not optional
        boolean effectivelyOptionalNamed = e instanceof WithName;
        if(effectivelyOptionalNamed) return false;

        // ebnf suffix indicates e is optional
        // if true and not a sub-rule then we can skip further tests since e is definitely effectively optional
        boolean effectivelyOptionalViaEbnf = (e.ebnfOptional() || e.ebnfZeroMany());

        // ebnf suffix indicates that this element is optional
        if(effectivelyOptionalViaEbnf) {

            // if the element is a sub-rule we have to check alternatives for named elements since named elements
            // make sub-rules non-optional, even if the sub-rule element is marked as optional via ebnf
            if(e instanceof SubRule) {
                UPRuleBase sre = (UPRuleBase) e;

                for(AlternativeBase a: sre.getAlternatives()) {
                    boolean onlyUnnamedSiblingsInAlt = onlyUnnamedElementsInAltPred.test(a);

                    if(onlyUnnamedSiblingsInAlt) {
                        List<UPRuleBase> subRuleElements = a.getElements().stream().
                                filter(el->el instanceof SubRule).map(el->(UPRuleBase)el).collect(Collectors.toList());

                        for(UPRuleBase r : subRuleElements) {
                            if(!isEffectivelyOptional((UPElement) r)) {
                                return false;
                            }
                        }
                    }
                }

            } else {
                // no sub-rule
                return true;
            }
        }


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

    /**
     * Indicates whether the specified element is a block set, i.e. {@code ('a'|'b')}.
     * @param element element to check
     * @return {@code true} if this element is a block set; {@code false} otherwise
     */
    public static boolean isBlockSet(UPElement element) {

        if (!(element instanceof SubRule)) {
            return false;
        }

        UPRuleBase sre = (UPRuleBase) element;

        for(AlternativeBase a : sre.getAlternatives()) {

            if(a.getElements().size() > 1) {
                return false;
            }

            UPElement e = a.getElements().get(0);

            if(!e.ebnfOne()) return false;

            if(!e.isTerminal() && !e.isLexerRule()) {
                return false;
            }
        }

        return true;
    }
}
