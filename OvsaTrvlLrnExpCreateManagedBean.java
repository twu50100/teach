package tw.com.skl.exp.web.jsf.managed.gae.expapply.inputapplyform;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.SortableModel;
import org.springframework.beans.BeanUtils;

import tw.com.skl.common.model6.web.jsf.managedbean.impl.TemplateDataTableManagedBean;
import tw.com.skl.common.model6.web.jsf.utils.FacesUtils;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.ApplInfo;
import tw.com.skl.exp.kernel.model6.bo.BigType;
import tw.com.skl.exp.kernel.model6.bo.BizMatter;
import tw.com.skl.exp.kernel.model6.bo.Country;
import tw.com.skl.exp.kernel.model6.bo.Currency;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.DepartmentMail;
import tw.com.skl.exp.kernel.model6.bo.Entry;
import tw.com.skl.exp.kernel.model6.bo.EntryGroup;
import tw.com.skl.exp.kernel.model6.bo.EntryType;
import tw.com.skl.exp.kernel.model6.bo.ExchangeRate;
import tw.com.skl.exp.kernel.model6.bo.ExpYears;
import tw.com.skl.exp.kernel.model6.bo.ExpapplC;
import tw.com.skl.exp.kernel.model6.bo.IncomeIdType;
import tw.com.skl.exp.kernel.model6.bo.OvsaCity;
import tw.com.skl.exp.kernel.model6.bo.OvsaDailyWork;
import tw.com.skl.exp.kernel.model6.bo.OvsaExpDrawInfo;
import tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp;
import tw.com.skl.exp.kernel.model6.bo.Station;
import tw.com.skl.exp.kernel.model6.bo.StationExpDetail;
import tw.com.skl.exp.kernel.model6.bo.TransitPaymentDetail;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.bo.ZoneType;
import tw.com.skl.exp.kernel.model6.bo.AccClassType.AccClassTypeCode;
import tw.com.skl.exp.kernel.model6.bo.AccTitle.AccTitleCode;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.BigType.BigTypeCode;
import tw.com.skl.exp.kernel.model6.bo.BizMatter.BizMatterCode;
import tw.com.skl.exp.kernel.model6.bo.Currency.CurrencyCode;
import tw.com.skl.exp.kernel.model6.bo.DrawAccountType.DrawAccountTypeCode;
import tw.com.skl.exp.kernel.model6.bo.EntryType.EntryTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ExpItem.ExpItemCode;
import tw.com.skl.exp.kernel.model6.bo.ExpType.ExpTypeCode;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExpItem.OvsaTrvlLrnExpItemCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentTarget.PaymentTargetCode;
import tw.com.skl.exp.kernel.model6.bo.PaymentType.PaymentTypeCode;
import tw.com.skl.exp.kernel.model6.bo.SysType.SysTypeCode;
import tw.com.skl.exp.kernel.model6.bo.ZoneType.ZoneTypeCode;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.facade.OvsaTrvlLrnExpFacade;
import tw.com.skl.exp.kernel.model6.logic.OvsaTrvlLrnExpService;
import tw.com.skl.exp.web.config.CrystalReportConfigManagedBean;
import tw.com.skl.exp.web.jsf.displaycontrol.DisplayControlAware;
import tw.com.skl.exp.web.jsf.displaycontrol.DisplayControlBean;
import tw.com.skl.exp.web.jsf.managed.FunctionCodeAware;
import tw.com.skl.exp.web.jsf.managed.gae.expapply.expsendverify.DeliverDaylistGenManagedBean;
import tw.com.skl.exp.web.jsf.managed.gae.expapply.expsendverify.TrvlAndOvsaExpApplManagedBean;
import tw.com.skl.exp.web.jsf.managed.gae.expapply.expsendverify.TrvlExpApplSearchManagedBean;
import tw.com.skl.exp.web.jsf.managed.gae.expverification.expverification.GovExpApproveManagedBean;
import tw.com.skl.exp.web.util.FileExporter;
import tw.com.skl.exp.web.util.SelectFactory;
import tw.com.skl.exp.web.util.SelectHelper;
import tw.com.skl.exp.web.vo.BizTripVo;
import tw.com.skl.exp.web.vo.DeliverDaylistVo;
import tw.com.skl.exp.web.vo.ExpapplCVo;
import tw.com.skl.exp.web.vo.GeneralExpVo;
import tw.com.skl.exp.web.vo.LbrHlthInsurExpVo;
import tw.com.skl.exp.web.vo.OvsaTrvlLrnExpVo;
import tw.com.skl.exp.web.vowrapper.OvsaTrvlLrnExpWrapper;
import tw.com.skl.common.model6.web.jsf.utils.Messages;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;

/**
 * C1.5.6輸入國外研修差旅費用核銷申請資料 ManagedBean 20100914 修改為輸入國外出差(研修)旅費核銷申請資料 ManagedBean
 * RE201601162_國外出差旅費 EC0416 修改
 */
public class OvsaTrvlLrnExpCreateManagedBean extends TemplateDataTableManagedBean<OvsaTrvlLrnExp, OvsaTrvlLrnExpService> implements FunctionCodeAware, DisplayControlAware {

	protected Log logger = LogFactory.getLog(this.getClass());

	private OvsaTrvlLrnExpFacade facade;
	/**
	 * 費用分錄
	 */
	private Entry expEntry;

	/**
	 * 用於頁面上的費用明細 詳細資料
	 */
	private CollectionModel entryDataModel;

	/**
	 * 用於頁面上的費用明細 詳細資料
	 */
	private CoreTable entryDataTable;

	/**
	 * 領款資料DataModel
	 */
	private CollectionModel ovsaExpDrawInfoDataModel;

	/**
	 * 領款資料DataTable
	 */
	private CoreTable ovsaExpDrawInfoDataTable;

	/**
	 * 駐在地點DataModel
	 */
	private CollectionModel stationDataModel;

	/**
	 * 駐在地點DataTable
	 */
	private CoreTable stationDataTable;

	/**
	 * 日支費DataModel
	 */
	private CollectionModel stationExpDetailDataModel;

	/**
	 * 日支費DataTable
	 */
	private CoreTable stationExpDetailDataTable;

	/**
	 * 日用費計算說明
	 */
	private String dailyExpNotes;

	/**
	 * 控制日支費是否為Disabled
	 */
	private boolean selectBizMatterItemDisabled = false;

	/**
	 * 出差事由下拉選單代碼s
	 */
	public static final BizMatterCode[] BIZ_MATTER_CODES = { BizMatterCode.OVSA_TRVL_EXP,
			// RE201601162_國外出差旅費 EC0416 20160406 start
			BizMatterCode.OVSA_TRVL_LRN_EXP, BizMatterCode.OVSA_SUMMIT_CONFERENCE
	// RE201601162_國外出差旅費 EC0416 20160406 end
	};

	/**
	 * 要刪除的分錄
	 */
	private List<Entry> deleteEntryList;

	/**
	 * 資料來自於哪個UC
	 */
	private FunctionCode fromFunctionCode;

	private boolean initUpdateDate = true;

	/**
	 * 用於頁面判斷是否有按下產生分錄
	 */
	private boolean isClickGenEntry = true;

	/**
	 * 是否顯示預算超支檢核結果按鈕
	 */
	private boolean showConfirmButtonBar = false;

	/**
	 * 判斷該資料是否為更新資料
	 */
	private boolean isUpdate = false;

	/**
	 * 選擇的所得人證號類別
	 */
	private IncomeIdType selectIncomeIdType;

	/**
	 * RE201101034核銷鎖住欄位: 由核銷頁面引導至此時鎖住某些欄位 2012/04/13 cm9539
	 * */
	private boolean readOnlyWhenPay;

	// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
	/**
	 * 修改日支費頁面需按下[計算]才可儲存
	 */
	private boolean saveCount = true;

	// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end

	// RE201601162_國外出差旅費 EC0416 20160428 start
	/**
	 * 用於駐在地點修改按鈕變成更新按鈕
	 */
	private boolean updateStationRendered = false;

	/**
	 * 用於修改行程取得行程索引
	 */
	private int updateStationIndex;

	/**
	 * 用來判斷是否為建立新資料或者更新資料
	 * <p>
	 * true:新建資料 false:更新資料
	 */
	private boolean isCreate = true;

	private boolean isUpdatePage = true;

	/**
	 * 來自的頁面
	 */
	private String outcomepage;

	private List<SelectItem> entryTypeList;

	// RE201601162_國外出差旅費 EC0416 20160428 END

	// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 start
	private Calendar ovsaStartDate;
	private Calendar ovsaEndDate;
	private String ovsaDaiylExpapplCID;
	private boolean isDailyWorkPage = true;

	// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 end
	public OvsaTrvlLrnExpCreateManagedBean() {

		super.setInitShowListData(true);
		super.setStatusSearched(true);
		initUpdateDate = true;

		// 設定VoWrapper
		this.setVoWrapper(new OvsaTrvlLrnExpWrapper());

		this.setUpdatingData(null);
		initFindCriteriaMap();

		fromFunctionCode = null;
		isClickGenEntry = true;
	}

	@Override
	protected void initFindCriteriaMap() {
		Map<String, Object> findCriteriaMap = new HashMap<String, Object>();
		findCriteriaMap.put("zoneType", null);// 地域別
		findCriteriaMap.put("country", null);// 國家
		findCriteriaMap.put("expApplNo", null);// 申請單號
		findCriteriaMap.put("accTitleCode", null);// 科目代號
		findCriteriaMap.put("accTitleName", null);// 科目中文
		findCriteriaMap.put("entryType", null);// 借貸別
		this.setFindCriteriaMap(findCriteriaMap);
	}

	@Override
	protected void initCreatingData() {
		OvsaTrvlLrnExpVo vo = new OvsaTrvlLrnExpVo();
		OvsaTrvlLrnExp exp = new OvsaTrvlLrnExp();
		exp.setExpapplC(this.initExpapplCBo());
		vo.setBo(exp);
		this.setUpdatingData(vo);
		this.setDataModel(null);
		initExpEntry();
		// RE201601162_國外出差旅費 EC0416 20160428 start
		// 初始化駐在地點
		vo.setStation(new Station());
		// 初始化領款人資料
		vo.setOvsaExpDrawInfo(initOvsaExpDrawInfo());
		// 初始化頁面分錄

		vo.setEntry(initEntry());
		// 初始化駐在地點名細LIST
		vo.setStationExpDetailList(intitStatinoDetailList());
		setStationDataModel(null);
		setOvsaExpDrawInfoDataModel(null);
		setEntryDataModel(null);

		// RE201601162_國外出差旅費 EC0416 20160428 end

	}

