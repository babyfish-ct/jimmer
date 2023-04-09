drop table author if exists;



create table author(
    id identity(100, 1) not null,
    first_name varchar(25) not null,
    last_name varchar(25) not null,
    gender char(1) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table author
    add constraint business_key_author
        unique(first_name, last_name)
;
alter table author
    add constraint ck_author_gender
        check gender in ('M', 'F');

insert into author(id, first_name, last_name, gender, created_time, modified_time) values
    (1, 'Eve', 'Procello', 'F', current_timestamp(), current_timestamp()),
    (2, 'Alex', 'Banks', 'M', current_timestamp(), current_timestamp()),
    (3, 'Dan', 'Vanderkam', 'M', current_timestamp(), current_timestamp()),
    (4, 'Boris', 'Cherny', 'M', current_timestamp(), current_timestamp()),
    (5, 'Samer', 'Buna', 'M', current_timestamp(), current_timestamp())
;