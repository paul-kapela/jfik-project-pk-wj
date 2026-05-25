grammar Projekt;

code: block
    ;

block: ( stat? NEWLINE )* stat?
    ;

stat: IF cond THEN blockIf
      elseIfClause*
      elseClause?
      ENDIF                      #if
    | READ expr                  #read
    | WRITE expr                 #write
    | lvalue EQUALS expr         #assign
    ;

blockIf: block
    ;

elseIfClause: ELSEIF cond THEN blockIf
    ;

elseClause: ELSE blockIf
    ;

lvalue: ID              #idLval
      | ID '[' expr ']' #indexLval
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

value: ID                   #id
     | INT                  #int
     | REAL                 #real
     | REALD                #reald
     | '(' expr ')'         #parentheses
     | STRING               #string
     | ID LBRAC expr RBRAC  #indexRval
     | arrayLiteral         #arrayLit
     | arrayInit            #arrayInitVal
     ;

arrayLiteral: LBRAC (expr (COMMA expr)*)? RBRAC #lit
     ;

arrayInit: NEW type LBRAC expr RBRAC #init
     ;

type: 'int' #tInt
    | 'real' #tReal
    | 'double' #tDouble
    | 'string' #tString
    ;

cond: expr condOp expr
    ;

condOp: EQ   #condEq
      | NEQ  #condNeq
      | LTE  #condLte
      | GTE  #condGte
      | LT   #condLt
      | GT   #condGt
      ;

READ: 'read';

WRITE: 'write';

NEW: 'new';

IF: 'if';

THEN: 'then';

ELSEIF: 'elseif';

ELSE: 'else';

ENDIF: 'endif';

EQ: '==';

EQUALS: '=';

ADD: '+';

SUBTRACT: '-';

MULTIPLY: '*';

DIVIDE: '/';

COMMA: ',';

LBRAC: '[';

RBRAC: ']';

NEQ: '!=';

LTE: '<=';

GTE: '>=';

LT: '<';

GT: '>';

ID: [a-zA-Z][a-zA-Z0-9]*;

INT: [0-9]+;

REAL: [0-9]+ '.' [0-9]+ 'f';

REALD: [0-9]+ '.' [0-9]+;

STRING: '"'~["\r\n]*'"';

NEWLINE: '\r'? '\n';

WS: [ \t]+ -> skip;
