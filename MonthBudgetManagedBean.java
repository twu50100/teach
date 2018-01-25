package tw.com.skl.exp.web.jsf.managed.bd.provisionbudget;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import tw.com.skl.common.model6.web.jsf.utils.FacesUtils;
import tw.com.skl.common.model6.web.util.ApplicationLocator;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartment;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartmentType;
import tw.com.skl.exp.kernel.model6.bo.BfmMonthBudget;
import tw.com.skl.exp.kernel.model6.bo.Budget;
import tw.com.skl.exp.kernel.model6.bo.BudgetYear;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.dto.SpliteBudgetItemDto;
import tw.com.skl.exp.kernel.model6.facade.BudgetFacade;
import tw.com.skl.exp.kernel.model6.logic.BudgetChangeItemService;
import tw.com.skl.exp.kernel.model6.logic.BudgetService;
import tw.com.skl.exp.kernel.model6.logic.MonthBudgetService;
import tw.com.skl.exp.web.util.BfmSelectFactory;
import tw.com.skl.exp.web.util.BfmSelectHelper;

/**
 * E01.03 編列月預算
 * 
 * @author CU3178
 * 
 */
public class MonthBudgetManagedBean implements Serializable {

	private static final long serialVersionUID = 1L;

	// confirmDepartmentAndYear.jsp確認的年份Id
	private String selectBudgetYearId;
	private Map<String, BudgetYear> budgetYearMap = null;
	private List<SelectItem> budgetYearList = null;

	// confirmDepartmentAndYear.jsp確認的父層編製單位Id
	private String selectDepartmentId;
	private Map<String, BfmDepartment> departmentMap = null;
	private List<SelectItem> departmentList = null;

	// 是否顯示renderMessage訊息
	private boolean renderInfo = false;
	// 訊息主體
	private String renderMessage;
	// a4j訊息
	private String renderA4jMessage = "";

	private Set<BfmMonthBudget> targetMonthBudgets;
	private Set<BfmMonthBudget> deleteMonthBudgets;

	private Budget targetBudget;

	private long totalOfMonthBudgets;

	// 是否disable儲存按鈕
	private boolean disableBtnSave;
	// 是否disable送出上傳按鈕
	private boolean disableBtnSubmit;

	// 拆分功能修改 EC0416 2018/1/22 start
	/**
	 * 修改頁面用SpliteBudgetItemDto
	 */
	private SpliteBudgetItemDto updatingDto = new SpliteBudgetItemDto();

	// 拆分功能修改 EC0416 2018/1/22 end

	public MonthBudgetManagedBean() {

	}

	public boolean isRenderInfo() {
		return renderInfo;
	}

	public void setRenderInfo(boolean renderInfo) {
		this.renderInfo = renderInfo;
	}

	public String getRenderMessage() {
		return renderMessage;
	}

	public void setRenderMessage(String renderMessage) {
		this.renderMessage = renderMessage;
	}

	public String getRenderA4jMessage() {
		return renderA4jMessage;
	}

	public void setRenderA4jMessage(String renderA4jMessage) {
		this.renderA4jMessage = renderA4jMessage;
	}

	// manyToOne selection menu begin
	public List<SelectItem> getBudgetYearList() {
		if (this.budgetYearList == null) {
			this.budgetYearList = BfmSelectFactory.creatFirstSecondYearBudgetYearOrderByYearDescSelect().getSelectList();
			this.budgetYearMap = (Map<String, BudgetYear>) FacesUtils.getSessionScope().get("firstSecondBudgetYearMap");
		}
		return budgetYearList;

	}

	public String getSelectBudgetYearId() {
		return this.selectBudgetYearId;
	}

	public void setSelectBudgetYearId(String selectBudgetYearId) {
		this.selectBudgetYearId = selectBudgetYearId;
	}

	// manyToOne selection menu end

	// manyToOne selection menu begin
	public String getSelectDepartmentId() {
		return selectDepartmentId;
	}

