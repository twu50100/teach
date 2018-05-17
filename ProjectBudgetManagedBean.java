package tw.com.skl.exp.web.jsf.managed.bd.provisionbudget;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import tw.com.skl.common.model6.exception.BusinessException;
import tw.com.skl.common.model6.web.jsf.managedbean.PagedOperation;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.BatchUpdatingUnit;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.BatchUtil;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.PagedOperationImpl;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.TempPagedDataModelImpl;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.TempPagedManagedBeanImpl;
import tw.com.skl.common.model6.web.jsf.utils.FacesUtils;
import tw.com.skl.common.model6.web.util.ApplicationLocator;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.common.model6.web.vo.impl.TempVoImpl;
import tw.com.skl.common.model6.web.vo.impl.VoWrapperImpl;
import tw.com.skl.exp.kernel.model6.bo.BfmBudgetType;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartment;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartmentType;
import tw.com.skl.exp.kernel.model6.bo.BfmGroup.BfmGroupCode;
import tw.com.skl.exp.kernel.model6.bo.Budget;
import tw.com.skl.exp.kernel.model6.bo.BudgetDiffExplain;
import tw.com.skl.exp.kernel.model6.bo.BudgetYear;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudget;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudgetItem;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudgetType;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.facade.BudgetFacade;
import tw.com.skl.exp.kernel.model6.facade.ProjectBudgetFacade;
import tw.com.skl.exp.kernel.model6.logic.BudgetService;
import tw.com.skl.exp.kernel.model6.logic.BudgetYearService;
import tw.com.skl.exp.kernel.model6.logic.ProjectBudgetService;
import tw.com.skl.exp.web.util.BfmSelectFactory;
import tw.com.skl.exp.web.util.BfmSelectHelper;
import tw.com.skl.exp.web.util.SelectFactory;
import tw.com.skl.exp.web.vo.ProjectBudgetVo;

/**
 * E01.02	專案費用
 * @author CU3178
 *
 */
