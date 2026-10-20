grammar NoSep;

// Separator-less repetition written as ID (ID)* so the unparser sees two
// labeled occurrences with zero terminals between them (separatorCount == 0)
// and add/remove can splice trivia. (A lone items+=ID+ round-trips too since
// 0.2.3 -- see tests/onemanylexer -- but has no splice hint.)
prog: items+=ID (items+=ID)* EOF;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
