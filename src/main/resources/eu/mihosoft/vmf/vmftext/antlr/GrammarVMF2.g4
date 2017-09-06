grammar GrammarVMF2;


/**

interface LangElement {

}


@InterfaceOnly
interface Scope extends LangElement {

}

@InterfaceOnly
interface ControlFlowScope extends Scope {
    @Contains(opposite="parentClass")
    Invocation[] getInvocations();
}

interface Class extends Scope {

    @Contained(opposite="enclosedClass")
    Class getEnclosingClass();

    @Contains(opposite="enclosingClass")
    Class[] getEnclosedClasses();

}

interface Invocation extends LangElement {
    @Contained(opposite="invocations")
    ControlFlowScope getParentScope();
}
interface ScopeInvocation extends Invocation {
    ControlFlowScope getControlFlow();
}

*/

program: classes += clazz*;

clazz: 'class' name=IDENTIFIER '{'

    (classes+=clazz|methodDeclarations+=methodDeclaration)*

'}';

methodDeclaration: 'method' name=IDENTIFIER '(' ')' '{'
controlFlow=controlFlowScope
'}';

invocation:
    name=IDENTIFIER '(' ')'              # methodInvocation
  | name=IDENTIFIER '(' ')' + '{'
     controlFlow=controlFlowScope
     '}'                                 # scopeInvocation
  ;

controlFlowScope : (invocations+=invocation)*;


/**
mappings:
INT             -> int
BOOL            -> boolean
BINOP           -> Operator
PREFIXUNARYOP   -> Operator
POSTFIXUNARYOP  -> Operator
*/

// NEWLINE : [\r\n]+ ;
INT     : [0-9]+ ;
BINOP      : ('*'|'/'|'+'|'-');
PREFIXUNARYOP    : ('++'|'--'|'!');
POSTFIXUNARYOP    : ('++'|'--');

BOOL : 'true' | 'false';

//STRING: '"' CHAR* '"';

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;


WhiteSpace
   : [ \r\n\t]+ -> skip;