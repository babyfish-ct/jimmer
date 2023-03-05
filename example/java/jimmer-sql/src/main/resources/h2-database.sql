drop table tree_node if exists;
drop table book_author_mapping if exists;
drop table book if exists;
drop table author if exists;
drop table book_store if exists;

create table book_store(
    id identity(100, 1) not null,
    name varchar(50) not null,
    website varchar(100),
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

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
alter table book
    add constraint fk_book__book_store
        foreign key(store_id)
            references book_store(id)
;

create table author(
    id identity(100, 1) not null,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table author
    add constraint business_key_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check gender in ('M', 'F');

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
alter table book_author_mapping
    add constraint fk_book_author_mapping__author
        foreign key(author_id)
            references author(id)
                on delete cascade
;

insert into book_store(id, name, created_time, modified_time) values
    (1, 'O''REILLY', current_timestamp(), current_timestamp()),
    (2, 'MANNING', current_timestamp(), current_timestamp())
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

insert into author(id, first_name, last_name, gender, created_time, modified_time) values
    (1, 'Eve', 'Procello', 'F', current_timestamp(), current_timestamp()),
    (2, 'Alex', 'Banks', 'M', current_timestamp(), current_timestamp()),
    (3, 'Dan', 'Vanderkam', 'M', current_timestamp(), current_timestamp()),
    (4, 'Boris', 'Cherny', 'M', current_timestamp(), current_timestamp()),
    (5, 'Samer', 'Buna', 'M', current_timestamp(), current_timestamp())
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

create table tree_node(
    node_id identity(100, 1) not null,
    name varchar(20) not null,
    parent_id bigint,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table tree_node
    add constraint business_key_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);

insert into tree_node(
    node_id, name, parent_id, created_time, modified_time
) values
    (1, 'Home', null, current_timestamp(), current_timestamp()),
        (2, 'Food', 1, current_timestamp(), current_timestamp()),
            (3, 'Drinks', 2, current_timestamp(), current_timestamp()),
                (4, 'Coca Cola', 3, current_timestamp(), current_timestamp()),
                (5, 'Fanta', 3, current_timestamp(), current_timestamp()),
            (6, 'Bread', 2, current_timestamp(), current_timestamp()),
                (7, 'Baguette', 6, current_timestamp(), current_timestamp()),
                (8, 'Ciabatta', 6, current_timestamp(), current_timestamp()),
        (9, 'Clothing', 1, current_timestamp(), current_timestamp()),
            (10, 'Woman', 9, current_timestamp(), current_timestamp()),
                (11, 'Casual wear', 10, current_timestamp(), current_timestamp()),
                    (12, 'Dress', 11, current_timestamp(), current_timestamp()),
                    (13, 'Miniskirt', 11, current_timestamp(), current_timestamp()),
                    (14, 'Jeans', 11, current_timestamp(), current_timestamp()),
                (15, 'Formal wear', 10, current_timestamp(), current_timestamp()),
                    (16, 'Suit', 15, current_timestamp(), current_timestamp()),
                    (17, 'Shirt', 15, current_timestamp(), current_timestamp()),
            (18, 'Man', 9, current_timestamp(), current_timestamp()),
                (19, 'Casual wear', 18, current_timestamp(), current_timestamp()),
                    (20, 'Jacket', 19, current_timestamp(), current_timestamp()),
                    (21, 'Jeans', 19, current_timestamp(), current_timestamp()),
                (22, 'Formal wear', 18, current_timestamp(), current_timestamp()),
                    (23, 'Suit', 22, current_timestamp(), current_timestamp()),
                    (24, 'Shirt', 22, current_timestamp(), current_timestamp())
;