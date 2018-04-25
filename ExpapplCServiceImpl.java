package tw.com.skl.exp.kernel.model6.logic.impl;

import static tw.com.skl.exp.kernel.model6.common.util.MessageUtils.getAccessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

import tw.com.skl.common.model6.logic.impl.BaseServiceImpl;
import tw.com.skl.common.model6.web.jsf.utils.Messages;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.AccTitle.IncomeFormCode;
import tw.com.skl.exp.kernel.model6.bo.ApplInfo;
import tw.com.skl.exp.kernel.model6.bo.ApplQuota;
import tw.com.skl.exp.kernel.model6.bo.ApplState;
import tw.com.skl.exp.kernel.model6.bo.BigType;
import tw.com.skl.exp.kernel.model6.bo.BudgetIn;
import tw.com.skl.exp.kernel.model6.bo.CaseWCtrl;
import tw.com.skl.exp.kernel.model6.bo.CashGiftPerApplQuota;
import tw.com.skl.exp.kernel.model6.bo.CashGiftYyApplQuota;
import tw.com.skl.exp.kernel.model6.bo.DeliverDaylist;
import tw.com.skl.exp.kernel.model6.bo.DepApplQuota;
import tw.com.skl.exp.kernel.model6.bo.DepApplQuotaDetail;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.Entry;
import tw.com.skl.exp.kernel.model6.bo.EntryGroup;
import tw.com.skl.exp.kernel.model6.bo.EntryType;
import tw.com.skl.exp.kernel.model6.bo.EventAllowanceQuota;
import tw.com.skl.exp.kernel.model6.bo.ExpapplC;
import tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail;
import tw.com.skl.exp.kernel.model6.bo.FlowCheckstatus;
import tw.com.skl.exp.kernel.model6.bo.GainPerson;
import tw.com.skl.exp.kernel.model6.bo.HealthEntryLog;
import tw.com.skl.exp.kernel.model6.bo.IncomeIdType;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.OvsaExpDrawInfo;
import tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp;
import tw.com.skl.exp.kernel.model6.bo.PaymentType;
import tw.com.skl.exp.kernel.model6.bo.PhoneFeeDetail;
import tw.com.skl.exp.kernel.model6.bo.PhoneInfo;
import tw.com.skl.exp.kernel.model6.bo.PhoneRoster;
import tw.com.skl.exp.kernel.model6.bo.Position.PositionCode;
import tw.com.skl.exp.kernel.model6.bo.QuotaDetail;
import tw.com.skl.exp.kernel.model6.bo.QuotaItem;
import tw.com.skl.exp.kernel.model6.bo.RegisterExpApplDetail;
import tw.com.skl.exp.kernel.model6.bo.RentExp;
import tw.com.skl.exp.kernel.model6.bo.ReturnStatement;
import tw.com.skl.exp.kernel.model6.bo.RosterDetail;
import tw.com.skl.exp.kernel.model6.bo.Subpoena;
import tw.com.skl.exp.kernel.model6.bo.TransitPaymentDetail;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.bo.VendorExp;
import tw.com.skl.exp.kernel.model6.bo.AccClassType.AccClassTypeCode;
import tw.com.skl.exp.kernel.model6.bo.AccTitle.AccTitleCode;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.BigType.BigTypeCode;
import tw.com.skl.exp.kernel.model6.bo.Department.DepartmentCode;
import tw.com.skl.exp.kernel.model6.bo.DepartmentLevelProperty.DepartmentLevelPropertyCode;
import tw.com.skl.exp.kernel.model6.bo.DepartmentType.DepartmentTypeCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeValueCode;
import tw.com.skl.exp.kernel.model6.bo.ExpItem.ExpItemCode;
import tw.com.skl.exp.kernel.model6.bo.ExpType.ExpTypeCode;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.Group.GroupCode;
import tw.com.skl.exp.kernel.model6.bo.IncomeIdType.IncomeIdTypeCode;
import tw.com.skl.exp.kernel.model6.bo.IncomeUserType.IncomeUserTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ListType.ListTypeCode;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentTarget.PaymentTargetCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentType.PaymentTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ProofType.ProofTypeCode;
import tw.com.skl.exp.kernel.model6.bo.RosterState.RosterStateCode;
import tw.com.skl.exp.kernel.model6.bo.SysType.SysTypeCode;
import tw.com.skl.exp.kernel.model6.bo.TaxType.TaxTypeCode;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpException;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.ArrayUtils;
import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dao.ExpapplCDao;
import tw.com.skl.exp.kernel.model6.dto.ExpapplCDto;
import tw.com.skl.exp.kernel.model6.dto.ExpapplCMaintainDto;
import tw.com.skl.exp.kernel.model6.dto.ReturnExpapplCDto;
import tw.com.skl.exp.kernel.model6.dto.RtnItemApplDto;
import tw.com.skl.exp.kernel.model6.dto.VendorExpDto;
import tw.com.skl.exp.kernel.model6.facade.ExpapplCFacade;
import tw.com.skl.exp.kernel.model6.logic.ExpapplCService;
import tw.com.skl.exp.kernel.model6.logic.enumeration.ApplStateEnum;
import tw.com.skl.exp.kernel.model6.sn.AbstractSNGenerator;
import tw.com.skl.exp.kernel.model6.sn.BatchNoGenerator;
import tw.com.skl.exp.kernel.model6.sn.ExpApplNoGenerator;
import tw.com.skl.exp.kernel.model6.sn.SNGenerator;

/**
 * 行政費用申請單 Service 類別。
 * 
 * @author Sabin Pan
 * @version 1.0, 2009/4/16
 */
public class ExpapplCServiceImpl extends BaseServiceImpl<ExpapplC, String, ExpapplCDao> implements ExpapplCService {
	protected Log logger = LogFactory.getLog(this.getClass());
	private ExpapplCFacade facade;
	/* RE201201260 二代健保 匯回款項 20130222 START */
	private String paybackType = "";
	private String projectCode;

	/* RE201201260 二代健保 匯回款項 20130222 End */
	/**
	 * @param facade
	 *            the facade to set
	 */
	public void setFacade(ExpapplCFacade facade) {
		this.facade = facade;
	}

	/**
	 * @return the facade
	 */
	public ExpapplCFacade getFacade() {
		return facade;
	}

