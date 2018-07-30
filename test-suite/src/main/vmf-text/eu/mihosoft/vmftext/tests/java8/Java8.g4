/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

//parser grammar JavaParser;

//options { tokenVocab=JavaLexer; }

grammar Java8;

compilationUnit
    : packageDecl=packageDeclaration? imports+=importDeclaration* typeDeclarations+=typeDeclaration* EOF
    ;

packageDeclaration
    : annotations+=annotation* PACKAGE packageName=qualifiedName ';'
    ;

importDeclaration
    : IMPORT importStatic=STATIC? packageOrClassName=qualifiedName starImport=DOT_STAR? ';'
    ;

DOT_STAR: '.' '*';

typeDeclaration
    : typeModifiers+=classOrInterfaceModifier*
      (classDecl=classDeclaration | enumDecl=enumDeclaration | interfaceDecl=interfaceDeclaration | annotationDecl=annotationTypeDeclaration)
    | ';'
    ;

modifier
    : typeModifier=classOrInterfaceModifier
    | nativeModifier=NATIVE
    | syncronizedModifier=SYNCHRONIZED
    | transientModifier=TRANSIENT
    | volatileModifier=VOLATILE
    ;

classOrInterfaceModifier
    : classAnnotation=annotation
    | publicModifier=PUBLIC
    | protectedModifier=PROTECTED
    | privateModifier=PRIVATE
    | staticModifier=STATIC
    | abstractModifier=ABSTRACT
    | finalModifier=FINAL    // FINAL for class only -- does not apply to interfaces
    | strictCTFPModifier=STRICTFP
    ;

variableModifier
    : finalModifier=FINAL
    | variableAnnotation=annotation
    ;

classDeclaration
    : CLASS className=IDENTIFIER typeParams=typeParameters?
      (EXTENDS extendsType=typeType)?
      (IMPLEMENTS implementsTypes=typeList)?
      body=classBody
    ;

typeParameters
    : '<' params+=typeParameter (',' params+=typeParameter)* '>'
    ;

typeParameter
    : annotations+=annotation* typeName=IDENTIFIER (EXTENDS boundType=typeBound)?
    ;

typeBound
    : types+=typeType ('&' types+=typeType)*
    ;

enumDeclaration
    : ENUM enumName=IDENTIFIER (IMPLEMENTS implementsTypes=typeList)? '{' constants=enumConstants? ','? enumDeclarations=enumBodyDeclarations? '}'
    ;

enumConstants
    : constants+=enumConstant (',' constants+=enumConstant)*
    ;

enumConstant
    : annotations+=annotation* name=IDENTIFIER args=arguments? body=classBody?
    ;

enumBodyDeclarations
    : ';' classBodyDeclarations+=classBodyDeclaration*
    ;

interfaceDeclaration
    : INTERFACE interfaceName=IDENTIFIER typeParams=typeParameters? (EXTENDS extendsTypes=typeList)? body=interfaceBody
    ;

classBody
    : '{' declarations+=classBodyDeclaration* '}'
    ;

interfaceBody
    : '{' declarations+=interfaceBodyDeclaration* '}'
    ;

classBodyDeclaration
    : ';'
    | staticModifier=STATIC? classBlock=block
    | modifiers+=modifier* declaration=memberDeclaration
    ;

memberDeclaration
    : methodDecl=methodDeclaration
    | genericMethodDecl=genericMethodDeclaration
    | fieldDecl=fieldDeclaration
    | constructorDecl=constructorDeclaration
    | genericConstructorDecl=genericConstructorDeclaration
    | interfaceDecl=interfaceDeclaration
    | annotationTypeDecl=annotationTypeDeclaration
    | classDecl=classDeclaration
    | enumDecl=enumDeclaration
    ;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration
    : type=typeTypeOrVoid methodName=IDENTIFIER params=formalParameterList arrayDims+=ARRAY_BRACKETS*
      (THROWS throwsExceptions=qualifiedNameList)?
      body=methodBody
    ;

