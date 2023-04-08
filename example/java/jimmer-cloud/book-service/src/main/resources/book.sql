drop table book_author_mapping if exists;
drop table book if exists;



create table book(
    id identity(100, 1) not null,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table book
    add constraint business_key_book
        unique(name, edition)
;

create table book_author_mapping(
    book_id bigint not null,
    author_id bigint not null
);
alter table book_author_mapping
    add constraint pk_book_author_mapping
        primary key(book_id, author_id)
;
alter table book_author_mapping
    add constraint fk_book_author_mapping__book
        foreign key(book_id)
            references book(id)
                on delete cascade
;

insert into book(id, name, edition, price, store_id, tenant, created_time, modified_time) values
    (1, 'Learning GraphQL', 1, 50, 1, 'a', current_timestamp(), current_timestamp()),
    (2, 'Learning GraphQL', 2, 55, 1, 'b', current_timestamp(), current_timestamp()),
    (3, 'Learning GraphQL', 3, 51, 1, 'a', current_timestamp(), current_timestamp()),

    (4, 'Effective TypeScript', 1, 73, 1, 'b', current_timestamp(), current_timestamp()),
    (5, 'Effective TypeScript', 2, 69, 1, 'a', current_timestamp(), current_timestamp()),
    (6, 'Effective TypeScript', 3, 88, 1, 'b', current_timestamp(), current_timestamp()),

    (7, 'Programming TypeScript', 1, 47.5, 1, 'a', current_timestamp(), current_timestamp()),
    (8, 'Programming TypeScript', 2, 45, 1, 'b', current_timestamp(), current_timestamp()),
    (9, 'Programming TypeScript', 3, 48, 1, 'a', current_timestamp(), current_timestamp()),

    (10, 'GraphQL in Action', 1, 80, 2, 'b', current_timestamp(), current_timestamp()),
    (11, 'GraphQL in Action', 2, 81, 2, 'a', current_timestamp(), current_timestamp()),
    (12, 'GraphQL in Action', 3, 80, 2, 'b', current_timestamp(), current_timestamp())
;

insert into book_author_mapping(book_id, author_id) values
    (1, 1),
    (2, 1),
    (3, 1),

    (1, 2),
    (2, 2),
    (3, 2),

    (4, 3),
    (5, 3),
    (6, 3),

    (7, 4),
    (8, 4),
    (9, 4),

    (10, 5),
    (11, 5),
    (12, 5)
;