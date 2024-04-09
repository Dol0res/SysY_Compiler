parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;//注意使用该语句指定词法分析器；请不要修改词法分析器或语法分析器的文件名，否则Makefile可能无法正常工作，影响评测结果
}

program
   : compUnit
   ;

compUnit
   : (funcDef | decl)+ EOF
   ;

decl : constDecl | varDecl;

constDecl : 'const' bType constDef ( ',' constDef )* ';';

bType : 'int';

constDef :
 IDENT ( '[' constExp ']' )* '=' constInitVal ;

constInitVal :
 constExp
| '{' ( constInitVal ( ',' constInitVal )* )? '}';

varDecl :
 bType varDef ( ',' varDef )* ';' ;

varDef :
 IDENT ( '[' constExp ']' )*
| IDENT ( '[' constExp ']' )* '=' initVal ;

initVal :
 exp | '{' ( initVal ( ',' initVal )* )? '}' ;

funcDef :
 funcType IDENT '(' (funcFParams)? ')' block ;

funcType :
 'void' | 'int' ;

funcFParams :
 funcFParam ( ',' funcFParam )* ;

funcFParam :
 bType IDENT ('[' ']' ( '[' exp ']' )*)? ;

 block :
  '{' ( blockItem )* '}' ;

 blockItem :
  decl | stat;

stat : lVal '=' exp ';' | (exp?) ';' | block
    | 'if' '(' cond ')' stat ( 'else' stat )?
    | 'while' '(' cond ')' stat
    | 'break' ';' | 'continue' ';'
    | 'return' (exp)? ';'
    ;

exp
   : L_PAREN exp R_PAREN
   | lVal
   | number
   | funcCall
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   ;

funcCall:IDENT L_PAREN funcRParams? R_PAREN;

cond
   : exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGER_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;
