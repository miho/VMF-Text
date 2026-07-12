/* Single-file Java24 grammar merged from ANTLR grammars-v4 + VMF-Text labels from Java8.g4 */

/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 Copyright (c) 2021 Michał Lorek (upgrade to Java 11)
 Copyright (c) 2022 Michał Lorek (upgrade to Java 17)
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

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging
grammar Java24;
options {
    superClass = JavaParserBase;
}

compilationUnit:packageDecl=packageDeclaration? (imports+=importDeclaration | ';')* (typeDeclarations+=typeDeclaration | ';')* EOF
    | modularCompulationUnit EOF
    ;

modularCompulationUnit
    : imports+=importDeclaration* moduleDecl=moduleDeclaration
    ;

packageDeclaration
    : annotations+=annotation* PACKAGE packageName=qualifiedName ';'
    ;

importDeclaration
    : IMPORT importStatic=STATIC? packageOrClassName=qualifiedName starImport=DOT_STAR? ';'
    ;

typeDeclaration:typeModifiers+=classOrInterfaceModifier* (
        classDecl=classDeclaration
        | enumDecl=enumDeclaration
        | interfaceDecl=interfaceDeclaration
        | annotationDecl=annotationTypeDeclaration
        | recordDecl=recordDeclaration
    );

modifier:typeModifier=classOrInterfaceModifier
    | nativeModifier=NATIVE
    | syncronizedModifier=SYNCHRONIZED
    | transientModifier=TRANSIENT
    | volatileModifier=VOLATILE;

classOrInterfaceModifier:classAnnotation=annotation
    | publicModifier=PUBLIC
    | protectedModifier=PROTECTED
    | privateModifier=PRIVATE
    | staticModifier=STATIC
    | abstractModifier=ABSTRACT
    | finalModifier=FINAL // FINAL for class only -- does not apply to interfaces
    | strictCTFPModifier=STRICTFP
    | sealedModifier=SEALED
    | nonSealedModifier=NON_SEALED;

variableModifier:finalModifier=FINAL
    | variableAnnotation=annotation;

classDeclaration
    : CLASS className=identifier typeParams=typeParameters? (EXTENDS extendsType=typeType)? (IMPLEMENTS implementsTypes=typeList)? (
        PERMITS permitsTypes+=typeList
    )?
    body=classBody
    ;

typeParameters
    : '<' params+=typeParameter (',' params+=typeParameter)* '>'
    ;

typeParameter
    : annotations+=annotation* typeName=identifier (EXTENDS annotations+=annotation* boundType=typeBound)?
    ;

typeBound
    : types+=typeType ('&' types+=typeType)*
    ;

enumDeclaration
    : ENUM enumName=identifier (IMPLEMENTS implementsTypes=typeList)? '{' constants=enumConstants? ','? enumDeclarations=enumBodyDeclarations? '}'
    ;

enumConstants
    : constants+=enumConstant (',' constants+=enumConstant)*
    ;

enumConstant
    : annotations+=annotation* name=identifier args=arguments? body=classBody?
    ;

enumBodyDeclarations
    : ';' classBodyDeclarations+=classBodyDeclaration*
    ;

interfaceDeclaration
    : INTERFACE interfaceName=identifier typeParams=typeParameters? (EXTENDS extendsTypes=typeList)? (PERMITS permitsTypes=typeList)? body=interfaceBody
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

memberDeclaration:recordDeclaration
    | methodDecl=methodDeclaration
    | genericMethodDecl=genericMethodDeclaration
    | fieldDecl=fieldDeclaration
    | constructorDecl=constructorDeclaration
    | genericConstructorDecl=genericConstructorDeclaration
    | interfaceDecl=interfaceDeclaration
    | annotationTypeDecl=annotationTypeDeclaration
    | classDecl=classDeclaration
    | enumDecl=enumDeclaration;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration:type=typeTypeOrVoid methodName=identifier params=formalParameters arrayDims+=ARRAY_BRACKETS* (THROWS throwsExceptions=qualifiedNameList)? body=methodBody;

