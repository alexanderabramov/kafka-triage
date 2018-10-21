create table record (
    id serial not null,
    key varchar(255),
    "offset" int8,
    partition int4 not null,
    replayed_offset int8,
    timestamp int8,
    topic varchar(255),
    triaged boolean not null,
    value varchar(255),
    primary key (id)
);

create table header (
    id serial not null,
    key varchar(255),
    value varchar(255),
    record_id int4 not null,
    primary key (id)
);

alter table header add constraint fk_record foreign key (record_id) references record;
