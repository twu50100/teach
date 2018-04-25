package tw.com.skl.exp.kernel.model6.logic;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import tw.com.skl.common.model6.logic.BaseService;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.BigType;
import tw.com.skl.exp.kernel.model6.bo.BudgetIn;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.Entry;
import tw.com.skl.exp.kernel.model6.bo.ExpapplC;
import tw.com.skl.exp.kernel.model6.bo.ExpapplCDetail;
import tw.com.skl.exp.kernel.model6.bo.FlowCheckstatus;
import tw.com.skl.exp.kernel.model6.bo.IncomeIdType;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp;
import tw.com.skl.exp.kernel.model6.bo.PaymentType;
import tw.com.skl.exp.kernel.model6.bo.QuotaDetail;
import tw.com.skl.exp.kernel.model6.bo.QuotaItem;
import tw.com.skl.exp.kernel.model6.bo.ReturnStatement;
import tw.com.skl.exp.kernel.model6.bo.RosterDetail;
import tw.com.skl.exp.kernel.model6.bo.Subpoena;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.BigType.BigTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ExpItem.ExpItemCode;
import tw.com.skl.exp.kernel.model6.bo.ExpType.ExpTypeCode;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentTarget.PaymentTargetCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentType.PaymentTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ProofType.ProofTypeCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpException;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.dto.ExpapplCDto;
import tw.com.skl.exp.kernel.model6.dto.ExpapplCMaintainDto;
import tw.com.skl.exp.kernel.model6.dto.ReturnExpapplCDto;
import tw.com.skl.exp.kernel.model6.dto.RtnItemApplDto;
import tw.com.skl.exp.kernel.model6.dto.VendorExpDto;
import tw.com.skl.exp.kernel.model6.logic.enumeration.ApplStateEnum;

/**
 * 行政費用申請單 Service 介面。
 * 
 * <pre>
 * Revision History
 * 2009/3/20, Chih-Liang Chang, 新增 List&lt;ExpapplC&gt; findApplyForDailyStmt(User user).
 * 2009/3/23, Chih-Liang Chang, 新增 List&lt;ExpapplC&gt; findRemittedApply().
 * 2009/4/23, Jackson Lee, 新增 List&lt;ExpapplC&gt; findByCancelCode().
 * 2009/6/19, Sunkist Wang, 更新 List&lt;ExpapplC&gt; findApplyForDailyStmt(User user) 的 java doc.
 * 2009/7/8, Eustace Hsieh, 新增List&lt;ExpapplC&gt; findForExpapplCFetchRelation(
 *             ApplStateCode[] applStateCodeArray,
 *             MiddleTypeCode[] middleTypeCodeArray,
 *             String applUserCode,
 *             ProofTypeCode proofTypeCode,
 *             Calendar createDateStart,
 *             Calendar createDateEnd,
 *             Boolean temporaryPayment,
 *             String deliverNo );
 * 2009/7/10, Eustace Hsieh, 新增List&lt;ExpapplC&gt; findByDeliverNo(String deliverNo)throws ExpException;
 * 2009/7/10, Eustace Hsieh, 新增List&lt;ExpapplC&gt; findByParams(List&lt;String&gt; deliverNos, BigType bigType, Boolean temporaryPayment, String userCode, ApplStateCode applStateCode)throws ExpException;
 * 2009/7/15, Eustace Hsieh, 新增List&lt;ExpapplC&gt; findByDeliverDayListNo(String deliverDayListNo) throws ExpRuntimeException;
 * 2009/7/20, Tim Chiang 更新 checkPrepaymentAccTtleColumns(ExpapplC expapplC).
 * 2009/7/27, Eustace Hsieh, 新增 String generateExpApplNo(String param1)throws ExpException, ExpRuntimeException;
 * 2009/7/29, Eustace Hsieh, 新增 List&lt;Entry&gt; doGenerateApplyRosterEntries(ExpapplC expapplC, Department department, Boolean boolean1)throws ExpException, ExpRuntimeException;
 * 2009/7/31, Eustace Hsieh, 新增 List&lt;ERtnItemApplDto&gt; findRtnItemApplDto(ApplStateCode applStateCode, BigTypeCode bigTypeCode)throws ExpException, ExpRuntimeException;
 * 2009/8/3,Tim Chiang 新增 List&lt;FlowCheckstatus&gt; findReturnGovApplyRecord(String expApplNo);
 * 2009/8/3, Eustace Hsieh, 更新 void doRtnItemResend(List&lt;String&gt; applyFormNoList, FunctionCode functionCode) throws ExpException;
 * 2009/8/4, Eustace Hsieh, 新增 void updateRosterState(RosterStateCode rosterStateCode, String expApplNo, List<String> listNos)throws ExpException, ExpRuntimeException;
 * 2009/8/12, Eustace Hsieh, 新增 void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department,
 *             String cancelCode, IncomeIdType incomeIdType, String incomeId, String industryCode)throws ExpException, ExpRuntimeException;
 * 2009/8/14, Eustace Hsieh, 新增 void toDelEntrys(ExpapplC expapplC, List&lt;Entry&gt; toDelEntryList, boolean isPressGetExpDetailAction) throws ExpException, ExpRuntimeException;
 * 2009/8/14, Eustace Hsieh, 新增 void doDelEntrys(List&lt;Entry&gt; delEntryList) throws ExpException, ExpRuntimeException;
 * 2009/8/14, Eustace Hsieh, 新增 void beforUpdateExp(ExpapplC expapplC, List&lt;Entry&gt; toDelEntryList) throws ExpException, ExpRuntimeException;
 * 2009/8/14, Eustace Hsieh, 新增 void verifyExpapplC(ExpapplC expapplC) throws ExpException, ExpRuntimeException;
 * 2009/8/17, Eustace Hsieh, 實作 void verifyExpapplC(ExpapplC expapplC) throws ExpException, ExpRuntimeException;
 * 2009/8/21, Eustace Hsieh, 更新 void verifyExpapplC(ExpapplC expapplC) throws ExpException, ExpRuntimeException;
 * 2009/8/26, Eustace Hsieh, 新增 List&lt;ExpapplC&gt; findByDeliverDayListNos(List&lt;String&gt; deliverDayListNos) throws ExpRuntimeException;
 * 2009/8/27, Eustace Hsieh, 更新 List&lt;ExpapplC&gt; findByDeliverDayListNos(List&lt;String&gt; deliverDayListNos, ApplStateCode[] applStateCodes) throws ExpRuntimeException;
 * 2009/9/1, Tim Chiang, 新增 checkCashGiftApplQuota(ExpapplC expapplC)
 * 2009/9/3, Eustace Hsieh, 更新 void doRtnItemResend(List&lt;String&gt; applyFormNoList, FunctionCode functionCode) throws ExpException;
 * 2009/9/7, Jackson Lee, 修改傳入參數，移除業別代號欄位 void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department,
 *           String cancelCode, IncomeIdType incomeIdType, String incomeId)throws ExpException, ExpRuntimeException;
 * 2009/9/11, Eustace Hsieh, 修正 void checkPrepaymentAccTtleColumns(ExpapplC expapplC);
 * 2009/9/11, Eustace Hsieh, 新增 BigDecimal caculateRealityAmount(ExpapplC expapplC);
 * 2009/9/18, Eustace Hsieh, 修正 void toDelEntrys(ExpapplC expapplC, List&lt;Entry&gt; toDelEntryList) throws ExpException;
 * 2009/10/12, Eustace Hsieh, 修正 void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department,
 *             BigDecimal amt, String cancelCode, IncomeIdType incomeIdType, String incomeId, String expSummary, String industryCode);
 * 2009/10/12, Eustace Hsieh, 修正 void verifyExpapplC(ExpapplC expapplC);
 * 2009/10/12, Eustace Hsieh, 新增 void verifyExpapplC(ExpapplC expapplC, Boolean isCarriedByStages);
 * 2009/10/13, Eustace Hsieh, 新增 void doRtnItemReapplied(String expApplNo, FunctionCode functionCode);
 * 2009/10/13, Eustace Hsieh, 新增 void doConfirmApplied(List<String> expApplNoList, FunctionCode functionCode);
 * 2009/10/16, Eustace Hsieh, 更新 void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department,
 *             BigDecimal amt, String cancelCode, String industryCode, String expSummary, IncomeIdType incomeIdType, String incomeId);
 * 2009/10/22, Eustace Hsieh, 新增 void checkExpApplNo(String expApplNo);
 * 2010/01/06, Sunkist Wang, 新增 List&lt;VendorExpDto&gt; findVendorExpDto(Calendar startDate, Calendar endDate);
 * 2010/01/11, Sunkist Wang, 新增 List&lt;CostCodeDto&gt; findCostCodeQuery(String expapplNo, Calendar subpoenaDate, String applUserCode);
 * 2010/01/12, Sunkist Wang, 新增 void updateCostCode(String expapplNo, String costCode);
 * 2010/01/12, Sunkist Wang, move findCostCodeQuery(), updateCostCode() to ChangeAccruedExpApplService.
 * </pre>
 * 
 */
public interface ExpapplCService extends BaseService<ExpapplC, String> {

	/**
	 * 依條件查詢行政費用資料
	 * <p>
	 * C3.2.2 核銷行政費用 -- 查詢
	 * </p>
	 * <ul>
	 * <li>"費用大分類"、"費用大分類"、"初審經辦(員工)代碼"及"是否臨時付款"為必要輸入欄位。</li>
	 * <li>依"費用中分類、經辦序號"排序。</li>
	 * </ul>
	 * <p>
	 * 查詢
	 * </p>
	 * <ul>
	 * <li>TODO</li>
	 * </ul>
	 * 
	 * @param bigTypeCode
	 *            費用大分類
	 * @param firstVerifyUserCode
	 *            初審經辦
	 * @param deliveryUnitCode
	 *            送件單位代號
	 * @param costUnitCode
	 *            成本單位代碼
	 * @param applyUserCode
	 *            申請人代碼
	 * @param isTempPay
	 *            是否臨時付款
	 * @param returnDate
	 *            退件日期
	 * @param states
	 *            申請單狀態
	 * @return
	 */
	List<ExpapplC> findForGovApprove(String bigTypeCode, String firstVerifyUserCode, String deliveryUnitCode, String costUnitCode, String applyUserCode, boolean isTempPay, Calendar returnDate, ApplStateCode[] states);

