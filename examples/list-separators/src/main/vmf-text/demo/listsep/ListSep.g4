grammar ListSep;

// Two list shapes on one grammar:
//   multi:  ID (',' 'and' ID)*   — separatorCount == 2
//   nosep:  ID (ID)*             — separatorCount == 0
root: multi=multiList ';' nosep=noSepList EOF;

multiList: items+=ID (',' 'and' items+=ID)* ;
noSepList: items+=ID (items+=ID)* ;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
