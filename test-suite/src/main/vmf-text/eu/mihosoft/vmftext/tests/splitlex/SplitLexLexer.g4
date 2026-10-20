lexer grammar SplitLexLexer;

// #39: a split grammar (lexer grammar + parser grammar with tokenVocab) is
// processed as one unit. The lexer has a base class, as grammars-v4 lexers do.
options { superClass = SplitLexLexerBase; }

tokens { VIRTUAL }

DEF : 'def' ;
EQ : '=' ;
ID : [a-z]+ ;
NUM : [0-9]+ ;
NEWLINE : '\r'? '\n' ;
WS : [ \t]+ -> channel(HIDDEN) ;