methodBody
    : methodBlock=block
    | ';'
    ;

typeTypeOrVoid
    : type=typeType
    | voidType=VOID
    ;

genericMethodDeclaration
    : typeParams=typeParameters methodDecl=methodDeclaration
    ;

genericConstructorDeclaration
    : typeParams=typeParameters constructorDecls=constructorDeclaration
    ;

constructorDeclaration
    : constructorName=IDENTIFIER params=formalParameterList (THROWS throwsExceptions=qualifiedNameList)? constructorBody=block
    ;

fieldDeclaration
    : fieldType=typeType varDecls+=variableDeclarator (',' varDecls+=variableDeclarator)* ';'
    ;

interfaceBodyDeclaration
    : modifiers+=modifier* interfaceDecl=interfaceMemberDeclaration
    | ';'
    ;

interfaceMemberDeclaration
    : constDecl=constDeclaration
    | methodDecl=interfaceMethodDeclaration
    | genericMethodDecl=genericInterfaceMethodDeclaration
    | interfaceDecl=interfaceDeclaration
    | annotationDecl=annotationTypeDeclaration
    | classDecl=classDeclaration
    | enumDecl=enumDeclaration
    ;

constDeclaration
    : type=typeType constDecls+=constantDeclarator (',' constDecls+=constantDeclarator)* ';'
    ;

constantDeclarator
    : name=IDENTIFIER arrayDims+=ARRAY_BRACKETS* '=' variableInitializer
    ;

// see matching of [] comment in methodDeclaratorRest
// methodBody from Java8
interfaceMethodDeclaration
    : modifiers+=interfaceMethodModifier* (type=typeTypeOrVoid | typeParams=typeParameters annotations+=annotation* type=typeTypeOrVoid)
      methodName=IDENTIFIER params=formalParameterList arrayDims+=ARRAY_BRACKETS* (THROWS throwsExceptions=qualifiedNameList)? body=methodBody
    ;

// Java8
interfaceMethodModifier
    : interfaceAnnotation=annotation
    | publicModifier=PUBLIC
    | abstractModifier=ABSTRACT
    | defaultModifier=DEFAULT
    | staticModifier=STATIC
    | strictCTFPModifier=STRICTFP
    ;

genericInterfaceMethodDeclaration
    : typeParams=typeParameters methodDecls=interfaceMethodDeclaration
    ;

variableDeclarators
    : varDecls+=variableDeclarator (',' varDecls+=variableDeclarator)*
    ;

//variableDeclarator
//    : varDeclId=variableDeclaratorId ('=' varInitializer=variableInitializer)?
//    ;
//variableDeclaratorId
//    : varName=IDENTIFIER arrayDims+=ARRAY_BRACKETS*
//    ;

variableDeclarator
    : varName=IDENTIFIER                                                                   # variableDeclaratorWithoutInit
    | varName=IDENTIFIER ('=' initializer = expression)?                                   # variableDeclaratorWithExprInit
    | varName=IDENTIFIER arrayDims+=ARRAY_BRACKETS* ('=' initializer = arrayInitializer)?  # variableDeclaratorWithArrayInit
    ;

variableDeclaratorId
    : varName=IDENTIFIER arrayDims+=ARRAY_BRACKETS*
    ;

variableInitializer
    : initializer=arrayInitializer
    | varExpression=expression
    ;

arrayInitializer
    : '{' (varInitializers+=variableInitializer (',' varInitializers+=variableInitializer)* commaAfterLastElement=','? )? '}'
    ;

classOrInterfaceType
    : typeNameElements+=IDENTIFIER typeArgs+=typeArguments? ('.' typeNameElements+=IDENTIFIER typeArgs+=typeArguments?)*
    ;

typeArgument
    : type=typeType
    | '?' ((extendsType=EXTENDS | superType=SUPER) extendsOrSuperType=typeType)?
    ;

qualifiedNameList
    : names+=qualifiedName (',' names+=qualifiedName)*
    ;

