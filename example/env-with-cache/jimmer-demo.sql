create database jimmer_demo;
use jimmer_demo;

create table book_store(
    id bigint unsigned not null auto_increment primary key,
    name varchar(50) not null,
    website varchar(100),
    created_time datetime not null,
    modified_time datetime not null
) engine=innodb;
alter table book_store auto_increment = 100;
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

create table book(
    id bigint unsigned not null auto_increment primary key,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint unsigned,
    tenant varchar(20),
    created_time datetime not null,
    modified_time datetime not null 
) engine=innodb;
alter table book_store auto_increment = 100;
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
    id bigint unsigned not null auto_increment primary key,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null,
    created_time datetime not null,
    modified_time datetime not null 
) engine=innodb;
alter table author auto_increment = 100;
alter table author
    add constraint business_key_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check (gender in('M', 'F'));

create table book_author_mapping(
    book_id bigint unsigned not null,
    author_id bigint unsigned not null
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

create table chapter(
    id bigint unsigned not null auto_increment primary key,
    book_id bigint unsigned not null,
    chapter_no int not null,
    title varchar(100) not null,
    created_time timestamp not null,
    modified_time timestamp not null
) engine=innodb;
alter table chapter auto_increment = 400;
alter table chapter
    add constraint business_key_chapter
        unique(book_id, chapter_no);
alter table chapter
    add constraint fk_chapter_book
        foreign key(book_id)
            references book(id);

create table tree_node(
    node_id bigint unsigned not null auto_increment primary key,
    name varchar(20) not null,
    parent_id bigint unsigned,
    created_time datetime not null,
    modified_time datetime not null 
) engine=innodb;
alter table tree_node auto_increment = 100;
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

insert into chapter(id, book_id, chapter_no, title, created_time, modified_time) values
    (1, 1, 0, 'Preface', now(), now()),
    (2, 1, 1, 'Welcome to GraphQL', now(), now()),
    (3, 1, 2, 'Graph Theory', now(), now()),
    (4, 1, 3, 'The GraphQL Query Language', now(), now()),
    (5, 1, 4, 'Designing a Schema', now(), now()),
    (6, 1, 5, 'Creating a GraphQL API', now(), now()),
    (7, 1, 6, 'GraphQL Clients', now(), now()),
    (8, 1, 7, 'GraphQL in the Real World', now(), now()),

    (9, 2, 0, 'Preface', now(), now()),
    (10, 2, 1, 'Welcome to GraphQL', now(), now()),
    (11, 2, 2, 'Graph Theory', now(), now()),
    (12, 2, 3, 'The GraphQL Query Language', now(), now()),
    (13, 2, 4, 'Designing a Schema', now(), now()),
    (14, 2, 5, 'Creating a GraphQL API', now(), now()),
    (15, 2, 6, 'GraphQL Clients', now(), now()),
    (16, 2, 7, 'GraphQL in the Real World', now(), now()),

    (17, 3, 0, 'Preface', now(), now()),
    (18, 3, 1, 'Welcome to GraphQL', now(), now()),
    (19, 3, 2, 'Graph Theory', now(), now()),
    (20, 3, 3, 'The GraphQL Query Language', now(), now()),
    (21, 3, 4, 'Designing a Schema', now(), now()),
    (22, 3, 5, 'Creating a GraphQL API', now(), now()),
    (23, 3, 6, 'GraphQL Clients', now(), now()),
    (24, 3, 7, 'GraphQL in the Real World', now(), now()),

    (101, 4, 0, 'Preface', now(), now()),
    (102, 4, 1, 'Getting to Know TypeScript', now(), now()),
    (103, 4, 2, 'TypeScript’s Type System', now(), now()),
    (104, 4, 3, 'Type Inference', now(), now()),
    (105, 4, 4, 'Type Design', now(), now()),
    (106, 4, 5, 'Working with any', now(), now()),
    (107, 4, 6, 'Types Declarations and @types', now(), now()),
    (108, 4, 7, 'Writing and Running Your Code', now(), now()),
    (109, 4, 8, 'Migrating to TypeScript', now(), now()),

    (110, 5, 0, 'Preface', now(), now()),
    (111, 5, 1, 'Getting to Know TypeScript', now(), now()),
    (112, 5, 2, 'TypeScript’s Type System', now(), now()),
    (113, 5, 3, 'Type Inference', now(), now()),
    (114, 5, 4, 'Type Design', now(), now()),
    (115, 5, 5, 'Working with any', now(), now()),
    (116, 5, 6, 'Types Declarations and @types', now(), now()),
    (117, 5, 7, 'Writing and Running Your Code', now(), now()),
    (118, 5, 8, 'Migrating to TypeScript', now(), now()),

    (119, 6, 0, 'Preface', now(), now()),
    (120, 6, 1, 'Getting to Know TypeScript', now(), now()),
    (121, 6, 2, 'TypeScript’s Type System', now(), now()),
    (122, 6, 3, 'Type Inference', now(), now()),
    (123, 6, 4, 'Type Design', now(), now()),
    (124, 6, 5, 'Working with any', now(), now()),
    (125, 6, 6, 'Types Declarations and @types', now(), now()),
    (126, 6, 7, 'Writing and Running Your Code', now(), now()),
    (127, 6, 8, 'Migrating to TypeScript', now(), now()),

    (201, 7, 0, 'Preface', now(), now()),
    (202, 7, 1, 'Introduction', now(), now()),
    (203, 7, 2, 'TypeScript: A 10_000 Foot View', now(), now()),
    (204, 7, 3, 'All About Types', now(), now()),
    (205, 7, 4, 'Functions', now(), now()),
    (206, 7, 5, 'Classes and Interfaces', now(), now()),
    (207, 7, 6, 'Advanced Types', now(), now()),
    (208, 7, 7, 'Handling Errors', now(), now()),
    (209, 7, 8, 'Asynchronous Programming, Concurrency, and Parallelism', now(), now()),
    (210, 7, 9, 'Frontend and Backend Frameworks', now(), now()),
    (211, 7, 10, 'Namespaces.Modules', now(), now()),
    (212, 7, 11, 'Interoperating with JavaScript', now(), now()),
    (213, 7, 12, 'Building and Running TypeScript', now(), now()),
    (214, 7, 13, 'Conclusion', now(), now()),

    (215, 8, 0, 'Preface', now(), now()),
    (216, 8, 1, 'Introduction', now(), now()),
    (217, 8, 2, 'TypeScript: A 10_000 Foot View', now(), now()),
    (218, 8, 3, 'All About Types', now(), now()),
    (219, 8, 4, 'Functions', now(), now()),
    (220, 8, 5, 'Classes and Interfaces', now(), now()),
    (221, 8, 6, 'Advanced Types', now(), now()),
    (222, 8, 7, 'Handling Errors', now(), now()),
    (223, 8, 8, 'Asynchronous Programming, Concurrency, and Parallelism', now(), now()),
    (224, 8, 9, 'Frontend and Backend Frameworks', now(), now()),
    (225, 8, 10, 'Namespaces.Modules', now(), now()),
    (226, 8, 11, 'Interoperating with JavaScript', now(), now()),
    (227, 8, 12, 'Building and Running TypeScript', now(), now()),
    (228, 8, 13, 'Conclusion', now(), now()),

    (229, 9, 0, 'Preface', now(), now()),
    (230, 9, 1, 'Introduction', now(), now()),
    (231, 9, 2, 'TypeScript: A 10_000 Foot View', now(), now()),
    (232, 9, 3, 'All About Types', now(), now()),
    (233, 9, 4, 'Functions', now(), now()),
    (234, 9, 5, 'Classes and Interfaces', now(), now()),
    (235, 9, 6, 'Advanced Types', now(), now()),
    (236, 9, 7, 'Handling Errors', now(), now()),
    (237, 9, 8, 'Asynchronous Programming, Concurrency, and Parallelism', now(), now()),
    (238, 9, 9, 'Frontend and Backend Frameworks', now(), now()),
    (239, 9, 10, 'Namespaces.Modules', now(), now()),
    (240, 9, 11, 'Interoperating with JavaScript', now(), now()),
    (241, 9, 12, 'Building and Running TypeScript', now(), now()),
    (242, 9, 13, 'Conclusion', now(), now()),

    (301, 10, 0, 'Introduction to GraphQL', now(), now()),
    (302, 10, 1, 'Exploring GraphQL APIs', now(), now()),
    (303, 10, 2, 'Customizing and organizing GraphQL operations', now(), now()),
    (304, 10, 3, 'Designing a GraphQL schema', now(), now()),
    (305, 10, 4, 'Implementing schema resolvers', now(), now()),
    (306, 10, 5, 'Working with database models and relations', now(), now()),
    (307, 10, 6, 'Optimizing data fetching', now(), now()),
    (308, 10, 7, 'Implementing mutations', now(), now()),
    (309, 10, 8, 'Using GraphQL APIs without a client library', now(), now()),
    (310, 10, 9, 'Using GraphQL APIs with Apollo client', now(), now()),

    (311, 11, 0, 'Introduction to GraphQL', now(), now()),
    (312, 11, 1, 'Exploring GraphQL APIs', now(), now()),
    (313, 11, 2, 'Customizing and organizing GraphQL operations', now(), now()),
    (314, 11, 3, 'Designing a GraphQL schema', now(), now()),
    (315, 11, 4, 'Implementing schema resolvers', now(), now()),
    (316, 11, 5, 'Working with database models and relations', now(), now()),
    (317, 11, 6, 'Optimizing data fetching', now(), now()),
    (318, 11, 7, 'Implementing mutations', now(), now()),
    (319, 11, 8, 'Using GraphQL APIs without a client library', now(), now()),
    (320, 11, 9, 'Using GraphQL APIs with Apollo client', now(), now()),

    (321, 12, 0, 'Introduction to GraphQL', now(), now()),
    (322, 12, 1, 'Exploring GraphQL APIs', now(), now()),
    (323, 12, 2, 'Customizing and organizing GraphQL operations', now(), now()),
    (324, 12, 3, 'Designing a GraphQL schema', now(), now()),
    (325, 12, 4, 'Implementing schema resolvers', now(), now()),
    (326, 12, 5, 'Working with database models and relations', now(), now()),
    (327, 12, 6, 'Optimizing data fetching', now(), now()),
    (328, 12, 7, 'Implementing mutations', now(), now()),
    (329, 12, 8, 'Using GraphQL APIs without a client library', now(), now()),
    (330, 12, 9, 'Using GraphQL APIs with Apollo client', now(), now())
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
