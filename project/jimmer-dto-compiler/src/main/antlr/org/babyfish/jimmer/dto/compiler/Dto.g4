grammar Dto;

@header {
package org.babyfish.jimmer.dto.compiler;
}

// Parser --------

dto
    :
    exportStatement?
    (importStatements += importStatement)*
    (dtoTypes+=dtoType)*
    EOF
    ;

exportStatement
    :
    'export' typeParts += Identifier ('.' typeParts += Identifier)*
    ('->' 'package' packageParts += Identifier ('.' packageParts += Identifier)*)?
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
    (doc = DocComment)?
    (annotations += annotation)*
    (modifiers += (Identifier | 'fixed' | 'static' | 'dynamic' | 'fuzzy'))*
    name=Identifier
    ('implements' superInterfaces += typeRef (',' superInterfaces += typeRef)*)?
    body=dtoBody
    ;

dtoBody
    :
    '{'
    (macros += macro)*
    ((explicitProps += explicitProp) (',' | ';')?)*
    '}'
    ;

explicitProp
    :
    aliasGroup | positiveProp | negativeProp | userProp
    ;

macro
    :
    '#' name = Identifier
    ('(' args+=qualifiedName (',' args+=qualifiedName)* ')')?
    (optional = '?' | required = '!')?
    ;

aliasGroup
    :
    pattern = aliasPattern '{' (macros += macro)* (props += positiveProp)* '}'
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

positiveProp
    :
    (doc = DocComment)?
    (configurations += configuration | annotations += annotation)*
    '+'?
    (modifier = ('fixed' | 'static' | 'dynamic' | 'fuzzy'))?
    (
        (func = Identifier | func = 'null')
        (flag = '/' (insensitive = Identifier)? (prefix = '^')? (suffix = '$')?)?
        '(' props += Identifier (',' props += Identifier)* ','? ')'
        |
        props += Identifier
    )
    (optional = '?' | required = '!' | recursive = '*')?
    ('as' alias=Identifier)?
    (
        (childDoc = DocComment)?
        (bodyAnnotations += annotation)*
        ('implements' bodySuperInterfaces += typeRef (',' bodySuperInterfaces += typeRef)*)?
        dtoBody
        |
        '->' enumBody
    )?
    ;

negativeProp
    :
    '-' prop = Identifier
    ;

userProp
    :
    (doc = DocComment)?
    (annotations += annotation)*
    prop = Identifier ':' typeRef
    (
        '='
        (defaultMinus = '-')?
        defaultValue = (BooleanLiteral | IntegerLiteral | StringLiteral | FloatingPointLiteral | 'null')
    )?
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

configuration
    :
    where
    |
    orderBy
    |
    filter
    |
    recursion
    |
    fetchType
    |
    limit
    |
    batch
    |
    recursionDepth
    ;

where
    :
    '!where' '(' predicate ')'
    ;

predicate
    :
    subPredicates += andPredicate ('or' subPredicates += andPredicate)*
    ;

andPredicate
    :
    subPredicates += atomPredicate ('and' subPredicates += atomPredicate)*
    ;

atomPredicate
    :
    '(' predicate ')'
    |
    cmpPredicate
    |
    nullityPredicate
    ;

cmpPredicate
    :
    left = propPath
    (
        op = '=' right = propValue
        |
        op = '<>' right = propValue
        |
        op = '!=' right = propValue
        |
        op = '<' right = propValue
        |
        op = '<=' right = propValue
        |
        op = '>' right = propValue
        |
        op = '>=' right = propValue
        |
        op = Identifier right = propValue
    )
    ;

nullityPredicate
    :
    propPath 'is' (not = 'not')? 'null'
    ;

propPath
    :
    parts += Identifier ('.' parts += Identifier)*
    ;

propValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringToken = SqlStringLiteral |
    (negative = '-')?  integerToken = IntegerLiteral |
    (negative = '-')?  floatingPointToken = FloatingPointLiteral |
    ;

