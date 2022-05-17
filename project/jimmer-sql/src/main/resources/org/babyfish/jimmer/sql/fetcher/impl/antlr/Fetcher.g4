grammar Fetcher;

fetcher
    :
    '{' field+ '}'
    ;

field
    :
    '*' |
    '-' IDENTIFIER |
    '+'? positiveField
    ;

positiveField
    :
    IDENTIFIER arguments? fetcher?
    ;

arguments
    :
    ':' '{' argument (',' argument)* '}'
    ;

argument
    :
    IDENTIFIER ':' INTEGER
    ;

IDENTIFIER
    :
    Letter LetterOrDigit*
    ;

INTEGER
    :
    [0-9]+
    ;

fragment LetterOrDigit
    :
    Letter
    | [0-9]
    ;

fragment Letter
    : [a-zA-Z$_]
    | ~[\u0000-\u007F\uD800-\uDBFF]
    | [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

WS
    :
    [ \t\r\n\u000C]+ -> channel(HIDDEN)
    ;

COMMENT
    :
    '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    :
    '//' ~[\r\n]*
    -> channel(HIDDEN)
    ;