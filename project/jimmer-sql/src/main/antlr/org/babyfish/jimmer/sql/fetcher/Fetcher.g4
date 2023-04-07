grammar Fetcher;

@headers {
package org.babyfish.jimmer.sql.fetcher;
}

fetcher
    :
    entityType fetchBody
    ;

fetchBody
    :
    '{' field+ '}'
    ;

field
    :
    prop = Identifier
    ;

argument
    :
    (name = Identifier)
    ':'
    (value = 'true' | 'false' | '')
    ;

entityType
    :
    Identifier ('.' Identifier)*
    ;

Identifier
    :
    [A-Za-z][A-Za-z0-9]*
    ;

Number
    :

    ;

WS
    :
    [ \r\t\n]+ -> skip
    ;