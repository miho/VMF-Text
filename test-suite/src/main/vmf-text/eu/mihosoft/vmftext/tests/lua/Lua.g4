/*
BSD License
Copyright (c) 2013, Kazunori Sakamoto
Copyright (c) 2016, Alexander Alexeev
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the NAME of Rainer Schuster nor the NAMEs of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
This grammar file derived from:
    Lua 5.3 Reference Manual
    http://www.lua.org/manual/5.3/manual.html
    Lua 5.2 Reference Manual
    http://www.lua.org/manual/5.2/manual.html
    Lua 5.1 grammar written by Nicolai Mainiero
    http://www.antlr3.org/grammar/1178608849736/Lua.g
Tested by Kazunori Sakamoto with Test suite for Lua 5.2 (http://www.lua.org/tests/5.2/)
Tested by Alexander Alexeev with Test suite for Lua 5.3 http://www.lua.org/tests/lua-5.3.2-tests.tar.gz
*/

grammar Lua;

chunk
    : codeBlock=block EOF
    ;

block
    : statement=stat* returnStatement=retstat?
    ;

stat
    : ';'                                                                        # emptyStat
    | vars=varlist '=' expressions=explist                                       # assignmentStat
    | call=functioncall                                                          # callStat
    | name=label                                                                 # labelStat
    | 'break'                                                                    # breakStat
    | 'goto' labelName=NAME                                                      # gotoStat
    | 'do' codeBlock=block 'end'                                                 # doLoop
    | 'while' condition=exp 'do' codeblock=block 'end'                           # whileLoop
    | 'repeat' codeblock=block 'until' condition=exp                             # untilLoop
    | 'if' ifCondition=exp 'then' ifBlock=block ('elseif' elseIfConditions+=exp 'then' elseIfBlocks+=block)* ('else' elseBlock=block)? 'end'   # ifElse
    | 'for' varName=NAME '=' fromExp=exp ',' toExpressions+=exp (',' toExpressions+=exp)? 'do' codeBlock=block 'end'                           # forLoop
    | 'for' names=namelist 'in' expressions=explist 'do' codeBlock=block 'end'                                                                 # forEach
    | 'function' name=funcname body=funcbody                                     # function
    | 'local' 'function' name=NAME body=funcbody                                 # localfunction
    | 'local' names=namelist ('=' expressions=explist)?                          # localVariables
    ;

retstat
    : 'return' expressions=explist? ';'?
    ;

label
    : '::' name=NAME '::'
    ;

funcname
    : parts+=NAME ('.' parts+=NAME)* (':' lastPart=NAME)?
    ;

varlist
    : vars+=var (',' vars+=var)*
    ;

namelist
    : names+=NAME (',' names+=NAME)*
    ;

explist
    : expressions+=exp (',' expressions+=exp)*
    ;

exp
    : 'nil'                                              # nilExp
    | 'false'                                            # falseExp
    | 'true'                                             # trueExp
    | value=numberValue                                  # numberExp
    | value=stringValue                                  # valueExp
    | '...'                                              # trippleDotExp
    | def=functiondef                                    # functionDefExp
    | expression=varOrExp arguments+=nameAndArgs*        # prefixExp
    | constructor=tableconstructor                       # tableConstructorExp
    | <assoc=right> left=exp op=operatorPower right=exp  # powerExp
    | op=operatorUnary expression=exp                    # unaryExp
    | left=exp op=operatorMulDivMod right=exp            # multDivModExp
    | left=exp op=operatorAddSub right=exp               # addSubExp
    | <assoc=right> left=exp op=operatorStrcat right=exp # strCatExp
    | left=exp op=operatorComparison right=exp           # comparisonExp
    | left=exp op=operatorAnd right=exp                  # andExp
    | left=exp op=operatorOr right=exp                   # orExp
    | left=exp op=operatorBitwise right=exp              # bitwiseExp
    ;

//prefixexp
//    : expression=varOrExp arguments+=nameAndArgs*
//    ;

functioncall
    : expression=varOrExp arguments=nameAndArgs+
    ;

varOrExp
    : variable=var | '(' expression=exp ')'
    ;

var
    : (name=NAME | '(' expression=exp ')' suffixes+=varSuffix) suffixes+=varSuffix*
    ;

