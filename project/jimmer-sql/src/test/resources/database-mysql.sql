use jimmer_test;

set foreign_key_checks = 0;

drop table if exists book_author_mapping;
drop table if exists book;
drop table if exists book_store;
drop table if exists author;
drop table if exists tree_node;
drop table if exists employee;
drop table if exists department;
drop table if exists time_row;
drop table if exists post2_item;
drop table if exists post_2_category_2_mapping;
drop table if exists category_2;
drop table if exists post_2;
drop table if exists document_storage;
drop table if exists machine;
drop table if exists shop;
drop table if exists customer;
drop table if exists card;
drop table if exists customer_card_mapping;
drop table if exists shop_customer_mapping;
drop table if exists shop_vendor_mapping;

set foreign_key_checks = 1;

create table book_store(
    id binary(16) not null primary key,
    name varchar(50) not null,
    website varchar(100),
    version int not null
) engine=innodb;
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id binary(16) not null primary key,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id binary(16)
) engine=innodb;
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
    id binary(16) not null primary key,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null
) engine=innodb;
alter table author
    add constraint business_key_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check (gender in('M', 'F'));

create table book_author_mapping(
    book_id binary(16) not null,
    author_id binary(16) not null
) engine=innodb;
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
    node_id bigint not null auto_increment primary key,
    name varchar(20) not null,
    parent_id bigint
) engine=innodb;
alter table tree_node auto_increment = 100;
alter table tree_node
    add constraint business_key_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);

insert into book_store(id, name, version) values
    (X'd38c10da6be84924b9b95e81899612a0', 'O''REILLY', 0),
    (X'2fa3955e3e8349b9902e0465c109c779', 'MANNING', 0)
;

insert into book(id, name, edition, price, store_id) values
    (X'e110c56423cc48119e81d587a13db634', 'Learning GraphQL', 1, 50, X'd38c10da6be84924b9b95e81899612a0'),
    (X'b649b11b11614ad2b261af0112fdd7c8', 'Learning GraphQL', 2, 55, X'd38c10da6be84924b9b95e81899612a0'),
    (X'648736315d824bae8eb872dd955bfc56', 'Learning GraphQL', 3, 51, X'd38c10da6be84924b9b95e81899612a0'),

    (X'8f30bc8a49f9481dbeca5fe2d147c831', 'Effective TypeScript', 1, 73, X'd38c10da6be84924b9b95e81899612a0'),
    (X'8e169cfb23734e448cce1f1277f730d1', 'Effective TypeScript', 2, 69, X'd38c10da6be84924b9b95e81899612a0'),
    (X'9eded40f6d2e41deb4e733a28b11c8b6', 'Effective TypeScript', 3, 88, X'd38c10da6be84924b9b95e81899612a0'),

    (X'914c859535cb4f67bbc78029e9e6245a', 'Programming TypeScript', 1, 47.5, X'd38c10da6be84924b9b95e81899612a0'),
    (X'058ecfd0047b4979a7dc46ee24d08f08', 'Programming TypeScript', 2, 45, X'd38c10da6be84924b9b95e81899612a0'),
    (X'782b9a9deac841c49f2d74a5d047f45a', 'Programming TypeScript', 3, 48, X'd38c10da6be84924b9b95e81899612a0'),

    (X'a62f7aa39490461298b598aae0e77120', 'GraphQL in Action', 1, 80, X'2fa3955e3e8349b9902e0465c109c779'),
    (X'e37a834473bb4b23ba7682eac11f03e6', 'GraphQL in Action', 2, 81, X'2fa3955e3e8349b9902e0465c109c779'),
    (X'780bdf0705af48bf9be9f8c65236fecc', 'GraphQL in Action', 3, 80, X'2fa3955e3e8349b9902e0465c109c779')
;

insert into author(id, first_name, last_name, gender) values
    (X'fd6bb6cf336d416c80051ae11a6694b5', 'Eve', 'Procello', 'F'),
    (X'1e93da94af8444f482d1d8a9fd52ea94', 'Alex', 'Banks', 'M'),
    (X'c14665c8c6894ac7b8cc6f065b8d835d', 'Dan', 'Vanderkam', 'M'),
    (X'718795ad77c14fcf994afec6a5a11f0f', 'Boris', 'Cherny', 'M'),
    (X'eb4963fd522343e8b06b81e6172ee7ae', 'Samer', 'Buna', 'M')
;

