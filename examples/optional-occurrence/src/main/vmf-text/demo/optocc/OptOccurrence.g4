grammar OptOccurrence;

root: items+=item (',' items+=item)* EOF;
item: 'x' ('(' name=ID ')')? ;
ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
