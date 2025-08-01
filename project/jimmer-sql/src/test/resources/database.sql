create schema if not exists A;
create schema if not exists B;
create schema if not exists C;
create schema if not exists D;


drop table issue1125_mp_role_perm if exists;
drop table issue1125_sys_role if exists;
drop table issue1125_sys_perm if exists;
drop table time_row if exists;
drop table issue888_item if exists;
drop table issue888_structure if exists;
drop table post_2_category_2_mapping if exists;
drop table post2_item if exists;
drop table post_2 if exists;
drop table category_2 if exists;
drop table sys_user if exists;
drop table players if exists;
drop table teams if exists;
drop table "group" if exists;
drop table unit if exists;
drop table medicine if exists;
drop alias contains_id if exists;
drop table customer_card_mapping if exists;
drop table shop_customer_mapping if exists;
drop table shop_vendor_mapping if exists;
drop table card if exists;
drop table shop if exists;
drop table vendor if exists;
drop table customer if exists;
drop table file_user_mapping if exists;
drop table file_user if exists;
drop table file if exists;
drop table organization if exists;
drop table task if exists;
drop table worker if exists;
drop table category if exists;
drop table post if exists;
drop table learning_link if exists;
drop table course_dependency if exists;
drop table course if exists;
drop table student if exists;
drop table ms_product if exists;
drop table ms_order_item_product_mapping if exists;
drop table ms_order_item if exists;
drop table ms_order if exists;
drop table employee if exists;
drop table department if exists;
drop table animal if exists;
drop table machine if exists;
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
drop table author_country if exists;
drop table book if exists;
drop table author if exists;
drop table company if exists;
drop table street if exists;
drop table city if exists;
drop table province if exists;
drop table country if exists;
drop table book_store if exists;
drop table tree_node if exists;
drop table array_model if exists;
drop table person if exists;
drop sequence file_user_id_seq if exists;
drop sequence file_id_seq if exists;
drop sequence tree_node_id_seq if exists;
drop table D.TABLE_D if exists;
drop table C.TABLE_C if exists;
drop table B.TABLE_B if exists;
drop table A.TABLE_A if exists;
drop table personal if exists;

create table A.TABLE_A(
    id bigint not null primary key,
    deleted boolean not null
);

create table B.TABLE_B(
    id bigint not null primary key,
    status varchar(7) not null,
    a_id bigint
);

create table C.TABLE_C(
    id bigint not null primary key,
    deleted_time datetime
);

create table D.TABLE_D(
    id bigint not null primary key,
    created_time datetime
);

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
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender varchar(6) not null,
    id uuid not null
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

create table province(
    id bigint not null,
    province_name varchar(50) not null,
    country_id varchar(10) null
);
alter table province
    add constraint pk_province
        primary key(id);
alter table province
    add constraint fk_province__country
        foreign key(country_id)
            references country(code);

create table city(
    id bigint not null,
    city_name varchar(50) not null,
    province_id bigint not null
);
alter table city
    add constraint pk_city
        primary key(id);
alter table city
    add constraint fk_city
        foreign key(province_id)
            references province(id);

create table street(
    id bigint not null,
    street_name varchar(50) not null,
    city_id bigint not null
);
alter table street
    add constraint pk_street
        primary key(id);
alter table street
    add constraint fk_street
        foreign key(city_id)
            references city(id);

create table company(
    id bigint not null,
    company_name varchar(50) not null,
    street_id bigint
);
alter table company
    add constraint pk_company
        primary key(id);
alter table company
    add constraint fk_company
        foreign key(street_id)
            references street(id);



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

create table author_country(
    code varchar(20) not null,
    name varchar(40) not null
);
alter table author_country
    add primary key(code);

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
            references author_country(code)
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
    ('China', 'People''s Republic of China'),
    ('USA', 'The United States of America')
;

