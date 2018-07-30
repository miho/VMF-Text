grammar GrammarProgram;
program: functions+=function* EOF;

function:
    returnType=type name=IDENTIFIER '(' (arguments+=argument (',' arguments+=argument)*)? ')' '{'
        controlFlow=controlFlowScope
    '}'
    ;

invocation:
    name=IDENTIFIER '(' ')'              # methodInvocation
  | 'for' '(' condition=forCondition ')' '{'
     controlFlow=controlFlowScope
     '}'                                 # scopeInvocation
  ;

forCondition :
  decl = declaration
  ;

declaration:
  declType = type varName = IDENTIFIER '=' assignment=invocation;

controlFlowScope : (invocations+=invocation ';'?)*;


argument: name=IDENTIFIER;
type : name = IDENTIFIER;



// NEWLINE : [\r\n]+ ;
INT     : [0-9]+ ;
BINOP      : ('*'|'/'|'+'|'-');
PREFIXUNARYOP    : ('++'|'--'|'!');
POSTFIXUNARYOP    : ('++'|'--');

BOOL : 'true' | 'false';

//STRING: '"' CHAR* '"';

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
   : [ \r\n\t]+ -> channel(HIDDEN);
