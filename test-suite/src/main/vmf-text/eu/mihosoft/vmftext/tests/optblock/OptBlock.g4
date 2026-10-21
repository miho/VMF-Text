grammar OptBlock;

// Python-trial regression (#52): an optional block whose only named element is
// itself optional — ('(' Arglist=arglist? ')')? in Python's classdef,
// (',' ('*' tfpdef)?)? in typedargslist — has no property that can stand in
// for its presence. Its terminals must stay state-driven, otherwise
// "class A:" unparses as "class A( ):" and "f(a, b)" gains a trailing comma.
// The trailing ';'? is literal-only sugar: an element built from scratch
// leaves it out, while ('(' bases=idList? ')')? has a property that may be
// set and keeps its present-by-default behaviour (#60).
unit: decls+=decl* EOF;

decl: 'class' name=ID ('(' bases=idList? ')')? ':' body=ID ';'? ;

idList: ids+=ID (',' ids+=ID)* (',' ('*' rest=ID)?)? ;

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
