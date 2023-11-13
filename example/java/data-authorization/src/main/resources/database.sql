create table file(
    id bigint not null,
    name varchar(40) not null,
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
create sequence file_id_seq as bigint start with 200;

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

insert into file(id, name, parent_id, type) values
    (1, 'etc', null, 'DIRECTORY'),
        (2, 'hosts', 1, 'FILE'),
        (3, 'ssh', 1, 'DIRECTORY'),
            (4, 'ssh_config', 3, 'FILE'),
            (5, 'sshd_config', 3, 'FILE'),
    (6, 'usr', null, 'DIRECTORY'),
        (7, 'bin', 6, 'DIRECTORY'),
            (8, 'man', 7, 'FILE'),
            (9, 'zip', 7, 'FILE'),
        (10, 'local', 6, 'DIRECTORY'),
            (11, 'include', 10, 'DIRECTORY'),
            (12, 'lib', 10, 'DIRECTORY'),
            (13, 'share', 10, 'DIRECTORY'),
    (19, 'Users', null, 'DIRECTORY'),
        (20, 'alex', 19, 'DIRECTORY'),
            (21, 'prj_01', 20, 'DIRECTORY'),
                (22, 'include', 21, 'DIRECTORY'),
                    (23, 'tiny_db.h', 22, 'FILE'),
                (24, 'impl', 21, 'DIRECTORY'),
                    (25, 'tiny_db.cpp', 24, 'FILE'),
                (26, 'lib', 21, 'DIRECTORY'),
                    (27, 'antlr.so', 26, 'FILE'),
                    (28, 'leveldb.so', 26, 'FILE'),
        (40, 'macy', 19, 'DIRECTORY'),
            (41, 'prj_01', 40, 'DIRECTORY'),
                (42, 'model', 41, 'DIRECTORY'),
                    (43, 'Department.java', 42, 'FILE'),
                    (44, 'Employee.java', 42, 'FILE'),
                (45, 'api', 41, 'DIRECTORY'),
                    (46, 'HumanResource.java', 45, 'FILE'),
                (47, 'impl', 41, 'DIRECTORY'),
                    (48, 'HumanResourceImpl.java', 47, 'FILE'),
        (60, 'dane', 19, 'DIRECTORY'),
            (61, 'prj_01', 60, 'DIRECTORY'),
                (62, 'src', 61, 'DIRECTORY'),
                    (63, 'MainView.kt', 62, 'FILE'),
                    (64, 'Remove.kt', 62, 'FILE'),
                (65, 'res', 61, 'DIRECTORY'),
                    (66, 'logo.png', 65, 'FILE'),
                (67, 'libs', 61, 'DIRECTORY'),
                    (68, 'kernel.so', 67, 'FILE'),
                    (69, 'arch-arm.so', 67, 'FILE'),
        (80, 'nancy', 19, 'DIRECTORY'),
            (81, 'prj_01', 80, 'DIRECTORY'),
                (82, 'appsettings.json', 81, 'FILE'),
                (83, 'Program.cs', 81, 'FILE'),
                (84, 'Model', 81, 'DIRECTORY'),
                    (85, 'Order.cs', 84, 'FILE'),
                    (86, 'OrderItem.cs', 84, 'FILE'),
                    (87, 'Product.cs', 84, 'FILE'),
                (88, 'Service', 81, 'DIRECTORY'),
                    (89, 'ShopService.cs', 88, 'FILE'),
                    (90, 'ProductService.cs', 88, 'FILE'),
                (91, 'Views', 81, 'DIRECTORY'),
                    (92, 'index.cshtml', 91, 'FILE'),
        (100, 'sam', 19, 'DIRECTORY'),
            (101, 'prj_01', 100, 'DIRECTORY'),
                (102, 'package.json', 101, 'DIRECTORY'),
                (103, 'src', 102, 'DIRECTORY'),
                    (104, 'index.tsx', 103, 'FILE'),
                    (105, 'App.tsx', 103, 'FILE'),
                    (106, 'components', 103, 'DIRECTORY'),
                        (107, 'ProductList.tsx', 106, 'FILE'),
                        (108, 'ProductDetail.tsx', 106, 'FILE'),
                        (109, 'ProductForm.tsx', 106, 'FILE'),
                (110, 'static', 101, 'DIRECTORY'),
                    (111, 'img', 110, 'DIRECTORY'),
        (120, 'zena', 19, 'DIRECTORY'),
            (121, 'prj_01', 120, 'DIRECTORY'),
                (122, 'public', 121, 'DIRECTORY'),
                    (123, 'index.html', 122, 'FILE'),
                (124, 'src', 121, 'DIRECTORY'),
                    (125, 'App.vue', 124, 'FILE'),
                    (126, 'components', 124, 'DIRECTORY'),
                        (127, 'Sidebar.vue', 126, 'FILE'),
                        (128, 'Header.vue', 126, 'FILE'),
                        (129, 'Content.vue', 126, 'FILE'),
                (130, 'assets', 121, 'DIRECTORY'),
        (140, 'carl', 19, 'DIRECTORY'),
            (141, 'prj_01', 140, 'DIRECTORY'),
                (142, 'network', 141, 'DIRECTORY'),
                    (143, 'util.py', 142, 'FILE'),
                    (144, 'cnn.py', 142, 'FILE'),
                (145, 'training', 141, 'DIRECTORY'),
                    (146, 'data_loader.py', 145, 'FILE'),
                    (147, 'training.py', 145, 'FILE'),
        (160, 'ida', 19, 'DIRECTORY'),
            (161, 'prj_01', 160, 'DIRECTORY'),
                (162, 'go.mod', 161, 'FILE'),
                (163, 'ent', 161, 'DIRECTORY'),
                    (164, 'schema', 163, 'DIRECTORY'),
                        (165, 'user.go', 164, 'FILE'),
                    (166, 'client.go', 163, 'FILE'),
                    (167, 'generate.go', 163, 'FILE'),
                    (168, 'user_query.go', 163, 'FILE'),
                    (169, 'user.go', 163, 'FILE');

insert into file_user_mapping(file_id, user_id)
    select f.id, u.id
    from file f, file_user u
    where f.id < 20;

insert into file_user_mapping(file_id, user_id)
    select id, 1
    from file
    where id >= 20 and id < 40;

insert into file_user_mapping(file_id, user_id)
    select id, 2
    from file
    where id >= 40 and id < 60;

insert into file_user_mapping(file_id, user_id)
    select id, 3
    from file
    where id >= 60 and id < 80;

insert into file_user_mapping(file_id, user_id)
    select id, 4
    from file
    where id >= 80 and id < 100;

insert into file_user_mapping(file_id, user_id)
    select id, 5
    from file
    where id >= 100 and id < 120;

insert into file_user_mapping(file_id, user_id)
    select id, 6
    from file
    where id >= 120 and id < 140;

insert into file_user_mapping(file_id, user_id)
    select id, 7
    from file
    where id >= 140 and id < 160;

insert into file_user_mapping(file_id, user_id)
    select id, 8
    from file
    where id >= 160 and id < 180;
