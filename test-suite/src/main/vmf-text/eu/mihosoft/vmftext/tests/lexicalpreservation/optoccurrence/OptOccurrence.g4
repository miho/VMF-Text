grammar OptOccurrence;

// Repeated optional groups with the same path pattern: each occurrence gets
// its own OptionalState.occurrenceIndex so mixed presence cannot drift.
root: items+=item (',' items+=item)* EOF;

item: 'x' ('(' name=ID ')')? ;

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
