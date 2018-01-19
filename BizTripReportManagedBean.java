package tw.com.skl.exp.web.jsf.managed.gae.query.querytravellearnexp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.kernel.model6.bo.Group.GroupCode;
import tw.com.skl.exp.kernel.model6.bo.IntrTrvlBizExp;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.facade.IntrTrvlBizExpFacade;
import tw.com.skl.exp.kernel.model6.logic.IntrTrvlBizExpService;
import tw.com.skl.exp.web.config.CrystalReportConfigManagedBean;
import tw.com.skl.exp.web.jsf.managed.ExpTemplateDataTableManagedBean;
import tw.com.skl.exp.web.jsf.managed.FunctionCodeAware;

/**
 * UC11.2.3國內出差(研修)旅費申請總表 ManagedBean。
 */
// RE201302964_aplog CU3178 in 2014/02/13 start
public class BizTripReportManagedBean extends ExpTemplateDataTableManagedBean<IntrTrvlBizExp, IntrTrvlBizExpService> implements FunctionCodeAware {
	// RE201302964_aplog CU3178 in 2014/02/13 end

	private static final long serialVersionUID = 1346885764371150652L;
	private IntrTrvlBizExpFacade facade;

	public BizTripReportManagedBean() {
		super.setInitShowListData(false);
		// 設定VoWrapper
		// this.setVoWrapper(new PaymentBatchWrapper());
		this.initFindCriteriaMap();

	}

	/**
	 * 依傳入的部室取得部級的單位
	 * 
	 * @param department
	 * @return
	 */
	private Department getDepartmentLevel(Department department) {
		if (department.getDepartmentLevelProperty().getCode().compareTo("1") != 0) {
			department = getDepartmentLevel(department.getParentDepartment());
		}
		return department;
	}

	@Override
	protected void initFindCriteriaMap() {

		Map<String, Object> findCriteriaMap = new HashMap<String, Object>();

		User loginUser = (User) AAUtils.getLoggedInUser();
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());

		findCriteriaMap.put("duringDateStart", null);// 起訖期間 Start
		findCriteriaMap.put("duringDateEnd", null);// 起訖期間 End
		findCriteriaMap.put("bizMatter", null);// 出差事由
		// 群組為一般使用者，員工代號為登入人員的代號
		if (GroupCode.GENERAL.equals(groupCode)) {
			findCriteriaMap.put("userId", loginUser.getCode());// 申請人員工代號
		} else {
			findCriteriaMap.put("userId", null);// 申請人員工代號
		}
		// 群組為預算編制員時，為登入人員的部級單位代號
		if (GroupCode.BUDGET.equals(groupCode)) {
			findCriteriaMap.put("costUnitCode", getDepartmentLevel(loginUser.getDepartment()).getCode());// 成本單位代號
		} else {
			findCriteriaMap.put("costUnitCode", null);// 成本單位代號
		}

