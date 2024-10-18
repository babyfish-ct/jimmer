create table container(
	id bigint not null,
	address inet not null
);
alter table container
	add constraint pk_container
	    primary key(id);
insert into container(
	id, address
) values
(1, '127.0.0.1'), (2, '192.168.1.1');