methodBody:methodBlock=block
    | ';'
    ;

typeTypeOrVoid:type=typeType
    | voidType=VOID;

genericMethodDeclaration:typeParams=typeParameters methodDecl=methodDeclaration;

genericConstructorDeclaration:typeParams=typeParameters constructorDecl=constructorDeclaration;

constructorDeclaration:constructorName=identifier params=formalParameters (THROWS throwsExceptions=qualifiedNameList)? constructorBody = block;

compactConstructorDeclaration
    : modifiers+=modifier* constructorName=identifier constructorBody = block
    ;

fieldDeclaration:fieldType=typeType varDecls+=variableDeclarators ';'
    ;

interfaceBodyDeclaration
    : modifiers+=modifier* interfaceDecl=interfaceMemberDeclaration
    | ';'
    ;

interfaceMemberDeclaration:recordDeclaration
    | constDecl=constDeclaration
    | methodDecl=interfaceMethodDeclaration
    | genericMethodDecl=genericInterfaceMethodDeclaration
    | interfaceDecl=interfaceDeclaration
    | annotationDecl=annotationTypeDeclaration
    | classDecl=classDeclaration
    | enumDecl=enumDeclaration;

constDeclaration:type=typeType constDecls+=constantDeclarator (',' constDecls+=constantDeclarator)* ';'
    ;

constantDeclarator:name=identifier arrayDims+=ARRAY_BRACKETS* '=' initializer=variableInitializer;

// Early versions of Java allows brackets after the method name, eg.
// public int[] return2DArray() [] { ... }
// is the same as
// public int[][] return2DArray() { ... }
interfaceMethodDeclaration:modifiers+=interfaceMethodModifier* methodDecl=interfaceCommonBodyDeclaration;

interfaceMethodModifier:interfaceAnnotation=annotation
    | publicModifier=PUBLIC
    | abstractModifier=ABSTRACT
    | defaultModifier=DEFAULT
    | staticModifier=STATIC
    | strictCTFPModifier=STRICTFP;

genericInterfaceMethodDeclaration:modifiers+=interfaceMethodModifier* typeParams=typeParameters methodDecl=interfaceCommonBodyDeclaration;

interfaceCommonBodyDeclaration
    : interfaceAnnotations+=annotation* type=typeTypeOrVoid name=identifier params=formalParameters arrayDims+=ARRAY_BRACKETS* (THROWS throwsExceptions=qualifiedNameList)? body=methodBody
    ;

variableDeclarators
    : varDecls+=variableDeclarator (',' varDecls+=variableDeclarator)*
    ;

variableDeclarator:varName=variableDeclaratorId ('=' initializer=variableInitializer)?;

variableDeclaratorId:varName=identifier arrayDims+=ARRAY_BRACKETS*;

variableInitializer:initializer=arrayInitializer
    | varExpression=expression;

arrayInitializer
    : '{' (varInitializers+=variableInitializer (',' varInitializers+=variableInitializer)* ','?)? '}'
    ;

classType:
    (
      ( mackageName=pkgName '.' annotations+=annotation* )? types+=typeIdentifier typeArgs+=typeArguments?
    )+ ( '.' annotations+=annotation* types+=typeIdentifier typeArgs+=typeArguments? )*
    ;

pkgName:
    elementNames+=identifier ('.' elementNames+=identifier)*
    ;

typeArgument:type=typeType
    | annotations+=annotation* '?' ((EXTENDS | superType=SUPER) extendsOrSuperType=typeType)?;

qualifiedNameList
    : name+=qualifiedName (',' name+=qualifiedName)*
    ;

formalParameters
    : '(' (
       ( receiverParam=receiverParameter | param+=formalParameter ) (',' paramList+=formalParameterList)*
    )? ')'
    ;

