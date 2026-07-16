grammar NoSep;

// Separator-less repetition written as ID (ID)* so the unparser sees two
// labeled occurrences with zero terminals between them (separatorCount == 0).
// (A lone items+=ID+ is a single EBNF element and does not round-trip.)
prog: items+=ID (items+=ID)* EOF;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
