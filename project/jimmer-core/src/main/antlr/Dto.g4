grammar Dto;

@header {
package org.babyfish.jimmer.dto;
}

// Parser --------

dto
    :
    (dtoTypes+=dtoType)*
    ;

dtoType
    :
    (
        modifier=Identifier name=Identifier
        |
        name=Identifier
    )
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
        '(' args+=Identifier (',' args+=Identifier)? ')'
    )?
    ;

explicitProp
    :
    positiveProp | negativeProp
    ;

positiveProp
    :
    '+'?
    (prop = Identifier | func = Identifier '(' prop = Identifier ')')
    ('as' alias=Identifier)?
    (dtoBody (recursive='*')?)?
    ;

negativeProp
    :
    '-' Identifier
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
    ('//' .*? ('\r\n' | '\r' | '\n')) -> skip
    ;