grammar PipeList;

// Non-comma single-terminal separator — splice uses slot rhythm, not ',' text.
prog: items+=ID ('|' items+=ID)* EOF;

ID : [a-zA-Z][a-zA-Z0-9]* ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