	/**
	 * 將費用申請單狀態變更為"退件"
	 * <p>
	 * C3.2.2 核銷行政費用--退件
	 * </p>
	 * <ul>
	 * <li>記錄流程簽核歷程(系統須記錄該申請單之退件日期、退件原因、初審經辦)</li>
	 * <li>費用申請單狀態變更為"退件"</li>
	 * </ul>
	 * 
	 * <p>
	 * 檢核條件
	 * </p>
	 * <ul>
	 * <li>檢核該申請單，狀態為"審核中"才可退件，若狀態不為"審核中"顯示《狀態錯誤，不可退件》訊息 ，且須回復已更新狀態之資料列。</li>
	 * </ul>
	 * 
	 * <p>
	 * PRE
	 * </p>
	 * <ul>
	 * <li>費用申請單狀態為"審核中"</li>
	 * </ul>
	 * <p>
	 * POST
	 * </p>
	 * <ul>
	 * <li>將費用申請單狀態變更為"退件"</li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param returnStatement
	 *            退件原因說明
	 * @return expapplC 費用申請單
	 */
	ExpapplC doReturnGovApplyForm(ExpapplC expapplC, ReturnStatement returnStatement) throws ExpException;

	/**
	 * 補辦費用申請單，狀態變更為"審核中"
	 * <p>
	 * C3.2.2 核銷行政費用 -- 補辦
	 * </p>
	 * <ul>
	 * <li>記錄流程簽核歷程(系統須更新該申請單之"補辦完成日期"、"初審經辦")</li>
	 * <li>費用申請單狀態變更為"審核中"</li>
	 * </ul>
	 * 
	 * <p>
	 * 檢核條件
	 * </p>
	 * <ul>
	 * <li>檢核申請單，狀態為"退件"才可回覆補辦完成，若狀態不為"退件"顯示《狀態錯誤，尚未完成補辦》訊息， 且須回復已更新狀態之資料列。</li>
	 * </ul>
	 * 
	 * <p>
	 * PRE
	 * </p>
	 * <ul>
	 * <li>費用申請單狀態為"退件"</li>
	 * </ul>
	 * <p>
	 * POST
	 * </p>
	 * <ul>
	 * <li>將費用申請單狀態變更為"審核中"</li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * 
	 */
	ExpapplC doReapplyGovApplyForm(ExpapplC expapplC) throws ExpException;

	/**
	 * 核銷費用申請單
	 * 
	 * <p>
	 * C3.2.2 核銷行政費用 -- 核銷
	 * </p>
	 * 
	 * <p>
	 * 檢核條件
	 * </p>
	 * <ol>
	 * <li>選擇核銷之申請單下所有費用申請單狀態修改為"已初審"時，顯示《"件數(系統計算選擇核銷之筆數)"已核銷》訊息</li>
	 * <li>若選擇核銷之費用申請單部份狀態不為"審核中"時，顯示《狀態錯誤，尚未完成核銷》訊息</li>
	 * </ol>
	 * <p>
	 * PRE
	 * </p>
	 * <ul>
	 * <li>費用申請單狀態為"審核中"</li>
	 * </ul>
	 * <p>
	 * POST
	 * </p>
	 * <ul>
	 * <li>將費用申請單狀態變更為"已初審"</li>
	 * </ul>
	 * 
	 * @param expApplcs
	 *            費用申請單s
	 * @return Integer 核銷筆數
	 */
	Integer doApproveGovApplyForm(List<ExpapplC> expApplCs) throws ExpException;

	/**
	 * UC3.2.2核銷行政費用 刪除費用申請單，將傳入的申請單號的費用申請單狀態改為"刪除"
	 * 
	 * <p>
	 * PRE
	 * </p>
	 * <ul>
	 * <li>費用申請單狀態為"審核中"</li>
	 * </ul>
	 * <p>
	 * POST
	 * </p>
	 * <ul>
	 * <li>將費用申請單狀態變更為"已刪除"</li>
	 * </ul>
	 * 
	 * @param expApplcs
	 *            費用申請單s
	 * 
	 */
	void doDeleteGovApplyForm(List<ExpapplC> expApplcs) throws ExpException;

	/**
	 * C 3.2.2 修改行政費用
	 * <p>
	 * 系統依「費用申請單.申請單號」前3碼(費用中分類代號)， 決定修改時申請單的顯示頁面。修改完後儲存時， 檢核規則與原申請功能相同，參考各UC。
	 * </p>
	 * 
	 * @param expapplC
	 * @return
	 * @throws ExpException
	 */
	ExpapplC doUpdateGovApplForm(ExpapplC expapplC) throws ExpException;

	/**
	 * C 3.2.2 退件記錄
	 * <p>
	 * 按下申請人姓名，系統顯示"點選"的"費用申請單退件記錄"
	 * </p>
	 * 
	 * @param expApplNo
	 *            費用申請單號
	 * @return List<FlowCheckstatus> 費用申請單
	 */
	List<FlowCheckstatus> findReturnGovApplyRecords(String expApplNo) throws ExpException;

	/**
	 * 依送件單號查出相關聯的行政費用申請單
	 * <p>
	 * UC 3.1.1簽收費用申請單
	 * </p>
	 * 
	 * @param deliverDayListNo
	 *            送件日計表單號
	 */
	List<ExpapplC> findByDeliverDayListNo(String deliverDayListNo) throws ExpRuntimeException;

	/**
	 * 依送件單號查出相關聯的行政費用申請單
	 * <p>
	 * C 3.3.1列印日計表
	 * </p>
	 * 
	 * @param deliverDayListNos
	 *            送件日計表單號s
	 * @param applStateCodes
	 *            申請單狀態代碼s
	 * @return
	 * @throws ExpRuntimeException
	 */
	List<ExpapplC> findByDeliverDayListNos(List<String> deliverDayListNos, ApplStateCode[] applStateCodes) throws ExpRuntimeException;

	/**
	 * 將費用申請單狀態改為"已刪件"。
	 * 
	 * @param expapplCs
	 */
	void doConfirmDelete(List<ExpapplC> expapplCs);

	/**
	 * 補辦費用申請單，狀態變更為"審核中"
	 */
	ExpapplC doReapplyApplyForm();

	/**
	 * 將費用申請單狀態變更為"退件"
	 * <p>
	 * UC2.1.1審查調查、業務稽查費用
	 * </p>
	 * <ul>
	 * <li>記錄流程簽核歷程</li>
	 * <li>費用申請單狀態變更為"退件"</li>
	 * </ul>
	 * <p>
	 * PRE
	 * </p>
	 * <ul>
	 * <li>費用申請單狀態為"審核中"</li>
	 * </ul>
	 * <p>
	 * POST
	 * </p>
	 * <ul>
	 * <li>將費用申請單狀態變更為"退件"</li>
	 * </ul>
	 */
	ExpapplC doReturnInvestApplyForm();

	/**
	 * UC1.6.8退件送件表
	 * 
	 * <ol>
	 * <li>將使用者勾選的所有費用申請單，變更費用申請單狀態:
	 * <ul>
	 * <li>若原申請單狀態為「退件」時狀態變更為「重新送件」</li>
	 * <li>若原申請單狀態為「退單重送」時狀態變更為「退單審核」</li>
	 * </ul>
	 * </li>
	 * <li>儲存申請單資料，並記錄流程簽核歷程</li>
	 * </ol>
	 * 
	 * @param applyFormNoList
	 *            費用申請單單號s
	 * @param functionCode
	 *            功能代碼
	 */
	void doRtnItemResend(List<String> applyFormNoList, FunctionCode functionCode) throws ExpException;

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
	List<RtnItemApplDto> findRtnItemApplDto(ApplStateCode applStateCode, BigTypeCode bigTypeCode, String deliverDaylistUser, String payYearMonth) throws ExpException, ExpRuntimeException;

	/**
	 * B 2.5 找出指定經辦所屬，可供"產生日計表"所使用的費用申請單。
	 * 
	 * <p>
	 * CRIT
	 * </p>
	 * <ol>
	 * <li>費用申請單狀態為"已初審"。</li>
	 * <li>付款方式為"匯款"，付款對象為"單位"，費用中分類為 N10, M10, M20, L10, L20, R20, H20, J40,
	 * K10, K20。</li>
	 * <li>或付款方式為"開票"，費用中分類為 N10。</li>
	 * <li>付款方式為"借支沖轉"，付款對象為單位，費用中分類為N10辦公費實報實支費用和E10一般費用。</li>
	 * </ol>
	 * 
	 * @param user
	 * @return
	 */
	List<ExpapplC> findApplyForDailyStmt(User user);

	/**
	 * B 1.3 行政費用已匯款之費用列表。
	 * 
	 * @return
	 */
	List<ExpapplC> findRemittedApply();

	/**
	 * C 11.7.2 查詢銷帳碼
	 * 
	 * @param cancelCodeType
	 *            銷帳碼類別 1.借支銷號碼 2.應付費用銷號碼 3.應付人事室費用 4.預付費用銷號碼 5.分期付款銷帳碼 6.商務卡銷帳碼
	 * @param stateType
	 *            狀態 0.全部 1.已內結 2.未內結
	 * @param bigType
	 *            費用大分類
	 * @param cancelCode
	 *            銷帳碼
	 * @param createDateBegin
	 *            建檔日期起
	 * @param createDateEnd
	 *            建檔日期訖
	 * @return
	 */
	List<ExpapplC> findByCancelCode(Integer cancelCodeType, Integer stateType, BigType bigType, String cancelCode, Calendar createDateBegin, Calendar createDateEnd);

	/**
	 * 依申請單號查出費用申請單
	 * 
	 * @param expApplNoList
	 *            申請單號List
	 * @return List<ExpapplC>
	 */
	List<ExpapplC> findByApplNo(List<String> expApplNoList);

	/**
	 * 依條件查詢出行政費用申請單
	 * 
	 * <p>
	 * UC 3.3.1 列印日計表
	 * </p>
	 * 
	 * @param deliverNos
	 *            送件日計表單號
	 * @param bigType
	 *            大分類
	 * @param temporaryPayment
	 *            臨時付款
	 * @param userCode
	 *            實際初審經辦代碼
	 * @param applStateCode
	 *            申請單狀態
	 * @return
	 */
	List<ExpapplC> findByParams(List<String> deliverNos, BigType bigType, Boolean temporaryPayment, String userCode, ApplStateCode applStateCode);

