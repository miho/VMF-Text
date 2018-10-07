grammar CombinedLexerRules;

program: '(' values+=COMBINED_LEXER_RULE (',' values+=COMBINED_LEXER_RULE)* ')';

COMBINED_LEXER_RULE :
         DIGIT+ DOT DIGIT+;

DIGIT : [0-9];
DOT : '.' ;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;
