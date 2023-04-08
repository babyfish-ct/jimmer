drop table book_store if exists;

create table book_store(
    id identity(100, 1) not null,
    name varchar(50) not null,
    website varchar(100),
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table book_store
    add constraint business_key_book_store
        unique(name)
;

insert into book_store(id, name, created_time, modified_time) values
    (1, 'O''REILLY', current_timestamp(), current_timestamp()),
    (2, 'MANNING', current_timestamp(), current_timestamp())
;