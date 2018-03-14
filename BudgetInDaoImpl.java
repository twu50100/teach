package tw.com.skl.exp.kernel.model6.dao.jpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.orm.jpa.JpaCallback;

import tw.com.skl.common.model6.dao.jpa.BaseDaoImpl;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.BudgetIn;
import tw.com.skl.exp.kernel.model6.bo.BudgetIn.ProjectTypeCode;
import tw.com.skl.exp.kernel.model6.bo.BudgetItem.BudgetItemCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeValueCode;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.dao.BudgetInDao;

/**
 * 預算轉入檔 Dao 類別。
 * 
 * @author Eustace
 * @version 1.0, 2009/10/21
 */
public class BudgetInDaoImpl extends BaseDaoImpl<BudgetIn, String> implements BudgetInDao {

	@SuppressWarnings("unchecked")
	private List findByNativeSQL(final String sqlString) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createNativeQuery(sqlString);
				return queryObject.getResultList();
			}
		});
	}

	public List<BudgetIn> findByParams(String year, String projectCode) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(projectCode)) {
			return null;
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct b");
		queryString.append(" from BudgetIn b");

		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append(" WHERE");
		// 預算年度:「預算轉入檔.預算年度」=傳入參數”預算年度”
		queryString.append(" b.year =:year");
		params.put("year", year);

		// 專案代碼:「預算轉入檔.專案代號」=傳入參數”專案代號”
		queryString.append(" AND b.projectCode =:projectCode");
		params.put("projectCode", projectCode);

		List<BudgetIn> entryList = findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(entryList)) {
			return null;
		} else {
			return entryList;
		}
	}

	// RE201500288_無法抓取多筆預算(defect_1348) CU3178 2015/03/02 START
	// public BudgetIn findBudgetInByParams(ProjectTypeCode projectTypeCode,
	// String year, String budgetItemCode, String arrangeUnitCode) {
	public List<BudgetIn> findBudgetInByParams(ProjectTypeCode projectTypeCode, String year, String budgetItemCode, String arrangeUnitCode) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return null;
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct b");
		queryString.append(" from BudgetIn b");

		Map<String, Object> params = new HashMap<String, Object>();
		// 預算年度:「預算轉入檔.專案類別代碼」=傳入參數”專案類別代碼”
		if (StringUtils.isNotBlank(projectTypeCode.getCode())) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.projectType =:projectType");
			params.put("projectType", projectTypeCode.getCode());
		}
		// 預算年度:「預算轉入檔.預算年度」=傳入參數”預算年度”
		if (StringUtils.isNotBlank(year)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.year =:year");
			params.put("year", year);
		}

		// 預算項目代號:「預算轉入檔.預算項目代號」=暫存變數”預算項目代號”
		if (StringUtils.isNotBlank(budgetItemCode)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 start
			// 當年度為2015且預算項目代號為"61430100_餐費"或"62100000_交際費"，合併餐費與交際費金額
			if (year.equals("2015") && ((budgetItemCode.equals(BudgetItemCode.MEAL_EXP.getCode())) || (budgetItemCode.equals(BudgetItemCode.FRIEND_EXP.getCode())))) {
				queryString.append(" (b.budgetItemCode =:budgetItemCodeMeal ");
				params.put("budgetItemCodeMeal", BudgetItemCode.MEAL_EXP.getCode());
				queryString.append(" or b.budgetItemCode =:budgetItemCodeFri) ");
				params.put("budgetItemCodeFri", BudgetItemCode.FRIEND_EXP.getCode());
			} else {
				queryString.append(" b.budgetItemCode =:budgetItemCode ");
				params.put("budgetItemCode", budgetItemCode);
			}
			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 end
		}

		// 編列單位代號:「預算轉入檔. 編列單位代號」=暫存變數”編列單位代號”
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.arrangeUnitCode =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		List<BudgetIn> entryList = findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(entryList)) {
			return null;
		} else {
			// return entryList.get(0);
			return entryList;
		}
	}

	// RE201500288_無法抓取多筆預算(defect_1348) CU3178 2015/03/02 END

	@SuppressWarnings("unchecked")
	public BigDecimal getProjectBudgetItemAppliedWithoutRemitMoneyAmt(String year, String budgetItemCode, String arrangeUnitCode, String expApplNo) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return BigDecimal.ZERO;
		}

		// 2010-08-13 修改重覆取金額問題
		// RE201003125 加入手工帳務調整總帳相關資料
		/*
		 * SELECT COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) -
		 * COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT FROM (
		 * SELECT DISTINCT TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO,
		 * TBEXP_ENTRY.ID AS ENTRY_ID, TBEXP_ENTRY_TYPE.ENTRY_VALUE AS
		 * ENTRY_VALUE, TBEXP_ENTRY.AMT AS AMT FROM TBEXP_ENTRY INNER JOIN
		 * TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID =
		 * TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) INNER JOIN TBEXP_EXPAPPL_C ON
		 * (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) INNER
		 * JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID =
		 * TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID INNER JOIN TBEXP_APPL_STATE ON
		 * TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID INNER JOIN
		 * TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID =
		 * TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID) INNER JOIN TBEXP_ACC_TITLE ON
		 * (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) INNER JOIN
		 * TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID =
		 * TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) INNER JOIN TBEXP_BUDGET_ITEM
		 * ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) INNER
		 * JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE
		 * INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID INNER JOIN
		 * TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID
		 * 
		 * WHERE TBEXP_APPL_STATE.CODE !=:applStateCode --「費用申請單.申請單狀態」不等於"刪件"
		 * AND TBEXP_MIDDLE_TYPE.ID !=:middleTypeCodeQ10 --排除費用中分類=Q10匯回款項 AND
		 * TBEXP_EXPAPPL_C.EXP_APPL_NO !=:expApplNo --費用申請單號(本次申請) AND
		 * TBEXP_EXPAPPL_C.EXP_YEARS LIKE :year --參數'預算年度' AND
		 * TBEXP_BUDGET_ITEM.CODE =:budgetItemCode -- 參數'預算項目代號 AND
		 * BUDGET_DEP_CODE =:arrangeUnitCode --參數'編列單位代號' AND
		 * NVL(TBEXP_EXPAPPL_C.PROJECT_CODE,' ') =' ' --專案代號為空白 UNION ALL
		 * 
		 * SELECT DISTINCT TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO,
		 * TBEXP_ENTRY.ID AS ENTRY_ID, TBEXP_ENTRY_TYPE.ENTRY_VALUE AS
		 * ENTRY_VALUE, TBEXP_ENTRY.AMT AS AMT FROM TBEXP_ENTRY INNER JOIN
		 * TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID =
		 * TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) INNER JOIN TBEXP_EXPAPPL_D ON
		 * (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) INNER
		 * JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID)
		 * INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID =
		 * TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID INNER JOIN TBEXP_MIDDLE_TYPE
		 * ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID
		 * INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID =
		 * TBEXP_APPL_STATE.ID INNER JOIN TBEXP_ENTRY_TYPE ON
		 * (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID) INNER JOIN
		 * TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID =
		 * TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) INNER JOIN TBEXP_ACC_CLASS_TYPE ON
		 * (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)
		 * INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID =
		 * TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) INNER JOIN TBEXP_DEPARTMENT
		 * COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE INNER JOIN
		 * TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID INNER JOIN
		 * TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID WHERE
		 * TBEXP_APPL_STATE.CODE !=:applStateCode --「費用申請單.申請單狀態」不等於"刪件" AND
		 * TBEXP_MIDDLE_TYPE.CODE ='T09' AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE,
		 * 'YYYYMMDD') LIKE :year --參數'預算年度' AND TBEXP_BUDGET_ITEM.CODE
		 * =:budgetItemCode -- 參數'預算項目代號 AND BUDGET_DEP_CODE =:arrangeUnitCode
		 * --參數'編列單位代號' AND NVL(TBEXP_MALACC_APPL.PROJECT_CODE,' ') =' '
		 * --專案代號為空白 AND (TBEXP_EXPAPPL_D.COST_TYPE IS NULL OR
		 * TBEXP_EXPAPPL_D.COST_TYPE !='W') --成本別不為W ) T ;
		 */
		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("SELECT COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT"); // 借方金額減貸方金額
		queryString.append(" FROM  ");
		// 調整預算實支SQL CU3178 2015/12/04 START
		queryString.append(" (" + " SELECT" + " DISTINCT" + " TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO," + " TBEXP_ENTRY.ID AS ENTRY_ID," + " TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE," + " TBEXP_ENTRY.AMT AS AMT" + " FROM" + " TBEXP_ENTRY"
		// +
		// " INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)"
		// +
		// " INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)"
				+ " INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)" + " INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID" + " INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID" + " INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)" + " INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)"
				// +
				// " INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)"
				+ " INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)" + " INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE");
		// +
		// " INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID"
		// +
		// " INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID");
		// 調整預算實支SQL CU3178 2015/12/04 END

		queryString.append(" WHERE TBEXP_APPL_STATE.CODE !=:applStateCode "); // 「費用申請單.申請單狀態」不等於"刪件"
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		// AND TBEXP_MIDDLE_TYPE.ID != 'Q10' --排除費用中分類=Q10匯回款項
		queryString.append(" AND TBEXP_MIDDLE_TYPE.ID !=:middleTypeCodeQ10");
		params.put("middleTypeCodeQ10", MiddleTypeCode.CODE_Q10.getCode());

		// 申請單號
		if (StringUtils.isNotBlank(expApplNo)) {
			// AND TBEXP_EXPAPPL_C.EXP_APPL_NO != 'B00200912160019'
			// --費用申請單號(本次申請)
			queryString.append(" AND TBEXP_EXPAPPL_C.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2009' --參數'預算年度'
			// 調整預算檢核效能 CU3178 2015/1/21 START
			// queryString.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE :year");
			// params.put("year", year+"%");
			queryString.append(" AND SUBSTR(TBEXP_EXPAPPL_C.EXP_YEARS,0,4) = :year");
			params.put("year", year);
			// 調整預算檢核效能 CU3178 2015/1/21 END
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號

			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 start
			// 當年度為2015且預算項目代號為"61430100_餐費"或"62100000_交際費"，合併餐費與交際費金額
			if (year.equals("2015") && ((budgetItemCode.equals(BudgetItemCode.MEAL_EXP.getCode())) || (budgetItemCode.equals(BudgetItemCode.FRIEND_EXP.getCode())))) {
				queryString.append(" AND (TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeMeal ");
				params.put("budgetItemCodeMeal", BudgetItemCode.MEAL_EXP.getCode());
				queryString.append(" OR TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeFri) ");
				params.put("budgetItemCodeFri", BudgetItemCode.FRIEND_EXP.getCode());
			} else {
				queryString.append(" AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode");
				params.put("budgetItemCode", budgetItemCode);
			}
			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 end
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}

		queryString.append(" AND NVL(TBEXP_EXPAPPL_C.PROJECT_CODE,' ') =' '");

		queryString.append("UNION ALL ");

		queryString.append("SELECT ");
		queryString.append("DISTINCT ");
		queryString.append("    TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    TBEXP_ENTRY.ID AS ENTRY_ID, ");
		queryString.append("    TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    TBEXP_ENTRY.AMT AS AMT ");
		queryString.append("FROM ");
		queryString.append("    TBEXP_ENTRY ");
		// 調整預算實支SQL CU3178 2015/12/04 START
		// queryString
		// .append("    INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D ON (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		queryString.append("    INNER JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID) ");
		queryString.append("    INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID = TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID ");
		queryString.append("    INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID) ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		// queryString
		// .append("    INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE ");
		// queryString
		// .append("    INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID ");
		// queryString
		// .append("    INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID ");
		queryString.append("WHERE TBEXP_APPL_STATE.CODE !=:applStateCode ");
		queryString.append("    AND TBEXP_MIDDLE_TYPE.CODE ='T09' ");
		queryString.append("    AND NVL(TBEXP_MALACC_APPL.PROJECT_CODE,' ') =' ' ");
		queryString.append("    AND (TBEXP_EXPAPPL_D.COST_TYPE IS NULL OR  TBEXP_EXPAPPL_D.COST_TYPE !='W') ");
		// 調整預算實支SQL CU3178 2015/12/04 END
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_D.CLOSE_DATE LIKE :year --參數'預算年度'
			// 調整預算檢核效能 CU3178 2015/1/21 START
			// queryString.append("    AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYYMMDD') LIKE :year ");
			queryString.append("    AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYY') = :year ");
			// 調整預算檢核效能 CU3178 2015/1/21 END

		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 start
			// 當年度為2015且預算項目代號為"61430100_餐費"或"62100000_交際費"，合併餐費與交際費金額
			if (year.equals("2015") && ((budgetItemCode.equals(BudgetItemCode.MEAL_EXP.getCode())) || (budgetItemCode.equals(BudgetItemCode.FRIEND_EXP.getCode())))) {
				queryString.append(" AND (TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeMeal ");
				params.put("budgetItemCodeMeal", BudgetItemCode.MEAL_EXP.getCode());
				queryString.append(" OR TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeFri) ");
				params.put("budgetItemCodeFri", BudgetItemCode.FRIEND_EXP.getCode());
			} else {
				queryString.append(" AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode");
				params.put("budgetItemCode", budgetItemCode);
			}
			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 end
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append("    AND BUDGET_DEP_CODE =:arrangeUnitCode ");
		}

		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			BigDecimal amt = (BigDecimal) list.get(0);
			if (amt != null) {
				return amt;
			}
		}
		return BigDecimal.ZERO;
	}

	// RE201701547_費用系統預算優化第二階段 EC0416 2017/4/7 start
	/**
	 * 計算一般非專案實支金額 陣列0:在途金額 陣列1:已日結金額
	 */
	@SuppressWarnings("unchecked")
	public BigDecimal[] getExpAmtWithNotProjectCode(String year, String budgetItemCode, String arrangeUnitCode, String expApplNo) {
		BigDecimal[] amtList = { BigDecimal.ZERO, BigDecimal.ZERO };

		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return amtList;
		}

		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();

		queryString.append(" SELECT  ");
		// 在途金額 加總
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.BAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.BAMT, 0)),0) AS BAMT,");
		// 已日結金額 加總
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.CAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.CAMT, 0)),0) AS CAMT ");
		queryString.append(" FROM  ");
		queryString.append(" (");
		// 行政費用 在途件
		queryString.append("  SELECT ");
		queryString.append("  DISTINCT ");
		queryString.append("  C.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("  E.ID AS ENTRY_ID, ");
		queryString.append("  ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("  E.AMT  AS BAMT, "); // 在途金額
		queryString.append("  0   AS CAMT    "); // 已日結金額
		queryString.append("  FROM ");
		queryString.append("  TBEXP_ENTRY E ");
		queryString.append("  INNER JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID = C.TBEXP_MIDDLE_TYPE_ID");
		queryString.append("  INNER JOIN TBEXP_APPL_STATE STATE ON C.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG ON BIG.ID=MID.TBEXP_BIG_TYPE_ID");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND C.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND C.EXP_YEARS LIKE '").append(year).append("%'");
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 不包含專案代號
		queryString.append("  AND C.PROJECT_CODE IS NULL ");
		// 排除W
		queryString.append("  AND (E.COST_CODE !='W' OR E.COST_CODE IS NULL) ");
		// 排除A60有購站
		queryString.append("  AND MID.CODE!='A60' ");

		// 總帳在途件
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT ");
		queryString.append("   DISTINCT ");
		queryString.append("    D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    E.ID AS ENTRY_ID, ");
		queryString.append("    ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    E.AMT AS BAMT, ");
		queryString.append("    0  AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("    TBEXP_ENTRY E ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("    INNER JOIN TBEXP_D_CHECK_DETAIL DCHECK ON DCHECK.ID = D.TBEXP_D_CHECK_DETAIL_ID ");
		queryString.append("  	INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID = DCHECK.TBEXP_MIDDLE_TYPE_ID");
		queryString.append("  	INNER JOIN TBEXP_BIG_TYPE BIG ON BIG.ID=MID.TBEXP_BIG_TYPE_ID");
		//defect4420 C1.5.1儲存檢核邏輯錯誤 EC0416 2017/6/30 start
		//queryString.append("    INNER JOIN TBEXP_D_CHECK_DETAIL DCHECK ON DCHECK.ID = D.TBEXP_D_CHECK_DETAIL_ID ");
		//defect4420 C1.5.1儲存檢核邏輯錯誤 EC0416 2017/6/30 end
		queryString.append("    LEFT JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND MAL.PROJECT_CODE IS NULL ");
		// 不為W件
		queryString.append("    AND (D.COST_TYPE IS NULL OR  D.COST_TYPE !='W') ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND TO_CHAR(D.CLOSE_DATE, 'YYYY') = :year ");
		}
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND D.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append("    AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode ");
		}
		// 排除15月決算、16資產區隔、18 年度部門提列
		queryString.append("  AND BIG.CODE NOT IN ('15','16','18') ");

		// 已日結費用
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT DISTINCT  ");
		queryString.append("   MAIN.EXP_APPL_NO, ");
		queryString.append("   E.ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0 AS BAMT, ");
		queryString.append("   E.AMT  AS CAMT");
		queryString.append("  FROM TBEXP_ENTRY E");
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  LEFT JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID=ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE= E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID ON SUBSTR(MAIN.EXP_APPL_NO,1,3)=MID.CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG ON BIG.ID=MID.TBEXP_BIG_TYPE_ID ");
		// 排除w
		queryString.append("  WHERE (E.COST_CODE !='W' OR E.COST_CODE IS NULL) ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYY') = :year");
			params.put("year", year);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 專案代號為空值
		queryString.append("  AND SUB.PROJECT_NO IS NULL ");
		// 排除15月決算、16資產區隔、18 年度部門提列
		queryString.append("  AND BIG.CODE NOT IN ('15','16','18') ");
		// 排除00辦公費但包含N10
		queryString.append("  AND (BIG.CODE!='00' OR MID.CODE='N10') ");
		// 排除A60有購站、T07部門提列費用
		queryString.append("  AND MID.CODE !='A60' ");

		// 在途-提列應付申請單T07
		queryString.append(" UNION ALL ");
		queryString.append("   SELECT DISTINCT  ");
		queryString.append("   DAA.DEP_ACCEXP_NO  AS EXP_APPL_NO, ");
		queryString.append("   DAD.ID  AS ENTRY_ID, ");
		queryString.append("   N'D', ");
		queryString.append("   DAD.ESTIMATION_AMT       AS BAMT, ");
		queryString.append("   0 AS CAMT ");
		queryString.append("   FROM TBEXP_DEP_ACCEXP_DETAIL DAD ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID=DAD.TBEXP_BUDGET_ITEM_ID ");
		queryString.append("   INNER JOIN TBEXP_DEP_ACCEXP_APPL DAA ON DAA.ID=DAD.TBEXP_DEP_ACCEXP_APPL_ID ");
		queryString.append("   LEFT JOIN TBEXP_EXPAPPL_D D ON D.ID=DAA.TBEXP_EXPAPPL_D_ID ");
		queryString.append("   LEFT JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE (STATE.CODE  <:applStateCode OR STATE.ID IS NULL) ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND DAD.PROJECT_NO  IS NULL ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND DAA.EXP_YEAR  = :year ");
		}
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND DAA.DEP_ACCEXP_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND DAD.COST_UNIT_CODE  =:arrangeUnitCode ");
		}

		// 已日結-提列應付申請單T07
		queryString.append(" UNION ALL ");
		queryString.append("   SELECT DISTINCT  ");
		queryString.append("   DAA.DEP_ACCEXP_NO  AS EXP_APPL_NO, ");
		queryString.append("   DAD.ID  AS ENTRY_ID, ");
		queryString.append("   N'D', ");
		queryString.append("   0       AS BAMT, ");
		queryString.append("   DAD.ESTIMATION_AMT AS CAMT ");
		queryString.append("   FROM TBEXP_DEP_ACCEXP_DETAIL DAD ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID=DAD.TBEXP_BUDGET_ITEM_ID ");
		queryString.append("   INNER JOIN TBEXP_DEP_ACCEXP_APPL DAA ON DAA.ID=DAD.TBEXP_DEP_ACCEXP_APPL_ID ");
		queryString.append("   INNER JOIN TBEXP_EXPAPPL_D D ON D.ID=DAA.TBEXP_EXPAPPL_D_ID ");
		queryString.append("   INNER JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE =:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND DAD.PROJECT_NO  IS NULL ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND DAA.EXP_YEAR  = :year ");
		}
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND DAA.DEP_ACCEXP_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND DAD.COST_UNIT_CODE  =:arrangeUnitCode ");
		}

		// 外部帳務資料
		queryString.append(" UNION ALL   ");
		queryString.append("  SELECT DISTINCT ");
		queryString.append("   EXT.SUBPOENA_NO AS EXP_APPL_NO, ");
		queryString.append("   EXT.ID AS ENTRY_ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0   AS BAMT,  ");
		queryString.append("   EXT.AMT AS CAMT ");
		queryString.append("  FROM TBEXP_EXT_SYS_ENTRY EXT ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON EXT.TBEXP_ENTRY_TYPE_ID =ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=EXT.ACCT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("  INNER JOIN  TBEXP_DEPARTMENT DEP  ON EXT.COST_UNIT_CODE = DEP.CODE     ");		
		// 專案代號為空值
		queryString.append("  WHERE EXT.PROJECT_NO IS NULL ");	
		queryString.append("  AND EXT.SUBPOENA_NO NOT IN('J827110002','J827115006','J827115009','J827115010') ");		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(EXT.SUBPOENA_DATE,'YYYY')= :year");
			params.put("year", year);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
			queryString.append(" AND DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			Object[] record = (Object[]) list.get(0);
			amtList[0] = (BigDecimal) record[0];// 在途金額
			amtList[1] = (BigDecimal) record[1];// 已日結金額
		}
		return amtList;
	}

	/**
	 * 計算專案代號實支金額 陣列0:在途金額 陣列1:已日結金額
	 */
	@SuppressWarnings("unchecked")
	public BigDecimal[] getExpAmtProjectCode(String projectCode, String expApplNo) {
		BigDecimal[] amtList = { BigDecimal.ZERO, BigDecimal.ZERO };
		if (StringUtils.isBlank(projectCode)) {
			return amtList;
		}
		StringBuffer queryString = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		// 行政費用-尚未日結(在途)
		queryString.append(" SELECT ");
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.BAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.BAMT, 0)),0) AS BAMT,");
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.CAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.CAMT, 0)),0) AS CAMT ");
		queryString.append(" FROM  ");
		queryString.append(" (");
		queryString.append("  SELECT ");
		queryString.append("  DISTINCT ");
		queryString.append("  C.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("  E.ID AS ENTRY_ID, ");
		queryString.append("  ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("  E.AMT AS BAMT, ");
		queryString.append("  0     AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("  TBEXP_ENTRY E ");
		queryString.append("  INNER JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER JOIN TBEXP_APPL_STATE STATE ON C.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND C.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算項目代號
		queryString.append(" AND BUD.CODE LIKE '6%' ");
		// 專案代號
		if (StringUtils.isNotBlank(projectCode)) {
			queryString.append(" AND C.PROJECT_CODE =:projectCode ");
			params.put("projectCode", projectCode);
		}

		// T09手工帳務調整(在途)
		queryString.append(" UNION ALL ");
		queryString.append(" SELECT ");
		queryString.append(" DISTINCT ");
		queryString.append("    D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    E.ID AS ENTRY_ID, ");
		queryString.append("    ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    E.AMT AS BAMT, ");
		queryString.append("    0     AS CAMT ");
		queryString.append(" FROM ");
		queryString.append("    TBEXP_ENTRY E ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("    INNER JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 預算項目代號
		queryString.append(" AND BUD.CODE LIKE '6%' ");
		// 專案代號
		if (StringUtils.isNotBlank(projectCode)) {
			queryString.append(" AND MAL.PROJECT_CODE =:projectCode ");
			params.put("projectCode", projectCode);
		}

		// 已日結 行政費用、T09(總帳只有手工帳務會寫入子檔)
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT DISTINCT  ");
		queryString.append("   MAIN.EXP_APPL_NO, ");
		queryString.append("   E.ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0      AS BAMT, ");
		queryString.append("   E.AMT  AS CAMT ");
		queryString.append("  FROM TBEXP_ENTRY E");
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID=ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE= E.COST_UNIT_CODE ");
		// 預算項目代號
		queryString.append(" WHERE BUD.CODE LIKE '6%'");
		// 專案代號
		if (StringUtils.isNotBlank(projectCode)) {
			queryString.append(" AND SUB.PROJECT_NO =:projectCode ");
			params.put("projectCode", projectCode);
		}

		// 外部帳務資料
		queryString.append(" UNION ALL   ");
		queryString.append("  SELECT DISTINCT ");
		queryString.append("   EXT.SUBPOENA_NO AS EXP_APPL_NO, ");
		queryString.append("   EXT.ID AS ENTRY_ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0       AS BAMT, ");
		queryString.append("   EXT.AMT AS CAMT ");
		queryString.append("  FROM TBEXP_EXT_SYS_ENTRY EXT ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON EXT.TBEXP_ENTRY_TYPE_ID =ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=EXT.ACCT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		// 預算項目代號
		queryString.append("  WHERE BUD.CODE LIKE '6%' ");
		// 專案代號
		if (StringUtils.isNotBlank(projectCode)) {
			queryString.append(" AND EXT.PROJECT_NO =:projectCode ");
			params.put("projectCode", projectCode);
		}
		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			Object[] record = (Object[]) list.get(0);
			amtList[0] = (BigDecimal) record[0];// 在途金額
			amtList[1] = (BigDecimal) record[1];// 已日結金額
		}
		return amtList;
	}

	/**
	 * 計算預算為交際費之W件金額 陣列0:在途金額 陣列1:已日結金額
	 */
	@SuppressWarnings("unchecked")
	public BigDecimal[] getSocialAndWNonProject(String year, String budgetItemCode, String arrangeUnitCode, String expApplNo) {
		BigDecimal[] amtList = { BigDecimal.ZERO, BigDecimal.ZERO };
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return amtList;
		}

		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		// 行政費用 W 在途件
		queryString.append(" SELECT ");
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.BAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.BAMT, 0)),0) AS BAMT,");
		queryString.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.CAMT, 0)),0)   - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.CAMT, 0)),0) AS CAMT");
		queryString.append(" FROM  ");
		queryString.append(" (");
		queryString.append("  SELECT ");
		queryString.append("  DISTINCT ");
		queryString.append("  C.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("  E.ID AS ENTRY_ID, ");
		queryString.append("  ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("  E.AMT AS BAMT, ");
		queryString.append("  0     AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("  TBEXP_ENTRY E ");
		queryString.append("  INNER JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID = C.TBEXP_MIDDLE_TYPE_ID");
		queryString.append("  INNER JOIN TBEXP_APPL_STATE STATE ON C.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG  ON BIG.ID=MID.TBEXP_BIG_TYPE_ID");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND C.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND C.EXP_YEARS LIKE '").append(year).append("%'");
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 不包含專案代號
		queryString.append("  AND C.PROJECT_CODE IS NULL");
		// W件
		queryString.append("  AND E.COST_CODE ='W'  ");
		// 排除A60有購站
		queryString.append("  AND MID.CODE!='A60'  ");

		// T09 W 在途件
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT ");
		queryString.append("   DISTINCT ");
		queryString.append("    D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    E.ID AS ENTRY_ID, ");
		queryString.append("    ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    E.AMT AS BAMT, ");
		queryString.append("    0     AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("    TBEXP_ENTRY E ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("    INNER JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND MAL.PROJECT_CODE IS NULL ");
		// W件
		queryString.append("    AND D.COST_TYPE ='W' ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND TO_CHAR(D.CLOSE_DATE, 'YYYY') = :year ");
		}
		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND D.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append("    AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode ");
		}

		// 今年已日結W件行政費用、T09(總帳只有手工帳務會寫入子檔)
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT DISTINCT  ");
		queryString.append("   MAIN.EXP_APPL_NO, ");
		queryString.append("   E.ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0     AS BAMT, ");
		queryString.append("   E.AMT AS CAMT ");
		queryString.append("  FROM TBEXP_ENTRY E"); 
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER  JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID=ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE= E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID  ON SUBSTR(MAIN.EXP_APPL_NO,1,3)=MID.CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG  ON BIG.ID =MID.TBEXP_BIG_TYPE_ID");
		// W件
		queryString.append("  WHERE E.COST_CODE ='W' ");
		// 預算年度(今年)
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYY') =TO_CHAR(TO_NUMBER(:year)+1) ");
			params.put("year", year);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 專案代號為空值
		queryString.append(" AND SUB.PROJECT_NO IS NULL ");
		// 排除00辦公費但包含N10
		queryString.append(" AND (BIG.CODE!='00' OR MID.CODE ='N10') ");
		// 排除A60有購站
		queryString.append(" AND MID.CODE!='A60'");

		// 前一年度已日結 非W 行政費用、總帳
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT DISTINCT  ");
		queryString.append("   MAIN.EXP_APPL_NO, ");
		queryString.append("   E.ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0      AS BAMT, ");
		queryString.append("   E.AMT  AS CAMT ");
		queryString.append("  FROM TBEXP_ENTRY E");
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  LEFT JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID=ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE= E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID  ON SUBSTR(MAIN.EXP_APPL_NO,1,3)=MID.CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG  ON BIG.ID =MID.TBEXP_BIG_TYPE_ID ");
		// 非W
		queryString.append("  WHERE (E.COST_CODE !='W' OR E.COST_CODE IS NULL) ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYY') = :year");
			params.put("year", year);
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 專案代號為空值
		queryString.append(" AND SUB.PROJECT_NO IS NULL ");
		// 排除15月決算、16資產區隔、18部門提列費用
		queryString.append(" AND BIG.CODE NOT IN ('15','16','18') ");
		// 排除00辦公費但包含N10    
		queryString.append(" AND (BIG.CODE!='00' OR MID.CODE='N10') ");

		// 外部帳務資料
		queryString.append(" UNION ALL   ");
		queryString.append("  SELECT DISTINCT ");
		queryString.append("   EXT.SUBPOENA_NO AS EXP_APPL_NO, ");
		queryString.append("   EXT.ID AS ENTRY_ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0       AS BAMT, ");
		queryString.append("   EXT.AMT AS CAMT ");
		queryString.append("  FROM TBEXP_EXT_SYS_ENTRY EXT ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON EXT.TBEXP_ENTRY_TYPE_ID =ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=EXT.ACCT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("  INNER JOIN  TBEXP_DEPARTMENT DEP  ON EXT.COST_UNIT_CODE = DEP.CODE ");
		// 專案代號為空值
		queryString.append("  WHERE EXT.PROJECT_NO IS NULL ");
		queryString.append("  AND EXT.SUBPOENA_NO NOT IN('J827110002','J827115006','J827115009','J827115010')");		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(EXT.SUBPOENA_DATE,'YYYY') = :year");
			params.put("year", year);
		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
			queryString.append(" AND DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			Object[] record = (Object[]) list.get(0);
			amtList[0] = (BigDecimal) record[0];// 在途金額
			amtList[1] = (BigDecimal) record[1];// 已日結金額
		}
		return amtList;
	}

	/**
	 * D4.3 提列費用計算交際費非W實支金額 陣列0:在途金額 陣列1:已日結金額
	 */
	@SuppressWarnings("unchecked")
	public BigDecimal[] getDepAccexpAndSocialNonW(String year, String budgetItemCode, String arrangeUnitCode, String expApplNo) {
		BigDecimal[] amtList = { BigDecimal.ZERO, BigDecimal.ZERO };
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return amtList;
		}

		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		// 行政費用 在途件
		queryString.append(" SELECT COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.BAMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.BAMT, 0)),0) AS EAMT,"); // 借方金額減貸方金額
		queryString.append("   COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.CAMT, 0)),0)      - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.CAMT, 0)),0) AS EEAMT  ");
		queryString.append(" FROM  ");
		queryString.append(" (");
		queryString.append("  SELECT ");
		queryString.append("  DISTINCT ");
		queryString.append("  C.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("  E.ID AS ENTRY_ID, ");
		queryString.append("  ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("  E.AMT AS BAMT, ");
		queryString.append("  0     AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("  TBEXP_ENTRY E ");
		queryString.append("  INNER JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID = C.TBEXP_MIDDLE_TYPE_ID");
		queryString.append("  INNER JOIN TBEXP_APPL_STATE STATE ON C.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG  ON BIG.ID  =MID.TBEXP_BIG_TYPE_ID ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND SUBSTR(C.EXP_YEARS,0,4) = :year");
			params.put("year", year);
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append(" AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		queryString.append("  AND NVL(C.PROJECT_CODE,' ') =' '");
		queryString.append("  AND (E.COST_CODE !='W' OR E.COST_CODE IS NULL) ");
		queryString.append("  AND (BIG.CODE!='00'OR MID.CODE='N10') ");
		queryString.append("  AND (BIG.CODE!='15'OR MID.CODE='T07') ");
		queryString.append("  AND BIG.CODE!='16' ");
		queryString.append("  AND MID.CODE!='A60' ");

		// T09 在途件
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT ");
		queryString.append("   DISTINCT ");
		queryString.append("    D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    E.ID AS ENTRY_ID, ");
		queryString.append("    ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    E.AMT AS BAMT, ");
		queryString.append("    0     AS CAMT ");
		queryString.append("  FROM ");
		queryString.append("    TBEXP_ENTRY E ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("    INNER JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID = E.TBEXP_ENTRY_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = E.COST_UNIT_CODE ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE STATE.CODE  <:applStateCode ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND NVL(MAL.PROJECT_CODE,' ') =' ' ");
		// 不為W件
		queryString.append("    AND (D.COST_TYPE IS NULL OR  D.COST_TYPE !='W') ");

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND TO_CHAR(D.CLOSE_DATE, 'YYYY') = :year ");
		}
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append("    AND COST_DEP.BUDGET_DEP_CODE =:arrangeUnitCode ");
		}

		// 已日結 行政費用、T09(總帳只有手工帳務會寫入子檔)
		queryString.append(" UNION ALL ");
		queryString.append("  SELECT DISTINCT  ");
		queryString.append("   MAIN.EXP_APPL_NO, ");
		queryString.append("   E.ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0     AS BAMT, ");
		queryString.append("   E.AMT AS CAMT  ");
		queryString.append("  FROM TBEXP_ENTRY E");
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("  LEFT JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID=ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID = E.TBEXP_ACC_TITLE_ID ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		queryString.append("  INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE= E.COST_UNIT_CODE ");
		queryString.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID  ON SUBSTR(MAIN.EXP_APPL_NO,1,3)=MID.CODE ");
		queryString.append("  INNER JOIN TBEXP_BIG_TYPE BIG  ON BIG.ID =MID.TBEXP_BIG_TYPE_ID");
		queryString.append("  WHERE (E.COST_CODE !='W' OR E.COST_CODE IS NULL) ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYY') = :year");
			params.put("year", year);
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 專案代號為空值
		queryString.append(" AND SUB.PROJECT_NO IS NULL ");
		queryString.append(" AND (BIG.CODE!='00' OR MID.CODE='N10') ");
		queryString.append(" AND  BIG.CODE NOT IN ('15','16') ");
		queryString.append(" AND MID.CODE!='A60' ");

		// 外部帳務資料
		queryString.append(" UNION ALL   ");
		queryString.append("  SELECT DISTINCT ");
		queryString.append("   EXT.SUBPOENA_NO AS EXP_APPL_NO, ");
		queryString.append("   EXT.ID AS ENTRY_ID, ");
		queryString.append("   ET.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("   0       AS BAMT, ");
		queryString.append("   EXT.AMT AS CAMT ");
		queryString.append("  FROM TBEXP_EXT_SYS_ENTRY EXT ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON EXT.TBEXP_ENTRY_TYPE_ID =ET.ID ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=EXT.ACCT_CODE ");
		queryString.append("  INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID = ACC.TBEXP_BUG_ITEM_ID ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("  INNER JOIN  TBEXP_DEPARTMENT DEP  ON EXT.COST_UNIT_CODE = DEP.CODE     ");
		// 專案代號為空值
		queryString.append("  WHERE EXT.PROJECT_NO IS NULL ");
		
		
		queryString.append("  AND EXT.SUBPOENA_NO NOT IN('J827110002','J827115006','J827115009','J827115010') ");		
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		
		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append(" AND TO_CHAR(EXT.SUBPOENA_DATE,'YYYY') = :year");
			params.put("year", year);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
			queryString.append(" AND DEP.BUDGET_DEP_CODE =:arrangeUnitCode");
			//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
			params.put("arrangeUnitCode", arrangeUnitCode);
		}

		// 在途 提列應付申請單T07
		queryString.append(" UNION ALL ");
		queryString.append("   SELECT DISTINCT  ");
		queryString.append("   DAA.DEP_ACCEXP_NO  AS EXP_APPL_NO, ");
		queryString.append("   DAD.ID  AS ENTRY_ID, ");
		queryString.append("   N'D', ");
		queryString.append("   DAD.ESTIMATION_AMT       AS BAMT, ");
		queryString.append("   0 AS CAMT ");
		queryString.append("   FROM TBEXP_DEP_ACCEXP_DETAIL DAD ");
		queryString.append("   INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID=DAD.TBEXP_BUDGET_ITEM_ID ");
		queryString.append("   INNER JOIN TBEXP_DEP_ACCEXP_APPL DAA ON DAA.ID=DAD.TBEXP_DEP_ACCEXP_APPL_ID ");
		queryString.append("   LEFT JOIN TBEXP_EXPAPPL_D D ON D.ID=DAA.TBEXP_EXPAPPL_D_ID ");
		queryString.append("   LEFT JOIN TBEXP_APPL_STATE STATE ON D.TBEXP_APPL_STATE_ID = STATE.ID ");
		// 「費用申請單.申請單狀態」小於日結
		queryString.append(" WHERE (STATE.CODE  <:applStateCode OR STATE.ID IS NULL) ");
		params.put("applStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		// 專案代號為空值
		queryString.append("    AND DAD.PROJECT_NO  IS NULL ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			queryString.append("    AND DAA.EXP_YEAR  = :year ");
		}

		// 排除本次申請的申請單
		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" AND DAA.DEP_ACCEXP_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			queryString.append(" AND BUD.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			queryString.append("    AND DAD.COST_UNIT_CODE  =:arrangeUnitCode ");
		}

		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) { 
			Object[] record = (Object[]) list.get(0);
			amtList[0] = (BigDecimal) record[0];// 在途金額
			amtList[1] = (BigDecimal) record[1];// 已日結金額
		}
		return amtList;
	}

	// RE201701547_費用系統預算優化第二階段 EC0416 2017/4/7 end

	@SuppressWarnings("unchecked")
	public BigDecimal getProjectBudgetItemAppliedByRemitMoneyAmt(String year, String budgetItemCode, String arrangeUnitCode) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return BigDecimal.ZERO;
		}

		/*
		 * SELECT COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS AMT FROM TBEXP_ENTRY
		 * INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID =
		 * TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) INNER JOIN TBEXP_EXPAPPL_C ON
		 * (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) INNER
		 * JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID =
		 * TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID INNER JOIN TBEXP_APPL_STATE ON
		 * TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID INNER JOIN
		 * TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID =
		 * TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID) INNER JOIN TBEXP_ACC_TITLE ON
		 * (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) INNER JOIN
		 * TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID =
		 * TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) INNER JOIN TBEXP_BUDGET_ITEM
		 * ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) INNER
		 * JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE
		 * INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID INNER JOIN
		 * TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID =
		 * TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID WHERE
		 * TBEXP_APPL_STATE.CODE != '99'--「費用申請單.申請單狀態」不等於"刪件" AND
		 * TBEXP_MIDDLE_TYPE.ID = 'Q10' --費用中分類=Q10匯回款項 AND
		 * TBEXP_ENTRY_TYPE.ENTRY_VALUE = 'C'--「分錄借貸別」=貸方 AND
		 * TBEXP_EXPAPPL_C.EXP_YEARS LIKE '' --參數'預算年度' AND
		 * TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號 AND BUDGET_DEP_CODE = ''
		 * --參數'編列單位代號'
		 */
		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("SELECT   COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS AMT ");
		queryString.append(" FROM  TBEXP_ENTRY");
		queryString.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)" + " INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)" + " INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID" + " INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID" + " INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)" + " INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)" + " INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)" + " INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)" + " INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE" + " INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID" + " INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID");

		// WHERE TBEXP_APPL_STATE.CODE != '99'--「費用申請單.申請單狀態」不等於"刪件"
		queryString.append(" WHERE TBEXP_APPL_STATE.CODE !=:applStateCode");
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		// AND TBEXP_MIDDLE_TYPE.ID = 'Q10' --費用中分類=Q10匯回款項
		queryString.append(" AND TBEXP_MIDDLE_TYPE.ID =:middleTypeCodeQ10");
		params.put("middleTypeCodeQ10", MiddleTypeCode.CODE_Q10.getCode());

		// AND TBEXP_ENTRY_TYPE.ENTRY_VALUE = 'C'--「分錄借貸別」=貸方
		queryString.append(" AND TBEXP_ENTRY_TYPE.ENTRY_VALUE =:entryTypeValueC");
		params.put("entryTypeValueC", EntryTypeValueCode.C.getValue());

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '' --參數'預算年度'
			queryString.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE :year");
			params.put("year", year + "%");
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號'

			// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 start
			// 當年度為2015且預算項目代號為"61430100_餐費"或"62100000_交際費"，合併餐費與交際費金額
			if (year.equals("2015") && ((budgetItemCode.equals(BudgetItemCode.MEAL_EXP.getCode())) || (budgetItemCode.equals(BudgetItemCode.FRIEND_EXP.getCode())))) {
				queryString.append(" AND (TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeMeal ");
				params.put("budgetItemCodeMeal", BudgetItemCode.MEAL_EXP.getCode());
				queryString.append(" OR TBEXP_BUDGET_ITEM.CODE =:budgetItemCodeFri) ");
				params.put("budgetItemCodeFri", BudgetItemCode.FRIEND_EXP.getCode());
			} else {
				queryString.append(" AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode");
				params.put("budgetItemCode", budgetItemCode);
			}
		}
		// RE201502184_調整餐交際費預算檢核條件 ec0416 2015/06/23 end

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			BigDecimal amt = (BigDecimal) list.get(0);
			if (amt != null) {
				return amt;
			}
		}
		return BigDecimal.ZERO;
	}

	// RE201403462_預算修改 CU3178 2014/10/24 START
	// RE201403338_配合預算實支控管103年規定 CU3178 2014/10/12 START
	@SuppressWarnings("unchecked")
	public BigDecimal getProjectBudgetItemAppliedWithoutRemitMoneyDateAmt(String year, String budgetItemCode, String arrangeUnitCode, String expApplNo, String startDate, String endDate, boolean state) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return BigDecimal.ZERO;
		}
		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("SELECT COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT"); // 借方金額減貸方金額
		queryString.append(" FROM  ");
		queryString.append(" (" + " SELECT" + " DISTINCT" + " TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO," + " TBEXP_ENTRY.ID AS ENTRY_ID," + " TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE," + " TBEXP_ENTRY.AMT AS AMT" + " FROM" + " TBEXP_ENTRY" + " INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)" + " INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)" + " INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID" + " INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID" + " INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)" + " INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)" + " INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)" + " INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)" + " INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE" + " INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID" + " INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID" + " LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID");

		// AND TBEXP_MIDDLE_TYPE.ID != 'Q10' --排除費用中分類=Q10匯回款項
		queryString.append(" WHERE  TBEXP_MIDDLE_TYPE.ID !=:middleTypeCodeQ10");
		params.put("middleTypeCodeQ10", MiddleTypeCode.CODE_Q10.getCode());

		queryString.append(" AND TBEXP_APPL_STATE.CODE !=:applStateCode "); // 「費用申請單.申請單狀態」不等於"刪件"
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		queryString.append(" AND TBEXP_APPL_STATE.CODE !=:applstatetemp "); // 「費用申請單.申請單狀態」不等於"暫存"
		params.put("applstatetemp", ApplStateCode.TEMP.getCode());

		// 申請單號
		if (StringUtils.isNotBlank(expApplNo)) {
			// AND TBEXP_EXPAPPL_C.EXP_APPL_NO != 'B00200912160019'
			// --費用申請單號(本次申請)
			queryString.append(" AND TBEXP_EXPAPPL_C.EXP_APPL_NO !=:expApplNo");
			params.put("expApplNo", expApplNo);
		}

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2009' --參數'預算年度'
			queryString.append(" AND SUBSTR(TBEXP_EXPAPPL_C.EXP_YEARS,0,4) = :year");
			params.put("year", year);
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號
			queryString.append(" AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}

		// 是否已日結
		if (state) {
			// 結帳起訖日
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				// AND MAIN.SUBPOENA_DATE = '' --參數'結帳起訖日'
				queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN :startDate AND :endDate");
				params.put("startDate", startDate);
				params.put("endDate", endDate);
			}
			queryString.append(" AND TBEXP_APPL_STATE.CODE =:isDailyApplStateCode "); // 「費用申請單.申請單狀態」等於"日結"
			params.put("isDailyApplStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		} else {
			queryString.append(" AND TBEXP_APPL_STATE.CODE !=:notDailyApplStateCode "); // 「費用申請單.申請單狀態」不等於"日結"
			params.put("notDailyApplStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		}

		// queryString.append(" AND NVL(TBEXP_EXPAPPL_C.PROJECT_CODE,' ') =' '");

		queryString.append("UNION ALL ");

		queryString.append("SELECT ");
		queryString.append("DISTINCT ");
		queryString.append("    TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO, ");
		queryString.append("    TBEXP_ENTRY.ID AS ENTRY_ID, ");
		queryString.append("    TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE, ");
		queryString.append("    TBEXP_ENTRY.AMT AS AMT ");
		queryString.append("FROM ");
		queryString.append("    TBEXP_ENTRY ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		queryString.append("    INNER JOIN TBEXP_EXPAPPL_D ON (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) ");
		queryString.append("    LEFT JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID) ");
		queryString.append("    INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID = TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID ");
		queryString.append("    INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID ");
		queryString.append("    INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID ");
		queryString.append("    INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID) ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		queryString.append("    INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) ");
		queryString.append("    INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		queryString.append("    INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE ");
		queryString.append("    INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID ");
		queryString.append("    INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID ");
		queryString.append("    LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("	WHERE TBEXP_APPL_STATE.CODE !=:applStateCode ");
		// queryString.append("    AND (TBEXP_MIDDLE_TYPE.CODE IN ('T09','T30','T31','T32','T33','T34','T35','T14') OR TBEXP_EXPAPPL_D.EXP_APPL_NO = 'T06201409050001')");
		queryString.append("    AND (TBEXP_MIDDLE_TYPE.CODE IN ('T09','T30','T31','T32','T33','T34','T35','T14') OR TBEXP_EXPAPPL_D.EXP_APPL_NO = 'T06201409050001')");
		// queryString.append("    AND NVL(TBEXP_MALACC_APPL.PROJECT_CODE,' ') =' ' ");
		queryString.append("    AND (TBEXP_EXPAPPL_D.COST_TYPE IS NULL OR  TBEXP_EXPAPPL_D.COST_TYPE !='W') ");
		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_D.CLOSE_DATE LIKE :year --參數'預算年度'
			queryString.append("    AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYY') = :year ");

		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號
			queryString.append("    AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode ");

		}
		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append("    AND BUDGET_DEP_CODE =:arrangeUnitCode ");

		}
		// 是否已日結
		if (state) {
			// 結帳起訖日
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				// AND MAIN.SUBPOENA_DATE = '' --參數'結帳起訖日'
				queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN :startDate AND :endDate");
			}
			queryString.append(" AND TBEXP_APPL_STATE.CODE =:isDailyApplStateCode "); // 「費用申請單.申請單狀態」等於"日結"

			// 已日結計算外部帳務系統
			queryString.append("UNION ALL ");
			queryString.append(" " + " SELECT" + " DISTINCT" + " N'' AS EXP_APPL_NO," + " ESE.ID AS ENTRY_ID," + " ET.ENTRY_VALUE AS ENTRY_VALUE," + " ESE.AMT AS AMT" + " FROM" + " TBEXP_EXT_SYS_ENTRY ESE" + " INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID " + " INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE " + " INNER JOIN TBEXP_BUDGET_ITEM BUD ON BUD.ID=ACCT.TBEXP_BUG_ITEM_ID");

			// 預算項目代號
			if (StringUtils.isNotBlank(budgetItemCode)) {
				// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號
				queryString.append(" WHERE BUD.CODE =:budgetItemCode");
				params.put("budgetItemCode", budgetItemCode);
			}

			// 編列單位代號
			if (StringUtils.isNotBlank(arrangeUnitCode)) {
				// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
				queryString.append(" AND ESE.COST_UNIT_CODE =:arrangeUnitCode");
				params.put("arrangeUnitCode", arrangeUnitCode);
			}
			// 結帳起訖日
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				// AND MAIN.SUBPOENA_DATE = '' --參數'結帳起訖日'
				queryString.append(" AND TO_CHAR(ESE.SUBPOENA_DATE,'YYYYMMDD') BETWEEN :startDate AND :endDate");
				params.put("startDate", startDate);
				params.put("endDate", endDate);
			}

		} else {
			queryString.append(" AND TBEXP_APPL_STATE.CODE !=:notDailyApplStateCode "); // 「費用申請單.申請單狀態」等於"日結"
		}

		queryString.append(" ) T ");

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			BigDecimal amt = (BigDecimal) list.get(0);
			if (amt != null) {
				return amt;
			}
		}
		return BigDecimal.ZERO;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal getProjectBudgetItemAppliedByRemitMoneyDateAmt(String year, String budgetItemCode, String arrangeUnitCode, String startDate, String endDate, boolean state) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return BigDecimal.ZERO;
		}
		StringBuffer queryString = new StringBuffer();

		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append("SELECT   COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS AMT ");
		queryString.append(" FROM  TBEXP_ENTRY");
		queryString.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)" + " INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)" + " INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID" + " INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID" + " INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)" + " INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)" + " INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)" + " INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)" + " INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE" + " INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID" + " INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID" + " LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");

		// WHERE TBEXP_APPL_STATE.CODE != '99'--「費用申請單.申請單狀態」不等於"刪件"
		queryString.append(" WHERE TBEXP_APPL_STATE.CODE !=:applStateCode");
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		// AND TBEXP_MIDDLE_TYPE.ID = 'Q10' --費用中分類=Q10匯回款項
		queryString.append(" AND TBEXP_MIDDLE_TYPE.ID =:middleTypeCodeQ10");
		params.put("middleTypeCodeQ10", MiddleTypeCode.CODE_Q10.getCode());

		// AND TBEXP_ENTRY_TYPE.ENTRY_VALUE = 'C'--「分錄借貸別」=貸方
		queryString.append(" AND TBEXP_ENTRY_TYPE.ENTRY_VALUE =:entryTypeValueC");
		params.put("entryTypeValueC", EntryTypeValueCode.C.getValue());

		// 預算年度
		if (StringUtils.isNotBlank(year)) {
			// AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '' --參數'預算年度'
			queryString.append(" AND SUBSTR(TBEXP_EXPAPPL_C.EXP_YEARS,0,4) = :year");
			params.put("year", year);
		}

		// 預算項目代號
		if (StringUtils.isNotBlank(budgetItemCode)) {
			// AND TBEXP_BUDGET_ITEM.CODE = ''-- 參數'預算項目代號'
			queryString.append(" AND TBEXP_BUDGET_ITEM.CODE =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			// AND BUDGET_DEP_CODE = '' --參數'編列單位代號'
			queryString.append(" AND BUDGET_DEP_CODE =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		// 是否已日結
		if (state) {
			// 結帳起訖日
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				// AND MAIN.SUBPOENA_DATE = '' --參數'結帳起訖日'
				queryString.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN :startDate AND :endDate");
				params.put("startDate", startDate);
				params.put("endDate", endDate);
			}
			queryString.append(" AND TBEXP_APPL_STATE.CODE =:isDailyApplStateCode "); // 「費用申請單.申請單狀態」等於"日結"
			params.put("isDailyApplStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		} else {
			queryString.append(" AND TBEXP_APPL_STATE.CODE !=:notDailyApplStateCode "); // 「費用申請單.申請單狀態」等於"日結"
			params.put("notDailyApplStateCode", ApplStateCode.DAILY_CLOSED.getCode());
		}

		List list = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));

		if (!CollectionUtils.isEmpty(list)) {
			BigDecimal amt = (BigDecimal) list.get(0);
			if (amt != null) {
				return amt;
			}
		}
		return BigDecimal.ZERO;
	}

	// RE201403462_預算修改 CU3178 2014/10/24 START

	public List<BudgetIn> findBudgetInBy103yearParams(String year, String budgetItemCode, String arrangeUnitCode) {
		if (StringUtils.isBlank(year) || StringUtils.isBlank(budgetItemCode) || StringUtils.isBlank(arrangeUnitCode)) {
			return null;
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct b");
		queryString.append(" from BudgetIn b");

		Map<String, Object> params = new HashMap<String, Object>();
		// 預算年度:「預算轉入檔.專案類別代碼」=傳入參數”專案類別代碼”
		/*
		 * if (StringUtils.isNotBlank(projectTypeCode.getCode())) { if
		 * (params.size() == 0) { queryString.append(" WHERE"); }else{
		 * queryString.append(" AND"); }
		 * queryString.append(" b.projectType =:projectType");
		 * params.put("projectType", projectTypeCode.getCode()); }
		 */
		// 預算年度:「預算轉入檔.預算年度」=傳入參數”預算年度”
		if (StringUtils.isNotBlank(year)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.year =:year");
			params.put("year", year);
		}

		// 預算項目代號:「預算轉入檔.預算項目代號」=暫存變數”預算項目代號”
		if (StringUtils.isNotBlank(budgetItemCode)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.budgetItemCode =:budgetItemCode");
			params.put("budgetItemCode", budgetItemCode);
		}

		// 編列單位代號:「預算轉入檔. 編列單位代號」=暫存變數”編列單位代號”
		if (StringUtils.isNotBlank(arrangeUnitCode)) {
			if (params.size() == 0) {
				queryString.append(" WHERE");
			} else {
				queryString.append(" AND");
			}
			queryString.append(" b.arrangeUnitCode =:arrangeUnitCode");
			params.put("arrangeUnitCode", arrangeUnitCode);
		}
		List<BudgetIn> entryList = findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(entryList)) {
			return null;
		} else {
			return entryList;
		}
	}

	// RE201403338_配合預算實支控管103年規定 CU3178 2014/10/12 END
	// RE201403462_預算修改 CU3178 2014/10/24 START
	public List findPhoneDetailAmt() {
		Map<String, Object> params = new HashMap<String, Object>();
		StringBuffer querySql = new StringBuffer();

		querySql.append(" SELECT ");
		querySql.append(" TO_CHAR(BUD.BUDGETCODE) AS CODE, ");
		querySql.append(" TO_CHAR(DEP.NAME) AS NAME,  ");
		querySql.append(" NVL(BAMT,0) AS BAMT, ");
		querySql.append(" NVL(AMT1_9,0), ");
		querySql.append(" ROUND((NVL(BAMT,0)-NVL(T5.AMT1_9,0)-NVL(T6.Q10AMT1_9,0))) AS REMINAMT, ");
		querySql.append(" ROUND(NVL(T1.AMT,0)-NVL(T2.Q10AMT,0)" + "NVL(T3.AMT,0)-NVL(T4.Q10AMT,0)-NVL(T5.AMT1_9,0)-NVL(T6.Q10AMT1_9,0)) AS AMT ");
		querySql.append(" FROM  ");
		querySql.append(" ( ");
		querySql.append(" SELECT  ");
		querySql.append("  SUM(BUDGET_ITEM_AMT) AS BAMT,  ");
		querySql.append("  BUD.ARRANGE_UNIT_CODE AS BUDGETCODE ");
		querySql.append("  FROM TBEXP_BUDGET_ITEM BITEM ");
		querySql.append("  LEFT JOIN( ");
		querySql.append("  SELECT *  ");
		querySql.append("  FROM TBEXP_BUDGET_IN WHERE YEAR='2014'  )BUD ");
		querySql.append("  ON BITEM.CODE=BUD.BUDGET_ITEM_CODE ");
		querySql.append(" WHERE  ");
		querySql.append("  BITEM.CODE ='62020100' ");
		querySql.append(" GROUP BY   BUD.ARRANGE_UNIT_CODE ");
		querySql.append(" ) BUD ");
		querySql.append(" INNER JOIN ( ");
		querySql.append(" SELECT  ");
		querySql.append(" DEP.BUDGET_DEP_CODE,  ");
		querySql.append(" DEP.NAME   ");
		querySql.append(" FROM TBEXP_DEPARTMENT DEP ");
		querySql.append(" INNER JOIN TBEXP_DEP_TYPE DTYPE ON DTYPE.ID=DEP.TBEXP_DEP_TYPE_ID ");
		querySql.append(" INNER JOIN TBEXP_DEP_LEVEL_PROP PROP ON PROP.ID=DEP.TBEXP_DEP_LEVEL_PROP_ID ");
		querySql.append(" where dtype.code in('0','1') AND PROP.CODE='1' ");
		querySql.append(" ) DEP ON DEP.BUDGET_DEP_CODE=BUD.BUDGETCODE ");
		querySql.append(" LEFT JOIN  ");
		querySql.append("  ( ");
		querySql.append(" SELECT  ");
		querySql.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT ");
		querySql.append(" ,T.CODE AS BUDGETCODE,T.DEPCODE ");
		querySql.append(" FROM (  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT  ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO, ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID, ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE, ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT ");
		querySql.append(" FROM TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)  ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID  ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE ='90'  AND TBEXP_MIDDLE_TYPE.ID !='Q10'  ");
		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20141231' ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE ='62020100' ");

		querySql.append(" UNION ALL  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT   ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO,    ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID,    ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE,    ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT FROM   ");
		querySql.append(" TBEXP_ENTRY    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)   ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_D ON (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) ");
		querySql.append(" LEFT JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID)   ");
		querySql.append(" INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID = TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID   ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID     ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)    ");
		querySql.append(" LEFT JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)    ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID   ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID    ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE ='90'     AND TBEXP_MIDDLE_TYPE.CODE IN ('T09','T30','T31','T32','T33','T34','T35','T14')     AND (TBEXP_EXPAPPL_D.COST_TYPE IS  NULL OR  TBEXP_EXPAPPL_D.COST_TYPE !='W')     ");
		querySql.append(" AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYYMMDD') LIKE '2014%'  ");
		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20141231' ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE  ='62020100' ");
		querySql.append(" ) T ");
		querySql.append(" GROUP BY CODE,DEPCODE ");
		querySql.append(" )T1 ON T1.DEPCODE=BUD.BUDGETCODE ");

		querySql.append(" LEFT JOIN ( ");
		querySql.append(" SELECT   COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS Q10AMT, TBEXP_BUDGET_ITEM.CODE AS CODE ,BUDGET_DEP_CODE AS DEPCODE ");
		querySql.append(" FROM  TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)  ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID  ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID   ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE !='99'  ");
		querySql.append(" AND TBEXP_MIDDLE_TYPE.ID ='Q10' ");
		querySql.append(" AND TBEXP_ENTRY_TYPE.ENTRY_VALUE ='C'  ");
		querySql.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2014%'  ");
		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20141231' ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE  ='62020100' ");
		querySql.append(" GROUP BY TBEXP_BUDGET_ITEM.CODE,BUDGET_DEP_CODE ");
		querySql.append(" ) T2 ON T2.DEPCODE=BUD.BUDGETCODE ");
		querySql.append(" LEFT JOIN  ");
		querySql.append(" ( ");
		querySql.append(" SELECT  ");
		querySql.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT ");
		querySql.append(" ,T.CODE AS BUDGETCODE,T.DEPCODE ");
		querySql.append(" FROM (  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT  ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO, ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID, ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE, ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT ");
		querySql.append(" FROM TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID  ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)  ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE !='99' AND TBEXP_APPL_STATE.CODE !='00' AND TBEXP_APPL_STATE.CODE !='90'  AND  TBEXP_MIDDLE_TYPE.ID !='Q10'  ");
		querySql.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE  ='62020100' ");

		querySql.append(" UNION ALL  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT   ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO,    ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID,    ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE,     ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT FROM   ");
		querySql.append(" TBEXP_ENTRY    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)   ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_D ON (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) ");
		querySql.append(" LEFT JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID)   ");
		querySql.append(" INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID = TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID   ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID     ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)    ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)   ");
		querySql.append(" LEFT JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)    ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID    ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID     ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE !='99' AND TBEXP_APPL_STATE.CODE !='90'    AND TBEXP_MIDDLE_TYPE.CODE IN ('T09','T31') ");
		querySql.append(" AND (TBEXP_EXPAPPL_D.COST_TYPE IS NULL OR  TBEXP_EXPAPPL_D.COST_TYPE !='W')     ");
		querySql.append(" AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYYMMDD') LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE  ='62020100' ");
		querySql.append(" ) T ");
		querySql.append(" GROUP BY CODE,DEPCODE ");
		querySql.append(" )T3 ON T3.DEPCODE=BUD.BUDGETCODE ");
		querySql.append(" LEFT JOIN ( ");
		querySql.append(" SELECT   COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS Q10AMT, TBEXP_BUDGET_ITEM.CODE AS CODE  ,BUDGET_DEP_CODE AS DEPCODE ");
		querySql.append(" FROM  TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)  ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID  ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID   ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE !='99'  ");
		querySql.append(" AND TBEXP_MIDDLE_TYPE.ID ='Q10' ");
		querySql.append(" AND TBEXP_APPL_STATE.CODE !='90' ");
		querySql.append(" AND TBEXP_ENTRY_TYPE.ENTRY_VALUE ='C'  ");
		querySql.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE ='62020100' ");
		querySql.append(" GROUP BY TBEXP_BUDGET_ITEM.CODE,BUDGET_DEP_CODE ");
		querySql.append(" ) T4 ON T4.DEPCODE=BUD.BUDGETCODE ");

		querySql.append(" LEFT JOIN  ");
		querySql.append(" ( ");
		querySql.append(" SELECT  ");
		querySql.append(" COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'D', T.AMT, 0)),0) - COALESCE(SUM(DECODE(T.ENTRY_VALUE, 'C', T.AMT, 0)),0) AS AMT1_9 ");
		querySql.append(" ,T.CODE AS BUDGETCODE1_9,T.DEPCODE ");
		querySql.append(" FROM (  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT  ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_C.EXP_APPL_NO AS EXP_APPL_NO, ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID, ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE, ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT ");
		querySql.append(" FROM TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)  ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID  ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID) ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)  ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE  TBEXP_MIDDLE_TYPE.ID !='Q10'  ");
		querySql.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE ='62020100' ");
		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20140930' ");
		querySql.append(" AND TBEXP_APPL_STATE.CODE ='90' ");
		querySql.append(" UNION ALL  ");
		querySql.append(" SELECT ");
		querySql.append(" DISTINCT   ");
		querySql.append(" BUDGET_DEP_CODE AS DEPCODE, ");
		querySql.append(" TBEXP_BUDGET_ITEM.CODE AS CODE, ");
		querySql.append(" TBEXP_EXPAPPL_D.EXP_APPL_NO AS EXP_APPL_NO,    ");
		querySql.append(" TBEXP_ENTRY.ID AS ENTRY_ID,    ");
		querySql.append(" TBEXP_ENTRY_TYPE.ENTRY_VALUE AS ENTRY_VALUE,     ");
		querySql.append(" TBEXP_ENTRY.AMT AS AMT FROM   ");
		querySql.append(" TBEXP_ENTRY    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID)   ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_D ON (TBEXP_EXPAPPL_D.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID) ");
		querySql.append(" LEFT JOIN TBEXP_MALACC_APPL ON (TBEXP_MALACC_APPL.ID=TBEXP_EXPAPPL_D.ID)   ");
		querySql.append(" INNER JOIN TBEXP_D_CHECK_DETAIL ON TBEXP_D_CHECK_DETAIL.ID = TBEXP_EXPAPPL_D.TBEXP_D_CHECK_DETAIL_ID   ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_D_CHECK_DETAIL.TBEXP_MIDDLE_TYPE_ID     ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_D.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID    ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID)    ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)  ");
		querySql.append(" LEFT JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID)    ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE   ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID   ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID     ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID ");
		querySql.append(" WHERE  TBEXP_MIDDLE_TYPE.CODE IN ('T09','T30','T31','T32','T33','T34','T35','T14') ");
		querySql.append(" AND (TBEXP_EXPAPPL_D.COST_TYPE IS NULL OR  TBEXP_EXPAPPL_D.COST_TYPE !='W')     ");
		querySql.append(" AND TO_CHAR(TBEXP_EXPAPPL_D.CLOSE_DATE, 'YYYYMMDD') LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE ='62020100' ");

		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20140930' ");
		querySql.append(" AND TBEXP_APPL_STATE.CODE ='90' ");

		querySql.append(" ) T ");
		querySql.append(" GROUP BY CODE,DEPCODE ");
		querySql.append(" )T5 ON T5.DEPCODE=BUD.BUDGETCODE ");
		querySql.append(" LEFT JOIN ( ");
		querySql.append(" SELECT   COALESCE(SUM(TBEXP_ENTRY.AMT), 0) AS Q10AMT1_9, TBEXP_BUDGET_ITEM.CODE AS CODE1_9 ,BUDGET_DEP_CODE AS DEPCODE ");
		querySql.append(" FROM  TBEXP_ENTRY ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_GROUP ON (TBEXP_ENTRY_GROUP.ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID) ");
		querySql.append(" INNER JOIN TBEXP_EXPAPPL_C ON (TBEXP_EXPAPPL_C.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY_GROUP.ID)  ");
		querySql.append(" INNER JOIN TBEXP_MIDDLE_TYPE ON TBEXP_MIDDLE_TYPE.ID = TBEXP_EXPAPPL_C.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append(" INNER JOIN TBEXP_APPL_STATE ON TBEXP_EXPAPPL_C.TBEXP_APPL_STATE_ID = TBEXP_APPL_STATE.ID ");
		querySql.append(" INNER JOIN TBEXP_ENTRY_TYPE ON (TBEXP_ENTRY_TYPE.ID = TBEXP_ENTRY.TBEXP_ENTRY_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE ON (TBEXP_ACC_TITLE.ID = TBEXP_ENTRY.TBEXP_ACC_TITLE_ID) ");
		querySql.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ON (TBEXP_ACC_CLASS_TYPE.ID = TBEXP_ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID)  ");
		querySql.append(" INNER JOIN TBEXP_BUDGET_ITEM ON (TBEXP_BUDGET_ITEM.ID = TBEXP_ACC_TITLE.TBEXP_BUG_ITEM_ID) ");
		querySql.append(" INNER JOIN TBEXP_DEPARTMENT COST_DEP ON COST_DEP.CODE = COST_UNIT_CODE  ");
		querySql.append(" INNER JOIN TBEXP_ACC_TITLE_EXP_ITEM_R ON TBEXP_ACC_TITLE.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_ACC_TITLE_ID  ");
		querySql.append(" INNER JOIN TBEXP_EXP_ITEM ON TBEXP_EXP_ITEM.ID = TBEXP_ACC_TITLE_EXP_ITEM_R.TBEXP_EXP_ITEM_ID  ");
		querySql.append(" LEFT JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID = TBEXP_ENTRY.TBEXP_ENTRY_GROUP_ID  ");
		querySql.append(" WHERE TBEXP_APPL_STATE.CODE !='99'  ");
		querySql.append(" AND TBEXP_MIDDLE_TYPE.ID ='Q10' ");
		querySql.append(" AND TBEXP_ENTRY_TYPE.ENTRY_VALUE ='C'  ");
		querySql.append(" AND TBEXP_EXPAPPL_C.EXP_YEARS LIKE '2014%'  ");
		querySql.append(" AND TBEXP_BUDGET_ITEM.CODE ='62020100' ");
		querySql.append(" AND TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') BETWEEN '20140101' AND '20140930' ");
		querySql.append(" AND TBEXP_APPL_STATE.CODE ='90' ");
		querySql.append(" GROUP BY TBEXP_BUDGET_ITEM.CODE,BUDGET_DEP_CODE ");
		querySql.append(" ) T6 ON T6.DEPCODE=BUD.BUDGETCODE ");
		querySql.append(" ORDER BY BUDGETCODE ");
		List list = new ArrayList();
		final String sqlString = StringUtils.queryStringAssembler(querySql.toString(), params);

		list = getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query query = em.createNativeQuery(sqlString);
				return query.getResultList();
			}
		});
		return list;
	}

	// RE201403462_預算修改 CU3178 2014/10/24 END

	// RE201500189_國內出差申請作業流程簡化 EC0416 2015/04/10 start
	// 檢核填寫的專案代號 在資料庫中是否有該專案代號
	public List<BudgetIn> findprojectcode(String projectCode, String year) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct b");
		queryString.append(" from BudgetIn b");
		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append(" WHERE");
		// 專案代碼:「預算轉入檔.專案代號」=傳入參數”專案代號”
		queryString.append("  b.projectCode =:projectCode");
		params.put("projectCode", projectCode);// 放入專案代號

		queryString.append(" AND  b.year =:year");
		params.put("year", year);

		List<BudgetIn> list = findByNamedParams(queryString.toString(), params);
		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	// RE201500189_國內出差申請作業流程簡化 EC0416 2015/04/10 end

	// RE201603570_預算暨費用系統_共用功能與模組整合 EC0416 2017/1/4 start
	/**
	 * 讀取預算年度預算檔
	 * 
	 * @return
	 */
	public List readBudgetIn() {

		StringBuffer querySql = new StringBuffer();

		querySql.append(" SELECT * FROM BFMADMIN.VWBFM_BUDGET_EXPORT");
		Map<String, Object> params = new HashMap<String, Object>();

		List list = new ArrayList();
		final String sqlString = StringUtils.queryStringAssembler(querySql.toString(), params);

		list = getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query query = em.createNativeQuery(sqlString);
				return query.getResultList();
			}
		});

		return list;
	}
	// RE201603570_預算暨費用系統_共用功能與模組整合 EC0416 2017/1/4 end
}