formalParameterList
    : '(' (params+=formalParameter (',' params+=formalParameter)* (',' dotDotDotParam=lastFormalParameter)?)? ')'
    | '(' dotDotDotParam=lastFormalParameter ')'
    ;

formalParameter
    : modifiers+=variableModifier* type=typeType varDecl=variableDeclaratorId
    ;

lastFormalParameter
    : modifiers+=variableModifier* type=typeType '...' varDecl=variableDeclaratorId
    ;

qualifiedName
    : element+=IDENTIFIER ('.' element+=IDENTIFIER)*
    ;

/*
literal
    : intValue         = integerLiteral
    | floatValue       = floatLiteral
    | charValue        = CHAR_LITERAL
    | stringValue      = STRING_LITERAL
    | boolValue        = BOOL_LITERAL
    | nullValue        = NULL_LITERAL
    ;

integerLiteral
    : decimalValue     = DECIMAL_LITERAL
    | hexValue         = HEX_LITERAL
    | octValue         = OCT_LITERAL
    | ninaryValue      = BINARY_LITERAL
    ;

floatLiteral
    : floatValue       = FLOAT_LITERAL
    | hexValue         = HEX_FLOAT_LITERAL
    ;
*/

literal
    :
    (
      decimalValue     = DECIMAL_LITERAL
      | hexValue       = HEX_LITERAL
      | octValue       = OCT_LITERAL
      | binaryValue    = BINARY_LITERAL
    )                                       # integerLiteral

    |

    (
       floatValue       = FLOAT_LITERAL
       | hexValue       = HEX_FLOAT_LITERAL
    )                                       # floatLiteral

    | charValue        = CHAR_LITERAL       # charLiteral
    | stringValue      = STRING_LITERAL     # stringLiteral
    | boolValue        = BOOL_LITERAL       # booleanLiteral
    |                    NULL_LITERAL       # nullLiteral
    ;

// ANNOTATIONS

annotation
    : '@' name=qualifiedName ('(' ( elemValuePairs=elementValuePairs | elemValue=elementValue )? ')')?
    ;

elementValuePairs
    : elemPairs+=elementValuePair (',' elemPairs+=elementValuePair)*
    ;

elementValuePair
    : name=IDENTIFIER '=' value=elementValue
    ;

elementValue
    : expressionValue=expression
    | annotationValue=annotation
    | elemValueArrayInit=elementValueArrayInitializer
    ;

elementValueArrayInitializer
    : '{' (values+=elementValue (',' values+=elementValue)*)? commaAfterLastValue=','? '}'
    ;

annotationTypeDeclaration
    : '@' INTERFACE name=IDENTIFIER body=annotationTypeBody
    ;

annotationTypeBody
    : '{' (declarations+=annotationTypeElementDeclaration)* '}'
    ;

annotationTypeElementDeclaration
    : modifiers+=modifier* declaration=annotationTypeElementRest
    | ';' // this is not allowed by the grammar, but apparently allowed by the actual compiler
    ;

annotationTypeElementRest
    : type=typeType methodOrConstRest=annotationMethodOrConstantRest ';'
    | classDecl=classDeclaration ';'?
    | interfaceDecl=interfaceDeclaration ';'?
    | enumDecl=enumDeclaration ';'?
    | typeDecl=annotationTypeDeclaration ';'?
    ;

annotationMethodOrConstantRest
    : method=annotationMethodRest
    | constant=annotationConstantRest
    ;

annotationMethodRest
    : methodName=IDENTIFIER '(' ')' value=defaultValue?
    ;

annotationConstantRest
    : constDecl=variableDeclarators
    ;

defaultValue
    : DEFAULT value=elementValue
    ;

// STATEMENTS / BLOCKS

block
    : '{' statements+=blockStatement* '}'
    ;

blockStatement
    : varDecl=localVariableDeclaration ';'
    | wrappedStatement=statement
    | typeDecl=localTypeDeclaration
    ;

