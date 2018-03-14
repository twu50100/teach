package tw.com.skl.exp.kernel.model6.dao.jpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.orm.jpa.JpaCallback;

import tw.com.skl.common.model6.dao.jpa.BaseDaoImpl;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.BudExpOverDetail;
import tw.com.skl.exp.kernel.model6.bo.BudExpOverrunAppl;
import tw.com.skl.exp.kernel.model6.bo.DepartmentMail;
/**RE201201260_二代健保 cm9539 2013/01/11 B7.18補充健保費格式代號維護  start*/
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
/**RE201201260_二代健保 cm9539 2013/01/11 B7.18補充健保費格式代號維護  end*/
import tw.com.skl.exp.kernel.model6.dao.AccTitleDao;
import tw.com.skl.exp.kernel.model6.dao.BudExpOverrunExplainDao;
import tw.com.skl.exp.kernel.model6.dao.DepartmentMailDao;
import tw.com.skl.exp.kernel.model6.dto.HealthAcctIncomeFormDto;

/**
 * RE201404290_系統寄發預算實支表&線上填寫實支異常說明。 UC10.8.12 寄發部門MAIL設定 DAO類別
 * 
 * @author EC0416
 * @version 1.0, 2015/7/22
 */
public class BudExpOverrunExplainDaoImpl extends BaseDaoImpl<BudExpOverrunAppl, String> implements BudExpOverrunExplainDao {

