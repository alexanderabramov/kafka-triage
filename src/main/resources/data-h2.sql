insert into record(id,topic,partition,"offset",key,value,triaged) values(1,'error-test',0,0,'rk1','rv1',false)
insert into header(record_id,key,value,native) values(1,'hk1','hv1',true)
insert into header(record_id,key,value,native) values(1,'hk2','hv2',true)