localVariableDeclaration
    : modifiers+=variableModifier* type=typeType varDecls=variableDeclarators
    ;

localTypeDeclaration
    : modifiers+=classOrInterfaceModifier*
      (classDecl=classDeclaration | interfaceDecl=interfaceDeclaration)
    | justSemicolon=';'
    ;

statement
    : blockLabel=block                                                                           # blockStmnt
    | ASSERT expressions+=expression (':' xpressions+=expression)? ';'                           # assertStmnt
    | IF check=parExpression ifStatement+=statement (ELSE elseStatement=statement)?              # ifElseStmnt
    | FOR '(' control=forControl ')' forStatement=statement                                      # forStmnt
    | WHILE control=parExpression whileStatement=statement                                       # whileStmnt
    | DO doStatement=statement WHILE control=parExpression ';'                                   # doWhileStmnt
    | TRY tryBlock=block (catchClauses+=catchClause+ finallyBlk=finallyBlock? | finallyBlk=finallyBlock)         # tryNoResourcesStmnt
    | TRY resourceSpec=resourceSpecification tryBlock=block catchClauses+=catchClause* finallyBlk=finallyBlock?  # tryWithResourcesStmnt
    | SWITCH check=parExpression '{' blockStatements+=switchBlockStatementGroup* switchLabels+=switchLabel* '}'  # switchStmnt
    | SYNCHRONIZED syncExpression=parExpression syncBlock=block                                                  # syncStmnt
    | RETURN returnValue=expression? ';'                                                                         # returnStmnt
    | THROW throwExpression=expression ';'                                                                       # throwStmnt
    | BREAK labelIdentifier=IDENTIFIER? ';'                                                                      # breakStmnt
    | CONTINUE labelIdentifier=IDENTIFIER? ';'                                                                   # continueStmnt
    | SEMI                                                                                                       # semicolonStmnt
    | statementExpression=expression ';'                                                                         # expressionStmnt
    | labelIdentifier=IDENTIFIER ':' labelStatement=statement                                                    # labelStmnt
    ;

catchClause
    : CATCH '(' modifiers+=variableModifier* type=catchType exceptionName=IDENTIFIER ')' catchBlock=block
    ;

catchType
    : elements+=qualifiedName ('|' elements+=qualifiedName)*
    ;

finallyBlock
    : FINALLY finallyBlk=block
    ;

resourceSpecification
    : '(' resSpec=resources ';'? ')'
    ;

resources
    : res+=resource (';' res+=resource)*
    ;

resource
    : modifiers+=variableModifier* type=classOrInterfaceType varDeclId=variableDeclaratorId '=' initExpression=expression
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : labels+=switchLabel+ statements+=blockStatement+
    ;

switchLabel
    : CASE (constantExpression=expression | enumConstantName=IDENTIFIER) ':'  # caseLabel
    | DEFAULT ':'                                                             # defaultLabel
    ;

forControl
    : modifiers+=variableModifier* type=typeType varDeclId=variableDeclaratorId ':' collectionExpression=expression    # forEachControl
    | init=forInit? ';' checkExpression=expression? ';' forUpdate=expressionList?                                      # simpleForControl
    ;

forInit
    : varDecl=localVariableDeclaration
    | expressions=expressionList
    ;


// EXPRESSIONS

parExpression
    : '(' inner=expression ')'
    ;

expressionList
    : elements+=expression (',' elements+=expression)*
    ;

methodCall
    : methodName=IDENTIFIER '(' args=expressionList? ')'
    ;

expression:
//    expr=primary                                                                                           # primaryExpression
// PRIMARY EXPR BEGIN
      '(' expr=expression ')'                                                           # paranExpr
    | THIS                                                                              # thisExpr
    | SUPER                                                                             # superExpr
    | lit=literal                                                                       # literalExpr
    | identifier=IDENTIFIER                                                             # identifierExpr
    | type=typeTypeOrVoid '.' CLASS                                                     # classExpr
    | typeArgs=nonWildcardTypeArguments (explicitGenericInvocSuffix=explicitGenericInvocationSuffix | THIS constructorArgs=arguments) # thisConstructorCallExpr