	/**
	 * 依條件查出一般費用資料，以供費用申請使用。
	 * 
	 * <ul>
	 * <li>一併查回一般費用
	 * </ul>
	 * 
	 * <p>
	 * C 1.6.1 一般費用申請紀錄表 -- 查詢
	 * </p>
	 * 
	 * @param applStateCodeArray
	 *            申請單狀態
	 * @param middleTypeCode
	 *            費用中分類代碼
	 * @param applUserCode
	 *            申請人員工代號
	 * @param proofTypeCode
	 *            憑證類別代碼
	 * @param createDateStart
	 *            建檔日 起
	 * @param createDateEnd
	 *            建檔日 迄
	 * @param temporaryPayment
	 *            臨時付款
	 * @param deliverNo
	 *            送件日計表單號
	 * @param createUserCode
	 *            建檔人員工代號 : (選擇性輸入)空白時，代入登入人員員工代號
	 * @return List<ExpapplC>
	 */
	List<ExpapplC> findForExpapplCFetchRelation(ApplStateCode[] applStateCodeArray, MiddleTypeCode[] middleTypeCodeArray, String applUserCode, ProofTypeCode proofTypeCode, Calendar createDateStart, Calendar createDateEnd, Boolean temporaryPayment, String deliverNo, String createUserCode);

	/**
	 * 依日計表單號找出行政費用申請單
	 * <p>
	 * 1.6.7 產生費用送件表
	 * </p>
	 * <p>
	 * UC 3.3.1 列印日計表
	 * </p>
	 * 
	 * @param deliverNo
	 *            日計表單號
	 * @return
	 * @throws ExpException
	 */
	List<ExpapplC> findByDeliverNo(String deliverNo) throws ExpException;

	// /**
	// * 依條件找出行政費用申請單
	// * @param deliverNoList 日計表單號List
	// * @param applStateCode 申請單狀態代碼(可null)
	// * @return
	// * @throws ExpException
	// */
	// List<ExpapplC> findByDeliverNo(List<String> deliverNoList, ApplStateCode
	// applStateCode)throws ExpException;

	/**
	 * 檢核費用申請單的進項稅相關欄位資訊
	 * <p>
	 * 檢核費用申請單的進項稅相關欄位資訊，含發票資料檢核
	 * </p>
	 * <ol>
	 * <li>If「費用申請單.是否扣繳進項稅」=true</li>
	 * <ul>
	 * <li>If「費用申請單.憑證類別.格式代號」為「21、23 、25、26、27」其中任一個值</li>
	 * <ol>
	 * <li>以下欄位不可為空值。若空值時，throw ExpRuntimeExcption，並顯示《"需輸入進項稅相關欄位"!》訊息</li>
	 * <ul>
	 * <li>費用申請單.發票日期</li>
	 * <li>費用申請單.發票號碼</li>
	 * <li>費用申請單.統一編號??</li>
	 * <li>費用申請單.憑證金額(未) 不可為0</li>
	 * <li>費用申請單.憑證金額(稅) 不可為0</li>
	 * <li>費用申請單.課稅別</li>
	 * <li>費用申請單.扣抵代號</li>
	 * </ul>
	 * <li>執行共用function《檢核費用申請單中，發票號碼是否合法》</li>
	 * <ul>
	 * <li>Else If「費用申請單.憑證類別.格式代號」為「22、24」其中任一個值， 依下述規則檢核，若不符合檢核條件時，throw
	 * ExpRuntimeExcption，並顯示《"需輸入進項稅相關欄位"!》訊息：</li>
	 * <ul>
	 * <li>「費用申請單.憑證金額(含)」、「費用申請單.憑證金額(未)」、「費用申請單.憑證金額(稅)」等欄位不可為0</li>
	 * <li>費用申請單.課稅別、費用申請單.扣抵代號 不可為空白</li>
	 * <li>「費用申請單.發票號碼」可為空白; 但不為空白時，需檢核字軌及發票號碼是否正確或重覆，亦需檢核”發票日期”需有值。(參考總綱)</li>
	 * <ul>
	 * <li>若發票號碼有值，執行共用function《檢核費用申請單中，發票號碼是否合法》</li>
	 * </ul>
	 * <li>發票號碼重覆不可儲存入檔</li>
	 * </ul>
	 * </ul>
	 * </ol>
	 * </ul> <li>回傳至主程式</li> </ol>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void checkInvoiceData(ExpapplC expapplC);

	/**
	 * <p>
	 * 提供費用申請單資料，讓發票是否重複的檢核使用
	 * </p>
	 * 
	 * @param params
	 *            查詢條件
	 * @return 費用申請單 List
	 */
	List<ExpapplC> findExpapplCbyInvoiceData(Map<String, Object> params);

	/**
	 * 檢核費用申請單中，發票號碼是否合法
	 * <p>
	 * 依傳入的費用申請單的發票日期、發票號碼，檢核是否合法。發票不能重覆申請，且字軌需在字軌檔中出現
	 * </P>
	 * <ol>
	 * <li>If傳入的「費用申請單.是否抵扣進項稅」=N。回傳至主程式</li>
	 * <li>If傳入參數為null值，或傳入的「費用申請單.發票日期」為null值， 或傳入的「費用申請單.發票號碼」為空值，throw
	 * ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>if「費用申請單.憑證附於」是空值，執行共用function《檢核發票號碼是否重覆》(參考SDD總綱)，
	 * 參數傳入”費用申請單.發票號碼”、”費用申請單.發票日期”的年月(YYYYMM)。 如果回傳true，throw
	 * ExpRuntimeExceiption，顯示《發票號碼重覆申請》</li>
	 * <ul>
	 * <li>Else if「費用申請單.憑證附於」不是空值，執行步驟3。不檢查發票重覆</li>
	 * </ul>
	 * <li>執行共用function《檢核字軌》(參考SDD總綱)。若回傳false，throw
	 * ExpRuntimeExceiption，顯示”發票字軌有誤”</li>
	 * <li>回傳至主程式
	 * <li>
	 * </ol>
	 * 
	 * @param expapplC
	 */
	void checkInvoiceLegality(ExpapplC expapplC);

	/**
	 * 計算費用申請單中，應付費用金額
	 * <p>
	 * 依傳入的費用申請單的分錄，計算應付費用金額。金額為借方減貸方之差額
	 * </p>
	 * <ol>
	 * <li>If傳入參數為null值，或傳入的「費用申請單.分錄群組」為null值， 或傳入的「費用申請單.分錄群組.分錄」為null值，throw
	 * ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>建立兩個BigDecimal變數: 借方總額、貸方總額</li>
	 * <li>for all 「費用申請單.分錄群組.分錄」</li>
	 * <p>
	 * If 「分錄.科目借貸別.借貸別值」=D
	 * </p>
	 * <p>
	 * 借方總額 =借方總額+「費用申請單.分錄群組.分錄.金額」
	 * </p>
	 * <p>
	 * Else
	 * </p>
	 * <p>
	 * 貸方總額 =貸方總額+「費用申請單.分錄群組.分錄.金額」
	 * </p>
	 * <li>if 借方總額>貸方總額，回傳(借方總額-貸方總額)的值; 否則回傳0</li>
	 * </ol>
	 * 
	 * @param expapplC
	 * @return 分錄是否平衡 BigDecimal
	 */
	BigDecimal caculateAccruedExpenseAmount(ExpapplC expapplC);

	/**
	 * 取得實付金額
	 * <p>
	 * 實付金額 = 借方總額
	 * 
	 * @param expapplC
	 * @return
	 */
	BigDecimal caculateRealityAmount(ExpapplC expapplC);

	/**
	 * 檢查申請單號是否存在(狀態不為刪除),查詢有核定申請單與行政費用申請單
	 * <p>
	 * 以申請單號查詢費用申請單，檢查是否存在狀態不為刪除的申請單
	 * </p>
	 * <ol>
	 * <li>以下列條件查詢費用申請單</li>
	 * <ul>
	 * <li>查詢條件:</li>
	 * <ol>
	 * <li>select「費用申請單」</li>
	 * <li>「費用申請單.申請單號」=傳入參數”申請單號”</li>
	 * </ol>
	 * <li>「費用申請單.申請單狀態」不等於”刪除”</li>
	 * <li>JOIN 條件</li>
	 * <p>
	 * INNER JOIN 「費用申請單」ON「費用申請單.分錄群組」=「分錄群組.ID」
	 * </p>
	 * </ul>
	 * </ol>
	 * 
	 * @param expApplNo
	 *            申請單號
	 * @return 申請單是否存在 boolean
	 */
	boolean isExpApplNoExists(String expApplNo);

	/**
	 * 檢核預付性質費用的必輸入欄位
	 * <p>
	 * 檢核費用申請單資料中，若傳入的申請單包含的分錄中， 任何會計科目為預付性質的科目，則檢查必要的欄位是否都有輸入
	 * </p>
	 * <ol>
	 * <li>if傳入參數”費用申請單”、”費用申請單.分錄群組” 或”費用申請單.分錄群組.分錄”為NULL時，throw
	 * ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>在 ”費用申請單.分錄群組.分錄”List中，依序取得”分錄.會計科目”</li>
	 * <li>對”分錄.會計科目”做以下檢查:</li>
	 * <ul>
	 * <li>if”分錄.會計科目.科目代號”=”63100223”(材料用品-用品費-報章雜誌)時，不用檢查，繼續執行步驟2，檢查下一個分錄</li>
	 * <li>若”分錄.會計科目.是否預付”=true</li>
	 * <ol>
	 * <li>檢查“費用申請單.預付起始日期”, “費用申請單.預付終止日期”等欄位若任一個為NULL，則throw
	 * ExpRuntimeException，顯示《預付起訖日期為必填!》訊息</li>
	 * <li>若皆不是NULL，則回傳主程式(return)</li>
	 * </ol>
	 * <li>若”分錄.會計科目.是否預付”=false</li>
	 * <ol>
	 * <li>繼續執行步驟2，檢查下一個分錄</li>
	 * </ol>
	 * </ul>
	 * <li>回傳主程式(return)</li>
	 * </ol>
	 * 
	 * @param expapplc
	 *            費用申請單
	 */
	void checkPrepaymentAccTtleColumns(ExpapplC expapplC);

