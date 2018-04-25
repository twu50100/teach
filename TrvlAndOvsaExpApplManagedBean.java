package tw.com.skl.exp.web.jsf.managed.gae.expapply.expsendverify;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.SortableModel;
import org.springframework.util.CollectionUtils;

import tw.com.skl.common.model6.web.jsf.managedbean.impl.TemplateDataTableManagedBean;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.ExpapplC;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.Group;
import tw.com.skl.exp.kernel.model6.bo.Group.GroupCode;
import tw.com.skl.exp.kernel.model6.bo.IntrTrvlBizExp;
import tw.com.skl.exp.kernel.model6.bo.IntrTrvlLrnExp;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.SystemParam;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.bo.OvsaTrvlLrnExp;
import tw.com.skl.exp.kernel.model6.bo.TrvlExpType;
import tw.com.skl.exp.kernel.model6.bo.SystemParam.SystemParamName;
import tw.com.skl.exp.kernel.model6.bo.TrvlExpType.TrvlExpTypeCode;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpException;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.CoreTableUtils;
import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dto.TrvlAndOvsaExpApplDto;
import tw.com.skl.exp.kernel.model6.facade.ExpapplCFacade;
import tw.com.skl.exp.kernel.model6.logic.ExpapplCService;
import tw.com.skl.exp.kernel.model6.logic.enumeration.ApplStateEnum;
import tw.com.skl.exp.web.jsf.managed.FunctionCodeAware;
import tw.com.skl.exp.web.util.JsfUtils;
import tw.com.skl.exp.web.util.SelectFactory;
import tw.com.skl.exp.web.vo.ExpapplCVo;
import tw.com.skl.exp.web.vowrapper.ExpapplCWrapper;

/**
 * C1.6.4研修差旅費用申請記錄表(含國外) ManagedBean
 * 
 * @author Eustace
 */
public class TrvlAndOvsaExpApplManagedBean extends TemplateDataTableManagedBean<ExpapplC, ExpapplCService> implements FunctionCodeAware {

	private static final long serialVersionUID = 695953204497290997L;

	private ExpapplCFacade facade;

	private List<TrvlAndOvsaExpApplDto> dtoDataList;

	private CollectionModel dataModel;
	private CoreTable dataTable;

	public TrvlAndOvsaExpApplManagedBean() {

		super.setInitShowListData(true);

		// 設定VoWrapper
		this.setVoWrapper(new ExpapplCWrapper());

		this.initFindCriteriaMap();

	}

	/**
	 * 人資部經辦(017)使用的中分類
	 */
	public static MiddleTypeCode[] codesA = { MiddleTypeCode.CODE_H10, MiddleTypeCode.CODE_K30 };

	/**
	 * 其他群組使用的中分類
	 */
	public static MiddleTypeCode[] codesB = { MiddleTypeCode.CODE_K10, MiddleTypeCode.CODE_H20, MiddleTypeCode.CODE_K20 };

	// RE201500189_國內出差旅費 EC0416 2015/04/02 START
	/**
	 * 行政費用群組群組使用的中分類
	 */
	public static MiddleTypeCode[] codesC = { MiddleTypeCode.CODE_H10, MiddleTypeCode.CODE_K10, MiddleTypeCode.CODE_H20, MiddleTypeCode.CODE_K20 };
	// RE201500189_國內出差旅費 EC0416 2015/04/02 END

	// /**
	// * 研修部、教育中心經辦(015)使用的中分類
	// */
	// public static MiddleTypeCode[] codesC = {
	// MiddleTypeCode.CODE_K20
	// };

	/**
	 * 管理人員(099)使用的中分類
	 */
	public static MiddleTypeCode[] codesAdmin = { MiddleTypeCode.CODE_H10, MiddleTypeCode.CODE_H20, MiddleTypeCode.CODE_K10, MiddleTypeCode.CODE_K20, MiddleTypeCode.CODE_K30 };

