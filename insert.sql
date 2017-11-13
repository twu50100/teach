--alter session set current_schema=expadmin;
--function sql
--C11.2.4 國外出差(研修)旅費申請總表
insert into tbexp_function values('C1124000-0000-0000-0000-000000000000','C11.02.04',
'國外出差(研修)旅費申請總表','/faces/kernel/gae/query/querytravellearnexp/ovsatripreport/readOvsaTripReport.jsp',
'C1120000-0000-0000-0000-000000000000',1,0,to_date('2017/11/13','YYYY/MM/DD'),
'8770b65e-7ceb-4224-b97a-49e6d94f3b22',to_date('2017/11/13','YYYY/MM/DD'),
'','',1);

INSERT INTO tbexp_item (id,name,create_date,update_date,version_no)
     VALUES ('10000000-C011-0002-0004-000000000001',
               '/faces/kernel/gae/query/querytravellearnexp/ovsatripreport/readOvsaTripReport.jsp',
               to_date('2017/11/13','YYYY/MM/DD'),'', 1);
                          
INSERT INTO tbexp_function_item_r (tbexp_function_id, tbexp_item_id)
     VALUES ('C1124000-0000-0000-0000-000000000000','10000000-C011-0002-0004-000000000001');