		this.setFindCriteriaMap(findCriteriaMap);
	}

	@Override
	protected void initCreatingData() {
	}

	@Override
	protected void setupUpdatingData() {
	}

	@Override
	protected void initUpdatingData(ValueObject<IntrTrvlBizExp> updatingData) {

	}

	public FunctionCode getFunctionCode() {
		return FunctionCode.C_11_2_3;
	}

	public String doPrintAction() {

		Map<String, Object> params = new HashMap<String, Object>();
		// RE201800088_國內/國外出差費用核銷費用明細總表新增資料欄位 EC0416 2018/1/12 start
		Map map = getFindCriteriaMap();

		StringBuffer queryString = new StringBuffer();
		// 起訖期間 Start
		Calendar duringDateStart = (Calendar) map.get("duringDateStart");
		// 起訖期間 End
		Calendar duringDateEnd = (Calendar) map.get("duringDateEnd");
		// 出差事由
		String bizMatter = (String) map.get("bizMatter");
		// 申請人員工代號
		String userId = (String) map.get("userId");
		// 成本單位代號
		String costUnitCode = (String) map.get("costUnitCode");

		if (StringUtils.isNotBlank(userId)) {
			User loginUser = (User) AAUtils.getLoggedInUser();
			GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());
			if (GroupCode.BUDGET.equals(groupCode)) {
				User searchUser = this.getFacade().getUserService().findByCode(userId);
				if (searchUser != null) {
					Department loginUserDepartment = this.getDepartmentLevel(loginUser.getDepartment());
					Department searchUserDepartment = this.getDepartmentLevel(searchUser.getDepartment());
					if (loginUserDepartment.getCode().compareTo(searchUserDepartment.getCode()) != 0) {
						throw new ExpRuntimeException(ErrorCode.C10540);
					}
				}
			}
		}

		// 申請人員工代號
		if (StringUtils.isBlank(userId)) {
			userId = "ALL";
		}

		// 成本單位代號
		if (StringUtils.isBlank(costUnitCode)) {
			costUnitCode = "ALL";
		}

		// 出差事由
		if (StringUtils.isBlank(bizMatter)) {
			bizMatter = "ALL";
		}
		//當起日與迄日不為空時，再判斷
		if(duringDateStart!=null&&duringDateEnd!=null){
			if (duringDateStart.compareTo(duringDateEnd) >= 1) {
			// 錯誤訊息 出差起日不可大於出差迄日
				throw new ExpRuntimeException(ErrorCode.C10634, new String[] { MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_biz_DuringDateStart"), MessageUtils.getAccessor().getMessage("tw_com_skl_exp_web_jsf_managed_gae_biz_duringDateEnd") });
			}
		}
		
		String rptName = "";
		// 報表名稱與路徑
		rptName = CrystalReportConfigManagedBean.getManagedBean().getBizTripReportName();

		params.put("rptName", rptName);
		// 成本單位代號
		params.put("costUnitCode", costUnitCode);
		// 申請人員工代號
		params.put("userId", userId);
		// 出差事由下拉選單
		params.put("bizMatter", bizMatter);
		// 起訖期間 Start
		if (null != duringDateStart) {
			params.put("duringDateStart", DateUtils.getISODateStr(duringDateStart.getTime(), ""));
		} else {
			params.put("duringDateStart", "ALL");
		}

		// 起訖期間 End
		if (null != duringDateEnd) {
			params.put("duringDateEnd", DateUtils.getISODateStr(duringDateEnd.getTime(), ""));
		} else {
			params.put("duringDateEnd", "ALL");
		}

		// 查詢字串
		// params.put("queryString", this.getQueryString());
		// RE201800088_國內/國外出差費用核銷費用明細總表新增資料欄位 EC0416 2018/1/12 end

		// RE201302964_aplog CU3178 in 2014/02/13 start
		logPrintApLog(params, "C11.2.3國內出差(研修)旅費申請總表"); //
		// 紀錄查詢條件至ap_log_server
		// RE201302964_aplog CU3178 in 2014/02/13 end

		// 產生報表
		CrystalReportConfigManagedBean.generateReport(params);
		return "dialog:print";

	}

	private String getQueryString() {
		Map map = getFindCriteriaMap();
		StringBuffer queryString = new StringBuffer();
		// 起訖期間 Start
		Calendar duringDateStart = (Calendar) map.get("duringDateStart");
		// 起訖期間 End
		Calendar duringDateEnd = (Calendar) map.get("duringDateEnd");
		// 出差事由
		String bizMatter = (String) map.get("bizMatter");
		// 申請人員工代號
		String userId = (String) map.get("userId");
		// 成本單位代號
		String costUnitCode = (String) map.get("costUnitCode");

		if (StringUtils.isNotBlank(userId)) {
			User loginUser = (User) AAUtils.getLoggedInUser();
			GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());
			if (GroupCode.BUDGET.equals(groupCode)) {
				User searchUser = this.getFacade().getUserService().findByCode(userId);
				if (searchUser != null) {
					Department loginUserDepartment = this.getDepartmentLevel(loginUser.getDepartment());
					Department searchUserDepartment = this.getDepartmentLevel(searchUser.getDepartment());
					if (loginUserDepartment.getCode().compareTo(searchUserDepartment.getCode()) != 0) {
						throw new ExpRuntimeException(ErrorCode.C10540);
					}
				}
			}
		}
		/*
		 * 
		 * WHERE T1.BIZ_START_DATE BETWEEN TO_DATE('20100505', 'YYYYMMDD') --參數
		 * 出差期間 查詢起日 AND TO_DATE('20100607', 'YYYYMMDD') AND--參數 出差期間 查詢迄日
		 * T1.USER_ID = 'AZ6897' --參數 員工代號、非必填 T1.COST_UNIT_CODE = '11L000'
		 * AND--參數 成本單位代號、非必填 T3.BIZ_MATTER = '3' AND--參數 出差事由 , 非必填
		 * 頁面上之選項需先置換成CODE
		 */

		Map<String, Object> params = new HashMap<String, Object>();
		// 起訖期間
		if (null != duringDateStart && null != duringDateEnd) {
			// T1.BIZ_START_DATE BETWEEN TO_DATE('20100505', 'YYYYMMDD') --參數
			// 出差期間 查詢起日
			// AND TO_DATE('20100607', 'YYYYMMDD') AND--參數 出差期間 查詢迄日
			queryString.append(" T1.BIZ_START_DATE BETWEEN TO_DATE(:duringDateStart, 'YYYYMMDD') AND TO_DATE(:duringDateEnd, 'YYYYMMDD') AND ");
			params.put("duringDateStart", DateUtils.getISODateStr(duringDateStart.getTime(), ""));
			params.put("duringDateEnd", DateUtils.getISODateStr(duringDateEnd.getTime(), ""));
		}

		// 申請人員工代號
		if (StringUtils.isNotBlank(userId)) {
			// T1.USER_ID = 'AZ6897' AND --參數 員工代號、非必填
			queryString.append(" T1.USER_ID = :userId AND ");
			params.put("userId", userId);
		}

		// 成本單位代號
		if (StringUtils.isNotBlank(costUnitCode)) {
			// T1.COST_UNIT_CODE = '11L000' AND--參數 成本單位代號、非必填
			queryString.append(" T1.COST_UNIT_CODE = :costUnitCode AND ");
			params.put("costUnitCode", costUnitCode);
		}

		// 出差事由
		if (StringUtils.isNotBlank(bizMatter)) {
			// T3.BIZ_MATTER = '3' AND--參數 出差事由 , 非必填 頁面上之選項需先置換成CODE
			queryString.append(" T3.BIZ_MATTER = :bizMatter AND ");
			params.put("bizMatter", bizMatter);
		}

		if (queryString.length() != 0) {
			int lastIndex = queryString.lastIndexOf("AND");
			queryString = queryString.delete(lastIndex, queryString.length());
		}

		return queryString.length() != 0 ? " WHERE " + StringUtils.queryStringAssembler(queryString.toString(), params) : " ";
	}

	/**
	 * 是否可操作成本單位代號
	 * 
	 * @return
	 */
	public boolean isDisabledCostUnit() {

		User loginUser = (User) AAUtils.getLoggedInUser();
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());
		if (GroupCode.GENERAL.equals(groupCode) || GroupCode.BUDGET.equals(groupCode)) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 是否可操作員工代號
	 * 
	 * @return
	 */
	public boolean isDisabledUserId() {

		User loginUser = (User) AAUtils.getLoggedInUser();
		GroupCode groupCode = GroupCode.getByValue(loginUser.getGroup().getCode());
		if (GroupCode.GENERAL.equals(groupCode)) {
			return true;
		} else {
			return false;
		}

	}

	public IntrTrvlBizExpFacade getFacade() {
		return facade;
	}

	public void setFacade(IntrTrvlBizExpFacade facade) {
		this.facade = facade;
	}

	// RE201302964_aplog CU3178 in 2014/02/13 start
	public void initialize() {
		// TODO Auto-generated method stub

	}
	// RE201302964_aplog CU3178 in 2014/02/13 end
}