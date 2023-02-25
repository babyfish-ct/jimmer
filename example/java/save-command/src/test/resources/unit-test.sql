create table book_store(
    id identity(1, 1) not null,
    name varchar(50) not null,
    website varchar(100)
);
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id identity(1, 1) not null,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint
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
    id identity(1, 1) not null,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null
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

create table tree_node(
    node_id identity(100, 1) not null,
    name varchar(20) not null,
    parent_id bigint
);
alter table tree_node
    add constraint business_key_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);
