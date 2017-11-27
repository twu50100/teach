package tw.com.skl.exp.web.jsf.managed.bd.provisionbudget;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;

import tw.com.skl.common.model6.web.jsf.managedbean.PagedOperation;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.PagedOperationImpl;
import tw.com.skl.common.model6.web.jsf.managedbean.impl.TempPagedManagedBeanImpl;
import tw.com.skl.common.model6.web.jsf.utils.FacesUtils;
import tw.com.skl.common.model6.web.util.ApplicationLocator;
import tw.com.skl.common.model6.web.util.MessageManager;
import tw.com.skl.common.model6.web.vo.ValueObject;
import tw.com.skl.common.model6.web.vo.impl.VoWrapperImpl;
import tw.com.skl.exp.kernel.model6.bo.BfmBudgetItem;
import tw.com.skl.exp.kernel.model6.bo.BfmBudgetType;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartment;
import tw.com.skl.exp.kernel.model6.bo.BfmGroup.BfmGroupCode;
import tw.com.skl.exp.kernel.model6.bo.Budget;
import tw.com.skl.exp.kernel.model6.bo.BudgetItemLargeType;
import tw.com.skl.exp.kernel.model6.bo.BudgetItemType;
import tw.com.skl.exp.kernel.model6.bo.BudgetYear;
import tw.com.skl.exp.kernel.model6.bo.GeneralExp;
import tw.com.skl.exp.kernel.model6.bo.Group.GroupCode;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudget;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudgetItem;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.util.AAUtils;
import tw.com.skl.exp.kernel.model6.logic.BfmBudgetItemService;
import tw.com.skl.exp.kernel.model6.logic.BudgetService;
import tw.com.skl.exp.kernel.model6.logic.ProjectBudgetItemService;
import tw.com.skl.exp.kernel.model6.logic.ProjectBudgetService;
import tw.com.skl.exp.web.util.BfmSelectFactory;
import tw.com.skl.exp.web.util.BfmSelectHelper;
import tw.com.skl.exp.web.util.BudgetItemLargeTypeSelectHelper;
import tw.com.skl.exp.web.vo.ProjectBudgetItemVo;

/**
 * E01.01	一般行政費用
 * @author CU3178
 *
 */
