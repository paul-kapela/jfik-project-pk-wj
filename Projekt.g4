grammar Projekt;

code: ( expr? NEWLINE )* expr?
;
//    | block

primalExpr: primalExpr MULTIPLY unaryExpr
    | primalExpr DIVIDE unaryExpr
    | unaryExpr
    ;

expr: expr ADD primalExpr
    | expr SUBTRACT primalExpr
    | primalExpr
    ;

unaryExpr: SUBTRACT unaryExpr
    | value
    ;

value: ID
    | INT
    | REAL
    | '(' expr ')'
    ;

ID: [a-zA-Z][a-zA-Z0-9]*;

INT: [0-9]+;

REAL: [0-9]+ '.' [0-9]+;

READ: 'read';

WRITE: 'write';

ADD: '+';

SUBTRACT: '-';

MULTIPLY: '*';

DIVIDE: '/';

NEWLINE: '\r'? '\n';

WS: [ \t]+ -> skip;