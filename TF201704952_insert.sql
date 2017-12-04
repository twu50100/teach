--alter session set current_schema=expadmin;
--AMP000520170921003
--新增語法
Insert into EXPADMIN.TBEXP_TAX_DETAIL
   (ID, WITHHOLD_ID, EMP_ID, TAX_ID, TAX_NAME, INCOME_FORM, SOURCE_KIND, PAY_CERT_NO,SUBPOENA_NO, SUBPOENA_DATE, TAXBIZ_CODE, CROSS_INCOME, TAX_RATE, WITHHOLDING_TAX, COST_UNIT_CODE, COST_UNIT_NAME, TEX_REMIT,ROSTER_NO, GROUP_ID, TBEXP_ENTRY_TAX_ID, CREATE_USER_ID, CREATE_DATE, VERSION_NO  )
    Values
   ('201710-00000000-000S006A110010000001', '61757843', 'EM6672', 'M221983632', '黃睦雅', '92','92', 'S006A11001','S006A11001', TO_DATE('2017-10-11','YYYY-MM-DD'), '8Z', '4940', '0', '0', 'AMP000', '埔欣通－收', '1','AMP000520170921003', 'IMP00000-0000-0000-S006A110010000001', '', 'F225942034', TO_DATE('2017-10-24','YYYY-MM-DD'), '1');
                       
   
INSERT INTO TBEXP_PROOF_TAX_DETAIL_R (TBEXP_TAX_DETAIL_ID,TBEXP_PROOF_ID)
Values('201710-00000000-000S006A110010000001','9F5188B2-ECE1-42B6-92E0-20B0918D29D3');

INSERT INTO TBEXP_ENTRY_EXP_GROUP(ID, GROUP_ID,TBEXP_ENTRY_ID)
VALUES ('IMP01024-0000-0000-S006A110010000001','IMP00000-0000-0000-S006A110010000001','54B99B8A-3FA0-4EEC-A81C-08915FCC8E50');

--新增語法--AG3000220170929003--1
Insert into EXPADMIN.TBEXP_TAX_DETAIL
   (ID, WITHHOLD_ID, EMP_ID, TAX_ID, TAX_NAME, INCOME_FORM, SOURCE_KIND, PAY_CERT_NO,SUBPOENA_NO, SUBPOENA_DATE, TAXBIZ_CODE, CROSS_INCOME, TAX_RATE, WITHHOLDING_TAX, COST_UNIT_CODE, COST_UNIT_NAME, TEX_REMIT,ROSTER_NO, GROUP_ID, TBEXP_ENTRY_TAX_ID, CREATE_USER_ID, CREATE_DATE, VERSION_NO  )
    Values
   ('201710-00000000-000S006A130010000001','33746450','','F122612082','林建邦', '91','91', 'S006A13001','S006A13001', TO_DATE('2017-10-13','YYYY-MM-DD'), '', '15999', '0.1', '0', 'AG3000', '新泰通－收', '1','AG3000220170929003', 'IMP00000-0000-0000-S006A130010000001', '', 'F225942034', TO_DATE('2017-10-24','YYYY-MM-DD'), '1');
                      
   
INSERT INTO TBEXP_PROOF_TAX_DETAIL_R (TBEXP_TAX_DETAIL_ID,TBEXP_PROOF_ID)
Values('201710-00000000-000S006A130010000001','19F32A85-5C5D-4EC4-9C52-17A5AE5DE271');

INSERT INTO TBEXP_ENTRY_EXP_GROUP(ID, GROUP_ID,TBEXP_ENTRY_ID)
VALUES ('IMP01024-0000-0000-S006A130010000001','IMP00000-0000-0000-S006A130010000001','A519C21E-F6EF-4C9E-8C7C-9794A35C7C4E');

--新增語法--AG3000220170929003--2
Insert into EXPADMIN.TBEXP_TAX_DETAIL
   (ID, WITHHOLD_ID, EMP_ID, TAX_ID, TAX_NAME, INCOME_FORM, SOURCE_KIND, PAY_CERT_NO,SUBPOENA_NO, SUBPOENA_DATE, TAXBIZ_CODE, CROSS_INCOME, TAX_RATE, WITHHOLDING_TAX, COST_UNIT_CODE, COST_UNIT_NAME, TEX_REMIT,ROSTER_NO, GROUP_ID, TBEXP_ENTRY_TAX_ID, CREATE_USER_ID, CREATE_DATE, VERSION_NO  )
    Values
   ('201710-00000000-000S006A130010000002','33746450','','F170119743','林明康', '91','91', 'S006A13001','S006A13001', TO_DATE('2017-10-13','YYYY-MM-DD'), '', '15999', '0.1', '0', 'AG3000', '新泰通－收', '1','AG3000220170929003', 'IMP00000-0000-0000-S006A130010000002', '', 'F225942034', TO_DATE('2017-10-24','YYYY-MM-DD'), '1');
                      
   
