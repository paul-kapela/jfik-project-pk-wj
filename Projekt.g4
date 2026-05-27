grammar Projekt;

code: ( (stat|functionDef)? NEWLINE )* stat?
;
//    | block

stat: READ expr          #read
    | WRITE expr         #write
    | lvalue EQUALS expr #assign
    | value              #valueCall 
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
     | ID LPAREN argList? RPAREN #functionCall
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

funType: VOID   #voidType
        | type  #typedReturn
        ;

returnStat: RETURN expr
        |  RETURN
        ;

param: type ID
    ;

paramList: param (COMMA param)*
    ;

argList: expr (COMMA expr)*
    ;

block: LBRACE NEWLINE (stat NEWLINE*)* returnStat NEWLINE RBRACE
    ;

functionDef: FUN funType ID LPAREN paramList? RPAREN block
    ;

READ: 'read';

WRITE: 'write';

NEW: 'new';

EQUALS: '=';

ADD: '+';

SUBTRACT: '-';

MULTIPLY: '*';

DIVIDE: '/';

COMMA: ',';

LBRAC: '[';

RBRAC: ']';

LBRACE: '{';

RBRACE: '}';

LPAREN: '(';

RPAREN: ')';

VOID: 'void';

FUN: 'fun';

RETURN: 'return';

ID: [a-zA-Z][a-zA-Z0-9]*;

INT: [0-9]+;

REAL: [0-9]+ '.' [0-9]+ 'f';

REALD: [0-9]+ '.' [0-9]+;

STRING: '"'~["\r\n]*'"';

NEWLINE: '\r'? '\n';

WS: [ \t]+ -> skip;