	@Override
	protected void initFindCriteriaMap() {
		// 初始化要查詢輸入的條件
		Map<String, Object> findCriteriaMap = new HashMap<String, Object>();
		findCriteriaMap.put("applState", ApplStateEnum.NOT_VERIFICATION_SEND);// 申請單狀態
		findCriteriaMap.put("middleType", null);// 費用中分類
		findCriteriaMap.put("applyUserCode", null);// 申請人員工代號
		// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 start
		Calendar endDate = Calendar.getInstance();
		Calendar startDate = DateUtils.add(endDate, Calendar.MONTH, -1);

		findCriteriaMap.put("createDateStart", startDate);// 建檔期間 起
		findCriteriaMap.put("createDateEnd", endDate);// 建檔期間迄
		// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 end

		// RE201504572_優化研修差旅 CU3178 2015/12/18 START
		findCriteriaMap.put("paperNo", null);// 建檔期間 起
		findCriteriaMap.put("classCode", null);// 建檔期間迄
		// RE201504572_優化研修差旅 CU3178 2015/12/18 END
		this.setFindCriteriaMap(findCriteriaMap);

	}

	@Override
	protected void initCreatingData() {
	}

	@Override
	protected void initUpdatingData(ValueObject<ExpapplC> updatingData) {
	}

	@Override
	protected void setupUpdatingData() {
	}

	/**
	 * 申請單狀態下拉選單
	 * <p>
	 * 項目有:
	 * </p>
	 * <ol>
	 * <li>已送審</li>
	 * <li>未送審</li>
	 * </ol>
	 * 
	 */
	public List<SelectItem> getApplStateList() {
		List<SelectItem> selectList = new ArrayList<SelectItem>();
		// 未送審
		selectList.add(new SelectItem(ApplStateEnum.NOT_VERIFICATION_SEND, MessageUtils.getAccessor().getMessage("applyStat")));

		// 審核中
		selectList.add(new SelectItem(ApplStateEnum.VERIFICATION_SEND, MessageUtils.getAccessor().getMessage("verifiedStat")));

		return selectList;
	}

	public List<SelectItem> getMiddleTypeList() {

		Group group = facade.getUserService().findGroupByCode(((User) AAUtils.getLoggedInUser()).getCode());
		if (null == group) {
			return null;
		}

		// defect4951_增加群組權限條件 CU3178 2018/1/25 START
		// // 登入人員群組是否為[管理人員(099)]
		// if (isPowerGroup()) {
		// return SelectFactory.createMiddleTypeByParamsSelect(
		// new Object[] { codesAdmin }).getSelectList();
		// }// 登入人員群組是否為[人資部經辦(017)]
		// else if
		// (GroupCode.HUMAN_RESOURCE.equals(GroupCode.getByValue(group))) {
		// return SelectFactory.createMiddleTypeByParamsSelect(
		// new Object[] { codesA }).getSelectList();
		// //RE201500189_國內出差旅費 CU3178 2015/04/02 START
		// }else if (GroupCode.GAE_GENERAL.equals(GroupCode.getByValue(group)))
		// {
		// return SelectFactory.createMiddleTypeByParamsSelect(
		// new Object[] { codesC }).getSelectList();
		// //RE201500189_國內出差旅費 CU3178 2015/04/02 END
		// } else {
		// return SelectFactory.createMiddleTypeByParamsSelect(
		// new Object[] { codesB }).getSelectList();
		// }
		return SelectFactory.createMiddleTypeByParamsSelect(new Object[] { codesAdmin }).getSelectList();
		// defect4951_增加群組權限條件 CU3178 2018/1/25 END

		// //登入人員群組是否為[研修部、教育中心經辦(015)]
		// if (GroupCode.LEARNING.equals(GroupCode.getByValue(group))) {
		// return SelectFactory.createMiddleTypeByParamsSelect(new
		// Object[]{codesC}).getSelectList();
		// }
		//
		// //登入人員群組是否為[內務(006), 行政費用總帳經辦(011)]
		// if (GroupCode.INTERIOR.equals(GroupCode.getByValue(group)) ||
		// GroupCode.GAE_GENERAL.equals(GroupCode.getByValue(group)) ) {
		// return SelectFactory.createMiddleTypeByParamsSelect(new
		// Object[]{codesB}).getSelectList();
		// }

		// return null;
	}

