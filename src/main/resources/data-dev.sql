do $$ begin
 if exists (select * from pg_available_extensions where name='pg_stat_statements')
  then create extension if not exists pg_stat_statements;
 end if;
end $$ ;;

insert into record(id,topic,partition,"offset",key,value,triaged) values(1,'error-test',0,0,'rk1','rv1',false);;
insert into header(record_id,key,value,native) values(1,'hk1','hv1',true);;
insert into header(record_id,key,value,native) values(1,'hk2','hv2',true);;

insert into record(id,topic,partition,"offset",key,value,triaged) values(2,'error-test',0,99999,'rk1','{"customer":{"id":"00112233-4455-6677-8899-aabbccddeeff","name":"Tester-さん ✨"}}',false);;
insert into header(record_id,key,value,native) values(2,'x-exception-message','Exception thrown while invoking com.company.FactoryManagerFacadeProvider#on[1 args]; nested exception com.company.NameValidator: Say what?;\n  nested exception is:\n\tcom.company.StarLookup: cannot find ✨: only * found',false);;
