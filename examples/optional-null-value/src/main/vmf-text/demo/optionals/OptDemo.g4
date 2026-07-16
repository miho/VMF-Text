grammar OptDemo;

root: items+=item (',' items+=item)* EOF;
item: 'r' ('(' name=ID ')')? ;
ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
