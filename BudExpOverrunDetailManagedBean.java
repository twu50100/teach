package tw.com.skl.exp.web.jsf.managed.gae.budgetexpexplain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.trinidad.component.core.data.CoreColumn;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.SortableModel;
import org.springframework.util.CollectionUtils;

import tw.com.skl.common.model6.web.jsf.managedbean.impl.TemplateDataTableManagedBean;
import tw.com.skl.common.model6.web.jsf.utils.FacesUtils;
import tw.com.skl.common.model6.web.jsf.utils.Messages;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.exp.kernel.model6.bo.AccTitle;
import tw.com.skl.exp.kernel.model6.bo.ApplState;
import tw.com.skl.exp.kernel.model6.bo.BudExpOverrunAppl;
import tw.com.skl.exp.kernel.model6.bo.CarAffidavit;
import tw.com.skl.exp.kernel.model6.bo.ExpMain;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.bo.YearDepartment;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.Function.FunctionCode;
import tw.com.skl.exp.web.util.ExpUtils;
import tw.com.skl.exp.web.util.ReportData;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.common.util.MessageUtils;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dto.UserTrainApplDto;
import tw.com.skl.exp.kernel.model6.facade.BudExpOverrunReviewFacade;
import tw.com.skl.exp.kernel.model6.facade.CarAffidavitFacade;
import tw.com.skl.exp.kernel.model6.facade.ExpMainFacade;
import tw.com.skl.exp.kernel.model6.logic.BudExpOverrunReviewService;
import tw.com.skl.exp.kernel.model6.logic.CarAffidavitService;
import tw.com.skl.exp.kernel.model6.logic.ExpMainService;
import tw.com.skl.exp.kernel.model6.logic.YearDepartmentService;
import tw.com.skl.exp.web.config.CrystalReportConfigManagedBean;
import tw.com.skl.exp.web.jsf.managed.FunctionCodeAware;
import tw.com.skl.exp.kernel.model6.bo.Function;
import tw.com.skl.exp.web.util.FileExporter;
import tw.com.skl.exp.web.vo.CarAffidavitVo;
import tw.com.skl.exp.web.vowrapper.CarAffidavitWrapper;
import tw.com.skl.exp.web.vowrapper.ExpapplCWrapper;
import tw.com.skl.exp.kernel.model6.logic.DepartmentService;

/**
 * C13.4 查詢預算實支異常說明
 * 
 * @author EC0416
 * @version RE201702775_預算實支追蹤表單_費用系統
 */
public class BudExpOverrunDetailManagedBean extends TemplateDataTableManagedBean<BudExpOverrunAppl, BudExpOverrunReviewService> {

	protected Log logger = LogFactory.getLog(this.getClass());

	private BudExpOverrunReviewFacade facade;
	private List<SelectItem> budgetItemList;

	private String depcod;

	private DepartmentService departmentService;

	public DepartmentService getDepartmentService() {
		return departmentService;
	}

	/**
	 * 一進入頁面的初始化
	 */
	public BudExpOverrunDetailManagedBean() {

		initFindCriteriaMap(); // 設定查詢條件
	}

	protected void initFindCriteriaMap() {
		Map<String, Object> findCriteriaMap = new HashMap<String, Object>();
		findCriteriaMap.put("codeList", null);// 單位代號
		findCriteriaMap.put("erYyMm", null); // 異常年月
		findCriteriaMap.put("budgetItemList", "1"); // 預算項目
		this.setFindCriteriaMap(findCriteriaMap);
	}

	/**
	 * 取得登入的使用者
	 * 
	 * @return User
	 */
	private User getLoginUser() {
		return this.facade.getUserService().findByPK(((User) AAUtils.getLoggedInUser()).getId());
	}

	// 連接資料庫 找出
	public List<SelectItem> getCodeList() {
		List<SelectItem> codeList = new ArrayList<SelectItem>();
	    
		// 如果登入人員所屬單位代號為110100(審計課)
		if (getLoginUser().getDepartment().getCode().equals("110100")) {
			//codeList.add(new SelectItem(null, "-----"));
			// 全部單位
			List allDepList = facade.getDepartmentService().findbudgetdepartment();
			
			for (Object obj : allDepList) {
				Object[] record = (Object[]) obj;
				String depcode = (String) record[0]; // 單位代號
				String depname = (String) record[1]; // 單位名稱
				codeList.add(new SelectItem(depcode, depcode + depname));// 呈現方式為
																			// 單位代號加單位名稱一同顯示在下拉式選單之中
			}
		} else {
			List depList = facade.getDepartmentService().findDepartmentByMail(getLoginUser().getCode());
			if (!CollectionUtils.isEmpty(depList)) {
				for (Object obj : depList) {
					Object[] record = (Object[]) obj;
					String depCode = (String) record[0]; // 單位代號
					String depName = (String) record[1]; // 單位中文

					codeList.add(new SelectItem(depCode, depCode + depName));

				}
			}
		}

		return codeList;
	}


