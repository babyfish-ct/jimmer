drop table transform if exists;
drop table permission if exists;
drop table administrator_role_mapping if exists;
drop table role if exists;
drop table administrator_metadata if exists;
drop table administrator if exists;
drop table book_author_mapping if exists;
drop table author_country_mapping if exists;
drop table book if exists;
drop table author if exists;
drop table country if exists;
drop table book_store if exists;
drop table tree_node if exists;
drop table primitive if exists;
drop sequence tree_node_id_seq if exists;

create table book_store(
    id bigint not null,
    name varchar(50) not null,
    website varchar(100),
    version int not null
);
alter table book_store
    add constraint pk_book_store
        primary key(id)
;
alter table book_store
    add constraint uq_book_store
        unique(name)
;

create table book(
    id bigint not null,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint
);
alter table book
    add constraint pk_book
        primary key(id)
;
alter table book
    add constraint uq_book
        unique(name, edition)
;
alter table book
    add constraint fk_book__book_store
        foreign key(store_id)
            references book_store(id)
;

create table author(
    id bigint not null,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender varchar(6) not null
);
alter table author
    add constraint pk_author
        primary key(id)
;
alter table author
    add constraint uq_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check gender in ('M', 'F');

create table country(
    code varchar(10) not null,
    name varchar(50) not null
);
alter table country
    add constraint pk_country
        primary key(code);
alter table country
    add constraint uq_country
        unique(code);

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

create table author_country_mapping(
    author_id bigint not null,
    country_code varchar(10) not null
);

alter table author_country_mapping
    add constraint pk_author_country_mapping
        primary key(author_id, country_code);
alter table author_country_mapping
    add constraint fk_author_country_mapping__author
        foreign key(author_id)
            references author(id)
                on delete cascade;
alter table author_country_mapping
    add constraint fk_author_country_mapping__country
        foreign key(country_code)
            references country(code)
                on delete cascade;

create table primitive(
    id bigint not null,
    boolean_value boolean not null,
    boolean_ref boolean null,
    char_value char(1) not null,
    char_ref char(1) null,
    byte_value tinyint not null,
    byte_ref tinyint null,
    short_value smallint not null,
    short_ref smallint null,
    int_value int not null,
    int_ref int null,
    long_value bigint not null,
    long_ref bigint null,
    float_value float not null,
    float_ref float null,
    double_value double not null,
    double_ref double null
);

insert into book_store(id, name, version) values
    (1, 'O''REILLY', 0),
    (2, 'MANNING', 0)
;

insert into book(id, name, edition, price, store_id) values
    (1, 'Learning GraphQL', 1, 50, 1),
    (2, 'Learning GraphQL', 2, 55, 1),
    (3, 'Learning GraphQL', 3, 51, 1),

    (4, 'Effective TypeScript', 1, 73, 1),
    (5, 'Effective TypeScript', 2, 69, 1),
    (6, 'Effective TypeScript', 3, 88, 1),

    (7, 'Programming TypeScript', 1, 47.5, 1),
    (8, 'Programming TypeScript', 2, 45, 1),
    (9, 'Programming TypeScript', 3, 48, 1),

    (10, 'GraphQL in Action', 1, 80, 2),
    (11, 'GraphQL in Action', 2, 81, 2),
    (12, 'GraphQL in Action', 3, 80, 2)
;

insert into author(id, first_name, last_name, gender) values
    (1, 'Eve', 'Procello', 'F'),
    (2, 'Alex', 'Banks', 'M'),
    (3, 'Dan', 'Vanderkam', 'M'),
    (4, 'Boris', 'Cherny', 'M'),
    (5, 'Samer', 'Buna', 'M')
;

insert into country(code, name) values
    ('USA', 'The United States of America')
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

insert into author_country_mapping(author_id, country_code) values
    (1, 'USA'),
    (2, 'USA'),
    (3, 'USA'),
    (4, 'USA'),
    (5, 'USA');

