grammar NestedUnnamed;

root : children1+=ruleWithOptionals1+
  (',' children2+=ruleWithOptionals2+)?
  (',' children3+=ruleWithOptionals3+)?
  (',' children4+=ruleWithOptionals4+)?
  (',' children5+=ruleWithOptionals5+)?
     ;

ruleWithOptionals1 : 'r1' ( '(' ')' )?;
ruleWithOptionals2 : 'r2' ('('  name=IDENTIFIER ')')?;
ruleWithOptionals3 : 'r3' ('('  name=IDENTIFIER? ')')?;
ruleWithOptionals4 : 'r4' ('(' (name=IDENTIFIER)? ')')?;
ruleWithOptionals5 : 'r5' ('(' (names+=IDENTIFIER)* ')')?;

IDENTIFIER : [a-zA-Z][a-zA-Z0-9]*;

WS : [ \t\n\r] + -> channel(HIDDEN)
   ;