	/**
	 * 重設費用申請單
	 * <p>
	 * 使用者按下”重設”按鈕時，重設費用申請單的部份欄位
	 * </p>
	 * <ol>
	 * <li>if傳入參數為NULL，throw ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>將「費用申請單.分錄群組.分錄」設為NULL(清空)</li>
	 * <li>設定以下的值:</li>
	 * <ul>
	 * <li>設定「費用申請單.所得稅額」=0</li>
	 * <li>設定「費用申請單.印花稅額」=0</li>
	 * <li>設定「費用申請單.實付金額」=「費用申請單.憑證金額(含)」</li>
	 * <li>設定「費用申請單.冊號類別」=null</li>
	 * <li>設定「費用申請單.冊號1」=null、「費用申請單.冊號2」=null</li>
	 * </ul>
	 * <li>回傳至主程式</li>
	 * </ol>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void resetExpapplC(ExpapplC expapplC);

	/**
	 * 產生應付費用科目帳務資料(分錄)
	 * <p>
	 * 依傳入的費用申請單所包含的分錄資料，產生應付費用科目帳務資料(分錄)，並回寫至傳入的費用申請單
	 * </p>
	 * <ol>
	 * <li>if傳入參數為NULL或不包含任何資料時(size=0)，throw ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>計算: 應付費用金額=借方分錄總額 - 貸方分錄總額</li>
	 * <li>依費用大分類取得貸方應付費用科目(最多二筆):</li>
	 * <ul>
	 * <li>若傳入參數「費用申請單.費用中分類.費用大分類」=”01廠商費用”</li>
	 * <p>
	 * 依應付費用貸方科目=”20210360應付費用-總務費用” 產生分錄
	 * </p>
	 * <li>若傳入參數「費用申請單.費用中分類.費用大分類」=”04醫檢費”</li>
	 * <p>
	 * 依應付費用貸方科目=” 應付費用-待匯”(20210391)” 產生分錄
	 * </p>
	 * <li>其他(02已付費用、03研修差旅、05公務車、07不動產費用)</li>
	 * <ul>
	 * <li>「付款方式」=匯款:</li>
	 * <p>
	 * 依銀行存款過渡科目: ”應付費用-待匯”(20210391)產生分錄
	 * </p>
	 * <li>「付款方式」=開票:</li>
	 * <p>
	 * 依銀行存款過渡科目: ”應付費用-待開”(20210392)產生分錄
	 * </p>
	 * <li>「付款方式」=約定轉帳扣款、現金:</li>
	 * <p>
	 * 依銀行存款過渡科目: ”應付費用-待付”(20210393) 產生分錄
	 * </p>
	 * <li>「付款方式」=沖轉暫付 或 沖轉預付:</li>
	 * <ol>
	 * <li>if傳入參數”銷帳碼”為空值，throw ExpRuntimeExceiption，顯示”傳入參數錯誤”</li>
	 * <li>計算可沖轉的借支餘額，執行共用function《計算可沖轉借支餘額》</li>
	 * <li>If “可沖轉的借支金額”小於或等於 0, 則顯示《銷帳碼錯誤或無餘額!》，且不可儲存入檔</li>
	 * <li>取得要產生的貸方會計科目，執行共用function《查詢可沖轉的借支分錄》</li>
	 * <li>產生分錄:</li>
	 * <ul>
	 * <li>If “可沖轉的借支金額” 大於 應付費用金額:</li>
	 * <p>
	 * 產生一筆貸方科目的分錄:
	 * </p>
	 * <ul>
	 * <li>「分錄.會計科目」=” 可沖轉的借支分錄.會計科目”;「分錄.金額」=應付費用金額; 「分錄.銷帳碼」=傳入參數”銷帳碼”</li>
	 * </ul>
	 * <li>If “可沖轉的借支金額” 小於 應付費用金額:</li>
	 * <p>
	 * 產生二筆貸方科目的分錄:
	 * </p>
	 * <ul>
	 * <li>「分錄.會計科目」=” 可沖轉的借支分錄. 會計科目” ;「分錄.金額」=可沖轉的借支餘額; 「分錄.銷帳碼」=傳入參數”銷帳碼”</li>
	 * <li>「分錄.會計科目」=應付費用-待匯”(20210391); 「分錄.金額」=”應付費用金額” - “可沖轉的借支餘額”</li>
	 * </ul>
	 * </ul>
	 * </ol>
	 * </ul> </ul> <li>將產生的應付費用科目，新增至「費用申請單.分錄群組.分錄」的List中</li> <li>回傳至主程式</li>
	 * </ol>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param cancelCode
	 *            銷帳碼(付款方式為沖轉暫付 或 沖轉預付時使用)
	 */
	void generatePayableExpenseEntry(ExpapplC expapplC, String cancelCode);

	/**
	 * 產生費用申請單號。
	 * <p>
	 * 編碼原則
	 * </p>
	 * <ul>
	 * <li>型態（長度）: 字元(15)</li>
	 * <li>編碼原則: 核銷代號(3)+西元年(4)月日(4)+流水序號(4)</li>
	 * <li>註：費用中分類前三碼即為核銷代號</li>
	 * </ul>
	 * <ul>
	 * </ul>
	 * 
	 * @param param1
	 *            核銷代號三碼
	 * @return
	 */
	String generateExpApplNo(String param1);

