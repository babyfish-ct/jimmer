drop table if exists book_store;
drop table if exists book;
drop table if exists author;
drop table if exists book_author_mapping;
drop table if exists tree_node;
drop table if exists department;
drop table if exists employee;

create table book_store
(
    id      text not null
        constraint image_pk primary key,
    name    text not null
        constraint business_key_book_store unique,
    website text,
    version int  not null
);

create table book
(
    id       text
        constraint pk_book primary key not null,
    name     text                      not null,
    edition  integer                   not null,
    price    numeric(10, 2)            not null,
    store_id text
        constraint fk_book__book_store references book_store (id),
    constraint business_key_book unique (name, edition)
);

create table author
(
    id         text
        constraint pk_author primary key                         not null,
    first_name text                                              not null,
    last_name  text                                              not null,
    gender     char(1)
        constraint ck_author_gender check (gender in ('M', 'F')) not null,
    constraint business_key_author unique (first_name, last_name)
);

create table book_author_mapping
(
    book_id   text not null,
    author_id text not null,
    constraint pk_book_author_mapping primary key (book_id, author_id),
    constraint fk_book_author_mapping__book foreign key (book_id) references book (id) on delete cascade,
    constraint fk_book_author_mapping__author foreign key (author_id) references author (id) on delete cascade
);

create table tree_node
(
    node_id   integer
        constraint pk_tree_node primary key not null,
    name      text                          not null,
    parent_id integer
        constraint fk_tree_node__parent references tree_node (node_id),
    constraint business_key_tree_node unique (parent_id, name)
);

insert into book_store(id, name, version)
values ('d38c10da-6be8-4924-b9b9-5e81899612a0', 'O''REILLY', 0),
       ('2fa3955e-3e83-49b9-902e-0465c109c779', 'MANNING', 0)
;