receiverParameter
    : type=typeType (elementNames+=identifier '.')* THIS
    ;

formalParameterList
    : params+=formalParameter (',' params+=formalParameter)*
    ;

formalParameter
    : modifiers+=variableModifier* type=typeType (annotation* '...')? varDecl=variableDeclaratorId
    ;

// local variable type inference
lambdaLVTIList
    : modifiers+=lambdaLVTIParameter (',' modifiers+=lambdaLVTIParameter)*
    ;

lambdaLVTIParameter
    : modifiers+=variableModifier* VAR name=identifier
    ;

qualifiedName
    : element+=identifier ('.' element+=identifier)*
    ;

literal:decimalValue     =  integerLiteral
    | floatValue         =  floatLiteral
    | charValue          =  CHAR_LITERAL
    | stringValue        =  STRING_LITERAL
    | boolValue          =  BOOL_LITERAL
    | nullValue          =  NULL_LITERAL
    | textBlockValue     =  TEXT_BLOCK;

integerLiteral:decimalValue=DECIMAL_LITERAL
    | hexValue=HEX_LITERAL
    | octValue=OCT_LITERAL
    | ninaryValue=BINARY_LITERAL;

floatLiteral:floatValue=FLOAT_LITERAL
    | hexValue=HEX_FLOAT_LITERAL;

// ANNOTATIONS
//altAnnotationQualifiedName
//    : (names+=identifier DOT)* '@' name+=identifier
//    ;

//annotation
//    : ('@' qualifiedName /* | altAnnotationQualifiedName */) ( '(' ( elementValuePairs | elementValue)? ')')?
//    ;

annotation:('@' name=qualifiedName /* | altAnnotationQualifiedName */) annotationFieldValues+=annotationFieldVals?;

annotationFieldVals:
	'(' ( annotationFieldValues+=annotationFieldValue ( ',' annotationFieldValues+=annotationFieldValue )* )? ')'
	;

annotationFieldValue:
	{ this.IsNotIdentifierAssign(); }? value=annotationVal
	| name=identifier '=' value=annotationVal
	;

annotationVal:
	expressionValue=expression //conditionalExpression
	| annotationValue=annotation
	| '{' ( values+=annotationVal ( ',' values+=annotationVal )* )? commaAfterLastValue=','? '}'
	;

//elementValuePairs
//    : elementValuePair (',' elementValuePair)*
//    ;

//elementValuePair
//    : identifier '=' elementValue
//    ;

elementValue:expressionValue=expression
    | annotationValue=annotation
    | elemValueArrayInit=elementValueArrayInitializer;

elementValueArrayInitializer
//    : '{' (elementValue (',' elementValue)*)? ','? '}'
    : '{' (values+=elementValue (',' values+=elementValue)*)? commaAfterLastValue=','? '}'
    ;

annotationTypeDeclaration
    : '@' INTERFACE name=identifier body=annotationTypeBody
    ;

annotationTypeBody
    : '{' (declarations+=annotationTypeElementDeclaration)* '}'
    ;

annotationTypeElementDeclaration
    : modifiers+=modifier* declaration=annotationTypeElementRest
    | ';' // this is not allowed by the grammar, but apparently allowed by the actual compiler
    ;

annotationTypeElementRest:type=typeType methodOrConstRest=annotationMethodOrConstantRest ';'
    | classDecl=classDeclaration ';'?
    | interfaceDecl=interfaceDeclaration ';'?
    | enumDecl=enumDeclaration ';'?
    | typeDecl=annotationTypeDeclaration ';'?
    | recordDecl=recordDeclaration ';'?
    ;

annotationMethodOrConstantRest:method=annotationMethodRest
    | constant=annotationConstantRest;

annotationMethodRest:methodName=identifier '(' ')' value=defaultValue?;

annotationConstantRest:constDecl=variableDeclarators;

defaultValue
    : DEFAULT value=elementValue
    ;