insert into province(id, province_name, country_id) values
    (1, 'SiChuan', 'China'),
    (2, 'GuangDong', 'China'),
    (3, 'HaiNan', 'China'),
    (4, 'Ohio', 'USA'),
    (5, 'California', 'USA'),
    (6, 'Michigan', 'USA')
;

insert into city(id, city_name, province_id) values
    (1, 'ChengDu', 1),
    (2, 'MianYang', 1),
    (3, 'GuangZhou', 2),
    (4, 'ShenZhen', 2),
    (5, 'SanYa', 3),
    (6, 'HaiKou', 3),
    (7, 'Columbus', 4),
    (8, 'Cleveland', 4),
    (9, 'Los Angeles', 5),
    (10, 'San Francisco', 5),
    (11, 'Lansing', 6),
    (12, 'Ann Arbor', 6)
;

insert into street(id, street_name, city_id) values
    (1, 'street-1', 1),
    (2, 'street-2', 1),
    (3, 'street-3', 2),
    (4, 'street-4', 2),
    (5, 'street-5', 3),
    (6, 'street-6', 3),
    (7, 'street-7', 4),
    (8, 'street-8', 4),
    (9, 'street-9', 5),
    (10, 'street-10', 5),
    (11, 'street-11', 6),
    (12, 'street-12', 6),
    (13, 'street-13', 7),
    (14, 'street-14', 7),
    (15, 'street-15', 8),
    (16, 'street-16', 8),
    (17, 'street-17', 9),
    (18, 'street-18', 9),
    (19, 'street-19', 10),
    (20, 'street-20', 10),
    (21, 'street-21', 11),
    (22, 'street-22', 11),
    (23, 'street-23', 12),
    (24, 'street-24', 12)
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

insert into author_country(code, name) values
    ('USA', 'The United States of America')
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
    id bigint auto_increment(100) not null,
    name varchar(50) not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null,
    __deleted_constraint_part int as (
        case deleted when false then 1 else null end
    )
);
alter table administrator
    add constraint pk_administrator
        primary key(id);
alter table administrator
    add constraint uq_administrator
        unique(name, __deleted_constraint_part);

