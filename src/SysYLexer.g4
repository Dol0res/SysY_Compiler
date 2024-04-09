lexer grammar SysYLexer;

CONST : 'const';

INT : 'int';

DOUBLE : 'double';

VOID : 'void';

IF : 'if';

THEN : 'then';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';
IDENT   : ('_' | LETTER) ('_'|NUMBER|LETTER)* ;
INTEGER_CONST : '0'
                | [1-9]NUMBER*
;

WS: [ \r\n\t]+
   ->skip;

LINE_COMMENT: '//' .*? '\n' ->skip;

MULTILINE_COMMENT
   : '/*' .*? '*/'
   ->skip;


fragment LETTER : [a-zA-Z];
fragment NUMBER : [0-9];