grammar CommaLift;

// JavaScript-trial regression (#64): grammars-v4 JavaScript's array literal,
// elementList: ','* arrayElement? (','+ arrayElement)* ','*  — the repeated
// literal ','+ inside the repeated block became one comma list shared by all
// iterations, and the unparser's first ','+ took every comma: [1, 2, 3] came
// back as [1,, 2]. Auto-labeling lifts such a block into a per-item rule, so
// each element keeps its own commas (elisions such as [1,,2] included).
unit: arrays+=array* EOF;

array: '[' elements ']' ;

elements: ','* element? (','+ element)* ','* ;

element: '...'? NUM ;

NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
