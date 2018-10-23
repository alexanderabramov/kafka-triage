create table record (
    id serial not null,
    key varchar(128),
    "offset" int8,
    partition int4 not null,
    replayed_offset int8,
    timestamp int8,
    topic varchar(128),
    triaged boolean not null,
    value text,
    primary key (id),
    constraint unique_record unique(topic, partition, "offset")
);

create table header (
    id serial not null,
    key varchar(128),
    value text,
    record_id int4 not null,
    primary key (id)
);

alter table header add constraint fk_record foreign key (record_id) references record;
