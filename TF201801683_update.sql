--alter session set current_schema=expadmin;
update TBEXP_EXT_SYS_ENTRY set PROJECT_NO='201812A300009'
where SUBPOENA_NO='J017331001' and COST_UNIT_CODE='12A000' and acct_code='61130123' and amt='15250';
