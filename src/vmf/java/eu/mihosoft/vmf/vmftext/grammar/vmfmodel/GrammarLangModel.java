package eu.mihosoft.vmf.vmftext.grammar.vmfmodel;

import eu.mihosoft.vmf.core.*;

import java.util.Optional;


@DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.CheckRulesDelegate")
interface GrammarModel {
    @Contains(opposite = "model")
    RuleClass[] getRuleClasses();

    String getGrammarName();

    String getPackageName();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.GetRootClassDelegate")
    boolean hasRootClass();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.GetRootClassDelegate")
    RuleClass rootClass();

    @Contains(opposite = "model")
    TypeMappings getTypeMappings();
}

@Immutable
interface CodeRange {
    CodeLocation getStart();
    CodeLocation getStop();
}

@Immutable
interface CodeLocation {
    int getIndex();
    int getLine();
    int getCharPosInLine();
}

@InterfaceOnly
interface LangElement {

}

@InterfaceOnly
interface CodeElement {
    CodeRange getCodeRange();
}

@InterfaceOnly
interface WithType extends LangElement {
    Type getType();
}

@InterfaceOnly
interface WithName extends LangElement {
    String getName();


// TODO find out why Velocity does not look for methods in super interface
//    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.NameDelegate")
//    String nameWithUpper();
}

@Immutable
interface Type extends LangElement, WithName {

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.TypeNameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.TypeNameDelegate")
    String asModelTypeName();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.TypeNameDelegate")
    String asJavaTypeNameNoCollections();

    String getPackageName();

    String getAntlrRuleName();

    boolean isRuleType();

    boolean isArrayType();
}

@DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.CheckPropertiesDelegate")
interface RuleClass extends WithName, CodeElement {

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.NameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.NameDelegate")
    String nameWithLower();

    @Contains(opposite = "ruleClasses")
    GrammarModel getModel();

    @Contains(opposite = "parent")
    Property[] getProperties();

    @Container(opposite = "childClasses")
    RuleClass getSuperClass();

    @Contains(opposite = "superClass")
    RuleClass[] getChildClasses();

    boolean isRoot();
}

interface Property extends WithName, WithType, CodeElement {
    @Container(opposite = "properties")
    RuleClass getParent();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.NameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.NameDelegate")
    String nameWithLower();

    Type getType();
}


// TypeMapping

interface TypeMappings {
    @Contains(opposite = "parent")
    TypeMapping[] getTypeMappings();

    @Container(opposite = "typeMappings")
    GrammarModel getModel();
}

interface TypeMapping {

    @Container(opposite = "typeMappings")
    TypeMappings getParent();

    @Contains(opposite = "parent")
    Mapping[] getEntries();

    @DelegateTo(className = "eu.mihosoft.vmf.vmftext.grammar.TypeMappingLookup")
    public Optional<Mapping> mappingByRuleName(String name);

    String[] getApplyToNames();


}

interface Mapping {
    @Container(opposite="entries")
    TypeMapping getParent();

    String getRuleName();

    String getTypeName();

    String getMappingCode();
}