create table administrator_metadata(
    id bigint auto_increment(100) not null,
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
    role_id bigint,
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
    (-1, 'a_-1', true, '2022-10-03 00:00:00', '2022-10-03 00:10:00'),
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



create table machine(
    id bigint auto_increment(100) not null,
    host varchar(20) not null,
    port int not null,
    secondary_host varchar(20),
    secondary_port int,
    cpu_frequency int not null,
    memory_size int not null,
    disk_size int not null,
    factory_map json(200),
    patent_map json(200)
);

alter table machine
    add constraint pk_machine
        primary key(id);

alter table machine
    add constraint uq_machine
        unique(host, port);

insert into machine(id, host, port, cpu_frequency, memory_size, disk_size, factory_map, patent_map)
values(
    1,
    'localhost',
    8080,
    2,
    8,
    256,
    '{"f-1": "factory-1", "f-2": "factory-2"}' format json,
    '{"p-1": "patent-1", "p-2": "patent-2"}' format json
);



create table animal(
    id bigint not null,
    name varchar(20) not null
);

alter table animal
    add constraint pk_animal
        primary key(id);

insert into animal(id, name) values(1, 'Trigger'), (2, 'Lion');



create table department(
    id bigint auto_increment(100) not null,
    name varchar(20) not null,
    deleted_millis bigint not null default 0
);
alter table department
    add constraint pk_department
        primary key(id);
alter table department
	add constraint uq_department
		unique(name, deleted_millis);

create table employee(
    id bigint auto_increment(100) not null,
    name varchar(20) not null,
    gender char(1) not null,
    department_id bigint,
    deleted_millis bigint not null default 0
);
alter table employee
    add constraint pk_employee
        primary key(id);
alter table employee
    add constraint uq_employee
        unique(name, deleted_millis);
alter table employee
    add constraint ck_employee_gender
        check(gender in ('M', 'F'));

insert into department(id, name) values(1, 'Market');
insert into employee(id, name, gender, department_id) values(1, 'Sam', 'M', 1);
insert into employee(id, name, gender, department_id) values(2, 'Jessica', 'F', 1);



create table ms_order(
    id bigint not null,
    name varchar(20) not null
);
alter table ms_order
    add constraint pk_ms_order
        primary key(id);

create table ms_order_item(
    id bigint not null,
    name varchar(20) not null,
    order_id bigint not null
);
alter table ms_order_item
    add constraint pk_ms_order_item
        primary key(id);

create table ms_order_item_product_mapping(
    order_item_id bigint not null,
    product_id bigint not null
);
alter table ms_order_item_product_mapping
    add constraint pk_ms_order_item_product_mapping
        primary key(order_item_id, product_id);
alter table ms_order_item_product_mapping
    add constraint fk_ms_order_item_product_mapping_order
        foreign key(order_item_id)
            references ms_order_item(id)
                on delete cascade;

create table ms_product(
    id bigint not null,
    name varchar(20) not null
);
alter table ms_product
    add constraint pk_ms_product
        primary key(id);

insert into ms_order(id, name) values(1, 'ms-order-1');
insert into ms_order(id, name) values(2, 'ms-order-2');
insert into ms_order_item(id, name, order_id) values(1, 'ms-order-1.item-1', 1);
insert into ms_order_item(id, name, order_id) values(2, 'ms-order-1.item-2', 1);
insert into ms_order_item(id, name, order_id) values(3, 'ms-order-2.item-1', 2);
insert into ms_order_item(id, name, order_id) values(4, 'ms-order-2.item-2', 2);
insert into ms_order_item(id, name, order_id) values(999, 'ms-order-X.item-X', 999);
insert into ms_product(id, name) values(1, 'ms-product-1');
insert into ms_product(id, name) values(2, 'ms-product-2');
insert into ms_product(id, name) values(3, 'ms-product-3');
insert into ms_order_item_product_mapping(order_item_id, product_id) values
    (1, 1), (1, 2),
    (2, 2), (2, 3),
    (3, 3), (3, 1),
    (4, 1), (4, 2), (4, 3);



create table student(
    id bigint not null,
    name varchar(20) not null
);
alter table student
    add constraint pk_student
        primary key(id);

create table course(
    id bigint not null,
    name varchar(20) not null,
    academic_credit int not null
);
alter table course
    add constraint pk_course
        primary key(id);

create table learning_link(
    id bigint not null,
    student_id bigint not null,
    course_id bigint not null,
    score int null
);
alter table learning_link
    add constraint pk_learning_link
        primary key(id);
alter table learning_link
    add constraint fk_learning_link_student
        foreign key(student_id)
            references student(id);
alter table learning_link
    add constraint fk_learning_link_course
        foreign key(course_id)
            references course(id);
alter table learning_link
    add constraint uq_course
        unique(student_id, course_id);

create table course_dependency(
    id bigint auto_increment(100) not null,
    prev_course_id bigint not null,
    next_course_id bigint not null,
    reason varchar(50) not null
);
alter table course_dependency
    add constraint pk_course_dependency
        primary key(id);
alter table course_dependency
    add constraint uq_course_dependency
        unique(prev_course_id, next_course_id);
alter table course_dependency
    add constraint fk_course_dependency__prev
        foreign key(prev_course_id)
            references course(id)
                on delete cascade;
alter table course_dependency
    add constraint fk_course_dependency__next
        foreign key(next_course_id)
            references course(id)
                on delete cascade;

insert into student(id, name) values(1, 'Oakes');
insert into student(id, name) values(2, 'Roach');
insert into course(id, name, academic_credit) values(1, 'Java', 2);
insert into course(id, name, academic_credit) values(2, 'Kotlin', 2);
insert into course(id, name, academic_credit) values(3, 'SQL', 2);
insert into learning_link(id, student_id, course_id, score) values
    (1, 1, 2, 78),
    (2, 1, 3, null),
    (3, 2, 3, 87),
    (4, 2, 1, null);
insert into course_dependency(prev_course_id, next_course_id, reason) values
    (3, 1, 'JDBC requires SQL'),
    (1, 2, 'Kotlin depends on JVM');



create table post(
    id bigint not null,
    name varchar(20) not null,
    category_ids varchar(200) not null
);
alter table post
    add constraint pk_post
        primary key(id);

create table category(
    id bigint not null,
    name varchar(20) not null
);
alter table category
    add constraint pk_category
        primary key(id);

insert into post(id, name, category_ids) values
    (1, 'post-1', '1, 2'),
    (2, 'post-2', '1, 2'),
    (3, 'post-3', '2, 3'),
    (4, 'post-4', '2, 3');

insert into category(id, name) values
    (1, 'category-1'),
    (2, 'category-2'),
    (3, 'category-3');

create alias contains_id for "org.babyfish.jimmer.sql.model.joinsql.H2ContainsIdFun.contains";



create table worker(
    id bigint auto_increment(100) not null,
    name varchar(20) not null
);
alter table worker
    add constraint pk_worker
        primary key(id);

create table task(
    id bigint auto_increment(100) not null,
    name varchar(20) not null,
    owner_id bigint
);
alter table task
    add constraint pk_task
        primary key(id);
alter table task
    add constraint fk_task_owner
        foreign key(owner_id)
            references worker(id);

insert into worker(id, name) values(1, 'Alex'), (2, 'James');
insert into task(id, name, owner_id) values(9, 'Release package', null), (10, 'Take photo', 2);



create table organization(
    id bigint not null,
    name varchar(20) not null,
    parent_id bigint,
    tenant varchar(10) not null
);
alter table organization
    add constraint pk_organization
        primary key(id);
alter table organization
    add constraint fk_organization_parent
        foreign key(parent_id)
            references organization(id);



create table file(
    id bigint not null,
    name varchar(20) not null,
    parent_id bigint
);

alter table file
    add constraint pk_file
        primary key(id);
alter table file
    add constraint uq_file
        unique(parent_id, name);
alter table file
    add constraint fk_file__parent
        foreign key(parent_id)
            references file(id);
create sequence file_id_seq as bigint start with 100;

create table file_user(
    id bigint not null,
    name varchar(20) not null,
    deleted_time datetime
);

alter table file_user
    add constraint pk_file_user
        primary key(id);
alter table file_user
    add constraint uq_file_user
        unique(name);
create sequence file_user_id_seq as bigint start with 100;

create table file_user_mapping(
    file_id bigint not null,
    user_id bigint not null
);
alter table file_user_mapping
    add constraint pk_file_user_mapping
        primary key(file_id, user_id);
alter table file_user_mapping
    add constraint fk_file_user_mapping__file
        foreign key(file_id)
            references file(id);
alter table file_user_mapping
    add constraint fk_file_user_mapping__user
        foreign key(user_id)
            references file_user(id);

insert into file(id, name, parent_id) values
    (1, 'usr', null),
        (2, 'bin', 1),
            (3, 'cd', 2),
            (4, 'vim', 2),
            (5, 'grep', 2),
            (6, 'wait', 2),
            (7, 'which', 2),
        (8, 'sbin', 1),
            (9, 'ipconfig', 8),
            (10, 'mtree', 8),
            (11, 'purge', 8),
            (12, 'ssh', 8),
            (13, 'tcpctl', 8),
        (14, 'lib', 1),
            (15, 'sqlite3', 14),
            (16, 'zsh', 14),
            (17, 'libstdc++.dylib', 14),
            (18, 'dtrace', 14),
            (19, 'libgmalloc.dylib', 14),
        (20, 'share', 1),
            (21, 'man', 20),
            (22, 'dict', 20),
            (23, 'sandbox', 20),
            (24, 'httpd', 20),
            (25, 'locale', 20),
        (26, 'local', 1),
            (27, 'include', 26),
                (28, 'node', 27),
                    (29, 'v8-external.h', 28),
                    (30, 'v8-internal.h', 28),
                    (31, 'v8-json.h', 28),
                    (32, 'v8-object.h', 28),
                    (33, 'v8-platform.h', 28),
            (34, 'lib', 26),
                (35, 'node_modules', 34),
                    (36, 'npm', 35),
                    (37, 'corepack', 35),
                    (38, 'typescript', 35),
                    (39, 'docsify-cli', 35),
    (40, 'etc', null),
        (41, 'passwd', 40),
        (42, 'hosts', 40),
        (43, 'ssh', 40),
        (44, 'profile', 40),
        (45, 'services', 40)
;

insert into file_user(id, name, deleted_time) values
    (1, 'root', '2023-10-13 04:48:21'),
    (2, 'bob', null),
    (3, 'alex', null),
    (4, 'jessica', null),
    (5, 'linda', '2023-10-13 04:48:24')
;

insert into file_user_mapping(file_id, user_id) values
    (1, 1), (1, 2), (1, 3), (1, 4),
        (2, 1), (2, 2), (2, 3),
            (3, 1), (3, 2),
            (4, 2), (4, 3),
            (5, 3), (5, 1),
            (6, 1), (6, 2),
            (7, 2), (7, 3),
        (8, 2), (8, 3), (8, 4),
            (9, 2), (9, 3),
            (10, 3), (10, 4),
            (11, 4), (11, 2),
            (12, 2), (12, 3),
            (13, 3), (13, 4),
        (14, 3), (14, 4), (14, 1),
            (15, 3), (15, 4),
            (16, 4), (16, 1),
            (17, 1), (17, 3),
            (18, 3), (18, 4),
            (19, 4), (19, 1),
        (20, 4), (20, 1), (20, 2),
            (21, 4), (21, 1),
            (22, 1), (22, 2),
            (23, 2), (23, 4),
            (24, 4), (24, 1),
            (25, 1), (25, 2),
        (26, 1), (26, 2), (26, 3),
            (27, 1), (27, 2), (27, 3),
                (28, 1), (28, 2), (28, 3),
                    (29, 1), (29, 2),
                    (30, 2), (30, 3),
                    (31, 3), (31, 1),
                    (32, 1), (32, 2),
                    (33, 2), (33, 3),
            (34, 1), (34, 2), (34, 3),
                (35, 1), (35, 2), (35, 3),
                    (36, 1), (36, 2),
                    (37, 2), (37, 3),
                    (38, 3), (38, 1),
                    (39, 1), (39, 2),
    (40, 2), (40, 3), (40, 4), (40, 5),
        (41, 2), (41, 3), (41, 4),
        (42, 3), (42, 4), (42, 5),
        (43, 4), (43, 5), (43, 2),
        (44, 5), (44, 2), (44, 3),
        (45, 2), (45, 3), (45, 4)
;

create table array_model(
    id uuid not null,
    integers integer array not null,
    ints int array not null,
    strings varchar(10) array not null,
    bytes bytea array not null,
    longs bigint array not null,
    uuids uuid array not null,
    floats decimal array not null
);
alter table array_model
    add constraint pk_arrays
        primary key(id)
;

insert into array_model(id, integers, ints, strings, bytes, longs, uuids, floats) values
    ('e110c564-23cc-4811-9e81-d587a13db635',
    array[3, 2, 1],
    array[6, 5, 4],
    array['3', '2', '1'],
    array[X'03' ,X'02', X'01'],
    array[3, 2, 1],
    array['e110c564-23cc-4811-9e81-d587a13db635'],
    array[3.0, 2.0, 1.0]
);




create table shop(
    id bigint not null,
    name varchar(20) not null
);
alter table shop
    add constraint pk_shop
        primary key(id);
alter table shop
    add constraint uq_shop
        unique(name);

create table customer(
    id bigint not null,
    name varchar(20) not null
);
alter table customer
    add constraint pk_customer
        primary key(id);
alter table customer
    add constraint uq_customer
        unique(name);

create table vendor(
    id bigint not null,
    name varchar(20) not null,
    deleted_millis bigint not null
);
alter table vendor
    add constraint pk_vendor
        primary key(id);
alter table vendor
    add constraint uq_vendor
        unique(name, deleted_millis);

create table shop_customer_mapping(
    shop_id bigint not null,
    customer_id bigint not null,
    deleted_millis bigint not null,
    type varchar(8) not null
);
alter table shop_customer_mapping
    add constraint pk_shop_customer_mapping
        primary key(shop_id, customer_id, deleted_millis, type);
alter table shop_customer_mapping
    add constraint fk_shop_customer_mapping__shop
        foreign key(shop_id)
            references shop(id);
alter table shop_customer_mapping
    add constraint fk_shop_customer_mapping__customer
        foreign key(customer_id)
            references customer(id);

create table shop_vendor_mapping(
    shop_id bigint not null,
    vendor_id bigint not null,
    deleted_millis bigint not null,
    type varchar(8) not null
);
alter table shop_vendor_mapping
    add constraint pk_shop_vendor_mapping
        primary key(shop_id, vendor_id, deleted_millis, type);
alter table shop_vendor_mapping
    add constraint fk_shop_vendor_mapping__shop
        foreign key(shop_id)
            references shop(id);
alter table shop_vendor_mapping
    add constraint fk_shop_vendor_mapping__vendor
        foreign key(vendor_id)
            references vendor(id);

create table card(
    id bigint not null,
    name varchar(20) not null
);
alter table card
    add constraint pk_card
        primary key(id);

create table customer_card_mapping(
    customer_id bigint not null,
    card_id bigint not null
);
alter table customer_card_mapping
    add constraint pk_customer_card_mapping
        primary key(customer_id, card_id);
alter table customer_card_mapping
    add constraint fk_customer_card_mapping__customer
        foreign key(customer_id)
            references customer(id);
alter table customer_card_mapping
    add constraint fk_customer_card_mapping__card
        foreign key(card_id)
            references card(id);
alter table customer_card_mapping
    add constraint uq_customer_card_mapping__card
        unique(card_id);



insert into shop(id, name) values(1, 'Starbucks');
insert into shop(id, name) values(2, 'Dunkin');

insert into customer(id, name) values
    (1, 'Alex'),
    (2, 'Tim'),
    (3, 'Jessica'),
    (4, 'Linda'),
    (5, 'Mary'),
    (6, 'Bob');

insert into vendor(id, name, deleted_millis) values
    (1, 'Vendor-1', 0),
    (2, 'Vendor-2', 0),
    (3, 'Vendor-3', 0);

insert into shop_customer_mapping(shop_id, customer_id, deleted_millis, type) values
    (1, 1, 0, 'VIP'),
    (1, 2, 0, 'ORDINARY'),
    (1, 3, 0, 'ORDINARY'),
    (1, 4, -1, 'ORDINARY'),
    (2, 3, 0, 'VIP'),
    (2, 4, 0, 'ORDINARY'),
    (2, 5, 0, 'ORDINARY'),
    (2, 6, -1, 'ORDINARY');

insert into shop_vendor_mapping(shop_id, vendor_id, deleted_millis, type) values
    (1, 1, 0, 'VIP'),
    (1, 2, 0, 'ORDINARY'),
    (2, 2, 0, 'VIP'),
    (2, 3, 0, 'ORDINARY');

insert into card(id, name) values
    (1, 'card-1'),
    (2, 'card-2');

insert into customer_card_mapping(customer_id, card_id) values
    (1, 1), (1, 2);



create table medicine(
    id bigint not null primary key,
    tags json(200) not null
);

insert into medicine(id, tags) values(
    1,
    '[{"name": "tag-1", "description": "tag-description-1"},{"name": "tag-2", "description": "tag-description-2"}]' format json
);




create table unit(
    id bigint auto_increment(100) primary key,
    name varchar(20) not null,
    type varchar(4) not null
);

insert into unit(id, name, type) values
    (1, 'Kane', 'FM'),
    (2, 'Quentin', 'FM'),
    (3, 'Carlisle', 'RM'),
    (4, 'Garth', 'RM'),
    (5, 'Zachariah', 'K'),
    (6, 'Ian', 'K');

create table "group"(
    "column" bigint not null
);
alter table "group"
    add constraint pk_group
        primary key("column");



create table teams(
    id bigint not null primary key,
    team_name varchar(20) not null
);

create table players(
    id bigint not null primary key,
    team_id bigint not null,
    player_name varchar(20) not null
);

alter table players
    add constraint fk_players_team
        foreign key (team_id)
            references teams(id);

insert into teams(id, team_name) values (1, 'Manchester United');
insert into players(id, team_id, player_name) values
    (1, 1, 'Noussair Mazraoui'),
    (2, 1, 'Mason Mount'),
    (3, 1, 'Christian Eriksen');


create table sys_user(
    id bigint auto_increment(100) not null,
    account varchar(20) not null,
    email varchar(50) not null,
    area varchar(10) not null,
    nick_name varchar(20) not null,
    description varchar(100) not null
);
alter table sys_user
    add constraint pk_sys_user
        primary key(id);
alter table sys_user
    add constraint uq_sys_user__account
        unique(account);
alter table sys_user
    add constraint uq_sys_user__email
        unique(email);
alter table sys_user
    add constraint uq_sys_user__area_nick_name
        unique(area, nick_name);

insert into sys_user(id, account, email, area, nick_name, description) values
    (1, 'sysusr_001', 'tom.cook@gmail.com', 'north', 'Tom', 'description_001'),
    (2, 'sysusr_002', 'linda.white@gmail.com', 'south', 'Linda', 'description_002'),
    (3, 'sysusr_003', 'alex.brown@gmail.com', 'east', 'Alex', 'description_003'),
    (4, 'sysusr_004', 'jessica.thomas@gmail.com', 'north', 'Jessica', 'description_004');



create table personal(
    id bigint not null,
    phone varchar(11) not null
);

alter table personal
    add constraint pk_personal
        primary key(id);

insert into personal(id, phone) values
    (1, '12345678910');



create table post_2(
    id bigint auto_increment(100) not null,
    name varchar(50) not null,
    deleted_uuid binary(16) not null
);
alter table post_2
    add constraint pk_post_2
        primary key(id);
alter table post_2
    add constraint uq_post_2
        unique(name);

create table post2_item(
    id bigint auto_increment(100) not null,
    name varchar(50) not null,
    deleted_uuid binary(16) not null,
    post_id bigint
);
alter table post2_item
    add constraint pk_post2_item
        primary key(id);
alter table post2_item
    add constraint fk_post2_item__post
        foreign key(post_id)
            references post_2(id);

create table category_2(
    id bigint auto_increment(100) not null,
    name varchar(50) not null,
    deleted_millis bigint not null
);
alter table category_2
    add constraint uq_category
        unique(name);

create table post_2_category_2_mapping(
    post_id bigint not null,
    category_id bigint not null,
    deleted_uuid binary(16) not null
);
alter table post_2_category_2_mapping
    add constraint pk_post_category_mapping
        primary key(post_id, category_id, deleted_uuid);

insert into post_2(id, name, deleted_uuid) values
    (1, 'post-1', x'00000000000000000000000000000000'),
    (2, 'post-2', x'00000000000000000000000000000000'),
    (3, 'post-3', x'00000000000000000000000000000000'),
    (4, 'post-4', x'00000000000000000000000000000000'),
    (5, 'post-5', x'00000000000000000000000000000000');

insert into category_2(id, name, deleted_millis) values
    (1, 'category-1', 0),
    (2, 'category-2', 0),
    (3, 'category-3', 0),
    (4, 'category-4', 0),
    (5, 'category-5', 0);

insert into post_2_category_2_mapping(post_id, category_id, deleted_uuid) values
    (1, 2, x'00000000000000000000000000000000'), (1, 3, x'00000000000000000000000000000000'),
    (2, 3, x'00000000000000000000000000000000'), (2, 4, x'00000000000000000000000000000000'),
    (3, 4, x'00000000000000000000000000000000'), (3, 5, x'00000000000000000000000000000000'),
    (4, 5, x'00000000000000000000000000000000'), (4, 1, x'00000000000000000000000000000000'),
    (5, 1, x'00000000000000000000000000000000'), (5, 2, x'00000000000000000000000000000000');



create table person(
    id bigint not null,
    name varchar(20) not null,
    friend_id bigint
);
alter table person
    add constraint pk_person
        primary key(id);
alter table person
    add constraint fk_person__friend
        foreign key(friend_id)
            references person(id);

insert into person(id, name, friend_id) values
    (1, 'Alex', null),
    (2, 'Tim', 1),
    (3, 'Bob', 2),
    (4, 'Jessica', 3);
update person
    set friend_id = 4
    where id = 1;



create table issue888_structure(
    id bigint not null,
    name varchar(20) not null
);
alter table issue888_structure
    add constraint pk_issue888_structure
        primary key(id);

create table issue888_item(
    id bigint not null,
    name varchar(20) not null,
    structure_id bigint,
    parent_id bigint
);
alter table issue888_item
    add constraint pk_issue888_item
        primary key(id);
alter table issue888_item
    add constraint fk_issue888_item__structure
        foreign key(structure_id)
            references issue888_structure(id);
alter table issue888_item
    add constraint fk_issue888_item__item
        foreign key(parent_id)
            references issue888_item(id);

INSERT INTO public.issue888_structure (id, name) VALUES (1, 'test');

INSERT INTO public.issue888_item (id, name, parent_id, structure_id) values
    (1, 'item1', NULL, 1),
        (5, 'child-item1',1, 1),
            (6, 'sub-child-item1', 5, 1),
        (4, 'child-item2', 1, 1),
            (2, 'sub-child-item2', 4, 1),
            (3, 'sub-child-item3', 4, 1);

create table time_row(
    id bigint not null,
    value1 timestamp not null,
    value2 date not null,
    value3 time not null,
    value4 timestamp not null,
    value5 date not null,
    value6 time not null,
    value7 timestamp not null,
    value8 timestamp with time zone not null,
    value9 timestamp with time zone not null
);
insert into time_row(
    id, value1, value2, value3, value4, value5, value6, value7, value8, value9
) values(
    1,
    '2025-04-13 13:32:01',
    '2025-04-13',
    '13:32:03',
    '2025-04-13 13:32:04',
    '2025-04-13',
    '13:32:06',
    '2025-04-13 13:32:07',
    '2025-04-13 13:32:08+08:00',
    '2025-04-13 13:32:09+08:00'
), (
    2,
    '2025-04-13 18:32:21',
    '2025-04-13',
    '18:32:23',
    '2025-04-13 18:32:24',
    '2025-04-13',
    '18:32:26',
    '2025-04-13 18:32:27',
    '2025-04-13 18:32:28+08:00',
    '2025-04-13 18:32:29+08:00'
);



create table issue1125_sys_perm (
  id bigint generated always as identity(start with 1 increment by 1) not null,
  deleted_at timestamp(0),
  constraint sys_perm_pkey primary key (id)
);

create table issue1125_mp_role_perm (
  role_id bigint not null,
  perm_id bigint not null,
  deleted_at timestamp(0)
);

create table issue1125_sys_role (
  id bigint generated always as identity(start with 1 increment by 1) not null,
  deleted_at timestamp(0),
  constraint sys_role_pkey primary key (id)
);