moduleDeclaration
    : annotations+=annotation* OPEN? MODULE name=qualifiedName '{' directives+=moduleDirective* '}'
    ;

moduleDirective
    : REQUIRES modifiers+=requiresModifier* name=qualifiedName ';'                                      # requiresDirective
    | EXPORTS name=qualifiedName (TO targets+=qualifiedName (',' targets+=qualifiedName)* )? ';'        # exportsDirective
    | OPENS name=qualifiedName (TO targets+=qualifiedName (',' targets+=qualifiedName)* )? ';'          # opensDirective
    | USES name=qualifiedName ';'                                                                       # usesDirective
    | PROVIDES name=qualifiedName WITH with=qualifiedName (',' with=qualifiedName)* ';'                 # providesDirective
    ;

requiresModifier
    : TRANSITIVE   # transitiveModifier
    | STATIC       # staticModifier
    ;

recordDeclaration
    : RECORD name=identifier typePArams=typeParameters? header=recordHeader (IMPLEMENTS typeLst=typeList)? body=recordBody
    ;

recordHeader
    : '(' compnentList=recordComponentList? ')'
    ;

recordComponentList
    : components+=recordComponent (',' components+=recordComponent)* { this.DoLastRecordComponent(); }?
    ;

recordComponent
    : annotations+=annotation* type=typeType (annotations+=annotation* ELLIPSIS)? name=identifier
    ;

recordBody
    : '{' (bodyDecl=classBodyDeclaration | constructorDecl=compactConstructorDeclaration)* '}'
    ;

// STATEMENTS / BLOCKS

block
    : '{' statements+=blockStatement* '}'
    ;

blockStatement:varDecl=localVariableDeclaration ';'
    | typeDecl=localTypeDeclaration
    | wrappedStatement=statement
    ;

localVariableDeclaration
    : modifiers+=variableModifier* (VAR varName=identifier '=' varExpr=expression | type=typeType varDecls=variableDeclarators)
    ;

identifier
    : IDENTIFIER
    | MODULE
    | OPEN
    | REQUIRES
    | EXPORTS
    | OPENS
    | TO
    | USES
    | PROVIDES
    | WHEN
    | WITH
    | TRANSITIVE
    | YIELD
    | SEALED
    | PERMITS
    | RECORD
    | VAR
    ;

typeIdentifier // Identifiers that are not restricted for type declarations
    : IDENTIFIER
    | MODULE
    | OPEN
    | REQUIRES
    | EXPORTS
    | OPENS
    | TO
    | USES
    | PROVIDES
    | WITH
    | TRANSITIVE
    | SEALED
    ;

localTypeDeclaration:modifiers+=classOrInterfaceModifier* (classDecl=classDeclaration | interfaceDecl=interfaceDeclaration | recordDecl=recordDeclaration | enumDecl=enumDeclaration);

statement:blockLabel = block                                                                                            # blockStmnt
    | ASSERT expressions+=expression (':' expressions+=expression)? ';'                                                 # assertStmnt
    | IF '(' check=expression ')' ifStatement=statement (ELSE elseStatement=statement)?                                 # ifElseStmnt
    | FOR '(' control=forControl ')' forStatement=statement                                                             # forStmnt
    | WHILE '(' control=expression ')' whileStatement=statement                                                         # whileStmnt
    | DO doStatement=statement WHILE '(' control=expression ')' ';'                                                     # doWhileStmnt
    | TRY tryBlock=block (catchClauses+=catchClause+ finallyBlk=finallyBlock? | finallyBlk=finallyBlock)                # tryNoResourcesStmnt
    | TRY resourceSpec=resourceSpecification tryBlock=block catchClauses+=catchClause* finallyBlk=finallyBlock?         # tryWithResourcesStmnt
    | SWITCH '(' check=expression ')' '{' blockStatements+=switchBlockStatementGroup* switchLabels+=switchLabel* '}'    # switchStmnt
    | SYNCHRONIZED '(' syncExpression=expression ')' syncBlock=block                                                    # synchronizedStmnt
    | RETURN returnValue=expression? ';'                                                                                # returnStmnt
    | THROW throwExpression=expression ';'                                                                              # throwStmnt
    | BREAK labelIdentifier=identifier? ';'                                                                             # breakStmnt
    | CONTINUE labelIdentifier=identifier? ';'                                                                          # continueStmnt
    | YIELD yieldExpr=expression ';'                                                                                    # yieldStmnt
    | SEMI                                                                                                              # semicolonStmnt
    | statementExpression = expression ';'                                                                              # expressionStmnt
    | switchExpr=switchExpression ';'?                                                                                  # switchExpressionStmnt
    | identifierLabel = identifier ':' labelStatement=statement                                                         # labeledStmnt
    ;