create table tree_node(
    node_id bigint not null,
    name varchar(20) not null,
    parent_id bigint
);
alter table tree_node
    add constraint pk_tree_node
        primary key(node_id);
alter table tree_node
    add constraint uq_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);
create sequence tree_node_id_seq as bigint start with 100;

insert into tree_node(node_id, name, parent_id) values
    (1, 'Home', null),
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

insert into primitive(
    id,
    boolean_value, boolean_ref,
    char_value, char_ref,
    byte_value, byte_ref,
    short_value, short_ref,
    int_value, int_ref,
    long_value, long_ref,
    float_value, float_ref,
    double_value, double_ref
) values
    (1, true, true, 'X', 'X', 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8),
    (2, true, null, 'X', null, 3, null, 4, null, 5, null, 6, null, 7, null, 8, null)
;

create table administrator(
    id bigint not null,
    name varchar(50) not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table administrator
    add constraint pk_administrator
        primary key(id);

create table administrator_metadata(
    id bigint not null,
    name varchar(50) not null,
    email varchar(50) not null,
    website varchar(50) not null,
    administrator_id bigint not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table administrator_metadata
    add constraint pk_administrator_metadata
        primary key(id);
alter table administrator_metadata
    add constraint fk_administrator_metadata_administrator
        foreign key(administrator_id)
            references administrator(id);

create table role(
    id bigint not null,
    name varchar(50) not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table role
    add constraint pk_role
        primary key(id);

create table administrator_role_mapping(
    administrator_id bigint not null,
    role_id bigint not null
);
alter table administrator_role_mapping
    add constraint pk_administrator_role_mapping
        primary key(administrator_id, role_id);
alter table administrator_role_mapping
    add constraint fk_administrator_role_mapping_administrator
        foreign key(administrator_id)
            references administrator(id);
alter table administrator_role_mapping
    add constraint fk_administrator_role_mapping_role
        foreign key(role_id)
            references role(id);

create table permission(
    id bigint not null,
    name varchar(50) not null,
    role_id bigint not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table permission
    add constraint pk_permission
        primary key(id);
alter table permission
    add constraint fk_permission
        foreign key(role_id)
            references role(id);

insert into administrator(id, name, deleted, created_time, modified_time)
    values
    (1, 'a_1', false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (2, 'a_2', true, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (3, 'a_3', false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (4, 'a_4', true, '2022-10-03 00:00:00', '2022-10-03 00:10:00');

insert into administrator_metadata(id, name, email, website, administrator_id, deleted, created_time, modified_time)
    values
    (10, 'am_1', 'email_1', 'website_1', 1, false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (20, 'am_2', 'email_2', 'website_2', 2, true, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (30, 'am_3', 'email_3', 'website_3', 3, false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (40, 'am_4', 'email_4', 'website_4', 4, true, '2022-10-03 00:00:00', '2022-10-03 00:10:00');

insert into role(id, name, deleted, created_time, modified_time)
    values
    (100, 'r_1', false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (200, 'r_2', true, '2022-10-03 00:00:00', '2022-10-03 00:10:00');

insert into administrator_role_mapping(administrator_id, role_id)
    values
    (1, 100),
    (2, 100),
    (3, 100),
    (2, 200),
    (3, 200),
    (4, 200);

insert into permission(id, name, role_id, deleted, created_time, modified_time)
    values
    (1000, 'p_1', 100, false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (2000, 'p_2', 100, true, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (3000, 'p_3', 200, false, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
    (4000, 'p_4', 200, true, '2022-10-03 00:00:00', '2022-10-03 00:10:00');




create table transform(
    id bigint not null,
    `left` bigint not null,
    top bigint not null,
    `right` bigint not null,
    bottom bigint not null,
    target_left bigint not null,
    target_top bigint not null,
    target_right bigint not null,
    target_bottom bigint not null
);

alter table transform
    add constraint pk_transform
        primary key(id);

insert into transform(id, `left`, top, `right`, bottom, target_left, target_top, target_right, target_bottom)
    values(1, 100, 120, 400, 320, 800, 600, 1400, 1000);
