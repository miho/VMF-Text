grammar NoSep;

// Separator-less repetition (item+).
prog: items+=ID+ EOF;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