catchClause
    : CATCH '(' modifiers+=variableModifier* type=catchType exceptionName=identifier ')' catchBlock=block
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
    : modifiers+=variableModifier* (type=classOrInterfaceType varDeclId=variableDeclaratorId | VAR varId=identifier) '=' initExpr=expression # resourceDeclaration
    | qualName = qualifiedName                                                                                                               # resourceExpression
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : (labels+=switchLabel ':')+ statements+=blockStatement+
    ;

switchLabel:CASE (
        constantExpression = expression
        | enumConstantName = IDENTIFIER
        | typeType varName = identifier
    )                                       # caseLabel
    | DEFAULT                               # defaultLabel
    ;

forControl:enhancedForControl
    | init=forInit? ';' conditionExpr=expression? ';' forUpdate = expressionList?
    ;

forInit:varDecl=localVariableDeclaration
    | expressions=expressionList;

enhancedForControl
    : modifiers+=variableModifier* (type=typeType | VAR) varDeclId=variableDeclaratorId ':' expr=expression
    ;

// EXPRESSIONS

expressionList
    : elements+=expression (',' elements+=expression)*
    ;

methodCall:(methodName=identifier | thisExpr=THIS | superExpr=SUPER) args=arguments;

expression
    // Expression order in accordance with https://introcs.cs.princeton.edu/java/11precedence/
    // Level 16, Primary, array and member access
    : primary                                                       #PrimaryExpression
    | expression '[' expression ']'                                 #SquareBracketExpression
    | expression bop = '.' (
        name=identifier
        | call=methodCall
        | thisExpr=THIS
        | NEW typeArgs=nonWildcardTypeArguments? inner=innerCreator
        | SUPER superSffx=superSuffix
        | invocation=explicitGenericInvocation
    )                                                               #MemberReferenceExpression
    // Method calls and method references are part of primary, and hence level 16 precedence
    | call=methodCall                                                    #MethodCallExpression
    | expr=expression '::' typeArgs=typeArguments? name=identifier       #MethodReferenceExpression
    | typeType '::' (typeArgs=typeArguments? name=identifier | NEW)               #MethodReferenceExpression
    | classType '::' typeArgs=typeArguments? NEW                             #MethodReferenceExpression
    
    | switchExpression                                              #ExpressionSwitch

    // Level 15 Post-increment/decrement operators
    | expr=expression postfix = ('++' | '--')                            #PostIncrementDecrementOperatorExpression

    // Level 14, Unary operators
    | prefix = ('+' | '-' | '++' | '--' | '~' | '!') expr=expression     #UnaryOperatorExpression

    // Level 13 Cast and object creation
    | '(' annotations+=annotation* types+=typeType ('&' types+=typeType)* ')' expr=expression       #CastExpression
    | NEW constructor=creator                                       #ObjectCreationExpression

    // Level 12 to 1, Remaining operators
    // Level 12, Multiplicative operators
    | left=expression bop = ('*' | '/' | '%') right=expression           #BinaryOperatorExpression
    // Level 11, Additive operators
    | left=expression bop = ('+' | '-') right=expression                 #BinaryOperatorExpression
    // Level 10, Shift operators
    | left=expression ('<' '<' | '>' '>' '>' | '>' '>') right=expression #BinaryOperatorExpression
    // Level 9, Relational operators
    | left=expression bop = ('<=' | '>=' | '>' | '<') right=expression   #BinaryOperatorExpression
    | expr=expression bop = INSTANCEOF (type=typeType | pattrn=pattern)        #InstanceOfOperatorExpression
    // Level 8, Equality Operators
    | left=expression bop = ('==' | '!=') right=expression               #BinaryOperatorExpression
    // Level 7, Bitwise AND
    | left=expression bop = '&' right=expression                         #BinaryOperatorExpression
    // Level 6, Bitwise XOR
    | left=expression bop = '^' right=expression                         #BinaryOperatorExpression
    // Level 5, Bitwise OR
    | left=expression bop = '|' right=expression                         #BinaryOperatorExpression
    // Level 4, Logic AND
    | left=expression bop = '&&' right=expression                        #BinaryOperatorExpression
    // Level 3, Logic OR
    | left=expression bop = '||' right=expression                        #BinaryOperatorExpression
    // Level 2, Ternary
    | <assoc = right> expression bop = '?' expression ':' expression #TernaryExpression
    // Level 1, Assignment
    | <assoc = right> assignTo=expression bop = (
        '='
        | '+='
        | '-='
        | '*='
        | '/='
        | '&='
        | '|='
        | '^='
        | '>>='
        | '>>>='
        | '<<='
        | '%='
    ) assignExpr=expression                                        #BinaryOperatorExpression

    // Level 0, Lambda Expression
    | expr=lambdaExpression                                        #ExpressionLambda
    ;

