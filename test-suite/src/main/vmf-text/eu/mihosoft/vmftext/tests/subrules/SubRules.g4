grammar SubRules;

subrules   :
             '(' INT (',' INT)* ')'
           | INT
           ;


INT: [0-9]+;