public class NormalBudgetManagedBean  extends TempPagedManagedBeanImpl
	<ProjectBudgetItem, ProjectBudgetItemService>{

	private static final long serialVersionUID = 1L;
	
	//會由ProjectBudget_ConfirmYearDepartmentManagedBean所注入的
	private String selectBudgetYearId;
	private String selectDepartmentId;	
	private String selectBudgetId;
	
	// master specific begin
	private BudgetItemLargeTypeSelectHelper budgetItemLargeTypeSelectHelper = new BudgetItemLargeTypeSelectHelper();
	
	private Map<String,BudgetItemLargeType> budgetItemLargeTypeMap = null;
	private List<SelectItem> budgetItemLargeTypeList = null;
	
	private Map<String,BudgetItemType> budgetItemTypeMap = null;
	private List<SelectItem> budgetItemTypeList = null;
	
	private Map<String, BfmBudgetItem> budgetItemMap = null;
	private List<SelectItem> budgetItemList = null;
	
	private ProjectBudgetService projectBudgetService;
	private BfmBudgetItemService budgetItemService;
	private BudgetService budgetService;	
	private Budget budget;
	private BudgetYear budgetYear;
	private BfmDepartment department;
	private ProjectBudget projectBudget;
	//僅用在update頁面
	private BfmBudgetItem updateBudgetItem; 
	//判斷是否進入修改頁面
	private boolean isInUpdatePage = false;
	
	public NormalBudgetManagedBean() {
		this.setVoWrapper(new NormalBudgetItemWrapper());
		this.setServiceName("projectBudgetItemService");
	}

	
	@Override
	protected void initUpdatingData(ValueObject<ProjectBudgetItem> updatingData) {
	}
	
	// master specific begin
	@Override
	protected void initCreatingData() {
		ProjectBudgetItemVo projectBudgetItemVo = new ProjectBudgetItemVo();
		projectBudgetItemVo.setBo(new ProjectBudgetItem());
		this.setUpdatingData(projectBudgetItemVo);
		//取得使用者欲查詢的BudgetYear、Department、ProjectBudget
		BudgetYear budgetYear = this.getBudgetYear();
		BfmDepartment department = this.getDepartment();
		this.setupBudgetAndProjectBudget();
	}
	// master specific end

	public BudgetYear getBudgetYear() {
		if(this.budgetYear==null){
			this.budgetYear = ((Map<String, BudgetYear>)FacesUtils.getSessionScope().get("firstSecondBudgetYearMap")).get(this.getSelectBudgetYearId());
		}
		return this.budgetYear;
	}
	
	public BfmDepartment getDepartment() {
		if(this.department==null){
			this.department = ((Map<String, BfmDepartment>)FacesUtils.getSessionScope().get("departmentMap")).get(this.getSelectDepartmentId());
		}
		return this.department;
	}
	
	public void setupBudgetAndProjectBudget() {
		if(this.budget==null){
			//取一個乾淨的budget
			this.budget = this.getBudgetService().findByPK(this.getSelectBudgetId());
			this.projectBudget = this.getProjectBudgetService().readByBudgetAndBudgetType(this.budget,BfmBudgetType.NORMAL_EXPENSE_TYPE);
		}
	}
	
	public ProjectBudgetItemVo wrap(Object object) {
		return (ProjectBudgetItemVo)this.getVoWrapper().wrap((ProjectBudgetItem) object);
	}

	/**
	 * getUpdatingData 和 setUpdatingData 其實可以不需要, 因為 super class 已經有了
	 * 存在的目的, 只是為了讓 IDE 知道它的確切的type為何, 讓 jsf 的頁面比較好拖拉
	 */
	@Override
	public ProjectBudgetItemVo getUpdatingData() {
		return (ProjectBudgetItemVo)super.getUpdatingData();
	}
	
	@Override
	public String doBtnTempCreateAction(){
		/*關聯為一般行政專案*/
		//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START
		validateText(this.getUpdatingData());
		//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END
		this.getUpdatingData().getBo().setProjectBudget(this.projectBudget);
		this.getUpdatingData().getBo().setBudgetItem(budgetItemMap.get(this.getUpdatingData().getSelectBudgetItemId()));
		//System.out.println("bean.create============ "+budgetItemMap.get(this.getUpdatingData().getSelectBudgetItemId()).getName());
		return super.doBtnTempCreateAction();
	}

	@Override
	public String doBtnTempUpdateAction(){
		//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START
		validateText(this.getUpdatingData());
		//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END
		this.getUpdatingData().getBo().setBudgetItem(budgetItemMap.get(this.getUpdatingData().getSelectBudgetItemId()));
		//System.out.println("bean.update============ "+budgetItemMap.get(this.getUpdatingData().getSelectBudgetItemId()).getName());
		isInUpdatePage = false;
		return super.doBtnTempUpdateAction();
	}
	
	//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 START

	/**
	 * @author 文珊
	 * 驗證勾選BSC後必填BSC策略編號,且策略編號長度不能大於10
	 */
	private void validateText(ProjectBudgetItemVo projectBudgetItemVo){
		
		String targetNotation = projectBudgetItemVo.getBo().getTargetNotation();

		String budgetNotation = projectBudgetItemVo.getBo().getBudgetNotation();
		
		this.getUpdatingData().getBo().setTargetNotation(targetNotation.replace("\"", ""));
		this.getUpdatingData().getBo().setBudgetNotation(budgetNotation.replace("\"", ""));

	}	
	//RE201502532_預算系統調整及撈取104年1-7月實支 CU3178 2015/7/24 END
	
	/**
	 * 進入update頁面時，取得budgetItemType,budgetItemType兩個下拉選單並給定值
	 */
	@Override
	public String doLnkUpdateAction(){
		
		isInUpdatePage = true;
		
		String result = super.doLnkUpdateAction();		
		updateBudgetItem = this.getBudgetItemService().findByPK(this.getUpdatingData().getSelectBudgetItemId());		
		return super.doLnkUpdateAction();
	}
	
	/**
	 * 進入新增PROJECT_BUDGET_ITEM頁面時，將budgetItemType,budgetItem 等三個下拉選單初始化
	 */
	@Override
	public String doBtnCreate1Action(){
		budgetItemLargeTypeList = null;
		budgetItemTypeList = null;
		budgetItemList = null;
		
		return super.doBtnCreate1Action();
	}
	
	public void setUpdatingData(ProjectBudgetItemVo vo) {
		super.setUpdatingData(vo);
	}
	
	protected void setupUpdatingData() {
	}
	
	
	public Budget getBudget(){
		return this.budget;
	}
	
	/**
	 * 回到下拉選單頁
	 * */
	public String doBtnReturnAction(){
		FacesUtils.getSessionScope().remove("projectBudget_ConfirmYearDepartmentManagedBean");
		FacesUtils.getSessionScope().remove("budgetItemLargeTypeMap");
		FacesUtils.getSessionScope().remove("budgetItemTypeMap");
		FacesUtils.getSessionScope().remove("budgetItemMap");
		FacesUtils.getSessionScope().remove("NormalBudgetManagedBean");
		FacesUtils.getSessionScope().remove("departmentMap");
		FacesUtils.getSessionScope().remove("budgetYearMap");
		return "confirmDepartmentAndYearForNormalBudget";
	}
	
	@Override
	public String doBtnCancelAction() {	
		isInUpdatePage = false;		
		return this.getREAD_PAGE() + this.getReturnPostFix();
	}
	
	public String getSelectDepartmentId() {
		return selectDepartmentId;
	}

	public void setSelectDepartmentId(String selectDepartmentId) {
		this.selectDepartmentId = selectDepartmentId;
	}
	
	public String getSelectBudgetYearId() {
		return selectBudgetYearId;
	}

	public void setSelectBudgetYearId(String selectBudgetYearId) {
		this.selectBudgetYearId = selectBudgetYearId;
	}

	public void onSelectItemLargeTypeChange(){
		this.budgetItemTypeList = null;
		this.budgetItemList = null;
	}
	
	public void onSelectItemTypeChange(){
		this.budgetItemList = null;
	}
	
	// manyToOne selection menu begin

	public List<SelectItem> getBudgetItemLargeTypeList() {
		if(budgetItemLargeTypeList == null){			
			budgetItemLargeTypeList = budgetItemLargeTypeSelectHelper.getBudgetItemLargeTypeSelect();			
			budgetItemLargeTypeMap = (Map<String, BudgetItemLargeType>)FacesUtils.getSessionScope().get("budgetItemLargeTypeMap");			
		}
		
	
		return budgetItemLargeTypeList;
	}
	
	/**
	 * 不能為預備金,當進入update頁面時需取得包含欲修改的BudgetItemType的List
	 */
	public List<SelectItem> getBudgetItemTypeList() {
		if(!isInUpdatePage){			
			if(budgetItemTypeList == null && this.getUpdatingData().getSelectBudgetItemLargeTypeId() != null){				
				BudgetItemLargeType bILT = budgetItemLargeTypeMap.get(this.getUpdatingData().getSelectBudgetItemLargeTypeId());
				budgetItemTypeList =  BfmSelectFactory.creatBudgetTypeItemForLargeType(bILT).getSelectList();			
				budgetItemTypeMap = (Map<String, BudgetItemType>)FacesUtils.getSessionScope().get("budgetItemTypeMap");
			}else if(budgetItemTypeList == null){				
				this.budgetItemTypeList = new ArrayList<SelectItem>();
				this.budgetItemTypeList.add(BfmSelectHelper.EMPTY_SELECTITEM);
			}
		}else{
			if(updateBudgetItem != null){
				this.setBudgetItemTypeList(BfmSelectFactory.creatBudgetTypeItemForLargeType(updateBudgetItem.getBudgetItemType().getBudgetItemLargeType()).getSelectList());
				budgetItemTypeMap = (HashMap<String, BudgetItemType>)FacesUtils.getSessionScope().get("budgetItemTypeMap");
			}
		}
		return budgetItemTypeList;
	}
	
	/**
	 * 不能為預備金,當進入update頁面時需取得包含欲修改的BudgetItem的List(由doLnkUpdateAction()取得)
	 */
	public List<SelectItem> getBudgetItemList() {
		if(!isInUpdatePage){			
			if (budgetItemList == null && this.getUpdatingData().getSelectBudgetItemLargeTypeId() != null && this.getUpdatingData().getSelectBudgetItemTypeId() != null) {				
				BudgetItemType bIT = budgetItemTypeMap.get(this.getUpdatingData().getSelectBudgetItemTypeId());
				budgetItemList = BfmSelectFactory.creatBudgetItemForNormalBudget(bIT).getSelectList();			
				budgetItemMap = (HashMap<String, BfmBudgetItem>)FacesUtils.getSessionScope().get("budgetItemMap");
				
			}else if(budgetItemList == null){
				this.budgetItemList = new ArrayList<SelectItem>();
				this.budgetItemList.add(BfmSelectHelper.EMPTY_SELECTITEM);
			}
		}else{
			if(updateBudgetItem != null){
				this.setBudgetItemList(BfmSelectFactory.creatBudgetItemForNormalBudget(updateBudgetItem.getBudgetItemType()).getSelectList());
				budgetItemMap = (HashMap<String, BfmBudgetItem>)FacesUtils.getSessionScope().put("budgetItemMap", budgetItemMap);
				isInUpdatePage = false;
			}
		}
		return budgetItemList;
	}

	private void setupBudgetItem() {
		if (this.getUpdatingData().getSelectBudgetItemId() != null && budgetItemMap != null) {
			this.getUpdatingData().getBo().setBudgetItem
				(budgetItemMap.get(this.getUpdatingData().getSelectBudgetItemId()));
		}
	}
	// manyToOne selection menu end
	
	public ProjectBudgetService getProjectBudgetService() {		
		return (ProjectBudgetService) ApplicationLocator.getBean("projectBudgetService");
	}
	
	public void setProjectBudgetService (ProjectBudgetService projectBudgetService) {
		this.projectBudgetService = projectBudgetService;
	}
	
	public BfmBudgetItemService getBudgetItemService() {
		return (BfmBudgetItemService) ApplicationLocator.getBean("bfmBudgetItemService");
	}

	public void setBudgetItemService (BfmBudgetItemService budgetItemService) {
		this.budgetItemService = budgetItemService;
	}
	
	@Override
	protected PagedOperation getPagedOperation() {
		if (pagedOperation == null) {
			BudgetYear budgetYear = this.getBudgetYear();
			BfmDepartment department = this.getDepartment();
			pagedOperation = new NormalBudgetPagedOperationImpl(this.getServiceName(),department,budgetYear);
		}
		return this.pagedOperation;
	}

	public String getSelectBudgetId() {
		return selectBudgetId;
	}

	public void setSelectBudgetId(String selectBudgetId) {
		this.selectBudgetId = selectBudgetId;
	}
	
	public BudgetService getBudgetService() {
		return (BudgetService) ApplicationLocator.getBean("budgetService");
	}

	public void setBudgetService(BudgetService budgetService) {
		this.budgetService = budgetService;
	}

	public ProjectBudget getProjectBudget() {
		return projectBudget;
	}

	/**
	 * @author 偉哲
	 * 驗證專案預算項目在修改新增時，該預算項是否已經存在，如存在則提出警訊
	 * 
	 * @param context
	 * @param component
	 * @param object
	 * @throws ValidatorException
	 */
	public void validateBudgetItem(FacesContext context, UIComponent component, Object object)
			throws ValidatorException {
		//目前的新值
		String selectBudgetItemId = (String) object;
		//假如是新增的話vo.getUpdatingData().getSelectBudgetItemId()會是null，所以不用驗證，沒有修改選擇的預算項目的話也不用驗證
		if((this.getUpdatingData().getSelectBudgetItemId()==null)||(this.getUpdatingData().getSelectBudgetItemId().compareTo(selectBudgetItemId)!=0)){
			if(this.getPagedDataModel()!=null){
				List<ProjectBudgetItemVo> projectBudgetItemVoList = (List)this.getPagedDataModel().getWrappedData();
				for(ProjectBudgetItemVo projectBudgetItemVo : projectBudgetItemVoList){
					if(selectBudgetItemId.compareTo(projectBudgetItemVo.getSelectBudgetItemId().toString())==0){
						//MessageManager.getInstance().showErrorMessage("validate_projectBudget_already_exist");
						String message = MessageManager.getInstance().getMessage("validate_projectBudget_already_exist");
						FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
						throw new ValidatorException(facesMessage);
					}
				}
			}
		}
	}

	public void setBudgetItemTypeList(List<SelectItem> budgetItemTypeList) {
		this.budgetItemTypeList = budgetItemTypeList;
	}

	public void setBudgetItemList(List<SelectItem> budgetItemList) {
		this.budgetItemList = budgetItemList;
	}

	
	/**
	 * 判斷是否進入修改頁面
	 * @return
	 */
	public boolean isInUpdatePage() {
		return isInUpdatePage;
	}

	public void setInUpdatePage(boolean isInUpdatePage) {
		this.isInUpdatePage = isInUpdatePage;
	}
	
	//----------------------------------------------//
//	是否顯示訊息
	private boolean renderInfo=false;
	//訊息主體
	private String renderMessage;
	

	private Map<String, BudgetYear> budgetYearMap = null;
	private List<SelectItem> budgetYearList = null;
	

	private Map<String, BfmDepartment> departmentMap = null;
	private List<SelectItem> departmentList = null;
	//之後要用ID注入到ProjectBudgetManagedBean的budgetId

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
	 * FUN2.1
	 * 確定年度與編列單位 
	 * @return
	 */
	public String doBtnConfrimActionForNormalBudget(){
		//因為訊息可能跑到下一頁，所以都先設為false
		this.renderInfo = false;
		BudgetYear budgetYear = budgetYearMap.get(this.getSelectBudgetYearId());
		BfmDepartment department=departmentMap.get(this.getSelectDepartmentId());
		//抓取目前登入人員的資料
		User user = (User) AAUtils.getLoggedInUser();
		
		//讀取選擇年度的預算表，因為原本的效能不好，所以修改成再讀取一次budget
		Budget lazyBudget = department.readBudgetByYear(budgetYear.getYear());
		Budget budget = this.getBudgetService().readBudgetByDepartmentAndYear(department,budgetYear.getYear());

		//判斷該預算表的狀態是否允許使用預算審查
		boolean permit = budget.getBudgetStateType().isHasFunctionFun0201();
		if ((permit == true)
				|| ((permit == false)
						&& (Integer.parseInt(budget.getBudgetStateType()
								.getId()) < 4) && ((BfmGroupCode.COMPLEX_PLANNING
						.getCode().equals(user.getBfmGroup().getCode())) || (BfmGroupCode.ADMIN
						.getCode().equals(user.getBfmGroup().getCode()))))) {
		//設定要NormalBudgetManagedBean的BudgetId
			this.selectBudgetId=budget.getId().toString();
			return "readNormalBudget";
		}else{
			ResourceBundle bundle = ResourceBundle.getBundle("applicationResources", FacesUtils.getFacesContext().getViewRoot().getLocale());
			this.renderInfo = true;
			this.setRenderMessage(bundle.getString("tw_com_skl_bfm_kernel_model5_web_NormalBudget_warn"));
		}

		return "confirmDepartmentAndYearForNormalBudget";
	}
}