	/**
	 * 判斷登入人員群組是否為以下群組
	 * <ul>
	 * <li>審計課初審經辦(001)</li>
	 * <li>審計課覆核經辦(002)</li>
	 * <li>審計課稅務經辦(003)</li>
	 * <li>審計課總帳經辦(004)</li>
	 * <li>人資部經辦(017)</li>
	 * <li>管理人員(099)</li>
	 * </ul>
	 * 
	 * @return
	 */
	private boolean isPowerGroup() {
		Group group = facade.getUserService().findGroupByCode(((User) AAUtils.getLoggedInUser()).getCode());
		if (null == group) {
			return false;
		}
		// defect4951_增加群組權限條件 CU3178 2018/1/25 START
		if (GroupCode.ADMIN.equals(GroupCode.getByValue(group)) || GroupCode.AUDITOR_FIRST_VERIFY.equals(GroupCode.getByValue(group)) || GroupCode.AUDITOR_REVIEW.equals(GroupCode.getByValue(group)) || GroupCode.AUDITOR_TAX.equals(GroupCode.getByValue(group)) || GroupCode.AUDITOR_GENERAL.equals(GroupCode.getByValue(group)) || GroupCode.HUMAN_RESOURCE.equals(GroupCode.getByValue(group)) || GroupCode.AUDITOR_PM_REVIEW.equals(GroupCode.getByValue(group))) {
			return true;
		}
		// defect4951_增加群組權限條件 CU3178 2018/1/25 END
		return false;
	}

	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 START
	/**
	 * 導頁至國內出差旅費or國外
	 */
	public String doMaintainAction() {
		this.setUpdatingData(new ExpapplCVo());
		((ExpapplCVo) getUpdatingData()).setUpdateAction(true);
		String expApplNo = null;
		TrvlAndOvsaExpApplDto dto = (TrvlAndOvsaExpApplDto) getDataTable().getRowData();
		this.getUpdatingData().setBo(dto.getExpapplC());
		expApplNo = dto.getExpapplC().getExpApplNo();
		setDtoDataList(null);
		setDataModel(null);
		// 清除Session內的ManagedBean
		doRemoveManagedBean();
		/*
		 * "申請單號前3碼"為H10、H20國內出差時，修改畫面與UC1.5.12國內出差旅費相同
		 * "申請單號前3碼"為K10國內研修差旅(人資)時，修改畫面與UC1.5.4輸入國內研修差旅費用核銷申請資料相同
		 * "申請單號前3碼"為K20國內研修差旅(研修)時，修改畫面與UC1.5.5輸入國內研修差旅費用核銷申請資料相同
		 * "申請單號前3碼"為K30國外研修差旅時，修改畫面與UC1.5.6輸入國外研修差旅費用核銷申請資料相同。
		 */
		if (StringUtils.isBlank(expApplNo)) {
			return null;
		} else if (StringUtils.equals(expApplNo.substring(0, 3), MiddleTypeCode.CODE_H10.getCode()) || StringUtils.equals(expApplNo.substring(0, 3), MiddleTypeCode.CODE_H20.getCode())) {
			facade.getUserService().checkFunctionByCode(FunctionCode.C_1_5_12);
			// 導頁至1.5.12
			return FunctionCode.C_1_5_12.getCode();
		} else if (StringUtils.equals(expApplNo.substring(0, 3), MiddleTypeCode.CODE_K10.getCode())) {
			facade.getUserService().checkFunctionByCode(FunctionCode.C_1_5_4);
			// 導頁至1.5.4
			return FunctionCode.C_1_5_4.getCode();
		} else if (StringUtils.equals(expApplNo.substring(0, 3), MiddleTypeCode.CODE_K20.getCode())) {
			facade.getUserService().checkFunctionByCode(FunctionCode.C_1_5_5);
			// 導頁至1.5.5
			return FunctionCode.C_1_5_5.getCode();
		} else if (StringUtils.equals(expApplNo.substring(0, 3), MiddleTypeCode.CODE_K30.getCode())) {
			facade.getUserService().checkFunctionByCode(FunctionCode.C_1_5_6);
			// 導頁至1.5.6
			return FunctionCode.C_1_5_6.getCode();
		}
		return null;
	}

