alter table header
drop constraint fk_record,
add constraint fk_record
   foreign key (record_id)
   references record
   on delete cascade;