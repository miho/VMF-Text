grammar NotOp;


program: r1=rule1 | r2=rule2 | r3=rule3 EOF;

rule1: (content=~ABC 'altOne' | content=~DEF 'altTwo') 'r1'?;

rule2: content=ABC 'altOne' 'r2'? | content=~ABC 'altTwo' 'r2'?;

rule3: content2=~'notthis' 'altOneR3';

ABC: [abc]+;

DEF: [def]+;

STR: [a-z|0-9]+;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;