	/**
	 * 清除Session內的ManagedBean
	 */
	private void doRemoveManagedBean() {
		// 要清除的ManagedBean
		FunctionCode[] functionCode = { FunctionCode.C_1_5_5, FunctionCode.C_1_5_4, FunctionCode.C_1_5_6, FunctionCode.C_1_5_12 };
		FacesContext context = FacesContext.getCurrentInstance();
		for (FunctionCode functionCode2 : functionCode) {
			// Session內的ManagedBean
			context.getExternalContext().getSessionMap().remove(functionCode2.getBeanId());
		}
	}

	// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 END

	/**
	 * 刪除費用申請單
	 * <ol>
	 * <li>【檢核】
	 * <ul>
	 * <li> 檢核欲刪除之申請單狀態是否為"申請中"，若否，則顯示《此申請單狀態不提供刪除》訊息。</li>
	 * </ul>
	 * </li>
	 * <li>
	 * 操作人員於欲刪除之資料列勾選按"刪除"欄位之核取方框後，按[刪除]按鈕，系統顯示《欲刪除"選擇筆數"筆資料》對話方框，操作人員按[確定]後，
	 * 系統將勾選之申請單刪除系統將所選取之資料刪除；或按[取消]後，系統不做任何處理動作。</li>
	 * <li>
	 * </li>
	 * </ol>
	 * 
	 * @throws ExpException
	 */
	public String doDeleteExpApplCAction() throws ExpException {
		List<ExpapplC> list = getData();
		if (CollectionUtils.isEmpty(list)) {
			throw new ExpRuntimeException(ErrorCode.A10024);
		}
		for (ExpapplC expapplC : list) {
			if (!ApplStateCode.APPLIED.getCode().equals(expapplC.getApplState().getCode())) {
				throw new ExpRuntimeException(ErrorCode.C10200);
			}
		}
		this.facade.getGeneralExpService().doDeleteGeneralExp(list, this.getFunctionCode());

		return this.doFindAction();
	}

	/**
	 * 取出頁面溝選的申請單
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<ExpapplC> getData() {
		if (null == getDataTable()) {
			return null;
		}

		List<TrvlAndOvsaExpApplDto> dtoList = CoreTableUtils.getSelectedRow(getDataTable());

		if (CollectionUtils.isEmpty(dtoList)) {
			return null;
		}

		List<ExpapplC> expapplCList = new ArrayList<ExpapplC>();

		for (TrvlAndOvsaExpApplDto dto : dtoList) {
			if (null == dto.getExpapplC()) {
				// 資料異常,請重新查詢
				throw new ExpRuntimeException(ErrorCode.C10526);
			}
			expapplCList.add(dto.getExpapplC());
		}

		return expapplCList;
	}

	/**
	 * 送出申請
	 */
	public String doApplyAction() {
		List<ExpapplC> list = getData();
		if (CollectionUtils.isEmpty(list)) {
			throw new ExpRuntimeException(ErrorCode.A10024);
		}
		List<String> applNoList = new ArrayList<String>();
		/*
		 * 勾選"送出申請"之申請單狀態須皆為"申請中"， 若有狀態為非"申請中"之申請單，不可送出申請， 顯示《選取送出申請之申請單狀態錯誤》訊息。
		 */
		for (ExpapplC expapplC : list) {
			if (!ApplStateCode.APPLIED.equals(ApplStateCode.getByValue(expapplC.getApplState()))) {
				throw new ExpRuntimeException(ErrorCode.C10201);
			}
			applNoList.add(expapplC.getExpApplNo());
		}

		facade.getDeliverDaylistService().doGenerateDeliverDayList(applNoList, this.getFunctionCode());

		return this.doFindAction();
	}

	/**
	 * @return the facade
	 */
	public ExpapplCFacade getFacade() {
		return facade;
	}