	private void initExpEntry() {
		expEntry = new Entry();
		expEntry.setAccTitle(facade.getAccTitleService().findByCode(AccTitleCode.CODE_62040523.getCode()));
		expEntry.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_3));
	}

	/**
	 * 取得初始化行政費用申請單BO
	 * 
	 * @return ExpapplC 行政費用申請單
	 */
	private ExpapplC initExpapplCBo() {
		// 初始化BO相關物件
		ExpapplC expapplC = new ExpapplC();
		facade.getExpapplCService().setApplStateByCode(expapplC, ApplStateCode.TEMP);
		expapplC.setApplyUserInfo(new ApplInfo());

		// 費用性質= 一般
		facade.getExpapplCService().setExpTypeByCode(expapplC, ExpTypeCode.GENERAL);
		// 付款對象改為"其它"，Defect:1792
		facade.getExpapplCService().setPaymentTargetByCode(expapplC, PaymentTargetCode.OTHERS);
		// 付款方式改為"調整"，Defect:1792
		facade.getExpapplCService().setPaymentTypeByCode(expapplC, PaymentTypeCode.C_ADJECT);
		expapplC.setWorkDate(Calendar.getInstance());
		expapplC.setEntryGroup(new EntryGroup());

		// PeterYu 20120801 RE201200382 Update start
		// “費用年月”欄位預設值，
		// ※ 若於『B7.17設定費用年月』功能設定之欄位值為空白時，則C1.5.1~C1.5.13之費用年月欄位值維持以現行以系統年月做為預設值。
		// ※
		// 若於『B7.17設定費用年月』功能設定之欄位值不為空白時，則C1.5.1~C1.5.13之費用年月欄位值以B7.17所設定之值，做為預設值。

		// expapplC.setExpYears(DateUtils.getYeayMonth(Calendar.getInstance()));

		List<ExpYears> results = facade.getExpYearsService().findExpYears();
		String ExpYM = null;
		if (results.size() >= 1) {
			ExpYears expYears = new ExpYears();
			expYears = results.get(0);
			ExpYM = expYears.getExpYM();

			if (ExpYM == null) {
				expapplC.setExpYears(DateUtils.getYeayMonth(Calendar.getInstance()));
			} else if (ExpYM.trim().length() > 0) {
				String YM = String.valueOf(Integer.parseInt(ExpYM.substring(0, 3)) + 1911) + ExpYM.substring(3);
				expapplC.setExpYears(YM);
			} else {
				expapplC.setExpYears(DateUtils.getYeayMonth(Calendar.getInstance()));
			}
		} else {

			expapplC.setExpYears(DateUtils.getYeayMonth(Calendar.getInstance()));

		}
		// PeterYu 20120801 RE201200382 Update end

		expapplC.setCreateUser(getLoginUser());
		return expapplC;
	}

	private StationExpDetail initStationExpDetailBo(StationExpDetail stationExpDetail) {
		StationExpDetail detail = new StationExpDetail();
		if (null == stationExpDetail) {
			detail.setOvsaTrvlLrnExpItem(facade.getOvsaTrvlLrnExpItemService().findByCode(OvsaTrvlLrnExpItemCode.TRAFFIC_EXP));
			detail.setPaymentType(facade.getExpapplCService().getPaymentTypeByCode(PaymentTypeCode.C_SALF_PAY));
		} else {
			detail.setOvsaTrvlLrnExpItem(stationExpDetail.getOvsaTrvlLrnExpItem());
			detail.setPaymentType(stationExpDetail.getPaymentType());
			detail.setCurrency(stationExpDetail.getCurrency());
			detail.setExchangeRate(stationExpDetail.getExchangeRate());
		}

		return detail;
	}

	@Override
	protected void initUpdatingData(ValueObject<OvsaTrvlLrnExp> updatingData) {
		isClickGenEntry = true;
		this.setDataModel(null);
		setEntryDataModel(null);
		this.expEntry = new Entry();
		// 找出國外研修旅費科目
		if (null == updatingData || null == updatingData.getBo() || null == updatingData.getBo().getExpapplC() || null == updatingData.getBo().getExpapplC().getEntryGroup() || CollectionUtils.isEmpty(updatingData.getBo().getExpapplC().getEntryGroup().getEntries())) {
			this.expEntry = new Entry();
		} else {
			AccTitleCode accTitleCode = getAccTitleCodeByBizMatter(updatingData.getBo().getBizMatter());
			for (Entry entry : updatingData.getBo().getExpapplC().getEntryGroup().getEntries()) {
				// 找出國外研修旅費科目
				if (accTitleCode.equals(AccTitleCode.getByValue(entry.getAccTitle()))) {
					expEntry = entry;
					break;
				}
			}

			deleteEntryList = new ArrayList<Entry>();

			for (Entry entry : updatingData.getBo().getExpapplC().getEntryGroup().getEntries()) {
				if (StringUtils.isNotBlank(entry.getId())) {
					Entry tempEntry = new Entry();
					BeanUtils.copyProperties(entry, tempEntry);
					deleteEntryList.add(tempEntry);
				}
			}
		}

	}

	@Override
	protected void setupUpdatingData() {
	}

	/**
	 * 在主頁面，按[儲存]按鈕。
	 * 
	 * <ol>
	 * <li>在資料庫中，新增一筆狀態為"暫存中"的國外研修差旅費用核銷申請資料。 call
	 * {@link tw.com.skl.exp.kernel.model6.logic.OvsaTrvlLrnExpService#doCreateTempOvsaTrvlLrnExp(tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp, Entry, FunctionCode)}
	 * </li>
	 * </ol>
	 */
	@Override
	public String doSaveCreateAction() {
		setShowConfirmButtonBar(false);

		getService().doCreateTempOvsaTrvlLrnExp(getUpdatingDataValue(), expEntry, this.getFunctionCode());

		isCreate = true;
		// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 start
		ovsaStartDate = updatingData.getBo().getAbroadStartDate();
		ovsaEndDate = updatingData.getBo().getAbroadEndDate();
		ovsaDaiylExpapplCID = getUpdatingDataValue().getExpapplC().getId();
		initOvsaDailyWork(null);
		// 清除所有資料
		setUpdatingData(null);
		doRefreshData();

		setDataModel(null);
		entryDataModel = null;
		// 設定從C1.5.6進去的國外日報表[返回]跳頁回C1.5.6頁面
		this.isDailyWorkPage = true;
		// return "read";
		return "createOvsaDailyWork";
		// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 end
	}

	/**
	 * 在進入的頁面，按[新增駐在地點]按鈕。
	 * 
	 * <ol>
	 * <li>導頁至輸入國外研修差旅費用核銷申請資料-駐在地點頁面。</li>
	 * <li>顯示已儲存的駐在地點資料。</li>
	 * </ol>
	 * 
	 */
	public String doMaintainStationAction() {
		setDataModel(null);
		OvsaTrvlLrnExp exp = getService().findByExpApplNo(((OvsaTrvlLrnExp) getDataTable().getRowData()).getExpapplC().getExpApplNo());
		OvsaTrvlLrnExpVo vo = new OvsaTrvlLrnExpVo(exp);

		vo.setStation(new Station());
		setStationDataModel(null);
		this.setUpdatingData(vo);
		return "createStation";
	}

	/**
	 * 在駐在地點頁面，修改駐在起訖期間任一欄位後，系統由「出國起迄日期」自動計算駐在日數。
	 * 
	 * @return
	 */
	public Integer getCalculateDaytime() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStation()) {
			return 0;
		}
		Calendar startDate = vo.getStation().getStationStartDate();
		Calendar endDate = vo.getStation().getStationEndDate();
		startDate = DateUtils.getFirstMinuteOfDay(startDate);
		endDate = DateUtils.getFirstMinuteOfDay(endDate);
		if (null == startDate || null == endDate || endDate.compareTo(startDate) < 0) {
			vo.getStation().setStayDays(0);
		} else {
			vo.getStation().setStayDays(DateUtils.getDays(startDate.getTime(), endDate.getTime()) + 1);
		}

		return vo.getStation().getStayDays();
	}

	// RE201601162_國外出差旅費 EC0416 20160428 start
	/**
	 * 在駐在地點頁面，按[新增]按鈕
	 * 
	 * <ol>
	 * <li>更新Session中，國外研修差旅費用核銷申請資料的駐在地點資料</li>
	 * <li>重新顯示駐在地點資料於畫面上</li>
	 * </ol>
	 * 
	 * @return
	 * 
	 */
	public String doCreateStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();

		// RE201601162_優化國外差旅 需求變更 2016/07/07 start
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		// RE201601162_國外出差旅費 EC0416 20160406 START
		// 主頁面檢核
		getService().doValidateOvsaExp(exp, expEntry);
		// 駐在地點頁面檢核
		facade.getStationService().doValidateStation(vo.getStation());
		// 日期起訖日檢核
		getService().checkDate(vo.getBo(), vo.getStation());
		// RE201601162_國外出差旅費 EC0416 20160406 end

		vo.setBo(getService().doSaveStation(vo.getBo(), vo.getStation()));
		vo.setStation(new Station());

		setStationDataModel(null);
		return null;
	}

	/**
	 * 在駐在地點頁面，按[刪除]按鈕
	 */
	public String doDeleteStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		// RE201601162_國外出差旅費 EC0416 20160406 start
		int index = getStationDataTable().getRowIndex();
		vo.setBo(getService().doDeleteStation(vo.getBo(), index));
		// RE201601162_國外出差旅費 EC0416 20160406 end
		setStationDataModel(null);

		return null;
	}

	/**
	 * 在駐在地點頁面，點選[駐在地點]Link欄位的值。
	 * 
	 * <ol>
	 * <li>導頁至日支費頁面</fdoDeleteDailySpendActionli>
	 * </ol>
	 * 
	 * @return
	 * 
	 */
	// RE201601162_國外出差旅費 EC0416 20160708 start
	public String doUpdateDailySpendAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setStationExpDetail(initStationExpDetailBo(null));
		vo.setStation((Station) getStationDataTable().getRowData());
		this.setStationExpDetailDataModel(null);
		return "createStationExpDetail";
	}

	// RE201601162_國外出差旅費 EC0416 20160708 end

	/**
	 * 在駐在地點頁面，按[更新]按鈕。
	 * 
	 * @return
	 */
	public String doUpdateNewStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		// 出差事由不可為空
		if (vo.getBo().getBizMatter() == null) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_OvsaTrvlLrnExp_bizMatter") });
		}

		// 出國事由(主辦單位 課程或會議主題)不可為空
		if (vo.getBo().getAbroadReason() == null) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_bo_OvsaTrvlLrnExp_Reason") });
		}
		// RE201601162_國外出差旅費 EC0416 20160406 START
		getService().checkDate(vo.getBo(), vo.getStation());
		// RE201601162_國外出差旅費 EC0416 20160406 end
		// 城市欄位不可為空白
		if (vo.getStation().getOvsaCity() == null) {
			throw new ExpRuntimeException(ErrorCode.C10638);
		}
		Station updateStaiton = vo.getStation();
		BeanUtils.copyProperties(updateStaiton, vo.getBo().getStations().get(getUpdateStationIndex()));
		vo.setBo(getService().doUpdateStation(getUpdatingDataValue(), vo.getStation()));
		vo.setStation(new Station());
		setStationDataModel(null);
		setUpdateStationRendered(false);
		return null;
	}

	/**
	 * 在駐在地點頁面，按[重設]按鈕。
	 * 
	 * @return
	 */
	public String doResetStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setStation(new Station());
		setStationDataModel(null);
		setUpdateStationRendered(false);
		return null;
	}

	/**
	 * 在駐在地點明細頁面，點選[修改]將資料顯示於駐在地點上"。
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String doUpdateStationAction() {
		Station updateStation = new Station();
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		BeanUtils.copyProperties((Station) getStationDataTable().getRowData(), updateStation);
		vo.setStation(updateStation);
		Map map = getFindCriteriaMap();
		map.put("country", updateStation.getOvsaCity().getCountry());
		map.put("zoneType", updateStation.getOvsaCity().getCountry().getZoneType());

		// 設為true則顯示更新按鈕
		// setUpdateStationRendered(true);
		setUpdateStationIndex(getStationDataTable().getRowIndex());

		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
		// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
		setSaveCount(true);
		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end

		return "updateStation";

	}

	/**
	 * 在"修改駐在地點"頁面，點選[返回]按鈕，導頁至"駐在地點"。
	 * 
	 * @return
	 */
	public String doCancelUpdateStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setBo(getService().doUpdateStation(getUpdatingDataValue(), vo.getStation()));
		vo.setStation(new Station());
		setStationDataModel(null);
		if (!isCreate) {
			return "saveStation";
		} else {
			return "updateStation";
		}
	}

	/**
	 * 在"修改駐在地點"頁面，點選[儲存]按鈕，更新該筆資料並導頁至"駐在地點"。
	 * 
	 * @return
	 */
	public String doSaveUpdateStationAction() {

		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();

		OvsaTrvlLrnExp exp = getUpdatingDataValue();

		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
		// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
		if (!saveCount) {
			throw new ExpRuntimeException(ErrorCode.C10659, new String[] {});
		}

		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end

		// boolean Breakfast=vo.getStation().isPayBreakfast();
		// boolean Lunch=vo.getStation().isPayLunch();
		// boolean Dinner=vo.getStation().isPayDinner();
		// BigDecimal DailyExp=vo.getStation().getDailyExp();
		// 駐在地點頁面檢核
		facade.getStationService().doValidateStation(vo.getStation());
		// 日期起訖日檢核
		getService().checkDate(vo.getBo(), vo.getStation());
		// RE201601162_國外出差旅費 EC0416 20160406 end
		// //城市欄位不可為空白
		// if(vo.getStation().getOvsaCity()==null){
		// throw new ExpRuntimeException(ErrorCode.C10638);
		// }
		// //膳食自付不可為空
		// if(!(Breakfast||Lunch||Dinner)){
		// throw new ExpRuntimeException(ErrorCode.A10001, new
		// String[]{MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Station_payMeal")});
		// }
		// //駐在期間日支費不可為空或負值
		// if(DailyExp.compareTo(BigDecimal.ZERO)<=0){
		// throw new ExpRuntimeException(ErrorCode.C10224, new
		// String[]{MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Station_dailyExp1")});
		// }

		// RE201601162_國外出差旅費 EC0416 20160406 START

		Station updateStaiton = vo.getStation();
		BeanUtils.copyProperties(updateStaiton, vo.getBo().getStations().get(getUpdateStationIndex()));
		vo.setBo(getService().doUpdateStation(getUpdatingDataValue(), vo.getStation()));
		vo.setStation(new Station());
		setStationDataModel(null);
		setUpdateStationRendered(false);
		if (!isCreate) {
			return "saveStation";
		} else {
			return "updateStation";
		}
	}

	/**
	 * 在日支費頁面，按[新增]按鈕。
	 * 
	 * <ol>
	 * <li>於Session中，新增日支費資料</li>
	 * <li>重新顯示日支費資料於畫面上</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doCreateDailySpendAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		// 匯率不可為零
		if (vo.getStationExpDetail().getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ExpRuntimeException(ErrorCode.C10224, new String[] { MessageUtils.getAccessor().getMessage("C10_8_13_exchange_rate") });
		}
		vo.setBo(getService().doSaveStationExpDetail(vo.getBo(), vo.getStation(), vo.getStationExpDetail()));
		vo.setStationExpDetail(initStationExpDetailBo(vo.getStationExpDetail()));
		// vo.getStationExpDetailList().set(index, initStationExpDetail());
		this.setStationExpDetailDataModel(null);
		return null;
	}

	// RE201601162_國外出差旅費 EC0416 start
	/**
	 * 在日支費頁面，按[刪除]按鈕。
	 * 
	 * <ol>
	 * <li>於更新Session中，刪除該筆日支費資料</li>
	 * <li>重新顯示日支費資料於畫面上</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doDeleteDailySpendAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		int index = getStationExpDetailDataTable().getRowIndex();
		vo.setBo(getService().doDeleteStationExpDetail(vo.getBo(), vo.getStation(), index));
		this.setStationExpDetailDataModel(null);
		return null;
	}

	// RE201601162_國外出差旅費 EC0416 20160428 end

	/**
	 * 在日支費頁面，按[返回]按鈕。
	 * 
	 * <ol>
	 * <li>導頁至駐在地點畫面</li>
	 * <li>重新顯示駐在地點資料於畫面上</li>
	 * </ol>
	 * 
	 */
	public String doReturnStationAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setStation(new Station());
		setStationDataModel(null);
		if (!isCreate) {
			return "createStation";
		} else {
			return "updateStation";
		}
	}

	/**
	 * 在進入的頁面，按[付款對象]按鈕。
	 * 
	 * <ol>
	 * <li>導頁至付款對象頁面</li>
	 * <li>顯示已儲存的付款對象資料。</li>
	 * </ol>
	 * 
	 */
	public String doMaintainPaymentTargetAction() {
		setDataModel(null);
		OvsaTrvlLrnExp exp = getService().findByExpApplNo(((OvsaTrvlLrnExp) getDataTable().getRowData()).getExpapplC().getExpApplNo());
		OvsaTrvlLrnExpVo vo = new OvsaTrvlLrnExpVo(exp);

		vo.setOvsaExpDrawInfo(initOvsaExpDrawInfo());
		setOvsaExpDrawInfoDataModel(null);
		this.setUpdatingData(vo);
		return "ovsaExpDrawInfo";
	}

	/**
	 * 在付款對象頁面，按[新增]按鈕。
	 * 
	 * <ol>
	 * <li>於Session中，新增付款對象資料</li>
	 * <li>重新顯示付款對象資料於畫面上</li>
	 * </ol>
	 * 
	 */
	public String doCreatePaymentTargetAction() {
		generateOvsaExpDrawInfo();
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		OvsaTrvlLrnExp exp = getService().doSaveOvsaExpDrawInfo(vo.getBo(), vo.getOvsaExpDrawInfo());
		vo.setBo(exp);
		setUpdatingData(vo);
		setOvsaExpDrawInfoDataModel(null);
		vo.setOvsaExpDrawInfo(initOvsaExpDrawInfo());
		return null;
	}

	private void generateOvsaExpDrawInfo() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo.getOvsaExpDrawInfo()) {
			return;
		}
		// 金額不可為零
		if (vo.getOvsaExpDrawInfo().getAmt() == null || vo.getOvsaExpDrawInfo().getAmt().compareTo(BigDecimal.ZERO) == 0) {
			throw new ExpRuntimeException(ErrorCode.D10026);
		}
		if (DrawAccountTypeCode.SALARY_ACCOUNT.equals(DrawAccountTypeCode.getByValue(vo.getOvsaExpDrawInfo().getDrawAccountType()))) {
			/*
			 *  若"領款帳號類別=薪資帳戶"，按[新增]按鈕後， 系統依輸入之"員工代號"讀取「使用者」資料， 以在職檔的"個人匯款總行代號"
			 * + "個人匯款分行代號"產生在"解款行代號"欄位； 「國外研修差旅費用領款資料.匯款開戶ID」=「使用者.個人匯款開戶ID」
			 * 以在職檔的"個人匯款帳碼"產生在"帳號"欄位， 員工代號、金額"為必填欄位。
			 */
			OvsaExpDrawInfo info = vo.getOvsaExpDrawInfo();
			User user = facade.getUserService().findByCode(info.getUser().getCode());
			if (null == user) {
				throw new ExpRuntimeException(ErrorCode.A10002);
			}

			// 輸入之”員工代號”的匯款資訊不足，請至有核定表UC7.5在職資料維護”建立相關資訊
			if (StringUtils.isBlank(user.getRemitBank()) || StringUtils.isBlank(user.getRemitSubbank()) || StringUtils.isBlank(user.getRemitAccount()) || StringUtils.isBlank(user.getRemitAccname())) {
				// 輸入之”員工代號”的匯款資訊不足，請至有核定表UC7.5在職資料維護”建立相關資訊
				throw new ExpRuntimeException(ErrorCode.C10407);
			}
			// 檢核-匯款開戶ID
			if (StringUtils.isBlank(user.getRemitAccId())) {
				if (checkCode(user.getRemitBank())) {
					throw new ExpRuntimeException(ErrorCode.C10382);
				}
			}

			info.setUser(user);
			info.setDrawMoneyUserName(user.getRemitAccname());
			// "個人匯款總行代號" + "個人匯款分行代號"產生在"解款行代號"欄位
			info.setOutwardBankCode(StringUtils.trimToEmpty(user.getRemitBank()) + StringUtils.trimToEmpty(user.getRemitSubbank()));
			// 匯款開戶ID =「使用者.個人匯款開戶ID」
			info.setRemitAccid(user.getRemitAccId());
			// 在職檔的"個人匯款帳碼"產生在"帳號"欄位
			info.setAccountNo(user.getRemitAccount());
			vo.setOvsaExpDrawInfo(info);
		}
	}

	/**
	 * 若傳入的解款行代碼或總行代碼前三碼為700時,回傳true
	 * 
	 * @param code
	 * @return
	 */
	private boolean checkCode(String code) {
		if (StringUtils.isBlank(code) || code.length() < 3) {
			return false;
		}

		if (StringUtils.equals(TransitPaymentDetail.OUTWARD_BANK_CODE_HEADER, code.substring(0, 3))) {
			return true;
		}
		return false;
	}

	/**
	 * 初始化BO
	 * 
	 * @return
	 */
	private OvsaExpDrawInfo initOvsaExpDrawInfo() {
		OvsaExpDrawInfo ovsaExpDrawInfo = new OvsaExpDrawInfo();
		ovsaExpDrawInfo.setUser(new User());
		ovsaExpDrawInfo.setDrawAccountType(facade.getDrawAccountTypeService().findByCode(DrawAccountTypeCode.SALARY_ACCOUNT));
		return ovsaExpDrawInfo;
	}

	// RE201601162_國外出差旅費 EC0416 20160406 start
	/**
	 * 在付款對象頁面，按[刪除]按鈕。
	 * 
	 * <ol>
	 * <li>於更新Session中，刪除該筆付款對象資料</li>
	 * <li>重新顯示付款對象資料於畫面上</li>
	 * </ol>
	 * 
	 */
	public String doDeletePaymentTargetAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		int index = getOvsaExpDrawInfoDataTable().getRowIndex();

		// OvsaExpDrawInfo ovsaExpDrawInfo = (OvsaExpDrawInfo)
		// getOvsaExpDrawInfoDataModel().getRowData();
		OvsaTrvlLrnExp exp = getService().doDeleteOvsaExpDrawInfo(getUpdatingDataValue(), index);
		vo.setBo(exp);
		setUpdatingData(vo);
		setOvsaExpDrawInfoDataModel(null);
		return null;
	}

	// RE201601162_國外出差旅費 EC0416 20160406 end

	/**
	 * 在進入的頁面，按[帳務資料]按鈕。
	 * 
	 * <ol>
	 * <li>導頁至帳務資料頁面</li>
	 * <li>列出該筆國外研修差旅費用的分錄資料</li>
	 * </ol>
	 * 
	 */
	public String doReadAccountInfoAction() {
		setDataModel(null);
		setStationDataModel(null);
		setEntryDataModel(null);

		OvsaTrvlLrnExp exp = getService().findByExpApplNo(((OvsaTrvlLrnExp) getDataTable().getRowData()).getExpapplC().getExpApplNo());
		OvsaTrvlLrnExpVo vo = new OvsaTrvlLrnExpVo(exp);

		this.setUpdatingData(vo);
		return "readEntry";
	}

	/**
	 * 按下[返回]按鈕
	 * <ul>
	 * 頁面如下:
	 * <li>駐在地點 頁面(createStation.jsp)</li>
	 * <li>付款對象 頁面(createOvsaExpDrawInfo.jsp)</li>
	 * <li>帳務資料 頁面(??)</li>
	 * </ul>
	 * 
	 * <ol>
	 * <li>導頁至進入頁面，不執行任何功能</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doCancelAction() {
		this.setDataModel(null);
		this.setOvsaExpDrawInfoDataModel(null);
		this.setStationDataModel(null);
		this.setStationExpDetailDataModel(null);
		setUpdatingData(null);
		return "read";
	}

	/**
	 * 在帳務資料頁面，按下[確認申請]按鈕
	 * 
	 * <ol>
	 * <li>將Session中，該筆國外研修差旅費用狀態設為"申請中"。 call
	 * {@link tw.com.skl.exp.kernel.model6.logic.OvsaTrvlLrnExpService#doConfirmOvsaTrvlLrnExp(tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp, FunctionCode)}
	 * </li>
	 * <li>導頁至進入頁面，並重新撈取費用資料，已確認的資料不再出現。</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doConfirmApplyAction() {
		setShowConfirmButtonBar(false);
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		getService().doConfirmOvsaTrvlLrnExp(exp, this.getFunctionCode());
		return doCancelAction();
	}

	/**
	 * 在帳務資料頁面，按下[科目中文連結]進入修改頁
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doUpdateEntryAction() {
		Entry updateEntry = (Entry) getEntryDataTable().getRowData();
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setEntry(updateEntry);

		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("code", updateEntry.getIncomeIdType());
		setSelectIncomeIdType(this.facade.getIncomeIdTypeService().findByCriteriaMapReturnUnique(criteriaMap));

		setEntryDataModel(null);

		Map map = getFindCriteriaMap();
		map.put("accTitleCode", updateEntry.getAccTitle().getCode());
		map.put("accTitleName", updateEntry.getAccTitle().getName());
		return "updateEntry";
	}

	/**
	 * 在帳務資料頁面，按下[刪除]按鈕刪除該筆分錄
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * @see tw.com.skl.common.model6.web.jsf.managedbean.impl.TemplateDataTableManagedBean#doDeleteOneAction()
	 * 
	 */
	@Override
	public String doDeleteOneAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		Entry deleteEntry = (Entry) getEntryDataTable().getRowData();
		OvsaTrvlLrnExp exp = getService().doDeleteEntry(vo.getBo(), deleteEntry);
		vo.setBo(exp);
		setUpdatingData(vo);
		entryDataModel = null;
		entryDataTable = null;

		return null;
	}

	/**
	 * 在帳務資料頁面，按下[新增]按鈕導向新增帳務頁
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doCreateEntryAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		setEntryDataModel(null);
		vo.setEntry(new Entry());
		Map map = getFindCriteriaMap();
		map.put("accTitleCode", null);
		map.put("accTitleName", null);

		return "createEntry";
	}

	/**
	 * 在帳務資料新增頁面，按下[儲存]按鈕儲存新增的帳務資料
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doSaveCreateEntryAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null != selectIncomeIdType) {
			vo.getEntry().setIncomeIdType(selectIncomeIdType.getCode());
		}

		// 判斷會計科目不可為空值
		if (vo.getEntry().getAccTitle() == null) {
			throw new ExpRuntimeException(ErrorCode.B10062);
		}

		// 「成本單位」檢核條件修改為「會計科目分類」為「4費用」時，再檢核「成本單位」為必填
		if (AccClassTypeCode.CODE_4.getCode().equals(vo.getEntry().getAccTitle().getAccClassType().getCode())) {
			if (StringUtils.isBlank(vo.getEntry().getCostUnitCode())) {
				throw new ExpRuntimeException(ErrorCode.C10263);
			}
		}
		if (vo.getEntry().getAmt() == null || vo.getEntry().getAmt().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ExpRuntimeException(ErrorCode.A10049);
		}
		// 新增一筆entry
		getService().doSaveEntry(getUpdatingDataValue(), vo.getEntry());
		vo.setEntry(initEntry());
		setEntryDataModel(null);
		return "readEntry";
	}

	/**
	 * 在帳務資料修改頁面，按下[儲存]按鈕儲存修改後的帳務資料
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doSaveUpdateEntryAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null != selectIncomeIdType) {
			vo.getEntry().setIncomeIdType(selectIncomeIdType.getCode());
		}
		// 覆蓋掉原本的資料
		vo.setBo(getService().doUpdateEntry(getUpdatingDataValue(), vo.getEntry()));
		vo.setEntry(new Entry());
		setEntryDataModel(null);
		return "readEntry";
	}

	/**
	 * 在帳務資料修改頁面，按下[取消]按鈕回到帳務資料頁
	 * 
	 * <ol>
	 * <li>RE201001687 : 201009新增功能 BY文珊</li>
	 * </ol>
	 * 
	 * 
	 */
	public String doCancelEntryAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		vo.setEntry(new Entry());
		setEntryDataModel(null);
		return "readEntry";
	}

	/**
	 * 地域別下拉選單
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getZoneTypeList() {
		Map map = getFindCriteriaMap();
		if (null == map.get("zoneType")) {
			map.put("zoneType", facade.getZoneTypeService().findByCode(ZoneTypeCode.CODE_1));
		}
		return SelectFactory.createZoneTypeSelect().getSelectListWithoutEmptyItem();
	}

	/**
	 * 國家名稱下拉選單
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getCountryList() {
		Map map = getFindCriteriaMap();
		ZoneType zoneType = (ZoneType) map.get("zoneType");
		return SelectFactory.cerateCountryByZoneTypeCodeSelect(ZoneTypeCode.getByValue(zoneType)).getSelectList();
	}

	/**
	 * 城市名稱下拉選單
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getOvsaCityList() {
		Map map = getFindCriteriaMap();
		Country country = (Country) map.get("country");
		if (null == country) {
			List<SelectItem> list = new ArrayList<SelectItem>();
			list.add(SelectHelper.EMPTY_SELECTITEM);
			return list;
		}
		return SelectFactory.createOvsaCityByCountry(country).getSelectList();
	}

	/**
	 * 項目下拉選單
	 * 
	 */
	public List<SelectItem> getOvsaTrvlLrnExpItemListList() {
		return SelectFactory.createOvsaTrvlLrnExpItemSelect().getSelectListWithoutEmptyItem();
	}

	/**
	 * 付款方式下拉選單
	 * <ul>
	 * 項目有
	 * <li>自付</li>
	 * <li>沖轉暫付</li>
	 * <li>沖轉預付</li>
	 * </ul>
	 * 
	 */
	public List<SelectItem> getPaymentTypeList() {
		SysTypeCode param1 = SysTypeCode.C;
		PaymentTypeCode[] params2 = { PaymentTypeCode.C_SALF_PAY, PaymentTypeCode.C_CHANGE_TEMP_PAY, PaymentTypeCode.C_CHANGE_TEMP_PREPAID };

		Object[] param = new Object[2];
		param[1] = param1;
		param[0] = (Object) params2;

		return SelectFactory.createPaymentTypeSelectByParams1(param).getSelectListWithoutEmptyItem();
	}

	/**
	 * 幣別下拉選單
	 */
	public List<SelectItem> getCurrencyList() {
		return SelectFactory.createCurrencySelect().getSelectList();
	}

	/**
	 * 出差事由下拉選單
	 */
	public List<SelectItem> getBizMatterList() {
		return SelectFactory.createBizMatterSelect(new Object[] { BIZ_MATTER_CODES }).getSelectList();
	}

	/**
	 * 沖轉科目下拉選單
	 * <ul>
	 * 項目有
	 * <li>自付</li>
	 * <li>沖轉暫付(借支)</li>
	 * <li>沖轉週轉金</li>
	 * </ul>
	 * 
	 * RE201001687:新增 BY.文珊
	 */
	public List<SelectItem> getImpulsingAccTileList() {
		SysTypeCode param1 = SysTypeCode.C;
		PaymentTypeCode[] params2 = { PaymentTypeCode.C_CHANGE_TEMP_PAY, PaymentTypeCode.C_WORKING_CAPITAL };

		Object[] param = new Object[2];
		param[1] = param1;
		param[0] = (Object) params2;

		return SelectFactory.createPaymentTypeSelectByParams1(param).getSelectList();
	}

	/**
	 * 借貸別下拉選單
	 * 
	 */
	public List<SelectItem> getEntryTypeList() {

		return SelectFactory.createEntryType2Select().getSelectListWithoutEmptyItem();

	}

	/**
	 * 所得人證號下拉選單
	 * 
	 * RE201001687:新增 BY.文珊
	 */
	public List<SelectItem> getIncomeIdTypeList() {
		return SelectFactory.createIncomeIdTypeSelect().getSelectList();
	}

	/**
	 * 取得領款帳號類別下拉選單
	 * 
	 */
	public List<SelectItem> getDrawAccountTypeList() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo.getOvsaExpDrawInfo().getDrawAccountType()) {
			vo.getOvsaExpDrawInfo().setDrawAccountType(facade.getDrawAccountTypeService().findByCode(DrawAccountTypeCode.SALARY_ACCOUNT));
		}
		return SelectFactory.createDrawAccountTypeSelect().getSelectListWithoutEmptyItem();
	}

	public FunctionCode getFunctionCode() {
		return FunctionCode.C_1_5_6;
	}

	public String getDisplayControlString() {
		DisplayControlBean bean = new DisplayControlBean();
		bean.setDefaultStatus(true);
		bean.setElseElementIds(new String[] { "userId" });
		return bean.toJonString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.common.model6.web.jsf.managedbean.impl.
	 * TemplateDataTableManagedBean#findAllData()
	 */
	@Override
	protected List<OvsaTrvlLrnExp> findAllData() {
		isUpdate = false;
		super.setStatusSearched(false);
		return getService().findByApplStateCode(ApplStateCode.TEMP);
	}

	/**
	 * @return 費用分錄
	 */
	public Entry getExpEntry() {
		return expEntry;
	}

	/**
	 * @param 費用分錄
	 */
	public void setExpEntry(Entry expEntry) {
		this.expEntry = expEntry;
	}

	/**
	 * 取得登入的使用者
	 * 
	 * @return 使用者
	 */
	public User getLoginUser() {
		return (User) AAUtils.getLoggedInUser();
	}

	/**
	 * @return 國外研修差旅費用Facade
	 */
	public OvsaTrvlLrnExpFacade getFacade() {
		return facade;
	}

	/**
	 * @param 國外研修差旅費用Facade
	 */
	public void setFacade(OvsaTrvlLrnExpFacade facade) {
		this.facade = facade;
	}

	/**
	 * valueChangeListener
	 * 
	 * @param vce
	 */
	@SuppressWarnings("unchecked")
	public void modifValueChangeEvent(ValueChangeEvent vce) {
		String tagId = vce.getComponent().getId();
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		Map<String, Object> criteriaMap = null;

		try {
			if (StringUtils.equals(tagId, "userId")) {
				// 變更-申請員工

				User applyUser = facade.getUserService().findByCode((String) vce.getNewValue());
				if (null == applyUser) {
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_bo_IntrTrvlBizExp_applyUserInfo") });
				}
				exp.getExpapplC().setApplyUserInfo(generateApplInfo(applyUser.getCode()));

				// 以「申請人員.匯款單位.單位代號」為預設值。若為空值則帶入「申請人員.所屬單位.單位代號」
				facade.getExpapplCService().generateDrawMoneyUnitByUserCode((String) vce.getNewValue(), exp.getExpapplC());

				// Defect:1378 UAT-C-UC1.5.12
				// 申請人的所屬部室為「分處」時，需帶出「申請人.使用者.成本歸屬之單位代號」做為「成本單位」預設值 #240
				Department department = facade.getCostUnitDefaultService().findDefaultCostUnit(getFunctionCode(), null, applyUser.getDepartment().getCode());
				if (null != department) {
					this.expEntry.setCostUnitCode(department.getCode());
					this.expEntry.setCostUnitName(department.getName());
				}
			}

			if (StringUtils.equals(tagId, "costUnitCode")) {
				// 變更- 成本單位代號
				Department department = facade.getDepartmentService().findByCode((String) vce.getNewValue());
				if (null == department) {
					this.expEntry.setCostUnitCode("");
					this.expEntry.setCostUnitName("");
					throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Entry_costUnit") });
				} else {
					this.expEntry.setCostUnitCode(department.getCode());
					this.expEntry.setCostUnitName(department.getName());
				}
			}

			if (StringUtils.equals(tagId, "bizMatter")) {
				// 變更-出差事由
				exp.setBizMatter((BizMatter) vce.getNewValue());
				// 出差事由不可為空
				if (exp.getBizMatter() == null) {
					throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_OvsaTrvlLrnExp_bizMatter") });
				}

				// 當出差事由為國外高峰會議則日用費為0
				if (exp.getBizMatter().getCode().equals(BizMatterCode.OVSA_SUMMIT_CONFERENCE.getCode())) {
					vo.getStation().setDailySpend(BigDecimal.ZERO);
					vo.getBo().setDailySpendTotalAmt(BigDecimal.ZERO);
					setSelectBizMatterItemDisabled(true);
				} else {
					setSelectBizMatterItemDisabled(false);
				}

				// 設定費用科目
				setExpEntryAcctitle();
				// 設定費用項目
				setExpItem();
			}

			if (StringUtils.equals(tagId, "abroadStartDate")) {
				// 變更-出國年月日起
				exp.setAbroadStartDate((Calendar) vce.getNewValue());

				if (null == vce.getNewValue()) {
					throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_OvsaTrvlLrnExp_abroadStartDate") });
				}
				doCaculateTrvlDaysAction();

				// RE201601162_國外出差旅費 EC0416 20160428 start
				List<ExchangeRate> USrateList = getCurrency(exp.getAbroadStartDate(), "USD");
				if (CollectionUtils.isEmpty(USrateList)) {
					throw new ExpRuntimeException(ErrorCode.C10631, new String[] {});
				}
				exp.setExchangeRateUS(USrateList.get(0).getExchangeRate());
				exp.setExchangeRateId(USrateList.get(0));
				// RE201601162_國外出差旅費 EC0416 20160428 end
			}

			if (StringUtils.equals(tagId, "abroadEndDate")) {
				// 變更-出國年月日迄
				exp.setAbroadEndDate((Calendar) vce.getNewValue());
				if (null == vce.getNewValue()) {
					throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_OvsaTrvlLrnExp_abroadEndDate") });
				}
				doCaculateTrvlDaysAction();
			}

			if (StringUtils.equals(tagId, "userCode")) {
				// 變更-員工代號
				String user = (String) vce.getNewValue();
				if (user.isEmpty()) {
					throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ApplInfo_userId") });

				}
				vo.getOvsaExpDrawInfo().setUser(getUserByCode((String) vce.getNewValue()));

			}

			if (StringUtils.equals(tagId, "zoneType")) {
				// 變更-地域別
				Map map = getFindCriteriaMap();
				map.put("zoneType", vce.getNewValue());
				// 清空-國家
				map.put("country", null);
				// 清空-城市
				vo.getStation().setOvsaCity(null);
				doCaculateDailySpend();
				doCaculateDailyExp();
			}

			if (StringUtils.equals(tagId, "country")) {
				// 變更-國家
				Map map = getFindCriteriaMap();
				map.put("country", vce.getNewValue());
				// 清空-城市
				vo.getStation().setOvsaCity(null);
				doCaculateDailySpend();
				doCaculateDailyExp();
			}

			if (StringUtils.equals(tagId, "ovsaCity")) {

				// 變更-城市名稱
				vo.getStation().setOvsaCity((OvsaCity) vce.getNewValue());

				doCaculateDailySpend();
				doCaculateDailyExp();

				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
				// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
				setSaveCount(false);
				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end
			}

			if (StringUtils.equals(tagId, "approveStayFee")) {
				// 變更-是否核宿費
				vo.getStation().setApproveStayFee((Boolean) vce.getNewValue());
				doCaculateDailySpend();
			}

			if (StringUtils.equals(tagId, "stationStartDate")) {
				// 變更-駐在起訖期間 : 起
				vo.getStation().setStationStartDate((Calendar) vce.getNewValue());
				getCalculateDaytime();
				doCaculateDailySpend();
				doCaculateDailyExp();

				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
				// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
				setSaveCount(false);
				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end

			}

			if (StringUtils.equals(tagId, "stationEndDate")) {
				// 變更-駐在起訖期間 : 訖
				vo.getStation().setStationEndDate((Calendar) vce.getNewValue());
				getCalculateDaytime();
				doCaculateDailySpend();
				doCaculateDailyExp();

				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
				// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
				setSaveCount(false);
				// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end
			}

			if (StringUtils.equals(tagId, "iValue")) {
				// 變更-i值
				vo.getStation().setIValue((BigDecimal) vce.getNewValue());
				getCalculateDaytime();
				doCaculateDailySpend();
				doCaculateDailyExp();
			}

			if (StringUtils.equals(tagId, "payBreakfast")) {
				// 變更-膳食自付 : 早
				vo.getStation().setPayBreakfast((Boolean) vce.getNewValue());
				doCaculateDailySpend();
			}

			if (StringUtils.equals(tagId, "payLunch")) {
				// 變更-膳食自付 : 午
				vo.getStation().setPayLunch((Boolean) vce.getNewValue());
				doCaculateDailySpend();
			}

			if (StringUtils.equals(tagId, "payDinner")) {
				// 變更-膳食自付 : 晚
				vo.getStation().setPayDinner((Boolean) vce.getNewValue());
				doCaculateDailySpend();
			}
			// RE201601162_國外出差旅費 EC0416 start
			if (StringUtils.equals(tagId, "originalRateAmount")) {
				// 變更-原幣別金額
				// vo.getStationExpDetail().setOriginalRateAmount((BigDecimal)
				// vce.getNewValue());
				// 計算新台幣
				// doCaculateNTD((BigDecimal) vce.getNewValue(),
				// vo.getStationExpDetail().getExchangeRate());
				BigDecimal originalRateAmount = (BigDecimal) vce.getNewValue();
				// int index = getStationDataTable().getRowIndex();
				BigDecimal exchangeRate = (BigDecimal) vo.getStationExpDetail().getExchangeRate();
				BigDecimal ntd = BigDecimal.ZERO;
				if (null == originalRateAmount || BigDecimal.ZERO.compareTo(originalRateAmount) == 0) {
					// RE201601162
					vo.getStationExpDetail().setAmount(ntd);
				}
				if (exchangeRate == null || BigDecimal.ZERO.compareTo(exchangeRate) == 0) {
					String currencyName = "";
					if (vo.getStationExpDetail().getCurrency() != null) {
						currencyName = vo.getStationExpDetail().getCurrency().getName();
					}
					throw new ExpRuntimeException(ErrorCode.C10630, new String[] { currencyName });
				}
				// 「原幣別金額X外幣兌換率」，若有小數時，自動四捨五入至整數
				ntd = originalRateAmount.multiply(exchangeRate);
				vo.getStationExpDetail().setAmount(ntd.setScale(0, BigDecimal.ROUND_HALF_UP));
			}
			if (StringUtils.equals(tagId, "exchangeRate")) {
				// 變更-外幣兌換率
				// vo.getStationExpDetail().setExchangeRate((BigDecimal)
				// vce.getNewValue());
				// 計算新台幣
				doCaculateNTD(vo.getStationExpDetail().getOriginalRateAmount(), (BigDecimal) vce.getNewValue());
			}
			// RE201601162_國外出差旅費 EC0416 start
			// 輸入會計科目代號會帶出會計科目名稱、業別代號 BY 文珊
			if (StringUtils.equals(tagId, "accTitleCode")) {
				if (null != vce.getNewValue() && ((String) vce.getNewValue()).length() >= 8) {
					criteriaMap = new HashMap<String, Object>();
					criteriaMap.put("code", vce.getNewValue());

					// 帶出會計科目名稱
					AccTitle accTitle = this.facade.getAccTitleService().findByCriteriaMapReturnUnique(criteriaMap);
					if (null != accTitle) {
						Map map = getFindCriteriaMap();
						map.put("accTitleName", accTitle.getName());
						vo.getEntry().setAccTitle(accTitle);
					} else {
						throw new ExpRuntimeException(ErrorCode.B10062, new String[] {});
					}

					// 帶出業別代號
					String incomeBiz = accTitle.getIncomeBiz();
					vo.getEntry().setIndustryCode(incomeBiz);

					criteriaMap = null;
				} else {
					throw new ExpRuntimeException(ErrorCode.B10062, new String[] {});
				}
			}

			// 更新費用明細_成本單位帶出中文 BY文珊
			if (StringUtils.equals(tagId, "costUnitNo")) {
				if (null != vce.getNewValue() && !vce.getNewValue().equals("")) {
					Department department = this.facade.getDepartmentService().getDepartmentByParam((String) vce.getNewValue());
					if (null != department) {
						// 是否啟用
						if (!department.isEnabled()) {
							throw new ExpRuntimeException(ErrorCode.A10050, new String[] { (String) vce.getNewValue() });
						}
						vo.getEntry().setCostUnitName(department.getName());
						vo.getEntry().setCostUnitCode(department.getCode());
					} else {
						vo.getEntry().setCostUnitName(null);
						vo.getEntry().setCostUnitCode(null);
						throw new ExpRuntimeException(ErrorCode.C10013);
					}

				} else {
					vo.getEntry().setCostUnitName(null);
					vo.getEntry().setCostUnitCode(null);
				}
			}
			// RE201601162_國外出差旅費 EC0416 end
			if (StringUtils.equals(tagId, "currency")) {
				// int index = getStationDataTable().getRowIndex();
				Station station = vo.getStation();
				Currency currency = (Currency) vce.getNewValue();
				if (currency == null) {
					vo.getStationExpDetail().setExchangeRate(BigDecimal.ZERO);
				} else {
					List<ExchangeRate> USrateList = getCurrency(vo.getBo().getAbroadStartDate(), currency.getCode());
					// 判斷如果幣別下拉式選單選擇新台幣則匯率塞入1
					if (currency.getCode().equals(CurrencyCode.NTD.getCode())) {
						vo.getStationExpDetail().setExchangeRate(BigDecimal.ONE);
						// 設定匯率關聯

					} else if (CollectionUtils.isEmpty(USrateList)) {
						vo.getStationExpDetail().setExchangeRate(BigDecimal.ZERO);

						throw new ExpRuntimeException(ErrorCode.C10630, new String[] { currency.getName() });
					} else {
						vo.getStationExpDetail().setExchangeRate(USrateList.get(0).getExchangeRate());
						// 設定匯率關聯
						vo.getStationExpDetail().setExchangeRateId(USrateList.get(0));
					}
					// 0713 start
					BigDecimal originalRateAmount = vo.getStationExpDetail().getOriginalRateAmount();
					BigDecimal exchangeRate = (BigDecimal) vo.getStationExpDetail().getExchangeRate();
					BigDecimal ntd = BigDecimal.ZERO;
					if (null == originalRateAmount || BigDecimal.ZERO.compareTo(originalRateAmount) == 0) {
						// RE201601162
						vo.getStationExpDetail().setAmount(ntd);
					}
					if (exchangeRate == null || BigDecimal.ZERO.compareTo(exchangeRate) == 0) {
						String currencyName = "";
						if (vo.getStationExpDetail().getCurrency() != null) {
							currencyName = vo.getStationExpDetail().getCurrency().getName();
						}
						throw new ExpRuntimeException(ErrorCode.C10630, new String[] { currencyName });
					} else if (originalRateAmount != null) {
						ntd = originalRateAmount.multiply(exchangeRate);
						vo.getStationExpDetail().setAmount(ntd.setScale(0, BigDecimal.ROUND_HALF_UP));
					}
					// 「原幣別金額X外幣兌換率」，若有小數時，自動四捨五入至整數
					ntd = originalRateAmount.multiply(exchangeRate);
					vo.getStationExpDetail().setAmount(ntd.setScale(0, BigDecimal.ROUND_HALF_UP));
				}
			}
			// RE201601162_0707

			if (StringUtils.equals(tagId, "ovsaTrvlLrnExpItem")) {

			}
			// RE201601162_國外出差旅費 EC0416 start

		} catch (ExpRuntimeException e) {
			logger.error(e.getMessage(), e);
			MessageManager.getInstance().showErrorCodeMessage(e.getErrorCode().toString(), null, (Object[]) e.getParams());
		}
	}

	private void setExpItem() {
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		exp.getExpapplC().setExpItem(null);
		// 出差事由 = 國外旅費
		if (BizMatterCode.OVSA_TRVL_EXP.equals(BizMatterCode.getByValue(exp.getBizMatter()))) {
			// 費用項目 = 維持費用-國外旅費 62040400
			facade.getExpapplCService().setExpItemByCode(exp.getExpapplC(), ExpItemCode.OVSA_TRVL_EXP);
		}
		// 出差事由 = 國外研修差旅費
		if (BizMatterCode.OVSA_TRVL_LRN_EXP.equals(BizMatterCode.getByValue(exp.getBizMatter())) || BizMatterCode.OVSA_SUMMIT_CONFERENCE.equals(BizMatterCode.getByValue(exp.getBizMatter()))) {
			// 費用項目 = 維持費用-國外研修旅費 62040500
			facade.getExpapplCService().setExpItemByCode(exp.getExpapplC(), ExpItemCode.OVSA_TRVL_LRN_EXP);
		}
	}

	/**
	 * 在駐在地點頁面按下[計算]按鈕
	 * 
	 * @return
	 */
	public String doCaculateDataAction() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();

		// 檢核日期
		getService().checkDate(vo.getBo(), vo.getStation());
		// 美金兌換率不可為0
		if (vo.getBo().getExchangeRateUS().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ExpRuntimeException(ErrorCode.C10224, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_OvsaTrvlLrnExp_exchangeRateUS") });
		}
		// 檢核城市
		if (vo.getStation().getOvsaCity() == null) {

		}
		// 計算-駐在日數
		getCalculateDaytime();
		// 計算-駐在期間日用費
		doCaculateDailySpend();
		// 計算-駐在期間日支費
		doCaculateDailyExp();

		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
		// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
		setSaveCount(true);
		// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end
		return null;
	}

	/**
	 * 計算新台幣
	 * 
	 * @param originalRateAmount
	 *            原幣別金額
	 * @param exchangeRate
	 *            外幣兌換率
	 */
	private void doCaculateNTD(BigDecimal originalRateAmount, BigDecimal exchangeRate) {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStationExpDetail()) {
			return;
		}
		BigDecimal ntd = BigDecimal.ZERO;// facade.getStationExpDetailService().doCaculateNTD(vo.getStationExpDetail());

		if (null == originalRateAmount || null == exchangeRate || BigDecimal.ZERO.compareTo(originalRateAmount) == 0 || BigDecimal.ZERO.compareTo(exchangeRate) == 0) {
			vo.getStationExpDetail().setAmount(ntd);
		}
		// 「原幣別金額X外幣兌換率」，若有小數時，自動四捨五入至整數
		ntd = originalRateAmount.multiply(exchangeRate);
		// 金額(台幣) 有小數時，自動四捨五入至整數
		vo.getStationExpDetail().setAmount(ntd.setScale(0, BigDecimal.ROUND_HALF_UP));

	}

	/**
	 * 處理-駐在期間日用費
	 */
	private void doCaculateDailySpend() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStation()) {
			return;
		}

		// 如果出國事由為國外高峰行程則日用費金額為零
		if (vo.getBo().getBizMatter() != null && vo.getBo().getBizMatter().getCode().equals(BizMatterCode.OVSA_SUMMIT_CONFERENCE.getCode())) {
			vo.getStation().setDailySpend(BigDecimal.ZERO);
			setSelectBizMatterItemDisabled(true);

		} else {
			// setSelectBizMatterItemDisabled(false);
			vo.getStation().setDailySpend(facade.getStationService().doCaculateDailySpend(getUpdatingDataValue(), vo.getStation()));
		}
	}

	/**
	 * 處理-駐在期間日支費
	 */
	private void doCaculateDailyExp() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStation()) {
			return;
		}
		vo.getStation().setDailyExp(facade.getStationService().doCaculateDailyExp(getUpdatingDataValue(), vo.getStation()));
	}

	/**
	 * 取得駐在期間字串 如: 2007/01/01~2007/01/20
	 * 
	 * @return
	 */
	public String getStayDate() {
		Station station = (Station) getStationDataTable().getRowData();
		StringBuffer sb = new StringBuffer();
		sb.append(DateUtils.getISODateStr(station.getStationStartDate().getTime(), DateUtils.ROC_LINK));
		sb.append("~");
		sb.append(DateUtils.getISODateStr(station.getStationEndDate().getTime(), DateUtils.ROC_LINK));

		return sb.toString();
	}

	/**
	 * 取得膳食自付字串 :午、晚
	 * 
	 * @return
	 */
	public String getPayMeal() {
		Station station = (Station) getStationDataTable().getRowData();
		StringBuffer sb = new StringBuffer();
		// 早
		if (station.isPayBreakfast()) {
			sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Station_payBreakfast1"));
		}

		// 午
		if (station.isPayLunch()) {
			if (sb.length() != 0) {
				sb.append("、");
			}
			sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Station_payLunch1"));
		}

		// 晚
		if (station.isPayDinner()) {
			if (sb.length() != 0) {
				sb.append("、");
			}
			sb.append(MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_Station_payDinner1"));
		}

		return sb.toString();
	}

	/**
	 * 計算-出差費用
	 * <p>
	 * 出差費用=「駐在地點.駐在地點之費用明細」List中，所有”金額”欄位的累計
	 * 
	 * @return
	 */
	public BigDecimal getAbrodTotalAmt() {
		Station station = (Station) getStationDataTable().getRowData();
		return facade.getStationService().doCaculateAbrodTotalAmt(station);
	}

	/**
	 * 計算-雜費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=雜費的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getMiscellaneousTotalExp() {
		Station station = (Station) getStationDataTable().getRowData();
		return facade.getStationService().doCaculateMiscellaneousTotalExp(station);
	}

	/**
	 * 計算-雜費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=雜費的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getMiscellaneousTotalExpForDetail() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		return facade.getStationService().doCaculateMiscellaneousTotalExp(vo.getStation());
	}

	/**
	 * 計算-宿泊費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=宿泊費的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getHotelTotalExp() {
		Station station = (Station) getStationDataTable().getRowData();
		return facade.getStationService().doCaculateHotelTotalExp(station);
	}

	/**
	 * 計算-宿泊費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=宿泊費的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getHotelTotalExpForDetail() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		return facade.getStationService().doCaculateHotelTotalExp(vo.getStation());
	}

	/**
	 * 計算-交通費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=交通費 的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getTrafficTotalExp() {
		Station station = (Station) getStationDataTable().getRowData();
		return facade.getStationService().doCaculateTrafficTotalExp(station);
	}

	/**
	 * 計算-交通費
	 * <p>
	 * 「駐在地點.駐在地點之費用明細」List中，” 國外差旅費用項目”=交通費 的”金額”欄位累計
	 * 
	 * @return
	 */
	public BigDecimal getTrafficTotalExpForDetail() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		return facade.getStationService().doCaculateTrafficTotalExp(vo.getStation());
	}

	private User getUserByCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		User user = facade.getUserService().findByCode(code);
		if (null == user) {
			// 顯示《員工代號錯誤，請查明後再申請》訊息
			throw new ExpRuntimeException(ErrorCode.A10002);
		} else if (!user.isEnabled()) {
			//  員工代號非在職(依離職日判斷)狀態，顯示《此員工代號非公司職員，請查明後再申請》訊息，仍可存檔，顯示之訊息亦需儲存。
			MessageManager.getInstance().showWarnningCodeMessage(ErrorCode.A10003.toString());
		}

		return user;
	}

	/**
	 * 產生申請人資訊
	 * 
	 * @param userId
	 *            員工代碼
	 */
	private ApplInfo generateApplInfo(String userId) {
		User user = getUserByCode(userId);
		ApplInfo applInfo = facade.getApplInfoService().generateApplyUserInfo(user.getCode(), user.getDepartment().getCode());
		return applInfo;
	}

	/**
	 * 依操作人員輸入出差起迄日期後系統自動計算出差天數（無條件捨去至小數一位），例如2.0天1夜 (綁定至"出差日期起"及"出差日期訖"兩個欄位)
	 * 
	 * @return
	 */
	private void doCaculateTrvlDaysAction() {
		OvsaTrvlLrnExp exp = this.getUpdatingDataValue();
		Calendar startDate = exp.getAbroadStartDate();
		Calendar endDate = exp.getAbroadEndDate();
		startDate = DateUtils.getFirstMinuteOfDay(startDate);
		endDate = DateUtils.getFirstMinuteOfDay(endDate);
		if (null == startDate || null == endDate || endDate.compareTo(startDate) < 0) {
			exp.setDaytimeNo(BigDecimal.ZERO);
			exp.setNighttimeNo(BigDecimal.ZERO);
		} else {
			exp.setDaytimeNo(new BigDecimal(DateUtils.getDays(startDate.getTime(), endDate.getTime()) + 1));
			if (BigDecimal.ZERO.compareTo(exp.getDaytimeNo()) < 0) {
				exp.setNighttimeNo(exp.getDaytimeNo().subtract(new BigDecimal("1")));
			} else {
				exp.setNighttimeNo(BigDecimal.ZERO);
			}
		}

	}

	/**
	 * 在維護國外研修差旅費用頁面按下[儲存]按鈕
	 */
	@Override
	public String doSaveUpdateAction() {
		setShowConfirmButtonBar(false);
		OvsaTrvlLrnExp exp = this.getUpdatingDataValue();

		// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 start
		/*
		 * if (StringUtils.equals(ApplStateCode.TEMP.getCode(),
		 * exp.getExpapplC().getApplState().getCode())) {
		 * getService().doUpdateTempOvsaTrvlLrnExp(exp, deleteEntryList);
		 * deleteEntryList = new ArrayList<Entry>(); setDataModel(null); return
		 * "read"; }else{
		 */
		// 檢核主頁面
		getService().doValidateOvsaExp(exp, expEntry);
		getService().doUpdateOvsaTrvlLrnExp(exp, fromFunctionCode, deleteEntryList);
		// 紀錄主頁面的出國年月日起跟出國年月日迄
		ovsaStartDate = updatingData.getBo().getAbroadStartDate();
		ovsaEndDate = updatingData.getBo().getAbroadEndDate();
		ovsaDaiylExpapplCID = getUpdatingDataValue().getExpapplC().getId();

		Map<String, Object> crit = new HashMap<String, Object>();
		crit.put("expapplC", getUpdatingDataValue().getExpapplC());
		List<OvsaDailyWork> ovsaDailyWorkList = facade.getOvsaDailyWorkService().findByCriteriaMap(crit);

		initOvsaDailyWork(ovsaDailyWorkList);
		// 判斷從C1.6.10國外申請單修改頁面進入國外日報表頁面，按[返回]會跳回C1.6.10
		this.isDailyWorkPage = false;

		return "createOvsaDailyWork";
		// return fromFunctionCode.getCode();
		// }
		// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 end
	}

	/**
	 * 在新增國外研修差旅費用頁面按下[儲存]按鈕
	 */
	public String doSaveCreatePageAction() {
		setShowConfirmButtonBar(false);
		return null;

	}

	/**
	 * 按下[更新費用明細]按鈕
	 * 
	 * @return
	 */
	public String doGetExpDetailAction() {
		OvsaTrvlLrnExp exp = this.getUpdatingDataValue();
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		// exp.getExpapplC().setPaymentType(vo.getImpulsingAccTile());
		// 檢核主頁面
		getService().doValidateOvsaExp(exp, expEntry);
		getService().calculateEntry(exp, expEntry, vo.getImpulsingAccTile());
		isClickGenEntry = false;
		setEntryDataModel(null);
		return null;
	}

	/**
	 * 重設
	 */
	public String doResetAction() {

		if (!isCreate) {
			isCreate = true;
		} else if (!isUpdatePage) {
			isUpdatePage = true;
		}

		return "create";
	}

	/**
	 * @return the entryDataModel
	 */
	public CollectionModel getEntryDataModel() {
		if (entryDataModel == null) {

			entryDataModel = new SortableModel();
			List<Entry> entryList = new ArrayList<Entry>();
			OvsaTrvlLrnExp exp = getUpdatingDataValue();
			facade.getExpapplCService().doSortEntry(exp.getExpapplC());
			if (null != exp && null != exp.getExpapplC() && null != exp.getExpapplC().getEntryGroup() && !CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
				entryList = exp.getExpapplC().getEntryGroup().getEntries();
			}
			// 將資料List放入DataModel內
			entryDataModel.setWrappedData(entryList);
		}
		return entryDataModel;
	}

	/**
	 * @param entryDataModel
	 *            the entryDataModel to set
	 */
	public void setEntryDataModel(CollectionModel entryDataModel) {
		this.entryDataModel = entryDataModel;
	}

	/**
	 * @return the entryDataTable
	 */
	public CoreTable getEntryDataTable() {
		return entryDataTable;
	}

	/**
	 * @param entryDataTable
	 *            the entryDataTable to set
	 */
	public void setEntryDataTable(CoreTable entryDataTable) {
		this.entryDataTable = entryDataTable;
	}

	/**
	 * @return 領款資料DataModel
	 */
	public CollectionModel getOvsaExpDrawInfoDataModel() {
		if (ovsaExpDrawInfoDataModel == null) {
			ovsaExpDrawInfoDataModel = new SortableModel();
			OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
			// 將資料List放入DataModel內
			ovsaExpDrawInfoDataModel.setWrappedData(vo.getBo().getOvsaExpDrawInfos());
		}
		return ovsaExpDrawInfoDataModel;
	}

	/**
	 * @param 領款資料DataModel
	 */
	public void setOvsaExpDrawInfoDataModel(CollectionModel ovsaExpDrawInfoDataModel) {
		this.ovsaExpDrawInfoDataModel = ovsaExpDrawInfoDataModel;
	}

	/**
	 * @return 領款資料DataTable
	 */
	public CoreTable getOvsaExpDrawInfoDataTable() {
		return ovsaExpDrawInfoDataTable;
	}

	/**
	 * @param 領款資料DataTable
	 */
	public void setOvsaExpDrawInfoDataTable(CoreTable ovsaExpDrawInfoDataTable) {
		this.ovsaExpDrawInfoDataTable = ovsaExpDrawInfoDataTable;
	}

	public boolean isModifyByDrawAccountType() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (DrawAccountTypeCode.OTHER_INPUT.equals(DrawAccountTypeCode.getByValue(vo.getOvsaExpDrawInfo().getDrawAccountType()))) {
			return true;
		}

		return false;
	}

	// RE201601162 0712
	public boolean isModifyByItem() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (OvsaTrvlLrnExpItemCode.TRAFFIC_EXP.equals(OvsaTrvlLrnExpItemCode.getByValue(vo.getStationExpDetail().getOvsaTrvlLrnExpItem().getName()))) {
			return true;
		}

		return false;
	}

	public boolean isModifyByItem2() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();

		if (OvsaTrvlLrnExpItemCode.MISCELLANEOUS_EXP.equals(OvsaTrvlLrnExpItemCode.getByValue(vo.getStationExpDetail().getOvsaTrvlLrnExpItem().getName()))) {
			return true;
		}
		return false;
	}

	public String getOvsaTrvlLrnExpItemName() {

		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (OvsaTrvlLrnExpItemCode.TRAFFIC_EXP.equals(OvsaTrvlLrnExpItemCode.getByValue(vo.getStationExpDetail().getOvsaTrvlLrnExpItem().getCode()))) {
			return MessageUtils.getAccessor().getMessage("C1_5_6_traffic_reason");
		}
		if (OvsaTrvlLrnExpItemCode.MISCELLANEOUS_EXP.equals(OvsaTrvlLrnExpItemCode.getByValue(vo.getStationExpDetail().getOvsaTrvlLrnExpItem().getCode()))) {
			return MessageUtils.getAccessor().getMessage("C1_5_6_charges_reason");
		}
		if (OvsaTrvlLrnExpItemCode.HOTEL_EXP.equals(OvsaTrvlLrnExpItemCode.getByValue(vo.getStationExpDetail().getOvsaTrvlLrnExpItem().getCode()))) {
			return "";
		}

		return null;

	}

	public boolean isApproveStayFeeY() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (vo.getStation().isApproveStayFee()) {
			return true;
		}
		return false;
	}

	public boolean isApproveStayFeeN() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (vo.getStation().isApproveStayFee()) {
			return false;
		}
		return true;
	}

	/**
	 * @return 駐在地點DataModel
	 */
	public CollectionModel getStationDataModel() {
		if (stationDataModel == null) {
			stationDataModel = new SortableModel();
			OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
			// 將資料List放入DataModel內
			stationDataModel.setWrappedData(vo.getBo().getStations());
		}
		return stationDataModel;
	}

	/**
	 * @param 駐在地點DataModel
	 */
	public void setStationDataModel(CollectionModel stationDataModel) {
		this.stationDataModel = stationDataModel;
	}

	/**
	 * @return 駐在地點DataTable
	 */
	public CoreTable getStationDataTable() {
		return stationDataTable;
	}

	/**
	 * @param 駐在地點DataTable
	 */
	public void setStationDataTable(CoreTable stationDataTable) {
		this.stationDataTable = stationDataTable;
	}

	/**
	 * @return the DataModel
	 */
	public CollectionModel getStationExpDetailDataModel() {
		// 20160708
		if (stationExpDetailDataModel == null) {
			stationExpDetailDataModel = new SortableModel();
			OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
			// 將資料List放入DataModel內
			stationExpDetailDataModel.setWrappedData(vo.getStation().getStationExpDetails());
		}
		return stationExpDetailDataModel;
	}

	/**
	 * @param stationExpDetailDataModel
	 *            the stationExpDetailDataModel to set
	 */
	public void setStationExpDetailDataModel(CollectionModel stationExpDetailDataModel) {
		this.stationExpDetailDataModel = stationExpDetailDataModel;
	}

	/**
	 * @return the stationExpDetailDataTable
	 */
	public CoreTable getStationExpDetailDataTable() {
		return stationExpDetailDataTable;
	}

	/**
	 * @param stationExpDetailDataTable
	 *            the stationExpDetailDataTable to set
	 */
	public void setStationExpDetailDataTable(CoreTable stationExpDetailDataTable) {
		this.stationExpDetailDataTable = stationExpDetailDataTable;
	}

	/**
	 * @return 日用費計算說明
	 */
	public String getDailyExpNotes() {
		return dailyExpNotes;
	}

	/**
	 * @param 日用費計算說明
	 */
	public void setDailyExpNotes(String dailyExpNotes) {
		this.dailyExpNotes = dailyExpNotes;
	}

	public boolean isNTD() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStationExpDetail() || null == vo.getStationExpDetail().getCurrency()) {
			return false;
		}

		if (CurrencyCode.NTD.equals(CurrencyCode.getByValue(vo.getStationExpDetail().getCurrency()))) {
			vo.getStationExpDetail().setExchangeRate(new BigDecimal("1"));
			doCaculateNTD(vo.getStationExpDetail().getOriginalRateAmount(), vo.getStationExpDetail().getExchangeRate());
			return true;
		}
		doCaculateNTD(vo.getStationExpDetail().getOriginalRateAmount(), vo.getStationExpDetail().getExchangeRate());
		return false;
	}

	private AccTitleCode getAccTitleCodeByBizMatter(BizMatter bizMatter) {
		if (null == bizMatter) {
			return null;
		}

		if (BizMatterCode.OVSA_TRVL_EXP.equals(BizMatterCode.getByValue(bizMatter))) {
			return AccTitleCode.OVSA_TRVL_EXP;
		}
		// RE201601162_國外研修差旅 EC0416 20160406 START
		if (BizMatterCode.OVSA_TRVL_LRN_EXP.equals(BizMatterCode.getByValue(bizMatter)) || (BizMatterCode.OVSA_SUMMIT_CONFERENCE.equals(BizMatterCode.getByValue(bizMatter)))) {
			return AccTitleCode.CODE_62040523;
		}
		// RE201601162_國外研修差旅 EC0416 20160406 END

		return null;
	}

	/**
	 * 依出差事由設定會計科目
	 */
	private void setExpEntryAcctitle() {
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		AccTitleCode accTitleCode = getAccTitleCodeByBizMatter(exp.getBizMatter());
		if (null != accTitleCode) {
			this.expEntry.setAccTitle(facade.getAccTitleService().findByCode(accTitleCode.getCode()));
		} else {
			this.expEntry.setAccTitle(null);
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean isShowEntryDataTable() {
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		if (StringUtils.equals(ApplStateCode.TEMP.getCode(), exp.getExpapplC().getApplState().getCode())) {
			return false;
		}
		return true;
	}

	/**
	 * 該筆資料狀態是否為暫存中
	 * 
	 * @return
	 */
	public boolean isTempState() {
		OvsaTrvlLrnExp exp = (OvsaTrvlLrnExp) getDataTable().getRowData();
		if (StringUtils.equals(ApplStateCode.TEMP.getCode(), exp.getExpapplC().getApplState().getCode())) {
			return true;
		}
		return false;
	}

	/**
	 * 資料若是來自UC3.2.2,則顯示退件表格
	 * 
	 * @return
	 */
	public boolean isFromGovExpApproveManagedBean() {
		if (null != fromFunctionCode && FunctionCode.C_3_2_2.equals(fromFunctionCode)) {
			return true;
		}
		return false;
	}

	public void doUpdateVo() {

		ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
		Map<String, Object> seeeionmap = context.getSessionMap();// 取出seeeionmap
																	// 內有放ManagedBean
		if (!seeeionmap.isEmpty() && seeeionmap.size() > 0) {

			for (Object obj : seeeionmap.values()) {
				if (obj instanceof TrvlAndOvsaExpApplManagedBean) {
					// 資料來自1.6.4
					TrvlAndOvsaExpApplManagedBean managedBean = (TrvlAndOvsaExpApplManagedBean) obj;

					ExpapplCVo vo = (ExpapplCVo) managedBean.getUpdatingData();
					if (null != vo && vo.isUpdateAction()) {
						fromFunctionCode = managedBean.getFunctionCode();
						doInitUpdateVo(vo.getBo().getExpApplNo());
						break;
					}

				} else if (obj instanceof DeliverDaylistGenManagedBean) {
					// 資料來自1.6.7
					DeliverDaylistGenManagedBean managedBean = (DeliverDaylistGenManagedBean) obj;

					DeliverDaylistVo vo = (DeliverDaylistVo) managedBean.getUpdatingData();

					if (null != vo && vo.isUpdateAction()) {
						fromFunctionCode = managedBean.getFunctionCode();
						doInitUpdateVo(vo.getExpApplNo());
						break;
					}
				} else if (obj instanceof GovExpApproveManagedBean) {
					// 資料來自UC 3.2.2
					GovExpApproveManagedBean managedBean = (GovExpApproveManagedBean) obj;

					ExpapplCVo vo = (ExpapplCVo) managedBean.getUpdatingData();

					// RE201101034核銷鎖住欄位: 由核銷頁面引導至此時鎖住某些欄位
					// 2012/04/13 cm9539
					this.readOnlyWhenPay = true;

					if (null != vo && vo.isUpdateAction()) {
						fromFunctionCode = managedBean.getFunctionCode();
						doInitUpdateVo(vo.getBo().getExpApplNo());
						break;
					}
				}
				// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 start
				else if (obj instanceof TrvlExpApplSearchManagedBean) {
					// 資料來自UC 1.6.10
					TrvlExpApplSearchManagedBean managedBean = (TrvlExpApplSearchManagedBean) obj;

					ExpapplCVo vo = (ExpapplCVo) managedBean.getUpdatingData();
					if (null != vo && vo.isUpdateAction()) {
						fromFunctionCode = managedBean.getFunctionCode();
						doInitUpdateVo(vo.getBo().getExpApplNo());
						break;
					}
				}
				// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 end
			}

		}

	}

	/**
	 * 初始化維護頁面資料
	 * 
	 * @param expApplNo
	 *            申請單單號
	 */
	@SuppressWarnings("unchecked")
	private void doInitUpdateVo(String expApplNo) {
		Map map = getFindCriteriaMap();
		map.put("expApplNo", expApplNo);
		// this.setDataModel(null);

		OvsaTrvlLrnExp bo = this.getService().findByExpApplNo(expApplNo);
		OvsaTrvlLrnExpVo vo = new OvsaTrvlLrnExpVo(bo);
		this.setUpdatingData(this.wrap(bo));
		// 初始修改的資料，所有修改資料的初始設定都在此method進行
		this.initUpdatingData(this.getUpdatingData());
		// 裡面沒有實作任何程式碼，只是先留下介面
		// 駐在地點
		setDataModel(null);

		vo.setStation(new Station());
		setStationDataModel(null);

		vo.setEntry(initEntry());
		setDataModel(null);

		vo.setOvsaExpDrawInfo(initOvsaExpDrawInfo());
		setOvsaExpDrawInfoDataModel(null);

		vo.setStationExpDetailList(intitStatinoDetailList());
		this.setUpdatingData(vo);
		this.doAfterUpdateAction();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tw.com.skl.common.model6.web.jsf.managedbean.impl.BaseManagedBeanImpl
	 * #findDataWithCriteria()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected List<OvsaTrvlLrnExp> findDataWithCriteria() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (initUpdateDate) {
			// 只做一次判斷
			doUpdateVo();
			initUpdateDate = false;
		}
		List<OvsaTrvlLrnExp> list = new ArrayList<OvsaTrvlLrnExp>();
		if (null == this.getFindCriteriaMap() || this.getFindCriteriaMap().size() == 0 || null == getFindCriteriaMap().get("expApplNo") || StringUtils.isBlank((String) getFindCriteriaMap().get("expApplNo"))) {
			list = this.findAllData();
		} else {
			Map map = getFindCriteriaMap();
			String expApplNo = (String) map.get("expApplNo");
			OvsaTrvlLrnExp exp = getService().findByExpApplNo(StringUtils.trimToNull(expApplNo));
			if (null != exp) {
				isUpdate = true;
				list.add(exp);
			} else {
				list = this.findAllData();
			}

		}
		return list;
	}

	public boolean isUpdate() {
		return isUpdate;
	}

	public void setUpdate(boolean isUpdate) {
		this.isUpdate = isUpdate;
	}

	/**
	 * @return the isClickGenEntry
	 */
	public boolean isClickGenEntry() {
		return isClickGenEntry;
	}

	/**
	 * @param isClickGenEntry
	 *            the isClickGenEntry to set
	 */
	public void setClickGenEntry(boolean isClickGenEntry) {
		this.isClickGenEntry = isClickGenEntry;
	}

	/**
	 * 是否顯示預算超支檢核結果按鈕
	 * 
	 * @return
	 */
	public boolean isShowConfirmButtonBar() {
		return showConfirmButtonBar;
	}

	/**
	 * 是否顯示預算超支檢核結果按鈕
	 * 
	 * @param
	 */
	public void setShowConfirmButtonBar(boolean showConfirmButtonBar) {
		this.showConfirmButtonBar = showConfirmButtonBar;
	}

	/**
	 * 在updateOvsaTrvlLrnExp.jsp頁面, 按下[儲存]按鈕執行預算超支檢核
	 * 
	 * @return
	 */
	public String doCheckBudgetAction() {
		Map<String, Object> map = facade.getExpapplCService().checkBudget(getUpdatingDataValue().getExpapplC());
		if (null == map) {
			return doSaveUpdateAction();
		}
		setShowConfirmButtonBar(true);

		return null;
	}

	/**
	 * 在readEntry.jsp頁面, 按下[確認申請]按鈕執行預算超支檢核
	 * 
	 * @return
	 */
	public String doCheckBudgetByTempDataAction() {
		// RE201001687 2010/11/11 需求變更,增加借貸平衡檢核 文珊
		OvsaTrvlLrnExp exp = this.getUpdatingDataValue();

		// 借方金額
		BigDecimal amtD = this.facade.getEntryGroupService().calcDebitTotal(exp.getExpapplC().getEntryGroup());

		// 貸方金額
		BigDecimal amtC = this.facade.getEntryGroupService().calcCreditTotal(exp.getExpapplC().getEntryGroup());

		BigDecimal remitAmt = amtD.subtract(amtC);
		if (BigDecimal.ZERO.compareTo(remitAmt) < 0) {
			// 顯示《借貸不平衡,請再確認》訊息
			throw new ExpRuntimeException(ErrorCode.C10085);
		}

		Map<String, Object> map = facade.getExpapplCService().checkBudget(exp.getExpapplC());
		if (null == map) {
			return doSaveCreateAction();
		}
		setShowConfirmButtonBar(true);

		return null;
	}

	public String doResetShowConfirmButtonBar() {
		setShowConfirmButtonBar(false);
		return null;
	}

	public String doBackAction() {

		return fromFunctionCode.getCode();
	}

	public boolean isCheckEntriesAmt() {
		if (null == getUpdatingDataValue()) {
			return true;
		}
		OvsaTrvlLrnExp exp = getUpdatingDataValue();
		if (null == exp.getExpapplC() || null == exp.getExpapplC().getEntryGroup() || CollectionUtils.isEmpty(exp.getExpapplC().getEntryGroup().getEntries())) {
			return true;
		}

		for (Entry entry : exp.getExpapplC().getEntryGroup().getEntries()) {
			if (null == entry.getAmt() || BigDecimal.ZERO.compareTo(entry.getAmt()) >= 0) {
				return true;
			}
		}

		return false;
	}

	public IncomeIdType getSelectIncomeIdType() {
		return selectIncomeIdType;
	}

	public void setSelectIncomeIdType(IncomeIdType selectIncomeIdType) {
		this.selectIncomeIdType = selectIncomeIdType;
	}

	/**
	 * RE201101034核銷鎖住欄位: 由核銷頁面引導至此時鎖住某些欄位 2012/04/13 cm9539
	 * 
	 * @return
	 */
	public boolean isReadOnlyWhenPay() {
		return readOnlyWhenPay;
	}

	/**
	 * RE201101034核銷鎖住欄位: 由核銷頁面引導至此時鎖住某些欄位 2012/04/13 cm9539
	 * 
	 * @param readOnlyWhenPay
	 */
	public void setReadOnlyWhenPay(boolean readOnlyWhenPay) {
		this.readOnlyWhenPay = readOnlyWhenPay;
	}

	// RE201601162_國外出差旅費 EC0416
	public String getInitCreate() {
		if (isCreate) {
			this.initCreatingData();
			isCreate = false;
		}
		return null;
	}

	public void setInitCreate(String str) {
	}

	public String getInitUpdate() {
		if (isUpdatePage) {
			this.doUpdateVo();
			isUpdatePage = false;
		}
		return null;
	}

	public void setInitUpdate(String str) {
	}

	public boolean isUpdateStationRendered() {
		return updateStationRendered;
	}

	public void setUpdateStationRendered(boolean updateStationRendered) {
		this.updateStationRendered = updateStationRendered;
	}

	public int getUpdateStationIndex() {
		return updateStationIndex;
	}

	public void setUpdateStationIndex(int updateStationIndex) {
		this.updateStationIndex = updateStationIndex;
	}

	public boolean isSelectBizMatterItemDisabled() {
		return selectBizMatterItemDisabled;
	}

	public void setSelectBizMatterItemDisabled(boolean selectBizMatterItemDisabled) {
		this.selectBizMatterItemDisabled = selectBizMatterItemDisabled;
	}

	/**
	 * 初始化日支費明細list
	 * 
	 * @return
	 */
	public List<StationExpDetail> intitStatinoDetailList() {
		List<StationExpDetail> stationExpDetailList = new ArrayList<StationExpDetail>();
		for (int i = 0; i <= 10; i++) {
			StationExpDetail stationExpDetail = new StationExpDetail();
			stationExpDetail.setOvsaTrvlLrnExpItem(facade.getOvsaTrvlLrnExpItemService().findByCode(OvsaTrvlLrnExpItemCode.TRAFFIC_EXP));
			stationExpDetail.setPaymentType(facade.getExpapplCService().getPaymentTypeByCode(PaymentTypeCode.C_SALF_PAY));
			stationExpDetailList.add(stationExpDetail);
		}
		return stationExpDetailList;
	}

	public String doBackUpadteAction() {
		String backPage = getReturnPage();
		// 清除所有資料
		this.initCreatingData();

		return backPage;
	}

	/**
	 * 取得導頁字串
	 */
	private String getReturnPage() {
		if (StringUtils.isNotBlank(outcomepage) && null != fromFunctionCode) {
			return outcomepage + fromFunctionCode.getCode();
		} else if (StringUtils.isBlank(outcomepage) && null != fromFunctionCode) {
			return fromFunctionCode.getCode();
		}
		return null;
	}

	public Entry initEntry() {
		Entry entry = new Entry();
		entry.setEntryType(facade.getEntryTypeService().findEntryTypeByEntryTypeCode(EntryTypeCode.TYPE_2_3));
		Map<String, Object> findCriteriaMap = new HashMap<String, Object>();
		findCriteriaMap.put("accTitleCode", null);// 科目代號
		findCriteriaMap.put("accTitleName", null);// 科目中文
		this.setFindCriteriaMap(findCriteriaMap);
		return entry;
	}

	public StationExpDetail initStationExpDetail() {
		StationExpDetail stationExpDetail = new StationExpDetail();
		stationExpDetail.setOvsaTrvlLrnExpItem(facade.getOvsaTrvlLrnExpItemService().findByCode(OvsaTrvlLrnExpItemCode.TRAFFIC_EXP));
		stationExpDetail.setPaymentType(facade.getExpapplCService().getPaymentTypeByCode(PaymentTypeCode.C_SALF_PAY));
		return stationExpDetail;
	}

	public List<ExchangeRate> getCurrency(Calendar day, String code) {

		Calendar nowDay = (Calendar) day.clone();

		nowDay.add(Calendar.DAY_OF_MONTH, -1);
		System.out.println(DateUtils.getSimpleISODateStr(nowDay.getTime()));

		List<ExchangeRate> exchangeRateList = facade.getExchangeRateService().findExchangeRateListByDateAndCurrencyOrderBy(nowDay, code);

		return exchangeRateList;
	}

	// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 start
	/**
	 * 顯示日用費公式
	 * 
	 * @return
	 */
	public String getDaySpendFormula() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStation()) {
			return "";
		}

		// 如果出國事由為國外高峰行程則日用費金額為零
		if (vo.getBo().getBizMatter() != null && vo.getBo().getBizMatter().getCode().equals(BizMatterCode.OVSA_SUMMIT_CONFERENCE.getCode())) {
			return "";
		} else {
			return facade.getStationService().getDaySpendFormula(getUpdatingDataValue(), vo.getStation());
		}
	}

	/**
	 * 顯示日支費公式
	 * 
	 * @return
	 */
	public String getDayExpFormula() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || null == vo.getStation()) {
			return "";
		}
		return facade.getStationService().getDayExpFormula(getUpdatingDataValue(), vo.getStation());
	}

	/**
	 * 顯示依規免稅額公式
	 * 
	 * @return
	 */
	public String getGaugeDutyFreeAmtFormula() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || vo.getBo() == null) {
			return "";
		}
		OvsaTrvlLrnExp exp = vo.getBo();
		String formula = "日支費合計(" + exp.getDailyExpTotalAmt().toString() + ")+交通費合計(" + exp.getTrafficTotalExp().toString() + ")+宿泊費合計(" + exp.getHotelTotalExp().toString() + ")+雜費合計(" + exp.getMiscellaneousTotalExp().toString() + ")";
		return formula;
	}

	/**
	 * 顯示併薪額公式
	 * 
	 * @return
	 */
	public String getSalaryAmtFormula() {
		OvsaTrvlLrnExpVo vo = (OvsaTrvlLrnExpVo) getUpdatingData();
		if (null == vo || vo.getBo() == null) {
			return "";
		}
		OvsaTrvlLrnExp exp = vo.getBo();
		String formula = "行程總計(" + exp.getAbrodTotalAmt().toString() + ")-依規免稅額(" + exp.getGaugeDutyFreeAmt().toString() + ")";
		if (exp.getSalaryAmt().compareTo(BigDecimal.ZERO) <= 0) {
			formula = formula + ";計算金額小於0，以0顯示";
		}
		return formula;
	}

	private CollectionModel ovsaDailyWorkDataModel;

	private CoreTable ovsaDailyWorkDataTable;

	/**
	 * 暫存國外出差日報紀錄
	 */
	public List<OvsaDailyWork> ovsaDailyWorkList = new ArrayList<OvsaDailyWork>();

	/**
	 * 頁面顯示是國外出差日報
	 */
	private OvsaDailyWork updatingOvsaDailyWork = new OvsaDailyWork();

	private boolean commandOvsaWork = false;

	/**
	 * 新增國外差旅工作日報表_初始資料
	 */
	private void initOvsaDailyWork(List<OvsaDailyWork> ovsaDailyWorkList) {
		if (CollectionUtils.isEmpty(ovsaDailyWorkList)) {
			setOvsaDailyWorkDtoList(new ArrayList<OvsaDailyWork>());
		} else {
			setOvsaDailyWorkDtoList(ovsaDailyWorkList);
		}
		setOvsaDailyWorkDataModel(null);
		updatingOvsaDailyWork = new OvsaDailyWork();
		updatingOvsaDailyWork.setStartTimeHour("00");
		updatingOvsaDailyWork.setStartTimeMinute("00");
		updatingOvsaDailyWork.setEndTimeHour("00");
		updatingOvsaDailyWork.setEndTimeMinute("00");
	}

	/**
	 * 用於修改國外出差日報表取得行程索引
	 */
	private int updateOvsaDailyWorkIndex;

	/**
	 * 在新增國外出差工作日報表按下[新增]
	 */
	public String doCreateOvsaDailyWorkAction() {
		checkOvsaDailyWork();
		ovsaDailyWorkList.add(getUpdatingOvsaDailyWork());
		setUpdatingOvsaDailyWork(new OvsaDailyWork());
		updatingOvsaDailyWork.setStartTimeHour("00");
		updatingOvsaDailyWork.setStartTimeMinute("00");
		updatingOvsaDailyWork.setEndTimeHour("00");
		updatingOvsaDailyWork.setEndTimeMinute("00");
		return null;
	}

	/**
	 * 在新增國外出差工作日報表按下[更新]
	 * 
	 * @return
	 */
	public String doUpdateOvsaDailyWorkAction() {
		checkOvsaDailyWork();

		BeanUtils.copyProperties(getUpdatingOvsaDailyWork(), ovsaDailyWorkList.get(getUpdateOvsaDailyWorkIndex()));
		setUpdatingOvsaDailyWork(new OvsaDailyWork());
		updatingOvsaDailyWork.setStartTimeHour("00");
		updatingOvsaDailyWork.setStartTimeMinute("00");
		updatingOvsaDailyWork.setEndTimeHour("00");
		updatingOvsaDailyWork.setEndTimeMinute("00");
		setCommandOvsaWork(false);
		return null;

	}

	/**
	 * 檢核出差報告表欄位
	 */
	public void checkOvsaDailyWork() {
		// 日期_月份是否為空值
		if (getUpdatingOvsaDailyWork().getAbroadDate() == null) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_ovsaDailyWork_days") });
		}

		// 說明否為空值
		if (StringUtils.isBlank(getUpdatingOvsaDailyWork().getWorkDetail())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_ovsaDailyWork_workDetail") });
		}

		// 判斷頁面填寫日期是否與申請日期相符
		if ((!(getUpdatingOvsaDailyWork().getAbroadDate().compareTo(ovsaStartDate) >= 0 && getUpdatingOvsaDailyWork().getAbroadDate().compareTo(ovsaEndDate) <= 0))) {
			throw new ExpRuntimeException(ErrorCode.C10593);
		}

		// 時間下拉式選單為必填
		if (StringUtils.isBlank(getUpdatingOvsaDailyWork().getStartTimeHour()) || StringUtils.isBlank(getUpdatingOvsaDailyWork().getEndTimeHour()) || StringUtils.isBlank(getUpdatingOvsaDailyWork().getStartTimeMinute()) || StringUtils.isBlank(getUpdatingOvsaDailyWork().getEndTimeMinute())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_ovsaDailyWork_times") });
		}

		// RE201702991_國外出差系統優化作業 EC0416 2017/11/13 start
		// 參訪機構是否為空值
		if (StringUtils.isBlank(getUpdatingOvsaDailyWork().getVisitAgency())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_ovsaDailyWork_visitAgency") });
		}

		// 參訪機構是否為空值
		if (StringUtils.isBlank(getUpdatingOvsaDailyWork().getVisitor())) {
			throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_ovsaDailyWork_visitor") });
		}
		// RE201702991_國外出差系統優化作業 EC0416 2017/11/13 end
	}

	/**
	 * 在新增國外出差工作日報表按下[重設]
	 */
	public String doResetOvsaDailyWorkAction() {
		setUpdatingOvsaDailyWork(new OvsaDailyWork());
		return "createOvsaDailyWork";
	}

	/**
	 * 在新增國外出差工作日報表按下[修改]
	 * 
	 */
	public String doUpdateTrvlAction() {
		this.setUpdateOvsaDailyWorkIndex(getOvsaDailyWorkDataTable().getRowIndex());
		OvsaDailyWork bo = new OvsaDailyWork();
		BeanUtils.copyProperties(((OvsaDailyWork) getOvsaDailyWorkDataTable().getRowData()), bo);
		setUpdatingOvsaDailyWork(bo);
		setCommandOvsaWork(true);
		return "createOvsaDailyWork";
	}

	/**
	 * 在新增國外出差工作日報表按下[刪除]
	 * 
	 * @return
	 */
	public String doDeleteOvsaDailyWork() {
		ovsaDailyWorkList.remove(getOvsaDailyWorkDataTable().getRowIndex());
		setCommandOvsaWork(false);
		return null;
	}

	/**
	 * 在新增國外出差工作日報表按下[儲存]
	 * 
	 * @return
	 */
	public String doSaveOvsaDailyWorkAction() {
		// 若資料為空，顯示錯誤訊息"出差工作日報表資料為空"不可儲存
		if (CollectionUtils.isEmpty(getOvsaDailyWorkList())) {
			throw new ExpRuntimeException(ErrorCode.C10655);
		}
		ExpapplC expapplC = facade.getExpapplCService().findByPK(ovsaDaiylExpapplCID);

		List<OvsaDailyWork> newOvsaDailyWorkList = new ArrayList<OvsaDailyWork>();
		for (OvsaDailyWork bo : getOvsaDailyWorkList()) {
			// 顯示錯誤訊息"申請單號錯誤"
			if (StringUtils.isBlank(ovsaDaiylExpapplCID)) {

			}
			OvsaDailyWork newbo = new OvsaDailyWork();
			newbo.setAbroadDate(bo.getAbroadDate());
			newbo.setStartTimeHour(bo.getStartTimeHour());
			newbo.setStartTimeMinute(bo.getStartTimeMinute());
			newbo.setEndTimeHour(bo.getEndTimeHour());
			newbo.setEndTimeMinute(bo.getEndTimeMinute());
			newbo.setWorkDetail(bo.getWorkDetail());
			newbo.setExpapplC(expapplC);
			newbo.setCreateDate(Calendar.getInstance());
			newbo.setCreateUser((User) AAUtils.getLoggedInUser());
			// RE201702991_國外出差系統優化作業 EC0416 2017/11/13 start
			newbo.setVisitAgency(bo.getVisitAgency());
			newbo.setVisitor(bo.getVisitor());
			// RE201702991_國外出差系統優化作業 EC0416 2017/11/13 end
			newOvsaDailyWorkList.add(newbo);
		}

		Map<String, Object> crit = new HashMap<String, Object>();
		crit.put("expapplC", expapplC);
		List<OvsaDailyWork> deleteOvsaDailyWork = facade.getOvsaDailyWorkService().findByCriteriaMap(crit);
		if (CollectionUtils.isNotEmpty(deleteOvsaDailyWork)) {
			for (OvsaDailyWork delBo : deleteOvsaDailyWork) {
				facade.getOvsaDailyWorkService().delete(delBo);
			}
		}

		facade.getOvsaDailyWorkService().createEntitys(newOvsaDailyWorkList);

		// 清空頁面
		initOvsaDailyWork(null);

		if (isDailyWorkPage) {
			return "create";
		} else {
			return fromFunctionCode.getCode();
		}
	}

	/**
	 * 在新增國外出差工作日報表按下[返回]
	 * 
	 * @return
	 */
	public String doCancelOvsaDailyWorkAction() {
		initOvsaDailyWork(null);

		if (isDailyWorkPage) {
			return "create";
		} else {
			return fromFunctionCode.getCode();
		}

	}

	/**
	 * 列印明細表 -按鈕
	 */
	public String doPrintDetailAction() {
		ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession mySession = (HttpSession) extCtx.getSession(true);
		Map<String, Object> params = new HashMap<String, Object>();
		List<OvsaDailyWork> dtoList = new ArrayList<OvsaDailyWork>();
		
		//1070111 START 
		
		List<OvsaDailyWork> sortList = getOvsaDailyWorkList();
        Collections.sort(sortList,new Comparator<OvsaDailyWork>(){
            public int compare(OvsaDailyWork arg0, OvsaDailyWork arg1) {
		    	 // 回傳值: -1 前者比後者小, 0 前者與後者相同, 1 前者比後者大        	    	        	      
		        if(arg0.getAbroadDate().compareTo(arg1.getAbroadDate())==1){
		        	return 1;
		        }     		            
		        else if(arg0.getAbroadDate().compareTo(arg1.getAbroadDate())==-1){ 
		            return -1;
		        }
		        else{
		        	return arg0.getStartTimeHour().compareTo(arg1.getStartTimeHour());
		        }
            }
        });
        
		// 將日期轉換為字串
		for (OvsaDailyWork dto : sortList) {
			//1070111 end
			String dayString = "";
			dayString = StringUtils.substring(StringUtils.leftPad(DateUtils.getROCDateStr(dto.getAbroadDate().getTime(), "", true), 7, "0"), 0, 7);
			dto.setAbroadDateString(dayString);
			dtoList.add(dto);
		}
		// Crystal Report 檔案。
		CrystalReportConfigManagedBean crystalReportConfigManagedBean = (CrystalReportConfigManagedBean) FacesUtils.getManagedBean("crystalReportConfigManagedBean");
		String rptName = crystalReportConfigManagedBean.getOvsaDailyWorkReportName();
		params.put("className", "tw.com.skl.exp.kernel.model6.bo.OvsaDailyWork");
		params.put("tableAlias", "OvsaDailyWork");
		params.put("rptName", rptName);
		params.put("dataSet", dtoList);
		mySession.setAttribute("params", params);
		return "dialog:printPassPojo";
	}

	public List<OvsaDailyWork> getOvsaDailyWorkList() {
		return ovsaDailyWorkList;
	}

	public void setOvsaDailyWorkDtoList(List<OvsaDailyWork> ovsaDailyWorkDtoList) {
		this.ovsaDailyWorkList = ovsaDailyWorkDtoList;
	}

	public CollectionModel getOvsaDailyWorkDataModel() {
		if (ovsaDailyWorkDataModel == null) {
			ovsaDailyWorkDataModel = new SortableModel();
			ovsaDailyWorkDataModel.setWrappedData(getOvsaDailyWorkList());
		}
		return ovsaDailyWorkDataModel;
	}

	public void setOvsaDailyWorkDataModel(CollectionModel ovsaDailyWorkDataModel) {
		this.ovsaDailyWorkDataModel = ovsaDailyWorkDataModel;
	}

	/**
	 * 檢核訊息
	 */
	public String getWarningMsg() {
		return "此份報表不會儲存，請自行留存!";
	}

	/**
	 * 檢核訊息
	 */
	public String getWarningMsg2() {
		return "申請人請務必另存檔案，以利後續更新";
	}

	public CoreTable getOvsaDailyWorkDataTable() {
		return ovsaDailyWorkDataTable;
	}

	public void setOvsaDailyWorkDataTable(CoreTable ovsaDailyWorkDataTable) {
		this.ovsaDailyWorkDataTable = ovsaDailyWorkDataTable;
	}

	public boolean isDisabled() {
		return false;
	}

	public int getUpdateOvsaDailyWorkIndex() {
		return updateOvsaDailyWorkIndex;
	}

	public void setUpdateOvsaDailyWorkIndex(int updateOvsaDailyWorkIndex) {
		this.updateOvsaDailyWorkIndex = updateOvsaDailyWorkIndex;
	}

	public OvsaDailyWork getUpdatingOvsaDailyWork() {
		return updatingOvsaDailyWork;
	}

	public void setUpdatingOvsaDailyWork(OvsaDailyWork updatingOvsaDailyWorkDto) {
		this.updatingOvsaDailyWork = updatingOvsaDailyWorkDto;
	}

	public boolean isCommandOvsaWork() {
		return commandOvsaWork;
	}

	public void setCommandOvsaWork(boolean commandOvsaWork) {
		this.commandOvsaWork = commandOvsaWork;
	}

	public String getOvsaDaiylExpapplCID() {
		return ovsaDaiylExpapplCID;
	}

	public void setOvsaDaiylExpapplCID(String ovsaDaiylExpapplCID) {
		this.ovsaDaiylExpapplCID = ovsaDaiylExpapplCID;
	}

	// RE201701039 國外出差系統優化作業第二階段 EC0416 201706016 end

	// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 start
	// 若設為False在修改頁面更動日支費相關欄位時，需按下[計算]才可以儲存
	public boolean isSaveCount() {
		return saveCount;
	}

	public void setSaveCount(boolean saveCount) {
		this.saveCount = saveCount;
	}
	// defect4747_差旅費申報表-依規免稅額欄位問題 ec0416 20171030 end
}