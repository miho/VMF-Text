package eu.mihosoft.vmf.vmftext.grammar.vmfmodel;

import eu.mihosoft.vmf.core.Container;
import eu.mihosoft.vmf.core.Contains;
import eu.mihosoft.vmf.core.DelegateTo;
import eu.mihosoft.vmf.core.InterfaceOnly;


@DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.unparser.RuleIndexUpdater")
interface UnparserModel {

    @Contains(opposite = "parent")
    UPRule[] getRules();
}

@InterfaceOnly
interface WithId {
    int getId();
}

@InterfaceOnly
@DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.unparser.AltIndexUpdater")
interface UPRuleBase extends WithText, WithId {
    @Contains(opposite = "parentRule")
    AlternativeBase[] getAlternatives();
}

interface UPRule extends WithName, UPRuleBase {
    @Container(opposite = "rules")
    UnparserModel getParent();
}

@InterfaceOnly
interface SubRule extends UPRuleBase {

}

@InterfaceOnly
@DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.unparser.SubRuleIndexUpdater")
interface AlternativeBase extends WithName, WithText, WithId {
    @Container(opposite = "alternatives")
    UPRuleBase getParentRule();

    @Contains(opposite = "parentAlt")
    UPElement[] getElements();
}

interface UPElement extends WithText {
    @Container(opposite = "elements")
    AlternativeBase getParentAlt();
}

interface UPNamedElement extends UPElement, WithName {

}

interface UPSubRuleElement extends UPElement, SubRule {

}

interface UPNamedSubRuleElement extends UPElement, SubRule, WithName {

}

interface Alternative extends AlternativeBase {

}

interface LabeledAlternative extends AlternativeBase, WithName {

}


