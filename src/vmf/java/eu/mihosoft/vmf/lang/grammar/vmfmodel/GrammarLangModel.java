package eu.mihosoft.vmf.lang.grammar.vmfmodel;

import eu.mihosoft.vmf.core.*;


interface GrammarModel {
    @Contains(opposite = "model")
    RuleClass[] getRuleClasses();

    String getGrammarName();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.GetRootClassDelegate")
    boolean hasRootClass();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.GetRootClassDelegate")
    RuleClass rootClass();
}

@Immutable
interface CodeRange {
    int getStartIndex();
    int getStopIndex();
    int getCharPosInLine();
    int getLine();
}

@InterfaceOnly
interface LangElement {

}

@InterfaceOnly
interface CodeElement {
    CodeRange getCodeLocation();
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

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.TypeNameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.TypeNameDelegate")
    String asModelTypeName();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.TypeNameDelegate")
    String asJavaTypeNameNoCollections();

    String getPackageName();

    boolean isRuleType();
    boolean isArrayType();
}

interface RuleClass extends WithName, CodeElement {

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.NameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.NameDelegate")
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

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.NameDelegate")
    String nameWithUpper();

    @DelegateTo(className = "eu.mihosoft.vmf.lang.grammar.NameDelegate")
    String nameWithLower();

    Type getType();
}