pattern
    : modifiers+=variableModifier* type=typeType annotations+=annotation* varDecls=variableDeclarators
    | type=typeType '(' patternList=componentPatternList? ')'
    ;

componentPatternList :
    patternElements+=componentPattern ( ',' patternElements+=componentPattern )*
    ;

componentPattern :
    pattern
    ;

lambdaExpression:params=lambdaParameters '->' body=lambdaBody;

lambdaParameters:name=identifier                                # simpleIdentifierLambdaParams
    | '(' paramsList=formalParameterList? ')'                   # formalParamListLambdaParams
    | '(' names+=identifier (',' names+=identifier)* ')'        # simpleIdentifierListLambdaParams
    | '(' typeInferenceList=lambdaLVTIList? ')'                 # lvtiListLambdaParams
    ;

lambdaBody:lambdaExpr=expression
    | lambdaBlock=block;

primary:'(' expressn=expression ')'                                                         # ParenExpression
    | THIS                                                                                  # ThisExpr
    | SUPER                                                                                 # SuperExpr
    | lit=literal                                                                           # LiteralExpr
    | identifier                                                                            # IdentifierExpr
    | type=typeTypeOrVoid '.' CLASS                                                         # ClassLiteralExpr
    | typeArgs=nonWildcardTypeArguments (invokationSuffix=explicitGenericInvocationSuffix | THIS args=arguments) # ExplicitGenericInvocationExpr
    ;

switchExpression
    : SWITCH '(' expressn=expression ')' '{' rules+=switchLabeledRule* '}'
    ;

switchLabeledRule
    : CASE (
	expressionLst = expressionList
	| NULL_LITERAL (',' DEFAULT)?
	| casePatterns+=casePattern (',' casePatterns+=casePattern)* when=guard?
	) (ARROW | COLON) switchRuleOutcome                 # SwitchRule
    | DEFAULT (ARROW | COLON) switchRuleOutcome         # DefaultSwitchRule
    ;

guard 
    : 'when' guardExpression=expression
    ;

casePattern
    : casePattrn=pattern
    ;

