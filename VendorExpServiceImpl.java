package tw.com.skl.exp.kernel.model6.logic.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import tw.com.skl.common.model6.logic.impl.BaseServiceImpl;
import tw.com.skl.common.model6.web.jsf.utils.Messages;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.ApplState;
import tw.com.skl.exp.kernel.model6.bo.BigEntry;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.Entry;
import tw.com.skl.exp.kernel.model6.bo.EntryGroup;
import tw.com.skl.exp.kernel.model6.bo.EntryType;
import tw.com.skl.exp.kernel.model6.bo.ExpapplC;
import tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail;
import tw.com.skl.exp.kernel.model6.bo.IncomeUser;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.PaymentBatch;
import tw.com.skl.exp.kernel.model6.bo.PubAffCarExp;
import tw.com.skl.exp.kernel.model6.bo.ReturnStatement;
import tw.com.skl.exp.kernel.model6.bo.Subpoena;
import tw.com.skl.exp.kernel.model6.bo.SystemParam;
import tw.com.skl.exp.kernel.model6.bo.TransitPaymentDetail;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.bo.Vendor;
import tw.com.skl.exp.kernel.model6.bo.VendorContract;
import tw.com.skl.exp.kernel.model6.bo.VendorExp;
import tw.com.skl.exp.kernel.model6.bo.AccBookType.AccBookTypeCode;
import tw.com.skl.exp.kernel.model6.bo.AccClassType.AccClassTypeCode;
import tw.com.skl.exp.kernel.model6.bo.AccTitle.AccTitleCode;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.BigType.BigTypeCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeValueCode;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.IncomeIdType.IncomeIdTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ListType.ListTypeCode;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentType.PaymentTypeCode;
import tw.com.skl.exp.kernel.model6.bo.SysType.SysTypeCode;
import tw.com.skl.exp.kernel.model6.bo.SystemParam.SystemParamName;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpException;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dao.VendorExpDao;
import tw.com.skl.exp.kernel.model6.dto.VendorExpApproveDto;
import tw.com.skl.exp.kernel.model6.dto.VendorExpDaylistDetailDto;
import tw.com.skl.exp.kernel.model6.dto.VendorExpInCloseTempPayDto;
import tw.com.skl.exp.kernel.model6.dto.VendorExpMonthCloseDto;
import tw.com.skl.exp.kernel.model6.facade.VendorExpFacade;
import tw.com.skl.exp.kernel.model6.logic.VendorExpService;
import tw.com.skl.exp.kernel.model6.logic.enumeration.ApplStateEnum;
import tw.com.skl.exp.kernel.model6.sn.AbstractSNGenerator;
import tw.com.skl.exp.kernel.model6.sn.PaymentBatchNoGenerator;
import tw.com.skl.exp.kernel.model6.sn.SNGenerator;
import tw.com.skl.exp.kernel.model6.sn.SubpoenaNoGenerator;
import tw.com.skl.exp.kernel.model6.sn.SysMonthCNoGenerator;

/**
 * 廠商費用 Service
 * 
 * @author Eustace
 * 
 */
public class VendorExpServiceImpl extends BaseServiceImpl<VendorExp, String, VendorExpDao> implements VendorExpService {

	/**
	 * Sleep 次數
	 */
	private int sleepCounter = 0;

	/**
	 * 借方
	 */
	private EntryType debit_1_EntryType;

	/**
	 * 貸方
	 */
	private EntryType credit_1_EntryType;

	/**
	 * 申請單狀態-已送匯
	 */
	private ApplState applSateRemitSend;

	/**
	 * 會計科目Cache: 科目代號/值
	 */
	private Map<String, AccTitle> accTitleCacheMap = new HashMap<String, AccTitle>();

	private VendorExpFacade facade;

	public VendorExpFacade getFacade() {
		return facade;
	}