INSERT INTO TBEXP_PROOF_TAX_DETAIL_R (TBEXP_TAX_DETAIL_ID,TBEXP_PROOF_ID)
Values('201710-00000000-000S006A130010000002','19F32A85-5C5D-4EC4-9C52-17A5AE5DE271');

INSERT INTO TBEXP_ENTRY_EXP_GROUP(ID, GROUP_ID,TBEXP_ENTRY_ID)
VALUES ('IMP01024-0000-0000-S006A130010000002','IMP00000-0000-0000-S006A130010000002','A519C21E-F6EF-4C9E-8C7C-9794A35C7C4E');

--新增語法--AG3000120170919001--1
Insert into EXPADMIN.TBEXP_TAX_DETAIL
   (ID, WITHHOLD_ID, EMP_ID, TAX_ID, TAX_NAME, INCOME_FORM, SOURCE_KIND, PAY_CERT_NO,SUBPOENA_NO, SUBPOENA_DATE, TAXBIZ_CODE, CROSS_INCOME, TAX_RATE, WITHHOLDING_TAX, COST_UNIT_CODE, COST_UNIT_NAME, TEX_REMIT,ROSTER_NO, GROUP_ID, TBEXP_ENTRY_TAX_ID, CREATE_USER_ID, CREATE_DATE, VERSION_NO  )
    Values
   ('201710-00000000-000S006A130010000003','33746450','BN4009','P223389250','吳寶珠', '91','91', 'S006A13001','S006A13001', TO_DATE('2017-10-13','YYYY-MM-DD'), '', '20000', '0.1', '0', 'AG3000', '新泰通－收', '1','AG3000120170919001', 'IMP00000-0000-0000-S006A130010000003', '', 'F225942034', TO_DATE('2017-10-24','YYYY-MM-DD'), '1');
                      
   
INSERT INTO TBEXP_PROOF_TAX_DETAIL_R (TBEXP_TAX_DETAIL_ID,TBEXP_PROOF_ID)
Values('201710-00000000-000S006A130010000003','180DDD6C-7F3C-4E05-B801-07B184AEB2E1');

INSERT INTO TBEXP_ENTRY_EXP_GROUP(ID, GROUP_ID,TBEXP_ENTRY_ID)
VALUES ('IMP01024-0000-0000-S006A130010000003','IMP00000-0000-0000-S006A130010000003','4F73BDA2-18B5-4566-A1FC-FF05D92DF862');

--新增語法--AG3000120170919001--2
Insert into EXPADMIN.TBEXP_TAX_DETAIL
   (ID, WITHHOLD_ID, EMP_ID, TAX_ID, TAX_NAME, INCOME_FORM, SOURCE_KIND, PAY_CERT_NO,SUBPOENA_NO, SUBPOENA_DATE, TAXBIZ_CODE, CROSS_INCOME, TAX_RATE, WITHHOLDING_TAX, COST_UNIT_CODE, COST_UNIT_NAME, TEX_REMIT,ROSTER_NO, GROUP_ID, TBEXP_ENTRY_TAX_ID, CREATE_USER_ID, CREATE_DATE, VERSION_NO  )
    Values
   ('201710-00000000-000S006A130010000004','33746450','EF5196','Q223394298','蘇宥臻', '91','91', 'S006A13001','S006A13001', TO_DATE('2017-10-13','YYYY-MM-DD'), '', '8000', '0.1', '0', 'AG3000', '新泰通－收', '1','AG3000120170925001', 'IMP00000-0000-0000-S006A130010000004', '', 'F225942034', TO_DATE('2017-10-24','YYYY-MM-DD'), '1');
                      
   
INSERT INTO TBEXP_PROOF_TAX_DETAIL_R (TBEXP_TAX_DETAIL_ID,TBEXP_PROOF_ID)
Values('201710-00000000-000S006A130010000004','5555CFC5-C060-4AA0-A6B1-2753DCAACA54');

INSERT INTO TBEXP_ENTRY_EXP_GROUP(ID, GROUP_ID,TBEXP_ENTRY_ID)
VALUES ('IMP01024-0000-0000-S006A130010000004','IMP00000-0000-0000-S006A130010000004','2FA4D008-99B9-4698-9AFF-DB02CEA210F5');