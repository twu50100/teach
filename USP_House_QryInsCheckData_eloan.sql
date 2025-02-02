USE [SKL_LOAN]
GO
/****** Object:  StoredProcedure [dbo].[USP_House_QryInsCheckData_eloan]    Script Date: 2019/12/25 下午 04:11:16 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
 -- Author:EC0416
  -- Create date: 2019/11/11
  -- Description:	查詢上線首日之前六個月的個資相關資訊已提供核保作業勾稽資料。
-- =============================================
ALTER PROCEDURE [dbo].[USP_House_QryInsCheckData_eloan]
AS
begin 
	create table #TempLNLMSP(
			LMSLLD varchar(8)   --撥款日
			,LMSFLA varchar(15)  --撥款金額
			,LMSACN varchar(20)  --戶號
			,LMSAPN varchar(3)   --額度
		)

		create table #TempLOAN(
			ApproveDate varchar(8)
			,ApproveAmount  varchar(18)
			,LoanKey uniqueidentifier
			,CaseNO varchar(20)
		)
		
		create table #TempInsCheckData(
             DataDate varchar(8)
            ,LoanKey uniqueidentifier 
			,CaseNO varchar(20) 
			,CustId varchar(20)
            ,CustNo varchar(10)
			,CustName nvarchar(70)
			,ApproveDate varchar(8)
			,CheckAmount  varchar(18)
			,LMSLLD varchar(8)
			,LMSFLA varchar(15)
 			,IncomeSalary varchar(13)
			,IncomeWork varchar(13)
			,IncomeBusiness varchar(13)
			,IncomeRant varchar(13)
            ,IncomeInterest varchar(13)
            ,IncomeOther varchar(13)
            ,IncomeYear varchar(13)
			,Estimate varchar(20)
            ,AngentEmpName  varchar(80)
			,AngentEmpNo  varchar(6)
			,AngentUnitNo  varchar(6)						
		    ,LastUpdateDate datetime
            )


		--判斷house_insCheckData是否有資料，若無則跑上限首日前六個月的資料
		if (select count(loankey) from skl_loan.dbo.house_insCheckData)=0
		begin
			declare @SourceDB varchar(50)  select top 1 @SourceDB=SourceDB FROM [SKL_CORE].[dbo].[Core_QueryItem] 
			declare @cmd nvarchar(MAX) set @cmd=''
			declare @GetDate varchar(10) set @GetDate = convert(varchar(8),dateadd(m,-6,getdate()),112)
			declare @EtlDate varchar(8) set @EtlDate = convert(varchar(8),dateadd(m,0,getdate()),112)
			

			--篩選AS400撥款日上線首日6個月內資料
			set @cmd = N''+
			' insert into #TempLNLMSP'+
			' select * from openquery(AS400,''select LMSLLD,LMSFLA,LMSACN,LMSAPN from '+@SourceDB+'.LA$LMSP'
			+' where LMSLLD between '  + @GetDate + ' and ' + @EtlDate + ''')'
			print @cmd
			exec(@cmd)

			;with ApproveDate as (
				--核貸日	LoanCloseStatus = 1(結案)且LoanCloseDecision = 51(核准)
				--20191220	ES1703	調整核准日的邏輯，已頁面批覆書抓取的日期為準。
				select 
					FH.Loankey
					,CaseNO
					--,LastFlowUpdateDate 
					,ApproveDate 
					from SKL_LOAN.dbo.Flow_House FH
					inner join SKL_LOAN.dbo.House_CustMain HC 
					on FH.LoanKey = HC.LoanKey
				where LoanCloseStatus = '1' and LoanCloseDecision = '51'  				
			),ApproveAmount as (
				--核准金額
				--House_AccountChief	審核結果輸入		取最後核准角色(LastUpdateRoleNo)的金額
				--Temp_House_AccountChange	授信條件變更審核表	取最後核准角色(LastUpdateRoleNo)的金額
				select 
					Loankey
					,sum(Amount) as ApproveAmount
					,LastUpdateRoleNo 
					from (
						select 
							ROW_NUMBER() over (partition by Loankey,Ukey order by House_AccountChief.LastUpdateDate desc) SeqNo
							,LoanKey
							,Ukey
							,Amount
							,LastUpdateRoleNo 
						from SKL_LOAN.dbo.House_AccountChief 
					) T1 where SeqNo = '1' group by Loankey,LastUpdateRoleNo 
				union
				select 
					Loankey
					,sum(Amount) as ApproveAmount
					,LastUpdateRoleNo 
					from (
						select 
							ROW_NUMBER() over (partition by Loankey,Ukey order by House_AccountChange.LastUpdateDate desc) SeqNo
							,LoanKey
							,Ukey
							,Amount
							,LastUpdateRoleNo 
						from SKL_LOAN.dbo.House_AccountChange 
					) T2 where SeqNo = '1' group by Loankey,LastUpdateRoleNo 
			),Summary as (
				select 
					LoanKey,
					CaseNO,
					ApproveDate,
					ApproveAmount 
					from (
						select 
							ROW_NUMBER() over (partition by ApproveDate.LoanKey order by convert(int,LastUpdateRoleNo) desc) SeqNo
							,ApproveDate.LoanKey
							,ApproveDate.CaseNO
							,ApproveDate.ApproveDate
							,ApproveAmount.ApproveAmount
							,ApproveAmount.LastUpdateRoleNo
						from ApproveDate 
						inner join ApproveAmount on ApproveDate.LoanKey = ApproveAmount.LoanKey
					) T where SeqNo = '1'
			)
		
			insert into #TempLOAN
			select 
				CONVERT(varchar(8), max(ApproveDate),112) as ApproveDate
				,ApproveAmount
				,LoanKey
				,CaseNO  
			from Summary
			group by ApproveAmount,LoanKey,CaseNO

			--將資料寫入#TempInsCheckData
			insert into #TempInsCheckData 
			select 
				T.DataDate
				,T.LoanKey
				,T.CaseNO
				,t.CustId
				,T.CustNo
				,T.CustName
				,T.ApproveDate
				,T.CheckAmount
				,T.LMSLLD
				,T.LMSFLA
				,T.IncomeSalary
				,T.IncomeWork
				,T.IncomeBusiness
				,T.IncomeRant
				,T.IncomeInterest
				,T.IncomeOther
				,T.IncomeYear
				,T.Estimate
				,T.AngentEmpName
				,T.AngentEmpNo
				,T.AngentUnitNo
				,T.LastUpdateDate
			from (
				select 
					ROW_NUMBER() over (partition by #TempLOAN.CaseNO order by convert(int,#TempLOAN.ApproveDate) desc) SeqNo
					,CONVERT(varchar(8), GETDATE(),112)  as DataDate --資料日期(年月日)
					,#TempLOAN.LoanKey as LoanKey
					,#TempLOAN.CaseNO as CaseNO--放款案號
					,m.CustId as CustId
					,m.CustNo as CustNo--借款人戶號
					,m.CustName as CustName--借款人姓名
					,#TempLOAN.ApproveDate as ApproveDate--核貸日
					,#TempLOAN.ApproveAmount as CheckAmount --核貸金額
					,tempLN.LMSLLD as LMSLLD --撥款日
					,CAST(tempLN.LMSFLA as DECIMAL ) AS LMSFLA
					,ISNULL(cast(round((i.IncomeSalary/10000),0)as int),0) as IncomeSalary
					,ISNULL(cast(round((i.IncomeWork/10000),0)as int),0) as IncomeWork
					,ISNULL(cast(round((i.IncomeBusiness/10000),0)as int),0) as IncomeBusiness
					,ISNULL(cast(round((i.IncomeRant/10000),0)as int),0)  as IncomeRant 
					,ISNULL(cast(round((i.IncomeInterest/10000),0)as int),0) as IncomeInterest
					,ISNULL(cast(round((i.IncomeOther/10000),0)as int),0) as IncomeOther
					,ISNULL(cast(round((i.IncomeYear/10000),0)as int),0) as IncomeYear
					,ISNULL(cast(round(((i.EstimateCust+i.EstimateMate+i.EstimateOther)/10000),0)as int),0) as Estimate
					,t.AngentEmpName as AngentEmpName  --介紹人
					,t.AngentEmpNo as AngentEmpNo  --員工代號
					,t.AngentUnitNo as AngentUnitNo  --單位代號			
					,getdate() as LastUpdateDate--產製時間
				from skl_loan.dbo.Flow_House f 
				inner join #TempLOAN on #TempLOAN.LoanKey=f.LoanKey
				inner join skl_loan.dbo.House_CustMain m on f.LoanKey=m.LoanKey
				inner join skl_loan.dbo.House_CustIncome i on m.LoanKey=i.LoanKey
				left join skl_loan.dbo.House_Introduce t on t.LoanKey=m.LoanKey
				left join (
					select 
						fh.CaseNO as CaseNO   --案號
						,MAX(tn.LMSLLD) as LMSLLD  --撥款日
						,SUM(CAST(tn.LMSFLA  as float))as LMSFLA  --撥款金額
					from skl_loan.dbo.Flow_House fh
					inner join skl_loan.dbo.Book_Account ba on ba.LoanKey=fh.LoanKey
					inner join #TempLOAN  on fh.LoanKey=#TempLOAN.LoanKey
					inner join skl_loan.dbo.House_CustMain m on #TempLOAN.LoanKey=m.LoanKey
					inner join #TempLNLMSP tn on m.CustNo= tn.LMSACN and ba.AccNo=tn.LMSAPN					 	 
					group by fh.CaseNO						
				)tempLN ON tempLN.CaseNO=#TempLOAN.CaseNO		    
				where  (#TempLOAN.ApproveDate between @GetDate and @EtlDate  --核准日日期區間
				or tempLN.LMSLLD between @GetDate and @EtlDate) --撥款日日期區間
				and m.LoanWay <> '13'	--20191220	ES1703	排除貸後授變的案件
					
			)T WHERE T.SeqNo='1'
	
		end

		--判斷house_insCheckData是否有資料，若有資料則跑上線次日每日執行
		else 
		begin

			declare @SourceDB1 varchar(50)  select top 1 @SourceDB1=SourceDB FROM [SKL_CORE].[dbo].[Core_QueryItem] 
			declare @cmd1 nvarchar(MAX) set @cmd1=''
			--當天資料
			declare @GetDate1 varchar(10) set @GetDate1 = convert(varchar(8),dateadd(m,0,getdate()),112)				
			set @cmd1 = N''+
			' insert into #TempLNLMSP'+
			' select * from openquery(AS400,'' select LMSLLD,LMSFLA,LMSACN,LMSAPN from '+@SourceDB1+'.LA$LMSP'
			+' where LMSLLD = '+  +@GetDate1+''')'
			print @cmd1
			exec(@cmd1)

			;with ApproveDate as (
				--核貸日	LoanCloseStatus = 1(結案)且LoanCloseDecision = 51(核准)
				select 
					Loankey
					,CaseNO
					,LastFlowUpdateDate 
				from SKL_LOAN.dbo.Flow_House 
				where LoanCloseStatus = '1' and LoanCloseDecision = '51' 				
			),ApproveAmount as (
				--核准金額
				--House_AccountChief	審核結果輸入		取最後核准角色(LastUpdateRoleNo)的金額
				--Temp_House_AccountChange	授信條件變更審核表	取最後核准角色(LastUpdateRoleNo)的金額
				select 
					Loankey
					,sum(Amount) as ApproveAmount
					,LastUpdateRoleNo 
				from (
					select 
						ROW_NUMBER() over (partition by Loankey,Ukey order by House_AccountChief.LastUpdateDate desc) SeqNo
						,LoanKey
						,Ukey
						,Amount
						,LastUpdateRoleNo 
					from SKL_LOAN.dbo.House_AccountChief 
				) T1 where SeqNo = '1' group by Loankey,LastUpdateRoleNo 
				union
				select 
					Loankey
					,sum(Amount) as ApproveAmount
					,LastUpdateRoleNo 
				from (
					select 
						ROW_NUMBER() over (partition by Loankey,Ukey order by House_AccountChange.LastUpdateDate desc) SeqNo
						,LoanKey
						,Ukey
						,Amount
						,LastUpdateRoleNo 
					from SKL_LOAN.dbo.House_AccountChange 
				) T2 where SeqNo = '1' group by Loankey,LastUpdateRoleNo 
			),Summary as (
				select 
					LoanKey
					,CaseNO
					,ApproveDate
					,ApproveAmount 
				from (
					select 
						ROW_NUMBER() over (partition by ApproveDate.LoanKey  order by convert(int,LastUpdateRoleNo) desc) SeqNo
						,ApproveDate.LoanKey
						,ApproveDate.CaseNO
						,ApproveDate.LastFlowUpdateDate as ApproveDate
						,ApproveAmount.ApproveAmount
						,ApproveAmount.LastUpdateRoleNo
					from ApproveDate 
					inner join ApproveAmount on ApproveDate.LoanKey = ApproveAmount.LoanKey
				) T where SeqNo = '1'
			)

			insert into #TempLOAN
			select 
				CONVERT(varchar(8), max(ApproveDate),112) as ApproveDate
				,ApproveAmount
				,LoanKey
				,CaseNO  
			from Summary
			group by ApproveAmount,LoanKey,CaseNO

			--將資料寫入house_insCheckData
			--insert into #TempInsCheckData 
			;with TempSummary as (
				select
					CONVERT(varchar(8),GETDATE(),112)  as DataDate --資料日期(年月日)
					,#TempLOAN.LoanKey as LoanKey
					,#TempLOAN.CaseNO as CaseNO--放款案號
					,m.CustId as CustId	
					,m.CustNo as CustNo--借款人戶號
					,m.CustName as CustName--借款人姓名
					,#TempLOAN.ApproveDate as ApproveDate--核貸日
					,#TempLOAN.ApproveAmount AS CheckAmount		--核貸金額				
				    ,MAX(#TempLNLMSP.LMSLLD) as LMSLLD  --撥款日
					,SUM(CAST(#TempLNLMSP.LMSFLA AS int)) as LMSFLA 			 
					,ISNULL(cast(round((i.IncomeSalary/10000),0)as int),0) as IncomeSalary
					,ISNULL(cast(round((i.IncomeWork/10000),0)as int),0) as IncomeWork
					,ISNULL(cast(round((i.IncomeBusiness/10000),0)as int),0) as IncomeBusiness
					,ISNULL(cast(round((i.IncomeRant/10000),0)as int),0)  as IncomeRant 
					,ISNULL(cast(round((i.IncomeInterest/10000),0)as int),0) as IncomeInterest
					,ISNULL(cast(round((i.IncomeOther/10000),0)as int),0) as IncomeOther
					,ISNULL(cast(round((i.IncomeYear/10000),0)as int),0) as IncomeYear
					,ISNULL(cast(round(((i.EstimateCust+i.EstimateMate+i.EstimateOther)/10000),0)as int),0) as Estimate
					,t.AngentEmpName as AngentEmpName  --介紹人
					,t.AngentEmpNo as AngentEmpNo  --員工代號
					,t.AngentUnitNo as AngentUnitNo  --單位代號			
					,getdate()as LastUpdateDate--產製時間
				from #TempLNLMSP			
				inner join skl_loan.dbo.House_CustMain m on m.CustNo=#TempLNLMSP.LMSACN			
				inner join skl_loan.dbo.Flow_House h on h.LoanKey= m.LoanKey
			    inner join skl_loan.dbo.Book_Account ba on  ba.AccNo=#TempLNLMSP.LMSAPN	 and ba.LoanKey= m.LoanKey
			    inner join #TempLOAN on #TempLOAN.LoanKey=m.LoanKey and #TempLOAN.caseno=h.caseno
			    inner join skl_loan.dbo.House_CustIncome i on h.LoanKey=i.LoanKey
			    left join skl_loan.dbo.House_Introduce t on t.LoanKey=h.LoanKey	
                group by 
					#TempLNLMSP.LMSACN,#TempLOAN.LoanKey,#TempLOAN.CaseNO,m.CustId,m.CustNo,m.CustName,#TempLOAN.ApproveDate
                   ,#TempLOAN.ApproveAmount,i.IncomeSalary,i.IncomeWork,i.IncomeBusiness,i.IncomeRant
					,i.IncomeInterest,i.IncomeOther,i.IncomeYear,i.EstimateCust,i.EstimateMate,i.EstimateOther,t.AngentEmpName,t.AngentEmpNo,t.AngentUnitNo						
				union
				select
					CONVERT(varchar(8),GETDATE(),112)  as DataDate --資料日期(年月日)
					,#TempLOAN.LoanKey as LoanKey
					,#TempLOAN.CaseNO as CaseNO--放款案號
					,main.CustId as CustId
					,main.CustNo as CustNo--借款人戶號
					,main.CustName as CustName--借款人姓名
					,#TempLOAN.ApproveDate as ApproveDate--核貸日
					,#TempLOAN.ApproveAmount AS CheckAmount		--核貸金額					
					,'' as LMSLLD--撥款日
					,'' as LMSFLA--撥款金額
					,ISNULL(cast(round((i.IncomeSalary/10000),0)as int),0) as IncomeSalary
					,ISNULL(cast(round((i.IncomeWork/10000),0)as int),0) as IncomeWork
					,ISNULL(cast(round((i.IncomeBusiness/10000),0)as int),0) as IncomeBusiness
					,ISNULL(cast(round((i.IncomeRant/10000),0)as int),0)  as IncomeRant 
					,ISNULL(cast(round((i.IncomeInterest/10000),0)as int),0) as IncomeInterest
					,ISNULL(cast(round((i.IncomeOther/10000),0)as int),0) as IncomeOther
					,ISNULL(cast(round((i.IncomeYear/10000),0)as int),0) as IncomeYear
					,ISNULL(cast(round(((i.EstimateCust+i.EstimateMate+i.EstimateOther)/10000),0)as int),0) as Estimate
					,t.AngentEmpName as AngentEmpName  --介紹人
					,t.AngentEmpNo as AngentEmpNo  --員工代號
					,t.AngentUnitNo as AngentUnitNo  --單位代號			
					, getdate() as LastUpdateDate--產製時間
				from skl_loan.dbo.Flow_House f 
				inner join skl_loan.dbo.House_CustMain main on f.LoanKey=main.LoanKey
				inner join #TempLOAN on #TempLOAN.LoanKey=main.LoanKey
				inner join skl_loan.dbo.House_CustIncome i on f.LoanKey=i.LoanKey
				left join skl_loan.dbo.House_Introduce t on t.LoanKey=f.LoanKey
				inner join skl_loan.dbo.Book_Account ba on ba.LoanKey=f.LoanKey		
				where #TempLOAN.ApproveDate = @GetDate1  --日期
				and main.LoanWay <> '13'	--20191220	ES1703	排除貸後授變的案件
				group by 
					#TempLOAN.LoanKey,#TempLOAN.CaseNO,main.custid,main.CustNo,main.CustName,#TempLOAN.ApproveAmount,#TempLOAN.ApproveDate
					,i.IncomeSalary,i.IncomeWork,i.IncomeBusiness,i.IncomeRant,i.IncomeInterest,i.IncomeOther,i.IncomeYear,i.EstimateCust,i.EstimateMate
					,i.EstimateOther,t.AngentEmpName,t.AngentEmpNo,t.AngentUnitNo		
			),TempOrder as(
					select 			
						TSS.DataDate
						,TSS.LoanKey
						,TSS.CaseNO
						,TSS.CustId
						,TSS.CustNo
						,TSS.CustName
						,TSS.ApproveDate
						,TSS.CheckAmount
						,TSS.LMSLLD
						,TSS.LMSFLA
						,TSS.IncomeSalary
						,TSS.IncomeWork
						,TSS.IncomeBusiness
						,TSS.IncomeRant
						,TSS.IncomeInterest
						,TSS.IncomeOther
						,TSS.IncomeYear
						,TSS.Estimate
						,TSS.AngentEmpName
						,TSS.AngentEmpNo
						,TSS.AngentUnitNo
						,TSS.LastUpdateDate
					from (
					select
						 ROW_NUMBER() over (partition by TS.CaseNO order by convert(int,TS.ApproveDate) desc)SeqNo
					    ,TS.DataDate
						,TS.LoanKey
						,TS.CaseNO
						,TS.CustId
						,TS.CustNo
						,ts.CustName
						,TS.ApproveDate
						,TS.CheckAmount
						,TS.LMSLLD
						,TS.LMSFLA
						,TS.IncomeSalary
						,TS.IncomeWork
						,TS.IncomeBusiness
						,TS.IncomeRant
						,TS.IncomeInterest
						,TS.IncomeOther
						,TS.IncomeYear
						,TS.Estimate
						,TS.AngentEmpName
						,TS.AngentEmpNo
						,TS.AngentUnitNo
						,TS.LastUpdateDate 
						from TempSummary ts
					)TSS
					WHERE TSS.SeqNo=1			
			)			

			insert into #TempInsCheckData 
			select  * from TempOrder
	end

		drop table #TempLNLMSP
		drop table #TempLOAN

		insert into House_InsCheckData
		select
				DataDate  as DataDate --資料日期(年月日)
				,LoanKey
				,CustNo--借款人戶號
				,CustName--借款人姓名
				,CaseNO--放款案號
				,ApproveDate--核貸日
				,CheckAmount--核貸金額				
				,LMSLLD  --撥款日
				,LMSFLA 		 
				,IncomeSalary
				,IncomeWork
				,IncomeBusiness
				,IncomeRant 
				,IncomeInterest
				,IncomeOther
				,IncomeYear
				,Estimate
				,AngentEmpName as AngentEmpName  --介紹人
				,AngentEmpNo as AngentEmpNo  --員工代號
				,AngentUnitNo as AngentUnitNo  --單位代號			
				,LastUpdateDate--產製時間
		from #TempInsCheckData  
		
		Select
		        i.CaseNo 
			+ '|'+ ISNULL (i.CustId,'')
			+ '|'+ ISNULL (i.CustName,'') 
			+ '|'+ CONVERT(varchar(8),i.ApproveDate,112) 
			+ '|'+ i.CheckAmount
			+ '|'+ ISNULL (i.LMSLLD,'') 
			+ '|'+ ISNULL (i.LMSFLA,'') 
			+ '|'+ i.IncomeSalary 
			+ '|'+ i.IncomeWork
			+ '|'+ i.IncomeBusiness
			+ '|'+ i.IncomeRant
			+ '|'+ i.IncomeInterest
			+ '|'+ i.IncomeOther
			+ '|'+ i.IncomeYear 
			+ '|'+ i.Estimate
			+ '|'+ ISNULL (i.AngentEmpName,'') 
			+ '|'+ ISNULL (i.AngentEmpNo,'') 
			+ '|'+ ISNULL (i.AngentUnitNo,'') 
		    as txt
		From #TempInsCheckData  i

		drop table #TempInsCheckData  
end
