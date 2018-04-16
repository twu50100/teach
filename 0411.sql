SELECT 
r.*, 
c.EXP_APPL_NO,
--distinct 
t.code,
--p.name,
t.name,
e.INCOME_ID_TYPE,
e.INCOME_ID,
P.NAME,
k.name,
ct.name,
M.TBEXP_OLD_EXPAPPL_C_ID,
s.subpoena_no,

ty.name
FROM TBEXP_EXPAPPL_C C
inner join TBEXP_ENTRY e ON E.TBEXP_entry_group_id =c.TBEXP_entry_group_id
left join TBEXP_TAX_DETAIL_EXPAPPL_C_R R on r.TBEXP_EXPAPPL_C_id=c.id
inner join tbexp_acc_title t on t.id=e.tbexp_acc_title_id
INNER JOIN TBEXP_REMIT_MONEY M ON M.TBEXP_EXPAPPL_C_ID=C.ID
INNER JOIN TBEXP_PAYBACK_TYPE P ON M.TBEXP_PAYBACK_TYPE_ID=P.ID
inner join tbexp_subpoena s on s.id=c.tbexp_subpoena_id
inner join TBEXP_ENTRY_TYPE ty on ty.id=e.TBEXP_ENTRY_TYPE_ID
inner join TBEXP_ACC_CLASS_TYPE ct on ct.id=t.TBEXP_ACC_CLASS_TYPE_ID
inner join TBEXP_ACC_KIND_TYPE k on k.id=t.TBEXP_ACC_KIND_TYPE_ID
inner join tbexp_appl_state a on a.id=c.tbexp_appl_state_id
where  s.subpoena_no in('S177214002','S177214003','S177305003')

k.name in ('扣繳','扣繳(外)','免扣繳')

and t.code in('20210222','20210223','20210224','20210225','20210229','20210231'
,'20210232','20210233','20210234','20210235','20210236','20210237','20210238'
,'20210239','20210241','20210243','20210247','20210252','20210261','20210262','20210263')