class NormalBudgetItemWrapper extends VoWrapperImpl{
	
	private static final long serialVersionUID = 1L;
	
	public ProjectBudgetItemVo wrap(Object object) {
		return new ProjectBudgetItemVo((ProjectBudgetItem)object);
	}
}

/**
 * Inner Class for override PagedDataModel
 * @author 
 *
 */
class NormalBudgetPagedOperationImpl extends PagedOperationImpl {

	private static final long serialVersionUID = 1L; 
	private String departmentId;
	private Integer year;
	
	public NormalBudgetPagedOperationImpl(){
	}
	
	public NormalBudgetPagedOperationImpl(final String serviceName,final BfmDepartment department, final BudgetYear budgetYear){
		super(serviceName);
		departmentId = department.getId();
		year = budgetYear.getYear();
	}    
	
    public List read(int firstResult,int maxResults) {
    	firstResult = 0;
		maxResults = readCount();
        return ((ProjectBudgetItemService)this.getPagedService()).readNormalBudgetByDepartmentIdYearBudgetType(firstResult, maxResults,Long.parseLong(this.getDepartmentId()), this.getYear(), BfmBudgetType.NORMAL_EXPENSE_TYPE);
    }

    public int readCount() {
    	//return pList.size();
    	int rowCount=0;
		rowCount = ((ProjectBudgetItemService)this.getPagedService()).readByDepartmentAndyYearForNormalBudgetCount(Long.parseLong(this.getDepartmentId()), this.getYear(), BfmBudgetType.NORMAL_EXPENSE_TYPE);
        return rowCount;
    }

	public String getDepartmentId() {
		return departmentId;
	}

	public Integer getYear() {
		return year;
	}
	
}