	/**
	 * @param facade
	 *            the facade to set
	 */
	public void setFacade(ExpapplCFacade facade) {
		this.facade = facade;
	}

	public FunctionCode getFunctionCode() {
		return FunctionCode.C_1_6_4;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.common.model6.web.jsf.managedbean.impl.
	 * TemplateDataTableManagedBean#doFindAction()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String doFindAction() {
		// // RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 start
		// this.doRefreshData();
		// // RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 end
		Map map = getFindCriteriaMap();
		// 費用中分類
		if (null == map.get("middleType")) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_MiddleType") });
		}

		// 申請單狀態
		if (null == map.get("applState")) {
			throw new ExpRuntimeException(ErrorCode.A10007, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_kernel_model6_bo_ExpapplC_applState") });
		}
		// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 start
		if (null == map.get("createDateStart") || null == map.get("createDateEnd")) {
			throw new ExpRuntimeException(ErrorCode.A10047, new String[] { "建檔期間起迄日" });
		}

		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
		User loginUser = (User) AAUtils.getLoggedInUser();
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());
		// 特定群組外的群組員工代號為必填
		if (!(GroupCode.ADMIN.equals(groupCode) || GroupCode.AUDITOR_FIRST_VERIFY.equals(groupCode) || GroupCode.AUDITOR_REVIEW.equals(groupCode) || GroupCode.AUDITOR_TAX.equals(groupCode) || GroupCode.AUDITOR_GENERAL.equals(groupCode) || GroupCode.GAE_GENERAL.equals(groupCode) || GroupCode.HUMAN_RESOURCE.equals(groupCode) || GroupCode.LEARNING.equals(groupCode) || GroupCode.AUDITOR_PM_REVIEW.equals(groupCode))) {
			if (null == map.get("applyUserCode")) {
				throw new ExpRuntimeException(ErrorCode.A10001, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_expapply_expsendverify_TrvlExpApplSearchManagedBean_applyUserCode") });
			}
		}

		// 員工代號是否存在
		if (null != map.get("applyUserCode")) {
			User applyUser = this.getFacade().getUserService().findByCode((String) map.get("applyUserCode"));
			if (applyUser == null) {
				throw new ExpRuntimeException(ErrorCode.C10100);
			}
		}
		// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end

		this.doRefreshData();
		// RE201400552_新增國外出差匯款對象與匯款日期 modify by michael in 2014/03/14 end

		List list = findDtoDate();
		if (CollectionUtils.isEmpty(list)) {
			MessageManager.getInstance().showInfoCodeMessage(ErrorCode.C10028.toString());
		} else {
			// set 查詢資料
			setDtoDataList(list);
		}

		return null;
	}