	public void setFacade(VendorExpFacade facade) {
		this.facade = facade;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.VendorExpService#
	 * findForVendorExpapplFetchRelation
	 * (tw.com.skl.exp.kernel.model6.logic.enumeration.ApplStateEnum,
	 * tw.com.skl.exp.kernel.model6.bo.BigType.BigTypeCode, java.lang.String,
	 * tw.com.skl.exp.kernel.model6.bo.ProofType.ProofTypeCode, java.util.Date,
	 * java.util.Date, java.lang.Boolean)
	 */
	public List<VendorExp> findForVendorExpapplFetchRelation(ApplStateEnum applStateEnum, String payYearMonthBegin, String payYearMonthEnd, String vendorCompId, String invoiceNo, String applUserCode, String generalMgrSn, String loginUserDepCode, String deliverNo) {

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" left join fetch vendorExp.vendor");
		boolean truncated = false;

		Map<String, Object> params = new HashMap<String, Object>();

		// 申請單狀態
		if (null != applStateEnum) {
			if (applStateEnum.equals(ApplStateEnum.NOT_VERIFICATION_SEND)) {
				if (!truncated) {
					queryString.append(" where");
					truncated = true;
				}
				queryString.append(" vendorExp.expapplC.applState.code =:applStateCode");
				params.put("applStateCode", ApplStateCode.APPLIED.getCode());

				queryString.append(" and vendorExp.expapplC.applState.sysType.code =:sysTypeCode");
				params.put("sysTypeCode", SysTypeCode.C.getCode());

				queryString.append(" and");

			} else if (applStateEnum.equals(ApplStateEnum.VERIFICATION_SEND)) {
				if (!truncated) {
					queryString.append(" where");
					truncated = true;
				}

				queryString.append(" vendorExp.expapplC.applState.code not in(:applStateCode1 , :applStateCode2)");
				params.put("applStateCode1", ApplStateCode.APPLIED.getCode());
				params.put("applStateCode2", ApplStateCode.DELETED.getCode());

				queryString.append(" and vendorExp.expapplC.applState.sysType.code =:sysTypeCode");
				params.put("sysTypeCode", SysTypeCode.C.getCode());

				queryString.append(" and");
			}
		}

		// 付款年月起(YYYYMM)
		if (StringUtils.isNotBlank(payYearMonthBegin)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.payYearMonth >=:payYearMonthBegin");
			params.put("payYearMonthBegin", payYearMonthBegin);

			queryString.append(" and");
		}

		// 付款年月迄(YYYYMM)
		if (StringUtils.isNotBlank(payYearMonthEnd)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.payYearMonth <=:payYearMonthEnd");
			params.put("payYearMonthEnd", payYearMonthEnd);

			queryString.append(" and");
		}

		// 統一編號
		if (StringUtils.isNotBlank(vendorCompId)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.vendor.vendorCompId =:vendorCompId");
			params.put("vendorCompId", vendorCompId);

			queryString.append(" and");
		}

		// 發票號碼
		if (StringUtils.isNotBlank(invoiceNo)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.invoiceNo like :invoiceNo");
			params.put("invoiceNo", invoiceNo + "%");

			queryString.append(" and");
		}

		// 申請人員工代號
		if (StringUtils.isNotBlank(applUserCode)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.createUser.code =:applUserCode");
			params.put("applUserCode", applUserCode);

			queryString.append(" and");
		}

		// 經辦序號
		if (StringUtils.isNotBlank(generalMgrSn)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.generalMgrSn =:generalMgrSn");
			params.put("generalMgrSn", generalMgrSn);

			queryString.append(" and");
		}

		// 所屬單位代號
		if (StringUtils.isNotBlank(loginUserDepCode)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			// defect:1361 UAT-C-UC1.6.5
			// 登入時顯示所有「狀態=申請中」，需再加上「廠商費用.費用申請單.建檔人員.所屬單位」=Session變數"登入人員.所屬單位"」
			queryString.append(" vendorExp.expapplC.createUser.department.code =:depCode");
			params.put("depCode", loginUserDepCode);

			queryString.append(" and");
		}

		// 送件日計表單號
		if (StringUtils.isNotBlank(deliverNo)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.deliverDaylist.deliverNo =:deliverNo");
			params.put("deliverNo", deliverNo);

			queryString.append(" and");
		}

		if (truncated) {
			// 刪除最後一個and字串
			queryString.delete(queryString.lastIndexOf("and"), queryString.length());
		}

		// 排序 : 依「廠商費用.經辦序號」、「費用申請單.申請單號」由小到大排序(#259)
		queryString.append(" ORDER BY vendorExp.generalMgrSn ASC, vendorExp.expapplC.expApplNo ASC");

		List<VendorExp> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 檢核分錄資料
	 * 
	 * @param entry
	 */
	private void verifyEntry(Entry entry) {
		if (null == entry) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry") });
		}
		if (null == entry.getAccTitle() || StringUtils.isBlank(entry.getAccTitle().getCode())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_accTitle") });
		}
		if (null == entry.getEntryType() || StringUtils.isBlank(entry.getEntryType().getCode())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_entryType") });
		}

		// 檢核頁面上，所有"會計科目代號"是否存在「會計科目」檔，若否，則顯示《會計科目代號錯誤》訊息，且不可儲存入檔。
		if (!this.facade.getAccTitleService().checkAccTitleCode(entry.getAccTitle().getCode())) {
			// 顯示《會計科目代號錯誤》訊息
			throw new ExpRuntimeException(ErrorCode.C10010);
		}

		// 檢核"借貸別"欄位僅可輸入"3"或"4"，若否，則顯示《借貸別錯誤》訊息，且不可儲存入檔
		if (!(EntryTypeCode.TYPE_2_3.getCode().equals(entry.getEntryType().getCode()) || EntryTypeCode.TYPE_2_4.getCode().equals(entry.getEntryType().getCode()))) {
			// 丟出 C10012:借貸別錯誤
			throw new ExpRuntimeException(ErrorCode.C10012);
		}

		// 檢核"成本單位代號"欄位內容值是否存在「組織單位.單位代號」，若否，則顯示《成本單位代號錯誤》訊息，且不可儲存入檔。
		if (StringUtils.isNotBlank(entry.getCostUnitCode())) {
			this.facade.getDepartmentService().checkDepartmentCode(entry.getCostUnitCode());
		}

		// 檢核"明細金額"欄位值須為正整數，若否，則顯示《明細金額輸入錯誤》訊息。
		if (null == entry.getAmt() || BigDecimal.ZERO.compareTo(entry.getAmt()) >= 0) {
			// 顯示《明細金額輸入錯誤》訊息
			throw new ExpRuntimeException(ErrorCode.C10014);
		}

		if (StringUtils.isNotBlank(entry.getSummary()) && entry.getSummary().length() > 50) {
			// 摘要長度不可超過50位數
			throw new ExpRuntimeException(ErrorCode.C10523, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_summary"), "50" });
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#addExpapplCDetail
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp,
	 * tw.com.skl.exp.kernel.model6.bo.Entry)
	 */
	public List<Entry> addExpapplCDetail(VendorExp exp, Entry expEntry, ExpapplCDetail expapplCDetail, List<Entry> withholdIncomeEntryList, String incomeId) {
		if (null == exp || null == expEntry) {
			List<String> errorStringList = new ArrayList<String>();
			if (null == exp) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp"));
			}
			if (null == expEntry) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry"));
			}

			String[] params = { errorStringList.toString() };
			// 傳入的參數{0}錯誤
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		verifyEntry(expEntry);

		// 處理分期付款邏輯 modify..2009/8/27, By Eustace
		doProcessInstallment(exp, expEntry);

		// 檢核費用資料
		this.verifyExp(exp, expEntry);

		expEntry.setEntryType(facade.getEntryTypeService().findEntryTypeByCode(expEntry.getEntryType().getCode(), EntryTypeCode.TYPE_2_3.getType()));

		if (null == exp.getExpapplC().getEntryGroup()) {
			exp.getExpapplC().setEntryGroup(new EntryGroup());
		}

		if (CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
			exp.getExpapplC().getEntryGroup().setEntries(new ArrayList<Entry>());
		}

		EntryGroup entryGroup = exp.getExpapplC().getEntryGroup();

		// 處理分期結轉邏輯 modify...2009/8/27, By Eustace
		doProcessCarriedByStages(exp, expEntry, entryGroup);

		if (null != exp.getExpapplC().getListType()) {
			if (ListTypeCode.JTEXP.equals(ListTypeCode.getByValue(exp.getExpapplC().getListType()))) {
				// 處理日通貨運
				/*
				 * 「費用申請單. 冊號類別」為”日通貨運批號”時: (註:不同寄件單位，故有多個成本單位) 
				 * 執行共用function《以日通貨運冊號，產生申請日通貨運費的分錄》
				 */
				List<Entry> jtexpList = this.facade.getEntryService().generateJtexpEntries(exp.getExpapplC(), expEntry);
				if (CollectionUtils.isEmpty(jtexpList)) {
					throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_JtexpInfo") });
				}
				expEntry = null;
				// 將產生的日通貨運分錄放回分錄群組
				for (Entry jtexpEntry : jtexpList) {
					jtexpEntry.setEntryGroup(entryGroup);
					entryGroup.getEntries().add(jtexpEntry);
				}
			} else {
				// IISI-20100805 : 修正費用明細沒有存入DB問題
				// 處理獎金品冊號流程
				facade.getExpapplCService().doGenerateApplyRosterEntries(exp.getExpapplC(), expEntry.getCostUnitCode(), false, expEntry.getSummary(), expEntry.getExpapplCDetail());
				expEntry = null;
			}
		} else {
			// 產生應付代扣科目

			// 產生該筆費用明細的分錄資料，與待新增的費用明細資料一併加入傳入的廠商費用資料中
			// List<Entry> entryList = new ArrayList<Entry>();

			if (!CollectionUtils.isEmpty(withholdIncomeEntryList)) {
				// 移除上次產生的分錄
				for (Entry withholdIncomeEntry : withholdIncomeEntryList) {
					exp.getExpapplC().getEntryGroup().getEntries().remove(withholdIncomeEntry);
				}
				withholdIncomeEntryList.clear();
			}

			Entry expTypeEntry = null;
			for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
				if (AccClassTypeCode.CODE_4.equals(AccClassTypeCode.getByValue(entry.getAccTitle().getAccClassType()))) {
					expTypeEntry = entry;
					break;
				}
			}

			if (null == expTypeEntry) {
				expTypeEntry = expEntry;
			}

			this.caculateIncomeAmt(exp);

			// 產生進項稅借方科目
			withholdIncomeEntryList = generateWithholdIncome(exp.getExpapplC(), expTypeEntry, withholdIncomeEntryList);

			// DEFECT_5038_92稅檔不全62080703 EC0416 20180403 start
			// 費用項目為“勞務費”時(費用項目代號為「62080100」)
			if (!"62080703".equals(expEntry.getAccTitle().getCode()) && EntryTypeCode.TYPE_2_3.getValue().equals(expEntry.getEntryType().getValue()) && !CollectionUtils.isEmpty(expEntry.getAccTitle().getExpItems()) && "62080100".equals(expEntry.getAccTitle().getExpItems().get(0).getCode())) {
				// DEFECT_5038_92稅檔不全62080703 EC0416 20180403 end
				if (StringUtils.isBlank(expTypeEntry.getIndustryCode())) {
					//  設定「分錄.業別代號」=「會計科目.所得稅業別代號」
					expTypeEntry.setIndustryCode(expTypeEntry.getAccTitle().getIncomeBiz());
				}

				if (facade.getAccTitleService().isWithholdType(expEntry.getAccTitle())) {
					AccTitle title = facade.getAccTitleService().findByCode(expEntry.getAccTitle().getCode());
					if (null == title.getWithhold()) {
						throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_withhold") });
					}
					/*
					 * 因為頁面沒有輸入所得人證號類別 所以要跑2次分別判斷, 所得人證號是否為身分證或者統編
					 */
					IncomeUser incomeUser = null;
					IncomeIdTypeCode incomeIdTypeCode = IncomeIdTypeCode.IDENTITY_ID;
					// 先查身分證字號
					incomeUser = facade.getIncomeUserService().findIncomeUser(incomeIdTypeCode, incomeId);
					if (null == incomeUser) {
						// 查不到則查統編
						incomeIdTypeCode = IncomeIdTypeCode.COMP_ID;
						incomeUser = facade.getIncomeUserService().findIncomeUser(incomeIdTypeCode, incomeId);
					}

					if (null == incomeUser) {
						// 身分證字號與統編都查不到
						// 查無則throw
						// ExpRuntimeException顯示《請先於UC10.8.4所得人資料維護功能建立該所得人資料》訊息
						throw new ExpRuntimeException(ErrorCode.C10097);
					}
					/*
					 * 於廠商費用輸入費用明細時， 不論明細金額是否達起扣金額(依會計科目檔之設定判斷該會計科目是否須代扣)，
					 * 一律代扣61120105
					 * -工員工資(本)代扣所得稅。expEntry.getAccTitle().getWithhold()
					 */
					/*
					 *  須檢核輸入的”業別代號”、”所得人證號”為必填，且必須存在「所得人資料」(#144) 
					 * 若”所得人證號”為空白，則顯示《憑證資訊的所得人證號資料不能為空白》且不可儲存入檔 
					 * 執行共用function《依所得人證號類別及所得人證號查詢所得人資料》，若查無則throw
					 * ExpRuntimeException顯示《請先於UC10.8.4所得人資料維護功能建立該所得人資料》訊息 ▲
					 * 參數: ☆ “所得人證號類別”為身份證字號或廠商統編 ☆ “所得人證號” 
					 * ”業別代號”存至勞務費科目的「分錄.業別代號」 
					 * ”所得人證號”存至科目「expEntry.getAccTitle
					 * ().getWithhold()」的「分錄.所得人證號類別」及「分錄.所得人證號」
					 */
					Entry entryC = new Entry();
					entryC.setAccTitle(title.getWithhold());
					entryC.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4));
					entryC.setAmt(facade.getAccTitleService().calculateTaxAmt(title, expEntry.getAmt(), false));
					entryC.setIncomeId(exp.getVendor().getVendorCompId());
					entryC.setIncomeIdType(incomeIdTypeCode.getCode());
					if (BigDecimal.ZERO.compareTo(entryC.getAmt()) < 0) {
						expEntry.setIncomeId(null);
						withholdIncomeEntryList.add(entryC);
					}
					entryC = null;

				}
				// DEFECT_5038_92稅檔不全62080703 EC0416 20180403 start
				// 如果會計科目為62080703維持費用-勞務費-其他勞務費(92所得)則須把所得人證號相關欄位寫入借方
			} else if ("62080703".equals(expEntry.getAccTitle().getCode())) {
				IncomeUser incomeUser = null;
				IncomeIdTypeCode incomeIdTypeCode = IncomeIdTypeCode.IDENTITY_ID;
				// 先查身分證字號
				incomeUser = facade.getIncomeUserService().findIncomeUser(incomeIdTypeCode, incomeId);
				if (null == incomeUser) {
					// 查不到則查統編
					incomeIdTypeCode = IncomeIdTypeCode.COMP_ID;
					incomeUser = facade.getIncomeUserService().findIncomeUser(incomeIdTypeCode, incomeId);
				}

				if (null == incomeUser) {
					// 身分證字號與統編都查不到
					// 查無則throw
					// ExpRuntimeException顯示《請先於UC10.8.4所得人資料維護功能建立該所得人資料》訊息
					throw new ExpRuntimeException(ErrorCode.C10097);
				}
				 if (EntryTypeCode.TYPE_2_3.getValue().equals(expEntry.getEntryType().getValue())) {
				Entry entryC = new Entry();
				entryC.setAccTitle(expEntry.getAccTitle());
				entryC.setAmt(expEntry.getAmt());
				entryC.setIncomeId(exp.getVendor().getVendorCompId());
				entryC.setIncomeIdType(incomeIdTypeCode.getCode());
				//entryC.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_3));
				withholdIncomeEntryList.add(entryC);
				entryC = null;
				 }
				

			} else if (EntryTypeCode.TYPE_2_3.getValue().equals(expEntry.getEntryType().getValue())&&!"62080703".equals(expEntry.getAccTitle().getCode())) {
              // DEFECT_5038_92稅檔不全62080703 EC0416 20180403 end
				BigDecimal taxAmt = facade.getAccTitleService().calculateTaxAmt(expEntry.getAccTitle(), expEntry.getAmt(), false);
				if (BigDecimal.ZERO.compareTo(taxAmt) < 0 && null != expEntry.getAccTitle().getWithhold()) {
					Entry entryC = new Entry();
					entryC.setAccTitle(expEntry.getAccTitle().getWithhold());
					entryC.setAmt(taxAmt);
					entryC.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4));
					entryC.setIncomeId(exp.getVendor().getVendorCompId());
					entryC.setIncomeIdType(IncomeIdTypeCode.COMP_ID.getCode());
					withholdIncomeEntryList.add(entryC);
					entryC = null;
				}
			}

			for (Entry entry : withholdIncomeEntryList) {
				entry.setEntryGroup(entryGroup);
				entryGroup.getEntries().add(entry);
			}

		}

		if (null != expEntry) {
			expEntry.setEntryGroup(entryGroup);
			entryGroup.getEntries().add(expEntry);
		}

		// 累加相同借貸別的"應附費用科目" modify...2009/8/19, By Eustace
		this.facade.getEntryService().calculateEntryies(entryGroup.getEntries());

		for (Entry entry : entryGroup.getEntries()) {
			generateCancelCodeByEntry(exp, entry);
			if (StringUtils.isBlank(entry.getEntryType().getId())) {
				entry.setEntryType(facade.getEntryTypeService().findEntryTypeByCode(entry.getEntryType().getCode(), "2"));
			}
		}
		return withholdIncomeEntryList;
	}

	/**
	 * 產生銷帳碼,當分錄的銷帳碼欄位為空時就產生銷帳碼
	 * 
	 * @param exp
	 * @param entry
	 */
	private void generateCancelCodeByEntry(VendorExp exp, Entry entry) {
		if (StringUtils.isBlank(entry.getCancelCode())) {
			// 需求單#271 第2點 輸入"20210342"科目時,借貸方都需產生銷帳碼
			if (StringUtils.equals(AccTitleCode.PAYBLE_PERSONNEL.getCode(), entry.getAccTitle().getCode()) && StringUtils.isNotBlank(entry.getExpapplCDetail().getTicketYearMonth())) {
				// 易飛網20210342需依訂票年月產生銷帳碼
				entry.setCancelCode(SysMonthCNoGenerator.getEzflySerialNumber(entry.getExpapplCDetail().getTicketYearMonth()));
				return;
			}

			String defaultTypeCode = "0002";
			String middleTypeCode = MiddleTypeCode.CODE_A20.getCode();

			// 檢核-廠商不可為空
			if (null == exp.getVendor() && StringUtils.isBlank(exp.getVendor().getVendorCompId())) {
				exp.getExpapplC().getEntryGroup().getEntries().remove(entry);
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Vendor_vendorCompId") });
			}
			// 會計科目(10840100,10840200)檢核-合約編號不可為空
			if (StringUtils.equals(AccTitleCode.PREPAY_COMPUTER_EQUIPMENT.getCode(), entry.getAccTitle().getCode()) || StringUtils.equals(AccTitleCode.PREPAY_COMPUTER_SOFTWARE.getCode(), entry.getAccTitle().getCode()) || exp.isInstallment()) {
				if (StringUtils.isBlank(exp.getVendorContractCode())) {
					exp.getExpapplC().getEntryGroup().getEntries().remove(entry);
					// 分期付款件須以合約類別產生銷帳碼,合約編號欄位不可為空白。
					throw new ExpRuntimeException(ErrorCode.C10388);
				}
			}

			// 10511204 預付費用-其他
			if (StringUtils.equals(AccTitleCode.PREPAY_OTHER.getCode(), entry.getAccTitle().getCode())) {
				if (exp.isInstallment()) {

					if (StringUtils.isBlank(exp.getVendorContractCode())) {
						exp.getExpapplC().getEntryGroup().getEntries().remove(entry);
						// 分期付款件須以合約類別產生銷帳碼,合約編號欄位不可為空白。
						throw new ExpRuntimeException(ErrorCode.C10388);
					}

					defaultTypeCode = "0003";
				} else {

					// 非分期付款 10511204”應為”預付費用-其他:
					// 產生規則(費用中分類(3)+西元年(後2碼)+月(2)+日(2)+流水號(5))
					middleTypeCode = MiddleTypeCode.CODE_A20.getCode();
					defaultTypeCode = "0002";
				}
			}
			VendorContract contract = null;
			String vendorContractTypeCode = null;
			if (StringUtils.isNotBlank(exp.getVendorContractCode())) {
				contract = this.facade.getVendorContractService().findByContractNo(exp.getVendorContractCode());
			}
			if (null != contract && null != contract.getVendorContractType()) {
				vendorContractTypeCode = contract.getVendorContractType().getCode();
			}

			try {

				// 產生銷帳碼
				entry.setCancelCode(facade.getCancelCodeTypeService().generateCancelCode(entry.getAccTitle(), entry.getEntryType().getCode(), defaultTypeCode, null, null, exp.getVendor().getVendorCompId(), null, middleTypeCode, null, vendorContractTypeCode, null, null));
			} catch (Exception e) {
				exp.getExpapplC().getEntryGroup().getEntries().remove(entry);
				throw new ExpRuntimeException(ErrorCode.A10060, new String[] { entry.getAccTitle().getCode() + entry.getAccTitle().getName() });
			}
		}
	}

	/**
	 * 處理分期付款邏輯
	 * 
	 * @param exp
	 *            廠商費用
	 * @param entry
	 *            分錄
	 */
	private void doProcessInstallment(VendorExp exp, Entry entry) {
		/*
		 * 若該費用為分期付款件，操作人員須於”分期付款”欄位輸入”Y”，
		 * 科目僅能輸入”10511100、10840100、10840200”(預付資產類)，
		 * 系統依”合約編號”、”科目代號”記錄該合約、科目之所有分期付款記錄記錄，以利於給付尾款時沖轉之用。
		 */
		if (exp.isInstallment()) {
			// 先判斷是否為分期付款科目,非分期付款科目時用費用科目找出分期付款科目
			AccTitle title = facade.getAccTitleService().findByCode(entry.getAccTitle().getCode());
			AccTitle installmentAccTitle = title.getInstallment();
			if (null == installmentAccTitle) {
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_installment") });
			}

			entry.setAccTitle(installmentAccTitle);

		}
	}

	/**
	 * 處理分期結轉
	 * 
	 * @param exp
	 *            廠商費用
	 * @param entry
	 *            分錄
	 * @param entryGroup
	 *            分錄群組
	 */
	protected void doProcessCarriedByStages(VendorExp exp, Entry entry, EntryGroup entryGroup) {
		// 處理”是否分期結轉=Y”
		if (exp.isCarriedByStages()) {
			// modify 2009/12/25 By Eustace 只須合約,不需要分期科目了
			// AccTitle expAccTitle =
			// facade.getAccTitleService().findByCode(entry.getAccTitle().getCode());
			//
			// if (null == expAccTitle.getInstallment()) {
			// throw new ExpRuntimeException(ErrorCode.A20002, new
			// String[]{MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_installment")});
			// }

			List<Entry> installmentEntries = this.facade.getEntryService().findInstallmentEntries(exp.getVendorContractCode(), null, null);

			if (!CollectionUtils.isEmpty(installmentEntries)) {
				Map<String, Entry> installmentEntryMap = new HashMap<String, Entry>();
				// 貸方分錄
				List<Entry> typeCEntries = new ArrayList<Entry>();
				// 借方分錄
				List<Entry> typeDEntries = new ArrayList<Entry>();

				// 過濾出借貸方(若銷帳碼為空時則跳過)
				for (Entry entry2 : installmentEntries) {
					if (StringUtils.isBlank(entry2.getCancelCode())) {
						continue;
					}
					if (EntryTypeValueCode.D.getValue().equals(entry2.getEntryType().getValue())) {
						typeDEntries.add(entry2);
					} else {
						typeCEntries.add(entry2);
					}
				}

				// 處理借方分錄
				for (Entry tempEntry : typeDEntries) {
					// 將借方依銷帳碼為群組放入MAP
					if (!installmentEntryMap.keySet().contains(tempEntry.getCancelCode())) {
						Entry entry3 = new Entry();
						org.springframework.beans.BeanUtils.copyProperties(tempEntry, entry3, new String[] { "id", "versionNo", "expapplCDetail" });
						installmentEntryMap.put(tempEntry.getCancelCode(), entry3);
						continue;
					} else {
						// 取出相同銷帳碼分錄
						Entry e = installmentEntryMap.get(tempEntry.getCancelCode());
						// 金額相加
						e.setAmt(e.getAmt().add(tempEntry.getAmt()));

						installmentEntryMap.put(tempEntry.getCancelCode(), e);
					}
				}

				// 若貸方LIST由值,則找出相對應的銷帳碼[借方].金額相減
				if (!CollectionUtils.isEmpty(typeCEntries)) {
					for (Entry entry2 : typeCEntries) {

						if (installmentEntryMap.keySet().contains(entry2.getCancelCode())) {
							// 取出借方金額
							BigDecimal amount = installmentEntryMap.get(entry2.getCancelCode()).getAmt();
							// 剩餘銷帳碼金額 = 借方-貸方
							amount = amount.subtract(entry2.getAmt());
							// 將剩餘金額塞回去MAP
							installmentEntryMap.get(entry2.getCancelCode()).setAmt(amount);
						} else {
							// 理論有貸方[銷帳碼]的出現,就會有借方[銷帳碼],若是沒有代表資料有問題
							throw new ExpRuntimeException(ErrorCode.A10041, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_cancelCode") });
						}
					}
				}

				// 貸方
				EntryType entryTypeC = this.facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4);
				for (Entry entry2 : installmentEntryMap.values()) {
					// 過濾掉金額小於等於0的狀況
					if (null == entry2.getAmt() || BigDecimal.ZERO.compareTo(entry2.getAmt()) >= 0) {
						continue;
					}
					entry2.setId(null);
					entry2.setExpapplCDetail(null);
					// 將借方轉成貸方
					entry2.setEntryType(entryTypeC);
					entry2.setEntryGroup(entryGroup);
					entryGroup.getEntries().add(entry2);
				}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#calculateEntry(tw
	 * .com.skl.exp.kernel.model6.bo.VendorExp,
	 * tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail)
	 */
	public List<Entry> calculateEntry(VendorExp exp, ExpapplCDetail expDetail) throws ExpException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#calculateTaxEntry
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp)
	 */
	public void calculateTaxEntry(VendorExp exp) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#deleteExpapplCDetail
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp, java.util.List)
	 */
	public void deleteExpapplCDetail(VendorExp exp, List<Integer> indexs) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doApproveApplyForm
	 * (java.util.List)
	 */
	public int doApproveApplyForm(List<String> expApplNoList, FunctionCode functionCode) {
		if (CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { "List<VendorExp> exps" });
		}
		Calendar sysDate = Calendar.getInstance();
		User user = getLoginUser();
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.FIRST_VERIFIED, SysTypeCode.C);

		List<ExpapplC> list = facade.getExpapplCService().findByApplNo(expApplNoList);

		int total = 0;
		for (ExpapplC expapplC : list) {

			// 借貸是否平衡
			this.facade.getEntryGroupService().calcBalance(expapplC.getEntryGroup());

			if (!expapplC.getEntryGroup().isBalanced()) {
				// 顯示《借貸不平衡,申請單號:{0}》
				throw new ExpRuntimeException(ErrorCode.C10531, new String[] { expapplC.getExpApplNo() });
			}
			// 檢核過渡付款明細金額是否相等於費用科目金額
			facade.getExpapplCService().checkTransitPaymentDetailAmountByExpApplNo(expapplC.getExpApplNo());

			if (expapplC.getApplState().getCode().equals(ApplStateCode.FIRST_VERIFICATION.getCode()) || expapplC.getApplState().getCode().equals(ApplStateCode.FIRST_VERIFICATION_REJECTED.getCode()) || expapplC.getApplState().getCode().equals(ApplStateCode.RE_VERIFICATION_SEND.getCode())) {
				expapplC.setActualVerifyUser(user);
				expapplC.setApplState(applState);
				expapplC.setUpdateDate(sysDate);
				expapplC.setUpdateUser(user);
				facade.getExpapplCService().update(expapplC);
				this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
			} else {
				// 不為"審核中"時，顯示《狀態錯誤，尚未完成核銷》訊息
				throw new ExpRuntimeException(ErrorCode.C10019);
			}
			total++;
		}

		StringBuffer sb = new StringBuffer();
		sb.append(total);
		sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_bo_VendorExpApprove_Approve"));
		MessageManager.getInstance().showInfoMessage(sb.toString());

		return total;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doNullifyVendorExp
	 * (java.util.List)
	 */
	public void doNullifyVendorExp(List<String> applyFormNoList, FunctionCode functionCode) {
		if (CollectionUtils.isEmpty(applyFormNoList)) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo", null) };
			throw new ExpRuntimeException(ErrorCode.A20002, params);
		}

		List<ExpapplC> expapplCList = facade.getExpapplCService().findByApplNo(applyFormNoList);

		if (CollectionUtils.isEmpty(expapplCList)) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_ExpapplC", null) + "List" };
			throw new ExpRuntimeException(ErrorCode.A20002, params);
		}

		this.facade.getGeneralExpService().doDeleteGeneralExp(expapplCList, functionCode);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doReapplyApplyForm
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp)
	 */
	public void doReapplyApplyForm(String expApplNo, FunctionCode functionCode) throws ExpException, ExpRuntimeException {
		Calendar sysDate = Calendar.getInstance();
		if (null == expApplNo || null == functionCode) {
			List<String> errorStringList = new ArrayList<String>();
			if (null == expApplNo) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp"));
			}
			if (null == functionCode) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Function_code"));
			}
			String[] params = { errorStringList.toString() };
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		ExpapplC expapplC = facade.getExpapplCService().findByExpApplNo(expApplNo);
		User user = getLoginUser();
		ApplState applState = this.facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.FIRST_VERIFICATION, SysTypeCode.C);
		if (expapplC.getApplState().getCode().equals(ApplStateCode.APPLICATION_REJECTED.getCode())) {
			// 申請單狀態 設為"審核中"
			expapplC.setApplState(applState);
			// 更新 補辦完成日期
			expapplC.setResendVerifyDate(sysDate);
			// 更新 初審經辦
			expapplC.setVerifyUser(user);
			expapplC.setUpdateDate(sysDate);
			expapplC.setUpdateUser(user);

			// 更新資料
			facade.getExpapplCService().update(expapplC);
			// 記錄「流程簽核歷程」
			this.facade.getFlowCheckstatusService().createByExpApplC(expapplC, functionCode, sysDate);
		} else {
			// 若狀態不為"退件"顯示《狀態錯誤，尚未完成補辦》訊息
			throw new ExpException(ErrorCode.C10020);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doReturnApplyForm
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp)
	 */
	public void doReturnApplyForm(VendorExp exp, FunctionCode functionCode, String returnCauseCode, String returnStatement) throws ExpException, ExpRuntimeException {
		if (null == exp || null == functionCode || StringUtils.isBlank(returnStatement) || StringUtils.isBlank(returnCauseCode)) {
			List<String> errorStringList = new ArrayList<String>();
			if (null == exp) {
				// 廠商費用
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp"));
			}
			if (null == functionCode) {
				// 功能代碼
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Function_code"));
			}
			if (StringUtils.isBlank(returnStatement)) {
				// 退件原因
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ReturnStatement_returnCause"));
			}
			if (StringUtils.isBlank(returnCauseCode)) {
				// 退件代號
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ReturnCause_code"));
			}
			String[] params = { errorStringList.toString() };
			// 傳入的參數{0}錯誤
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		if (StringUtils.equals("99", returnCauseCode)) {
			// 退件代號不可為99
			throw new ExpRuntimeException(ErrorCode.C10059);
		}

		// 系統時間
		Calendar sysDate = Calendar.getInstance();
		User user = getLoginUser();

		// 申請單狀態"退件"
		ApplState applState = facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.APPLICATION_REJECTED, SysTypeCode.C);

		exp.getExpapplC().setApplState(applState);
		exp.getExpapplC().setUpdateDate(sysDate);
		exp.getExpapplC().setUpdateUser(user);
		// 更新廠商費用
		getDao().update(exp);
		// 產生退件原因說明
		ReturnStatement statement = this.facade.getReturnStatementService().getReturnStatement(returnCauseCode, returnStatement);
		// 紀錄流程簽核
		this.facade.getFlowCheckstatusService().createByExpApplC(functionCode, exp.getExpapplC(), statement, sysDate);

	}

	/**
	 * 檢核-憑證附於
	 */
	private void checkProofAdd(VendorExp exp) {
		if (null == exp || null == exp.getExpapplC() || StringUtils.isBlank(exp.getExpapplC().getProofAdd())) {
			return;
		}

		VendorExp vendorExp = findByExpApplNoFetchRelation(exp.getExpapplC().getProofAdd());
		if (null == vendorExp) {
			// 不存在，throw ExpRuntimeExcption，顯示《憑證附於單號錯誤》
			throw new ExpRuntimeException(ErrorCode.C10261);
		}

		if (!StringUtils.equals(exp.getExpapplC().getInvoiceNo(), vendorExp.getExpapplC().getInvoiceNo())) {
			// 發票號碼需與憑證附於之原申請單的發票號碼號相同
			throw new ExpRuntimeException(ErrorCode.C10342, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo"), MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo") });
		}

		if (!StringUtils.equals(exp.getVendor().getVendorCompId(), vendorExp.getVendor().getVendorCompId())) {
			// 廠商統一編號需與憑證附於之原申請單的廠商統一編號相同
			throw new ExpRuntimeException(ErrorCode.C10342, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Vendor_vendorCompId3"), MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Vendor_vendorCompId3") });
		}

	}

	public void doSaveExp(FunctionCode functionCode, VendorExp exp) {
		// 系統時間
		Calendar sysDate = Calendar.getInstance();
		User user = getLoginUser();
		if (null != exp) {

			if (null != exp.getExpapplC()) {
				checkProofAdd(exp);
				if (null == exp.getExpapplC().getEntryGroup() || CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
					List<String> errorStringList = new ArrayList<String>();
					if (null == exp.getExpapplC().getEntryGroup()) {
						errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_EntryGroup"));
					}
					if (null == exp.getExpapplC().getEntryGroup()) {
						errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry"));
					}
					String[] params = { errorStringList.toString() };
					// 傳入的參數[XX]錯誤
					throw new ExpRuntimeException(ErrorCode.A10007, params);
				} else {
					// 檢核資料
					for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
						verifyExp(exp, entry);
					}
					// 設備件預設:false
					exp.setEquipmentItem(false);
					// 費用中分類預設為:A20 廠商費用(一般）
					MiddleType middleType = this.facade.getMiddleTypeService().findByCode(MiddleTypeCode.CODE_A20.getCode());
					// 判斷是否為設備件
					for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
						if (facade.getAccTitleService().isEquipmentItem(entry.getAccTitle())) {
							// 設備件:A10 廠商費用(設備)
							middleType = this.facade.getMiddleTypeService().findByCode(MiddleTypeCode.CODE_A10.getCode());
							exp.setEquipmentItem(true);
							break;
						}
					}
					// set 費用中分類
					exp.getExpapplC().setMiddleType(middleType);

					if (exp.getExpapplC().isNeedTaxRemit() == false && null != exp.getExpapplC().getEntryGroup() && !CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
						for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
							if (StringUtils.equals("63300205", entry.getAccTitle().getCode()) || StringUtils.equals("63300406", entry.getAccTitle().getCode())) {
								exp.getExpapplC().setNeedTaxRemit(true);
								break;
							}
						}
					}

					if (false == exp.getExpapplC().isWithholdIncome()) {
						exp.getExpapplC().setInvoiceNoneTaxAmt(BigDecimal.ZERO);
						exp.getExpapplC().setInvoiceTaxAmt(BigDecimal.ZERO);
						exp.getExpapplC().setIncomeAmt(BigDecimal.ZERO);
					}

					exp.getExpapplC().setRelationVendorCompId(exp.getVendor().getVendorCompId());
					// 檢核預付性質費用的必輸入欄位
					this.facade.getExpapplCService().checkPrepaymentAccTtleColumns(exp.getExpapplC());

					// 檢核費用申請單的進項稅相關欄位資訊
					this.facade.getExpapplCService().checkInvoiceData(exp.getExpapplC());

					// 產生應付費用科目帳務資料(分錄)
					this.facade.getExpapplCService().generatePayableExpenseEntry(exp.getExpapplC(), null);

					// 累加相同借貸別的"應附費用科目"
					this.facade.getEntryService().calculateEntryies(exp.getExpapplC().getEntryGroup().getEntries());

					// 借貸是否平衡
					this.facade.getEntryGroupService().calcBalance(exp.getExpapplC().getEntryGroup());
				}

				this.facade.getApplInfoService().create(exp.getExpapplC().getApplyUserInfo());
				if (null != exp.getExpapplC().getDrawMoneyUserInfo()) {
					this.facade.getApplInfoService().create(exp.getExpapplC().getDrawMoneyUserInfo());
				}

				if (null == exp.getExpapplC().getListType() || StringUtils.isEmpty(exp.getExpapplC().getListType().getCode())) {
					exp.getExpapplC().setListType(null);
				}

				// 產生單號
				exp.getExpapplC().setExpApplNo(this.facade.getExpapplCService().generateExpApplNo(exp.getExpapplC().getMiddleType().getCode()));
				exp.getExpapplC().setCreateDate(sysDate);
				exp.getExpapplC().setCreateUser(user);

			} else {
				throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC") });
			}

			// set進項稅=憑證金額(稅)
			exp.getExpapplC().setIncomeAmt(exp.getExpapplC().getInvoiceTaxAmt());

			// 計算憑證金額(未) = 實付金額-進項稅
			exp.getExpapplC().setInvoiceNoneTaxAmt(exp.getExpapplC().getRealityAmt().subtract(exp.getExpapplC().getIncomeAmt()));

			// 檢核與合併相同的銷帳碼金額
			checkCancelCodeAmt(exp);
			// 檢核行政費用申請單資料內容是否正確，包含各種規則
			this.facade.getExpapplCService().verifyExpapplC(exp.getExpapplC(), exp.isCarriedByStages());

			this.getDao().create(exp);

			generateVendorExp(functionCode, exp, sysDate);

		}

	}

	/**
	 * 檢核銷帳碼金額
	 * 
	 * @param exp
	 */
	private void checkCancelCodeAmt(VendorExp exp) {
		if (null == exp || null == exp.getExpapplC() || null == exp.getExpapplC().getEntryGroup() || CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
			return;
		}

		// 只處理分期付款件或者分期結轉
		if (!(exp.isInstallment() || exp.isCarriedByStages())) {
			return;
		}

		List<Entry> cancelCodeEntrise = new ArrayList<Entry>();
		List<Entry> entrise = new ArrayList<Entry>();

		// 篩選銷帳碼與非銷帳碼科目
		for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
			Entry tempEntry = new Entry();
			BeanUtils.copyProperties(entry, tempEntry);
			if (StringUtils.isNotBlank(entry.getCancelCode())) {
				cancelCodeEntrise.add(tempEntry);
			} else {
				entrise.add(tempEntry);
			}
		}

		if (CollectionUtils.isEmpty(cancelCodeEntrise)) {
			// 無銷帳碼的分錄所以就不用處理了
			return;
		}

		// 貸方銷帳碼Map
		Map<String, BigDecimal> cancelCodeMap = new HashMap<String, BigDecimal>();
		for (Entry entry : cancelCodeEntrise) {
			if (EntryTypeValueCode.D.equals(EntryTypeValueCode.getByValue(entry.getEntryType()))) {
				continue;
			}

			if (!cancelCodeMap.keySet().contains(entry.getCancelCode())) {
				cancelCodeMap.put(entry.getCancelCode(), BigDecimal.ZERO);
			}
			// 累加相同銷帳碼金額
			cancelCodeMap.put(entry.getCancelCode(), cancelCodeMap.get(entry.getCancelCode()).add(entry.getAmt()));
		}

		// 檢核銷帳碼是否有超出
		for (String cancelCode : cancelCodeMap.keySet()) {
			if (facade.getEntryService().getCancelCodeBalance(cancelCode, exp.getExpapplC().getExpApplNo()).compareTo(cancelCodeMap.get(cancelCode)) < 0) {
				// 超出餘額顯示銷帳碼累計申請金額已超過原銷帳碼金額
				throw new ExpRuntimeException(ErrorCode.C10436, new String[] { cancelCode });
			}
		}
	}

	/**
	 * 處理名冊狀態&產生過度付款明細&記錄一筆流程簽核歷程&設定費用明細與申請單之間的關聯
	 * 
	 * @param functionCode
	 *            功能代碼
	 * @param exp
	 *            廠商費用
	 * @param sysDate
	 *            系統日
	 */
	private void generateVendorExp(FunctionCode functionCode, VendorExp exp, Calendar sysDate) {
		// 處理名冊狀態(新增)
		if (null != exp.getExpapplC().getListType()) {
			List<String> list = new ArrayList<String>();
			if (StringUtils.isNotBlank(exp.getExpapplC().getListNo1())) {
				list.add(exp.getExpapplC().getListNo1());
			} else {
				throw new ExpRuntimeException(ErrorCode.A20001);
			}

			if (StringUtils.isNotBlank(exp.getExpapplC().getListNo2())) {
				list.add(exp.getExpapplC().getListNo2());
			}

			if (ListTypeCode.PREMIUM_AWARD.equals(ListTypeCode.getByValue(exp.getExpapplC().getListType()))) {
				this.facade.getExpapplCService().updateRosterState(0, exp.getExpapplC().getExpApplNo(), list);
			}
		}

		// 檢核行政費用申請單資料內容是否正確，包含各種規則
		this.facade.getExpapplCService().verifyExpapplC(exp.getExpapplC(), exp.isCarriedByStages());

		// 設定費用明細與申請單之間的關聯 2010/03/03 By Eustace
		facade.getExpapplCService().updateExpapplCDetail(exp.getExpapplC());

		// 產生過渡付款明細
		this.generateTransitPaymentDetail(exp);
		// 記錄一筆流程簽核歷程
		this.facade.getFlowCheckstatusService().createByExpApplC(exp.getExpapplC(), functionCode, sysDate);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doUpdateExp(tw.com
	 * .skl.exp.kernel.model6.bo.Function.FunctionCode,
	 * tw.com.skl.exp.kernel.model6.bo.VendorExp, java.util.List)
	 */
	public void doUpdateExp(FunctionCode functionCode, VendorExp exp, List<Entry> deleteEntryList, List<String> listNos) {
		// 系統時間
		Calendar sysDate = Calendar.getInstance();
		User user = getLoginUser();
		if (null != exp) {

			ExpapplC expapplC = null;

			if (null != exp.getExpapplC()) {
				checkProofAdd(exp);

				expapplC = exp.getExpapplC();

				// 處理要刪除的分錄
				this.facade.getExpapplCService().toDelEntrys(expapplC, deleteEntryList);

				// 檢核資料
				for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
					verifyExp(exp, entry);
				}

				if (false == exp.getExpapplC().isWithholdIncome()) {
					exp.getExpapplC().setInvoiceNoneTaxAmt(BigDecimal.ZERO);
					exp.getExpapplC().setInvoiceTaxAmt(BigDecimal.ZERO);
					exp.getExpapplC().setIncomeAmt(BigDecimal.ZERO);
				}

				boolean isNeedTaxRemit = false;
				for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
					if (StringUtils.equals("63300205", entry.getAccTitle().getCode()) || StringUtils.equals("63300406", entry.getAccTitle().getCode())) {
						isNeedTaxRemit = true;
						break;
					}
				}
				exp.getExpapplC().setNeedTaxRemit(isNeedTaxRemit);
				exp.getExpapplC().setRelationVendorCompId(exp.getVendor().getVendorCompId());
				// 檢核預付性質費用的必輸入欄位
				this.facade.getExpapplCService().checkPrepaymentAccTtleColumns(exp.getExpapplC());

				// 檢核費用申請單的進項稅相關欄位資訊
				this.facade.getExpapplCService().checkInvoiceData(exp.getExpapplC());

				// 產生應付費用科目帳務資料(分錄)
				this.facade.getExpapplCService().generatePayableExpenseEntry(exp.getExpapplC(), null);

				// 累加相同借貸別的"應附費用科目"
				this.facade.getEntryService().calculateEntryies(exp.getExpapplC().getEntryGroup().getEntries());

				// 借貸是否平衡
				this.facade.getEntryGroupService().calcBalance(exp.getExpapplC().getEntryGroup());

				List<Entry> entryList = expapplC.getEntryGroup().getEntries();
				exp.getExpapplC().setExpapplCDetails(null);
				for (Entry entry : entryList) {
					if (null != entry.getExpapplCDetail()) {
						entry.getExpapplCDetail().setExpapplC(expapplC);
					}
				}

				// 更新 申請人資訊
				this.facade.getApplInfoService().update(expapplC.getApplyUserInfo());

				if (null != expapplC.getDrawMoneyUserInfo()) {
					// 更新 領款人資訊
					this.facade.getApplInfoService().update(expapplC.getDrawMoneyUserInfo());
				}

				// 設定更新的系統時間
				expapplC.setUpdateDate(sysDate);
				// 設定更新的人員
				expapplC.setUpdateUser(user);
			}

			// set進項稅=憑證金額(稅)
			exp.getExpapplC().setIncomeAmt(exp.getExpapplC().getInvoiceTaxAmt());
			// 計算憑證金額(未) = 實付金額-進項稅
			exp.getExpapplC().setInvoiceNoneTaxAmt(exp.getExpapplC().getRealityAmt().subtract(exp.getExpapplC().getIncomeAmt()));
			// 儲存時，須產生20210342科目之銷號碼
			// generateCancelCodeByEzfly(exp);
			// 檢核行政費用申請單資料內容是否正確，包含各種規則
			this.facade.getExpapplCService().verifyExpapplC(exp.getExpapplC(), exp.isCarriedByStages());

			// 檢核銷帳碼金額
			checkCancelCodeAmt(exp);

			facade.getEntryService().doCreateEntry(exp.getExpapplC().getEntryGroup().getEntries());
			// 更新 廠商費用
			exp = this.getDao().update(exp);

			if (ListTypeCode.PREMIUM_AWARD.equals(ListTypeCode.getByValue(exp.getExpapplC().getListType()))) {
				// 變更名冊狀態(修改申請單)
				if (!CollectionUtils.isEmpty(listNos)) {
					this.facade.getExpapplCService().updateRosterState(1, exp.getExpapplC().getExpApplNo(), listNos);
				}
			}

			generateVendorExp(functionCode, exp, sysDate);

		}
	}

	public List<VendorExpApproveDto> findForApproveDto(MiddleType middleType, String payYearMonth, String invoiceNo, String vendorCompId, String userId, String expApplNo, String firstVerifyUserId, String sendItemNo, Boolean isTemporaryPayment, ApplStateCode[] states) {

		return getDao().findForApproveDto(middleType, payYearMonth, invoiceNo, vendorCompId, userId, expApplNo, firstVerifyUserId, sendItemNo, isTemporaryPayment, states);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#findForApprove(tw
	 * .com.skl.exp.kernel.model6.bo.MiddleType, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.Boolean, java.util.Calendar,
	 * tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode[])
	 */
	public List<VendorExp> findForApprove(MiddleType middleType, String payYearMonth, String invoiceNo, String vendorCompId, String userId, String expApplNo, String firstVerifyUserId, Boolean isTemporaryPayment, Calendar returnDate, ApplStateCode[] states) {

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" left join fetch vendorExp.vendor");
		queryString.append(" left join vendorExp.expapplC expapplC");
		queryString.append(" left join fetch expapplC.deliverDaylist");
		boolean truncated = false;

		Map<String, Object> params = new HashMap<String, Object>();

		// 費用中分類
		if (null != middleType && StringUtils.isNotBlank(middleType.getCode())) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.middleType.code =:middleTypeCode");
			params.put("middleTypeCode", middleType.getCode());

			queryString.append(" and");
		}

		// 付款年月
		if (StringUtils.isNotBlank(payYearMonth)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.payYearMonth =:payYearMonth");
			params.put("payYearMonth", payYearMonth);

			queryString.append(" and");
		}

		// 發票號碼
		if (StringUtils.isNotBlank(invoiceNo)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.invoiceNo =:invoiceNo");
			params.put("invoiceNo", invoiceNo);

			queryString.append(" and");
		}

		// 廠商統一編號
		if (StringUtils.isNotBlank(vendorCompId)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.vendor.vendorCompId =:vendorCompId");
			params.put("vendorCompId", vendorCompId);

			queryString.append(" and");
		}

		// 申請人員工代號 ApplInfo.userId
		if (StringUtils.isNotBlank(userId)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.applyUserInfo.userId =:userId");
			params.put("userId", userId);

			queryString.append(" and");
		}

		// 申請單號 ExpapplC.expApplNo
		if (StringUtils.isNotBlank(expApplNo)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.expApplNo =:expApplNo");
			params.put("expApplNo", expApplNo);

			queryString.append(" and");
		}

		// 初審經辦的員工代號 vendorExp.expapplC.verifyUser.code
		if (StringUtils.isNotBlank(firstVerifyUserId)) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.verifyUser.code =:firstVerifyUserId");
			params.put("firstVerifyUserId", firstVerifyUserId);

			queryString.append(" and");
		}

		// 是否為臨時付款
		if (null != isTemporaryPayment) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}

			queryString.append(" vendorExp.expapplC.temporaryPayment =:isTemporaryPayment");
			params.put("isTemporaryPayment", isTemporaryPayment.booleanValue());

			queryString.append(" and");
		}

		// 申請單狀態
		if (null != states && states.length > 0) {
			if (!truncated) {
				queryString.append(" where");
				truncated = true;
			}
			queryString.append(" vendorExp.expapplC.applState.code in(");

			for (int index = 0; index < states.length; index++) {

				if (index != 0) {
					queryString.append(" , ");
				}

				String str = "applStateCode" + index;

				queryString.append(":" + str);

				params.put(str, states[index].getCode());
			}

			queryString.append(") ");

			queryString.append(" and");
		}

		if (truncated) {
			// 刪除最後一個and字串
			queryString.delete(queryString.lastIndexOf("and"), queryString.length());
		}

		queryString.append(" order by vendorExp.expapplC.middleType.code" + ", vendorExp.generalMgrSn");

		List<VendorExp> list = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(list)) {
			return list;
		} else {
			return null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.VendorExpService#
	 * isInvoiceAmtEqualsRealityAmt(tw.com.skl.exp.kernel.model6.bo.VendorExp)
	 */
	public boolean isInvoiceAmtEqualsRealityAmt(VendorExp exp) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#updateExpapplCDetail
	 * (tw.com.skl.exp.kernel.model6.bo.VendorExp, java.lang.Integer,
	 * tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail)
	 */
	public void updateExpapplCDetail(VendorExp exp, Integer index, ExpapplCDetail expDetail) throws ExpException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#verifyExp(tw.com.
	 * skl.exp.kernel.model6.bo.VendorExp,
	 * tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail)
	 */
	public void verifyExp(VendorExp exp, Entry entry) {

		if (null == exp || null == entry) {
			List<String> errorStringList = new ArrayList<String>();
			if (null == exp) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp"));
			}
			if (null == entry) {
				errorStringList.add(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry"));
			}

			String[] params = { errorStringList.toString() };
			// 傳入的參數{0}錯誤
			throw new ExpRuntimeException(ErrorCode.A10007, params);
		}

		verifyEntry(entry);

		// 若輸入之科目代號為「62100023」需檢核「員工代號」為必填欄位，若空白，則顯示《員工代號為必填立》訊息
		if ("62100023".equals(entry.getAccTitle().getCode())) {
			// Defect: 912 URS文件在1.0時有需要判斷,但在新版時移除卻沒有告知變更
			// //檢核「員工代號」為必填欄位，若空白，則顯示《員工代號為必填立》訊息，則不可儲存入檔。
			// if (null == entry.getExpapplCDetail() ||
			// StringUtils.isBlank(entry.getExpapplCDetail().getStaffId())) {
			// //顯示《員工代號為必填立》訊息
			// throw new ExpRuntimeException(ErrorCode.C10034);
			// }

			/*
			 * 若「廠商費用.代墊廠商統編」不是空值，則查詢此統編是否存在「廠商費用」中。
			 * 若不存在，則顯示《代墊廠商統編需於UC10.7.1中建檔!》，且不可儲存入檔。
			 * (系統產生廠商付款明細，實際付款對象為「代墊廠商統編
			 * 」，非頁面上輸入之「廠商統一編號」，代墊付款廠商僅影響付款明細，若有稅額資料以UC1.5.9「廠商統編」欄位產生進項稅或所得稅。)
			 */
			if (StringUtils.isNotBlank(exp.getPadVendorCompId())) {
				List<VendorExp> list = this.findForVendorExpapplFetchRelation(null, null, null, exp.getPadVendorCompId(), null, null, null, null, null);
				if (CollectionUtils.isEmpty(list)) {
					// 若不存在 顯示《代墊廠商統編需於UC10.7.1中建檔!》
					throw new ExpRuntimeException(ErrorCode.C10035);
				}
			}
		}

		// 檢查發票字軌等等
		this.facade.getExpapplCService().checkInvoiceLegality(exp.getExpapplC());

		// 若為成本別為"W"，則執行W件檢核
		this.facade.getExpapplCService().handleCaseW(exp.getExpapplC());

		// “分期付款件結轉”=Y時，”分期付款”欄位值不可設為”Y”
		if (exp.isCarriedByStages() && exp.isInstallment()) {
			throw new ExpRuntimeException(ErrorCode.C10046);
		}

		// 檢查廠商是否存在
		if (null == exp.getVendor() || !facade.getVendorService().checkVendorCompId(exp.getVendor().getVendorCompId())) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Vendor_vendorCompId") });
		}

		// 檢核廠商合約編號是否存在
		if (StringUtils.isNotBlank(exp.getVendorContractCode())) {
			facade.getVendorContractService().checkContractNo(exp.getVendorContractCode());
		}

		if (StringUtils.isBlank(exp.getGeneralMgrSn())) {
			// 經辦序號不可為空值
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp_generalMgrSn") });
		}

		if (StringUtils.isBlank(exp.getPayYearMonth())) {
			// 付款年月不可為空值
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp_payYearMonth") });
		}

		// TODO
		// 輸入借方明細時，若科目代號為"61440123(餐費)、62100023(交際費)"時，須檢核成本單位代號之"餐費、交際費"預算項目不可超支，
		// 若申請金額超過可申請限額，則顯示已超支且不可儲存送出申請，若有輸入”專案代號”時，則不在此限。

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.VendorExpService#
	 * calculateVerifyUserInputVendorExpCount(java.lang.String,
	 * java.lang.String)
	 */
	public VendorExp findLastUserInputVendorExp(String createUserCode, String payYearMonth) {

		if (StringUtils.isBlank(createUserCode) || StringUtils.isBlank(payYearMonth)) {
			return null;
		}

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" where vendorExp.generalMgrSn like :sn");
		queryString.append(" order by vendorExp.generalMgrSn desc");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("sn", StringUtils.upperCase(StringUtils.trim(createUserCode)) + StringUtils.trim(payYearMonth) + "%");

		List<VendorExp> vendorExp = getDao().findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(vendorExp)) {
			return null;
		} else {
			return vendorExp.get(0);
		}
	}

	public VendorExp findByExpApplNoFetchRelation(String expApplNo) {
		if (StringUtils.isBlank(expApplNo)) {
			return null;
		}

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" left join vendorExp.vendor vendor");
		queryString.append(" left join fetch vendorExp.vendor");
		queryString.append(" left join fetch vendor.vendorContracts");
		queryString.append(" left join fetch vendorExp.routeType");
		queryString.append(" left join vendorExp.insurAgent insurAgent");
		queryString.append(" left join fetch vendorExp.insurAgent");
		queryString.append(" left join fetch insurAgent.routeType");
		queryString.append(" left join vendorExp.expapplC expapplC");
		queryString.append(" left join fetch vendorExp.expapplC");
		queryString.append(" left join expapplC.expItem expItem");
		queryString.append(" left join fetch expapplC.expItem");
		queryString.append(" left join fetch expItem.budgetItem");
		queryString.append(" left join fetch expapplC.applyUserInfo");
		queryString.append(" left join fetch expapplC.drawMoneyUserInfo");
		queryString.append(" left join fetch expapplC.deliverDaylist");
		queryString.append(" left join fetch expapplC.dailyStatement");
		// queryString.append(" left join fetch vendorExp.expapplC.subpoena");
		queryString.append(" left join fetch expapplC.verifyUser");
		queryString.append(" left join fetch expapplC.actualVerifyUser");
		queryString.append(" inner join expapplC.createUser createUser");
		queryString.append(" inner join fetch expapplC.createUser");
		queryString.append(" inner join fetch createUser.department");
		queryString.append(" left join fetch expapplC.updateUser");
		queryString.append(" left join fetch expapplC.expapplCDetails");
		queryString.append(" left join expapplC.entryGroup entryGroup");
		queryString.append(" left join fetch expapplC.entryGroup");
		queryString.append(" inner join fetch entryGroup.entries");
		queryString.append(" left join entryGroup.entries entry");
		queryString.append(" left join fetch entry.entryGroup");
		queryString.append(" left join fetch entry.accTitle");
		queryString.append(" left join fetch entry.subpoena");
		queryString.append(" left join fetch entry.subpoenaD");
		queryString.append(" left join fetch entry.expapplCDetail");
		queryString.append(" where vendorExp.expapplC.expApplNo =:expApplNo");

		Map<String, Object> params = new HashMap<String, Object>();
		// 申請單號
		params.put("expApplNo", expApplNo);

		// 過濾掉刪除(99)
		queryString.append(" and expapplC.applState.code <>:applStateCode");
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		List<VendorExp> vendorExp = getDao().findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(vendorExp)) {
			return null;
		} else {
			VendorExp exp = vendorExp.get(0);
			facade.getExpapplCService().doSortEntry(exp.getExpapplC());
			return exp;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.VendorExpService#
	 * findByDeliverDaylistNoFetchVendor(java.lang.String)
	 */
	public List<VendorExp> findByDeliverDaylistNoFetchVendor(String deliverDaylistNo) throws ExpException {
		if (StringUtils.isBlank(deliverDaylistNo)) {
			return null;
		}

		List<VendorExp> vendorExpList = this.findForVendorExpapplFetchRelation(null, null, null, null, null, null, null, null, deliverDaylistNo);

		if (CollectionUtils.isEmpty(vendorExpList)) {
			return null;
		} else {
			return vendorExpList;
		}
	}

	public List<VendorExpDaylistDetailDto> findVendorExpDaylistDetailDtoByDeliverDaylistNo(String deliverDaylistNo) throws ExpException {
		if (StringUtils.isBlank(deliverDaylistNo)) {
			String[] params = { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_DeliverDaylist_deliverNo", null) };
			// 找不到分錄群組
			throw new ExpException(ErrorCode.A20002, params);
		}

		return this.getDao().findVendorExpDaylistDetailDtoByDeliverDaylistNo(deliverDaylistNo);
	}

	private void generateTransitPaymentDetail(VendorExp exp) {
		/*
		 * modify...2009/8/25, By Eustace 產生過渡付款明細規則  預設以「廠商費用.廠商」欄位產生過渡付款明細 
		 * 若「廠商費用.代墊廠商統編」不為空值時，以此欄位資料查詢「廠商」資料表。並以此廠商之金融帳號為付款帳號。(#107) 
		 * 若此代墊廠商統編不存在「廠商」資料表，throw ExpRuntimeExcept，顯示”查無代墊廠商統編”
		 */
		Vendor vendor = null;
		if (StringUtils.isNotBlank(exp.getPadVendorCompId())) {
			// 檢核代墊廠商統編
			if (!this.facade.getVendorService().checkVendorCompId(exp.getPadVendorCompId())) {
				throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp_padVendorCompId") });
			}
			vendor = facade.getVendorService().findByVendorCompIdFetchVendorContract(exp.getPadVendorCompId());
		} else {
			vendor = exp.getVendor();
		}

		this.facade.getTransitPaymentDetailService().generateVendorTransitPaymentDetail(exp.getExpapplC(), vendor);

	}

	public void checkInvoice(String invoiceNo, Calendar invoiceDate, String expApplNo) {
		// 檢核發票號碼
		if (StringUtils.isBlank(invoiceNo)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo") });
		}
		// 檢核發票日期
		if (null == invoiceDate) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceDate") });
		}

		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" where");
		queryString.append("  vendorExp.expapplC.invoiceNo =:invoiceNo " + " and vendorExp.expapplC.invoiceDate >=:invoiceDateStart" + " and vendorExp.expapplC.invoiceDate <:invoiceDateEnd" + " and vendorExp.expapplC.applState.code <>:applStateCode");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("invoiceNo", invoiceNo);

		Calendar[] range = DateUtils.getCalendarRange(invoiceDate, 1);
		params.put("invoiceDateStart", range[0]);
		params.put("invoiceDateEnd", range[1]);
		params.put("applStateCode", ApplStateCode.DELETED.getCode());

		if (StringUtils.isNotBlank(expApplNo)) {
			queryString.append(" and vendorExp.expapplC.expApplNo <>:expApplNo");
			params.put("expApplNo", expApplNo);
		}

		List<VendorExp> vendorExp = getDao().findByNamedParams(queryString.toString(), params);

		if (!CollectionUtils.isEmpty(vendorExp)) {
			// 重覆時，顯示《發票號碼重覆》訊息
			throw new ExpRuntimeException(ErrorCode.A10022);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.exp.kernel.model6.logic.VendorExpService#
	 * findApplyAmtByVendorContractCode(java.lang.String,
	 * tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode)
	 */
	public BigDecimal findApplyAmtByVendorContractCode(String vendorContractCode, ApplStateCode applStateCode) throws ExpException, ExpRuntimeException {
		if (StringUtils.isBlank(vendorContractCode)) {
			throw new ExpRuntimeException(ErrorCode.A10020);
		}

		return this.getDao().findApplyAmtByVendorContractCode(vendorContractCode, applStateCode);
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
	private List<Entry> generateWithholdIncome(ExpapplC expapplC, Entry entry, List<Entry> entryList) {
		if (!EntryTypeCode.TYPE_2_3.getValue().equals(entry.getEntryType().getValue())) {
			// 不是借方跳過
			return entryList;
		}

		if (expapplC.isWithholdIncome()) {
			entryList = new ArrayList<Entry>();
			// 費用科目
			AccTitle expAccTitle = entry.getAccTitle();

			// 憑證金額(含)
			BigDecimal invoiceAmt = expapplC.getInvoiceAmt();
			// 借方
			EntryType entryTypeD = this.facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_3);
			// 貸方
			EntryType entryTypeC = this.facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_4);

			if (expapplC.isHadAllowanceSlip()) {
				// 有折讓

				BigDecimal stampAmt = expapplC.getStampAmt();// 印花稅額
				BigDecimal taxAmt = expapplC.getTaxAmt();// 所得稅額

				// 計算實付金額 :實付金額= 憑證金額-印花稅額-所得稅額
				BigDecimal realityAmt = invoiceAmt.subtract(stampAmt).subtract(taxAmt);
				// set實付金額
				expapplC.setRealityAmt(realityAmt);

				// 金額大於0 則產生貸方折讓分錄(進項)
				if (BigDecimal.ZERO.compareTo(expapplC.getAllowanceTaxAmt()) < 0) {
					Entry withholdIncomeEntryForC = new Entry();
					AccTitle title = facade.getAccTitleService().findByCode(expAccTitle.getCode());
					if (null == title.getVat()) {
						throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_vat") });
					}
					withholdIncomeEntryForC.setAccTitle(title.getVat());
					withholdIncomeEntryForC.setEntryType(entryTypeC);
					withholdIncomeEntryForC.setAmt(expapplC.getAllowanceTaxAmt());
					entryList.add(withholdIncomeEntryForC);
				}

				// //金額大於0 則產生貸方折讓分錄(費用)
				// if
				// (BigDecimal.ZERO.compareTo(expapplC.getAllowanceNoneTaxAmt())
				// < 0) {
				// Entry expEntry = new Entry();
				// expEntry.setAccTitle(expAccTitle);
				// expEntry.setEntryType(entryTypeC);
				// expEntry.setAmt(expapplC.getAllowanceNoneTaxAmt());
				// entryList.add(expEntry);
				// }

			}

			// 金額大於0 則產生借方分錄(進項)
			if (BigDecimal.ZERO.compareTo(expapplC.getInvoiceTaxAmt()) < 0) {
				Entry withholdIncomeEntryForD = new Entry();
				AccTitle title = facade.getAccTitleService().findByCode(expAccTitle.getCode());
				if (null == title.getVat()) {
					throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_AccTitle_vat") });
				}
				withholdIncomeEntryForD.setAccTitle(title.getVat());
				withholdIncomeEntryForD.setEntryType(entryTypeD);
				withholdIncomeEntryForD.setAmt(expapplC.getInvoiceTaxAmt());
				entryList.add(withholdIncomeEntryForD);
			}

		}
		return entryList;
	}

	public List<VendorExp> findByExpApplNoList(List<String> expApplNoList) {
		if (CollectionUtils.isEmpty(expApplNoList)) {
			return null;
		}
		StringBuffer queryString = new StringBuffer();
		queryString.append("select distinct vendorExp");
		queryString.append(" from VendorExp vendorExp");
		queryString.append(" where vendorExp.expapplC.expApplNo in(");

		Map<String, Object> params = new HashMap<String, Object>();

		for (int index = 0; index < expApplNoList.size(); index++) {

			if (index != 0) {
				queryString.append(" , ");
			}

			String str = "expApplNo" + index;

			queryString.append(":" + str);

			params.put(str, expApplNoList.get(index));
		}

		queryString.append(")");

		queryString.append(" order by vendorExp.generalMgrSn");

		List<VendorExp> vendorExpList = getDao().findByNamedParams(queryString.toString(), params);

		if (CollectionUtils.isEmpty(vendorExpList)) {
			return null;
		} else {
			return vendorExpList;
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

	public List<VendorExpMonthCloseDto> findForVendorExpMonthClose(String payYearMonth, Calendar expectRemitDate) {
		List<VendorExpMonthCloseDto> veList = getDao().findForVendorExpMonthClose(payYearMonth, expectRemitDate);
		StringBuffer querySql = new StringBuffer();
		querySql.append("select vexp from VendorExp vexp" + " join vexp.expapplC eac" + " join eac.middleType mt" + " join mt.bigType bt" + " where eac.inCloseTemporaryPayment='1' and eac.applState.code='70'" + " and bt.code='01'" + " and vexp.payYearMonth=:payYM and eac.expectRemitDate=:erDate");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("payYM", payYearMonth);
		params.put("erDate", expectRemitDate);
		if (getDao().findByNamedParams(querySql.toString(), params).size() > 0) {
			throw new ExpRuntimeException(ErrorCode.C10086);
		}
		return veList;
	}

	/**
	 * <p>
	 * 產生送匯批次記錄，共用方法 for C 5.1.1廠商月結 & C 5.1.2廠商內結臨付
	 * </P>
	 * 
	 * @param sysDate
	 *            系統時間
	 * @return paymentBatch 送匯批次記錄
	 */
	private PaymentBatch genPaymentBatch(Calendar sysDate) {
		// 要產生送匯批次記錄
		PaymentBatch paymentBatch = new PaymentBatch();
		// 送匯批次記錄批號
		SNGenerator gen = AbstractSNGenerator.getInstance(PaymentBatchNoGenerator.class.getName(), facade.getSequenceService());
		Map<String, String> params = new HashMap<String, String>();
		params.put("param1", null);
		String sn = gen.getSerialNumber(params);
		paymentBatch.setSysType(facade.getSysTypeService().findBySysTypeCode(SysTypeCode.C));
		paymentBatch.setBatchRemitNo(sn);
		paymentBatch.setFlowRemitDate(sysDate);
		paymentBatch.setDownloadDate(null);
		// 要記得存更新日期和人員
		paymentBatch.setCreateDate(sysDate);
		paymentBatch.setCreateUserId(getLoginUser().getIdentityId());
		// 因要要傳給付款明細用，所以要先把資料存到DB，才會產生id
		facade.getPaymentBatchService().create(paymentBatch);

		return paymentBatch;
	}

	/**
	 * 暫存待修改的過渡付款明細。
	 * 
	 * @author JacksonLee
	 * 
	 */
	class ToUpdateTransitPaymentDetail {

		private TransitPaymentDetail tpd;

		private String cancelCode;

		private Vendor vendor;

		public ToUpdateTransitPaymentDetail(TransitPaymentDetail tpd, String cancelCode, Vendor vendor) {
			this.tpd = tpd;
			this.cancelCode = cancelCode;
			this.vendor = vendor;
		}

		public TransitPaymentDetail getTpd() {
			return tpd;
		}

		public String getCancelCode() {
			return cancelCode;
		}

		public Vendor getVendor() {
			return vendor;
		}

	}

	/**
	 * 暫存待修改的分錄資料。
	 * 
	 * @author JacksonLee
	 * 
	 */
	class ToUpdateEntry {
		private Entry entry;

		private String cancelCode;

		public ToUpdateEntry(Entry entry, String cancelCode) {
			this.entry = entry;
			this.cancelCode = cancelCode;
		}

		public Entry getEntry() {
			return entry;
		}

		public String getCancelCode() {
			return cancelCode;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.exp.kernel.model6.logic.VendorExpService#doMonthlyClose(java
	 * .util.List)
	 */
	public void doMonthlyClose(List<String> subpoenaNoList, Calendar accountDate) throws ExpException {
		Calendar sysDate = Calendar.getInstance();
		// 2009/11/12,TIM,修改：設定大分錄的SerNo-->2009/11/28,一個日結單代傳票裡的大分錄序號，不能重複
		int bigEnSerNo = 0;
		// 2009/12/10,全部要產生的資料，只有一筆新的日結單代傳票，和(最多)3筆大分錄
		BigEntry tempDebitBigEn = null; // 借方: 應付費用 - 總務費用
		BigEntry tempCreditRemitBigEn = null; // 貸: 應付費用 - 待匯
		BigEntry tempCreditCheckBigEn = null; // 貸: 應付費用 -待開
		// 要放上面幾個東西，擺個Map
		Map<String, Object> dataMap = new HashMap<String, Object>();
		List<ExpapplC> eacNeedUpdateList = new ArrayList<ExpapplC>();

		sleepCounter = 0;

		// 暫存所有待儲存的過渡付款明細
		List<ToUpdateTransitPaymentDetail> allToSaveTpds = new ArrayList<ToUpdateTransitPaymentDetail>();

		// 暫存所有待儲存的分錄資料
		List<ToUpdateEntry> allToSaveEntries = new ArrayList<ToUpdateEntry>();

		// 2009/12/10,把外面傳進來的日結單代傳票資料全部處理完(Loop跑完)，才處理s大分錄，和新產生的日結單代傳票
		for (String subpoenaNo : subpoenaNoList) {
			// 廠商月結，是由內結接下來的步驟，所以應該已有日結單代傳票的資料了
			Subpoena subp = facade.getSubpoenaService().findSubpoenaBySubpoenaNo(subpoenaNo);
			List<ExpapplC> eacList = facade.getExpapplCService().findExpapplCsWithoutExpapplCDetailBySubpoena(subp);

			vendorMonthlyCloseThreadSleep(subpoenaNo, 500); // 2010-07-29

			// 抽出廠商月結的method
			// 重覆執行Loop，直到原日結單代傳票包含的所有費用申請單均處理完畢
			for (ExpapplC eac : eacList) {
				if (eac.isTemporaryNoPayment() || eac.isInCloseTemporaryPayment()) {
					continue;
				}
				if (sleepCounter % 100 == 0) {
					vendorMonthlyCloseThreadSleep(subpoenaNo + ", " + eac.getExpApplNo(), 500); // 2010-07-29
				}
				sleepCounter++;
				dataMap = doProcessExpapplCForMonthClose1(eac, tempDebitBigEn, tempCreditRemitBigEn, tempCreditCheckBigEn, sysDate, allToSaveEntries, allToSaveTpds);
				tempDebitBigEn = (BigEntry) dataMap.get("tempDebitBigEn");
				tempCreditRemitBigEn = (BigEntry) dataMap.get("tempCreditRemitBigEn");
				tempCreditCheckBigEn = (BigEntry) dataMap.get("tempCreditCheckBigEn");
				allToSaveEntries = (List<ToUpdateEntry>) dataMap.get("allToSaveEntries");
				allToSaveTpds = (List<ToUpdateTransitPaymentDetail>) dataMap.get("allToSaveTpds");
				eacNeedUpdateList.add((ExpapplC) dataMap.get("eac"));
			}
		}

		// 產生一筆新的日結單代傳票
		// RE201002676改為依作帳日期產生傳票
		Subpoena newSubp = genSubpoena(BigTypeCode.VENDOR_EXP.getCode(), accountDate);

		// 要產生送匯批次記錄
		PaymentBatch paymentBatch = genPaymentBatch(sysDate);

		// 儲存新產生的「日結單代傳票」及暫存的「大分錄」資料(最多三筆)
		List<BigEntry> bigEnList = new ArrayList<BigEntry>();
		// 2009/12/11,照順序排20210391、20210392、20210360
		if (tempDebitBigEn != null && tempDebitBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempDebitBigEn.setSubpoena(newSubp);
			tempDebitBigEn = facade.getBigEntryService().create(tempDebitBigEn);
			bigEnList.add(tempDebitBigEn);
		}
		if (tempCreditRemitBigEn != null && tempCreditRemitBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempCreditRemitBigEn.setSubpoena(newSubp);
			tempCreditRemitBigEn = facade.getBigEntryService().create(tempCreditRemitBigEn);
			bigEnList.add(tempCreditRemitBigEn);
		}
		if (tempCreditCheckBigEn != null && tempCreditCheckBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempCreditCheckBigEn.setSubpoena(newSubp);
			tempCreditCheckBigEn = facade.getBigEntryService().create(tempCreditCheckBigEn);
			bigEnList.add(tempCreditCheckBigEn);
		}
		// 塞大分錄序號
		for (BigEntry bn : bigEnList) {
			bigEnSerNo++;
			DecimalFormat df = new DecimalFormat("000");
			bn.setSerNo(df.format(bigEnSerNo));
		}

		// 更新並儲存分錄
		for (ToUpdateEntry tue : allToSaveEntries) {
			Entry en = tue.getEntry();
			en.setCancelCode(tue.getCancelCode());
			facade.getEntryService().update(en);
		}

		// 更新並儲存過渡付款明細
		for (ToUpdateTransitPaymentDetail tUPD : allToSaveTpds) {

			TransitPaymentDetail tpd = tUPD.getTpd();

			tpd.setManualInd("A");
			tpd.setPaymentSource("b");
			// 更新過渡付款明細金融資料
			facade.getTransitPaymentDetailService().updateFinanceDataByVendor(tpd, tUPD.getVendor());// 設定過渡付款明細資料，未執行update
			tpd = facade.getTransitPaymentDetailService().update(tpd);
		}

		// 儲存申請單、流程簽核歷程
		if (org.apache.commons.collections.CollectionUtils.isNotEmpty(eacNeedUpdateList)) {

			for (ExpapplC eac : eacNeedUpdateList) {
				facade.getPaymentDetailService().generatePaymentDetail(eac, paymentBatch, sysDate);// TODO:
																									// update

				// 設定所有日結單代傳票所包含的「費用申請單.申請單狀態」=”已送匯”(包含外幣件)，並對每一筆費用申請單產生「流程簽核歷程」
				eac.setApplState(applSateRemitSend);

				// 2009/11/28,費用申請單要設定新的日結單代傳票
				// 更新費用申請單(ExpapplC&Entry&TransitPaymentDetail有設定cascade)
				eac = facade.getExpapplCService().update(eac);
				facade.getFlowCheckstatusService().createByExpApplC(eac, FunctionCode.C_5_1_1, sysDate);
			}

			// 設定新產生「日結單代傳票」及「大分錄」之間的關聯
			newSubp.setBigEntries(bigEnList);
			// 依照產生的「大分錄」，新增對應的「分錄群組」及「分錄」資料
			newSubp = genEntryGroupAndEntry(bigEnList, newSubp);
			// 儲存新產生的日結單代傳票
			newSubp = facade.getSubpoenaService().create(newSubp);
		}
	}

	/**
	 * <p>
	 * C 5.1.1
	 * </p>
	 * <p>
	 * 廠商月結
	 * </p>
	 * <ul>
	 * <li>只要產生一筆日結單代傳票，而且不用和原有的費用申請單設關聯!!</li>
	 * </ul>
	 * *
	 * 
	 * @param eac
	 *            申請單
	 * @param tempDebitBigEn
	 *            暫存的借方大分錄
	 * @param tempCreditRemitBigEn
	 *            暫存的貸方待匯大分錄
	 * @param tempCreditCheckBigEn
	 *            暫存的貸方待開大分錄
	 * @param sysDate
	 *            系統時間
	 * @param allToSaveEntries
	 *            待儲存的分錄
	 * @param allToSaveTpdList
	 *            待儲存的過渡付款明細
	 * @return
	 * @throws ExpException
	 */
	private Map<String, Object> doProcessExpapplCForMonthClose1(ExpapplC eac, BigEntry tempDebitBigEn, BigEntry tempCreditRemitBigEn, BigEntry tempCreditCheckBigEn, Calendar sysDate, List<ToUpdateEntry> allToSaveEntries, List<ToUpdateTransitPaymentDetail> allToSaveTpds) throws ExpException {

		/* 取得借、貸方物件 */
		// 借方
		if (null == debit_1_EntryType) {
			debit_1_EntryType = facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_D);
		}

		// 貸方
		if (null == credit_1_EntryType) {
			credit_1_EntryType = facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_C);
		}

		// 申請單狀態-已送匯
		if (null == applSateRemitSend) {
			applSateRemitSend = facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.REMIT_SEND, SysTypeCode.C);
		}

		// 查出廠商資料
		Vendor vendor = findByVendorExpUseExpapplC(eac); // modified by Jackson
															// 2010/2/10

		// 要放上面幾個東西，擺個Map
		Map<String, Object> dataMap = new HashMap<String, Object>();
		for (Entry en : eac.getEntryGroup().getEntries()) {

			// ” 20210360”應付費用-總務費用，所需檢核
			if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {

				List<TransitPaymentDetail> tpdList = findAndCheckTransitPaymentDetails(eac, en);

				// 累計金額至暫存的”大分錄”變數，借方
				if (tempDebitBigEn == null) {
					// 只要new一筆新的就好
					tempDebitBigEn = genBigEntry(tempDebitBigEn, en, debit_1_EntryType);
				} else {
					tempDebitBigEn.setBigEntryAmt(tempDebitBigEn.getBigEntryAmt().add(en.getAmt()));
				}
				// 2009/12/1,廠商費用一定是開票或是匯款，若不是這兩個付款方式，則丟出錯誤訊息
				// 累計金額至暫存的”大分錄”變數，貸方-->要拿應付費用科目來當金額來源
				/* 付款方式為開票時 */
				if (PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode())) {
					// 若借方(開票)大分錄為null時
					if (tempCreditCheckBigEn == null || tempCreditCheckBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) == 0) {
						AccTitle acct = getAccTitleWithCache(AccTitleCode.PAYBLE_CHECK.getCode());
						tempCreditCheckBigEn = genBigEntry(tempCreditCheckBigEn, en, credit_1_EntryType);
						tempCreditCheckBigEn.setAccTitle(acct);

						// 2009/12/24, 摘要放會計科目中文IR#1625
						try {
							tempCreditCheckBigEn.setSummary(acct.getName());
						} catch (Exception e) {
						}
						// 大分錄裡面只要有一組銷帳碼就好!! #168
						if (StringUtils.isBlank(tempCreditCheckBigEn.getCancelCode())) {
							String cancelCode = genBigEntryCancelCode(en, acct);
							tempCreditCheckBigEn.setCancelCode(cancelCode);

							ToUpdateEntry tUE = new ToUpdateEntry(en, cancelCode);
							allToSaveEntries.add(tUE);
							for (TransitPaymentDetail tpd : tpdList) {
								tpd.setPaymentDesc(tempCreditCheckBigEn.getCancelCode()); // 2010/11/25
								ToUpdateTransitPaymentDetail tUTPD = new ToUpdateTransitPaymentDetail(tpd, cancelCode, vendor);
								allToSaveTpds.add(tUTPD);
							}
							// break;
						}
					}
					// 若借方(開票)大分錄有值時
					else {
						tempCreditCheckBigEn.setBigEntryAmt(tempCreditCheckBigEn.getBigEntryAmt().add(en.getAmt()));
						ToUpdateEntry tUE = new ToUpdateEntry(en, tempCreditCheckBigEn.getCancelCode());
						allToSaveEntries.add(tUE);
						for (TransitPaymentDetail tpd : tpdList) {
							tpd.setPaymentDesc(tempCreditCheckBigEn.getCancelCode()); // 2010/11/24
							ToUpdateTransitPaymentDetail tUTPD = new ToUpdateTransitPaymentDetail(tpd, tempCreditCheckBigEn.getCancelCode(), vendor);
							allToSaveTpds.add(tUTPD);
						}
					}
				}
				/* 付款方式為匯款時 */
				else if (PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode())) {
					// 若借方(匯款)大分錄為null時
					if (tempCreditRemitBigEn == null || tempCreditRemitBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) == 0) {
						AccTitle acct = getAccTitleWithCache(AccTitleCode.PAYBLE_REMIT.getCode());
						tempCreditRemitBigEn = genBigEntry(tempCreditRemitBigEn, en, credit_1_EntryType);
						tempCreditRemitBigEn.setAccTitle(acct);

						// 2009/12/24, IR#1625, 摘要放會計科目中文
						try {
							tempCreditRemitBigEn.setSummary(acct.getName());
						} catch (Exception e) {
						}
						if (StringUtils.isBlank(tempCreditRemitBigEn.getCancelCode())) { // 大分錄裡面只要有一組銷帳碼就好!!
																							// #168
							String cancelCode = genBigEntryCancelCode(en, acct);
							tempCreditRemitBigEn.setCancelCode(cancelCode);
							ToUpdateEntry tUE = new ToUpdateEntry(en, tempCreditRemitBigEn.getCancelCode());
							allToSaveEntries.add(tUE);
							for (TransitPaymentDetail tpd : tpdList) {
								tpd.setPaymentDesc(tempCreditRemitBigEn.getCancelCode()); // 2010/11/24
								ToUpdateTransitPaymentDetail tUTPD = new ToUpdateTransitPaymentDetail(tpd, cancelCode, vendor);
								allToSaveTpds.add(tUTPD);
							}
						}
					}
					// 若借方(匯款)大分錄有值時
					else {
						tempCreditRemitBigEn.setBigEntryAmt(tempCreditRemitBigEn.getBigEntryAmt().add(en.getAmt()));
						ToUpdateEntry tUE = new ToUpdateEntry(en, tempCreditRemitBigEn.getCancelCode());
						allToSaveEntries.add(tUE);
						for (TransitPaymentDetail tpd : tpdList) {
							tpd.setPaymentDesc(tempCreditRemitBigEn.getCancelCode()); // 2010/11/25
							ToUpdateTransitPaymentDetail tUTPD = new ToUpdateTransitPaymentDetail(tpd, tempCreditRemitBigEn.getCancelCode(), vendor);
							allToSaveTpds.add(tUTPD);
						}
					}
				} else {
					throw new ExpRuntimeException(ErrorCode.C10383);
				}
			}

			// 產生貸方的待匯或待開要在"總務費用判斷時處理"
		}
		dataMap.put("eac", eac);
		dataMap.put("tempDebitBigEn", tempDebitBigEn);
		dataMap.put("tempCreditRemitBigEn", tempCreditRemitBigEn);
		dataMap.put("tempCreditCheckBigEn", tempCreditCheckBigEn);

		dataMap.put("allToSaveEntries", allToSaveEntries);
		dataMap.put("allToSaveTpds", allToSaveTpds);

		return dataMap;
	}

	/**
	 * Sleep for a second
	 * 
	 * @param tag
	 * @param millisec
	 * @return
	 */
	private String vendorMonthlyCloseThreadSleep(String tag, int millisec) {
		String sleepTime = DateUtils.getISODateTimeStr(new java.util.Date());
		System.out.println("[" + tag + "]VendorExp monthly close Sleep Time : " + sleepTime);
		// try {
		// Thread.sleep(millisec);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		return sleepTime;
	}

	/**
	 * <p>
	 * C 5.1.1 廠商月結 & C 5.1.2 廠商內結臨付
	 * </p>
	 * <ul>
	 * <li>將分錄資料過到大分錄，但未建立與日結單代傳票的關聯</li>
	 * </ul>
	 * 
	 * @param bigEn
	 *            大分錄
	 * @param en
	 *            分錄
	 * @return genBigEntry 產生的大分錄
	 */
	private BigEntry genBigEntry(BigEntry bigEn, Entry en, EntryType et) {
		bigEn = new BigEntry();
		bigEn.setAccTitle(en.getAccTitle());
		bigEn.setBigEntryAmt(en.getAmt());
		bigEn.setEntryType(et);
		try {
			bigEn.setSummary(en.getAccTitle().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		bigEn.setSerNo(null);// 序號
		return bigEn;
	}

	/**
	 * 於Cache取得會計科目。
	 * 
	 * @param accTitleCode
	 * @return
	 */
	private AccTitle getAccTitleWithCache(String accTitleCode) {
		AccTitle accTitle = null;
		if (null != accTitleCacheMap) {
			accTitle = accTitleCacheMap.get(accTitleCode);
		} else {
			accTitleCacheMap = new HashMap<String, AccTitle>();
		}

		if (null != accTitle) {
			return accTitle;
		} else {
			accTitle = facade.getAccTitleService().findByCode(accTitleCode);
			accTitleCacheMap.put(accTitleCode, accTitle);
			return accTitle;
		}

	}

	/**
	 * 由分錄取得過渡付款明細List
	 * 
	 * @param en
	 * @return
	 */
	private List<TransitPaymentDetail> getTransitPaymentDetailByEntry(Entry en) {
		List<TransitPaymentDetail> tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
		return tpdList;
	}

	/**
	 * 取得分錄包含的所有過渡付款明細，並檢核總額是否與申請單金額相等。
	 * 
	 * @param eac
	 *            申請單
	 * @param en
	 *            分錄
	 * @return 過渡付款明細List
	 */
	private List<TransitPaymentDetail> findAndCheckTransitPaymentDetails(ExpapplC eac, Entry en) {
		List<TransitPaymentDetail> tranDetails = getTransitPaymentDetailByEntry(en);
		// 2009/11/04,TIM,因為分錄和過渡付款明細設定fetch=lazy，所以要另外用分錄把過渡付款明細資料找出來
		if (!CollectionUtils.isEmpty(tranDetails)) {
			BigDecimal countAmt = BigDecimal.ZERO;
			for (TransitPaymentDetail tPD : tranDetails) {
				countAmt = countAmt.add(tPD.getPaymentAmt());
			}
			checkTransitPaymentDetailAmt(eac, en, countAmt);
		} else if (CollectionUtils.isEmpty(tranDetails)) {
			throw new ExpRuntimeException(ErrorCode.C10088);
		}
		return tranDetails;
	}

	/**
	 * 檢核過渡付款明細總金額，是否與分錄金額相同。
	 * 
	 * @param eac
	 *            申請單
	 * @param en
	 *            分錄
	 * @param countAmt
	 *            過渡付款明細總金額
	 */
	private void checkTransitPaymentDetailAmt(ExpapplC eac, Entry en, BigDecimal countAmt) {
		if (en.getAmt().compareTo(countAmt) != 0) {
			StringBuffer sb = new StringBuffer();
			// 申請單號:xxx, 分錄(20210360)金額:xx, 過渡付款明細總金額:xx
			sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo"));
			sb.append(":" + eac.getExpApplNo());
			sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry"));
			sb.append("(" + en.getAccTitle().getCode() + ") ");
			sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_amt") + ":" + en.getAmt());
			sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_TransitPaymentDetail") + "totalAmt:" + countAmt);
			MessageManager.getInstance().showErrorMessage(sb.toString());
			throw new ExpRuntimeException(ErrorCode.C10088);
		}
	}

	/**
	 * 產生大分錄的銷帳碼。
	 * 
	 * @param en
	 * @param acct
	 * @return
	 */
	private String genBigEntryCancelCode(Entry en, AccTitle acct) {
		SNGenerator cancelCodeGenerator = facade.getCancelCodeTypeService().getCancelCodeGenerator(acct, en.getEntryType().getValue());
		Map<String, String> params = new HashMap<String, String>();
		params.put("param1", BigTypeCode.VENDOR_EXP.getCode());
		String cancelCode = cancelCodeGenerator.getSerialNumber(params);
		return cancelCode;
	}

	/**
	 * <p>
	 * C 5.1.1 廠商月結
	 * </p>
	 * <ul>
	 * <li>產生日結單代傳票</li>
	 * </ul>
	 * 
	 * @param btCode
	 *            費用大分類代號
	 * @return genSubpoena 日結單代傳票
	 */
	private Subpoena genSubpoena(String btCode, Calendar subpoenaDate) {
		/*
		 * 傳票號碼原則，傳票號碼編碼方式：一碼"S" + 二碼識別碼（費用大分類代碼） +1 碼年（西元年最末碼，即0~9） + 1 碼月（1~9
		 * 代表1~9 月，A、B、C 代表10、11、12 月） + 2 碼日 + 3 碼流水號。(SubpoenaNoGenerator)
		 */
		SNGenerator gen = AbstractSNGenerator.getInstance(SubpoenaNoGenerator.class.getName(), facade.getSequenceService());
		Map<String, String> params = new HashMap<String, String>();
		params.put("param1", btCode);
		// 2009/11/02,TIM,由於日結單待傳票序號產生器已被修改，所以新加參數，畫面傳的作帳日期
		params.put("param2", DateUtils.getRocDateStr7Digits(subpoenaDate.getTime(), ""));
		// 產生序號
		String sn = gen.getSerialNumber(params);
		Subpoena subpoena = new Subpoena();
		// 序號
		subpoena.setSubpoenaNo(sn);
		// 帳冊別
		subpoena.setAccBookType(facade.getAccBookTypeService().findByAccBookTypeCode(AccBookTypeCode.AccBookTypeCode_000));
		// 來源別110100審計課
		Department source = facade.getDepartmentService().findByCode(Department.AUDIT_DIVSIO);
		subpoena.setSourceCode(source.getCode());
		subpoena.setSourceName(source.getName());
		// 大分錄會在主要的邏輯中放到subpoena
		subpoena.setBigEntries(new ArrayList<BigEntry>());
		subpoena.setSubpoenaDate(subpoenaDate);
		// 2009/12/16,廠商月結該欄位要設true
		subpoena.setVendorMonthClose(true);
		subpoena = facade.getSubpoenaService().create(subpoena);
		return subpoena;
	}

	/**
	 * <p>
	 * C 5.1.2 廠商內結臨付
	 * </p>
	 * <ul>
	 * <li>將分錄資料過到大分錄</li>
	 * </ul>
	 * 
	 * @param bigEn
	 *            大分錄
	 * @param en
	 *            分錄
	 * @return genBigEntry 產生的大分錄
	 */
	private BigEntry genBigEntry(BigEntry bigEn, Entry en, EntryType et, Subpoena subpoena) {
		bigEn = new BigEntry();
		bigEn.setAccTitle(en.getAccTitle());
		bigEn.setBigEntryAmt(en.getAmt());
		bigEn.setEntryType(et);
		try {
			bigEn.setSummary(en.getAccTitle().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		bigEn.setSerNo(null);// 序號
		bigEn.setSubpoena(subpoena);
		return bigEn;
	}

	/**
	 * <p>
	 * C 5.1.1 廠商月結 & C 5.1.2 廠商內結臨付
	 * </p>
	 * <ul>
	 * <li>以大分錄資料產生新的分錄群組，以及分錄資料 並分回新產生的日結單代傳票</li>
	 * </ul>
	 * 
	 * @param bigEnList
	 *            大分錄資料
	 * @param subpoena
	 *            新產生的日結單代傳票
	 * @return subpoena 新產生的日結單代傳票
	 */
	private Subpoena genEntryGroupAndEntry(List<BigEntry> bigEnList, Subpoena subpoena) {
		EntryGroup eg = new EntryGroup();
		eg = facade.getEntryGroupService().create(eg);
		List<Entry> entries = new ArrayList<Entry>();
		for (BigEntry bigEn : bigEnList) {
			Entry en = new Entry();
			en.setEntryGroup(eg);
			org.springframework.beans.BeanUtils.copyProperties(bigEn, en);
			// 2009/12/11,補上金額欄位的值
			en.setAmt(bigEn.getBigEntryAmt());
			en.setSubpoena(subpoena);
			facade.getEntryService().create(en);
			entries.add(en);
		}
		subpoena.setEntries(entries);
		return subpoena;
	}

	/**
	 * <p>
	 * C 5.1.1 廠商月結
	 * </p>
	 * <ul>
	 * <li>查詢廠商資料，以費用申請單來查詢廠商費用裡的廠商</li>
	 * <li>若"廠商費用.代墊廠商統編"有值，則回傳該代墊廠商資料</li>
	 * </ul>
	 * 
	 * @param eac
	 *            費用申請單
	 * @return Vendor 廠商資料
	 */
	private Vendor findByVendorExpUseExpapplC(ExpapplC eac) {
		if (MiddleTypeCode.CODE_A30.getCode().equals(eac.getMiddleType().getCode())) {
			PubAffCarExp pexp = facade.getPubAffCarExpService().findByExpapplC(eac);
			if (pexp != null && pexp.getVendor() != null) {
				return pexp.getVendor();
			} else {
				return null;
			}
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("expapplC", eac);
		VendorExp vexp = findByCriteriaMap(params).get(0);
		if (StringUtils.isNotBlank(vexp.getPadVendorCompId())) {
			Vendor padVendor = this.getFacade().getVendorService().findByCompanyId(vexp.getPadVendorCompId());
			if (null != padVendor) {
				return padVendor;
			}
		}

		return vexp.getVendor();
	}

	public List<VendorExp> findForInCloseTempPay(String payYearMonth, String invoiceNo, boolean inCloseTempPay, boolean tempNoPay) {
		if (payYearMonth == null) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { Messages.getString("applicationResources", "tw_com_skl_exp_kernel_model6_bo_VendorExp_payYearMonth", null) });
		}
		StringBuffer querySql = new StringBuffer();
		querySql.append("select vExp from VendorExp vExp join vExp.expapplC eac join eac.middleType mt join mt.bigType bt join eac.applState aplState" + " where vExp.payYearMonth=:pym and aplState.code='70' and bt.code='01'");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("pym", payYearMonth);

		if (StringUtils.isNotBlank(invoiceNo)) {
			querySql.append(" and eac.invoiceNo=:invoiceNo ");
			params.put("invoiceNo", invoiceNo);
		}

		if (inCloseTempPay) {
			querySql.append(" and eac.inCloseTemporaryPayment=1");
		}
		if (tempNoPay) {
			querySql.append(" and eac.temporaryNoPayment=1");
		}
		return getDao().findByNamedParams(querySql.toString(), params);
	}

	private List<VendorExp> findVendorExpUseExpApplNoList(List<String> expApplNoList) {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(expApplNoList))
			return null;
		return getDao().findVendorExpUseExpApplNoList(expApplNoList);
	}

	public void doInCloseTemporaryPayment(List<String> expApplNoList) {
		List<VendorExp> vendorExpList = findVendorExpUseExpApplNoList(expApplNoList);
		List<PubAffCarExp> pexpList = facade.getPubAffCarExpService().findPubAffCarExpUserExpApplNoList(expApplNoList);
		if (vendorExpList == null && pexpList == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_monthlyclose_vexp_and_pexp") });
		}
		ExpapplC eac = null;
		for (VendorExp vExp : vendorExpList) {
			eac = vExp.getExpapplC();
			eac.setInCloseTemporaryPayment(true);
			eac = facade.getExpapplCService().update(eac);
			vExp.setExpapplC(eac);
			vExp = getDao().update(vExp);
		}
		for (PubAffCarExp pExp : pexpList) {
			eac = pExp.getExpapplC();
			eac.setInCloseTemporaryPayment(true);
			eac = facade.getExpapplCService().update(eac);
			pExp.setExpapplC(eac);
			pExp = facade.getPubAffCarExpService().update(pExp);
		}
	}

	public void doCancelInCloseTemporaryPayment(List<String> expApplNoList) {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}
		List<VendorExp> vendorExpList = findVendorExpUseExpApplNoList(expApplNoList);
		List<PubAffCarExp> pexpList = facade.getPubAffCarExpService().findPubAffCarExpUserExpApplNoList(expApplNoList);
		if (vendorExpList == null && pexpList == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_monthlyclose_vexp_and_pexp") });
		}
		ExpapplC eac = null;
		for (VendorExp vendorExp : vendorExpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(vendorExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10090);
			}
			eac = vendorExp.getExpapplC();
			eac.setInCloseTemporaryPayment(false);
			eac = facade.getExpapplCService().update(eac);
			vendorExp.setExpapplC(eac);
			vendorExp = getDao().update(vendorExp);
		}
		for (PubAffCarExp pExp : pexpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(pExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10090);
			}
			eac = pExp.getExpapplC();
			eac.setInCloseTemporaryPayment(false);
			eac = facade.getExpapplCService().update(eac);
			pExp.setExpapplC(eac);
			pExp = facade.getPubAffCarExpService().update(pExp);
		}
	}

	public void doCancelTemporaryNoPayment(List<String> expApplNoList, Calendar expectRemitDate) {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}
		List<VendorExp> vendorExpList = findVendorExpUseExpApplNoList(expApplNoList);
		List<PubAffCarExp> pexpList = facade.getPubAffCarExpService().findPubAffCarExpUserExpApplNoList(expApplNoList);
		if (vendorExpList == null && pexpList == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_monthlyclose_vexp_and_pexp") });
		}
		ExpapplC eac = null;
		if (expectRemitDate == null) {
			throw new ExpRuntimeException(ErrorCode.C10091);
		}
		for (VendorExp vendorExp : vendorExpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(vendorExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10093);
			}
			eac = vendorExp.getExpapplC();
			eac.setTemporaryNoPayment(false);
			eac.setExpectRemitDate(expectRemitDate);
			eac = facade.getExpapplCService().update(eac);
			vendorExp.setExpapplC(eac);
			vendorExp = getDao().update(vendorExp);
		}
		for (PubAffCarExp pExp : pexpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(pExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10093);
			}
			eac = pExp.getExpapplC();
			eac.setTemporaryNoPayment(false);
			// 將此取消-->3. 設定所有勾選的「費用申請單.預計付款日」=傳入參數”預計付款日”。(2009/12/22)
			// eac.setExpectRemitDate(expectRemitDate);
			eac = facade.getExpapplCService().update(eac);
			pExp.setExpapplC(eac);
			pExp = facade.getPubAffCarExpService().update(pExp);
		}
	}

	public void doConfirmInCloseTemporaryPayment(List<String> expApplNoList, Calendar expectRemitDate) throws ExpException {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}
		List<VendorExp> vendorExpList = findVendorExpUseExpApplNoList(expApplNoList);
		List<PubAffCarExp> pexpList = facade.getPubAffCarExpService().findPubAffCarExpUserExpApplNoList(expApplNoList);
		if (vendorExpList == null && pexpList == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_monthlyclose_vexp_and_pexp") });
		}
		List<Object> xExpList = new ArrayList<Object>();
		for (VendorExp vexp : vendorExpList) {
			xExpList.add(vexp);
		}
		for (PubAffCarExp pexp : pexpList) {
			xExpList.add(pexp);
		}
		VendorExp vendorExp = null;
		PubAffCarExp pcarExp = null;
		Vendor vendor = null;
		ExpapplC eac = null;
		List<ExpapplC> eac2Save = new ArrayList<ExpapplC>();
		// 判斷為哪種費用，才能決定如何取得要用的廠商資料
		for (Object ob : xExpList) {
			if (ob instanceof VendorExp) {
				vendorExp = (VendorExp) ob;
				vendor = vendorExp.getVendor();
				eac = vendorExp.getExpapplC();
			} else if (ob instanceof VendorExp) {
				pcarExp = (PubAffCarExp) ob;
				vendor = pcarExp.getVendor();
				eac = pcarExp.getExpapplC();
			}
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(eac.getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10094);
			}
			// 2009/12/22，內結臨付=false和暫不付款=true，"不需要"作"確認臨付"(不處理此申請單)
			if (eac.isTemporaryNoPayment() || !eac.isInCloseTemporaryPayment()) {
				continue;
			}
			eac2Save.add(eac);
		}
		Calendar sysDate = Calendar.getInstance();
		// 要產生送匯批次記錄
		PaymentBatch paymentBatch = genPaymentBatch(sysDate);
		// 廠商資料，提到上面處理
		// 產生一筆新的日結單代傳票-->2009/11/28,只要一筆就好!
		Subpoena newSubp = genSubpoena(BigTypeCode.VENDOR_EXP.getCode(), sysDate);
		// 2009/11/12,TIM,修改：設定大分錄的SerNo-->2009/11/28,一個日結單代傳票裡的大分錄序號，不能重複
		int bigEnSerNo = 0;
		BigEntry tempDebitBigEn = null; // 借方: 應付費用 - 總務費用
		BigEntry tempCreditRemitBigEn = null; // 貸: 應付費用 - 待匯
		BigEntry tempCreditCheckBigEn = null; // 貸: 應付費用 -待開
		// 要放上面幾個東西，擺個Map
		Map<String, Object> dataMap = new HashMap<String, Object>();
		// 2009/11/28, 如果資料全都室內結臨付=Y and 暫不付款=Y,就沒有資料可以處理!!
		if (CollectionUtils.isEmpty(eac2Save))
			return;
		for (ExpapplC eac2 : eac2Save) {
			dataMap = doProcessExpapplCForMonthClose(eac2, newSubp, paymentBatch, tempDebitBigEn, tempCreditRemitBigEn, tempCreditCheckBigEn, sysDate, expectRemitDate);
			tempDebitBigEn = (BigEntry) dataMap.get("tempDebitBigEn");
			tempCreditRemitBigEn = (BigEntry) dataMap.get("tempCreditRemitBigEn");
			tempCreditCheckBigEn = (BigEntry) dataMap.get("tempCreditCheckBigEn");
		}
		// 儲存新產生的「日結單代傳票」及暫存的「大分錄」資料(最多三筆)
		List<BigEntry> bigEnList = new ArrayList<BigEntry>();
		// 2009/12/11,還要照順序排
		if (tempDebitBigEn != null && tempDebitBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempDebitBigEn = facade.getBigEntryService().create(tempDebitBigEn);
			bigEnList.add(tempDebitBigEn);
		}
		if (tempCreditRemitBigEn != null && tempCreditRemitBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempCreditRemitBigEn = facade.getBigEntryService().create(tempCreditRemitBigEn);
			bigEnList.add(tempCreditRemitBigEn);
		}
		if (tempCreditCheckBigEn != null && tempCreditCheckBigEn.getBigEntryAmt().compareTo(BigDecimal.ZERO) > 0) {
			tempCreditCheckBigEn = facade.getBigEntryService().create(tempCreditCheckBigEn);
			bigEnList.add(tempCreditCheckBigEn);
		}
		for (BigEntry bn : bigEnList) {
			bigEnSerNo++;
			DecimalFormat df = new DecimalFormat("000");
			bn.setSerNo(df.format(bigEnSerNo));
		}
		// 設定新產生「日結單代傳票」及「大分錄」之間的關聯
		newSubp.setBigEntries(bigEnList);
		// 依照產生的「大分錄」，新增對應的「分錄群組」及「分錄」資料
		newSubp = genEntryGroupAndEntry(bigEnList, newSubp);
		// 儲存新產生的日結單代傳票
		newSubp = facade.getSubpoenaService().create(newSubp);
		// 2009/11/27,是否要把關連設回費用申請單
	}

	public void doTemporaryNoPayment(List<String> expApplNoList) {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(expApplNoList)) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo") });
		}
		List<VendorExp> vendorExpList = findVendorExpUseExpApplNoList(expApplNoList);
		List<PubAffCarExp> pexpList = facade.getPubAffCarExpService().findPubAffCarExpUserExpApplNoList(expApplNoList);
		if (vendorExpList == null && pexpList == null) {
			throw new ExpRuntimeException(ErrorCode.A20002, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_monthlyclose_vexp_and_pexp") });
		}
		ExpapplC eac = null;
		for (VendorExp vendorExp : vendorExpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(vendorExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10092);
			}
			eac = vendorExp.getExpapplC();
			eac.setTemporaryNoPayment(true);
			eac = facade.getExpapplCService().update(eac);
			vendorExp.setExpapplC(eac);
			vendorExp = getDao().update(vendorExp);
		}
		for (PubAffCarExp pExp : pexpList) {
			if (!ApplStateCode.INTERNAL_CLOSED.getCode().equals(pExp.getExpapplC().getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10092);
			}
			eac = pExp.getExpapplC();
			eac.setTemporaryNoPayment(true);
			eac = facade.getExpapplCService().update(eac);
			pExp.setExpapplC(eac);
			pExp = facade.getPubAffCarExpService().update(pExp);
		}
	}

	public void caculateIncomeAmt(VendorExp vendorExp) {
		// 輸入”發票金額”欄位後，若使用者將“進項稅額”欄位改為”Y”值，系統自動設定”進項稅金額欄位”為可操作，並帶出預設進項稅金額於”進項稅金額欄位”
		if (vendorExp.getExpapplC().isWithholdIncome()) {

			// 預設進項稅金額=”發票金額” * (「系統參數設定檔」中的”系統預設進項稅的稅率”)
			if (null != vendorExp.getExpapplC().getInvoiceAmt() && vendorExp.getExpapplC().getInvoiceAmt().compareTo(BigDecimal.ZERO) > 0) {

				// 若進項稅<=0 則代出系統預設的稅率計算進項稅
				if (BigDecimal.ZERO.compareTo(vendorExp.getExpapplC().getInvoiceTaxAmt()) >= 0) {

					SystemParam systemParam = this.facade.getSystemParamService().findBySystemParamName(SystemParamName.DEFAULT_INCOME_TAX_RATE);
					// 系統預設進項稅的稅率
					BigDecimal tax = new BigDecimal(systemParam.getValue());
					// 憑證金額(含)
					BigDecimal amt = vendorExp.getExpapplC().getInvoiceAmt();
					// 計算進項稅額
					BigDecimal incomeAmt = this.facade.getAccTitleService().calculateIncomeAmt(amt, tax);

					// set憑證金額(稅)
					vendorExp.getExpapplC().setInvoiceTaxAmt(incomeAmt);
				}

				// set憑證金額(未) = 憑證金額(含)- 憑證金額(稅)
				vendorExp.getExpapplC().setInvoiceNoneTaxAmt(vendorExp.getExpapplC().getInvoiceAmt().subtract(vendorExp.getExpapplC().getInvoiceTaxAmt()));

			}

		} else {
			// 若使用者將“進項稅額”欄位改為”---”值，系統自動設定”進項稅金額欄位”為不可操作，並將進項稅金額設為0
			vendorExp.getExpapplC().setIncomeAmt(BigDecimal.ZERO);
			vendorExp.getExpapplC().setInvoiceTaxAmt(BigDecimal.ZERO);
			vendorExp.getExpapplC().setInvoiceNoneTaxAmt(BigDecimal.ZERO);
		}
	}

	public List<VendorExp> findForExportImmovExpMgBn(Calendar sendItemStartDate, Calendar sendItemEndDate) {
		return getDao().findForExportImmovExpMgBn(sendItemStartDate, sendItemEndDate);
	}

	public List<VendorExpInCloseTempPayDto> findForInCloseTempPay(String payYearMonth, String invoiceNo, Boolean inCloseTemporaryPay, Boolean temporaryNoPayment) {
		if (StringUtils.isBlank(payYearMonth))
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_VendorExp_payYearMonth") });
		// if(StringUtils.isBlank(invoiceNo))
		// throw new ExpRuntimeException(ErrorCode.A10007, new
		// String[]{MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_invoiceNo")});
		return getDao().findForInCloseTempPay(payYearMonth, invoiceNo, inCloseTemporaryPay, temporaryNoPayment);
	}

	/**
	 * <p>
	 * C 5.1.2
	 * </p>
	 * <p>
	 * 確認內結臨付
	 * </p>
	 * <ul>
	 * <li>只要產生一筆日結單代傳票，而且不用和原有的費用申請單設關聯!!</li>
	 * </ul>
	 * 
	 * @param newSubp
	 *            代回傳的日結單代傳票
	 * @param paymentBatch
	 *            送匯批次記錄
	 * @param tempDebitBigEn
	 *            暫存的借方大分錄
	 * @param tempCreditRemitBigEn
	 *            暫存的貸方待匯大分錄
	 * @param tempCreditCheckBigEn
	 *            暫存的貸方待開大分錄
	 * @param sysDate
	 *            系統時間
	 * @param expectRemitDate
	 *            預計付款日期，確認內結臨付才會傳
	 * @param eacList
	 *            費用申請單集合
	 * @return Map 資料群
	 * @throws ExpException
	 */
	private Map<String, Object> doProcessExpapplCForMonthClose(ExpapplC eac, Subpoena newSubp, PaymentBatch paymentBatch, BigEntry tempDebitBigEn, BigEntry tempCreditRemitBigEn, BigEntry tempCreditCheckBigEn, Calendar sysDate, Calendar expectRemitDate) throws ExpException {

		// 要放上面幾個東西，擺個Map
		Map<String, Object> dataMap = new HashMap<String, Object>();
		List<TransitPaymentDetail> tranDetails = null;
		for (Entry en : eac.getEntryGroup().getEntries()) {
			if (tranDetails == null) {
				tranDetails = new ArrayList<TransitPaymentDetail>();
			}
			// ” 20210360”應付費用-總務費用，所需檢核
			if (AccTitleCode.PAYBLE_LEDGER.getCode().equals(en.getAccTitle().getCode())) {
				tranDetails = facade.getTransitPaymentDetailService().findByEntry(en);
				// 2009/11/04,TIM,因為分錄和過渡付款明細設定fetch=lazy，所以要另外用分錄把過渡付款明細資料找出來
				if (!CollectionUtils.isEmpty(tranDetails)) {
					BigDecimal countAmt = BigDecimal.ZERO;
					for (TransitPaymentDetail tPD : tranDetails) {
						countAmt = countAmt.add(tPD.getPaymentAmt());
					}
					if (en.getAmt().compareTo(countAmt) != 0) {
						StringBuffer sb = new StringBuffer();
						// 申請單號:xxx, 分錄(20210360)金額:xx, 過渡付款明細總金額:xx
						sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_expApplNo"));
						sb.append(":" + eac.getExpApplNo());
						sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry"));
						sb.append("(" + en.getAccTitle().getCode() + ") ");
						sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_amt") + ":" + en.getAmt());
						sb.append(", " + MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_TransitPaymentDetail") + "totalAmt:" + countAmt);
						MessageManager.getInstance().showErrorMessage(sb.toString());
						throw new ExpRuntimeException(ErrorCode.C10088);
					}
				} else if (CollectionUtils.isEmpty(tranDetails)) {
					throw new ExpRuntimeException(ErrorCode.C10088);
				}
				// 累計金額至暫存的”大分錄”變數，借方
				if (tempDebitBigEn == null) {
					// 只要new一筆新的就好
					tempDebitBigEn = genBigEntry(tempDebitBigEn, en, facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_D), newSubp);
				} else {
					tempDebitBigEn.setBigEntryAmt(tempDebitBigEn.getBigEntryAmt().add(en.getAmt()));
				}
				// 2009/12/1,廠商費用一定是開票或是匯款，若不是這兩個付款方式，則丟出錯誤訊息
				// 累計金額至暫存的”大分錄”變數，貸方-->要拿應付費用科目來當金額來源
				if (PaymentTypeCode.C_CHECK.getCode().equals(eac.getPaymentType().getCode())) {
					if (tempCreditCheckBigEn == null) {
						AccTitle acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_CHECK.getCode());
						tempCreditCheckBigEn = genBigEntry(tempCreditCheckBigEn, en, facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_C), newSubp);
						tempCreditCheckBigEn.setAccTitle(acct);
						// 2009/12/24, 摘要放會計科目中文IR#1625
						try {
							tempCreditCheckBigEn.setSummary(acct.getName());
						} catch (Exception e) {
						}
						if (StringUtils.isBlank(tempCreditCheckBigEn.getCancelCode())) { // 大分錄裡面只要有一組銷帳碼就好!!
																							// #168
							String cancelCode = genBigEntryCancelCode(en, acct);
							tempCreditCheckBigEn.setCancelCode(cancelCode);
							en.setCancelCode(cancelCode);
							List<TransitPaymentDetail> tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
							for (TransitPaymentDetail tpd : tpdList) {
								tpd.setPaymentDesc(cancelCode);
								facade.getTransitPaymentDetailService().update(tpd);
							}
							// break;
						}
					} else {
						tempCreditCheckBigEn.setBigEntryAmt(tempCreditCheckBigEn.getBigEntryAmt().add(en.getAmt()));
						List<TransitPaymentDetail> tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
						en.setCancelCode(tempCreditCheckBigEn.getCancelCode());
						for (TransitPaymentDetail tpd : tpdList) {
							tpd.setPaymentDesc(tempCreditCheckBigEn.getCancelCode());
							facade.getTransitPaymentDetailService().update(tpd);
						}
					}
				} else if (PaymentTypeCode.C_REMIT.getCode().equals(eac.getPaymentType().getCode())) {
					if (tempCreditRemitBigEn == null) {
						AccTitle acct = facade.getAccTitleService().findByCode(AccTitleCode.PAYBLE_REMIT.getCode());
						tempCreditRemitBigEn = genBigEntry(tempCreditRemitBigEn, en, facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_1_C), newSubp);
						tempCreditRemitBigEn.setAccTitle(acct);
						// 2009/12/24, IR#1625, 摘要放會計科目中文
						try {
							tempCreditRemitBigEn.setSummary(acct.getName());
						} catch (Exception e) {
						}
						if (StringUtils.isBlank(tempCreditRemitBigEn.getCancelCode())) { // 大分錄裡面只要有一組銷帳碼就好!!
																							// #168
							String cancelCode = genBigEntryCancelCode(en, acct);
							tempCreditRemitBigEn.setCancelCode(cancelCode);
							en.setCancelCode(cancelCode);
							List<TransitPaymentDetail> tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
							for (TransitPaymentDetail tpd : tpdList) {
								tpd.setPaymentDesc(cancelCode);
								facade.getTransitPaymentDetailService().update(tpd);
							}
							// break;
						}
					} else {
						tempCreditRemitBigEn.setBigEntryAmt(tempCreditRemitBigEn.getBigEntryAmt().add(en.getAmt()));
						List<TransitPaymentDetail> tpdList = facade.getTransitPaymentDetailService().findByEntry(en);
						en.setCancelCode(tempCreditRemitBigEn.getCancelCode());
						for (TransitPaymentDetail tpd : tpdList) {
							tpd.setPaymentDesc(tempCreditRemitBigEn.getCancelCode());
							facade.getTransitPaymentDetailService().update(tpd);
						}
					}
				} else {
					throw new ExpRuntimeException(ErrorCode.C10383);
				}
			}
			tranDetails.clear();
			tranDetails = facade.getTransitPaymentDetailService().findByEntry(en);
			Vendor vendor = findByVendorExpUseExpapplC(eac); // modified by
																// Jackson
																// 2010/2/10
			for (TransitPaymentDetail tPD : tranDetails) {
				tPD.setManualInd("A");
				tPD.setPaymentSource("b");
				// 更新過渡付款明細金融資料
				facade.getTransitPaymentDetailService().updateFinanceDataByVendor(tPD, vendor);
				// 由過渡付款明細資料，產生付款明細
				facade.getPaymentDetailService().generatePaymentDetail(eac, paymentBatch, sysDate);
				// 更新過渡付款明細到DB
				tPD = facade.getTransitPaymentDetailService().update(tPD);
			}
			// 產生貸方的待匯或待開要在"總務費用判斷時處理"
		}
		// 設定所有日結單代傳票所包含的「費用申請單.申請單狀態」=”已送匯”(包含外幣件)，並對每一筆費用申請單產生「流程簽核歷程」
		eac.setApplState(facade.getApplStateService().findByCodeFetchSysType(ApplStateCode.REMIT_SEND, SysTypeCode.C));
		if (expectRemitDate != null) {
			// 設定所有勾選的「費用申請單.預計付款日」=傳入參數”預計付款日”。(2009/12/22)
			eac.setExpectRemitDate(expectRemitDate);
		}
		// 不用設定關聯!! eac.setSubpoena(newSubp); //2009/11/28,費用申請單要設定新的日結單代傳票
		// 更新費用申請單(ExpapplC&Entry&TransitPaymentDetail有設定cascade)
		eac = facade.getExpapplCService().update(eac);
		facade.getFlowCheckstatusService().createByExpApplC(eac, FunctionCode.C_5_1_1, sysDate);
		dataMap.put("eac", eac);
		dataMap.put("tempDebitBigEn", tempDebitBigEn);
		dataMap.put("tempCreditRemitBigEn", tempCreditRemitBigEn);
		dataMap.put("tempCreditCheckBigEn", tempCreditCheckBigEn);

		return dataMap;
	}

	// /**
	// * 判斷會計科目是否為分期付款科目
	// * @param code
	// * @return
	// */
	// private boolean isInstallmentAccTitleCode(String code) {
	// if (StringUtils.isBlank(code)) {
	// return false;
	// }
	// code = StringUtils.trimToNull(code);
	// if (StringUtils.equals("10511100", code) ||
	// StringUtils.equals("10840100", code) ||
	// StringUtils.equals("10840200", code)) {
	// return true;
	// }
	//
	// return false;
	// }
}