insert into book(id, name, edition, price, store_id)
values ('e110c564-23cc-4811-9e81-d587a13db634', 'Learning GraphQL', 1, 50, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('b649b11b-1161-4ad2-b261-af0112fdd7c8', 'Learning GraphQL', 2, 55, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('64873631-5d82-4bae-8eb8-72dd955bfc56', 'Learning GraphQL', 3, 51, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),

       ('8f30bc8a-49f9-481d-beca-5fe2d147c831', 'Effective TypeScript', 1, 73, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('8e169cfb-2373-4e44-8cce-1f1277f730d1', 'Effective TypeScript', 2, 69, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('9eded40f-6d2e-41de-b4e7-33a28b11c8b6', 'Effective TypeScript', 3, 88, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),

       ('914c8595-35cb-4f67-bbc7-8029e9e6245a', 'Programming TypeScript', 1, 47.5,
        'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('058ecfd0-047b-4979-a7dc-46ee24d08f08', 'Programming TypeScript', 2, 45,
        'd38c10da-6be8-4924-b9b9-5e81899612a0'),
       ('782b9a9d-eac8-41c4-9f2d-74a5d047f45a', 'Programming TypeScript', 3, 48,
        'd38c10da-6be8-4924-b9b9-5e81899612a0'),

       ('a62f7aa3-9490-4612-98b5-98aae0e77120', 'GraphQL in Action', 1, 80, '2fa3955e-3e83-49b9-902e-0465c109c779'),
       ('e37a8344-73bb-4b23-ba76-82eac11f03e6', 'GraphQL in Action', 2, 81, '2fa3955e-3e83-49b9-902e-0465c109c779'),
       ('780bdf07-05af-48bf-9be9-f8c65236fecc', 'GraphQL in Action', 3, 80, '2fa3955e-3e83-49b9-902e-0465c109c779')
;

insert into author(id, first_name, last_name, gender)
values ('fd6bb6cf-336d-416c-8005-1ae11a6694b5', 'Eve', 'Procello', 'F'),
       ('1e93da94-af84-44f4-82d1-d8a9fd52ea94', 'Alex', 'Banks', 'M'),
       ('c14665c8-c689-4ac7-b8cc-6f065b8d835d', 'Dan', 'Vanderkam', 'M'),
       ('718795ad-77c1-4fcf-994a-fec6a5a11f0f', 'Boris', 'Cherny', 'M'),
       ('eb4963fd-5223-43e8-b06b-81e6172ee7ae', 'Samer', 'Buna', 'M')
;

insert into book_author_mapping(book_id, author_id)
values ('e110c564-23cc-4811-9e81-d587a13db634', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5'),
       ('b649b11b-1161-4ad2-b261-af0112fdd7c8', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5'),
       ('64873631-5d82-4bae-8eb8-72dd955bfc56', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5'),

       ('e110c564-23cc-4811-9e81-d587a13db634', '1e93da94-af84-44f4-82d1-d8a9fd52ea94'),
       ('b649b11b-1161-4ad2-b261-af0112fdd7c8', '1e93da94-af84-44f4-82d1-d8a9fd52ea94'),
       ('64873631-5d82-4bae-8eb8-72dd955bfc56', '1e93da94-af84-44f4-82d1-d8a9fd52ea94'),

       ('8f30bc8a-49f9-481d-beca-5fe2d147c831', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d'),
       ('8e169cfb-2373-4e44-8cce-1f1277f730d1', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d'),
       ('9eded40f-6d2e-41de-b4e7-33a28b11c8b6', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d'),

       ('914c8595-35cb-4f67-bbc7-8029e9e6245a', '718795ad-77c1-4fcf-994a-fec6a5a11f0f'),
       ('058ecfd0-047b-4979-a7dc-46ee24d08f08', '718795ad-77c1-4fcf-994a-fec6a5a11f0f'),
       ('782b9a9d-eac8-41c4-9f2d-74a5d047f45a', '718795ad-77c1-4fcf-994a-fec6a5a11f0f'),

       ('a62f7aa3-9490-4612-98b5-98aae0e77120', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae'),
       ('e37a8344-73bb-4b23-ba76-82eac11f03e6', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae'),
       ('780bdf07-05af-48bf-9be9-f8c65236fecc', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae')
;

insert into tree_node(node_id, name, parent_id)
values (1, 'Home', null),
       (2, 'Food', 1),
       (3, 'Drinks', 2),
       (4, 'Coca Cola', 3),
       (5, 'Fanta', 3),
       (6, 'Bread', 2),
       (7, 'Baguette', 6),
       (8, 'Ciabatta', 6),
       (9, 'Clothing', 1),
       (10, 'Woman', 9),
       (11, 'Casual wear', 10),
       (12, 'Dress', 11),
       (13, 'Miniskirt', 11),
       (14, 'Jeans', 11),
       (15, 'Formal wear', 10),
       (16, 'Suit', 15),
       (17, 'Shirt', 15),
       (18, 'Man', 9),
       (19, 'Casual wear', 18),
       (20, 'Jacket', 19),
       (21, 'Jeans', 19),
       (22, 'Formal wear', 18),
       (23, 'Suit', 22),
       (24, 'Shirt', 22)
;

create table department
(
    name           varchar(20)                             not null,
    deleted_millis integer                                 not null default 0,
    id             integer
    constraint pk_department primary key autoincrement not null,
    constraint uq_department unique (name, deleted_millis)
);

create table employee
(
    name           varchar(20)                                not null,
    gender         char(1)
    constraint ck_employee check ( gender in ('M', 'F') ) not null,
    department_id  integer,
    deleted_millis integer                                    not null default 0,
    id             integer
    constraint pk_employee primary key autoincrement      not null,
    constraint uq_employee unique (name, deleted_millis)
);

insert into department(id, name)
values (1, 'Market');
insert into employee(id, name, gender, department_id)
values (1, 'Sam', 'M', 1);
insert into employee(id, name, gender, department_id)
values (2, 'Jessica', 'F', 1);
