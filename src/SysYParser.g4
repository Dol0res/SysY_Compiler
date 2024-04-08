parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;//注意使用该语句指定词法分析器；请不要修改词法分析器或语法分析器的文件名，否则Makefile可能无法正常工作，影响评测结果
}

program
   : compUnit
   ;
compUnit
   : (functionDecl | varDecl)+ EOF
   ;
varDecl : type IDENT ('=' exp)? (',' IDENT ('=' exp)?)? ';' ;

type : INT | DOUBLE | VOID ;

functionDecl : type IDENT '(' funcRParams? ')' block ;
funcCall: IDENT '(' funcRParams? ')';
block : '{' stat* '}' ;

stat : block    # BlockStat
     | varDecl  # VarDeclStat
     | 'return' exp? ';'   # ReturnStat
     | exp '=' exp ';'    # AssignStat
     | exp ';' # ExprStat
     | loop #NeedTab
     ;

exp
   : L_PAREN exp R_PAREN
   | lVal
   | number
   | IDENT L_PAREN funcRParamsCall? R_PAREN
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   | funcCall
   ;

loop
    :'if' cond ('then' stat ('else' stat)? )?
    | 'while' cond? block
    | 'while' cond? stat
    ;

cond
   : exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   | L_PAREN cond R_PAREN
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
   : type IDENT
   ;
funcRParamsCall
   : exp (COMMA exp)*
   ;


constExp
   : exp
   ;