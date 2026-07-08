-- Oracle database initialization script

-- Drop tables (Oracle doesn't support 'IF EXISTS', use PL/SQL block)
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE book_author_mapping CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE book CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE author CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE book_store CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE tree_node CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE tree_node_id_seq';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- Create tables

create table book_store(
    id VARCHAR2(36) not null,
    name VARCHAR2(100) not null,
    website VARCHAR2(200),
    version NUMBER(10) not null
);

alter table book_store
    add constraint pk_book_store
        primary key(id);

alter table book_store
    add constraint business_key_book_store
        unique(name);

create table book(
    id VARCHAR2(36) not null,
    name VARCHAR2(100) not null,
    edition NUMBER(10) not null,
    price NUMBER(10, 2) not null,
    store_id VARCHAR2(36)
);

alter table book
    add constraint pk_book
        primary key(id);

alter table book
    add constraint business_key_book
        unique(name, edition);

alter table book
    add constraint fk_book__book_store
        foreign key(store_id)
            references book_store(id);

create table author(
    id VARCHAR2(36) not null,
    first_name VARCHAR2(100) not null,
    last_name VARCHAR2(100) not null,
    gender CHAR(1) not null
);

alter table author
    add constraint pk_author
        primary key(id);

alter table author
    add constraint business_key_author
        unique(first_name, last_name);

alter table author
    add constraint ck_author_gender
        check (gender in('M', 'F'));

create table book_author_mapping(
    book_id VARCHAR2(36) not null,
    author_id VARCHAR2(36) not null
);

alter table book_author_mapping
    add constraint pk_book_author_mapping
        primary key(book_id, author_id);

alter table book_author_mapping
    add constraint fk_book_author_mapping__book
        foreign key(book_id)
            references book(id)
                on delete cascade;

alter table book_author_mapping
    add constraint fk_book_author_mapping__author
        foreign key(author_id)
            references author(id)
                on delete cascade;

create table tree_node(
    node_id NUMBER(19) not null,
    name VARCHAR2(100) not null,
    parent_id NUMBER(19)
);

alter table tree_node
    add constraint pk_tree_node
        primary key(node_id);

alter table tree_node
    add constraint business_key_tree_node
        unique(parent_id, name);

alter table tree_node
    add constraint fk_tree_node__parent
        foreign key(parent_id)
            references tree_node(node_id);

create sequence tree_node_id_seq;

-- Insert data

insert into book_store(id, name, version) values
    ('d38c10da-6be8-4924-b9b9-5e81899612a0', 'O''REILLY', 0);

insert into book_store(id, name, version) values
    ('2fa3955e-3e83-49b9-902e-0465c109c779', 'MANNING', 0);

insert into book(id, name, edition, price, store_id) values
    ('e110c564-23cc-4811-9e81-d587a13db634', 'Learning GraphQL', 1, 50, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('b649b11b-1161-4ad2-b261-af0112fdd7c8', 'Learning GraphQL', 2, 55, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('64873631-5d82-4bae-8eb8-72dd955bfc56', 'Learning GraphQL', 3, 51, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('8f30bc8a-49f9-481d-beca-5fe2d147c831', 'Effective TypeScript', 1, 73, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('8e169cfb-2373-4e48-cce1-0f1277f730d1', 'Effective TypeScript', 2, 69, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('9eded40f-6d2e-41de-b4e7-33a28b11c8b6', 'Effective TypeScript', 3, 88, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('914c8595-35cb-4f67-bbc7-8029e9e6245a', 'Programming TypeScript', 1, 47.5, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('058ecfd0-047b-4979-a7dc-46ee24d08f08', 'Programming TypeScript', 2, 45, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('782b9a9d-eac8-41c4-9f2d-74a5d047f45a', 'Programming TypeScript', 3, 48, 'd38c10da-6be8-4924-b9b9-5e81899612a0');

insert into book(id, name, edition, price, store_id) values
    ('a62f7aa3-9490-4612-98b5-98aae0e77120', 'GraphQL in Action', 1, 80, '2fa3955e-3e83-49b9-902e-0465c109c779');

insert into book(id, name, edition, price, store_id) values
    ('e37a8344-73bb-4b23-ba76-82eac11f03e6', 'GraphQL in Action', 2, 81, '2fa3955e-3e83-49b9-902e-0465c109c779');

insert into book(id, name, edition, price, store_id) values
    ('780bdf07-05af-48bf-9be9-f8c65236fecc', 'GraphQL in Action', 3, 80, '2fa3955e-3e83-49b9-902e-0465c109c779');

insert into author(id, first_name, last_name, gender) values
    ('fd6bb6cf-336d-416c-8005-1ae11a6694b5', 'Eve', 'Procello', 'F');

insert into author(id, first_name, last_name, gender) values
    ('1e93da94-af84-44f4-82d1-d8a9fd52ea94', 'Alex', 'Banks', 'M');

insert into author(id, first_name, last_name, gender) values
    ('c14665c8-c689-4ac7-b8cc-6f065b8d835d', 'Dan', 'Vanderkam', 'M');

insert into author(id, first_name, last_name, gender) values
    ('718795ad-77c1-4fcf-994a-fec6a5a11f0f', 'Boris', 'Cherny', 'M');

insert into author(id, first_name, last_name, gender) values
    ('eb4963fd-5223-43e8-b06b-81e6172ee7ae', 'Samer', 'Buna', 'M');

insert into book_author_mapping(book_id, author_id) values
    ('e110c564-23cc-4811-9e81-d587a13db634', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5');

insert into book_author_mapping(book_id, author_id) values
    ('b649b11b-1161-4ad2-b261-af0112fdd7c8', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5');

insert into book_author_mapping(book_id, author_id) values
    ('64873631-5d82-4bae-8eb8-72dd955bfc56', 'fd6bb6cf-336d-416c-8005-1ae11a6694b5');

insert into book_author_mapping(book_id, author_id) values
    ('e110c564-23cc-4811-9e81-d587a13db634', '1e93da94-af84-44f4-82d1-d8a9fd52ea94');

insert into book_author_mapping(book_id, author_id) values
    ('b649b11b-1161-4ad2-b261-af0112fdd7c8', '1e93da94-af84-44f4-82d1-d8a9fd52ea94');

insert into book_author_mapping(book_id, author_id) values
    ('64873631-5d82-4bae-8eb8-72dd955bfc56', '1e93da94-af84-44f4-82d1-d8a9fd52ea94');

insert into book_author_mapping(book_id, author_id) values
    ('8f30bc8a-49f9-481d-beca-5fe2d147c831', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d');

insert into book_author_mapping(book_id, author_id) values
    ('8e169cfb-2373-4e48-cce1-0f1277f730d1', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d');

insert into book_author_mapping(book_id, author_id) values
    ('9eded40f-6d2e-41de-b4e7-33a28b11c8b6', 'c14665c8-c689-4ac7-b8cc-6f065b8d835d');

insert into book_author_mapping(book_id, author_id) values
    ('914c8595-35cb-4f67-bbc7-8029e9e6245a', '718795ad-77c1-4fcf-994a-fec6a5a11f0f');

insert into book_author_mapping(book_id, author_id) values
    ('058ecfd0-047b-4979-a7dc-46ee24d08f08', '718795ad-77c1-4fcf-994a-fec6a5a11f0f');

insert into book_author_mapping(book_id, author_id) values
    ('782b9a9d-eac8-41c4-9f2d-74a5d047f45a', '718795ad-77c1-4fcf-994a-fec6a5a11f0f');

insert into book_author_mapping(book_id, author_id) values
    ('a62f7aa3-9490-4612-98b5-98aae0e77120', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae');

insert into book_author_mapping(book_id, author_id) values
    ('e37a8344-73bb-4b23-ba76-82eac11f03e6', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae');

insert into book_author_mapping(book_id, author_id) values
    ('780bdf07-05af-48bf-9be9-f8c65236fecc', 'eb4963fd-5223-43e8-b06b-81e6172ee7ae');

insert into tree_node(node_id, name, parent_id) values
    (1, 'Home', null);

insert into tree_node(node_id, name, parent_id) values
    (2, 'Food', 1);

insert into tree_node(node_id, name, parent_id) values
    (3, 'Drinks', 2);

insert into tree_node(node_id, name, parent_id) values
    (4, 'Coca Cola', 3);

insert into tree_node(node_id, name, parent_id) values
    (5, 'Fanta', 3);

insert into tree_node(node_id, name, parent_id) values
    (6, 'Bread', 2);

insert into tree_node(node_id, name, parent_id) values
    (7, 'Baguette', 6);

insert into tree_node(node_id, name, parent_id) values
    (8, 'Ciabatta', 6);

insert into tree_node(node_id, name, parent_id) values
    (9, 'Clothing', 1);

insert into tree_node(node_id, name, parent_id) values
    (10, 'Woman', 9);

insert into tree_node(node_id, name, parent_id) values
    (11, 'Casual wear', 10);

insert into tree_node(node_id, name, parent_id) values
    (12, 'Dress', 11);

insert into tree_node(node_id, name, parent_id) values
    (13, 'Miniskirt', 11);

insert into tree_node(node_id, name, parent_id) values
    (14, 'Jeans', 11);

insert into tree_node(node_id, name, parent_id) values
    (15, 'Formal wear', 10);

insert into tree_node(node_id, name, parent_id) values
    (16, 'Suit', 15);

insert into tree_node(node_id, name, parent_id) values
    (17, 'Shirt', 15);

insert into tree_node(node_id, name, parent_id) values
    (18, 'Man', 9);

insert into tree_node(node_id, name, parent_id) values
    (19, 'Casual wear', 18);

insert into tree_node(node_id, name, parent_id) values
    (20, 'Jacket', 19);

insert into tree_node(node_id, name, parent_id) values
    (21, 'Jeans', 19);

insert into tree_node(node_id, name, parent_id) values
    (22, 'Formal wear', 18);

insert into tree_node(node_id, name, parent_id) values
    (23, 'Suit', 22);

insert into tree_node(node_id, name, parent_id) values
    (24, 'Shirt', 22);

commit;