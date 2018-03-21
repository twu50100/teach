package tw.com.skl.exp.kernel.model6.dao.jpa;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.orm.jpa.JpaCallback;

import tw.com.skl.common.model6.dao.jpa.BaseDaoImpl;
import tw.com.skl.exp.kernel.model6.bo.ExpMain;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dao.ExpMainDao;
import tw.com.skl.exp.kernel.model6.dto.AccruedExpenseDto;
import tw.com.skl.exp.kernel.model6.dto.ApplStateFlowDto;
import tw.com.skl.exp.kernel.model6.dto.BudExpDetailDto;
import tw.com.skl.exp.kernel.model6.dto.BudExpPayDetailDto;
import tw.com.skl.exp.kernel.model6.dto.EntryDto;
import tw.com.skl.exp.kernel.model6.dto.ExpMainDto;
import tw.com.skl.exp.kernel.model6.dto.IncomeOutcomeTaxDto;
import tw.com.skl.exp.kernel.model6.dto.KPIDetailDto;
import tw.com.skl.exp.kernel.model6.dto.LocalSalesmanEncouragementDetailDto;
import tw.com.skl.exp.kernel.model6.dto.MealExpenseCancelDetailDto;
import tw.com.skl.exp.kernel.model6.dto.OfficeExpReportedThatSupportDto;
import tw.com.skl.exp.kernel.model6.dto.OutMsgRewardDto;
import tw.com.skl.exp.kernel.model6.dto.PrizeExpenseCancelDetailDto;
import tw.com.skl.exp.kernel.model6.dto.ProjectBudExpDto;
import tw.com.skl.exp.kernel.model6.dto.ProjectDetailBudExpDto;
import tw.com.skl.exp.kernel.model6.dto.TableADto;
import tw.com.skl.exp.kernel.model6.dto.TableBDto;
import tw.com.skl.exp.kernel.model6.dto.VoucherDataDto;

/**
 * 費用主檔的Dao介面。
 * 
 * <pre>
 * Revision History
 * 2009/03/10, Sunkist Wang update 報表本月金額與累計金額相同的問題。
 * </pre>
 * 
 * @author CL Chang
 * @version 1.0, 2009/05/21
 * @param <ApplStateFlowDto>
 */

// PeterYu RE201200382_20120808
// 1.D11.1業推業發費
// a.計算”已核銷總額、還款總額、未核銷總額”值時，資料來源僅需為中分類510、610之申請單資料，
// 新增INNER JOIN TBEXP_MIDDLE_TYPE MIDDLE及異動WHERE 條件將KIND.CODE = '2'
// 修改為MIDDLE.CODE IN ('510','610')
// 註:現行KIND.CODE = '2'的費用中分類有510、610、810，於D11.1業推業發費僅需顯示510、610的資料。
// b.按【檔案下載】鈕時，資料修改方式與a相同，資料來源僅需有費用中分類MIDDLE.CODE IN ('510','610')的資料即可。

public class ExpMainDaoImpl extends BaseDaoImpl<ExpMain, String> implements ExpMainDao {

	@SuppressWarnings("unchecked")
	private List findByNativeSQL(final String sqlString, final List<Object> parameters) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createNativeQuery(sqlString);
				if (!CollectionUtils.isEmpty(parameters)) {
					for (int i = 0; i < parameters.size(); i++) {
						Object parameter = parameters.get(i);
						if (parameter instanceof Calendar) {
							queryObject = queryObject.setParameter(i + 1, (Calendar) parameter, TemporalType.TIMESTAMP);
						} else {
							queryObject = queryObject.setParameter(i + 1, parameter);
						}
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<String> findWithCancelCode(String cancelCode) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT DISTINCT ENTRY.CANCEL_CODE FROM TBEXP_EXP_MAIN EXP");
		queryString.append("   INNER JOIN TBEXP_ENTRY ENTRY");
		queryString.append("   ON EXP.TBEXP_ENTRY_GROUP_ID = ENTRY.TBEXP_ENTRY_GROUP_ID");
		queryString.append(" WHERE NOT(ENTRY.CANCEL_CODE IS NULL)");
		List<String> list = findByNativeSQL(queryString.toString(), null);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<EntryDto> findCancelDetail1(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 11.1 轉出明細： JPQL 轉換成 SQL (IISI modified by Sunkist 2010/06/23 修訂)
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();

				// PeterYu RE201200382_20120808 Update start
				queryString.append(" SELECT "); // --已核銷明細
				queryString.append("        RAT.SUBPOENA_DATE, ");
				queryString.append("        RAT.SUBPOENA_NO, ");
				queryString.append("        RAT.ACCTCODE, ");
				queryString.append("        RAT.COST_UNIT_CODE, ");
				queryString.append("        RAT.DAMT  ");
				queryString.append("   FROM (SELECT  "); // --已核銷明細
				queryString.append("                SUB.SUBPOENA_DATE,  "); // --作帳日
				queryString.append("                SUB.SUBPOENA_NO,  "); // --傳票號碼
				queryString.append("                ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
				queryString.append("                E.COST_UNIT_CODE,  "); // --成本單位代號
				queryString.append("                ET.ENTRY_VALUE,  ");
				queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
				queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  "); // --貸方金額
				queryString.append("                   DECODE(ET.ENTRY_VALUE || ACCT.CODE,'C10130200',E.AMT,0) AS PAYMENT_AMT  "); // --貸方金額
				// 還款金額
				queryString.append("           FROM TBEXP_ENTRY E  ");
				queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append("          INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID  ");
				queryString.append("          WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
				queryString.append("                                          B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append("                                     FROM TBEXP_EXPAPPL_B B  ");
				queryString.append("                                     INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("                                     INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID  ");
				queryString.append("                                  INNER JOIN TBEXP_MIDDLE_TYPE MID on MID.TBEXP_KIND_TYPE_ID = KIND.ID");
				queryString.append("                                     INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append("                                     WHERE MID.CODE in ('510','610') "); // --2.主任/組長
				queryString.append("            AND STATE.CODE = '90')  "); // --日結
				queryString.append("         ) RAT  ");
				queryString.append("  WHERE RAT.ENTRY_VALUE = 'D'  ");
				queryString.append("    AND RAT.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");
				// PeterYu RE201200382_20120808 Update end

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<EntryDto> list = new ArrayList<EntryDto>();
				for (Object[] cols : rows) {
					EntryDto dto = new EntryDto();
					Timestamp ts = (Timestamp) cols[0];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());
					dto.setSubpoenaDate(subpoenaDate);
					dto.setSubpoenaNo((String) cols[1]);
					dto.setAccTitleCode((String) cols[2]);
					dto.setCostUnitCode((String) cols[3]);
					dto.setAmt((BigDecimal) cols[4]);
					list.add(dto);
				}
				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<EntryDto> findCancelDetail2(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 11.2 轉出明細： JPQL 轉換成 SQL (IISI modified by Sunkist 2010/06/23 修訂)
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append(" SELECT "); // --已核銷明細
				queryString.append("        RAT.SUBPOENA_DATE, ");
				queryString.append("        RAT.SUBPOENA_NO, ");
				queryString.append("        RAT.ACCTCODE, ");
				queryString.append("        RAT.COST_UNIT_CODE, ");
				queryString.append("        RAT.DAMT  ");
				queryString.append("   FROM (SELECT "); // --已核銷明細
				queryString.append(" SUB.SUBPOENA_DATE, "); // --作帳日
				queryString.append(" SUB.SUBPOENA_NO, "); // --傳票號碼
				queryString.append(" ACCT.CODE AS ACCTCODE, "); // --會計科目代號
				queryString.append(" E.COST_UNIT_CODE, "); // --成本單位代號
				queryString.append(" ET.ENTRY_VALUE,  ");
				queryString.append(" DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT, "); // --借方金額
				queryString.append(" DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT "); // --貸方金額
				queryString.append(" FROM TBEXP_ENTRY E ");
				queryString.append(" INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append(" INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append(" INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID  ");
				queryString.append(" WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT "); // --取得核銷資料
				queryString.append(" B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append(" FROM TBEXP_EXPAPPL_B B  ");
				queryString.append(" INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append(" INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append(" WHERE   TBEXP_RATIFY_ID IN (SELECT "); // --處長駐區辦公費
				// 核銷代號
				queryString.append("                         RAT.ID  ");
				queryString.append(" FROM TBEXP_RATIFY RAT  ");
				queryString.append(" INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID ");
				queryString.append(" WHERE MID.CODE IN ('310','320','330','3A0','3B0','210','220','230','240','250','260','2A0','2B0','2C0','2D0','G10','710')) ");
				queryString.append(" AND STATE.CODE = '90') "); // --日結
				queryString.append(" ) RAT  ");
				queryString.append("  WHERE RAT.ENTRY_VALUE = 'D' ");
				queryString.append("    AND RAT.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<EntryDto> list = new ArrayList<EntryDto>();
				for (Object[] cols : rows) {
					EntryDto dto = new EntryDto();
					Timestamp ts = (Timestamp) cols[0];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());
					dto.setSubpoenaDate(subpoenaDate);
					dto.setSubpoenaNo((String) cols[1]);
					dto.setAccTitleCode((String) cols[2]);
					dto.setCostUnitCode((String) cols[3]);
					dto.setAmt((BigDecimal) cols[4]);
					list.add(dto);
				}
				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<EntryDto> findCancelDetail3(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 11.3 轉出明細： JPQL 轉換成 SQL (IISI modified by Sunkist 2010/06/23 修訂)
			// IISI-20100701 新光要求增加中分類 3J0
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append(" SELECT  "); // --已核銷明細
				queryString.append("   RAT.SUBPOENA_DATE, ");
				queryString.append("   RAT.SUBPOENA_NO, ");
				queryString.append("   RAT.ACCTCODE, ");
				queryString.append("   RAT.COST_UNIT_CODE, ");
				queryString.append("   RAT.DAMT  ");
				queryString.append("  FROM (SELECT  "); // --已核銷明細
				queryString.append("         SUB.SUBPOENA_DATE,  "); // --作帳日
				queryString.append("         SUB.SUBPOENA_NO,  "); // --傳票號碼
				queryString.append("         ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
				queryString.append("         E.COST_UNIT_CODE,  "); // --成本單位代號
				queryString.append("         ET.ENTRY_VALUE, ");
				queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
				queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT  "); // --貸方金額
				queryString.append("       FROM TBEXP_ENTRY E  ");
				queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append("         INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append("         INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID  ");
				queryString.append("       WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
				queryString.append("                                           B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append("                                         FROM TBEXP_EXPAPPL_B B  ");
				queryString.append("                                           INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("                                           INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append("                                         WHERE    TBEXP_RATIFY_ID IN (SELECT  "); // --駐區業務獎勵費
																														// 核銷代號
				queryString.append("                                                                     RAT.ID  ");
				queryString.append("                                                                   FROM TBEXP_RATIFY RAT  ");
				queryString.append("                                                                     INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
				queryString.append("                                                                   WHERE MID.CODE IN ('2H0','3H0','3J0')) ");
				queryString.append("                                           AND STATE.CODE = '90')  "); // --日結
				queryString.append("       ) RAT ");
				queryString.append("  WHERE RAT.ENTRY_VALUE = 'D'  ");
				queryString.append("    AND RAT.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");
				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<EntryDto> list = new ArrayList<EntryDto>();
				for (Object[] cols : rows) {
					EntryDto dto = new EntryDto();
					Timestamp ts = (Timestamp) cols[0];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());
					dto.setSubpoenaDate(subpoenaDate);
					dto.setSubpoenaNo((String) cols[1]);
					dto.setAccTitleCode((String) cols[2]);
					dto.setCostUnitCode((String) cols[3]);
					dto.setAmt((BigDecimal) cols[4]);
					list.add(dto);
				}
				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<EntryDto> findCancelDetail11(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 11.2 轉出明細： JPQL 轉換成 SQL (IISI modified by Sunkist 2010/06/23 修訂)
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append(" SELECT "); // --已核銷明細
				queryString.append("        RAT.SUBPOENA_DATE, ");
				queryString.append("        RAT.SUBPOENA_NO, ");
				queryString.append("        RAT.ACCTCODE, ");
				queryString.append("        RAT.COST_UNIT_CODE, ");
				queryString.append("        RAT.DAMT  ");
				queryString.append("   FROM (SELECT "); // --已核銷明細
				queryString.append(" SUB.SUBPOENA_DATE, "); // --作帳日
				queryString.append(" SUB.SUBPOENA_NO, "); // --傳票號碼
				queryString.append(" ACCT.CODE AS ACCTCODE, "); // --會計科目代號
				queryString.append(" E.COST_UNIT_CODE, "); // --成本單位代號
				queryString.append(" ET.ENTRY_VALUE,  ");
				queryString.append(" DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT, "); // --借方金額
				queryString.append(" DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT "); // --貸方金額
				queryString.append(" FROM TBEXP_ENTRY E ");
				queryString.append(" INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append(" INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append(" INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID  ");
				queryString.append(" WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT "); // --取得核銷資料
				queryString.append(" B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append(" FROM TBEXP_EXPAPPL_B B  ");
				queryString.append(" INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append(" INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append(" WHERE   TBEXP_RATIFY_ID IN (SELECT "); // --組織發展費
				// 核銷代號
				queryString.append("                         RAT.ID  ");
				queryString.append(" FROM TBEXP_RATIFY RAT  ");
				queryString.append(" INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID ");
				queryString.append(" WHERE MID.CODE IN ('810')) ");
				queryString.append(" AND STATE.CODE = '90') "); // --日結
				queryString.append(" ) RAT  ");
				queryString.append(" WHERE RAT.ENTRY_VALUE = 'D' ");
				queryString.append(" AND RAT.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<EntryDto> list = new ArrayList<EntryDto>();
				for (Object[] cols : rows) {
					EntryDto dto = new EntryDto();
					Timestamp ts = (Timestamp) cols[0];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());
					dto.setSubpoenaDate(subpoenaDate);
					dto.setSubpoenaNo((String) cols[1]);
					dto.setAccTitleCode((String) cols[2]);
					dto.setCostUnitCode((String) cols[3]);
					dto.setAmt((BigDecimal) cols[4]);
					list.add(dto);
				}
				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findCancelTotal1(Calendar startDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		// PeterYu RE201200382_20120808 Update start
		queryString.append(" SELECT "); // --已核銷總額
		queryString.append("        SUM(RAT.DAMT - PAYMENT_AMT) AS total, ");
		queryString.append("        SUM(PAYMENT_AMT) AS paymentAmt ");
		queryString.append("   FROM (SELECT  "); // --已核銷明細
		queryString.append("                SUB.SUBPOENA_DATE,  "); // --作帳日
		queryString.append("                SUB.SUBPOENA_NO,  "); // --傳票號碼
		queryString.append("                ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
		queryString.append("                E.COST_UNIT_CODE,  "); // --成本單位代號
		queryString.append("                ET.ENTRY_VALUE,  ");
		queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
		queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  "); // --貸方金額
		queryString.append("                DECODE(ET.ENTRY_VALUE || ACCT.CODE,'C10130200',E.AMT,0) AS PAYMENT_AMT  "); // --貸方金額
		// 還款金額
		queryString.append("           FROM TBEXP_ENTRY E ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("          INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("          WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
		queryString.append("                                                   B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("                                              FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                                                INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("                                                INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                                             INNER JOIN TBEXP_MIDDLE_TYPE MID on MID.TBEXP_KIND_TYPE_ID = KIND.ID");
		queryString.append("                                                INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                                             WHERE MID.CODE in ('510','610') "); // --2.主任/組長
		queryString.append("                                               AND STATE.CODE = '90')  "); // --日結
		// PeterYu RE201200382_20120808 Update end
		queryString.append("        ) RAT ");
		queryString.append("  WHERE RAT.SUBPOENA_DATE BETWEEN ");
		queryString.append(" TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')");
		queryString.append(" AND ");
		queryString.append(" TO_DATE('");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')"); // 作帳起迄日
		if (logger.isDebugEnabled()) {
			logger.debug(" findCancelTotal1() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		BigDecimal paymentAmt = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				Object[] record = (Object[]) list.get(0);
				total = (BigDecimal) record[0];
				if (total == null) {
					total = BigDecimal.ZERO;
				}
				paymentAmt = (BigDecimal) record[1];
				if (paymentAmt == null) {
					paymentAmt = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, paymentAmt };
		return result;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findCancelTotal2(Calendar startDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT "); // --已核銷總額 借方金額加總
		queryString.append("        SUM(RAT.DAMT) AS total ");
		queryString.append("   FROM (SELECT  "); // --已核銷明細
		queryString.append("                SUB.SUBPOENA_DATE,  "); // --作帳日
		queryString.append("                SUB.SUBPOENA_NO,  "); // --傳票號碼
		queryString.append("                ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
		queryString.append("                E.COST_UNIT_CODE,  "); // --成本單位代號
		queryString.append("                ET.ENTRY_VALUE,  ");
		queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
		queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT  "); // --貸方金額
		queryString.append("           FROM TBEXP_ENTRY E ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("          INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("          WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
		queryString.append("                                                   B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("                                              FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                                                INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("                                                INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                                                INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                                             WHERE TBEXP_RATIFY_ID IN (SELECT  "); // --處長駐區辦公費
		// 核銷代號
		queryString.append("                                                                              RAT.ID  ");
		queryString.append("                                                                            FROM TBEXP_RATIFY RAT  ");
		queryString.append("                                                                           INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
		queryString.append("                                                                           WHERE MID.CODE IN ('310','320','330','3A0','3B0','210','220','230','240','250','260','2A0','2B0','2C0','2D0','G10','710')) ");
		queryString.append("                                               AND STATE.CODE = '90')  "); // --日結
		queryString.append("        ) RAT ");
		queryString.append("  WHERE RAT.SUBPOENA_DATE BETWEEN ");
		queryString.append(" TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')");
		queryString.append(" AND ");
		queryString.append(" TO_DATE('");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')"); // 作帳起迄日
		if (logger.isDebugEnabled()) {
			logger.debug(" findCancelTotal2() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, null };
		return result;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findCancelTotal3(Calendar startDate, Calendar endDate) {
		// IISI-20100701 新光要求增加中分類 3J0
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT "); // --已核銷總額 借方金額加總
		queryString.append("        SUM(RAT.DAMT) AS total ");
		queryString.append("   FROM (SELECT  "); // --已核銷明細
		queryString.append("                SUB.SUBPOENA_DATE,  "); // --作帳日
		queryString.append("                SUB.SUBPOENA_NO,  "); // --傳票號碼
		queryString.append("                ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
		queryString.append("                E.COST_UNIT_CODE,  "); // --成本單位代號
		queryString.append("                ET.ENTRY_VALUE,  ");
		queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
		queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT  "); // --貸方金額
		queryString.append("           FROM TBEXP_ENTRY E ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("          INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("          WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
		queryString.append("                                                   B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("                                              FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                                                INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("                                                INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                                                INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                                             WHERE TBEXP_RATIFY_ID IN (SELECT  "); // --駐區業務獎勵費
		// 核銷代號
		queryString.append("                                                                              RAT.ID  ");
		queryString.append("                                                                            FROM TBEXP_RATIFY RAT  ");
		queryString.append("                                                                           INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
		queryString.append("                                                                           WHERE MID.CODE IN ('2H0','3H0','3J0')) ");
		queryString.append("                                               AND STATE.CODE = '90')  "); // --日結
		queryString.append("        ) RAT ");
		queryString.append("  WHERE RAT.SUBPOENA_DATE BETWEEN ");
		queryString.append(" TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')");
		queryString.append(" AND ");
		queryString.append(" TO_DATE('");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')"); // 作帳起迄日
		if (logger.isDebugEnabled()) {
			logger.debug(" findCancelTotal2() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, null };
		return result;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findCancelTotal11(Calendar startDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT "); // --已核銷總額 借方金額加總
		queryString.append("        nvl(SUM(RAT.DAMT),0) AS total ");
		queryString.append("   FROM (SELECT  "); // --已核銷明細
		queryString.append("                SUB.SUBPOENA_DATE,  "); // --作帳日
		queryString.append("                SUB.SUBPOENA_NO,  "); // --傳票號碼
		queryString.append("                ACCT.CODE AS ACCTCODE,  "); // --會計科目代號
		queryString.append("                E.COST_UNIT_CODE,  "); // --成本單位代號
		queryString.append("                ET.ENTRY_VALUE,  ");
		queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
		queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT  "); // --貸方金額
		queryString.append("          FROM TBEXP_ENTRY E ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("          INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("          WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  "); // --取得核銷資料
		queryString.append("                                                   B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("                                              FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                                                INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("                                                INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                                                INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                                              WHERE TBEXP_RATIFY_ID IN (SELECT  "); // --處長駐區辦公費
		// 核銷代號
		queryString.append("                                                                              RAT.ID  ");
		queryString.append("                                                                           FROM TBEXP_RATIFY RAT  ");
		queryString.append("                                                                           INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
		queryString.append("                                                                           WHERE MID.CODE IN ('810')) ");
		queryString.append("                                               AND STATE.CODE = '90')  "); // --日結
		queryString.append("        ) RAT ");
		queryString.append("  WHERE RAT.SUBPOENA_DATE BETWEEN ");
		queryString.append(" TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')");
		queryString.append(" AND ");
		queryString.append(" TO_DATE('");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')"); // 作帳起迄日
		if (logger.isDebugEnabled()) {
			logger.debug(" findCancelTotal2() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, null };
		return result;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal findNotCancelTotal1() {
		StringBuffer queryString = new StringBuffer();
		// 2010-06-30 Beryl 修改D11.1 未核銷總額 = 核定金額 - 第一次核銷 - 第二次核銷
		// PeterYu RE201200382_20120808 Update start
		queryString.append(" SELECT SUM(RAT.RATIFY_AMT  - DECODE(B1.VERIFY_AMT1,NULL,0,B1.VERIFY_AMT1)- DECODE(B2.VERIFY_AMT2,NULL,0,B2.VERIFY_AMT2))");// --未核銷總額
																																						// =
																																						// 核定金額
																																						// -
																																						// 第一次核銷
																																						// -
																																						// 第二次核銷
		queryString.append(" FROM TBEXP_RATIFY RAT ");
		queryString.append("  INNER JOIN (SELECT ");
		queryString.append("                MID.ID, ");
		queryString.append("                KIND.CODE AS KINDCODE ");
		queryString.append("              FROM TBEXP_MIDDLE_TYPE MID ");
		queryString.append("                INNER JOIN TBEXP_KIND_TYPE KIND ON MID.TBEXP_KIND_TYPE_ID = KIND.ID                  ");
		queryString.append("              where MID.CODE in ('510','610') "); // --主任/組長
		queryString.append("              ) MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID ");
		queryString.append("  LEFT  JOIN (SELECT ");// --第一次核銷
		queryString.append("                B1.* ");
		queryString.append("              FROM (SELECT ");// --取得借支金額
		queryString.append("                      DISTINCT RANK() OVER(PARTITION BY B.TBEXP_RATIFY_ID ORDER BY B.EXP_APPL_NO DESC) AS RN,");
		queryString.append("                      B.TBEXP_RATIFY_ID,");
		queryString.append("                      SUB.SUBPOENA_DATE AS VERIFY_DATE1, ");// --傳票日期
		queryString.append("                      B.PAY_AMT AS VERIFY_AMT1, ");// --實支金額
		queryString.append("                        B.ADVPAY_AMT AS ADVPAY_AMT1, ");// --借支金額
		queryString.append("                      B.PAYMENT_AMT AS PAYMENT_AMT1 ");// --還款金額
		queryString.append("                    FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                      INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID");
		queryString.append("                      INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                      INNER JOIN TBEXP_MIDDLE_TYPE MID on MID.TBEXP_KIND_TYPE_ID = KIND.ID");
		queryString.append("                      INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                    WHERE    B.PAY_AMT > 0 ");
		queryString.append("                      AND MID.CODE in ('510','610') ");// 2.主任/組長
		queryString.append("                      AND STATE.CODE = '90') B1 ");// --申請單狀態為日結
		queryString.append("              WHERE B1.RN = 1) B1 ON RAT.ID = B1.TBEXP_RATIFY_ID");
		queryString.append("  LEFT  JOIN (SELECT ");// --第二次核銷
		queryString.append("                B2.* ");
		queryString.append("              FROM (SELECT ");// --取得借支金額
		queryString.append("                      DISTINCT RANK() OVER(PARTITION BY B.TBEXP_RATIFY_ID ORDER BY B.EXP_APPL_NO DESC) AS RN,");
		queryString.append("                      B.TBEXP_RATIFY_ID,");
		queryString.append("                      SUB.SUBPOENA_DATE AS VERIFY_DATE2, ");// --傳票日期
		queryString.append("                      B.PAY_AMT AS VERIFY_AMT2, ");// --實支金額
		queryString.append("                        B.ADVPAY_AMT AS ADVPAY_AMT2, ");// --借支金額
		queryString.append("                      B.PAYMENT_AMT AS PAYMENT_AMT2 ");// --還款金額
		queryString.append("                    FROM TBEXP_EXPAPPL_B B ");
		queryString.append("                      INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID");
		queryString.append("                      INNER JOIN TBEXP_KIND_TYPE KIND ON B.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("                      INNER JOIN TBEXP_MIDDLE_TYPE MID on MID.TBEXP_KIND_TYPE_ID = KIND.ID");
		queryString.append("                      INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID ");
		queryString.append("                    WHERE    B.PAY_AMT > 0 ");
		queryString.append("                      AND MID.CODE in ('510','610') ");// 2.主任/組長
		queryString.append("                      AND STATE.CODE = '90') B2 ");// --申請單狀態為日結
		queryString.append("              WHERE B2.RN = 2) B2 ON RAT.ID = B2.TBEXP_RATIFY_ID");
		queryString.append("  INNER JOIN TBEXP_USER USR ON RAT.TBEXP_USER_ID=USR.ID");
		queryString.append(" WHERE RAT.MM_FINAL_DATE IS NULL ");
		queryString.append("   ORDER BY USR.CODE");
		// PeterYu RE201200382_20120808 Update end
		if (logger.isDebugEnabled()) {
			logger.debug(" findNotCancelTotal1() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal findNotCancelTotal2(Calendar startDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT "); // --未核銷總額 = 核定金額 - 已核銷金額
		queryString.append("        SUM(RATIFY_AMT - DECODE(B.DAMT,NULL,0,B.DAMT)) ");
		queryString.append("   FROM TBEXP_RATIFY RAT  ");
		queryString.append("  INNER JOIN (SELECT  ");
		queryString.append("                     MID.ID,  ");
		queryString.append("                     KIND.CODE AS KINDCODE  ");
		queryString.append("                FROM TBEXP_MIDDLE_TYPE MID  ");
		queryString.append("               INNER JOIN TBEXP_KIND_TYPE KIND ON MID.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("               WHERE MID.CODE IN ");
		queryString.append("                     ('310','320','330','3A0','3B0','210','220','230','240','250','260','2A0','2B0','2C0','2D0','G10','710') ");
		queryString.append("             ) MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID ");
		queryString.append("   LEFT JOIN (SELECT  "); // --已核銷金額
		queryString.append("                     B.TBEXP_RATIFY_ID, ");
		queryString.append("                     SUM(E.AMT) AS DAMT  "); // --借方金額
		queryString.append("                FROM TBEXP_ENTRY E ");
		queryString.append("               INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("               INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("                  INNER JOIN TBEXP_EXPAPPL_B B ON E.TBEXP_ENTRY_GROUP_ID = B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("               WHERE ET.ENTRY_VALUE = 'D' ");
		queryString.append("               GROUP BY B.TBEXP_RATIFY_ID) B ON RAT.ID = B.TBEXP_RATIFY_ID ");
		queryString.append("  WHERE RAT.WK_YYMM ");
		queryString.append("        BETWEEN ( SELECT DISTINCT RAT.WK_UYYMM ");
		queryString.append("                    FROM TBEXP_RATIFY RAT  ");
		queryString.append("                   WHERE RAT.WK_YYMM = ");
		if (startDate != null) { // --業績年月起日
			queryString.append(DateUtils.getYeayMonth(startDate));
		}
		queryString.append("                )   AND ");
		if (endDate != null) { // --業績年月迄日
			queryString.append(DateUtils.getYeayMonth(endDate));
		}
		if (logger.isDebugEnabled()) {
			logger.debug(" findNotCancelTotal2() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal findNotCancelTotal11(Calendar startDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT "); // --未核銷總額 = 核定金額 - 已核銷金額
		queryString.append("        nvl(SUM(RATIFY_AMT - DECODE(B.DAMT,NULL,0,B.DAMT)),0) ");
		queryString.append("   FROM TBEXP_RATIFY RAT  ");
		queryString.append("   INNER JOIN (SELECT  ");
		queryString.append("                     MID.ID,  ");
		queryString.append("                     KIND.CODE AS KINDCODE  ");
		queryString.append("               FROM TBEXP_MIDDLE_TYPE MID  ");
		queryString.append("               INNER JOIN TBEXP_KIND_TYPE KIND ON MID.TBEXP_KIND_TYPE_ID = KIND.ID ");
		queryString.append("               WHERE MID.CODE IN ");
		queryString.append("                     ('810') ");
		queryString.append("             ) MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID ");
		queryString.append("   LEFT JOIN (SELECT  "); // --已核銷金額
		queryString.append("                     B.TBEXP_RATIFY_ID, ");
		queryString.append("                     SUM(E.AMT) AS DAMT  "); // --借方金額
		queryString.append("                FROM TBEXP_ENTRY E ");
		queryString.append("               INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("               INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("                  INNER JOIN TBEXP_EXPAPPL_B B ON E.TBEXP_ENTRY_GROUP_ID = B.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("               WHERE ET.ENTRY_VALUE = 'D' ");
		queryString.append("               GROUP BY B.TBEXP_RATIFY_ID) B ON RAT.ID = B.TBEXP_RATIFY_ID ");
		queryString.append("  WHERE RAT.WK_YYMM ");
		queryString.append("        BETWEEN ( SELECT DISTINCT RAT.WK_UYYMM ");
		queryString.append("                    FROM TBEXP_RATIFY RAT  ");
		queryString.append("                   WHERE RAT.WK_YYMM = ");
		if (startDate != null) { // --業績年月起日
			queryString.append(DateUtils.getYeayMonth(startDate));
		}
		queryString.append("               and RAT.WK_UYYMM is not null )   AND ");
		if (endDate != null) { // --業績年月迄日
			queryString.append(DateUtils.getYeayMonth(endDate));
		}
		if (logger.isDebugEnabled()) {
			logger.debug(" findNotCancelTotal2() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public List<OutMsgRewardDto> findOutMsgReward(Calendar startDate, Calendar endDate, String papersNo) {
		List<OutMsgRewardDto> resultList = new ArrayList<OutMsgRewardDto>();
		StringBuffer queryString = new StringBuffer();
		// 發文獎勵費查詢：未核銷明細檔
		queryString.append(" SELECT  "); // --未核銷明細檔
		queryString.append(" OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append(" OMRQ.UNIT_ABB,   "); // --成本單位名稱
		queryString.append(" OMRQ.PAPERS_NO,  "); // --文號
		queryString.append(" OMRQ.APPROVE_AMOUNT_TOTAL - (DECODE(E.DAMT,NULL,0,E.DAMT) - DECODE(E.CAMT,NULL,0,E.CAMT)),  "); // --未核銷總額
		queryString.append(" SUM(DECODE(E.DAMT,NULL,0,E.DAMT) - DECODE(E.CAMT,NULL,0,E.CAMT))   "); // --已核銷總額
		queryString.append(" FROM (SELECT DISTINCT  "); // --已核銷明細檔
		queryString.append(" OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append(" OMRQ.UNIT_ABB,  "); // --成本單位名稱
		queryString.append(" OMRQ.PAPERS_NO,  "); // --文號
		queryString.append(" SUM(OMRQ.APPROVE_AMOUNT_TOTAL) AS APPROVE_AMOUNT_TOTAL   "); // --核定金額合計
		queryString.append(" FROM TBEXP_OUT_MSG_REWARD_QUOTA OMRQ ");
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 START
		// queryString.append(" WHERE OMRQ.CREATE_DATE  ");
		// queryString.append("                 BETWEEN TO_DATE('");
		// if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD')");
		// queryString.append(" AND ");
		// queryString.append(" TO_DATE('");
		// if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD') "); // --查詢起迄日
		queryString.append(" WHERE TO_CHAR(OMRQ.CREATE_DATE,'YYYYMMDD')  ");
		queryString.append("                 BETWEEN '");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("' AND '");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("' "); // --查詢起迄日
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 END

		if (StringUtils.isNotBlank(papersNo)) { // 頁面輸入的文號
			queryString.append("        AND OMRQ.PAPERS_NO = '").append(papersNo).append("' ");
		}
		queryString.append(" GROUP BY OMRQ.UNIT_CODE, OMRQ.UNIT_ABB, OMRQ.PAPERS_NO ");
		queryString.append(" ) OMRQ  ");
		queryString.append(" LEFT OUTER JOIN ( ");
		queryString.append(" SELECT  ");
		queryString.append(" COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append(" COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append(" PAPERS_NO,  "); // --文號
		queryString.append(" SUM(DAMT) AS DAMT,  "); // --借方金額
		queryString.append(" SUM(CAMT) AS CAMT  "); // --貸方金額
		queryString.append(" FROM ");
		queryString.append(" ( ");
		queryString.append(" SELECT  ");
		queryString.append(" PAPERS_NO.PAPERS_NO AS PAPERS_NO,  "); // --文號
		queryString.append(" APPL_INFO.DEP_UNIT_CODE3 AS COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append(" APPL_INFO.DEP_UNIT_NAME3 AS COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append(" EN_TYPE.ENTRY_VALUE,  ");
		queryString.append(" COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'D'  ");
		queryString.append("           THEN SUM(EN.AMT) ");
		queryString.append("           END), 0) ");
		queryString.append("           AS DAMT,   "); // --借方總額
		queryString.append(" COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'C' ");
		queryString.append("           THEN SUM(EN.AMT) ");
		queryString.append("           END), 0) AS CAMT    "); // --貸方且「分錄.會計科目分類」不等於”1
		// 資產”、”2 負債”、”3
		// 收入”的「分錄.金額」
		queryString.append(" FROM TBEXP_EXPAPPL_C C ");
		// start RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append(" LEFT JOIN TBEXP_GENERAL_EXP genexp on genexp.TBEXP_EXPAPPL_C_ID = C.id  ");
		queryString.append(" LEFT JOIN TBEXP_SAL_DEP_OFFICE_EXP SAL_EXP ON SAL_EXP.TBEXP_EXPAPPL_C_ID = C.ID ");
		// end RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append(" INNER JOIN TBEXP_APPL_INFO APPL_INFO ON APPL_INFO.ID = C.TBEXP_APPL_INFO_ID ");
		queryString.append(" INNER JOIN TBEXP_ENTRY_GROUP EG ON EG.ID = C.TBEXP_ENTRY_GROUP_ID ");
		queryString.append(" INNER JOIN TBEXP_ENTRY EN ON EN.TBEXP_ENTRY_GROUP_ID = EG.ID ");
		queryString.append(" INNER JOIN TBEXP_ENTRY_TYPE EN_TYPE ON EN_TYPE.ID = EN.TBEXP_ENTRY_TYPE_ID ");
		queryString.append(" INNER JOIN TBEXP_PAPERS_NO PAPERS_NO ON (PAPERS_NO.ID = SAL_EXP.TBEXP_PAPERS_NO_ID OR PAPERS_NO.id=genexp.TBEXP_PAPERS_NO_id) ");// RE201302778
																																								// 把一般費用的發文獎勵篩選出來
		queryString.append(" INNER JOIN TBEXP_ACC_TITLE ACC_TITLE ON ACC_TITLE.ID = EN.TBEXP_ACC_TITLE_ID ");
		queryString.append(" INNER JOIN TBEXP_ACC_CLASS_TYPE ACC_CLASS_TYPE ON ACC_CLASS_TYPE.ID = ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID ");
		queryString.append(" INNER JOIN TBEXP_APPL_STATE APPL_STATE ON APPL_STATE.ID = C.TBEXP_APPL_STATE_ID ");
		queryString.append(" WHERE APPL_STATE.CODE IN ('70', '80', '90')  "); // --狀態為已內結、已送匯、已日結
		queryString.append(" AND ACC_CLASS_TYPE.CODE NOT IN ('1', '2', '3')  "); // --「分錄.會計科目分類」不等於”1
		// 資產”、”2
		// 負債”、”3
		// 收入”
		queryString.append(" GROUP BY  ");
		queryString.append(" PAPERS_NO.PAPERS_NO, ");
		queryString.append(" APPL_INFO.DEP_UNIT_CODE3, ");
		queryString.append(" APPL_INFO.DEP_UNIT_NAME3, ");
		queryString.append(" EN_TYPE.ENTRY_VALUE ");
		queryString.append(" )T1 ");
		queryString.append(" GROUP BY ");
		queryString.append(" T1.PAPERS_NO,  ");
		queryString.append(" T1.COST_UNIT_CODE, ");
		queryString.append(" T1.COST_UNIT_NAME ");
		queryString.append(" ) E ");
		queryString.append(" ON OMRQ.PAPERS_NO = E.PAPERS_NO AND OMRQ.UNIT_CODE = E.COST_UNIT_CODE ");
		queryString.append(" GROUP BY ");
		queryString.append(" OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append(" OMRQ.UNIT_ABB,   "); // --成本單位名稱
		queryString.append(" OMRQ.PAPERS_NO,  "); // --文號
		queryString.append(" OMRQ.APPROVE_AMOUNT_TOTAL - (DECODE(E.DAMT,NULL,0,E.DAMT) - DECODE(E.CAMT,NULL,0,E.CAMT))  "); // --未核銷總額
		queryString.append("   ORDER BY OMRQ.UNIT_CODE ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findOutMsgReward() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		if (!list.isEmpty()) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				String costUnitCode = (String) record[0];
				String costUnitName = (String) record[1];
				String paperNo = (String) record[2];
				BigDecimal notCancelAmt = (BigDecimal) record[3];
				if (notCancelAmt == null) {
					notCancelAmt = BigDecimal.ZERO;
				}
				BigDecimal cancelAmt = (BigDecimal) record[4];
				if (cancelAmt == null) {
					cancelAmt = BigDecimal.ZERO;
				}
				OutMsgRewardDto dto = new OutMsgRewardDto();
				dto.setCostUnitCode(costUnitCode);
				dto.setCostUnitName(costUnitName);
				dto.setPaperNo(paperNo);
				dto.setNotCancelAmt(notCancelAmt);
				dto.setCancelAmt(cancelAmt);
				resultList.add(dto);
			}
		}
		return resultList;
	}

	@SuppressWarnings("unchecked")
	public List findCancelDetailForExport(Calendar startDate, Calendar endDate, String papersNo) {
		List list = new ArrayList();
		StringBuffer queryString = new StringBuffer();
		// 發文獎勵費查詢：已核銷明細檔
		queryString.append("SELECT  "); // --已核銷明細檔
		queryString.append("E.COST_UNIT_CODE,  "); // --成本單位代號
		queryString.append("E.COST_UNIT_NAME,  "); // --成本單位名稱
		queryString.append("E.PAPERS_NO,  "); // --文號
		queryString.append("E.SUBPOENA_DATE,  "); // --作帳日傳票日期
		queryString.append("E.SUBPOENA_NO,  "); // --傳票號碼傳票號碼
		queryString.append("E.ACCTCODE,  "); // --會計科目代號分錄
		queryString.append("E.DAMT - E.CAMT,  "); // --核銷金額
		queryString.append("OMRQ.CREATE_DATE  "); // --核定檔的轉入日期
		queryString.append("FROM (SELECT DISTINCT  "); // --已核銷明細檔
		queryString.append("OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append("OMRQ.UNIT_ABB,  "); // --成本單位名稱
		queryString.append("OMRQ.PAPERS_NO,  "); // --文號
		queryString.append("OMRQ.CREATE_DATE ");
		queryString.append("FROM TBEXP_OUT_MSG_REWARD_QUOTA OMRQ ");
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 START
		// queryString.append("WHERE OMRQ.CREATE_DATE  ");
		// queryString.append("                BETWEEN TO_DATE('"); // --查詢迄日
		// if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD')");
		// queryString.append(" AND ");
		// queryString.append(" TO_DATE('");
		// if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD') "); // --查詢起迄日
		// if (StringUtils.isNotBlank(papersNo)) { // 頁面輸入的文號
		// queryString.append("        AND OMRQ.PAPERS_NO = '").append(papersNo).append("' ");
		// }
		queryString.append("WHERE TO_CHAR(OMRQ.CREATE_DATE,'YYYYMMDD')  ");
		queryString.append("                BETWEEN '"); // --查詢迄日
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("' AND '");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("' "); // --查詢起迄日
		if (StringUtils.isNotBlank(papersNo)) { // 頁面輸入的文號
			queryString.append("        AND OMRQ.PAPERS_NO = '").append(papersNo).append("' ");
		}
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 END
		queryString.append(") OMRQ  ");
		queryString.append("INNER JOIN ( ");
		queryString.append("SELECT  ");
		queryString.append("COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append("COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append("PAPERS_NO,  "); // --文號
		queryString.append("SUBPOENA_NO,  ");
		queryString.append("SUBPOENA_DATE, ");
		queryString.append("ACCTCODE,  "); // --會計科目代號分錄
		queryString.append("SUM(DAMT) AS DAMT,  "); // --借方金額
		queryString.append("SUM(CAMT) AS CAMT  "); // --貸方金額
		queryString.append("FROM ");
		queryString.append("( ");
		queryString.append("SELECT  ");
		queryString.append("SUBPOENA.SUBPOENA_NO AS SUBPOENA_NO, ");
		queryString.append("SUBPOENA.SUBPOENA_DATE AS SUBPOENA_DATE, ");
		queryString.append("PAPERS_NO.PAPERS_NO AS PAPERS_NO,  "); // --文號
		queryString.append("APPL_INFO.DEP_UNIT_CODE3 AS COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append("APPL_INFO.DEP_UNIT_NAME3 AS COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append("ACC_TITLE.CODE AS ACCTCODE, ");
		queryString.append("EN_TYPE.ENTRY_VALUE,  ");
		queryString.append("COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'D'  ");
		queryString.append("          THEN SUM(EN.AMT) ");
		queryString.append("          END), 0) ");
		queryString.append("          AS DAMT,   "); // --借方總額
		queryString.append("COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'C' ");
		queryString.append("          THEN SUM(EN.AMT) ");
		queryString.append("          END), 0) AS CAMT    "); // --貸方且「分錄.會計科目分類」不等於”1
		// 資產”、”2 負債”、”3
		// 收入”的「分錄.金額」
		queryString.append("FROM TBEXP_EXPAPPL_C C ");
		// start RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append(" LEFT JOIN TBEXP_GENERAL_EXP genexp on genexp.TBEXP_EXPAPPL_C_ID = C.id  ");
		queryString.append(" LEFT JOIN TBEXP_SAL_DEP_OFFICE_EXP SAL_EXP ON SAL_EXP.TBEXP_EXPAPPL_C_ID = C.ID ");
		// end RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append("INNER JOIN TBEXP_APPL_INFO APPL_INFO ON APPL_INFO.ID = C.TBEXP_APPL_INFO_ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY_GROUP EG ON EG.ID = C.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY EN ON EN.TBEXP_ENTRY_GROUP_ID = EG.ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY_TYPE EN_TYPE ON EN_TYPE.ID = EN.TBEXP_ENTRY_TYPE_ID ");
		queryString.append(" INNER JOIN TBEXP_PAPERS_NO PAPERS_NO ON (PAPERS_NO.ID = SAL_EXP.TBEXP_PAPERS_NO_ID OR PAPERS_NO.id=genexp.TBEXP_PAPERS_NO_id) "); // RE201302778
																																								// 把一般費用的發文獎勵篩選出來
		queryString.append("INNER JOIN TBEXP_ACC_TITLE ACC_TITLE ON ACC_TITLE.ID = EN.TBEXP_ACC_TITLE_ID ");
		queryString.append("INNER JOIN TBEXP_ACC_CLASS_TYPE ACC_CLASS_TYPE ON ACC_CLASS_TYPE.ID = ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID ");
		queryString.append("INNER JOIN TBEXP_APPL_STATE APPL_STATE ON APPL_STATE.ID = C.TBEXP_APPL_STATE_ID ");
		queryString.append("INNER JOIN TBEXP_SUBPOENA SUBPOENA ON SUBPOENA.ID = C.TBEXP_SUBPOENA_ID ");
		queryString.append("WHERE APPL_STATE.CODE IN ('70', '80', '90')  "); // --狀態為已內結、已送匯、已日結
		queryString.append("AND ACC_CLASS_TYPE.CODE NOT IN ('1', '2', '3')  "); // --「分錄.會計科目分類」不等於”1
		// 資產”、”2
		// 負債”、”3
		// 收入”
		queryString.append("GROUP BY  ");
		queryString.append("SUBPOENA.SUBPOENA_NO, ");
		queryString.append("SUBPOENA.SUBPOENA_DATE, ");
		queryString.append("PAPERS_NO.PAPERS_NO, ");
		queryString.append("APPL_INFO.DEP_UNIT_CODE3, ");
		queryString.append("APPL_INFO.DEP_UNIT_NAME3, ");
		queryString.append("EN_TYPE.ENTRY_VALUE,  ");
		queryString.append("ACC_TITLE.CODE ");
		queryString.append(")T1 ");
		queryString.append("GROUP BY ");
		queryString.append("T1.SUBPOENA_NO, ");
		queryString.append("T1.SUBPOENA_DATE, ");
		queryString.append("T1.PAPERS_NO,  ");
		queryString.append("T1.COST_UNIT_CODE, ");
		queryString.append("T1.COST_UNIT_NAME, ");
		queryString.append("T1.ACCTCODE ");
		queryString.append(") E ");
		queryString.append("ON OMRQ.PAPERS_NO = E.PAPERS_NO AND OMRQ.UNIT_CODE = E.COST_UNIT_CODE ");
		queryString.append("   ORDER BY E.COST_UNIT_CODE ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findCancelDetailForExport() SQL = " + queryString.toString());
		}
		list = findByNativeSQL(queryString.toString(), null);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List findNotCancelDetailForExport(Calendar startDate, Calendar endDate, String papersNo) {
		List list = new ArrayList();
		StringBuffer queryString = new StringBuffer();
		// 發文獎勵費查詢：未核銷明細檔
		queryString.append("SELECT  "); // --未核銷明細檔
		queryString.append("OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append("OMRQ.UNIT_ABB,   "); // --成本單位名稱
		queryString.append("OMRQ.PAPERS_NO,  "); // --文號
		queryString.append("OMRQ.APPROVE_AMOUNT_TOTAL - (DECODE(E.DAMT,NULL,0,E.DAMT) - DECODE(E.CAMT,NULL,0,E.CAMT))  "); // --未核銷總額
		queryString.append("FROM (SELECT DISTINCT  "); // --已核銷明細檔
		queryString.append("OMRQ.UNIT_CODE,  "); // --成本單位代號
		queryString.append("OMRQ.UNIT_ABB,  "); // --成本單位名稱
		queryString.append("OMRQ.PAPERS_NO,  "); // --文號
		queryString.append("SUM(OMRQ.APPROVE_AMOUNT_TOTAL) AS APPROVE_AMOUNT_TOTAL   "); // --核定金額合計
		queryString.append("FROM TBEXP_OUT_MSG_REWARD_QUOTA OMRQ ");
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 START
		// queryString.append(" WHERE OMRQ.CREATE_DATE  ");
		// queryString.append("                BETWEEN TO_DATE('");
		// if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD')");
		// queryString.append(" AND ");
		// queryString.append(" TO_DATE('");
		// if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
		// queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		// }
		// queryString.append("', 'YYYYMMDD') "); // --查詢起迄日
		queryString.append(" WHERE TO_CHAR(OMRQ.CREATE_DATE,'YYYYMMDD')  ");
		queryString.append("                BETWEEN '");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("' AND '");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("' "); // --查詢起迄日
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 END
		if (StringUtils.isNotBlank(papersNo)) { // 頁面輸入的文號
			queryString.append("        AND OMRQ.PAPERS_NO = '").append(papersNo).append("' ");
		}
		queryString.append("GROUP BY OMRQ.UNIT_CODE, OMRQ.UNIT_ABB, OMRQ.PAPERS_NO ");
		queryString.append(") OMRQ  ");
		queryString.append("LEFT OUTER JOIN ( ");
		queryString.append("SELECT  ");
		queryString.append("COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append("COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append("PAPERS_NO,  "); // --文號
		queryString.append("SUM(DAMT) AS DAMT,  "); // --借方金額
		queryString.append("SUM(CAMT) AS CAMT  "); // --貸方金額
		queryString.append("FROM ");
		queryString.append("( ");
		queryString.append("SELECT  ");
		queryString.append("PAPERS_NO.PAPERS_NO AS PAPERS_NO,  "); // --文號
		queryString.append("APPL_INFO.DEP_UNIT_CODE3 AS COST_UNIT_CODE,  "); // --申請人所屬單位代號
		queryString.append("APPL_INFO.DEP_UNIT_NAME3 AS COST_UNIT_NAME,  "); // --申請人所屬單位名稱
		queryString.append("EN_TYPE.ENTRY_VALUE,  ");
		queryString.append("COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'D'  ");
		queryString.append("          THEN SUM(EN.AMT) ");
		queryString.append("          END), 0) ");
		queryString.append("          AS DAMT,   "); // --借方總額
		queryString.append("COALESCE((CASE WHEN EN_TYPE.ENTRY_VALUE = 'C' ");
		queryString.append("          THEN SUM(EN.AMT) ");
		queryString.append("          END), 0) AS CAMT    "); // --貸方且「分錄.會計科目分類」不等於”1
																// 資產”、”2 負債”、”3
																// 收入”的「分錄.金額」
		queryString.append("FROM TBEXP_EXPAPPL_C C ");
		// start RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append(" LEFT JOIN TBEXP_GENERAL_EXP genexp on genexp.TBEXP_EXPAPPL_C_ID = C.id  ");
		queryString.append(" LEFT JOIN TBEXP_SAL_DEP_OFFICE_EXP SAL_EXP ON SAL_EXP.TBEXP_EXPAPPL_C_ID = C.ID ");
		// end RE201302778 把一般費用的發文獎勵篩選出來
		queryString.append("INNER JOIN TBEXP_APPL_INFO APPL_INFO ON APPL_INFO.ID = C.TBEXP_APPL_INFO_ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY_GROUP EG ON EG.ID = C.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY EN ON EN.TBEXP_ENTRY_GROUP_ID = EG.ID ");
		queryString.append("INNER JOIN TBEXP_ENTRY_TYPE EN_TYPE ON EN_TYPE.ID = EN.TBEXP_ENTRY_TYPE_ID ");
		queryString.append(" INNER JOIN TBEXP_PAPERS_NO PAPERS_NO ON (PAPERS_NO.ID = SAL_EXP.TBEXP_PAPERS_NO_ID OR PAPERS_NO.id=genexp.TBEXP_PAPERS_NO_id) ");// RE201302778
																																								// 把一般費用的發文獎勵篩選出來
		queryString.append("INNER JOIN TBEXP_ACC_TITLE ACC_TITLE ON ACC_TITLE.ID = EN.TBEXP_ACC_TITLE_ID ");
		queryString.append("INNER JOIN TBEXP_ACC_CLASS_TYPE ACC_CLASS_TYPE ON ACC_CLASS_TYPE.ID = ACC_TITLE.TBEXP_ACC_CLASS_TYPE_ID ");
		queryString.append("INNER JOIN TBEXP_APPL_STATE APPL_STATE ON APPL_STATE.ID = C.TBEXP_APPL_STATE_ID ");
		queryString.append("WHERE APPL_STATE.CODE IN ('70', '80', '90')  "); // --狀態為已內結、已送匯、已日結
		queryString.append("AND ACC_CLASS_TYPE.CODE NOT IN ('1', '2', '3')  "); // --「分錄.會計科目分類」不等於”1
																				// 資產”、”2
																				// 負債”、”3
																				// 收入”
		queryString.append("GROUP BY  ");
		queryString.append("PAPERS_NO.PAPERS_NO, ");
		queryString.append("APPL_INFO.DEP_UNIT_CODE3, ");
		queryString.append("APPL_INFO.DEP_UNIT_NAME3, ");
		queryString.append("EN_TYPE.ENTRY_VALUE ");
		queryString.append(")T1 ");
		queryString.append("GROUP BY ");
		queryString.append("T1.PAPERS_NO,  ");
		queryString.append("T1.COST_UNIT_CODE, ");
		queryString.append("T1.COST_UNIT_NAME ");
		queryString.append(") E ");
		queryString.append("ON OMRQ.PAPERS_NO = E.PAPERS_NO AND OMRQ.UNIT_CODE = E.COST_UNIT_CODE ");
		queryString.append("   ORDER BY OMRQ.UNIT_CODE ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findNotCancelDetailForExport() SQL = " + queryString.toString());
		}
		list = findByNativeSQL(queryString.toString(), null);
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<OfficeExpReportedThatSupportDto> findCancelDetail7(Calendar startDate, Calendar endDate, String depCode, String localCode, String unitCode, String acctCode) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT ");
		queryString.append("        MAIN.SUBPOENA_DATE,  "); // --作帳日
		queryString.append("        E.DEP_UNIT_CODE1,  "); // --部門代號
		queryString.append("        E.DEP_UNIT_CODE2,  "); // --區部代號
		queryString.append("        E.COST_UNIT_CODE,  "); // --單位代號
		queryString.append("        ACCT.CODE,  "); // --會計科目代號
		queryString.append("           SUM(DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) - DECODE(ET.ENTRY_VALUE,'C',E.AMT,0)) AS AMT  "); // --借方-貸方後之餘額
		queryString.append("   FROM TBEXP_ENTRY E ");
		queryString.append("  INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
		queryString.append("  INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
		queryString.append("  INNER JOIN TBEXP_EXP_MAIN MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
		//defect4540_辦公費實報實支查詢問題 CU3178 START
		//queryString.append("  WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) = 'N10'  "); // --費用中分類為N10
		queryString.append("  WHERE MAIN.EXP_APPL_NO LIKE  'N10%'  "); // --費用中分類為N10
		//defect4540_辦公費實報實支查詢問題 CU3178 END
		queryString.append("    AND SUBSTR(ACCT.CODE,1,1) = '6'  "); // --費用會計科目
		// --查詢篩選條件--
		if (StringUtils.isNotBlank(depCode)) {
			queryString.append(" AND E.DEP_UNIT_CODE1 = '");
			queryString.append(depCode).append("' "); // --部門代號
		}
		if (StringUtils.isNotBlank(localCode)) {
			queryString.append(" AND E.DEP_UNIT_CODE2 = '");
			queryString.append(localCode).append("' "); // --區部代號
		}
		if (StringUtils.isNotBlank(unitCode)) {
			queryString.append(" AND E.COST_UNIT_CODE = '");
			queryString.append(unitCode).append("' "); // --單位代號
		}
		if (StringUtils.isNotBlank(acctCode)) {
			queryString.append(" AND ACCT.CODE = '");
			queryString.append(acctCode).append("' "); // --會計科目代號
		}
		// --則限制輸入費用會計科目
		queryString.append("    AND MAIN.SUBPOENA_DATE ");
		queryString.append("             BETWEEN TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD')");
		queryString.append(" AND ");
		queryString.append(" TO_DATE('");
		if (endDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("', 'YYYYMMDD') "); // --查詢起迄日
		queryString.append("     GROUP BY MAIN.SUBPOENA_DATE, E.DEP_UNIT_CODE1, E.DEP_UNIT_CODE2, E.COST_UNIT_CODE, ACCT.CODE ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findNotCancelDetailForExport() SQL = " + queryString.toString());
		}
		List list = findByNativeSQL(queryString.toString(), null);
		List<OfficeExpReportedThatSupportDto> resultList = new ArrayList<OfficeExpReportedThatSupportDto>();
		if (!list.isEmpty()) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				Timestamp ts = (Timestamp) record[0];
				Calendar subpoenaDate = Calendar.getInstance();
				if (ts != null) {
					subpoenaDate.setTimeInMillis(ts.getTime());
				}
				String rDepCode = (String) record[1];
				String rLocalCode = (String) record[2];
				String rUnitCode = (String) record[3];
				String rAcctCode = (String) record[4];
				BigDecimal amt = (BigDecimal) record[5];
				if (amt == null) {
					amt = BigDecimal.ZERO;
				}
				OfficeExpReportedThatSupportDto dto = new OfficeExpReportedThatSupportDto();
				dto.setSubpoenaDate(subpoenaDate);
				dto.setDepCode(rDepCode);
				dto.setLocalCode(rLocalCode);
				dto.setUnitCode(rUnitCode);
				dto.setAcctCode(rAcctCode);
				dto.setAmt(amt);
				resultList.add(dto);
			}
		}
		return resultList;
	}

	private String getSQLByTaxType(Calendar startDate, Calendar endDate, String taxType, String acctCode) {
		StringBuffer queryString = new StringBuffer();
		if (IncomeOutcomeTaxDto.INCOME.equalsIgnoreCase(taxType)) {
			queryString.append(" SELECT "); // --進項稅額查詢
			queryString.append("        E.SUBPOENA_DATE, ");
			queryString.append("        E.ACCTNAME, ");
			queryString.append("        E.SUBPOENA_NO, ");
			queryString.append("        E.INVOICE_NO, ");
			queryString.append("        E.COMP_ID, ");
			queryString.append("        E.DAMT, ");
			queryString.append("        E.CAMT, ");
			queryString.append("        E.ENTRY_VALUE, ");
			queryString.append("        E.SUMMARY, ");
			queryString.append("        E.ID ");
			queryString.append("   FROM (SELECT "); // --費用系統
			queryString.append("                E.ID, ");
			queryString.append("                MAIN.SUBPOENA_DATE AS SUBPOENA_DATE, "); // --作帳日
			// 分錄
			queryString.append("                ACCT.CODE || ACCT.NAME AS ACCTNAME, "); // --會計科目
			// 分錄
			queryString.append("                MAIN.SUBPOENA_NO,  "); // --傳票號碼
			// 分錄
			queryString.append("                VAT.INVOICE_NO,  "); // --發票號碼
			// 進項稅額明細
			queryString.append("                VAT.COMP_ID,  "); // --統一編號
			// 進項稅額明細
			queryString.append("                DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
			queryString.append("                DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  "); // --貸方金額
			queryString.append("                DECODE(ET.ENTRY_VALUE,'D','3','C','4') AS ENTRY_VALUE,  "); // --借貸別
			// 分錄
			queryString.append("                E.SUMMARY  "); // --摘要 分錄
			queryString.append("           FROM TBEXP_ENTRY E ");
			queryString.append("          INNER JOIN TBEXP_VAT_DETAIL VAT ON E.ID = VAT.TBEXP_ENTRY_VAT_ID ");
			queryString.append("             INNER JOIN TBEXP_EXP_MAIN MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
			queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
			queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
			queryString.append("          UNION ");
			queryString.append("          SELECT ");
			queryString.append("                 EE.ID, ");
			queryString.append("                 EE.SUBPOENA_DATE AS SUBPOENA_DATE,  "); // --作帳日
			// 分錄
			queryString.append("                 ACCT.CODE || ACCT.NAME AS ACCTNAME,  "); // --會計科目
			// 分錄
			queryString.append("                 EE.SUBPOENA_NO,  "); // --傳票號碼
			// 分錄
			queryString.append("                 NULL AS INVOICE_NO,  "); // --發票號碼
			queryString.append("                 EE.COMP_ID,  "); // --統一編號
			// 進項稅額明細
			queryString.append("                 DECODE(ET.ENTRY_VALUE,'D',EE.AMT,0) AS DAMT,  "); // --借方金額
			queryString.append("                 DECODE(ET.ENTRY_VALUE,'C',EE.AMT,0) AS CAMT,  "); // --貸方金額
			queryString.append("                 DECODE(ET.ENTRY_VALUE,'D','3','C','4') AS ENTRY_VALUE,  "); // --借貸別
			// 分錄
			queryString.append("                 EE.SUMMARY  "); // --摘要
			queryString.append("            FROM TBEXP_EXT_SYS_ENTRY EE ");
			queryString.append("            INNER JOIN TBEXP_ACC_TITLE ACCT ON EE.ACCT_CODE = ACCT.CODE ");
			queryString.append("            INNER JOIN TBEXP_ENTRY_TYPE ET ON EE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
			queryString.append("            WHERE SUBSTR(ACCT.CODE,1,4) = '").append(IncomeOutcomeTaxDto.INCOME).append("' "); // --進項稅
			queryString.append("        ) E ");
			queryString.append("  WHERE E.SUBPOENA_DATE ");
			queryString.append("                BETWEEN TO_DATE('");
			if (startDate != null) { // 這裡的作帳日期不應該為空值，前端應該做完檢核。
				queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
			}
			queryString.append("', 'YYYYMMDD')");
			queryString.append(" AND ");
			queryString.append(" TO_DATE('");
			if (endDate != null) { // 這裡的作帳日期不應該為空值，前端應該做完檢核。
				queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
			}
			queryString.append("', 'YYYYMMDD') "); // --作帳起迄日
			if (StringUtils.isNotBlank(acctCode)) {
				queryString.append(" AND SUBSTR(E.ACCTNAME,1,8) = '");
				queryString.append(acctCode).append("' "); // --會計科目代號--則限制輸入進項稅會計科目
			}
			queryString.append("  ORDER BY E.ACCTNAME, E.SUBPOENA_DATE ");
			return queryString.toString();
		} else {
			// IncomeOutcomeTaxDto.OUTCOME.equalsIgnoreCase(taxType)
			// D11.8 銷項稅的SQL調整 (IISI modified by Sunkist 2010/06/23 代修訂)
			queryString.append(" SELECT  "); // --銷項稅額查詢
			queryString.append("   E.SUBPOENA_DATE, ");
			queryString.append("   E.ACCTNAME, ");
			queryString.append("   E.SUBPOENA_NO, ");
			queryString.append("   E.INVOICE_NO, ");
			queryString.append("   E.COMP_ID, ");
			queryString.append("   E.DAMT, ");
			queryString.append("   E.CAMT, ");
			queryString.append("   E.ENTRY_VALUE, ");
			queryString.append("   E.SUMMARY  ");
			queryString.append(" FROM ( ");
			queryString.append(" SELECT  "); // --費用系統
			queryString.append("         E.ID,  ");
			queryString.append("         MAIN.SUBPOENA_DATE,  "); // --作帳日 分錄
			queryString.append("         ACCT.CODE || ACCT.NAME AS ACCTNAME,  "); // --會計科目
																					// 分錄
			queryString.append("         MAIN.SUBPOENA_NO,  "); // --傳票號碼 分錄
			queryString.append("         NULL AS INVOICE_NO,  "); // --發票號碼
			queryString.append("         NULL AS COMP_ID,  "); // --統一編號
			queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  "); // --借方金額
			queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  "); // --貸方金額
			queryString.append("         DECODE(ET.ENTRY_VALUE,'D','3','C','4') AS ENTRY_VALUE,  "); // --借貸別
																										// 分錄
			queryString.append("         E.SUMMARY  "); // --摘要 分錄
			queryString.append("       FROM TBEXP_ENTRY E  ");
			queryString.append("         INNER JOIN TBEXP_EXP_MAIN MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  ");
			queryString.append("         INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
			queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
			queryString.append("       WHERE SUBSTR(ACCT.CODE,1,4) = '2042'  "); // --銷項稅
			// IISI-20100701 加上會計科目
			if (StringUtils.isNotBlank(acctCode)) {
				queryString.append(" AND ACCT.CODE = '");
				queryString.append(acctCode).append("' "); // --會計科目代號--則限制輸入銷項稅會計科目
			}

			queryString.append(" UNION  ");
			queryString.append(" SELECT  "); // --銷項稅額查詢
			queryString.append("   EE.ID,  ");
			queryString.append("   EE.SUBPOENA_DATE,  "); // --作帳日 分錄
			queryString.append("   ACCT.CODE || ACCT.NAME AS ACCTNAME,  "); // --會計科目
																			// 分錄
			queryString.append("   EE.SUBPOENA_NO,  "); // --傳票號碼 分錄
			queryString.append("   NULL AS INVOICE_NO,  "); // --發票號碼
			queryString.append("   EE.COMP_ID,  "); // --統一編號 進項稅額明細
			queryString.append("   DECODE(ET.ENTRY_VALUE,'D',EE.AMT,0) AS DAMT,  "); // --借方金額
			queryString.append("   DECODE(ET.ENTRY_VALUE,'C',EE.AMT,0) AS CAMT,  "); // --貸方金額
			queryString.append("   DECODE(ET.ENTRY_VALUE,'D','3','C','4') AS ENTRY_VALUE,  "); // --借貸別
																								// 分錄
			queryString.append("   EE.SUMMARY  "); // --摘要
			queryString.append(" FROM TBEXP_EXT_SYS_ENTRY EE  ");
			queryString.append("   INNER JOIN TBEXP_ACC_TITLE ACCT ON EE.ACCT_CODE = ACCT.CODE    ");
			queryString.append("   INNER JOIN TBEXP_ENTRY_TYPE ET ON EE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
			queryString.append("       WHERE SUBSTR(ACCT.CODE,1,4) = '2042'  "); // --銷項稅
			// IISI-20100701 加上會計科目
			if (StringUtils.isNotBlank(acctCode)) {
				queryString.append(" AND ACCT.CODE = '");
				queryString.append(acctCode).append("' "); // --會計科目代號--則限制輸入銷項稅會計科目
			}
			queryString.append(" ) E ");
			queryString.append("    WHERE E.SUBPOENA_DATE ");
			queryString.append("             BETWEEN TO_DATE('");
			if (startDate != null) { // 這裡的作帳日期不應該為空值，前端應該做完檢核。
				queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
			}
			queryString.append("', 'YYYYMMDD')");
			queryString.append(" AND ");
			queryString.append(" TO_DATE('");
			if (endDate != null) { // 這裡的作帳日期不應該為空值，前端應該做完檢核。
				queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
			}
			queryString.append("', 'YYYYMMDD') "); // --作帳起迄日
			// IISI-20100701 將會計科目檢查移到前面
			// if (StringUtils.isNotBlank(acctCode)) {
			// queryString.append(" AND ACCT.CODE = '");
			// queryString.append(acctCode).append("' "); //
			// --會計科目代號--則限制輸入銷項稅會計科目
			// }
			queryString.append(" ORDER BY E.ACCTNAME, E.SUBPOENA_DATE ");
			return queryString.toString();
		}
	}

	@SuppressWarnings("unchecked")
	public List<IncomeOutcomeTaxDto> findIncomeOutcomeTax(Calendar startDate, Calendar endDate, String taxType, String acctCode) {
		String queryString = getSQLByTaxType(startDate, endDate, taxType, acctCode);
		if (logger.isDebugEnabled()) {
			logger.debug(" findIncomeOutcomeTax() SQL = " + queryString);
		}
		List list = findByNativeSQL(queryString, null);
		List<IncomeOutcomeTaxDto> resultList = new ArrayList<IncomeOutcomeTaxDto>();
		if (!list.isEmpty()) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				Timestamp ts = (Timestamp) record[0];
				Calendar subpoenaDate = Calendar.getInstance();
				if (ts != null) {
					subpoenaDate.setTimeInMillis(ts.getTime());
				}
				String acctName = (String) record[1];
				String subpoenaNo = (String) record[2];
				String invoiceNo = (String) record[3];
				String compId = (String) record[4];
				BigDecimal damt = (BigDecimal) record[5];
				if (damt == null) {
					damt = BigDecimal.ZERO;
				}
				BigDecimal camt = (BigDecimal) record[6];
				if (camt == null) {
					camt = BigDecimal.ZERO;
				}
				String entryValue = (String) record[7];
				String summary = (String) record[8];
				IncomeOutcomeTaxDto dto = new IncomeOutcomeTaxDto();
				dto.setSubpoenaDate(subpoenaDate);
				dto.setAcctName(acctName);
				dto.setSubpoenaNo(subpoenaNo);
				dto.setInvoiceNo(invoiceNo);
				dto.setCompId(compId);
				dto.setDamt(damt);
				dto.setCamt(camt);
				dto.setEntryValue(entryValue);
				dto.setSummary(summary);
				resultList.add(dto);
			}
		}
		return resultList;
	}

	@SuppressWarnings("unchecked")
	public List<String> exportCA00BusinessCostStatistics(Calendar beginDate, Calendar endDate, boolean caseW, String yyyymm) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT"); // 業務成本統計檔
		queryString.append("   CA00.WK_YYMM,"); // 業績年月
		queryString.append("   CA00.CUST_NO,"); // 成本代號
		queryString.append("   CA00.DEP_UNIT_CODE1,"); // 部室代號
		queryString.append("   CA00.DEP_UNIT_CODE2,"); // 駐區代號
		queryString.append("   CA00.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("   SUM(CA00.AMT) AS AMT"); // 實支金額，借方為正值，貸方為負值
		queryString.append(" FROM (SELECT"); // CA00
		queryString.append("         MAIN.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("         MAIN.SUBPOENA_NO,"); // 傳票號碼
		queryString.append("         MAIN.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("         MAIN.WK_YYMM, "); // 業績年月
		queryString.append("         ACCT.ACCTCODE,"); // 會計科目
		queryString.append("         ACCT.ACCTNAME,"); // 會計科目中文名稱
		queryString.append("         E.DEP_UNIT_CODE1,"); // 部室級
		queryString.append("         DECODE(SUBSTR(MAIN.EXP_APPL_NO,1,3),'2H0','2S0000',E.DEP_UNIT_CODE2) AS DEP_UNIT_CODE2,"); // 駐區級
		queryString.append("         E.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("         ESUB.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("         ET.ENTRY_VALUE,"); // 借貸別
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,"); // 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,"); // 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS AMT,"); // 實支金額，借方為正值，貸方為負值
		queryString.append("         E.COST_CODE,"); // 成本別
		queryString.append("         MAIN.UCODE,"); // 申請人員工代號
		queryString.append("         ESUB.PAPERS_NO,"); // 文號
		queryString.append("         E.SUMMARY,"); // 摘要
		queryString.append("         CASE WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) IN ('3H0','2H0','3J0') THEN '300020' ELSE TO_CHAR(ACCT.CUST_CODE) END AS CUST_NO,"); // 成本代號
		queryString.append("         CASE WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) IN ('3H0','2H0','3J0') THEN '1'");
		queryString.append("              WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) NOT IN ('3H0','2H0','3J0') THEN");
		queryString.append("                   (CASE WHEN ACCT.DIVCH_FLAG  = 1 AND DEP.DLPCODE = '1' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.BRNCH_FLAG  = 1 AND DEP.DLPCODE = '2' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.DIVCH_FLAG  = 1 AND DEP.DLPCODE = '1' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		queryString.append("                         WHEN ACCT.BRNCH_FLAG  = 1 AND DEP.DLPCODE = '2' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		// IISI-20100531： 同一張申請單會重覆出現在成本統計與偵錯檔, 因為TBEXP_CA_ACC_R
		// 有重覆的會計科目(成本別'Z')
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '1' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '2' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         ELSE '2' END)");
		queryString.append("              END  AS TY"); // TY=1 分進業務成本統計檔TY=2
														// 分進偵錯檔
		queryString.append("       FROM TBEXP_ENTRY E");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("         LEFT  JOIN TBEXP_EXP_SUB ESUB ON E.ID = ESUB.TBEXP_ENTRY_ID");
		queryString.append("         LEFT  JOIN (SELECT"); // 業務成本類別CA00
		queryString.append("                       ACCT.ID,");
		queryString.append("                       ACCT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("                       ACCT.NAME AS ACCTNAME,"); // 會計科目
		queryString.append("                       CA00.DIVCH_FLAG,"); // 部室代號
		queryString.append("                       CA00.BRNCH_FLAG,"); // 駐區代號
		queryString.append("                       CA00.UNITCH_FLAG,"); // 單位代號
		queryString.append("                       CA00.COST_TYPE,"); // 成本別
		queryString.append("                       CAREF.CUST_CODE"); // CA成本代號
		queryString.append("                     FROM TBEXP_CA_ACC_R CA00");
		queryString.append("                       INNER JOIN TBEXP_ACC_TITLE ACCT ON CA00.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("                       INNER JOIN TBEXP_CA_REFERENCE CAREF ON CA00.TBEXP_CA_REFERENCE_ID = CAREF.ID");
		queryString.append("                     WHERE CA00.OP_CUST_TYPE = '0') ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("         INNER JOIN (SELECT");
		queryString.append("                       MAIN.TBEXP_ENTRY_GROUP_ID,");
		queryString.append("                       MAIN.EXP_APPL_NO,");
		queryString.append("                       MAIN.SUBPOENA_NO,");
		queryString.append("                       MAIN.SUBPOENA_DATE,");
		queryString.append("                       MAIN.WK_YYMM,");
		queryString.append("                       INFO.USER_ID AS UCODE");
		queryString.append("                     FROM TBEXP_EXP_MAIN MAIN");
		queryString.append("                       INNER JOIN TBEXP_APPL_INFO INFO ON MAIN.TBEXP_APPL_INFO_ID = INFO.ID");
		queryString.append("                     ) MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         INNER JOIN (SELECT"); // 成本單位的組織型態
		queryString.append("                       DEP.CODE AS DEPCODE,");
		queryString.append("                       DT.CODE  AS DTCODE, "); // 組織型態
		queryString.append("                       DLP.CODE AS DLPCODE "); // 層級屬性
		queryString.append("                     FROM TBEXP_DEPARTMENT DEP");
		queryString.append("                       INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
		queryString.append("                       INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
		queryString.append("                     ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
		queryString.append("       WHERE DEP.DTCODE IN ('2','3','P')"); // 成本單位的組織型態為2:二階、3:三階、P:Pro
		queryString.append("         AND SUBSTR(MAIN.SUBPOENA_NO,2,2) NOT IN ('04','12','14','15','16','18')"); // 傳票號碼第2~3碼不為04醫檢費、12總管理處交際費分攤、14預付遞延攤銷、15月決算、16資產區隔、18年度提列部門費用
		queryString.append("         AND (SUBSTR(MAIN.SUBPOENA_NO,2,2) || SUBSTR(MAIN.EXP_APPL_NO,1,3)) NOT IN"); // 傳票號碼第2~3碼不為xx且費用中分類不為xxx
		queryString.append("             ('00310','00320','00330','003A0','003B0','00210','00220','00230','00240','00250','00260','002A0','002B0','002C0','002D0',");
		/** RE201301487_業務成本統計檔排除810 2013/07/11 cm9539 start */
		queryString.append("              '00G10','00510','00610','00M10','00M20','02J10','02D00','17Q20','07R10','00810')");
		/** RE201301487_業務成本統計檔排除810 2013/07/11 cm9539 end */
		queryString.append("     ) CA00");
		queryString.append(" WHERE CA00.TY = '1'"); // 業務成本統計檔
		queryString.append("   AND CASE WHEN CA00.COST_CODE = 'W' THEN 'Y'");
		// IISI-20100505：修正成本別為 NULL 時的判斷
		queryString.append("            WHEN (CA00.COST_CODE NOT IN ('W','S','F') OR CA00.COST_CODE IS NULL) THEN 'N' END = ?1"); // 輸入是否W
																																	// 點選W核取方塊則輸入為Y；未點選“W”核取方塊則輸入為N
		queryString.append("   AND (CA00.SUBPOENA_DATE >= ?2 AND CA00.SUBPOENA_DATE <= ?3)"); // YYYYMMDD作帳起迄日
		queryString.append(" GROUP BY CA00.COST_UNIT_CODE, CA00.WK_YYMM, CA00.CUST_NO, CA00.DEP_UNIT_CODE2, CA00.DEP_UNIT_CODE1");
		List<Object> parameters = new ArrayList<Object>();
		if (caseW) {
			parameters.add("Y");
		} else {
			parameters.add("N");
		}
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				String wkYyymm = yyyymm;
				String custNo = record[1] == null ? "" : (String) record[1];
				String depUnitCode1 = record[2] == null ? "" : (String) record[2];
				String depUnitCode2 = record[3] == null ? "" : (String) record[3];
				String costUnitCode = record[4] == null ? "" : (String) record[4];
				BigDecimal amt = BigDecimal.ZERO;
				if (record[5] != null) {
					amt = (BigDecimal) record[5];
				}

				String userCode = ((User) AAUtils.getLoggedInUser()).getCode();
				StringBuffer exportRecord = new StringBuffer();
				exportRecord.append(costUnitCode).append(","); // 成本單位代號
				exportRecord.append(","); // 空白
				exportRecord.append(wkYyymm).append(","); // 業績年月，操作人員所輸入的業績年月需轉換為西元年月YYYYMM
				exportRecord.append(custNo).append(","); // 成本代號，(依上述第3、4點之說明)
				exportRecord.append(","); // 空白
				exportRecord.append(costUnitCode).append(","); // 成本單位代號
				exportRecord.append(depUnitCode1).append(","); // 成本單位的上層部室代號(若成本單位已為部室級，則放置成本單位代號即可)，核銷代號為「2H0」者，此欄改為"2S0000"
				exportRecord.append(","); // 空白
				exportRecord.append(depUnitCode2).append(","); // 成本單位的上層區部代號(若成本單位已為駐區級，則放置成本單位代號即可)
				exportRecord.append(","); // 空白
				exportRecord.append(amt.toString()).append(","); // 實支金額，借方為正值，貸方為負值
				exportRecord.append(","); // 空白
				exportRecord.append(userCode); // 轉檔人的員工代號
				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<String> exportCA00Debug(Calendar beginDate, Calendar endDate, boolean caseW) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT"); // 偵錯檔
		queryString.append("   CA00.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("   CA00.SUBPOENA_NO,"); // 傳票號碼傳票號碼
		queryString.append("   CA00.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("   CA00.ACCTCODE,"); // 會計科目
		queryString.append("   CA00.ACCTNAME,"); // 會計科目中文名稱
		queryString.append("   CA00.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("   CA00.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("   CA00.DAMT,"); // 借方金額
		queryString.append("   CA00.CAMT,"); // 貸方金額
		queryString.append("   CA00.COST_CODE,"); // 成本別
		queryString.append("   CA00.UCODE,"); // 申請人員工代號
		queryString.append("   CA00.PAPERS_NO,"); // 文號
		queryString.append("   CA00.SUMMARY "); // 摘要
		queryString.append(" FROM (SELECT"); // CA00
		queryString.append("         MAIN.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("         MAIN.SUBPOENA_NO,"); // 傳票號碼
		queryString.append("         MAIN.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("         MAIN.WK_YYMM, "); // 業績年月
		queryString.append("         ACT.CODE AS ACCTCODE, "); // 會計科目
		queryString.append("         ACT.NAME AS ACCTNAME, "); // 會計科目中文名稱
		queryString.append("         E.DEP_UNIT_CODE1,"); // 部室級
		queryString.append("         DECODE(SUBSTR(MAIN.EXP_APPL_NO,1,3),'2H0','2S0000',E.DEP_UNIT_CODE2) AS DEP_UNIT_CODE2,"); // 駐區級
		queryString.append("         E.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("         ESUB.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("         ET.ENTRY_VALUE,"); // 借貸別
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,"); // 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,"); // 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS AMT,"); // 實支金額，借方為正值，貸方為負值
		queryString.append("         E.COST_CODE,"); // 成本別
		queryString.append("         MAIN.UCODE,"); // 申請人員工代號
		queryString.append("         ESUB.PAPERS_NO,"); // 文號
		queryString.append("         E.SUMMARY,"); // 摘要
		queryString.append("         CASE WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) IN ('3H0','2H0','3J0') THEN '300020' ELSE TO_CHAR(ACCT.CUST_CODE) END AS CUST_NO,"); // 成本代號
		queryString.append("         CASE WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) IN ('3H0','2H0','3J0') THEN '1'");
		queryString.append("              WHEN SUBSTR(MAIN.EXP_APPL_NO,1,3) NOT IN ('3H0','2H0','3J0') THEN");
		queryString.append("                   (CASE WHEN ACCT.DIVCH_FLAG  = 1 AND DEP.DLPCODE = '1' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.BRNCH_FLAG  = 1 AND DEP.DLPCODE = '2' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND ACCT.COST_TYPE IS NULL THEN '1'");
		queryString.append("                         WHEN ACCT.DIVCH_FLAG  = 1 AND DEP.DLPCODE = '1' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		queryString.append("                         WHEN ACCT.BRNCH_FLAG  = 1 AND DEP.DLPCODE = '2' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND E.COST_CODE = ACCT.COST_TYPE THEN '1'");
		// IISI-20100531： 同一張申請單會重覆出現在成本統計與偵錯檔, 因為TBEXP_CA_ACC_R
		// 有重覆的會計科目(成本別'Z')
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '1' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '2' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         WHEN ACCT.UNITCH_FLAG = 1 AND DEP.DLPCODE = '3' AND ACCT.COST_TYPE='Z' AND ACCT.ACCTCODE != '62060423'  THEN '3'");
		queryString.append("                         ELSE '2' END)");
		queryString.append("              END  AS TY"); // TY=1 分進業務成本統計檔TY=2
														// 分進偵錯檔 TY=3
														// 成本別'Z'重覆出現者
		queryString.append("       FROM TBEXP_ENTRY E");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("         LEFT  JOIN TBEXP_ACC_TITLE ACT ON E.TBEXP_ACC_TITLE_ID = ACT.ID "); // IISI-20100531
																											// :
																											// 進入偵錯檔需要將原始會計科目顯示出來
		queryString.append("         LEFT  JOIN TBEXP_EXP_SUB ESUB ON E.ID = ESUB.TBEXP_ENTRY_ID");
		queryString.append("         LEFT  JOIN (SELECT"); // 業務成本類別CA00
		queryString.append("                       ACCT.ID,");
		queryString.append("                       ACCT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("                       ACCT.NAME AS ACCTNAME,"); // 會計科目
		queryString.append("                       CA00.DIVCH_FLAG,"); // 部室代號
		queryString.append("                       CA00.BRNCH_FLAG,"); // 駐區代號
		queryString.append("                       CA00.UNITCH_FLAG,"); // 單位代號
		queryString.append("                       CA00.COST_TYPE,"); // 成本別
		queryString.append("                       CAREF.CUST_CODE"); // CA成本代號
		queryString.append("                     FROM TBEXP_CA_ACC_R CA00");
		queryString.append("                       INNER JOIN TBEXP_ACC_TITLE ACCT ON CA00.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("                       INNER JOIN TBEXP_CA_REFERENCE CAREF ON CA00.TBEXP_CA_REFERENCE_ID = CAREF.ID");
		queryString.append("                     WHERE CA00.OP_CUST_TYPE = '0') ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("         INNER JOIN (SELECT");
		queryString.append("                       MAIN.TBEXP_ENTRY_GROUP_ID,");
		queryString.append("                       MAIN.EXP_APPL_NO,");
		queryString.append("                       MAIN.SUBPOENA_NO,");
		queryString.append("                       MAIN.SUBPOENA_DATE,");
		queryString.append("                       MAIN.WK_YYMM,");
		queryString.append("                       INFO.USER_ID AS UCODE");
		queryString.append("                     FROM TBEXP_EXP_MAIN MAIN");
		queryString.append("                       INNER JOIN TBEXP_APPL_INFO INFO ON MAIN.TBEXP_APPL_INFO_ID = INFO.ID");
		queryString.append("                     ) MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         INNER JOIN (SELECT"); // 成本單位的組織型態
		queryString.append("                       DEP.CODE AS DEPCODE,");
		queryString.append("                       DT.CODE  AS DTCODE, "); // 組織型態
		queryString.append("                       DLP.CODE AS DLPCODE "); // 層級屬性
		queryString.append("                     FROM TBEXP_DEPARTMENT DEP");
		queryString.append("                       INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
		queryString.append("                       INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
		queryString.append("                     ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
		queryString.append("       WHERE DEP.DTCODE IN ('2','3','P')"); // 成本單位的組織型態為2:二階、3:三階、P:Pro
		queryString.append("         AND SUBSTR(MAIN.SUBPOENA_NO,2,2) NOT IN ('04','12','14','15','16','18')"); // 傳票號碼第2~3碼不為04醫檢費、12總管理處交際費分攤、14預付遞延攤銷、15月決算、16資產區隔、18年度提列部門費用
		queryString.append("         AND (SUBSTR(MAIN.SUBPOENA_NO,2,2) || SUBSTR(MAIN.EXP_APPL_NO,1,3)) NOT IN"); // 傳票號碼第2~3碼不為xx且費用中分類不為xxx
		queryString.append("             ('00310','00320','00330','003A0','003B0','00210','00220','00230','00240','00250','00260','002A0','002B0','002C0','002D0',");
		queryString.append("              '00G10','00510','00610','00M10','00M20','02J10','02D00','17Q20','07R10')");
		queryString.append("     ) CA00");
		queryString.append(" WHERE CA00.TY = '2'"); // 偵錯檔
		queryString.append("   AND CASE WHEN CA00.COST_CODE = 'W' THEN 'Y'");
		// IISI-20100505：修正成本別為 NULL 時的判斷
		queryString.append("            WHEN (CA00.COST_CODE NOT IN ('W','S','F') OR CA00.COST_CODE IS NULL) THEN 'N' END = ?1"); // 輸入是否W
																																	// 點選W核取方塊則輸入為Y；未點選“W”核取方塊則輸入為N
		queryString.append("   AND (CA00.SUBPOENA_DATE >= ?2 AND CA00.SUBPOENA_DATE <= ?3)"); // YYYYMMDD作帳起迄日
		List<Object> parameters = new ArrayList<Object>();
		if (caseW) {
			parameters.add("Y");
		} else {
			parameters.add("N");
		}
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				Calendar subpoenaDate = null;
				if (record[0] != null) {
					subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(((Timestamp) record[0]).getTime());
				}
				String subpoenaNo = record[1] == null ? "" : (String) record[1];
				String expApplNo = record[2] == null ? "" : (String) record[2];
				String acctCode = record[3] == null ? "" : (String) record[3];
				String acctName = record[4] == null ? "" : (String) record[4];
				String costUnitCode = record[5] == null ? "" : (String) record[5];
				String insurCode = record[6] == null ? "" : (String) record[6];
				BigDecimal debitAmt = BigDecimal.ZERO;
				if (record[7] != null) {
					debitAmt = (BigDecimal) record[7];
				}
				BigDecimal creditAmt = BigDecimal.ZERO;
				if (record[8] != null) {
					creditAmt = (BigDecimal) record[8];
				}
				String costCode = record[9] == null ? "" : (String) record[9];
				String userId = record[10] == null ? "" : (String) record[10];
				String paperNo = record[11] == null ? "" : (String) record[11];
				String summary = record[12] == null ? "" : (String) record[12];
				StringBuffer exportRecord = new StringBuffer();
				exportRecord.append(DateUtils.getROCDateStr(subpoenaDate.getTime(), "", true)).append(",");// IISI,
																											// 2011/01/04
																											// 修改民國百年問題
																											// By
																											// Eustace
				exportRecord.append(subpoenaNo).append(",");
				exportRecord.append(expApplNo).append(",");
				exportRecord.append(acctCode).append(",");
				exportRecord.append(acctName).append(",");
				exportRecord.append(costUnitCode).append(",");
				exportRecord.append(insurCode).append(",");
				exportRecord.append(debitAmt).append(",");
				exportRecord.append(creditAmt).append(",");
				exportRecord.append(costCode).append(",");
				exportRecord.append(userId).append(",");
				exportRecord.append(paperNo).append(",");
				exportRecord.append(summary);
				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<String> exportCA10BusinessCostStatistics(Calendar beginDate, Calendar endDate) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT"); // 業務成本統計檔
		queryString.append("   CA10.UNIT_CODE3,"); // 申請人單位代號
		queryString.append("   CA10.WK_YYMM,"); // 業績年月
		queryString.append("   CA10.CUST_NO,"); // 成本代號
		queryString.append("   CA10.UNIT_CODE1,"); // 部室代號
		queryString.append("   CA10.UNIT_CODE2,"); // 區部代號
		queryString.append("   CA10.UNIT_CODE3,"); // 單位代號
		queryString.append("   CA10.UCODE,"); // 申請人員工代號
		queryString.append("   SUM(CA10.AMT) AS AMT"); // 實支金額，借方為正值，貸方為負值
		queryString.append(" FROM (SELECT"); // CA10
		queryString.append("         MAIN.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("         MAIN.WK_YYMM,"); // 業績年月
		queryString.append("         MAIN.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("         ACCT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("         ACCT.NAME AS ACCTNAME,"); // 會計科目中文名稱
		queryString.append("         MAIN.UNIT_CODE1,"); // 部室代號
		queryString.append("         MAIN.UNIT_CODE2,"); // 區部代號
		queryString.append("         MAIN.UNIT_CODE3,"); // 成本單位代號/申請人單位代號
		queryString.append("         TO_CHAR('300010') AS CUST_NO,"); // 成本代號
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS AMT,"); // 實支金額，借方為正值，貸方為負值
		queryString.append("         E.COST_CODE,"); // 成本別
		queryString.append("         MAIN.UCODE"); // 申請人員工代號
		queryString.append("       FROM TBEXP_ENTRY E");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("         INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("         INNER JOIN (SELECT");
		queryString.append("                       MAIN.TBEXP_ENTRY_GROUP_ID,");
		queryString.append("                       MAIN.EXP_APPL_NO,");
		queryString.append("                       MAIN.SUBPOENA_NO,");
		queryString.append("                       MAIN.SUBPOENA_DATE,");
		queryString.append("                       RAT.WK_YYMM,");
		queryString.append("                       RAT.UCODE,");
		queryString.append("                       RAT.UNIT_CODE1,");
		queryString.append("                       RAT.UNIT_CODE2,");
		queryString.append("                       RAT.UNIT_CODE3,");
		queryString.append("                       RAT.DEPCODE,");
		queryString.append("                       RAT.DTCODE,");
		queryString.append("                       RAT.DLPCODE");
		queryString.append("                     FROM TBEXP_EXP_MAIN MAIN");
		queryString.append("                       INNER JOIN (SELECT");
		queryString.append("                                     RAT.WK_YYMM,"); // 業績年月YYYYMM
		queryString.append("                                     B.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("                                     U.CODE AS UCODE,"); // 員工代號
		queryString.append("                                     RAT.UNIT_CODE1,"); // 所屬部室代號
		queryString.append("                                     RAT.UNIT_CODE2,"); // 所屬駐區代號
		queryString.append("                                     RAT.UNIT_CODE3,"); // 所屬單位代號
		queryString.append("                                     DEP.DEPCODE,");
		queryString.append("                                     DEP.DTCODE,");
		queryString.append("                                     DEP.DLPCODE");
		queryString.append("                                   FROM TBEXP_RATIFY RAT");
		queryString.append("                                     INNER JOIN TBEXP_USER U ON RAT.TBEXP_USER_ID = U.ID");
		queryString.append("                                     INNER JOIN (SELECT");
		queryString.append("                                                   DEP.CODE AS DEPCODE,");
		queryString.append("                                                   DT.CODE  AS DTCODE, "); // 組織型態
		queryString.append("                                                   DLP.CODE AS DLPCODE "); // 層級屬性
		queryString.append("                                                 FROM TBEXP_DEPARTMENT DEP");
		queryString.append("                                                   INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
		queryString.append("                                                   INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
		queryString.append("                                                 ) DEP ON RAT.UNIT_CODE3 = DEP.DEPCODE");
		queryString.append("                                     INNER JOIN (SELECT");
		queryString.append("                                                   B.TBEXP_RATIFY_ID,");
		queryString.append("                                                   B.EXP_APPL_NO");
		queryString.append("                                                 FROM TBEXP_EXPAPPL_B B");
		queryString.append("                                                   INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID");
		queryString.append("                                                 WHERE STATE.CODE = '90'"); // 已日結
		queryString.append("                                                 ) B ON RAT.ID = B.TBEXP_RATIFY_ID");
		queryString.append("                                   ) RAT ON MAIN.EXP_APPL_NO = RAT.EXP_APPL_NO");
		queryString.append("                   ) MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("       WHERE MAIN.DTCODE IN ('2','3','5','P')"); // 成本單位的組織型態為二階、三階、通路、Pro
		queryString.append("         AND SUBSTR(ACCT.CODE,1,1) = '6'");
		queryString.append("         AND SUBSTR(MAIN.EXP_APPL_NO,1,3) IN"); // 費用中分類為xxx
		queryString.append("             ('310','320','330','3A0','3B0','210','220','230','240','250','260','2A0','2B0','2C0','2D0','G10')");
		queryString.append("     ) CA10");
		queryString.append(" WHERE CA10.SUBPOENA_DATE >= ?1 AND CA10.SUBPOENA_DATE <= ?2"); // YYYYMMDD作帳起迄日
		queryString.append(" GROUP BY CA10.WK_YYMM, CA10.CUST_NO, CA10.UNIT_CODE1, CA10.UNIT_CODE2, CA10.UNIT_CODE3, CA10.UCODE");
		queryString.append(" ORDER BY CA10.UCODE");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				String depUnitCode3 = record[0] == null ? "" : (String) record[0];
				String wkYyyymm = record[1] == null ? "" : (String) record[1];
				String custNo = record[2] == null ? "" : (String) record[2];
				String depUnitCode1 = record[3] == null ? "" : (String) record[3];
				String depUnitCode2 = record[4] == null ? "" : (String) record[4];
				String userCode = record[6] == null ? "" : (String) record[6];
				BigDecimal amt = BigDecimal.ZERO;
				if (record[7] != null) {
					amt = (BigDecimal) record[7];
				}
				StringBuffer exportRecord = new StringBuffer();
				exportRecord.append(depUnitCode3).append(","); // 申請人單位代號
				exportRecord.append(","); // 空白
				exportRecord.append(wkYyyymm).append(","); // 業績年月，使用篩選出資料的的業績年月值，需轉換為西元年月YYYYMM
				exportRecord.append(custNo).append(","); // 成本代號，(依上述第3、4點之說明)
				exportRecord.append(","); // 空白
				exportRecord.append(depUnitCode3).append(","); // 申請人單位代號(以核定表的資料為主)
				exportRecord.append(depUnitCode1).append(","); // 申請人部室代號(以核定表的資料為主)
				exportRecord.append(","); // 空白
				exportRecord.append(depUnitCode2).append(","); // 申請人區部代號(以核定表的資料為主)
				exportRecord.append(","); // 空白
				exportRecord.append(amt.toString()).append(","); // 實支金額，借方為正值，貸方為負值
				exportRecord.append(","); // 空白
				exportRecord.append(userCode); // 申請人員工代號(即核銷檔的員工代號)
				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<String> exportCA20BusinessCostStatistics(Calendar beginDate, Calendar endDate, boolean caseW, String yyyymm) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT"); // 業務成本統計檔
		queryString.append("   DECODE(CA20.DTCODE,'2','222222','3','333333',NULL) AS ST, "); // 制度別為2則寫入222222，或為3則寫入333333
		queryString.append("   CA20.CUST_NO,"); // 成本代號
		queryString.append("   CA20.DTCODE, "); // 薪津制度別，制度別為2則寫入2，或為3則寫入3
		queryString.append("   SUM(CA20.AMT) AS AMT"); // 實支金額，借方為正值，貸方為負值
		queryString.append(" FROM (SELECT"); // CA20
		queryString.append("         MAIN.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("         MAIN.SUBPOENA_NO,"); // 傳票號碼
		queryString.append("         MAIN.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("         ACCT.ACCTCODE,"); // 會計科目
		queryString.append("         ACCT.ACCTNAME,"); // 會計科目中文名稱
		queryString.append("         E.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("         ESUB.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("         ET.ENTRY_VALUE,"); // 借貸別
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,"); // 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,"); // 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS AMT,"); // 實支金額，借方為正值，貸方為負值
		queryString.append("         E.COST_CODE,"); // 成本別
		queryString.append("         MAIN.UCODE,"); // 申請人員工代號
		queryString.append("         ESUB.PAPERS_NO,"); // 文號
		queryString.append("         E.SUMMARY,"); // 摘要
		queryString.append("         ACCT.CUST_CODE AS CUST_NO,"); // 成本代號
		queryString.append("         MAIN.DTCODE,"); // 制度別
		queryString.append("         CASE WHEN ACCT.ACCTCODE IS NULL THEN '2' ELSE '1' END AS TY"); // TY=1
																									// 分進業務成本統計檔
																									// TY=2
																									// 分進偵錯檔
		queryString.append("       FROM TBEXP_ENTRY E");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("         LEFT  JOIN (SELECT"); // 業務成本類別
		queryString.append("                       ACCT.ID,");
		queryString.append("                       ACCT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("                       ACCT.NAME AS ACCTNAME,"); // 會計科目
		queryString.append("                       CAREF.CUST_CODE"); // CA成本代號
		queryString.append("                     FROM TBEXP_CA_ACC_R CA00");
		queryString.append("                       INNER JOIN TBEXP_ACC_TITLE ACCT ON CA00.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("                       INNER JOIN TBEXP_CA_REFERENCE CAREF ON CA00.TBEXP_CA_REFERENCE_ID = CAREF.ID");
		queryString.append("                     WHERE CA00.OP_CUST_TYPE = '2') ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("         INNER JOIN (SELECT");
		queryString.append("                       MAIN.TBEXP_ENTRY_GROUP_ID,");
		queryString.append("                       MAIN.EXP_APPL_NO,");
		queryString.append("                       MAIN.SUBPOENA_NO,");
		queryString.append("                       MAIN.SUBPOENA_DATE,");
		queryString.append("                       INFO.USER_ID AS UCODE,");
		queryString.append("                       PAYDTL.DTCODE");
		queryString.append("                     FROM TBEXP_EXP_MAIN MAIN");
		queryString.append("                       INNER JOIN TBEXP_APPL_INFO INFO ON MAIN.TBEXP_APPL_INFO_ID = INFO.ID");
		queryString.append("                       INNER JOIN (SELECT");
		queryString.append("                                     PAYDTL.EXP_APPL_NO,");
		queryString.append("                                     DT.CODE AS DTCODE");
		queryString.append("                                   FROM TBEXP_DEPARTMENT DEP");
		queryString.append("                                     INNER JOIN TBEXP_PAYMENT_DETAIL PAYDTL ON PAYDTL.DEPARTMENT_ID = DEP.CODE");
		queryString.append("                                     INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
		queryString.append("                                   WHERE DT.CODE IN ('2','3')) PAYDTL ON MAIN.EXP_APPL_NO = PAYDTL.EXP_APPL_NO");
		queryString.append("                     ) MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         LEFT  JOIN TBEXP_EXP_SUB ESUB ON E.ID = ESUB.TBEXP_ENTRY_ID");
		queryString.append("       WHERE (SUBSTR(MAIN.SUBPOENA_NO,2,2) IN ('17','18')");
		queryString.append("          OR (SUBSTR(MAIN.SUBPOENA_NO,2,2) || SUBSTR(MAIN.EXP_APPL_NO,1,3)) IN ('00N10','01A20','02E10','02J00'))");
		// 201402574_業務成本統計檔CA2修改 2015/03/30 EC0416 start
		queryString.append("         AND  E.COST_UNIT_CODE  IN ('117000','12A000')"); // 成本單位的單位代號為117000外企部,12A000市場行銷部
		queryString.append("         AND  SUBSTR(ESUB.PAPERS_NO,1,6)  IN ('117000','12A000') "); // 文號有值，且前六碼為117000外企部與12A000市場行銷部發文之文號
		// 201402574_業務成本統計檔CA2修改 2015/03/30 EC0416 end
		queryString.append("     ) CA20");
		queryString.append(" WHERE CA20.TY = '1'"); // 業務成本統計檔
		queryString.append("   AND DECODE(CA20.COST_CODE,'W','Y','N') = ?1"); // 若操作人員未點選“W”核取方塊時，成本別為“X”的資
		queryString.append("   AND (CA20.SUBPOENA_DATE >= ?2 AND CA20.SUBPOENA_DATE <= ?3)"); // YYYYMMDD作帳起迄日
		queryString.append(" GROUP BY CA20.CUST_NO, CA20.DTCODE");
		List<Object> parameters = new ArrayList<Object>();
		if (caseW) {
			parameters.add("Y");
		} else {
			parameters.add("N");
		}
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				String sysType = record[0] == null ? "" : (String) record[0];
				String custNo = record[1] == null ? "" : (String) record[1];
				String dtCode = record[2] == null ? "" : (String) record[2];
				BigDecimal amt = BigDecimal.ZERO;
				if (record[3] != null) {
					amt = (BigDecimal) record[3];
				}
				String userCode = ((User) AAUtils.getLoggedInUser()).getCode();
				StringBuffer exportRecord = new StringBuffer();
				exportRecord.append(sysType).append(","); // 匯款單位的組織型態為”二階”則寫入222222，匯款單位的組織型態為”三階”則寫入333333
				exportRecord.append(","); // 空白
				exportRecord.append(yyyymm).append(","); // 業績年月，操作人員所輸入的業績年月需轉換為西元年月YYYYMM
				exportRecord.append(custNo).append(","); // 成本代號，(依上述第3、4點之說明)
				exportRecord.append(dtCode).append(","); // 薪津制度別，匯款單位的組織型態為”二階”則寫入2，匯款單位的組織型態為”三階”則寫入3
				exportRecord.append(",");
				exportRecord.append(",");
				exportRecord.append(",");
				exportRecord.append(",");
				exportRecord.append(",");
				exportRecord.append(amt.toString()).append(",");
				exportRecord.append(",");
				exportRecord.append(userCode);
				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<String> exportCA20Debug(Calendar beginDate, Calendar endDate, boolean caseW) {
		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT"); // 偵錯檔
		queryString.append("   CA20.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("   CA20.SUBPOENA_NO,"); // 傳票號碼
		queryString.append("   CA20.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("   CA20.ACCTCODE,"); // 會計科目
		queryString.append("   CA20.ACCTNAME,"); // 會計科目中文名稱
		queryString.append("   CA20.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("   CA20.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("   CA20.DAMT,"); // 借方金額
		queryString.append("   CA20.CAMT,"); // 貸方金額
		queryString.append("   CA20.COST_CODE,"); // 成本別
		queryString.append("   CA20.UCODE,"); // 申請人員工代號
		queryString.append("   CA20.PAPERS_NO,"); // 文號
		queryString.append("   CA20.SUMMARY"); // 摘要
		queryString.append(" FROM (SELECT"); // CA20
		queryString.append("         MAIN.SUBPOENA_DATE,"); // 作帳年月日
		queryString.append("         MAIN.SUBPOENA_NO,"); // 傳票號碼
		queryString.append("         MAIN.EXP_APPL_NO,"); // 費用申請單號
		queryString.append("         ACT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("         ACT.NAME AS ACCTNAME,"); // 會計科目中文名稱
		queryString.append("         E.COST_UNIT_CODE,"); // 成本單位代號
		queryString.append("         ESUB.INSUR_AGENT_CODE,"); // 保代代號
		queryString.append("         ET.ENTRY_VALUE,"); // 借貸別
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,"); // 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,"); // 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS AMT,"); // 實支金額，借方為正值，貸方為負值
		queryString.append("         E.COST_CODE,"); // 成本別
		queryString.append("         MAIN.UCODE,"); // 申請人員工代號
		queryString.append("         ESUB.PAPERS_NO,"); // 文號
		queryString.append("         E.SUMMARY,"); // 摘要
		queryString.append("         ACCT.CUST_CODE AS CUST_NO,"); // 成本代號
		queryString.append("         MAIN.DTCODE,"); // 制度別
		queryString.append("         CASE WHEN ACCT.ACCTCODE IS NULL THEN '2' ELSE '1' END AS TY"); // TY=1
																									// 分進業務成本統計檔
																									// TY=2
																									// 分進偵錯檔
		queryString.append("       FROM TBEXP_ENTRY E");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("         LEFT  JOIN TBEXP_ACC_TITLE ACT ON E.TBEXP_ACC_TITLE_ID = ACT.ID "); // IISI-20100531
																											// 偵錯檔中需顯示原來的會計科目
		queryString.append("         LEFT  JOIN (SELECT"); // 業務成本類別
		queryString.append("                       ACCT.ID,");
		queryString.append("                       ACCT.CODE AS ACCTCODE,"); // 會計科目
		queryString.append("                       ACCT.NAME AS ACCTNAME,"); // 會計科目
		queryString.append("                       CAREF.CUST_CODE"); // CA成本代號
		queryString.append("                     FROM TBEXP_CA_ACC_R CA00");
		queryString.append("                       INNER JOIN TBEXP_ACC_TITLE ACCT ON CA00.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("                       INNER JOIN TBEXP_CA_REFERENCE CAREF ON CA00.TBEXP_CA_REFERENCE_ID = CAREF.ID");
		queryString.append("                     WHERE CA00.OP_CUST_TYPE = '2') ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("         INNER JOIN (SELECT");
		queryString.append("                       MAIN.TBEXP_ENTRY_GROUP_ID,");
		queryString.append("                       MAIN.EXP_APPL_NO,");
		queryString.append("                       MAIN.SUBPOENA_NO,");
		queryString.append("                       MAIN.SUBPOENA_DATE,");
		queryString.append("                       INFO.USER_ID AS UCODE,");
		queryString.append("                       PAYDTL.DTCODE");
		queryString.append("                     FROM TBEXP_EXP_MAIN MAIN");
		queryString.append("                       INNER JOIN TBEXP_APPL_INFO INFO ON MAIN.TBEXP_APPL_INFO_ID = INFO.ID");
		queryString.append("                       INNER JOIN (SELECT");
		queryString.append("                                     PAYDTL.EXP_APPL_NO,");
		queryString.append("                                     DT.CODE AS DTCODE");
		queryString.append("                                   FROM TBEXP_DEPARTMENT DEP");
		queryString.append("                                     INNER JOIN TBEXP_PAYMENT_DETAIL PAYDTL ON PAYDTL.DEPARTMENT_ID = DEP.CODE");
		queryString.append("                                     INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
		queryString.append("                                   WHERE DT.CODE IN ('2','3')) PAYDTL ON MAIN.EXP_APPL_NO = PAYDTL.EXP_APPL_NO");
		queryString.append("                     ) MAIN ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         LEFT  JOIN TBEXP_EXP_SUB ESUB ON E.ID = ESUB.TBEXP_ENTRY_ID");
		queryString.append("       WHERE (SUBSTR(MAIN.SUBPOENA_NO,2,2) IN ('17','18')");
		queryString.append("          OR (SUBSTR(MAIN.SUBPOENA_NO,2,2) || SUBSTR(MAIN.EXP_APPL_NO,1,3)) IN ('00N10','01A20','02E10','02J00'))");
		queryString.append("         AND  E.COST_UNIT_CODE = '117000'"); // 成本單位的單位代號為117000外企部
		queryString.append("         AND  SUBSTR(ESUB.PAPERS_NO,1,6) = '117000' "); // 文號有值，且前六碼為117000外企部發文之文號
		queryString.append("     ) CA20");
		queryString.append(" WHERE CA20.TY = '2'"); // 偵錯檔
		queryString.append("   AND DECODE(CA20.COST_CODE,'W','Y','N') = ?1"); // 若操作人員未點選“W”核取方塊時，成本別為“X”的資
		queryString.append("   AND (CA20.SUBPOENA_DATE >= ?2 AND CA20.SUBPOENA_DATE <= ?3)"); // YYYYMMDD作帳起迄日
		List<Object> parameters = new ArrayList<Object>();
		if (caseW) {
			parameters.add("Y");
		} else {
			parameters.add("N");
		}
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;
				Calendar subpoenaDate = null;
				if (record[0] != null) {
					subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(((Timestamp) record[0]).getTime());
				}
				String subpoenaNo = record[1] == null ? "" : (String) record[1];
				String expApplNo = record[2] == null ? "" : (String) record[2];
				String acctCode = record[3] == null ? "" : (String) record[3];
				String acctName = record[4] == null ? "" : (String) record[4];
				String costUnitCode = record[5] == null ? "" : (String) record[5];
				String insurCode = record[6] == null ? "" : (String) record[6];
				BigDecimal debitAmt = BigDecimal.ZERO;
				if (record[7] != null) {
					debitAmt = (BigDecimal) record[7];
				}
				BigDecimal creditAmt = BigDecimal.ZERO;
				if (record[8] != null) {
					creditAmt = (BigDecimal) record[8];
				}
				String costCode = record[9] == null ? "" : (String) record[9];
				String userId = record[10] == null ? "" : (String) record[10];
				String paperNo = record[11] == null ? "" : (String) record[11];
				String summary = record[12] == null ? "" : (String) record[12];
				StringBuffer exportRecord = new StringBuffer();
				exportRecord.append(DateUtils.getROCDateStr(subpoenaDate.getTime(), "", true)).append(",");// IISI,
																											// 2011/01/04
																											// 修改民國百年問題
																											// By
																											// Eustace
				exportRecord.append(subpoenaNo).append(",");
				exportRecord.append(expApplNo).append(",");
				exportRecord.append(acctCode).append(",");
				exportRecord.append(acctName).append(",");
				exportRecord.append(costUnitCode).append(",");
				exportRecord.append(insurCode).append(",");
				exportRecord.append(debitAmt).append(",");
				exportRecord.append(creditAmt).append(",");
				exportRecord.append(costCode).append(",");
				exportRecord.append(userId).append(",");
				exportRecord.append(paperNo).append(",");
				exportRecord.append(summary);
				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.dao.ExpMainDao#exportExpMain(java.util.Calendar
	 * , java.util.Calendar)
	 * 
	 * RE201102308_D6.5費用主檔 : To revise the SQL script of D6.5 20120229 CM9539
	 * 
	 * RE201201380-修正D6.5費用主檔SQL(自提單) 20120607 cm9539 To fix the problem of
	 * lacking 3C0&3H0 checking requisitions.
	 */
	// RE201202140 modify by michael in 2013/06/27 start
	/**
	 * public ExpMainDto exportExpMain(Calendar beginDate, Calendar endDate) {
	 * // TODO Yunglin // TODO Yunglin ExpMainDto dto = new ExpMainDto();
	 * StringBuffer queryString = new StringBuffer();
	 * 
	 * queryString.append(" SELECT EXP.REACODE, "); // " 初審員代", ");
	 * queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
	 * queryString.append("        EXP.REMIT_UNIT_CODE, "); // " 匯款單位", ");
	 * queryString.append("        EXP.REMIT_UNIT_NAME, "); // " 匯款單位中文", ");
	 * queryString.append("        EXP.REMIT_USER_CODE, "); // " 受款員代", ");
	 * queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
	 * // "); queryString.append(" MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
	 * queryString
	 * .append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); //
	 * " 傳票日期", // "); queryString.append(" E.DEP_UNIT_CODE1, "); // " 部級代號",
	 * "); queryString.append(" E.DEP_UNIT_CODE2, "); // " 駐區級代號",
	 * "); queryString.append(" E.COST_UNIT_CODE, "); // " 成本單位",
	 * "); queryString.append(" ACC.CODE, "); // " 會計科目代號", ");
	 * queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
	 * queryString.append("        E.AMT, "); // " 金額", ");
	 * queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
	 * queryString.append(
	 * "        CASE                                                                             "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               "
	 * ); queryString.append(
	 * "                AND ET.ENTRY_VALUE = 'D'                                                 "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              E.AMT                                                                      "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               "
	 * ); queryString.append(
	 * "                AND ET.ENTRY_VALUE = 'C'                                                 "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              -E.AMT                                                                     "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              -E.AMT                                                                     "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              E.AMT                                                                      "
	 * ); queryString.append(
	 * "           ELSE                                                                          "
	 * ); queryString.append(
	 * "              E.AMT                                                                      "
	 * ); queryString.append(
	 * "        END                                                                              "
	 * ); queryString.append("          , "); // " 調整借貸別金額", ");
	 * queryString.append("        E.COST_CODE, "); // " 成本別", ");
	 * queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
	 * queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
	 * queryString.append("        PT.NAME, "); // " 付款別", ");
	 * queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
	 * queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
	 * queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
	 * queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
	 * queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
	 * queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
	 * queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
	 * queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); //
	 * " 發票廠商簡稱", // "); queryString.append(" ' ' , "); //
	 * --SUB.PAY_ID" 受款對象證號", ");
	 * queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); //
	 * " 受款廠商簡稱", // "); queryString.append(" SUB.INVOICE_NO, "); // " 發票號碼",
	 * "); queryString.append(" SUB.PAPERS_NO, "); // " 文號", ");
	 * queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
	 * queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
	 * queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
	 * queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
	 * queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
	 * queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
	 * queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
	 * queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
	 * // "); queryString.append(" SUB.WARRANTY_YEAR, "); // " 保固年限", ");
	 * queryString
	 * .append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); //
	 * " 保固起日", // "); queryString.append(
	 * "        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
	 * // "); queryString.append(" SUB.THIS_PERIOD_DEGREE, "); // " 本期度數",
	 * "); queryString.append(" SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
	 * queryString.append(
	 * "        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期", //
	 * "); queryString.append(" SUB.AMOUNT, "); // " 招待人數", "); queryString
	 * .append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); //
	 * " 摘要", // "); queryString.append(" E.INDUSTRY_CODE, "); // " 業別代號",
	 * "); queryString.append(" SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號",
	 * "); queryString.append(" SUB.ROSTER_NO, "); // " 名冊單號",
	 * "); queryString.append(" SUB.TRAINING_TYPE, "); // " 訓練類別",
	 * "); queryString.append(" SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構", //
	 * "); queryString.append(" SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
	 * // "); queryString.append(" NVL (SUB.SUBJECT, ''), "); // " 訓練課程",
	 * "); queryString.append(" SUB.SYSTEM_TYPE, "); // " 制度別",
	 * "); queryString.append(" U.CODE, "); // " 建檔人員工代號", ");
	 * queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
	 * queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
	 * queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
	 * queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), ");
	 * // " 匯款日期", // "); queryString.append(" NULL, "); // " 業務線代號", ");
	 * queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_level_prop                                            "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = E.COST_UNIT_CODE))                                   "
	 * ); queryString.append(
	 * "           ,                                                                     "
	 * );// 層級屬性 queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_type                                                  "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_type_id                                           "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = E.COST_UNIT_CODE))                                   "
	 * ); queryString.append(
	 * "           ,                                                                     "
	 * );// 組織型態 queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_prop                                                  "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_prop_id                                           "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = E.COST_UNIT_CODE))                                   "
	 * ); queryString.append(
	 * "           ,                                                                     "
	 * );// 部門屬性 queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_dep_group                                                 "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_dep_group_id                                          "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = E.COST_UNIT_CODE))                                   "
	 * ); queryString.append(
	 * "           ,                                                                     "
	 * );// 組織群組 queryString.append("        BUD.CODE, "); // " 預算項目", ");
	 * queryString.append("        BUD.NAME "); // " 預算項目中文" ");
	 * queryString.append(
	 * "   FROM EXPADMIN.TBEXP_ENTRY E                                                           "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             "
	 * ); queryString.append(
	 * "           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  "
	 * ); queryString.append(
	 * "        INNER JOIN (SELECT              U.CODE AS REACODE,             "
	 * );// --初審經辦員工代號 queryString.append(
	 * "                           U.NAME AS REANAME,                              "
	 * );// --初審經辦姓名 queryString.append(
	 * "                           D.CODE AS REMIT_UNIT_CODE,                      "
	 * );// --匯款單位代號 queryString.append(
	 * "                           D.NAME AS REMIT_UNIT_NAME,                      "
	 * );// --匯款單位中文 queryString.append(
	 * "                           U1.CODE AS REMIT_USER_CODE,                   "
	 * );// --受款人員工代號 queryString.append(
	 * "                           U1.NAME AS REMIT_USER_NAME,                       "
	 * );// --受款人姓名 queryString.append(
	 * "                           B.EXP_APPL_NO,                                                "
	 * ); queryString.append(
	 * "                           B.TBEXP_APPL_INFO_ID,                                         "
	 * ); queryString.append(
	 * "                           B.TBEXP_CREATE_USER_ID,                                       "
	 * ); queryString.append(
	 * "                           B.TBEXP_PAYMENT_TYPE_ID,                                      "
	 * ); queryString.append(
	 * "                           B.TBEXP_ENTRY_GROUP_ID                                        "
	 * ); queryString.append(
	 * "                      FROM EXPADMIN.TBEXP_EXPAPPL_B B                                    "
	 * ); queryString.append(
	 * "                           INNER JOIN TBEXP_USER U                                       "
	 * ); queryString.append(
	 * "                              ON B.TBEXP_REAMGR_USER_ID||'' = U.ID                       "
	 * ); queryString.append(
	 * "                           LEFT JOIN TBEXP_DEPARTMENT D                                 "
	 * ); queryString.append(
	 * "                              ON D.ID = B.TBEXP_DEPARTMENT_ID||''                        "
	 * ); queryString.append(
	 * "                           LEFT JOIN TBEXP_USER U1                                      "
	 * ); queryString.append(
	 * "                              ON U1.ID = B.TBEXP_REMIT_USER_ID||''                       "
	 * ); queryString.append(
	 * "                    UNION ALL                                                            "
	 * ); queryString.append(
	 * "                    SELECT U.CODE,                                     "
	 * );// --初審經辦員工代號 queryString.append(
	 * "                           U.NAME,                                     "
	 * );// --初審經辦員工姓名 queryString.append(
	 * "                           C.DRAW_MONEY_UNIT_CODE,                         "
	 * );// --單位匯款代號 queryString.append(
	 * "                           C.DRAW_MONEY_UNIT_NAME,                         "
	 * );// --單位匯款中文 queryString.append(
	 * "                           INFO.USER_ID,                                   "
	 * );// --受款員工代號 queryString.append(
	 * "                           INFO.USER_NAME,                                 "
	 * );// --受款員工姓名 queryString.append(
	 * "                           C.EXP_APPL_NO,                                                "
	 * ); queryString.append(
	 * "                           C.TBEXP_APPL_INFO_ID,                                         "
	 * ); queryString.append(
	 * "                           C.TBEXP_CREATE_USER_ID,                                       "
	 * ); queryString.append(
	 * "                           C.TBEXP_PAYMENT_TYPE_ID,                                      "
	 * ); queryString.append(
	 * "                           C.TBEXP_ENTRY_GROUP_ID                                        "
	 * ); queryString.append(
	 * "                      FROM EXPADMIN.TBEXP_EXPAPPL_C C                                    "
	 * ); queryString.append(
	 * "                           INNER JOIN TBEXP_USER U                                       "
	 * ); queryString.append(
	 * "                              ON U.ID = C.TBEXP_ACTUAL_VERIFY_USER_ID                    "
	 * ); queryString.append(
	 * "                           LEFT JOIN TBEXP_APPL_INFO INFO                                "
	 * ); queryString.append(
	 * "                              ON INFO.ID = TBEXP_DRAW_MONEY_USER_INFO_ID                 "
	 * ); queryString.append(
	 * "                    UNION ALL                                                            "
	 * ); queryString.append(
	 * "                    SELECT U.CODE,                                     "
	 * );// --初審經辦員工代號 queryString.append(
	 * "                           U.NAME,                                     "
	 * );// --初審經辦中文姓名 queryString.append(
	 * "                           DD.CODE,                                        "
	 * );// --匯款單位代號 queryString.append(
	 * "                           DD.NAME,                                        "
	 * );// --匯款單位中文 queryString.append(
	 * "                           U1.CODE,                                      "
	 * );// --受款人員工代號 queryString.append(
	 * "                           U1.NAME,                                          "
	 * );// --受款人姓名 queryString.append(
	 * "                           D.EXP_APPL_NO,                                                "
	 * ); queryString.append(
	 * "                           D.TBEXP_APPL_INFO_ID,                                         "
	 * ); queryString.append(
	 * "                           D.TBEXP_CREATE_USER_ID,                                       "
	 * ); queryString.append(
	 * "                           DECODE (                                                      "
	 * ); queryString.append(
	 * "                              D.DTYPE,                                                   "
	 * ); queryString.append(
	 * "                              'ManualAccountAppl', MAL.TBEXP_PAYMENT_TYPE_ID,            "
	 * ); queryString.append(
	 * "                              ''),                                                       "
	 * ); queryString.append(
	 * "                           D.TBEXP_ENTRY_GROUP_ID                                        "
	 * ); queryString.append(
	 * "                      FROM EXPADMIN.TBEXP_EXPAPPL_D D                                    "
	 * ); queryString.append(
	 * "                           LEFT JOIN EXPADMIN.TBEXP_MALACC_APPL MAL                      "
	 * ); queryString.append(
	 * "                              ON MAL.ID = D.ID                                           "
	 * ); queryString.append(
	 * "                           INNER JOIN TBEXP_USER U                                       "
	 * ); queryString.append(
	 * "                              ON U.ID = D.TBEXP_REAMGR_USER_ID                           "
	 * ); queryString.append(
	 * "                           LEFT JOIN TBEXP_DEPARTMENT DD                                 "
	 * ); queryString.append(
	 * "                              ON DD.CODE = MAL.PAYMENT_TARGET_ID                         "
	 * ); queryString.append(
	 * "                           LEFT JOIN TBEXP_USER U1                                       "
	 * ); queryString.append(
	 * "                              ON U1.CODE = MAL.PAYMENT_TARGET_ID) EXP                    "
	 * ); queryString.append(
	 * "           ON EXP.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID                          "
	 * ); queryString.append(
	 * "        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          "
	 * ); queryString.append(
	 * "           ON MAIN.EXP_APPL_NO = EXP.EXP_APPL_NO                                         "
	 * ); queryString.append(
	 * "        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          "
	 * ); queryString.append(
	 * "           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         "
	 * ); queryString.append(
	 * "           ON PT.ID = EXP.TBEXP_PAYMENT_TYPE_ID                                          "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           "
	 * ); queryString.append(
	 * "           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  "
	 * ); queryString.append(
	 * "           ON U.ID = EXP.TBEXP_CREATE_USER_ID                                            "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          "
	 * ); queryString.append(
	 * "           ON INFO.ID = EXP.TBEXP_APPL_INFO_ID                                           "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         "
	 * ); queryString.append(
	 * "           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             "
	 * ); queryString.append(
	 * "  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            "
	 * ); queryString.append(
	 * "        AND EXP_APPL_NO NOT LIKE 'T4%'                                                   "
	 * ); queryString.append(
	 * " UNION ALL                                                                               "
	 * ); queryString.append(
	 * " SELECT   NULL,                                                                          "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.SUBPOENA_NO,                                                                 "
	 * ); queryString.append(
	 * "        TO_CHAR (EXT.SUBPOENA_DATE, 'YYYY/MM/DD'),                                       "
	 * ); queryString.append(
	 * "        EXT.DEP_UNIT_CODE1,                                                              "
	 * ); queryString.append(
	 * "        EXT.DEP_UNIT_CODE2,                                                              "
	 * ); queryString.append(
	 * "        EXT.COST_UNIT_CODE,                                                              "
	 * ); queryString.append(
	 * "        EXT.ACCT_CODE,                                                                   "
	 * ); queryString.append(
	 * "        ACC.NAME,                                                                        "
	 * ); queryString.append(
	 * "        EXT.AMT,                                                                         "
	 * ); queryString.append(
	 * "        ET.ENTRY_VALUE,                                                                  "
	 * ); queryString.append(
	 * "        CASE                                                                             "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               "
	 * ); queryString.append(
	 * "                AND ET.ENTRY_VALUE = 'D'                                                 "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              EXT.AMT                                                                    "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               "
	 * ); queryString.append(
	 * "                AND ET.ENTRY_VALUE = 'C'                                                 "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              -EXT.AMT                                                                   "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              -EXT.AMT                                                                   "
	 * ); queryString.append(
	 * "           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           "
	 * ); queryString.append(
	 * "           THEN                                                                          "
	 * ); queryString.append(
	 * "              EXT.AMT                                                                    "
	 * ); queryString.append(
	 * "           ELSE                                                                          "
	 * ); queryString.append(
	 * "              EXT.AMT                                                                    "
	 * ); queryString.append(
	 * "        END                                                                              "
	 * ); queryString.append(
	 * "           AMT_MODIFIED,                                                                 "
	 * ); queryString.append(
	 * "        EXT.COST_CODE,                                                                   "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.COMP_ID,                                                                     "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.PAPERS_NO,                                                                   "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.INSUR_AGENT_CODE,                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.PROJECT_NO,                                                                  "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        CASE WHEN EXT.SUMMARY = ',' THEN N'，' ELSE EXT.SUMMARY END SUMMARY,             "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        NULL,                                                                            "
	 * ); queryString.append(
	 * "        EXT.GENERAL_MGR_SN,                                                              "
	 * ); queryString.append(
	 * "        EXT.WK_YYMM,                                                                     "
	 * ); queryString.append(
	 * "        CONCAT (TO_CHAR (TO_NUMBER (EXP_YYYY) + 1911), EXT.EXP_MM),                      "
	 * ); queryString.append(
	 * "        TO_CHAR (EXT.REMIT_DATE, 'YYYY/MM/DD'),                                                                  "
	 * ); queryString.append(
	 * "        EXT.SALES_LINE_CODE,                                                             "
	 * ); queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_level_prop                                            "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = EXT.COST_UNIT_CODE))                                 "
	 * ); queryString.append(
	 * "           level_group_name,                                                             "
	 * ); queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_type                                                  "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_type_id                                           "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = EXT.COST_UNIT_CODE))                                 "
	 * ); queryString.append(
	 * "           dep_type_name,                                                                "
	 * ); queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_DEP_prop                                                  "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_DEP_prop_id                                           "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = EXT.COST_UNIT_CODE))                                 "
	 * ); queryString.append(
	 * "           dep_prop_name,                                                                "
	 * ); queryString.append(
	 * "        (SELECT Name                                                                     "
	 * ); queryString.append(
	 * "           FROM EXPADMIN.TBEXP_dep_group                                                 "
	 * ); queryString.append(
	 * "          WHERE id = (SELECT TBEXP_dep_group_id                                          "
	 * ); queryString.append(
	 * "                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   "
	 * ); queryString.append(
	 * "                       WHERE code = EXT.COST_UNIT_CODE))                                 "
	 * ); queryString.append(
	 * "           group_name,                                                                   "
	 * ); queryString.append(
	 * "        BUD.CODE Budget_Code,                                                            "
	 * ); queryString.append(
	 * "        BUD.NAME Budget_Name                                                             "
	 * ); queryString.append(
	 * "   FROM EXPADMIN.TBEXP_EXT_SYS_ENTRY EXT                                                 "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                           "
	 * ); queryString.append(
	 * "           ON ACC.CODE = EXT.ACCT_CODE                                                   "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           "
	 * ); queryString.append(
	 * "           ON ET.ID = EXT.TBEXP_ENTRY_TYPE_ID                                            "
	 * ); queryString.append(
	 * "        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         "
	 * ); queryString.append(
	 * "           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             "
	 * ); queryString.append(
	 * "  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            "
	 * );
	 * 
	 * List<Object> parameters = new ArrayList<Object>();
	 * parameters.add(beginDate); parameters.add(endDate); List list =
	 * findByNativeSQL(queryString.toString(), parameters); List<String> result
	 * = new ArrayList<String>(); BigDecimal totalAmt = BigDecimal.ZERO; if
	 * (!CollectionUtils.isEmpty(list)) { for (Object obj : list) { Object[]
	 * record = (Object[]) obj; // 總金額處理 BigDecimal amt = BigDecimal.ZERO; if
	 * (record[8] != null) { amt = (BigDecimal) record[13]; }
	 * 
	 * String entryTypeValue = (String) record[14];
	 * 
	 * if ("D".equals(entryTypeValue)) { totalAmt = totalAmt.add(amt); } else {
	 * totalAmt = totalAmt.subtract(amt); } // 匯出字串處理 StringBuffer exportRecord
	 * = new StringBuffer(); for (int i = 0; i < record.length; i++) { Object
	 * recObj = record[i]; if (recObj == null) { exportRecord.append(""); } else
	 * { if (recObj instanceof Timestamp) { Calendar date =
	 * Calendar.getInstance(); date.setTimeInMillis(((Timestamp)
	 * recObj).getTime());
	 * exportRecord.append(DateUtils.getISODateStr(date.getTime(), "")); } else
	 * if (recObj instanceof BigDecimal) { exportRecord.append(((BigDecimal)
	 * recObj).toString()); } else { exportRecord.append((String) recObj); } }
	 * if (i != record.length - 1) { exportRecord.append(","); } }
	 * result.add(exportRecord.toString()); } } dto.setRecords(result);
	 * dto.setTotalAmt(totalAmt);
	 * 
	 * dto = exportExpMain1(result, beginDate, endDate);
	 * 
	 * return dto; }
	 */

	public ExpMainDto exportExpMain(Calendar beginDate, Calendar endDate) {

		ExpMainDto result = new ExpMainDto();

		processExpApplB(result, beginDate, endDate);

		processExpApplC(result, beginDate, endDate);

		processExpApplD(result, beginDate, endDate);

		processExtSysEntry(result, beginDate, endDate);

		return result;
	}

	private void processExpApplB(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT BU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        D.CODE, "); // " 匯款單位", ");
		queryString.append("        D.NAME, "); // " 匯款單位中文", ");
		queryString.append("        U1.CODE, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME "); // " 預算項目中文" ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_B B                                    ");
		queryString.append("			ON B.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("        INNER JOIN TBEXP_USER BU                                       ");
		queryString.append("			ON B.TBEXP_REAMGR_USER_ID||'' = BU.ID                       ");
		queryString.append("		LEFT JOIN TBEXP_DEPARTMENT D                                 ");
		queryString.append("            ON D.ID = B.TBEXP_DEPARTMENT_ID||''                        ");
		queryString.append("        LEFT JOIN TBEXP_USER U1                                      ");
		queryString.append("            ON U1.ID = B.TBEXP_REMIT_USER_ID||''                       ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = B.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = B.TBEXP_PAYMENT_TYPE_ID                                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = B.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = B.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		// 10/07
		queryString.append("        AND MAIN.EXP_APPL_NO NOT LIKE 'T4%'                                                   ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseExportMain(expMainDto, list);

	}

	private void processExpApplC(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT CU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        C.DRAW_MONEY_UNIT_CODE, "); // " 匯款單位", ");
		queryString.append("        C.DRAW_MONEY_UNIT_NAME, "); // " 匯款單位中文",
																// ");
		queryString.append("        INFO1.USER_ID, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME "); // " 預算項目中文" ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_C C                                    ");
		queryString.append("			ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("        INNER JOIN TBEXP_USER CU                                       ");
		queryString.append("			ON CU.ID = C.TBEXP_ACTUAL_VERIFY_USER_ID                    ");
		queryString.append("        LEFT JOIN TBEXP_APPL_INFO INFO1                                ");
		queryString.append("            ON INFO1.ID = TBEXP_DRAW_MONEY_USER_INFO_ID                 ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = C.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = C.TBEXP_PAYMENT_TYPE_ID                                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = C.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = C.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		// 10/07
		queryString.append("        AND MAIN.EXP_APPL_NO NOT LIKE 'T4%'                                                   ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseExportMain(expMainDto, list);
	}

	private void processExpApplD(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT UU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        DD.CODE, "); // " 匯款單位", ");
		queryString.append("        DD.NAME, "); // " 匯款單位中文", ");
		queryString.append("        U1.CODE, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME "); // " 預算項目中文" ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_D D                                    ");
		queryString.append("           ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_MALACC_APPL MAL                      ");
		queryString.append("        	ON MAL.ID = D.ID                                           ");
		queryString.append("        INNER JOIN TBEXP_USER UU                                       ");
		queryString.append("			ON UU.ID = D.TBEXP_REAMGR_USER_ID                           ");
		queryString.append("		LEFT JOIN TBEXP_DEPARTMENT DD                                 ");
		queryString.append("        	ON DD.CODE = MAL.PAYMENT_TARGET_ID                         ");
		queryString.append("        LEFT JOIN TBEXP_USER U1                                       ");
		queryString.append("        	ON U1.CODE = MAL.PAYMENT_TARGET_ID                    ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = DECODE (D.DTYPE, 'ManualAccountAppl', MAL.TBEXP_PAYMENT_TYPE_ID, '') ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = D.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = D.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		// RE201500288_排除大分類16資產區隔(#defect_1651) CU3178 2015/02/26 START
		queryString.append("        INNER JOIN TBEXP_D_CHECK_DETAIL DC ON DC.ID=D.TBEXP_D_CHECK_DETAIL_ID             ");
		queryString.append("        INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID=DC.TBEXP_MIDDLE_TYPE_ID             ");
		queryString.append("        INNER JOIN TBEXP_BIG_TYPE BIG ON BIG.ID=MID.TBEXP_BIG_TYPE_ID             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		// queryString.append("        AND EXP_APPL_NO NOT LIKE 'T4%'                                                   ");
		queryString.append("        AND BIG.CODE !='16'                                                   ");
		// RE201500288_排除大分類16資產區隔(#defect_1651) CU3178 2015/02/26 END
		// RE201300832 CU3178 修改T07的篩選條件 2013/12/10 start
		/*
		 * 由於部門提列應付費用(T07)在日結完專案代號無法寫入子檔中，因此為了顯示專案代號把T07獨立出來，並join部門提列應付費用明細(
		 * TBEXP_DEP_ACCEXP_DETAIL)抓取專案代號
		 * 而子檔(TBEXP_EXP_SUB)由於沒有寫入專案代號，因此在此就不把它join出來
		 */
		/*
		 * 由於一張總帳申請單可能會有多張部門提列應付費用申請單，而在產生分錄的時候是依據相同部門提列應付費用申請單中相同的會計科目才會合在一起，
		 * 因此一張總帳申請單可能會有相同會計科目的
		 * 分錄，所以在JOIN的時候無法正確找到該部門提列應付費用申請單的分錄，因此在這邊只找出所有的明細出來
		 * ，而在下面的語法中是專門找該申請單的貸方分錄
		 */
		// 10/07
		queryString.append("    AND MAIN.EXP_APPL_NO NOT LIKE 'T07%'                                          ");
		queryString.append("     UNION  ALL                                                                 ");
		queryString.append("     SELECT                                                                  ");
		queryString.append("        UU.CODE,                                                             "); // 初審員工代號
		queryString.append("        ' ' , NULL , NULL, NULL, ' ' ,                                       ");
		queryString.append("        MAIN.SUBPOENA_NO,                                                    "); // 傳票號碼
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'),                          "); // 傳票日期
		// 12/11start
		queryString.append("        COALESCE((SELECT DEP.CODE FROM TBEXP_DEPARTMENT DEP WHERE DEP.ID=DETAIL.DEPT_LEVEL1),DETAIL.COST_UNIT_CODE),     "); // 部級代號
		queryString.append("        COALESCE((SELECT DEP.CODE FROM TBEXP_DEPARTMENT DEP WHERE DEP.ID=DETAIL.DEPT_LEVEL2),DETAIL.COST_UNIT_CODE),     "); // 駐區級代號
		// 12/11 end
		queryString.append("        DETAIL.COST_UNIT_CODE,                                               "); // 成本單位
		queryString.append("        ACC.CODE,                                                            "); // 會計科目代號
		queryString.append("        ACC.NAME,                                                            "); // 會計科目中文
		queryString.append("        DETAIL.ESTIMATION_AMT,                                               "); // 金額
		queryString.append("        N'D' AS  ENTRY_VALUE,                                                "); // 該語法只會找借方
		queryString.append("        DETAIL.ESTIMATION_AMT,                                               "); // 調整借貸別金額
		queryString.append("        NULL,                                                                ");
		queryString.append("        MAIN.EXP_APPL_NO,                                                    "); // 費用申請單號
		queryString.append("        MAIN.DELIVER_NO,                                                     "); // 計表單號
		queryString.append("        NULL, NULL, 0, NULL,  0, ' ',  0,  NULL, NULL,  ' ' ,                ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        DETAIL.PROJECT_NO,                                                   "); // 專案代號
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL,                                                    ");
		// 12/11摘要修改START
		queryString.append("        N'提列' || (DEPAPPL.EXP_YEAR-1911) || N'年度應付' || N' '||DETAIL.SUMMARY,     "); // 摘要
		// 12/11摘要修改END
		queryString.append("        NULL,NULL, NULL, NULL, NULL, NULL, NULL, NULL,                       ");
		queryString.append("        U.CODE,                                                              "); // 建檔人員工代號
		queryString.append("        INFO.USER_ID,                                                        "); // 申請人員工代號
		queryString.append("        MAIN.WK_YYMM,                                                        "); // 業績年月
		queryString.append("        MAIN.EXP_YEARS,                                                      "); // 費用年月
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'),                             "); // 匯款日期
		queryString.append("        NULL,                                                                ");
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_level_prop                                   ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_level_prop_id                            ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)),                 "); // 層級屬性
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_type                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_type_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)) ,                "); // 組織型態
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_prop                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_prop_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)) ,                "); // 部門屬性
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_dep_group                                        ");
		queryString.append("       WHERE id = (SELECT TBEXP_dep_group_id                                 ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)),                 "); // 組織群組
		queryString.append("        BUD.CODE,                                                            "); // 預算項目
		queryString.append("     BUD.NAME                                                                "); // 預算項目中文
		queryString.append("     FROM TBEXP_EXPAPPL_D D                                                  ");
		queryString.append("     INNER JOIN TBEXP_DEP_ACCEXP_APPL DEPAPPL                                ");
		queryString.append("        ON  DEPAPPL.TBEXP_EXPAPPL_D_ID=D.ID                                  ");
		queryString.append("     INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL                               ");
		queryString.append("       ON  DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DEPAPPL.ID                        ");
		queryString.append("     INNER JOIN TBEXP_USER UU                                                ");
		queryString.append("         ON UU.ID = D.TBEXP_REAMGR_USER_ID                                   ");
		queryString.append("     INNER JOIN TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("        ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                  ");
		queryString.append("     INNER JOIN TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("        ON ACC.ID = DETAIL.TBEXP_ACC_TITLE_ID                                ");
		queryString.append("     LEFT JOIN TBEXP_USER U                                                  ");
		queryString.append("        ON U.ID = D.TBEXP_CREATE_USER_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_APPL_INFO INFO                                          ");
		queryString.append("        ON INFO.ID = D.TBEXP_APPL_INFO_ID                                    ");
		queryString.append("     LEFT JOIN TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("        ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                    ");
		// 10/07
		queryString.append("     WHERE SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2   AND  MAIN.EXP_APPL_NO  LIKE 'T07%'  ");
		// T07貸方的分錄
		queryString.append("     UNION   ALL                                                                ");
		queryString.append("     SELECT                                                                  ");
		queryString.append("        UU.CODE,                                                             ");// 初審員工代號"
		queryString.append("        ' ' , NULL , NULL, NULL, ' ' ,                                       ");
		queryString.append("        MAIN.SUBPOENA_NO,                                                    ");// 傳票號碼"
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'),                          ");// 傳票日期
		queryString.append("        E.DEP_UNIT_CODE1,                                                    ");// 部級代號"
		queryString.append("        E.DEP_UNIT_CODE2,                                                    ");// 駐區級代號"
		queryString.append("        E.COST_UNIT_CODE,                                                    ");// 成本單位"
		queryString.append("        ACC.CODE,                                                            ");// 會計科目代號"
		queryString.append("        ACC.NAME,                                                            ");// 會計科目中文"
		queryString.append("        E.AMT,                                                               ");// 金額
		queryString.append("        N'C' AS  ENTRY_VALUE,                                                ");// 該語法只會找貸方
		queryString.append("        E.AMT,                                                               ");
		queryString.append("        E.COST_CODE,                                                         ");// --
																											// 成本別"
		queryString.append("        MAIN.EXP_APPL_NO,                                                    ");// --
																											// 費用申請單號"
		queryString.append("        MAIN.DELIVER_NO,                                                     ");// --
																											// 日計表單號"
		queryString.append("        NULL, NULL, 0, NULL,  0, ' ',  0,  NULL, NULL,  ' ' ,                ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL,NULL,                                               ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY,                        ");// --
																											// 摘要"
		queryString.append("        E.INDUSTRY_CODE,                                                     ");// --
																											// 業別代號"
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        U.CODE ,                                                             ");// --
																											// 建檔人員工代號
																											// "
		queryString.append("        INFO.USER_ID,                                                        ");// --
																											// 申請人員工代號"
		queryString.append("        MAIN.WK_YYMM,                                                        ");// --
																											// 業績年月"
		queryString.append("        MAIN.EXP_YEARS,                                                      ");// --
																											// 費用年月"
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'),                             ");// --
																											// 匯款日期"
		queryString.append("        NULL,                                                                ");
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_level_prop                                   ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_level_prop_id                            ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)),                      ");// --
																											// 層級屬性
																											// "
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_type                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_type_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)) ,                     ");// --
																											// 組織型態
																											// "
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_prop                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_prop_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)) ,                     ");// --
																											// 部門屬性"
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_dep_group                                        ");
		queryString.append("       WHERE id = (SELECT TBEXP_dep_group_id                                 ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)),                      ");// --
																											// 組織群組"
		queryString.append("        BUD.CODE,                                                            ");// --
																											// 預算項目
																											// "
		queryString.append("     BUD.NAME                                                                ");
		queryString.append("    FROM TBEXP_EXPAPPL_D D                                                     ");
		queryString.append("     INNER JOIN TBEXP_ENTRY E                                                ");
		queryString.append("      ON E.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID                       ");
		queryString.append("     INNER JOIN TBEXP_USER UU                                                ");
		queryString.append("         ON UU.ID = D.TBEXP_REAMGR_USER_ID                                   ");
		queryString.append("     INNER JOIN TBEXP_EXP_MAIN MAIN                                           ");
		queryString.append("        ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                  ");
		queryString.append("     INNER JOIN TBEXP_ACC_TITLE ACC                                            ");
		queryString.append("        ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_USER U                                                   ");
		queryString.append("        ON U.ID = D.TBEXP_CREATE_USER_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_APPL_INFO INFO                                          ");
		queryString.append("        ON INFO.ID = D.TBEXP_APPL_INFO_ID                                    ");
		queryString.append("     LEFT JOIN TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("        ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                    ");
		queryString.append("     WHERE SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                       ");
		// 10/07
		queryString.append(" AND  MAIN.EXP_APPL_NO  LIKE 'T07%'  AND E.TBEXP_ENTRY_TYPE_ID='12300000-0000-0000-0000-000000000002'");
		// RE201300832 CU3178 修改T07的篩選條件 2013/12/10 end
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseExportMain(expMainDto, list);

	}

	private void processExtSysEntry(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {

		StringBuilder queryString = new StringBuilder();
		queryString.append(" SELECT   NULL,                                                                          ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.SUBPOENA_NO,                                                                 ");
		queryString.append("        TO_CHAR (EXT.SUBPOENA_DATE, 'YYYY/MM/DD'),                                       ");
		queryString.append("        EXT.DEP_UNIT_CODE1,                                                              ");
		queryString.append("        EXT.DEP_UNIT_CODE2,                                                              ");
		queryString.append("        EXT.COST_UNIT_CODE,                                                              ");
		queryString.append("        EXT.ACCT_CODE,                                                                   ");
		queryString.append("        ACC.NAME,                                                                        ");
		queryString.append("        EXT.AMT,                                                                         ");
		queryString.append("        ET.ENTRY_VALUE,                                                                  ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -EXT.AMT                                                                   ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -EXT.AMT                                                                   ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("        END                                                                              ");
		queryString.append("           AMT_MODIFIED,                                                                 ");
		queryString.append("        EXT.COST_CODE,                                                                   ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.COMP_ID,                                                                     ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.PAPERS_NO,                                                                   ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.INSUR_AGENT_CODE,                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.PROJECT_NO,                                                                  ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        CASE WHEN EXT.SUMMARY = ',' THEN N'，' ELSE EXT.SUMMARY END SUMMARY,             ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.GENERAL_MGR_SN,                                                              ");
		queryString.append("        EXT.WK_YYMM,                                                                     ");
		queryString.append("        CONCAT (TO_CHAR (TO_NUMBER (EXP_YYYY) + 1911), EXT.EXP_MM),                      ");
		queryString.append("        TO_CHAR (EXT.REMIT_DATE, 'YYYY/MM/DD'),                                                                  ");
		queryString.append("        EXT.SALES_LINE_CODE,                                                             ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           level_group_name,                                                             ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           dep_type_name,                                                                ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           dep_prop_name,                                                                ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           group_name,                                                                   ");
		queryString.append("        BUD.CODE Budget_Code,                                                            ");
		queryString.append("        BUD.NAME Budget_Name                                                             ");
		queryString.append("   FROM EXPADMIN.TBEXP_EXT_SYS_ENTRY EXT                                                 ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                           ");
		queryString.append("           ON ACC.CODE = EXT.ACCT_CODE                                                   ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = EXT.TBEXP_ENTRY_TYPE_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseExportMain(expMainDto, list);
	}

	private void parseExportMain(ExpMainDto expMainDto, List resultRecords) {

		if (!CollectionUtils.isEmpty(resultRecords)) {
			for (Object obj : resultRecords) {
				Object[] record = (Object[]) obj;
				// 總金額處理
				BigDecimal amt = BigDecimal.ZERO;
				if (record[8] != null) {
					amt = (BigDecimal) record[13];
				}

				String entryTypeValue = (String) record[14];

				if ("D".equals(entryTypeValue)) {
					// totalAmt = totalAmt.add(amt);
					expMainDto.setTotalAmt(expMainDto.getTotalAmt().add(amt));
				} else {
					// totalAmt = totalAmt.subtract(amt);
					expMainDto.setTotalAmt(expMainDto.getTotalAmt().subtract(amt));
				}

				// RE201400382_修改出差報告內容第7點 modify by michael in 2014/02/19 start
				String summary = (String) record[47];
				if (summary != null) {
					// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in
					// 2014/03/14 start
					record[47] = summary.replace(",", " ");
					// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in
					// 2014/03/14 end
				}
				// RE201400382_修改出差報告內容第7點 modify by michael in 2014/02/19 end

				// 匯出字串處理
				StringBuffer exportRecord = new StringBuffer();
				for (int i = 0; i < record.length; i++) {
					Object recObj = record[i];
					if (recObj == null) {
						exportRecord.append("");
					} else {
						if (recObj instanceof Timestamp) {
							Calendar date = Calendar.getInstance();
							date.setTimeInMillis(((Timestamp) recObj).getTime());
							exportRecord.append(DateUtils.getISODateStr(date.getTime(), ""));
						} else if (recObj instanceof BigDecimal) {
							exportRecord.append(((BigDecimal) recObj).toString());
						} else {
							exportRecord.append((String) recObj);
						}
					}
					if (i != record.length - 1) {
						exportRecord.append(",");
					}
				}

				expMainDto.getRecords().add(exportRecord.toString());
			}
		}
	}

	// RE201202140 modify by michael in 2013/06/27 end

	@SuppressWarnings("unchecked")
	public List<ExpMain> findExpMain(Calendar startDate, Calendar endDate) {
		return getJpaTemplate().find("SELECT expMain FROM ExpMain expMain WHERE expMain.subpoenaDate BETWEEN ?1 AND ?2", startDate, endDate);
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findGeneralExpForTableA(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append("SELECT ");
				sb.append("  '總費用明細表' AS REPORTNAME, ");
				sb.append("  'A-XZ0000' AS REPORTCODE, ");
				sb.append("  RT.BTCODE, ");
				sb.append("  RT.BTNAME, ");
				sb.append("  RT.BICODE, ");
				sb.append("  RT.BINAME, ");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT ");
				sb.append("        ABT.CODE AS BTCODE, ");
				sb.append("        ABT.NAME AS BTNAME, ");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, ");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME, ");
				sb.append("        DECODE(MM.XAMT,NULL,0,MM.XAMT) AS MM1AMT, ");
				sb.append("        DECODE(MM.ZAMT,NULL,0,MM.ZAMT) AS MM2AMT, ");
				sb.append("        0 AS MM3AMT, ");
				sb.append("        DECODE(YY.XAMT,NULL,0,YY.XAMT) AS YY1AMT, ");
				sb.append("        DECODE(YY.ZAMT,NULL,0,YY.ZAMT) AS YY2AMT, ");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI ");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID ");
				sb.append("        LEFT  JOIN (SELECT ");
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.XAMT) AS XAMT, ");
				sb.append("                      SUM(MM.ZAMT) AS ZAMT ");
				sb.append("                    FROM (SELECT ");
				sb.append("                            E.ID, ");
				sb.append("                            MAIN.SUBPOENA_DATE, ");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(DEP.FSPCODE,'X',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS XAMT, ");
				sb.append("                            DECODE(DEP.FSPCODE,'Z',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS ZAMT ");
				sb.append("                          FROM TBEXP_ENTRY E ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                              WHERE BIG.CODE != '16') ");
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DEP.CODE AS DEPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN 'Z' ELSE 'X' END AS FSPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN '管理費用' ELSE '業務費用' END AS FSPNAME ");
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                          INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT ");
				sb.append("                            ESE.ID, ");
				sb.append("                            ESE.SUBPOENA_DATE, ");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(DEP.FSPCODE,'X',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS XAMT, ");
				sb.append("                            DECODE(DEP.FSPCODE,'Z',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS ZAMT ");
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DEP.CODE AS DEPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN 'Z' ELSE 'X' END AS FSPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN '管理費用' ELSE '業務費用' END AS FSPNAME ");
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                          INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM ");
				sb.append("                    WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID ");
				sb.append("        LEFT  JOIN (SELECT ");
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.XAMT) AS XAMT, ");
				sb.append("                      SUM(YY.ZAMT) AS ZAMT ");
				sb.append("                    FROM (SELECT ");
				sb.append("                            E.ID, ");
				sb.append("                            MAIN.SUBPOENA_DATE, ");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(DEP.FSPCODE,'X',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS XAMT, ");
				sb.append("                            DECODE(DEP.FSPCODE,'Z',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS ZAMT ");
				sb.append("                          FROM TBEXP_ENTRY E ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                              WHERE BIG.CODE != '16') ");
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DEP.CODE AS DEPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN 'Z' ELSE 'X' END AS FSPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN '管理費用' ELSE '業務費用' END AS FSPNAME ");
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                          INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT ");
				sb.append("                            ESE.ID, ");
				sb.append("                            ESE.SUBPOENA_DATE, ");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(DEP.FSPCODE,'X',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS XAMT, ");
				sb.append("                            DECODE(DEP.FSPCODE,'Z',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS ZAMT ");
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DEP.CODE AS DEPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN 'Z' ELSE 'X' END AS FSPCODE, ");
				sb.append("                                          CASE WHEN PROP.CODE IN ('B','E') THEN '管理費用' ELSE '業務費用' END AS FSPNAME ");
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                          INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY ");
				sb.append("                    WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 ");
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append("GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append("ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findAirportExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出機場展業費用明細表資料(9.1.10) for table a (2010/03/02修訂)
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append("SELECT ");
				sb.append("  '展業費用' || RT.DEPNAME || '總費用明細表' AS REPORTNAME,  "); // --報表名稱固定為展業費用+
																					// 部室名稱
																					// +
																					// 總費用明細表
				sb.append("  CASE WHEN RT.DEPCODE = 'AYE000' THEN 'A-1YYE0'");
				sb.append("       WHEN RT.DEPCODE = 'AYE100' THEN 'A-1YYE1' ");
				sb.append("       WHEN RT.DEPCODE = 'AYH000' THEN 'A-1YYJ0' ");
				sb.append("       ELSE TO_CHAR(RT.DEPCODE) END AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.DEPCODE,");
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT  ");
				sb.append("FROM (SELECT");
				sb.append("        M.DEPCODE, "); // --部門 群組
				sb.append("        M.DEPNAME,");
				sb.append("        M.BTCODE, "); // --預算類別
				sb.append("        M.BTNAME,");
				sb.append("        DECODE(SUBSTR(M.BICODE,1,4),'6007','60070000',M.BICODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(M.BICODE,1,4),'6007','承保佣金支出-其他',M.BINAME) AS BINAME,          ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        0 AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        0 AS YY2AMT,");
				sb.append("        0 AS YY3AMT  ");
				sb.append("      FROM (SELECT ");
				sb.append("              T.DEPCODE, T.DEPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME ");
				sb.append("            FROM (SELECT ");
				sb.append("                    DEP.CODE AS DEPCODE,  "); // --單位
				sb.append("                    DEP.NAME AS DEPNAME   "); // --單位
				sb.append("                  FROM TBEXP_DEPARTMENT DEP");
				sb.append("                    INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                    INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                  WHERE DT.CODE = 'Y'  "); // --團意險部
				sb.append("                    AND DLP.CODE = '1' "); // --部級
				sb.append("                    AND DEP.CODE != 'A0Y000' AND DEP.CODE != '109000') T,  "); // --不包含團體意外險部(109000,A0Y000)
				sb.append("                 (SELECT "); // --預算
				sb.append("                    BI.ID, ");
				sb.append("                    ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("                    ABT.NAME AS BTNAME,");
				sb.append("                    BI.CODE  AS BICODE, "); // --預算項目
				sb.append("                    BI.NAME  AS BINAME       ");
				sb.append("                  FROM TBEXP_BUDGET_ITEM BI ");
				sb.append("                    INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M ");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID,");
				sb.append("                      MM.DEP1CODE, MM.DEP1NAME, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT  ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                            DEP.DEP1CODE, DEP.DEP1NAME, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          MAIN.SUBPOENA_NO ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEP1CODE, ");
				sb.append("                                          DEP.NAME AS DEP1NAME "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE = 'Y'  "); // --團意險部
				sb.append("                                          AND DLP.CODE = '1'  "); // --部級
				sb.append("                                          AND DEP.CODE != 'A0Y000' AND DEP.CODE != '109000'  "); // --不包含團體意外險部(109000,A0Y000)
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEP1CODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DEP.DEP1CODE, DEP.DEP1NAME,                      ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEP1CODE,  "); // --單位
				sb.append("                                          DEP.NAME AS DEP1NAME  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE = 'Y'  "); // --團意險部
				sb.append("                                          AND DLP.CODE = '1'  "); // --部級
				sb.append("                                          AND DEP.CODE != 'A0Y000' AND DEP.CODE != '109000'  "); // --不包含團體意外險部(109000,A0Y000)
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEP1CODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEP1CODE, MM.DEP1NAME) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEP1CODE  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID,");
				sb.append("                      YY.DEP1CODE, YY.DEP1NAME,                  ");
				sb.append("                      SUM(YY.AAMT) AS AAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                            DEP.DEP1CODE, DEP.DEP1NAME, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          MAIN.SUBPOENA_NO  ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEP1CODE,  "); // --單位
				sb.append("                                          DEP.NAME AS DEP1NAME  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE = 'Y'  "); // --團意險部
				sb.append("                                          AND DLP.CODE = '1'  "); // --部級
				sb.append("                                          AND DEP.CODE != 'A0Y000' AND DEP.CODE != '109000'  "); // --不包含團體意外險部(109000,A0Y000)
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEP1CODE  ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DEP.DEP1CODE, DEP.DEP1NAME,                      ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEP1CODE,  "); // --單位
				sb.append("                                          DEP.NAME AS DEP1NAME  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE = 'Y'  "); // --團意險部
				sb.append("                                          AND DLP.CODE = '1'  "); // --部級
				sb.append("                                          AND DEP.CODE != 'A0Y000' AND DEP.CODE != '109000'  "); // --不包含團體意外險部(109000,A0Y000)
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEP1CODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEP1CODE, YY.DEP1NAME) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEP1CODE          ");
				sb.append("      ) RT ");
				sb.append("GROUP BY RT.DEPCODE, RT.DEPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append("ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setMm4Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findBusinessExpForTableA(final Calendar endDate) {
		// D 6.9 找出業務費用明細表資料(9.1.2) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT");
				sb.append("   '業務費用明細表' AS REPORTNAME, ");
				sb.append("   'A-X00000' AS REPORTCODE, ");
				sb.append("   RT.BTCODE,");
				sb.append("   RT.BTNAME,");
				sb.append("   RT.BICODE,");
				sb.append("   RT.BINAME,");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append(" FROM (SELECT");
				sb.append("         ABT.CODE AS BTCODE, ");
				sb.append("         ABT.NAME AS BTNAME,");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME, ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT, ");
				sb.append("         DECODE(MM.DAMT,NULL,0,MM.DAMT) AS MM2AMT, ");
				sb.append("         DECODE(MM.CAMT,NULL,0,MM.CAMT) AS MM3AMT, ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT, ");
				sb.append("         DECODE(YY.DAMT,NULL,0,YY.DAMT) AS YY2AMT, ");
				sb.append("         DECODE(YY.CAMT,NULL,0,YY.CAMT) AS YY3AMT ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("         LEFT  JOIN (SELECT ");
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.DAMT) AS DAMT,");
				sb.append("                       SUM(MM.CAMT) AS CAMT ");
				sb.append("                     FROM (SELECT ");
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DECODE(DEP.PROPCODE,'A',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS AAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'D',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS DAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'C',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS CAMT ");
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID ");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16') ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                         WHERE PROP.CODE IN ('A','C','D')");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                           UNION ");
				sb.append("                           SELECT ");
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DECODE(DEP.PROPCODE,'A',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS AAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'D',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS DAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'C',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS CAMT ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                         WHERE PROP.CODE IN ('A','C','D')");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID ");
				sb.append("         LEFT  JOIN (SELECT ");
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.DAMT) AS DAMT,");
				sb.append("                       SUM(YY.CAMT) AS CAMT ");
				sb.append("                     FROM (SELECT ");
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DECODE(DEP.PROPCODE,'A',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS AAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'D',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS DAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'C',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS CAMT ");
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID ");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16') ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                         WHERE PROP.CODE IN ('A','C','D')");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                           UNION ");
				sb.append("                           SELECT ");
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DECODE(DEP.PROPCODE,'A',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS AAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'D',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS DAMT, ");
				sb.append("                             DECODE(DEP.PROPCODE,'C',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS CAMT ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                         WHERE PROP.CODE IN ('A','C','D')");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 ");
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findChannelExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出通路展業費用明細表資料(9.1.7) for table a。
		// 2010/03/19 變更
		// 總公司費用為抓取成本單位為“金融保險部”、“行銷通路部”的費用(會計科目)
		// 金融保險部：：抓取成本單位“11E000金融保險部”，刪除(但須排除有保代代號的費用)。
		// 行銷通路部：抓取成本單位“11Q000行銷通路部”，刪除(但須排除有保代代號的費用)。
		// 刪除(以下檢核)
		// 分公司費用
		// 金融保險部：抓取成本單位“11E000金融保險部”且有保代代號的費用。
		// 行銷通路部：抓取成本單位“11Q000行銷通路部”且 有保代代號的費用。

		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				// 通路展業費用明細表 (2010/03/05 修正)
				sb.append(" SELECT  ");
				sb.append("   '展業費用' || RT.DEPNAME || '總費用明細表' AS REPORTNAME,   "); // --報表名稱固定為展業費用+
																					// 部室名稱
																					// +
																					// 總費用明細表
				sb.append("   CASE WHEN RT.DEPCODE = '11E000' THEN 'A-11E0' ");
				sb.append("        WHEN RT.DEPCODE = '11Q000' THEN 'A-11Q0' END AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.DEPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT ");
				sb.append("         M.DEPCODE,  "); // --部門 群組
				sb.append("         M.DEPNAME, ");
				sb.append("         M.BTCODE,  "); // --預算類別
				sb.append("         M.BTNAME, ");
				sb.append("         DECODE(SUBSTR(M.BICODE,1,4),'6007','60070000',M.BICODE) AS BICODE,  "); // --預算項目
				sb.append("         DECODE(SUBSTR(M.BICODE,1,4),'6007','承保佣金支出-其他',M.BINAME) AS BINAME,           ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT, ");
				sb.append("         0 AS MM2AMT, ");
				sb.append("         0 AS MM3AMT, ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT, ");
				sb.append("         0 AS YY2AMT, ");
				sb.append("         0 AS YY3AMT  ");
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  ");
				sb.append("                     DEP.CODE AS DEPCODE,  "); // --單位
				sb.append("                     DEP.NAME AS DEPNAME   "); // --單位
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                   WHERE DEP.CODE IN ('11E000','11Q000')) T,  "); // --金融保險部、行銷通路部
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.DEP1CODE, MM.DEP1NAME,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT   ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEP1CODE, DEP.DEP1NAME,  ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE, ");
				sb.append("                                           MAIN.SUBPOENA_NO  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEP1CODE,  ");
				sb.append("                                           DEP.NAME AS DEP1NAME  "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DEP.CODE IN ('11E000','11Q000')                     ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEP1CODE  ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEP1CODE, DEP.DEP1NAME,  ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT  "); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEP1CODE,   "); // --單位
				sb.append("                                           DEP.NAME AS DEP1NAME   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DEP.CODE IN ('11E000','11Q000')      ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEP1CODE) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEP1CODE, MM.DEP1NAME) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEP1CODE    ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.DEP1CODE, YY.DEP1NAME,                   ");
				sb.append("                       SUM(YY.AAMT) AS AAMT                   ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEP1CODE, DEP.DEP1NAME,  ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE, ");
				sb.append("                                           MAIN.SUBPOENA_NO   ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEP1CODE,   "); // --單位
				sb.append("                                           DEP.NAME AS DEP1NAME   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DEP.CODE IN ('11E000','11Q000')                     ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEP1CODE   ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEP1CODE, DEP.DEP1NAME,                       ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT   "); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEP1CODE,  "); // --單位
				sb.append("                                           DEP.NAME AS DEP1NAME   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DEP.CODE IN ('11E000','11Q000')     ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEP1CODE) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEP1CODE, YY.DEP1NAME) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEP1CODE  ");
				sb.append("       ) RT ");
				sb.append(" GROUP BY RT.DEPCODE, RT.DEPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.DEPCODE, RT.BTCODE, RT.BICODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setMm4Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findCrossMarketingForTableA(final Calendar endDate) {
		// D 6.9 找出交叉行銷展業費用明細表資料(9.1.6) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT ");
				sb.append(" '展業費用 交叉行銷 總費用明細表' AS REPORTNAME, "); // --報表名稱
				sb.append(" 'A-11I0' AS REPORTCODE, "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT  ");
				sb.append(" FROM (SELECT ");
				sb.append("    ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("    ABT.NAME AS BTNAME, ");
				sb.append("    DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,  "); // --預算項目
				sb.append("    DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,  "); // --預算項目
				sb.append("    DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT, ");
				sb.append("    0 AS MM2AMT, ");
				sb.append("    0 AS MM3AMT, ");
				sb.append("    DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT, ");
				sb.append("    0 AS YY2AMT, ");
				sb.append("    0 AS YY3AMT  ");
				sb.append("  FROM TBEXP_BUDGET_ITEM BI ");
				sb.append("    INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID ");
				sb.append("    LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("       MM.TBEXP_BUG_ITEM_ID,  ");
				sb.append("       SUM(MM.AAMT) AS AAMT   ");
				sb.append("     FROM (SELECT  "); // --費用系統本月金額
				sb.append("             E.ID, ");
				sb.append("             MAIN.SUBPOENA_DATE, ");
				sb.append("             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("             CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                  ELSE 0 END AS AAMT   "); // --總公司
				sb.append("           FROM TBEXP_ENTRY E ");
				sb.append("             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("             INNER JOIN (SELECT ");
				sb.append("                 MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                 MAIN.SUBPOENA_DATE  ");
				sb.append("               FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("               WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                 IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                       INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                     WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("               ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("             INNER JOIN (SELECT  ");
				sb.append("                 DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                 DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                 DEP.CODE AS DEPCODE   "); // --單位
				sb.append("               FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                 INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                 INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("               ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("           UNION  ");
				sb.append("           SELECT  "); // --外部系統本月金額
				sb.append("             ESE.ID, ");
				sb.append("             ESE.SUBPOENA_DATE, ");
				sb.append("             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("             CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                  ELSE 0 END AS AAMT  "); // --總公司
				sb.append("           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("             INNER JOIN (SELECT  ");
				sb.append("                 DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                 DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                 DEP.CODE AS DEPCODE   "); // --單位
				sb.append("               FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                 INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                 INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("               ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM ");
				sb.append("     WHERE MM.SUBPOENA_DATE  ");
				sb.append("       BETWEEN ?1 AND ?2  "); // --查詢迄日
				sb.append("     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID   ");
				sb.append("    LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("       YY.TBEXP_BUG_ITEM_ID,  ");
				sb.append("       SUM(YY.AAMT) AS AAMT                   ");
				sb.append("     FROM (SELECT  "); // --費用系統本月金額
				sb.append("             E.ID, ");
				sb.append("             MAIN.SUBPOENA_DATE, ");
				sb.append("             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("             CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                  ELSE 0 END AS AAMT  "); // --總公司
				sb.append("           FROM TBEXP_ENTRY E ");
				sb.append("             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("             INNER JOIN (SELECT ");
				sb.append("                MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                MAIN.SUBPOENA_DATE  ");
				sb.append("              FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("              WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                      INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                    WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("              ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("             INNER JOIN (SELECT  ");
				sb.append("                DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                DEP.CODE AS DEPCODE   "); // --單位
				sb.append("              FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("              ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("           UNION  ");
				sb.append("           SELECT  "); // --外部系統本月金額
				sb.append("             ESE.ID, ");
				sb.append("             ESE.SUBPOENA_DATE, ");
				sb.append("             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("             CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                  ELSE 0 END AS AAMT  "); // --總公司
				sb.append("           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("             INNER JOIN (SELECT  ");
				sb.append("                DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                DEP.CODE AS DEPCODE   "); // --單位
				sb.append("              FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("              ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY ");
				sb.append("     WHERE YY.SUBPOENA_DATE  ");
				sb.append("       BETWEEN ?3 AND ?4  "); // --查詢迄日
				sb.append("     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT  ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME   ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findExternalBizPersonnelForTableA(final Calendar endDate) {
		// D 6.9 找出外務人事部展業費用明細表資料(9.1.9) for table a。(2010/03/09修訂)
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append("SELECT");
				sb.append("  '展業費用 外務人事部 總費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-1K0' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,          ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        0 AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        0 AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT   ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '11K000' "); // --外務人事部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '11K000' "); // --外務人事部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?1 AND ?2"); // --查詢迄日
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '11K000' "); // --外務人事部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '11K000' "); // --外務人事部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?3 AND ?4"); // --查詢迄日
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findExternalBizPlanningForTableA(final Calendar endDate) {
		// D 6.9 找出外務企劃部展業費用明細表資料(9.1.5) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				// 外務企劃部展業費用明細表 (2010/03/05修正)
				sb.append("SELECT");
				sb.append("  '展業費用 外務企劃部 總費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-1727' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME, ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT, ");
				sb.append("                      SUM(MM.BAMT) AS BAMT  ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '117000' "); // --外務企劃部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '117000' "); // --外務企劃部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2");
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT, ");
				sb.append("                      SUM(YY.BAMT) AS BAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '117000' "); // --外務企劃部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '117000' "); // --外務企劃部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4");
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findGeneralAffairsForTableA(final Calendar endDate) {
		// D 6.9 找出總務部費用明細表資料(9.1.16) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				// 總務部費用明細表 (2010/03/05 修正)
				sb.append("SELECT");
				sb.append("  '總務部費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-105000' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,          ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        0 AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        0 AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT   ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '105000' "); // --總務部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '105000' "); // --總務部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?1 AND ?2"); // --查詢迄日
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '105000' "); // --總務部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT  "); // --總公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '105000' "); // --總務部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?3 AND ?4"); // --查詢迄日
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findGroupInsuranceExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出團險展業費用明細表資料(9.1.8) for table a。(2010/03/19修訂)
		// D9.1.8團險展業費用明細表
		// 原本規則：
		// 總公司費用
		// a.組織型態：團意險
		// b.層級屬性：部級
		// c.扣除60120000
		// 改為以下：
		// 總公司只抓成本單位為109000的費用，但還是要扣除60120000
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT ");
				sb.append("   '展業費用 團體意外險部 總費用明細表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'A-1Y2Y' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT  ");
				sb.append(" FROM (SELECT ");
				sb.append("         ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("         ABT.NAME AS BTNAME, ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,  "); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,           ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT, ");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT, ");
				sb.append("         0 AS MM3AMT, ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT, ");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT, ");
				sb.append("         0 AS YY3AMT  ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI ");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT,  ");
				sb.append("                       SUM(MM.BAMT) AS BAMT   ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DEPCODE = '109000' AND ACCT.CODE != '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE IN ('2','3') OR ACCT.CODE = '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DT.CODE = 'Y'   "); // --團意險部
				sb.append("                                            OR (DT.CODE = '0' AND DEP.CODE = '109000') ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DEPCODE = '109000' AND ACCT.CODE != '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE IN ('2','3') OR ACCT.CODE = '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DT.CODE = 'Y'   "); // --團意險部
				sb.append("                                            OR (DT.CODE = '0' AND DEP.CODE = '109000') ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID   ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT,  ");
				sb.append("                       SUM(YY.BAMT) AS BAMT                  ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DEPCODE = '109000' AND ACCT.CODE != '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE IN ('2','3') OR ACCT.CODE = '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DT.CODE = 'Y'   "); // --團意險部
				sb.append("                                            OR (DT.CODE = '0' AND DEP.CODE = '109000') ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DEPCODE = '109000' AND ACCT.CODE != '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE IN ('2','3') OR ACCT.CODE = '60120000' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  ");
				sb.append("                                           DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                         WHERE DT.CODE = 'Y'   "); // --團意險部
				sb.append("                                            OR (DT.CODE = '0' AND DEP.CODE = '109000')  ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT  ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME   ");
				sb.append(" ORDER BY RT.BTCODE, RT.BICODE  ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findIncomeExpForTableA(final Calendar endDate) {
		// D 6.9 找出收金費用明細表資料(9.1.15) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT");
				sb.append(" '收金費用明細表' AS REPORTNAME,"); // --報表名稱
				sb.append(" 'A-D00000' AS REPORTCODE,"); // --報表代號
				sb.append("   RT.BTCODE,");
				sb.append("   RT.BTNAME,");
				sb.append("   RT.BICODE,");
				sb.append("   RT.BINAME,");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT");
				sb.append(" FROM (SELECT");
				sb.append("         ABT.CODE AS BTCODE,"); // --預算類別
				sb.append("         ABT.NAME AS BTNAME,");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,"); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,"); // --預算項目
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,"); // --總公司
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,"); // --分公司
				sb.append("         0 AS MM3AMT,");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,"); // --總公司
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,"); // --分公司
				sb.append("         0 AS YY3AMT");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("         LEFT  JOIN (SELECT"); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(MM.AAMT) AS AAMT,");
				sb.append("                       SUM(MM.BAMT) AS BAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ELSE 0 END AS AAMT,"); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN 0 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT"); // --部門屬性
																				// 層級屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE,");
				sb.append("                                           DLP.CODE AS DLPCODE,");
				sb.append("                                           DEP.VIRTUAL_UNIT");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID");
				sb.append("                                         WHERE PROP.CODE = 'D'"); // --收金
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ELSE 0 END AS AAMT,"); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN 0 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT"); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE,");
				sb.append("                                           DLP.CODE AS DLPCODE,");
				sb.append("                                           DEP.VIRTUAL_UNIT");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID");
				sb.append("                                         WHERE PROP.CODE = 'D'"); // --收金
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                     WHERE MM.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID");
				sb.append("         LEFT  JOIN (SELECT"); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(YY.AAMT) AS AAMT,");
				sb.append("                       SUM(YY.BAMT) AS BAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ELSE 0 END AS AAMT,"); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN 0 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT"); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE,");
				sb.append("                                           DLP.CODE AS DLPCODE,");
				sb.append("                                           DEP.VIRTUAL_UNIT");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID");
				sb.append("                                         WHERE PROP.CODE = 'D'"); // --收金
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ELSE 0 END AS AAMT,"); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND DEP.VIRTUAL_UNIT = 0 AND ACCT.CODE != '61130123'");
				sb.append("                                  THEN 0 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT"); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE,");
				sb.append("                                           PROP.CODE AS PROPCODE,");
				sb.append("                                           DLP.CODE AS DLPCODE,");
				sb.append("                                           DEP.VIRTUAL_UNIT");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID");
				sb.append("                                         WHERE PROP.CODE = 'D'"); // --收金
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                     WHERE YY.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findInternationalExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出國際聯保展業費用明細表資料(9.1.12) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT");
				sb.append("   '展業費用 國際聯保(IGP) 總費用明細表' AS REPORTNAME,"); // --報表名稱
				sb.append("   'A-601200' AS REPORTCODE,"); // --報表代號
				sb.append("   RT.BTCODE,");
				sb.append("   RT.BTNAME,");
				sb.append("   RT.BICODE,");
				sb.append("   RT.BINAME,");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT");
				sb.append(" FROM (SELECT");
				sb.append("         ABT.CODE AS BTCODE,"); // --預算類別
				sb.append("         ABT.NAME AS BTNAME,");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,"); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,"); // --預算項目
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("         0 AS MM2AMT,");
				sb.append("         0 AS MM3AMT,");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("         0 AS YY2AMT,");
				sb.append("         0 AS YY3AMT");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("         LEFT  JOIN (SELECT"); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(MM.AAMT) AS AAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ACCT.CODE,'60120000',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = '109000'"); // --團體意外險部
																									// ,2010/03/02
																									// 由A0Y00改為10900
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ACCT.CODE,'60120000',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = 'A0Y000'"); // --團體意外險部
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                     WHERE MM.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID");
				sb.append("         LEFT  JOIN (SELECT"); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(YY.AAMT) AS AAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ACCT.CODE,'60120000',DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT),0) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = 'A0Y000'"); // --團體意外險部
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ACCT.CODE,'60120000',DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT),0) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = 'A0Y000'"); // --團體意外險部
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                     WHERE YY.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findNewContractExpForTableA(final Calendar endDate) {
		// D 6.9 找出新契約費用明細表資料(9.1.14) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT ");
				sb.append("   '展業費用明細表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'A-A00000' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT       ");
				sb.append(" FROM (SELECT ");
				sb.append("         ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("         ABT.NAME AS BTNAME, ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,  "); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,           ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,  "); // --總公司
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,  "); // --分公司
				sb.append("         0 AS MM3AMT,  ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,  "); // --總公司
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,  "); // --分公司
				sb.append("         0 AS YY3AMT   ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI ");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT,  ");
				sb.append("                       SUM(MM.BAMT) AS BAMT                   ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN 0  ");
				sb.append("                                  ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
																				// 層級屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           DLP.CODE AS DLPCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID  ");
				sb.append("                                         WHERE PROP.CODE = 'A'   "); // --新契約
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN 0  ");
				sb.append("                                  ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           DLP.CODE AS DLPCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID  ");
				sb.append("                                         WHERE PROP.CODE = 'A'   "); // --新契約
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE  ");
				sb.append("                       BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT,  ");
				sb.append("                       SUM(YY.BAMT) AS BAMT                  ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN 0  ");
				sb.append("                                  ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           DLP.CODE AS DLPCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID  ");
				sb.append("                                         WHERE PROP.CODE = 'A'   "); // --新契約
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                             CASE WHEN DEP.DLPCODE = '1' AND ACCT.CODE != '61130123' THEN 0  ");
				sb.append("                                  ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS BAMT  "); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
																				// 業務費用
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           DLP.CODE AS DLPCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID =DLP.ID  ");
				sb.append("                                         WHERE PROP.CODE = 'A'   "); // --新契約
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  ");
				sb.append("                       BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT  ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE  ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findProExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出PRO展業費用明細表資料(9.1.11) for table a。
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT");
				sb.append("   '展業費用 PRO業務部 總費用明細表' AS REPORTNAME,"); // --報表名稱
				sb.append("   'A-1P2P' AS REPORTCODE,"); // --報表代號
				sb.append("   RT.BTCODE,");
				sb.append("   RT.BTNAME,");
				sb.append("   RT.BICODE,");
				sb.append("   RT.BINAME,");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT");
				sb.append(" FROM (SELECT");
				sb.append("         ABT.CODE AS BTCODE,"); // --預算類別
				sb.append("         ABT.NAME AS BTNAME,");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,"); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,"); // --預算項目
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,");
				sb.append("         0 AS MM3AMT,");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,");
				sb.append("         0 AS YY3AMT");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("         LEFT  JOIN (SELECT"); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(MM.AAMT) AS AAMT,");
				sb.append("                       SUM(MM.BAMT) AS BAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(DEP.DLPCODE,'1',E.AMT,0) AS AAMT,"); // --總公司
				sb.append("                             DECODE(DEP.DLPCODE,'1',0,E.AMT) AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DT.CODE = 'P'"); // --PRO
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(DEP.DLPCODE,'1',ESE.AMT,0) AS AAMT,"); // --總公司
				sb.append("                             DECODE(DEP.DLPCODE,'1',0,ESE.AMT) AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DT.CODE = 'P'"); // --PRO
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                     WHERE MM.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID");
				sb.append("         LEFT  JOIN (SELECT"); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(YY.AAMT) AS AAMT,");
				sb.append("                       SUM(YY.BAMT) AS BAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(DEP.DLPCODE,'1',E.AMT,0) AS AAMT,"); // --總公司
				sb.append("                             DECODE(DEP.DLPCODE,'1',0,E.AMT) AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DT.CODE = 'P'"); // --PRO
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(DEP.DLPCODE,'1',ESE.AMT,0) AS AAMT,"); // --總公司
				sb.append("                             DECODE(DEP.DLPCODE,'1',0,ESE.AMT) AS BAMT"); // --分公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DT.CODE = 'P'"); // --PRO
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                     WHERE YY.SUBPOENA_DATE");
				sb.append("                       BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findStratum2ExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出二階展業費用明細表資料(9.1.4) for table a。 (2010/03/02修訂)
		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();

				sb.append("SELECT");
				sb.append("  '展業費用 二階 總費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-1S2S' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,          ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT, ");
				sb.append("                      SUM(MM.BAMT) AS BAMT  ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '3' AND (MAIN.MIDCODE = '2H0' OR E.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '3' AND (MAIN.MIDCODE = '2H0' OR E.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          SUBSTR(MAIN.EXP_APPL_NO,1,3) AS MIDCODE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE IN ('2','3') "); // --組織型態為2
																									// 3階
				sb.append("                                          AND DLP.CODE IN ('1','2','3')                     ");
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '3' AND (ESE.SALES_LINE_CODE = '21' OR ESE.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '3' AND (ESE.SALES_LINE_CODE = '21' OR ESE.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE IN ('2','3') "); // --組織型態為2
																									// 3階
				sb.append("                                          AND DLP.CODE IN ('1','2','3')     ");
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?1 AND ?2"); // --查詢迄日
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT, ");
				sb.append("                      SUM(YY.BAMT) AS BAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '3' AND (MAIN.MIDCODE = '2H0' OR E.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '3' AND (MAIN.MIDCODE = '2H0' OR E.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          SUBSTR(MAIN.EXP_APPL_NO,1,3) AS MIDCODE  ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE IN ('2','3') "); // --組織型態為2
																									// 3階
				sb.append("                                          AND DLP.CODE IN ('1','2','3') ");
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE = '1' AND DEP.DTCODE = '3' AND (ESE.SALES_LINE_CODE = '21' OR ESE.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '3' AND (ESE.SALES_LINE_CODE = '21' OR ESE.COST_CODE = 'R') ");
				sb.append("                                 THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --駐區單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DT.CODE IN ('2','3') "); // --組織型態為2
																									// 3階
				sb.append("                                          AND DLP.CODE IN ('1','2','3')    ");
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?3 AND ?4"); // --查詢迄日
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append("GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append("ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findStratum3ExpandBizForTableA(final Calendar endDate) {
		// D 6.9 找出三階展業費用明細表資料(9.1.3) for table a。(2010/03/22修訂)

		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT  ");
				sb.append("   '展業費用' || RT.DEPNAME || '總費用明細表' AS REPORTNAME,   "); // --報表名稱固定為展業費用+
																					// 部室名稱
																					// +
																					// 總費用明細表
				sb.append("   CASE WHEN RT.DEPCODE = 'A0M000' THEN 'A-1M2M' ");
				sb.append("        WHEN RT.DEPCODE = 'A0Z000' THEN 'A-1Z2Z' ");
				sb.append("        WHEN RT.DEPCODE = 'A0B000' THEN 'A-1B2B' ");
				sb.append("        WHEN RT.DEPCODE = 'A0E000' THEN 'A-1E2E' ");
				sb.append("        WHEN RT.DEPCODE = 'A0F000' THEN 'A-1F2F' ");

				/*
				 * RE201401217 靜怡 2014/05/05 start
				 */
				// sb.append("        WHEN RT.DEPCODE = 'A0K000' THEN 'A-1K2K' END AS REPORTCODE,   ");
				// //--報表代號
				sb.append("        WHEN RT.DEPCODE = 'A0K000' THEN 'A-1K2K' "); // --報表代號
				sb.append("       ELSE TO_CHAR(RT.DEPCODE) END AS REPORTCODE,  ");
				/*
				 * RE201401217 靜怡 2014/05/05end
				 */

				sb.append("   RT.DEPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT, ");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE,  "); // --部門 群組
				sb.append("         M.DEPNAME, ");
				sb.append("         M.BTCODE,  "); // --預算類別
				sb.append("         M.BTNAME, ");
				sb.append("         DECODE(SUBSTR(M.BICODE,1,4),'6007','60070000',M.BICODE) AS BICODE,  "); // --預算項目
				sb.append("         DECODE(SUBSTR(M.BINAME,1,4),'6007','承保佣金支出-其他',M.BINAME) AS BINAME,           ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT, ");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,  ");
				sb.append("         0 AS MM3AMT, ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT, ");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT, ");
				sb.append("         0 AS YY3AMT    ");
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --三階部級
				sb.append("                     DEP.CODE AS DEPCODE,  ");
				sb.append("                     DEP.NAME AS DEPNAME ");
				sb.append("                   FROM TBEXP_DEPARTMENT DEP  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                   WHERE DT.CODE = '3' AND DLP.CODE = '1') T,  "); // --組織型態為三階
																								// //--部級
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("             LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                           MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                           MM.DEP1CODE,   ");
				sb.append("                           SUM(MM.AAMT) AS AAMT,  ");
				sb.append("                           SUM(MM.BAMT) AS BAMT   ");
				sb.append("                         FROM (SELECT  "); // --費用系統本月金額
				sb.append("                                 E.ID, ");
				sb.append("                                 MAIN.SUBPOENA_DATE, ");
				sb.append("                                 ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                                 E.DEP_UNIT_CODE1 AS DEP1CODE,   "); // --
																								// 改取分錄中部門代號
				sb.append("                                 DEP.DEP1NAME, ");
				sb.append("                                 CASE WHEN E.COST_CODE = 'R' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                      ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                                 CASE WHEN E.COST_CODE = 'R' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '2' OR DEP.DLPCODE = '3' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                      ELSE 0 END AS BAMT   "); // --分公司
				sb.append("                               FROM TBEXP_ENTRY E ");
				sb.append("                                 INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                                 INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                                 LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                                 INNER JOIN (SELECT ");
				sb.append("                                               MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                               MAIN.SUBPOENA_DATE  ");
				sb.append("                                             FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                             WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                               IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                     INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                                   WHERE BIG.CODE != '16'  "); // --不包含資產區隔的費用
				sb.append("                                                     AND MID.CODE != '2H0')  ");
				sb.append("                                             ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                                 INNER JOIN (SELECT  ");
				sb.append("                                               DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                               DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.CODE,DEP1.CODE) AS DEP1CODE,  "); // --部CODE
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.NAME,DEP1.NAME) AS DEP1NAME,  "); // --部NAME
				sb.append("                                               DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                             FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                               INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEPARTMENT DEP1 ON DEP.TBEXP_DEPARTMENT_ID = DEP1.ID ");
				sb.append("                                             WHERE DT.CODE = '3'  "); // --組織型態為三階
				sb.append("                                               AND DLP.CODE IN ('1','2', '3')                     ");
				sb.append("                                             ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                               UNION  ");
				sb.append("                               SELECT  "); // --外部系統本月金額
				sb.append("                                 ESE.ID, ");
				sb.append("                                 ESE.SUBPOENA_DATE, ");
				sb.append("                                 ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                                 ESE.DEP_UNIT_CODE1 AS DEP1CODE,   "); // --
																									// 改取分錄中部門代號
				sb.append("                                 DEP.DEP1NAME,                     ");
				sb.append("                                 CASE WHEN ESE.COST_CODE = 'R' OR ESE.SALES_LINE_CODE = '21' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                      ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                                 CASE WHEN ESE.COST_CODE = 'R' OR ESE.SALES_LINE_CODE = '21' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '2' OR DEP.DLPCODE = '3' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                      ELSE 0 END AS BAMT   "); // --分公司
				sb.append("                               FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                                 INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                                 INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                                 INNER JOIN (SELECT  ");
				sb.append("                                               DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                               DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.CODE,DEP1.CODE) AS DEP1CODE,  "); // --部
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.NAME,DEP1.NAME) AS DEP1NAME,  "); // --部
				sb.append("                                               DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                             FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                               INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEPARTMENT DEP1 ON DEP.TBEXP_DEPARTMENT_ID = DEP1.ID ");
				sb.append("                                             WHERE DT.CODE = '3'  "); // --組織型態為三階
				sb.append("                                               AND DLP.CODE IN ('1','2' ,'3')     ");
				sb.append("                                             ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM ");
				sb.append("                         WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                         GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEP1CODE) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEP1CODE                  ");
				sb.append("             LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                           YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                           YY.DEP1CODE,                   ");
				sb.append("                           SUM(YY.AAMT) AS AAMT,  ");
				sb.append("                           SUM(YY.BAMT) AS BAMT                  ");
				sb.append("                         FROM (SELECT  "); // --費用系統本月金額
				sb.append("                                 E.ID, ");
				sb.append("                                 MAIN.SUBPOENA_DATE, ");
				sb.append("                                 ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                                 E.DEP_UNIT_CODE1 AS DEP1CODE,   "); // --
																								// 改取分錄中部門代號
				sb.append("                                 DEP.DEP1NAME,  ");
				sb.append("                                 CASE WHEN E.COST_CODE = 'R' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                      ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                                 CASE WHEN E.COST_CODE = 'R' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '2' OR DEP.DLPCODE = '3' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                      ELSE 0 END AS BAMT   "); // --分公司
				sb.append("                               FROM TBEXP_ENTRY E ");
				sb.append("                                 INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                                 INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                                 LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                                 INNER JOIN (SELECT ");
				sb.append("                                               MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                               MAIN.SUBPOENA_DATE  ");
				sb.append("                                             FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                             WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                               IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                     INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                                   WHERE BIG.CODE != '16'  "); // --不包含資產區隔的費用
				sb.append("                                                     AND MID.CODE != '2H0')   ");
				sb.append("                                             ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                                 INNER JOIN (SELECT  ");
				sb.append("                                               DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                               DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.CODE,DEP1.CODE) AS DEP1CODE,  "); // --部
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.NAME,DEP1.NAME) AS DEP1NAME,  "); // --部
				sb.append("                                               DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                             FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                               INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEPARTMENT DEP1 ON DEP.TBEXP_DEPARTMENT_ID = DEP1.ID ");
				sb.append("                                             WHERE DT.CODE = '3'  "); // --組織型態為三階
				sb.append("                                               AND DLP.CODE IN ('1','2' ,'3')                     ");
				sb.append("                                             ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE  ");
				sb.append("                               UNION  ");
				sb.append("                               SELECT  "); // --外部系統本月金額
				sb.append("                                 ESE.ID, ");
				sb.append("                                 ESE.SUBPOENA_DATE, ");
				sb.append("                                 ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                                 ESE.DEP_UNIT_CODE1 AS DEP1CODE,   "); // --
																									// 改取分錄中部門代號
				sb.append("                                 DEP.DEP1NAME,                    ");
				sb.append("                                 CASE WHEN ESE.COST_CODE = 'R' OR ESE.SALES_LINE_CODE = '21' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                      ELSE 0 END AS AAMT,  "); // --總公司
				sb.append("                                 CASE WHEN ESE.COST_CODE = 'R' OR ESE.SALES_LINE_CODE = '21' THEN 0  ");
				sb.append("                                      WHEN DEP.DLPCODE = '2' OR DEP.DLPCODE = '3' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                      ELSE 0 END AS BAMT   "); // --分公司
				sb.append("                               FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                                 INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                                 INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                                 INNER JOIN (SELECT  ");
				sb.append("                                               DT.CODE  AS DTCODE,   "); // --組織型態
				sb.append("                                               DLP.CODE AS DLPCODE,  "); // --層級屬性
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.CODE,DEP1.CODE) AS DEP1CODE,  "); // --部
				sb.append("                                               DECODE(DEP1.CODE,'ROOT',DEP.NAME,DEP1.NAME) AS DEP1NAME,  "); // --部
				sb.append("                                               DEP.CODE AS DEPCODE   "); // --單位
				sb.append("                                             FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                               INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID  ");
				sb.append("                                               INNER JOIN TBEXP_DEPARTMENT DEP1 ON DEP.TBEXP_DEPARTMENT_ID = DEP1.ID ");
				sb.append("                                             WHERE DT.CODE = '3'  "); // --組織型態為三階
				sb.append("                                               AND DLP.CODE IN ('1', '2', '3')    ");
				sb.append("                                             ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY ");
				sb.append("                         WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                         GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEP1CODE) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEP1CODE      ");
				sb.append("       ) RT  ");
				sb.append(" GROUP BY RT.DEPCODE, RT.DEPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY REPORTCODE, RT.DEPCODE, RT.BTCODE, RT.BICODE ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setMm4Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> findExternalBizPersonnel2ForTableA(final Calendar endDate) {
		// D 6.9 找出業務分攤展業費用明細表(9.1.13) for table a。

		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT");
				sb.append("  '展業費用 業務分攤總費用明細表' AS REPORTNAME,"); // --報表名稱
				sb.append("  'A-148000' AS REPORTCODE,"); // --報表代號
				sb.append("   RT.BTCODE,");
				sb.append("   RT.BTNAME,");
				sb.append("   RT.BICODE,");
				sb.append("   RT.BINAME,");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("   SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("   SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT,");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("   SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("   SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT");
				sb.append(" FROM (SELECT");
				sb.append("         ABT.CODE AS BTCODE,"); // --預算類別
				sb.append("         ABT.NAME AS BTNAME,");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,"); // --預算項目
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("         0 AS MM2AMT,");
				sb.append("         0 AS MM3AMT,");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("         0 AS YY2AMT,");
				sb.append("         0 AS YY3AMT");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("         LEFT  JOIN (SELECT"); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(MM.AAMT) AS AAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = '148000'"); // --業務分攤
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = '148000'"); // --業務分攤
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                     WHERE MM.SUBPOENA_DATE "); // --查詢迄日
				sb.append("                       BETWEEN ?1 AND ?2 ");
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID");
				sb.append("         LEFT  JOIN (SELECT"); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,");
				sb.append("                       SUM(YY.AAMT) AS AAMT");
				sb.append("                     FROM (SELECT"); // --費用系統本月金額
				sb.append("                             E.ID,");
				sb.append("                             MAIN.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_ENTRY E");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                           MAIN.SUBPOENA_DATE");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                               WHERE BIG.CODE != '16')"); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = '148000'"); // --業務分攤
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE");
				sb.append("                           UNION");
				sb.append("                           SELECT"); // --外部系統本月金額
				sb.append("                             ESE.ID,");
				sb.append("                             ESE.SUBPOENA_DATE,");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS AAMT"); // --總公司
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE");
				sb.append("                             INNER JOIN (SELECT");
				sb.append("                                           DT.CODE  AS DTCODE,"); // --組織型態
				sb.append("                                           DLP.CODE AS DLPCODE,"); // --層級屬性
				sb.append("                                           DEP.CODE AS DEPCODE"); // --單位
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                           INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID");
				sb.append("                                         WHERE DEP.CODE = '148000'"); // --業務分攤
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                     WHERE YY.SUBPOENA_DATE "); // --查詢迄日
				sb.append("                       BETWEEN ?3 AND ?4 ");
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	// RE201502770_費用系統新增OIU帳冊 CU3178 2015/8/11 START
	@SuppressWarnings("unchecked")
	public List<TableADto> findOIUForTableA(final Calendar endDate) {
		// 找出OIU展業費用明細表資料 for table a。

		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append("SELECT");
				sb.append("  '展業費用國際保險業務分公司總費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-12B' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,          ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT, ");
				sb.append("                      SUM(MM.BAMT) AS BAMT  ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3') AND DEP.DTCODE = '2' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          SUBSTR(MAIN.EXP_APPL_NO,1,3) AS MIDCODE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE ='12B000' "); // --單位代號12B000
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1'  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3')  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE ='12B000' "); // --單位代號12B000
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?1 AND ?2"); // --查詢迄日
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT, ");
				sb.append("                      SUM(YY.BAMT) AS BAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3')  THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE,");
				sb.append("                                          SUBSTR(MAIN.EXP_APPL_NO,1,3) AS MIDCODE  ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE ='12B000' "); // --單位代號12B000
				sb.append("                                        ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN DEP.DLPCODE = '1'  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN DEP.DLPCODE IN ('2','3')  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)");
				sb.append("                                 ELSE 0 END AS BAMT  "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --駐區單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE ='12B000' "); // --單位代號12B000
				sb.append("                                        ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE ");
				sb.append("                      BETWEEN ?3 AND ?4"); // --查詢迄日
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append("GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append("ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableADto> find12A000ForTableA(final Calendar endDate) {
		// 找出12A000展業費用明細表資料 for table a。

		return getJpaTemplate().executeFind(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				StringBuffer sb = new StringBuffer();
				sb.append("SELECT");
				sb.append("  '展業費用 市場行銷部 總費用明細表' AS REPORTNAME,  "); // --報表名稱
				sb.append("  'A-12A000' AS REPORTCODE,  "); // --報表代號
				sb.append("  RT.BTCODE,");
				sb.append("  RT.BTNAME,");
				sb.append("  RT.BICODE,");
				sb.append("  RT.BINAME,");
				sb.append("  SUM(RT.MM1AMT) AS MM1AMT,");
				sb.append("  SUM(RT.MM2AMT) AS MM2AMT,");
				sb.append("  SUM(RT.MM3AMT) AS MM3AMT,");
				sb.append("  SUM(RT.MM1AMT + RT.MM2AMT + RT.MM3AMT) AS MM4AMT, ");
				sb.append("  SUM(RT.YY1AMT) AS YY1AMT,");
				sb.append("  SUM(RT.YY2AMT) AS YY2AMT,");
				sb.append("  SUM(RT.YY3AMT) AS YY3AMT,");
				sb.append("  SUM(RT.YY1AMT + RT.YY2AMT + RT.YY3AMT) AS YY4AMT ");
				sb.append("FROM (SELECT");
				sb.append("        ABT.CODE AS BTCODE, "); // --預算類別
				sb.append("        ABT.NAME AS BTNAME,");
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE, "); // --預算項目
				sb.append("        DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME, ");
				sb.append("        DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,");
				sb.append("        DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,");
				sb.append("        0 AS MM3AMT,");
				sb.append("        DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,");
				sb.append("        DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,");
				sb.append("        0 AS YY3AMT ");
				sb.append("      FROM TBEXP_BUDGET_ITEM BI");
				sb.append("        INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID");
				sb.append("        LEFT  JOIN (SELECT "); // --本月金額
				sb.append("                      MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(MM.AAMT) AS AAMT, ");
				sb.append("                      SUM(MM.BAMT) AS BAMT  ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '12A000' "); // --外務企劃部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '12A000' "); // --外務企劃部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) MM");
				sb.append("                    WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2");
				sb.append("                    GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID  ");
				sb.append("        LEFT  JOIN (SELECT "); // --累積金額
				sb.append("                      YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                      SUM(YY.AAMT) AS AAMT, ");
				sb.append("                      SUM(YY.BAMT) AS BAMT                 ");
				sb.append("                    FROM (SELECT "); // --費用系統本月金額
				sb.append("                            E.ID,");
				sb.append("                            MAIN.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(SUB.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_ENTRY E");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID");
				sb.append("                            LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID  ");
				sb.append("                            INNER JOIN (SELECT");
				sb.append("                                          MAIN.TBEXP_ENTRY_GROUP_ID,");
				sb.append("                                          MAIN.SUBPOENA_DATE ");
				sb.append("                                        FROM TBEXP_EXP_MAIN MAIN");
				sb.append("                                        WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)");
				sb.append("                                          IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID");
				sb.append("                                                INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID");
				sb.append("                                              WHERE BIG.CODE != '16') "); // --不包含資產區隔的費用
				sb.append("                                        ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '12A000' "); // --外務企劃部
				sb.append("                                        ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                          UNION ");
				sb.append("                          SELECT "); // --外部系統本月金額
				sb.append("                            ESE.ID,");
				sb.append("                            ESE.SUBPOENA_DATE,");
				sb.append("                            ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' OR ACCT.CODE = '61130123' THEN 0 ");
				sb.append("                                 ELSE DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) END AS AAMT, "); // --總公司
				sb.append("                            CASE WHEN SUBSTR(ESE.PAPERS_NO,1,3) = '11I' THEN 0 ");
				sb.append("                                 WHEN ACCT.CODE = '61130123' THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) ");
				sb.append("                                 ELSE 0 END AS BAMT "); // --分公司
				sb.append("                          FROM TBEXP_EXT_SYS_ENTRY ESE ");
				sb.append("                            INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                            INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE ");
				sb.append("                            INNER JOIN (SELECT ");
				sb.append("                                          DT.CODE  AS DTCODE,  "); // --組織型態
				sb.append("                                          DLP.CODE AS DLPCODE, "); // --層級屬性
				sb.append("                                          DEP.CODE AS DEPCODE  "); // --單位
				sb.append("                                        FROM TBEXP_DEPARTMENT DEP");
				sb.append("                                          INNER JOIN TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID = DLP.ID ");
				sb.append("                                          INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID ");
				sb.append("                                        WHERE DEP.CODE = '12A000' "); // --外務企劃部
				sb.append("                                        ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE) YY");
				sb.append("                    WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4");
				sb.append("                    GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE ");

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<TableADto> dtos = new ArrayList<TableADto>();
				for (Object[] cols : rows) {
					TableADto dto = new TableADto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setMm4Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	// RE201502770_費用系統新增OIU帳冊 CU3178 2015/8/11 END

	@SuppressWarnings("unchecked")
	public List<TableBDto> findBudgetExecuteForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 預算執行明細表資料(9.2.1) for table b。
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用預算執行明細表' AS REPORTNAME,   "); // --報表名稱固定為為部門屬性
																				// +
																				// 費用預算執行明細表
				sb.append("   'B-' || RT.PROPCODE || '00000' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT     ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("                     DEP.CODE AS DEPCODE,  "); // --單位群組
				sb.append("                     DEP.NAME AS DEPNAME,  "); // --單位群組
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID) T,  ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.DEPCODE, MM.DEPNAME, ");
				sb.append("                       MM.PROPCODE, MM.PROPNAME,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME, ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
				sb.append("                             ESE.SUBPOENA_NO AS SUBPOENA_NO,   "); 
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
				sb.append("                                         AND ESE.SUBPOENA_NO NOT  IN('J827110002','J827115006','J827115009','J827115010')     ");	
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
				
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEPCODE, MM.DEPNAME, MM.PROPCODE, MM.PROPNAME) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.DEPCODE, YY.DEPNAME, ");
				sb.append("                       YY.PROPCODE, YY.PROPNAME,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEPCODE, YY.DEPNAME, YY.PROPCODE, YY.PROPNAME) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.DEPCODE, ZZ.DEPNAME,                       ");
				sb.append("                       ZZ.PROPCODE, ZZ.PROPNAME,  ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.DEPCODE, ZZ.DEPNAME, ZZ.PROPCODE, ZZ.PROPNAME) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.DEPCODE  ");
				sb.append(" ) RT                            ");
				sb.append(" GROUP BY RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.PROPCODE, RT.BTCODE, RT.BICODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findBudgetExecuteGeneralDetailForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 預算執行總明細表資料(9.2.3) for table b。
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT  ");
				sb.append("   '預算執行總明細表' AS REPORTNAME,   "); // --報表名稱固定為預算執行總明細表
				sb.append("   'B-XZ0000' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("                     DEP.CODE AS DEPCODE,  "); // --單位群組
				sb.append("                     DEP.NAME AS DEPNAME,  "); // --單位群組
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID) T,  ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.DEPCODE, MM.DEPNAME, ");
				sb.append("                       MM.PROPCODE, MM.PROPNAME,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME, ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEPCODE, MM.DEPNAME, MM.PROPCODE, MM.PROPNAME) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.DEPCODE, YY.DEPNAME, ");
				sb.append("                       YY.PROPCODE, YY.PROPNAME,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                                         ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEPCODE, YY.DEPNAME, YY.PROPCODE, YY.PROPNAME) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.DEPCODE, ZZ.DEPNAME,                       ");
				sb.append("                       ZZ.PROPCODE, ZZ.PROPNAME,  ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.DEPCODE, ZZ.DEPNAME, ZZ.PROPCODE, ZZ.PROPNAME) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.DEPCODE  ");
				sb.append(" ) RT  ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.BTCODE, RT.BICODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findEconomyInstituteBudgetExecuteForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 經策會預算執行總明細表資料(9.2.6) for table b
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT                                                                                                          ");
				sb.append("   '維持費用 經策會 本部預算執行總明細表' AS REPORTNAME,                                                         ");
				sb.append("   'B-900000' AS REPORTCODE,                                                                                     ");
				sb.append("   RT.BTCODE,                                                                                                    ");
				sb.append("   RT.BTNAME,                                                                                                    ");
				sb.append("   RT.BICODE,                                                                                                    ");
				sb.append("   RT.BINAME,                                                                                                    ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,                                                                                     ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,                                                                                     ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,                                                                         ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,                                                                                     ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,                                                                                     ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT,                                                                         ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT                                                                          ");
				sb.append(" FROM (SELECT                                                                                                    ");
				sb.append("         ABT.CODE AS BTCODE,                                                                                     ");
				sb.append("         ABT.NAME AS BTNAME,                                                                                     ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,                                        ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,                               ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,                                                               ");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,                                                               ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,                                                               ");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,                                                               ");
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT                                                                ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI                                                                                 ");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID                                            ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(MM.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(MM.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) MM                                                                                  ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2                                                    ");
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(YY.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(YY.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,   ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) YY                                                                                  ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4                                ");
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,   ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT                                                          ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) ZZ                                                                                  ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5                                                ");
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID) ZZ ON BI.ID = ZZ.TBEXP_BUG_ITEM_ID) RT                       ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME                                                             ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE                                                                       ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findEconomyInstituteReportForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 經策會報表資料(9.2.4) for table b。
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT                                                                                                          ");
				sb.append("   '維持費用 經策會 本部預算執行明細表' AS REPORTNAME,                                                           ");
				sb.append("   'B-9A0' AS REPORTCODE,                                                                                        ");
				sb.append("   RT.BTCODE,                                                                                                    ");
				sb.append("   RT.BTNAME,                                                                                                    ");
				sb.append("   RT.BICODE,                                                                                                    ");
				sb.append("   RT.BINAME,                                                                                                    ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,                                                                                     ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,                                                                                     ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,                                                                         ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,                                                                                     ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,                                                                                     ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT,                                                                         ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT                                                                          ");
				sb.append(" FROM (SELECT                                                                                                    ");
				sb.append("         ABT.CODE AS BTCODE,                                                                                     ");
				sb.append("         ABT.NAME AS BTNAME,                                                                                     ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,                                        ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,                               ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,                                                               ");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,                                                               ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,                                                               ");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,                                                               ");
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT                                                                ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI                                                                                 ");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID                                            ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(MM.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(MM.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                             WHERE ACCT.CODE IN ('61110105','61110206')                                          ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                             WHERE ACCT.CODE IN ('61110105','61110206')                                          ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,  ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                             WHERE ACCT.CODE = '61110000'                                                        ");
				sb.append("                           ) MM                                                                                  ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2                                                    ");
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(YY.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(YY.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                             WHERE ACCT.CODE IN ('61110105','61110206')                                          ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                             WHERE ACCT.CODE IN ('61110105','61110206')                                          ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,  ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                             WHERE ACCT.CODE = '61110000'                                                        ");
				sb.append("                           ) YY                                                                                  ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4                                                    ");
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT                                                          ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '2'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                             WHERE ACCT.CODE = '61110000'                                                        ");
				sb.append("                           ) ZZ                                                                                  ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5                                                ");
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID) ZZ ON BI.ID = ZZ.TBEXP_BUG_ITEM_ID) RT                       ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME                                                             ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE                                                                       ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findGeneralBudgetExecuteForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 總管理處預算執行明細表資料(9.2.5) for table b
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();
				sb.append(" SELECT                                                                                                          ");
				sb.append("   '維持費用 總管理處 本部預算執行明細表' AS REPORTNAME,                                                         ");
				sb.append("   'B-101000' AS REPORTCODE,                                                                                     ");
				sb.append("   RT.BTCODE,                                                                                                    ");
				sb.append("   RT.BTNAME,                                                                                                    ");
				sb.append("   RT.BICODE,                                                                                                    ");
				sb.append("   RT.BINAME,                                                                                                    ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT,                                                                                     ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT,                                                                                     ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,                                                                         ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT,                                                                                     ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT,                                                                                     ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT,                                                                         ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT                                                                          ");
				sb.append(" FROM (SELECT                                                                                                    ");
				sb.append("         ABT.CODE AS BTCODE,                                                                                     ");
				sb.append("         ABT.NAME AS BTNAME,                                                                                     ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','60070000',BI.CODE) AS BICODE,                                        ");
				sb.append("         DECODE(SUBSTR(BI.CODE,1,4),'6007','承保佣金支出-其他',BI.NAME) AS BINAME,                               ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,                                                               ");
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,                                                               ");
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,                                                               ");
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,                                                               ");
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT                                                                ");
				sb.append("       FROM TBEXP_BUDGET_ITEM BI                                                                                 ");
				sb.append("         INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID                                            ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       MM.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(MM.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(MM.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,  ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) MM                                                                                  ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2                                                    ");
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID) MM ON BI.ID = MM.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       YY.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(YY.AAMT) AS AAMT,                                                                     ");
				sb.append("                       SUM(YY.BAMT) AS BAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             E.ID,                                                                               ");
				sb.append("                             MAIN.SUBPOENA_DATE,                                                                 ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT                         ");
				sb.append("                           FROM TBEXP_ENTRY E                                                                    ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID                   ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID                    ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID,                                            ");
				sb.append("                                           MAIN.SUBPOENA_DATE                                                    ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN                                                ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3)                                      ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID                        ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')                                           ");
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID           ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE                                 ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             ESE.ID,                                                                             ");
				sb.append("                             ESE.SUBPOENA_DATE,                                                                  ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,                                                             ");
				sb.append("                             0 AS AAMT,                                                                          ");
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT                     ");
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE                                                          ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID                  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE                        ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE                               ");
				sb.append("                           UNION                                                                                 ");
				sb.append("                           SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE,   ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,                                                         ");
				sb.append("                             0 AS BAMT                                                                           ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) YY                                                                                  ");
				sb.append("                     WHERE YY.SUBPOENA_DATE BETWEEN ?3 AND ?4                                                    ");
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID) YY ON BI.ID = YY.TBEXP_BUG_ITEM_ID                           ");
				sb.append("         LEFT  JOIN (SELECT                                                                                      ");
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID,                                                                     ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT                                                                      ");
				sb.append("                     FROM (SELECT                                                                                ");
				sb.append("                             MB.ID,                                                                              ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID,                                                               ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT                                                          ");
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB                                                            ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON MB.TBEXP_BUG_ITEM_ID = ACCT.TBEXP_BUG_ITEM_ID    ");
				sb.append("                             INNER JOIN (SELECT                                                                  ");
				sb.append("                                           DEP.CODE AS DEPCODE                                                   ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP                                               ");
				sb.append("                                           INNER JOIN TBEXP_DEP_GROUP DG ON DEP.TBEXP_DEP_GROUP_ID = DG.ID       ");
				sb.append("                                         WHERE DG.CODE = '1'                                                     ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE                                      ");
				sb.append("                           ) ZZ                                                                                  ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5                                                ");
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID) ZZ ON BI.ID = ZZ.TBEXP_BUG_ITEM_ID) RT                       ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME                                                             ");
				sb.append(" ORDER BY REPORTCODE, RT.BTCODE, RT.BICODE                                                                       ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findHQBudgetExecuteReportForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// --9.2.2 本部預算執行報告表 (2010/03/09 修訂)
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用' || RT.DEPNAME || '本部預算執行報告表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'B-' || RT.DEPCODE AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.DEPCODE,  ");
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("                     DEP.CODE AS DEPCODE,  "); // --單位群組
				sb.append("                     DEP.NAME AS DEPNAME,  "); // --單位群組
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID  ");
				sb.append("                   WHERE LEV.CODE = '1') T,  ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.DEPCODE, MM.DEPNAME, ");
				sb.append("                       MM.PROPCODE, MM.PROPNAME,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME, ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') AND DEP.DTCODE = DEP3.DTCODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME, ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,  ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                    ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                    ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') AND DEP.DTCODE = DEP3.DTCODE THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,    ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID    ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                    ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                           ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,    ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.DEPCODE, MM.DEPNAME, MM.PROPCODE, MM.PROPNAME) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.DEPCODE, YY.DEPNAME, ");
				sb.append("                       YY.PROPCODE, YY.PROPNAME,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') AND DEP.DTCODE = DEP3.DTCODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND E.DEP_UNIT_CODE1 = E.COST_UNIT_CODE THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         WHERE SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                           IN (SELECT MID.CODE FROM TBEXP_MIDDLE_TYPE MID ");
				sb.append("                                                 INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ");
				sb.append("                                               WHERE BIG.CODE != '16')  "); // --不包含資產區隔的費用
				sb.append("                                         ) MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,    ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                      ");
				sb.append("                                         ) DEP ON E.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                    ");
				sb.append("                                         ) DEP3 ON E.COST_UNIT_CODE = DEP3.DEPCODE                                          ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             CASE WHEN DEP.DTCODE IN ('0','1') AND DEP.DTCODE = DEP3.DTCODE THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  WHEN DEP.DTCODE IN ('2','3','5','Y') AND ESE.DEP_UNIT_CODE1 = ESE.COST_UNIT_CODE  ");
				sb.append("                                  THEN DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)  ");
				sb.append("                                  ELSE 0 END AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,    ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID   ");
				sb.append("                                         ) DEP ON ESE.DEP_UNIT_CODE1 = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID                    ");
				sb.append("                                         ) DEP3 ON ESE.COST_UNIT_CODE = DEP3.DEPCODE                            ");
				sb.append("                           UNION  ");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME,    ");
				sb.append("                                           DT.CODE AS DTCODE ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                                           INNER JOIN TBEXP_DEP_TYPE DT   ON DEP.TBEXP_DEP_TYPE_ID = DT.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.DEPCODE, YY.DEPNAME, YY.PROPCODE, YY.PROPNAME) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.DEPCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.DEPCODE, ZZ.DEPNAME,                       ");
				sb.append("                       ZZ.PROPCODE, ZZ.PROPNAME,  ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             DEP.DEPCODE, DEP.DEPNAME,                             ");
				sb.append("                             DEP.PROPCODE, DEP.PROPNAME, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.ID,  ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           PROP.CODE AS PROPCODE,  ");
				sb.append("                                           PROP.NAME AS PROPNAME    ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                           INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID    ");
				sb.append("                                         ) DEP ON MB.DEP_CODE = DEP.DEPCODE) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.DEPCODE, ZZ.DEPNAME, ZZ.PROPCODE, ZZ.PROPNAME) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.DEPCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.DEPCODE, RT.DEPNAME, RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.DEPCODE, RT.PROPCODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[4]);
					dto.setABTypeName((String) cols[5]);
					dto.setBudgetItemCode((String) cols[6]);
					dto.setBudgetItemName((String) cols[7]);
					dto.setMm1Amt((BigDecimal) cols[8]);
					dto.setMm2Amt((BigDecimal) cols[9]);
					dto.setMm3Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<LocalSalesmanEncouragementDetailDto> findLocalSalesmanEncouragementDetail(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// D11.3 部室/駐區專案獎勵費 -- 部室/區部專案獎勵明細 下載
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append("SELECT "); // 已核銷明細
				queryString.append("  RAT.MTCODE, ");
				queryString.append("  RAT.COST_UNIT_CODE, ");
				queryString.append("  RAT.COST_UNIT_NAME, ");
				queryString.append("  RAT.WK_YYMM, ");
				queryString.append("  RAT.RATIFY_AMT, ");
				queryString.append("  SUM(RAT.DAMT) DAMT ");
				queryString.append("FROM (SELECT "); // 已核銷明細
				queryString.append("        RATI.WK_YYMM, ");
				queryString.append("        RATI.RATIFY_AMT, ");
				queryString.append("        SUB.SUBPOENA_DATE, "); // 作帳日
				queryString.append("        SUB.SUBPOENA_NO, "); // 傳票號碼
				queryString.append("        B.EXP_APPL_NO, ");
				queryString.append("        B.PROJECT_NO, ");
				queryString.append("        ACCT.CODE AS ACCTCODE, "); // 會計科目代號
				queryString.append("        ACCT.NAME AS ACCTNAME, "); // 會計科目中文
				queryString.append("        BUD.CODE AS BUGCODE, ");
				queryString.append("        BUD.NAME AS BUGNAME, ");
				queryString.append("        E.COST_UNIT_CODE, ");
				queryString.append("        E.COST_UNIT_NAME, "); // 成本單位代號
				queryString.append("        ET.ENTRY_VALUE, ");
				queryString.append("        MT.CODE MTCODE, ");
				queryString.append("        DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT, "); // 借方金額
				queryString.append("        DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT, "); // 貸方金額
				queryString.append("        E.SUMMARY ");
				queryString.append("      FROM TBEXP_ENTRY E  ");
				queryString.append("        INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append("        INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append("        INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("        INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
				queryString.append("        INNER JOIN TBEXP_RATIFY RATI ON RATI.ID=B.TBEXP_RATIFY_ID ");
				queryString.append("        INNER JOIN TBEXP_MIDDLE_TYPE MT ON MT.ID = RATI.TBEXP_MIDDLE_TYPE_ID ");
				queryString.append("        INNER JOIN TBEXP_BUDGET_ITEM BUD ON ACCT.TBEXP_BUG_ITEM_ID = BUD.ID ");
				queryString.append("      WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  ");// 取得核銷資料
				queryString.append("                                          B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append("                                        FROM TBEXP_EXPAPPL_B B  ");
				queryString.append("                                          INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("                                          INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append("                                        WHERE    TBEXP_RATIFY_ID IN (SELECT "); // 駐區業務獎勵費
																													// 核銷代號
				queryString.append("                                                                    RAT.ID  ");
				queryString.append("                                                                  FROM TBEXP_RATIFY RAT  ");
				queryString.append("                                                                    INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
				queryString.append("                                                                  WHERE MID.CODE IN ('2H0','3H0','3C0')) ");
				queryString.append("                                          AND STATE.CODE = '90') "); // 日結
				queryString.append("AND SUB.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");// 前頁參數
				queryString.append("      ) RAT ");
				queryString.append("WHERE RAT.ENTRY_VALUE = 'D' ");
				queryString.append("      GROUP BY RAT.MTCODE,COST_UNIT_CODE,COST_UNIT_NAME,WK_YYMM,RATIFY_AMT ");
				queryString.append("      ORDER BY COST_UNIT_CODE,MTCODE ");

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<LocalSalesmanEncouragementDetailDto> list = new ArrayList<LocalSalesmanEncouragementDetailDto>();
				for (Object[] cols : rows) {
					LocalSalesmanEncouragementDetailDto dto = new LocalSalesmanEncouragementDetailDto();

					dto.setCostUnitCode((String) cols[1]);
					dto.setCostUnitName((String) cols[2]);
					dto.setDamt((BigDecimal) cols[5]);
					dto.setMtcode((String) cols[0]);
					dto.setRatifyAmt((BigDecimal) cols[4]);
					dto.setWkYymm((String) cols[3]);

					list.add(dto);
				}

				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<MealExpenseCancelDetailDto> findMealExpenseCancelDetail(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// D11.3 部室/駐區專案獎勵費 -- 部室/區部專案獎勵明細 下載
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append("SELECT  ");// 已核銷明細
				queryString.append("  RAT.MTCODE, ");
				queryString.append("  RAT.COST_UNIT_CODE, ");
				queryString.append("  RAT.COST_UNIT_NAME, ");
				queryString.append("  RAT.WK_YYMM, ");
				queryString.append("  RAT.SUBPOENA_DATE, ");
				queryString.append("  RAT.PROJECT_NO, ");
				queryString.append("  RAT.EXP_APPL_NO, ");
				queryString.append("  RAT.ACCTCODE, ");
				queryString.append("  RAT.ACCTNAME, ");
				queryString.append("  RAT.MODIAMT, ");
				queryString.append("  RAT.SUMMARY ");
				queryString.append("FROM (SELECT  ");// 已核銷明細
				queryString.append("        RATI.WK_YYMM, ");
				queryString.append("        RATI.RATIFY_AMT, ");
				queryString.append("        SUB.SUBPOENA_DATE,  ");// 作帳日
				queryString.append("        SUB.SUBPOENA_NO,  ");// 傳票號碼
				queryString.append("        B.EXP_APPL_NO, ");
				queryString.append("        B.PROJECT_NO, ");
				queryString.append("        ACCT.CODE AS ACCTCODE,  ");// 會計科目代號
				queryString.append("        ACCT.NAME AS ACCTNAME,  ");// 會計科目中文
				queryString.append("        BUD.CODE AS BUGCODE, ");
				queryString.append("        BUD.NAME AS BUGNAME, ");
				queryString.append("        E.COST_UNIT_CODE, ");
				queryString.append("        E.COST_UNIT_NAME,  ");// 成本單位代號
				queryString.append("        ET.ENTRY_VALUE, ");
				queryString.append("        MT.CODE MTCODE,  ");
				queryString.append("        DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  ");// 借方金額
				queryString.append("        DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  ");// 貸方金額
				queryString.append("        DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS MODIAMT, ");
				queryString.append("        E.SUMMARY ");
				queryString.append("      FROM TBEXP_ENTRY E  ");
				queryString.append("        INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append("        INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append("        INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("        INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
				queryString.append("        INNER JOIN TBEXP_RATIFY RATI ON RATI.ID=B.TBEXP_RATIFY_ID ");
				queryString.append("        INNER JOIN TBEXP_MIDDLE_TYPE MT ON MT.ID = RATI.TBEXP_MIDDLE_TYPE_ID ");
				queryString.append("        INNER JOIN TBEXP_BUDGET_ITEM BUD ON ACCT.TBEXP_BUG_ITEM_ID = BUD.ID ");
				queryString.append("      WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  ");// 取得核銷資料
				queryString.append("                                          B.TBEXP_ENTRY_GROUP_ID  ");
				queryString.append("                                        FROM TBEXP_EXPAPPL_B B  ");
				queryString.append("                                          INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("                                          INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append("                                        WHERE    TBEXP_RATIFY_ID IN (SELECT  ");// 駐區業務獎勵費
																													// 核銷代號
				queryString.append("                                                                    RAT.ID  ");
				queryString.append("                                                                  FROM TBEXP_RATIFY RAT  ");
				queryString.append("                                                                    INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
				queryString.append("                                                                  WHERE MID.CODE IN ('2H0','3H0','3C0')) ");
				queryString.append("                                          AND STATE.CODE = '90')  ");// 日結
				queryString.append("                                          AND BUD.CODE ='61430100' ");
				queryString.append("      AND SUB.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");// 前頁參數
				queryString.append(" ");
				queryString.append("      ) RAT ");
				queryString.append("ORDER BY COST_UNIT_CODE,MTCODE ");

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<MealExpenseCancelDetailDto> list = new ArrayList<MealExpenseCancelDetailDto>();
				for (Object[] cols : rows) {
					MealExpenseCancelDetailDto dto = new MealExpenseCancelDetailDto();

					Timestamp ts = (Timestamp) cols[4];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());

					dto.setMtcode((String) cols[0]);
					dto.setCostUnitCode((String) cols[1]);
					dto.setCostUnitName((String) cols[2]);
					dto.setWkYymm((String) cols[3]);
					dto.setSubpoenaDate(subpoenaDate);
					dto.setProjectNo((String) cols[5]);
					dto.setExpApplNo((String) cols[6]);
					dto.setAcctCode((String) cols[7]);
					dto.setAcctName((String) cols[8]);
					dto.setModiAmt((BigDecimal) cols[9]);
					dto.setSummary((String) cols[10]);

					list.add(dto);
				}

				return list;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<PrizeExpenseCancelDetailDto> findPriZeExpenseCancelDetail(final Calendar startDate, final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// D11.3 部室/駐區專案獎勵費 -- 部室/區部專案獎勵明細 下載
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer queryString = new StringBuffer();
				queryString.append("SELECT  ");// 已核銷明細
				queryString.append("  RAT.MTCODE, ");
				queryString.append("  RAT.COST_UNIT_CODE, ");
				queryString.append("  RAT.COST_UNIT_NAME, ");
				queryString.append("  RAT.WK_YYMM, ");
				queryString.append("  RAT.SUBPOENA_DATE, ");
				queryString.append("  RAT.PROJECT_NO, ");
				queryString.append("  RAT.EXP_APPL_NO, ");
				queryString.append("  RAT.ACCTCODE, ");
				queryString.append("  RAT.ACCTNAME, ");
				queryString.append("  RAT.MODIAMT, ");
				queryString.append("  RAT.SUMMARY ");
				queryString.append("FROM (SELECT  ");// 已核銷明細
				queryString.append("        RATI.WK_YYMM, ");
				queryString.append("        RATI.RATIFY_AMT, ");
				queryString.append("        SUB.SUBPOENA_DATE,  ");// 作帳日
				queryString.append("        SUB.SUBPOENA_NO,  ");// 傳票號碼
				queryString.append("        B.EXP_APPL_NO, ");
				queryString.append("        B.PROJECT_NO, ");
				queryString.append("        ACCT.CODE AS ACCTCODE,  ");// 會計科目代號
				queryString.append("        ACCT.NAME AS ACCTNAME,  ");// 會計科目中文
				queryString.append("        BUD.CODE AS BUGCODE, ");
				queryString.append("        BUD.NAME AS BUGNAME, ");
				queryString.append("        E.COST_UNIT_CODE, ");
				queryString.append("        E.COST_UNIT_NAME,  ");// 成本單位代號
				queryString.append("        ET.ENTRY_VALUE, ");
				queryString.append("        MT.CODE MTCODE,  ");
				queryString.append("        DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  ");// 借方金額
				queryString.append("        DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  ");// 貸方金額
				queryString.append("        DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS MODIAMT, ");
				queryString.append("        E.SUMMARY ");
				queryString.append("      FROM TBEXP_ENTRY E  ");
				queryString.append("        INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				queryString.append("        INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
				queryString.append("        INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("        INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
				queryString.append("        INNER JOIN TBEXP_RATIFY RATI ON RATI.ID=B.TBEXP_RATIFY_ID ");
				queryString.append("        INNER JOIN TBEXP_MIDDLE_TYPE MT ON MT.ID = RATI.TBEXP_MIDDLE_TYPE_ID ");
				queryString.append("        INNER JOIN TBEXP_BUDGET_ITEM BUD ON ACCT.TBEXP_BUG_ITEM_ID = BUD.ID ");
				queryString.append("      WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  ");// 取得核銷資料
				queryString.append("                                          B.TBEXP_ENTRY_GROUP_ID   ");
				queryString.append("                                        FROM TBEXP_EXPAPPL_B B  ");
				queryString.append("                                          INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
				queryString.append("                                          INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
				queryString.append("                                        WHERE    TBEXP_RATIFY_ID IN (SELECT  ");// 駐區業務獎勵費
																													// 核銷代號
				queryString.append("                                                                    RAT.ID  ");
				queryString.append("                                                                  FROM TBEXP_RATIFY RAT  ");
				queryString.append("                                                                    INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
				queryString.append("                                                                  WHERE MID.CODE IN ('2H0','3H0','3C0')) ");
				queryString.append("                                          AND STATE.CODE = '90')  ");// 日結
				queryString.append("      AND BUD.CODE <> '61430100' ");
				queryString.append("      AND SUB.SUBPOENA_DATE BETWEEN ?1 AND ?2 ");// 前頁參數
				queryString.append("      ) RAT ");
				queryString.append("ORDER BY COST_UNIT_CODE,MTCODE ");

				Query query = em.createNativeQuery(queryString.toString());
				query.setParameter(1, startDate, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				List<Object[]> rows = query.getResultList();
				List<PrizeExpenseCancelDetailDto> list = new ArrayList<PrizeExpenseCancelDetailDto>();
				for (Object[] cols : rows) {
					PrizeExpenseCancelDetailDto dto = new PrizeExpenseCancelDetailDto();
					Timestamp ts = (Timestamp) cols[4];
					Calendar subpoenaDate = Calendar.getInstance();
					subpoenaDate.setTimeInMillis(ts.getTime());

					dto.setMtcode((String) cols[0]);
					dto.setCostUnitCode((String) cols[1]);
					dto.setCostUnitName((String) cols[2]);
					dto.setWkYymm((String) cols[3]);
					dto.setSubpoenaDate(subpoenaDate);
					dto.setProjectNo((String) cols[5]);
					dto.setExpApplNo((String) cols[6]);
					dto.setAcctCode((String) cols[7]);
					dto.setAcctName((String) cols[8]);
					dto.setModiAmt((BigDecimal) cols[9]);
					dto.setSummary((String) cols[10]);

					list.add(dto);
				}

				return list;
			}
		});

	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findMealExpenseCancelTotal(Calendar startDate, Calendar endDate) {

		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT  ");
		queryString.append(" nvl(SUM(RAT.MODIAMT),0) MODIAMT ");
		queryString.append(" FROM (SELECT  ");// 已核銷明細
		queryString.append("         RATI.WK_YYMM, ");
		queryString.append("         RATI.RATIFY_AMT, ");
		queryString.append("         SUB.SUBPOENA_DATE,  ");// 作帳日
		queryString.append("         SUB.SUBPOENA_NO,  ");// 傳票號碼
		queryString.append("         B.EXP_APPL_NO, ");
		queryString.append("         B.PROJECT_NO, ");
		queryString.append("         ACCT.CODE AS ACCTCODE,  ");// 會計科目代號
		queryString.append("         ACCT.NAME AS ACCTNAME,  ");// 會計科目中文
		queryString.append("         BUD.CODE AS BUGCODE, ");
		queryString.append("         BUD.NAME AS BUGNAME, ");
		queryString.append("         E.COST_UNIT_CODE, ");
		queryString.append("         E.COST_UNIT_NAME,  ");// 成本單位代號
		queryString.append("         ET.ENTRY_VALUE, ");
		queryString.append("         MT.CODE MTCODE,  ");
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,  ");// 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,  ");// 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS MODIAMT, ");
		queryString.append("         E.SUMMARY ");
		queryString.append("       FROM TBEXP_ENTRY E  ");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
		queryString.append("         INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
		queryString.append("         INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("         INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("         INNER JOIN TBEXP_RATIFY RATI ON RATI.ID=B.TBEXP_RATIFY_ID ");
		queryString.append("         INNER JOIN TBEXP_MIDDLE_TYPE MT ON MT.ID = RATI.TBEXP_MIDDLE_TYPE_ID ");
		queryString.append("         INNER JOIN TBEXP_BUDGET_ITEM BUD ON ACCT.TBEXP_BUG_ITEM_ID = BUD.ID ");
		queryString.append("       WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT  ");// 取得核銷資料
		queryString.append("                                           B.TBEXP_ENTRY_GROUP_ID    ");
		queryString.append("                                         FROM TBEXP_EXPAPPL_B B  ");
		queryString.append("                                           INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID ");
		queryString.append("                                           INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID  ");
		queryString.append("                                         WHERE    TBEXP_RATIFY_ID IN (SELECT  ");// 駐區業務獎勵費
																												// 核銷代號
		queryString.append("                                                                     RAT.ID  ");
		queryString.append("                                                                   FROM TBEXP_RATIFY RAT  ");
		queryString.append("                                                                     INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID  ");
		queryString.append("                                                                   WHERE MID.CODE IN ('2H0','3H0','3C0'))  ");
		queryString.append("                                           AND STATE.CODE = '90')  ");// 日結
		queryString.append("                                           AND BUD.CODE ='61430100' ");
		queryString.append("       AND SUB.SUBPOENA_DATE BETWEEN TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("       ','YYYYMMDD') ");
		queryString.append("       AND TO_DATE(' ");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("       ','YYYYMMDD') ");// 前頁參數
		queryString.append("  ");
		queryString.append("       ) RAT ");
		queryString.append(" ORDER BY COST_UNIT_CODE,MTCODE ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findMealExpenseCancelTotal() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, null };
		return result;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal[] findPrizeExpenseCancelTotal(Calendar startDate, Calendar endDate) {

		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT  ");
		queryString.append(" nvl(SUM(RAT.MODIAMT),0) MODIAMT   ");
		queryString.append(" FROM (SELECT   ");// 已核銷明細
		queryString.append("         RATI.WK_YYMM,  ");
		queryString.append("         RATI.RATIFY_AMT,  ");
		queryString.append("         SUB.SUBPOENA_DATE,   ");// 作帳日
		queryString.append("         SUB.SUBPOENA_NO,   ");// 傳票號碼
		queryString.append("         B.EXP_APPL_NO,  ");
		queryString.append("         B.PROJECT_NO,  ");
		queryString.append("         ACCT.CODE AS ACCTCODE,   ");// 會計科目代號
		queryString.append("         ACCT.NAME AS ACCTNAME,   ");// 會計科目中文
		queryString.append("         BUD.CODE AS BUGCODE,  ");
		queryString.append("         BUD.NAME AS BUGNAME,  ");
		queryString.append("         E.COST_UNIT_CODE,  ");
		queryString.append("         E.COST_UNIT_NAME,   ");// 成本單位代號
		queryString.append("         ET.ENTRY_VALUE,  ");
		queryString.append("         MT.CODE MTCODE,   ");
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,0) AS DAMT,   ");// 借方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'C',E.AMT,0) AS CAMT,   ");// 貸方金額
		queryString.append("         DECODE(ET.ENTRY_VALUE,'D',E.AMT,'C',-1*E.AMT) AS MODIAMT,  ");
		queryString.append("         E.SUMMARY  ");
		queryString.append("       FROM TBEXP_ENTRY E   ");
		queryString.append("         INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID   ");
		queryString.append("         INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID   ");
		queryString.append("         INNER JOIN TBEXP_SUBPOENA SUB ON E.TBEXP_SUBPOENA_ID = SUB.ID  ");
		queryString.append("         INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append("         INNER JOIN TBEXP_RATIFY RATI ON RATI.ID=B.TBEXP_RATIFY_ID  ");
		queryString.append("         INNER JOIN TBEXP_MIDDLE_TYPE MT ON MT.ID = RATI.TBEXP_MIDDLE_TYPE_ID  ");
		queryString.append("         INNER JOIN TBEXP_BUDGET_ITEM BUD ON ACCT.TBEXP_BUG_ITEM_ID = BUD.ID  ");
		queryString.append("       WHERE E.TBEXP_ENTRY_GROUP_ID IN ( SELECT   ");// 取得核銷資料
		queryString.append("                                           B.TBEXP_ENTRY_GROUP_ID    ");
		queryString.append("                                         FROM TBEXP_EXPAPPL_B B   ");
		queryString.append("                                           INNER JOIN TBEXP_SUBPOENA SUB ON B.TBEXP_SUBPOENA_ID = SUB.ID  ");
		queryString.append("                                           INNER JOIN TBEXP_APPL_STATE STATE ON B.TBEXP_APPL_STATE_ID = STATE.ID   ");
		queryString.append("                                         WHERE    TBEXP_RATIFY_ID IN (SELECT   ");// 駐區業務獎勵費
																												// 核銷代號
		queryString.append("                                                                     RAT.ID   ");
		queryString.append("                                                                   FROM TBEXP_RATIFY RAT   ");
		queryString.append("                                                                     INNER JOIN TBEXP_MIDDLE_TYPE MID ON RAT.TBEXP_MIDDLE_TYPE_ID = MID.ID   ");
		queryString.append("                                                                   WHERE MID.CODE IN ('2H0','3H0','3C0'))  ");
		queryString.append("                                           AND STATE.CODE = '90')   ");// 日結
		queryString.append("                                           AND BUD.CODE <> '61430100'  ");

		queryString.append("       AND SUB.SUBPOENA_DATE BETWEEN TO_DATE('");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(startDate.getTime()));
		}
		queryString.append("       ','YYYYMMDD') ");
		queryString.append("       AND TO_DATE(' ");
		if (startDate != null) { // 這裡的查詢日期不應該為空值，前端應該做完檢核。
			queryString.append(DateUtils.getSimpleISODateStr(endDate.getTime()));
		}
		queryString.append("       ','YYYYMMDD') ");// 前頁參數
		queryString.append("       ) RAT  ");
		queryString.append("       ORDER BY COST_UNIT_CODE,MTCODE  ");
		if (logger.isDebugEnabled()) {
			logger.debug(" findPrizeExpenseCancelTotal() SQL = " + queryString.toString());
		}
		List<Object> list = findByNativeSQL(queryString.toString(), null);
		BigDecimal total = BigDecimal.ZERO;
		if (!list.isEmpty()) {
			if (list.get(0) != null) {
				total = (BigDecimal) list.get(0);
				if (total == null) {
					total = BigDecimal.ZERO;
				}
			}
		}
		BigDecimal[] result = { total, null };
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.dao.ExpMainDao#findVoucherDada(java.lang
	 * .String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String)
	 * 
	 * RE201102353 : A new operation for querying data of vouchers. 20120319
	 * CM9539
	 */
	@SuppressWarnings("unchecked")
	public List<VoucherDataDto> findVoucherDada(String voucherNo, String dailyStmtNo, String expApplNo, String invoiceNo, String voucherDate, String userCode) {

		StringBuffer queryString = new StringBuffer();
		queryString.append(" SELECT  ");
		queryString.append(" EM.SUBPOENA_NO,  ");// --傳票號碼
		queryString.append(" EM.SUBPOENA_DATE, ");// --傳票日期
		queryString.append(" EM.DELIVER_NO, ");// --日計表單號
		queryString.append(" EM.EXP_APPL_NO, ");// --費用申請單號
		queryString.append(" ACCT.CODE, ");// --會計科目代碼
		queryString.append(" ACCT.NAME, ");// --會計科目中文
		queryString.append(" ET.ENTRY_VALUE, ");// --借貸別
		queryString.append(" EN.AMT, ");// --金額
		queryString.append(" EN.SUMMARY, ");// --摘要
		queryString.append(" EN.COST_UNIT_CODE, ");// --成本單位代號
		queryString.append(" EN.COST_UNIT_NAME, ");// --成本單位名稱
		queryString.append(" ES.INVOICE_COMP_ID, ");// --開立發票廠商的統一編號
		queryString.append(" ES.INVOICE_COMP_NAME, ");// --發票廠商中文名稱
		queryString.append(" ES.INVOICE_NO, ");// --發票號碼(含字軌)
		queryString.append(" ES.PAPERS_NO, ");// --申請時輸入之文號（發文獎勵費之文號等）
		queryString.append(" ES.PROJECT_NO, ");// --專案代號
		queryString.append(" ES.ROSTER_NO ");// --名冊單號
		queryString.append(" FROM TBEXP_EXP_MAIN EM   ");
		queryString.append(" join TBEXP_ENTRY_GROUP EG ON EM.TBEXP_ENTRY_GROUP_ID=EG.ID  ");
		queryString.append(" join TBEXP_ENTRY EN ON EG.ID=EN.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append(" join TBEXP_ENTRY_TYPE ET ON EN.TBEXP_ENTRY_TYPE_ID=ET.ID  ");
		queryString.append(" join TBEXP_ACC_TITLE ACCT ON EN.TBEXP_ACC_TITLE_ID=ACCT.ID  ");
		queryString.append(" join TBEXP_EXP_SUB ES ON EN.ID=ES.TBEXP_ENTRY_ID  ");

		List<String> ar = new ArrayList<String>();
		if (voucherNo != null && !"".equals(voucherNo)) {
			voucherNo = " EM.SUBPOENA_NO ='" + voucherNo + "' ";
			ar.add(voucherNo);
		}
		if (dailyStmtNo != null && !"".equals(dailyStmtNo)) {
			dailyStmtNo = " EM.DELIVER_NO ='" + dailyStmtNo + "' ";
			ar.add(dailyStmtNo);
		}
		if (expApplNo != null && !"".equals(expApplNo)) {
			expApplNo = " EM.EXP_APPL_NO ='" + expApplNo + "' ";
			ar.add(expApplNo);
		}
		if (voucherDate != null && !"".equals(voucherDate)) {
			voucherDate = " EM.SUBPOENA_DATE  BETWEEN TO_DATE('" + DateUtils.getSimpleISODateStr(DateUtils.getCalendarByYYYYMM(DateUtils.getYYYYMMByRocYYYMM(voucherDate)).getTime()) + "', 'YYYYMMDD') AND TO_DATE('" + DateUtils.getSimpleISODateStr(DateUtils.getLastDayOfMonth(DateUtils.getCalendarByYYYYMM(DateUtils.getYYYYMMByRocYYYMM(voucherDate)).getTime())) + "', 'YYYYMMDD') ";
			ar.add(voucherDate);
		}
		if (userCode != null && !"".equals(userCode)) {
			userCode = " EM.DELIVER_NO like'" + userCode + "%' ";
			ar.add(userCode);
		}
		if (invoiceNo != null && !"".equals(invoiceNo)) {
			invoiceNo = " ES.INVOICE_NO ='" + invoiceNo + "' ";
			ar.add(invoiceNo);
		}

		for (int i = 0; i < ar.size(); i++) {
			if (i == 0) {
				queryString.append(" where " + (String) ar.get(i));
			} else {
				queryString.append(" and " + (String) ar.get(i));
			}
		}
		queryString.append(" ORDER BY EM.SUBPOENA_NO, EM.DELIVER_NO, EM.EXP_APPL_NO, ET.ENTRY_VALUE  ");

		List<Object[]> result = findByNativeSQL(queryString.toString(), null);
		List<VoucherDataDto> list = new ArrayList<VoucherDataDto>();

		for (Object[] cols : result) {
			Calendar subpoenaDate = Calendar.getInstance();// 傳票日期,
															// 因Calendar是Static變數,
															// 故每個迴圈取
			subpoenaDate.setTimeInMillis(((Timestamp) cols[1]).getTime());

			VoucherDataDto dto = new VoucherDataDto();
			dto.setSubpoenaNo((String) cols[0]);
			dto.setSubpoenaDate(subpoenaDate);
			dto.setDeliverNo((String) cols[2]);
			dto.setExpApplNo((String) cols[3]);
			dto.setAcctTitle((String) cols[4]);
			dto.setAcctName((String) cols[5]);
			dto.setEntryValue((String) cols[6]);
			dto.setAmt((BigDecimal) cols[7]);
			dto.setSummary((String) cols[8]);
			dto.setCostUnitCode((String) cols[9]);
			dto.setCostUnitName((String) cols[10]);
			dto.setInvoiceCompId((String) cols[11]);
			dto.setInvoiceCompName((String) cols[12]);
			dto.setInvoiceNo((String) cols[13]);
			dto.setPapersNo((String) cols[14]);
			dto.setProjectNo((String) cols[15]);
			dto.setRosterNo((String) cols[16]);

			list.add(dto);
		}

		return list;
	}

	// RE201201347_B表新增部室外埠二、三階 CU3178 2014/8/5 START
	@SuppressWarnings("unchecked")
	public List<TableBDto> findNewBudgetExecuteForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 改成由預算單位找出實支及預算
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			// 依各單位的屬性來區分
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用預算執行明細表' AS REPORTNAME,    "); // --報表名稱
				sb.append("   'B-' || RT.PROPCODE || '00000' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start		
				sb.append("                             ESE.SUBPOENA_NO,   "); // --預算
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
				sb.append("                                    WHERE ESE.SUBPOENA_NO NOT  IN('J827110002','J827115006','J827115009','J827115010') ");
				//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
				
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START				
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION ALL ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.PROPCODE, RT.BTCODE, RT.BICODE   ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findNewHQBudgetExecuteReportForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 依各預算單位
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用' || RT.DEPNAME || '預算執行報告表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'B-' || RT.DEPCODE AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.DEPCODE,  ");
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.DEPCODE, RT.DEPNAME, RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.DEPCODE, RT.PROPCODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[4]);
					dto.setABTypeName((String) cols[5]);
					dto.setBudgetItemCode((String) cols[6]);
					dto.setBudgetItemName((String) cols[7]);
					dto.setMm1Amt((BigDecimal) cols[8]);
					dto.setMm2Amt((BigDecimal) cols[9]);
					dto.setMm3Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findNewBudgetExecuteGeneralDetailForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 總計
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   '預算執行總明細表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'B-XZ0000' AS REPORTCODE,    "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				// sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID  AND BCODE != '16'  ");
				// sb.append("                                          AND ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE!='63100000' OR BCODE!='15')  ) )  ");
				// //--不包含資產區隔的費用
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE ");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 START
				sb.append("                           UNION  ALL");
				// RE201504772_報表實支撈取規則修正 CU3178 2016/1/18 END
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.BTCODE, RT.BICODE ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	// RE201201347_B表新增部室外埠二、三階 CU3178 2014/8/5 END

	// RE201504772_年度組織控管 CU3178 2016/2/25 START
	@SuppressWarnings("unchecked")
	public List<TableBDto> findYearNewBudgetExecuteForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 改成由預算單位找出實支及預算
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			// 依各單位的屬性來區分
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用預算執行明細表' AS REPORTNAME,    "); // --報表名稱
				sb.append("   'B-' || RT.PROPCODE || '00000' AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                     AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP  ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION ALL ");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.PROPCODE, RT.BTCODE, RT.BICODE   ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[3]);
					dto.setABTypeName((String) cols[4]);
					dto.setBudgetItemCode((String) cols[5]);
					dto.setBudgetItemName((String) cols[6]);
					dto.setMm1Amt((BigDecimal) cols[7]);
					dto.setMm2Amt((BigDecimal) cols[8]);
					dto.setMm3Amt((BigDecimal) cols[9]);
					dto.setYy1Amt((BigDecimal) cols[10]);
					dto.setYy2Amt((BigDecimal) cols[11]);
					dto.setYy3Amt((BigDecimal) cols[12]);
					dto.setYy4Amt((BigDecimal) cols[13]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findYearNewHQBudgetExecuteReportForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 依各預算單位
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   RT.PROPNAME || '費用' || RT.DEPNAME || '預算執行報告表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'B-' || RT.DEPCODE AS REPORTCODE,   "); // --報表代號
				sb.append("   RT.DEPCODE,  ");
				sb.append("   RT.PROPCODE, ");
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                    AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.DEPCODE, RT.DEPNAME, RT.PROPCODE, RT.PROPNAME, RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.DEPCODE, RT.PROPCODE  ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[4]);
					dto.setABTypeName((String) cols[5]);
					dto.setBudgetItemCode((String) cols[6]);
					dto.setBudgetItemName((String) cols[7]);
					dto.setMm1Amt((BigDecimal) cols[8]);
					dto.setMm2Amt((BigDecimal) cols[9]);
					dto.setMm3Amt((BigDecimal) cols[10]);
					dto.setYy1Amt((BigDecimal) cols[11]);
					dto.setYy2Amt((BigDecimal) cols[12]);
					dto.setYy3Amt((BigDecimal) cols[13]);
					dto.setYy4Amt((BigDecimal) cols[14]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<TableBDto> findYearNewBudgetExecuteGeneralDetailForTableB(final Calendar endDate) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			// 總計
			// 修改規則:
			// 1.改成由預算單位來區分預算及實支
			// 2.如果預算單位為101100則分別顯示，例101100、1#0200、1#9B00這三個分別顯示。
			// 3.排除不使用的單位
			// 4.若為外埠單位'A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00'，排除有核定表的實支及月決算中的63100000用品費
			public Object doInJpa(EntityManager em) throws PersistenceException {
				StringBuffer sb = new StringBuffer();

				sb.append(" SELECT  ");
				sb.append("   '預算執行總明細表' AS REPORTNAME,   "); // --報表名稱
				sb.append("   'B-XZ0000' AS REPORTCODE,    "); // --報表代號
				sb.append("   RT.BTCODE, ");
				sb.append("   RT.BTNAME, ");
				sb.append("   RT.BICODE, ");
				sb.append("   RT.BINAME, ");
				sb.append("   SUM(RT.MM1AMT) AS MM1AMT, ");
				sb.append("   SUM(RT.MM2AMT) AS MM2AMT, ");
				sb.append("   SUM(RT.MM1AMT - RT.MM2AMT) AS MM3AMT,  ");
				sb.append("   SUM(RT.YY1AMT) AS YY1AMT, ");
				sb.append("   SUM(RT.YY2AMT) AS YY2AMT, ");
				sb.append("   SUM(RT.YY1AMT - RT.YY2AMT) AS YY3AMT, ");
				sb.append("   SUM(RT.ZZ1AMT - RT.YY2AMT) AS YY4AMT   ");
				sb.append(" FROM (SELECT  ");
				sb.append("         M.DEPCODE, M.DEPNAME, M.PROPCODE, M.PROPNAME, ");
				sb.append("         M.BTCODE, M.BTNAME, M.BICODE, M.BINAME,  ");
				sb.append("         DECODE(MM.AAMT,NULL,0,MM.AAMT) AS MM1AMT,   "); // --月預算
				sb.append("         DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,   "); // --月實支
				sb.append("         DECODE(YY.AAMT,NULL,0,YY.AAMT) AS YY1AMT,   "); // --累計預算
				sb.append("         DECODE(YY.BAMT,NULL,0,YY.BAMT) AS YY2AMT,   "); // --累計實支
				sb.append("         DECODE(ZZ.AAMT,NULL,0,ZZ.AAMT) AS ZZ1AMT    "); // --年度預算
				sb.append("       FROM (SELECT  ");
				sb.append("               T.DEPCODE, T.DEPNAME, T.PROPCODE, T.PROPNAME, ABT.ID, ABT.BTCODE, ABT.BTNAME, ABT.BICODE, ABT.BINAME  ");
				sb.append("             FROM (SELECT  "); // --部門屬性
				sb.append("            	 DISTINCT  ");
				sb.append("                     CASE WHEN  DEP.BUDGET_DEP_CODE='101100' THEN DEP.CODE   ");
				sb.append("                     	 ELSE DEP.BUDGET_DEP_CODE END AS DEPCODE,   "); // --預算代號
				sb.append("                     CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'  ");
				sb.append("                     	 WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'  ");
				sb.append("                    		 WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'  ");
				sb.append("                    		 ELSE DEP.NAME ||'本部' END AS DEPNAME,  "); // --預算代號中文
				sb.append("                     PROP.CODE AS PROPCODE,  "); // --部門屬性群組
				sb.append("                     PROP.NAME AS PROPNAME   "); // --部門屬性群組
				sb.append("                   FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                     INNER JOIN TBEXP_DEP_PROP PROP ON DEP.TBEXP_DEP_PROP_ID = PROP.ID  ");
				sb.append("                     INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID ");
				sb.append("                    WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))   ");
				sb.append("                   AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                     ) T,   ");
				sb.append("                  (SELECT  "); // --預算
				sb.append("                     BI.ID,  ");
				sb.append("                     ABT.CODE AS BTCODE,  "); // --預算類別
				sb.append("                     ABT.NAME AS BTNAME, ");
				sb.append("                     BI.CODE  AS BICODE,  "); // --預算項目
				sb.append("                     BI.NAME  AS BINAME        ");
				sb.append("                   FROM TBEXP_BUDGET_ITEM BI  ");
				sb.append("                     INNER JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID) ABT) M  ");
				sb.append("         LEFT  JOIN (SELECT  "); // --本月金額
				sb.append("                       MM.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       MM.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(MM.AAMT) AS AAMT, ");
				sb.append("                       SUM(MM.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)  AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE, ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) MM ");
				sb.append("                     WHERE MM.SUBPOENA_DATE BETWEEN ?1 AND ?2 "); // --查詢迄日
				sb.append("                     GROUP BY MM.TBEXP_BUG_ITEM_ID, MM.BUDCODE ) MM ON M.ID || M.DEPCODE = MM.TBEXP_BUG_ITEM_ID || MM.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --累積金額
				sb.append("                       YY.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       YY.BUDCODE AS BUDCODE,  ");
				sb.append("                       SUM(YY.AAMT) AS AAMT, ");
				sb.append("                       SUM(YY.BAMT) AS BAMT    ");
				sb.append("                     FROM (SELECT  "); // --費用系統本月金額
				sb.append("                             E.ID, ");
				sb.append("                             MAIN.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS BAMT  "); // --實支
				sb.append("                           FROM TBEXP_ENTRY E ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID = ACCT.ID ");
				sb.append("                             INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID ");
				sb.append("                             LEFT  JOIN TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE, ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON E.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                             INNER JOIN (SELECT ");
				sb.append("                                           MID.CODE AS MCODE, ");
				sb.append("                                           BIG.CODE AS BCODE, ");
				sb.append("                                           MAIN.TBEXP_ENTRY_GROUP_ID, ");
				sb.append("                                           MAIN.SUBPOENA_DATE  ");
				sb.append("                                         FROM TBEXP_EXP_MAIN MAIN ");
				sb.append("                                         INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.CODE=SUBSTR(MAIN.EXP_APPL_NO,1,3) ");
				sb.append("                                         INNER JOIN TBEXP_BIG_TYPE BIG ON MID.TBEXP_BIG_TYPE_ID = BIG.ID ) ");
				sb.append("                                          MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
				sb.append("   										 WHERE  MAIN.BCODE != '16'  AND ");
				sb.append("   										 ( DEP.BUDCODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  OR ");
				sb.append("											 ( (MAIN.BCODE!='00' OR MAIN.MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR MAIN.BCODE!='15' ) AND   ");
				sb.append("   										 (MAIN.MCODE!='A60' OR  E.COST_CODE='W' ) AND (ACCT.CODE!='61130523') AND (B.CODE!='63300000' OR MAIN.MCODE NOT IN ('T05','T12','Q10'))) )   ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT  "); // --外部系統本月金額
				sb.append("                             ESE.ID, ");
				sb.append("                             ESE.SUBPOENA_DATE, ");
				sb.append("                             ACCT.TBEXP_BUG_ITEM_ID,  ");
				sb.append("                             CASE  WHEN DEP.BUDCODE='101100' THEN DEPCODE  ");
				sb.append("                             ELSE DEP.BUDCODE   END AS BUDCODE  ,  ");
				sb.append("                             0 AS AAMT,   "); // --預算
				sb.append("                             DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT)    AS BAMT "); // --實支
				sb.append("                           FROM TBEXP_EXT_SYS_ENTRY ESE  ");
				sb.append("                             INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID  ");
				sb.append("                             INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE  ");
				sb.append("                             INNER JOIN (SELECT  "); // --部門屬性
				sb.append("                                           DEP.YEAR AS YEAR, ");
				sb.append("                                           DEP.CODE AS DEPCODE,  ");
				sb.append("                                           DEP.NAME AS DEPNAME,                                           ");
				sb.append("                                           DEP.BUDGET_DEP_CODE AS BUDCODE  ");
				sb.append("                                         FROM TBEXP_YEAR_DEPARTMENT DEP ");
				sb.append("                                         ) DEP ON ESE.COST_UNIT_CODE = DEP.DEPCODE AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
				sb.append("                           UNION  ALL");
				sb.append("                           SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,          ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT,   "); // --預算
				sb.append("                             0 AS BAMT   "); // --實支
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                           ) YY ");
				sb.append("                     WHERE YY.SUBPOENA_DATE  BETWEEN ?3 AND ?4 "); // --查詢迄日
				sb.append("                     GROUP BY YY.TBEXP_BUG_ITEM_ID, YY.BUDCODE ) YY ON M.ID || M.DEPCODE = YY.TBEXP_BUG_ITEM_ID || YY.BUDCODE ");
				sb.append("         LEFT  JOIN (SELECT  "); // --年度預算金額
				sb.append("                       ZZ.TBEXP_BUG_ITEM_ID, ");
				sb.append("                       ZZ.BUDCODE, ");
				sb.append("                       SUM(ZZ.AAMT) AS AAMT    ");
				sb.append("                     FROM (SELECT   "); // --月預算檔本月金額
				sb.append("                             MB.ID, ");
				sb.append("                             TO_DATE(CONCAT(TO_CHAR(TO_NUMBER(SUBSTR(MB.YYYMM, 1,3)) + 1911) , SUBSTR(MB.YYYMM,4,2)|| '01'),'YYYYMMDD') AS SUBPOENA_DATE, ");
				sb.append("                             MB.TBEXP_BUG_ITEM_ID, ");
				sb.append("                             MB.DEP_CODE AS BUDCODE,   ");
				sb.append("                             MB.BUDGET_ITEM_AMT AS AAMT   "); // --預算
				sb.append("                           FROM TBEXP_MONTH_BUDGET MB  ");
				sb.append("                          ) ZZ ");
				sb.append("                     WHERE TO_CHAR(ZZ.SUBPOENA_DATE, 'YYYY') = ?5 "); // --查詢迄日
				sb.append("                     GROUP BY ZZ.TBEXP_BUG_ITEM_ID, ZZ.BUDCODE) ZZ ON M.ID || M.DEPCODE = ZZ.TBEXP_BUG_ITEM_ID || ZZ.BUDCODE  ");
				sb.append(" ) RT ");
				sb.append(" GROUP BY RT.BTCODE, RT.BTNAME, RT.BICODE, RT.BINAME  ");
				sb.append(" ORDER BY RT.BTCODE, RT.BICODE ");

				Calendar startMonth = (Calendar) endDate.clone();
				startMonth.set(Calendar.DAY_OF_MONTH, 1);

				Calendar startYear = (Calendar) endDate.clone();
				startYear.set(Calendar.MONTH, 0);
				startYear.set(Calendar.DAY_OF_MONTH, 1);

				Query query = em.createNativeQuery(sb.toString());
				query.setParameter(1, startMonth, TemporalType.DATE);
				query.setParameter(2, endDate, TemporalType.DATE);
				query.setParameter(3, startYear, TemporalType.DATE);
				query.setParameter(4, endDate, TemporalType.DATE);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				query.setParameter(5, sdf.format(endDate.getTime()));
				List<Object[]> rows = query.getResultList();
				List<TableBDto> dtos = new ArrayList<TableBDto>();
				for (Object[] cols : rows) {
					TableBDto dto = new TableBDto();
					dto.setReportName((String) cols[0]);
					dto.setReportCode((String) cols[1]);
					dto.setABTypeCode((String) cols[2]);
					dto.setABTypeName((String) cols[3]);
					dto.setBudgetItemCode((String) cols[4]);
					dto.setBudgetItemName((String) cols[5]);
					dto.setMm1Amt((BigDecimal) cols[6]);
					dto.setMm2Amt((BigDecimal) cols[7]);
					dto.setMm3Amt((BigDecimal) cols[8]);
					dto.setYy1Amt((BigDecimal) cols[9]);
					dto.setYy2Amt((BigDecimal) cols[10]);
					dto.setYy3Amt((BigDecimal) cols[11]);
					dto.setYy4Amt((BigDecimal) cols[12]);
					dtos.add(dto);
				}
				return dtos;
			}
		});
	}

	// RE201504772_年度組織控管 CU3178 2016/2/25 END

	// RE201502770_費用系統新增OIU帳冊 CU3178 2015/8/11 START
	public ExpMainDto exportNewExpMain(Calendar beginDate, Calendar endDate) {

		ExpMainDto result = new ExpMainDto();

		processNewExpApplB(result, beginDate, endDate);

		processNewExpApplC(result, beginDate, endDate);

		processNewExpApplD(result, beginDate, endDate);

		processNewExtSysEntry(result, beginDate, endDate);

		return result;
	}

	private void processNewExpApplB(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT         ");
		queryString.append("        BU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        D.CODE, "); // " 匯款單位", ");
		queryString.append("        D.NAME, "); // " 匯款單位中文", ");
		queryString.append("        U1.CODE, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME, "); // " 預算項目中文" ");
		queryString.append("        N'全帳冊'  "); // "費用系統固定為全帳冊", ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_B B                                    ");
		queryString.append("			ON B.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("        INNER JOIN TBEXP_USER BU                                       ");
		queryString.append("			ON B.TBEXP_REAMGR_USER_ID||'' = BU.ID                       ");
		queryString.append("		LEFT JOIN TBEXP_DEPARTMENT D                                 ");
		queryString.append("            ON D.ID = B.TBEXP_DEPARTMENT_ID||''                        ");
		queryString.append("        LEFT JOIN TBEXP_USER U1                                      ");
		queryString.append("            ON U1.ID = B.TBEXP_REMIT_USER_ID||''                       ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = B.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = B.TBEXP_PAYMENT_TYPE_ID                                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = B.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = B.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		queryString.append("        AND MAIN.EXP_APPL_NO NOT LIKE 'T4%'                                                   ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseNewExportMain(expMainDto, list);

	}

	private void processNewExpApplC(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT  ");
		queryString.append(" 		CU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        C.DRAW_MONEY_UNIT_CODE, "); // " 匯款單位", ");
		queryString.append("        C.DRAW_MONEY_UNIT_NAME, "); // " 匯款單位中文",
																// ");
		queryString.append("        INFO1.USER_ID, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME, "); // " 預算項目中文" ");
		queryString.append("        N'全帳冊'  "); // "費用系統固定為全帳冊", ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_C C                                    ");
		queryString.append("			ON C.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID ");
		queryString.append("        INNER JOIN TBEXP_USER CU                                       ");
		queryString.append("			ON CU.ID = C.TBEXP_ACTUAL_VERIFY_USER_ID                    ");
		queryString.append("        LEFT JOIN TBEXP_APPL_INFO INFO1                                ");
		queryString.append("            ON INFO1.ID = TBEXP_DRAW_MONEY_USER_INFO_ID                 ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = C.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = C.TBEXP_PAYMENT_TYPE_ID                                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = C.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = C.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		queryString.append("        AND MAIN.EXP_APPL_NO NOT LIKE 'T4%'                                                   ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseNewExportMain(expMainDto, list);
	}

	private void processNewExpApplD(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {
		// TODO Yunglin
		ExpMainDto dto = new ExpMainDto();
		StringBuffer queryString = new StringBuffer();

		queryString.append(" SELECT  ");
		queryString.append(" 		UU.CODE, "); // " 初審員代", ");
		queryString.append("        ' ' , "); // --EXP.REANAME" 初審姓名", ");
		queryString.append("        DD.CODE, "); // " 匯款單位", ");
		queryString.append("        DD.NAME, "); // " 匯款單位中文", ");
		queryString.append("        U1.CODE, "); // " 受款員代", ");
		queryString.append("        ' ' , "); // --EXP.REMIT_USER_NAME" 受款員代姓名",
												// ");
		queryString.append("        MAIN.SUBPOENA_NO, "); // " 傳票號碼", ");
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'), "); // " 傳票日期",
																					// ");
		queryString.append("        E.DEP_UNIT_CODE1, "); // " 部級代號", ");
		queryString.append("        E.DEP_UNIT_CODE2, "); // " 駐區級代號", ");
		queryString.append("        E.COST_UNIT_CODE, "); // " 成本單位", ");
		queryString.append("        ACC.CODE, "); // " 會計科目代號", ");
		queryString.append("        ACC.NAME, "); // " 會計科目中文", ");
		queryString.append("        E.AMT, "); // " 金額", ");
		queryString.append("        ET.ENTRY_VALUE, "); // " 借貸別", ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -E.AMT                                                                     ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              E.AMT                                                                      ");
		queryString.append("        END                                                                              ");
		queryString.append("          , "); // " 調整借貸別金額", ");
		queryString.append("        E.COST_CODE, "); // " 成本別", ");
		queryString.append("        MAIN.EXP_APPL_NO, "); // " 費用申請單號", ");
		queryString.append("        MAIN.DELIVER_NO, "); // " 日計表單號", ");
		queryString.append("        PT.NAME, "); // " 付款別", ");
		queryString.append("        SUB.STAMP_TAX_CODE, "); // " 印花代號", ");
		queryString.append("        SUB.STAMP_AMT, "); // " 印花金額", ");
		queryString.append("        SUB.TAX_CODE, "); // " 所得進項科目", ");
		queryString.append("        SUB.TAX_AMT, "); // " 所得進項金額", ");
		queryString.append("        ' ' , "); // "--SUB.INCOME_ID 所得人證號", ");
		queryString.append("        SUB.PAY_AMT, "); // " 付款金額", ");
		queryString.append("        SUB.INVOICE_COMP_ID, "); // " 發票廠商統編", ");
		queryString.append("        NVL (SUB.INVOICE_COMP_NAME, ''), "); // " 發票廠商簡稱",
																			// ");
		queryString.append("        ' ' , "); // --SUB.PAY_ID" 受款對象證號", ");
		queryString.append("        NVL (SUB.PAY_COMP_NAME, ''), "); // " 受款廠商簡稱",
																		// ");
		queryString.append("        SUB.INVOICE_NO, "); // " 發票號碼", ");
		queryString.append("        SUB.PAPERS_NO, "); // " 文號", ");
		queryString.append("        SUB.CAR_NO, "); // " 車牌號碼", ");
		queryString.append("        SUB.REPAIR_CODE, "); // " 修繕代碼", ");
		queryString.append("        SUB.INSUR_AGENT_CODE, "); // " 保代代號", ");
		queryString.append("        SUB.ROUTE_TYPE, "); // " 通路別", ");
		queryString.append("        SUB.PROJECT_NO, "); // " 專案代號", ");
		queryString.append("        SUB.CONTRACT_NO, "); // " 合約編號", ");
		queryString.append("        SUB.BUILDING_CODE, "); // " 大樓代號", ");
		queryString.append("        NVL (SUB.BUILDING_NAME, ''), "); // " 大樓名稱",
																		// ");
		queryString.append("        SUB.WARRANTY_YEAR, "); // " 保固年限", ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_START_DATE, 'YYYY/MM/DD'), "); // " 保固起日",
																							// ");
		queryString.append("        TO_CHAR (SUB.WARRANTY_END_DATE, 'YYYY/MM/DD'), "); // " 保固迄日",
																						// ");
		queryString.append("        SUB.THIS_PERIOD_DEGREE, "); // " 本期度數", ");
		queryString.append("        SUB.LAST_PERIOD_DEGREE, "); // " 前期度數", ");
		queryString.append("        TO_CHAR (SUB.ENTERTAIN_DATE, 'YYYY/MM/DD'), "); // " 招待日期",
																					// ");
		queryString.append("        SUB.AMOUNT, "); // " 招待人數", ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY, "); // " 摘要",
																						// ");
		queryString.append("        E.INDUSTRY_CODE, "); // " 業別代號", ");
		queryString.append("        SUB.ADD_EXP_APPL_NO, "); // " 憑證附於申請單號", ");
		queryString.append("        SUB.ROSTER_NO, "); // " 名冊單號", ");
		queryString.append("        SUB.TRAINING_TYPE, "); // " 訓練類別", ");
		queryString.append("        SUB.TRAINING_COMPANY_NAME, "); // " 訓練機構",
																	// ");
		queryString.append("        SUB.TRAINING_COMPANY_COMP_ID, "); // " 訓練廠商統編",
																		// ");
		queryString.append("        NVL (SUB.SUBJECT, ''), "); // " 訓練課程", ");
		queryString.append("        SUB.SYSTEM_TYPE, "); // " 制度別", ");
		queryString.append("        U.CODE, "); // " 建檔人員工代號", ");
		queryString.append("        INFO.USER_ID, "); // " 申請人員工代號", ");
		queryString.append("        MAIN.WK_YYMM, "); // " 業績年月", ");
		queryString.append("        MAIN.EXP_YEARS, "); // " 費用年月", ");
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'), "); // " 匯款日期",
																					// ");
		queryString.append("        NULL, "); // " 業務線代號", ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 層級屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織型態
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 部門屬性
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE))                                   ");
		queryString.append("           ,                                                                     ");// 組織群組
		queryString.append("        BUD.CODE, "); // " 預算項目", ");
		queryString.append("        BUD.NAME, "); // " 預算項目中文" ");
		queryString.append("        N'全帳冊'  "); // "費用系統固定為全帳冊", ");
		queryString.append("   FROM EXPADMIN.TBEXP_ENTRY E                                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_EXP_SUB SUB                                             ");
		queryString.append("           ON SUB.TBEXP_ENTRY_ID = E.ID                                                  ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXPAPPL_D D                                    ");
		queryString.append("           ON D.TBEXP_ENTRY_GROUP_ID = E.TBEXP_ENTRY_GROUP_ID                          ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_MALACC_APPL MAL                      ");
		queryString.append("        	ON MAL.ID = D.ID                                           ");
		queryString.append("        INNER JOIN TBEXP_USER UU                                       ");
		queryString.append("			ON UU.ID = D.TBEXP_REAMGR_USER_ID                           ");
		queryString.append("		LEFT JOIN TBEXP_DEPARTMENT DD                                 ");
		queryString.append("        	ON DD.CODE = MAL.PAYMENT_TARGET_ID                         ");
		queryString.append("        LEFT JOIN TBEXP_USER U1                                       ");
		queryString.append("        	ON U1.CODE = MAL.PAYMENT_TARGET_ID                    ");

		queryString.append("        INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("           ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                         ");
		queryString.append("        INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("           ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_PAYMENT_TYPE PT                                         ");
		queryString.append("           ON PT.ID = DECODE (D.DTYPE, 'ManualAccountAppl', MAL.TBEXP_PAYMENT_TYPE_ID, '') ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = E.TBEXP_ENTRY_TYPE_ID                                              ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_USER U                                                  ");
		queryString.append("           ON U.ID = D.TBEXP_CREATE_USER_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_APPL_INFO INFO                                          ");
		queryString.append("           ON INFO.ID = D.TBEXP_APPL_INFO_ID                                           ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		// RE201500288_排除大分類16資產區隔(#defect_1651) CU3178 2015/02/26 START
		queryString.append("        INNER JOIN TBEXP_D_CHECK_DETAIL DC ON DC.ID=D.TBEXP_D_CHECK_DETAIL_ID             ");
		queryString.append("        INNER JOIN TBEXP_MIDDLE_TYPE MID ON MID.ID=DC.TBEXP_MIDDLE_TYPE_ID             ");
		queryString.append("        INNER JOIN TBEXP_BIG_TYPE BIG ON BIG.ID=MID.TBEXP_BIG_TYPE_ID             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");
		// queryString.append("        AND EXP_APPL_NO NOT LIKE 'T4%'                                                   ");
		queryString.append("        AND BIG.CODE !='16'                                                   ");
		// RE201500288_排除大分類16資產區隔(#defect_1651) CU3178 2015/02/26 END
		// RE201300832 CU3178 修改T07的篩選條件 2013/12/10 start
		/*
		 * 由於部門提列應付費用(T07)在日結完專案代號無法寫入子檔中，因此為了顯示專案代號把T07獨立出來，並join部門提列應付費用明細(
		 * TBEXP_DEP_ACCEXP_DETAIL)抓取專案代號
		 * 而子檔(TBEXP_EXP_SUB)由於沒有寫入專案代號，因此在此就不把它join出來
		 */
		/*
		 * 由於一張總帳申請單可能會有多張部門提列應付費用申請單，而在產生分錄的時候是依據相同部門提列應付費用申請單中相同的會計科目才會合在一起，
		 * 因此一張總帳申請單可能會有相同會計科目的
		 * 分錄，所以在JOIN的時候無法正確找到該部門提列應付費用申請單的分錄，因此在這邊只找出所有的明細出來
		 * ，而在下面的語法中是專門找該申請單的貸方分錄
		 */
		queryString.append("    AND MAIN.EXP_APPL_NO NOT LIKE 'T07%'                                          ");
		queryString.append("     UNION  ALL                                                                 ");
		queryString.append("     SELECT                                                                  ");
		queryString.append("        UU.CODE,                                                             "); // 初審員工代號
		queryString.append("        ' ' , NULL , NULL, NULL, ' ' ,                                       ");
		queryString.append("        MAIN.SUBPOENA_NO,                                                    "); // 傳票號碼
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'),                          "); // 傳票日期
		// 12/11start
		queryString.append("        COALESCE((SELECT DEP.CODE FROM TBEXP_DEPARTMENT DEP WHERE DEP.ID=DETAIL.DEPT_LEVEL1),DETAIL.COST_UNIT_CODE),     "); // 部級代號
		queryString.append("        COALESCE((SELECT DEP.CODE FROM TBEXP_DEPARTMENT DEP WHERE DEP.ID=DETAIL.DEPT_LEVEL2),DETAIL.COST_UNIT_CODE),     "); // 駐區級代號
		// 12/11 end
		queryString.append("        DETAIL.COST_UNIT_CODE,                                               "); // 成本單位
		queryString.append("        ACC.CODE,                                                            "); // 會計科目代號
		queryString.append("        ACC.NAME,                                                            "); // 會計科目中文
		queryString.append("        DETAIL.ESTIMATION_AMT,                                               "); // 金額
		queryString.append("        N'D' AS  ENTRY_VALUE,                                                "); // 該語法只會找借方
		queryString.append("        DETAIL.ESTIMATION_AMT,                                               "); // 調整借貸別金額
		queryString.append("        NULL,                                                                ");
		queryString.append("        MAIN.EXP_APPL_NO,                                                    "); // 費用申請單號
		queryString.append("        MAIN.DELIVER_NO,                                                     "); // 計表單號
		queryString.append("        NULL, NULL, 0, NULL,  0, ' ',  0,  NULL, NULL,  ' ' ,                ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        DETAIL.PROJECT_NO,                                                   "); // 專案代號
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL,                                                    ");
		// 12/11摘要修改START
		queryString.append("        N'提列' || (DEPAPPL.EXP_YEAR-1911) || N'年度應付' || N' '||DETAIL.SUMMARY,     "); // 摘要
		// 12/11摘要修改END
		queryString.append("        NULL,NULL, NULL, NULL, NULL, NULL, NULL, NULL,                       ");
		queryString.append("        U.CODE,                                                              "); // 建檔人員工代號
		queryString.append("        INFO.USER_ID,                                                        "); // 申請人員工代號
		queryString.append("        MAIN.WK_YYMM,                                                        "); // 業績年月
		queryString.append("        MAIN.EXP_YEARS,                                                      "); // 費用年月
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'),                             "); // 匯款日期
		queryString.append("        NULL,                                                                ");
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_level_prop                                   ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_level_prop_id                            ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)),                 "); // 層級屬性
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_type                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_type_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)) ,                "); // 組織型態
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_prop                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_prop_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)) ,                "); // 部門屬性
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_dep_group                                        ");
		queryString.append("       WHERE id = (SELECT TBEXP_dep_group_id                                 ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = DETAIL.COST_UNIT_CODE)),                 "); // 組織群組
		queryString.append("        BUD.CODE,                                                            "); // 預算項目
		queryString.append("     BUD.NAME,                                                                "); // 預算項目中文
		queryString.append("     N'全帳冊'  "); // "費用系統固定為全帳冊", ");
		queryString.append("     FROM TBEXP_EXPAPPL_D D                                                  ");
		queryString.append("     INNER JOIN TBEXP_DEP_ACCEXP_APPL DEPAPPL                                ");
		queryString.append("        ON  DEPAPPL.TBEXP_EXPAPPL_D_ID=D.ID                                  ");
		queryString.append("     INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL                               ");
		queryString.append("       ON  DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DEPAPPL.ID                        ");
		queryString.append("     INNER JOIN TBEXP_USER UU                                                ");
		queryString.append("         ON UU.ID = D.TBEXP_REAMGR_USER_ID                                   ");
		queryString.append("     INNER JOIN TBEXP_EXP_MAIN MAIN                                          ");
		queryString.append("        ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                  ");
		queryString.append("     INNER JOIN TBEXP_ACC_TITLE ACC                                          ");
		queryString.append("        ON ACC.ID = DETAIL.TBEXP_ACC_TITLE_ID                                ");
		queryString.append("     LEFT JOIN TBEXP_USER U                                                  ");
		queryString.append("        ON U.ID = D.TBEXP_CREATE_USER_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_APPL_INFO INFO                                          ");
		queryString.append("        ON INFO.ID = D.TBEXP_APPL_INFO_ID                                    ");
		queryString.append("     LEFT JOIN TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("        ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                    ");
		queryString.append("     WHERE SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2   AND  MAIN.EXP_APPL_NO  LIKE 'T07%'  ");
		// T07貸方的分錄
		queryString.append("     UNION   ALL                                                                ");
		queryString.append("     SELECT                                                                  ");
		queryString.append("        UU.CODE,                                                             ");// 初審員工代號"
		queryString.append("        ' ' , NULL , NULL, NULL, ' ' ,                                       ");
		queryString.append("        MAIN.SUBPOENA_NO,                                                    ");// 傳票號碼"
		queryString.append("        TO_CHAR (MAIN.SUBPOENA_DATE, 'YYYY/MM/DD'),                          ");// 傳票日期
		queryString.append("        E.DEP_UNIT_CODE1,                                                    ");// 部級代號"
		queryString.append("        E.DEP_UNIT_CODE2,                                                    ");// 駐區級代號"
		queryString.append("        E.COST_UNIT_CODE,                                                    ");// 成本單位"
		queryString.append("        ACC.CODE,                                                            ");// 會計科目代號"
		queryString.append("        ACC.NAME,                                                            ");// 會計科目中文"
		queryString.append("        E.AMT,                                                               ");// 金額
		queryString.append("        N'C' AS  ENTRY_VALUE,                                                ");// 該語法只會找貸方
		queryString.append("        E.AMT,                                                               ");
		queryString.append("        E.COST_CODE,                                                         ");// --
																											// 成本別"
		queryString.append("        MAIN.EXP_APPL_NO,                                                    ");// --
																											// 費用申請單號"
		queryString.append("        MAIN.DELIVER_NO,                                                     ");// --
																											// 日計表單號"
		queryString.append("        NULL, NULL, 0, NULL,  0, ' ',  0,  NULL, NULL,  ' ' ,                ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        NULL, NULL, NULL,NULL,                                               ");
		queryString.append("        REPLACE(NVL (E.SUMMARY, ''),',','，') SUMMARY,                        ");// --
																											// 摘要"
		queryString.append("        E.INDUSTRY_CODE,                                                     ");// --
																											// 業別代號"
		queryString.append("        NULL, NULL, NULL, NULL, NULL, NULL, NULL,                            ");
		queryString.append("        U.CODE ,                                                             ");// --
																											// 建檔人員工代號
																											// "
		queryString.append("        INFO.USER_ID,                                                        ");// --
																											// 申請人員工代號"
		queryString.append("        MAIN.WK_YYMM,                                                        ");// --
																											// 業績年月"
		queryString.append("        MAIN.EXP_YEARS,                                                      ");// --
																											// 費用年月"
		queryString.append("        TO_CHAR (MAIN.REMIT_DATE, 'YYYY/MM/DD'),                             ");// --
																											// 匯款日期"
		queryString.append("        NULL,                                                                ");
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_level_prop                                   ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_level_prop_id                            ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)),                      ");// --
																											// 層級屬性
																											// "
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_type                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_type_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)) ,                     ");// --
																											// 組織型態
																											// "
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_DEP_prop                                         ");
		queryString.append("       WHERE id = (SELECT TBEXP_DEP_prop_id                                  ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)) ,                     ");// --
																											// 部門屬性"
		queryString.append("     (SELECT Name                                                            ");
		queryString.append("        FROM EXPADMIN.TBEXP_dep_group                                        ");
		queryString.append("       WHERE id = (SELECT TBEXP_dep_group_id                                 ");
		queryString.append("                     FROM EXPADMIN.TBEXP_DEPARTMENT                          ");
		queryString.append("                       WHERE code = E.COST_UNIT_CODE)),                      ");// --
																											// 組織群組"
		queryString.append("        BUD.CODE,                                                            ");// --
																											// 預算項目
																											// "
		queryString.append("     BUD.NAME,                                                                ");
		queryString.append("     N'全帳冊'  "); // "費用系統固定為全帳冊", ");
		queryString.append("    FROM TBEXP_EXPAPPL_D D                                                     ");
		queryString.append("     INNER JOIN TBEXP_ENTRY E                                                ");
		queryString.append("      ON E.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID                       ");
		queryString.append("     INNER JOIN TBEXP_USER UU                                                ");
		queryString.append("         ON UU.ID = D.TBEXP_REAMGR_USER_ID                                   ");
		queryString.append("     INNER JOIN TBEXP_EXP_MAIN MAIN                                           ");
		queryString.append("        ON MAIN.EXP_APPL_NO = D.EXP_APPL_NO                                  ");
		queryString.append("     INNER JOIN TBEXP_ACC_TITLE ACC                                            ");
		queryString.append("        ON ACC.ID = E.TBEXP_ACC_TITLE_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_USER U                                                   ");
		queryString.append("        ON U.ID = D.TBEXP_CREATE_USER_ID                                     ");
		queryString.append("     LEFT JOIN TBEXP_APPL_INFO INFO                                          ");
		queryString.append("        ON INFO.ID = D.TBEXP_APPL_INFO_ID                                    ");
		queryString.append("     LEFT JOIN TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("        ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                    ");
		queryString.append("     WHERE SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                       ");
		queryString.append(" AND  MAIN.EXP_APPL_NO  LIKE 'T07%'  AND E.TBEXP_ENTRY_TYPE_ID='12300000-0000-0000-0000-000000000002'");
		// RE201300832 CU3178 修改T07的篩選條件 2013/12/10 end
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseNewExportMain(expMainDto, list);

	}

	private void processNewExtSysEntry(ExpMainDto expMainDto, Calendar beginDate, Calendar endDate) {

		StringBuilder queryString = new StringBuilder();

		queryString.append(" SELECT  ");
		queryString.append("        NULL,                                                                          ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.SUBPOENA_NO,                                                                 ");
		queryString.append("        TO_CHAR (EXT.SUBPOENA_DATE, 'YYYY/MM/DD'),                                       ");
		queryString.append("        EXT.DEP_UNIT_CODE1,                                                              ");
		queryString.append("        EXT.DEP_UNIT_CODE2,                                                              ");
		queryString.append("        EXT.COST_UNIT_CODE,                                                              ");
		queryString.append("        EXT.ACCT_CODE,                                                                   ");
		queryString.append("        ACC.NAME,                                                                        ");
		queryString.append("        EXT.AMT,                                                                         ");
		queryString.append("        ET.ENTRY_VALUE,                                                                  ");
		queryString.append("        CASE                                                                             ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'D'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('6', '1', '5')                               ");
		queryString.append("                AND ET.ENTRY_VALUE = 'C'                                                 ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -EXT.AMT                                                                   ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'D'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              -EXT.AMT                                                                   ");
		queryString.append("           WHEN SUBSTR (ACC.CODE, 1, 1) IN ('2', '4') AND ET.ENTRY_VALUE = 'C'           ");
		queryString.append("           THEN                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("           ELSE                                                                          ");
		queryString.append("              EXT.AMT                                                                    ");
		queryString.append("        END                                                                              ");
		queryString.append("           AMT_MODIFIED,                                                                 ");
		queryString.append("        EXT.COST_CODE,                                                                   ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.COMP_ID,                                                                     ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.PAPERS_NO,                                                                   ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.INSUR_AGENT_CODE,                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.PROJECT_NO,                                                                  ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        CASE WHEN EXT.SUMMARY = ',' THEN N'，' ELSE EXT.SUMMARY END SUMMARY,             ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        NULL,                                                                            ");
		queryString.append("        EXT.GENERAL_MGR_SN,                                                              ");
		queryString.append("        EXT.WK_YYMM,                                                                     ");
		queryString.append("        CONCAT (TO_CHAR (TO_NUMBER (EXP_YYYY) + 1911), EXT.EXP_MM),                      ");
		queryString.append("        TO_CHAR (EXT.REMIT_DATE, 'YYYY/MM/DD'),                                                                  ");
		queryString.append("        EXT.SALES_LINE_CODE,                                                             ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_level_prop                                            ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_level_prop_id                                     ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           level_group_name,                                                             ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_type                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_type_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           dep_type_name,                                                                ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_DEP_prop                                                  ");
		queryString.append("          WHERE id = (SELECT TBEXP_DEP_prop_id                                           ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           dep_prop_name,                                                                ");
		queryString.append("        (SELECT Name                                                                     ");
		queryString.append("           FROM EXPADMIN.TBEXP_dep_group                                                 ");
		queryString.append("          WHERE id = (SELECT TBEXP_dep_group_id                                          ");
		queryString.append("                        FROM EXPADMIN.TBEXP_DEPARTMENT                                   ");
		queryString.append("                       WHERE code = EXT.COST_UNIT_CODE))                                 ");
		queryString.append("           group_name,                                                                   ");
		queryString.append("        BUD.CODE Budget_Code,                                                            ");
		queryString.append("        BUD.NAME Budget_Name,                                                             ");
		queryString.append("        ABOOK.NAME  ");
		queryString.append("   FROM EXPADMIN.TBEXP_EXT_SYS_ENTRY EXT                                                 ");
		queryString.append("   	    INNER JOIN TBEXP_ACC_BOOK_TYPE ABOOK ON ABOOK.ID=EXT.TBEXP_ACC_BOOK_TYPE_ID       ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ACC_TITLE ACC                                           ");
		queryString.append("           ON ACC.CODE = EXT.ACCT_CODE                                                   ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET                                           ");
		queryString.append("           ON ET.ID = EXT.TBEXP_ENTRY_TYPE_ID                                            ");
		queryString.append("        LEFT JOIN EXPADMIN.TBEXP_BUDGET_ITEM BUD                                         ");
		queryString.append("           ON ACC.TBEXP_BUG_ITEM_ID = BUD.ID                                             ");
		queryString.append("  WHERE     SUBPOENA_DATE >= ?1 AND SUBPOENA_DATE <= ?2                            ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(beginDate);
		parameters.add(endDate);
		List list = findByNativeSQL(queryString.toString(), parameters);

		parseExportMain(expMainDto, list);
	}

	private void parseNewExportMain(ExpMainDto expMainDto, List resultRecords) {

		if (!CollectionUtils.isEmpty(resultRecords)) {
			for (Object obj : resultRecords) {
				Object[] record = (Object[]) obj;
				// 總金額處理
				BigDecimal amt = BigDecimal.ZERO;
				if (record[8] != null) {
					amt = (BigDecimal) record[13];
				}

				String entryTypeValue = (String) record[14];

				if ("D".equals(entryTypeValue)) {
					// totalAmt = totalAmt.add(amt);
					expMainDto.setTotalAmt(expMainDto.getTotalAmt().add(amt));
				} else {
					// totalAmt = totalAmt.subtract(amt);
					expMainDto.setTotalAmt(expMainDto.getTotalAmt().subtract(amt));
				}
				String summary = (String) record[47];
				if (summary != null) {
					record[47] = summary.replace(",", " ");
				}
				// 匯出字串處理
				StringBuffer exportRecord = new StringBuffer();
				for (int i = 0; i < record.length; i++) {
					Object recObj = record[i];
					if (recObj == null) {
						exportRecord.append("");
					} else {
						if (recObj instanceof Timestamp) {
							Calendar date = Calendar.getInstance();
							date.setTimeInMillis(((Timestamp) recObj).getTime());
							exportRecord.append(DateUtils.getISODateStr(date.getTime(), ""));
						} else if (recObj instanceof BigDecimal) {
							exportRecord.append(((BigDecimal) recObj).toString());
						} else {
							exportRecord.append((String) recObj);
						}
					}
					if (i != record.length - 1) {
						exportRecord.append(",");
					}
				}

				expMainDto.getRecords().add(exportRecord.toString());
			}
		}
	}

	// RE201502770_費用系統新增OIU帳冊 CU3178 2015/8/11 END

	// RE201503701_關係人交易明細查詢功能優化 CU3178 2015/10/19 START
	/**
	 * D9.20關係人交易明細表 下載功能
	 * 
	 * @param relationFlag
	 * @param startDate
	 * @param endDate
	 * @param taxId
	 * @param taxName
	 * @param acctCode
	 * @return
	 */
	public List<String> downLoadRelationTradeDetail(String relationFlag, String startDate, String endDate, String taxId, String taxName, String acctCode) {

		StringBuilder queryString = new StringBuilder();

		queryString.append("   SELECT   ");
		queryString.append("    Distinct    ");
		queryString.append("    TO_CHAR(MA.RELATION_FLAG) AS RELATION_FLAG,    "); // --是否為關係人",
		queryString.append("    TO_CHAR(MA.TAX_ID) AS TAX_ID,    "); // --統一編號/身分證字號
																		// 保代代號",
		queryString.append("    TO_CHAR(MA.TAX_NAME) AS TAX_NAME,    "); // --廠商簡稱",
		queryString.append("    DECODE(TO_CHAR(MA.COMP_ID),NULL,TO_CHAR(MA.TAX_ID),TO_CHAR(MA.COMP_ID)) AS COMP_ID,    "); // --總機構代號",
		queryString.append("    DECODE(TO_CHAR(MA.COMP_NAME),NULL,TO_CHAR(MA.TAX_NAME),TO_CHAR(MA.COMP_NAME)) AS COMP_NAME,    "); // --總機構名稱",
		queryString.append("    TO_CHAR(TO_NUMBER(TO_CHAR(MA.SUBPOENA_DATE,'YYYYMMDD'))-19110000,'0000000') AS SUBPOENA_DATE,    "); // --作帳日期",

		queryString.append("    DECODE(MA.REMIT_DATE,NULL,NULL,TO_CHAR(TO_NUMBER(TO_CHAR(MA.REMIT_DATE,'YYYYMMDD'))-19110000,'0000000')) AS REMIT_DATE,    "); // --付款日期",

		queryString.append("    TO_CHAR(MA.SUBPOENA_NO) AS SUBPOENA_NO,   ");// --傳票號碼",
		queryString.append("    TO_CHAR(MA.INVOICE_NO) AS INVOICE_NO,    ");// --發票號碼",
		queryString.append("    DECODE(MA.INVOICE_DATE,NULL,NULL,TO_CHAR(TO_NUMBER(TO_CHAR(MA.INVOICE_DATE,'YYYYMMDD'))-19110000,'0000000')) AS INVOICE_DATE,    ");// --發票日期",

		queryString.append("    TO_CHAR(MA.ACCTCODE) AS ACCTCODE,    "); // --會計科目",
		queryString.append("    TO_CHAR(MA.ACCTNAME) AS ACCTNAME,    "); // --會計科目",
		queryString.append("    MA.AMT,  "); // --金額--實際收付金額
												// 實際付款的金額，借方為正值、貸方為負值",
		queryString.append("    CASE WHEN MA.ACCTCODE IN ('62010123','62010223','62010323','62020123') THEN MA.AMT ELSE 0 END AS AMT2,    "); // --代收支費用",
		queryString.append("    TO_CHAR(MA.COST_UNIT_CODE) AS COST_UNIT_CODE,    "); // --成本單位",
		queryString.append("    TO_CHAR(MA.COST_UNIT_NAME) AS COST_UNIT_NAME,   "); // --成本單位
																					// ",
		queryString.append("    TO_CHAR(MA.SUMMARY) AS SUMMARY,    "); // --摘要,
		queryString.append("    TO_CHAR(MA.ID)AS ID    ");
		queryString.append("    FROM (SELECT   "); // --外部系統帳務,
		queryString.append("          ESE.ID,    "); // --來源",
		queryString.append("          TO_CHAR('ESE') AS SNAME,    "); // --來源",
		queryString.append("          TO_CHAR(ESE.RELATION_FLAG) AS RELATION_FLAG,  ");// --是否為關係人",
		queryString.append("          CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_CODE   ");
		queryString.append("          ELSE DECODE(DEP.INSUR_AGENT_ID,NULL,ESE.INSUR_AGENT_CODE,DEP.INSUR_AGENT_ID) END AS TAX_ID,   ");// --統一編號/身分證字號
																																		// 保代代號",
		queryString.append("          CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_NAME   ");
		queryString.append("          ELSE ESE.INSUR_AGENT_NAME END AS TAX_NAME,    "); // --廠商簡稱",
		queryString.append("          R.COMP_ID,   ");
		queryString.append("          R.COMP_NAME,   ");
		queryString.append("          ESE.SUBPOENA_DATE,   ");// --作帳日期",
		queryString.append("          ESE.REMIT_DATE,    ");// --付款日期",
		queryString.append("          ESE.SUBPOENA_NO,  ");// --傳票號碼",
		queryString.append("          NULL AS INVOICE_NO,   ");// --發票號碼",
		queryString.append("          NULL AS INVOICE_DATE,    ");// --發票日期ENTERTAIN_DATE",
		queryString.append("          ACCT.CODE AS ACCTCODE,    ");// --會計科目",
		queryString.append("          ACCT.NAME AS ACCTNAME,   ");// --會計科目",
		queryString.append("          DECODE(ET.ENTRY_VALUE,'D',ESE.AMT,-1*ESE.AMT) AS AMT,   ");// --金額--實際收付金額
																									// 實際付款的金額，借方為正值、貸方為負值",
		queryString.append("          DEP1.CODE AS COST_UNIT_CODE,    ");// --成本單位",
		queryString.append("          DEP1.NAME AS COST_UNIT_NAME,   ");// --成本單位
																		// ",
		queryString.append("          ESE.SUMMARY,   ");// --摘要",
		queryString.append("           NULL AS APPITEM_NAME   ");// --交易項目,
		queryString.append("         FROM EXPADMIN.TBEXP_EXT_SYS_ENTRY ESE   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_DEPARTMENT DEP1 ON ESE.COST_UNIT_CODE = DEP1.CODE  ");
		queryString.append("           LEFT  JOIN EXPADMIN.TBEXP_DEPARTMENT DEP ON ESE.INSUR_AGENT_CODE = DEP.CODE   ");
		queryString.append("                      LEFT JOIN (   ");
		queryString.append("                      	SELECT COMP_ID,COMP_NAME,BRANCH_HEAD_OFFICE_CODE,BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      	UNION ALL   ");
		queryString.append("                      	SELECT DISTINCT COMP_ID,COMP_NAME,COMP_ID AS BRANCH_HEAD_OFFICE_CODE,COMP_NAME AS BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      )  R ON  R.BRANCH_HEAD_OFFICE_CODE=DECODE(DEP.INSUR_AGENT_ID,NULL,ESE.INSUR_AGENT_CODE,DEP.INSUR_AGENT_ID)         ");
		queryString.append("        WHERE SUBSTR(ACCT.CODE,1,1) IN ('1','6')   ");// --含資產類和費用類",
		queryString.append("         UNION ALL   ");
		queryString.append("         SELECT    ");// --所得--進項--子檔,
		queryString.append("           E.ID,   ");
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TAX.SNAME  ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN VAT.SNAME  ");
		queryString.append("               WHEN ESB.SNAME = 'ESB' THEN ESB.SNAME END SNAME, ");// --來源",
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TO_CHAR(TAX.RELATION_FLAG)  ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN TO_CHAR(VAT.RELATION_FLAG)  ");
		queryString.append("                WHEN ESB.SNAME = 'ESB' THEN TO_CHAR(ESB.RELATION_FLAG)  ");
		queryString.append("               ELSE '0' END AS RELATION_FLAG,   ");// --是否為關係人
																				// ",
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TAX.TAX_ID   ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN VAT.TAX_ID  ");
		queryString.append("                WHEN ESB.SNAME = 'ESB' THEN ESB.TAX_ID  ");
		queryString.append("               ELSE NULL END AS TAX_ID,    ");// --統一編號/身分證字號
																			// 所得人證號
																			// ",
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TAX.TAX_NAME   ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN VAT.TAX_NAME  ");
		queryString.append("                WHEN ESB.SNAME = 'ESB' THEN ESB.TAX_NAME  ");
		queryString.append("               ELSE NULL END AS TAX_NAME,    ");// --廠商簡稱
																			// 所得人姓名
																			// ",
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TAX.COMP_ID   ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN VAT.COMP_ID  ");
		queryString.append("                WHEN ESB.SNAME = 'ESB' THEN ESB.COMP_ID  ");
		queryString.append("               ELSE NULL END AS COMP_ID,    ");// --總機構ID
		queryString.append("           CASE WHEN TAX.SNAME = 'TAX' THEN TAX.COMP_NAME   ");
		queryString.append("                WHEN VAT.SNAME = 'VAT' THEN VAT.COMP_NAME  ");
		queryString.append("                WHEN ESB.SNAME = 'ESB' THEN ESB.COMP_NAME  ");
		queryString.append("               ELSE NULL END AS COMP_NAME,    ");// --總機構名稱
		queryString.append("          MAIN.SUBPOENA_DATE,    ");// --作帳日期",
		queryString.append("          MAIN.REMIT_DATE,   ");// --付款日期",
		queryString.append("          MAIN.SUBPOENA_NO,    ");// --傳票號碼",
		queryString.append("          SUB.INVOICE_NO,   ");// --發票號碼",
		queryString.append("          SUB.ENTERTAIN_DATE AS INVOICE_DATE,    ");// --發票日期
																				// ",
		queryString.append("          ACCT.CODE AS ACCTCODE,  ");// --會計科目 ",
		queryString.append("          ACCT.NAME AS ACCTNAME,   ");// --會計科目 ",
		queryString.append("          DECODE(ET.ENTRY_VALUE,'D',E.AMT,-1*E.AMT) AS AMT,  ");// --金額--實際收付金額
																							// 實際付款的金額，借方為正值、貸方為負值",
		queryString.append("          E.COST_UNIT_CODE,    ");// --成本單位",
		queryString.append("          E.COST_UNIT_NAME,    ");// --成本單位 ",
		queryString.append("          E.SUMMARY,    ");// --摘要",
		queryString.append("           APPITEM.APPITEM_NAME    ");// --交易項目 ,
		queryString.append("        FROM EXPADMIN.TBEXP_ENTRY E   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_ENTRY_TYPE ET  ON E.TBEXP_ENTRY_TYPE_ID = ET.ID   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACCT ON E.TBEXP_ACC_TITLE_ID  = ACCT.ID   ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN  ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("           LEFT  JOIN EXPADMIN.TBEXP_EXP_SUB SUB    ON E.ID = SUB.TBEXP_ENTRY_ID   ");
		queryString.append("           LEFT  JOIN (SELECT   ");
		queryString.append("                        ACCT.CODE || MID.CODE AS ACCTMID,  ");
		queryString.append("                         APPITEM.APPITEM_NAME  ");
		queryString.append("                       FROM EXPADMIN.TBEXP_ACC_APPITEM APPITEM   ");
		queryString.append("                         INNER JOIN EXPADMIN.TBEXP_ACC_TITLE ACCT ON APPITEM.TBEXP_ACC_TITLE_ID = ACCT.ID  ");
		queryString.append("                         INNER JOIN EXPADMIN.TBEXP_MIDDLE_TYPE MID ON APPITEM.TBEXP_MIDDLE_TYPE_ID = MID.ID     ");
		queryString.append("                      ) APPITEM ON ACCT.CODE || SUBSTR(MAIN.EXP_APPL_NO,1,3) = APPITEM.ACCTMID     ");
		queryString.append("           LEFT  JOIN (SELECT   ");// --所得,
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN DECODE(R.BRANCH_HEAD_OFFICE_CODE,NULL,'','TAX')   ");
		queryString.append("                        ELSE 'TAX' END AS SNAME,  ");
		queryString.append("                        EEG.TBEXP_ENTRY_ID,  ");
		queryString.append("                        TAX.RELATION_FLAG,    ");// --是否為關係人",
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_CODE    ");// --統一編號/身分證字號
																															// 所得人證號",
		queryString.append("                        ELSE TAX.TAX_ID END AS TAX_ID,    ");
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_NAME   ");// --廠商簡稱
																															// 所得人姓名
																															// ,
		queryString.append("                        ELSE TAX.TAX_NAME END AS TAX_NAME,   ");// --廠商簡稱
																							// 所得人姓名
																							// ,
		queryString.append("                       R.COMP_ID,   ");
		queryString.append("                       R.COMP_NAME   ");
		queryString.append("                       FROM EXPADMIN.TBEXP_TAX_DETAIL TAX   ");
		queryString.append("                         INNER JOIN EXPADMIN.TBEXP_ENTRY_EXP_GROUP EEG ON TAX.GROUP_ID = EEG.GROUP_ID   ");
		queryString.append("                      LEFT JOIN (   ");
		queryString.append("                      	SELECT COMP_ID,COMP_NAME,BRANCH_HEAD_OFFICE_CODE,BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      	UNION ALL   ");
		queryString.append("                      	SELECT DISTINCT COMP_ID,COMP_NAME,COMP_ID AS BRANCH_HEAD_OFFICE_CODE,COMP_NAME AS BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      )  R ON  R.BRANCH_HEAD_OFFICE_CODE=TAX.TAX_ID   ");
		queryString.append("                      ) TAX ON E.ID = TAX.TBEXP_ENTRY_ID   ");
		queryString.append("           LEFT  JOIN (SELECT   ");// --進項,
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN DECODE(R.BRANCH_HEAD_OFFICE_CODE,NULL,'','VAT')   ");
		queryString.append("                        ELSE 'VAT' END AS SNAME,  ");
		queryString.append("                        VEE.TBEXP_ENTRY_ID ,");
		queryString.append("                        VAT.RELATION_FLAG,   ");// --是否為關係人",
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_CODE    ");// --統一編號/身分證字號
																															// 所得人證號",
		queryString.append("                        ELSE VAT.COMP_ID END AS TAX_ID,    ");
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_NAME   ");// --廠商簡稱
																															// 所得人姓名
																															// ,
		queryString.append("                        ELSE VEN.VENDOR_CALLED END AS TAX_NAME,   ");// --廠商簡稱
																									// 所得人姓名
																									// ,
		queryString.append("                       R.COMP_ID,   ");
		queryString.append("                       R.COMP_NAME   ");
		queryString.append("                       FROM EXPADMIN.TBEXP_VAT_DETAIL VAT  ");
		queryString.append("                         INNER JOIN EXPADMIN.TBEXP_VAT_EXP_ENTRY_R VEE ON VAT.ID = VEE.TBEXP_VAT_DETAIL_ID   ");
		queryString.append("                        LEFT  JOIN EXPADMIN.TBEXP_VENDOR VEN ON VAT.COMP_ID = VEN.VENDOR_COMP_ID     ");
		queryString.append("                      LEFT JOIN (   ");
		queryString.append("                      	SELECT COMP_ID,COMP_NAME,BRANCH_HEAD_OFFICE_CODE,BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      	UNION ALL   ");
		queryString.append("                      	SELECT DISTINCT COMP_ID,COMP_NAME,COMP_ID AS BRANCH_HEAD_OFFICE_CODE,COMP_NAME AS BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      )  R ON  R.BRANCH_HEAD_OFFICE_CODE=VAT.COMP_ID      ");
		queryString.append("                       ) VAT ON E.ID = VAT.TBEXP_ENTRY_ID   ");
		queryString.append("           LEFT  JOIN (SELECT   ");// --子檔,
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN DECODE(R.BRANCH_HEAD_OFFICE_CODE,NULL,'','ESB')   ");
		queryString.append("                        ELSE 'ESB' END AS SNAME,  ");
		queryString.append("                        ESB.TBEXP_ENTRY_ID,  ");
		queryString.append("                        ESB.RELATION_FLAG,    ");// --是否為關係人",
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_CODE    ");// --統一編號/身分證字號
																															// 所得人證號",
		queryString.append("                        ELSE ESB.INVOICE_COMP_ID END AS TAX_ID,    ");
		queryString.append("                        CASE WHEN " + relationFlag + " =2 THEN R.BRANCH_HEAD_OFFICE_NAME   ");// --廠商簡稱
																															// 所得人姓名
																															// ,
		queryString.append("                        ELSE ESB.INVOICE_COMP_NAME END AS TAX_NAME,   ");// --廠商簡稱
																										// 所得人姓名
																										// ,
		queryString.append("                       R.COMP_ID,   ");
		queryString.append("                       R.COMP_NAME   ");
		queryString.append("                       FROM EXPADMIN.TBEXP_EXP_SUB ESB   ");
		queryString.append("                      LEFT JOIN (   ");
		queryString.append("                      	SELECT COMP_ID,COMP_NAME,BRANCH_HEAD_OFFICE_CODE,BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      	UNION ALL   ");
		queryString.append("                      	SELECT DISTINCT COMP_ID,COMP_NAME,COMP_ID AS BRANCH_HEAD_OFFICE_CODE,COMP_NAME AS BRANCH_HEAD_OFFICE_NAME FROM TBEXP_RELATION_MAINTAIN　R    ");
		queryString.append("                      )  R ON  R.BRANCH_HEAD_OFFICE_CODE=ESB.INVOICE_COMP_ID      ");
		queryString.append("                       WHERE NOT ESB.INVOICE_COMP_ID IS NULL   ");
		queryString.append("                       ) ESB ON E.ID = ESB.TBEXP_ENTRY_ID                  ");
		queryString.append("       WHERE SUBSTR(ACCT.CODE,1,1) IN ('1','6')    "); // --含資產類和費用類",
		queryString.append("         ) MA   ");
		queryString.append("   WHERE TO_CHAR(TO_NUMBER(TO_CHAR(MA.SUBPOENA_DATE,'YYYYMM'))-191100,'00000') BETWEEN TO_CHAR( '" + startDate + "', '00000') AND TO_CHAR('" + endDate + "', '00000')   ");// --查詢年月",
		/*
		 * if(StringUtils.isNotEmpty(relationFlag)){
		 * queryString.append("   AND DECODE(R.COMP_ID,null,'1','2')='"
		 * +relationFlag+"'  ");// --關係狀態
		 * 1.關係人(統編/身分證字號)2.非關係人(統編/身分證字號)0.無關關係人(無統編/身分證字號),
		 * 
		 * }
		 */
		if (StringUtils.isNotEmpty(taxId)) {
			queryString.append("    AND  MA.COMP_ID = '" + taxId + "'  "); // --統一編號/身分證字號
																			// 保代代號",

		}
		if (StringUtils.isNotEmpty(taxName)) {
			queryString.append("    AND  MA.COMP_NAME like '%" + taxName + "%'   "); // --廠商簡稱",
		}
		if (StringUtils.isNotEmpty(acctCode)) {
			queryString.append("    AND  MA.ACCTCODE = '" + acctCode + "'   "); // --會計科目代號",
		}
		queryString.append("   AND MA.TAX_ID IS NOT NULL  ");
		queryString.append("   ORDER BY SUBPOENA_DATE,ACCTCODE  ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		String title = "是否為關係人,分支機構ID,分支機構名稱,關係人總機構ID,關係人總機構名稱,作帳日期,付款日期,傳票號碼,發票號碼,發票日期,會計科目代號,會計科目名稱,金額,代收支費用,成本單位代號,成本單位中文,摘要";
		result.add(title);
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;

				// 匯出字串處理
				StringBuffer exportRecord = new StringBuffer();
				for (int i = 0; i < record.length - 1; i++) {
					Object recObj = record[i];
					if (recObj == null) {
						exportRecord.append("");
					} else {
						if (recObj instanceof Timestamp) {
							Calendar date = Calendar.getInstance();
							date.setTimeInMillis(((Timestamp) recObj).getTime());
							exportRecord.append(DateUtils.getISODateStr(date.getTime(), ""));
						} else if (recObj instanceof BigDecimal) {
							exportRecord.append(((BigDecimal) recObj).toString());
						} else {
							exportRecord.append((String) recObj);
						}
					}
					if (i != record.length - 1) {
						exportRecord.append(",");
					}
				}

				result.add(exportRecord.toString());
			}
		}
		return result;

	}

	// RE201503701_關係人交易明細查詢功能優化 CU3178 2015/10/19 END

	// RE201600212_105上半年度費用系統DEFECTS修改 EC0416 2016/2/2 START
	/**
	 * 依需求單單號查詢該申請單狀態及簽核流程
	 */
	public List<ApplStateFlowDto> findData(String expappls) {
		StringBuffer querySql = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		querySql.append("  SELECT  ");
		querySql.append("  M.CODE AS MIDDLECODE, ");
		querySql.append("  M.NAME AS MIDDLENAME, ");
		querySql.append("  N'', ");
		querySql.append("  N'', ");
		querySql.append("  N'', ");
		querySql.append("  DS.DELIVER_NO, ");
		querySql.append("  S.SUBPOENA_NO, ");
		querySql.append("  S.SUBPOENA_DATE, ");
		querySql.append("  ST.CODE, ");
		querySql.append("  ST.NAME, ");
		querySql.append("  FC.FUNCTION_CODE,  ");
		querySql.append("  FC.FUNCTION_NAME, ");
		querySql.append("  FC.USER_CODE, ");
		querySql.append("  FC.USER_NAME, ");
		querySql.append("  FC.STATE_CODE, ");
		querySql.append("  FC.STATE_NAME, ");
		querySql.append("  FC.APPROVE_DATE ");
		querySql.append("  FROM TBEXP_EXPAPPL_B B ");
		querySql.append("  INNER JOIN TBEXP_RATIFY R ON B.TBEXP_RATIFY_ID=R.ID  ");
		querySql.append("  INNER JOIN TBEXP_MIDDLE_TYPE M ON M.ID=R.TBEXP_MIDDLE_TYPE_ID ");
		querySql.append("  LEFT JOIN TBEXP_DAILY_STAT DS ON B.TBEXP_DAILY_STATEMENT_ID=DS.ID ");
		querySql.append("  LEFT JOIN TBEXP_SUBPOENA S ON S.ID=B.TBEXP_SUBPOENA_ID ");
		querySql.append("  LEFT JOIN TBEXP_APPL_STATE ST ON ST.ID=B.TBEXP_APPL_STATE_ID ");
		querySql.append("  INNER JOIN TBEXP_FLOW_CHECKSTATUS FC ON FC.EXP_APPL_NO=B.EXP_APPL_NO ");
		querySql.append("  WHERE B.EXP_APPL_NO='").append(expappls).append("' ");
		querySql.append("  UNION ALL  ");
		querySql.append("  SELECT ");
		querySql.append("  M.CODE AS MIDDLECODE, ");
		querySql.append("  M.NAME AS MIDDLENAME, ");
		querySql.append("  DD.DELIVER_NO, ");
		querySql.append("  DEP.CODE, ");
		querySql.append("  DEP.NAME, ");
		querySql.append("  DS.DELIVER_NO, ");
		querySql.append("  S.SUBPOENA_NO, ");
		querySql.append("  S.SUBPOENA_DATE, ");
		querySql.append("  ST.CODE, ");
		querySql.append("  ST.NAME, ");
		querySql.append("  FC.FUNCTION_CODE, ");
		querySql.append("  FC.FUNCTION_NAME, ");
		querySql.append("  FC.USER_CODE, ");
		querySql.append("  FC.USER_NAME, ");
		querySql.append("  FC.STATE_CODE, ");
		querySql.append("  FC.STATE_NAME, ");
		querySql.append("  FC.APPROVE_DATE ");
		querySql.append("  FROM TBEXP_EXPAPPL_C C ");
		querySql.append("  INNER JOIN TBEXP_MIDDLE_TYPE M ON M.ID=C.TBEXP_MIDDLE_TYPE_ID ");
		querySql.append("  LEFT JOIN TBEXP_DELIVER_DAYLIST DD ON DD.ID=C.TBEXP_DELIVER_DAYLIST_ID ");
		querySql.append("  LEFT JOIN TBEXP_DEPARTMENT DEP ON DEP.ID=DD.TBEXP_DEPARTMENT_ID ");
		querySql.append("  LEFT JOIN TBEXP_DAILY_STAT DS ON DS.ID=C.TBEXP_DAILY_STATEMENT_ID ");
		querySql.append("  LEFT JOIN TBEXP_SUBPOENA S ON S.ID=C.TBEXP_SUBPOENA_ID ");
		querySql.append("  LEFT JOIN TBEXP_APPL_STATE ST ON ST.ID=C.TBEXP_APPL_STATE_ID ");
		querySql.append("  INNER JOIN TBEXP_FLOW_CHECKSTATUS FC ON FC.EXP_APPL_NO=C.EXP_APPL_NO ");
		querySql.append("  WHERE C.EXP_APPL_NO='").append(expappls).append("' ");
		querySql.append("  UNION ALL ");
		querySql.append("  SELECT ");
		querySql.append("  M.CODE AS MIDDLECODE, ");
		querySql.append("  M.NAME AS MIDDLENAME, ");
		querySql.append("  N'', ");
		querySql.append("  N'', ");
		querySql.append("  N'', ");
		querySql.append("  N'', ");
		querySql.append("  SD.SUBPOENA_NO, ");
		querySql.append("  SD.SUBPOENA_DATE, ");
		querySql.append("  ST.CODE, ");
		querySql.append("  ST.NAME, ");
		querySql.append("  FC.FUNCTION_CODE, ");
		querySql.append("  FC.FUNCTION_NAME, ");
		querySql.append("  FC.USER_CODE, ");
		querySql.append("  FC.USER_NAME, ");
		querySql.append("  FC.STATE_CODE, ");
		querySql.append("  FC.STATE_NAME, ");
		querySql.append("  FC.APPROVE_DATE ");
		querySql.append("  FROM ");
		querySql.append("  TBEXP_EXPAPPL_D D ");
		querySql.append("  INNER JOIN TBEXP_D_CHECK_DETAIL CD ON CD.ID=D.TBEXP_D_CHECK_DETAIL_ID ");
		querySql.append("  INNER JOIN TBEXP_MIDDLE_TYPE M ON M.ID=CD.TBEXP_MIDDLE_TYPE_ID ");
		querySql.append("  LEFT JOIN TBEXP_SUBPOENA_D SD ON SD.ID=D.TBEXP_SUBPOENA_D_ID ");
		querySql.append("  LEFT JOIN TBEXP_APPL_STATE ST ON ST.ID=D.TBEXP_APPL_STATE_ID ");
		querySql.append("  INNER JOIN TBEXP_FLOW_CHECKSTATUS FC ON FC.EXP_APPL_NO=D.EXP_APPL_NO ");
		querySql.append("  WHERE D.EXP_APPL_NO='").append(expappls).append("' ");
		querySql.append("  order by  APPROVE_DATE ");

		params.put("expappls", expappls);

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(expappls);
		List list = findByNativeSQL(querySql.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			List<ApplStateFlowDto> applStateFlowList = new ArrayList<ApplStateFlowDto>();

			for (Object object : list) {
				Object[] record = (Object[]) object;
				ApplStateFlowDto dto = new ApplStateFlowDto();
				//
				dto.setMiddleTypeCode((String) record[0]);

				dto.setMiddleTypeName((String) record[1]);

				dto.setDeliverDaylistDeliverNo((String) record[2]);

				dto.setDepartmentCode((String) record[3]);

				dto.setDepartmentName((String) record[4]);

				dto.setDailyStatDeliverNo((String) record[5]);

				dto.setSubpoenaNo((String) record[6]);

				Calendar subpoenaDate = Calendar.getInstance();
				if (record[7] != null) {
					subpoenaDate.setTimeInMillis(((Timestamp) record[7]).getTime());
					dto.setSubpoenaDate(subpoenaDate);
				}

				dto.setApplStateCode((String) record[8]);

				dto.setApplStateName((String) record[9]);

				dto.setFunctionCode((String) record[10]);

				dto.setFunctionName((String) record[11]);

				dto.setFlowCheckstatusUsercode((String) record[12]);

				dto.setFlowCheckstatusUserName((String) record[13]);

				dto.setFlowCheckstatusStateCode((String) record[14]);

				dto.setFlowCheckstatusStateName((String) record[15]);

				dto.setExpAppls(expappls);

				applStateFlowList.add(dto);
			}
			return applStateFlowList;
		}

	}

	// RE201600212_105上半年度費用系統DEFECTS修改 EC0416 2016/2/2 END

	// RE201601995_內務關鍵績效指標費用作帳錯誤率 EC0416 2016/7/18 START
	/**
	 * C11.7.14 作帳錯誤率明細_部室內務下載 下載功能
	 * 
	 */
	public List<String> downLoadKPIDetailDep(String year, String midCode) {

		StringBuilder queryString = new StringBuilder();

		queryString.append("    SELECT      ");
		queryString.append("      DATA.DEP_UNIT_CODEA,       ");
		queryString.append("      DATA.DEP_UNIT_NAMEA,       ");
		queryString.append("      DATA.DEP_UNIT_CODEB,       ");
		queryString.append("      DATA.DEP_UNIT_NAMEB,       ");
		queryString.append("      DATA.DLPCODE,       ");
		queryString.append("      DATA.ACC_STATE,       ");
		queryString.append("      DATA.JAN_AMT,       ");
		queryString.append("      DATA.FEB_AMT,       ");
		queryString.append("      DATA.MAR_AMT,       ");
		queryString.append("      DATA.APR_AMT,       ");
		queryString.append("      DATA.MAY_AMT,       ");
		queryString.append("      DATA.JUN_AMT,       ");
		queryString.append("      DATA.JUL_AMT,       ");
		queryString.append("      DATA.AUG_AMT,       ");
		queryString.append("      DATA.SEP_AMT,       ");
		queryString.append("      DATA.OCT_AMT,       ");
		queryString.append("      DATA.NOV_AMT,       ");
		queryString.append("      DATA.DEC_AMT,       ");
		queryString.append("      DATA.ERR_STATE,       ");
		queryString.append("      TO_NUMBER(TO_CHAR(ERR_STATE/ACC_STATE*100,'000.00'))AS REALRATIO       ");
		queryString.append("      FROM       ");
		queryString.append("      (SELECT INFO.DEP_UNIT_CODEA,       ");
		queryString.append("        INFO.DEP_UNIT_NAMEA,       ");
		queryString.append("        INFO.DEP_UNIT_CODEB,       ");
		queryString.append("        INFO.DEP_UNIT_NAMEB,       ");
		queryString.append("        INFO.DLPCODE,       ");
		queryString.append("        SUM(MA.ACC_STATE)AS ACC_STATE,       ");// --總件數
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='01' THEN MA.ERR_STATE ELSE 0 END)AS JAN_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='02' THEN MA.ERR_STATE ELSE 0 END)AS  FEB_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='03' THEN MA.ERR_STATE ELSE 0 END)AS MAR_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='04' THEN MA.ERR_STATE ELSE 0  END)AS APR_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='05' THEN MA.ERR_STATE  ELSE 0 END)AS MAY_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='06' THEN MA.ERR_STATE ELSE 0  END)AS JUN_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='07' THEN MA.ERR_STATE ELSE 0  END)AS JUL_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='08' THEN MA.ERR_STATE ELSE 0  END)AS AUG_AMT,   ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='09' THEN MA.ERR_STATE  ELSE 0  END)AS SEP_AMT,  ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='10' THEN MA.ERR_STATE  ELSE 0  END)AS OCT_AMT,  ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='11' THEN MA.ERR_STATE  ELSE 0  END)AS NOV_AMT,  ");
		queryString.append("        SUM(CASE  WHEN TO_CHAR(MAIN.CLOSE_DATE,'MM')='12' THEN MA.ERR_STATE ELSE 0 END)AS DEC_AMT,    ");
		queryString.append("        SUM(MA.ERR_STATE)AS ERR_STATE        ");// --退件累計
		queryString.append("        FROM       ");
		queryString.append("        (SELECT B.EXP_APPL_NO,     ");// --費用申請單號
		queryString.append("                B.TBEXP_ENTRY_GROUP_ID,1 AS ACC_STATE,    ");// --申請資訊
		queryString.append("                SUM(DECODE(PROOF.TBEXP_PROOF_STATE_ID,'21200000-0000-0000-0000-000000000001', 0, 1))AS ERR_STATE       ");
		queryString.append("                FROM EXPADMIN.TBEXP_EXPAPPL_B B       ");
		queryString.append("                LEFT JOIN EXPADMIN.TBEXP_PROOF PROOF ON B.ID=PROOF.TBEXP_EXPAPPL_B_ID     ");
		queryString.append("                INNER JOIN EXPADMIN.TBEXP_APPL_STATE STATE  ON B.TBEXP_APPL_STATE_ID=STATE.ID      ");
		queryString.append("                WHERE STATE.CODE='90'  GROUP BY B.EXP_APPL_NO, B.TBEXP_ENTRY_GROUP_ID     ");// --日結
		queryString.append("          UNION ALL       ");
		queryString.append("         SELECT C.EXP_APPL_NO,          ");// --費用申請單號
		queryString.append("                C.TBEXP_ENTRY_GROUP_ID,1 AS ACC_STATE,  ");// --申請資訊
		queryString.append("                SUM(CASE WHEN FLOW.STATE_CODE IN ('19')THEN 1  ELSE 0  END) AS ERR_STATE      ");
		queryString.append("                FROM EXPADMIN.TBEXP_EXPAPPL_C C       ");
		queryString.append("                INNER JOIN EXPADMIN.TBEXP_FLOW_CHECKSTATUS FLOW  ON C.EXP_APPL_NO=FLOW.EXP_APPL_NO       ");
		queryString.append("                INNER JOIN EXPADMIN.TBEXP_APPL_STATE STATE ON C.TBEXP_APPL_STATE_ID=STATE.ID       ");
		queryString.append("                WHERE STATE.CODE='90'  GROUP BY C.EXP_APPL_NO, C.TBEXP_ENTRY_GROUP_ID)MA   ");// --日結
		queryString.append("        INNER JOIN       ");
		queryString.append("          (SELECT MID.CODE AS MIDCODE  FROM EXPADMIN.TBEXP_MIDDLE_TYPE MID          ");
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_BIG_TYPE BIG  ON MID.TBEXP_BIG_TYPE_ID=BIG.ID        ");
		queryString.append("           WHERE BIG.CODE='00')MID ON SUBSTR(MA.EXP_APPL_NO, 1, 3) = MID.MIDCODE     ");// --辦公費
		queryString.append("           INNER JOIN EXPADMIN.TBEXP_EXP_MAIN MAIN   ON MA.EXP_APPL_NO=MAIN.EXP_APPL_NO      ");
		queryString.append("           INNER JOIN       ");
		queryString.append("          (SELECT DISTINCT INFO.TBEXP_ENTRY_GROUP_ID,DLP.CODE AS REALDLP_CODE,DLP.CODE AS DLPCODE,        ");
		queryString.append("          INFO.DEP_UNIT_CODE1 AS DEP_UNIT_CODEA,       ");
		queryString.append("          INFO.DEP_UNIT_NAME1 AS DEP_UNIT_NAMEA,       ");
		queryString.append("          INFO.DEP_UNIT_CODE2 AS DEP_UNIT_CODEB,       ");
		queryString.append("          INFO.DEP_UNIT_NAME2 AS DEP_UNIT_NAMEB       ");
		queryString.append("          FROM EXPADMIN.TBEXP_DEPARTMENT DEP       ");
		queryString.append("          INNER JOIN EXPADMIN.TBEXP_ENTRY INFO  ON DEP.CODE=INFO.COST_UNIT_CODE        ");
		queryString.append("          INNER JOIN       ");
		queryString.append("          (SELECT DEP.ID,       ");
		queryString.append("            PARENT.CODE AS CODE1,       ");
		queryString.append("            PARENT.NAME AS NAME1,       ");
		queryString.append("            DEP.CODE    AS CODE2,       ");
		queryString.append("            DEP.NAME    AS NAME2       ");
		queryString.append("            FROM EXPADMIN. TBEXP_DEPARTMENT DEP       ");
		queryString.append("            LEFT JOIN EXPADMIN.TBEXP_DEPARTMENT PARENT  ON PARENT.ID=DEP. TBEXP_DEPARTMENT_ID           ");
		queryString.append("            )PARENT ON PARENT.ID=DEP.TBEXP_DEPARTMENT_ID       ");
		queryString.append("          INNER JOIN EXPADMIN.TBEXP_DEP_LEVEL_PROP DLP ON DEP.TBEXP_DEP_LEVEL_PROP_ID=DLP.ID      ");
		queryString.append("          INNER JOIN EXPADMIN.TBEXP_DEP_TYPE DT  ON DEP.TBEXP_DEP_TYPE_ID=DT.ID       ");
		queryString.append("          WHERE DT.CODE IN ('2','3')       ");
		queryString.append("          )INFO ON MA.TBEXP_ENTRY_GROUP_ID=INFO.TBEXP_ENTRY_GROUP_ID       ");
		queryString.append("          WHERE TO_CHAR(MAIN.CLOSE_DATE,'yyyy')='" + year + "'       ");
		queryString.append("         AND MID.MIDCODE IN ('" + midCode + "')       ");
		queryString.append("        GROUP BY        ");
		queryString.append("        INFO.DEP_UNIT_CODEA,       ");
		queryString.append("        INFO.DEP_UNIT_NAMEA,       ");
		queryString.append("        INFO.DEP_UNIT_CODEB,       ");
		queryString.append("        INFO.DEP_UNIT_NAMEB,       ");
		queryString.append("        INFO.DLPCODE)DATA        ");
		queryString.append("        ORDER BY DATA.DEP_UNIT_CODEA ASC  ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		String title = "部室代號,部室名稱,區部代號,區部名稱,單位層級,總件數,一月,二月,三月,四月,五月,六月,七月,八月,九月,十月,十一月,十二月,總錯誤件數,作帳錯誤率";
		result.add(title);
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;

				// 匯出字串處理
				StringBuffer exportRecord = new StringBuffer();
				for (int i = 0; i < record.length; i++) {
					Object recObj = record[i];
					if (recObj == null) {
						exportRecord.append("");
					} else {
						if (recObj instanceof Timestamp) {
							Calendar date = Calendar.getInstance();
							date.setTimeInMillis(((Timestamp) recObj).getTime());
							exportRecord.append(DateUtils.getISODateStr(date.getTime(), ""));
						} else if (recObj instanceof BigDecimal) {
							exportRecord.append(((BigDecimal) recObj).toString());
						} else {
							exportRecord.append((String) recObj);
						}
					}
					if (i != record.length - 1) {
						exportRecord.append(",");
					}
				}

				result.add(exportRecord.toString());
			}
		}
		return result;

	}

	/**
	 * 依查詢起迄日與員編查詢費用作帳錯誤率
	 */
	public List<KPIDetailDto> findKPIData(Calendar findStartDay, Calendar findEndDay, String userId) {
		StringBuffer querySql = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		querySql.append("  SELECT  ");
		querySql.append("  B.EXP_APPL_NO AS EXPAPPLNO,   ");
		querySql.append("  P.PROOF_AMT AS PROOFAMT,    ");
		querySql.append("  R.UNIT_CODE2 AS UNITCODE,    ");
		querySql.append("  R.UNIT_NAME2 AS UNITNAME,   ");
		querySql.append("  P.RETURN_DATE AS REJECTDAY,   ");
		querySql.append("  D.CODE AS DEPCODE,   ");
		querySql.append("  D.NAME AS DEPNAME,   ");
		querySql.append("  U.CODE AS USERCODE,   ");
		querySql.append("  U.NAME AS USERNAME,   ");
		querySql.append("  C.RET_CAUSE AS REASON   ");
		querySql.append("  FROM TBEXP_RATIFY R   ");
		querySql.append("  INNER JOIN TBEXP_USER U ON U.ID=R.TBEXP_USER_ID  ");
		querySql.append("  INNER JOIN TBEXP_DEPARTMENT D ON D.ID=U.TBEXP_DEPARTMENT_ID  ");
		querySql.append("  INNER JOIN TBEXP_EXPAPPL_B B ON B.TBEXP_RATIFY_ID=R.ID   ");
		querySql.append("  INNER JOIN TBEXP_MIDDLE_TYPE MID ON  MID.ID = R.TBEXP_MIDDLE_TYPE_ID   ");
		querySql.append("  INNER JOIN TBEXP_PROOF P ON P.TBEXP_EXPAPPL_B_ID=B.ID ");
		querySql.append("  LEFT JOIN TBEXP_RETURN_CAUSE C ON C.ID=P.TBEXP_RETURN_CAUSE_ID  ");
		querySql.append("  INNER JOIN TBEXP_PROOF_STATE PS ON PS.ID=P.TBEXP_PROOF_STATE_ID  ");
		querySql.append("  INNER JOIN TBEXP_EXP_MAIN M ON M.EXP_APPL_NO=B.EXP_APPL_NO  ");
		querySql.append("  INNER JOIN TBEXP_APPL_STATE S ON S.ID=B.TBEXP_APPL_STATE_ID ");
		querySql.append("  WHERE ");
		querySql.append("  PS.CODE='2' AND MID.CODE IN('3A0','3B0','310') AND ");

		if (StringUtils.isNotBlank(userId)) {
			querySql.append("  U.CODE='").append(userId).append("' AND ");
		}

		querySql.append("  S.CODE='90' AND  ");
		querySql.append("  TO_CHAR(M.CLOSE_DATE ,'YYYYMMDD') BETWEEN '").append(DateUtils.getSimpleISODateStr(findStartDay.getTime())).append("' ");
		querySql.append("  AND '").append(DateUtils.getSimpleISODateStr(findEndDay.getTime())).append("' ");
		querySql.append("  ORDER BY M.CLOSE_DATE ASC ");

		List<Object> parameters = new ArrayList<Object>();

		List list = findByNativeSQL(querySql.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			List<KPIDetailDto> kPIDetailList = new ArrayList<KPIDetailDto>();

			for (Object object : list) {
				Object[] record = (Object[]) object;
				KPIDetailDto dto = new KPIDetailDto();

				dto.setExpAppls((String) record[0]);

				dto.setProofAmt((BigDecimal) record[1]);

				dto.setUnitCode((String) record[2]);

				dto.setUnitName((String) record[3]);

				Calendar rejectDay = Calendar.getInstance();
				if (record[4] == null) {

				} else {
					Calendar date = Calendar.getInstance();
					date.setTimeInMillis(((Timestamp) record[4]).getTime());
					dto.setRejectYearMonthDay(date);
				}

				dto.setDepartmentCode((String) record[5]);

				dto.setDepartmentName((String) record[6]);

				dto.setUserCode((String) record[7]);

				dto.setUserName((String) record[8]);

				String reason = ((String) record[9]);
				if (reason == null) {
					dto.setReturnCause("");
				} else {
					dto.setReturnCause((String) record[9]);
				}
				kPIDetailList.add(dto);
			}
			return kPIDetailList;
		}

	}

	// RE201601995_內務關鍵績效指標費用作帳錯誤率 EC0416 2016/7/18END

	// RE201701547_費用系統預算優化第二階段 EC0416 2017/5/5 start
	// RE201603206_內網預算實支查詢移至費用系統-第一階段 EC0416 2016/9/6 START
	/**
	 * C11.8.5部室預算實支查詢-總表查詢
	 */
	public List findData(Calendar endDate, String depCode, String acctCode) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("   SELECT       ");
		queryString.append("      TO_CHAR(M.DEPCODE) AS DEPCODE,       ");
		queryString.append("      TO_CHAR(M.DEPNAME)AS DEPNAME,    ");
		queryString.append("      TO_CHAR ( M.BICODE) as BICODE,      ");
		queryString.append("      TO_CHAR ( M.BINAME) as BINAME,      ");
		queryString.append("      DECODE(MB.AAMT,NULL,0,MB.AAMT) AS MM1AMT,        ");
		queryString.append("      DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,        ");
		queryString.append("      DECODE(YB.AAMT,NULL,0,YB.AAMT) AS YY1AMT    ");
		queryString.append("    FROM (SELECT       ");
		queryString.append("          T.DEPCODE, T.DEPNAME,     ");
		queryString.append("          ABT.ABTCODE,     ");
		queryString.append("          ABT.ID,     ");
		queryString.append("          ABT.BICODE, ABT.BINAME       ");
		queryString.append("          FROM (SELECT    ");
		queryString.append("                  DISTINCT        ");
		queryString.append("                   DEP.BUDGET_DEP_CODE  AS DEPCODE,        ");
		queryString.append("                   CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'    ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("                   ELSE DEP.NAME  END AS DEPNAME    ");
		queryString.append("                 FROM TBEXP_DEPARTMENT DEP      ");
		queryString.append("                 INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID      ");
		queryString.append("                 WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000')   ");
		queryString.append("                  ) ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("                 ) T,        ");
		queryString.append("               (SELECT    ");
		queryString.append("                   BI.ID,       ");
		queryString.append("                   ABT.CODE AS ABTCODE,    ");
		queryString.append("                   BI.CODE  AS BICODE,    ");
		queryString.append("                   BI.NAME  AS BINAME             ");
		queryString.append("                  FROM TBEXP_BUDGET_ITEM BI       ");
		queryString.append("                  LEFT JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID    ");
		queryString.append("    			  WHERE (BI.CODE LIKE '6%' OR BI.CODE IN('10810000','10830000','10850000','10920620'))  ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        	  AND BI.CODE='").append(acctCode).append("'  ");
		}
		queryString.append("                ) ABT ) M       ");
		queryString.append("  	LEFT JOIN (    ");
		queryString.append("  			SELECT       ");
		queryString.append("  				MM.DEPCODE AS DEPCODE,          ");
		queryString.append("  				MM.BCODE AS BCODE,          ");
		queryString.append("  				SUM(MM.BAMT) AS BAMT         ");
		queryString.append(" 			FROM (    ");
		queryString.append("  				SELECT     ");
		queryString.append(" 					MAIN.SUBPOENA_DATE,      ");
		queryString.append(" 					DEP.BUDGET_DEP_CODE AS DEPCODE,      ");
		queryString.append("  					B.CODE AS BCODE,    ");
		queryString.append(" 					DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT)  AS BAMT    ");
		queryString.append("  				FROM (    ");
		queryString.append("  					SELECT    ");
		queryString.append("  						E.TBEXP_ENTRY_TYPE_ID,    ");
		queryString.append("  						E.COST_UNIT_CODE,    ");
		queryString.append("  						E.TBEXP_ACC_TITLE_ID,    ");
		queryString.append("  						E.AMT,    ");
		queryString.append(" 						MID.CODE AS MCODE,    ");
		queryString.append("  						BIG.CODE AS BCODE,    ");
		queryString.append("  						E.COST_CODE,    ");
		queryString.append("  						MAIN.SUBPOENA_DATE    ");
		queryString.append("  					FROM TBEXP_BIG_TYPE BIG    ");
		queryString.append("    				INNER JOIN TBEXP_MIDDLE_TYPE MID  ON BIG.ID = MID.TBEXP_BIG_TYPE_ID    ");
		queryString.append("    				INNER JOIN TBEXP_EXP_MAIN MAIN   ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)    ");
		queryString.append("    				INNER JOIN TBEXP_ENTRY E   ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
		queryString.append("    				WHERE  TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		                SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		                AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("   					AND BIG.CODE!='16'    ");
		queryString.append("  						) MAIN     ");
		queryString.append("  				INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID    ");
		queryString.append("  				INNER JOIN TBEXP_DEPARTMENT DEP ON MAIN.COST_UNIT_CODE = DEP.CODE    ");
		queryString.append(" 				INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID    ");
		queryString.append("  				INNER JOIN TBEXP_ACC_TITLE ACCT ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID    ");
		queryString.append("  				INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID  =B.ID    ");
		queryString.append("  				WHERE     ");
		queryString.append("  				( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  ");
		queryString.append(" 					OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR BCODE!='15' ) AND     ");
		queryString.append(" 				 (MCODE!='A60' OR MAIN.COST_CODE='W'  ) AND (ACCT.CODE!='61130523') ");
		queryString.append(" 					AND (B.CODE!='63300000' OR MCODE NOT IN ('T05','T12','Q10'))    ");
		queryString.append("  				) )     ");
		queryString.append("  				AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))       ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        	AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("			AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  			UNION ALL     ");
		queryString.append("  			SELECT    ");
		queryString.append("  				ESE.SUBPOENA_DATE,       ");
		queryString.append(" 				DEP.BUDGET_DEP_CODE AS DEPCODE,      ");
		queryString.append("  				B.CODE AS BCODE,    ");
		queryString.append("  				DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT        ");
		queryString.append("  			FROM (    ");
		queryString.append("  				SELECT     ");
		queryString.append("  					TBEXP_ENTRY_TYPE_ID,    ");
		queryString.append("  					ACCT_CODE,    ");
		queryString.append("  					COST_UNIT_CODE,    ");
		queryString.append("  					AMT,    ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("  					SUBPOENA_DATE,    ");
		queryString.append("  					SUBPOENA_NO    ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		queryString.append("  				FROM TBEXP_EXT_SYS_ENTRY ESE    ");
		queryString.append("  				WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		                SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		                AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append(" 				 ) ESE    ");
		queryString.append("  			INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID       ");
		queryString.append("  			INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE       ");
		queryString.append("  			INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID      ");
		queryString.append("  			INNER JOIN  TBEXP_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE    ");
		queryString.append("  			INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID    ");
		queryString.append("  			WHERE (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))      ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("		AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("             AND ESE.SUBPOENA_NO NOT  IN('J827110002','J827115006','J827115009','J827115010')    ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end		
		queryString.append("  			) MM       ");
		queryString.append("  			GROUP BY  MM.BCODE ,MM.DEPCODE    ");
		queryString.append(" 	)MM ON  M.BICODE = MM.BCODE AND M.DEPCODE=MM.DEPCODE     ");
		queryString.append("  	LEFT JOIN(    ");
		queryString.append("  		SELECT        ");
		queryString.append("  			MON.DEP_CODE AS DEPCODE,    ");
		queryString.append("  			B.CODE AS BCODE,    ");
		queryString.append("  			SUM(MON.BUDGET_ITEM_AMT )AS AAMT      ");
		queryString.append("  		FROM TBEXP_MONTH_BUDGET MON    ");
		queryString.append("  		INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID    ");
		queryString.append("  		WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)||'01'   BETWEEN  ");
		queryString.append("		     SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		     AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("  		AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))    ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("    AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND MON.DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  		GROUP BY B.CODE,MON.DEP_CODE    ");
		queryString.append("  	)MB ON MB.BCODE=M.BICODE AND MB.DEPCODE=M.DEPCODE     ");
		queryString.append("  	LEFT JOIN(    ");
		queryString.append("  		SELECT       ");
		queryString.append("  			MON.DEP_CODE AS DEPCODE,    ");
		queryString.append("  			B.CODE AS BCODE,    ");
		queryString.append("  			SUM(MON.BUDGET_ITEM_AMT) AS AAMT     ");
		queryString.append("  		FROM TBEXP_MONTH_BUDGET MON    ");
		queryString.append("  		INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID    ");
		queryString.append("  		WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)||'01'  BETWEEN  ");
		queryString.append("  		 	TO_CHAR(TO_DATE('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("','YYYYMMDD')+1,'YYYYMMDD') ");
		queryString.append("			AND SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '1231'     ");
		queryString.append("  		AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))    ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("    AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND MON.DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  		GROUP BY B.CODE ,MON.DEP_CODE    ");
		queryString.append("  	)YB ON YB.BCODE=M.BICODE  AND MB.DEPCODE=M.DEPCODE   ");
		queryString.append("  	ORDER BY ABTCODE,DECODE(BICODE,'10830000',2,'10920620',3,'10810000',4,'10850000',5,1),BICODE     ");
		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		return list;
	}

	/**
	 * C11.8.5部室預算實支查詢-明細查詢
	 */
	public List findDetailData(Calendar endDate, String depCode, String acctCode) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("   SELECT");
		queryString.append("     MM.DEPCODE,     ");
		queryString.append("     MM.DEPNAME,     ");
		queryString.append("     TO_CHAR(MM.SUBPOENA_DATE,'YYYY/MM/DD'),     ");
		queryString.append("     MM.BUDGETNAME,     ");
		queryString.append("     MM.BUDGETCODE,     ");
		queryString.append("     MM.SUBPOENA_NO,     ");
		queryString.append("     MM.ACCTCODE,     ");
		queryString.append("     MM.ACCTNAME,     ");
		queryString.append("     MM.PROJECT_CODE,     ");
		queryString.append("     MM.BAMT,     ");
		queryString.append("     MM.SUMMARY,     ");
		queryString.append("     MM.COST_CODE,     ");
		queryString.append("     MM.CREATER,     ");
		queryString.append("     MM.RECEIVER,     ");
		queryString.append("     MM.VCNAME");
		queryString.append("     FROM");
		queryString.append("       (");
		queryString.append("         SELECT ");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,      ");
		queryString.append("         MAIN.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         MAIN.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         SUB.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT) AS BAMT,     ");
		queryString.append("         MAIN.SUMMARY,     ");
		queryString.append("         MAIN.COST_CODE,     ");
		queryString.append("         CASE WHEN CUSER.CODE IS NOT NULL THEN CUSER.NAME");
		queryString.append("              WHEN DUSER.CODE IS NOT NULL THEN DUSER.NAME ");
		queryString.append("         ELSE N' '  END AS CREATER,     ");
		queryString.append("         APPL.USER_NAME AS RECEIVER,  ");
		queryString.append("         CASE WHEN VCUSER.CODE IS NOT NULL THEN VCUSER.NAME    ");
		queryString.append("              WHEN VDUSER.CODE IS NOT NULL THEN VDUSER.NAME ");
		queryString.append("         ELSE N' ' END AS VCNAME,  ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("       FROM");
		queryString.append("         (SELECT ");
		queryString.append("           E.ID,     ");
		queryString.append("           E.TBEXP_ENTRY_TYPE_ID,     ");
		queryString.append("           E.COST_UNIT_CODE,     ");
		queryString.append("           E.TBEXP_ACC_TITLE_ID,     ");
		queryString.append("           E.AMT,     ");
		queryString.append("           MID.CODE AS MCODE,     ");
		queryString.append("           BIG.CODE AS BCODE,     ");
		queryString.append("           E.COST_CODE,     ");
		queryString.append("           E.SUMMARY,     ");
		queryString.append("           MAIN.SUBPOENA_NO,     ");
		queryString.append("           MAIN.SUBPOENA_DATE,     ");
		queryString.append("           E.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         FROM TBEXP_BIG_TYPE BIG");
		queryString.append("         INNER JOIN TBEXP_MIDDLE_TYPE MID       ON BIG.ID = MID.TBEXP_BIG_TYPE_ID");
		queryString.append("         INNER JOIN TBEXP_EXP_MAIN MAIN       ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)     ");
		queryString.append("         INNER JOIN TBEXP_ENTRY E     ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		       SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		       AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("         AND BIG.CODE!='16' AND  MID.CODE!='T07'    ");
		queryString.append("         ) MAIN");
		queryString.append("       LEFT JOIN TBEXP_EXPAPPL_C EXPAPPL ON MAIN.TBEXP_ENTRY_GROUP_ID = EXPAPPL.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append("       LEFT JOIN TBEXP_USER VCUSER ON EXPAPPL.TBEXP_VERIFY_USER_ID=VCUSER.ID");
		queryString.append("       left join TBEXP_USER CUSER on EXPAPPL.TBEXP_CREATE_USER_ID = CUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_EXPAPPL_D EXPAPPLD ON MAIN.TBEXP_ENTRY_GROUP_ID = EXPAPPLD.TBEXP_ENTRY_GROUP_ID");
		queryString.append("       LEFT JOIN TBEXP_USER VDUSER ON EXPAPPLD.TBEXP_REAMGR_USER_ID=VDUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_USER DUSER ON EXPAPPLD.TBEXP_CREATE_USER_ID = DUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_APPL_INFO APPL ON EXPAPPL.TBEXP_DRAW_MONEY_USER_INFO_ID = APPL.ID");
		queryString.append("       INNER JOIN TBEXP_ENTRY_TYPE ET    ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID");
		queryString.append("       INNER JOIN TBEXP_DEPARTMENT DEP    ON MAIN.COST_UNIT_CODE = DEP.CODE");
		queryString.append("       INNER JOIN TBEXP_DEP_GROUP DG     ON DG.ID=DEP.TBEXP_DEP_GROUP_ID");
		queryString.append("       INNER JOIN TBEXP_ACC_TITLE ACCT     ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("       LEFT  JOIN TBEXP_EXP_SUB SUB    ON MAIN.ID = SUB.TBEXP_ENTRY_ID");
		queryString.append("       INNER JOIN TBEXP_BUDGET_ITEM B     ON ACCT.TBEXP_BUG_ITEM_ID =B.ID");
		queryString.append("       LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("       WHERE ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')     ");
		queryString.append("       OR ( (BCODE!                     ='00'");
		queryString.append("       OR MCODE                         ='N10')");
		queryString.append("       AND (B.CODE NOT                 IN ('63100000','64100000')     ");
		queryString.append("       OR BCODE!                        ='15' )");
		queryString.append("       AND (MCODE!                      ='A60'");
		queryString.append("       OR MAIN.COST_CODE                ='W' )");
		queryString.append("       AND (ACCT.CODE!                  ='61130523')");
		queryString.append("       AND (B.CODE!                     ='63300000'");
		queryString.append("       OR MCODE NOT                    IN ('T05','T12','Q10')) ) )     ");
		queryString.append("       AND (B.CODE LIKE '6%'  OR B.CODE  IN('10810000','10830000','10850000','10920620')) ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("       UNION ALL");
		queryString.append("        SELECT ");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,   ");
		queryString.append("         MAIN.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         MAIN.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         DETAIL.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DETAIL.ESTIMATION_AMT AS BAMT,     ");
		queryString.append("         DETAIL.SUMMARY,     ");
		queryString.append("         N'',     ");
		queryString.append("         DRUSER.NAME AS CREATER,     ");
		queryString.append("         N'' ,     ");
		queryString.append("         U.NAME AS VDNAME,     ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("        FROM TBEXP_EXPAPPL_D D   ");
		queryString.append("        INNER JOIN TBEXP_USER DRUSER ON DRUSER.ID=D.TBEXP_CREATE_USER_ID       ");
		queryString.append("        INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID     ");
		queryString.append("        INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID    ");
		queryString.append("        INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID   ");
		queryString.append("        INNER JOIN  TBEXP_DEPARTMENT DEP  ON DEP.code=DETAIL.COST_UNIT_CODE ");
		queryString.append("        INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("        INNER JOIN TBEXP_USER U ON U.ID=D.TBEXP_REAMGR_USER_ID");
		queryString.append("        LEFT JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID = B.ID  ");
		queryString.append("        LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("     WHERE ");
		queryString.append("       TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD')  BETWEEN  ");
		queryString.append("		 SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		 AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("       AND ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')     ");
		queryString.append("       OR (ACCT.CODE! ='61130523' ) )");
		queryString.append("       AND (B.CODE LIKE '6%'  OR B.CODE  IN('10810000','10830000','10850000','10920620'))     ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("   ");
		queryString.append("       UNION ALL");
		queryString.append("         SELECT");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,     ");
		queryString.append("         ESE.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         ESE.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         ESE.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT,     ");
		queryString.append("         ESE.SUMMARY,     ");
		queryString.append("         N'',     ");
		queryString.append("         N'',     ");
		queryString.append("         N'' ,     ");
		queryString.append("         N'' ,     ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("       FROM");
		queryString.append("         (SELECT TBEXP_ENTRY_TYPE_ID,     ");
		queryString.append("           ACCT_CODE,     ");
		queryString.append("           COST_UNIT_CODE,     ");
		queryString.append("           AMT,     ");
		queryString.append("           ESE.SUMMARY,     ");
		queryString.append("           ESE.PROJECT_NO,     ");
		queryString.append("           SUBPOENA_NO,     ");
		queryString.append("           SUBPOENA_DATE");
		queryString.append("         FROM TBEXP_EXT_SYS_ENTRY ESE");
		queryString.append("         WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD')  BETWEEN  ");
		queryString.append("		     SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		     AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("         ) ESE");
		queryString.append("       INNER JOIN TBEXP_ENTRY_TYPE ET     ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("       INNER JOIN TBEXP_ACC_TITLE ACCT     ON ESE.ACCT_CODE = ACCT.CODE");
		queryString.append("       INNER JOIN TBEXP_BUDGET_ITEM B     ON ACCT.TBEXP_BUG_ITEM_ID=B.ID");
		queryString.append("       LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("       INNER JOIN TBEXP_DEPARTMENT DEP     ON ESE.COST_UNIT_CODE = DEP.CODE");
		queryString.append("       INNER JOIN TBEXP_DEP_GROUP DG     ON DG.ID=DEP.TBEXP_DEP_GROUP_ID");
		queryString.append("       WHERE (B.CODE LIKE '6%' ");
		queryString.append("       OR B.CODE             IN('10810000','10830000','10850000','10920620'))     ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("       ) MM");
		queryString.append("       ORDER BY MM.SUBPOENA_DATE,MM.ABCODE, DECODE(BUDGETCODE,'10830000',2,'10920620',3,'10810000',4,'10850000',5,1), BUDGETCODE     ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);

		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}

	}

	/**
	 * C11.8.6專案預算實支查詢明細下載-總額查詢
	 */
	public List findProjectData(Calendar endDate, String depCode, String project) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("     SELECT      ");
		queryString.append("      TO_CHAR(CASE WHEN  DEP.DEPCODE IS NULL THEN BI.DEPCODE       ");
		queryString.append("      ELSE DEP.DEPCODE END) AS DEPCODE,       ");
		queryString.append("      TO_CHAR(CASE WHEN  DEP.DEPNAME IS NULL THEN BI.DEPNAME      ");
		queryString.append("      ELSE DEP.DEPNAME END) AS DEPNAME,       ");
		queryString.append("      TO_CHAR(BI.PROJECT_CODE) AS PROJECT_CODE,       ");
		queryString.append("      TO_CHAR(BI.PROJECT_NAME) AS PROJECT_NAME,       ");
		queryString.append("      BI.BUDAMTA,       ");
		queryString.append("      BI.BUDAMTB,       ");
		queryString.append("      DECODE(EXPAPPLC.AMTA,NULL,0,EXPAPPLC.AMTA) AS AMTA,       ");
		queryString.append("      DECODE(EXPAPPLC.AMTB,NULL,0,EXPAPPLC.AMTB) AS AMTB     ");
		queryString.append("          FROM(     ");
		queryString.append("          SELECT     ");
		queryString.append("          DISTINCT     ");
		queryString.append("      DEP.BUDGET_DEP_CODE AS DEPCODE,       ");
		queryString.append("      DEP.DEPNAME  AS DEPNAME,       ");
		queryString.append("      BI.PROJECT_CODE,       ");
		queryString.append("      BI.PROJECT_NAME,       ");
		queryString.append("      SUM(CASE WHEN  BI.BUDGET_ITEM_BIG_TYPE_CODE='1' THEN BI.BUDGET_ITEM_AMT     ");
		queryString.append("        ELSE 0 END) AS BUDAMTA ,      ");
		queryString.append("          SUM(CASE WHEN  BI.BUDGET_ITEM_BIG_TYPE_CODE='2' THEN BI.BUDGET_ITEM_AMT     ");
		queryString.append("      ELSE 0 END) AS BUDAMTB     ");
		queryString.append("          FROM TBEXP_BUDGET_IN BI     ");
		queryString.append("          INNER JOIN     ");
		queryString.append("          (     ");
		queryString.append("          SELECT      ");
		queryString.append("          DISTINCT     ");
		queryString.append("            DEP.ENABLED ,      ");
		queryString.append("            LEV.CODE ,      ");
		queryString.append("            DEP.BUDGET_DEP_CODE AS BUDGET_DEP_CODE,        ");
		queryString.append("           CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("             ELSE DEP.NAME  END AS DEPNAME      ");
		queryString.append("          FROM TBEXP_DEPARTMENT DEP     ");
		queryString.append("          INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID      ");
		queryString.append("          )DEP ON BI.ARRANGE_UNIT_CODE=DEP.BUDGET_DEP_CODE     ");
		queryString.append("          WHERE DEP.ENABLED=1 AND      ");
		queryString.append("      (DEP.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000')) AND     ");
		queryString.append("      BI.PROJECT_TYPE='2' AND BI.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND PROJECT_CODE  ='").append(project).append("'     ");
		}
		queryString.append("          GROUP BY BUDGET_DEP_CODE, DEPNAME, PROJECT_CODE ,PROJECT_NAME        ");
		queryString.append("          )BI     ");
		queryString.append("    LEFT JOIN (     ");
		queryString.append("      SELECT      ");
		queryString.append("      PROJECT_CODE,       ");
		queryString.append("      COST_UNIT_CODE,       ");
		queryString.append("      SUM(AMTA) AS AMTA ,      ");
		queryString.append("      SUM(AMTB) AS AMTB     ");
		queryString.append("      FROM(     ");
		queryString.append("          SELECT     ");
		queryString.append("      	  SUB.PROJECT_NO AS PROJECT_CODE ,      ");
		queryString.append("      	  DEP.BUDGET_DEP_CODE   AS COST_UNIT_CODE  ,       ");
		queryString.append("      	  CASE WHEN  ACT.CODE='1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("        	ELSE 0 END AS AMTA,        ");
		queryString.append("      	  CASE WHEN  ACT.CODE='4' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("            ELSE 0 END AS AMTB     ");
		queryString.append("          FROM TBEXP_ENTRY E     ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID     ");
		queryString.append("          INNER JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID     ");
		queryString.append("          INNER JOIN  TBEXP_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE       ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID=E.TBEXP_ACC_TITLE_ID     ");
		queryString.append("          INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACC.TBEXP_ACC_CLASS_TYPE_ID     ");
		queryString.append("          INNER JOIN Tbexp_Budget_Item B ON B.ID=ACC.TBEXP_BUG_ITEM_ID     ");
		queryString.append("      WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	  AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND SUB.PROJECT_NO ='").append(project).append("'      ");
		}
		// DEFECT#3819 專案代號須包含提列應付費用 CU3178 2017/1/9 START
		queryString.append("          UNION ALL     ");
		queryString.append("          SELECT      ");
		queryString.append("     		DETAIL.PROJECT_NO           AS PROJECT_CODE,       ");
		queryString.append("      		DEP.CODE               AS COST_UNIT_CODE,       ");
		queryString.append("      		CASE WHEN  ACT.CODE='1' THEN DETAIL.ESTIMATION_AMT ");
		queryString.append("			ELSE 0 END AS AMTA,       ");
		queryString.append("          	CASE WHEN  ACT.CODE='4' THEN DETAIL.ESTIMATION_AMT       ");
		queryString.append("          	ELSE 0 END AS AMTB       ");
		queryString.append("          FROM TBEXP_EXPAPPL_D D      ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID     ");
		queryString.append("      	  INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID     ");
		queryString.append("      	  INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID       ");
		queryString.append("      	  INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACCT.TBEXP_ACC_CLASS_TYPE_ID       ");
		queryString.append("    	  LEFT JOIN TBEXP_BUDGET_ITEM bugItem ON (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID)          ");
		queryString.append("   	      INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID        ");
		queryString.append("    	  INNER JOIN TBEXP_DEPARTMENT DEP ON DEP.code=DETAIL.COST_UNIT_CODE          ");
		queryString.append("      WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	  AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND DETAIL.PROJECT_NO ='").append(project).append("'      ");
		}
		// DEFECT#3819 專案代號須包含提列應付費用 CU3178 2017/1/9 END
		queryString.append("          UNION ALL     ");
		queryString.append("          SELECT      ");
		queryString.append("      E.PROJECT_NO AS PROJECT_CODE ,      ");
		queryString.append("      DEP.BUDGET_DEP_CODE AS COST_UNIT_CODE ,        ");
		queryString.append("      CASE WHEN  ACT.CODE='1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("        ELSE 0 END AS AMTA ,      ");
		queryString.append("      CASE WHEN  ACT.CODE='4' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("            ELSE 0 END AS AMTB     ");
		queryString.append("          FROM TBEXP_EXT_SYS_ENTRY E     ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("          INNER JOIN  TBEXP_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE       ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=E.ACCT_CODE     ");
		queryString.append("          INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACC.TBEXP_ACC_CLASS_TYPE_ID     ");
		queryString.append("      WHERE TO_CHAR(E.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	 AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND PROJECT_NO ='").append(project).append("'      ");
		}
		queryString.append("          )     ");
		queryString.append("          GROUP BY   PROJECT_CODE, COST_UNIT_CODE   ");
		queryString.append("          ) EXPAPPLC ON BI.PROJECT_CODE=EXPAPPLC.PROJECT_CODE     ");
		queryString.append("     LEFT JOIN (     ");
		queryString.append("          SELECT      ");
		queryString.append("          DISTINCT     ");
		queryString.append("       DEP.BUDGET_DEP_CODE AS DEPCODE,       ");
		queryString.append("           CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("             ELSE DEP.NAME  END AS DEPNAME     ");
		queryString.append("          FROM TBEXP_DEPARTMENT DEP      ");
		queryString.append("          INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID       ");
		queryString.append("      WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))         ");
		queryString.append("          ) DEP ON  EXPAPPLC.COST_UNIT_CODE =DEP.DEPCODE     ");
		queryString.append("      WHERE( DEP.DEPCODE=BI.DEPCODE OR (DECODE(EXPAPPLC.AMTA,NULL,0,EXPAPPLC.AMTA) !=0 OR DECODE(EXPAPPLC.AMTB,NULL,0,EXPAPPLC.AMTB) !=0 ) OR DEP.DEPCODE IS NULL)         ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("       AND   ((CASE WHEN  DEP.DEPCODE IS NULL THEN BI.DEPCODE       ");
			queryString.append("             ELSE DEP.DEPCODE END) ='").append(depCode).append("'  )     ");
		}
		queryString.append("         ORDER BY BI.PROJECT_CODE   ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}

	}

	/**
	 * C11.8.6專案預算實支查詢明細下載-明細查詢
	 */
	public List findDetailData(Calendar endDate, String project) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("       SELECT    ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(MAIN.SUBPOENA_DATE, 'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           MAIN.SUBPOENA_NO AS SubpoenaNo,   ");
		queryString.append("		   bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	       bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,   ");
		queryString.append("           ACCT.NAME AS AcctName,   ");
		queryString.append("           SUB.PROJECT_NO AS ProjectNo,   ");
		queryString.append("           DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS Amt,   ");
		queryString.append("           E.SUMMARY AS Summary,   ");
		queryString.append("           E.COST_CODE AS CostCode,  ");
		queryString.append("          CASE WHEN CRUSER.CODE IS NOT NULL THEN CRUSER.NAME   ");
		queryString.append("                WHEN DRUSER.CODE IS NOT NULL THEN DRUSER.NAME   ");
		queryString.append("                ELSE N' '  ");
		queryString.append("           END AS Creater,   ");
		queryString.append("          CASE WHEN APPL.USER_ID IS NOT NULL THEN APPL.USER_NAME  ");
		queryString.append("               WHEN MAL.TBEXP_PAYMENT_TARGET_ID IS NOT NULL THEN MAL.TBEXP_PAYMENT_TARGET_ID   ");
		queryString.append("               ELSE N' '  ");
		queryString.append("          END AS Receiver,   ");
		queryString.append("          CASE   WHEN VCUSER.CODE IS NOT NULL THEN VCUSER.NAME    ");
		queryString.append("				 WHEN VDUSER.code IS NOT NULL THEN VDUSER.name ELSE N' ' END 	AS VCNAME  ");
		queryString.append("          FROM TBEXP_ENTRY E    ");
		queryString.append("          INNER JOIN  TBEXP_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE   ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = E.TBEXP_ACC_TITLE_ID   ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID  ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID = E.ID   ");
		queryString.append("          LEFT JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_USER VCUSER ON C.TBEXP_VERIFY_USER_ID=VCUSER.ID      ");
		queryString.append("          LEFT JOIN TBEXP_USER CRUSER ON CRUSER.ID=C.TBEXP_CREATE_USER_ID     ");
		queryString.append("          LEFT JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_USER VDUSER ON D.TBEXP_REAMGR_USER_ID=VDUSER.ID     ");
		queryString.append("          LEFT JOIN TBEXP_USER DRUSER ON CRUSER.ID=D.TBEXP_CREATE_USER_ID     ");
		queryString.append("          LEFT JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID   ");
		queryString.append("          LEFT JOIN TBEXP_APPL_INFO APPL ON C.TBEXP_DRAW_MONEY_USER_INFO_ID = appl.ID    ");
		queryString.append("          LEFT JOIN TBEXP_USER cuser ON MAIN.CREATE_USER_ID = cuser.ID    ");
		queryString.append("      	WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("           AND SUB.PROJECT_NO IN  ('").append(project).append("')  ");
		queryString.append("            AND SUBSTR(MAIN.EXP_APPL_NO,1,3)!='T07'  ");
		queryString.append("          UNION  ALL ");
		queryString.append("          SELECT  ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(MAIN.SUBPOENA_DATE, 'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           MAIN.SUBPOENA_NO AS SubpoenaNo,  ");
		queryString.append("			     	bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	                bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,  ");
		queryString.append("           ACCT.NAME AS AcctName,  ");
		queryString.append("           DETAIL.PROJECT_NO AS ProjectNo, ");
		queryString.append("           DETAIL.ESTIMATION_AMT AS Amt,   ");
		queryString.append("           DETAIL.SUMMARY AS Summary,   ");
		queryString.append("           N'',   ");
		queryString.append("           DRUSER.NAME  AS Creater,  ");
		queryString.append("          N'' ,   ");
		queryString.append("          U.NAME   ");
		queryString.append("          FROM TBEXP_EXPAPPL_D D  ");
		queryString.append("          INNER JOIN TBEXP_USER DRUSER ON DRUSER.ID=D.TBEXP_CREATE_USER_ID      ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID    ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID   ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID  ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_USER U ON U.ID=D.TBEXP_REAMGR_USER_ID    ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append("          INNER JOIN  TBEXP_DEPARTMENT DEP  ON DEP.code=DETAIL.COST_UNIT_CODE   ");
		queryString.append("      	WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("          AND DETAIL.PROJECT_NO IN  ('").append(project).append("')  ");
		queryString.append("          UNION  ALL   ");
		queryString.append("       SELECT    ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(ESE.SUBPOENA_DATE,'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           ESE.SUBPOENA_NO AS SubpoenaNo,   ");
		queryString.append("		   bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	       bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,   ");
		queryString.append("           ACCT.NAME AS AcctName,   ");
		queryString.append("           ESE.PROJECT_NO AS ProjectNo,   ");
		queryString.append("           DECODE(ET.ENTRY_VALUE, 'D', AMT, 'C', -1 * AMT) AS Amt,   ");
		queryString.append("           ESE.SUMMARY AS Summary,   ");
		queryString.append("           ESE.COST_CODE CostCode,   ");
		queryString.append("           N' ' Creater,   ");
		queryString.append("           N' ' Receiver,   ");
		queryString.append("           N' '    ");
		queryString.append("          FROM TBEXP_EXT_SYS_ENTRY ESE   ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE   ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=ESE.TBEXP_ENTRY_TYPE_ID  ");
		queryString.append("          INNER JOIN  TBEXP_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE    ");
		queryString.append("      	  WHERE TO_CHAR(ESE.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("          AND ESE.PROJECT_NO IN  ('").append(project).append("')  ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}
	}

	// RE201603206_內網預算實支查詢移至費用系統-第一階段 EC0416 2016/9/6 END
	// RE201701547_費用系統預算優化第二階段 EC0416 2017/5/5 END

	// 手工帳務調整 EC0416 2016/9/13 start
	public List<AccruedExpenseDto> findPayYearMonthData(String payYearMonth) {
		StringBuffer querySql = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		querySql.append("  SELECT  ");
		querySql.append("   COALESCE(V.PAY_YEAR_MONTH, cexp.PAY_YEAR_MONTH) as 付款年月, ");
		querySql.append("   sum(CASE WHEN E.COST_code='W' THEN E.AMT ELSE 0 END)AS W金額,   ");
		querySql.append("   sum(CASE WHEN (E.COST_code!='W' or E.COST_code is null) THEN E.AMT ELSE 0 END) AS 非W金額  ");
		querySql.append("   from tbexp_expappl_c c  ");
		querySql.append("   inner join tbexp_vendor_exp v on c.id=v.TBEXP_EXPAPPL_C_ID  ");
		querySql.append("   inner join tbexp_appl_state a on a.id=c.TBEXP_APPL_STATE_ID   ");
		querySql.append("   inner join tbexp_entry e on e.TBEXP_ENTRY_GROUP_ID=c.TBEXP_ENTRY_GROUP_ID  ");
		querySql.append("   inner join TBEXP_DELIVER_DAYLIST dd on dd.id=c.TBEXP_DELIVER_DAYLIST_ID   ");
		querySql.append("   inner join TBEXP_ACC_TITLE t on t.id=e.TBEXP_ACC_TITLE_ID ");
		querySql.append("   inner join tbexp_entry_type et on et.id=e.TBEXP_ENTRY_TYPE_ID  ");
		querySql.append("   left join TBEXP_PUB_AFF_CAR_EXP cexp on cexp.TBEXP_EXPAPPL_C_ID=c.id ");
		querySql.append("   inner join TBEXP_MIDDLE_TYPE type on type.id=c.TBEXP_MIDDLE_TYPE_ID  ");
		querySql.append("   inner join tbexp_big_type bt on type.TBEXP_BIG_TYPE_ID=bt.id  ");
		querySql.append("   where a.code between 10 and 40 and c.TEMPORARY_PAYMENT =0   ");
		querySql.append("   AND ( V.PAY_YEAR_MONTH='").append(payYearMonth).append("' ");
		querySql.append("   or cexp.PAY_YEAR_MONTH='").append(payYearMonth).append("') ");
		querySql.append("   and bt.code='01' group by(V.PAY_YEAR_MONTH, cexp.PAY_YEAR_MONTH)  ");

		List<Object> parameters = new ArrayList<Object>();
		parameters.add(payYearMonth);
		List list = findByNativeSQL(querySql.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			List<AccruedExpenseDto> accruedExpenseList = new ArrayList<AccruedExpenseDto>();

			for (Object object : list) {
				Object[] record = (Object[]) object;
				AccruedExpenseDto dto = new AccruedExpenseDto();
				//
				dto.setPayYearMonth((String) record[0]);

				dto.setPayAmtNoW((BigDecimal) record[1]);

				dto.setPayAmtW((BigDecimal) record[2]);

				accruedExpenseList.add(dto);
			}
			return accruedExpenseList;
		}

	}

	public List<String> downLoadPayYearMonthData(String payYearMonth) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("    SELECT      ");
		queryString.append("    COALESCE(V.PAY_YEAR_MONTH, cexp.PAY_YEAR_MONTH) as 付款年月,     ");
		queryString.append("    dd.DELIVER_NO as 送件表單號,     ");
		queryString.append("    c.EXP_APPL_NO as 申請表單號,     ");
		queryString.append("    t.code as 會計科目代號,     ");
		queryString.append("    t.name as 會計科目名稱,     ");
		queryString.append("    e.COST_UNIT_CODE as 成本單位代號,     ");
		queryString.append("    CASE WHEN ET.ENTRY_VALUE='D' THEN E.AMT ELSE 0 END AS 借方金額,      ");
		queryString.append("    CASE WHEN ET.ENTRY_VALUE='C' THEN E.AMT ELSE 0 END AS 貸方金額,     ");
		queryString.append("    (CASE WHEN ET.ENTRY_VALUE='D' THEN E.AMT ELSE 0 END)-(CASE WHEN ET.ENTRY_VALUE='C' THEN E.AMT ELSE 0 END) AS 付款金額,     ");
		queryString.append("    E.SUMMARY AS 摘要,     ");
		queryString.append("    C.COST_TYPE_CODE AS 成本別,     ");
		queryString.append("    C.PROJECT_CODE AS 專案代號,     ");
		queryString.append("    A.NAME AS 申請單狀態名稱     ");
		queryString.append("    from tbexp_expappl_c c    ");
		queryString.append("    inner join tbexp_vendor_exp v on c.id=v.TBEXP_EXPAPPL_C_ID     ");
		queryString.append("    inner join tbexp_appl_state a on a.id=c.TBEXP_APPL_STATE_ID     ");
		queryString.append("    inner join tbexp_entry e on e.TBEXP_ENTRY_GROUP_ID=c.TBEXP_ENTRY_GROUP_ID     ");
		queryString.append("    inner join TBEXP_DELIVER_DAYLIST dd on dd.id=c.TBEXP_DELIVER_DAYLIST_ID      ");
		queryString.append("    inner join TBEXP_ACC_TITLE t on t.id=e.TBEXP_ACC_TITLE_ID     ");
		queryString.append("    inner join tbexp_entry_type et on et.id=e.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("    left join TBEXP_PUB_AFF_CAR_EXP cexp on cexp.TBEXP_EXPAPPL_C_ID=c.id      ");
		queryString.append("    inner join TBEXP_MIDDLE_TYPE type on type.id=c.TBEXP_MIDDLE_TYPE_ID     ");
		queryString.append("    inner join tbexp_big_type bt on type.TBEXP_BIG_TYPE_ID=bt.id       ");
		queryString.append("    where a.code between 10 and 40 and c.TEMPORARY_PAYMENT =0      ");
		queryString.append("    AND ( V.PAY_YEAR_MONTH='").append(payYearMonth).append("'     ");
		queryString.append("    or cexp.PAY_YEAR_MONTH='").append(payYearMonth).append("')     ");
		queryString.append("    and bt.code='01'     ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		String title = "付款年月,送件表單號,申請單代號,會計科目代號,會計科目名稱,成本單位,借方金額,貸方金額,付款金額,摘要,成本別,專案代號,狀態值";
		result.add(title);
		if (!CollectionUtils.isEmpty(list)) {
			for (Object obj : list) {
				Object[] record = (Object[]) obj;

				// 匯出字串處理
				StringBuffer exportRecord = new StringBuffer();
				for (int i = 0; i < record.length; i++) {
					Object recObj = record[i];
					if (recObj == null) {
						exportRecord.append("");
					} else {
						if (recObj instanceof Timestamp) {
							Calendar date = Calendar.getInstance();
							date.setTimeInMillis(((Timestamp) recObj).getTime());
							exportRecord.append(DateUtils.getISODateStr(date.getTime(), ""));
						} else if (recObj instanceof BigDecimal) {
							exportRecord.append(((BigDecimal) recObj).toString());
						} else {
							exportRecord.append((String) recObj);
						}
					}
					if (i != record.length - 1) {
						exportRecord.append(",");
					}
				}

				result.add(exportRecord.toString());
			}
		}
		return result;
	}

	// 手工帳務調整 EC0416 2016/9/13 END

	// RE201701547_費用系統預算優化第二階段 EC0416 2017/5/5 start
	/**
	 * C11.8.5部室預算實支查詢-總表查詢 join年度組織檔
	 */
	public List findYearDepData(Calendar endDate, String depCode, String acctCode) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("   SELECT       ");
		queryString.append("      TO_CHAR(M.DEPCODE) AS DEPCODE,       ");
		queryString.append("      TO_CHAR(M.DEPNAME)AS DEPNAME,    ");
		queryString.append("      TO_CHAR ( M.BICODE) as BICODE,      ");
		queryString.append("      TO_CHAR ( M.BINAME) as BINAME,      ");
		queryString.append("      DECODE(MB.AAMT,NULL,0,MB.AAMT) AS MM1AMT,        ");
		queryString.append("      DECODE(MM.BAMT,NULL,0,MM.BAMT) AS MM2AMT,        ");
		queryString.append("      DECODE(YB.AAMT,NULL,0,YB.AAMT) AS YY1AMT    ");
		queryString.append("    FROM (SELECT       ");
		queryString.append("          T.DEPCODE, T.DEPNAME,     ");
		queryString.append("          ABT.ABTCODE,     ");
		queryString.append("          ABT.ID,     ");
		queryString.append("          ABT.BICODE, ABT.BINAME       ");
		queryString.append("          FROM (SELECT    ");
		queryString.append("                  DISTINCT        ");
		queryString.append("                   DEP.BUDGET_DEP_CODE  AS DEPCODE,        ");
		queryString.append("                   CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='11K000' THEN N'外務人事部本部'       ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='A2S1Q0' THEN N'行銷通路部外埠'    ");
		queryString.append("                   WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("                   ELSE DEP.NAME  END AS DEPNAME    ");
		queryString.append("                 FROM TBEXP_YEAR_DEPARTMENT DEP      ");
		queryString.append("                 INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID      ");
		queryString.append("                 WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000')   ");
		queryString.append("                  ) ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("                 AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("                 ) T,        ");
		queryString.append("               (SELECT    ");
		queryString.append("                   BI.ID,       ");
		queryString.append("                   ABT.CODE AS ABTCODE,    ");
		queryString.append("                   BI.CODE  AS BICODE,    ");
		queryString.append("                   BI.NAME  AS BINAME             ");
		queryString.append("                  FROM TBEXP_BUDGET_ITEM BI       ");
		queryString.append("                  LEFT JOIN TBEXP_AB_TYPE ABT ON BI.TBEXP_AB_TYPE_ID = ABT.ID    ");
		queryString.append("    			  WHERE (BI.CODE LIKE '6%' OR BI.CODE IN('10810000','10830000','10850000','10920620'))  ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        	  AND BI.CODE='").append(acctCode).append("'  ");
		}
		queryString.append("                ) ABT ) M       ");
		queryString.append("  	LEFT JOIN (    ");
		queryString.append("  			SELECT       ");
		queryString.append("  				MM.DEPCODE AS DEPCODE,          ");
		queryString.append("  				MM.BCODE AS BCODE,          ");
		queryString.append("  				SUM(MM.BAMT) AS BAMT         ");
		queryString.append(" 			FROM (    ");
		queryString.append("  				SELECT     ");
		queryString.append(" 					MAIN.SUBPOENA_DATE,      ");
		queryString.append(" 					DEP.BUDGET_DEP_CODE AS DEPCODE,      ");
		queryString.append("  					B.CODE AS BCODE,    ");
		queryString.append(" 					DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT)  AS BAMT    ");
		queryString.append("  				FROM (    ");
		queryString.append("  					SELECT    ");
		queryString.append("  						E.TBEXP_ENTRY_TYPE_ID,    ");
		queryString.append("  						E.COST_UNIT_CODE,    ");
		queryString.append("  						E.TBEXP_ACC_TITLE_ID,    ");
		queryString.append("  						E.AMT,    ");
		queryString.append(" 						MID.CODE AS MCODE,    ");
		queryString.append("  						BIG.CODE AS BCODE,    ");
		queryString.append("  						E.COST_CODE,    ");
		queryString.append("  						MAIN.SUBPOENA_DATE    ");
		queryString.append("  					FROM TBEXP_BIG_TYPE BIG    ");
		queryString.append("    				INNER JOIN TBEXP_MIDDLE_TYPE MID  ON BIG.ID = MID.TBEXP_BIG_TYPE_ID    ");
		queryString.append("    				INNER JOIN TBEXP_EXP_MAIN MAIN   ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)    ");
		queryString.append("    				INNER JOIN TBEXP_ENTRY E   ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID    ");
		queryString.append("    				WHERE  TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		                SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		                AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("   					AND BIG.CODE!='16'    ");
		queryString.append("  						) MAIN     ");
		queryString.append("  				INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID    ");
		queryString.append("  				INNER JOIN TBEXP_YEAR_DEPARTMENT DEP ON MAIN.COST_UNIT_CODE = DEP.CODE    ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  				AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append(" 				INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID    ");
		queryString.append("  				INNER JOIN TBEXP_ACC_TITLE ACCT ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID    ");
		queryString.append("  				INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID  =B.ID    ");
		queryString.append("  				WHERE     ");
		queryString.append("  				( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')  ");
		queryString.append(" 					OR ( (BCODE!='00' OR MCODE='N10') AND (B.CODE NOT IN ('63100000','64100000') OR BCODE!='15' ) AND     ");
		queryString.append(" 				 (MCODE!='A60' OR MAIN.COST_CODE='W'  ) AND (ACCT.CODE!='61130523') ");
		queryString.append(" 					AND (B.CODE!='63300000' OR MCODE NOT IN ('T05','T12','Q10'))    ");
		queryString.append("  				) )     ");
		queryString.append("  				AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))       ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        	AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("			AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  			UNION ALL     ");
		queryString.append("  			SELECT    ");
		queryString.append("  				ESE.SUBPOENA_DATE,       ");
		queryString.append(" 				DEP.BUDGET_DEP_CODE AS DEPCODE,      ");
		queryString.append("  				B.CODE AS BCODE,    ");
		queryString.append("  				DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT        ");
		queryString.append("  			FROM (    ");
		queryString.append("  				SELECT     ");
		queryString.append("  					TBEXP_ENTRY_TYPE_ID,    ");
		queryString.append("  					ACCT_CODE,    ");
		queryString.append("  					COST_UNIT_CODE,    ");
		queryString.append("  					AMT,    ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("  					SUBPOENA_DATE ,   ");
		queryString.append("  					SUBPOENA_NO    ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end		
		queryString.append("  				FROM TBEXP_EXT_SYS_ENTRY ESE    ");
		queryString.append("  				WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		                SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		                AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append(" 				 ) ESE    ");
		queryString.append("  			INNER JOIN TBEXP_ENTRY_TYPE ET  ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID       ");
		queryString.append("  			INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE       ");
		queryString.append("  			INNER JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID=B.ID      ");
		queryString.append("  			INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE    ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  			AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("  			INNER JOIN TBEXP_DEP_GROUP DG ON DG.ID=DEP.TBEXP_DEP_GROUP_ID    ");
		queryString.append("  			WHERE (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))      ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("		AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
		queryString.append("    AND ESE.SUBPOENA_NO NOT  IN('J827110002','J827115006','J827115009','J827115010')   ");
		//RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end
		
		queryString.append("  			) MM       ");
		queryString.append("  			GROUP BY  MM.BCODE ,MM.DEPCODE    ");
		queryString.append(" 	)MM ON  M.BICODE = MM.BCODE AND M.DEPCODE=MM.DEPCODE     ");
		queryString.append("  	LEFT JOIN(    ");
		queryString.append("  		SELECT        ");
		queryString.append("  			MON.DEP_CODE AS DEPCODE,    ");
		queryString.append("  			B.CODE AS BCODE,    ");
		queryString.append("  			SUM(MON.BUDGET_ITEM_AMT )AS AAMT      ");
		queryString.append("  		FROM TBEXP_MONTH_BUDGET MON    ");
		queryString.append("  		INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID    ");
		queryString.append("  		WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)||'01'   BETWEEN  ");
		queryString.append("		     SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		     AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("  		AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))    ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("    AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND MON.DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  		GROUP BY B.CODE,MON.DEP_CODE    ");
		queryString.append("  	)MB ON MB.BCODE=M.BICODE AND MB.DEPCODE=M.DEPCODE     ");
		queryString.append("  	LEFT JOIN(    ");
		queryString.append("  		SELECT       ");
		queryString.append("  			MON.DEP_CODE AS DEPCODE,    ");
		queryString.append("  			B.CODE AS BCODE,    ");
		queryString.append("  			SUM(MON.BUDGET_ITEM_AMT) AS AAMT     ");
		queryString.append("  		FROM TBEXP_MONTH_BUDGET MON    ");
		queryString.append("  		INNER JOIN TBEXP_BUDGET_ITEM B ON MON.TBEXP_BUG_ITEM_ID=B.ID    ");
		queryString.append("  		WHERE TO_CHAR(TO_NUMBER(SUBSTR(MON.YYYMM, 1,3)+ 1911))|| SUBSTR(MON.YYYMM,4,2)||'01'  BETWEEN  ");
		queryString.append("  		 	TO_CHAR(TO_DATE('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("','YYYYMMDD')+1,'YYYYMMDD') ");
		queryString.append("			AND SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '1231'     ");
		queryString.append("  		AND (B.CODE LIKE '6%' OR B.CODE IN('10810000','10830000','10850000','10920620'))    ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("    AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND  MON.DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("  		GROUP BY B.CODE ,MON.DEP_CODE    ");
		queryString.append("  	)YB ON YB.BCODE=M.BICODE  AND MB.DEPCODE=M.DEPCODE   ");
		queryString.append("  	ORDER BY ABTCODE,DECODE(BICODE,'10830000',2,'10920620',3,'10810000',4,'10850000',5,1),BICODE     ");
		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		List<String> result = new ArrayList<String>();
		return list;
	}

	/**
	 * C11.8.5部室預算實支查詢-tbexp_year_department明細查詢
	 */
	public List findYearDepDetailData(Calendar endDate, String depCode, String acctCode) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("   SELECT");
		queryString.append("     MM.DEPCODE,     ");
		queryString.append("     MM.DEPNAME,     ");
		queryString.append("     TO_CHAR(MM.SUBPOENA_DATE,'YYYY/MM/DD'),     ");
		queryString.append("     MM.BUDGETNAME,     ");
		queryString.append("     MM.BUDGETCODE,     ");
		queryString.append("     MM.SUBPOENA_NO,     ");
		queryString.append("     MM.ACCTCODE,     ");
		queryString.append("     MM.ACCTNAME,     ");
		queryString.append("     MM.PROJECT_CODE,     ");
		queryString.append("     MM.BAMT,     ");
		queryString.append("     MM.SUMMARY,     ");
		queryString.append("     MM.COST_CODE,     ");
		queryString.append("     MM.CREATER,     ");
		queryString.append("     MM.RECEIVER,     ");
		queryString.append("     MM.VCNAME");
		queryString.append("     FROM");
		queryString.append("       (");
		queryString.append("         SELECT ");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,      ");
		queryString.append("         MAIN.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         MAIN.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         SUB.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DECODE(ET.ENTRY_VALUE, 'D', MAIN.AMT, 'C', -1 * MAIN.AMT) AS BAMT,     ");
		queryString.append("         MAIN.SUMMARY,     ");
		queryString.append("         MAIN.COST_CODE,     ");
		queryString.append("         CASE WHEN CUSER.CODE IS NOT NULL THEN CUSER.NAME");
		queryString.append("              WHEN DUSER.CODE IS NOT NULL THEN DUSER.NAME ");
		queryString.append("         ELSE N' '  END AS CREATER,     ");
		queryString.append("         APPL.USER_NAME AS RECEIVER,  ");
		queryString.append("         CASE WHEN VCUSER.CODE IS NOT NULL THEN VCUSER.NAME    ");
		queryString.append("              WHEN VDUSER.CODE IS NOT NULL THEN VDUSER.NAME ");
		queryString.append("         ELSE N' ' END AS VCNAME,  ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("       FROM");
		queryString.append("         (SELECT ");
		queryString.append("           E.ID,     ");
		queryString.append("           E.TBEXP_ENTRY_TYPE_ID,     ");
		queryString.append("           E.COST_UNIT_CODE,     ");
		queryString.append("           E.TBEXP_ACC_TITLE_ID,     ");
		queryString.append("           E.AMT,     ");
		queryString.append("           MID.CODE AS MCODE,     ");
		queryString.append("           BIG.CODE AS BCODE,     ");
		queryString.append("           E.COST_CODE,     ");
		queryString.append("           E.SUMMARY,     ");
		queryString.append("           MAIN.SUBPOENA_NO,     ");
		queryString.append("           MAIN.SUBPOENA_DATE,     ");
		queryString.append("           E.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         FROM TBEXP_BIG_TYPE BIG");
		queryString.append("         INNER JOIN TBEXP_MIDDLE_TYPE MID       ON BIG.ID = MID.TBEXP_BIG_TYPE_ID");
		queryString.append("         INNER JOIN TBEXP_EXP_MAIN MAIN       ON MID.CODE =SUBSTR(MAIN.EXP_APPL_NO,1,3)     ");
		queryString.append("         INNER JOIN TBEXP_ENTRY E     ON E.TBEXP_ENTRY_GROUP_ID = MAIN.TBEXP_ENTRY_GROUP_ID");
		queryString.append("         WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD') BETWEEN  ");
		queryString.append("		       SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		       AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("         AND BIG.CODE!='16' AND  MID.CODE!='T07'    ");
		queryString.append("         ) MAIN");
		queryString.append("       LEFT JOIN TBEXP_EXPAPPL_C EXPAPPL ON MAIN.TBEXP_ENTRY_GROUP_ID = EXPAPPL.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append("       LEFT JOIN TBEXP_USER VCUSER ON EXPAPPL.TBEXP_VERIFY_USER_ID=VCUSER.ID");
		queryString.append("       left join TBEXP_USER CUSER on EXPAPPL.TBEXP_CREATE_USER_ID = CUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_EXPAPPL_D EXPAPPLD ON MAIN.TBEXP_ENTRY_GROUP_ID = EXPAPPLD.TBEXP_ENTRY_GROUP_ID");
		queryString.append("       LEFT JOIN TBEXP_USER VDUSER ON EXPAPPLD.TBEXP_REAMGR_USER_ID=VDUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_USER DUSER ON EXPAPPLD.TBEXP_CREATE_USER_ID = DUSER.ID");
		queryString.append("       LEFT JOIN TBEXP_APPL_INFO APPL ON EXPAPPL.TBEXP_DRAW_MONEY_USER_INFO_ID = APPL.ID");
		queryString.append("       INNER JOIN TBEXP_ENTRY_TYPE ET    ON ET.ID=MAIN.TBEXP_ENTRY_TYPE_ID");
		queryString.append("       INNER JOIN TBEXP_YEAR_DEPARTMENT DEP    ON MAIN.COST_UNIT_CODE = DEP.CODE");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("       INNER JOIN TBEXP_DEP_GROUP DG     ON DG.ID=DEP.TBEXP_DEP_GROUP_ID");
		queryString.append("       INNER JOIN TBEXP_ACC_TITLE ACCT     ON MAIN.TBEXP_ACC_TITLE_ID = ACCT.ID");
		queryString.append("       LEFT  JOIN TBEXP_EXP_SUB SUB    ON MAIN.ID = SUB.TBEXP_ENTRY_ID");
		queryString.append("       INNER JOIN TBEXP_BUDGET_ITEM B     ON ACCT.TBEXP_BUG_ITEM_ID =B.ID");
		queryString.append("       LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("       WHERE ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')     ");
		queryString.append("       OR ( (BCODE!                     ='00'");
		queryString.append("       OR MCODE                         ='N10')");
		queryString.append("       AND (B.CODE NOT                 IN ('63100000','64100000')     ");
		queryString.append("       OR BCODE!                        ='15' )");
		queryString.append("       AND (MCODE!                      ='A60'");
		queryString.append("       OR MAIN.COST_CODE                ='W' )");
		queryString.append("       AND (ACCT.CODE!                  ='61130523')");
		queryString.append("       AND (B.CODE!                     ='63300000'");
		queryString.append("       OR MCODE NOT                    IN ('T05','T12','Q10')) ) )     ");
		queryString.append("       AND (B.CODE LIKE '6%'  OR B.CODE  IN('10810000','10830000','10850000','10920620')) ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("       UNION ALL");
		queryString.append("        SELECT ");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,   ");
		queryString.append("         MAIN.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         MAIN.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         DETAIL.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DETAIL.ESTIMATION_AMT AS BAMT,     ");
		queryString.append("         DETAIL.SUMMARY,     ");
		queryString.append("         N'',     ");
		queryString.append("         DRUSER.NAME AS CREATER,     ");
		queryString.append("         N'' ,     ");
		queryString.append("         U.NAME AS VDNAME,     ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("        FROM TBEXP_EXPAPPL_D D   ");
		queryString.append("        INNER JOIN TBEXP_USER DRUSER ON DRUSER.ID=D.TBEXP_CREATE_USER_ID       ");
		queryString.append("        INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID     ");
		queryString.append("        INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID    ");
		queryString.append("        INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID   ");
		queryString.append("        INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON DEP.code=DETAIL.COST_UNIT_CODE ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("        INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("        INNER JOIN TBEXP_USER U ON U.ID=D.TBEXP_REAMGR_USER_ID");
		queryString.append("        LEFT JOIN TBEXP_BUDGET_ITEM B ON ACCT.TBEXP_BUG_ITEM_ID = B.ID  ");
		queryString.append("        LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("     WHERE ");
		queryString.append("       TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD')  BETWEEN  ");
		queryString.append("		 SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		 AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("       AND ( DEP.BUDGET_DEP_CODE NOT IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000','A2S1Q0','A0XY00')     ");
		queryString.append("       OR (ACCT.CODE! ='61130523' ) )");
		queryString.append("       AND (B.CODE LIKE '6%'  OR B.CODE  IN('10810000','10830000','10850000','10920620'))     ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("   ");
		queryString.append("       UNION ALL");
		queryString.append("         SELECT");
		queryString.append("         DEP.CODE AS DEPCODE,     ");
		queryString.append("         DEP.NAME AS DEPNAME,     ");
		queryString.append("         ESE.SUBPOENA_DATE AS SUBPOENA_DATE,     ");
		queryString.append("         B.NAME AS BUDGETNAME, ");
		queryString.append("         B.CODE AS BUDGETCODE, ");
		queryString.append("         ESE.SUBPOENA_NO AS SUBPOENA_NO,      ");
		queryString.append("         ACCT.CODE ACCTCODE,     ");
		queryString.append("         ACCT.NAME ACCTNAME,     ");
		queryString.append("         ESE.PROJECT_NO AS PROJECT_CODE,     ");
		queryString.append("         DECODE(ET.ENTRY_VALUE, 'D', ESE.AMT, 'C', -1 * ESE.AMT) AS BAMT,     ");
		queryString.append("         ESE.SUMMARY,     ");
		queryString.append("         N'',     ");
		queryString.append("         N'',     ");
		queryString.append("         N'' ,     ");
		queryString.append("         N'' ,     ");
		queryString.append("         ABT.CODE AS ABCODE");
		queryString.append("       FROM");
		queryString.append("         (SELECT TBEXP_ENTRY_TYPE_ID,     ");
		queryString.append("           ACCT_CODE,     ");
		queryString.append("           COST_UNIT_CODE,     ");
		queryString.append("           AMT,     ");
		queryString.append("           ESE.SUMMARY,     ");
		queryString.append("           ESE.PROJECT_NO,     ");
		queryString.append("           SUBPOENA_NO,     ");
		queryString.append("           SUBPOENA_DATE");
		queryString.append("         FROM TBEXP_EXT_SYS_ENTRY ESE");
		queryString.append("         WHERE TO_CHAR(SUBPOENA_DATE,'YYYYMMDD')  BETWEEN  ");
		queryString.append("		     SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		     AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("         ) ESE");
		queryString.append("       INNER JOIN TBEXP_ENTRY_TYPE ET     ON ESE.TBEXP_ENTRY_TYPE_ID = ET.ID");
		queryString.append("       INNER JOIN TBEXP_ACC_TITLE ACCT     ON ESE.ACCT_CODE = ACCT.CODE");
		queryString.append("       INNER JOIN TBEXP_BUDGET_ITEM B     ON ACCT.TBEXP_BUG_ITEM_ID=B.ID");
		queryString.append("       LEFT JOIN TBEXP_AB_TYPE ABT     ON B.TBEXP_AB_TYPE_ID = ABT.ID");
		queryString.append("       INNER JOIN TBEXP_YEAR_DEPARTMENT DEP     ON ESE.COST_UNIT_CODE = DEP.CODE");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append(" 	   AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("       INNER JOIN TBEXP_DEP_GROUP DG     ON DG.ID=DEP.TBEXP_DEP_GROUP_ID");
		queryString.append("       WHERE (B.CODE LIKE '6%' ");
		queryString.append("       OR B.CODE             IN('10810000','10830000','10850000','10920620'))     ");
		if (!StringUtils.isBlank(acctCode)) {
			queryString.append("        AND B.CODE='").append(acctCode).append("'  ");
		}
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		queryString.append("       ) MM");
		queryString.append("       ORDER BY MM.SUBPOENA_DATE,MM.ABCODE, DECODE(BUDGETCODE,'10830000',2,'10920620',3,'10810000',4,'10850000',5,1), BUDGETCODE     ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);

		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}

	}

	/**
	 * C11.8.6專案預算實支查詢明細下載-TBEXP_YEAR_DEPARTMENT 總額查詢
	 */
	public List findYearDepProjectData(Calendar endDate, String depCode, String project) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("     SELECT      ");
		queryString.append("      TO_CHAR(CASE WHEN  DEP.DEPCODE IS NULL THEN BI.DEPCODE       ");
		queryString.append("      ELSE DEP.DEPCODE END) AS DEPCODE,       ");
		queryString.append("      TO_CHAR(CASE WHEN  DEP.DEPNAME IS NULL THEN BI.DEPNAME      ");
		queryString.append("      ELSE DEP.DEPNAME END) AS DEPNAME,       ");
		queryString.append("      TO_CHAR(BI.PROJECT_CODE) AS PROJECT_CODE,       ");
		queryString.append("      TO_CHAR(BI.PROJECT_NAME) AS PROJECT_NAME,       ");
		queryString.append("      BI.BUDAMTA,       ");
		queryString.append("      BI.BUDAMTB,       ");
		queryString.append("      DECODE(EXPAPPLC.AMTA,NULL,0,EXPAPPLC.AMTA) AS AMTA,       ");
		queryString.append("      DECODE(EXPAPPLC.AMTB,NULL,0,EXPAPPLC.AMTB) AS AMTB     ");
		queryString.append("          FROM(     ");
		queryString.append("          SELECT     ");
		queryString.append("          DISTINCT     ");
		queryString.append("      DEP.BUDGET_DEP_CODE AS DEPCODE,       ");
		queryString.append("      DEP.DEPNAME  AS DEPNAME,       ");
		queryString.append("      BI.PROJECT_CODE,       ");
		queryString.append("      BI.PROJECT_NAME,       ");
		queryString.append("      SUM(CASE WHEN  BI.BUDGET_ITEM_BIG_TYPE_CODE='1' THEN BI.BUDGET_ITEM_AMT     ");
		queryString.append("        ELSE 0 END) AS BUDAMTA ,      ");
		queryString.append("          SUM(CASE WHEN  BI.BUDGET_ITEM_BIG_TYPE_CODE='2' THEN BI.BUDGET_ITEM_AMT     ");
		queryString.append("      ELSE 0 END) AS BUDAMTB     ");
		queryString.append("          FROM TBEXP_BUDGET_IN BI     ");
		queryString.append("          INNER JOIN     ");
		queryString.append("          (     ");
		queryString.append("          SELECT      ");
		queryString.append("          DISTINCT     ");
		queryString.append("            DEP.ENABLED ,      ");
		queryString.append("            LEV.CODE ,      ");
		queryString.append("            DEP.BUDGET_DEP_CODE AS BUDGET_DEP_CODE,        ");
		queryString.append("           CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("             ELSE DEP.NAME  END AS DEPNAME      ");
		queryString.append("          FROM TBEXP_YEAR_DEPARTMENT DEP     ");
		queryString.append("          INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID      ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		  WHERE DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("          )DEP ON BI.ARRANGE_UNIT_CODE=DEP.BUDGET_DEP_CODE     ");
		queryString.append("          WHERE DEP.ENABLED=1 AND      ");
		queryString.append("      (DEP.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000')) AND     ");
		queryString.append("      BI.PROJECT_TYPE='2' AND BI.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND PROJECT_CODE  ='").append(project).append("'     ");
		}
		queryString.append("          GROUP BY BUDGET_DEP_CODE, DEPNAME, PROJECT_CODE ,PROJECT_NAME        ");
		queryString.append("          )BI     ");
		queryString.append("    LEFT JOIN (     ");
		queryString.append("      SELECT      ");
		queryString.append("      PROJECT_CODE,       ");
		queryString.append("      COST_UNIT_CODE,       ");
		queryString.append("      SUM(AMTA) AS AMTA ,      ");
		queryString.append("      SUM(AMTB) AS AMTB     ");
		queryString.append("      FROM(     ");
		queryString.append("          SELECT     ");
		queryString.append("      	  SUB.PROJECT_NO AS PROJECT_CODE ,      ");
		queryString.append("      	  DEP.BUDGET_DEP_CODE   AS COST_UNIT_CODE  ,       ");
		queryString.append("      	  CASE WHEN  ACT.CODE='1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("        	ELSE 0 END AS AMTA,        ");
		queryString.append("      	  CASE WHEN  ACT.CODE='4' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("            ELSE 0 END AS AMTB     ");
		queryString.append("          FROM TBEXP_ENTRY E     ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID     ");
		queryString.append("          INNER JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID=E.ID     ");
		queryString.append("          INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE       ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.ID=E.TBEXP_ACC_TITLE_ID     ");
		queryString.append("          INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACC.TBEXP_ACC_CLASS_TYPE_ID     ");
		queryString.append("          INNER JOIN Tbexp_Budget_Item B ON B.ID=ACC.TBEXP_BUG_ITEM_ID     ");
		queryString.append("      WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	  AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND SUB.PROJECT_NO ='").append(project).append("'      ");
		}
		// DEFECT#3819 專案代號須包含提列應付費用 CU3178 2017/1/9 START
		queryString.append("          UNION ALL     ");
		queryString.append("          SELECT      ");
		queryString.append("     		DETAIL.PROJECT_NO           AS PROJECT_CODE,       ");
		queryString.append("      		DEP.CODE               AS COST_UNIT_CODE,       ");
		queryString.append("      		CASE WHEN  ACT.CODE='1' THEN DETAIL.ESTIMATION_AMT ");
		queryString.append("			ELSE 0 END AS AMTA,       ");
		queryString.append("          	CASE WHEN  ACT.CODE='4' THEN DETAIL.ESTIMATION_AMT       ");
		queryString.append("          	ELSE 0 END AS AMTB       ");
		queryString.append("          FROM TBEXP_EXPAPPL_D D      ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID     ");
		queryString.append("      	  INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID     ");
		queryString.append("      	  INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID       ");
		queryString.append("      	  INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACCT.TBEXP_ACC_CLASS_TYPE_ID       ");
		queryString.append("    	  LEFT JOIN TBEXP_BUDGET_ITEM bugItem ON (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID)          ");
		queryString.append("   	      INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID        ");
		queryString.append("    	  INNER JOIN TBEXP_YEAR_DEPARTMENT DEP ON DEP.code=DETAIL.COST_UNIT_CODE          ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("      WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	  AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND DETAIL.PROJECT_NO ='").append(project).append("'      ");
		}
		// DEFECT#3819 專案代號須包含提列應付費用 CU3178 2017/1/9 END
		queryString.append("          UNION ALL     ");
		queryString.append("          SELECT      ");
		queryString.append("      E.PROJECT_NO AS PROJECT_CODE ,      ");
		queryString.append("      DEP.BUDGET_DEP_CODE AS COST_UNIT_CODE ,        ");
		queryString.append("      CASE WHEN  ACT.CODE='1' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("        ELSE 0 END AS AMTA ,      ");
		queryString.append("      CASE WHEN  ACT.CODE='4' THEN DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT)          ");
		queryString.append("            ELSE 0 END AS AMTB     ");
		queryString.append("          FROM TBEXP_EXT_SYS_ENTRY E     ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID     ");
		queryString.append("          INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE       ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACC ON ACC.CODE=E.ACCT_CODE     ");
		queryString.append("          INNER JOIN TBEXP_ACC_CLASS_TYPE ACT ON ACT.ID=ACC.TBEXP_ACC_CLASS_TYPE_ID     ");
		queryString.append("      WHERE TO_CHAR(E.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("	 AND DEP.BUDGET_DEP_CODE='").append(depCode).append("'   ");
		}
		if (!StringUtils.isBlank(project)) {
			queryString.append("          AND PROJECT_NO ='").append(project).append("'      ");
		}
		queryString.append("          )     ");
		queryString.append("          GROUP BY   PROJECT_CODE, COST_UNIT_CODE   ");
		queryString.append("          ) EXPAPPLC ON BI.PROJECT_CODE=EXPAPPLC.PROJECT_CODE     ");
		queryString.append("     LEFT JOIN (     ");
		queryString.append("          SELECT      ");
		queryString.append("          DISTINCT     ");
		queryString.append("       DEP.BUDGET_DEP_CODE AS DEPCODE,       ");
		queryString.append("           CASE WHEN DEP.BUDGET_DEP_CODE ='A2B000' THEN N'營業管理部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2E000' THEN N'業務推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2F000' THEN N'營業推展部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2K000' THEN N'展業部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2M000' THEN N'業務開發部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='A2Y000' THEN N'團體意外險部外埠'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='111500' THEN N'放款區域中心本部'        ");
		queryString.append("              WHEN DEP.BUDGET_DEP_CODE ='101100' THEN N'總管理處'     ");
		queryString.append("             ELSE DEP.NAME  END AS DEPNAME     ");
		queryString.append("          FROM TBEXP_YEAR_DEPARTMENT DEP      ");
		queryString.append("          INNER JOIN TBEXP_DEP_LEVEL_PROP LEV ON DEP.TBEXP_DEP_LEVEL_PROP_ID = LEV.ID       ");
		queryString.append("      WHERE DEP.ENABLED=1 AND (LEV.CODE=1 OR DEP.BUDGET_DEP_CODE IN('A2B000','A2E000','A2F000','A2K000','A2M000','A2Y000'))         ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("          ) DEP ON  EXPAPPLC.COST_UNIT_CODE =DEP.DEPCODE     ");
		queryString.append("      WHERE( DEP.DEPCODE=BI.DEPCODE OR (DECODE(EXPAPPLC.AMTA,NULL,0,EXPAPPLC.AMTA) !=0 OR DECODE(EXPAPPLC.AMTB,NULL,0,EXPAPPLC.AMTB) !=0 ) OR DEP.DEPCODE IS NULL)         ");
		if (depCode == null || !depCode.equals("ALL")) {
			queryString.append("       AND   ((CASE WHEN  DEP.DEPCODE IS NULL THEN BI.DEPCODE       ");
			queryString.append("             ELSE DEP.DEPCODE END) ='").append(depCode).append("'  )     ");
		}
		queryString.append("         ORDER BY BI.PROJECT_CODE   ");
		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}

	}

	/**
	 * C11.8.6專案預算實支查詢明細下載-明細查詢
	 */
	public List findYearDepDetailData(Calendar endDate, String project) {
		StringBuilder queryString = new StringBuilder();
		queryString.append("       SELECT    ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(MAIN.SUBPOENA_DATE, 'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           MAIN.SUBPOENA_NO AS SubpoenaNo,   ");
		queryString.append("		   bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	       bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,   ");
		queryString.append("           ACCT.NAME AS AcctName,   ");
		queryString.append("           SUB.PROJECT_NO AS ProjectNo,   ");
		queryString.append("           DECODE(ET.ENTRY_VALUE, 'D', E.AMT, 'C', -1 * E.AMT) AS Amt,   ");
		queryString.append("           E.SUMMARY AS Summary,   ");
		queryString.append("           E.COST_CODE AS CostCode,  ");
		queryString.append("          CASE WHEN CRUSER.CODE IS NOT NULL THEN CRUSER.NAME   ");
		queryString.append("                WHEN DRUSER.CODE IS NOT NULL THEN DRUSER.NAME   ");
		queryString.append("                ELSE N' '  ");
		queryString.append("           END AS Creater,   ");
		queryString.append("          CASE WHEN APPL.USER_ID IS NOT NULL THEN APPL.USER_NAME  ");
		queryString.append("               WHEN MAL.TBEXP_PAYMENT_TARGET_ID IS NOT NULL THEN MAL.TBEXP_PAYMENT_TARGET_ID   ");
		queryString.append("               ELSE N' '  ");
		queryString.append("          END AS Receiver,   ");
		queryString.append("          CASE   WHEN VCUSER.CODE IS NOT NULL THEN VCUSER.NAME    ");
		queryString.append("				 WHEN VDUSER.code IS NOT NULL THEN VDUSER.name ELSE N' ' END 	AS VCNAME  ");
		queryString.append("          FROM TBEXP_ENTRY E    ");
		queryString.append("          INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON E.COST_UNIT_CODE = DEP.CODE   ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = E.TBEXP_ACC_TITLE_ID   ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=E.TBEXP_ENTRY_TYPE_ID  ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_EXP_SUB SUB ON SUB.TBEXP_ENTRY_ID = E.ID   ");
		queryString.append("          LEFT JOIN TBEXP_EXPAPPL_C C ON C.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_USER VCUSER ON C.TBEXP_VERIFY_USER_ID=VCUSER.ID      ");
		queryString.append("          LEFT JOIN TBEXP_USER CRUSER ON CRUSER.ID=C.TBEXP_CREATE_USER_ID     ");
		queryString.append("          LEFT JOIN TBEXP_EXPAPPL_D D ON D.TBEXP_ENTRY_GROUP_ID=E.TBEXP_ENTRY_GROUP_ID   ");
		queryString.append("          LEFT JOIN TBEXP_USER VDUSER ON D.TBEXP_REAMGR_USER_ID=VDUSER.ID     ");
		queryString.append("          LEFT JOIN TBEXP_USER DRUSER ON CRUSER.ID=D.TBEXP_CREATE_USER_ID     ");
		queryString.append("          LEFT JOIN TBEXP_MALACC_APPL MAL ON MAL.ID=D.ID   ");
		queryString.append("          LEFT JOIN TBEXP_APPL_INFO APPL ON C.TBEXP_DRAW_MONEY_USER_INFO_ID = appl.ID    ");
		queryString.append("          LEFT JOIN TBEXP_USER cuser ON MAIN.CREATE_USER_ID = cuser.ID    ");
		queryString.append("      	WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("           AND SUB.PROJECT_NO IN  ('").append(project).append("')  ");
		queryString.append("            AND SUBSTR(MAIN.EXP_APPL_NO,1,3)!='T07'  ");
		queryString.append("          UNION  ALL ");
		queryString.append("          SELECT  ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(MAIN.SUBPOENA_DATE, 'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           MAIN.SUBPOENA_NO AS SubpoenaNo,  ");
		queryString.append("			     	bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	                bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,  ");
		queryString.append("           ACCT.NAME AS AcctName,  ");
		queryString.append("           DETAIL.PROJECT_NO AS ProjectNo, ");
		queryString.append("           DETAIL.ESTIMATION_AMT AS Amt,   ");
		queryString.append("           DETAIL.SUMMARY AS Summary,   ");
		queryString.append("           N'',   ");
		queryString.append("           DRUSER.NAME  AS Creater,  ");
		queryString.append("          N'' ,   ");
		queryString.append("          U.NAME   ");
		queryString.append("          FROM TBEXP_EXPAPPL_D D  ");
		queryString.append("          INNER JOIN TBEXP_USER DRUSER ON DRUSER.ID=D.TBEXP_CREATE_USER_ID      ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_APPL DAPPL ON DAPPL.TBEXP_EXPAPPL_D_ID=D.ID    ");
		queryString.append("          INNER JOIN TBEXP_DEP_ACCEXP_DETAIL DETAIL ON DETAIL.TBEXP_DEP_ACCEXP_APPL_ID=DAPPL.ID   ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ACCT.ID = DETAIL.TBEXP_ACC_TITLE_ID  ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_USER U ON U.ID=D.TBEXP_REAMGR_USER_ID    ");
		queryString.append("          INNER JOIN TBEXP_EXP_MAIN MAIN ON MAIN.TBEXP_ENTRY_GROUP_ID=D.TBEXP_ENTRY_GROUP_ID  ");
		queryString.append("          INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON DEP.code=DETAIL.COST_UNIT_CODE   ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append("  		 AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("      	WHERE TO_CHAR(MAIN.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("          AND DETAIL.PROJECT_NO IN  ('").append(project).append("')  ");
		queryString.append("          UNION  ALL   ");
		queryString.append("       SELECT    ");
		queryString.append("           DEP.CODE as depCode,    ");
		queryString.append("           DEP.NAME as depName,  ");
		queryString.append("           TO_CHAR(ESE.SUBPOENA_DATE,'YYYY/MM/DD') AS SubpoenaDate,   ");
		queryString.append("           ESE.SUBPOENA_NO AS SubpoenaNo,   ");
		queryString.append("		   bugItem.NAME		AS BUDGETNAME,   ");
		queryString.append("	       bugItem.CODE		AS BUDGETCODE,   ");
		queryString.append("           ACCT.CODE AS AcctCode,   ");
		queryString.append("           ACCT.NAME AS AcctName,   ");
		queryString.append("           ESE.PROJECT_NO AS ProjectNo,   ");
		queryString.append("           DECODE(ET.ENTRY_VALUE, 'D', AMT, 'C', -1 * AMT) AS Amt,   ");
		queryString.append("           ESE.SUMMARY AS Summary,   ");
		queryString.append("           ESE.COST_CODE CostCode,   ");
		queryString.append("           N' ' Creater,   ");
		queryString.append("           N' ' Receiver,   ");
		queryString.append("           N' '    ");
		queryString.append("          FROM TBEXP_EXT_SYS_ENTRY ESE   ");
		queryString.append("          INNER JOIN TBEXP_ACC_TITLE ACCT ON ESE.ACCT_CODE = ACCT.CODE   ");
		queryString.append("          left join TBEXP_BUDGET_ITEM bugItem on (ACCT.TBEXP_BUG_ITEM_ID = bugItem.ID) ");
		queryString.append("          INNER JOIN TBEXP_ENTRY_TYPE ET ON ET.ID=ESE.TBEXP_ENTRY_TYPE_ID  ");
		queryString.append("          INNER JOIN  TBEXP_YEAR_DEPARTMENT DEP  ON ESE.COST_UNIT_CODE = DEP.CODE    ");
		//defect4914_year_department 未加入年度 CU3178 2018/1/9 START
		queryString.append(" 		  AND DEP.YEAR=SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) ");
		 //defect4914_year_department 未加入年度 CU3178 2018/1/9 END
		queryString.append("      	  WHERE TO_CHAR(ESE.SUBPOENA_DATE,'YYYYMMDD') 	 ");
		queryString.append("   		  BETWEEN SUBSTR('").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("',1,4) || '0101' ");
		queryString.append(" 		  AND '").append(DateUtils.getSimpleISODateStr(endDate.getTime())).append("'   ");
		queryString.append("          AND ESE.PROJECT_NO IN  ('").append(project).append("')  ");

		List<Object> parameters = new ArrayList<Object>();
		List list = findByNativeSQL(queryString.toString(), parameters);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}
	}
	// RE201701547_費用系統預算優化第二階段 EC0416 2017/5/5 END

}