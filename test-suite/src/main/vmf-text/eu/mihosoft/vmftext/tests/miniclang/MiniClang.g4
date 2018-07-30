grammar MiniClang;

program: header+=persistentComment*
('#include' LT 'stdio.h' GT )?
('#include' LT 'stdbool.h' GT )?
('#include' LT 'stdint.h' GT )?
includes+=include* constants+=constantDef* (forwardDeclarations+=forwardDecl|functions+=functionDecl)*
mainFunction=mainFunctionDecl footer+=persistentComment* EOF;

include : comments+=persistentComment* '#include' LT fileName=ID('.h'|'.H')? GT;

constantDef: comments+=persistentComment* '#define'? varName=ID value=INT
        ;

mainFunctionDecl:
         comments+=persistentComment*
           'int' 'main' LP ('int' 'argc' ',' 'char' '*' 'argv' LSB RSB)? RP '{' statements+=statement* '}'
         ;

forwardDecl:
         comments+=persistentComment*
         returnType=type functionName=ID LP (params+=parameter (',' params+=parameter)*)? RP ';'
         ;

functionDecl:
         comments+=persistentComment*
         returnType=type functionName=ID LP (params+=parameter (',' params+=parameter)*)? RP '{' statements+=statement* '}'
         ;

statement
:	'{' statements+=statement* '}'                                                                            # blockStatement
|	'if' LP condition=expression RP ifBlock=statement ('else' elseBlock=statement)?                           # ifElseStatement
|	'while' LP check=expression RP block=statement                                                            # whileStatement
|	'for' LP init=expression ';' check=expression ';' inc=expression RP block=statement                       # forStatement
|	'printf' LP printExpression=expression (',' valueExpressions+=expression)* RP ';'                         # printStatement
|   declType=type varName=ID (LSB arraySizes+=(INT|ID) RSB)+ ';'                                              # arrayDeclStatement
|	declType=type? varName=ID ASS assignmentExpression=expression ';'                                         # variableAssignmentStatement
|   declType=type varName=ID ';'                                                                              # varDeclStatement
|	varName=ID (LSB arrayIndices+=expression RSB)+ ASS assignmentExpression=expression ';'                    # arrayAssignmentStatement
|   RETURN returnValue=expression ';'                                                                         # returnStatement
|   functionName=ID LP ( args+=expression ( ',' args+=expression )* )? RP ';'                                 # functionCallStatement
|   comment=persistentComment                                                                                 # commentStatement
;


expression:
    arrayVariableExpression=expression (LSB arrayIndices+=expression RSB)+                                    # arrayAccessExpression
|   functionName=ID LP ( args+=expression ( ',' args+=expression )* )? RP                                     # functionCallExpression
|   NOT operatorExpression=expression                                                                         # notExpression
|   '&' operatorExpression=expression                                                                         # addressOperator
|   '*' operatorExpression=expression                                                                         # dereferenceOperator
|   LP castType=type RP operatorExpression=expression                                                         # castOperatorExpression
|   left=expression TIMES right=expression                                                                    # multExpression
|   left=expression DIV right=expression                                                                      # divExpression
|   left=expression PLUS right=expression                                                                     # addExpression
|   left=expression MINUS right=expression                                                                    # subExpression
|   left=expression LT right=expression                                                                       # ltExpression
|   left=expression AND right=expression                                                                      # andExpression
|   left=expression EQ right=expression                                                                       # equalExpression
|   left=expression NEQ right=expression                                                                      # nonEqualExpression
|   left=expression LTEQ right=expression                                                                     # ltEqualExpression
|   left=expression GTEQ right=expression                                                                     # gtEqualExpression
|   declType=type? varName=ID ASS assignment=expression                                                       # assignmentExpression
|   varName=ID '+=' assignment=expression                                                                     # assignmentPlusExpression
|   varName=ID '-=' assignment=expression                                                                     # assignmentMinusExpression
|   varName=ID '++'                                                                                           # incPostExpression
|   varName=ID '--'                                                                                           # decPostExpression
|   '++' varName=ID                                                                                           # incPreExpression
|   '--' varName=ID                                                                                           # decPreExpression
|   varName=ID                                                                                                # identifierExpression
|   value=INT                                                                                                 # intExpression
|   value=DOUBLE                                                                                              # doubleExpression
|   value=BOOL                                                                                                # booleanExpression
|   value=STRING                                                                                              # stringExpression
|   LP paranExpr=expression RP                                                                                # parenExpression
;

parameter
:   declType=type pointer='*'? varName=ID (LSB arraySizes+=(INT|ID) RSB)*
;


type
:	typeName='int'
|   typeName='uintptr_t'
|   typeName='void'
|   typeName='double'
|	typeName='bool'
|   typeName='const char*'
;


persistentComment:  text = PERSISTENT_LINE_COMMENT;

intLiteral : value=INT ;

AND:'&&';
LT:'<';
GT:'>';
PLUS:'+';
MINUS:'-';
TIMES:'*';
DIV:'/';
NOT:'!';
LSB:'[';
RSB:']';
LP:'(';
RP:')';
RETURN: 'return';
ASS: '=';
EQ: '==';
NEQ: '!=';
LTEQ: '<=';
GTEQ: '>=';


BOOL
:	'true'
|	'false'
;


DOUBLE :
         DIGIT+ DOT DIGIT*
       | DOT DIGIT+
       | DIGIT+ DOT
       ;