	/**
	 * 以名冊冊號，產生申請獎金品時的分錄(不包含應付費用科目)
	 * <p>
	 * UC1.5.1、1.5.7、UC 1.5.9、1.5.13
	 * </p>
	 * 
	 * <p>
	 * 說明
	 * </p>
	 * <ul>
	 * <li>是否產生應付代扣科目欄位
	 * <ul>
	 * <li>UC1.5.1、1.5.13: 當付款方式為沖轉暫付時，為稅金匯回，不產生應付代扣科目</li>
	 * <li>UC1.5.9: 一定是稅金回，不產生應付代扣科目</li>
	 * <li>UC1.5.7: 要產生應付代扣科目</li>
	 * </ul>
	 * </li>
	 * <li>兩個冊號所對應到的借方科目一樣時，產生的借方分錄合併為同一筆，否則借方拆為兩筆。(例如:一本國、一外國)</li>
	 * <li>分錄產生後，設定名冊資料的值:
	 * <ul>
	 * <li>「名冊.名冊狀態」=請領完畢</li>
	 * <li>「名冊.已使用金額」=已產生分錄的金額</li>
	 * <li>「名冊.費用申請單號」=傳入的”費用申請單號”</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param departmentCode
	 *            成本單位代號
	 * @param boolean1
	 *            是否代扣所得稅(產生應付代扣貸方科目)
	 * @return
	 */
	List<Entry> doGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean boolean1);

	/**
	 * 以名冊冊號，產生申請獎金品時的分錄(不包含應付費用科目)
	 * <p>
	 * UC 1.5.9 輸入廠商費用核銷申請資料
	 * </p>
	 * 
	 * <p>
	 * 說明
	 * </p>
	 * <ul>
	 * <li>是否產生應付代扣科目欄位
	 * <ul>
	 * <li>UC1.5.1、1.5.13: 當付款方式為沖轉暫付時，為稅金匯回，不產生應付代扣科目</li>
	 * <li>UC1.5.9: 一定是稅金回，不產生應付代扣科目</li>
	 * <li>UC1.5.7: 要產生應付代扣科目</li>
	 * </ul>
	 * </li>
	 * <li>兩個冊號所對應到的借方科目一樣時，產生的借方分錄合併為同一筆，否則借方拆為兩筆。(例如:一本國、一外國)</li>
	 * <li>分錄產生後，設定名冊資料的值:
	 * <ul>
	 * <li>「名冊.名冊狀態」=請領完畢</li>
	 * <li>「名冊.已使用金額」=已產生分錄的金額</li>
	 * <li>「名冊.費用申請單號」=傳入的”費用申請單號”</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param departmentCode
	 *            成本單位代號
	 * @param boolean1
	 *            是否代扣所得稅(產生應付代扣貸方科目)
	 * @param summary
	 *            摘要
	 * @param expapplCDetail
	 *            費用明細 IISI-20100805 : 修正費用明細沒有存入DB問題
	 * @return
	 */
	List<Entry> doGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean boolean1, String summary, ExpapplCDetail expapplCDetail);

	/**
	 * 依費用申請單號，變更名冊狀態
	 * <p>
	 * UC 1.5.9 輸入廠商費用核銷申請資料
	 * </p>
	 * <p>
	 * 功能說明
	 * </p>
	 * <ul>
	 * 使用名冊的UC有: UC1.5.1、UC1.5.7、UC1.5.9(不產生應付代扣科目)、UC1.5.13 及其對應的修改資料UC
	 * <li>新增/修改申請單
	 * <p>
	 * 若該申請單在新增/修改後，為名冊的申請單，儲存後要變更該名冊的欄位:
	 * <ol>
	 * <li>「名冊.名冊狀態」=請領完畢</li>
	 * <li>「名冊.已使用金額」=申請總額</li>
	 * <li>「名冊.費用申請單號」=傳入的”費用申請單.申請單號”</li>
	 * </ol>
	 * </li>
	 * <li>修改申請單
	 * <p>
	 * 若該申請單在修改前，為名冊的申請單，儲存後要復原該名冊的欄位:
	 * <ol>
	 * <li>「名冊.名冊狀態」設為尚未請領。</li>
	 * <li>「名冊.已使用金額」設為0</li>
	 * <li>「名冊.費用申請單號」設為null</li>
	 * </ol>
	 * </li>
	 * </ul>
	 * 
	 * @param state
	 *            名冊狀態代碼(0: 請領名冊 1:復原名冊狀態)
	 * @param expApplNo
	 *            費用申請單號
	 * @param listNos
	 *            最多包含兩個字串: 冊號1、冊號2(Option)
	 */
	void updateRosterState(Integer state, String expApplNo, List<String> listNos);

	/**
	 * 依費用申請單號，變更名冊狀態
	 * <p>
	 * UC 1.5.1 輸入一般費用核銷申請資料
	 * </p>
	 * <p>
	 * 功能說明
	 * </p>
	 * <ul>
	 * 使用名冊的UC有: UC1.5.1
	 * <li>新增/修改申請單
	 * <p>
	 * 若該申請單在新增/修改後，為名冊的申請單，儲存後要變更該名冊的欄位:
	 * <ol>
	 * <li>「報名費明細.是否已轉入申請單」=true</li>
	 * <li>「報名費明細.更新人員」=系統登入者</li>
	 * <li>「報名費明細.更新日期」=系統日</li>
	 * </ol>
	 * </li>
	 * <li>修改申請單
	 * <p>
	 * 若該申請單在修改前，為名冊的申請單，儲存後要復原該名冊的欄位:
	 * <ol>
	 * <li>「報名費明細.是否已轉入申請單」=false</li>
	 * <li>「報名費明細.更新人員」=系統登入者</li>
	 * <li>「報名費明細.更新日期」=系統日</li>
	 * </ol>
	 * </li>
	 * </ul>
	 * 
	 * @param state
	 *            名冊狀態代碼(1: 已申請 0:復原為未申請)
	 * @param expApplNo
	 *            費用申請單號
	 * @param listNo
	 *            冊號1
	 */
	void updateRegisterRosterState(Integer state, String expApplNo, String listNo);

	/**
	 * 依費用申請單號，變更名冊狀態
	 * <p>
	 * UC 1.5.1 輸入一般費用核銷申請資料
	 * </p>
	 * <p>
	 * 功能說明
	 * </p>
	 * <ul>
	 * 使用名冊的UC有: UC1.5.1
	 * <li>新增/修改申請單
	 * <p>
	 * 若該申請單在新增/修改後，為名冊的申請單，儲存後要變更該名冊的欄位:
	 * <ol>
	 * <li>「電話費明細.是否已轉入申請單」=true</li>
	 * <li>「電話費明細.更新人員」=系統登入者</li>
	 * <li>「電話費明細.更新日期」=系統日</li>
	 * </ol>
	 * </li>
	 * <li>修改申請單
	 * <p>
	 * 若該申請單在修改前，為名冊的申請單，儲存後要復原該名冊的欄位:
	 * <ol>
	 * <li>「電話費明細.是否已轉入申請單」=false</li>
	 * <li>「電話費明細.更新人員」=系統登入者</li>
	 * <li>「電話費明細.更新日期」=系統日</li>
	 * </ol>
	 * </li>
	 * </ul>
	 * 
	 * @param state
	 *            名冊狀態代碼(1: 已申請 0:復原為未申請)
	 * @param expApplNo
	 *            費用申請單號
	 * @param listNo
	 *            冊號1
	 */
	void updatePhoneRosterState(Integer state, String expApplNo, String listNo);

	/**
	 * 產生預設行政費用帳務資料(分錄)
	 * <p>
	 * 說明
	 * </p>
	 * <ol>
	 * <li>按下”更新費用明細”時產生。(UC1.5.3在儲存時會自動再執行一次)
	 * <p>
	 * UC1.5.1、UC1.5.3、UC1.5.5、UC1.5.6、UC1.5.7、UC1.5.8、UC1.5.12、UC1.5.1</li>
	 * <li>折讓單相關UC
	 * <ul>
	 * <li>「費用申請單.是否開立折讓單」= true: UC1.5.1、UC1.5.13</li>
	 * <li>產生費用科目的折讓單貸方進項稅分錄: UC1.5.1</li>
	 * </ul>
	 * </li>
	 * <li>所得稅相關UC (只有一個所得人)
	 * <ul>
	 * <li>UC1.5.1、UC1.5.3、UC1.5.13</li>
	 * </ul>
	 * </li>
	 * <li>執行此共用程式前，若有需要產生以下相對應的分錄資料，在傳入參數”費用申請單”必須將金額先填入相對應的欄位:
	 * <ul>
	 * <li>「費用申請單.進項稅額」</li>
	 * <li>「費用申請單.印花稅額」</li>
	 * <li>「費用申請單.所得稅額」</li>
	 * <li>「費用申請單.是否扣繳印花稅」</li>
	 * <li>「費用申請單.是否扣繳進項稅」</li>
	 * </ul>
	 * </li>
	 * <li>關於傳入參數費用科目金額(明細金額)
	 * <ul>
	 * <li>若畫面上有”明細金額”欄位，則帶入”明細金額”(如: UC1.5.7、UC1.5.8)</li>
	 * <li>若傳入NULL或0依下列規則產生金額:
	 * <ul>
	 * <li>若「費用申請單.是否扣繳進項稅」=True時，帶入「費用申請單.憑證金額(未)」欄位</li>
	 * <li>若否，帶入「費用申請單.憑證金額(含)」欄位</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ol>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param accTitle
	 *            費用科目
	 * @param department
	 *            成本單位(用來產生費用科目，必填)
	 * @param amt
	 *            費用科目(明細金額,此參數若為null或小於等於0,則使用憑證金額(含))
	 * @param cancelCode
	 *            銷帳碼(若無銷帳碼，此參數傳null)
	 * @param industryCode
	 *            業別代號
	 * @param expSummary
	 *            費用科目摘要
	 * @param incomeIdType
	 *            所得人證號類別
	 * @param incomeId
	 *            所得人證號
	 */
	void generateDefaultEntries(ExpapplC expapplC, AccTitle accTitle, Department department, BigDecimal amt, String cancelCode, String industryCode, String expSummary, IncomeIdType incomeIdType, String incomeId);

	/**
	 * 以日結單代傳票，查詢費用申請單資料(包含ExpapplCDetail)
	 * <p>
	 * C 4.1.2
	 * </p>
	 * 
	 * @param subpoena
	 *            日結單代傳票
	 * @return List<ExpapplC>
	 */
	List<ExpapplC> findExpapplCsBySubpoena(Subpoena subpoena);

	/**
	 * 以日結單代傳票，查詢費用申請單資料(不包含ExpapplCDetail)
	 * <p>
	 * C 5.1.1
	 * </p>
	 * 
	 * @param subpoena
	 *            日結單代傳票
	 * @return List<ExpapplC>
	 */
	List<ExpapplC> findExpapplCsWithoutExpapplCDetailBySubpoena(Subpoena subpoena);

	/**
	 * 處理要刪除的分錄
	 * <p>
	 * UC 1.5.5 輸入國內研修差旅費用核銷申請資料
	 * </p>
	 * 
	 * @param toDelEntryList
	 *            要刪除的分錄
	 * @param expapplc
	 *            費用申請單
	 */
	void toDelEntrys(ExpapplC expapplC, List<Entry> toDelEntryList);

	/**
	 * 刪除分錄與相關的關聯資訊
	 * <p>
	 * UC 1.5.5 輸入國內研修差旅費用核銷申請資料
	 * </p>
	 * <ul>
	 * 流程
	 * <li>刪除-分錄.費用申請單明細</li>
	 * <li>刪除-過渡付款明細</li>
	 * <li>刪除-分錄</li>
	 * </ul>
	 * 
	 * @param delEntryList
	 *            分錄
	 */
	void doDelEntrys(List<Entry> delEntryList);

	/**
	 * 過濾應附科目
	 * 
	 * <p>
	 * UC 1.5.5 輸入國內研修差旅費用核銷申請資料
	 * </p>
	 * <p>
	 * 說明:
	 * </p>
	 * 
	 * <pre>
	 * 將原有的分錄群組.分錄s,放入待刪除分錄List
	 * 並將原有的分錄群組.分錄s內的應附科目移除
	 * </pre>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param toDelEntryList
	 *            待刪除的分錄List
	 * @throws ExpException
	 * @throws ExpRuntimeException
	 */
	void beforUpdateExp(ExpapplC expapplC, List<Entry> toDelEntryList) throws ExpException, ExpRuntimeException;

	/**
	 * 檢核行政費用申請單資料內容是否正確，包含各種規則
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void verifyExpapplC(ExpapplC expapplC);

	/**
	 * 檢核行政費用申請單資料內容是否正確，包含各種規則
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param isCarriedByStages
	 *            是否分期結轉
	 */
	void verifyExpapplC(ExpapplC expapplC, Boolean isCarriedByStages);

	/* RE201201260 二代健保 匯回款項 20130222 START */
	/**
	 * 檢核行政費用申請單資料內容是否正確，包含各種規則
	 * 
	 * @param paybackType
	 *            還款類別為溢付匯款時不需檢查憑證金額
	 */
	void verifyExpapplC(String paybackType);

	/* RE201201260 二代健保 匯回款項 20130222 End */

	/**
	 * <p>
	 * 共用---11.1.30 檢核婚喪禮金補助津貼限額
	 * </p>
	 * <p>
	 * 當費用申請單的費用項目為”主管婚喪禮金費用(62102000)”時，檢核其年度及單次限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void checkCashGiftApplQuota(ExpapplC expapplC);

	/**
	 * <p>
	 * 共用---11.1.38 檢核婚喪禮金補助津貼限額
	 * </p>
	 * <p>
	 * 當費用申請單的費用項目為”職員禮金、奠儀(費用項目代號61130100、會計科目代號61130223)”時，檢核其限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param expapplCDetail
	 *            費用明細
	 */
	void checkCashGiftAllowanceQuota(ExpapplC expapplC, ExpapplCDetail expapplCDetail);

	/**
	 * <p>
	 * C 1.5.1
	 * </p>
	 * <p>
	 * 以電話費冊號，產生申請電話費時的分錄(不包含應付費用科目)
	 * </p>
	 * <ul>
	 * <li>費用項目為"電話費"時，費用成本單位依"電話費冊號"之話機歸屬
	 * <p>
	 * (話機歸屬於UC10.3.2話機基本資料檔維護功能設定)歸屬各話機所屬部室成本；
	 * </P>
	 * <p>
	 * 若同一成本部室於該電話費冊號內有多筆費用明細時，
	 * </p>
	 * 帳務資料同一成本單位合計顯示一筆明細金額即可。</li>
	 * <li>不同部室要拆為多筆借方科目</li>
	 * <li>貸方依付款方式只產生一筆應付費用</li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param summary
	 *            費用科目分錄摘要
	 * @return 產生對應的費用借方科目(沒有應付代扣貸方科目)
	 */
	List<Entry> generateTelephoneFeeEntries(ExpapplC expapplC, String summary);

	/**
	 * <p>
	 * 共用---11.1.24 以個人申請限額，更新費用申請單的憑證金額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param accTitle
	 *            會計科目
	 * @param checkType
	 *            檢核方式(0:年度限額, 1:年月限額)
	 * @return 回傳數字0表示正常
	 */
	int modifyInvoiceAmtByPersonalApplyQuota(ExpapplC expapplC, AccTitle accTitle, String checkType);

	/**
	 * 
	 * <p>
	 * 共用11.1.36--在申請費用時， 檢核個人指定費用年月的申請限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void checkPersonalMonthApplyQuota(ExpapplC expapplC);

	/**
	 * <p>
	 * 共用11.1.37---計算已申請的費用申請單總額(月份總額)
	 * </p>
	 * 
	 * @param middleTypeCode
	 *            費用中分類代碼
	 * @param userId
	 *            申請人員工代碼(六碼)
	 * @param accTitleCode
	 *            科目代號(八碼)
	 * @param expYearMonth
	 *            費用年月(YYYYMM)
	 * @return BigDecimal 申請總額
	 * @param expNo
	 *            費用申請單號
	 */
	BigDecimal getAppliedTotalAmount(MiddleTypeCode middleTypeCode, String userId, String accTitleCode, String expYearMonth, String expNo);

	/**
	 * <p>
	 * 共用11.1.41--在申請費用時，檢核個人年度申請限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void checkPersonalYearApplyQuota(ExpapplC expapplC);

	/**
	 * <p>
	 * 共用11.1.42--計算指定費用年月已申請的費用項目總額(年份總額)
	 * </p>
	 * 
	 * @param middleTypeCode
	 *            費用中分類代號
	 * @param userId
	 *            申請人員工代碼(六碼)
	 * @param expYear
	 *            費用申請單.費用年月的年度(YYYY)
	 * @return BigDecimal 申請總額
	 */
	BigDecimal getAppliedTotalAmount(String middleTypeCode, String userId, String expYearMonth);

	/**
	 * <p>
	 * C10.8.3--計算指定使用者、費用年，已申請的費用項目=交通費-汽機車燃料費，各月已申請的總額
	 * </p>
	 * 
	 * @param userId
	 *            申請人員工代碼(六碼)
	 * @param expYear
	 *            費用年月(YYYY)
	 * @param middleType
	 *            費用中分類
	 * @return Map<String, BigDecimal> 每月申請總額
	 */
	Map<String, BigDecimal> getAppliedFuelAmountByYearAndUserId(String userId, String expYear, MiddleType middleType);

	// RE201300775 modify by michael in 2013/04/12 start
	/**
	 * 取得多筆QuotaDetail的消費金額
	 * 
	 * @param detail
	 * @return
	 */
	public Map<String, BigDecimal> getAppliedExpItemAmountByQuotaDetails(QuotaItem item, List<QuotaDetail> details);

	/**
	 * 取得QuotaDetail的消費金額
	 * 
	 * @param detail
	 * @return
	 */
	public BigDecimal getAppliedExpItemAmountByQuotaDetail(QuotaDetail detail);

	// RE201300775 modify by michael in 2013/04/12 end

	/**
	 * 當「費用申請單.成本別」=”W”時，所要做的控管:
	 * <p>
	 * <li>檢查W件是否開啟。</li>
	 * <li>若超支，於費用申請單之“摘要”欄位附加「W超支金額」字樣。</li>
	 * </P>
	 * </br>
	 * 
	 * <ol>
	 * <li>檢查W件是否開啟。</li>
	 * <ul>
	 * If 「W件申請控管.是否開放」= false, 則丟出帶有訊息《W件未開放申請!》的ExpException。
	 * </ul>
	 * <li>計算部門提列的應付費用總額:call
	 * {@link DepAccruedExpensesDetailService#sumEstimationAmtByExpapplC(tw.com.skl.exp.kernel.model6.bo.ExpapplC)}
	 * </li>
	 * <ul>
	 * 查詢條件
	 * <ol>
	 * <li>「部門提列應付費用申請單.提列年度」=傳入參數「費用申請單.費用年月」前4碼(西元年)</li>
	 * <li>「部門提列應付費用申請單.部門提列應付費用明細.成本單位代號」=傳入參數第一筆借方的「費用申請單.分錄群組.分錄.成本單位代號」</li>
	 * </ol>
	 * 資料篩選
	 * <ol>
	 * <li>「部門提列應付費用申請單.提列申請單狀態」=送審</li>
	 * </ol>
	 * JOIN 條件
	 * <ol>
	 * <li>Inner Join 部門提列應付費用申請單</li>
	 * </ol>
	 * </ul>
	 * <li>計算已申請的W件費用申請單申請總額:call
	 * {@link EntryService#entryCaseWExpSum(tw.com.skl.exp.kernel.model6.bo.ExpapplC)}
	 * </li></li>
	 * <ul>
	 * 查詢條件
	 * <ol>
	 * <li>Select sum(分錄.金額) from 分錄</li>
	 * <li>「費用申請單.費用年月」前四碼=傳入參數「費用申請單.費用年月」前4碼(西元年)</li>
	 * <li>「費用申請單.分錄群組.分錄.成本單位代號」=傳入參數第一筆借方的「費用申請單.分錄群組.分錄.成本單位代號」</li>
	 * </ol>
	 * 資料篩選
	 * <ol>
	 * <li>「費用申請單.成本別」=”W”</li>
	 * <li>「費用申請單.分錄群組.分錄.科目借貸別」=借方</li>
	 * <li>「費用申請單.申請單狀態」不等於”刪除”</li>
	 * </ol>
	 * JOIN 條件
	 * <ol>
	 * <li>Inner Join費用申請單</li>
	 * </ol>
	 * </ul>
	 * <li>計算W件超支金額:</li>
	 * <ul>
	 * W件超支金額=已申請的W件費用申請單申請總額-傳入參數中，借方的「費用申請單.分錄群組.分錄.金額」-部門提列的應付費用總額
	 * </ul>
	 * <li>若計算出的W件超支金額>0，表示已超支。附加”W超支金額”(金額為計算出的超支金額)到傳入的「費用申請單.系統摘要」</li>
	 * <li>將傳入的「費用申請單」相關聯的分錄資料中，設定所有的「分錄.成本代號」=”W”</li>
	 * 
	 * </ol>
	 * 
	 * @param expapplC
	 */
	void handleCaseW(ExpapplC expapplC);

	/**
	 * <p>
	 * If 「費用申請單.是否扣繳進項稅」=true且「 費用申請單.憑證類別.格式代號」為「21、23
	 * 、25、26、27」其中任一個值。要檢查統一編號欄位必須有值
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void checkInvoiceNumberIsMustType(ExpapplC expapplC);

	/**
	 * 共用11.1.39 檢核成本單位申請限額
	 * <p>
	 * C 1.5.13
	 * </p>
	 * <p>
	 * 在申請費用時，檢核單位的限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param department
	 *            成本單位
	 */
	void checkDepartmentApplyQuota(ExpapplC expapplC, Department department);

	// RE201300147 modify by michael in 2013/06/25 start

	/**
	 * 共用11.1.39 檢核成本單位年度申請限額
	 * 
	 * 原checkDepartmentApplyYearQuota是錯的，它是檢核月,不是年
	 * 
	 * <p>
	 * C 1.5.13
	 * </p>
	 * <p>
	 * 在申請費用時，檢核單位的年度限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param department
	 *            成本單位
	 */
	void checkDepartmentAppliedQuotaByYear(ExpapplC expapplC, Department department);

	// RE201300147 modify by michael in 2013/06/25 end

	/**
	 * 共用11.1.39 檢核成本單位年度申請限額
	 * <p>
	 * C 1.5.13
	 * </p>
	 * <p>
	 * 在申請費用時，檢核單位的年度限額
	 * </p>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param department
	 *            成本單位
	 */
	void checkDepartmentApplyYearQuota(ExpapplC expapplC, Department department);

	/**
	 * 共用11.1.40 計算指定成本單位在某費用年月已申請的費用項目總額
	 * <p>
	 * 計算指定費用年月已申請的費用項目總額
	 * </p>
	 * 
	 * @param departmentCode
	 *            成本單位代碼(六碼)
	 * @param expItemCode
	 *            費用項目代號(八碼)
	 * @param expYearMonth
	 *            費用年月(YYYYMM)
	 * @return BigDecimal 申請總額
	 * @param expApplNo
	 *            費用申請單號
	 */
	BigDecimal getCostUnitAppliedTotalAmount(String departmentCode, String expItemCode, String expYearMonth, String expApplNo);

	/**
	 * <p>
	 * C 1.5.13 依冊號=冊號1或冊號2，查詢是否有已申請的費用申請單
	 * </p>
	 * 
	 * @param listNo1
	 *            冊號1
	 * @param listNo2
	 *            冊號2
	 * @param applUserDepCode
	 *            申請人所屬單位代號
	 * @return
	 */
	ExpapplC findExpapplCByListNoOnBonusAwardListType(String listNo1, String listNo2, String applUserDepCode);

	/**
	 * 將「費用申請單」的狀態設為”申請中”，並儲存之。 並記錄流程簽核歷程。
	 * <p>
	 * UC 2.1.1 審查調查、業務稽查費用
	 * </p>
	 * 
	 * @param expApplNo
	 *            申請單號
	 * @param functionCode
	 *            功能代碼
	 */
	void doRtnItemReapplied(String expApplNo, FunctionCode functionCode);

	/**
	 * 將「費用申請單」的狀態設為”確認申請”，並儲存之。 並記錄流程簽核歷程。
	 * <p>
	 * UC 1.6.6 調查費、業務稽查費用申請記錄表
	 * </p>
	 * 
	 * @param expApplNoList
	 * @param functionCode
	 */
	void doConfirmApplied(List<String> expApplNoList, FunctionCode functionCode);

	/**
	 * 排序分錄(先借後貸)
	 * 
	 * @param expapplC
	 *            費用申請單
	 */
	void doSortEntry(ExpapplC expapplC);

	/**
	 * 依條件查出申請單號
	 * <p>
	 * C1.6.4 研修差旅費用申請記錄表(含國外)
	 * </p>
	 * <p>
	 * C1.6.10出差報告表列印
	 * </p>
	 * 
	 * @param applStateEnum
	 *            申請單狀態Enum
	 * @param middleTypeCode
	 *            費用中分類代碼
	 * @param applyUserCode
	 *            申請人員工代號
	 * @param createDateStart
	 *            建檔期間 起
	 * @param createDateEnd
	 *            建檔期間 迄
	 * @param isFindDepartmentCode
	 *            是否需查詢單位代碼
	 * @param is1610
	 *            是否為C1.6.10呼叫
	 * @return
	 */
	List<String> findByParams(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610);

	// RE201504572_優化研修差旅 CU3178 2015/12/18 START

	// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
	List<String> findByParams2(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1604);

	// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end

	/**
	 * 依條件查出申請單號
	 * <p>
	 * C1.6.4 研修差旅費用申請記錄表(研修差旅)
	 * </p>
	 * 
	 * @param applStateEnum
	 *            申請單狀態Enum
	 * @param middleTypeCode
	 *            費用中分類代碼
	 * @param applyUserCode
	 *            申請人員工代號
	 * @param createDateStart
	 *            建檔期間 起
	 * @param createDateEnd
	 *            建檔期間 迄
	 * @param isFindDepartmentCode
	 *            是否需查詢單位代碼
	 * @param is1610
	 *            是否為C1.6.10呼叫
	 * @param 文號
	 * @param 班別
	 * @return
	 */
	List<String> findByParamsLrn(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610, String paperNo, String classCode);

	// RE201504572_優化研修差旅 CU3178 2015/12/18 END

	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 START
	/**
	 * 依條件查出申請單號-by1.5.4 (K10)
	 * <p>
	 * C1.6.4 研修差旅費用申請記錄表(研修差旅)
	 * </p>
	 * 
	 * @param applStateEnum
	 *            申請單狀態Enum
	 * @param middleTypeCode
	 *            費用中分類代碼
	 * @param applyUserCode
	 *            申請人員工代號
	 * @param createDateStart
	 *            建檔期間 起
	 * @param createDateEnd
	 *            建檔期間 迄
	 * @param isFindDepartmentCode
	 *            是否需查詢單位代碼
	 * @param is1610
	 *            是否為C1.6.10呼叫
	 * @param 文號
	 * @param 班別
	 * @return
	 */
	List<String> findByParamsHRLrn(ApplStateEnum applStateEnum, MiddleTypeCode middleTypeCode, String applyUserCode, Calendar createDateStart, Calendar createDateEnd, boolean isFindDepartmentCode, boolean is1610, String paperNo, String classCode);

	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 END

	/**
	 * 計算應匯金額 = 費用合計-沖轉金額
	 * 
	 * @param expapplC
	 *            行政費用申請單
	 * @return
	 */
	BigDecimal caculateShouldRemitAmount(ExpapplC expapplC);

	/**
	 * 依申請單號查出費用申請單
	 * 
	 * @param expApplNo
	 *            申請單號
	 * @return
	 */
	ExpapplC findByExpApplNo(String expApplNo);

	/**
	 * 依使用者代碼產生領款單位
	 * <p>
	 * 以「申請人員.匯款單位.單位代號」為預設值。若為空值則帶入「申請人員.所屬單位.單位代號」
	 * 
	 * @param userCode
	 *            使用者代碼
	 * @param expapplC
	 *            費用申請單
	 */
	void generateDrawMoneyUnitByUserCode(String userCode, ExpapplC expapplC);

	/**
	 * <p>
	 * C 1.6.8 廠商退件送件表-付款合計: 顯示應付費用-總務費用科目金額
	 * </p>
	 * 
	 * @param expapplNos
	 *            申請單號資料
	 * @return
	 */
	BigDecimal findPrintRtnItemForVendor(List<String> expapplNos);

	/**
	 * <p>
	 * C 1.6.8 一般退件送件表-行政部室合計: 合計借方科目其”成本單位”的組織型態為0總公司
	 * </p>
	 * 
	 * @param expapplNos
	 *            申請單號資料
	 * @return
	 */
	BigDecimal findPrintRtnItemForGeneralDep(List<String> expapplNos);

	/**
	 * <p>
	 * C 1.6.8 一般退件送件表-業務部室合計: 合計借方科目其”成本單位”的組織型態不為0總公司
	 * </p>
	 * 
	 * @param expapplNos
	 *            申請單號資料
	 * @return
	 */
	BigDecimal findPrintRtnItemForSalDep(List<String> expapplNos);

	/**
	 * <p>
	 * C 1.6.8 退件送件表-付款合計: 顯示應付費用-待匯、待開、待付金額
	 * </p>
	 * 
	 * @param expapplNos
	 *            申請單號資料
	 * @return
	 */
	BigDecimal findPrintRtnItemForTotalPayAmt(List<String> expapplNos);

	/**
	 * 依費用項目代碼設定 費用申請單.費用項目
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param expItemCode
	 *            費用項目代碼
	 */
	void setExpItemByCode(ExpapplC expapplC, ExpItemCode expItemCode);

	/**
	 * 依申請單狀態代碼設定 費用申請單.申請單狀態
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param applStateCode
	 *            申請單狀態代碼
	 */
	void setApplStateByCode(ExpapplC expapplC, ApplStateCode applStateCode);

	/**
	 * 依費用性質代碼設定 費用申請單.費用性質
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param expTypeCode
	 *            費用性質代碼
	 */
	void setExpTypeByCode(ExpapplC expapplC, ExpTypeCode expTypeCode);

	/**
	 * 依付款對象代碼設定 費用申請單.付款對象
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param paymentTargetCode
	 *            付款對象代碼
	 */
	void setPaymentTargetByCode(ExpapplC expapplC, PaymentTargetCode paymentTargetCode);

	/**
	 * 依付款方式代碼設定 費用申請單.付款方式
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param paymentTypeCode
	 *            付款方式代碼
	 */
	void setPaymentTypeByCode(ExpapplC expapplC, PaymentTypeCode paymentTypeCode);

	/**
	 * 依代碼找出付款方式
	 * 
	 * @param paymentTypeCode
	 *            付款方式代碼
	 * @return
	 */
	PaymentType getPaymentTypeByCode(PaymentTypeCode paymentTypeCode);

	/**
	 * 依費用中分類代碼設定 費用申請單.費用中分類
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param middleTypeCode
	 *            費用中分類代碼
	 */
	void setMiddleTypeByCode(ExpapplC expapplC, MiddleTypeCode middleTypeCode);

	/**
	 * 產生過渡付款明細
	 * <p>
	 * C 1.5.6 輸入國外研修差旅費用核銷申請資料
	 * </p>
	 * 
	 * @param exp
	 *            國外研修差旅費用
	 * @param remitEntry
	 *            分錄(應付費用分錄)
	 * @return
	 */
	void generateTransitPaymentDetailByOvsaTrvlLrnExp(OvsaTrvlLrnExp exp, Entry remitEntry);

	/**
	 * <p>
	 * C 4.1.3 行政日結時，查詢費用申請單資料
	 * </p>
	 * 
	 * @param expApplNo
	 *            費用申請單號
	 * @return
	 */
	ExpapplC findByExpApplNoFetchData(String expApplNo);

	/**
	 * 執行預算檢核
	 * 
	 * @author Eustace
	 * @param expapplC
	 *            費用申請單
	 * @return Map<String, Object>: KEY/VALUE如下
	 *         <p>
	 *         “MESSAGE”: 記錄回傳訊息於一個List
	 */
	Map<String, Object> checkBudget(ExpapplC expapplC);

	// RE201403462_預算修改 CU3178 2014/10/24 START
	/**
	 * 執行預算檢核
	 * 
	 * @author Eustace
	 * @param expapplC
	 *            費用申請單
	 * @return Map<String, Object>: KEY/VALUE如下
	 *         <p>
	 *         “MESSAGE”: 記錄回傳訊息於一個List
	 */
	Map<String, Object> checkBudgetNew(ExpapplC expapplC, FunctionCode functionCode);

	// RE201403462_預算修改 CU3178 2014/10/24 END

	/**
	 * 依條件查出退件Dto資料
	 * <p>
	 * C11.7.4退件查詢
	 * </p>
	 * 
	 * @param subpoenaDateStart
	 *            傳票起訖日期Start
	 * @param subpoenaDateEnd
	 *            傳票起訖日期End
	 * @param departmentCode
	 *            單位代號
	 * @return
	 */
	List<ReturnExpapplCDto> findReturnExpapplCDtoByParams(Calendar subpoenaDateStart, Calendar subpoenaDateEnd, String departmentCode);

	/**
	 * D 11.5 廠商費用-資料來源篩選。
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<VendorExpDto> findVendorExpDto(Calendar startDate, Calendar endDate);

	/**
	 * 依條件查出費用申請單狀態Dto
	 * <p>
	 * C11.7.5費用申請單狀態查詢
	 * </p>
	 * <p>
	 * 2010/5/19:RE201001030需求變更,以該送件單位及其轄屬單位做為查詢條件 BY 文珊
	 * </p>
	 * 
	 * @param userCode
	 *            申請員工代號
	 * @param deliverDepList
	 *            送件單位代號 (必要輸入欄位)
	 * @param invoiceAmt
	 *            憑證金額(含)
	 * @param createDateStart
	 *            建檔期間起日
	 * @param createDateEnd
	 *            建檔期間迄日
	 * @return
	 */
	List<ExpapplCDto> findExpapplCDtoByParams(String userCode, List<Department> deliverDepList, BigDecimal invoiceAmt, Calendar createDateStart, Calendar createDateEnd);

	/**
	 * 檢核過渡付款明細金額是否相同於應付費用金額
	 * 
	 * @param expApplNo
	 *            行政費用申請單號
	 */
	void checkTransitPaymentDetailAmountByExpApplNo(String expApplNo);

	/**
	 * 設定費用明細與申請單之間的關聯
	 * 
	 * @param expapplC
	 *            行政費用申請單
	 */
	void updateExpapplCDetail(ExpapplC expapplC);

	/**
	 * 設定費用申請單的狀態
	 * 
	 * @param applNoList
	 *            費用申請單單號List
	 * @param functionCode
	 *            功能代碼
	 * @param applStateCpde
	 *            申請單狀態代碼
	 */
	void updateExpapplCState(List<String> applNoList, FunctionCode functionCode, ApplStateCode applStateCpde);

	/**
	 * 查詢「業務部室實報實支」申請單中,該文號費用狀態不為99刪件的 申請單筆數
	 * <p>
	 * C9.3.1發文獎勵費資料轉入
	 * </p>
	 * <p>
	 * C9.3.2發文獎勵費維護
	 * </p>
	 * 
	 * @param papersNo
	 *            文號
	 * @return
	 */
	long findSalDepOfficeExpCountByPaperNo(String papersNo);

	/**
	 * 查詢「一般費用」申請單中,該文號費用狀態不為99刪件的 申請單筆數
	 * <p>
	 * C9.3.1發文獎勵費資料轉入
	 * </p>
	 * <p>
	 * C9.3.2發文獎勵費維護
	 * </p>
	 * 
	 * @param papersNo
	 *            文號
	 * @return
	 */
	long findGeneralExpCountByPaperNo(String papersNo);

	/**
	 * 查詢「業務部室實報實支」申請單中,以文號、申請人所屬單位代號查找費用狀態不為99刪件的 申請單筆數
	 * <p>
	 * C9.3.2發文獎勵費維護
	 * </p>
	 * 
	 * @param papersNo
	 *            文號
	 * @param depUtilCode
	 *            申請人所屬單位代號
	 * @return
	 */
	long findSalDepOfficeExpCountByPaperNoDepUtilCode(String papersNo, String depUtilCode);

	/**
	 * 查詢「一般費用」申請單中,以文號、申請人所屬單位代號查找費用狀態不為99刪件的 申請單筆數
	 * <p>
	 * C9.3.2發文獎勵費維護
	 * </p>
	 * 
	 * @param papersNo
	 *            文號
	 * @param depUtilCode
	 *            申請人所屬單位代號
	 * @return
	 */
	long findGeneralExpCountByPaperNoDepUtilCode(String papersNo, String depUtilCode);

	/**
	 * 查詢行政費用申請單
	 * <p>
	 * C10.8.6費用申請單維護
	 * </p>
	 * 
	 * @param bigTypeCode
	 * @param middleTypeCode
	 * @param depCode
	 * @param createDateStart
	 * @param createDateEnd
	 * @param expApplNo
	 * @param projectCode
	 * @param userCode
	 * @return
	 */
	List<ExpapplCMaintainDto> findExpapplCMaintainDtoByParams(String bigTypeCode, String middleTypeCode, String depCode, Calendar createDateStart, Calendar createDateEnd, String expApplNo, String projectCode, String userCode);

	/**
	 * 二代健保相關檢查 若憑證金額達"二代健保起扣金額下限"且"免扣取補充保費 為未勾選(需扣)"時，檢核"實際扣繳保費"欄位值不可為0，
	 * 若為0，顯示《該所得已達二代健保補充保費下限，若不需扣取，請勾選"免扣取補充保費"，並選擇"免扣補充保費原因"》。
	 * 
	 * RE201201260_二代健保 cm9539 2012/11/07
	 * 
	 * @param acct
	 * @param expapplc
	 */
	void doHealInsMinAmtCheck(AccTitle acct, ExpapplC expapplc);

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
	boolean doCheckHealInsIncomeForm(AccTitle acct, String inComeIdTypeCode);

	/** RE201201260_二代健保 C7.1.8扣繳健保補充保費 start */
	/**
	 * 查出行政費用已覆核且非租賃費用的二代健保類申請單 for C7.1.8扣繳健保補充保費修改
	 * 
	 * RE201201260_二代健保 cm9539 2012/12/03
	 * 
	 * @param expappl
	 * @return
	 */
	public ExpapplC findHealthPremiumAppl(String expNo);

	/**
	 * 依據頁面輸入的分錄, 插入原分錄 RE201201260_二代健保 cm9539 2012/12/06
	 * 
	 * @param expC
	 * @param healEntry
	 * @return
	 */
	public List<Entry> calculateTempHealthPremiumEntry(ExpapplC expC, Entry healEntry);

	/** RE201201260_二代健保 C7.1.8扣繳健保補充保費 end */

	/**
	 * 依據頁面輸入的分錄調整原分錄 RE201201260_二代健保 cm9539 2012/12/06
	 * 
	 * @param expC
	 * @param healEntry
	 * @return
	 */
	public List<Entry> calculateHealthPremiumEntry(ExpapplC expC);

	/**
	 * 依據單號查詢行政費用申請單所得人 for C7.1.8扣繳健保補充保費 RE201201260_二代健保 cm9539 2012/12/13
	 * 
	 * @param expNo
	 * @return
	 */
	public List<String> findHealthPremiumApplIncomeId(String expNo);

	/** RE201201260_二代健保 C7.1.8扣繳健保補充保費 end */
	/** RE201201260_二代健保 2013/07/01 start */
	/** RE201201260_二代健保 cm9539 2013/06/18 start */
	/** RE201201260_二代健保 cm9539 2013/05/27 start */
	/**
	 * Add the function of generating batchNo.
	 * 
	 * @return
	 */
	public String genBatchNoB(Calendar expectRemitDate);

	public String genBatchNoC(Calendar expectRemitDate);

	/** RE201201260_二代健保 cm9539 2013/05/27 end */
	/** RE201201260_二代健保 cm9539 2013/06/18 end */
	/** RE201201260_二代健保 2013/07/01 end */

	// RE201401980_新增檢核借貸方是否平衡 CU3178 2014/9/30 start
	/**
	 * 判斷借貸方是否平衡
	 */
	public void checkcalcBalance(String expApplNo, String functionCode);

	// RE201401980_新增檢核借貸方是否平衡 CU3178 2014/9/30 end

	// RE201500189_國內出差申請作業流程簡化 EC0416 2015/04/10 start
	List<BudgetIn> findprojectcode(String projectCode, String year);

	// RE201500189_國內出差申請作業流程簡化 EC0416 2015/04/10 end

	// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 START
	/**
	 * 以名冊冊號，產生申請獎金品時的分錄(不包含應付費用科目)
	 * <p>
	 * UC 1.5.13 業務核算費用申請
	 * </p>
	 * 
	 * <p>
	 * 說明
	 * </p>
	 * <ul>
	 * <li>是否產生應付代扣科目欄位
	 * <ul>
	 * <li>UC1.5.1、1.5.13: 當付款方式為沖轉暫付時，為稅金匯回，不產生應付代扣科目</li>
	 * <li>UC1.5.9: 一定是稅金回，不產生應付代扣科目</li>
	 * <li>UC1.5.7: 要產生應付代扣科目</li>
	 * </ul>
	 * </li>
	 * <li>兩個冊號所對應到的借方科目一樣時，產生的借方分錄合併為同一筆，否則借方拆為兩筆。(例如:一本國、一外國)</li>
	 * <li>分錄產生後，設定名冊資料的值:
	 * <ul>
	 * <li>「名冊.名冊狀態」=請領完畢</li>
	 * <li>「名冊.已使用金額」=已產生分錄的金額</li>
	 * <li>「名冊.費用申請單號」=傳入的”費用申請單號”</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param expapplC
	 *            費用申請單
	 * @param departmentCode
	 *            成本單位代號
	 * @param boolean1
	 *            是否代扣所得稅(產生應付代扣貸方科目)
	 * @param summary
	 *            摘要
	 * @param expapplCDetail
	 *            費用明細 IISI-20100805 : 修正費用明細沒有存入DB問題
	 * @return
	 */
	List<Entry> doSalGenerateApplyRosterEntries(ExpapplC expapplC, String departmentCode, Boolean boolean1, String summary, ExpapplCDetail expapplCDetail);

	/**
	 * 依費用申請單號，變更名冊狀態
	 * <p>
	 * UC 1.5.13 業務部室實報實支辦公費
	 * </p>
	 * <p>
	 * 功能說明
	 * </p>
	 * <ul>
	 * 使用名冊的UC有: UC1.5.1、UC1.5.7、UC1.5.9(不產生應付代扣科目)、UC1.5.13 及其對應的修改資料UC
	 * <li>新增/修改申請單
	 * <p>
	 * 若該申請單在新增/修改後，為名冊的申請單，儲存後要變更該名冊的欄位:
	 * <ol>
	 * <li>「名冊.名冊狀態」=請領完畢</li>
	 * <li>「名冊.已使用金額」=申請總額</li>
	 * <li>「名冊.費用申請單號」=傳入的”費用申請單.申請單號”</li>
	 * </ol>
	 * </li>
	 * <li>修改申請單
	 * <p>
	 * 若該申請單在修改前，為名冊的申請單，儲存後要復原該名冊的欄位:
	 * <ol>
	 * <li>「名冊.名冊狀態」設為尚未請領。</li>
	 * <li>「名冊.已使用金額」設為0</li>
	 * <li>「名冊.費用申請單號」設為null</li>
	 * </ol>
	 * </li>
	 * </ul>
	 * 
	 * @param state
	 *            名冊狀態代碼(0: 請領名冊 1:復原名冊狀態)
	 * @param expApplNo
	 *            費用申請單號
	 * @param listNos
	 *            最多包含兩個字串: 冊號1、冊號2(Option)
	 */
	void updateRosterStateList(Integer state, ExpapplC expapplc);

	void deleteRoster(List<RosterDetail> rosterdetailList);

	// RE201500829_發文獎勵費用申請流程優化 CU3178 2015/5/20 END

	// RE201501248_檢核專案代號與成本單位 EC 2015/6/29 start
	public void checkProjectCode(String projectCode, String costCode);

	// RE201501248_檢核專案代號與成本單位 EC0416 2015/6/29 end

	// RE201504024_C10.8.6申請單維護、C1.5.3公務車 CU3178 2015/10/26 START
	int pubcaraffyearqouta(ExpapplC eac, AccTitle accTitle, String carLoanCode);

	// RE201504024_C10.8.6申請單維護、C1.5.3公務車 CU3178 2015/10/26 END

	// RE201502395_調整B2_5效能_V2 2015/11/02 START
	/**
	 * B 2.5 找出指定經辦所屬，可供"產生日計表"所使用的費用申請單,已送結。
	 * 
	 * <p>
	 * CRIT
	 * </p>
	 * <ol>
	 * <li>費用申請單狀態為"已初審"。</li>
	 * <li>付款方式為"匯款"，付款對象為"單位"，費用中分類為 N10, M10, M20, L10, L20, R20, H20, J40,
	 * K10, K20。</li>
	 * <li>或付款方式為"開票"，費用中分類為 N10。</li>
	 * <li>付款方式為"借支沖轉"，付款對象為單位，費用中分類為N10辦公費實報實支費用和E10一般費用。</li>
	 * </ol>
	 * 
	 * @param user
	 * @return
	 */
	List<ExpapplC> findApplyForDailyStmtClosed(User user);

	// RE201502395_調整B2_5效能_V2 2015/11/02 END

	// RE201601158_優化簽收核銷日計表 EC0416 2016/5/9 START
	public void doReturn(String deliverDayListNo, FunctionCode functionCode) throws ExpException;

	// RE201601158_優化簽收核銷日計表 EC0416 2016/5/9 END

	// defect#3361 EC0416 2016/6/23 START
	List<ExpapplC> findByDeliverDayListNoByW(String deliverNo, String entryCostCode);
	// defect#3361 EC0416 2016/6/23 END
}