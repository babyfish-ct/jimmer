create table book_store(
    id bigserial not null,
    name text not null,
    website text,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter sequence book_store_id_seq restart with 100;
alter table book_store
    add constraint pk_book_store
        primary key(id)
;
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id bigserial not null,
    name text not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint,
    tenant text not null,
    created_time timestamp not null,
    modified_time timestamp not null 
);
alter sequence book_id_seq restart with 100;
alter table book
    add constraint pk_book
        primary key(id)
;
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
    id bigserial not null,
    first_name text not null,
    last_name text not null,
    gender char(1) not null,
    created_time timestamp not null,
    modified_time timestamp not null 
);
alter sequence author_id_seq restart with 100;
alter table author
    add constraint pk_author
        primary key(id)
;
alter table author
    add constraint business_key_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check (gender in('M', 'F'));

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

create table tree_node(
    node_id bigserial not null,
    name text not null,
    parent_id bigint,
    created_time timestamp not null,
    modified_time timestamp not null 
);
alter sequence tree_node_node_id_seq restart with 100;
alter table tree_node
    add constraint pk_tree_node
        primary key(node_id)
;
alter table tree_node
    add constraint business_key_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);

insert into book_store(id, name, created_time, modified_time) values
    (1, 'O''REILLY', now(), now()),
    (2, 'MANNING', now(), now())
;

insert into book(id, name, edition, price, store_id, tenant, created_time, modified_time) values
    (1, 'Learning GraphQL', 1, 50, 1, 'a', now(), now()),
    (2, 'Learning GraphQL', 2, 55, 1, 'b', now(), now()),
    (3, 'Learning GraphQL', 3, 51, 1, 'a', now(), now()),

    (4, 'Effective TypeScript', 1, 73, 1, 'b', now(), now()),
    (5, 'Effective TypeScript', 2, 69, 1, 'a', now(), now()),
    (6, 'Effective TypeScript', 3, 88, 1, 'b', now(), now()),

    (7, 'Programming TypeScript', 1, 47.5, 1, 'a', now(), now()),
    (8, 'Programming TypeScript', 2, 45, 1, 'b', now(), now()),
    (9, 'Programming TypeScript', 3, 48, 1, 'a', now(), now()),

    (10, 'GraphQL in Action', 1, 80, 2, 'b', now(), now()),
    (11, 'GraphQL in Action', 2, 81, 2, 'a', now(), now()),
    (12, 'GraphQL in Action', 3, 80, 2, 'b', now(), now())
;

insert into author(id, first_name, last_name, gender, created_time, modified_time) values
    (1, 'Eve', 'Procello', 'F', now(), now()),
    (2, 'Alex', 'Banks', 'M', now(), now()),
    (3, 'Dan', 'Vanderkam', 'M', now(), now()),
    (4, 'Boris', 'Cherny', 'M', now(), now()),
    (5, 'Samer', 'Buna', 'M', now(), now())
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

insert into tree_node(
    node_id, name, parent_id, created_time, modified_time
) values
    (1, 'Home', null, now(), now()),
        (2, 'Food', 1, now(), now()),
            (3, 'Drinks', 2, now(), now()),
                (4, 'Coca Cola', 3, now(), now()),
                (5, 'Fanta', 3, now(), now()),
            (6, 'Bread', 2, now(), now()),
                (7, 'Baguette', 6, now(), now()),
                (8, 'Ciabatta', 6, now(), now()),
        (9, 'Clothing', 1, now(), now()),
            (10, 'Woman', 9, now(), now()),
                (11, 'Casual wear', 10, now(), now()),
                    (12, 'Dress', 11, now(), now()),
                    (13, 'Miniskirt', 11, now(), now()),
                    (14, 'Jeans', 11, now(), now()),
                (15, 'Formal wear', 10, now(), now()),
                    (16, 'Suit', 15, now(), now()),
                    (17, 'Shirt', 15, now(), now()),
            (18, 'Man', 9, now(), now()),
                (19, 'Casual wear', 18, now(), now()),
                    (20, 'Jacket', 19, now(), now()),
                    (21, 'Jeans', 19, now(), now()),
                (22, 'Formal wear', 18, now(), now()),
                    (23, 'Suit', 22, now(), now()),
                    (24, 'Shirt', 22, now(), now())
;

alter system set wal_level = 'logical';
alter table book_store replica identity full;
alter table book replica identity full;
alter table author replica identity full;
alter table book_author_mapping replica identity full;
alter table tree_node replica identity full;