switchRuleOutcome
    : switchBlock=block
    | statements+=blockStatement* // is *-operator correct??? I don't think so. https://docs.oracle.com/javase/specs/jls/se24/html/jls-14.html#jls-BlockStatements
    ;

classOrInterfaceType
    : type=classType // classType, interfaceType are all essentially identical to classOrInterfaceType because of no symbol table.
    ;

creator:typeArgs=nonWildcardTypeArguments? name=createdName rest=classCreatorRest
    | name=createdName arrayRest=arrayCreatorRest;

createdName:nameElements+=identifier typeArgs+=typeArgumentsOrDiamond? ('.' nameElements+=identifier typeArgs+=typeArgumentsOrDiamond?)*
    | type=primitiveType;

innerCreator:name=identifier typeArgs=nonWildcardTypeArgumentsOrDiamond? rest=classCreatorRest;

arrayCreatorRest
    : ('[' ']')+ initializer=arrayInitializer
    | ('[' expressions+=expression ']')+ ('[' ']')*
    ;

classCreatorRest:args=arguments classBody?;

explicitGenericInvocation:typeArgs=nonWildcardTypeArguments genericInvocationSuffix=explicitGenericInvocationSuffix;

typeArgumentsOrDiamond:
      '<' '>'                   # diamondTypeArgs
    | typeArgs=typeArguments    # typeArgs
    ;

nonWildcardTypeArgumentsOrDiamond:
      '<' '>'                               # diamondNonWildCardTypeArgs
    | typeArgs=nonWildcardTypeArguments     # nonWildCardTypeArgs
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
    : BOOLEAN       # booleanType
    | CHAR          # charType
    | BYTE          # byteType
    | SHORT         # shortType
    | INT           # intType
    | LONG          # longType
    | FLOAT         # floatType
    | DOUBLE        # doubleType
    ;

typeArguments
    : '<' typeArgs+=typeArgument (',' typeArgs+=typeArgument)* '>'
    ;

superSuffix:args=arguments
    | '.' typeArgs=typeArguments? name=identifier args=arguments?;

explicitGenericInvocationSuffix:SUPER superSuffx=superSuffix
    | name=identifier args=arguments;

arguments
    : '(' args=expressionList? ')'
    ;

// added by miho: 18.01.2018

ARRAY_BRACKETS: '[' ']';

DOT_STAR: '.' '*';


// ---- Lexer rules (from JavaLexer.g4) ----

/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 Copyright (c) 2021 Michał Lorek (upgrade to Java 11)
 Copyright (c) 2022 Michał Lorek (upgrade to Java 17)
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

// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true
// Keywords

ABSTRACT     : 'abstract';
ASSERT       : 'assert';
BOOLEAN      : 'boolean';
BREAK        : 'break';
BYTE         : 'byte';
CASE         : 'case';
CATCH        : 'catch';
CHAR         : 'char';
CLASS        : 'class';
CONST        : 'const';
CONTINUE     : 'continue';
DEFAULT      : 'default';
DO           : 'do';
DOUBLE       : 'double';
ELSE         : 'else';
ENUM         : 'enum';
EXPORTS    : 'exports';
EXTENDS      : 'extends';
FINAL        : 'final';
FINALLY      : 'finally';
FLOAT        : 'float';
FOR          : 'for';
GOTO         : 'goto';
IF           : 'if';
IMPLEMENTS   : 'implements';
IMPORT       : 'import';
INSTANCEOF   : 'instanceof';
INT          : 'int';
INTERFACE    : 'interface';
LONG         : 'long';
MODULE     : 'module';
NATIVE       : 'native';
NEW          : 'new';
NON_SEALED : 'non-sealed';
OPEN       : 'open';
OPENS      : 'opens';
PACKAGE      : 'package';
PERMITS    : 'permits';
PRIVATE      : 'private';
PROTECTED    : 'protected';
PROVIDES   : 'provides';
PUBLIC       : 'public';
RECORD: 'record';
REQUIRES   : 'requires';
RETURN       : 'return';
SEALED     : 'sealed';
SHORT        : 'short';
STATIC       : 'static';
STRICTFP     : 'strictfp';
SUPER        : 'super';
SWITCH       : 'switch';
SYNCHRONIZED : 'synchronized';
THIS         : 'this';
THROW        : 'throw';
THROWS       : 'throws';
TO         : 'to';
TRANSIENT    : 'transient';
TRANSITIVE : 'transitive';
TRY          : 'try';
USES       : 'uses';
VAR: 'var'; // reserved type name
VOID         : 'void';
VOLATILE     : 'volatile';
WHEN : 'when';
WHILE        : 'while';
WITH       : 'with';
YIELD: 'yield'; // reserved type name from Java 14

