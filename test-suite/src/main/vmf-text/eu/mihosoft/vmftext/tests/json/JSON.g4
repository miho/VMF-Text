/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from http://json.org
grammar JSON;

json
   : value=val
   ;
obj
   : '{' pairs+=pair (',' pairs+=pair)* '}'
   | '{' '}'
   ;
pair
   : key=STRING ':' value=val
   ;
array
   : '[' values+=val (',' values+=val)* ']'
   | '[' ']'
   ;
val
   : value=STRING     # stringValue
   | value=NUMBER     # numberValue
   | value=obj        # objectValue
   | value=array      # arrayValue
   | value=BOOLEAN    # booleanValue
   | 'null'           # nullValue
   ;
STRING
   : '"' (ESC | SAFECODEPOINT)* '"'
   ;
fragment ESC
   : '\\' (["\\/bfnrt] | UNICODE)
   ;
fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;
fragment HEX
   : [0-9a-fA-F]
   ;
fragment SAFECODEPOINT
   : ~ ["\\\u0000-\u001F]
   ;

NUMBER
   : '-'? INT ('.' [0-9] +)? EXP?
   ;
BOOLEAN
   : 'true'
   | 'false'
   ;
fragment INT
   : '0' | [1-9] [0-9]*
   ;
// no leading zeros
fragment EXP
   : [Ee] [+\-]? INT
   ;
// \- since - means "range" inside [...]
WS
   : [ \t\n\r] + -> channel(HIDDEN)
   ;

/*<!vmf-text!>

TypeMap() {

  (rule: NUMBER  -> type: java.lang.Double) = (
      toType:   'java.lang.Double.parseDouble(entry.getText())',
      toString: 'entry.toString()'
  )

  (rule:BOOLEAN   -> type: java.lang.Boolean ) = (
      toType:   'java.lang.Boolean.parseBoolean(entry.getText())',
      toString: 'entry.toString()'
  )

  (rule: STRING -> type: java.lang.String) = {
        toType:   'entry.getText().isEmpty()?"\"\"":entry.getText().substring(1,entry.getText().length()-1)',
        toString: 'entry.toString().trim().startsWith("\"")?entry:"\""+entry+"\""'
  }

}

*/

/*<!vmf-text!>

//@InterfaceOnly
interface Val {

    @GetterOnly
    Object getValue();

}

interface StringValue {
    String getValue();
}

interface NumberValue {
    Double getValue();
}

interface BooleanValue {
    Boolean getValue();
}

interface ObjectValue {
    Obj getValue();
}

interface ArrayValue {
    Array getValue();
}

interface NullValue {
    Object getValue();
}

*/