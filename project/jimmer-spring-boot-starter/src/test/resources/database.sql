drop table order_item_product_mapping if exists;
drop table product if exists;
drop table order_item if exists;
drop table order_ if exists;
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
drop sequence tree_node_id_seq if exists;

create table book_store(
    id uuid not null,
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
    id uuid not null,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id uuid
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
    id uuid not null,
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
    book_id uuid not null,
    author_id uuid not null
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
    author_id uuid not null,
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

insert into book_store(id, name, version) values
    ('d38c10da-6be8-4924-b9b9-5e81899612a0', 'O''REILLY', 0),
    ('2fa3955e-3e83-49b9-902e-0465c109c779', 'MANNING', 0)
;

insert into book(id, name, edition, price, store_id) values
    ('e110c564-23cc-4811-9e81-d587a13db634', 'Learning GraphQL', 1, 50, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('b649b11b-1161-4ad2-b261-af0112fdd7c8', 'Learning GraphQL', 2, 55, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('64873631-5d82-4bae-8eb8-72dd955bfc56', 'Learning GraphQL', 3, 51, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),

    ('8f30bc8a-49f9-481d-beca-5fe2d147c831', 'Effective TypeScript', 1, 73, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('8e169cfb-2373-4e44-8cce-1f1277f730d1', 'Effective TypeScript', 2, 69, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('9eded40f-6d2e-41de-b4e7-33a28b11c8b6', 'Effective TypeScript', 3, 88, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),

    ('914c8595-35cb-4f67-bbc7-8029e9e6245a', 'Programming TypeScript', 1, 47.5, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('058ecfd0-047b-4979-a7dc-46ee24d08f08', 'Programming TypeScript', 2, 45, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),
    ('782b9a9d-eac8-41c4-9f2d-74a5d047f45a', 'Programming TypeScript', 3, 48, 'd38c10da-6be8-4924-b9b9-5e81899612a0'),

    ('a62f7aa3-9490-4612-98b5-98aae0e77120', 'GraphQL in Action', 1, 80, '2fa3955e-3e83-49b9-902e-0465c109c779'),
    ('e37a8344-73bb-4b23-ba76-82eac11f03e6', 'GraphQL in Action', 2, 81, '2fa3955e-3e83-49b9-902e-0465c109c779'),
    ('780bdf07-05af-48bf-9be9-f8c65236fecc', 'GraphQL in Action', 3, 80, '2fa3955e-3e83-49b9-902e-0465c109c779')
;

insert into author(id, first_name, last_name, gender) values
    ('fd6bb6cf-336d-416c-8005-1ae11a6694b5', 'Eve', 'Procello', 'F'),
    ('1e93da94-af84-44f4-82d1-d8a9fd52ea94', 'Alex', 'Banks', 'M'),
    ('c14665c8-c689-4ac7-b8cc-6f065b8d835d', 'Dan', 'Vanderkam', 'M'),
    ('718795ad-77c1-4fcf-994a-fec6a5a11f0f', 'Boris', 'Cherny', 'M'),
    ('eb4963fd-5223-43e8-b06b-81e6172ee7ae', 'Samer', 'Buna', 'M')
;

insert into country(code, name) values
    ('USA', 'The United States of America')
;

insert into book_author_mapping(book_id, author_id) values
    ('e110c564-23cc-4811-9e81-d587a13db634', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5'),
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

insert into author_country_mapping(author_id, country_code) values
    ('fd6bb6cf-336d-416c-8005-1ae11a6694b5', 'USA'),
    ('1e93da94-af84-44f4-82d1-d8a9fd52ea94', 'USA'),
    ('c14665c8-c689-4ac7-b8cc-6f065b8d835d', 'USA'),
    ('718795ad-77c1-4fcf-994a-fec6a5a11f0f', 'USA'),
    ('eb4963fd-5223-43e8-b06b-81e6172ee7ae', 'USA');

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
    administrator_id bigint,
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
    target_left bigint,
    target_top bigint,
    target_right bigint,
    target_bottom bigint
);

alter table transform
    add constraint pk_transform
        primary key(id);

insert into transform(
    id, `left`, top, `right`, bottom,
    target_left, target_top, target_right, target_bottom
) values
    (1, 100, 120, 400, 320, 800, 600, 1400, 1000),
    (2, 150, 170, 450, 370, null, null, null, null);




create table order_(
    order_x varchar(10) not null,
    order_y varchar(10) not null,
    name varchar(20) not null
);
alter table order_
    add constraint pk_order
        primary key(order_x, order_y);

create table order_item(
    order_item_a int not null,
    order_item_b int not null,
    order_item_c int not null,
    name varchar(20) not null,
    fk_order_x varchar(10),
    fk_order_y varchar(10)
);
alter table order_item
    add constraint pk_order_item
        primary key(order_item_a, order_item_b, order_item_c);
alter table order_item
    add constraint fk_order_item
        foreign key(fk_order_x, fk_order_y)
            references order_(order_x, order_y)
                on delete cascade;

create table product(
    product_alpha varchar(10) not null,
    product_beta varchar(10) not null,
    name varchar(20) not null
);
alter table product
    add constraint pk_product
        primary key(product_alpha, product_beta);

create table order_item_product_mapping(
    fk_order_item_a int not null,
    fk_order_item_b int not null,
    fk_order_item_c int not null,
    fk_product_alpha varchar(10) not null,
    fk_product_beta varchar(10) not null
);
alter table order_item_product_mapping
    add constraint pk_order_item_product_mapping
        primary key(
            fk_order_item_a,
            fk_order_item_b,
            fk_order_item_c,
            fk_product_alpha,
            fk_product_beta
        );
alter table order_item_product_mapping
    add constraint fk_order_item_product_mapping_source
        foreign key(fk_order_item_a, fk_order_item_b, fk_order_item_c)
            references order_item(order_item_a, order_item_b, order_item_c)
                on delete cascade;
alter table order_item_product_mapping
    add constraint fk_order_item_product_mapping_target
        foreign key(fk_product_alpha, fk_product_beta)
            references product(product_alpha, product_beta)
                on delete cascade;



insert into order_(order_x, order_y, name) values
    ('001', '001', 'order-1'),
    ('001', '002', 'order-2');

insert into order_item(order_item_a, order_item_b, order_item_c, name, fk_order_x, fk_order_y) values
    (1, 1, 1, 'order-item-1-1', '001', '001'),
    (1, 1, 2, 'order-item-1-2', '001', '001'),
    (1, 2, 1, 'order-item-2-1', '001', '002'),
    (2, 1, 1, 'order-item-2-2', '001', '002'),
    (9, 9, 9, 'order-item-X-X', null, null);

insert into product(product_alpha, product_beta, name) values
    ('00A', '00A', 'Car'),
    ('00A', '00B', 'Boat'),
    ('00B', '00A', 'Bike');

insert into order_item_product_mapping(fk_order_item_a, fk_order_item_b, fk_order_item_c, fk_product_alpha, fk_product_beta) values
    (1, 1, 1, '00A', '00A'),
    (1, 1, 1, '00B', '00A'),
    (1, 1, 2, '00A', '00B'),
    (1, 1, 2, '00A', '00A'),
    (1, 2, 1, '00B', '00A'),
    (1, 2, 1, '00A', '00B'),
    (2, 1, 1, '00A', '00B'),
    (2, 1, 1, '00B', '00A');