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
    ((explicitProps += explicitProp) (',' | ';')?)*
    '}'
    ;

explicitProp
    :
    allScalars | aliasGroup | positiveProp | negativeProp
    ;

allScalars
    :
    '#' name=Identifier
    (
        '(' args+=qualifiedName (',' args+=qualifiedName)* ')'
    )?
    ;

aliasGroup
    :
    pattern = aliasPattern '{' (props += aliasGroupProp)* '}'
    ;

aliasPattern
    :
    'as' '('
    (prefix = '^')?
    (original = Identifier)?
    (suffix = '$')?
    (translator = '->')
    (replacement = Identifier)?
    ')'
    ;

aliasGroupProp
    :
    allScalars | positiveProp
    ;

positiveProp
    :
    '+'?
    (func = Identifier '(' prop = Identifier ')' | prop = Identifier)
    (optional = '?')?
    ('as' alias=Identifier)?
    (dtoBody (recursive='*')?)?
    ;

negativeProp
    :
    '-' prop = Identifier
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
    ('//' ~[\r\n]* ('\r\n' | '\r' | '\n')?) -> skip
    ;