fragment SIGN :'-'|'+' ;
fragment DIGIT : [0-9];
fragment DOT : '.' ;

STRING: '"' (~["\\\r\n] | ESCAPE_SEQUENCE)* '"';

fragment ESCAPE_SEQUENCE
: '\\' [btnfr"\\]
;


INT   : [0-9]+ ;

ID: [a-zA-Z][a-zA-Z0-9_]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

PERSISTENT_LINE_COMMENT
    	:  '//' .*? '\n'
    	;

MULTILINE_COMMENT
	:  '/*' .*? '*/' -> channel(HIDDEN)
	;


/*<!vmf-text!>

TypeMap() {
  (rule: INT    -> type: java.lang.Integer) = {
      toType:   'java.lang.Integer.parseInt(entry.getText())',
      toString: 'entry.toString()'
  }

  (DOUBLE  -> java.lang.Double) = (
      toType:   'java.lang.Double.parseDouble(entry.getText())',
      toString: 'entry.toString()'
  )

  (BOOL   -> java.lang.Boolean ) = (
      toType:   'java.lang.Boolean.parseBoolean(entry.getText())',
      toString: 'entry.toString()'
  )

  (PERSISTENT_LINE_COMMENT -> java.lang.String) = (
      toType:   'entry.getText()',
      toString: 'entry.trim().startsWith("//")?entry:"//"+entry'
  )

  (STRING -> java.lang.String) = {
      toType:   'entry.getText()',
      toString: 'entry.trim().startsWith("\"")?entry:"\""+entry+"\""'
  }
}

*/

/*<!vmf-text!>

interface Program {
    @DelegateTo(className="eu.mihosoft.vmftext.tests.miniclang.ProgramDelegate")
    Optional<FunctionDecl> resolveFunction(String fcn, int numArgs);
}

@InterfaceOnly
interface ControlFlowChildNode {
    @DelegateTo(className="eu.mihosoft.vmftext.tests.miniclang.ControlFlowChildNodeDelegate")
    ControlFlowScope[] parentScopes();
}

@InterfaceOnly
interface WithId extends CodeElement {
    int getId();
}

interface Expression extends WithId, ControlFlowChildNode {

    // Expression getParent();

}

@InterfaceOnly
interface DeclStatement extends WithVarName {
  Type getDeclType();
  String getVarName();
  @GetterOnly
  String[] getArraySizes();
}

@InterfaceOnly
interface BinaryOperator extends Expression {
    Expression getLeft();
    Expression getRight();
}

interface AssignmentExpression extends DeclStatement {}

interface DivExpression extends BinaryOperator {}
interface AddExpression extends BinaryOperator {}
interface SubExpression extends BinaryOperator {}
interface LtExpression extends BinaryOperator {}
interface AndExpression extends BinaryOperator {}
interface EqualExpression extends BinaryOperator {}
interface NonEqualExpression extends BinaryOperator {}
interface LtEqualExpression extends BinaryOperator {}
interface GtEqualExpression extends BinaryOperator {}


@InterfaceOnly
interface ConstExpression {

    @GetterOnly
    Object getValue();

}

interface IntExpression extends ConstExpression {}
interface DoubleExpression extends ConstExpression {}
interface BooleanExpression extends ConstExpression {}
interface StringExpression extends ConstExpression {}


interface Statement extends WithId, ControlFlowChildNode {}

@InterfaceOnly
interface WithVarName {
    String getVarName();
}

@InterfaceOnly
interface ControlFlowScope extends WithId, ControlFlowChildNode {
    Statement[] getStatements();

    @DelegateTo(className="eu.mihosoft.vmftext.tests.miniclang.ControlFlowDelegate")
    Optional<DeclStatement> resolveVariable(String name);
}

@InterfaceOnly
interface ControlFlowContainer extends WithId {

}


interface ForStatement extends ControlFlowContainer {}
interface WhileStatement extends ControlFlowContainer {}
interface IfElseStatement extends ControlFlowContainer {}

interface BlockStatement extends ControlFlowScope {}

interface ConstantDef extends WithVarName, DeclStatement {}

interface ArrayDeclStatement extends WithVarName, DeclStatement {}
interface VariableAssignmentStatement extends WithVarName, DeclStatement {}
interface VarDeclStatement extends WithVarName, DeclStatement {}
interface ArrayAssignmentStatement extends WithVarName, DeclStatement {}
interface AssignmentExpression extends WithVarName {}
interface AssignmentPlusExpression extends WithVarName {}
interface IncPostExpression extends WithVarName {}
interface DecPostExpression extends WithVarName {}
interface IncPreExpression extends WithVarName {}
interface DecPreExpression extends WithVarName {}
interface IdentifierExpression extends WithVarName {}
interface Parameter extends WithVarName {}


@InterfaceOnly
interface WithFunctionName {
    String getFunctionName();
}

interface ForwardDecl extends WithFunctionName {}
interface FunctionDecl extends WithFunctionName, ControlFlowScope {}
interface MainFunctionDecl extends WithFunctionName, ControlFlowScope {}
interface FunctionCallExpression extends WithFunctionName {}
interface FunctionCallStatement extends WithFunctionName {}



@InterfaceOnly
interface WithArraySizes {
    String[] getArraySizes();
}

interface ArrayDeclStatement extends WithArraySizes {}

interface Parameter extends WithArraySizes, WithId, DeclStatement {}

*/