	public void setSelectDepartmentId(String selectDepartmentId) {
		this.selectDepartmentId = selectDepartmentId;
	}

	public List<SelectItem> getDepartmentList() {
		if (this.selectBudgetYearId != null && departmentList == null) {
			// UserVo userVo = (UserVo)
			// FacesUtils.getSessionScope().get("userVo");
			User user = (User) AAUtils.getLoggedInUser();
			this.departmentList = BfmSelectFactory.creatDepartmentByYearForHumanAllocation(budgetYearMap.get(selectBudgetYearId), user, BfmDepartmentType.SECTION_DEPARTMENT_TYPE).getSelectList();
			this.departmentMap = (Map<String, BfmDepartment>) FacesUtils.getSessionScope().get("departmentMap");

			// 如果只有一筆(預設為SelectHelper.EMPTY_SELECTITEM), 則代表沒有權限使用此功能
			// System.out.println(this.departmentList.size());
			if (this.departmentList.size() == 1) {
				ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
				this.renderA4jMessage = bundle.getString("tw_com_skl_bfm_kernel_model5_web_HumanAllocation_noRightWarn");
			}
		} else if (departmentList == null) {
			this.departmentList = new ArrayList<SelectItem>();
			this.departmentList.add(BfmSelectHelper.EMPTY_SELECTITEM);

		}
		return departmentList;

	}

	// manyToOne selection menu end

	public void onBudgetYearChange() {
		this.departmentList = null;
		this.renderA4jMessage = "";
		// AjaxUtils.refreshZones("zoneItemType");
	}

	/**
	 * 確認查詢按鈕
	 * 
	 * @return
	 */
	public String doBtnConfirmAction() {
		this.renderInfo = false;
		this.setRenderMessage("");
		BudgetYear budgetYear = budgetYearMap.get(this.getSelectBudgetYearId());
		BfmDepartment department = departmentMap.get(this.getSelectDepartmentId());
		targetBudget = department.readBudgetByYear(budgetYear.getYear());
		//
		User loginUser = (User) AAUtils.getLoggedInUser();
		// 判斷該預算表的狀態是否允許使用編列月預算
		boolean permit = targetBudget.getBudgetStateType().isHasFunctionFun0401();

		if (permit) {
			// if ((permit &&
			// (loginUser.getBo().getGroup().getName().compareTo("預算編制群組")==0||loginUser
			// .getBo().getGroup().getName().compareTo("系統管理群組") == 0))
			// ||(targetBudget.getBudgetStateType().equals(BudgetStateType.RERE_MONTH_BUDGET)
			// && loginUser
			// .getBo().getGroup().getName().compareTo("管理群組") == 0)) {
			targetMonthBudgets = this.getBudgetFacade().readAggregatedMonthBudgets(budgetYearMap.get(selectBudgetYearId).getYear(), departmentMap.get(selectDepartmentId));
			deleteMonthBudgets = this.getBudgetFacade().readNotExistMonthBudgets(budgetYearMap.get(selectBudgetYearId).getYear(), departmentMap.get(selectDepartmentId));
			return "readMonthBudget";
		} else {
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			this.renderInfo = true;
			this.setRenderMessage(bundle.getString("tw_com_skl_bfm_kernel_model5_web_MonthBudget_unavailableWarn"));
		}

		return "confirmDepartmentAndYearForMonthBudget";
	}

