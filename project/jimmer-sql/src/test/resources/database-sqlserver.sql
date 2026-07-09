IF OBJECT_ID('book_author_mapping', 'U') IS NOT NULL DROP TABLE book_author_mapping;
IF OBJECT_ID('author_country_mapping', 'U') IS NOT NULL DROP TABLE author_country_mapping;
IF OBJECT_ID('book', 'U') IS NOT NULL DROP TABLE book;
IF OBJECT_ID('book_store', 'U') IS NOT NULL DROP TABLE book_store;
IF OBJECT_ID('author', 'U') IS NOT NULL DROP TABLE author;
IF OBJECT_ID('author_country', 'U') IS NOT NULL DROP TABLE author_country;
IF OBJECT_ID('tree_node', 'U') IS NOT NULL DROP TABLE tree_node;
IF OBJECT_ID('employee', 'U') IS NOT NULL DROP TABLE employee;
IF OBJECT_ID('department', 'U') IS NOT NULL DROP TABLE department;
IF OBJECT_ID('time_row', 'U') IS NOT NULL DROP TABLE time_row;
IF OBJECT_ID('post2_item', 'U') IS NOT NULL DROP TABLE post2_item;
IF OBJECT_ID('post_2_category_2_mapping', 'U') IS NOT NULL DROP TABLE post_2_category_2_mapping;
IF OBJECT_ID('category_2', 'U') IS NOT NULL DROP TABLE category_2;
IF OBJECT_ID('post_2', 'U') IS NOT NULL DROP TABLE post_2;
IF OBJECT_ID('document_storage', 'U') IS NOT NULL DROP TABLE document_storage;
IF OBJECT_ID('machine', 'U') IS NOT NULL DROP TABLE machine;
IF OBJECT_ID('shop_customer_mapping', 'U') IS NOT NULL DROP TABLE shop_customer_mapping;
IF OBJECT_ID('shop_vendor_mapping', 'U') IS NOT NULL DROP TABLE shop_vendor_mapping;
IF OBJECT_ID('customer_card_mapping', 'U') IS NOT NULL DROP TABLE customer_card_mapping;
IF OBJECT_ID('shop', 'U') IS NOT NULL DROP TABLE shop;
IF OBJECT_ID('customer', 'U') IS NOT NULL DROP TABLE customer;
IF OBJECT_ID('card', 'U') IS NOT NULL DROP TABLE card;
IF OBJECT_ID('vendor', 'U') IS NOT NULL DROP TABLE vendor;
IF OBJECT_ID('company', 'U') IS NOT NULL DROP TABLE company;
IF OBJECT_ID('street', 'U') IS NOT NULL DROP TABLE street;
IF OBJECT_ID('city', 'U') IS NOT NULL DROP TABLE city;
IF OBJECT_ID('province', 'U') IS NOT NULL DROP TABLE province;
IF OBJECT_ID('country', 'U') IS NOT NULL DROP TABLE country;

create table book_store(
    id char(36) not null primary key,
    name varchar(50) not null,
    website varchar(100),
    version int not null
);
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id char(36) not null primary key,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id char(36)
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
    id char(36) not null primary key,
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
        check (gender in('M', 'F'));

create table author_country(
    code varchar(10) not null primary key,
    name varchar(40) not null
);

create table book_author_mapping(
    book_id char(36) not null,
    author_id char(36) not null
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
    author_id char(36) not null,
    country_code varchar(10) not null,
    constraint pk_author_country_mapping
        primary key(author_id, country_code)
);
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

