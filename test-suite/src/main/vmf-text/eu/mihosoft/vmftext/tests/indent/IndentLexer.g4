lexer grammar IndentLexer;

// #60: a Python-like indentation language. Line ends and block structure are
// tokens that the lexer base class synthesizes: NEWLINE (the line break),
// INDENT (its text is the indentation of the block's first line) and DEDENT
// (empty). Blank lines, comments and same-level indentation go to the hidden
// channel, so the token stream is lossless.
options { superClass = IndentLexerBase; }

tokens { INDENT, DEDENT }

IF : 'if' ;
ELSE : 'else' ;
PASS : 'pass' ;
RETURN : 'return' ;
COLON : ':' ;
ASSIGN : '=' ;
LPAREN : '(' ;
RPAREN : ')' ;
COMMA : ',' ;
NAME : [a-z]+ ;
NUMBER : [0-9]+ ;
NEWLINE : ( {atStartOfInput()}? SPACES | ( '\r'? '\n' | '\r' | '\f' ) SPACES? ) { onNewLine(); } ;
SKIP_ : ( SPACES | COMMENT ) -> channel(HIDDEN) ;

fragment SPACES : [ \t]+ ;
fragment COMMENT : '#' ~[\r\n\f]* ;