	/**
	 * 送出上傳按鈕
	 * 
	 * @return
	 */
	public String doBtnSubmitAction() {
		// 檢查各月的金額加總是否等於全年預算
		for (BfmMonthBudget monthBudget : targetMonthBudgets) {
			if (monthBudget.getTotalBudgetAmount() != monthBudget.getSumOfMonthBudget()) {
				String[] message = { monthBudget.getBudgetItem().getName() };
				MessageManager.getInstance().showInfoCodeMessage("tw_com_skl_bfm_kernel_model5_web_MonthBudget_notMatch", message);
				return null;
			}
		}

		// 儲存後再送出上傳
		this.getBudgetFacade().doSubmitMonthBudgets(targetMonthBudgets, targetBudget, departmentMap.get(selectDepartmentId), budgetYearMap.get(selectBudgetYearId).getYear());

		// 拆分功能修改 EC0416 2018/1/22 start
		// 當執行月預算編列完成後，若有執行拆分過的單位須依比例重新計算拆分金額並回寫入拆分檔TBBFM_PROJECT_BUDGET_ITEMS
		doSplitAction(getSelectBudgetYearId(), getSelectDepartmentId());
		// 拆分功能修改 EC0416 2018/1/22 end

		// 顯示編列月預算完成
		MessageManager.getInstance().showInfoCodeMessage("tw_com_skl_bfm_kernel_model5_web_MonthBudget_finish");

		// disable送出上傳,儲存按鈕
		this.setDisableBtnSubmit(true);
		this.setDisableBtnSave(true);

		return null;
	}

	/**
	 * 重新整理按鈕
	 * 
	 * @return
	 */
	public String doBtnSearchAction() {
		this.setTargetMonthBudgets(null);
		this.setDeleteMonthBudgets(null);
		this.targetMonthBudgets = this.getBudgetFacade().readAggregatedMonthBudgets(budgetYearMap.get(selectBudgetYearId).getYear(), departmentMap.get(selectDepartmentId));
		this.deleteMonthBudgets = this.getBudgetFacade().readNotExistMonthBudgets(budgetYearMap.get(selectBudgetYearId).getYear(), departmentMap.get(selectDepartmentId));

		return "readMonthBudget";
	}

	/**
	 * 儲存按鈕
	 * 
	 * @return
	 */
	public String doBtnSaveAction() {

		// 檢查各月的金額加總是否等於全年預算
		for (BfmMonthBudget monthBudget : targetMonthBudgets) {
			if (monthBudget.getTotalBudgetAmount() != monthBudget.getSumOfMonthBudget()) {
				String[] message = { monthBudget.getBudgetItem().getName() };
				MessageManager.getInstance().showInfoCodeMessage("tw_com_skl_bfm_kernel_model5_web_MonthBudget_notMatch", message);
				return null;
			}
		}

		// 儲存月預算
		this.getBudgetFacade().doSaveMonthBudgets(targetMonthBudgets);

		// 刪除不存在的預算編制項目
		this.getBudgetFacade().doDeleteMonthBudgets(deleteMonthBudgets);
		this.doBtnSearchAction();

		return null;
	}

	// 拆分功能修改 EC0416 2018/1/22 start
	/**
	 * 依比例重新計算拆分金額回寫拆分檔
	 * 
	 * @param bfmDepartment
	 * @param i
	 */
	public void doSplitAction(String yyyy, String depCode) {
		List<SpliteBudgetItemDto> dtoList = new ArrayList<SpliteBudgetItemDto>();
		// 依據傳入的年份與單位代號找出對應的拆分檔
		dtoList = getBudgetFacade().getProjectBudgetItemService().findSpliteDtoByyyyyAndDepCode(yyyy, depCode);
		// 如果拆分後屬性不為空，則依比例重新計算拆分後金額
		if (!CollectionUtils.isEmpty(dtoList)) {
			for (Object obj : dtoList) {
				SpliteBudgetItemDto dto = new SpliteBudgetItemDto();
				Object[] record = (Object[]) obj;
				// 拆分屬性
				String originDepPropName = dto.getOriginDepPropCode();
				// 若拆分屬性有值則比較編制金額
				if (originDepPropName != null) {
					// 編制金額
					BigDecimal amount = dto.getAmount();
					// 拆分後原屬性金額
					BigDecimal splitOriginAmt = dto.getOriginAmount();
					// 拆分後新屬性金額
					BigDecimal splitNewAmt = dto.getSpliteAmount();
					//原屬性金額+拆分後金額
					BigDecimal splitTotalAmt=splitOriginAmt.add(splitNewAmt);
					
					//如果編制金額與(原屬性金額+拆分後金額)不相等				
					if(amount.compareTo(splitTotalAmt) != 0){
					//依比例重新計算原屬性金額與拆分後金額並寫回拆分檔	
					}
					

				}

				
				// 新的拆分後原屬性金額
				BigDecimal newSplitOriginAmt = BigDecimal.ZERO;
				// 新的拆分後新屬性金額
				BigDecimal newSplitNewAmt = BigDecimal.ZERO;

				// 新的拆分後新屬性金額
				// 編制金額(amount)X(拆分後新屬性金額(
				// split_new_amt))/(（拆分原屬性金額(split_origin_amt)+拆分後新屬性金額(split_new_amt)）)
				// newSplitNewAmt=amount.multiply(splitNewAmt.divide(splitOriginAmt.add(splitNewAmt))).setScale(0,
				// BigDecimal.ROUND_HALF_UP);;
				//
				// //新的拆分後原屬性金額
				// //編制金額(amount)-新的拆分後新屬性金額(tbbfm_project_budget_items.split_new_amt)
				// newSplitOriginAmt=amount.subtract(newSplitNewAmt);

				// 重新計算過的金額寫回拆分檔
				// 拆分後原屬性金額
				// bo.setOriginAmount(newSplitOriginAmt);
				// //拆分後新屬性金額
				// bo.setSpliteAmount(newSplitNewAmt);

				// }
			}
		}

	}

