grammar MiniJava;

/*<!vmf-text!>

TypeMap() {
  IntegerLiteral    -> java.lang.Integer via (
      'java.lang.Integer.parseInt(entry.getText())',
      'entry.toString()'
  )
  // DOUBLE -> java.lang.Double  via 'java.lang.Double.parseDouble(entry.getText())'
  BooleanLiteral    -> java.lang.Boolean via (
      'java.lang.Boolean.parseBoolean(entry.getText())',
      'entry.toString()'
  )

  ARRAYTYPE    -> java.lang.Boolean via (
      'eu.mihosoft.vmftext.tests.minijava.TypeConverter.stringToType(entry.getText())',
      'eu.mihosoft.vmftext.tests.minijava.TypeConverter.typeToString(entry)'
  )

}

*/

program
:	mainClassDecl=mainClass classes+=classDeclaration* EOF
;

mainClass
:	'class' name=Identifier '{' /*<!vmft-format!> inc(2); linebreak; */ 'public' 'static' 'void' 'main' '(' 'String' ('[' ']'|'[]') args=Identifier ')' '{' /*<!vmft-format!> inc(2); linebreak; */ block = statement? '}' /*<!vmft-format!> dec(2); linebreak;*/ '}' /*<!vmft-format!> dec(2); linebreak;*/;

classDeclaration
:	'class' name=Identifier ( 'extends' extend+=Identifier (',' extend+=Identifier )* )? '{'/*<!vmft-format!> inc(2);linebreak;*/ varDeclarations+=fieldDeclaration* methodDeclarations+=methodDeclaration* '}'/*<!vmft-format!> dec(2); linebreak;*/;

fieldDeclaration
:	decl=varDeclaration ;

localDeclaration
:	decl=varDeclaration ;

varDeclaration
:	varType=type name=Identifier ';' /*<!vmft-format!> linebreak;*/;

methodDeclaration
:	'public' returnType=type name=Identifier '(' args=parameterList? ')' '{'/*<!vmft-format!> inc(2); linebreak; */ body=methodBody '}'/*<!vmft-format!> dec(2); linebreak; */;

parameterList
:   params+=parameter (',' params+=parameter)*
;

parameter
:   paramType=type name=Identifier
;

methodBody
:	varDeclarations+=localDeclaration* block+=statement* RETURN returnExpr=expression ';'
;

type
:	typeName='int' arrayType = ARRAYTYPE?
|	typeName='boolean'
|	typeName='int'
|	typeName=Identifier
;

statement
:	'{'/*<!vmft-format!> inc(2);linebreak;*/ statements+=statement* '}'/*<!vmft-format!> dec(2);linebreak;*/
#blockStatement
|	'if' LP condition=expression RP ifBlock=statement 'else' elseBlock=statement
#ifElseStatement
|	'while' LP checkExpr=expression RP whileBlock=statement
#whileStatement
|	'System.out.println' LP  printExpression=expression RP ';' /*<!vmft-format!>linebreak;*/
#printStatement
|	varName=Identifier EQ assignmentExpression=expression ';' /*<!vmft-format!>linebreak;*/
#variableAssignmentStatement
|	varName=Identifier LSB arrayIndexExpression=expression RSB EQ assignmentExpression=expression ';' /*<!vmft-format!>linebreak;*/
#arrayAssignmentStatement
;

expression
:   arrayVariableExpression=expression LSB arrayIndexExpression=expression RSB
# arrayAccessExpression

|   arrayVariableExpression=expression DOTLENGTH
# arrayLengthExpression

|   methodObjectExpression=expression '.' methodName=Identifier '(' ( args+=expression ( ',' args+=expression )* )? ')'
# methodCallExpression

|   NOT operatorExpression=expression
# notExpression

|   'new' 'int' LSB arraySizeExpression=expression RSB
# arrayInstantiationExpression

|   'new' className=Identifier '(' ')'
# objectInstantiationExpression

|	left=expression POWER right=expression
# powExpression

|   left=expression TIMES right=expression
# mulExpression

|   left=expression PLUS right=expression
# addExpression

|   left=expression MINUS right=expression
# subExpression

|   left=expression LT right=expression
# ltExpression

|   left=expression AND right=expression
# andExpression

|   value=IntegerLiteral
# intLitExpression

|   value=BooleanLiteral
# booleanLitExpression

|   name=Identifier
# identifierExpression

|   'this'
# thisExpression

|   '(' paranExpr=expression ')'
# parenExpression
;

ARRAYTYPE:'[]';

AND:'&&';
LT:'<';
PLUS:'+';
MINUS:'-';
TIMES:'*';
POWER:'**';
NOT:'!';
LSB:'[';
RSB:']';
DOTLENGTH:'.length';
LP:'(';
RP:')';
RETURN: 'return';
EQ: '=';

BooleanLiteral
:	'true'
|	'false'
;

Identifier
:	JavaLetter JavaLetterOrDigit*
;

fragment
JavaLetter
:	[a-zA-Z$_] // these are the 'java letters' below 0xFF
;

fragment
JavaLetterOrDigit
:	[a-zA-Z0-9$_] // these are the 'java letters or digits' below 0xFF
;

IntegerLiteral
:	DecimalIntegerLiteral
;

fragment
DecimalIntegerLiteral
:	DecimalNumeral IntegertypeSuffix?
;

fragment
IntegertypeSuffix
:	[lL]
;

fragment
DecimalNumeral
	:	'0'
|	NonZeroDigit (Digits? | Underscores Digits)
	;

	fragment
	Digits
	:	Digit (DigitsAndUnderscores? Digit)?
	;

	fragment
	Digit
	:	'0'
	|	NonZeroDigit
	;

	fragment
	NonZeroDigit
	:	[1-9]
	;

	fragment
	DigitsAndUnderscores
	:	DigitOrUnderscore+
	;

	fragment
	DigitOrUnderscore
	:	Digit
	|	'_'
	;

	fragment
	Underscores
	:	'_'+
	;

	WS
	:   [ \r\t\n]+ -> skip
	;

	MULTILINE_COMMENT
	:  '/*' .*? '*/' -> skip
	;
	LINE_COMMENT
	:  '//' .*? '\n' -> skip
;