insert into book_author_mapping(book_id, author_id) values
    (X'e110c56423cc48119e81d587a13db634', X'fd6bb6cf336d416c80051ae11a6694b5'),
    (X'b649b11b11614ad2b261af0112fdd7c8', X'fd6bb6cf336d416c80051ae11a6694b5'),
    (X'648736315d824bae8eb872dd955bfc56', X'fd6bb6cf336d416c80051ae11a6694b5'),

    (X'e110c56423cc48119e81d587a13db634', X'1e93da94af8444f482d1d8a9fd52ea94'),
    (X'b649b11b11614ad2b261af0112fdd7c8', X'1e93da94af8444f482d1d8a9fd52ea94'),
    (X'648736315d824bae8eb872dd955bfc56', X'1e93da94af8444f482d1d8a9fd52ea94'),

    (X'8f30bc8a49f9481dbeca5fe2d147c831', X'c14665c8c6894ac7b8cc6f065b8d835d'),
    (X'8e169cfb23734e448cce1f1277f730d1', X'c14665c8c6894ac7b8cc6f065b8d835d'),
    (X'9eded40f6d2e41deb4e733a28b11c8b6', X'c14665c8c6894ac7b8cc6f065b8d835d'),

    (X'914c859535cb4f67bbc78029e9e6245a', X'718795ad77c14fcf994afec6a5a11f0f'),
    (X'058ecfd0047b4979a7dc46ee24d08f08', X'718795ad77c14fcf994afec6a5a11f0f'),
    (X'782b9a9deac841c49f2d74a5d047f45a', X'718795ad77c14fcf994afec6a5a11f0f'),

    (X'a62f7aa39490461298b598aae0e77120', X'eb4963fd522343e8b06b81e6172ee7ae'),
    (X'e37a834473bb4b23ba7682eac11f03e6', X'eb4963fd522343e8b06b81e6172ee7ae'),
    (X'780bdf0705af48bf9be9f8c65236fecc', X'eb4963fd522343e8b06b81e6172ee7ae')
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



create table department(
    name varchar(20) not null,
    deleted_millis bigint not null default 0,
    id bigint not null auto_increment primary key
);
alter table department auto_increment = 100;
alter table department
	add constraint uq_department
		unique(name, deleted_millis);

create table employee(
    name varchar(20) not null,
    gender char(1) not null,
    department_id bigint,
    deleted_millis bigint not null default 0,
    id bigint not null auto_increment primary key
);
alter table employee auto_increment = 100;
alter table employee
    add constraint uq_employee
        unique(name, deleted_millis);
alter table employee
    add constraint ck_employee_gender
        check(gender in ('M', 'F'));

insert into department(id, name) values(1, 'Market');
insert into employee(id, name, gender, department_id) values(1, 'Sam', 'M', 1);
insert into employee(id, name, gender, department_id) values(2, 'Jessica', 'F', 1);



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
    deleted_uuid binary(16) not null
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
    deleted_uuid binary(16) not null
);
alter table post_2_category_2_mapping
    add constraint pk_post_category_mapping
        primary key(post_id, category_id, deleted_uuid);

insert into post_2(id, name, deleted_uuid) values
    (1, 'post-1', 0x00000000000000000000000000000000),
    (2, 'post-2', 0x00000000000000000000000000000000),
    (3, 'post-3', 0x00000000000000000000000000000000),
    (4, 'post-4', 0x00000000000000000000000000000000),
    (5, 'post-5', 0x00000000000000000000000000000000);

insert into category_2(id, name, deleted_millis) values
    (1, 'category-1', 0),
    (2, 'category-2', 0),
    (3, 'category-3', 0),
    (4, 'category-4', 0),
    (5, 'category-5', 0);

insert into post_2_category_2_mapping(post_id, category_id, deleted_uuid) values
    (1, 2, 0x00000000000000000000000000000000), (1, 3, 0x00000000000000000000000000000000),
    (2, 3, 0x00000000000000000000000000000000), (2, 4, 0x00000000000000000000000000000000),
    (3, 4, 0x00000000000000000000000000000000), (3, 5, 0x00000000000000000000000000000000),
    (4, 5, 0x00000000000000000000000000000000), (4, 1, 0x00000000000000000000000000000000),
    (5, 1, 0x00000000000000000000000000000000), (5, 2, 0x00000000000000000000000000000000);



create table document_storage (
    id bigint primary key,
    file_name varchar(100) NOT NULL,
    file_content longblob
);

create table machine(
    id bigint not null auto_increment primary key,
    host varchar(20) not null,
    port int not null,
    secondary_host varchar(20),
    secondary_port int,
    cpu_frequency int not null,
    memory_size int not null,
    disk_size int not null,
    factory_map json,
    patent_map json
);
alter table machine auto_increment = 100;
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
    '{"f-1": "factory-1", "f-2": "factory-2"}',
    '{"p-1": "patent-1", "p-2": "patent-2"}'
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
