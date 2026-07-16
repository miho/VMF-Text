grammar MultiSep;

// Multi-token separator between items: ',' 'and'
prog: items+=ID (',' 'and' items+=ID)* EOF;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
