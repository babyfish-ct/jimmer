drop table eyepiece if exists;
drop table objective if exists;
drop table camera if exists;
drop table topic if exists;
drop table contact if exists;
drop table customer if exists;
drop table dependency if exists;
drop table server if exists;
drop table machine if exists;
drop table file_user_mapping if exists;
drop table file_user if exists;
drop table file if exists;
drop table learning_link if exists;
drop table course if exists;
drop table student if exists;
drop table ms_product if exists;
drop table ms_order_item_product_mapping if exists;
drop table ms_order_item if exists;
drop table ms_order if exists;
drop table employee if exists;
drop table department if exists;
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
drop table personal if exists;
drop table monster if exists;
drop table work_user if exists;

drop sequence file_user_id_seq if exists;
drop sequence file_id_seq if exists;
drop sequence tree_node_id_seq if exists;
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
    id bigint auto_increment(100) not null,
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
    id bigint auto_increment(100) not null,
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
    node_id bigint auto_increment(100) not null,
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




create table department(
    id bigint not null,
    name varchar(20) not null,
    deleted_time datetime
);
alter table department
    add constraint pk_department
        primary key(id);

create table employee(
    id bigint not null,
    name varchar(20) not null,
    department_id bigint,
    deleted_uuid uuid
);
alter table employee
    add constraint pk_employee
        primary key(id);
alter table employee
    add constraint fk_employee_department
        foreign key(department_id)
            references department(id);

insert into department(id, name) values(1, 'Market');
insert into employee(id, name, department_id) values(1, 'Sam', 1);
insert into employee(id, name, department_id) values(2, 'Jessica', 1);



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



create table machine(
    id bigint not null,
    host varchar(20) not null,
    port int not null,
    factory_map json(200),
    patent_map json(200)
);

alter table machine
    add constraint pk_machine
        primary key(id);

insert into machine(id, host, port, factory_map, patent_map)
values(
    1,
    'localhost',
    8080,
    '{"f-1": "factory-1", "f-2": "factory-2"}' format json,
    '{"p-1": "patent-1", "p-2": "patent-2"}' format json
);



create table server(
    id bigint auto_increment(100) not null primary key,
    host_name varchar(40) not null,
    is_arm boolean not null,
    is_ssd boolean not null
);

insert into server(id, host_name, is_arm, is_ssd) values
    (1, 'internal_server', true, true),
    (2, 'external_server', false, false);

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

create table personal(
    id bigint not null,
    phone varchar(11) not null
);

alter table personal
    add constraint pk_personal
        primary key(id);

insert into personal(id, phone) values
    (1, '12345678910');



create table customer(
    id bigint auto_increment(100) not null primary key,
    name varchar(20) not null
);
alter table customer
    add constraint uq_customer
        unique(name);

create table contact(
    id bigint auto_increment(100) not null primary key,
    email varchar(20) not null,
    address varchar(50) not null,
    customer_id bigint not null
);
alter table contact
    add constraint fk_contact__customer
        foreign key(customer_id)
            references(id);
alter table contact
    add constraint uq_contact
        unique(customer_id);



create table topic(
    id bigint auto_increment(100) not null primary key,
    name varchar(20) not null,
    tags_mask int
);

insert into topic(id, name, tags_mask) values
    (1, 'What is the best ORM', 9);



create table camera(
    id bigint not null,
    name varchar(20) not null
);
alter table camera
    add constraint pk_camera
        primary key(id);

create table eyepiece(
    id bigint not null,
    name varchar(20) not null,
    eye_relief float not null,
    camera_id bigint
);
alter table eyepiece
    add constraint pk_eyepiece
        primary key(id);
alter table eyepiece
    add constraint fk_eyepiece_camera
        foreign key(camera_id)
            references eyepiece(id);

create table objective(
    id bigint not null,
    name varchar(20) not null,
    working_distance float not null,
    camera_id bigint
);
alter table objective
    add constraint pk_objective
        primary key(id);
alter table objective
    add constraint fk_objective_camera
        foreign key(camera_id)
            references objective(id);

insert into camera(id, name) values(1, 'camera-1');
insert into eyepiece(id, name, eye_relief, camera_id) values(1, 'eyepiece-1', 10, 1);
insert into objective(id, name, working_distance, camera_id) values(1, 'objective-1', 2500, 1);

create table monster(
     id int not null PRIMARY KEY,
     base_id int null
);

alter table monster
    add constraint fk_monster_base
        foreign key(base_id)
            references monster(id);



create table work_user(
    id bigint not null,
    name varchar(20) null
);
alter table work_user
    add constraint pk_work_user
        primary key(id);