public class ProjectBudgetManagedBean extends TempPagedManagedBeanImpl
	<ProjectBudget, ProjectBudgetService>{

	private static final long serialVersionUID = 1L;

	// master specific begin
	private ProjectBudget_ProjectBudgetItemManagedBean projectBudget_ProjectBudgetItemManagedBean;
	//會由ProjectBudget_ConfirmYearDepartmentManagedBean所注入的年份
	private String selectBudgetYearId;
	//會由ProjectBudget_ConfirmYearDepartmentManagedBean所注入的編列單位編號
	private String selectDepartmentId;
	//會由ProjectBudget_ConfirmYearDepartmentManagedBean所注入的預算表編號
	private String selectBudgetId;
	//會與目前的ProjectBudget連結的department，編號則由selectDepartmentId決定
	private BfmDepartment department;
	//會與目前的ProjectBudget連結的budgetYear，編號則由selectBudgetId決定
	private BudgetYear budgetYear;
	//會與目前的ProjectBudget連結的projectBudgetType
	private ProjectBudgetType projectBudgetType;
	//會與目前的ProjectBudget連結的budget，編號則由selectBudgetId決定
	private Budget budget;
	//會與目前的ProjectBudget連結的budgetType	
	private BfmBudgetType budgetType;

	//RE201801038_預算同期差異說明 CU3178 2018/4/19 START
	//續編專案代號下拉式選單
	private List<SelectItem> continueProjectbudgetList = null;
	
	//續編專案代號
	private ProjectBudget continueProjectBudget = null;
	
	//是否為續編專案
	private boolean commandContinue = false;
	//RE201801038_預算同期差異說明 CU3178 2018/4/19 END
	

	public ProjectBudgetManagedBean() {
		
		this.setVoWrapper(new ProjectBudgetWrapper());
		this.setServiceName("projectBudgetService");
	}

	// master specific begin
	@Override
	protected void initCreatingData() {
		
		ProjectBudgetVo projectBudgetItemVo = new ProjectBudgetVo();
		projectBudgetItemVo.setBo(new ProjectBudget());
		this.setUpdatingData(projectBudgetItemVo);

		BudgetYear budgetYear = this.getBudgetYear();
		BfmDepartment department = this.getDepartment();
		
		//建立format物件，以格式化字串
		NumberFormat formatter= new DecimalFormat("000");
		String projectCode = budgetYear.getYear() + department.getCode().trim()+formatter.format(this.getBudget().getLastProjectCodeNumber()+1);
		//專案代號編號加1
		this.getBudget().setLastProjectCodeNumber(this.getBudget().getLastProjectCodeNumber()+1);
		//一般來說都是到儲存才建立關聯，不過由於此兩項目都是固定的所已於此建立，budget的關聯則，到save在建立，因為新增時可能會有不新增的狀況
		this.getUpdatingData().getBo().setCode(projectCode);
		//this.setupBudget();
		this.setupBudgetType();
		this.setupProjectBudgetType();
		
		//RE201801038_預算同期差異說明 CU3178 2018/4/19 START
		//若為續編專案，則將該專案代號資料帶入
		if(isCommandContinue()){			
			this.getUpdatingData().getBo().setName(getContinueProjectBudget().getName());//專案名稱
			this.getUpdatingData().getBo().setStartDate(getContinueProjectBudget().getStartDate());//專案起日
			this.getUpdatingData().getBo().setEndDate(getContinueProjectBudget().getEndDate());//專案迄日
			this.getUpdatingData().getBo().setMeasured(getContinueProjectBudget().isMeasured());//是否可量化	
			this.getUpdatingData().getBo().setBenefitMilestone(getContinueProjectBudget().getBenefitMilestone());//專案執行計畫
			this.getUpdatingData().getBo().setBenefitSchedule(getContinueProjectBudget().getBenefitSchedule());//專案預算及效益說明
			this.getUpdatingData().getBo().setBenefitAmount(getContinueProjectBudget().getBenefitAmount());//專案未來效益評估
			this.getUpdatingData().getBo().setNotation(getContinueProjectBudget().getNotation());//專案目標
			
			//查詢該續編專案預算項目
			List<ProjectBudgetItem> continueProBudgetItem = getBudgetFacade().getProjectBudgetItemService().readByProjectBudget(getContinueProjectBudget());
			
			//放入續編專案預算項目
			for(ProjectBudgetItem continueBo: continueProBudgetItem){
				ProjectBudgetItem bo = new ProjectBudgetItem();		
				bo.setBudgetItem(continueBo.getBudgetItem());
				bo.setBudgetNotation(continueBo.getBudgetNotation());
				bo.setAmount(continueBo.getAmount());				
				this.getUpdatingData().getBo().addProjectBudgetItem(bo);
			}
			
			
			//建立預算差異說明資料
			BudgetDiffExplain budgetDiffExplain = new BudgetDiffExplain();
			//放入本期專案預算資料表
			budgetDiffExplain.setThisProjectBudget(this.getUpdatingData().getBo());
			//放入上期專案預算資料表
			budgetDiffExplain.setLastProjectBudget(getContinueProjectBudget());
			User user = (User) AAUtils.getLoggedInUser();
			//登入人員
			budgetDiffExplain.setCreateUser(user);
			//將資料暫存至預算資料表中
			this.getUpdatingData().getBo().addBudgetDiffExplain(budgetDiffExplain);
		}	
		//RE201801038_預算同期差異說明 CU3178 2018/4/19 END

		
	}

	public ProjectBudget_ProjectBudgetItemManagedBean getProjectBudget_ProjectBudgetItemManagedBean() {
		return projectBudget_ProjectBudgetItemManagedBean;
	}

	public void setProjectBudget_ProjectBudgetItemManagedBean(ProjectBudget_ProjectBudgetItemManagedBean projectBudget_ProjectBudgetItemManagedBean) {
		this.projectBudget_ProjectBudgetItemManagedBean = projectBudget_ProjectBudgetItemManagedBean;
		this.addDetailManagedBean(this.projectBudget_ProjectBudgetItemManagedBean);
	}

	// master specific end
	public String getSelectBudgetYearId() {
		return selectBudgetYearId;
	}

	public void setSelectBudgetYearId(String selectBudgetYearId) {
		this.selectBudgetYearId = selectBudgetYearId;
	}

	public String getSelectDepartmentId() {
		return selectDepartmentId;
	}

	public void setSelectDepartmentId(String selectDepartmentId) {
		this.selectDepartmentId = selectDepartmentId;
	}
	
	public String getSelectBudgetId() {
		return selectBudgetId;
	}

	public void setSelectBudgetId(String selectBudgetId) {
		this.selectBudgetId = selectBudgetId;
	}


	public BfmDepartment getDepartment() {
		if(this.department==null){
			this.department = ((Map<String, BfmDepartment>)FacesUtils.getSessionScope().get("departmentMap")).get(this.getSelectDepartmentId());
		}
		return this.department;
	}
	
	

	public BudgetYear getBudgetYear() {
		if(this.budgetYear==null){
			this.budgetYear = ((Map<String, BudgetYear>)FacesUtils.getSessionScope().get("firstSecondBudgetYearMap")).get(this.getSelectBudgetYearId());
		}
		return this.budgetYear;
	}

	

	private ProjectBudgetFacade getProjectBudgetFacade(){
		return (ProjectBudgetFacade) ApplicationLocator.getBean("projectBudgetFacade");
	}	
	
	private BudgetService getBudgetService() {
		return (BudgetService) ApplicationLocator.getBean("budgetService");
	}

	public Budget getBudget(){
		if((this.budget==null)&&(this.getSelectBudgetId()!=null)){
			
			this.budget = this.getDepartment().readBudgetByYear(this.getBudgetYear().getYear());
			//this.budget = projectBudgetVo.getBo().getBudget();
		}

		return this.budget;
	}
	

	public ProjectBudgetVo wrap(Object object) {
		return (ProjectBudgetVo)this.getVoWrapper().wrap(object);
	}

	/**
	 * getUpdatingData 和 setUpdatingData 其實可以不需要, 因為 super class 已經有了
	 * 存在的目的, 只是為了讓 IDE 知道它的確切的type為何, 讓 jsf 的頁面比較好拖拉
	 */
	@Override
	public ProjectBudgetVo getUpdatingData() {
		return (ProjectBudgetVo)super.getUpdatingData();
	}

	public void setUpdatingData(ProjectBudgetVo vo) {
		super.setUpdatingData(vo);
	}

	private void setupBudget() {
		
		if (this.getUpdatingData().getBo().getBudget()== null && this.getBudget() != null) {
			this.getBudgetService().readInitializeProjectBudgets(this.getBudget());
			this.getBudget().addProjectBudgets(this.getUpdatingData().getBo());
			

			
		}
		
	}
	
	/**
	 * @author 偉哲
	 * 取得專案所屬的BudgetType
	 * @return
	 */
	public BfmBudgetType getBudgetType() {
		if (this.budgetType == null) {
			this.budgetType=BfmBudgetType.PROJECT_EXPENSE_TYPE;
		}
		return this.budgetType;
	}


	
	private void setupBudgetType() {
		
		if (this.getUpdatingData().getBo().getBudgetType()== null && this.getBudgetType() != null) {
			this.getUpdatingData().getBo().setBudgetType(this.getBudgetType());
		}
		
	}
	
	/**
	 * @author 偉哲
	 * 取得專案所屬的ProjectBudgetType
	 * @return
	 */
	public ProjectBudgetType getProjectBudgetType() {
		if (this.projectBudgetType == null) {
			
			BfmDepartment department = this.getDepartment();
			
			if(department.getDepartmentType().getId().compareTo(BfmDepartmentType.SECTION_DEPARTMENT_TYPE.getId())==0){
				this.projectBudgetType=ProjectBudgetType.SECTION_DEPARTMENT_PROJECT_TYPE;
			}else if(department.getDepartmentType().getId().compareTo(BfmDepartmentType.UNIT_DEPARTMENT_TYPE.getId())==0){
				this.projectBudgetType=ProjectBudgetType.UNIT_DEPARTMENT_PROJECT_TYPE;
			}

		}
		return this.projectBudgetType;
	}

	private void setupProjectBudgetType() {
		if (this.getUpdatingData().getBo().getProjectBudgetType()== null && this.getProjectBudgetType() != null) {
			this.getUpdatingData().getBo().setProjectBudgetType(this.getProjectBudgetType());
		}
	}
	// manyToOne selection menu end

	protected void setupUpdatingData() {
		this.setupBudget();
		//this.setupBudgetType();
		//this.setupProjectBudgetType();
	}

	@Override
	protected PagedOperation getPagedOperation(){
		if (pagedOperation == null) {
			if (StringUtils.isNotBlank(this.getServiceName())) {
				BudgetYear budgetYear = this.getBudgetYear();
				BfmDepartment department = this.getDepartment();
				pagedOperation = new ProjectBudgetPagedOperationImpl(this.getServiceName(), budgetYear, department);
			}
		}
		return this.pagedOperation;		
	}
	/**
	 * recursive 取出每個 vo 內的 detailDataModelMap,
	 * 來設定最後要傳出去的 BatchUpdatingUnit
	 *
	 * @param allList
	 * @param allBuu
	 */
	
	@SuppressWarnings("unchecked")
	private void setupBatchUpdatingUnit(final List allList, final BatchUpdatingUnit allBuu) {
		for (final Iterator<TempVoImpl> iter = allList.iterator(); iter.hasNext();) {
			final TempVoImpl vo = iter.next();
			allBuu.addAll(vo.generateBatchUpdatingUnit());
		}
	}
	
	
	/**
	 * @author 偉哲
	 * 因為需要異動不少層面的東西，如有由此method發動的話，則transaction無法包含整個異動，
	 * 所以複寫此method，主要由ProjectBudgetFacade來處理 
	 * 
	 */
/*	@Override
	public String doBtnSaveAction() {	
		
		
		//清除所有vo的deatil東西
//		if(this.getPagedDataModel()!=null){
//			
//			List<ProjectBudgetVo> projectBudgetVoList = (List)this.getPagedDataModel().getWrappedData();
//			for(ProjectBudgetVo projectBudgetVo : projectBudgetVoList){
//				if(projectBudgetVo.getDetailDataModel("ProjectBudgetItem")!=null){
//					projectBudgetVo.getDetailDataModelMap().clear();
//				}
//			}
//		}
		this.resetCurrentModified();
	
		
		return "read";
	}*/
	
	@Override
	public void batchUpdateData(){
		// allList 只有包含當頁的資料
		final List allList = (List)(this.getPagedDataModel().getWrappedData());

		// 每次儲存的時候, 將所有需要 save 的資料放入 allBuu
		final BatchUpdatingUnit allBuu = new BatchUpdatingUnit();
		allBuu.addAll(BatchUtil.generateBatchUpdatingUnit(allList));
		//recursive 去將 detail 頁面的資料放進去
		
		this.setupBatchUpdatingUnit(allList, allBuu);
		try {
						
			//抓取目前登入人員的資料
			User user = (User) AAUtils.getLoggedInUser();
			this.getProjectBudgetFacade().updateBatchForProjectBudget(user, this.getBudget(), allBuu.getCreatingList(), allBuu.getUpdatingList(), allBuu.getDeletingList());
			
			// 呼叫 refhresListData() 清除了 pagedDataModel 內的資料, 
			// 但不會清除 creating list 內的資料
			this.refreshListData();

			
			// 如果 read 頁面已經顯示過資料 (isShowData = true),
			// 就必須 clearTempData, 否則  table 內, 剛新增的資料會重複出現
			// (creating list 和 pagedDataModel 內的資料會重複)
			if (this.pagedDataModel.isShowData()) {
				((TempPagedDataModelImpl)this.pagedDataModel).clearTempData();
			}		
			// 如果read 頁面沒有顯示過資料, 不進行 clear temp data
			// 這樣才能讓 user 看到 剛剛新增過的資料 (顯示 creating list 內的資料)
			else {
				// 移除在 creating list 內, 被 delete 掉的資料
				// 否則有可能會造成  新資料儲存後, 馬上再被 delete, 資料仍然留在畫面上
				this.removeDeletedCreatingData();

				// reset 所有遺留的 Vo 的 status (creating list)
				for (final Iterator<ValueObject> iter = allList.iterator(); 
					iter.hasNext(); ) {
					
					iter.next().resetStatus();
				}				
			}
					
			
		} catch (BusinessException e) {
			// 假如其中一個出錯, 就不show其他的 error
			if ((! this.processException(allBuu.getCreatingList()) &&
					!this.processException(allBuu.getUpdatingList()) &&
					!this.processException(allBuu.getDeletingList())) ) {

				//假如上面的都沒有處理掉 exception, 就直接拋出
				throw e;
			}
		}
		
	}
	
	
	
	
	/**
	 * @author 偉哲
	 * 因為取消新增的狀況，需要將budget.lastProjectCodeNumber給退回一號，所以複寫此function
	 */
	@Override
	public String doBtnCancelAction() {		
		//看看是不是新增的,將budget.lastProjectCodeNumber給退回一號
		if(this.getUpdatingData().isFresh()){
			if(this.getBudget().getLastProjectCodeNumber()>0){
				this.getBudget().setLastProjectCodeNumber((this.getBudget().getLastProjectCodeNumber()-1));
			}
		}
		if (updatingData != null && updatingData.getPrevStatus() != null&& updatingData.isCreating()!=true) {
			// 將 updatingData 的資料, 從先前 serialization 出去的 bytes,
			// 再 serialization 一次回來
			ValueObject<ProjectBudget> preVo = updatingData.retrievePrevStatus();
			this.pagedDataModel.replaceWrappedData(preVo);
		}

		//for (TempPagedManagedBeanImpl bean : detailManagedBeans) {
		//	bean.resetCurrentModified();
		//}
		return this.getREAD_PAGE() + this.getReturnPostFix();
	}
	
	
	@Override
	public String doBtnTempCreateAction() {	
		boolean validateEndDate = validateTxtEndDate(this.getUpdatingData());
		boolean validateBSCCode = validateTxtBSCCode(this.getUpdatingData());
		if(validateEndDate){			
			if(validateBSCCode){
				//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START
				if(validateText(this.getUpdatingData())){
					return null;
				}
				//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END
				return super.doBtnTempCreateAction();
			}else{
				ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
				FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("validate_projectBudget_BSCCode"), bundle.getString("validate_projectBudget_BSCCode"));
				
				FacesContext.getCurrentInstance().addMessage(null, message);
				return null;
			}			
		}else{
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("validate_projectBudget_endDate"), bundle.getString("validate_projectBudget_endDate"));
			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return null;
		}
	}
	
	@Override
	public String doBtnTempUpdateAction() {
		boolean validateEndDate = validateTxtEndDate(this.getUpdatingData());
		boolean validateBSCCode = validateTxtBSCCode(this.getUpdatingData());
		if(validateEndDate){			
			if(validateBSCCode){
				//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START
				if(validateText(this.getUpdatingData())){
					return null;
				}
				//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END
				return super.doBtnTempUpdateAction();
			}else{
				ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
				FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("validate_projectBudget_BSCCode"), bundle.getString("validate_projectBudget_BSCCode"));
				
				FacesContext.getCurrentInstance().addMessage(null, message);
				return null;
			}	
		}else{
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("validate_projectBudget_endDate"), bundle.getString("validate_projectBudget_endDate"));
			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return null;
		}
		
	}
	
	/**
	 * @author 文珊
	 * 驗證勾選BSC後必填BSC策略編號,且策略編號長度不能大於10
	 */
	private boolean validateTxtBSCCode(ProjectBudgetVo projectBudgetVo){
		
		Boolean haveBSC = projectBudgetVo.getBo().isHaveBSC();
		String BSCCode = projectBudgetVo.getBo().getBSCCode();		
		//有勾選沒填編號
		if(haveBSC && BSCCode.length()==0){			
			return false;
			//有勾選但編號長度大於10
		}else if(haveBSC && BSCCode.length() > 10){
			return false;
		}else{
			return true;
		}
	}	
	

	//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START
	/**
	 * @author 文珊
	 * 驗證勾選BSC後必填BSC策略編號,且策略編號長度不能大於10
	 */
	private boolean validateText(ProjectBudgetVo projectBudgetVo){
		
		String benefitMilestone = projectBudgetVo.getBo().getBenefitMilestone();

		String benefitSchedule = projectBudgetVo.getBo().getBenefitSchedule();

		String benefitAmount = projectBudgetVo.getBo().getBenefitAmount();

		String notation = projectBudgetVo.getBo().getNotation();

		if(StringUtils.isEmpty(benefitMilestone)){
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitMilestoneWarning"), 
					bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitMilestoneWarning"));			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return true;
		}
		
		if(StringUtils.isEmpty(benefitSchedule)){
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitScheduleWarning"), 
					bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitScheduleWarning"));			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return true;
		}
		
		if(StringUtils.isEmpty(benefitAmount)){
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitAmountWarning"), 
					bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_benefitAmountWarning"));			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return true;
		}
		
		if(StringUtils.isEmpty(notation)){
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_notationWarning"), 
					bundle.getString("tw_com_skl_bfm_kernel_model5_bo_ProjectBudget_notationWarning"));			
			FacesContext.getCurrentInstance().addMessage(null, message);
			return true;
		}
		
		this.getUpdatingData().getBo().setBenefitMilestone(benefitMilestone.replace("\"", ""));
		this.getUpdatingData().getBo().setBenefitSchedule(benefitSchedule.replace("\"", ""));
		this.getUpdatingData().getBo().setBenefitAmount(benefitAmount.replace("\"", ""));
		this.getUpdatingData().getBo().setNotation(notation.replace("\"", ""));
		return false;
	}	
	//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END

	/**
	 * @author 偉哲
	 * 驗證專案結束日期是否大於專案起始日
	 * 理論上寫在驗證事件就可以，但是因為txtStartDate沒法取到新值，只好單獨寫一個
	 */
	private boolean validateTxtEndDate(ProjectBudgetVo projectBudgetVo){
		
		
		Date txtEndDate = projectBudgetVo.getBo().getEndDate();
		Date txtStartDate = projectBudgetVo.getBo().getStartDate();
		if(txtStartDate.before(txtEndDate)){
			return true;
		}else{
			return false;
		}
	}

	@Override
	protected void initUpdatingData(ValueObject<ProjectBudget> updatingData) {
		// TODO Auto-generated method stub
		
	}
	
	//--------------------//
	//是否顯示訊息
	private boolean renderInfo=false;
	//訊息主體
	private String renderMessage;
	
	private Map<String, BudgetYear> budgetYearMap = null;
	private List<SelectItem> budgetYearList = null;
	
	private Map<String, BfmDepartment> departmentMap = null;
	private List<SelectItem> departmentList = null;
	
	//判斷是否為第一次讀取
	private boolean firstLoad =false;
	
	
	
	public boolean isFirstLoad() {
		return firstLoad;
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
	
	
	public List<SelectItem> getBudgetYearList() {
		if (this.budgetYearList == null) {
			this.budgetYearList = BfmSelectFactory.creatFirstSecondYearBudgetYearOrderByYearDescSelect().getSelectList();
			this.budgetYearMap = (Map<String, BudgetYear>)FacesUtils.getSessionScope().get("firstSecondBudgetYearMap");
		}
		return budgetYearList;

	}

	public List<SelectItem> getDepartmentList() {
		if (this.selectBudgetYearId!=null&&departmentList==null) {
			//抓取目前登入人員的資料
			User user = (User) AAUtils.getLoggedInUser();
			this.departmentList = BfmSelectFactory.creatDepartmentByYearForProjectBudgetNormalBudget(budgetYearMap.get(selectBudgetYearId), user).getSelectList();
			this.departmentMap = (Map<String, BfmDepartment>)FacesUtils.getSessionScope().get("departmentMap");
			//非第一次讀取，且未找到任何可選的編列單位
			if((this.isFirstLoad()==false)&&(departmentMap.size()<=0)){
		
				ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
				this.renderInfo = true;
				this.setRenderMessage(bundle.getString("tw_com_skl_bfm_kernel_model5_web_HumanAllocation_noRightWarn"));
			
			}
		}else if(departmentList==null){
			this.departmentList=new ArrayList<SelectItem>();
			this.departmentList.add(BfmSelectHelper.EMPTY_SELECTITEM);
			this.firstLoad = false;
			
			
		}
		return departmentList;

	}
	
	public void onBudgetYearChange() {
		this.departmentList= null;
		this.renderInfo = false;
		
	}
	/**
	 * 確定年度與編列單位 
	 * @return
	 */
	public String doBtnConfrimAction(){
		//因為訊息可能跑到下一頁，所以都先設為false
		this.renderInfo = false;
		BudgetYear budgetYear = budgetYearMap.get(this.getSelectBudgetYearId());
		BfmDepartment department=departmentMap.get(this.getSelectDepartmentId());
		//抓取目前登入人員的資料
		User user = (User) AAUtils.getLoggedInUser();
		
		//修改查詢cache問題 CU3178 2018/1/22 START
		//讀取選擇年度的預算表
		//Budget lazyBudget = department.readBudgetByYear(budgetYear.getYear());

		//判斷該預算表的狀態是否允許使用專案費用編制，因為原本的效能不好，所以修改成再讀取一次budget
		//Budget budget = this.getBudgetService().readBudgetByBudget(lazyBudget);
		Budget budget = this.getBudgetService().readBudgetByDepartmentAndYear(department,budgetYear.getYear());
		//修改查詢cache問題 CU3178 2018/1/22 END
		
		boolean permit = budget.getBudgetStateType().isHasFunctionFun0202();
				
		if ((permit == true)
				|| ((permit == false)
						&& (Integer.parseInt(budget.getBudgetStateType()
								.getId()) < 4) && ((BfmGroupCode.COMPLEX_PLANNING
						.getCode().equals(user.getBfmGroup().getCode())) || (BfmGroupCode.ADMIN
						.getCode().equals(user.getBfmGroup().getCode()))))) {
			//設定要ProjectBudgetManagedBean的BudgetId
			this.selectBudgetId=budget.getId().toString();
			return "readProjectBudget";
		}else{
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			this.renderInfo = true;
			this.setRenderMessage(bundle.getString("tw_com_skl_bfm_kernel_model5_web_ProjectBudget_warn"));
		}

		return "confirmDepartmentAndYearForProjectBudget";
	}
	
	//RE201801038_預算同期差異說明 CU3178 2018/4/19 START
	/**
	 * 續編專案代號下拉式選單
	 * @return
	 */
	public List<SelectItem> getProjectbudgetList() {				
		if (this.continueProjectbudgetList == null && this.selectBudgetYearId != null && this.selectDepartmentId != null) {
			this.continueProjectbudgetList = new ArrayList<SelectItem>();
			this.continueProjectbudgetList.add(BfmSelectHelper.EMPTY_SELECTITEM);
			
			//將頁面選取年度-1
			BudgetYear lastBudgetYear = getBudgetFacade().getBudgetYearService().readBudgetYearByYear(budgetYearMap.get(selectBudgetYearId).getYear()-1);

			//依頁面選取年度-1及編列單位查詢出上一年度已編列專案代號												
			List<ProjectBudget> projectBudgetList = getBudgetFacade().getProjectBudgetService()
					.readProjectBudgetByYearAndDep(lastBudgetYear,
							departmentMap.get(selectDepartmentId));		
			for(ProjectBudget bo :projectBudgetList){
				//專案代號顯示方式:專案代號+"-"+專案名稱
				SelectItem ietm = new SelectItem(bo,bo.getCode()+"-"+bo.getName());
				this.continueProjectbudgetList.add(ietm);
			}
		}else if (continueProjectbudgetList == null) {
			this.continueProjectbudgetList = new ArrayList<SelectItem>();
			this.continueProjectbudgetList.add(BfmSelectHelper.EMPTY_SELECTITEM);
		}		
		return continueProjectbudgetList;
	}
	
	/**
	 * [新增專案預算]按鈕
	 * @return
	 */
	public String doBtnCreateAction(){
		//設定不為續編專案
		setCommandContinue(false);
		return super.doBtnCreate1Action();
	}
	
	
	/**
	 * [續編專案代號]按鈕
	 * @return
	 */
	public String doBtnContinueAction(){
		return "continue";
	}
	
	/**
	 * 續編專案代號頁面-[確認]按鈕
	 * @return
	 */
	public String doBtnConfirmProjectAction(){
		//檢核續編專案欄位須必填，若否，則顯示《續編專案代號不可為空!》
		if(getContinueProjectBudget()==null){
			throw new ExpRuntimeException(ErrorCode.E00010);
		}
		List<BudgetDiffExplain> list = getBudgetFacade().getBudgetDiffExplainService().findBudgetByLastProjectId(getContinueProjectBudget());
		//依續編專案代號查詢是否已有續編
        if (CollectionUtils.isNotEmpty(list)) {
        	throw new ExpRuntimeException(ErrorCode.E00011);
        }
		//設定為續編專案
		setCommandContinue(true);
		return super.doBtnCreate1Action();
	}
	
	/**
	 * 續編專案代號
	 * @return
	 */
	public ProjectBudget getContinueProjectBudget() {
		return continueProjectBudget;
	}

	/**
	 * 續編專案代號
	 * @param sequelProjectBudget
	 */
	public void setContinueProjectBudget(ProjectBudget continueProjectBudget) {
		this.continueProjectBudget = continueProjectBudget;
	}
	
	/**
	 * 是否為續編專案
	 * @return
	 */
	public boolean isCommandContinue() {
		return commandContinue;
	}

	/**
	 * 是否為續編專案
	 * @param commandContinue
	 */
	public void setCommandContinue(boolean commandContinue) {
		this.commandContinue = commandContinue;
	}
	
	private BudgetFacade getBudgetFacade() {
		return (BudgetFacade) ApplicationLocator.getBean("budgetFacade");
	}
	//RE201801038_預算同期差異說明 CU3178 2018/4/19 END


}

