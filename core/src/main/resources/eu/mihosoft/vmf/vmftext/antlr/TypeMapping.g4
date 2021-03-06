grammar TypeMapping;

typeMappingCode: typemappings+=typeMapping* ;

typeMapping:
    'TypeMap' '(' (applyTo+=Identifier (',' applyTo+=Identifier)*)? ')'  '{'
      entries+=mapping*
    '}'
     ;

mapping:
    '('? ('rule:')? ruleName=Identifier '->' ('type:')? typeName=javaType ')'? ('via'|'=')
    (
        ('('|'{')
        ('toType' ':')?  stringToTypeCode = STRING_SINGLE ',' ('toString' ':')? typeToStringCode = STRING_SINGLE (',' + ('default' ':')? defaultCode = STRING_SINGLE)?
        (')'|'}')

        |

        ('('|'{')
           stringToTypeCode = STRING_SINGLE
        (')'|'}')

        |

        ('toType' ':')? stringToTypeCode = STRING_SINGLE
    )
    ;


javaType
    : JavaIdentifier '[' ']'
    | JavaIdentifier typeArguments
    | JavaIdentifier
    ;


typeBound
	:	'extends' javaType
	|	'extends' javaType additionalBound*
	;

additionalBound
	:	'&' javaType
	;

typeArguments
	:	'<' typeArgumentList '>'
	;

typeArgumentList
	:	typeArgument (',' typeArgument)*
	;

typeArgument
	:	javaType
	|	wildcard
	;

wildcard
	:	'?' wildcardBounds?
	;

wildcardBounds
	:	'extends' javaType
	|	'super' javaType
	;



// ANTLR

JavaIdentifier : Identifier ('.' Identifier)+
       ;


// Derived from ANTR4 grammar (we use the same Identifier for rules and Java classes)
Identifier: NameStartChar NameChar*
    ;


STRING_DOUBLE: '"'  (~["\\\r\n] | ESCAPE_SEQUENCE)* '"';

STRING_SINGLE: '\'' (~['\\\r\n] | ESCAPE_SEQUENCE)* '\'';


fragment ESCAPE_SEQUENCE
: '\\' [btnfr"\\]
;

fragment
NameChar
   : NameStartChar
   | '0'..'9'
   | '_'
   | '\u00B7'
   | '\u0300'..'\u036F'
   | '\u203F'..'\u2040'
   ;
fragment
NameStartChar
   : 'A'..'Z' | 'a'..'z'
   | '\u00C0'..'\u00D6'
   | '\u00D8'..'\u00F6'
   | '\u00F8'..'\u02FF'
   | '\u0370'..'\u037D'
   | '\u037F'..'\u1FFF'
   | '\u200C'..'\u200D'
   | '\u2070'..'\u218F'
   | '\u2C00'..'\u2FEF'
   | '\u3001'..'\uD7FF'
   | '\uF900'..'\uFDCF'
   | '\uFDF0'..'\uFFFD'
   ;


//
// Whitespace and comments
//

WS  :  [ \t\r\n]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN)
    ;