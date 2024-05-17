parser grammar SysYParser;

options {
    tokenVocab = SysYLexer; //调用Lexer
}

program
   : compUnit
   ;
compUnit
   : (funcDef | decl)+ EOF
   ;
decl
    :constDecl | varDecl
    ;
constDecl
    :CONST bType constDef (COMMA constDef)* SEMICOLON
    ;
bType
    :INT
    ;
constDef
    :IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal
    ;
constInitVal
    :constExp
    | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE
    ;
constExp
    :exp
    ;
exp: L_PAREN exp R_PAREN # parenExp
	| lVal # lValExp
	| number # numExp
	| IDENT L_PAREN funcRParams? R_PAREN # funcCallExp
	| unaryOp exp # unaryExp
	| exp (MUL | DIV | MOD) exp # mulExp
	| exp (PLUS | MINUS) exp # addExp
	;
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
varDecl
   :bType varDef (COMMA varDef)* SEMICOLON
   ;
varDef
   :IDENT (L_BRACKT constExp R_BRACKT)* | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal
   ;
initVal
    :exp
    | L_BRACE (initVal (COMMA initVal)*)? R_BRACE
    ;
funcDef
    :funcType IDENT L_PAREN (funcFParams)? R_PAREN block
    ;
funcType
    :VOID | INT
    ;
funcFParams
    :funcFParam (COMMA funcFParam)*
    ;
funcFParam
    :bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?
    ;

block
    :L_BRACE (blockItem)* R_BRACE
    ;
blockItem
    :decl | stmt
    ;
stmt
    :lVal ASSIGN exp SEMICOLON
    |exp? SEMICOLON
    |block
    |IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
    |WHILE L_PAREN cond R_PAREN stmt
    |BREAK SEMICOLON
    |CONTINUE SEMICOLON
    |RETURN (exp)? SEMICOLON
    ;