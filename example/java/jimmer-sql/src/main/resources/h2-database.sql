drop table tree_node if exists;
drop table chapter if exists;
drop table book_author_mapping if exists;
drop table book if exists;
drop table author if exists;
drop table book_store if exists;

create table book_store(
    id identity(100, 1) not null,
    name varchar(50) not null,
    website varchar(100),
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table book_store
    add constraint uq_book_store
        unique(name)
;

create table book(
    id identity(100, 1) not null,
    name varchar(50) not null,
    edition integer not null,
    price numeric(10, 2) not null,
    store_id bigint,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
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
    id identity(100, 1) not null,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table author
    add constraint uq_author
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

create table chapter(
    id bigint not null,
    book_id bigint not null,
    index int not null,
    title varchar(100) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);

alter table chapter
    add constraint pk_chapter
        primary key(id);

alter table chapter
    add constraint business_key_chapter
        unique(book_id, index);

alter table chapter
    add constraint fk_chapter_book
        foreign key(book_id)
            references book(id);

insert into book_store(id, name, created_time, modified_time) values
    (1, 'O''REILLY', current_timestamp(), current_timestamp()),
    (2, 'MANNING', current_timestamp(), current_timestamp())
;

insert into book(id, name, edition, price, store_id, tenant, created_time, modified_time) values
    (1, 'Learning GraphQL', 1, 50, 1, 'a', current_timestamp(), current_timestamp()),
    (2, 'Learning GraphQL', 2, 55, 1, 'b', current_timestamp(), current_timestamp()),
    (3, 'Learning GraphQL', 3, 51, 1, 'a', current_timestamp(), current_timestamp()),

    (4, 'Effective TypeScript', 1, 73, 1, 'b', current_timestamp(), current_timestamp()),
    (5, 'Effective TypeScript', 2, 69, 1, 'a', current_timestamp(), current_timestamp()),
    (6, 'Effective TypeScript', 3, 88, 1, 'b', current_timestamp(), current_timestamp()),

    (7, 'Programming TypeScript', 1, 47.5, 1, 'a', current_timestamp(), current_timestamp()),
    (8, 'Programming TypeScript', 2, 45, 1, 'b', current_timestamp(), current_timestamp()),
    (9, 'Programming TypeScript', 3, 48, 1, 'a', current_timestamp(), current_timestamp()),

    (10, 'GraphQL in Action', 1, 80, 2, 'b', current_timestamp(), current_timestamp()),
    (11, 'GraphQL in Action', 2, 81, 2, 'a', current_timestamp(), current_timestamp()),
    (12, 'GraphQL in Action', 3, 80, 2, 'b', current_timestamp(), current_timestamp())
;

insert into author(id, first_name, last_name, gender, created_time, modified_time) values
    (1, 'Eve', 'Procello', 'F', current_timestamp(), current_timestamp()),
    (2, 'Alex', 'Banks', 'M', current_timestamp(), current_timestamp()),
    (3, 'Dan', 'Vanderkam', 'M', current_timestamp(), current_timestamp()),
    (4, 'Boris', 'Cherny', 'M', current_timestamp(), current_timestamp()),
    (5, 'Samer', 'Buna', 'M', current_timestamp(), current_timestamp())
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

insert into chapter(id, book_id, index, title, created_time, modified_time) values
    (1, 1, 0, 'Preface', current_timestamp(), current_timestamp()),
    (2, 1, 1, 'Welcome to GraphQL', current_timestamp(), current_timestamp()),
    (3, 1, 2, 'Graph Theory', current_timestamp(), current_timestamp()),
    (4, 1, 3, 'The GraphQL Query Language', current_timestamp(), current_timestamp()),
    (5, 1, 4, 'Designing a Schema', current_timestamp(), current_timestamp()),
    (6, 1, 5, 'Creating a GraphQL API', current_timestamp(), current_timestamp()),
    (7, 1, 6, 'GraphQL Clients', current_timestamp(), current_timestamp()),
    (8, 1, 7, 'GraphQL in the Real World', current_timestamp(), current_timestamp()),

    (9, 2, 0, 'Preface', current_timestamp(), current_timestamp()),
    (10, 2, 1, 'Welcome to GraphQL', current_timestamp(), current_timestamp()),
    (11, 2, 2, 'Graph Theory', current_timestamp(), current_timestamp()),
    (12, 2, 3, 'The GraphQL Query Language', current_timestamp(), current_timestamp()),
    (13, 2, 4, 'Designing a Schema', current_timestamp(), current_timestamp()),
    (14, 2, 5, 'Creating a GraphQL API', current_timestamp(), current_timestamp()),
    (15, 2, 6, 'GraphQL Clients', current_timestamp(), current_timestamp()),
    (16, 2, 7, 'GraphQL in the Real World', current_timestamp(), current_timestamp()),

    (17, 3, 0, 'Preface', current_timestamp(), current_timestamp()),
    (18, 3, 1, 'Welcome to GraphQL', current_timestamp(), current_timestamp()),
    (19, 3, 2, 'Graph Theory', current_timestamp(), current_timestamp()),
    (20, 3, 3, 'The GraphQL Query Language', current_timestamp(), current_timestamp()),
    (21, 3, 4, 'Designing a Schema', current_timestamp(), current_timestamp()),
    (22, 3, 5, 'Creating a GraphQL API', current_timestamp(), current_timestamp()),
    (23, 3, 6, 'GraphQL Clients', current_timestamp(), current_timestamp()),
    (24, 3, 7, 'GraphQL in the Real World', current_timestamp(), current_timestamp()),

    (101, 4, 0, 'Preface', current_timestamp(), current_timestamp()),
    (102, 4, 1, 'Getting to Know TypeScript', current_timestamp(), current_timestamp()),
    (103, 4, 2, 'TypeScript’s Type System', current_timestamp(), current_timestamp()),
    (104, 4, 3, 'Type Inference', current_timestamp(), current_timestamp()),
    (105, 4, 4, 'Type Design', current_timestamp(), current_timestamp()),
    (106, 4, 5, 'Working with any', current_timestamp(), current_timestamp()),
    (107, 4, 6, 'Types Declarations and @types', current_timestamp(), current_timestamp()),
    (108, 4, 7, 'Writing and Running Your Code', current_timestamp(), current_timestamp()),
    (109, 4, 8, 'Migrating to TypeScript', current_timestamp(), current_timestamp()),

    (110, 5, 0, 'Preface', current_timestamp(), current_timestamp()),
    (111, 5, 1, 'Getting to Know TypeScript', current_timestamp(), current_timestamp()),
    (112, 5, 2, 'TypeScript’s Type System', current_timestamp(), current_timestamp()),
    (113, 5, 3, 'Type Inference', current_timestamp(), current_timestamp()),
    (114, 5, 4, 'Type Design', current_timestamp(), current_timestamp()),
    (115, 5, 5, 'Working with any', current_timestamp(), current_timestamp()),
    (116, 5, 6, 'Types Declarations and @types', current_timestamp(), current_timestamp()),
    (117, 5, 7, 'Writing and Running Your Code', current_timestamp(), current_timestamp()),
    (118, 5, 8, 'Migrating to TypeScript', current_timestamp(), current_timestamp()),

    (119, 6, 0, 'Preface', current_timestamp(), current_timestamp()),
    (120, 6, 1, 'Getting to Know TypeScript', current_timestamp(), current_timestamp()),
    (121, 6, 2, 'TypeScript’s Type System', current_timestamp(), current_timestamp()),
    (122, 6, 3, 'Type Inference', current_timestamp(), current_timestamp()),
    (123, 6, 4, 'Type Design', current_timestamp(), current_timestamp()),
    (124, 6, 5, 'Working with any', current_timestamp(), current_timestamp()),
    (125, 6, 6, 'Types Declarations and @types', current_timestamp(), current_timestamp()),
    (126, 6, 7, 'Writing and Running Your Code', current_timestamp(), current_timestamp()),
    (127, 6, 8, 'Migrating to TypeScript', current_timestamp(), current_timestamp()),

    (201, 7, 0, 'Preface', current_timestamp(), current_timestamp()),
    (202, 7, 1, 'Introduction', current_timestamp(), current_timestamp()),
    (203, 7, 2, 'TypeScript: A 10_000 Foot View', current_timestamp(), current_timestamp()),
    (204, 7, 3, 'All About Types', current_timestamp(), current_timestamp()),
    (205, 7, 4, 'Functions', current_timestamp(), current_timestamp()),
    (206, 7, 5, 'Classes and Interfaces', current_timestamp(), current_timestamp()),
    (207, 7, 6, 'Advanced Types', current_timestamp(), current_timestamp()),
    (208, 7, 7, 'Handling Errors', current_timestamp(), current_timestamp()),
    (209, 7, 8, 'Asynchronous Programming, Concurrency, and Parallelism', current_timestamp(), current_timestamp()),
    (210, 7, 9, 'Frontend and Backend Frameworks', current_timestamp(), current_timestamp()),
    (211, 7, 10, 'Namespaces.Modules', current_timestamp(), current_timestamp()),
    (212, 7, 11, 'Interoperating with JavaScript', current_timestamp(), current_timestamp()),
    (213, 7, 12, 'Building and Running TypeScript', current_timestamp(), current_timestamp()),
    (214, 7, 13, 'Conclusion', current_timestamp(), current_timestamp()),

    (215, 8, 0, 'Preface', current_timestamp(), current_timestamp()),
    (216, 8, 1, 'Introduction', current_timestamp(), current_timestamp()),
    (217, 8, 2, 'TypeScript: A 10_000 Foot View', current_timestamp(), current_timestamp()),
    (218, 8, 3, 'All About Types', current_timestamp(), current_timestamp()),
    (219, 8, 4, 'Functions', current_timestamp(), current_timestamp()),
    (220, 8, 5, 'Classes and Interfaces', current_timestamp(), current_timestamp()),
    (221, 8, 6, 'Advanced Types', current_timestamp(), current_timestamp()),
    (222, 8, 7, 'Handling Errors', current_timestamp(), current_timestamp()),
    (223, 8, 8, 'Asynchronous Programming, Concurrency, and Parallelism', current_timestamp(), current_timestamp()),
    (224, 8, 9, 'Frontend and Backend Frameworks', current_timestamp(), current_timestamp()),
    (225, 8, 10, 'Namespaces.Modules', current_timestamp(), current_timestamp()),
    (226, 8, 11, 'Interoperating with JavaScript', current_timestamp(), current_timestamp()),
    (227, 8, 12, 'Building and Running TypeScript', current_timestamp(), current_timestamp()),
    (228, 8, 13, 'Conclusion', current_timestamp(), current_timestamp()),

    (229, 9, 0, 'Preface', current_timestamp(), current_timestamp()),
    (230, 9, 1, 'Introduction', current_timestamp(), current_timestamp()),
    (231, 9, 2, 'TypeScript: A 10_000 Foot View', current_timestamp(), current_timestamp()),
    (232, 9, 3, 'All About Types', current_timestamp(), current_timestamp()),
    (233, 9, 4, 'Functions', current_timestamp(), current_timestamp()),
    (234, 9, 5, 'Classes and Interfaces', current_timestamp(), current_timestamp()),
    (235, 9, 6, 'Advanced Types', current_timestamp(), current_timestamp()),
    (236, 9, 7, 'Handling Errors', current_timestamp(), current_timestamp()),
    (237, 9, 8, 'Asynchronous Programming, Concurrency, and Parallelism', current_timestamp(), current_timestamp()),
    (238, 9, 9, 'Frontend and Backend Frameworks', current_timestamp(), current_timestamp()),
    (239, 9, 10, 'Namespaces.Modules', current_timestamp(), current_timestamp()),
    (240, 9, 11, 'Interoperating with JavaScript', current_timestamp(), current_timestamp()),
    (241, 9, 12, 'Building and Running TypeScript', current_timestamp(), current_timestamp()),
    (242, 9, 13, 'Conclusion', current_timestamp(), current_timestamp()),

    (301, 10, 0, 'Introduction to GraphQL', current_timestamp(), current_timestamp()),
    (302, 10, 1, 'Exploring GraphQL APIs', current_timestamp(), current_timestamp()),
    (303, 10, 2, 'Customizing and organizing GraphQL operations', current_timestamp(), current_timestamp()),
    (304, 10, 3, 'Designing a GraphQL schema', current_timestamp(), current_timestamp()),
    (305, 10, 4, 'Implementing schema resolvers', current_timestamp(), current_timestamp()),
    (306, 10, 5, 'Working with database models and relations', current_timestamp(), current_timestamp()),
    (307, 10, 6, 'Optimizing data fetching', current_timestamp(), current_timestamp()),
    (308, 10, 7, 'Implementing mutations', current_timestamp(), current_timestamp()),
    (309, 10, 8, 'Using GraphQL APIs without a client library', current_timestamp(), current_timestamp()),
    (310, 10, 9, 'Using GraphQL APIs with Apollo client', current_timestamp(), current_timestamp()),

    (311, 11, 0, 'Introduction to GraphQL', current_timestamp(), current_timestamp()),
    (312, 11, 1, 'Exploring GraphQL APIs', current_timestamp(), current_timestamp()),
    (313, 11, 2, 'Customizing and organizing GraphQL operations', current_timestamp(), current_timestamp()),
    (314, 11, 3, 'Designing a GraphQL schema', current_timestamp(), current_timestamp()),
    (315, 11, 4, 'Implementing schema resolvers', current_timestamp(), current_timestamp()),
    (316, 11, 5, 'Working with database models and relations', current_timestamp(), current_timestamp()),
    (317, 11, 6, 'Optimizing data fetching', current_timestamp(), current_timestamp()),
    (318, 11, 7, 'Implementing mutations', current_timestamp(), current_timestamp()),
    (319, 11, 8, 'Using GraphQL APIs without a client library', current_timestamp(), current_timestamp()),
    (320, 11, 9, 'Using GraphQL APIs with Apollo client', current_timestamp(), current_timestamp()),

    (321, 12, 0, 'Introduction to GraphQL', current_timestamp(), current_timestamp()),
    (322, 12, 1, 'Exploring GraphQL APIs', current_timestamp(), current_timestamp()),
    (323, 12, 2, 'Customizing and organizing GraphQL operations', current_timestamp(), current_timestamp()),
    (324, 12, 3, 'Designing a GraphQL schema', current_timestamp(), current_timestamp()),
    (325, 12, 4, 'Implementing schema resolvers', current_timestamp(), current_timestamp()),
    (326, 12, 5, 'Working with database models and relations', current_timestamp(), current_timestamp()),
    (327, 12, 6, 'Optimizing data fetching', current_timestamp(), current_timestamp()),
    (328, 12, 7, 'Implementing mutations', current_timestamp(), current_timestamp()),
    (329, 12, 8, 'Using GraphQL APIs without a client library', current_timestamp(), current_timestamp()),
    (330, 12, 9, 'Using GraphQL APIs with Apollo client', current_timestamp(), current_timestamp())
    ;

create table tree_node(
    node_id identity(100, 1) not null,
    name varchar(20) not null,
    parent_id bigint,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table tree_node
    add constraint uq_tree_node
        unique(parent_id, name);
alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);

insert into tree_node(
    node_id, name, parent_id, created_time, modified_time
) values
    (1, 'Home', null, current_timestamp(), current_timestamp()),
        (2, 'Food', 1, current_timestamp(), current_timestamp()),
            (3, 'Drinks', 2, current_timestamp(), current_timestamp()),
                (4, 'Coca Cola', 3, current_timestamp(), current_timestamp()),
                (5, 'Fanta', 3, current_timestamp(), current_timestamp()),
            (6, 'Bread', 2, current_timestamp(), current_timestamp()),
                (7, 'Baguette', 6, current_timestamp(), current_timestamp()),
                (8, 'Ciabatta', 6, current_timestamp(), current_timestamp()),
        (9, 'Clothing', 1, current_timestamp(), current_timestamp()),
            (10, 'Woman', 9, current_timestamp(), current_timestamp()),
                (11, 'Casual wear', 10, current_timestamp(), current_timestamp()),
                    (12, 'Dress', 11, current_timestamp(), current_timestamp()),
                    (13, 'Miniskirt', 11, current_timestamp(), current_timestamp()),
                    (14, 'Jeans', 11, current_timestamp(), current_timestamp()),
                (15, 'Formal wear', 10, current_timestamp(), current_timestamp()),
                    (16, 'Suit', 15, current_timestamp(), current_timestamp()),
                    (17, 'Shirt', 15, current_timestamp(), current_timestamp()),
            (18, 'Man', 9, current_timestamp(), current_timestamp()),
                (19, 'Casual wear', 18, current_timestamp(), current_timestamp()),
                    (20, 'Jacket', 19, current_timestamp(), current_timestamp()),
                    (21, 'Jeans', 19, current_timestamp(), current_timestamp()),
                (22, 'Formal wear', 18, current_timestamp(), current_timestamp()),
                    (23, 'Suit', 22, current_timestamp(), current_timestamp()),
                    (24, 'Shirt', 22, current_timestamp(), current_timestamp())
;