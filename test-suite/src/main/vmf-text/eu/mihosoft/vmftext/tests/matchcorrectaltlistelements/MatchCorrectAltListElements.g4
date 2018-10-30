grammar MatchCorrectAltListElements;

root: ruleOne=rule1* ruleTwo=rule2*;

rule1: elements+='[' elements+=']' | elements+='[]';

rule2: elements+='[]' | elements+='[' elements+=']';

WS
:   [ \r\t\n]+ -> channel(HIDDEN)
;