// PRIMARY EXPR END
    | firstExpr=expression bop='.'
      (name=IDENTIFIER
      | call=methodCall
      | thisExpr=THIS
      | NEW typeArgs=nonWildcardTypeArguments? inner=innerCreator
      | SUPER superSffx=superSuffix
      | invocation=explicitGenericInvocation
      )                                                                                                      # strangeCallExpr
    | arrayExpression=expression '[' arrayIndexExpression=expression ']'                                     # arrayExpr
    | call=methodCall                                                                                        # methodCallExpr
    | NEW constructor=creator                                                                                # instantiationExpr
    | '(' type=typeType ')' expressionToCast=expression                                                      # typeCastExpr
    | expr=expression postfix=('++' | '--')                                                                  # postFixOpExpr
    | prefix=('+'|'-'|'++'|'--') expr=expression                                                             # unaryIncDecOpExpr
    | prefix=('~'|'!') expr=expression                                                                       # xorOrNotOpExpr
    | left=expression bop=('*'|'/'|'%') right=expression                                                     # multDivModuloOpExpr
    | left=expression bop=('+'|'-') right=expression                                                         # plusMinusOpExpr
    | left=expression ('<' '<' | '>' '>' '>' | '>' '>') right=expression                                     # binaryShiftOpExpr
    | left=expression bop=('<=' | '>=' | '>' | '<') right=expression                                         # compareOpExpr
    | expr=expression bop=INSTANCEOF type=typeType                                                           # instanceofOpExpr
    | left=expression bop=('==' | '!=') right=expression                                                     # equalsNotEqualsOpExpr
    | left=expression bop='&' right=expression                                                               # binaryAndOpExpr
    | left=expression bop='^' right=expression                                                               # binaryXorOpExpr
    | left=expression bop='|' right=expression                                                               # binaryOrOpExpr
    | left=expression bop='&&' right=expression                                                              # booleanAndOpExpr
    | left=expression bop='||' right=expression                                                              # booleanOrOpExpr
    | left=expression bop='?' trueExpr=expression ':' falseExpr=expression                                   # ternaryOpExpr
    | <assoc=right> assignTo=expression
      bop=('=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '>>=' | '>>>=' | '<<=' | '%=')
      assignExpr=expression                                                                                  # assignOpExpr
    | expr=lambdaExpression /*Java8*/                                                                        # lambdaExpr

    // Java 8 methodReference
    | methodRefExpr=expression '::' typeArgs=typeArguments? methodName=IDENTIFIER                            # methodRefExpr
    | type=typeType '::' (typeArgs=typeArguments? methodName=IDENTIFIER | NEW)                               # methodRefExpr
    | clsType=classType '::' typeArgs=typeArguments? NEW                                                     # methodRefExpr
    ;

// Java8
lambdaExpression
    : params=lambdaParameters '->' body=lambdaBody
    ;

// Java8
lambdaParameters
    : name=IDENTIFIER                                               # simpleIdentifierLambdaParams
    | paramList=formalParameterList?                                # formalParamListLambdaParams
    | '(' names+=IDENTIFIER (',' names+=IDENTIFIER)* ')'            # simpleIdentifierListLambdaParams
    ;

// Java8
lambdaBody
    : lambdaExpr=expression
    | lambdaBlock=block
    ;

/*
primary
    : '(' expr=expression ')'                                                           # paranExpr
    | THIS                                                                              # thisExpr
    | SUPER                                                                             # superExpr
    | lit=literal                                                                       # literalExpr
    | identifier=IDENTIFIER                                                             # identifierExpr
    | type=typeTypeOrVoid '.' CLASS                                                     # classExpr
    | typeArgs=nonWildcardTypeArguments (explicitGenericInvocSuffix=explicitGenericInvocationSuffix | THIS constructorArgs=arguments) # thisConstructorCallExpr
    ;
*/

classType
    : (type=classOrInterfaceType '.')? annotations+=annotation* className=IDENTIFIER typeArgs=typeArguments?
    ;

creator
    : typeArgs=nonWildcardTypeArguments name=createdName rest=classCreatorRest
    | name=createdName (arrayRest=arrayCreatorRest | rest=classCreatorRest)
    ;

createdName
    : nameElements+=IDENTIFIER typeArgs+=typeArgumentsOrDiamond? ('.' nameElements+=IDENTIFIER typeArgs+=typeArgumentsOrDiamond?)*
    | type=primitiveType
    ;

innerCreator
    : name=IDENTIFIER typeArgs=nonWildcardTypeArgumentsOrDiamond? rest=classCreatorRest
    ;

arrayCreatorRest
    : '[' (']' ('[' ']')* initializer=arrayInitializer | expressions+=expression ']' ('[' expressions+=expression ']')* ('[' ']')*)
    ;

classCreatorRest
    : args=arguments body=classBody?
    ;

explicitGenericInvocation
    : typeArgs=nonWildcardTypeArguments genericInvocationSuffix=explicitGenericInvocationSuffix
    ;

typeArgumentsOrDiamond
    : '<' '>'                     # diamondTypeArgs
    | typeArgs=typeArguments      # typeArgs
    ;

nonWildcardTypeArgumentsOrDiamond
    : '<' '>'                              # diamondNonWildCardTypeArgs
    | typeArgs=nonWildcardTypeArguments    # nonWildCardTypeArgs
    ;

nonWildcardTypeArguments
    : '<' types=typeList '>'
    ;

typeList
    : types+=typeType (',' types+=typeType)*
    ;

typeType
    : ann=annotation? (objectType=classOrInterfaceType | simpleType=primitiveType) arrayDims+=ARRAY_BRACKETS*
    ;

primitiveType
    : BOOLEAN      # booleanType
    | CHAR         # charType
    | BYTE         # byteType
    | SHORT        # shortType
    | INT          # intType
    | LONG         # longType
    | FLOAT        # floatType
    | DOUBLE       # doubleType
    ;

typeArguments
    : '<' typeArgs+=typeArgument (',' typeArgs+=typeArgument)* '>'
    ;

superSuffix
    : args=arguments
    | '.' name=IDENTIFIER args=arguments?
    ;

explicitGenericInvocationSuffix
    : SUPER superSuffx=superSuffix
    | name=IDENTIFIER args=arguments
    ;

arguments
    : '(' args=expressionList? ')'
    ;


// added by miho: 18.01.2018

ARRAY_BRACKETS: '[' ']';


/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

//lexer grammar JavaLexer;

// Keywords

ABSTRACT:           'abstract';
ASSERT:             'assert';
BOOLEAN:            'boolean';
BREAK:              'break';
BYTE:               'byte';
CASE:               'case';
CATCH:              'catch';
CHAR:               'char';
CLASS:              'class';
CONST:              'const';
CONTINUE:           'continue';
DEFAULT:            'default';
DO:                 'do';
DOUBLE:             'double';
ELSE:               'else';
ENUM:               'enum';
EXTENDS:            'extends';
FINAL:              'final';
FINALLY:            'finally';
FLOAT:              'float';
FOR:                'for';
IF:                 'if';
GOTO:               'goto';
IMPLEMENTS:         'implements';
IMPORT:             'import';
INSTANCEOF:         'instanceof';
INT:                'int';
INTERFACE:          'interface';
LONG:               'long';
NATIVE:             'native';
NEW:                'new';
PACKAGE:            'package';
PRIVATE:            'private';
PROTECTED:          'protected';
PUBLIC:             'public';
RETURN:             'return';
SHORT:              'short';
STATIC:             'static';
STRICTFP:           'strictfp';
SUPER:              'super';
SWITCH:             'switch';
SYNCHRONIZED:       'synchronized';
THIS:               'this';
THROW:              'throw';
THROWS:             'throws';
TRANSIENT:          'transient';
TRY:                'try';
VOID:               'void';
VOLATILE:           'volatile';
WHILE:              'while';

// Literals

DECIMAL_LITERAL:    ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
HEX_LITERAL:        '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL:        '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL:     '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:      (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
             |       Digits (ExponentPart [fFdD]? | [fFdD])
             ;

HEX_FLOAT_LITERAL:  '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL:       'true'
            |       'false'
            ;

CHAR_LITERAL:       '\'' (~['\\\r\n] | EscapeSequence) '\'';

STRING_LITERAL:     '"' (~["\\\r\n] | EscapeSequence)* '"';

NULL_LITERAL:       'null';

// Separators

LPAREN:             '(';
RPAREN:             ')';
LBRACE:             '{';
RBRACE:             '}';
LBRACK:             '[';
RBRACK:             ']';
SEMI:               ';';
COMMA:              ',';
DOT:                '.';

// Operators

ASSIGN:             '=';
GT:                 '>';
LT:                 '<';
BANG:               '!';
TILDE:              '~';
QUESTION:           '?';
COLON:              ':';
EQUAL:              '==';
LE:                 '<=';
GE:                 '>=';
NOTEQUAL:           '!=';
AND:                '&&';
OR:                 '||';
INC:                '++';
DEC:                '--';
ADD:                '+';
SUB:                '-';
MUL:                '*';
DIV:                '/';
BITAND:             '&';
BITOR:              '|';
CARET:              '^';
MOD:                '%';

ADD_ASSIGN:         '+=';
SUB_ASSIGN:         '-=';
MUL_ASSIGN:         '*=';
DIV_ASSIGN:         '/=';
AND_ASSIGN:         '&=';
OR_ASSIGN:          '|=';
XOR_ASSIGN:         '^=';
MOD_ASSIGN:         '%=';
LSHIFT_ASSIGN:      '<<=';
RSHIFT_ASSIGN:      '>>=';
URSHIFT_ASSIGN:     '>>>=';

// Java 8 tokens

ARROW:              '->';
COLONCOLON:         '::';

// Additional symbols not defined in the lexical specification

AT:                 '@';
ELLIPSIS:           '...';

// Whitespace and comments

WS:                 [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT:            '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT:       '//' ~[\r\n]*    -> channel(HIDDEN);

// Identifiers

IDENTIFIER:         Letter LetterOrDigit*;

// Fragment rules

fragment ExponentPart
    : [eE] [+-]? Digits
    ;

fragment EscapeSequence
    : '\\' [btnfr"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigits
    : HexDigit ((HexDigit | '_')* HexDigit)?
    ;

fragment HexDigit
    : [0-9a-fA-F]
    ;

fragment Digits
    : [0-9] ([0-9_]* [0-9])?
    ;

fragment LetterOrDigit
    : Letter
    | [0-9]
    ;

fragment Letter
    : [a-zA-Z$_] // these are the "java letters" below 0x7F
    | ~[\u0000-\u007F\uD800-\uDBFF] // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
    ;



/*<!vmf-text!>

TypeMap() {
  HEX_FLOAT_LITERAL    -> java.lang.Double via (
    'java.lang.Double.parseDouble(entry.getText())',
    'java.lang.Double.toHexString(entry)'
  )
  FLOAT_LITERAL -> java.lang.Double  via 'java.lang.Double.parseDouble(entry.getText())'

  (rule: VOID    -> type: java.lang.Boolean) = {
      toType:   '"void".equals(entry.getText())',
      toString: '"void"',
      default:  'false'
  }
}

*/


/*<!vmf-text!>

interface PackageDeclaration {

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java8.PackageDeclarationDelegate")
    void defPackageNameFromString(String packageName);

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java8.PackageDeclarationDelegate")
    String packageNameAsString();

}

interface MethodDeclaration {

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java8.MethodDeclarationDelegate")
    boolean returnsVoid();

}


*/