varSuffix
    : arguments+=nameAndArgs* ('[' expression=exp ']' | '.' name=NAME)
    ;

nameAndArgs
    : (':' name=NAME)? arguments=args
    ;

/*
var
    : NAME | prefixexp '[' exp ']' | prefixexp '.' NAME
    ;
prefixexp
    : var | functioncall | '(' exp ')'
    ;
functioncall
    : prefixexp args | prefixexp ':' NAME args
    ;
*/

args
    : '(' expressions=explist? ')' | constructor=tableconstructor | stringArg=stringValue
    ;

functiondef
    : 'function' body=funcbody
    ;

funcbody
    : '(' parameters=parlist? ')' codeBlock=block 'end'
    ;

parlist
    : names=namelist (sep=',' trippleDots='...')? | trippleDots='...'
    ;

tableconstructor
    : '{' fields=fieldlist? '}'
    ;

fieldlist
    : fields+=field (sep=fieldSep fields+=field)* sep=fieldSep?
    ;

field
    : '[' keyExp=exp ']' '=' assignmentExp=exp | name=NAME '=' assignmentExp=exp | fieldExp=exp
    ;

fieldSep
    : sep=',' | sep=';'
    ;

operatorOr
	: op='or';

operatorAnd
	: op='and';

operatorComparison
	: op='<' | op='>' | op='<=' | op='>=' | op='~=' | op='==';

operatorStrcat
	: op='..';

operatorAddSub
	: op='+' | op='-';

operatorMulDivMod
	: op='*' | op='/' | op='%' | op='//';

operatorBitwise
	: op='&' | op='|' | op='~' | op='<<' | op='>>';

operatorUnary
    : op='not' | op='#' | op='-' | op='~';

operatorPower
    : op='^';

numberValue
    : value=INT | value=HEX | value=FLOAT | value=HEX_FLOAT
    ;

stringValue
    : value=NORMALSTRING | value=CHARSTRING | value=LONGSTRING
    ;

// LEXER

NAME
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

NORMALSTRING
    : '"' ( EscapeSequence | ~('\\'|'"') )* '"'
    ;

CHARSTRING
    : '\'' ( EscapeSequence | ~('\''|'\\') )* '\''
    ;

LONGSTRING
    : '[' NESTED_STR ']'
    ;

fragment
NESTED_STR
    : '=' NESTED_STR '='
    | '[' .*? ']'
    ;

INT
    : Digit+
    ;

HEX
    : '0' [xX] HexDigit+
    ;

FLOAT
    : Digit+ '.' Digit* ExponentPart?
    | '.' Digit+ ExponentPart?
    | Digit+ ExponentPart
    ;

HEX_FLOAT
    : '0' [xX] HexDigit+ '.' HexDigit* HexExponentPart?
    | '0' [xX] '.' HexDigit+ HexExponentPart?
    | '0' [xX] HexDigit+ HexExponentPart
    ;

fragment
ExponentPart
    : [eE] [+-]? Digit+
    ;

fragment
HexExponentPart
    : [pP] [+-]? Digit+
    ;

fragment
EscapeSequence
    : '\\' [abfnrtvz"'\\]
    | '\\' '\r'? '\n'
    | DecimalEscape
    | HexEscape
    | UtfEscape
    ;

fragment
DecimalEscape
    : '\\' Digit
    | '\\' Digit Digit
    | '\\' [0-2] Digit Digit
    ;

fragment
HexEscape
    : '\\' 'x' HexDigit HexDigit
    ;
fragment
UtfEscape
    : '\\' 'u{' HexDigit+ '}'
    ;
fragment
Digit
    : [0-9]
    ;
fragment
HexDigit
    : [0-9a-fA-F]
    ;
COMMENT
    : '--[' NESTED_STR ']' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '--'
    (                                               // --
    | '[' '='*                                      // --[==
    | '[' '='* ~('='|'['|'\r'|'\n') ~('\r'|'\n')*   // --[==AA
    | ~('['|'\r'|'\n') ~('\r'|'\n')*                // --AAA
    ) ('\r\n'|'\r'|'\n'|EOF)
    -> channel(HIDDEN)
    ;

WS
    : [ \t\u000C\r\n]+ -> skip
    ;
SHEBANG
    : '#' '!' ~('\n'|'\r')* -> channel(HIDDEN)
    ;