// Literals

DECIMAL_LITERAL : ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
HEX_LITERAL     : '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL     : '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL  : '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:
    (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
    | Digits (ExponentPart [fFdD]? | [fFdD])
;

HEX_FLOAT_LITERAL: '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL: 'true' | 'false';

CHAR_LITERAL: '\'' (~['\\\r\n] | EscapeSequence) '\'';

STRING_LITERAL: '"' (~["\\\r\n] | EscapeSequence)* '"';

TEXT_BLOCK: '"""' [ \t]* [\r\n] (. | EscapeSequence)*? '"""';

NULL_LITERAL: 'null';

// Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
SEMI   : ';';
COMMA  : ',';
DOT    : '.';

// Operators

ASSIGN   : '=';
GT       : '>';
LT       : '<';
BANG     : '!';
TILDE    : '~';
QUESTION : '?';
COLON    : ':';
EQUAL    : '==';
LE       : '<=';
GE       : '>=';
NOTEQUAL : '!=';
AND      : '&&';
OR       : '||';
INC      : '++';
DEC      : '--';
ADD      : '+';
SUB      : '-';
MUL      : '*';
DIV      : '/';
BITAND   : '&';
BITOR    : '|';
CARET    : '^';
MOD      : '%';

ADD_ASSIGN     : '+=';
SUB_ASSIGN     : '-=';
MUL_ASSIGN     : '*=';
DIV_ASSIGN     : '/=';
AND_ASSIGN     : '&=';
OR_ASSIGN      : '|=';
XOR_ASSIGN     : '^=';
MOD_ASSIGN     : '%=';
LSHIFT_ASSIGN  : '<<=';
RSHIFT_ASSIGN  : '>>=';
URSHIFT_ASSIGN : '>>>=';

// Java 8 tokens

ARROW      : '->';
COLONCOLON : '::';

// Additional symbols not defined in the lexical specification

AT       : '@';
ELLIPSIS : '...';

// Whitespace and comments

WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT : '//' ~[\r\n]*    -> channel(HIDDEN);

// Identifiers

IDENTIFIER: Letter LetterOrDigit*;

// Fragment rules

fragment ExponentPart: [eE] [+-]? Digits;

fragment EscapeSequence:
    '\\' 'u005c'? [bstnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigits: HexDigit ((HexDigit | '_')* HexDigit)?;

fragment HexDigit: [0-9a-fA-F];

fragment Digits: [0-9] ([0-9_]* [0-9])?;

fragment LetterOrDigit: Letter | [0-9];

fragment Letter:
    [a-zA-Z$_]                        // these are the "java letters" below 0x7F
    | ~[\u0000-\u007F\uD800-\uDBFF]   // covers all characters above 0x7F which are not a surrogate
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

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java24.PackageDeclarationDelegate")
    void defPackageNameFromString(String packageName);

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java24.PackageDeclarationDelegate")
    String packageNameAsString();

}

interface MethodDeclaration {

    @DelegateTo(className="eu.mihosoft.vmftext.tests.java24.MethodDeclarationDelegate")
    boolean returnsVoid();

}


*/