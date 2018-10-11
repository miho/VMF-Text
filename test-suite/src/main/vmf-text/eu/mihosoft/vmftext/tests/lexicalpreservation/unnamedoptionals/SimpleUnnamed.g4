grammar SimpleUnnamed;

root : children+=ruleWithOptionals*;

ruleWithOptionals : 'name' '=' name=IDENTIFIER ';'?;

IDENTIFIER : [a-zA-Z][a-zA-Z0-9]*;

WS : [ \t\n\r] + -> channel(HIDDEN)
   ;