class ProjectBudgetWrapper extends VoWrapperImpl{
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public ProjectBudgetVo wrap(Object object) {
		return new ProjectBudgetVo((ProjectBudget)object);
	}
}

/**
 * Inner Class for override PagedOperation
 * @author 
 *
 */
class ProjectBudgetPagedOperationImpl extends PagedOperationImpl {

	private static final long serialVersionUID = 1L;
	 
	private BudgetYear budgetYear = null;
	private BfmDepartment department = null;
	
	public ProjectBudgetPagedOperationImpl(final String serviceName, final BudgetYear budgetYear, final BfmDepartment department) {    	
		super(serviceName);
        this.budgetYear = budgetYear;
        this.department = department;
        
    }    
	
	public List read(int firstResult, int maxResults) {
		firstResult = 0;
		maxResults = readCount();
		System.out.println("firstResult="+firstResult);
		System.out.println("maxResults="+maxResults);
		Set<ProjectBudget> projectBudgetSet = ((ProjectBudgetService)this.getPagedService()).readByDepartmentAndyYearForPrjoectBudget(this.getDepartment(), this.getBudgetYear(), BfmBudgetType.PROJECT_EXPENSE_TYPE, firstResult, maxResults);
        List<ProjectBudget> projectBudgetList=new ArrayList<ProjectBudget>();
        for(ProjectBudget projectBudget : projectBudgetSet){
        	projectBudgetList.add(projectBudget);	        	
        }
		return projectBudgetList;
    }

	public int readCount() {
		int rowCount=0;
		rowCount = ((ProjectBudgetService)this.getPagedService()).readByDepartmentAndyYearForPrjoectBudgetCount(this.getDepartment(), this.getBudgetYear(), BfmBudgetType.PROJECT_EXPENSE_TYPE);
        return rowCount;
    }

	public BudgetYear getBudgetYear() {
		return budgetYear;
	}

	public BfmDepartment getDepartment() {
		return department;
	}
	
	
}	