grammar ModelBareList;

// Bare model-typed delimited list (no brackets). Parent holds comma slots;
// each item owns its leading trivia — same footprint family as JSON without
// the outer '[' / ']'.
root: items+=item (',' items+=item)* EOF;

item: name=ID ;

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
