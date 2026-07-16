grammar MiniJson;

json: value=array EOF;
array
   : '[' values+=num (',' values+=num)* ']'
   | '[' ']'
   ;
num: value=NUMBER ;
NUMBER : '-'? [0-9]+ ('.' [0-9]+)? ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  NUMBER -> java.lang.Double via 'java.lang.Double.parseDouble(entry.getText())'
}
*/