	/**
	 * 查詢method,會將查詢條件交給Service Layer的findByCriteriaMap()進行查詢
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<TrvlAndOvsaExpApplDto> findDtoDate() {
		List<TrvlAndOvsaExpApplDto> list = new ArrayList<TrvlAndOvsaExpApplDto>();
		if (this.getFindCriteriaMap().size() == 0) {
			throw new ExpRuntimeException(ErrorCode.A10007);
		} else {
			Map map = getFindCriteriaMap();

			ApplStateEnum applStateEnum = (ApplStateEnum) map.get("applState");// 申請單狀態
			MiddleType middleType = (MiddleType) map.get("middleType");// 研修差旅類別
			String applyUserCode = (String) map.get("applyUserCode");// 申請人員工代號
			Calendar createDateStart = (Calendar) map.get("createDateStart");// 建檔期間
																				// 起
			Calendar createDateEnd = (Calendar) map.get("createDateEnd");// 建檔期間迄

			MiddleTypeCode middleTypeCode = MiddleTypeCode.getByValue(middleType);
			// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 START
			// RE201504572_優化研修差旅 CU3178 2015/12/18 START
			String paperNo = (String) map.get("paperNo");// 申請人員工代號
			String classCode = (String) map.get("classCode");// 申請人員工代號
			List<String> expApplNoList = new ArrayList<String>();
			if (MiddleTypeCode.CODE_K20.equals(middleTypeCode)) {
				// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 start
				expApplNoList = getService().findByParamsLrn(applStateEnum, middleTypeCode, applyUserCode, createDateStart, createDateEnd, !isPowerGroup(), true, paperNo, classCode);
			} else if (MiddleTypeCode.CODE_K10.equals(middleTypeCode)) {
				expApplNoList = getService().findByParamsHRLrn(applStateEnum, middleTypeCode, applyUserCode, createDateStart, createDateEnd, !isPowerGroup(), true, paperNo, classCode);
			} else {
				expApplNoList = getService().findByParams2(applStateEnum, middleTypeCode, applyUserCode, createDateStart, createDateEnd, !isPowerGroup(), true);
				// DEFECT5059_國外差旅記錄無限制查詢權限問題 ,應以建單人為依據區分查詢權限 2018/4/24 end
			}
			// RE201504572_優化研修差旅 CU3178 2015/12/18 END
			// RE201602265_將舊有功能1.5.5移至1.5.4 CU3178 2016/7/7 END

			if (CollectionUtils.isEmpty(expApplNoList)) {
				return list;
			}

			// 國內出差旅費H10, H20
			if (MiddleTypeCode.CODE_H10.equals(middleTypeCode) || MiddleTypeCode.CODE_H20.equals(middleTypeCode)) {
				List<IntrTrvlBizExp> dataList = this.facade.getIntrTrvlBizExpService().findByExpApplNoFetchRelation(expApplNoList);
				if (CollectionUtils.isEmpty(dataList)) {
					return null;
				}
				TrvlExpType trvlExpType = facade.getTrvlExpTypeService().findByTrvlExpTypeCode(TrvlExpTypeCode.A);
				for (IntrTrvlBizExp exp : dataList) {
					list.add(new TrvlAndOvsaExpApplDto(exp, trvlExpType, getService().caculateShouldRemitAmount(exp.getExpapplC())));
				}
			}

			// 國內研修差旅費用 K10, K20
			if (MiddleTypeCode.CODE_K10.equals(middleTypeCode) || MiddleTypeCode.CODE_K20.equals(middleTypeCode)) {
				List<IntrTrvlLrnExp> dataList = this.facade.getIntrTrvlLrnExpService().findByExpApplNoFetchRelation(expApplNoList);
				if (CollectionUtils.isEmpty(dataList)) {
					return null;
				}
				TrvlExpType trvlExpType = facade.getTrvlExpTypeService().findByTrvlExpTypeCode(TrvlExpTypeCode.B);
				for (IntrTrvlLrnExp exp : dataList) {
					list.add(new TrvlAndOvsaExpApplDto(exp, trvlExpType, getService().caculateShouldRemitAmount(exp.getExpapplC())));
				}
			}

			// 國外研修差旅費用 K30
			if (MiddleTypeCode.CODE_K30.equals(middleTypeCode)) {
				List<OvsaTrvlLrnExp> dataList = this.facade.getOvsaTrvlLrnExpService().findByExpApplNo(expApplNoList);
				if (CollectionUtils.isEmpty(dataList)) {
					return null;
				}
				TrvlExpType trvlExpType = facade.getTrvlExpTypeService().findByTrvlExpTypeCode(TrvlExpTypeCode.B);
				for (OvsaTrvlLrnExp exp : dataList) {
					list.add(new TrvlAndOvsaExpApplDto(exp, trvlExpType, getService().caculateShouldRemitAmount(exp.getExpapplC())));
				}
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public boolean isDisableButton() {
		Map map = getFindCriteriaMap();
		ApplStateEnum applStateEnum = (ApplStateEnum) map.get("applState");
		if (null == applStateEnum || ApplStateEnum.VERIFICATION_SEND.equals(applStateEnum)) {
			return true;
		} else if (ApplStateEnum.NOT_VERIFICATION_SEND.equals(applStateEnum)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the dtoDataList
	 */
	public List<TrvlAndOvsaExpApplDto> getDtoDataList() {
		if (null == dtoDataList) {
			dtoDataList = findDtoDate();
			if (CollectionUtils.isEmpty(dtoDataList)) {
				dtoDataList = new ArrayList<TrvlAndOvsaExpApplDto>();
			}
		}
		return dtoDataList;
	}

