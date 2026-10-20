grammar RuleInfo;

root: classes1+=ruleClass1+ EOF;

// The literal-only block is lifted into typed alternatives (#37), so '[' and ']'
// are mandatory within their alternative and NOT reported as optional any more;
// the trailing '!'? keeps a genuinely optional terminal in this grammar.
ruleClass1: ('[' ']'|'[]') '!'?;

WS : [ \t\n\r] + -> channel(HIDDEN)
   ;