	// mail批次
	// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
	public List findBudExpOverrunDetail(String YYYYMMDD) {
		final StringBuffer queryString = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();

		queryString.append("   SELECT     ");
		queryString.append("   TO_CHAR ( M.BICODE) as BICODE,             ");
		queryString.append("   DECODE(MB.AAMT,NULL,0,MB.AAMT) AS MM1AMT,     ");
		queryString.append("   DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,     ");
		queryString.append("   DECODE(MA.AAMT,NULL,0,MA.AAMT) AS MM3AMT,     ");
		queryString.append("   TO_CHAR(M.DEPCODE) AS DEPCODE     ");
		queryString.append("   FROM (SELECT DISTINCT  T.DEPCODE, ABT.ID, ABT.BICODE ");
		queryString.append("   FROM (SELECT   DISTINCT  DEP.BUDGET_DEP_CODE  AS DEPCODE     ");
		queryString.append("   FROM TBEXP_DEPARTMENT DEP     ");
		queryString.append("   INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON      ");
		queryString.append("   DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID      ");
		queryString.append("   WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR      ");
		queryString.append("   DEP.BUDGET_DEP_CODE      ");
		queryString.append("   IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000') ) ) T,      ");
		queryString.append("   (SELECT   BI.ID,BI.CODE AS BICODE,BI.NAME  AS BINAME  FROM TBEXP_BUDGET_ITEM BI       ");
		queryString.append("   INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID     ");
		queryString.append("   WHERE BI.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')) ABT	) M    ");
		queryString.append("   LEFT JOIN (     ");
		queryString.append("   SELECT   MM.BCODE AS BCODE, MM.DCODE AS DCODE, SUM(MM.BAMT) AS BAMT       ");
		queryString.append("   FROM (     ");
		queryString.append("   SELECT  MAIN.SUBPOENA_DATE,B.CODE AS BCODE, DEP.BUDGET_DEP_CODE AS DCODE,    ");
		queryString.append("   DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT)  AS BAMT     ");
		queryString.append("   FROM  (   ");
		queryString.append("   SELECT  E.TBEXP_ENTRY_TYPE_ID,E.COST_UNIT_CODE,E.COST_CODE,E.TBEXP_ACC_TITLE_ID,E.AMT,MID.CODE AS MCODE,BIG.CODE AS BCODE,MAIN.SUBPOENA_DATE	     ");
		queryString.append("   FROM TBEXP_BIG_TYPE BIG     ");
		queryString.append("   INNER JOIN TBEXP_MIDDLE_TYPE MID  ON BIG.ID = MID.TBEXP_BIG_TYPE_ID     ");
		queryString.append("   INNER JOIN TBEXP_EXP_MAIN MAIN   ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)     ");
		queryString.append("   INNER JOIN TBEXP_ENTRY E   ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID     ");
		queryString.append("   WHERE   ");
		queryString.append("   TO_CHAR(SUBPOENA_DATE,'YYYYMM')  ");
		queryString.append("   BETWEEN   ");
		queryString.append("   TO_CHAR(SUBSTR('").append(YYYYMMDD).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR(SUBSTR('").append(YYYYMMDD).append("',0,6)) ");
		queryString.append("   AND BIG.CODE!='16') MAIN     ");
		queryString.append("   INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("   INNER JOIN TBEXP_DEPARTMENT DEP ON MAIN.COST_UNIT_CODE = DEP.CODE     ");
		queryString.append("   INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID     ");
		queryString.append("   INNER JOIN TBEXP_ACC_TITLE ACCT ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID     ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID  =B.ID     ");
		queryString.append("   INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID     ");
		queryString.append("   WHERE      ");
		queryString.append("   ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')      ");
		queryString.append("    OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR BCODE!='15' )      ");
		queryString.append("   AND  (MCODE!='A60' OR MAIN.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MCODE NOT IN ('T05','T12','Q10'))) )      ");
		queryString.append("   AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')      ");
		queryString.append("   UNION ALL      ");
		queryString.append("   SELECT      ");
		queryString.append("   ESE.SUBPOENA_DATE,B.CODE AS BCODE,DEP.BUDGET_DEP_CODE AS DCODE, DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT      ");
		queryString.append("   FROM (     ");
		queryString.append("   SELECT   TBEXP_ENTRY_TYPE_ID,ACCT_CODE,COST_UNIT_CODE,AMT,SUBPOENA_DATE   ");
		queryString.append("   FROM TBEXP_EXT_SYS_ENTRY ESE     ");
		queryString.append("   WHERE   ");
		queryString.append("   TO_CHAR(SUBPOENA_DATE,'YYYYMM')  ");
		queryString.append("   BETWEEN   ");
		queryString.append("   (SUBSTR('").append(YYYYMMDD).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR(SUBSTR('").append(YYYYMMDD).append("',0,6)))  ESE ");
		queryString.append("   INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID        ");
		queryString.append("   INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE       ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID       ");
		queryString.append("   INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID     ");
		queryString.append("   INNER JOIN  TBEXP_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE     ");
		queryString.append("   INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID     ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("   WHERE B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')     ");
		queryString.append("   AND ESE.SUBPOENA_NO NOT IN('J827110002','J827115006','J827115009','J827115010'))MM  ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		queryString.append("   GROUP BY  MM.BCODE,MM.DCODE )MM ON  M.BICODE = MM.BCODE AND MM.DCODE=M.DEPCODE    ");
		queryString.append("   LEFT JOIN(     ");
		queryString.append("   SELECT   B.CODE AS BCODE,   ");
		queryString.append("   MON.DEP_CODE AS DCODE,   ");
		queryString.append("   SUM(MON.BUDGET_ITEM_AMT )AS AAMT     ");
		queryString.append("   FROM TBEXP_MONTH_BUDGET MON     ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID     ");
		queryString.append("   INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID     ");
		queryString.append("   WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)     ");
		queryString.append("   BETWEEN   ");
		queryString.append("   (SUBSTR('").append(YYYYMMDD).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR(SUBSTR('").append(YYYYMMDD).append("',0,6)) ");
		queryString.append("   AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')GROUP BY B.CODE,MON.DEP_CODE)    ");
		queryString.append("   MB ON MB.BCODE=M.BICODE AND MB.DCODE=M.DEPCODE    ");
		queryString.append("   LEFT JOIN(     ");
		queryString.append("   SELECT   B.CODE AS BCODE,   ");
		queryString.append("   MON.DEP_CODE AS DCODE,   ");
		queryString.append("   SUM(MON.BUDGET_ITEM_AMT )AS AAMT     ");
		queryString.append("   FROM TBEXP_MONTH_BUDGET MON     ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID     ");
		queryString.append("   INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID     ");
		queryString.append("   WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)     ");
		queryString.append("   BETWEEN   ");
		queryString.append("   (SUBSTR('").append(YYYYMMDD).append("',0,4) || '01')");
		queryString.append("  AND (SUBSTR('").append(YYYYMMDD).append("',0,4) || '12')");
		queryString.append("   AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')GROUP BY B.CODE,MON.DEP_CODE)    ");
		queryString.append("   MA ON MA.BCODE=M.BICODE AND MA.DCODE=M.DEPCODE    ");
		// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
		List list = new ArrayList();
		// final String sqlString =
		// StringUtils.queryStringAssembler(queryString.toString(), params);

		list = getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query query = em.createNativeQuery(queryString.toString());
				return query.getResultList();
			}
		});
		return list;

	}

	public List<BudExpOverrunAppl> findError() {
		// TODO Auto-generated method stub
		return null;
	}

	public BudExpOverrunAppl findbudexpAppl(String depcode, String YYYYMM) {
		StringBuffer queryString = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("   select   a  from BudExpOverrunAppl a     ");
		queryString.append("   where a.erYymm =:YYYYMM              ");
		// 10/6
		if (depcode.equals("A2B000") || depcode.equals("A2F000") || depcode.equals("A2K000") || depcode.equals("A2M000") || depcode.equals("A2Y000") || depcode.equals("111500") || depcode.equals("A2E000") || depcode.equals("A2S1Q0")) {
			queryString.append("   and a.departmentID =:depcode          ");

		} else {
			queryString.append("   and a.department.code =:depcode          ");
		
		}
		
		params.put("YYYYMM", YYYYMM);
		params.put("depcode", depcode);

		List<BudExpOverrunAppl> list = findByNamedParams(queryString.toString(), params);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list.get(0);
		}
	}

	// 利用異常年月 找尋所有的 預算實支異常說明申請的資料
	public List<BudExpOverrunAppl> findbudexpoverrunAppl(String YYYYMM) {
		StringBuffer queryString = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("   select   a  from BudExpOverrunAppl a     ");
		queryString.append("   where a.erYymm =:YYYYMM              ");
		params.put("YYYYMM", YYYYMM);
		return findByNamedParams(queryString.toString(), params);

	}

	public List<BudExpOverDetail> findDetail(String resApplNo) {
		// TODO Auto-generated method stub
		return null;
	}

	public List findDepartmentMail(String depcode) {
		// TODO Auto-generated method stub
		return null;
	}

	public void excutesql(final String querysql) {

		getJpaTemplate().execute(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {

				Query query = em.createNativeQuery(querysql.toString());

				int updateCount = query.executeUpdate();

				return updateCount;
			}
		});
	}
	

	// RE201702775_預算實支追蹤表單_費用系統 ec0416 2017/9/29 start
	public List findBudExpOverrunAppl(String er_yymm) {

		StringBuffer queryString = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();

		queryString.append("	 SELECT   	 ");
		queryString.append("	T1.errorYYMM as er_yymm	,  ");
		queryString.append("	DEPCODE as depCode	,  ");
		queryString.append("	T1.res_appl_no as res_appl_no	,  ");
		queryString.append("	 MM1AMT as MM1AMT 	,  ");
		queryString.append("	 MM2AMT as MM2AMT	,  ");
		queryString.append("	(MM1AMT-MM2AMT) as dep_count_last	,  ");
		queryString.append("	round(NVL(MM2AMT/NULLIF(MM1AMT,0),0)*100,2)||'%' as rate,	 ");
		queryString.append("	(MM1AMT+YY1AMT) as year_dep_budget	,  ");
		queryString.append("	MM2AMT as year_dep_amt	,  ");
		queryString.append("	((MM1AMT+YY1AMT)-MM2AMT)as year_dep_last	,  ");
		queryString.append("	round(NVL(MM2AMT/NULLIF((MM1AMT+YY1AMT),0),0)*100,2)||'%' as dep_year_rate,	 ");
		queryString.append("	T1.budgetCode as budgetCode 	,  ");
		queryString.append("	T1.budgetName as budgetName	,  ");
		queryString.append("	T1.countBudget as countBudget	,  ");
		queryString.append("	T1.countAmt as countAmt	,  ");
		queryString.append("	((T1.countBudget)-(T1.countAmt)) as count_last	,  ");
		queryString.append("	T1.rate as count_rate	,  ");
		queryString.append("	T1.yaerRate as year_rate	 ");
		queryString.append("	FROM (	 ");
		queryString.append("	      SELECT   	 ");
		queryString.append("	                  TO_CHAR(M.DEPCODE) AS DEPCODE	,  ");
		queryString.append("	                  TO_CHAR(M.DEPNAME)AS DEPNAME	,  ");
		queryString.append("	  DECODE(MB.AAMT,NULL,0,MB.AAMT) AS MM1AMT,    	 ");
		queryString.append("	         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT, 	 ");
		queryString.append("	   DECODE(YB.AAMT,NULL,0,YB.AAMT) AS YY1AMT	 ");
		queryString.append("	                FROM (SELECT   	 ");
		queryString.append("	                      DISTINCT    	 ");
		queryString.append("	                      DEP.BUDGET_DEP_CODE  AS DEPCODE,	 ");
		queryString.append("	                      CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'   	 ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'   ");
		queryString.append("	                                    	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  	 ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠' 	 ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部' 	 ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部' 	 ");
		queryString.append("	                                   		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'	 ");
		queryString.append("	                                         WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'	 ");
		queryString.append("	                                   		 ELSE DEP.NAME  END AS DEPNAME   	 ");
		queryString.append("	                                  FROM TBEXP_DEPARTMENT DEP  	 ");
		queryString.append("	                                    INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID	 ");
		queryString.append("	                                    INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID	  ");
		queryString.append("	       WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))	 ");
		queryString.append("			   AND DG.CODE ='0'	AND DEP.CODE!='1#0A00'	 ) M   ");
		queryString.append("	LEFT JOIN (	 ");
		queryString.append("	SELECT   	 ");
		queryString.append("	MM.BUDCODE AS BUDCODE	,  ");
		queryString.append("	SUM(MM.BAMT) AS BAMT     	");
		queryString.append("	FROM (	 ");
		queryString.append("	SELECT 	 ");
		queryString.append("	MAIN.SUBPOENA_DATE	,  ");
		queryString.append("	DEP.BUDGET_DEP_CODE  AS BUDCODE  	,  ");
		queryString.append("	DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT)  AS BAMT  	 ");
		queryString.append("	FROM	 ");
		queryString.append("	(	 ");
		queryString.append("	SELECT 	 ");
		queryString.append("	E.TBEXP_ENTRY_TYPE_ID	,  ");
		queryString.append("	E.COST_UNIT_CODE	,  ");
		queryString.append("	E.TBEXP_ACC_TITLE_ID	,  ");
		queryString.append("	E.AMT	,  ");
		queryString.append("	MID.CODE AS MCODE	,  ");
		queryString.append("	BIG.CODE AS BCODE	,  ");
		queryString.append("	E.COST_CODE	,  ");
		queryString.append("	MAIN.SUBPOENA_DATE	");
		queryString.append("	FROM TBEXP_BIG_TYPE BIG	");
		queryString.append("	  INNER JOIN TBEXP_MIDDLE_TYPE MID  ON BIG.ID = MID.TBEXP_BIG_TYPE_ID	");
		queryString.append("	  INNER JOIN TBEXP_EXP_MAIN MAIN   ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)	");
		queryString.append("	  INNER JOIN TBEXP_ENTRY E   ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID	");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 start
		queryString.append("	WHERE  TO_CHAR(SUBPOENA_DATE,'YYYYMM') BETWEEN  ");
		queryString.append("   TO_CHAR(SUBSTR('").append(er_yymm).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR('").append(er_yymm).append("') ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 end
		queryString.append("	  AND BIG.CODE!='16'	");
		queryString.append("	) MAIN 	");
		queryString.append("	INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID	");
		queryString.append("	INNER JOIN TBEXP_DEPARTMENT DEP ON MAIN.COST_UNIT_CODE = DEP.CODE	");
		queryString.append("	INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID	");
		queryString.append("	INNER JOIN TBEXP_ACC_TITLE ACCT ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID	");
		queryString.append("	INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID  =B.ID	");
		queryString.append("	INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID	");
		queryString.append("	 WHERE ");
		queryString.append("	 ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR BCODE!='15' ) AND ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 start
		queryString.append("	 (MCODE!='A60' OR MAIN.COST_CODE='W'  ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MCODE NOT IN ('T05','T12','Q10')) ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 end
		queryString.append("	) ) ");
		queryString.append("	AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000') ");
		queryString.append("	 AND  DG.CODE ='0' ");
		queryString.append("	UNION ALL 	");
		queryString.append("	SELECT 	");
		queryString.append("	ESE.SUBPOENA_DATE	,  ");
		queryString.append("	DEP.BUDGET_DEP_CODE  AS BUDCODE 	,  ");
		queryString.append("	DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT	");
		queryString.append("	FROM 	");
		queryString.append("	(	");
		queryString.append("	SELECT 	");
		queryString.append("	TBEXP_ENTRY_TYPE_ID	,  ");
		queryString.append("	ACCT_CODE	,  ");
		queryString.append("	COST_UNIT_CODE	,  ");
		queryString.append("	AMT	,  ");
		queryString.append("	SUBPOENA_DATE	 ");
		queryString.append("	FROM TBEXP_EXT_SYS_ENTRY ESE	 ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 start
		queryString.append("	WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMM') BETWEEN  ");
		queryString.append("   TO_CHAR(SUBSTR('").append(er_yymm).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR('").append(er_yymm).append("'))  ESE ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 end
		queryString.append("	INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID   	 ");
		queryString.append("	INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE   	 ");
		queryString.append("	INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID  	 ");
		queryString.append("	INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID	 ");
		queryString.append("	INNER JOIN  TBEXP_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE	 ");
		queryString.append("	INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID	 ");
		queryString.append("	WHERE B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')	 ");
		queryString.append("	AND  DG.CODE = '0'	 ");
		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("   AND ESE.SUBPOENA_NO NOT IN('J827110002','J827115006','J827115009','J827115010') ");		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		
		queryString.append("	) MM   	 ");
		queryString.append("	GROUP BY  MM.BUDCODE 	 ");
		queryString.append("	)MM ON  M.DEPCODE = MM.BUDCODE 	 ");
		queryString.append("	LEFT JOIN(	 ");
		queryString.append("	SELECT   	 ");
		queryString.append("	MON.DEP_CODE AS BUDCODE	,  ");
		queryString.append("	SUM(MON.BUDGET_ITEM_AMT )AS AAMT  	 ");
		queryString.append("	FROM TBEXP_MONTH_BUDGET MON	 ");
		queryString.append("	INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID	 ");
		queryString.append("	INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID	 ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 start
		queryString.append("	WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2) BETWEEN	 ");
		queryString.append("   TO_CHAR(SUBSTR('").append(er_yymm).append("',0,4) || '01')");
		queryString.append("   AND TO_CHAR('").append(er_yymm).append("') ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 end
		queryString.append("	AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000',	 ");
		queryString.append("	'60110000','60120000','60200000','60300001','60800000','60900000')	 ");
		queryString.append("	GROUP BY DEP_CODE	 ");
		queryString.append("	)MB ON MB.BUDCODE=M.DEPCODE 	 ");
		queryString.append("	LEFT JOIN(	 ");
		queryString.append("	SELECT  	 ");
		queryString.append("	MON.DEP_CODE AS BUDCODE	,  ");
		queryString.append("	SUM(MON.BUDGET_ITEM_AMT) AS AAMT    	");
		queryString.append("	FROM TBEXP_MONTH_BUDGET MON	");
		queryString.append("	INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID	");
		queryString.append("	INNER JOIN TBEXP_AB_TYPE ABT ON B.TBEXP_AB_TYPE_ID = ABT.ID	");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 start
		queryString.append("	WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2) 	");
		queryString.append("   BETWEEN TO_CHAR(TO_NUMBER('").append(er_yymm).append("'+1)) AND TO_CHAR(SUBSTR('").append(er_yymm).append("',1,4) || '12') ");
		// defect#4720_ 預算實支修改SQL條件 ec0416 20171011 end
		queryString.append("	AND B.CODE NOT IN ('61110000','61210000','61310000','61420000','69000000' ,'60070000','60080000','60090000','60110000','60120000','60200000','60300001','60800000','60900000')	");
		queryString.append("	GROUP BY DEP_CODE	");
		queryString.append("	)YB ON YB.BUDCODE=M.DEPCODE	");
		queryString.append("	)T	");
		queryString.append("	INNER JOIN (	");
		queryString.append("	select	");
		queryString.append("	TO_CHAR(B.NAME) AS budgetName	,  ");
		queryString.append("	b.CODE as budgetCode	,  ");
		queryString.append("	D.budget_amt as countBudget	,  ");
		queryString.append("	D.exp_amt as countAmt	,  ");
		queryString.append("	TO_CHAR(D.overrun_explain) as explain	,  ");
		queryString.append("	D.YEAR_BUDGET_AMT AS YEAR_BUDGET_AMT	,  ");
		queryString.append("	TO_CHAR(D.IMPROVEMENT) AS improve	,  ");
		queryString.append("	TO_char(D.AUTUAL_RATE) as rate	,  ");
		queryString.append("	to_char(D.YEAR_AUTUAL_RATE) as yaerRate	,  ");
		// defect#4720_ 預算實支修改SQL條件_第二階段 ec0416 20171013 start
		queryString.append("	CASE WHEN DEP.ID IS NULL THEN  A.TBEXP_DEPARTMENT_ID ELSE DEP.BUDGET_DEP_CODE  END  AS DCODE , ");
		// defect#4720_ 預算實支修改SQL條件_第二階段 ec0416 20171013 start
		queryString.append("	A.er_yymm as errorYYMM	,  ");
		queryString.append("	A.res_appl_no	");
		queryString.append("	from tbexp_bud_exp_overrun_appl A	");
		queryString.append("	INNER JOIN tbexp_bud_exp_over_detail D ON D.tbexp_bud_exp_overrun_appl_ID=A.ID	");
		queryString.append("	INNER JOIN TBEXP_BUDGET_ITEM B ON B.ID=D.TBEXP_BUDGET_ITEM_ID	");
		// defect#4720_ 預算實支修改SQL條件_第二階段 ec0416 20171013 start
		queryString.append("	 LEFT JOIN TBEXP_DEPARTMENT DEP  ON DEP.ID      =A.TBEXP_DEPARTMENT_ID 	");
		// defect#4720_ 預算實支修改SQL條件_第二階段 ec0416 20171013 end
		queryString.append("	WHERE A.ER_YYMM = '").append(er_yymm).append("'  ");
		params.put("er_yymm", er_yymm);

		queryString.append("	order by b.code )T1 ON T.DEPCODE=T1.DCODE	");
		queryString.append("	ORDER BY T.DEPCODE	");
		List<Object> parameters = new ArrayList<Object>();
		return findNativeSQLByParameters(queryString.toString(), parameters);

	}

	// RE201702775_預算實支追蹤表單_費用系統 ec0416 2017/9/29 end
	
	// RE201702775_預算實支追蹤表單_費用系統_第二階段 ec0416 2017/9/29 start
	// 利用異常年月 找尋所有的 預算實支異常說明申請的資料
	public List<BudExpOverrunAppl> findResApplNo(String resApplNo) {
		StringBuffer queryString = new StringBuffer();

		queryString.append("   select appl from BudExpOverrunAppl appl     ");
		queryString.append("   where appl.resApplNo =:resApplNo ");

		Map<String, Object> params = new HashMap<String, Object>();

		params.put("resApplNo", resApplNo);

		return findByNamedParams(queryString.toString(), params);

	}
	// RE201702775_預算實支追蹤表單_費用系統_第二階段 ec0416 2017/9/29 end
}
