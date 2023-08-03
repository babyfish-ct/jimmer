grammar Dto;

@header {
package org.babyfish.jimmer.dto.compiler;
}

// Parser --------

dto
    :
    (dtoTypes+=dtoType)*
    EOF
    ;

dtoType
    :
    (modifiers += Identifier)*
    name=Identifier
    (':' superNames += Identifier (',' superNames += Identifier)*)?
    body=dtoBody
    ;

dtoBody
    :
    '{'
    macro?
    ((explicitProps+=explicitProp) (',' | ';')?)*
    '}'
    ;

macro
    :
    '#' name=Identifier
    (
        '(' args+=qualifiedName (',' args+=qualifiedName)* ')'
    )?
    ;

explicitProp
    :
    positiveProp | negativeProp
    ;

positiveProp
    :
    '+'?
    (func = Identifier '(' prop = Identifier ')' | prop = Identifier)
    ('as' alias=Identifier)?
    (dtoBody (recursive='*')?)?
    ;

negativeProp
    :
    '-'
    (func = Identifier '(' prop = Identifier ')' | prop = Identifier)
    ;

qualifiedName
    :
    parts+=Identifier ('.' parts+=Identifier)*
    ;

// Lexer --------

Identifier
    :
    [$A-Za-z][$A-Za-z0-9]*
    ;

WhiteSpace
    :    (' ' | '\u0009' | '\u000C' | '\r' | '\n') -> skip
    ;

BlockComment
    :    ('/*' .*? '*/') -> skip
    ;

LineComment
    :
    ('//' .*? ('\r\n' | '\r' | '\n')?) -> skip
    ;