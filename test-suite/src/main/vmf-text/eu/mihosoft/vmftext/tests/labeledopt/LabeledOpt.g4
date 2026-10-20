grammar LabeledOpt;

// Python-trial regression (#53): an optional literal inside a second (or
// later) labeled alternative. The parser recorded its presence under the
// alternative's index in the rule (/r1/a1/...), the unparser — for which a
// labeled alternative is a rule of its own — looked under /r1/a0/..., so the
// literal was always taken for absent: "[a,];" unparsed as "[a];".
unit: items+=item* EOF;

item
    : name=ID ';'                 # Plain
    | '[' name=ID ','? ']' ';'    # Bracketed
    | '{' name=ID ('=' value=ID)? ';'? '}'  # Braced
    ;

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