	public Integer doApproveGovApplyForm(List<ExpapplC> expApplcs) throws ExpException {
		if (expApplcs == null) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { "List<ExpapplC>" });
		}
		// 登入的使用者
		User logonUser = getLoginUser();
		// 比較是否超過限額時，所需的費用申請人
		// User applUser = null;
		int count = 0;
		for (ExpapplC eac : expApplcs) {

			// 借貸是否平衡
			this.facade.getEntryGroupService().calcBalance(eac.getEntryGroup());

			if (!eac.getEntryGroup().isBalanced()) {
				// 顯示《借貸不平衡,申請單號:{0}》
				throw new ExpRuntimeException(ErrorCode.C10531, new String[] { eac.getExpApplNo() });
			}
			// 檢核過渡付款明細金額是否相等於費用科目金額
			checkTransitPaymentDetailAmountByExpApplNo(eac.getExpApplNo());

			// 「費用申請單.申請單狀態」不為"審核中"
			// 2009/12/21,IR#1551
			if (!(ObjectUtils.equals(ApplStateCode.FIRST_VERIFICATION.getCode(), eac.getApplState().getCode()) || ObjectUtils.equals(ApplStateCode.FIRST_VERIFICATION_REJECTED.getCode(), eac.getApplState().getCode()) || ObjectUtils.equals(ApplStateCode.RE_VERIFICATION_SEND.getCode(), eac.getApplState().getCode()) || ObjectUtils.equals(ApplStateCode.RETURN_EXPAPPL_VERIFICATION.getCode(), eac.getApplState().getCode()))) {
				throw new ExpRuntimeException(ErrorCode.C10019);
			}
			// 檢核：費用項目：主管婚喪禮金補助
			if (eac.getExpItem() != null && ExpItemCode.CHIEF_CASHGIFT_ALLOWANCE.getCode().equals(eac.getExpItem().getCode())) {
				// 須要先查出申請人
				// 檢核用共用method來作就好
				checkCashGiftApplQuota(eac);
			}
			// 設定所有「費用申請單.實際初審經辦」為登入的使用者，並將「費用申請單.申請單狀態」設為”已初審”
			eac.setActualVerifyUser(logonUser);
			eac.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.FIRST_VERIFIED, SysTypeCode.C));

			// 儲存申請單資料，並記錄”流程簽核歷程”
			getDao().update(eac);
			facade.getFlowCheckstatusService().createByExpApplC(eac, FunctionCode.C_3_2_2, Calendar.getInstance());
			count++;
		}
		return count;
	}

	public void doConfirmDelete(List<ExpapplC> expapplCs) {
		// TODO Auto-generated method stub

	}

	public void doDeleteGovApplyForm(List<ExpapplC> expApplcs) throws ExpException {
		if (CollectionUtils.isEmpty(expApplcs)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}
		Calendar nowDate = Calendar.getInstance();
		for (ExpapplC eac : expApplcs) {
			// 獎金品相關的申請單，必須於刪除時回復名冊的狀態
			try {
				// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 START
				if (eac.getMiddleType().getCode().equals(MiddleTypeCode.CODE_N10.getCode())) {
					// C1.5.13名冊輸入方式從TBEXP_EXPAPPL_C.LIST1、LIST2改為TBEXP_ROSTER_DETAIL.TBEXP_EXPAPPL_C的方式
					updateRosterStateList(1, eac);
				} else {
					List<String> listNos = new ArrayList<String>();
					if (StringUtils.isNotBlank(eac.getListNo1()))
						listNos.add(eac.getListNo1());
					if (StringUtils.isNotBlank(eac.getListNo2()))
						listNos.add(eac.getListNo2());
					if (eac.getListType() != null && ListTypeCode.PREMIUM_AWARD.getCode().equals(eac.getListType().getCode())) {
						updateRosterState(1, eac.getExpApplNo(), listNos);
					}
					// RE201600212 電話費刪除狀態會改變 CU3178 2016/3/9 START
					// 電話費
					else if (ListTypeCode.TELEPHONE.getCode().equals(eac.getListType().getCode())) {
						List<PhoneRoster> prs = facade.getPhoneRosterService().findByListNo(eac.getListNo1());
						if (CollectionUtils.isNotEmpty(prs)) {
							// 更新「電話費名冊.電話費明細」List，參數傳'0'表示要回復名冊資料
							updatePhoneRosterState(0, eac.getExpApplNo(), eac.getListNo1());
						}
					}
					// RE201600212 電話費刪除狀態會改變 CU3178 2016/3/9 END
				}
				// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 END
			} catch (Exception e) {
				e.printStackTrace();
				logger.debug(e);
			}
			eac.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.DELETED, SysTypeCode.C));
			facade.getFlowCheckstatusService().createByExpApplC(eac, FunctionCode.C_3_2_2, nowDate);
			getDao().update(eac);
		}
	}

	public ExpapplC doReapplyApplyForm() {
		// TODO Auto-generated method stub
		return null;
	}

	public ExpapplC doReturnInvestApplyForm() {
		// TODO Auto-generated method stub
		return null;
	}

	public void doRtnItemResend(List<String> applyFormNoList, FunctionCode functionCode) throws ExpException {
		if (CollectionUtils.isEmpty(applyFormNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10020);
		}
		Calendar sysDate = Calendar.getInstance();

		// 查出費用申請單
		List<ExpapplC> expapplCList = this.findByApplNo(applyFormNoList);

		ApplState applState = null;

		for (ExpapplC expapplC : expapplCList) {
			if (ApplStateCode.APPLICATION_REJECTED.getCode().equals(expapplC.getApplState().getCode())) {
				// 若原申請單狀態為「退件」時狀態變更為「重新送件」
				applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.RE_VERIFICATION_SEND, SysTypeCode.C);
				expapplC.setApplState(applState);
			} else {
				continue;
			}
			expapplC.setUpdateDate(sysDate);
			expapplC.setUpdateUser(getLoginUser());

			// 儲存申請單資料
			this.update(expapplC);

			// 記錄流程簽核歷程
			this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
		}

	}

	public void doRtnItemReapplied(String expApplNo, FunctionCode functionCode) {
		if (StringUtils.isBlank(expApplNo)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}
		Calendar sysDate = Calendar.getInstance();
		List<String> expApplNoList = new ArrayList<String>();
		expApplNoList.add(expApplNo);
		// 查出費用申請單
		List<ExpapplC> expapplCList = this.findByApplNo(expApplNoList);

		// 「費用申請單」的狀態設為”申請中” ，並儲存之
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.APPLIED, SysTypeCode.C);

		for (ExpapplC expapplC : expapplCList) {
			expapplC.setApplState(applState);
			expapplC.setUpdateDate(sysDate);
			expapplC.setUpdateUser(getLoginUser());

			// 儲存申請單資料
			this.update(expapplC);

			// 記錄流程簽核歷程
			this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
		}
	}

	public List<ExpapplC> findApplyForDailyStmt(User user) {
		// Map<String, Object> params = new HashMap<String, Object>();
		// StringBuffer queryString = new StringBuffer();
		// queryString.append(" select expapplC from ExpapplC expapplC");
		// queryString.append(" left join fetch expapplC.dailyStatement");
		// queryString.append(" where ");
		// queryString.append(" ((expapplC.paymentType.code = '1' and expapplC.paymentType.sysType.code = 'C')");
		// queryString.append(" and (expapplC.paymentTarget.code = '2' and expapplC.paymentTarget.sysType.code = 'C')");
		// queryString
		// .append(" and expapplC.middleType.code in ('N10','M10','M20','L10','L20','E10','H20','K10','K20','R20')");
		// queryString.append(" and expapplC.applState.code = :applStatCode");
		// queryString.append(" and expapplC.actualVerifyUser = :actualVerifyUser)");
		// queryString.append(" or ((expapplC.paymentType.code = '2' and expapplC.paymentType.sysType.code = 'C')");
		// queryString.append(" and expapplC.middleType.code = 'N10'");
		// queryString.append(" and expapplC.applState.code = :applStatCode");
		// queryString.append(" and expapplC.actualVerifyUser = :actualVerifyUser)");
		// queryString.append(" or ((expapplC.paymentType.code = '5' and expapplC.paymentType.sysType.code = 'C')");
		// queryString.append(" and (expapplC.paymentTarget.code = '2' and expapplC.paymentTarget.sysType.code = 'C')");
		// queryString.append(" and expapplC.middleType.code in ('N10', 'E10')");
		// queryString.append(" and expapplC.applState.code = :applStatCode");
		// queryString.append(" and expapplC.actualVerifyUser = :actualVerifyUser)");
		// params.put("applStatCode", ApplStateCode.FIRST_VERIFIED.getCode());
		// params.put("actualVerifyUser", user);
		// return super.getDao().findByNamedParams(queryString.toString(),
		// params);

		// IISI-20100805: 因查詢速度過慢, 將JPQL查詢改成SQL查詢 Modify By Eustace
		List<String> expapplCNos = getDao().findApplyForDailyStmt(user);
		if (CollectionUtils.isEmpty(expapplCNos)) {
			return null;
		}

		return this.findByApplNo(expapplCNos);
	}

	public List<ExpapplC> findByCancelCode(Integer cancelCodeType, Integer stateType, BigType bigType, String cancelCode, Calendar createDateBegin, Calendar createDateEnd) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ExpapplC> findByDeliverDayListNo(String deliverDayListNo) throws ExpRuntimeException {
		if (StringUtils.isBlank(deliverDayListNo)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo") });
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct e");
		queryString.append(" from ExpapplC e");
		// queryString.append(" left join fetch e.deliverDaylist");
		queryString.append(" where e.deliverDaylist.deliverNo =:deliverNo");
		queryString.append(" order by e.expApplNo");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("deliverNo", deliverDayListNo);

		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	public List<ExpapplC> findForGovApprove(String bigTypeCode, String firstVerifyUserCode, String deliveryUnitCode, String costUnitCode, String applyUserCode, boolean isTempPay, Calendar returnDate, ApplStateCode[] states) {
		StringBuffer querySql = new StringBuffer();

		querySql.append("select distinct eac from ExpapplC eac" + " join fetch eac.entryGroup" + " join eac.entryGroup eg" + " join fetch eg.entries" + " join eg.entries en" + " join eac.middleType mt" + " left join eac.verifyUser vUser" + " join fetch eac.applyUserInfo" + " join fetch eac.deliverDaylist");
		// 臨時付款因為是boolean，所以直接放在查詢條件
		querySql.append(" where eac.temporaryPayment=:isTempPay");
		boolean truncated = false;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("isTempPay", isTempPay);
		truncated = true;
		// 費用大分類，查出大分類下的中分類
		List<MiddleType> midTypes = facade.getMiddleTypeService().findMiddleTypeByBigTypeCode(bigTypeCode);
		// 費用中分類
		querySql.append(" and (");
		// 把查出來的中分類資料，放到查詢條件中
		int i = 0;
		if (CollectionUtils.isNotEmpty(midTypes)) {
			for (MiddleType mt : midTypes) {
				querySql.append(" mt.code=:middleTypeCode" + i);
				params.put("middleTypeCode" + i, mt.getCode());
				querySql.append(" or");
				i++;
			}
			querySql.delete(querySql.lastIndexOf("or"), querySql.length());
			querySql.append(" ) and");
		}
		// 初審經辦
		if (StringUtils.isNotBlank(firstVerifyUserCode)) {
			if (!truncated) {
				querySql.append(" where");
				truncated = true;
			}
			querySql.append(" vUser.code=:firstVerifyUserCode");
			params.put("firstVerifyUserCode", firstVerifyUserCode);
			querySql.append(" and");
		}
		// 送件單位代號
		if (StringUtils.isNotBlank(deliveryUnitCode)) {
			if (!truncated) {
				querySql.append(" where");
				truncated = true;
			}
			querySql.append(" eac.createUser.department.code=:deliveryUnitCode");
			params.put("deliveryUnitCode", deliveryUnitCode);
			querySql.append(" and");
		}
		// 成本單位代號
		if (StringUtils.isNotBlank(costUnitCode)) {
			if (!truncated) {
				querySql.append(" where");
				truncated = true;
			}
			querySql.append(" en.costUnitCode=:costUnitCode");
			params.put("costUnitCode", costUnitCode);
			querySql.append(" and");
		}
		// 使用者代號
		if (StringUtils.isNotBlank(applyUserCode)) {
			if (!truncated) {
				querySql.append(" where");
				truncated = true;
			}
			querySql.append(" eac.applyUserInfo.userName=:applyUserName");
			params.put("applyUserName", applyUserCode);
			querySql.append(" and");
		}
		// 申請單狀態
		boolean cutStringIsOR = false;
		if (states != null && states.length > 0) {
			int count = 0;
			if (!truncated) {
				querySql.append(" where (");
				truncated = true;
			} else {
				querySql.append(" (");
			}
			for (ApplStateCode state : states) {
				querySql.append(" eac.applState.code=:state" + count);
				params.put("state" + count, state.getCode());
				querySql.append(" or");
				count++;
			}
			cutStringIsOR = true;
		}
		// 如果申請單狀態有加入查詢的Sql中，那就要把or清掉，並且加上最後的)
		if (truncated && cutStringIsOR) {
			querySql.delete(querySql.lastIndexOf("or"), querySql.length());
			querySql.append(" )");
		} else if (truncated && !cutStringIsOR) {
			querySql.delete(querySql.lastIndexOf("and"), querySql.length());
		}
		// // 更新日期
		// if (returnDate != null) {
		// querySql.append(" order by eac.updateDate=:returnDate");
		// params.put("returnDate", returnDate);
		// truncated = false;
		// }
		// 2009/12/7,CR#245 ,改為用申請單號排序
		querySql.append(" order by eac.expApplNo");
		truncated = false;
		List<ExpapplC> list = null;
		list = getDao().findByNamedParams(querySql.toString(), params);
		return list;
	}

	public List<ExpapplC> findRemittedApply() {
		// TODO Auto-generated method stub
		return null;
	}

	public ExpapplC doReapplyGovApplyForm(ExpapplC expapplC) throws ExpException {
		if (expapplC == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC", null) });
		// 檢核費用申請單狀態
		if (!ObjectUtils.equals(ApplStateCode.APPLICATION_REJECTED.getCode(), expapplC.getApplState().getCode())) {
			throw new ExpRuntimeException(ErrorCode.C10020);
		}
		// 將「費用申請單.申請單狀態」設為"審核中"，並更新「費用申請單. 補辦完成日期」
		// 、「費用申請單.初審經辦」為登入的使用者
		Calendar nowDate = Calendar.getInstance();
		expapplC.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.FIRST_VERIFICATION, SysTypeCode.C));
		expapplC.setResendVerifyDate(nowDate);
		expapplC.setVerifyUser(this.getLoginUser());

		// 儲存申請單資料，並記錄「流程簽核歷程」
		expapplC = getDao().update(expapplC);
		facade.getFlowCheckstatusService().createByExpApplC(expapplC, FunctionCode.C_3_2_2, nowDate);

		return expapplC;
	}

	public ExpapplC doReturnGovApplyForm(ExpapplC expapplC, ReturnStatement rs) throws ExpException {
		if (expapplC == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC", null) });
		// 99時，檢核「費用申請單.申請單狀態」要等於”審核中”或”退單審核”，若否顯示《狀態錯誤，尚未完成退單》訊息，且須回復已更新狀態之資料列。
		String asCode = expapplC.getApplState().getCode();
		String returnCauseCode = rs.getReturnCause().getCode();
		if ("99".equals(returnCauseCode)) {
			if (!(ObjectUtils.equals(ApplStateCode.FIRST_VERIFICATION.getCode(), asCode) || ObjectUtils.equals(ApplStateCode.RETURN_EXPAPPL_VERIFICATION.getCode(), asCode))) {
				throw new ExpRuntimeException(ErrorCode.C10063);
			}// 變更狀態為”退單”
			if ("N10".equals(expapplC.getExpApplNo().substring(0, 3).toUpperCase())) {
				expapplC.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.RETURN_EXPAPPL, SysTypeCode.C));
			} else {// 且須回復已更新狀態之資料列
				throw new ExpRuntimeException(ErrorCode.C10063);
			}
		} else {
			// 2009/11/04,TIM,user需求為"審核中"或是"退單審核"狀態，才可以退件。2009/11/27,CR#227加上"退回審核"
			if ((ObjectUtils.equals(ApplStateCode.FIRST_VERIFICATION.getCode(), asCode) || ObjectUtils.equals(ApplStateCode.RETURN_EXPAPPL_VERIFICATION.getCode(), asCode) || ObjectUtils.equals(ApplStateCode.FIRST_VERIFICATION_REJECTED.getCode(), asCode))) {
				expapplC.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.APPLICATION_REJECTED, SysTypeCode.C));
			} else {
				throw new ExpRuntimeException(ErrorCode.C10060);
			}
		}
		// 設定「費用申請單.初審經辦」為登入的使用者
		expapplC.setVerifyUser(this.getLoginUser());
		// 儲存申請單資料，並記錄「流程簽核歷程」及「退件原因說明」
		expapplC = update(expapplC);
		facade.getFlowCheckstatusService().createByExpApplC(FunctionCode.C_3_2_2, expapplC, rs, Calendar.getInstance());
		return expapplC;
	}

	public ExpapplC doUpdateGovApplForm(ExpapplC expapplC) throws ExpException {
		// TODO 修改功能
		if (expapplC == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC", null) });
		return null;
	}

	public List<FlowCheckstatus> findReturnGovApplyRecords(String expApplNo) throws ExpException {

		ApplStateCode[] applStates = new ApplStateCode[] { ApplStateCode.APPLICATION_REJECTED, ApplStateCode.RETURN_EXPAPPL };
		List<FlowCheckstatus> flowCSList = facade.getFlowCheckstatusService().findByParams(expApplNo, SysTypeCode.C, applStates);

		return flowCSList;
	}

	public List<ExpapplC> findByApplNo(List<String> expApplNoList) {
		if (CollectionUtils.isEmpty(expApplNoList)) {
			return null;
		}

		StringBuffer queryString = new StringBuffer();
		queryString.append("select e");
		queryString.append(" from ExpapplC e");
		queryString.append(" where e.expApplNo in(");

		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		int index = 0;
		for (String expApplNo : expApplNoList) {
			if (index != 0) {
				queryString.append(", ");
			}
			String key = "expApplNo" + index;
			queryString.append(":" + key);
			criteriaMap.put(key, expApplNo);
			index++;
		}

		queryString.append(")");

		List<ExpapplC> expapplCList = getDao().findByNamedParams(queryString.toString(), criteriaMap);

		return expapplCList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seetw.com.skl.exp.kernel.model6.logic.ExpapplCService#
	 * findForExpapplCFetchRelation
	 * (tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode[],
	 * tw.com.skl.exp.kernel.model6.bo.MiddleType[], java.lang.String,
	 * tw.com.skl.exp.kernel.model6.bo.ProofType.ProofTypeCode,
	 * java.util.Calendar, java.util.Calendar, java.lang.Boolean,
	 * java.lang.String)
	 */
	// C 1.6.1
	public List<ExpapplC> findForExpapplCFetchRelation(ApplStateCode[] applStateCodeArray, MiddleTypeCode[] middleTypeCodeArray, String applUserCode, ProofTypeCode proofTypeCode, Calendar createDateStart, Calendar createDateEnd, Boolean temporaryPayment, String deliverNo, String createUserCode) {
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct exp");
		queryString.append(" from ExpapplC exp join exp.createUser createU");
		boolean truncated = false;

		Map<String, Object> params = new HashMap<String, Object>();
		User createUser = null;
		createUser = getLoginUser();
		if (createUser.getDepartment() == null) { // 沒有fetch到所屬單位的處理
			Department dep = facade.getDepartmentService().findByUser(createUser);
			createUser.setDepartment(dep);
		}
		// 2009/12/1,CR #240
		if (DepartmentTypeCode.PARENT_COMPANY.getCode().equals(createUser.getDepartment().getDepartmentType().getCode()) || DepartmentTypeCode.SERVICE_CENTER.getCode().equals(createUser.getDepartment().getDepartmentType().getCode())) {
			queryString.append(" where (createU.department.code=:loginUserDepCode1 or createU.department.code=:loginUserDepCode2) and ");
			truncated = true;
			String loginUserDepCode1 = createUser.getDepartment().getParentDepartment().getCode();
			String loginUserDepCode2 = createUser.getDepartment().getCode();
			params.put("loginUserDepCode1", loginUserDepCode1);
			params.put("loginUserDepCode2", loginUserDepCode2);
		} else {
			queryString.append(" where createU.department.code=:loginUserDepCode and ");
			truncated = true;
			params.put("loginUserDepCode", getLoginUser().getDepartment().getCode());
		}
		// 2009/12/16,CR#240,
		// 建檔人員工代號 : (選擇性輸入)空白時，代入登入人員員工代號
		if (StringUtils.isNotBlank(createUserCode)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			queryString.append(" createU.code =:createUserCode");
			params.put("createUserCode", createUserCode);
			queryString.append(" and");
		}
		// 申請單狀態
		if (!ArrayUtils.isEmpty(applStateCodeArray)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.applState.code in(");

			for (int index = 0; index < applStateCodeArray.length; index++) {

				if (index != 0) {
					queryString.append(" , ");
				}

				String str = "applStateCode" + index;

				queryString.append(":" + str);

				params.put(str, applStateCodeArray[index].getCode());
			}

			queryString.append(") ");

			queryString.append(" and");

			queryString.append(" exp.applState.sysType.code =:sysTypeCode");
			params.put("sysTypeCode", SysTypeCode.C.getCode());

			queryString.append(" and");

		} else {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			// 過濾掉刪件
			queryString.append(" exp.applState.code <>:applStateCode");
			params.put("applStateCode", ApplStateCode.DELETED.getCode());

			queryString.append(" and");

			queryString.append(" exp.applState.sysType.code =:sysTypeCode");
			params.put("sysTypeCode", SysTypeCode.C.getCode());

			queryString.append(" and");
		}

		// 中分類 : 「費用申請單.中分類」=傳入參數「中分類」
		if (!ArrayUtils.isEmpty(middleTypeCodeArray)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.middleType.code in(");

			for (int index = 0; index < middleTypeCodeArray.length; index++) {

				if (index != 0) {
					queryString.append(" , ");
				}

				String str = "middleType" + index;

				queryString.append(":" + str);

				params.put(str, middleTypeCodeArray[index].getCode());
			}

			queryString.append(") ");

			queryString.append(" and");

		}

		// 申請人員工代號 : 「費用申請單.申請人資訊.員工代號」=傳入參數「申請人員工代號」
		if (StringUtils.isNotBlank(applUserCode)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.applyUserInfo.userId =:applUserCode");
			params.put("applUserCode", applUserCode);

			queryString.append(" and");
		}

		// 憑証類別代碼 : 「費用申請單.憑證類別」=傳入參數「憑證類別」
		if (null != proofTypeCode) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			queryString.append(" exp.proofType.formatCode =:formatCode");
			params.put("formatCode", proofTypeCode.getFormatCode());
			queryString.append(" and");

			queryString.append(" exp.proofType.sysType.code =:sysTypeCode");
			params.put("sysTypeCode", proofTypeCode.getSysTypeCode().getCode());
			queryString.append(" and");
		}

		// 建檔日起 : 「費用申請單.建檔日期」>=傳入參數「付款年月起」的0:00
		if (null != createDateStart) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			queryString.append(" exp.createDate >=:createDateStart");
			params.put("createDateStart", createDateStart);
			queryString.append(" and");
		}

		// 建檔日迄 : 「費用申請單.建檔日期」<傳入參數「付款年月迄」+1天的0:00
		if (null != createDateEnd) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			createDateEnd.add(Calendar.DAY_OF_WEEK, 1);
			queryString.append(" exp.createDate <:createDateEnd");
			params.put("createDateEnd", createDateEnd);

			queryString.append(" and");
		}

		// 臨時付款 : 「費用申請單.臨時付款期」=傳入參數「臨時付款」
		if (null != temporaryPayment) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.temporaryPayment =:temporaryPayment");
			params.put("temporaryPayment", temporaryPayment.booleanValue());

			queryString.append(" and");
		}

		// 送件日計表單號
		if (StringUtils.isNotBlank(deliverNo)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.deliverDaylist.deliverNo =:deliverNo");
			params.put("deliverNo", deliverNo);

			queryString.append(" and");
		}

		if (truncated) {
			// 刪除最後一個and字串
			queryString.delete(queryString.lastIndexOf("and"), queryString.length());
		}
		// 2009/11/25,加上order by 作業日期
		queryString.append(" order by exp.workDate");
		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	public List<ExpapplC> findByDeliverNo(String deliverNo) throws ExpException {
		if (StringUtils.isBlank(deliverNo)) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_DailyStatement_deliverNo", null) };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct e");
		queryString.append(" from ExpapplC e");
		queryString.append(" left join fetch e.dailyStatement");
		queryString.append(" where e.dailyStatement.deliverNo =:deliverNo order by e.costTypeCode");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("deliverNo", deliverNo);

		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#findByParams(java.
	 * util.List, tw.com.skl.exp.kernel.model6.bo.BigType, java.lang.Boolean,
	 * java.lang.String,
	 * tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode)
	 */
	public List<ExpapplC> findByParams(List<String> deliverNos, BigType bigType, Boolean isTemporaryPayment, String generalMgrUserId, ApplStateCode stateCode) {
		if (CollectionUtils.isEmpty(deliverNos)) {
			List<String> errorStringList = new ArrayList<String>();
			if (CollectionUtils.isEmpty(deliverNos)) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo"));
			}
			String[] params = { errorStringList.toString() };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		StringBuffer queryString = new StringBuffer();

		queryString.append("Select distinct expapplC");
		queryString.append(" From ExpapplC expapplC");
		queryString.append(" left join expapplC.deliverDaylist deliverDaylist");

		boolean truncated = false;

		Map<String, Object> params = new HashMap<String, Object>();

		// 送件日計表單號s
		if (!CollectionUtils.isEmpty(deliverNos)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" deliverDaylist.deliverNo in(");

			for (int index = 0; index < deliverNos.size(); index++) {

				if (index != 0) {
					queryString.append(" , ");
				}

				String str = "deliverNo" + index;

				queryString.append(":" + str);

				params.put(str, deliverNos.get(index));
			}

			queryString.append(") ");

			queryString.append(" and");

		}

		// 大分類
		if (null != bigType && StringUtils.isNotBlank(bigType.getCode())) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			queryString.append(" expapplC.deliverDaylist.bigType.code =:bigTypeCode");
			params.put("bigTypeCode", bigType.getCode());

			queryString.append(" and");

		}

		// 臨時付款
		if (null != isTemporaryPayment) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" expapplC.temporaryPayment =:temporaryPayment");
			params.put("temporaryPayment", isTemporaryPayment.booleanValue());

			queryString.append(" and");
		}

		// 實際初審經辦
		if (StringUtils.isNotBlank(generalMgrUserId)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" expapplC.actualVerifyUser.code =:generalMgrUserId");
			params.put("generalMgrUserId", generalMgrUserId);

			queryString.append(" and");
		}

		// 申請單狀態代碼
		if (null != stateCode) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" expapplC.applState.code =:applStateCode");
			params.put("applStateCode", stateCode.getCode());
			queryString.append(" and");

			queryString.append(" expapplC.applState.sysType.code =:sysTypeCode");
			params.put("sysTypeCode", SysTypeCode.C.getCode());
			queryString.append(" and");
		}

		if (truncated) {
			// 刪除最後一個and字串
			queryString.delete(queryString.lastIndexOf("and"), queryString.length());
		}

		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		} else {
			return list;
		}
	}

	public void checkInvoiceData(ExpapplC eac) {

		if (null == eac) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}
		// 若「費用申請單.憑證附於」欄位有值時，回傳主程式(Defect 2092) 2010/03/02 By eustace
		if (StringUtils.isNotBlank(eac.getProofAdd())) {
			return;
		}

		// 2009/12/18,CR#249,修改檢核邏輯
		if (eac.isWithholdIncome()) {
			if (eac.getEntryGroup() == null || (eac.getEntryGroup() != null && eac.getEntryGroup().getEntries() == null))
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_Entry") });
			String acctCode = "";
			int countDebit = 0;
			int countCredit = 0;
			// 所有「分錄」List中，「分錄.會計科目」僅能且需有一筆進項(進項科目代號10531001~10531005)
			for (Entry en : eac.getEntryGroup().getEntries()) {
				acctCode = en.getAccTitle() != null ? en.getAccTitle().getCode() : "";
				if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
					if (facade.getAccTitleService().isIncomeAccTitle(acctCode)) {
						countDebit++;
					}
				} else {
					if (facade.getAccTitleService().isIncomeAccTitle(acctCode)) {
						countCredit++;
					}
				}
			}
			if (countDebit > 1)
				throw new ExpRuntimeException(ErrorCode.C10418);
			if (countCredit > 1)
				throw new ExpRuntimeException(ErrorCode.C10431);
			// 「費用申請單.課稅別」須為”1 應稅”
			if (eac.getTaxType() == null || (eac.getTaxType() != null && !TaxTypeCode.A.getCode().equals(eac.getTaxType().getCode())))
				throw new ExpRuntimeException(ErrorCode.C10419);
			// 「費用申請單.扣抵代號」不能為NULL值
			if (eac.getCompensateType() == null)
				throw new ExpRuntimeException(ErrorCode.C10410, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_compensateType") });
			// 2009/12/28，要分憑證類別，若「憑證類別=22、 24」時(發票號碼可為空白)
			String formatCode = ProofTypeCode.PROOF_TYPE_C_22.getFormatCode() + "&" + ProofTypeCode.PROOF_TYPE_C_24.getFormatCode();
			if (formatCode.indexOf(eac.getProofType().getFormatCode()) != -1) {
				// 「費用申請單.發票日期」、「費用申請單.廠商統編」不能為為空值
				if (eac.getInvoiceDate() == null || StringUtils.isBlank(eac.getRelationVendorCompId())) {
					throw new ExpRuntimeException(ErrorCode.C10410, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceDate") + "、" + getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_relationVendorCompId") });
				}
			} else {
				// 「費用申請單.發票號碼」、「費用申請單.發票日期」、「費用申請單.廠商統編」不能為為空值
				if (StringUtils.isBlank(eac.getInvoiceNo()) || eac.getInvoiceDate() == null || StringUtils.isBlank(eac.getRelationVendorCompId())) {
					throw new ExpRuntimeException(ErrorCode.C10410, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo") + "、" + getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceDate") + "、" + getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_relationVendorCompId") });
				}
			}
			// 「費用申請單.憑證金額(含)」、「費用申請單.憑證金額(未)」、「費用申請單.憑證金額(稅)」金額不可為0
			if (eac.getInvoiceAmt().compareTo(BigDecimal.ZERO) <= 0 || eac.getInvoiceNoneTaxAmt().compareTo(BigDecimal.ZERO) <= 0 || eac.getInvoiceTaxAmt().compareTo(BigDecimal.ZERO) <= 0) {
				throw new ExpRuntimeException(ErrorCode.C10411, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceAmt") + "、" + getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNoneTaxAmt") + "、" + getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceTaxAmt") });
			}
			// 「費用申請單.憑證類別」不可為”6 茲收到”
			if (ProofTypeCode.PROOF_TYPE_D_X.getFormatCode().equals(eac.getProofType().getCode()))
				throw new ExpRuntimeException(ErrorCode.C10412);
		} else {
			// String formatCode3 =
			// ProofTypeCode.PROOF_TYPE_C_21.getFormatCode()
			// +"&"+ProofTypeCode.PROOF_TYPE_C_25.getFormatCode();
			// 「費用申請單.課稅別」須為 NULL
			if (eac.getTaxType() != null)
				throw new ExpRuntimeException(ErrorCode.C10413, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_taxType"), getAplctnMsg("tw_com_skl_msg_empty") });
			// 「費用申請單.扣抵代號」須為NULL值
			if (eac.getCompensateType() != null)
				throw new ExpRuntimeException(ErrorCode.C10413, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_ExpapplC_compensateType"), getAplctnMsg("tw_com_skl_msg_empty") });
			// 「費用申請單.憑證金額(未)」、「費用申請單.憑證金額(稅)」金額須為0
			if (eac.getInvoiceNoneTaxAmt().compareTo(BigDecimal.ZERO) > 0 || eac.getInvoiceTaxAmt().compareTo(BigDecimal.ZERO) > 0)
				throw new ExpRuntimeException(ErrorCode.C10416);
			// 2009/12/22,CR#254該檢核取消
			// //若「費用申請單.憑證類別」等於”21三聯式發票”或 “25收銀機”，「費用申請單.廠商統編(關係人)」不能為為空值
			// if(formatCode3.indexOf(eac.getProofType().getFormatCode())!=-1 &&
			// StringUtils.isBlank(eac.getRelationVendorCompId()))
			// throw new ExpRuntimeException(ErrorCode.C10414);
		}
		if (StringUtils.isNotBlank(eac.getInvoiceNo()))
			// 檢核費用申請單中，發票號碼是否合法
			checkInvoiceLegality(eac);
	}

	private String getAplctnMsg(String code) {
		return MessageUtils.getAccessor().getMessage(code);
	}

	public List<ExpapplC> findExpapplCbyInvoiceData(Map<String, Object> params) {
		StringBuffer querySql = new StringBuffer();
		querySql.append("select expapplC from ExpapplC expapplC" + " join expapplC.applState apst" + " where" + " expapplC.invoiceNo=:invoiceNo" + " and expapplC.invoiceDate between :start_date and :end_date " + " and apst.code<>:applStateCode" + " and expapplC.expApplNo<>:expApplNo");

		if (StringUtils.isNotBlank((String) params.get("expApplNo"))) {
			querySql.append(" and expapplC.proofAdd <>:expApplNo");
		}
		return getDao().findByNamedParams(querySql.toString(), params);
	}

	public void checkInvoiceLegality(ExpapplC expapplC) {
		if (expapplC == null) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail_expapplC", null) };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		// 刪除 1. If 傳入的「費用申請單.是否扣抵進項稅」=N，回傳至主程式，不檢核。 Defect:2097 2010/03/02 By
		// Eustace
		// if(!expapplC.isWithholdIncome()){
		// return;
		// }
		// 2009/11/18,加入條件:憑證類別為茲收到，且沒填發票號碼，2009/12/29CR#239加上"其他憑證/長條二聯式發票"
		if (ProofTypeCode.PROOF_TYPE_D_X.getFormatCode().equals(expapplC.getProofType().getFormatCode()) || ProofTypeCode.PROOF_TYPE_C_22.getFormatCode().equals(expapplC.getProofType().getFormatCode())) {
			return;
		}
		// 分開檢核發票日期和發票號碼
		if (expapplC.getInvoiceDate() == null) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceDate", null) };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		if (StringUtils.isEmpty(expapplC.getInvoiceNo())) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo", null) };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		String yearMonth = DateUtils.getSimpleISODateStr(expapplC.getInvoiceDate().getTime()).substring(0, 6);
		String invoiceCode = expapplC.getInvoiceNo().substring(0, 2);
		String invoiceNo = expapplC.getInvoiceNo();
		if (invoiceNo.length() != 10) {
			throw new ExpRuntimeException(ErrorCode.A10066, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_InvoiceCode_length") });
		}
		Pattern pattern = Pattern.compile("[0-9]*");
		for (int i = 2; i < 10; i++) {
			if (!pattern.matcher(invoiceNo.substring(i, i + 1)).matches()) {
				throw new ExpRuntimeException(ErrorCode.A10066, new String[] { getAplctnMsg("tw_com_skl_exp_kernel_model6_bo_InvoiceCode_pattern") });
			}
		}
		if (StringUtils.isEmpty(expapplC.getProofAdd())) {
			if (facade.getInvoiceCodeService().isInvoiceDuplicate(yearMonth, expapplC.getInvoiceNo(), expapplC.getExpApplNo()))
				throw new ExpRuntimeException(ErrorCode.A10022);
		}
		if (!facade.getInvoiceCodeService().isInvoiceCodeLegal(yearMonth, invoiceCode)) {
			throw new ExpRuntimeException(ErrorCode.A10025, new String[] { yearMonth, invoiceCode });
		}

	}

	public BigDecimal caculateAccruedExpenseAmount(ExpapplC expapplC) {
		if (expapplC == null || expapplC.getEntryGroup() == null || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			String[] param = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail_expapplC", null) };
			throw new ExpRuntimeException(ErrorCode.A10007, param);
		}
		BigDecimal debitAmt = BigDecimal.ZERO;
		BigDecimal creditAmt = BigDecimal.ZERO;
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			if ("D".equals(en.getEntryType().getValue())) {
				debitAmt = debitAmt.add(en.getAmt());
			} else if ("C".equals(en.getEntryType().getValue())) {
				creditAmt = creditAmt.add(en.getAmt());
			}
		}
		// 不管正負，都回傳
		return debitAmt.subtract(creditAmt);
	}

	public boolean isExpApplNoExists(String expApplNo) {
		if (getDao().isExpapplCExists(expApplNo) || null != facade.getExpapplBService().findByExpApplNo(expApplNo)) {
			return true;
		}
		return false;
	}

	/**
	 * 檢核此申請單是否有被憑證附於, 若被憑證附於時則發票號碼皆要相同
	 * 
	 * @param expapplC
	 */
	private void checkProofAddedInfo(ExpapplC expapplC) {

		if (null == expapplC || StringUtils.isBlank(expapplC.getId()) || StringUtils.isBlank(expapplC.getExpApplNo())) {
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("proofAdd", expapplC.getExpApplNo());
		List<ExpapplC> list = this.findByCriteriaMap(map);
		if (!CollectionUtils.isEmpty(list)) {
			for (ExpapplC tempC : list) {
				if (ApplStateCode.DELETED.getCode().equals(tempC.getApplState().getCode())) {
					continue;
				}
				if (!StringUtils.equals(expapplC.getInvoiceNo(), tempC.getInvoiceNo())) {
					throw new ExpRuntimeException(ErrorCode.C10533, new String[] { expapplC.getExpApplNo(), tempC.getExpApplNo() });
				}
			}
			return;
		}

	}

	public void checkPrepaymentAccTtleColumns(ExpapplC expapplC) {
		if (expapplC == null || expapplC.getEntryGroup() == null || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}
		// 費用申請單明細，從分錄撈
		ExpapplCDetail eacDetail = null;
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			// 2009/07/20新增條件-->2009/09/23，修改資訊來源
			if (facade.getNoPrepayDayAccTitleService().isColumnsNoRequired(en.getAccTitle().getCode())) {
				continue;
			}
			if (en.getAccTitle().isPrepaid()) {
				if (en.getExpapplCDetail() == null) {
					throw new ExpRuntimeException(ErrorCode.C10029);
				}
				eacDetail = en.getExpapplCDetail();
				if (eacDetail.getPrepayStartDate() == null || eacDetail.getPrepayExpiryDate() == null) {
					throw new ExpRuntimeException(ErrorCode.C10029);
				}
			}
		}

	}

	public void resetExpapplC(ExpapplC expapplC) {
		if (expapplC == null) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}

		if (null != expapplC.getEntryGroup()) {
			expapplC.getEntryGroup().setEntries(null);
		}
		expapplC.setTaxAmt(BigDecimal.ZERO);
		expapplC.setStampAmt(BigDecimal.ZERO);
		expapplC.setRealityAmt(expapplC.getInvoiceAmt());
		expapplC.setListType(null);
		expapplC.setListNo1(null);
		expapplC.setListNo2(null);

	}

	private List<Entry> genCrebitEntry(Entry accExpEntry, ExpapplC expapplC, BigDecimal accExp, AccTitle acct, List<Entry> enList) {
		accExpEntry.setEntryGroup(expapplC.getEntryGroup());
		// 貸方
		accExpEntry.setEntryType(this.getEntryTypeForC());
		accExpEntry.setAmt(accExp);
		accExpEntry.setAccTitle(acct);
		accExpEntry.setEntryGroup(expapplC.getEntryGroup());
		enList.add(accExpEntry);
		return enList;
	}

	public void generatePayableExpenseEntry(ExpapplC expapplC, String cancelCode) {
		if (expapplC == null) {
			logger.debug("generatePayableExpenseEntry ERROR");
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}
		if (expapplC.getPaymentType() == null) {
			logger.debug("generatePayableExpenseEntry ERROR::noPaymentType");
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_paymentType") });
		}
		// 應付費用分錄
		Entry accExpEntry = new Entry();
		// 原費用申請單的分錄群組
		List<Entry> enList = new ArrayList<Entry>();
		List<Entry> deleteEntries = new ArrayList<Entry>();
		// 2009/11/19,避免多筆費用科目，所以刪掉貸方應付費用科目
		StringBuffer payableCode = new StringBuffer();
		for (String s : AccTitle.EXP_PAYABLE_ACCOUNTS) {
			payableCode.append(s + "&");
		}
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			if (payableCode.toString().indexOf(en.getAccTitle().getCode()) != -1) {
				try {// 發生EntityNotFoundException，表示資料已經刪除，不存在DB了
						// 2009/12/10,避免刪除同一筆資料而出錯，所以要去撈DB的資料比較準
					if (StringUtils.isNotBlank(en.getId()) && facade.getEntryService().findByPK(en.getId()) != null)
						deleteEntries.add(en); // 有存在DB，才需要刪除
				} catch (EntityNotFoundException e) {
					logger.error(e);
				}
			} else {
				enList.add(en);
			}
		}
		expapplC.getEntryGroup().setEntries(enList);
		// 把應付科目刪掉
		if (CollectionUtils.isNotEmpty(deleteEntries)) {
			for (Entry en : deleteEntries) {
				// 因為fetch有時不會fetch到過渡付款明細
				List<TransitPaymentDetail> tpdList = en.getTransitPaymentDetails();
				if (CollectionUtils.isEmpty(tpdList)) { // 所以從DB撈看看
					tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
				}
				// 如果fetch不到，DB也撈不到，表示在C1.5.X的交易中，所以還沒有過度付款明細資料
				for (TransitPaymentDetail tpd : tpdList) {
					if (StringUtils.isNotBlank(tpd.getId())) { // 有Id，表示在DB才有值
						facade.getTransitPaymentDetailService().delete(tpd); // 刪除
					}
				} // 有Id，表示才能從DB刪除
				if (en.getExpapplCDetail() != null && StringUtils.isNotBlank(en.getExpapplCDetail().getId())) {
					facade.getExpapplCDetailService().delete(en.getExpapplCDetail());
				}
			}
			facade.getEntryService().delete(deleteEntries); // 關連的資料都刪掉，才能把分錄資料刪掉
		}
		// 計算應付費用金額
		BigDecimal accExp = caculateAccruedExpenseAmount(expapplC);
		if (BigDecimal.ZERO.equals(accExp)) {
			// expapplC = calculateRealityAmt(expapplC, null);
			// defect_避免於新增費用明細中無輸入銷帳碼 CU3178 2018/1/31 START
			if (PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(expapplC.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(expapplC.getPaymentType().getCode())) {
				boolean havaCancelCode = false;
				for (Entry en : expapplC.getEntryGroup().getEntries()) {
					if (StringUtils.isNotBlank(en.getCancelCode())) {
						havaCancelCode = true;
					}
				}
				if (!havaCancelCode) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_cancelCode") });
				}
			}
			// defect_避免於新增費用明細中無輸入銷帳碼 CU3178 2018/1/31 END
			return;
		}
		AccTitle acct = null;
		// 依費用大分類取得貸方應付費用科目(最多二筆):
		// 依應付費用貸方科目=”20210360應付費用-總務費用” 產生分錄
		// 若傳入參數「費用申請單.費用中分類.費用大分類」=”01廠商費用”
		if (BigTypeCode.VENDOR_EXP.getCode().equals(expapplC.getMiddleType().getBigType().getCode())) {
			acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_LEDGER.getCode());
			enList = genCrebitEntry(accExpEntry, expapplC, accExp, acct, enList);
			// 把分錄加回費用申請單
			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, null);
			return;
		}
		// 依應付費用貸方科目=” 應付費用-待匯”(20210391)” 產生分錄
		// 若傳入參數「費用申請單.費用中分類.費用大分類」=”04醫檢費”
		if (BigTypeCode.MEDICAL_EXP.getCode().equals(expapplC.getMiddleType().getBigType().getCode())) {
			acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_REMIT.getCode());
			enList = genCrebitEntry(accExpEntry, expapplC, accExp, acct, enList);
			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, null);
			return;
		}
		// 待匯
		if (PaymentTypeCode.C_REMIT.getCode().equals(expapplC.getPaymentType().getCode())) {
			acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_REMIT.getCode());
			enList = genCrebitEntry(accExpEntry, expapplC, accExp, acct, enList);
			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, null);
			return;
		}
		// 待開
		if (PaymentTypeCode.C_CHECK.getCode().equals(expapplC.getPaymentType().getCode())) {
			acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_CHECK.getCode());
			enList = genCrebitEntry(accExpEntry, expapplC, accExp, acct, enList);
			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, null);
			return;
		}
		// 約定轉帳扣款&繳款件
		if (PaymentTypeCode.C_TRANSFER.getCode().equals(expapplC.getPaymentType().getCode()) || PaymentTypeCode.C_PAYMENT.getCode().equals(expapplC.getPaymentType().getCode())) {
			acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_TO_PAY.getCode());
			enList = genCrebitEntry(accExpEntry, expapplC, accExp, acct, enList);
			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, null);
			return;
		}
		// 沖轉暫付&沖轉預付
		if (PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(expapplC.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(expapplC.getPaymentType().getCode())) {
			if (StringUtils.isEmpty(cancelCode)) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_cancelCode") });
			}
			BigDecimal cancelAmt = facade.getEntryService().getCancelCodeBalance(cancelCode, expapplC.getExpApplNo());
			if (BigDecimal.ZERO.equals(cancelAmt) || cancelAmt.equals(cancelAmt.min(BigDecimal.ZERO))) {
				throw new ExpRuntimeException(ErrorCode.C10022);
			}
			// List<Entry> cancelEntries =
			// facade.getEntryService().findCancelCodeEntry(cancelCode);
			// 2009/12/9,取得要產生的貸方會計科目，執行共用function《以銷帳碼查出對應的會計科目》(2009/12/9)
			acct = facade.getEntryService().findCancelCodeAccTitle(cancelCode);
			Entry accExpOldEntry = null;
			try {
				accExpOldEntry = facade.getEntryService().getEntryByCancelCode(cancelCode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (acct == null)
				throw new ExpRuntimeException(ErrorCode.C10022);
			if (cancelAmt.compareTo(accExp) >= 0) { // 可銷帳的金額大於申請金額時
													// 產生一筆貸方科目的分錄:
				accExpEntry.setAccTitle(acct);
				accExpEntry.setAmt(accExp);
				accExpEntry.setCancelCode(cancelCode);
				accExpEntry.setEntryGroup(expapplC.getEntryGroup());
				if (accExpOldEntry != null) {
					accExpEntry.setCostUnitCode(accExpOldEntry.getCostUnitCode());
					accExpEntry.setCostUnitName(accExpOldEntry.getCostUnitName());
				}
				enList.add(accExpEntry);
			}
			// 可銷帳的金額小於申請金額，那就要產生兩筆(要付錢的)分錄
			else if (cancelAmt.compareTo(accExp) < 0) {
				Entry rollEntry = new Entry(); // 待匯的分錄
				// 2009/12/9,取得要產生的貸方會計科目，執行共用function《以銷帳碼查出對應的會計科目》(2009/12/9)
				AccTitle accExpAcct = facade.getEntryService().findCancelCodeAccTitle(cancelCode);
				// 待匯科目
				acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_REMIT.getCode());
				// 產生'2'筆貸方科目的分錄: accExpEntry == 貸方的應付費用科目分錄
				// 可沖轉的借支分錄. 會計科目
				accExpEntry.setAccTitle(accExpAcct);
				accExpEntry.setAmt(cancelAmt);
				accExpEntry.setCancelCode(cancelCode);
				accExpEntry.setEntryGroup(expapplC.getEntryGroup());
				if (accExpOldEntry != null) {
					accExpEntry.setCostUnitCode(accExpOldEntry.getCostUnitCode());
					accExpEntry.setCostUnitName(accExpOldEntry.getCostUnitName());
				}
				// 把分錄放到分錄群組用
				enList.add(accExpEntry);
				rollEntry.setEntryGroup(expapplC.getEntryGroup());
				rollEntry.setEntryType(this.getEntryTypeForC());
				rollEntry.setAmt(accExp.subtract(cancelAmt));
				// 待匯
				rollEntry.setAccTitle(acct);
				// 把分錄放到分錄群組用
				enList.add(rollEntry);
			}
			accExpEntry.setEntryGroup(expapplC.getEntryGroup());
			// 貸方
			accExpEntry.setEntryType(this.getEntryTypeForC());

			expapplC.getEntryGroup().setEntries(enList);
			// expapplC = calculateRealityAmt(expapplC, cancelAmt);
			return;
		}

	}

	/**
	 * 計算實付金額--待確認
	 * 
	 * @param eac
	 * @return
	 */
	@SuppressWarnings("unused")
	private ExpapplC calculateRealityAmt(ExpapplC eac, BigDecimal cancelAmt) {
		logger.debug("StartCalcuReality$$::..");
		if (BigTypeCode.VENDOR_EXP.getCode().equals(eac.getMiddleType().getBigType().getCode())) {
			logger.debug("StartCalcuReality$$::....isVendorExp");
			if (eac.getRealityAmt().compareTo(BigDecimal.ZERO) < 0)
				throw new ExpRuntimeException(ErrorCode.A10056, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_realityAmt") });
		} else {
			logger.debug("StillCalcuReality$$::....realityAmt=ZERO");
			BigDecimal realityAmt = (eac.getInvoiceAmt().subtract(eac.getStampAmt()).subtract(eac.getTaxAmt()));
			if (cancelAmt != null) {
				logger.debug("StillCalcuReality$$::......realityAmt=" + realityAmt + "---CancelAmt=" + cancelAmt);
				realityAmt = realityAmt.subtract(cancelAmt);
			}
			eac.setRealityAmt(realityAmt);
			logger.debug("EndCalcuReality$$::.........realityAmt=" + realityAmt);
		}
		return eac;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#generateExpApplNo(
	 * java.lang.String)
	 */
	public String generateExpApplNo(String param1) {
		SNGenerator gen = AbstractSNGenerator.getInstance(ExpApplNoGenerator.class.getName(), facade.getSequenceService());
		Map<String, String> params = new HashMap<String, String>();
		params.put("param1", param1);

		return gen.getSerialNumber(params);
	}

	/**
	 * 產生進項稅借方科目
	 * 
	 * @param expapplC
	 *            行政費用申請單
	 * @param entry
	 *            費用分錄
	 * @param entryList
	 */
	private void generateWithholdIncome(ExpapplC expapplC, AccTitle expAccTitle, List<Entry> entryList) {
		if (expapplC.isWithholdIncome()) {
			// 費用科目
			// AccTitle expAccTitle = entry.getAccTitle();
			// 進項稅
			BigDecimal withholdIncomeAmt = BigDecimal.ZERO;

			// 憑證金額(含)
			BigDecimal invoiceAmt = expapplC.getInvoiceAmt();
			// 借方
			EntryType entryTypeD = this.getEntryTypeForD();
			// 貸方
			EntryType entryTypeC = this.getEntryTypeForC();

			if (expapplC.isHadAllowanceSlip()) {
				// 有折讓

				BigDecimal stampAmt = expapplC.getStampAmt();// 印花稅額
				BigDecimal taxAmt = expapplC.getTaxAmt();// 所得稅額

				// 計算實付金額 :實付金額= 憑證金額-印花稅額-所得稅額
				BigDecimal realityAmt = invoiceAmt.subtract(stampAmt).subtract(taxAmt);
				// set實付金額
				expapplC.setRealityAmt(realityAmt);

				withholdIncomeAmt = this.facade.getAccTitleService().calculateIncomeAmt(expAccTitle.getCode(), true, realityAmt);

				// 金額大於0 則產生貸方折讓分錄(進項)
				if (BigDecimal.ZERO.compareTo(withholdIncomeAmt) < 0) {
					Entry withholdIncomeEntryForC = new Entry();
					withholdIncomeEntryForC.setAccTitle(expAccTitle.getVat());
					withholdIncomeEntryForC.setEntryType(entryTypeC);
					withholdIncomeEntryForC.setAmt(withholdIncomeAmt);
					entryList.add(withholdIncomeEntryForC);
				}

				// 金額大於0 則產生貸方折讓分錄(費用)
				if (BigDecimal.ZERO.compareTo(withholdIncomeAmt) < 0) {
					Entry expEntry = new Entry();
					expEntry.setAccTitle(expAccTitle);
					expEntry.setEntryType(entryTypeC);
					expEntry.setAmt(withholdIncomeAmt);
					entryList.add(expEntry);
				}
			} else {
				// 無折讓
				withholdIncomeAmt = this.facade.getAccTitleService().calculateIncomeAmt(expAccTitle.getCode(), true, invoiceAmt);
			}

			// 金額大於0 則產生借方分錄(進項)
			if (BigDecimal.ZERO.compareTo(withholdIncomeAmt) < 0) {
				Entry withholdIncomeEntryForD = new Entry();
				withholdIncomeEntryForD.setAccTitle(expAccTitle.getVat());
				withholdIncomeEntryForD.setEntryType(entryTypeD);
				withholdIncomeEntryForD.setAmt(withholdIncomeAmt);
				entryList.add(withholdIncomeEntryForD);
			}

		}
	}

	/**
	 * @param expapplC
	 *            行政費用申請單
	 * @param department
	 *            成本單位
	 * @param rosterDetail
	 *            名冊
	 * @param listNo
	 *            冊號
	 * @param entryList
	 * @param isBegWithHold
	 *            是否代扣所得稅
	 * @param expapplCDetail
	 *            費用明細
	 */
	private Entry doGenerateApplyRosterEntrie(ExpapplC expapplC, String department, RosterDetail rosterDetail, String listNo, List<Entry> entryList, Boolean isBegWithHold, ExpapplCDetail expapplCDetail) {
		if (null == rosterDetail) {
			String[] params = { listNo };
			// 顯示《查無名冊資料，名冊單號: ”冊號”》
			throw new ExpRuntimeException(ErrorCode.C10057, params);
		}

		// 4.若「名冊.名冊狀態」不等於”0.尚未請領”，throw ExpRuntimeExceiption，顯示《名冊單號: ”冊號”
		// 重覆請領》
		// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 START
		// if
		// (!RosterStateCode.UNAPPLIED.getCode().equals(rosterDetail.getRosterState().getCode())&&rosterDetail.getExpApplNo().compareTo(expapplC.getExpApplNo())!=0)
		// {
		if (!RosterStateCode.UNAPPLIED.getCode().equals(rosterDetail.getRosterState().getCode()) && (null == rosterDetail.getExpApplNo() || !rosterDetail.getExpApplNo().equals(expapplC.getExpApplNo()))) {
			// RE201502395_104年下半年度 費用系統defects修改 2015/10/1 END
			String[] params = { listNo };
			// 顯示《名冊單號: ”冊號” 重覆請領》
			throw new ExpRuntimeException(ErrorCode.C10058, params);
		}

		// 5.new 一個BigDecimal變數”申請總額”，用來暫存各「領獎人.獎項金額」總額。
		BigDecimal totalAmount = BigDecimal.ZERO;

		// 科目借貸別=貸方
		EntryType entryTypeForC = this.getEntryTypeForC();
		// 科目借貸別=借方
		EntryType entryTypeForD = this.getEntryTypeForD();

		List<GainPerson> gainPersonList = facade.getGainPersonService().findByRosterDetail(rosterDetail);

		// 6.取得「名冊.領獎人」List，並對每一筆領獎人執行以下計算，新增應付代扣科目及計算申請總額:
		for (GainPerson gainPerson : gainPersonList) {
			// ”申請總額” = ”申請總額”+ 「領獎人.獎項金額」
			totalAmount = totalAmount.add(gainPerson.getGainAmt());

			if (isBegWithHold != true) {
				continue;
			}
			// 計算所得稅 2009/12/16 改用名冊建檔所計算出的金額 By Eustace
			BigDecimal taxAmt = gainPerson.getTaxAmt2();
			// this.facade.getAccTitleService().calculateTaxAmt(rosterDetail.getAccTitle(),
			// gainPerson.getGainAmt(), true);
			// 若計算出的所得稅等於0，繼續執行下一個領獎人(continue)
			if (null == taxAmt || BigDecimal.ZERO.equals(taxAmt)) {
				continue;
			}

			// 若所得稅不等於0，new 一筆分錄資料，用來記錄此領獎人的應付代扣科目，參數如下，並新增到分錄的List:
			Entry taxEntry = new Entry();
			/*
			 * ☆ ”分錄.會計科目”=「名冊.科目代號.代扣科目代號」 ☆ ”分錄.業別代號”=「名冊.科目代號.代扣科目代號.所得稅業別代號」
			 * ☆ ”分錄.科目借貸別”=貸方 ☆ ”分錄.金額”=計算出的所得稅金額 ☆ “分錄.業別代號”及”分錄.所得人證號”欄位: ＊
			 * 預設”分錄.所得人證號類別”=4(員工代號); ”分錄.所得人證號”=領獎人.領獎人員工代號 ＊
			 * 當領獎人.領獎人員工代號為空值時，以”領獎人.所得人資料.所得人類別”判斷分錄.所得人證號類別。
			 * 當”領獎人.所得人資料.所得人類別”=自然人，分錄.所得人證號類別”=1(身份證字號);
			 * 否則分錄.所得人證號類別”=3(廠商統編); ”分錄.所得人證號”=所得人資料. 身份證字號/廠商統編
			 */
			taxEntry.setAccTitle(rosterDetail.getAccTitle().getWithhold());
			taxEntry.setIndustryCode(rosterDetail.getAccTitle().getWithhold().getIncomeBiz());
			taxEntry.setEntryType(entryTypeForC);
			taxEntry.setAmt(taxAmt);

			if (StringUtils.isNotBlank(gainPerson.getGainUserId())) {
				taxEntry.setIncomeIdType(IncomeIdTypeCode.EMP_ID.getCode());
				taxEntry.setIncomeId(gainPerson.getGainUserId());
			} else {
				if (IncomeUserTypeCode.NATURAL_PERSON.getCode().equals(gainPerson.getIncomeUser().getIncomeUserType().getCode())) {
					taxEntry.setIncomeIdType(IncomeIdTypeCode.IDENTITY_ID.getCode());
				} else {
					taxEntry.setIncomeIdType(IncomeIdTypeCode.COMP_ID.getCode());
				}
				taxEntry.setIncomeId(gainPerson.getIncomeUser().getIdentityId());
			}

			entryList.add(taxEntry);

		}

		// 7.產生一筆費用科目的分錄，資料內容如下，並新增到分錄的List:
		Entry expEntry = new Entry();
		/*
		 * ”分錄.會計科目”=查回的「名冊.科目代號」 ”分錄.科目借貸別”=借方 ”分錄.金額”=”申請總額”
		 * ”分錄.成本單位”=傳入參數”成本單位”
		 */
		expEntry.setAccTitle(rosterDetail.getAccTitle());
		expEntry.setEntryType(entryTypeForD);
		expEntry.setAmt(totalAmount);
		Department dep = facade.getDepartmentService().findByCode(department);
		expEntry.setCostUnitCode(dep.getCode());
		expEntry.setCostUnitName(dep.getName());
		if (null != expapplCDetail) {// IISI-20100805 : 修正費用明細沒有存入DB問題
			ExpapplCDetail detail = new ExpapplCDetail();
			BeanUtils.copyProperties(expapplCDetail, detail);
			expEntry.setExpapplCDetail(detail);
		}

		return expEntry;
	}

	/**
	 * 依條件查詢退件送件表 RE201000504_20100304:新增送件經辦代號、付款年月兩查詢條件；費用大分類 =
	 * 廠商費用時,付款年月才開放輸入 UC1.6.8退件送件表
	 * 
	 * @param applStateCode
	 *            申請單狀態代碼
	 * @param bigTypeCode
	 *            費用大分類代碼
	 * @param deliverDaylistUser
	 *            送件經辦員工代號
	 * @param payYearMonth
	 *            付款年月
	 * @return
	 * @throws ExpException
	 * @throws ExpRuntimeException
	 * @author 文珊
	 */
	public List<RtnItemApplDto> findRtnItemApplDto(ApplStateCode applStateCode, BigTypeCode bigTypeCode, String deliverDaylistUser, String payYearMonth) throws ExpException, ExpRuntimeException {
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct exp");
		if (BigTypeCode.VENDOR_EXP.equals(bigTypeCode)) {
			queryString.append(" from VendorExp vendorExp");
			queryString.append(" left join vendorExp.expapplC exp");
			queryString.append(" inner join exp.deliverDaylist dd");
			queryString.append(" inner join dd.daylistUser duser");
		} else {
			queryString.append(" from ExpapplC exp");
			queryString.append(" inner join exp.deliverDaylist dd");
			queryString.append(" inner join dd.daylistUser duser");
		}
		boolean truncated = false;

		Map<String, Object> params = new HashMap<String, Object>();

		// 申請單狀態
		if (null != applStateCode) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.applState.code =:applStateCode");

			params.put("applStateCode", applStateCode.getCode());

			queryString.append(" and");

			queryString.append(" exp.applState.sysType.code =:sysTypeCode");
			params.put("sysTypeCode", SysTypeCode.C.getCode());

			queryString.append(" and");

		}

		// 費用大分類
		if (null != bigTypeCode) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" exp.middleType.bigType.code =:bigTypeCode");

			params.put("bigTypeCode", bigTypeCode.getCode());

			queryString.append(" and");

		}

		// 送件經辦員工代號 @author 文珊
		if (null != deliverDaylistUser) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" duser.code =:deliverDaylistUser");

			params.put("deliverDaylistUser", deliverDaylistUser);

			queryString.append(" and");

		}

		// 廠商費用.付款年月 @author 文珊
		if (BigTypeCode.VENDOR_EXP.equals(bigTypeCode)) {
			if (null != payYearMonth) {
				if (!truncated) {
					queryString.append(" where");
					truncated = true;
				}

				queryString.append(" vendorExp.payYearMonth =:payYearMonth");

				params.put("payYearMonth", payYearMonth);

				queryString.append(" and");

			}
		}

		if (truncated) {
			// 刪除最後一個and字串
			queryString.delete(queryString.lastIndexOf("and"), queryString.length());
		}

		// 排序條件
		if (null != bigTypeCode) {
			//  「費用大分類」= 廠商費用(01)時，顯示退件資料列時，以"經辦序號"排序
			if (BigTypeCode.VENDOR_EXP.equals(bigTypeCode)) {
				queryString.append(" order by vendorExp.generalMgrSn");
			} else {
				// 「費用大分類」=辦公費(00)，顯示之資料列以"申請單號"排序。
				queryString.append(" order by exp.expApplNo");
			}
		}

		List<ExpapplC> expapplCList = getDao().findByNamedParams(queryString.toString(), params);

		List<RtnItemApplDto> rtnItemApplDtoList = new ArrayList<RtnItemApplDto>();

		if (null != bigTypeCode && BigTypeCode.VENDOR_EXP.equals(bigTypeCode)) {
			List<String> expApplNoList = new ArrayList<String>();

			for (ExpapplC expapplC : expapplCList) {
				expApplNoList.add(expapplC.getExpApplNo());
			}

			List<VendorExp> vendorExpList = this.facade.getVendorExpService().findByExpApplNoList(expApplNoList);

			if (!CollectionUtils.isEmpty(vendorExpList)) {

				for (VendorExp vendorExp : vendorExpList) {
					RtnItemApplDto dto = new RtnItemApplDto();
					dto.setExpapplC(vendorExp.getExpapplC());
					dto.setCompName(vendorExp.getVendor().getCompName());
					dto.setVendorCompId(vendorExp.getVendor().getVendorCompId());
					dto.setContractNo(vendorExp.getVendorContractCode());

					// modify...2009/8/25, By Eustace
					ApplStateCode[] applStateCodes = null;
					if (null != applStateCode) {
						applStateCodes = new ApplStateCode[] { applStateCode };
					}
					FlowCheckstatus flowCheckstatus = this.facade.getFlowCheckstatusService().findByParams(vendorExp.getExpapplC().getExpApplNo(), SysTypeCode.C.getCode(), applStateCodes);
					dto.setFlowCheckstatus(flowCheckstatus);
					if (null != flowCheckstatus && null != flowCheckstatus.getReturnStatement()) {
						dto.setReturnStatement(flowCheckstatus.getReturnStatement());
					}
					rtnItemApplDtoList.add(dto);
				}
			}
		} else {

			for (ExpapplC expapplC : expapplCList) {
				RtnItemApplDto dto = new RtnItemApplDto();
				dto.setExpapplC(expapplC);

				// 合約編號
				if (MiddleTypeCode.CODE_D00.getCode().equals(expapplC.getMiddleType().getCode())) {
					RentExp rentExp = this.facade.getRentExpService().findByExpApplNoFetchRelation(expapplC.getExpApplNo());
					String contractNo = null;
					if (null != rentExp && null != rentExp.getRentContract()) {
						contractNo = rentExp.getRentContract().getContractNo();
					}
					dto.setContractNo(contractNo);
				}

				FlowCheckstatus flowCheckstatus = this.facade.getFlowCheckstatusService().findByParams(expapplC.getExpApplNo(), SysTypeCode.C.getCode(), new ApplStateCode[] { applStateCode });
				dto.setFlowCheckstatus(flowCheckstatus);
				if (null != flowCheckstatus && null != flowCheckstatus.getReturnStatement()) {
					dto.setReturnStatement(flowCheckstatus.getReturnStatement());
				}
				rtnItemApplDtoList.add(dto);

			}

		}
		return rtnItemApplDtoList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#updateRosterState(
	 * tw.com.skl.exp.kernel.model6.bo.RosterState.RosterStateCode,
	 * java.lang.String, java.util.List)
	 */
	public void updateRosterState(Integer state, String expApplNo, List<String> listNos) {
		/*
		 * 1. if傳入參數為以下情況，throw ExpRuntimeException，顯示”傳入參數錯誤” 傳入參數”動作”不等於0或1
		 * 費用申請單號為空值 當傳入參數”動作”=0，且册號List為空值或List長度不等於1或2
		 */
		if (null == state || !(state == 1 || state == 0)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterState_code") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.isBlank(expApplNo)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (CollectionUtils.isEmpty(listNos) || listNos.size() > 2) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		Calendar sysDate = Calendar.getInstance();

		/*
		 * 2. 依傳入參數”動作”執行以下邏輯:  2a. 若傳入參數”動作”=0時:
		 * 1.依傳入的冊號List，檢查所有「名冊.名冊狀態」必須為”尚未請領”  若「名冊.名冊狀態」不等於”0.尚未請領”，throw
		 * ExpRuntimeExceiption，顯示《名冊單號: ”冊號” 重覆請領》 2. 依冊號1，計算該「名冊.領獎人.獎項金額」的總和
		 * 3. 設定以下欄位: ▲ 1.「名冊.名冊狀態」=請領完畢 ▲ 2.「名冊.已使用金額」=獎項金額總額 ▲
		 * 3.「名冊.費用申請單號」=傳入的”費用申請單.申請單號” 4. 儲存「名冊」 5. 若有參數”冊號2”，重覆執行步驟2-4
		 */
		if (state == 0) {
			for (String listNo : listNos) {
				// 查出名冊
				RosterDetail rosterDetail = this.facade.getRosterDetailService().findByRosterNoFetchRelation(listNo);
				if (null == rosterDetail) {
					throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterDetail") });
				}
				// 「名冊.名冊狀態」不等於”0.
				if (!RosterStateCode.UNAPPLIED.getCode().equals(rosterDetail.getRosterState().getCode())) {
					String[] params = { listNo };
					// 顯示《名冊單號: ”冊號” 重覆請領》
					throw new ExpRuntimeException(ErrorCode.C10058, params);
				}

				// 2. 獎項金額總額
				BigDecimal totalAmount = BigDecimal.ZERO;
				for (GainPerson gainPerson : rosterDetail.getGainPersons()) {
					// 計算該「名冊.領獎人.獎項金額」的總和
					totalAmount = totalAmount.add(gainPerson.getGainAmt());
				}

				// 3.設定以欄位
				// 「名冊.名冊狀態」=請領完畢
				rosterDetail.setRosterState(this.facade.getRosterStateService().findByCode(RosterStateCode.APPLIED));
				// 「名冊.已使用金額」=獎項金額總額
				rosterDetail.setUseAmt(totalAmount);
				// 「名冊.費用申請單號」=傳入的”費用申請單.申請單號”
				rosterDetail.setExpApplNo(expApplNo);

				rosterDetail.setUpdateDate(sysDate);
				rosterDetail.setUpdateUser(getLoginUser());

				// 4. 儲存「名冊」
				this.facade.getRosterDetailService().update(rosterDetail);
			}
		}

		/*
		 * 2b. 若傳入參數”動作”=1時: 1. 依申請單號查出「費用申請單」 2. 依「費用申請單.冊號1」，查出「名冊」資料，並設定以下的值
		 * ▲ 1. 「名冊.名冊狀態」設為尚未請領。 ▲ 2. 「名冊.已使用金額」設為0 ▲ 3. 「名冊.費用申請單號」設為null 3.
		 * 儲存「名冊」 4. 若「費用申請單.冊號2」不為空值，重覆執行步驟2-4
		 */
		if (state == 1) {
			for (String listNo : listNos) {
				// 1.依申請單號查出「費用申請單」
				Map<String, Object> criteriaMap = new HashMap<String, Object>();
				criteriaMap.put("expApplNo", expApplNo);
				ExpapplC expapplC = this.getDao().findByCriteriaMapReturnUnique(criteriaMap);

				// List<String> list = new ArrayList<String>();
				// if (StringUtils.isNotBlank(expapplC.getListNo1())) {
				// list.add(expapplC.getListNo1());
				// } else {
				// throw new ExpRuntimeException(ErrorCode.A20001);
				// }
				//
				// if (StringUtils.isNotBlank(expapplC.getListNo2())) {
				// list.add(expapplC.getListNo2());
				// }
				//
				// for (String listNo : list) {
				// 2.查出名冊
				RosterDetail rosterDetail = this.facade.getRosterDetailService().findByRosterNoFetchRelation(listNo);
				// 設定以欄位
				// 「名冊.名冊狀態」設為尚未請領
				rosterDetail.setRosterState(this.facade.getRosterStateService().findByCode(RosterStateCode.UNAPPLIED));
				// 「名冊. 已使用金額」設為0
				rosterDetail.setUseAmt(BigDecimal.ZERO);
				// 「名冊.費用申請單號」設為null
				rosterDetail.setExpApplNo(null);

				rosterDetail.setUpdateDate(sysDate);
				rosterDetail.setUpdateUser(getLoginUser());
				// 3. 儲存「名冊」
				this.facade.getRosterDetailService().update(rosterDetail);
				// }
			}
		}
	}

	public void updateRegisterRosterState(Integer state, String expApplNo, String listNo) {
		if (null == state || !(state == 1 || state == 0)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RegisterRoster_listNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.isBlank(expApplNo)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.isBlank(listNo)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		Calendar sysDate = Calendar.getInstance();
		User u = getLoginUser();
		if (state == 0) { // 恢復為未申請
			updateRegisterExpApplDetailByExpApplNoAndListNo(false, listNo, sysDate, u);
		}// 狀態更新為已申請
		else if (state == 1) {
			updateRegisterExpApplDetailByExpApplNoAndListNo(true, listNo, sysDate, u);
		}
	}

	/**
	 * 以申請單的名冊編號，更報名費用明細
	 * 
	 * @param applState
	 *            預更新的狀態
	 * @param listNo
	 *            冊號
	 * @param sysDate
	 *            系統日
	 * @param u
	 *            系統登入使用者
	 */
	private void updateRegisterExpApplDetailByExpApplNoAndListNo(Boolean applState, String listNo, Calendar sysDate, User u) {
		List<RegisterExpApplDetail> registerExpApplDetailList = facade.getRegisterExpApplDetailService().findByRegisterRosterListNo(listNo);
		if (CollectionUtils.isNotEmpty(registerExpApplDetailList)) {
			for (RegisterExpApplDetail read : registerExpApplDetailList) {

				read.setAppliedFormFlag(applState);
				read.setUpdateDate(sysDate);
				read.setUpdateUser(u);
				facade.getRegisterExpApplDetailService().update(read);
			}
		} else {
			throw new ExpRuntimeException(ErrorCode.C10551);
		}
	}

	public void updatePhoneRosterState(Integer state, String expApplNo, String listNo) {
		if (null == state || !(state == 1 || state == 0)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterState_code") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.isBlank(expApplNo)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.isBlank(listNo)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		Calendar sysDate = Calendar.getInstance();
		User u = getLoginUser();
		if (state == 0) { // 恢復為未申請
			updatePhoneFeeDetailByExpApplNoAndListNo(false, listNo, sysDate, u);
		}// 狀態更新為已申請
		else if (state == 1) {
			updatePhoneFeeDetailByExpApplNoAndListNo(true, listNo, sysDate, u);
		}
	}

	/**
	 * 以申請單的名冊編號，更新電話費用明細
	 * 
	 * @param applState
	 *            預更新的狀態
	 * @param listNo
	 *            冊號
	 * @param sysDate
	 *            系統日
	 * @param u
	 *            系統登入使用者
	 */
	private void updatePhoneFeeDetailByExpApplNoAndListNo(Boolean applState, String listNo, Calendar sysDate, User u) {
		List<PhoneRoster> phoneRosterList = facade.getPhoneRosterService().findByListNo(listNo);
		if (CollectionUtils.isNotEmpty(phoneRosterList)) {
			for (PhoneRoster pr : phoneRosterList) {
				for (PhoneFeeDetail pfd : pr.getPhoneFeeDetails()) {
					pfd.setAppliedFromFlag(applState);
					pfd.setUpdateDate(sysDate);
					pfd.setUpdateUser(u);
					// RE201600212 電話費刪除狀態會改變 CU3178 2016/3/9 START
					facade.getPhoneFeeDetailService().update(pfd);
					// RE201600212 電話費刪除狀態會改變 CU3178 2016/3/9 END
				}
			}
		} else {
			throw new ExpRuntimeException(ErrorCode.C10203, new String[] { listNo });
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#generateDefaultEntries
	 * (tw.com.skl.exp.kernel.model6.bo.ExpapplC,
	 * tw.com.skl.exp.kernel.model6.bo.AccTitle,
	 * tw.com.skl.exp.kernel.model6.bo.Department, java.lang.String,
	 * tw.com.skl.exp.kernel.model6.bo.IncomeIdType, java.lang.String,
	 * java.lang.String)
	 */
	public void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department, BigDecimal amt, String cancelCode, String industryCode, String expSummary, IncomeIdType incomeIdType, String incomeId) {
		/*
		 * 1.【檢核】 若傳入的參數”費用申請單”否為空值，則throw ExpRuntimeExceiption，顯示《傳入參數錯誤》
		 * 若費用申請單.冊號類別不為null，則throw ExpRuntimeExceiption，顯示《傳入參數:
		 * 費用申請單.冊號類別必須為null》
		 * 若傳入的參數「費用申請單.是否扣繳進項稅」=false，但「費用申請單.進項稅額」大於0，則throw
		 * ExpRuntimeException, 顯示《傳入參數: 費用申請單.進項稅額資料錯誤》
		 * 若傳入的參數「費用申請單.是否扣繳印花稅」=false，但「費用申請單.印花稅額」大於0，則throw
		 * ExpRuntimeException, 顯示《傳入參數: 費用申請單.印花稅額資料錯誤》
		 */
		// 若傳入的參數”費用申請單”否為空值
		if (null == expapplC) {
			// 則throw ExpRuntimeExceiption，顯示《傳入參數錯誤》
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}

		// 若費用申請單.冊號類別不為null且冊號類別不為報名費
		if ((null != expapplC.getListType()) && (!expapplC.getListType().getCode().equals(ListTypeCode.REGISTER.getCode()))) {
			StringBuffer str = new StringBuffer();
			str.append(": ");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC"));
			str.append(".");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listType"));
			str.append(MessageUtils.getAccessor().getMessage("must_be_null"));

			// 則throw ExpRuntimeExceiption，顯示《傳入參數: 費用申請單.冊號類別必須為null》
			throw new ExpRuntimeException(ErrorCode.A10032, new String[] { str.toString() });
		}

		// 若傳入的參數「費用申請單.是否扣繳進項稅」=false，但「費用申請單.憑證金額(稅)」大於0
		if (expapplC.isWithholdIncome() == false && expapplC.getInvoiceTaxAmt().compareTo(BigDecimal.ZERO) != 0) {
			StringBuffer str = new StringBuffer();
			str.append(": ");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC"));
			str.append(".");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_incomeAmt"));
			// 則throw ExpRuntimeException, 顯示《傳入參數: 費用申請單.進項稅額資料錯誤》
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { str.toString() });

		}

		// 若傳入的參數「費用申請單.是否扣繳印花稅」=false，但「費用申請單.印花稅額」大於0
		if (expapplC.isWithholdStamp() == false && expapplC.getStampAmt().compareTo(BigDecimal.ZERO) != 0) {
			StringBuffer str = new StringBuffer();
			str.append(": ");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC"));
			str.append(".");
			str.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_stampAmt"));
			// 則throw ExpRuntimeException, 顯示《傳入參數: 費用申請單.印花稅額資料錯誤》
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { str.toString() });
		}

		if (null == accTitle) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle") });
		}

		if (null == department || DepartmentCode.ROOT.equals(DepartmentCode.getByValue(department))) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Department") });
		}

		/*
		 * 2.修改欄位值:  設定「費用申請單.進項稅額」=「費用申請單.憑證金額(稅)」 
		 * 設定「費用申請單.憑證金額(未)」=「費用申請單.憑證金額(含)」-「費用申請單.憑證金額(稅)」
		 */
		// 設定「費用申請單.進項稅額」=「費用申請單.憑證金額(稅)」
		expapplC.setIncomeAmt(expapplC.getInvoiceTaxAmt());
		// 設定「費用申請單.憑證金額(未)」=「費用申請單.憑證金額(含)」-「費用申請單.憑證金額(稅)」
		expapplC.setInvoiceNoneTaxAmt(expapplC.getInvoiceAmt().subtract(expapplC.getInvoiceTaxAmt()));

		// 3.建立一個分錄List，用來暫存產生的分錄資料
		List<Entry> entryList = new ArrayList<Entry>();

		/*
		 * 4.產生費用科目的借方科目及金額。  依傳入的參數”成本單位”，填入「分錄.成本單位代號」及「分錄.成本單位名稱」 
		 * 「分錄.金額」=傳入參數” 費用科目金額(明細金額)”  若傳入參數” 費用科目金額(明細金額)”為NULL或0 
		 * 若「費用申請單.是否扣繳進項稅」=True時，設定「分錄.金額」=「費用申請單.憑證金額(未)」
		 * 若否，「分錄.金額」=設定「費用申請單.憑證金額(含)」欄位
		 * 
		 * 「分錄.業別代號」=傳入參數”業別代號” 「分錄.摘要」=傳入參數”費用科目摘要”
		 * 
		 *  將新產生的分錄加入暫存的分錄List
		 */
		Entry expEntryForD = new Entry();
		// 依傳入的參數”成本單位”，填入「分錄.成本單位代號」及「分錄.成本單位名稱」
		expEntryForD.setCostUnitCode(department.getCode());
		expEntryForD.setCostUnitName(department.getName());
		// 「分錄.金額」=傳入參數” 費用科目金額(明細金額)”
		if (null == amt || BigDecimal.ZERO.compareTo(amt) >= 0) {

			if (BigDecimal.ZERO.compareTo(expapplC.getInvoiceAmt()) >= 0) {
				throw new ExpRuntimeException(ErrorCode.A10038, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceAmt") });
			}
			// 若傳入參數” 費用科目金額(明細金額)”為NULL或0
			if (expapplC.isWithholdIncome()) {
				if (BigDecimal.ZERO.compareTo(expapplC.getInvoiceNoneTaxAmt()) >= 0) {
					throw new ExpRuntimeException(ErrorCode.A10038, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNoneTaxAmt") });
				}
				// 「費用申請單.是否扣繳進項稅」=True時，設定「分錄.金額」=「費用申請單.憑證金額(未)」
				expEntryForD.setAmt(expapplC.getInvoiceNoneTaxAmt());
			} else {
				// 「分錄.金額」=設定「費用申請單.憑證金額(含)」欄位
				expEntryForD.setAmt(expapplC.getInvoiceAmt());
			}
		} else {
			expEntryForD.setAmt(amt);
		}
		// 「分錄.業別代號」=傳入參數”業別代號”
		expEntryForD.setIndustryCode(industryCode);
		// 「分錄.摘要」=傳入參數”費用科目摘要”
		expEntryForD.setSummary(expSummary);
		expEntryForD.setAccTitle(accTitle);

		// RE201800999_C1.5.1新增預付 CU3178 2018/3/30 START
		// 若會計科目為10511205預付費用-商務卡，則需寫入銷帳碼
		if (accTitle.getCode().equals(AccTitleCode.PREPAY_BIZCARD.getCode()) || accTitle.getCode().equals(AccTitleCode.PREPAY_OTHER.getCode())) {
			expEntryForD.setCancelCode(cancelCode);
		}
		// RE201800999_C1.5.1新增預付 CU3178 2018/3/30 END

		expEntryForD.setEntryType(getEntryTypeForD());
		entryList.add(expEntryForD);

		if (null == expEntryForD.getAmt() || BigDecimal.ZERO.compareTo(expEntryForD.getAmt()) >= 0) {
			throw new ExpRuntimeException(ErrorCode.A10038, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_amt1") });
		}

		/*
		 * 5.若「費用申請單.是否扣繳進項稅」=true，且「費用申請單.進項稅額」不等於0，則產生借方扣抵進項稅額之科目及其金額顯示於帳務資料。
		 *  產生借方科目分錄, 會計科目=「傳入的費用科目. 進項稅科目代號」, 金額=「費用申請單.憑證金額(稅)」 (進項稅) 
		 * 將新產生的分錄加入暫存的分錄List
		 */
		if (expapplC.isWithholdIncome() && expapplC.getIncomeAmt().compareTo(BigDecimal.ZERO) != 0) {
			Entry incomeAmtEntry = new Entry();
			// 產生借方科目分錄
			incomeAmtEntry.setEntryType(getEntryTypeForD());

			if (null != accTitle.getVat()) {
				// 會計科目=「傳入的費用科目. 進項稅科目代號」
				incomeAmtEntry.setAccTitle(accTitle.getVat());
			} else {
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_vat") });
			}

			// 金額=「費用申請單.憑證金額(稅)」 (進項稅)
			incomeAmtEntry.setAmt(expapplC.getInvoiceTaxAmt());
			// 將新產生的分錄加入暫存的分錄List
			entryList.add(incomeAmtEntry);
		}

		/*
		 * 6.若「費用申請單.所得稅額」不等於0，產生貸方所得稅科目及金額。  產生貸方科目分錄, 會計科目=「傳入的費用科目.代扣科目代號」,
		 * 金額=「費用申請單.所得稅額」  依傳入的參數(所得人證號類別、所得人證號、業別代號)，填入以下的欄位:(不要填入業別代號) 
		 * 分錄.所得人證號類別  分錄.所得人證號   將新產生的分錄加入暫存的分錄List
		 */
		if (expapplC.getTaxAmt().compareTo(BigDecimal.ZERO) != 0) {
			Entry taxAmtEntry = new Entry();
			// 產生貸方科目分錄
			taxAmtEntry.setEntryType(getEntryTypeForC());

			if (null != accTitle.getWithhold()) {
				// 會計科目=「傳入的費用科目.代扣科目代號」
				taxAmtEntry.setAccTitle(accTitle.getWithhold());
			} else {
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_withhold") });
			}

			// 金額=「費用申請單.所得稅額」
			taxAmtEntry.setAmt(expapplC.getTaxAmt()); // 改抓所得稅
			try {
				// 分錄.所得人證號類別
				taxAmtEntry.setIncomeIdType(incomeIdType.getCode());
			} catch (Exception e) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_IncomeIdType") });
			}
			// 分錄.所得人證號
			taxAmtEntry.setIncomeId(incomeId);
			// 將新產生的分錄加入暫存的分錄List
			entryList.add(taxAmtEntry);
		}

		/*
		 * 7.若「費用申請單. 是否扣繳印花稅」=true，且「費用申請單.印花稅額」不等於0，產生貸方扣抵印花稅之科目及其金額顯示於帳務資料。 
		 * 產生貸方科目分錄, 會計科目=「傳入的費用科目.印花稅代扣科目代號」, 金額=「費用申請單.印花稅額」 
		 * 將新產生的分錄加入暫存的分錄List
		 */
		if (expapplC.isWithholdStamp() && expapplC.getStampAmt().compareTo(BigDecimal.ZERO) != 0) {
			Entry stampAmtEntry = new Entry();
			// 產生貸方科目分錄
			stampAmtEntry.setEntryType(getEntryTypeForC());

			if (null != accTitle.getStampTax()) {
				// 會計科目=「傳入的費用科目.印花稅代扣科目代號」
				stampAmtEntry.setAccTitle(accTitle.getStampTax());
			} else {
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_stampTax") });
			}

			// 金額=「費用申請單.印花稅額」
			stampAmtEntry.setAmt(expapplC.getStampAmt());

			// 將新產生的分錄加入暫存的分錄List
			entryList.add(stampAmtEntry);
		}

		// /*
		// * 8.若「費用申請單.是否開立折讓單」= true, 產生費用科目的折讓單貸方分錄:
		// *  產生貸方科目分錄, 會計科目=傳入的”費用科目”, 金額=「費用申請單.折讓金額(含)」
		// */
		// if (expapplC.isHadAllowanceSlip()) {
		// Entry hadAllowanceSlipEntry = new Entry();
		// // 產生貸方科目分錄
		// hadAllowanceSlipEntry.setEntryType(getEntryTypeForC());
		// //會計科目=傳入的”費用科目”
		// hadAllowanceSlipEntry.setAccTitle(accTitle);
		//
		// //金額=「費用申請單.折讓金額(未)」(#194)
		// hadAllowanceSlipEntry.setAmt(expapplC.getAllowanceNoneTaxAmt());
		//
		// // 將新產生的分錄加入暫存的分錄List
		// entryList.add(hadAllowanceSlipEntry);
		// } (2009/12/28 改為不產生折讓的費用科目，只產生折讓的進項稅科目)

		/*
		 * 9.若(「費用申請單.是否扣繳進項稅」=true AND 「費用申請單.是否開立折讓單」= true ),
		 * 且「費用申請單.折讓金額(稅)」大於0時, 產生費用科目的折讓單貸方進項稅分錄:  產生貸方科目分錄, 會計科目=傳入的”費用科目”,
		 * 金額=以「費用申請單.折讓金額(含)」計算進項稅
		 */
		if (expapplC.isWithholdIncome() && expapplC.isHadAllowanceSlip() && BigDecimal.ZERO.compareTo(expapplC.getAllowanceTaxAmt()) < 0) {
			Entry expEntry = new Entry();
			// 產生貸方科目分錄
			expEntry.setEntryType(getEntryTypeForC());
			try {
				// 會計科目=傳入的”費用科目”
				expEntry.setAccTitle(accTitle);
			} catch (Exception e) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle") });
			}

			// 金額=「費用申請單.折讓金額(稅)」
			expEntry.setAmt(expapplC.getAllowanceTaxAmt());

			// 將新產生的分錄加入暫存的分錄List
			entryList.add(expEntry);

		}

		/** RE201201260_二代健保 2012/12/27 start */
		/*
		 * 11. 若為二代健保費用申請單, 則產生相關分錄
		 * 
		 * 實際扣繳保費=0，貸方不產生"二代健保代扣科目代號"。  *
		 * 實際扣繳保費不為0，依選取之會計科目讀取會計科目設定檔之"二代健保代扣科目代號"
		 * ，產生貸方的"二代健保代扣科目代號"、科目金額=實際扣繳保費之欄位值，且於該扣繳科目記錄所得人類別、所得人證號。
		 * ※若該會計科目的『適用二代健保=Y、所得格式代號=50』 　 * ◎則『所得人證號類別=1.身份證字號
		 * 2.工員工資代號』時，若需扣繳保費且"實際扣繳保費之欄位值不為0"時，產生「20210921應付代收全民健保費-補充保費」科目。 　 *
		 * ◎若所得人證號類別=3員工代號時，需將"預計扣繳保費、實際扣繳保費"欄位清為0 ※若該會計科目的『適用二代健保=Y、所得格式代號不為50』
		 * ◎則『所得人證號類別=1.身份證字號 2.工員工資代號 3員工代號』時，若需扣繳保費且"實際扣繳保費之欄位值不為0"時，產生「20
		 * 
		 * RE201201260_二代健保 cm9539 2012/11/05
		 */
		if (!expapplC.isNonHealthFlag() && expapplC.getActualSupplementaryPremium().compareTo(BigDecimal.ZERO) > 0) {

			boolean isHealInsIncomeForm = doCheckHealInsIncomeForm(accTitle, incomeIdType.getCode());
			if (isHealInsIncomeForm) {
				Entry healInsEntry = new Entry();
				healInsEntry.setEntryType(getEntryTypeForC());
				try {
					healInsEntry.setAccTitle(accTitle.getHealthInsuranceCode());
				} catch (Exception e) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle") });
				}

				// 金額=「實際扣繳保費」
				healInsEntry.setAmt(expapplC.getActualSupplementaryPremium());
				try {
					// 分錄.所得人證號類別
					healInsEntry.setIncomeIdType(incomeIdType.getCode());
				} catch (Exception e) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_IncomeIdType") });
				}
				// 分錄.所得人證號
				healInsEntry.setIncomeId(incomeId);
				// 將新產生的分錄加入暫存的分錄List
				entryList.add(healInsEntry);

			} else {
				expapplC.setActualSupplementaryPremium(BigDecimal.ZERO);
				expapplC.setSupplementaryPremium(BigDecimal.ZERO);

			}
		}
		/** RE201201260_二代健保 2012/12/27 end */

		if (null == expapplC.getEntryGroup()) {
			expapplC.setEntryGroup(new EntryGroup());
		}

		if (CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			expapplC.getEntryGroup().setEntries(new ArrayList<Entry>());
		}

		// 10.將產生的分錄List設回傳入參數的”費用申請單.分錄群組.分錄”
		for (Entry entry : entryList) {
			entry.setEntryGroup(expapplC.getEntryGroup());
			expapplC.getEntryGroup().getEntries().add(entry);
		}
	}

	/**
	 * 取得分錄借貸別(借方)
	 * 
	 * @return 借貸別(借方)
	 */
	private EntryType getEntryTypeForD() {
		return this.facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_3);

	}

	/**
	 * 取得分錄借貸別(貸方)
	 * 
	 * @return 借貸別(貸方)
	 */
	private EntryType getEntryTypeForC() {
		return this.facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4);

	}

	public List<ExpapplC> findExpapplCsBySubpoena(Subpoena subpoena) {
		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("subpoena", subpoena);
		List<ExpapplC> eacList = getDao().findByCriteriaMap(criteriaMap);
		// 2009/12/15,因為只撈費用申請單是不夠的，還要撈費用申請單明細
		if (CollectionUtils.isNotEmpty(eacList)) {
			for (ExpapplC eac : eacList) {
				if (CollectionUtils.isEmpty(eac.getExpapplCDetails())) {
					eac.setExpapplCDetails(facade.getExpapplCDetailService().findByExpapplC(eac));
				}
			}
		}
		return eacList;
	}

	public List<ExpapplC> findExpapplCsWithoutExpapplCDetailBySubpoena(Subpoena subpoena) {
		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("subpoena", subpoena);
		List<ExpapplC> eacList = getDao().findByCriteriaMap(criteriaMap);
		return eacList;
	}

	public void toDelEntrys(ExpapplC expapplC, List<Entry> toDelEntryList) {

		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			if (null == expapplC) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
			}

			if (null == expapplC.getEntryGroup()) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_entryGroup") });
			}

			if (CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_entryGroup_entrys") });
			}
		}

		if (CollectionUtils.isEmpty(toDelEntryList)) {
			return;
		}

		// 刪除expapplC.EntryGroup內的Entry(應付費用科目) modify...2009/9/18
		for (Entry entry : toDelEntryList) {

			// 只處理貸方科目
			if (null == entry.getEntryType() || !EntryTypeCode.TYPE_2_4.getValue().equals(entry.getEntryType().getValue())) {
				// 非貸方科目,跳下一筆分錄
				continue;
			}

			// 找出應付費用科目(貸方)
			if (facade.getAccTitleService().isExpPayableAccTitle(entry.getAccTitle().getCode())) {

				// 刪除(應付費用科目)
				expapplC.getEntryGroup().getEntries().remove(entry);
			}
		}

		// 過濾toDelEntryList,因為申請單類內有存在的舊分錄,則只是更新該舊分錄而不是刪除舊的分錄
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			// 若申請單類內有存在的舊分錄,則只是更新該舊分錄而不是刪除舊的分錄
			if (StringUtils.isEmpty(entry.getId())) {
				// 過濾掉不存在於DB的分錄
				continue;
			}
			toDelEntryList.remove(entry);
		}

		this.doDelEntrys(toDelEntryList);

	}

	public void doDelEntrys(List<Entry> delEntryList) {
		if (!CollectionUtils.isEmpty(delEntryList)) {
			// 刪除 entrys & expapplCDetails
			List<ExpapplCDetail> deleteExpapplCDetailList = new ArrayList<ExpapplCDetail>();
			for (Entry entry : delEntryList) {
				if (null != entry.getExpapplCDetail()) {

					deleteExpapplCDetailList.add(entry.getExpapplCDetail());
				}
			}

			// 刪除已建立的過度付款明細
			List<TransitPaymentDetail> transitPaymentDetailList = this.facade.getTransitPaymentDetailService().findByEntry(delEntryList);
			if (!CollectionUtils.isEmpty(transitPaymentDetailList)) {
				this.facade.getTransitPaymentDetailService().delete(transitPaymentDetailList);
			}

			// 刪除 費用明細
			this.facade.getExpapplCDetailService().delete(deleteExpapplCDetailList);
			// 刪除 分錄
			this.facade.getEntryService().delete(delEntryList);
		}
	}

	public void beforUpdateExp(ExpapplC expapplC, List<Entry> toDelEntryList) throws ExpException, ExpRuntimeException {

		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			if (null == expapplC) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
			}

			if (null == expapplC.getEntryGroup()) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_entryGroup") });
			}

			if (CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_entryGroup_entrys") });
			}

			// throw new ExpException(ErrorCode.A10004);
		}

		// 將所有Entries 放入待刪除的toDelEntryList
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			Entry entry2 = new Entry();
			BeanUtils.copyProperties(entry, entry2);
			toDelEntryList.add(entry2);
		}

	}

	/**
	 * 矯正金額欄位= null 時 將金額欄位塞0
	 * 
	 * @param expapplC
	 */
	private void checkBigDecimalColums(ExpapplC expapplC) {
		if (null == expapplC) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}

		if (null == expapplC.getAllowanceAmt()) {
			expapplC.setAllowanceAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getAllowanceNoneTaxAmt()) {
			expapplC.setAllowanceNoneTaxAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getAllowanceTaxAmt()) {
			expapplC.setAllowanceTaxAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getImposeExp()) {
			expapplC.setImposeExp(BigDecimal.ZERO);
		}

		if (null == expapplC.getIncomeAmt()) {
			expapplC.setIncomeAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getInvoiceAmt()) {
			expapplC.setInvoiceAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getInvoiceNoneTaxAmt()) {
			expapplC.setInvoiceNoneTaxAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getInvoiceTaxAmt()) {
			expapplC.setInvoiceTaxAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getRealityAmt()) {
			expapplC.setRealityAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getStampAmt()) {
			expapplC.setStampAmt(BigDecimal.ZERO);
		}

		if (null == expapplC.getTaxAmt()) {
			expapplC.setTaxAmt(BigDecimal.ZERO);
		}

	}

	public void verifyExpapplC(ExpapplC expapplC, Boolean isCarriedByStages) {
		// If傳入參數為NULL， throw ExpRuntimeExceiption，顯示《傳入參數錯誤》
		if (null == expapplC) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		}

		if (null == expapplC.getEntryGroup()) {
			// 請點選更新費用明細按鈕或新增費用明細按鈕
			throw new ExpRuntimeException(ErrorCode.C10424);
		}

		// 檢核-申請人資訊不可為空
		if (null == expapplC.getApplyUserInfo()) {
			throw new ExpRuntimeException(ErrorCode.A10041, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_applyUserInfo") });
		}

		// 檢核-申請人員工代號不可為空
		if (StringUtils.isBlank(expapplC.getApplyUserInfo().getUserId())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_applyUserInfo_userCode") });
		}

		// 若申請員工代號不存在則丟出錯誤 2010/02/09 by Eustace
		if (null == facade.getUserService().checkUserIsExist(expapplC.getApplyUserInfo().getUserId(), null)) {
			// 若不存在 則丟出 申請人員工代號錯誤!!
			throw new ExpRuntimeException(ErrorCode.A10041, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_applyUserInfo") });
		}

		// 檢核-領款人資訊(領款人資訊不為空時)
		if (null != expapplC.getDrawMoneyUserInfo()) {
			// 檢核-領款人代號不可為空
			if (StringUtils.isBlank(expapplC.getDrawMoneyUserInfo().getUserId())) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_drawMoneyUserInfo_userCode") });
			}

			// 若領款員工代號不存在 則丟出錯誤
			if (null == facade.getUserService().checkUserIsExist(expapplC.getDrawMoneyUserInfo().getUserId(), null)) {
				// 若不存在 則丟出 申請人員工代號錯誤!!
				throw new ExpRuntimeException(ErrorCode.A10041, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_drawMoneyUserInfo_userCode") });
			}
		}

		if (CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			// 請點選更新費用明細按鈕或新增費用明細按鈕
			throw new ExpRuntimeException(ErrorCode.C10424);
		} else {
			for (Entry entry : expapplC.getEntryGroup().getEntries()) {
				if (null == entry.getAccTitle()) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_accTitle") });
				}

				if (null == entry.getEntryGroup()) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_entryGroup") });
				}

				if (StringUtils.isNotBlank(entry.getCostCode()) && entry.getCostCode().length() > 1) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_costCode") });
				}
			}
		}

		// 【檢核】
		/*
		 * If 「費用申請單.課稅別」=”應稅”，則「費用申請單.進項稅額」不可為0  否則throw
		 * ExpRuntimeException，顯示《課稅別為”應稅”，進項稅額不可為0》
		 */
		if (TaxTypeCode.A.equals(TaxTypeCode.getByValue(expapplC.getTaxType()))) {
			if (expapplC.getIncomeAmt().compareTo(BigDecimal.ZERO) <= 0) {
				throw new ExpRuntimeException(ErrorCode.C10075);
			}

			/*
			 * 「費用申請單.進項稅額」必須等於「費用申請單.憑證金額(稅)」 否則throw
			 * ExpRuntimeException，顯示《”費用申請單.進項稅額”欄位值，必須等於”費用申請單.憑證金額(稅)”》
			 */

			if (!expapplC.getIncomeAmt().equals(expapplC.getInvoiceTaxAmt())) {
				throw new ExpRuntimeException(ErrorCode.C10076);
			}
		}

		//  檢核「費用申請單.付款對象」
		if (null == expapplC.getPaymentTarget()) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_paymentTarget") });
		}
		/*
		 *  若付款對象=”個人”，以下欄位不能為空值，否則顯示《”費用申請單. 領款人資訊”欄位值不完全》  「費用申請單.領款人資訊」 
		 * 「費用申請單.領款人資訊.員工代號」  「費用申請單.領款人資訊.員工姓名」  「費用申請單.領款人資訊.匯款單位」 
		 * 「費用申請單.領款人資訊.匯款總行代號」  「費用申請單.領款人資訊.匯款分行代號」  「費用申請單.領款人資訊.匯款帳號」 
		 * 「費用申請單.領款人資訊.個人匯款戶名」  「費用申請單.領款人資訊.所屬單位代號」  「費用申請單.領款人資訊.所屬單位名稱」
		 */
		checkPaymentTargetCodeByPersonal(expapplC);

		/*
		 *  若付款對象=”單位” ，以下欄位不能為空值，否則顯示《”費用申請單. 領款人單位”欄位值不完全》  「費用申請單. 領款單位代碼」
		 *  「費用申請單. 領款單位名稱」
		 */
		if (PaymentTargetCode.BUSSINESS_REVIEW.getCode().equals(expapplC.getPaymentTarget().getCode())) {
			if (StringUtils.isBlank(expapplC.getDrawMoneyUnitCode())) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_drawMoneyUnitCode") });
			}
			if (StringUtils.isBlank(expapplC.getDrawMoneyUnitName())) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_drawMoneyUnitName") });
			}
		}

		//  檢核「費用申請單.付款方式」
		if (null == expapplC.getPaymentType() || !SysTypeCode.C.equals(SysTypeCode.getByValue(expapplC.getPaymentType().getSysType()))) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_paymentType") });
		}

		/*
		 *  計算以下總額:  借方總額  貸方總額扣除應付費用科目金額: 貸方總額減掉以下科目的總額  20210360 應付費用-總帳費用
		 *  20210391應付費用-待匯科目  20210392 應付費用-待開  若”借方總額”小於”
		 * 貸方總額扣除應付費用科目金額”，throw ExpRuntimeException，顯示《借方總額不能小於貸方總額》 
		 * 若”借方總額”減” 貸方總額扣除應付費用科目金額”等於0，則跳過以下「費用申請單.付款方式」的檢核
		 */
		caculateEntriesAmt(expapplC);

		// 借貸是否平衡
		this.facade.getEntryGroupService().calcBalance(expapplC.getEntryGroup());

		/*
		 *  檢核「費用申請單.分錄群組.借貸平衡」是否等於true  否則throw
		 * ExpRuntimeException，顯示《帳務資料錯誤，借方與貸方不平衡》
		 */
		if (!expapplC.getEntryGroup().isBalanced()) {
			// 顯示《帳務資料錯誤，借方與貸方不平衡》
			throw new ExpRuntimeException(ErrorCode.C10081);
		}

		/*
		 * modify...2009/9/1, By Eustace  檢核「費用申請單.冊號類別」欄位 
		 * 若冊號類別=NULL，「費用申請單.冊號1」、「費用申請單.冊號2」必須為空值  否則throw
		 * ExpRuntimeException，顯示《費用申請單錯誤，非名冊的申請單，冊號欄位必須為空值》  若冊號類別=”
		 * 獎金品冊號”，「費用申請單.冊號1」不能為空值  否則throw
		 * ExpRuntimeException，顯示《費用申請單錯誤，獎金品申請單，冊號欄位必須填值》  若冊號類別不等於”
		 * 獎金品冊號”，「費用申請單.冊號1」不能為空值且「費用申請單.冊號2」必須為空值  否則throw
		 * ExpRuntimeException，顯示《費用申請單錯誤，一般名冊的申請單，必須填入唯一的冊號欄位》
		 */

		// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 START
		// C1.5.13名冊輸入方式從TBEXP_EXPAPPL_C.LIST1、LIST2改為TBEXP_ROSTER_DETAIL.TBEXP_EXPAPPL_C的方式
		if (MiddleTypeCode.CODE_N10.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType()))) {

		} else {
			checkExpapplCByListType(expapplC);
		}
		// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 END

		/*
		 * 若「費用申請單.費用項目」=”獎金品”(費用項目代號63301000、63302000)(2010/1/6)
		 * 
		 * 「費用申請單.付款方式」=”沖轉暫付”，設定「費用申請單.稅金需匯回」 = true。(其它狀況不用更新此欄位)
		 * 
		 * 「費用申請單.費用中分類」=”N10辦公費”且「費用申請單.付款方式」=”開票”，設定「費用申請單.稅金需匯回」 = true。
		 */
		if (null != expapplC.getExpItem() && (ExpItemCode.INCENTIVE_EXP_AWARD.getCode().equals(expapplC.getExpItem().getCode()) || ExpItemCode.INCENTIVE_EXP_BONUS.getCode().equals(expapplC.getExpItem().getCode()))) {

			if (PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(expapplC.getPaymentType().getCode())) {
				// 「費用申請單.付款方式」=”沖轉暫付”，設定「費用申請單.稅金需匯回」 = true。
				expapplC.setNeedTaxRemit(true);
			}

			if (null != expapplC.getMiddleType() && MiddleTypeCode.CODE_N10.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType())) && PaymentTypeCode.C_CHECK.getCode().equals(expapplC.getPaymentType().getCode())) {
				//  「費用申請單.費用中分類」=”N10辦公費”且「費用申請單.付款方式」=”開票”，設定「費用申請單.稅金需匯回」 =
				// true。
				expapplC.setNeedTaxRemit(true);
			}
		}

		// 檢核-費用年月(不可為空值)
		if (StringUtils.isBlank(expapplC.getExpYears())) {
			// 丟出 費用年月欄位不可為空白，請輸入資料。
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expYears") });
		}

		/*
		 *  檢核「費用申請單.憑證附於」欄位(#176) 
		 * 若「費用申請單.憑證附於」有值，查詢是否存在「費用申請單.申請單號」=「費用申請單.憑證附於
		 * 」且「費用申請單.申請狀態」不等於”刪除”的費用申請單。  若不存在，throw
		 * ExpRuntimeExcption，顯示《憑證附於單號錯誤》
		 */
		if (StringUtils.isNotBlank(expapplC.getProofAdd())) {
			// 查詢申請單號是否存在
			if (!isExpApplNoExists(expapplC.getProofAdd())) {
				// 不存在，throw ExpRuntimeExcption，顯示《憑證附於單號錯誤》
				throw new ExpRuntimeException(ErrorCode.C10261);
			}
		}

		// 檢核此申請單是否有被憑證附於, 若被憑證附於時則發票號碼皆要相同
		checkProofAddedInfo(expapplC);

		/*
		 * 檢核-借方總額 須小於、等於「費用申請單.憑證金額(含)」(#180)
		 * 
		 * 若「費用申請單.費用中分類」等於 “A10_廠商費用_設備” 或 “A20_廠商費用_一般” 
		 * 以申請單號查出此「廠商費用」，若該「廠商費用.是否分期結轉」=TRUE，則不執行此檢核; 反之則繼續檢核。 
		 * 累計所有「分錄.科目借貸別」=”借方”的「分錄.金額」。累計的借方總額 需小於、等於 「費用申請單.憑證金額(含)」  否則throw
		 * ExpRuntimeException，顯示《借方總額 需小於、等於 憑證金額(含)》
		 * 
		 * 檢核費用、資產類科目的「分錄.成本單位代號」及「分錄.成本單位名稱」欄位(#180) 
		 * 檢查所有「分錄」中，檢查該「分錄.會計科目.會計科目分類」欄位符合以下情況時， 「分錄.成本單位代號」及「分錄.成本單位名稱」都必須有值;
		 * 否則顯示《費用及資產類科目，成本單位必須有值》 ▲ 若「會計科目分類」=”4費用”時，需檢核 ▲
		 * 若「會計科目分類」=”1資產”時，若該「會計科目」與「費用項目」有關聯時，才需檢核 ☆
		 * 以「會計科目」為查詢條件，並以「費用項目」INNER JOIN「會計科目」
		 */
		checkEntriesAmtC(expapplC, isCarriedByStages);

		/*
		 * 若「費用申請單.課稅別」=2零稅率或3免稅，分錄中不可存在「進項稅科目」
		 * (會計科目前5碼為「10531」即為進項稅科目)，且「費用申請單.憑證金額(稅)」 及「費用申請單.進項稅」需為0(#233) 
		 * 若分錄中存在進項稅科目，throw
		 * ExpRuntimeexception，顯示《課稅別為”零稅率”、”免稅”時，不可存在進項稅科目》且不可儲存入檔
		 * 
		 *  若「費用申請單.憑證金額(稅)」及「費用申請單.進項稅」不為0，throw
		 * ExpRuntimeexception，顯示《課稅別為”零稅率”、”免稅”時，憑證金額(稅)、進項稅額需為0》且不可儲存入檔
		 */
		if (TaxTypeCode.B.equals(TaxTypeCode.getByValue(expapplC.getTaxType())) || TaxTypeCode.C.equals(TaxTypeCode.getByValue(expapplC.getTaxType()))) {
			// 若「費用申請單.憑證金額(稅)」及「費用申請單.進項稅」不為0，throw
			// ExpRuntimeexception，顯示《課稅別為”零稅率”、”免稅”時，憑證金額(稅)、進項稅額需為0》且不可儲存入檔
			if (BigDecimal.ZERO.compareTo(expapplC.getInvoiceTaxAmt()) != 0 || BigDecimal.ZERO.compareTo(expapplC.getIncomeAmt()) != 0) {
				// 顯示《課稅別為”零稅率”、”免稅”時，憑證金額(稅)、進項稅額需為0》
				throw new ExpRuntimeException(ErrorCode.C10379);
			}

			// 若分錄中存在進項稅科目，throw
			// ExpRuntimeexception，顯示《課稅別為”零稅率”、”免稅”時，不可存在進項稅科目》且不可儲存入檔
			for (Entry entry : expapplC.getEntryGroup().getEntries()) {
				if (null == entry.getAccTitle()) {
					continue;
				}
				// 若分錄中存在進項稅科目
				if (facade.getAccTitleService().isIncomeAccTitle(entry.getAccTitle().getCode())) {
					// 顯示《課稅別為”零稅率”、”免稅”時，不可存在進項稅科目》
					throw new ExpRuntimeException(ErrorCode.C10380);
				}
			}
		}

		/*
		 * 檢核所有「分錄」，若「分錄.成本單位代號」不為空值者，以該值查詢「組織單位」，並依下列規則檢核: (#240)
		 * 
		 * 若該「成本單位.組織型態=0總公司 或 1服務中心」時，其「成本單位.層級屬性」必須為”1部級”。 否則throw
		 * ExpRuntimeException，顯示《需以部室(部層級)做為成本單位》，且不可儲存入檔。
		 * 
		 * 若該「成本單位.組織型態 不等於 0總公司 或 1服務中心」時，依下列規則檢核:
		 * 
		 *  若該「成本單位.成本歸屬單位」為空值，可儲存入檔。
		 * 
		 * 若該「成本單位.成本歸屬單位」為有值，且與頁面輸入的成本單位不同時， 顯示《需以本處做為成本單立》訊息，且不可儲存入檔。
		 */
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (StringUtils.isBlank(entry.getCostUnitCode())) {
				continue;
			}
			facade.getDepartmentService().checkDepartmentCode(entry.getCostUnitCode());
			Department department = facade.getDepartmentService().findByCode(entry.getCostUnitCode());

			// 若該「成本單位.是否啟用」=False，顯示《找不到組織單位xxx》，xxx為該「成本單位.單位代號」+「成本單位.單位名稱」(2010/1/14)
			if (null != department && false == department.isEnabled()) {
				throw new ExpRuntimeException(ErrorCode.C10508, new String[] { department.getCode() + department.getName() });
			}
			if (null != department.getDepartmentType() && (StringUtils.equals(DepartmentTypeCode.PARENT_COMPANY.getCode(), department.getDepartmentType().getCode()) || StringUtils.equals(DepartmentTypeCode.SERVICE_CENTER.getCode(), department.getDepartmentType().getCode()))) {
				// 若該「成本單位.組織型態=0總公司 或 1服務中心」時，其「成本單位.層級屬性」必須為”1部級”。
				if (!DepartmentLevelPropertyCode.A.getCode().equals(department.getDepartmentLevelProperty().getCode())) {
					// 否則throw ExpRuntimeException，顯示《需以部室(部層級)做為成本單位》，且不可儲存入檔。
					throw new ExpRuntimeException(ErrorCode.C10389);
				}
			} else {
				if (null == department.getDepartmentCost()) {
					// 若該「成本單位.成本歸屬單位」為空值，可儲存入檔。
					continue;
				}
				// 若該「成本單位.成本歸屬單位」為有值，且與頁面輸入的成本單位不同時
				if (!StringUtils.equals(department.getDepartmentCost().getCode(), entry.getCostUnitCode())) {
					// 顯示《需以本處做為成本單立》訊息，且不可儲存入檔。
					throw new ExpRuntimeException(ErrorCode.C10390);
				}
			}

		}

		facade.getEntryService().generateUnitDepInfo(expapplC.getEntryGroup().getEntries());
		// 矯正金額欄位= null 時 將金額欄位塞0
		checkBigDecimalColums(expapplC);
	}

	/**
	 * <pre>
	 * 檢核-借方總額 須小於、等於「費用申請單.憑證金額(含)」(#180)
	 * 
	 *  若「費用申請單.費用中分類」等於 “A10_廠商費用_設備” 或 “A20_廠商費用_一般”
	 *         以申請單號查出此「廠商費用」，若該「廠商費用.是否分期結轉」=TRUE，則不執行此檢核; 反之則繼續檢核。
	 *  
	 *  累計所有「分錄.科目借貸別」=”借方”的「分錄.金額」。累計的借方總額 需小於、等於 「費用申請單.憑證金額(含)」
	 *         否則throw ExpRuntimeException，顯示《借方總額 需小於、等於 憑證金額(含)》
	 * 
	 *  檢核費用、資產類科目的「分錄.成本單位代號」及「分錄.成本單位名稱」欄位(#180)
	 *         檢查所有「分錄」中，檢查該「分錄.會計科目.會計科目分類」欄位符合以下情況時，
	 *      「分錄.成本單位代號」及「分錄.成本單位名稱」都必須有值;
	 *      否則顯示《費用及資產類科目，成本單位必須有值》
	 *       ▲       若「會計科目分類」=”4費用”時，需檢核
	 *       ▲       若「會計科目分類」=”1資產”時，若該「會計科目」與「費用項目」有關聯時，才需檢核
	 *       ☆       以「會計科目」為查詢條件，並以「費用項目」INNER JOIN「會計科目」
	 * 
	 *  @param expapplC 行政費用申請單
	 * @param isCarriedByStages 是否分期結轉
	 */
	private void checkEntriesAmtC(ExpapplC expapplC, Boolean isCarriedByStages) {
		BigDecimal amtC = BigDecimal.ZERO; // 借方總額
		boolean isMiddyTypeChecked = false;
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (EntryTypeCode.TYPE_2_3.getValue().equals(entry.getEntryType().getValue())) {
				amtC = amtC.add(entry.getAmt());
			}

			if (!isMiddyTypeChecked) {
				if (MiddleTypeCode.CODE_A10.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType())) || MiddleTypeCode.CODE_A20.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType()))) {
					// 中分類 = A10 || A20
					// //以申請單號查出此「廠商費用」，若該「廠商費用.是否分期結轉」=TRUE，則不執行此檢核; 反之則繼續檢核。
					// VendorExp exp =
					// facade.getVendorExpService().findByExpApplNoFetchRelation(expapplC.getExpApplNo());

					if (null == isCarriedByStages) {
						throw new ExpRuntimeException(ErrorCode.A10007, new String[] { "isCarriedByStages" });
					}
					if (isCarriedByStages) {
						// 不執行此檢核
						break;
					}
				}
				isMiddyTypeChecked = true;
			}

			/* RE201201260 二代健保 匯回款項 20130222 START */
			// 累計的借方總額 需小於、等於 「費用申請單.憑證金額(含)」
			if (!paybackType.equals("3")) {
				if (amtC.compareTo(expapplC.getInvoiceAmt()) > 0) {
					if (null == isCarriedByStages || false == isCarriedByStages) {
						// 顯示《借方總額 需小於、等於 憑證金額(含)》
						throw new ExpRuntimeException(ErrorCode.C10262);
					}
				}
			}
			/* RE201201260 二代健保 匯回款項 20130222 End */

			if (AccClassTypeCode.CODE_4.equals(AccClassTypeCode.getByValue(entry.getAccTitle().getAccClassType()))) {
				if (StringUtils.isBlank(entry.getCostUnitCode()) || StringUtils.isBlank(entry.getCostUnitName())) {
					// 顯示《費用及資產類科目，成本單位必須有值》
					throw new ExpRuntimeException(ErrorCode.C10263);
				}
			}

			if (AccClassTypeCode.CODE_1.equals(AccClassTypeCode.getByValue(entry.getAccTitle().getAccClassType()))) {
				if (!CollectionUtils.isEmpty(facade.getExpItemService().findByAcctitleCode(entry.getAccTitle().getCode()))) {
					if (StringUtils.isBlank(entry.getCostUnitCode()) || StringUtils.isBlank(entry.getCostUnitName())) {
						// 顯示《費用及資產類科目，成本單位必須有值》
						throw new ExpRuntimeException(ErrorCode.C10263);
					}
				}
			}
		}
	}

	/**
	 * <pre>
	 * modify...2009/9/1, By Eustace
	 *     檢核「費用申請單.冊號類別」欄位
	 *        若冊號類別=NULL，「費用申請單.冊號1」、「費用申請單.冊號2」必須為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，非名冊的申請單，冊號欄位必須為空值》
	 *        若冊號類別=” 獎金品冊號”，「費用申請單.冊號1」不能為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，獎金品申請單，冊號欄位必須填值》
	 *        若冊號類別不等於” 獎金品冊號”，「費用申請單.冊號1」不能為空值且「費用申請單.冊號2」必須為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，一般名冊的申請單，必須填入唯一的冊號欄位》
	 */
	private void checkExpapplCByListType(ExpapplC expapplC) {
		if (null == expapplC.getListType()) {
			// 若冊號類別=NULL，「費用申請單.冊號1」、「費用申請單.冊號2」必須為空值
			if (StringUtils.isNotBlank(expapplC.getListNo1()) || StringUtils.isNotBlank(expapplC.getListNo2())) {
				// 顯示《費用申請單錯誤，非名冊的申請單，冊號欄位必須為空值》
				throw new ExpRuntimeException(ErrorCode.C10101);
			}

		} else if (ListTypeCode.PREMIUM_AWARD.equals(ListTypeCode.getByValue(expapplC.getListType()))) {
			// 若冊號類別=” 獎金品冊號”，「費用申請單.冊號1」不能為空值
			if (StringUtils.isBlank(expapplC.getListNo1())) {
				// 顯示《費用申請單錯誤，獎金品申請單，冊號欄位必須填值》
				throw new ExpRuntimeException(ErrorCode.C10102);
			}

		} else {
			// 若冊號類別不等於” 獎金品冊號”，「費用申請單.冊號1」不能為空值且「費用申請單.冊號2」必須為空值
			if (StringUtils.isBlank(expapplC.getListNo1()) || StringUtils.isNotBlank(expapplC.getListNo2())) {
				// 顯示《費用申請單錯誤，一般名冊的申請單，必須填入唯一的冊號欄位》
				throw new ExpRuntimeException(ErrorCode.C10103);
			}
		}
	}

	/**
	 * <pre>
	 *     計算以下總額:
	 *     借方總額
	 *     貸方總額扣除應付費用科目金額: 貸方總額減掉以下科目的總額
	 *        20210360 應付費用-總帳費用
	 *        20210391應付費用-待匯科目
	 *        20210392 應付費用-待開
	 *        若”借方總額”小於” 貸方總額扣除應付費用科目金額”，throw ExpRuntimeException，顯示《借方總額不能小於貸方總額》
	 *        若”借方總額”減” 貸方總額扣除應付費用科目金額”等於0，則跳過以下「費用申請單.付款方式」的檢核
	 * 
	 */
	private void caculateEntriesAmt(ExpapplC expapplC) {
		BigDecimal totalAmtD = BigDecimal.ZERO;// 借方總額
		BigDecimal totalAmtC = BigDecimal.ZERO;// 代方總額
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (null == entry.getEntryType()) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_entryType") });
			}
			if (EntryTypeCode.TYPE_1_D.getValue().equals(entry.getEntryType().getValue())) {
				totalAmtD = totalAmtD.add(entry.getAmt());
			}

			if (EntryTypeCode.TYPE_1_C.getValue().equals(entry.getEntryType().getValue())) {
				if (facade.getAccTitleService().isExpPayableAccTitle(entry.getAccTitle().getCode())) {
					// 過濾應付費用科目
					continue;
				}
				totalAmtC = totalAmtC.add(entry.getAmt());
			}
		}

		if (totalAmtD.compareTo(totalAmtC) > 0) {
			// 借方大於貸方
			// 檢核「費用申請單.付款方式」
			doCheckPaymentType(expapplC);
		} else if (totalAmtD.compareTo(totalAmtC) < 0) {
			// ”借方總額”小於” 貸方總額
			// 顯示《借方總額不能小於貸方總額》
			throw new ExpRuntimeException(ErrorCode.C10296);
		}
	}

	/**
	 * @param expapplC
	 */
	private void checkPaymentTargetCodeByPersonal(ExpapplC expapplC) {
		if (PaymentTargetCode.PERSONAL.getCode().equals(expapplC.getPaymentTarget().getCode())) {
			if (null == expapplC.getDrawMoneyUserInfo()) {
				throw new ExpRuntimeException(ErrorCode.A10041, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_drawMoneyUserInfo") });
			}

			ApplInfo drawMoneyUserInfo = expapplC.getDrawMoneyUserInfo();
			List<String> errorString = new ArrayList<String>();

			// 「費用申請單.領款人資訊.員工代號」
			if (StringUtils.isBlank(drawMoneyUserInfo.getUserId())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_userId"));
			}

			// 「費用申請單.領款人資訊.員工姓名」
			if (StringUtils.isBlank(drawMoneyUserInfo.getUserName())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_userName"));
			}

			// //「費用申請單.領款人資訊.匯款單位」
			// if (StringUtils.isBlank(drawMoneyUserInfo.getRunitCode())) {
			// errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_runitCode"));
			// }

			// 「費用申請單.領款人資訊.匯款總行代號」
			if (StringUtils.isBlank(drawMoneyUserInfo.getRemitBank())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_remitBank"));
			}

			// 「費用申請單.領款人資訊.匯款分行代號」
			if (StringUtils.isBlank(drawMoneyUserInfo.getRemitSubBank())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_remitSubBank"));
			}

			// 「費用申請單.領款人資訊.匯款帳號」
			if (StringUtils.isBlank(drawMoneyUserInfo.getRemitAccount())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_remitAccount"));
			}

			// 「費用申請單.領款人資訊.個人匯款戶名」
			if (StringUtils.isBlank(drawMoneyUserInfo.getRemitAccName())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_remitAccName"));
			}

			// 「費用申請單.領款人資訊.所屬單位代號」
			if (StringUtils.isBlank(drawMoneyUserInfo.getDepUnitCode3())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_depUnitCode3"));
			}

			// 「費用申請單.領款人資訊.所屬單位名稱」
			if (StringUtils.isBlank(drawMoneyUserInfo.getDepUnitName3())) {
				errorString.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_depUnitName3"));
			}

			if (!CollectionUtils.isEmpty(errorString)) {
				throw new ExpRuntimeException(ErrorCode.C10082, new String[] { errorString.toString() });
			}
		}
	}

	/**
	 * @param expapplC
	 */
	private void doCheckPaymentType(ExpapplC expapplC) {
		/*
		 * modify 2009/8/20 By Eustace 若「費用申請單.費用中分類.費用大分類」=01廠商費用，分錄要有唯一的”
		 * 20210360 應付費用-總帳費用”，且出現在貸方 否則throw
		 * ExpRuntimeException，顯示《帳務資料錯誤，廠商只能有唯一的應付費用-總帳費用科目》
		 */
		if (BigTypeCode.VENDOR_EXP.equals(BigTypeCode.getByValue(expapplC.getMiddleType().getBigType()))) {
			Entry entry = null;

			for (Entry en : expapplC.getEntryGroup().getEntries()) {
				if (!EntryTypeCode.TYPE_2_4.getValue().equals(en.getEntryType().getValue())) {
					continue;
				}
				if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {
					if (null != entry) {
						// 顯示《帳務資料錯誤，廠商只能有唯一的應付費用-總帳費用科目》
						throw new ExpRuntimeException(ErrorCode.C10083);
					}
					entry = en;
				}

				if (AccTitleCode.PAYBLE_CHECK.getCode().equals(en.getAccTitle().getCode())) {
					// 顯示《帳務資料錯誤，廠商只能有唯一的應付費用-總帳費用科目》
					throw new ExpRuntimeException(ErrorCode.C10083);
				}

				if (AccTitleCode.PAYBLE_REMIT.getCode().equals(en.getAccTitle().getCode())) {
					// 顯示《帳務資料錯誤，廠商只能有唯一的應付費用-總帳費用科目》
					throw new ExpRuntimeException(ErrorCode.C10083);
				}
			}

			if (null == entry || !EntryTypeCode.TYPE_2_4.getValue().equals(entry.getEntryType().getValue())) {
				// 顯示《帳務資料錯誤，廠商只能有唯一的應付費用-總帳費用科目》
				throw new ExpRuntimeException(ErrorCode.C10083);
			}
		} else {
			//  若非廠商費用

			/*
			 * 且付款方式=”匯款”  分錄要有唯一的”20210391應付費用-待匯科目”，且出現在貸方  否則throw
			 * ExpRuntimeException，顯示《帳務資料錯誤，只能有唯一的待匯科目》
			 */
			if (PaymentTypeCode.C_REMIT.getCode().equals(expapplC.getPaymentType().getCode())) {
				Entry entry = null;

				for (Entry en : expapplC.getEntryGroup().getEntries()) {
					if (!EntryTypeCode.TYPE_2_4.getValue().equals(en.getEntryType().getValue())) {
						continue;
					}
					if (AccTitleCode.PAYBLE_REMIT.getCode().equals(en.getAccTitle().getCode())) {
						if (null != entry) {
							// 顯示《帳務資料錯誤，只能有唯一的待匯科目》
							throw new ExpRuntimeException(ErrorCode.C10077);
						}
						entry = en;
					}

					if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {
						// 顯示《帳務資料錯誤，只能有唯一的待匯科目》
						throw new ExpRuntimeException(ErrorCode.C10077);
					}

					if (AccTitleCode.PAYBLE_CHECK.getCode().equals(en.getAccTitle().getCode())) {
						// 顯示《帳務資料錯誤，只能有唯一的待匯科目》
						throw new ExpRuntimeException(ErrorCode.C10077);
					}

				}

				if (null == entry || !EntryTypeCode.TYPE_2_4.getValue().equals(entry.getEntryType().getValue())) {
					// 顯示《帳務資料錯誤，只能有唯一的待匯科目》
					throw new ExpRuntimeException(ErrorCode.C10077);
				}
			}

			/*
			 * 且付款方式=”開票”  分錄要有唯一的” 20210392 應付費用-待開” ，且出現在貸方  否則throw
			 * ExpRuntimeException，顯示《帳務資料錯誤，只能有唯一的待開科目》
			 */
			if (PaymentTypeCode.C_CHECK.getCode().equals(expapplC.getPaymentType().getCode())) {
				Entry entry = null;
				for (Entry en : expapplC.getEntryGroup().getEntries()) {
					if (!EntryTypeCode.TYPE_2_4.getValue().equals(en.getEntryType().getValue())) {
						continue;
					}
					if (AccTitleCode.PAYBLE_CHECK.getCode().equals(en.getAccTitle().getCode())) {
						if (null != entry) {
							// 顯示《帳務資料錯誤，只能有唯一的待開科目》
							throw new ExpRuntimeException(ErrorCode.C10078);
						}
						entry = en;
					}

					if (AccTitleCode.PAYBLE_REMIT.getCode().equals(en.getAccTitle().getCode())) {
						// 顯示《帳務資料錯誤，只能有唯一的待開科目》
						throw new ExpRuntimeException(ErrorCode.C10078);
					}

					if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {
						// 顯示《帳務資料錯誤，只能有唯一的待開科目》
						throw new ExpRuntimeException(ErrorCode.C10078);
					}
				}

				if (null == entry || !EntryTypeCode.TYPE_2_4.getValue().equals(entry.getEntryType().getValue())) {
					// 顯示《帳務資料錯誤，只能有唯一的待開科目》
					throw new ExpRuntimeException(ErrorCode.C10078);
				}
			}
		}

		/*
		 *  若付款方式=”沖轉暫付”，分錄只能有最多一筆的” 20210391應付費用-待匯科目” ，且出現在貸方  否則throw
		 * ExpRuntimeException，顯示《帳務資料錯誤，只能最多有一筆待匯科目》
		 */
		if (PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(expapplC.getPaymentType().getCode())) {
			Entry entry = null;
			boolean flag = false; // 預設無銷帳碼
			for (Entry en : expapplC.getEntryGroup().getEntries()) {
				if (!EntryTypeCode.TYPE_2_4.getValue().equals(en.getEntryType().getValue())) {
					continue;
				}
				if (AccTitleCode.PAYBLE_CHECK.getCode().equals(en.getAccTitle().getCode())) {
					// 帳務資料錯誤，只能最多有一筆待匯科目
					throw new ExpRuntimeException(ErrorCode.C10079);
				}

				if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {
					// 帳務資料錯誤，只能最多有一筆待匯科目
					throw new ExpRuntimeException(ErrorCode.C10079);
				}

				if (AccTitleCode.PAYBLE_REMIT.getCode().equals(en.getAccTitle().getCode())) {
					if (null != entry) {
						// 帳務資料錯誤，只能最多有一筆待匯科目
						throw new ExpRuntimeException(ErrorCode.C10079);
					}
					entry = en;
				}

				if (StringUtils.isNotBlank(en.getCancelCode())) {
					flag = true;// 有銷帳碼
				}

			}
			/*
			 *  若付款方式=”沖轉暫付”，必須有一筆「分錄.銷帳碼」有值  否則throw
			 * ExpRuntimeException，顯示《請填入銷帳碼》
			 */
			if (flag == false) {
				// 顯示《請填入銷帳碼》
				throw new ExpRuntimeException(ErrorCode.C10080);
			}

		}
	}

	public void verifyExpapplC(ExpapplC expapplC) {
		this.verifyExpapplC(expapplC, null);
	}

	/* RE201201260 二代健保 匯回款項 20130222 START */
	public void verifyExpapplC(String paybackType) {
		this.paybackType = paybackType;
	}

	/* RE201201260 二代健保 匯回款項 20130222 End */

	public List<ExpapplC> findByDeliverDayListNos(List<String> deliverDayListNos, ApplStateCode[] applStateCodes) throws ExpRuntimeException {

		if (CollectionUtils.isEmpty(deliverDayListNos)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo") });
		}

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct e");
		queryString.append(" from ExpapplC e");
		queryString.append(" where");
		Map<String, Object> params = new HashMap<String, Object>();
		queryString.append(" e.deliverDaylist.deliverNo in(");
		int index = 0;
		for (String deliverNo : deliverDayListNos) {
			if (index != 0) {
				queryString.append(",");
			}
			String key = "deliverNo" + index;
			queryString.append(" :" + key);
			params.put(key, deliverNo);

			index++;
		}
		queryString.append(")");

		if (null != applStateCodes && applStateCodes.length > 0) {
			queryString.append(" and e.applState.code in(");
			StringBuffer key = new StringBuffer();
			index = 0;
			for (ApplStateCode applStateCode : applStateCodes) {
				if (index != 0) {
					queryString.append(",");
				}
				key.append("applStateCode");
				key.append(applStateCode.getCode());
				queryString.append(" :" + key.toString());
				params.put(key.toString(), applStateCode.getCode());
				index++;
			}
			queryString.append(")");
		}

		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	public void checkCashGiftApplQuota(ExpapplC expapplC) {
		// 費用申請單錯誤
		if (expapplC == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		// 2009/11/20,CR#166 費用項目改為==職員禮金、奠儀：費用項目代號61130100
		// 「費用申請單.費用項目」不等於” 職員禮金、奠儀，費用項目代號61130100”，則回傳
		if (!ExpItemCode.CHIEF_CASHGIFT_ALLOWANCE.getCode().equals(expapplC.getExpItem().getCode())) {
			return;
		}
		// 費用明細錯誤
		if (CollectionUtils.isEmpty(expapplC.getExpapplCDetails())) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail") });
		}
		// 婚喪禮金人數錯誤
		int cashGiftPersonAmt = 0;
		for (ExpapplCDetail ecd : expapplC.getExpapplCDetails()) {
			if (ecd.getCashGiftPersonAmt() == null || ecd.getCashGiftPersonAmt() <= 0) {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail_cashGiftPersonAmt") });
			} else if (ecd.getCashGiftPersonAmt() != null) {
				cashGiftPersonAmt = ecd.getCashGiftPersonAmt() + cashGiftPersonAmt;
			}
		}
		// 檢查「費用申請單.分錄群組.分錄」List中，是否存在借方且「分錄.會計科目」等於”維持費用-交際費-交際費”
		EntryGroup eg = expapplC.getEntryGroup();
		boolean haveAccTitleEquals62102000 = false;
		BigDecimal cgYearAmt = BigDecimal.ZERO;
		for (Entry en : eg.getEntries()) {
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (AccTitleCode.SOCIAL.getCode().equals(en.getAccTitle().getCode())) {
					haveAccTitleEquals62102000 = true;
					cgYearAmt = cgYearAmt.add(en.getAmt());
				}
			}
		}
		if (!haveAccTitleEquals62102000) {
			throw new ExpRuntimeException(ErrorCode.C10104);
		}
		// 取得申請人年度申請限額，執行共用function《取得婚喪禮金年度申請限額》
		CashGiftYyApplQuota cashGiftYearQuota = facade.getCashGiftYyApplQuotaService().getApplQuotaByUserCode(expapplC.getApplyUserInfo().getUserId(), expapplC.getExpYears());
		if (cashGiftYearQuota == null) {
			throw new ExpRuntimeException(ErrorCode.C10105);
		}
		// 取得該職位的單次限額(部份名稱):
		CashGiftPerApplQuota cashGiftPerQuota = facade.getCashGiftPerApplQuotaService().getApplQuotaByUserCode(expapplC.getApplyUserInfo().getUserId());
		if (cashGiftPerQuota == null) {
			throw new ExpRuntimeException(ErrorCode.C10106);
		}
		// 該次申請金額
		/*
		 * 數” 費用申請單.費用明細.婚喪禮金人數” 乘以 “該職位的單次限額.金額”小於傳入參數”費用申請單.憑證金額(含)”， 則throw
		 * new ExpRuntimeException，顯示《已超過主管婚喪禮金補助「單次」限額》
		 */
		if (cashGiftPerQuota.getAmt().multiply(BigDecimal.valueOf(cashGiftPersonAmt)).compareTo(expapplC.getInvoiceAmt()) < 0) {
			throw new ExpRuntimeException(ErrorCode.C10107);
		}
		// 累積申請人於系統年度申請婚喪禮金總金額:
		BigDecimal cgYearAmtByApplUser = calculateCashGiftYearAmtByApplyUser(expapplC, cashGiftYearQuota, cashGiftPerQuota);
		if (cgYearAmt.add(cgYearAmtByApplUser).compareTo(cashGiftYearQuota.getAmt()) > 0) {
			throw new ExpRuntimeException(ErrorCode.C10108);
		}

	}

	/**
	 * <p>
	 * 累積申請人於系統年度申請單婚喪禮金總金額
	 * </p>
	 * 
	 * @param eac
	 * @param cashGiftYearQuota
	 * @param cashGiftPerQuota
	 * @return BigDecimal 累積金額
	 */
	private BigDecimal calculateCashGiftYearAmtByApplyUser(ExpapplC eac, CashGiftYyApplQuota cashGiftYearQuota, CashGiftPerApplQuota cashGiftPerQuota) {
		StringBuffer querySql = new StringBuffer();
		Map<String, Object> params = new HashMap<String, Object>();
		querySql.append("select sum(en.amt) from ExpapplC eac join eac.applyUserInfo aui " + "  join eac.entryGroup eg join eg.entries en join en.entryType et" + " where eac.expYears=:expYear and aui.userId=:userId and eac.expItem.code=:expItemCode" + " and eac.applState.code=:applStateCode and en.accTitle.code=:accTitleCode" + " and et.value=:etCode");
		params.put("expYear", eac.getExpYears().substring(0, 4) + "%");
		params.put("userId", eac.getApplyUserInfo().getUserId());
		params.put("expItemCode", eac.getExpItem().getCode());
		params.put("applStateCode", eac.getApplState().getCode());
		params.put("accTitleCode", AccTitleCode.SOCIAL.getCode());
		params.put("etCode", EntryTypeCode.TYPE_1_D.getValue());
		Object o = getDao().findByNamedParams(querySql.toString(), params);
		if (o instanceof BigDecimal) {
			return (BigDecimal) o;
		}
		return BigDecimal.ZERO;
	}

	public void checkCashGiftAllowanceQuota(ExpapplC eac, ExpapplCDetail expapplCDetail) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		if (!ExpItemCode.GIFT_BEREAVED_GIVENFUNERAL.getCode().equals(eac.getExpItem().getCode()))
			return;

		if (null == expapplCDetail) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail") });
		}
		String eventAllowanceQuotaCode = expapplCDetail.getEventAllowanceQuotaCode();
		if (StringUtils.isBlank(eventAllowanceQuotaCode)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplCDetail_EventAllowanceQuotaCode") });
		}
		List<EventAllowanceQuota> eaqList = facade.getEventAllowanceQuotaService().findForCheckCashGiftAllowance(eventAllowanceQuotaCode);
		if (CollectionUtils.isEmpty(eaqList)) {
			throw new ExpRuntimeException(ErrorCode.C10375, new String[] { eventAllowanceQuotaCode });
		}
		Entry en = null;
		Boolean has61130223 = false;
		for (Entry en2 : eac.getEntryGroup().getEntries()) {
			if (AccTitleCode.GIFT_FUNERAL_MONEY.getCode().equals(en2.getAccTitle().getCode())) {
				has61130223 = true;
				en = en2;
			}
		}
		if (!has61130223) { // 若沒有61130223會計科目的分錄
			throw new ExpRuntimeException(ErrorCode.C10374);
		}
		if (CollectionUtils.isNotEmpty(eaqList)) {
			if (en.getAmt().compareTo(eaqList.get(0).getPerAllowanceAmt()) > 0)
				throw new ExpRuntimeException(ErrorCode.C10373, new String[] { eaqList.get(0).getPerAllowanceAmt().toString() });
		}
	}

	public BigDecimal caculateRealityAmount(ExpapplC expapplC) {
		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			return BigDecimal.ZERO;
		}
		BigDecimal realityAmount = BigDecimal.ZERO;

		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (null == entry.getEntryType()) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_entryType") });
			}
			if (EntryTypeCode.TYPE_2_3.getValue().equals(entry.getEntryType().getValue())) {
				realityAmount = realityAmount.add(entry.getAmt());
			}
		}

		return realityAmount;
	}

	public List<Entry> generateTelephoneFeeEntries(ExpapplC eac, String summary) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		if (eac.getListType() == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listType") });
		if (!ListTypeCode.TELEPHONE.getCode().equals(eac.getListType().getCode()))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ListType_ListTypeCode") });
		if (StringUtils.isBlank(eac.getListNo1()))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listNo1") });
		List<PhoneRoster> phoneRosterS = facade.getPhoneRosterService().findByListNo(eac.getListNo1());
		if (CollectionUtils.isEmpty(phoneRosterS))
			throw new ExpRuntimeException(ErrorCode.C10226, new String[] { eac.getListNo1() });
		List<Entry> entries = new ArrayList<Entry>();
		Entry en = null;
		EntryType debitET = facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_D);
		AccTitle accTitle = facade.getAccTitleService().findByCode(AccTitleCode.TELEPHONE_EXP.getCode());
		PhoneInfo phoneInfo = null;
		// 取得「電話費名冊.電話費明細」List，並依每一筆「電話費明細」檢查是否有值
		for (PhoneRoster pr : phoneRosterS) {
			if (CollectionUtils.isEmpty(pr.getPhoneFeeDetails())) {
				// 2009/12/29，修正fetch不到資料問題
				List<PhoneFeeDetail> pfdList = facade.getPhoneFeeDetailService().findForPhoneFeeImportMaintain(pr);
				if (CollectionUtils.isNotEmpty(pfdList))
					pr.setPhoneFeeDetails(pfdList);
				else
					throw new ExpRuntimeException(ErrorCode.C10227, new String[] { pr.getListNo() });
			}
			boolean sameCost = false;
			for (PhoneFeeDetail pfd : pr.getPhoneFeeDetails()) {
				// 以「電話費明細」查出對應的「話機基本資料」
				phoneInfo = facade.getPhoneInfoService().findByPhoneDistCodeAndPhoneNumber(pfd.getPhoneDistCode(), pfd.getPhoneNumber());
				if (phoneInfo == null)
					throw new ExpRuntimeException(ErrorCode.C10228, new String[] { pfd.getPhoneNumber() });
				sameCost = false;
				// 相同成本單位計同一比會計科目資料
				if (en != null && en.getUuid() != null && CollectionUtils.isNotEmpty(entries)) {
					for (Entry en2 : entries) {
						if (en2.getEntryType().equals(en.getEntryType())) {
							String costCode = phoneInfo.getDepartment().getCode() != null ? phoneInfo.getDepartment().getCode() : "";
							String costName = phoneInfo.getDepartment().getName() != null ? phoneInfo.getDepartment().getName() : "";
							if (costCode.equals(en2.getCostUnitCode()) && costName.equals(en2.getCostUnitName())) {
								en2.setAmt(en2.getAmt().add(pfd.getTotalAmt()));
								sameCost = true;
								break;
							}
						}
					}
				}
				if (sameCost)
					continue;
				// genEntry 依每一筆「電話費明細」及其對應的「話機基本資料」，產生一筆借方的費用科目分錄
				en = new Entry();
				en.setAccTitle(accTitle);
				en.setIndustryCode(accTitle.getIncomeBiz());
				en.setEntryType(debitET);
				en.setCostUnitCode(pfd.getCostUnitId());
				Department department = facade.getDepartmentService().findByCode(pfd.getCostUnitId());
				// 找不到單位時都出錯誤
				if (null == department) {
					// 找不到{成本單位代號}為{pfd.getCostUnitId}的資料
					throw new ExpRuntimeException(ErrorCode.A20008, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_costUnitCode"), pfd.getCostUnitId() });
				}
				en.setCostUnitName(department.getName());
				en.setAmt(pfd.getTotalAmt());
				en.setEntryGroup(eac.getEntryGroup());
				en.setSummary(summary);
				entries.add(en);
			}
		}
		// 若「費用申請單.是否扣繳印花稅」=true，產生印花稅貸方科目
		if (eac.isWithholdStamp()) {
			Entry stampEn = facade.getEntryService().generateWithholdStampEntry(eac, accTitle);
			if (stampEn != null)
				entries.add(stampEn);
		}
		// 若「費用申請單.是否扣繳進項稅」=true，產生進項稅借方科目
		if (eac.isWithholdIncome()) {
			Entry wholdEn = facade.getEntryService().geterateWithholdIncome(eac, accTitle);
			if (wholdEn != null)
				entries.add(wholdEn);
		}
		// 將分錄放回分錄群組，和費用申請單
		eac.getEntryGroup().setEntries(entries);
		return entries;
	}

	public void checkPersonalMonthApplyQuota(ExpapplC eac) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });

		// // RE201300775_悠遊卡 modify by michael in 2013/10/15 start
		User applUser = facade.getUserService().findByCode(eac.getApplyUserInfo().getUserId());
		if (applUser == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo1") });
		}
		// // RE201300775_悠遊卡 modify by michael in 2013/10/15 end

		String mustProcessCode = ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode() + "&" + ExpItemCode.OTHER_TRAFFIC_EXP.getCode() + ExpItemCode.TELEPHONE_EXP.getCode() + "&" + ExpItemCode.PARKING_EXP.getCode();
		// 2009/12/7,”費用申請單.費用中分類”等於”M20 業務稽查費”時，且不為上面的項目，就回傳
		if (MiddleTypeCode.CODE_M20.getCode().equals(eac.getMiddleType().getCode())) {
			if (mustProcessCode.indexOf(eac.getExpItem().getCode()) == -1) {
				return; // 不是上面的費用項目，就回到原程式
			}
		}
		// RE201300775_悠遊卡 modify by michael in 2013/10/15 start
		// RE201300775_限額檔維護 modify by michael in 2013/11/19 start
		else if (ExpItemCode.OTHER_TRAFFIC_EXP.getCode().equals(eac.getExpItem().getCode())) {
			// RE201300775_限額檔維護 modify by michael in 2013/11/19 end
			// defect4968_E10核銷問題 CU3178 2018/2/1 START
			// 檢查其position是否為區部內務
			if (applUser.getPosition() == null || !PositionCode.CODE_SA5102.getCode().equals(applUser.getPosition().getCode())) {
				return;
			}
			// defect4968_E10核銷問題 CU3178 2018/2/1 END
		}
		// RE201300775_悠遊卡 modify by michael in 2013/10/15 end

		// 否則，”費用申請單.費用項目”不包含於以下的項目時，回傳至主程式。 (不用檢查申請限額)
		// RE201403569_C1.5.1有輸入專案代號時汽機車燃料費不檔限額 CU3178 2014/12/02 START
		// 若會計科目為汽機車燃料費時 若C1.5.1有輸入專案代號不檢核限額
		// else if( !
		// ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode().equals(eac.getExpItem().getCode())){
		else if (!ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode().equals(eac.getExpItem().getCode()) || (!StringUtils.isEmpty(eac.getProjectCode()) && (eac.getMiddleType().getCode().equals("J00")) || eac.getMiddleType().getCode().equals("E10"))) {
			// RE201403569_C1.5.1有輸入專案代號時汽機車燃料費不檔限額 CU3178 2014/12/02 END
			return;
		}
		// 2009/11/05,新增需求：檢核該申請人之「申請限額」是否啟用(#162)
		facade.getApplQuotaService().checkApplQuotaByYearAndUserCode(eac.getExpYears().substring(0, 4), eac.getApplyUserInfo().getUserId(), eac.getMiddleType().getCode());
		// RE201300775_悠遊卡 modify by michael in 2013/10/15 start
		// User applUser =
		// facade.getUserService().findByCode(eac.getApplyUserInfo().getUserId());
		// if (applUser == null)
		// throw new ExpRuntimeException(ErrorCode.A20002, new String[] {
		// MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo1")
		// });
		// 如果是特簽人員，就直接回傳
		if (facade.getApplQuotaService().isSpecialPerson(applUser))
			return;
		// 以費用年月查出「限額項目」中，申請人的「限額明細」資料：應該只會有一筆資料
		QuotaDetail qd = facade.getQuotaDetailService().findByPersonApplyQuota(eac);
		String acctCode = "";
		if (CollectionUtils.isNotEmpty(eac.getExpItem().getAccTitles()))
			acctCode = eac.getExpItem().getAccTitles().get(0).getCode();
		if (qd == null)
			throw new ExpRuntimeException(ErrorCode.C10232);
		// 查出該費用項目，已申請的年月限額
		String expNo = eac.getExpApplNo() != null ? eac.getExpApplNo() : "";
		BigDecimal appliedTotalAmt = getAppliedTotalAmount(MiddleTypeCode.getByValue(eac.getMiddleType()), eac.getApplyUserInfo().getUserId(), acctCode, eac.getExpYears(), expNo);
		// 這一次申請單的限額
		BigDecimal thisTimeApplyAmt = BigDecimal.ZERO;
		for (Entry en : eac.getEntryGroup().getEntries()) {
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				thisTimeApplyAmt = en.getAmt().add(thisTimeApplyAmt);
			}
		}
		// 比較限額明細裡的限額，是否還可以申請
		if (qd.getPerQuotaAmt().compareTo(appliedTotalAmt.add(thisTimeApplyAmt)) < 0) {
			// 2009/12/7,錯誤訊息要傳入年月和項目
			throw new ExpRuntimeException(ErrorCode.C10233, new String[] { eac.getExpYears(), eac.getExpItem().getCode() });
		}
	}

	public void checkPersonalYearApplyQuota(ExpapplC eac) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		String mustProcessCode = ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode();
		if (mustProcessCode.indexOf(eac.getExpItem().getCode()) == -1) {
			return; // 不是上面的費用項目，就回到原程式
		}
		// 2009/11/05,新增需求：檢核該申請人之「申請限額」是否啟用(#162)
		facade.getApplQuotaService().checkApplQuotaByYearAndUserCode(eac.getExpYears().substring(0, 4), eac.getApplyUserInfo().getUserId(), eac.getMiddleType().getCode());
		User applUser = facade.getUserService().findByCode(eac.getApplyUserInfo().getUserId());
		if (applUser == null)
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo1") });
		// 如果是特簽人員，就直接回傳
		else if (facade.getApplQuotaService().isSpecialPerson(applUser))
			return;
		// 以費用年月的年度查出申請人的「申請限額」資料
		List<ApplQuota> applQuotas = facade.getApplQuotaService().findByYearAndUserCodeAndMiddleTypeAndExpItem(eac.getExpYears().substring(0, 4), eac.getApplyUserInfo().getUserId(), eac.getMiddleType().getCode(), eac.getExpItem().getCode());
		// 查無該userCode的申請限額資料，則拋錯
		if (CollectionUtils.isEmpty(applQuotas))
			throw new ExpRuntimeException(ErrorCode.C10232);
		ApplQuota userQuota = applQuotas.get(0);
		String acctCode = "";
		if (CollectionUtils.isNotEmpty(eac.getExpItem().getAccTitles()))
			acctCode = eac.getExpItem().getAccTitles().get(0).getCode();
		// 查出該費用項目，已申請的年月限額
		String expNo = eac.getExpApplNo() != null ? eac.getExpApplNo() : "";
		BigDecimal appliedTotalAmt = getAppliedTotalAmount(MiddleTypeCode.getByValue(eac.getMiddleType()), eac.getApplyUserInfo().getUserId(), acctCode, eac.getExpYears(), expNo);
		// 這一次申請單的限額
		BigDecimal thisTimeApplyAmt = BigDecimal.ZERO;
		for (Entry en : eac.getEntryGroup().getEntries()) {
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				thisTimeApplyAmt = en.getAmt().add(thisTimeApplyAmt);
			}
		}
		// 比較申請限額裡的年度限額，是否還可以申請
		if (userQuota.getYyQuota().compareTo(appliedTotalAmt.add(thisTimeApplyAmt)) < 0) {
			// 2009/12/7,錯誤訊息要傳入年月和項目
			throw new ExpRuntimeException(ErrorCode.C10233, new String[] { eac.getExpYears(), eac.getExpItem().getCode() });
		}
	}

	public BigDecimal getAppliedTotalAmount(MiddleTypeCode middleTypeCode, String userId, String accTitleCode, String expYearMonth, String expNo) {
		if (middleTypeCode == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_middleType") });
		if (StringUtils.isBlank(userId))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_code") });
		if (StringUtils.isBlank(accTitleCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_code") });
		if (StringUtils.isBlank(expYearMonth))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expYears") });
		return getDao().getAppliedTotalAmount(middleTypeCode, userId, accTitleCode, expYearMonth, expNo);
	}

	// 2009/12/7,CR取消會計科目的條件
	public BigDecimal getAppliedTotalAmount(String middleTypeCode, String userId, String expYear) {
		if (StringUtils.isBlank(middleTypeCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_middleType") });
		if (StringUtils.isBlank(userId))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_code") });
		// if(StringUtils.isBlank(accTitleCode))throw new
		// ExpRuntimeException(ErrorCode.A10007,
		// new
		// String[]{getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_code")});
		if (StringUtils.isBlank(expYear))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expYears") });
		return getDao().getAppliedTotalAmount(middleTypeCode, userId, expYear);
	}

	/**
	 * 2010/09/06 修改檢核條件順序,部分檢核從原本的公共檢核移置"若傳入參數”檢核方式”等於1，取得年月限額"判斷中
	 */
	public int modifyInvoiceAmtByPersonalApplyQuota(ExpapplC eac, AccTitle accTitle, String checkType) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		if (StringUtils.isBlank(checkType))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_checkType") });
		// 必須是以下費用項目
		String mustProcessCode = ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode() + "&" + ExpItemCode.OTHER_TRAFFIC_EXP.getCode() + ExpItemCode.TELEPHONE_EXP.getCode() + "&" + ExpItemCode.PARKING_EXP.getCode();

		User applUser = facade.getUserService().findByCode(eac.getApplyUserInfo().getUserId());
		// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
		// 若中分類為C00、A30則不需檢核特簽人員
		if (!(MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) || MiddleTypeCode.CODE_A30.getCode().equals(eac.getMiddleType().getCode()))) {
			if (applUser == null)
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo1") });
			// 如果是特簽人員，就直接回傳
			else if (facade.getApplQuotaService().isSpecialPerson(applUser))
				return 0;
		}
		// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 END
		// 要更新分錄資料裡的金額、和憑證金額(含)的金額
		BigDecimal canApplAmt = BigDecimal.ZERO;
		BigDecimal appliedAmt = BigDecimal.ZERO;

		// 若傳入參數”檢核方式”等於0，取得年度限額: 以費用年月的年度查出「申請限額」中，申請人的「申請限額.年度金額」資料
		if ("0".equals(checkType)) {
			/*
			 * List<ApplQuota> applQuotas = facade.getApplQuotaService().
			 * findByYearAndUserCodeAndMiddleTypeAndExpItem
			 * (eac.getExpYears().substring(0, 4),
			 * eac.getApplyUserInfo().getUserId(),
			 * eac.getMiddleType().getCode(), eac.getExpItem().getCode() );
			 */

			// 2010/9/20 修改:協理車(M)及非經理切結車(J)專用。
			// 限額檔一人只能建置一筆固定金額,固協理車及非經理切結車一律只能以C00檢核;
			// 但付費對象為廠商時,兩者實際存檔的費用中分類是A30
			List<ApplQuota> applQuotas = facade.getApplQuotaService().findByYearAndUserCodeAndMiddleTypeAndExpItem(eac.getExpYears().substring(0, 4), eac.getApplyUserInfo().getUserId(), "C00", eac.getExpItem().getCode());

			// 查無該userCode的申請限額資料，則拋錯
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
			if (CollectionUtils.isEmpty(applQuotas)) {
				// 若查無限額中分類為公務車，付款對象為:個人、單位、廠商，付款方式為:開票、匯款、沖轉暫付、沖轉預付則跳過檢核。
				if ((MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) || MiddleTypeCode.CODE_A30.getCode().equals(eac.getMiddleType().getCode())) && (PaymentTargetCode.PERSONAL.getCode().equals(eac.getPaymentTarget().getCode()) || PaymentTargetCode.BUSSINESS_REVIEW.getCode().equals(eac.getPaymentTarget().getCode()) || PaymentTargetCode.VENDOR.getCode().equals(eac.getPaymentTarget().getCode())) && (PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode()))) {
					return 0;
				} else {
					throw new ExpRuntimeException(ErrorCode.C10527);
				}
			}
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 END
			ApplQuota userQuota = applQuotas.get(0);
			// 2010/9/20 修改:協理車(M)及非經理切結車(J)專用。
			// appliedAmt = getAppliedTotalAmount(eac.getMiddleType().getCode(),
			// eac.getApplyUserInfo().getUserId(),
			// eac.getExpYears().substring(0,4));
			appliedAmt = getAppliedTotalAmount("C00", eac.getApplyUserInfo().getUserId(), eac.getExpYears().substring(0, 4)).add(getAppliedTotalAmount("A30", eac.getApplyUserInfo().getUserId(), eac.getExpYears().substring(0, 4)));
			canApplAmt = userQuota.getYyQuota().subtract(appliedAmt);
		}
		// 若傳入參數”檢核方式”等於1，取得年月限額: 以費用年月查出「限額項目」中，申請人的「限額明細.金額」資料
		else {

			// 若傳入參數”費用申請單.費用中分類”等於”M20 業務稽查費”時，”費用申請單.費用項目”不包含於以下的項目時，回傳至主程式。
			// (不用檢查申請限額)
			// Defect:原本放置於此method第五行,但因協理車、切結車不需要此判斷,故移置此處. 2010/9/6 文珊
			if (MiddleTypeCode.CODE_M20.getCode().equals(eac.getMiddleType().getCode())) {
				if (mustProcessCode.indexOf(eac.getExpItem().getCode()) == -1)
					return 0; // 不是上面的費用項目，就回到原程式
			}

			// RE201300775_悠遊卡 modify by michael in 2013/10/15 start
			// RE201300775_限額檔維護 modify by michael in 2013/11/19
			else if (ExpItemCode.OTHER_TRAFFIC_EXP.getCode().equals(eac.getExpItem().getCode())) {
				// RE201300775_限額檔維護 modify by michael in 2013/11/19
				// 檢查其position是否為區部內務
				if (!PositionCode.CODE_SA5102.getCode().equals(applUser.getPosition().getCode())) {
					return 0;
				}
			}
			// RE201300775_悠遊卡 modify by michael in 2013/10/15 end
			// 否則，”費用申請單.費用項目”不包含於以下的項目時，回傳至主程式。 (不用檢查申請限額)
			else if (!ExpItemCode.AUTOMOBILE_FUEL_EXP.getCode().equals(eac.getExpItem().getCode())) {
				return 0;
			}
			// 先取得該費用申請單的費用項目對應的會計科目，若找不到，就拋錯誤訊息
			if (CollectionUtils.isEmpty(eac.getExpItem().getAccTitles()) || (CollectionUtils.isNotEmpty(eac.getExpItem().getAccTitles()) && StringUtils.isBlank(eac.getExpItem().getAccTitles().get(0).getCode()))) {
				throw new ExpRuntimeException(ErrorCode.C10323, new String[] { eac.getExpItem().getName() });
			}

			// 以費用年月查出「限額項目」中，申請人的「限額明細」資料：應該只會有一筆資料
			QuotaDetail qd = facade.getQuotaDetailService().findByPersonApplyQuota(eac);
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
			if (qd == null) {
				// 若查無限額中分類為公務車，付款方式為沖轉預付、沖轉戰付，付款對象為個人不作檢核。
				if (MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) && PaymentTargetCode.PERSONAL.getCode().equals(eac.getPaymentTarget().getCode()) && (PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(eac.getPaymentType().getCode()))) {
					return 0;
				} else {
					throw new ExpRuntimeException(ErrorCode.C10527);
				}
			}
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 END
			// RE201300775 modify by michael in 2013/10/14 start
			// 查出該費用項目，已申請的年月限額
			String expNo = eac.getExpApplNo() != null ? eac.getExpApplNo() : "";

			appliedAmt = getAppliedTotalAmount(MiddleTypeCode.getByValue(eac.getMiddleType()), eac.getApplyUserInfo().getUserId(), eac.getExpItem().getAccTitles().get(0).getCode(), eac.getExpYears(), expNo);// getAppliedTotalAmount(eac.getApplyUserInfo().getUserId(),
			// eac.getExpYears());
			// RE201300775 modify by michael in 2013/10/14 end
			canApplAmt = qd.getPerQuotaAmt().subtract(appliedAmt);
		}
		// 2009/12/24, 當可申請的餘額小於or等於'零'的時候，拋Exception，不要產生分錄資料了
		// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
		if (canApplAmt.compareTo(BigDecimal.ZERO) <= 0) {
			// 若中分類為C00公務車顯示警示訊息，程式仍可執行
			if (MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) && PaymentTargetCode.PERSONAL.getCode().equals(eac.getPaymentTarget().getCode()) && (PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(eac.getPaymentType().getCode()))) {
				if ("0".equals(checkType)) {
					MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
				} else {
					MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears(), eac.getExpItem().getName(), canApplAmt.toString() });
				}
				return 0;
			} else {
				if ("0".equals(checkType)) {
					throw new ExpRuntimeException(ErrorCode.C10324, new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
				} else {
					throw new ExpRuntimeException(ErrorCode.C10324, new String[] { eac.getExpYears(), eac.getExpItem().getName(), canApplAmt.toString() });
				}
			}
		}
		// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 END
		// 若”可申請的餘額” 大於
		// 傳入參數”費用申請單.分錄群組.分錄”(分錄.會計科目=指定費用項目對應的會計科目，且分錄借貸別=借方)，則不處理回傳至主程式
		for (Entry en : eac.getEntryGroup().getEntries()) {
			if (eac.getExpItem().getAccTitles().get(0).getCode().equals(en.getAccTitle().getCode()) && EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (canApplAmt.compareTo(en.getAmt()) >= 0) {
				}
				// 並設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
				else
					en.setAmt(canApplAmt);
			}
		}
		if (canApplAmt.compareTo(eac.getInvoiceAmt()) >= 0)
			return 0;
		// 否則顯示《費用年月、費用項目申請金額餘額不足!》訊息(但程式可繼續執行)，並設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
		else {
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
			if (!(MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) && PaymentTargetCode.PERSONAL.getCode().equals(eac.getPaymentTarget().getCode()) && (PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(eac.getPaymentType().getCode())))) {
				eac.setInvoiceAmt(canApplAmt);
				eac.setInvoiceTaxAmt(BigDecimal.ZERO);
				eac.setInvoiceNoneTaxAmt(BigDecimal.ZERO);
				if (eac.isWithholdIncome()) {
					// 重新計算憑證金額(稅)(未) 2010/02/24 By Eustace
					BigDecimal invoiceTaxAmt = facade.getAccTitleService().calculateIncomeAmt(accTitle.getCode(), true, canApplAmt);// 進項稅額(incomeAmt)
																																	// 憑證金額(稅invoiceTaxAmt)
					BigDecimal invoiceNoneTaxAmt = canApplAmt.subtract(invoiceTaxAmt);// 憑證金額(未)
					eac.setInvoiceNoneTaxAmt(invoiceNoneTaxAmt);
					eac.setInvoiceTaxAmt(invoiceTaxAmt);
				}
			}
			if ("0".equals(checkType)) {
				MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
			} else {
				MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears(), eac.getExpItem().getName(), canApplAmt.toString() });
			}
			// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 END
			return -1;
		}
	}

	public ExpapplC findByExpApplNoFetchData(String expApplNo) {
		if (StringUtils.isBlank(expApplNo)) {
			return null;
		}
		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("expApplNo", expApplNo);
		ExpapplC eac = getDao().findByCriteriaMapReturnUnique(criteriaMap);
		if (eac.getEntryGroup() == null) {
			EntryGroup entryGroup = facade.getEntryGroupService().findByExpapplCNo(eac);
			if (entryGroup != null)
				eac.setEntryGroup(entryGroup);
		}
		return eac;
	}

	// RE201300775 add by michael in 2013/04/15 start
	/**
	 * 依費用明細的費用項目取得已申請金額
	 * 
	 * 
	 * @param item
	 * @param details
	 * @return
	 */
	public Map<String, BigDecimal> getAppliedExpItemAmountByQuotaDetails(QuotaItem item, List<QuotaDetail> details) {

		ApplQuota applQuota = item.getApplQuota();

		String userId = applQuota.getUser().getCode();

		String expYear = applQuota.getYear();

		MiddleType middleType = applQuota.getMiddleType();

		if (StringUtils.isBlank(userId)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_userId") });
		}

		if (StringUtils.isBlank(expYear)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_ApplQuotaMaintainManagedBean_expYear") });
		}

		String expYearMonth = ""; // 要查詢的年月
		String middleTypeCode = middleType != null ? middleType.getCode() : "";
		Map<String, BigDecimal> result = new HashMap<String, BigDecimal>(); // 要回傳的Map，裡面以費用年月當Key，存入該月份已申請金額
		BigDecimal totalAppliedAmt = BigDecimal.ZERO; // 已累積的額度
		BigDecimal expYearMonthAmt = BigDecimal.ZERO; // 該月份的金額
		for (int i = 1; i <= 13; i++) {
			if (i < 10) {
				expYearMonth = expYear + "0" + i;
			} else {
				expYearMonth = expYear + i;
			}

			String expItemCode = getExpItemCode(item, expYearMonth, details);

			expYearMonthAmt = getDao().getAppliedExpItemAmountByYearMonth(userId, expYearMonth, middleTypeCode, expItemCode);
			if (i < 13) { //
				result.put(expYearMonth, expYearMonthAmt);
				totalAppliedAmt = expYearMonthAmt.add(totalAppliedAmt);
			} else {
				result.put(expYearMonth, totalAppliedAmt);
			}
		}
		return result;

	}

	/**
	 * 從details取得某月份的費用項目
	 * 
	 * @param yearMonth
	 * @param details
	 * @return
	 */
	private String getExpItemCode(QuotaItem defaultItem, String yearMonth, List<QuotaDetail> details) {

		String result = null;

		for (QuotaDetail detail : details) {

			if (yearMonth.equals(detail.getYearMonth())) {
				result = detail.getQuotaItem().getExpItem().getCode();
				break;
			}
		}

		if (result == null) {
			result = defaultItem.getExpItem().getCode();
		}
		return result;
	}

	/**
	 * 取得QuotaDetail的消費金額
	 * 
	 * @param detail
	 * @return
	 */
	public BigDecimal getAppliedExpItemAmountByQuotaDetail(QuotaDetail detail) {

		QuotaItem quotaItem = detail.getQuotaItem();

		ApplQuota applQuota = quotaItem.getApplQuota();

		return getDao().getAppliedExpItemAmountByYearMonth(applQuota.getUser().getCode(), detail.getYearMonth(), applQuota.getMiddleType().getCode(), quotaItem.getExpItem().getCode());
	}

	// RE201300775 add by michael in 2013/04/15 end

	public Map<String, BigDecimal> getAppliedFuelAmountByYearAndUserId(String userId, String expYear, MiddleType middleType) {
		if (StringUtils.isBlank(userId)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_userId") });
		}
		if (StringUtils.isBlank(expYear)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_ApplQuotaMaintainManagedBean_expYear") });
		}
		String expYearMonth = ""; // 要查詢的年月
		String middleTypeCode = middleType != null ? middleType.getCode() : "";
		Map<String, BigDecimal> result = new HashMap<String, BigDecimal>(); // 要回傳的Map，裡面以費用年月當Key，存入該月份已申請金額
		BigDecimal totalAppliedAmt = BigDecimal.ZERO; // 已累積的額度
		BigDecimal expYearMonthAmt = BigDecimal.ZERO; // 該月份的金額
		for (int i = 1; i <= 13; i++) {
			if (i < 10) {
				expYearMonth = expYear + "0" + i;
			} else {
				expYearMonth = expYear + i;
			}
			expYearMonthAmt = getDao().getAppliedFuelAmountByYearMonth(userId, expYearMonth, middleTypeCode);
			if (i < 13) { //
				result.put(expYearMonth, expYearMonthAmt);
				totalAppliedAmt = expYearMonthAmt.add(totalAppliedAmt);
			} else {
				result.put(expYearMonth, totalAppliedAmt);
			}
		}
		return result;
	}

	public void handleCaseW(ExpapplC expapplC) {
		/*
		 * 1. 檢查W件是否開啟。 If 「W件申請控管.是否開放」= false, 則丟出帶有訊息《W件未開放申請!》的ExpException。
		 * 2. 計算部門提列的應付費用總額: 查詢條件 1. 「部門提列應付費用申請單.提列年度」=傳入參數「費用申請單.費用年月」前4碼(西元年)
		 * 2. 「部門提列應付費用申請單.部門提列應付費用明細.成本單位代號」=傳入參數第一筆借方的「費用申請單.分錄群組.分錄.成本單位代號」
		 * 資料篩選 1.「部門提列應付費用申請單.提列申請單狀態」=送審 JOIN 條件 1. Inner Join 部門提列應付費用申請單
		 * 3.計算已申請的W件費用申請單申請總額:查詢費用申請單所有資料來作計算 可直接於分錄的Service加新method，來作計算 查詢條件
		 * 1. 「費用申請單.費用年月」前四碼=傳入參數「費用申請單.費用年月」前4碼(西元年) 2.
		 * 「費用申請單.分錄群組.分錄.成本單位代號」=傳入參數第一筆借方的「費用申請單.分錄群組.分錄.成本單位代號」 資料篩選 1.
		 * 「費用申請單.成本別」=”W” 2. 「費用申請單.分錄群組.分錄.科目借貸別」=借方 3. 「費用申請單.申請單狀態」不等於”刪除”
		 * JOIN 條件 1. Inner Join費用申請單 4. 計算W件超支金額:
		 * W件超支金額=已申請的W件費用申請單申請總額-傳入參數中，"借方"的「費用申請單.分錄群組.分錄.金額」-部門提列的應付費用總額 5.
		 * 若計算出的W件超支金額>0，表示已超支。附加”W超支金額”(金額為計算出的超支金額)到傳入的「費用申請單.系統摘要」 6.
		 * 將傳入的「費用申請單」相關聯的分錄資料中，設定所有的「分錄.成本代號」=”W”
		 */
		// 費用申請單的成本別必須為'W'才執行管控
		if ("W".equalsIgnoreCase(expapplC.getCostTypeCode())) {
			// modifly 2009/8/14, By Eustace
			expapplC.setCostTypeCode("W");
			List<CaseWCtrl> wCtrlList = getFacade().getCaseWCtrlService().findAll();
			if (CollectionUtils.isNotEmpty(wCtrlList)) {
				CaseWCtrl cwc = (CaseWCtrl) wCtrlList.get(0);
				if (!cwc.isOpenFlag()) {
					throw new ExpRuntimeException(ErrorCode.C10021);// W件未開放申請!
				}
			} else if (CollectionUtils.isEmpty(wCtrlList)) {
				throw new ExpRuntimeException(ErrorCode.C10021);// W件未開放申請!
			}
			// 當分錄是空的，直接結束，避免錯誤
			if (null != expapplC.getEntryGroup() && !CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
				// 2.計算部門提列的應付費用總額
				BigDecimal depAccExpSum = facade.getDepAccruedExpensesDetailService().sumEstimationAmtByExpapplC(expapplC);
				// 3.計算已申請的W件費用申請單申請總額
				BigDecimal entryCaseWExpSum = facade.getEntryService().entryCaseWExpSum(expapplC);
				// 4.計算W件超支金額，只要抓"借方"的來計算
				List<Entry> enList = expapplC.getEntryGroup().getEntries();
				BigDecimal entrySumAmt = BigDecimal.ZERO;
				for (Entry entry : enList) {
					if (entry.getAmt() != null)
						continue;
					if ("D".equals(entry.getEntryType().getValue())) {
						entrySumAmt = entrySumAmt.add(entry.getAmt());
					}
				}
				if (depAccExpSum == null) {
					depAccExpSum = BigDecimal.ZERO;
				}
				if (entryCaseWExpSum == null) {
					entryCaseWExpSum = BigDecimal.ZERO;
				}
				BigDecimal caseWOverspend = entryCaseWExpSum.add(entrySumAmt).subtract(depAccExpSum);
				// 5.若計算出的W件超支金額>0，表示已超支
				if (caseWOverspend != null && caseWOverspend.max(BigDecimal.ZERO).equals(caseWOverspend)) {
					// 超支金額要用properities設定
					try {
						// modify By Eustace, 2009/07/31,
						// 因為舊有的在TestCase會出錯,所以改成此方式
						String overspendMessage = MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_logic_CaseWCtrlService_caseWOverspend");
						expapplC.setSystemNotes(overspendMessage.trim() + "：" + StringUtils.getMoneyStr(caseWOverspend.setScale(0).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// 6.設定傳入的「費用申請單」相關聯的分錄資料的「分錄.成本代號」=”W”
				List<Entry> entryList = expapplC.getEntryGroup().getEntries();
				Entry en;
				for (int i = 0; i < entryList.size(); i++) {
					en = (Entry) entryList.get(i);
					en.setCostCode("W");
				}
			}
		} else if (StringUtils.isBlank(expapplC.getCostTypeCode())) {
			// 2009/08/13,TIM,成本別只能輸入"空白"，或是"W"
			expapplC.setCostTypeCode(" ");

			// RE201701547_ 成本別 修改 EC0416 2017/4/12 start
			if (expapplC.getEntryGroup() != null && expapplC.getEntryGroup().getEntries() != null) {
				List<Entry> entryList = expapplC.getEntryGroup().getEntries();
				Entry en;
				for (int i = 0; i < entryList.size(); i++) {
					en = (Entry) entryList.get(i);
					en.setCostCode(" ");
				}
			}
			// RE201701547_ 成本別 修改 EC0416 2017/4/12 END
		} else {
			// 2009/08/13, Eustace Heieh ,成本別亂輸入就丟出 "傳入的參數成本別錯誤"
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_costTypeCode") });
		}
	}

	public void checkInvoiceNumberIsMustType(ExpapplC eac) {
		// If 「費用申請單.是否扣繳進項稅」=true
		// 且「 費用申請單.憑證類別.格式代號」為「21、23 、25、26、27」其中任一個值。要檢查統一編號欄位必須有值
		String ptCode = ProofTypeCode.PROOF_TYPE_C_21.getFormatCode() + "&" + ProofTypeCode.PROOF_TYPE_C_23.getFormatCode() + "&" + ProofTypeCode.PROOF_TYPE_C_25.getFormatCode() + "&26&27";
		if (eac.isWithholdIncome() && ptCode.indexOf(eac.getProofType().getFormatCode()) != -1) {
			if (StringUtils.isBlank(eac.getInvoiceNo())) {
				throw new ExpRuntimeException(ErrorCode.A10023);
			}
		}
	}

	public void checkDepartmentApplyQuota(ExpapplC expapplC, Department department) {
		List<DepApplQuotaDetail> dqDetails = facade.getDepApplQuotaDetailService().getDepartmentApplyQuota(expapplC, department);
		if (CollectionUtils.isEmpty(dqDetails))
			throw new ExpRuntimeException(ErrorCode.C10269);
		BigDecimal dqAmt = BigDecimal.ZERO;
		// 該單位的每月限額
		for (DepApplQuotaDetail dqD : dqDetails) {
			dqAmt = dqAmt.add(dqD.getPerMonthQuotaAmt());
		}
		// 如果是修改申請單的情況下，要避開自己這比"已"申請的金額
		String expNo = "";
		if (StringUtils.isNotBlank(expapplC.getExpApplNo()))
			expNo = expapplC.getExpApplNo();
		// 該單位已申請的金額
		BigDecimal costUnitAppliedAmt = getCostUnitAppliedTotalAmount(department.getCode(), expapplC.getExpItem().getCode(), expapplC.getExpYears(), expNo);
		BigDecimal entriesTotalAmt = BigDecimal.ZERO;
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			// 計算此次申請的金額
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (en.getAccTitle().getExpItems().contains(expapplC.getExpItem())) {
					entriesTotalAmt = en.getAmt().add(entriesTotalAmt);
				}
			}
		}
		/*
		 * 若” 已申請的總額”+傳入參數”費用申請單.分錄群組.分錄.金額”(分錄.會計科目=指定費用項目對應的會計科目，且分錄借貸別=借方)
		 * 大於查回的「單位限額.金額」，throw ExpRuntimeException，顯示《該成本單位”費用項目名稱”已超支!》
		 */
		if (costUnitAppliedAmt.add(entriesTotalAmt).compareTo(dqAmt) > 0)
			throw new ExpRuntimeException(ErrorCode.C10268, new String[] { expapplC.getExpItem().getName() });
	}

	// RE201300147 modify by michael in 2013/06/25 start
	// 原checkDepartmentApplyYearQuota method是錯的,所以新增此method,改用這y
	/*
	 * 檢核年度限額 (RE201000731)
	 */
	public void checkDepartmentAppliedQuotaByYear(ExpapplC expapplC, Department department) {
		List<DepApplQuota> dqDetails = facade.getDepApplQuotaService().findByParams(expapplC.getExpYears().substring(0, 4), expapplC.getExpItem(), null, department.getCode());
		if (CollectionUtils.isEmpty(dqDetails))
			throw new ExpRuntimeException(ErrorCode.C10269);
		BigDecimal dqAmt = BigDecimal.ZERO;
		// 該單位的年度限額
		dqAmt = ((DepApplQuota) dqDetails.get(0)).getYearQuotaAmt();

		// 如果是修改申請單的情況下，要避開自己這比"已"申請的金額
		String expNo = "";
		if (StringUtils.isNotBlank(expapplC.getExpApplNo()))
			expNo = expapplC.getExpApplNo();
		// 該單位已申請的金額
		String expYear = expapplC.getExpYears().substring(0, 4);
		// RE201300147 modify by michael in 2013/07/01 start
		BigDecimal costUnitAppliedAmt = getCostUnitE10N10AppliedAmtByYear(department.getCode(), expapplC.getExpItem().getCode(), expYear, expNo);
		// RE201300147 modify by michael in 2013/07/01 end

		BigDecimal entriesTotalAmt = BigDecimal.ZERO;
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			// 計算此次申請的金額
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (en.getAccTitle().getExpItems().contains(expapplC.getExpItem())) {
					entriesTotalAmt = en.getAmt().add(entriesTotalAmt);
				}
			}
		}
		/*
		 * 若” 已申請的總額”+傳入參數”費用申請單.分錄群組.分錄.金額”(分錄.會計科目=指定費用項目對應的會計科目，且分錄借貸別=借方)
		 * 大於查回的「單位限額.金額」，throw ExpRuntimeException，顯示《該成本單位”費用項目名稱”已超支!》
		 */
		if (costUnitAppliedAmt.add(entriesTotalAmt).compareTo(dqAmt) > 0) {
			// RE201300147 modify by michael in 2013/06/28 start
			BigDecimal remainAmt = BigDecimal.ZERO;
			if (costUnitAppliedAmt.compareTo(dqAmt) < 0) {
				remainAmt = dqAmt.subtract(costUnitAppliedAmt);
			}

			// 設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
			expapplC.setInvoiceAmt(remainAmt);
			// RE201300147 modify by michael in 2013/06/28 end
			// 設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
			// expapplC.setInvoiceAmt(dqAmt.subtract(costUnitAppliedAmt));
			throw new ExpRuntimeException(ErrorCode.C10268, new String[] { expapplC.getExpItem().getName() });
		}
	}

	// RE201300147 modify by michael in 2013/06/25 end

	/*
	 * 檢核年度限額 (RE201000731)
	 */

	public void checkDepartmentApplyYearQuota(ExpapplC expapplC, Department department) {
		List<DepApplQuota> dqDetails = facade.getDepApplQuotaService().findByParams(expapplC.getExpYears().substring(0, 4), expapplC.getExpItem(), null, department.getCode());
		if (CollectionUtils.isEmpty(dqDetails))
			throw new ExpRuntimeException(ErrorCode.C10269);
		BigDecimal dqAmt = BigDecimal.ZERO;
		// 該單位的年度限額
		dqAmt = ((DepApplQuota) dqDetails.get(0)).getYearQuotaAmt();

		// 如果是修改申請單的情況下，要避開自己這比"已"申請的金額
		String expNo = "";
		if (StringUtils.isNotBlank(expapplC.getExpApplNo()))
			expNo = expapplC.getExpApplNo();
		// 該單位已申請的金額
		BigDecimal costUnitAppliedAmt = getCostUnitAppliedTotalAmount(department.getCode(), expapplC.getExpItem().getCode(), expapplC.getExpYears(), expNo);
		BigDecimal entriesTotalAmt = BigDecimal.ZERO;
		for (Entry en : expapplC.getEntryGroup().getEntries()) {
			// 計算此次申請的金額
			if (EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (en.getAccTitle().getExpItems().contains(expapplC.getExpItem())) {
					entriesTotalAmt = en.getAmt().add(entriesTotalAmt);
				}
			}
		}
		/*
		 * 若” 已申請的總額”+傳入參數”費用申請單.分錄群組.分錄.金額”(分錄.會計科目=指定費用項目對應的會計科目，且分錄借貸別=借方)
		 * 大於查回的「單位限額.金額」，throw ExpRuntimeException，顯示《該成本單位”費用項目名稱”已超支!》
		 */
		if (costUnitAppliedAmt.add(entriesTotalAmt).compareTo(dqAmt) > 0) {
			// 設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
			expapplC.setInvoiceAmt(dqAmt.subtract(costUnitAppliedAmt));
			throw new ExpRuntimeException(ErrorCode.C10268, new String[] { expapplC.getExpItem().getName() });
		}
	}

	// RE201300147 modify by michael in 2013/06/25 start
	// 因本次不改1.5.13,且getCostUnitAppliedTotalAmount是錯的,所以保留getCostUnitAppliedTotalAmount
	// method另外寫這一個
	@SuppressWarnings("unchecked")
	// RE201300147 modify by michael in 2013/07/01 start
	public BigDecimal getCostUnitE10N10AppliedAmtByYear(String departmentCode, String expItemCode, String expYear, String expNo) {
		// RE201300147 modify by michael in 2013/07/01 end

		if (StringUtils.isBlank(departmentCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_costUnitCode") });
		if (StringUtils.isBlank(expItemCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expItem") });
		if (StringUtils.isBlank(expYear))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expYears") });
		// 2009/12/24,IR#1618,要回傳查到的資料，不要再回傳null了

		// RE201300147 modify by michael in 2013/07/01 start
		List<MiddleTypeCode> middleTypeCodes = new ArrayList<MiddleType.MiddleTypeCode>();
		middleTypeCodes.add(MiddleTypeCode.CODE_E10);
		middleTypeCodes.add(MiddleTypeCode.CODE_N10);

		return getDao().getCostUnitAppliedAmtByYear(departmentCode, expItemCode, expYear, expNo, middleTypeCodes);
		// RE201300147 modify by michael in 2013/07/01 end
	}

	// RE201300147 modify by michael in 2013/06/25 end

	public BigDecimal getCostUnitAppliedTotalAmount(String departmentCode, String expItemCode, String expYearMonth, String expNo) {
		if (StringUtils.isBlank(departmentCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_costUnitCode") });
		if (StringUtils.isBlank(expItemCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expItem") });
		if (StringUtils.isBlank(expYearMonth))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expYears") });
		// 2009/12/24,IR#1618,要回傳查到的資料，不要再回傳null了
		return getDao().getCostUnitAppliedTotalAmount(departmentCode, expItemCode, expYearMonth, expNo);
	}

	public ExpapplC findExpapplCByListNoOnBonusAwardListType(String listNo1, String listNo2, String applUserDepCode) {
		return getDao().findExpapplCByListNoOnBonusAwardListType(listNo1, listNo2, applUserDepCode);
	}

	public void doConfirmApplied(List<String> expApplNoList, FunctionCode functionCode) {
		if (CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") + "List" });
		}
		Calendar sysDate = Calendar.getInstance();
		// 查出費用申請單
		List<ExpapplC> expapplCList = this.findByApplNo(expApplNoList);

		for (ExpapplC expapplC : expapplCList) {
			// 借貸是否平衡
			this.facade.getEntryGroupService().calcBalance(expapplC.getEntryGroup());

			if (!expapplC.getEntryGroup().isBalanced()) {
				// 顯示《帳務資料錯誤，借方與貸方不平衡》
				throw new ExpRuntimeException(ErrorCode.C10531, new String[] { expapplC.getExpApplNo() });
			}
		}

		// 「費用申請單」的狀態設為”確認申請” ，並儲存之
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.CONFIRM_APPLIED, SysTypeCode.C);

		for (ExpapplC expapplC : expapplCList) {
			expapplC.setApplState(applState);
			expapplC.setUpdateDate(sysDate);
			expapplC.setUpdateUser(getLoginUser());

			// 儲存申請單資料
			this.update(expapplC);

			// 記錄流程簽核歷程
			this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
		}

	}

	/**
	 * 取得登入的使用者
	 * 
	 * @return User
	 */
	private User getLoginUser() {
		return this.facade.getUserService().findByPK(((User) AAUtils.getLoggedInUser()).getId());
	}

	public List<Entry> doGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean boolean1) {
		return doGenerateApplyRosterEntries(expapplC, departmentCode, boolean1, null, null);
	}

	public List<Entry> doGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean isBegWithHold, String summary, ExpapplCDetail expapplCDetail) {
		if (null == expapplC || null == departmentCode) {
			return null;
		}

		// 1.If傳入參數”費用申請單.冊號類別”不等於獎金品冊號或”費用申請單.冊號1”為空值， throw
		// ExpRuntimeExceiption，顯示”傳入參數錯誤”
		if (null == expapplC.getListType() || !ListTypeCode.PREMIUM_AWARD.getCode().equals(expapplC.getListType().getCode()) || StringUtils.isBlank(expapplC.getListNo1())) {
			// 顯示”傳入參數錯誤”
			// 2009/11/05,IR單號794
			throw new ExpRuntimeException(ErrorCode.C10288, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterDetail_rosterNo") });
		}

		// 2.new 一個待回傳分錄的List
		List<Entry> entryList = new ArrayList<Entry>();

		// 借方費用科目1
		Entry expEntry1 = null;
		// 借方費用科目2
		Entry expEntry2 = null;

		// 3.以”費用申請單.冊號1”查詢「名冊」資料表，若查無資料，throw
		// ExpRuntimeExceiption，顯示《查無名冊資料，名冊單號: ”冊號”》
		RosterDetail rosterDetail1 = this.facade.getRosterDetailService().findByRosterNoFetchRelation(expapplC.getListNo1());
		if (null == rosterDetail1) {
			throw new ExpRuntimeException(ErrorCode.C10314, new String[] { expapplC.getListNo1() });
		}
		expEntry1 = doGenerateApplyRosterEntrie(expapplC, departmentCode, rosterDetail1, expapplC.getListNo1(), entryList, isBegWithHold, expapplCDetail);

		// 9.若”費用申請單.冊號2”不為空值，以費用申請單.冊號2為參數，繼續執行步驟3~8，以產生另一個借方費用科目及另一組貸方科目，並新增至暫存分錄List待回傳
		if (!StringUtils.isBlank(expapplC.getListNo2())) {
			RosterDetail rosterDetail2 = this.facade.getRosterDetailService().findByRosterNoFetchRelation(expapplC.getListNo2());
			expEntry2 = doGenerateApplyRosterEntrie(expapplC, departmentCode, rosterDetail2, expapplC.getListNo2(), entryList, isBegWithHold, expapplCDetail);
		}

		// 10.若產生二筆借方費用科目，且其會計科目相同時，將其分錄合併為一筆(金額相加)
		if (null != expEntry1 && null != expEntry2) {
			if (expEntry1.getAccTitle().getId().equals(expEntry2.getAccTitle().getId())) {
				expEntry1.setAmt(expEntry1.getAmt().add(expEntry2.getAmt()));
				expEntry1.setSummary(summary);
				entryList.add(expEntry1);
			} else {
				expEntry1.setSummary(summary);
				entryList.add(expEntry1);
				expEntry2.setSummary(summary);
				entryList.add(expEntry2);
			}

		} else {
			expEntry1.setSummary(summary);
			entryList.add(expEntry1);
		}

		// 11.若「費用申請單.是否扣繳印花稅」=true，產生印花稅貸方科目。
		if (expapplC.isWithholdStamp()) {
			BigDecimal withholdStamp = expapplC.getStampAmt();

			// withholdStamp =
			// this.facade.getAccTitleService().calculateStampAmt(rosterDetail1.getAccTitle().getCode(),
			// expapplC.getInvoiceAmt());

			Entry withholdStampEntry = new Entry();
			withholdStampEntry.setAccTitle(rosterDetail1.getAccTitle().getStampTax());
			withholdStampEntry.setAmt(withholdStamp);
			withholdStampEntry.setEntryType(this.getEntryTypeForC());

			// 印花稅金額必須大於0,才能將分入加入List
			if (null != withholdStampEntry.getAmt() && BigDecimal.ZERO.compareTo(withholdStampEntry.getAmt()) < 0) {
				entryList.add(withholdStampEntry);
			}
		}

		// 12.若「費用申請單.是否扣繳進項稅」=true，產生進項稅借方科目。
		/*
		 * 執行共用function《檢核費用申請單的進項稅相關欄位資訊》 計算進項稅邏輯，執行共用function《計算進項稅》(參考SDD總綱)
		 */
		generateWithholdIncome(expapplC, rosterDetail1.getAccTitle(), entryList);

		if (null == expapplC.getEntryGroup()) {
			expapplC.setEntryGroup(new EntryGroup());
		}

		for (Entry entry : entryList) {
			entry.setEntryGroup(expapplC.getEntryGroup());
		}

		expapplC.getEntryGroup().setEntries(entryList);
		// 13.回傳分錄的List
		return entryList;
	}

	public void doSortEntry(ExpapplC expapplC) {
		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			return;
		}

		expapplC.getEntryGroup().setEntries(facade.getEntryService().doSortByEntryType(expapplC.getEntryGroup().getEntries()));

	}

	public List<String> findByParams(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610) {
		// 要查詢的申請單狀態
		ApplStateCode applStateCode = null;

		// 要排除查詢的申請單狀態
		List<ApplStateCode> delApplStateCodeList = null;
		// 申請單狀態
		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			applStateCode = ApplStateCode.APPLIED;
		}

		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.TEMP;
		}

		if (ApplStateEnum.DELETE.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.DELETED;
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.APPLIED);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.TEMP);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}
		User loginUser = (User) AAUtils.getLoggedInUser();
		List<Department> loginUserDepList = null;
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());

		boolean isPowerGroup = true;
		// defect4951_增加群組權限條件 CU3178 2018/1/25 START
		// 為C1.6.10時，且不等於GroupCode的群組則存取設限
		if (is1610 && !(GroupCode.ADMIN.equals(groupCode) || GroupCode.AUDITOR_FIRST_VERIFY.equals(groupCode) || GroupCode.AUDITOR_REVIEW.equals(groupCode) || GroupCode.AUDITOR_TAX.equals(groupCode) || GroupCode.AUDITOR_GENERAL.equals(groupCode) || GroupCode.GAE_GENERAL.equals(groupCode) || GroupCode.HUMAN_RESOURCE.equals(groupCode) || GroupCode.LEARNING.equals(groupCode) || GroupCode.AUDITOR_PM_REVIEW.equals(groupCode))) {
			// defect4951_增加群組權限條件 CU3178 2018/1/25 END
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && isFindDepartmentCode) {
			// 不為C1.6.10時，且isFindDepartmentCode不等於true
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && !MiddleTypeCode.CODE_H10.equals(middleTypeCode)) {
			// 不為C1.6.10時，且中分類不能於H10
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		}

		List<String> departmentCodes = null;
		if (isFindDepartmentCode) {
			Department loginDep = facade.getUserService().getLoggedInUser().getDepartment();
			if (null == loginDep) {
				StringBuffer sb = new StringBuffer();
				sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_department"));
				sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User"));
				sb.append(":" + facade.getUserService().getLoggedInUser().getCode() + facade.getUserService().getLoggedInUser().getName());
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { sb.toString() });
			}
			departmentCodes = new ArrayList<String>();
			departmentCodes.add(loginDep.getCode());
			// 找出本處與分處的單位代號
			if (DepartmentLevelPropertyCode.C.getCode().equals(loginDep.getDepartmentLevelProperty().getCode())) {
				if (null != loginDep.getDepartmentCost()) {
					departmentCodes.add(loginDep.getDepartmentCost().getCode());
				} else {
					List<Department> list = facade.getDepartmentService().findDepartmentCostByCode(loginDep.getCode());
					if (!CollectionUtils.isEmpty(list)) {
						for (Department department : list) {
							departmentCodes.add(department.getCode());
						}
					}
				}
			}
		}

		// 找出費用申請單
		List<ExpapplC> dataList = getDao().findByParams(applStateCode, middleTypeCode, applyUserCode, createDateStart, createDateEnd, departmentCodes, delApplStateCodeList, loginUserDepList, loginUser, isPowerGroup);

		if (CollectionUtils.isEmpty(dataList)) {
			return null;
		}

		// 取出申請單號
		List<String> list = new ArrayList<String>();
		for (ExpapplC expapplC : dataList) {
			list.add(expapplC.getExpApplNo());
		}
		return list;
	}
	
	// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
	public List<String> findByParams2(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1604) {
		// 要查詢的申請單狀態
		ApplStateCode applStateCode = null;

		// 要排除查詢的申請單狀態
		List<ApplStateCode> delApplStateCodeList = null;
		// 申請單狀態
		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1604 == true) {
			applStateCode = ApplStateCode.APPLIED;
		}

		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1604 == false) {
			applStateCode = ApplStateCode.TEMP;
		}

		if (ApplStateEnum.DELETE.equals(applStateEnum) && is1604 == true) {
			applStateCode = ApplStateCode.DELETED;
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1604 == true) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.APPLIED);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1604 == false) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.TEMP);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}

		User loginUser = (User) AAUtils.getLoggedInUser();
		List<Department> loginUserDepList = null;
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());

		boolean isPowerGroup = true;
		
		// 為C1.6.4時，且不等於GroupCode的群組則存取設限
		if (is1604 && !(GroupCode.ADMIN.equals(groupCode) || GroupCode.AUDITOR_FIRST_VERIFY.equals(groupCode) || GroupCode.AUDITOR_REVIEW.equals(groupCode) || GroupCode.AUDITOR_TAX.equals(groupCode) || GroupCode.AUDITOR_GENERAL.equals(groupCode) || GroupCode.GAE_GENERAL.equals(groupCode) || GroupCode.HUMAN_RESOURCE.equals(groupCode) || GroupCode.LEARNING.equals(groupCode) || GroupCode.AUDITOR_PM_REVIEW.equals(groupCode))) {
		
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1604 && isFindDepartmentCode) {
			// 不為C1.6.4時，且isFindDepartmentCode不等於true
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1604 && !MiddleTypeCode.CODE_H10.equals(middleTypeCode)) {
			// 不為C1.6.10時，且中分類不能於H10
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		}

		List<String> departmentCodes = null;
		if (isFindDepartmentCode) {
			Department loginDep = facade.getUserService().getLoggedInUser().getDepartment();
			if (null == loginDep) {
				StringBuffer sb = new StringBuffer();
				sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_department"));
				sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User"));
				sb.append(":" + facade.getUserService().getLoggedInUser().getCode() + facade.getUserService().getLoggedInUser().getName());
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { sb.toString() });
			}
			departmentCodes = new ArrayList<String>();
			departmentCodes.add(loginDep.getCode());
			// 找出本處與分處的單位代號
			if (DepartmentLevelPropertyCode.C.getCode().equals(loginDep.getDepartmentLevelProperty().getCode())) {
				if (null != loginDep.getDepartmentCost()) {
					departmentCodes.add(loginDep.getDepartmentCost().getCode());
				} else {
					List<Department> list = facade.getDepartmentService().findDepartmentCostByCode(loginDep.getCode());
					if (!CollectionUtils.isEmpty(list)) {
						for (Department department : list) {
							departmentCodes.add(department.getCode());
						}
					}
				}
			}
		}

		// 找出費用申請單
		List<ExpapplC> dataList = getDao().findByParams(applStateCode, middleTypeCode, applyUserCode, createDateStart, createDateEnd, departmentCodes, delApplStateCodeList, loginUserDepList, loginUser, isPowerGroup);

		if (CollectionUtils.isEmpty(dataList)) {
			return null;
		}

		// 取出申請單號
		List<String> list = new ArrayList<String>();
		for (ExpapplC expapplC : dataList) {
			list.add(expapplC.getExpApplNo());
		}
		return list;
	}
	// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end

	public BigDecimal caculateShouldRemitAmount(ExpapplC expapplC) {
		BigDecimal amt = BigDecimal.ZERO;
		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			return amt;
		}

		// 計算應匯金額 = 費用合計-沖轉金額; 費用合計=借方總額 ;沖轉金額=貸方科目有銷帳碼的金額
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (StringUtils.equals(EntryTypeValueCode.D.getValue(), entry.getEntryType().getValue())) {
				if (null != entry.getAmt()) {
					amt = amt.add(entry.getAmt());
				}
			} else {
				// 銷帳碼不為空
				if (StringUtils.isNotBlank(entry.getCancelCode())) {
					// 沖轉金額
					amt = amt.subtract(entry.getAmt());
				}
			}

		}
		return amt;
	}

	public ExpapplC findByExpApplNo(String expApplNo) {
		if (StringUtils.isBlank(expApplNo)) {
			return null;
		}

		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("expApplNo", expApplNo);
		return getDao().findByCriteriaMapReturnUnique(criteriaMap);
	}

	public void generateDrawMoneyUnitByUserCode(String userCode, ExpapplC expapplC) {
		if (null == expapplC) {
			return;
		}
		expapplC.setDrawMoneyUnitName(null);
		expapplC.setDrawMoneyUnitCode(null);

		if (StringUtils.isBlank(userCode)) {
			return;
		}

		User user = facade.getUserService().findByCode(userCode);
		if (null == user) {
			return;
		}
		// 以「申請人員.匯款單位.單位代號」為預設值。若為空值則帶入「申請人員.所屬單位.單位代號」Defect:1244
		if (null != user.getRemitDepartment()) {
			expapplC.setDrawMoneyUnitName(user.getRemitDepartment().getName());
			expapplC.setDrawMoneyUnitCode(user.getRemitDepartment().getCode());
		} else {
			expapplC.setDrawMoneyUnitName(user.getDepartment().getName());
			expapplC.setDrawMoneyUnitCode(user.getDepartment().getCode());
		}
	}

	public BigDecimal findPrintRtnItemForVendor(List<String> expapplNos) {
		if (CollectionUtils.isEmpty(expapplNos))
			return BigDecimal.ZERO;
		return getDao().findPrintRtnItemForVendor(expapplNos);
	}

	public BigDecimal findPrintRtnItemForGeneralDep(List<String> expapplNos) {
		if (CollectionUtils.isEmpty(expapplNos))
			return BigDecimal.ZERO;
		return getDao().findPrintRtnItemForGeneralDep(expapplNos);
	}

	public BigDecimal findPrintRtnItemForSalDep(List<String> expapplNos) {
		if (CollectionUtils.isEmpty(expapplNos))
			return BigDecimal.ZERO;
		return getDao().findPrintRtnItemForSalDep(expapplNos);
	}

	public BigDecimal findPrintRtnItemForTotalPayAmt(List<String> expapplNos) {
		if (CollectionUtils.isEmpty(expapplNos))
			return BigDecimal.ZERO;
		return getDao().findPrintRtnItemForTotalPayAmt(expapplNos);
	}

	public void setExpItemByCode(ExpapplC expapplC, ExpItemCode expItemCode) {
		if (null == expapplC) {
			return;
		}

		if (null == expItemCode) {
			expapplC.setExpItem(null);
		} else {
			expapplC.setExpItem(facade.getExpItemService().findByExpItemCode(expItemCode.getCode()));
		}

	}

	public void setApplStateByCode(ExpapplC expapplC, ApplStateCode applStateCode) {
		if (null == expapplC) {
			return;
		}

		if (null == applStateCode) {
			expapplC.setApplState(null);
		} else {
			expapplC.setApplState(facade.getApplStateService().findByCodeFetchSysType(applStateCode, SysTypeCode.C));
		}

	}

	public void setExpTypeByCode(ExpapplC expapplC, ExpTypeCode expTypeCode) {
		if (null == expapplC) {
			return;
		}

		if (null == expTypeCode) {
			expapplC.setExpType(null);
		} else {
			expapplC.setExpType(facade.getExpTypeService().findExpTypeByExpTypeCode(expTypeCode));
		}

	}

	public void setPaymentTargetByCode(ExpapplC expapplC, PaymentTargetCode paymentTargetCode) {
		if (null == expapplC) {
			return;
		}

		if (null == paymentTargetCode) {
			expapplC.setPaymentTarget(null);
		} else {
			expapplC.setPaymentTarget(facade.getPaymentTargetService().findByCodeFetchSysType(SysTypeCode.C, paymentTargetCode));
		}

	}

	public void setPaymentTypeByCode(ExpapplC expapplC, PaymentTypeCode paymentTypeCode) {
		if (null == expapplC) {
			return;
		}

		if (null == paymentTypeCode) {
			expapplC.setPaymentType(null);
		} else {
			expapplC.setPaymentType(getPaymentTypeByCode(paymentTypeCode));
		}
	}

	public PaymentType getPaymentTypeByCode(PaymentTypeCode paymentTypeCode) {
		if (null == paymentTypeCode) {
			return null;
		}
		return facade.getPaymentTypeService().findByCodeFetchSysType(SysTypeCode.C, paymentTypeCode);
	}

	public void setMiddleTypeByCode(ExpapplC expapplC, MiddleTypeCode middleTypeCode) {
		if (null == expapplC) {
			return;
		}

		if (null == middleTypeCode) {
			expapplC.setMiddleType(null);
		} else {
			expapplC.setMiddleType(facade.getMiddleTypeService().findByCode(middleTypeCode.getCode()));
		}

	}

	public void generateTransitPaymentDetailByOvsaTrvlLrnExp(OvsaTrvlLrnExp exp, Entry remitEntry) {
		if (null == remitEntry || CollectionUtils.isEmpty(exp.getOvsaExpDrawInfos())) {
			return;
		}
		// RE201601162_國外出差旅費 EC0416 20160707 START
		// 在儲存時刪除重複的過度付款明細
		List<TransitPaymentDetail> deleteTransitPaymentDetailList = facade.getTransitPaymentDetailService().findByEntry(remitEntry);

		if (!CollectionUtils.isEmpty(deleteTransitPaymentDetailList)) {
			facade.getTransitPaymentDetailService().delete(deleteTransitPaymentDetailList);
		}
		// RE201601162_國外出差旅費 EC0416 20160707 END

		// 產生過渡付款明細
		List<TransitPaymentDetail> transitPaymentDetailList = new ArrayList<TransitPaymentDetail>();
		/*
		 *  若”領款帳號類別”=”薪資帳戶” 參考共用function《產生過渡付款明細(付款對象為個人或單位)》，付款對象為個人時;
		 * 
		 *  若”領款帳號類別”=”自行輸入”，欄位值如下:  付款方式 = R:匯款  票據抬頭/匯款戶名 =國外研修差旅費用領款資料.
		 * 領款人姓名  受款總行 =國外研修差旅費用領款資料.解款行前三碼  受款分行 =國外研修差旅費用領款資料.解款行後四碼  匯款帳號
		 * =國外研修差旅費用領款資料.帳號  金額 =國外研修差旅費用領款資料.金額  廠商統編 = 空白  受款人代號
		 * =國外研修差旅費用領款資料.匯款開戶ID  單位代號 = 空白  分錄 = 應付費用科目分錄(待匯科目)
		 */

		for (OvsaExpDrawInfo info : exp.getOvsaExpDrawInfos()) {
			transitPaymentDetailList.add(facade.getTransitPaymentDetailService().generateTransitPaymentDetailByOvsaExpDrawInfo(info, remitEntry, exp.getExpapplC()));
		}

		if (!CollectionUtils.isEmpty(transitPaymentDetailList)) {
			remitEntry.setTransitPaymentDetails(transitPaymentDetailList);
		}

	}

	public Map<String, Object> checkBudget(ExpapplC expapplC) {
		// RE201403462_預算修改 CU3178 2014/10/24 START
		// RE201403338_配合預算實支控管103年規定 CU3178 2014/10/12 START
		// 排除C1.5.13、C1.5.3
		if (StringUtils.equals("2014", expapplC.getExpYears().substring(0, 4)) && !(MiddleTypeCode.CODE_N10.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType()))) && !(MiddleTypeCode.CODE_C00.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType())))) {
			return facade.getBudgetInService().check103yearBudget(expapplC, null);
		} else {
			// RE201701547_費用系統預算優化第二階段 EC0416 2017/4/7 start
			return facade.getBudgetInService().checkBudget2(expapplC);
			// RE201701547_費用系統預算優化第二階段 EC0416 2017/4/7 end
		}
		// RE201403338_配合預算實支控管103年規定 CU3178 2014/10/12 END
		// RE201403462_預算修改 CU3178 2014/10/24 END
	}

	// RE201403462_預算修改 CU3178 2014/10/24 START
	public Map<String, Object> checkBudgetNew(ExpapplC expapplC, FunctionCode functionCode) {
		// 排除C1.5.13、C1.5.3
		if (StringUtils.equals("2014", expapplC.getExpYears().substring(0, 4)) && !(MiddleTypeCode.CODE_N10.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType())) && !(MiddleTypeCode.CODE_C00.equals(MiddleTypeCode.getByValue(expapplC.getMiddleType()))))
		// RE201403569_排除廠商件 CU3178 2014/12/25 START
				&& !(FunctionCode.C_1_5_9.getCode().equals(functionCode.getCode()))
		// RE201403569_排除廠商件 CU3178 2014/12/25 END
		) {
			return facade.getBudgetInService().check103yearBudget(expapplC, functionCode);
		} else {
			return facade.getBudgetInService().checkBudget(expapplC);
		}
	}

	// RE201403462_預算修改 CU3178 2014/10/24 END

	public List<ReturnExpapplCDto> findReturnExpapplCDtoByParams(Calendar subpoenaDateStart, Calendar subpoenaDateEnd, String departmentCode) {
		List<String> departmentCodes = getDepartmentCodesByDepartmentCode(departmentCode);
		return getDao().findReturnExpapplCDtoByParams(subpoenaDateStart, subpoenaDateEnd, departmentCodes);
	}

	private List<String> getDepartmentCodesByDepartmentCode(String departmentCode) {
		if (StringUtils.isBlank(departmentCode)) {
			return null;
		}

		Department department = facade.getDepartmentService().findByCode(departmentCode);
		if (null == department) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Department_code") });
		}
		DepartmentLevelPropertyCode departmentLevelPropertyCode = null;
		if (DepartmentLevelPropertyCode.A.getCode().equals(department.getDepartmentLevelProperty().getCode())) {
			departmentLevelPropertyCode = DepartmentLevelPropertyCode.B;
		}

		if (DepartmentLevelPropertyCode.B.getCode().equals(department.getDepartmentLevelProperty().getCode())) {
			departmentLevelPropertyCode = DepartmentLevelPropertyCode.C;
		}

		List<Department> departmentList = null;
		if (null != departmentLevelPropertyCode) {
			Map<String, Object> criteriaMap = new HashMap<String, Object>();
			criteriaMap.put("departmentLevelProperty.code", departmentLevelPropertyCode.getCode());
			criteriaMap.put("parentDepartment", department);
			departmentList = facade.getDepartmentService().findByCriteriaMap(criteriaMap);
		}

		List<String> departmentCodes = new ArrayList<String>();
		departmentCodes.add(department.getCode());

		if (!CollectionUtils.isEmpty(departmentList)) {
			for (Department dep : departmentList) {
				departmentCodes.add(dep.getCode());
			}
		}
		logger.info("departmentLevelProperty = " + department.getDepartmentLevelProperty().getCode());
		logger.info("parentDepartment = " + department.getCode());
		logger.info("departmentCodes = " + departmentCodes.toString());
		return departmentCodes;
	}

	public List<VendorExpDto> findVendorExpDto(Calendar startDate, Calendar endDate) {
		return getDao().findVendorExpDto(startDate, endDate);
	}

	// RE201400844_C11.7.5費用申請單狀態查效能修改 CU3178 2014/9/16 START
	public List<ExpapplCDto> findExpapplCDtoByParams(String userCode, List<Department> deliverDepList, BigDecimal invoiceAmt, Calendar createDateStart, Calendar createDateEnd) {

		// return getDao().findExpapplCDtoByParams(userCode,
		// deliverDepartmentCode, invoiceAmt, createDateStart, createDateEnd);
		return getDao().findExpapplCDtoByParamsNew(userCode, deliverDepList, invoiceAmt, createDateStart, createDateEnd);
	}

	// RE201400844_C11.7.5費用申請單狀態查效能修改 CU3178 2014/9/16 END

	public void checkTransitPaymentDetailAmountByExpApplNo(String expApplNo) {
		if (StringUtils.isBlank(expApplNo)) {
			return;
		}

		ExpapplC expapplC = this.findByExpApplNo(expApplNo);
		if (null == expapplC) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") + expApplNo });
		}

		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (CollectionUtils.isEmpty(entry.getTransitPaymentDetails())) {
				continue;
			}
			getDao().checkTransitPaymentDetailAmountByExpApplNo(entry, expApplNo);
		}
	}

	public void updateExpapplCDetail(ExpapplC expapplC) {
		if (null == expapplC || null == expapplC.getEntryGroup() || CollectionUtils.isEmpty(expapplC.getEntryGroup().getEntries())) {
			// 防爆處理
			return;
		}

		List<ExpapplCDetail> expapplCDetailList = null;
		for (Entry entry : expapplC.getEntryGroup().getEntries()) {
			if (null != entry.getExpapplCDetail()) {
				if (CollectionUtils.isEmpty(expapplCDetailList)) {
					expapplCDetailList = new ArrayList<ExpapplCDetail>();
				}
				// 設定明細與申請單的關聯
				entry.getExpapplCDetail().setExpapplC(expapplC);
				// 設定明細與分錄的關聯
				entry.getExpapplCDetail().setEntry(entry);
				expapplCDetailList.add(entry.getExpapplCDetail());
				// 更新明細
				facade.getExpapplCDetailService().update(entry.getExpapplCDetail());
			}
		}
		// 設定申請單與明細的關聯
		expapplC.setExpapplCDetails(expapplCDetailList);

		update(expapplC);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#updateExpapplCState
	 * (java.util.List, tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode,
	 * tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode)
	 */
	public void updateExpapplCState(List<String> applNoList, FunctionCode functionCode, ApplStateCode applStateCpde) {
		Calendar sysDate = Calendar.getInstance();

		if (CollectionUtils.isEmpty(applNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}

		// 檢核每筆申請單的借貸方是否平衡
		checkEntriesIsBalance(applNoList);

		// 取得ExpapplC List
		List<ExpapplC> expapplCList = this.findByApplNo(applNoList);

		// 申請單狀態
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(applStateCpde, SysTypeCode.C);

		// 申請單狀態不可為空值
		if (null == applState) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplState") });
		}

		User loginUser = facade.getUserService().getLoggedInUser();

		if (!CollectionUtils.isEmpty(expapplCList)) {

			for (ExpapplC expapplC : expapplCList) {

				// set 費用申請單.申請單狀態
				if (!expapplC.getApplState().getCode().equals(ApplStateCode.APPLICATION_REJECTED.getCode())) {// 不為退件才改狀態(2009/12/10)
					expapplC.setApplState(applState);
				}

				// set 費用申請單.更新日
				expapplC.setUpdateDate(sysDate);
				// set 費用申請單.更新人員
				expapplC.setUpdateUser(loginUser);

				// 更新費用申請單
				this.update(expapplC);

				// 建立流程簽核歷程
				if (!expapplC.getApplState().getCode().equals(ApplStateCode.APPLICATION_REJECTED.getCode())) {// 不為退件才記錄流程簽核歷程(2009/12/14)
					this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
				}
			}

		}

	}

	/**
	 * 檢核分錄是否平衡
	 * 
	 * @param applNoList
	 *            費用申請單單號List
	 */
	private void checkEntriesIsBalance(List<String> applNoList) {
		if (CollectionUtils.isEmpty(applNoList)) {
			return;
		}

		List<ExpapplC> expList = this.findByApplNo(applNoList);
		for (ExpapplC expapplC : expList) {
			// 借貸是否平衡
			this.facade.getEntryGroupService().calcBalance(expapplC.getEntryGroup());

			if (!expapplC.getEntryGroup().isBalanced()) {
				// 顯示《借貸不平衡,申請單號:{0}》
				throw new ExpRuntimeException(ErrorCode.C10531, new String[] { expapplC.getExpApplNo() });
			}

			this.checkTransitPaymentDetailAmountByExpApplNo(expapplC.getExpApplNo());
		}
	}

	public long findSalDepOfficeExpCountByPaperNo(String papersNo) {
		return getDao().findSalDepOfficeExpCountByPaperNo(papersNo);
	}

	public long findGeneralExpCountByPaperNo(String papersNo) {
		return getDao().findGeneralExpCountByPaperNo(papersNo);
	}

	public long findSalDepOfficeExpCountByPaperNoDepUtilCode(String papersNo, String depUtilCode) {
		return getDao().findSalDepOfficeExpCountByPaperNoDepUtilCode(papersNo, depUtilCode);
	}

	public long findGeneralExpCountByPaperNoDepUtilCode(String papersNo, String depUtilCode) {
		return getDao().findGeneralExpCountByPaperNoDepUtilCode(papersNo, depUtilCode);
	}

	public List<ExpapplCMaintainDto> findExpapplCMaintainDtoByParams(String bigTypeCode, String middleTypeCode, String depCode, Calendar createDateStart, Calendar createDateEnd, String expApplNo, String projectCode, String userCode) {
		return getDao().findExpapplCMaintainDtoByParams(bigTypeCode, middleTypeCode, depCode, createDateStart, createDateEnd, expApplNo, projectCode, userCode);
	}

	/** RE201201260_二代健保 2012/12/27 start */
	/**
	 * 二代健保相關檢查 1. 若憑證金額達"二代健保起扣金額下限"且"免扣取補充保費 為未勾選(需扣)"時，檢核"實際扣繳保費"欄位值不可為0，
	 * 若為0，顯示《該所得已達二代健保補充保費下限，若不需扣取，請勾選"免扣取補充保費"，並選擇"免扣補充保費原因"》。
	 * 
	 * 2.若憑證金額達"二代健保起扣金額下限"且"免扣取補充保費 為勾選(不需扣)"時，將實繳保費欄位歸0。
	 * 
	 * RE201201260_二代健保 cm9539 2012/11/07
	 * 
	 * 新增適用二代健保科目的判斷 RE201201260_二代健保 cm9539 2012/11/23
	 * 
	 * @param vo
	 */
	public void doHealInsMinAmtCheck(AccTitle acct, ExpapplC expapplc) {
		BigDecimal healthMinAmt = acct.getHealthMinAmt();
		/** defect 2013/01/18 RE201201260_二代健保 cm9539 修正非健保件為健保科目會被認定為健保件 start */
		boolean isHealAcct = expapplc.isHealthFlag();
		/** defect 2013/01/18 RE201201260_二代健保 cm9539 修正非健保件為健保科目會被認定為健保件 end */

		BigDecimal invoiceAmt = expapplc.getInvoiceAmt();

		if (isHealAcct && !(invoiceAmt.compareTo(healthMinAmt) < 0)) {
			boolean nonHealIns = expapplc.isNonHealthFlag();
			BigDecimal actualHealFee = expapplc.getActualSupplementaryPremium();

			if (nonHealIns) {
				if (null == expapplc.getPremiumReason()) {
					throw new ExpRuntimeException(ErrorCode.A10048, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_premiumReason") });

				}
				expapplc.setActualSupplementaryPremium(BigDecimal.ZERO);
			} else {
				if (!(actualHealFee.compareTo(BigDecimal.ZERO) > 0)) {

					throw new ExpRuntimeException(ErrorCode.A10073, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_expapply_inputapplyform_generalExpCreate_checkActualSupplementaryPremium") });
				}
			}
		}
	}

	/**
	 * 檢查所得格式與所得人證號類別是否符合二代健保規則: 所得人證號類別=1身份證字號、2工員工資代號 或 所得人證號類別=3員工代號且所得格式代號
	 * 不為 50
	 * 
	 * RE201201260_二代健保 cm9539 2012/11/07
	 * 
	 * @param acct
	 * @param inComeIdTypeCode
	 * @return
	 */
	public boolean doCheckHealInsIncomeForm(AccTitle acct, String inComeIdTypeCode) {
		boolean isHealIns = false;

		// 所得人證號類別=1身份證字號、2工員工資代號
		if (inComeIdTypeCode.equals(IncomeIdTypeCode.IDENTITY_ID.getCode()) || inComeIdTypeCode.equals(IncomeIdTypeCode.EMP_SALARY_ID.getCode())) {
			isHealIns = true;
			// 所得人證號類別=3員工代號且所得格式代號 不為 50
		} else if (!acct.getIncomeForm().equals(IncomeFormCode.INCOME_FORM_50.getCode()) && inComeIdTypeCode.equals(IncomeIdTypeCode.EMP_ID.getCode())) {
			isHealIns = true;
		}

		return isHealIns;
	}

	/** RE201201260_二代健保 C7.1.8扣繳健保補充保費 start */
	/*
	 * 依據單號查詢行政費用已送結非租賃之費用申請單 RE201201260_二代健保 cm9539 2012/12/03
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.ExpapplCService#findHealthPremiumAppl
	 * (tw.com.skl.exp.kernel.model6.bo.ExpapplC)
	 */
	public ExpapplC findHealthPremiumAppl(String expNo) {
		return getDao().findHealthPremiumAppl(expNo);
	}

	/*
	 * 根據user輸入分錄, 調整原分錄借貸並顯示在畫面上
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.ExpapplCService#
	 * calculateTempHealthPremiumEntry(tw.com.skl.exp.kernel.model6.bo.ExpapplC,
	 * tw.com.skl.exp.kernel.model6.bo.Entry)
	 */
	public List<Entry> calculateTempHealthPremiumEntry(ExpapplC expC, Entry healEntry) {
		List<Entry> entries = expC.getEntryGroup().getEntries();
		Entry oldHealEn = null;
		Entry healEn = new Entry();
		BeanUtils.copyProperties(healEntry, healEn);
		Entry remitEn = null;

		Iterator<Entry> it = entries.iterator();
		while (it.hasNext()) {
			Entry en = it.next();
			if (en.getAccTitle().getCode().equals(AccTitleCode.PAYBLE_REMIT.getCode())
			// 加入代開 2012/12/21
					|| en.getAccTitle().getCode().equals(AccTitleCode.PAYBLE_CHECK.getCode())) {
				remitEn = en;
				continue;
			}
			if (en.getAccTitle().getCode().equals(AccTitleCode.HEALTH_INSURANCE_20210921.getCode())) {
				oldHealEn = en;
				continue;
			}
		}

		if (null == oldHealEn) {
			if (healEn.getEntryType().getValue().equals(EntryTypeCode.TYPE_2_4.getValue())) {
				remitEn.setAmt(remitEn.getAmt().subtract(healEn.getAmt()));

			} else {
				remitEn.setAmt(remitEn.getAmt().add(healEn.getAmt()));
			}
		} else {
			BigDecimal newDAmt = facade.getEntryService().getAmtByEntryType(healEn, EntryTypeValueCode.D.getValue());
			BigDecimal newCAmt = facade.getEntryService().getAmtByEntryType(healEn, EntryTypeValueCode.C.getValue());
			remitEn.setAmt(remitEn.getAmt().subtract(newCAmt));
			remitEn.setAmt(remitEn.getAmt().add(newDAmt));
		}
		entries.add(healEn);
		return entries;
	}

	/*
	 * 儲存調整後分錄
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.ExpapplCService#
	 * calculateHealthPremiumEntry(tw.com.skl.exp.kernel.model6.bo.ExpapplC)
	 */
	public List<Entry> calculateHealthPremiumEntry(ExpapplC expC) {
		List<Entry> entries = expC.getEntryGroup().getEntries();
		Entry oldHealEn = null;// 原有補扣健保費分錄
		Entry newHealEn = null;// 新建補扣健保費分錄
		Entry remitEn = null;// 應付費用待匯待開科目分錄
		BigDecimal newDAmt = new BigDecimal(0);// 新建補扣健保費分錄科目金額(借方)
		BigDecimal newCAmt = new BigDecimal(0);// 新建補扣健保費分錄科目金額(貸方)
		BigDecimal oldDAmt = new BigDecimal(0);// 原有補扣健保費科目金額(借方)
		BigDecimal oldCAmt = new BigDecimal(0);// 原有補扣健保費科目金額(貸方)
		BigDecimal totalDAmt = new BigDecimal(0);
		BigDecimal totalCAmt = new BigDecimal(0);

		HealthEntryLog healLog = null;// 健保扣費調整log

		// 存入各分錄資料
		Iterator<Entry> it = entries.listIterator();
		while (it.hasNext()) {
			Entry en = it.next();
			if (null == en.getEntryGroup()) {
				newHealEn = en;
				newDAmt = facade.getEntryService().getAmtByEntryType(newHealEn, EntryTypeValueCode.D.getValue());
				newCAmt = facade.getEntryService().getAmtByEntryType(newHealEn, EntryTypeValueCode.C.getValue());
				it.remove();
			} else {
				if (en.getAccTitle().getCode().equals(AccTitleCode.PAYBLE_REMIT.getCode()) || en.getAccTitle().getCode().equals(AccTitleCode.PAYBLE_CHECK)) {
					remitEn = en;
				} else if (en.getAccTitle().getCode().equals(AccTitleCode.HEALTH_INSURANCE_20210921.getCode())) {
					oldHealEn = en;
					oldDAmt = facade.getEntryService().getAmtByEntryType(oldHealEn, EntryTypeValueCode.D.getValue());
					oldCAmt = facade.getEntryService().getAmtByEntryType(oldHealEn, EntryTypeValueCode.C.getValue());
				}
			}
		}
		totalDAmt = oldDAmt.add(newDAmt);
		totalCAmt = oldCAmt.add(newCAmt);

		// 分錄群組內無原有健保扣費科目分錄
		if (null == oldHealEn) {

			/** 2012/12/28 插入這個範圍 start cm9539 */
			if (newHealEn.getEntryType().getValue().equals(EntryTypeValueCode.D.getValue())) {
				throw new ExpRuntimeException(ErrorCode.A10073, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_healthpremium_HealthPremiumWithholdManagedBean_debitCannotMoreThanCredit") });

			}
			/** 2012/12/28 插入這個範圍 cm9539 end */

			newHealEn.setEntryGroup(expC.getEntryGroup());
			newHealEn = facade.getEntryService().create(newHealEn);
			entries.add(newHealEn);
			expC.getEntryGroup().setEntries(entries);
			healLog = facade.getHealthEntryLogService().createHealthEntryLogByExpapplC(expC, null, newHealEn, remitEn);
			// 分錄群組內原有健保扣費科目分錄與新建健保扣費科目分錄借貸不同之狀況
		} else if (!oldHealEn.getEntryType().getValue().equals(newHealEn.getEntryType().getValue())) {
			// 調整健保扣費科目後貸大於借
			if (totalCAmt.compareTo(totalDAmt) > 0) {
				healLog = facade.getHealthEntryLogService().createHealthEntryLogByExpapplC(expC, oldHealEn, newHealEn, remitEn);

				oldHealEn.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4));
				oldHealEn.setAmt(oldHealEn.getAmt().subtract(newHealEn.getAmt()));
				oldHealEn.setSummary(newHealEn.getSummary());
				// 調整後健保扣費科目net移除
			} else if (totalCAmt.compareTo(totalDAmt) == 0) {
				it = entries.iterator();
				while (it.hasNext()) {
					Entry en = it.next();
					if (en.getAccTitle().getCode().equals(AccTitleCode.HEALTH_INSURANCE_20210921.getCode())) {
						it.remove();
					}
				}

				healLog = facade.getHealthEntryLogService().createHealthEntryLogByExpapplC(expC, oldHealEn, null, remitEn);
				facade.getEntryService().delete(oldHealEn);
				// 調整後健保扣費科目貸餘, 拋錯中斷
			} else {
				throw new ExpRuntimeException(ErrorCode.A10073, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_healthpremium_HealthPremiumWithholdManagedBean_debitCannotMoreThanCredit") });
			}
			// 原有與新建健保扣費科目同為貸方
		} else {
			healLog = facade.getHealthEntryLogService().createHealthEntryLogByExpapplC(expC, oldHealEn, newHealEn, remitEn);
			oldHealEn.setAmt(oldHealEn.getAmt().add(newHealEn.getAmt()));
			oldHealEn.setSummary(newHealEn.getSummary());
		}

		// 更新過渡付款明細
		List<TransitPaymentDetail> tPDs = remitEn.getTransitPaymentDetails();
		for (TransitPaymentDetail tp : tPDs) {
			tp.setPaymentAmt(remitEn.getAmt());
		}
		// 設定更新的系統時間
		expC.setRealityAmt(remitEn.getAmt());
		expC.setUpdateDate(Calendar.getInstance());
		// 設定更新的人員
		expC.setUpdateUser(getLoginUser());
		update(expC);

		facade.getHealthEntryLogService().create(healLog);
		return entries;
	}

	/**
	 * 依據單號查詢行政費用申請單所得人 for C7.1.8扣繳健保補充保費 RE201201260_二代健保 cm9539 2012/12/13
	 * 
	 * @param expNo
	 * @return
	 */
	public List<String> findHealthPremiumApplIncomeId(String expNo) {
		return getDao().findHealthPremiumApplIncomeId(expNo);
	}

	/** RE201201260_二代健保 C7.1.8扣繳健保補充保費 end */

	/** RE201201260_二代健保 2013/07/01 start */
	/** RE201201260_二代健保 cm9539 2013/06/18 start */
	public String genBatchNoB(Calendar expectRemitDate) {
		List<ExpapplC> expList;
		ExpapplC exp;
		String batchNo;

		expList = getDao().findApplBeforeRemitB(expectRemitDate);
		if (expList.size() > 0) {
			exp = expList.get(0);
			if (StringUtils.isNotBlank(exp.getBatchNo())) {
				batchNo = exp.getBatchNo();
				return batchNo;
			}
		}

		SNGenerator gen = AbstractSNGenerator.getInstance(BatchNoGenerator.class.getName(), facade.getSequenceService());
		Map<String, String> params = new HashMap<String, String>();
		batchNo = gen.getSerialNumber(params);

		return batchNo;
	}

	public String genBatchNoC(Calendar expectRemitDate) {
		List<ExpapplC> expList;
		ExpapplC exp;
		String batchNo;

		expList = getDao().findApplBeforeRemitC(expectRemitDate);
		if (expList.size() > 0) {
			exp = expList.get(0);
			if (StringUtils.isNotBlank(exp.getBatchNo())) {
				batchNo = exp.getBatchNo();
				return batchNo;
			}
		}

		SNGenerator gen = AbstractSNGenerator.getInstance(BatchNoGenerator.class.getName(), facade.getSequenceService());
		Map<String, String> params = new HashMap<String, String>();
		batchNo = gen.getSerialNumber(params);

		return batchNo;
	}

	/** RE201201260_二代健保 cm9539 2013/06/18 end */
	/** RE201201260_二代健保 2013/07/01 end */

	// RE201401980_新增檢核借貸方是否平衡 CU3178 2014/9/30 start
	/**
	 * 判斷借貸方是否平衡
	 */
	public void checkcalcBalance(String expapplno, String functionCode) {
		List entrylist = getDao().findForEntryGroup(expapplno); // 查詢
		BigDecimal camt = BigDecimal.ZERO; // 貸方金額
		BigDecimal damt = BigDecimal.ZERO; // 借方金額
		for (Object obj : entrylist) {
			Object[] record = (Object[]) obj;
			BigDecimal amt = (BigDecimal) record[0]; // 金額
			String entrytype = (String) record[1]; // 借貸方
			if (entrytype.equals("C")) {
				camt = camt.add(amt);
			} else if (entrytype.equals("D")) {
				damt = damt.add(amt);
			}
		}
		if (camt.compareTo(damt) != 0) {
			// RE201400844_去掉MAIL CU3178 2014/11/17 START
			/*
			 * User user = (User) AAUtils.getLoggedInUser(); String sendto =
			 * "skcs0459@skl.com.tw;skcu3178@skl.com.tw;"; //收件者 String subject
			 * = "申請單號:"+expapplno+"借貸不平衡"; //傳送的主旨 String text =
			 * "操作人員為:"+user.getCode()+user.getName()+"，操作功能為:"+functionCode;
			 * //內容 facade.getMailService().sendMail(sendto,subject, text);
			 */
			// RE201400844_去掉MAIL CU3178 2014/11/17 END
			throw new ExpRuntimeException(ErrorCode.C10578, new String[] { expapplno });
		}
	}

	// RE201401980_新增檢核借貸方是否平衡 CU3178 2014/9/30 end

	// RE201500189_國內出差申請作業流程簡化 EC0416 2015/04/10 start
	public List<BudgetIn> findprojectcode(String projectCode, String year) {
		return facade.getBudgetInService().findprojectcode(projectCode, year);
	}

	// RE201500189_ 國內出差申請作業流程簡化 EC0416 2015/04/10 end

	// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 START
	public List<Entry> doSalGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean isBegWithHold, String summary, ExpapplCDetail expapplCDetail) {
		if (null == expapplC || null == departmentCode) {
			return null;
		}

		// 1.If傳入參數”費用申請單.冊號類別”不等於獎金品冊號或”費用申請單.冊號1”為空值， throw
		// ExpRuntimeExceiption，顯示”傳入參數錯誤”
		if (null == expapplC.getListType() || !ListTypeCode.PREMIUM_AWARD.getCode().equals(expapplC.getListType().getCode()) || CollectionUtils.isEmpty(expapplC.getRosterDetail())) {
			throw new ExpRuntimeException(ErrorCode.C10288, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterDetail_rosterNo") });
		}

		// 2.new 一個待回傳分錄的List
		List<Entry> entryList = new ArrayList<Entry>();

		// 5.new 一個BigDecimal變數”申請總額”，用來暫存各「領獎人.獎項金額」總額。
		BigDecimal totalAmount = BigDecimal.ZERO;

		// 7.產生一筆費用科目的分錄，資料內容如下，並新增到分錄的List:
		Entry expEntry = new Entry();

		// 科目借貸別=貸方
		EntryType entryTypeForC = this.getEntryTypeForC();
		// 科目借貸別=借方
		EntryType entryTypeForD = this.getEntryTypeForD();

		for (RosterDetail rosterDetail : expapplC.getRosterDetail()) {
			// 4.若「名冊.名冊狀態」不等於”0.尚未請領”，throw ExpRuntimeExceiption，顯示《名冊單號: ”冊號”
			// 重覆請領》
			/*
			 * if (!RosterStateCode.UNAPPLIED.getCode().equals(rosterDetail.
			 * getRosterState
			 * ().getCode())&&rosterDetail.getExpApplNo().compareTo
			 * (expapplC.getExpApplNo())!=0) { String[] params = { listNo }; //
			 * 顯示《名冊單號: ”冊號” 重覆請領》 throw new
			 * ExpRuntimeException(ErrorCode.C10058, params); }
			 */

			List<GainPerson> gainPersonList = facade.getGainPersonService().findByRosterDetail(rosterDetail);

			// 6.取得「名冊.領獎人」List，並對每一筆領獎人執行以下計算，新增應付代扣科目及計算申請總額:
			for (GainPerson gainPerson : gainPersonList) {
				// ”申請總額” = ”申請總額”+ 「領獎人.獎項金額」
				totalAmount = totalAmount.add(gainPerson.getGainAmt());

				if (isBegWithHold != true) {
					continue;
				}
				// 改用名冊建檔所計算出的金額
				BigDecimal taxAmt = gainPerson.getTaxAmt2();

				// 若計算出的所得稅等於0，繼續執行下一個領獎人(continue)
				if (null == taxAmt || BigDecimal.ZERO.equals(taxAmt)) {
					continue;
				}

				// 若所得稅不等於0，new 一筆分錄資料，用來記錄此領獎人的應付代扣科目，參數如下，並新增到分錄的List:
				Entry taxEntry = new Entry();
				/*
				 * ☆ ”分錄.會計科目”=「名冊.科目代號.代扣科目代號」 ☆
				 * ”分錄.業別代號”=「名冊.科目代號.代扣科目代號.所得稅業別代號」 ☆ ”分錄.科目借貸別”=貸方 ☆
				 * ”分錄.金額”=計算出的所得稅金額 ☆ “分錄.業別代號”及”分錄.所得人證號”欄位: ＊
				 * 預設”分錄.所得人證號類別”=4(員工代號); ”分錄.所得人證號”=領獎人.領獎人員工代號 ＊
				 * 當領獎人.領獎人員工代號為空值時，以”領獎人.所得人資料.所得人類別”判斷分錄.所得人證號類別。
				 * 當”領獎人.所得人資料.所得人類別”=自然人，分錄.所得人證號類別”=1(身份證字號);
				 * 否則分錄.所得人證號類別”=3(廠商統編); ”分錄.所得人證號”=所得人資料. 身份證字號/廠商統編
				 */
				taxEntry.setAccTitle(rosterDetail.getAccTitle().getWithhold());
				taxEntry.setIndustryCode(rosterDetail.getAccTitle().getWithhold().getIncomeBiz());
				taxEntry.setEntryType(entryTypeForC);
				taxEntry.setAmt(taxAmt);

				if (StringUtils.isNotBlank(gainPerson.getGainUserId())) {
					taxEntry.setIncomeIdType(IncomeIdTypeCode.EMP_ID.getCode());
					taxEntry.setIncomeId(gainPerson.getGainUserId());
				} else {
					if (IncomeUserTypeCode.NATURAL_PERSON.getCode().equals(gainPerson.getIncomeUser().getIncomeUserType().getCode())) {
						taxEntry.setIncomeIdType(IncomeIdTypeCode.IDENTITY_ID.getCode());
					} else {
						taxEntry.setIncomeIdType(IncomeIdTypeCode.COMP_ID.getCode());
					}
					taxEntry.setIncomeId(gainPerson.getIncomeUser().getIdentityId());
				}

				entryList.add(taxEntry);

			}

			if (null == expEntry.getAccTitle()) {
				/*
				 * ”分錄.會計科目”=查回的「名冊.科目代號」 ”分錄.科目借貸別”=借方 ”分錄.金額”=”申請總額”
				 * ”分錄.成本單位”=傳入參數”成本單位”
				 */
				expEntry.setAccTitle(rosterDetail.getAccTitle());
				expEntry.setEntryType(entryTypeForD);
				expEntry.setSummary(summary);
				Department dep = facade.getDepartmentService().findByCode(departmentCode);
				expEntry.setCostUnitCode(dep.getCode());
				expEntry.setCostUnitName(dep.getName());
				if (null != expapplCDetail) {// IISI-20100805 : 修正費用明細沒有存入DB問題
					ExpapplCDetail detail = new ExpapplCDetail();
					BeanUtils.copyProperties(expapplCDetail, detail);
					expEntry.setExpapplCDetail(detail);
				}
			}
		}

		expEntry.setAmt(totalAmount);

		entryList.add(expEntry);

		//
		// 11.若「費用申請單.是否扣繳印花稅」=true，產生印花稅貸方科目。
		if (expapplC.isWithholdStamp()) {
			BigDecimal withholdStamp = expapplC.getStampAmt();
			Entry withholdStampEntry = new Entry();
			withholdStampEntry.setAccTitle(expEntry.getAccTitle().getStampTax());
			withholdStampEntry.setAmt(withholdStamp);
			withholdStampEntry.setEntryType(this.getEntryTypeForC());

			// 印花稅金額必須大於0,才能將分入加入List
			if (null != withholdStampEntry.getAmt() && BigDecimal.ZERO.compareTo(withholdStampEntry.getAmt()) < 0) {
				entryList.add(withholdStampEntry);
			}
		}

		// 12.若「費用申請單.是否扣繳進項稅」=true，產生進項稅借方科目。
		/*
		 * 執行共用function《檢核費用申請單的進項稅相關欄位資訊》 計算進項稅邏輯，執行共用function《計算進項稅》(參考SDD總綱)
		 */
		generateWithholdIncome(expapplC, expEntry.getAccTitle(), entryList);

		if (null == expapplC.getEntryGroup()) {
			expapplC.setEntryGroup(new EntryGroup());
		}

		for (Entry entry : entryList) {
			entry.setEntryGroup(expapplC.getEntryGroup());
		}

		expapplC.getEntryGroup().setEntries(entryList);
		// 13.回傳分錄的List
		return entryList;
	}

	public void updateRosterStateList(Integer state, ExpapplC expapplc) {
		/*
		 * 1. if傳入參數為以下情況，throw ExpRuntimeException，顯示”傳入參數錯誤” 傳入參數”動作”不等於0或1
		 * 費用申請單號為空值 當傳入參數”動作”=0，且册號List為空值或List長度不等於1或2
		 */
		if (null == state || !(state == 1 || state == 0)) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_RosterState_code") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (CollectionUtils.isEmpty(expapplc.getRosterDetail())) {
			String[] params = { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_listNo") };
			// 顯示”傳入參數錯誤”
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}
		Calendar sysDate = Calendar.getInstance();

		/*
		 * 2. 依傳入參數”動作”執行以下邏輯:  2a. 若傳入參數”動作”=0時:
		 * 1.依傳入的冊號List，檢查所有「名冊.名冊狀態」必須為”尚未請領”  若「名冊.名冊狀態」不等於”0.尚未請領”，throw
		 * ExpRuntimeExceiption，顯示《名冊單號: ”冊號” 重覆請領》 2. 依冊號1，計算該「名冊.領獎人.獎項金額」的總和
		 * 3. 設定以下欄位: ▲ 1.「名冊.名冊狀態」=請領完畢 ▲ 2.「名冊.已使用金額」=獎項金額總額 ▲
		 * 3.「名冊.費用申請單號」=傳入的”費用申請單.申請單號” 4. 儲存「名冊」 5. 若有參數”冊號2”，重覆執行步驟2-4
		 */
		if (state == 0) {
			for (RosterDetail listNo : expapplc.getRosterDetail()) {
				// 查出名冊
				/*
				 * RosterDetail rosterDetail =
				 * this.facade.getRosterDetailService
				 * ().findByRosterNoFetchRelation(listNo); if (null ==
				 * rosterDetail) { throw new
				 * ExpRuntimeException(ErrorCode.A20002, new
				 * String[]{MessageUtils.getAccessor()
				 * .getMessage("tw_com_skl_exp_kernel_model6_bo_RosterDetail"
				 * )}); }
				 */
				// 「名冊.名冊狀態」不等於”0.
				if (!RosterStateCode.UNAPPLIED.getCode().equals(listNo.getRosterState().getCode())) {
					String[] params = { listNo.getRosterNo() };
					// 顯示《名冊單號: ”冊號” 重覆請領》
					throw new ExpRuntimeException(ErrorCode.C10058, params);
				}

				// 2. 獎項金額總額
				BigDecimal totalAmount = BigDecimal.ZERO;
				for (GainPerson gainPerson : listNo.getGainPersons()) {
					// 計算該「名冊.領獎人.獎項金額」的總和
					totalAmount = totalAmount.add(gainPerson.getGainAmt());
				}

				// 3.設定以欄位
				// 「名冊.名冊狀態」=請領完畢
				listNo.setRosterState(this.facade.getRosterStateService().findByCode(RosterStateCode.APPLIED));
				// 「名冊.已使用金額」=獎項金額總額
				listNo.setUseAmt(totalAmount);
				// 「名冊.費用申請單號」=傳入的”費用申請單.申請單號”
				listNo.setExpApplNo(expapplc.getExpApplNo());
				// 「名冊.費用申請」=傳入的”費用申請單”
				listNo.setExpapplC(expapplc);

				listNo.setUpdateDate(sysDate);
				listNo.setUpdateUser(getLoginUser());

				// 4. 儲存「名冊」
				this.facade.getRosterDetailService().update(listNo);
			}
		}

		/*
		 * 2b. 若傳入參數”動作”=1時: 1. 依申請單號查出「費用申請單」 2. 依「費用申請單.冊號1」，查出「名冊」資料，並設定以下的值
		 * ▲ 1. 「名冊.名冊狀態」設為尚未請領。 ▲ 2. 「名冊.已使用金額」設為0 ▲ 3. 「名冊.費用申請單號」設為null 3.
		 * 儲存「名冊」 4. 若「費用申請單.冊號2」不為空值，重覆執行步驟2-4
		 */
		if (state == 1) {
			for (RosterDetail listNo : expapplc.getRosterDetail()) {
				// 「名冊.名冊狀態」設為尚未請領
				listNo.setRosterState(this.facade.getRosterStateService().findByCode(RosterStateCode.UNAPPLIED));
				// 「名冊. 已使用金額」設為0
				listNo.setUseAmt(BigDecimal.ZERO);
				// 「名冊.費用申請單號」設為null
				listNo.setExpApplNo(null);
				// 「名冊.費用申請」設為null
				listNo.setExpapplC(null);
				listNo.setUpdateDate(sysDate);
				listNo.setUpdateUser(getLoginUser());
				// 3. 儲存「名冊」
				this.facade.getRosterDetailService().update(listNo);
			}
		}
	}

	/**
	 * <pre>
	 * modify...2009/9/1, By Eustace
	 *     檢核「費用申請單.冊號類別」欄位
	 *        若冊號類別=NULL，「費用申請單.冊號1」、「費用申請單.冊號2」必須為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，非名冊的申請單，冊號欄位必須為空值》
	 *        若冊號類別=” 獎金品冊號”，「費用申請單.冊號1」不能為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，獎金品申請單，冊號欄位必須填值》
	 *        若冊號類別不等於” 獎金品冊號”，「費用申請單.冊號1」不能為空值且「費用申請單.冊號2」必須為空值
	 *        否則throw ExpRuntimeException，顯示《費用申請單錯誤，一般名冊的申請單，必須填入唯一的冊號欄位》
	 */
	public void checkExpapplCByListTypeN10(ExpapplC expapplC) {
		if (null == expapplC.getListType()) {
			// 若冊號類別=NULL，「費用申請單.名冊」必須為空值
			if (CollectionUtils.isEmpty(expapplC.getRosterDetail())) {
				// 顯示《費用申請單錯誤，非名冊的申請單，冊號欄位必須為空值》
				throw new ExpRuntimeException(ErrorCode.C10101);
			}

		} else if (ListTypeCode.PREMIUM_AWARD.equals(ListTypeCode.getByValue(expapplC.getListType()))) {
			// 若冊號類別=” 獎金品冊號”，「費用申請單.冊號1」不能為空值
			if (!CollectionUtils.isEmpty(expapplC.getRosterDetail())) {
				// 顯示《費用申請單錯誤，獎金品申請單，冊號欄位必須填值》
				throw new ExpRuntimeException(ErrorCode.C10102);
			}

		}/*
		 * else{ // 若冊號類別不等於” 獎金品冊號”，「費用申請單.冊號1」不能為空值且「費用申請單.冊號2」必須為空值 if
		 * (StringUtils.isBlank(expapplC.getListNo1()) ||
		 * StringUtils.isNotBlank(expapplC.getListNo2())) {
		 * //顯示《費用申請單錯誤，一般名冊的申請單，必須填入唯一的冊號欄位》 throw new
		 * ExpRuntimeException(ErrorCode.C10103); } }
		 */
	}

	public void deleteRoster(List<RosterDetail> rosterDetailList) {
		Calendar sysDate = Calendar.getInstance();
		for (RosterDetail rosterDetail : rosterDetailList) {
			rosterDetail.setRosterState(facade.getRosterStateService().findByCode(RosterStateCode.UNAPPLIED));
			// 「名冊. 已使用金額」設為0
			rosterDetail.setUseAmt(BigDecimal.ZERO);
			// 「名冊.費用申請單號」設為null
			rosterDetail.setExpApplNo(null);
			// 「名冊.費用申請」設為null
			rosterDetail.setExpapplC(null);
			rosterDetail.setUpdateDate(sysDate);
			rosterDetail.setUpdateUser(getLoginUser());
			// 3. 儲存「名冊」
			this.facade.getRosterDetailService().update(rosterDetail);
		}
	}

	// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 END

	// RE201501248_檢核專案代號與成本單位 EC0416 2015/6/29 start
	// 傳入專案代號與成本單位做比較
	public void checkProjectCode(String projectCode, String costCode) {
		// 如果專案代號欄位不為空
		if (StringUtils.isNotEmpty(projectCode)) {
			// 因為直接抓取頁面的專案代號可能會有抓到不是部級的單位代號，所以要從預算轉入檔
			// TBEXP_BUDGET_IN裡面找出專案代號欄位並連結到
			// 編列單位代號(部級)TBEXP_BUDGET_IN.ARRANGE_UNIT_CODE 跟頁面上的成本單位做比較
			List<BudgetIn> budgetina = facade.getBudgetInService().findByCode(projectCode);
			Department costdep = facade.getDepartmentService().findByCode(costCode);
			// RE201600212_C1.5.1成本單位KEY錯 按下更新費用明細不會檢核(有輸入專案代號) CU3178 2016/1/29
			// START
			if (null == costdep) {
				throw new ExpRuntimeException(ErrorCode.D10035);
			}
			// RE201600212_C1.5.1成本單位KEY錯 按下更新費用明細不會檢核(有輸入專案代號 ) CU3178
			// 2016/1/29 END
			// 判斷的單位代號
			String compdepcode = costCode;
			// 如果專案代號欄位不是空的
			if (CollectionUtils.isNotEmpty(budgetina)) {
				BudgetIn budgetin = budgetina.get(0);
				// 如果組織型態為外埠 則需利用成本單位抓取其預算單位
				if (!DepartmentTypeCode.PARENT_COMPANY.getCode().equals(costdep.getDepartmentType().getCode()) && !DepartmentTypeCode.SERVICE_CENTER.getCode().equals(costdep.getDepartmentType().getCode())) {
					compdepcode = costdep.getBudgetDepCode();
				}
				// 如果編列單位代號(部級)不等於成本單位代號 則會出現錯誤訊息
				if (!budgetin.getArrangeUnitCode().equals(compdepcode)) {
					throw new ExpRuntimeException(ErrorCode.C10604, new String[] { budgetin.getArrangeUnitCode(), costCode });
				}
			} else {
				throw new ExpRuntimeException(ErrorCode.C10009);
			}
		}
	}

	// RE201501248_檢核專案代號與成本單位 EC0416 2015/6/29 end

	// //RE201504024_C10.8.6申請單維護、C1.5.3公務車 CU3178 2015/10/26 START
	/**
	 * 檢核公務車年度限額
	 * 
	 * @param eac
	 * @param accTitle
	 * @param carLoanCode
	 * @return
	 */
	public int pubcaraffyearqouta(ExpapplC eac, AccTitle accTitle, String carLoanCode) {
		if (eac == null)
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
		if (StringUtils.isBlank(carLoanCode))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_checkType") });

		User applUser = facade.getUserService().findByCode(eac.getApplyUserInfo().getUserId());

		// 若中分類為C00、A30則不需檢核特簽人員
		if (!(MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) || MiddleTypeCode.CODE_A30.getCode().equals(eac.getMiddleType().getCode()))) {
			if (applUser == null)
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo1") });
			// 如果是特簽人員，就直接回傳
			else if (facade.getApplQuotaService().isSpecialPerson(applUser))
				return 0;
		}

		// 要更新分錄資料裡的金額、和憑證金額(含)的金額
		BigDecimal canApplAmt = BigDecimal.ZERO;
		BigDecimal appliedAmt = BigDecimal.ZERO;

		// 2010/9/20 修改:協理車(M)及非經理切結車(J)專用。
		// 限額檔一人只能建置一筆固定金額,固協理車及非經理切結車一律只能以C00檢核;
		// 但付費對象為廠商時,兩者實際存檔的費用中分類是A30
		List<ApplQuota> applQuotas = facade.getApplQuotaService().findByYearAndUserCodeAndMiddleTypeAndExpItem(eac.getExpYears().substring(0, 4), eac.getApplyUserInfo().getUserId(), "C00", eac.getExpItem().getCode());

		// 查無該userCode的申請限額資料，則拋錯
		// RE201502606_公務車費用系統可做『沖轉預付費用』 CU3178 2015/7/28 START
		if (CollectionUtils.isEmpty(applQuotas)) {
			// 若查無限額中分類為公務車，付款對象為:個人、單位、廠商，付款方式為:開票、匯款、沖轉暫付、沖轉預付則跳過檢核。
			if ((MiddleTypeCode.CODE_C00.getCode().equals(eac.getMiddleType().getCode()) || MiddleTypeCode.CODE_A30.getCode().equals(eac.getMiddleType().getCode())) && (PaymentTargetCode.PERSONAL.getCode().equals(eac.getPaymentTarget().getCode()) || PaymentTargetCode.BUSSINESS_REVIEW.getCode().equals(eac.getPaymentTarget().getCode()) || PaymentTargetCode.VENDOR.getCode().equals(eac.getPaymentTarget().getCode())) && (PaymentTypeCode.C_CHANGE_TEMP_PREPAID.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHANGE_TEMP_PAY.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode()))) {
				return 0;
			} else {
				throw new ExpRuntimeException(ErrorCode.C10527);
			}
		}
		ApplQuota userQuota = applQuotas.get(0);
		// 2010/9/20 修改:協理車(M)及非經理切結車(J)專用。
		appliedAmt = getAppliedTotalAmount("C00", eac.getApplyUserInfo().getUserId(), eac.getExpYears().substring(0, 4)).add(getAppliedTotalAmount("A30", eac.getApplyUserInfo().getUserId(), eac.getExpYears().substring(0, 4)));
		canApplAmt = userQuota.getYyQuota().subtract(appliedAmt);

		if (canApplAmt.compareTo(BigDecimal.ZERO) <= 0) {
			// 若公務車貸款別=M(協理車)、J(非經理階切結車)，付款方式為匯款或開票則跳出警示不會產生分錄，付款方式為沖轉預付、沖轉暫付跳出警示會產生分錄
			if (carLoanCode.equals("M") || carLoanCode.equals("J")) {
				if (PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode())) {
					throw new ExpRuntimeException(ErrorCode.C10324, new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
				} else {
					MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
				}
				return 0;
			} else {
				MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });
			}
		}
		// 若”可申請的餘額” 大於
		// 傳入參數”費用申請單.分錄群組.分錄”(分錄.會計科目=指定費用項目對應的會計科目，且分錄借貸別=借方)，則不處理回傳至主程式
		for (Entry en : eac.getEntryGroup().getEntries()) {
			if (eac.getExpItem().getAccTitles().get(0).getCode().equals(en.getAccTitle().getCode()) && EntryTypeCode.TYPE_1_D.getValue().equals(en.getEntryType().getValue())) {
				if (canApplAmt.compareTo(en.getAmt()) >= 0) {
				}
				// 並設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
				else
					en.setAmt(canApplAmt);
			}
		}
		if (canApplAmt.compareTo(eac.getInvoiceAmt()) >= 0)
			return 0;
		// 否則顯示《費用年月、費用項目申請金額餘額不足!》訊息(但程式可繼續執行)，並設定「費用申請單.憑證金額(含)」等於”可申請的餘額”
		else {
			if ((carLoanCode.equals("M") || carLoanCode.equals("J")) && (PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode()) || PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode()))) {
				eac.setInvoiceAmt(canApplAmt);
				eac.setInvoiceTaxAmt(BigDecimal.ZERO);
				eac.setInvoiceNoneTaxAmt(BigDecimal.ZERO);
				if (eac.isWithholdIncome()) {
					// 重新計算憑證金額(稅)(未) 2010/02/24 By Eustace
					BigDecimal invoiceTaxAmt = facade.getAccTitleService().calculateIncomeAmt(accTitle.getCode(), true, canApplAmt);// 進項稅額(incomeAmt)
																																	// 憑證金額(稅invoiceTaxAmt)
					BigDecimal invoiceNoneTaxAmt = canApplAmt.subtract(invoiceTaxAmt);// 憑證金額(未)
					eac.setInvoiceNoneTaxAmt(invoiceNoneTaxAmt);
					eac.setInvoiceTaxAmt(invoiceTaxAmt);
				}
			}
			MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10324.toString(), new String[] { eac.getExpYears().substring(0, 4), eac.getExpItem().getName(), canApplAmt.toString() });

			return -1;
		}
	}

	// RE201504024_C10.8.6申請單維護、C1.5.3公務車 CU3178 2015/10/26 END

	// RE201502395_調整B2_5效能_V2 2015/11/02 START
	public List<ExpapplC> findApplyForDailyStmtClosed(User user) {
		List<String> expapplCNos = getDao().findApplyForDailyStmtClosed(user);
		if (CollectionUtils.isEmpty(expapplCNos)) {
			return null;
		}

		return this.findByApplNo(expapplCNos);
	}

	// RE201502395_調整B2_5效能_V2 2015/11/02 END

	// RE201504572_優化研修差旅 CU3178 2015/12/18 START
	public List<String> findByParamsLrn(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610, String paperNo, String classCode) {
		// 要查詢的申請單狀態
		ApplStateCode applStateCode = null;

		// 要排除查詢的申請單狀態
		List<ApplStateCode> delApplStateCodeList = null;
		// 申請單狀態
		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.APPLIED;
		}
		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			applStateCode = ApplStateCode.TEMP;
		}

		if (ApplStateEnum.DELETE.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.DELETED;
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.APPLIED);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.TEMP);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}
		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end

		User loginUser = (User) AAUtils.getLoggedInUser();
		List<Department> loginUserDepList = null;
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());

		boolean isPowerGroup = true;
		// defect4951_增加群組權限條件 CU3178 2018/1/25 START
		// 為C1.6.10時，且不等於GroupCode的群組則存取設限
		if (is1610 && !(GroupCode.ADMIN.equals(groupCode) || GroupCode.AUDITOR_FIRST_VERIFY.equals(groupCode) || GroupCode.AUDITOR_REVIEW.equals(groupCode) || GroupCode.AUDITOR_TAX.equals(groupCode) || GroupCode.AUDITOR_GENERAL.equals(groupCode) || GroupCode.GAE_GENERAL.equals(groupCode) || GroupCode.HUMAN_RESOURCE.equals(groupCode) || GroupCode.LEARNING.equals(groupCode) || GroupCode.AUDITOR_PM_REVIEW.equals(groupCode))) {
			// defect4951_增加群組權限條件 CU3178 2018/1/25 END
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && isFindDepartmentCode) {
			// 不為C1.6.10時，且isFindDepartmentCode不等於true
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && !MiddleTypeCode.CODE_H10.equals(middleTypeCode)) {
			// 不為C1.6.10時，且中分類不能於H10
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		}

		List<String> departmentCodes = null;
		if (isFindDepartmentCode) {
			Department loginDep = facade.getUserService().getLoggedInUser().getDepartment();
			if (null == loginDep) {
				StringBuffer sb = new StringBuffer();
				sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_department"));
				sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User"));
				sb.append(":" + facade.getUserService().getLoggedInUser().getCode() + facade.getUserService().getLoggedInUser().getName());
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { sb.toString() });
			}
			departmentCodes = new ArrayList<String>();
			departmentCodes.add(loginDep.getCode());
			// 找出本處與分處的單位代號
			if (DepartmentLevelPropertyCode.C.getCode().equals(loginDep.getDepartmentLevelProperty().getCode())) {
				if (null != loginDep.getDepartmentCost()) {
					departmentCodes.add(loginDep.getDepartmentCost().getCode());
				} else {
					List<Department> list = facade.getDepartmentService().findDepartmentCostByCode(loginDep.getCode());
					if (!CollectionUtils.isEmpty(list)) {
						for (Department department : list) {
							departmentCodes.add(department.getCode());
						}
					}
				}
			}
		}

		// 找出費用申請單
		List<ExpapplC> dataList = getDao().findByParamsLrn(applStateCode, middleTypeCode, applyUserCode, createDateStart, createDateEnd, departmentCodes, delApplStateCodeList, loginUserDepList, loginUser, isPowerGroup, paperNo, classCode);

		if (CollectionUtils.isEmpty(dataList)) {
			return null;
		}

		// 取出申請單號
		List<String> list = new ArrayList<String>();
		for (ExpapplC expapplC : dataList) {
			list.add(expapplC.getExpApplNo());
		}
		return list;
	}

	// RE201504572_優化研修差旅 CU3178 2015/12/18 END

	// RE201601158_優化簽收核銷日計表 EC0416 2016/5/9 START
	public void doReturn(String deliverDayListNo, FunctionCode functionCode) throws ExpException {
		Calendar sysDate = Calendar.getInstance();
		if (StringUtils.isBlank(deliverDayListNo)) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo", null) };
			throw new ExpException(ErrorCode.A10007, params);
		}

		DeliverDaylist deliverDaylist = facade.getDeliverDaylistService().findByDeliverDaylistNo(deliverDayListNo);
		/**
		 * 尚未完成: 判斷若大分類為"辦公費"，則 申請單:狀態改為"送審" 送件表:已送件設為false。 若否，則 申請單狀態改為"未送件"。
		 * 
		 * 共同要作: 送件表簽收日設為null、簽收人員設為null
		 */

		// 申請件狀態"未送件"
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.NOT_SEND, SysTypeCode.C);

		// 申請件狀態"送審"
		ApplState applState1 = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.FIRST_VERIFICATION_SEND, SysTypeCode.C);
		// mark0515 start
		for (ExpapplC expapplC : deliverDaylist.getExpapplCs()) {
			/**
			 * 條件判斷改為不為20審核中、39退回審核、11.重新送件、38.退單審核
			 * 錯誤訊息改為:送件表單號:{0}中，申請單號{1}，狀態不為審核中、退回審核、重新送件、退單審核。
			 */
			if (!(expapplC.getApplState().getCode().equals(ApplStateCode.FIRST_VERIFICATION.getCode()) || (expapplC.getApplState().getCode().equals(ApplStateCode.FIRST_VERIFICATION_REJECTED.getCode())) || (expapplC.getApplState().getCode().equals(ApplStateCode.RE_VERIFICATION_SEND.getCode())) || (expapplC.getApplState().getCode().equals(ApplStateCode.RETURN_EXPAPPL_VERIFICATION.getCode())))) {
				throw new ExpRuntimeException(ErrorCode.C10633, new String[] { deliverDaylist.getDeliverNo(), expapplC.getExpApplNo() });
			}

			if (!(deliverDaylist.getBigType().getCode().equals(BigTypeCode.OFFICE_EXP.getCode()))) {
				// 申請表狀態改為"送審"
				expapplC.setApplState(applState1);

			} else {
				// 費用申請件狀態修改為"未送件"
				expapplC.setApplState(applState);
				/** 只有辦公費才要將已送件設為null **/
				deliverDaylist.setDelivered(false);
			}
			/** 初審經辦欄位清空 **/
			expapplC.setVerifyUser(null);
			// 送件表簽收日為NULL
			deliverDaylist.setSignDate(null);
		}

		this.facade.getDeliverDaylistService().update(deliverDaylist);
		for (ExpapplC expapplC : deliverDaylist.getExpapplCs()) {
			this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
		}
	}

	// RE201601158_優化簽收核銷日計表 EC0416 2016/5/9 END

	// defect#3361 EC0416 2016/6/23 START
	public List<ExpapplC> findByDeliverDayListNoByW(String deliverDayListNo, String entryCostCode) throws ExpRuntimeException {
		if (StringUtils.isBlank(deliverDayListNo)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo") });
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct e");
		queryString.append(" from ExpapplC e");
		// queryString.append(" left join fetch e.deliverDaylist");
		queryString.append(" where e.deliverDaylist.deliverNo =:deliverNo");
		if ("W".equals(entryCostCode)) {
			queryString.append(" and e.costTypeCode ='W'");
		} else {
			queryString.append(" and e.costTypeCode <>'W' or e.costTypeCode is null");
		}
		queryString.append(" order by e.expApplNo");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("deliverNo", deliverDayListNo);

		List<ExpapplC> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	// defect#3361 EC0416 2016/6/23 END

	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 START
	public List<String> findByParamsHRLrn(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610, String paperNo, String classCode) {
		// 要查詢的申請單狀態
		ApplStateCode applStateCode = null;

		// 要排除查詢的申請單狀態
		List<ApplStateCode> delApplStateCodeList = null;
		// 申請單狀態
		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.APPLIED;
		}

		if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			applStateCode = ApplStateCode.TEMP;
		}

		if (ApplStateEnum.DELETE.equals(applStateEnum) && is1610 == true) {
			applStateCode = ApplStateCode.DELETED;
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == true) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.APPLIED);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}

		if (ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum) && is1610 == false) {
			delApplStateCodeList = new ArrayList<ApplStateCode>();
			delApplStateCodeList.add(ApplStateCode.TEMP);
			delApplStateCodeList.add(ApplStateCode.DELETED);
		}
		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end

		User loginUser = (User) AAUtils.getLoggedInUser();
		List<Department> loginUserDepList = null;
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());

		boolean isPowerGroup = true;
		// defect4951_增加群組權限條件 CU3178 2018/1/25 START
		// 為C1.6.10時，且不等於GroupCode的群組則存取設限
		if (is1610 && !(GroupCode.ADMIN.equals(groupCode) || GroupCode.AUDITOR_FIRST_VERIFY.equals(groupCode) || GroupCode.AUDITOR_REVIEW.equals(groupCode) || GroupCode.AUDITOR_TAX.equals(groupCode) || GroupCode.AUDITOR_GENERAL.equals(groupCode) || GroupCode.GAE_GENERAL.equals(groupCode) || GroupCode.HUMAN_RESOURCE.equals(groupCode) || GroupCode.LEARNING.equals(groupCode) || GroupCode.AUDITOR_PM_REVIEW.equals(groupCode))) {
			// defect4951_增加群組權限條件 CU3178 2018/1/25 END
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && isFindDepartmentCode) {
			// 不為C1.6.10時，且isFindDepartmentCode不等於true
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		} else if (!is1610 && !MiddleTypeCode.CODE_H10.equals(middleTypeCode)) {
			// 不為C1.6.10時，且中分類不能於H10
			isPowerGroup = false;
			User applyUser = this.getFacade().getUserService().findByCode(applyUserCode);
			if (applyUser != null) {
				loginUserDepList = this.getFacade().getDepartmentService().findAllLevelDepartment(loginUser.getDepartment());

			}
		}

		List<String> departmentCodes = null;
		if (isFindDepartmentCode) {
			Department loginDep = facade.getUserService().getLoggedInUser().getDepartment();
			if (null == loginDep) {
				StringBuffer sb = new StringBuffer();
				sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User_department"));
				sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_User"));
				sb.append(":" + facade.getUserService().getLoggedInUser().getCode() + facade.getUserService().getLoggedInUser().getName());
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { sb.toString() });
			}
			departmentCodes = new ArrayList<String>();
			departmentCodes.add(loginDep.getCode());
			// 找出本處與分處的單位代號
			if (DepartmentLevelPropertyCode.C.getCode().equals(loginDep.getDepartmentLevelProperty().getCode())) {
				if (null != loginDep.getDepartmentCost()) {
					departmentCodes.add(loginDep.getDepartmentCost().getCode());
				} else {
					List<Department> list = facade.getDepartmentService().findDepartmentCostByCode(loginDep.getCode());
					if (!CollectionUtils.isEmpty(list)) {
						for (Department department : list) {
							departmentCodes.add(department.getCode());
						}
					}
				}
			}
		}

		// 找出費用申請單
		List<ExpapplC> dataList = getDao().findByParamsHRLrn(applStateCode, middleTypeCode, applyUserCode, createDateStart, createDateEnd, departmentCodes, delApplStateCodeList, loginUserDepList, loginUser, isPowerGroup, paperNo, classCode);

		if (CollectionUtils.isEmpty(dataList)) {
			return null;
		}

		// 取出申請單號
		List<String> list = new ArrayList<String>();
		for (ExpapplC expapplC : dataList) {
			list.add(expapplC.getExpApplNo());
		}
		return list;
	}
	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 END

}
