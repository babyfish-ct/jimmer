create table file(
    id bigint not null,
    name varchar(20) not null,
    parent_id bigint,
    type varchar(9) not null
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
alter table file
    add constraint ck_file__type
        check(type = 'FILE' or type = 'DIRECTORY');
create sequence file_id_seq as bigint start with 100;

create table file_user(
    id bigint not null,
    nick_name varchar(20) not null
);

alter table file_user
    add constraint pk_file_user
        primary key(id);
alter table file_user
    add constraint uq_file_user
        unique(nick_name);
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

insert into file_user(id, nick_name) values
    (1, 'alex'),
    (2, 'macy'),
    (3, 'dane'),
    (4, 'nancy'),
    (5, 'sam'),
    (6, 'zena'),
    (7, 'carl'),
    (8, 'ida');