	/**
	 * @param dtoDataList
	 *            the dtoDataList to set
	 */
	public void setDtoDataList(List<TrvlAndOvsaExpApplDto> dtoDataList) {
		this.dtoDataList = dtoDataList;
	}

	/**
	 * @return the dataModel
	 */
	public CollectionModel getDataModel() {
		if (this.dataModel == null) {
			this.dataModel = new SortableModel();
			dataModel.setWrappedData(this.getDtoDataList());
		}
		return this.dataModel;
	}

	/**
	 * @param dataModel
	 *            the dataModel to set
	 */
	public void setDataModel(CollectionModel dataModel) {
		this.dataModel = dataModel;
	}

	/**
	 * @return the dataTable
	 */
	public CoreTable getDataTable() {
		return dataTable;
	}

	/**
	 * @param dataTable
	 *            the dataTable to set
	 */
	public void setDataTable(CoreTable dataTable) {
		this.dataTable = dataTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tw.com.skl.common.model6.web.jsf.managedbean.impl.
	 * TemplateDataTableManagedBean#doRefreshData()
	 */
	@Override
	public void doRefreshData() {
		this.setDtoDataList(null);
		super.doRefreshData();
	}

	// RE201500189_國內出差旅費 EC0416 2015/04/02 START
	/**
	 * 退件
	 */
	public String doReturnAction() {
		List<ExpapplC> list = getData();
		if (CollectionUtils.isEmpty(list)) {
			throw new ExpRuntimeException(ErrorCode.A10024);
		}
		List<String> applNoList = new ArrayList<String>();
		/*
		 * 勾選"送出申請"之申請單狀態須皆為"申請中"， 若有狀態為非"申請中"之申請單，不可送出申請， 顯示《選取送出申請之申請單狀態錯誤》訊息。
		 */
		for (ExpapplC expapplC : list) {
			if (!ApplStateCode.APPLIED.equals(ApplStateCode.getByValue(expapplC.getApplState()))) {
				throw new ExpRuntimeException(ErrorCode.C10201);
			}
			applNoList.add(expapplC.getExpApplNo());
		}

		this.getService().updateExpapplCState(applNoList, this.getFunctionCode(), ApplStateCode.TEMP);

		return this.doFindAction();
	}

	/**
	 * 研修差旅費用(國外) 在選擇中分類為H10時才能啟用
	 * 
	 * @return the showTraingingType
	 */
	public boolean getShowTraingingType() {
		MiddleType MiddleTypecode = (MiddleType) this.getFindCriteriaMap().get("middleType");
		if (null == MiddleTypecode || !MiddleTypecode.getCode().equals("H10")) {
			return true;
		} else {
			return false;
		}
	}

	// RE201500189_國內出差旅費 EC0416 2015/04/02 END

	// RE201504572_優化研修差旅 CU3178 2015/12/18 START
	public String getStartEndDateName() {
		MiddleType MiddleTypecode = (MiddleType) this.getFindCriteriaMap().get("middleType");
		// 中分類為研修差旅 顯示"開班起日"字串
		if (MiddleTypecode != null && (MiddleTypecode.getCode().equals("K10") || MiddleTypecode.getCode().equals("K20"))) {
			return MessageUtils.getAccessor().getMessage("class_available_start_end_date");
		} else {
			return MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_expapply_expsendverify_TrvlAndOvsaExpApplManagedBean_createDate");
		}
	}

	public String getShow() {
		MiddleType MiddleTypecode = (MiddleType) this.getFindCriteriaMap().get("middleType");
		// 中分類為研修差旅 顯示"開班起日"字串
		if (MiddleTypecode != null && (MiddleTypecode.getCode().equals("K10") || MiddleTypecode.getCode().equals("K20"))) {
			return JsfUtils.showTag;
		} else {
			return JsfUtils.hiddenTag;
		}
	}
	// RE201504572_優化研修差旅 CU3178 2015/12/18 END

}