	@Override
	protected void initUpdatingData(ValueObject<BudExpOverrunAppl> updatingData) {
	}

	@Override
	protected void setupUpdatingData() {
	}

	@Override
	protected void initCreatingData() {
	}

	public FunctionCode getFunctionCode() {
		return FunctionCode.C_13_4;
	}

	// 總表查詢
	public String doQueryAction() {
		
		Map map = getFindCriteriaMap();
		String erYyMm = (String) map.get("erYyMm");
		ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession mySession = (HttpSession) extCtx.getSession(true);
		if (erYyMm == null) {
			String[] paramStrs = { Messages.getString("applicationResources", "erYyMm", null) };
			// {0}欄位-必須有值。
			throw new ExpRuntimeException(ErrorCode.A10039, paramStrs);
		}

		Map<String, Object> params = new HashMap<String, Object>();

		//單位代號下拉選單
		//會計部的人員才可使用全選功能
		String codeList = (String) map.get("codeList");
		if (getLoginUser().getDepartment().getCode().equals("110100")){
			if (StringUtils.isBlank(codeList)) {
			codeList = "ALL";
			}
		}
		// 預算項目下拉選單
		String budgetItemList = (String) map.get("budgetItemList");
		if (budgetItemList.equals("1")) {
			budgetItemList = "ALL";
		}
		
		//如果單位代號點選全選與預算項目點選單選 OR 單位代號單選與預算項目單選 則跳出錯誤訊息
		if(!budgetItemList.equals("ALL")){
			 throw new ExpRuntimeException(ErrorCode.C10657, new String[] {});	
		}
		
		String rptName = "";
		rptName = CrystalReportConfigManagedBean.getManagedBean().getBudExpOverrunDetailTotalReportName();
		params.put("depCode", codeList);
		params.put("erYyMm", erYyMm);
		params.put("rptName", rptName);
		CrystalReportConfigManagedBean.generateReport(params);

		return "dialog:print";
	}

	// 明細查詢
	public String doQueryDetailAction() {
		Map map = getFindCriteriaMap();
		String erYyMm = (String) map.get("erYyMm");

		if (erYyMm == null) {
			String[] paramStrs = { Messages.getString("applicationResources", "erYyMm", null) };
			// {0}欄位-必須有值。
			throw new ExpRuntimeException(ErrorCode.A10039, paramStrs);
		}

		Map<String, Object> params = new HashMap<String, Object>();

		// 預算項目下拉選單
		String codeList = (String) map.get("codeList");
		if (StringUtils.isBlank(codeList)) {
			codeList = "ALL";
		}

		// 預算項目下拉選單
		String budgetItemList = (String) map.get("budgetItemList");
		if (budgetItemList.equals("1")) {
			budgetItemList = "ALL";
		}

		// 明細表

		String rptName = "";
		rptName = CrystalReportConfigManagedBean.getManagedBean().getBudExpOverrunDetailReportName();
		params.put("erYyMm", erYyMm);
		params.put("rptName", rptName);
		params.put("budgetCode", budgetItemList);
		params.put("depCode", codeList);
		CrystalReportConfigManagedBean.generateReport(params);

		return "dialog:print";
	}

	public BudExpOverrunReviewFacade getFacade() {
		return facade;
	}

	public void setFacade(BudExpOverrunReviewFacade facade) {
		this.facade = facade;
	}

	/**
	 * 顯示預算代號下拉式選單
	 */
	public List<SelectItem> getBudgetItemList() {
		List<SelectItem> budgetItemList = new ArrayList<SelectItem>();
		budgetItemList.add(new SelectItem("1", "-----"));
		List budgetItem = facade.getBudgetItemService().findBudgetItemList();
		if (!CollectionUtils.isEmpty(budgetItem)) {
			for (Object obj : budgetItem) {
				Object[] record = (Object[]) obj;
				String code = (String) record[0]; // 預算代號
				String name = (String) record[1]; // 預算中文
				budgetItemList.add(new SelectItem(code, code + name));
			}
		}
		return budgetItemList;
	}

	public void setbudgetItemList(List<SelectItem> budgetItemList) {
		this.budgetItemList = budgetItemList;
	}

}