	private SpliteBudgetItemDto getUpdatingDto() {
		// TODO Auto-generated method stub
		return updatingDto;
	}

	/**
	 * 修改頁面用SpliteBudgetItemDto
	 * 
	 * @param updatingDto
	 */
	public void setUpdatingDto(SpliteBudgetItemDto updatingDto) {
		this.updatingDto = updatingDto;
	}

	// 拆分功能修改 EC0416 2018/1/22 end

	private BudgetService getBudgetService() {
		return (BudgetService) ApplicationLocator.getBean("budgetService");
	}

	private MonthBudgetService getMonthBudgetService() {
		return (MonthBudgetService) ApplicationLocator.getBean("monthBudgetService");
	}

	private BudgetChangeItemService getBudgetChangeItemService() {
		return (BudgetChangeItemService) ApplicationLocator.getBean("budgetChangeItemService");
	}

	public Budget getTargetBudget() {
		return targetBudget;
	}

	public void setTargetBudget(Budget targetBudget) {
		this.targetBudget = targetBudget;
	}

	public Set<BfmMonthBudget> getTargetMonthBudgets() {
		return targetMonthBudgets;
	}

	public void setTargetMonthBudgets(Set<BfmMonthBudget> targetMonthBudgets) {
		this.targetMonthBudgets = targetMonthBudgets;
	}

	public Set<BfmMonthBudget> getDeleteMonthBudgets() {
		return deleteMonthBudgets;
	}

	public void setDeleteMonthBudgets(Set<BfmMonthBudget> deleteMonthBudgets) {
		this.deleteMonthBudgets = deleteMonthBudgets;
	}

	public boolean isDisableBtnSubmit() {
		return disableBtnSubmit;
	}

	public void setDisableBtnSubmit(boolean disableBtnSubmit) {
		this.disableBtnSubmit = disableBtnSubmit;
	}

	public boolean isDisableBtnSave() {
		return disableBtnSave;
	}

	public void setDisableBtnSave(boolean disableBtnSave) {
		this.disableBtnSave = disableBtnSave;
	}

	public long getTotalOfMonthBudgets() {
		totalOfMonthBudgets = 0;
		for (BfmMonthBudget monthBudget : this.targetMonthBudgets) {
			totalOfMonthBudgets = totalOfMonthBudgets + monthBudget.getSumOfMonthBudget();
		}
		return totalOfMonthBudgets;
	}

	private BudgetFacade getBudgetFacade() {
		return (BudgetFacade) ApplicationLocator.getBean("budgetFacade");
	}
}
