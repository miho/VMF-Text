grammar RuleInfo;

root: classes1+=ruleClass1+ EOF;

ruleClass1: ('[' ']'|'[]');

WS : [ \t\n\r] + -> channel(HIDDEN)
   ;
