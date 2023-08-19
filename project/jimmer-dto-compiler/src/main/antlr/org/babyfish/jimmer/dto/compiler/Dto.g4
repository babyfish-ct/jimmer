grammar Dto;

@header {
package org.babyfish.jimmer.dto.compiler;
}

// Parser --------

dto
    :
    (importStatements += importStatement)*
    (dtoTypes+=dtoType)*
    EOF
    ;

importStatement
    :
    'import' parts += Identifier ('.' parts += Identifier)*
    (
        '.' '{' importedTypes += importedType (',' importedTypes += importedType)* '}' |
        'as' alias = Identifier
    )?
    ;

importedType
    :
    name = Identifier ('as' alias = Identifier)?
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
    allScalars | aliasGroup | positiveProp | negativeProp | userProp
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

userProp
    :
    prop = Identifier ':' typeRef
    ;

typeRef
    :
    qualifiedName
    ('<' genericArguments += genericArgument (',' genericArguments += genericArgument)? '>')?
    (optional = '?')?
    ;

genericArgument
    :
    wildcard = '*' |
    (modifier = Identifier)? typeRef
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