orderBy
    :
    '!orderBy' '(' items += orderByItem (',' items += orderByItem)* ')'
    ;

orderByItem
    :
    propPath (orderMode = Identifier)?
    ;

filter
    :
    '!filter' '(' qualifiedName ')'
    ;

recursion
    :
    '!recursion' '(' qualifiedName ')'
    ;

fetchType
    :
    '!fetchType' '(' fetchMode = Identifier ')'
    ;

limit
    :
    '!limit' '(' limitArg = IntegerLiteral (',' offsetArg = IntegerLiteral)? ')'
    ;

batch
    :
    '!batch' '(' IntegerLiteral ')'
    ;

recursionDepth
    :
    '!depth' '(' IntegerLiteral ')'
    ;

annotation
    :
    '@' typeName = qualifiedName ('(' annotationArguments? ')')?
    ;

annotationArguments
    :
    defaultArgument = annotationValue (',' namedArguments += annotationNamedArgument)*
    |
    namedArguments += annotationNamedArgument (',' namedArguments += annotationNamedArgument)*
    ;

annotationNamedArgument
    :
    name = Identifier '=' value = annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringTokens += StringLiteral ('+' stringTokens += StringLiteral)* |
    (negative = '-')? integerToken = IntegerLiteral |
    (negative = '-')? floatingPointToken = FloatingPointLiteral |
    qualifiedPart = qualifiedName classSuffix? |
    annotationPart = annotation |
    nestedAnnotationPart = nestedAnnotation
    ;

annotationArrayValue
    :
    '{' elements += annotationSingleValue (',' elements += annotationSingleValue)* '}'
    |
    '[' elements += annotationSingleValue (',' elements += annotationSingleValue)* ']'
    ;

nestedAnnotation
    :
    typeName = qualifiedName '(' annotationArguments? ')'
    ;

enumBody
    :
    '{' (mappings += enumMapping (','|';')?)+ '}'
    ;

enumMapping
    :
    constant = Identifier ':'
    (
        value = StringLiteral | (negative = '-')? value = IntegerLiteral
    )
    ;

classSuffix
    :
    '?'? ('.' | '::') 'class'
    ;

// Lexer --------

WhiteSpace
    :
    (' ' | '\u0009' | '\u000C' | '\r' | '\n') -> channel(HIDDEN)
    ;

DocComment
    :
    ('/**' .*? '*/')
    ;

BlockComment
    :
    ('/*' .*? '*/') -> channel(HIDDEN)
    ;

LineComment
    :
    ('//' ~[\r\n]* ('\r\n' | '\r' | '\n')?) -> channel(HIDDEN)
    ;

SqlStringLiteral
    :
    '\'' ( ~'\'' | '\'\'' )* '\''
    ;

BooleanLiteral
    :
    'true' | 'false'
    ;

CharacterLiteral
	:
	'\'' SingleCharacter '\''
	|
	'\'' EscapeSequence '\''
	;

fragment
SingleCharacter
	:
	~['\\\r\n]
	;

StringLiteral
	:
	'"' StringCharacters? '"'
	;

fragment
StringCharacters
	:
	StringCharacter+
	;

fragment
StringCharacter
	:
	~["\\\r\n] | EscapeSequence
	;

fragment
EscapeSequence
	:
	'\\' [btnfr"'\\]
    |
    UnicodeEscape // This is not in the spec but prevents having to preprocess the input
    ;

fragment
UnicodeEscape
    :
    '\\' 'u'+  HexDigit HexDigit HexDigit HexDigit
    ;

fragment
HexDigit
    :
    [0-9] | [a-f] | [A-F]
    ;

IntegerLiteral
	:
	'0' | [1-9][0-9]*
	;

FloatingPointLiteral
    :
    [0-9]+ '.' [0-9]+
    ;

Identifier
    :
    [$A-Za-z_][$A-Za-z_0-9]*
    ;
