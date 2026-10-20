grammar OneManyLexer;

// A one-or-more list written as a single EBNF element (words+=WORD+).
// Until 0.2.3 this shape parsed but could not be unparsed ("Language model is
// invalid!"): the generated matcher only counted parser-rule elements as
// consumed. Imported grammars use it constantly (e.g. C's StringLiteral+).
prog: parts+=part+ EOF;

part: words+=WORD+ ';'
    | strings+=STRING+ ';'
    ;

WORD   : [a-zA-Z]+ ;
STRING : '"' ~["\r\n]* '"' ;
WS     : [ \t\r\n]+ -> channel(HIDDEN) ;
