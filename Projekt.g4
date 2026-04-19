grammar Projekt;

code: ( stat? NEWLINE )* stat?
;
//    | block

stat: READ expr         #read
    | WRITE expr        #write
    | ID EQUALS expr    #assign
    ;

primalExpr: primalExpr MULTIPLY unaryExpr #mul
    | primalExpr DIVIDE unaryExpr         #div
    | unaryExpr                           #primal
    ;

expr: expr ADD primalExpr       #add
    | expr SUBTRACT primalExpr  #sub
    | primalExpr                #expr0
    ;

unaryExpr: SUBTRACT unaryExpr   #neg
    | value                     #val
    ;

value: ID           #id
    | INT           #int
    | REAL          #real
    | '(' expr ')'  #parentheses
    ;

READ: 'read';

WRITE: 'write';

EQUALS: '=';

ADD: '+';

SUBTRACT: '-';

MULTIPLY: '*';

DIVIDE: '/';

ID: [a-zA-Z][a-zA-Z0-9]*;

INT: [0-9]+;

REAL: [0-9]+ '.' [0-9]+;

NEWLINE: '\r'? '\n';

WS: [ \t]+ -> skip;
