grammar DotAsTerminal;

// the purpose of this rule is to check whether vmf-text handels a dot correctly
// mainRule: (myLabel+=.)*;


mainRuleWorking: myLabel=ANY;

MY_LEXER_RULE: 'abc';

ANY: .;