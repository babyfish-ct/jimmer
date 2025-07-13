
drop table if exists book_store cascade;
drop table if exists book cascade;
drop table if exists author cascade;
drop table if exists book_author_mapping cascade;
drop table if exists tree_node cascade;
drop sequence if exists tree_node_id_seq;
drop table if exists pg_json_wrapper cascade;
drop table if exists department cascade;
drop table if exists employee cascade;
drop table if exists sys_user cascade;
drop table if exists administrator cascade;
drop table if exists administrator_metadata cascade;
drop table if exists role cascade;
drop table if exists administrator_role_mapping cascade;
drop table if exists permission cascade;
drop table if exists time_row cascade;
drop table if exists pg_type_row cascade;
drop table if exists machine cascade;
drop table if exists shop_customer_mapping cascade;
drop table if exists shop cascade;
drop table if exists customer cascade;
drop table if exists pg_array_model cascade;
drop table if exists pg_date_time cascade;
drop table if exists container cascade;
drop table if exists nullable_bool cascade;

create table book_store(
    id uuid not null,
    name text not null,
    website text,
    version int not null
);
alter table book_store
    add constraint pk_book_store
        primary key(id)
;
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id uuid not null,
    name text not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id uuid
);
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
    id uuid not null,
    first_name text not null,
    last_name text not null,
    gender char(1) not null
);
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

create table tree_node(
    node_id bigint not null,
    name text not null,
    parent_id bigint
);
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

create sequence tree_node_id_seq;

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

insert into tree_node(
    node_id, name, parent_id
) values
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

create table pg_json_wrapper(
    id bigint not null primary key,
    json_1 jsonb,
    json_2 jsonb,
    json_3 jsonb,
    json_4 jsonb,
    json_5 jsonb
);

create table department(
    name varchar(20) not null,
    deleted_millis bigint not null default 0,
    id bigint not null generated by default as identity(start with 1 increment by 1)
);
alter table department
    add constraint pk_department
        primary key(id);
alter table department
	add constraint uq_department
		unique(name, deleted_millis);

create table employee(
    name varchar(20) not null,
    gender char(1) not null,
    department_id bigint,
    deleted_millis bigint not null default 0,
    id bigint not null generated by default as identity(start with 1 increment by 1)
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



 create table sys_user(
    id bigint not null generated by default as identity(start with 100 increment by 1),
    account text not null,
    email text not null,
    area text not null,
    nick_name text not null,
    description text not null
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


create table administrator(
    id bigint not null generated by default as identity(start with 1 increment by 1),
    name varchar(50) not null,
    deleted boolean not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table administrator
    add constraint pk_administrator
        primary key(id);
alter table administrator
    add constraint uq_administrator
        unique(name, deleted);

create table administrator_metadata(
    id bigint not null generated by default as identity(start with 1 increment by 1),
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
    id bigint not null generated by default as identity(start with 1 increment by 1),
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
    id bigint not null generated by default as identity(start with 1 increment by 1),
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



create table pg_type_row(
    id bigint not null generated by default as identity(start with 100 increment by 1),
    mac_address macaddr
);
alter table pg_type_row
    add primary key(id);



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


create table machine(
    id bigint not null generated by default as identity(start with 100 increment by 1),
    host varchar(20) not null,
    port int not null,
    secondary_host varchar(20),
    secondary_port int,
    cpu_frequency int not null,
    memory_size int not null,
    disk_size int not null,
    factory_map jsonb,
    patent_map jsonb
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
    '{"f-1": "factory-1", "f-2": "factory-2"}'::jsonb,
    '{"p-1": "patent-1", "p-2": "patent-2"}'::jsonb
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

insert into shop(id, name) values(1, 'Starbucks');
insert into shop(id, name) values(2, 'Dunkin');

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

insert into customer(id, name) values
(1, 'Alex'),
(2, 'Tim'),
(3, 'Jessica'),
(4, 'Linda'),
(5, 'Mary'),
(6, 'Bob');

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

insert into shop_customer_mapping(shop_id, customer_id, deleted_millis, type) values
(1, 1, 0, 'VIP'),
(1, 2, 0, 'ORDINARY'),
(1, 3, 0, 'ORDINARY'),
(1, 4, -1, 'ORDINARY'),
(2, 3, 0, 'VIP'),
(2, 4, 0, 'ORDINARY'),
(2, 5, 0, 'ORDINARY'),
(2, 6, -1, 'ORDINARY');

create table pg_array_model(
    id bigint not null,
    int_arr integer[] not null,
    integer_arr integer[] not null,
    text_arr text[] not null,
    text_list text[] not null,
    varchar_arr varchar[] not null,
    varchar_list varchar[] not null
);
alter table pg_array_model
    add constraint pk_arrays
        primary key(id)
;

insert into pg_array_model(id, int_arr, integer_arr, text_arr, text_list, varchar_arr, varchar_list) values
('1',
 array[1, 2, 3],
 array[4, 5, 6],
 array['a', 'b', 'c'],
 array['d', 'e', 'f'],
 array['g', 'h', 'i'],
 array['j', 'k', 'l']
);

create table pg_date_time(
    id int not null,
    dt date not null,
    ts timestamptz not null
);
alter table pg_date_time
    add constraint pk_pg_date_time
        primary key(id);

create table dependency(
    group_id varchar(50) not null,
    artifact_id varchar(50) not null,
    version varchar(50) not null,
    scope varchar(20) not null default 'C'
);
alter table dependency
    add constraint pk_dependency
        primary key(group_id, artifact_id);

insert into dependency(group_id, artifact_id, version) values
    ('org.babyfish.jimmer', 'jimmer-sql-kotlin', '0.8.177');

create table container(
    id bigint not null,
    address inet not null
);
alter table container
    add constraint pk_container
        primary key(id);

insert into container(id, address) values
(1, '127.0.0.1'), (2, '192.168.1.1');

create table nullable_bool(
    id bigint not null,
    value boolean
);
alter table nullable_bool
    add constraint pk_nullable_bool
        primary key(id);