create table tree_node(
    node_id bigint identity(1,1) primary key,
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

insert into author_country(code, name) values
    ('USA', 'The United States of America')
;

insert into author_country_mapping(author_id, country_code) values
    ('fd6bb6cf-336d-416c-8005-1ae11a6694b5', 'USA'),
    ('1e93da94-af84-44f4-82d1-d8a9fd52ea94', 'USA'),
    ('c14665c8-c689-4ac7-b8cc-6f065b8d835d', 'USA'),
    ('718795ad-77c1-4fcf-994a-fec6a5a11f0f', 'USA'),
    ('eb4963fd-5223-43e8-b06b-81e6172ee7ae', 'USA')
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

set identity_insert tree_node on;
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
set identity_insert tree_node off;



create table department(
    name varchar(20) not null,
    deleted_millis bigint not null default 0,
    id bigint identity(1,1) primary key
);
alter table department
	add constraint uq_department
		unique(name, deleted_millis);

create table employee(
    name varchar(20) not null,
    gender char(1) not null,
    department_id bigint,
    deleted_millis bigint not null default 0,
    id bigint identity(1,1) primary key
);
alter table employee
    add constraint uq_employee
        unique(name, deleted_millis);
alter table employee
    add constraint ck_employee_gender
        check(gender in ('M', 'F'));

set identity_insert department on;
insert into department(id, name) values(1, 'Market');
set identity_insert department off;
set identity_insert employee on;
insert into employee(id, name, gender, department_id) values(1, 'Sam', 'M', 1);
insert into employee(id, name, gender, department_id) values(2, 'Jessica', 'F', 1);
set identity_insert employee off;



create table time_row(
    id bigint not null,
    value1 datetime not null,
    value2 date not null,
    value3 time not null,
    value4 datetime not null,
    value5 date not null,
    value6 time not null,
    value7 datetime not null,
    value8 datetime not null,
    value9 datetime not null
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
    '2025-04-13 13:32:08',
    '2025-04-13 13:32:09'
), (
   2,
   '2025-04-13 18:32:21',
   '2025-04-13',
   '18:32:23',
   '2025-04-13 18:32:24',
   '2025-04-13',
   '18:32:26',
   '2025-04-13 18:32:27',
   '2025-04-13 18:32:28',
   '2025-04-13 18:32:29'
);



create table post_2(
    id bigint not null,
    name varchar(50) not null,
    deleted_uuid char(36) not null
);
alter table post_2
    add constraint pk_post_2
        primary key(id);
alter table post_2
    add constraint uq_post_2
        unique(name);

create table post2_item(
    id bigint not null,
    name varchar(50) not null,
    deleted_uuid char(36) not null,
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
    id bigint not null,
    name varchar(50) not null,
    deleted_millis bigint not null
);
alter table category_2
    add constraint uq_category
        unique(name);

create table post_2_category_2_mapping(
    post_id bigint not null,
    category_id bigint not null,
    deleted_uuid char(36) not null
);
alter table post_2_category_2_mapping
    add constraint pk_post_category_mapping
        primary key(post_id, category_id, deleted_uuid);

insert into post_2(id, name, deleted_uuid) values
    (1, 'post-1', '00000000-0000-0000-0000-000000000000'),
    (2, 'post-2', '00000000-0000-0000-0000-000000000000'),
    (3, 'post-3', '00000000-0000-0000-0000-000000000000'),
    (4, 'post-4', '00000000-0000-0000-0000-000000000000'),
    (5, 'post-5', '00000000-0000-0000-0000-000000000000');

insert into category_2(id, name, deleted_millis) values
    (1, 'category-1', 0),
    (2, 'category-2', 0),
    (3, 'category-3', 0),
    (4, 'category-4', 0),
    (5, 'category-5', 0);

insert into post_2_category_2_mapping(post_id, category_id, deleted_uuid) values
    (1, 2, '00000000-0000-0000-0000-000000000000'), (1, 3, '00000000-0000-0000-0000-000000000000'),
    (2, 3, '00000000-0000-0000-0000-000000000000'), (2, 4, '00000000-0000-0000-0000-000000000000'),
    (3, 4, '00000000-0000-0000-0000-000000000000'), (3, 5, '00000000-0000-0000-0000-000000000000'),
    (4, 5, '00000000-0000-0000-0000-000000000000'), (4, 1, '00000000-0000-0000-0000-000000000000'),
    (5, 1, '00000000-0000-0000-0000-000000000000'), (5, 2, '00000000-0000-0000-0000-000000000000');



create table document_storage (
    id bigint primary key,
    file_name varchar(100) NOT NULL,
    file_content varbinary(max)
);

create table machine(
    id bigint identity(1,1) primary key,
    host varchar(20) not null,
    port int not null,
    secondary_host varchar(20),
    secondary_port int,
    cpu_frequency int not null,
    memory_size int not null,
    disk_size int not null,
    factory_map nvarchar(max),
    patent_map nvarchar(max)
);
alter table machine
    add constraint uq_machine
        unique(host, port);

set identity_insert machine on;
insert into machine(id, host, port, cpu_frequency, memory_size, disk_size, factory_map, patent_map)
values(
    1,
    'localhost',
    8080,
    2,
    8,
    256,
    '{"f-1": "factory-1", "f-2": "factory-2"}',
    '{"p-1": "patent-1", "p-2": "patent-2"}'
);
set identity_insert machine off;

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

insert into shop(id, name) values(1, 'Starbucks');
insert into shop(id, name) values(2, 'Dunkin');

insert into customer(id, name) values
    (1, 'Alex'),
    (2, 'Tim'),
    (3, 'Jessica'),
    (4, 'Linda'),
    (5, 'Mary'),
    (6, 'Bob');

create table card(
     id bigint not null,
     name varchar(20) not null
);
alter table card
    add constraint pk_card
        primary key(id);

insert into card(id, name) values
    (1, 'card-1'),
    (2, 'card-2');

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

insert into vendor(id, name, deleted_millis) values
     (1, 'Vendor-1', 0),
     (2, 'Vendor-2', 0),
     (3, 'Vendor-3', 0);

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

insert into shop_vendor_mapping(shop_id, vendor_id, deleted_millis, type) values
    (1, 1, 0, 'VIP'),
    (1, 2, 0, 'ORDINARY'),
    (2, 2, 0, 'VIP'),
    (2, 3, 0, 'ORDINARY');


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

insert into customer_card_mapping(customer_id, card_id) values
    (1, 1), (1, 2);

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
    add constraint fk_city__province
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
    add constraint fk_street__city
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
    add constraint fk_company__street
        foreign key(street_id)
            references street(id);

insert into country(code, name) values
    ('China', 'People''s Republic of China'),
    ('USA', 'The United States of America');

insert into province(id, province_name, country_id) values
    (1, 'SiChuan', 'China'),
    (2, 'GuangDong', 'China'),
    (3, 'HaiNan', 'China'),
    (4, 'Ohio', 'USA'),
    (5, 'California', 'USA'),
    (6, 'Michigan', 'USA');

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
    (12, 'Ann Arbor', 6);

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
    (24, 'street-24', 12);
