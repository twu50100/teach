package tw.com.skl.exp.kernel.model6.dao.jpa;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.orm.jpa.JpaCallback;

import tw.com.skl.common.model6.dao.jpa.BaseDaoImpl;
import tw.com.skl.exp.kernel.model6.bo.BfmBudgetItem;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartment;
import tw.com.skl.exp.kernel.model6.bo.BfmDepartmentType;
import tw.com.skl.exp.kernel.model6.bo.Budget;
import tw.com.skl.exp.kernel.model6.bo.BudgetStateType;
import tw.com.skl.exp.kernel.model6.bo.BudgetYear;
import tw.com.skl.exp.kernel.model6.bo.DepartmentLargeType;
import tw.com.skl.exp.kernel.model6.bo.ProjectBudget;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.dao.BudgetDao;


public class BudgetDaoImpl extends  BaseDaoImpl<Budget,String>
		implements BudgetDao {

	public BudgetDaoImpl() {
		super(Budget.class);
	}
	
	/**FUN 1.1 更新子層預算表為2
	 * @param parentBudget
	 * @return
	 */
//	public Budget updateChildrenBudgetsBudgetStateType(final Budget parentBudget){
//		
//		session.lock(parentBudget, LockMode.NONE);
//		Hibernate.initialize(parentBudget.getChildrenBudgets());
//		this.releaseSession(session);
//		
//		if(parentBudget.getChildrenBudgets().size()>0){
//			for(Budget childrenBudget : parentBudget.getChildrenBudgets()){
//				childrenBudget.setBudgetStateType(BudgetStateType.MAKE_BUDGET);
//				this.update(childrenBudget);
//				childrenBudget = this.updateChildrenBudgetsBudgetStateType(childrenBudget);
//			}
//		}
//		
//		return parentBudget;
//	} 

	/**
	 * Fun 2.1,Fun2.2
	 * @author 偉哲
	 * 依據傳入的預算，來查詢預算，主要是查詢到是否允許編列的資料	 * 
	 * 
	 * @param budget
	 * @return
	 */
	// RE201702268_新增醫檢轉入資料欄位 ec0416 20171106 start
	public Budget readBudgetByBudget(Budget budget){
		
		// RE201702268_新增醫檢轉入資料欄位 ec0416 20171106 start
	    StringBuffer queryString = new StringBuffer();
        queryString.append(" select*from tbbfm_budgets b ");
        queryString.append(" inner join tbbfm_departments dep on dep.id=b.tbbfm_departments_id ");
        queryString.append(" inner join TBBFM_BUDGET_STATE_TYPES t on t.id=b.tbbfm_budget_state_types_id ");
        queryString.append(" left join Tbbfm_FUNCTIONs f on f.id=t.id ");
        queryString.append(" where b.id= '").append(budget.getId()).append("'");
        
        execNativeSQL(queryString.toString(), null);
		return budget;
    
    	
//		String queryString ="SELECT budget FROM Budget budget											"+
//				"	join budget.department department	"+
//				"	join budget.budgetStateType	budgetStateType  "+
//				"	left join budgetStateType.functions functions	"+
//				"WHERE														"+ 
//				"	budget.id=:budgetId		 									";
//
//		Map<String, Object> params= new HashMap<String, Object>();
//		params.put("budgetId", budget.getId());
//	
//		return findByNamedParamsUnique(queryString.toString(), params);
		// RE201702268_新增醫檢轉入資料欄位 ec0416 20171106 end
	}
	
	// RE201702268_新增醫檢轉入資料欄位 ec0416 20171106 start
	 private Object execNativeSQL(final String sqlString, final List<Object> parameters) {
	        return getJpaTemplate().execute(new JpaCallback() {
	            public Object doInJpa(EntityManager em) throws PersistenceException {
	                Query queryObject = em.createNativeQuery(sqlString);
	                return queryObject.executeUpdate();
	            }
	        });
	    }
	// RE201702268_新增醫檢轉入資料欄位 ec0416 20171106 end
	 
	/**
	 * Fun2.3 部室人力配置
	 * @author 芷筠
	 * 依據部門和年份, 傳回Budget
	 * @param department
	 * @param year
	 */
	public Budget readByDepartmentAndYear(BfmDepartment department, int year){
		Budget budget = null;
		String[] fields = {"department", "year", "dropped"};
		Object[] values = {department, year, false};			
		List<Budget> budgetList = this.readByFields(fields, values);
		Iterator<Budget> iter = budgetList.iterator();
		if (iter.hasNext()) {			
			budget = this.readInitializeBudgetStateType(iter.next());			
		}
		return budget;
	}
	
	/**
	 * Fun2.3 部室人力配置
	 * @author 芷筠
	 * 依據部門和年份, 傳回該部門及其子層部門Budget
	 * dropped=false
	 * @param department
	 * @param year
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readByDepartmentAndYearIncludeChildren(BfmDepartment department, int year){		
		String queryString ="SELECT budget FROM Budget budget											"+
							"	left join budget.department department		"+
							"	left join department.parentDepartment parentDepartment  "+
							"   left join budget.budgetStateType budgetStateType		"+												
							"WHERE														"+ 
							"	budget.year=:year AND 									"+ 
							"	budget.dropped=false AND								"+
							"	(department.id=:departmentId OR							"+
							"	parentDepartment.id=:departmentId) 		"+
//							"	( 														"+
//							"		(													"+
//							"			department.dropped=0 AND 					"+ 
//							"			department.droppedDate IS null					"+
//							"		) OR												"+ 
//							"		(													"+
//							"			budget.year<= SUBSTRING(department.droppedDate,1,4) 		"+
//							"		)													"+
//							"	)														"+		
							"ORDER BY department.id 									";

		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", year);
		params.put("departmentId", department.getId());		

		return findByNamedParams(queryString.toString(), params);
	}

	/**
	 * Fun2.3 部室人力配置
	 * 
	 * @author 芷筠 依據部門和年份, 傳回該部門及但不包含子層部門Budget 
	 * dropped=false
	 * @param department
	 * @param year
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readByDepartmentAndYearWithoutChildren(BfmDepartment department,
			int year){
		String queryString ="SELECT budget FROM Budget budget						"+
		"	join budget.department department		"+
		"	join budget.budgetStateType budgetStateType "+		
		"WHERE														"+ 
		"	budget.year=:year AND 									"+ 
		"	budget.dropped=false AND								"+
		"	department.id=:departmentId 							";
//		"	( 														"+
//		"		(													"+
//		"			department.dropped=0 AND 					"+ 
//		"			department.droppedDate IS null					"+
//		"		) OR												"+ 
//		"		(													"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 		"+
//		"		)													"+
//		"	)														";

		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", year);
		params.put("departmentId", department.getId());		

		return findByNamedParams(queryString.toString(), params);		
	}	
	
	/**
	 * FUN3.2
	 * 找出
	 * 1.同一DepartmentLargeType
	 * 2.部級Budget
	 * 3.預算狀態為3 or 8
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readBudgetByTheSameDepLargeType(DepartmentLargeType depLarType){
		String queryString ="SELECT budget FROM Budget budget													"+
			"	join budget.department department								"+
			"	join department.departmentLargeType departmentLargeType		"+
			"	join budget.budgetStateType budgetStateType              		"+
			"WHERE  																			"+
			"	budget.dropped = false AND														"+			
//			"	(																				"+
//			"		(budget.department.dropped=0 AND budget.department.droppedDate IS null) OR "+
//			"		(budget.year<= SUBSTRING(department.droppedDate,1,4))								"+
//			"	)AND 																			"+
			"	departmentLargeType.id = :departmentLargeTypeId AND  							"+
			"	(																				"+
			"		(budgetStateType.id = :budgetStateTypeId1 ) OR                              "+
			"		(budgetStateType.id = :budgetStateTypeId2 )							        "+
			"	)        AND                                                                    "+
			"	budget.department.departmentType.id = :departmentTypeId 						"+
			"ORDER BY budget.id																	";

		Map<String, Object> params= new HashMap<String, Object>();
		params.put("departmentLargeTypeId", depLarType.getId());
		params.put("budgetStateTypeId1", BudgetStateType.AUDIT_BUDGET.getId());
		params.put("budgetStateTypeId2", BudgetStateType.REAUDIT_BUDGET.getId());
		params.put("departmentTypeId", BfmDepartmentType.SECTION_DEPARTMENT_TYPE.getId());
		
		return findByNamedParams(queryString.toString(), params);	
	}

	/**
	 * Fun5.1 
	 * 1.依年度查詢出可上傳的預算 
	 * 2.BudgetStateType=(5、6、10、12)
	 * 3.Department的是否上傳大電腦為true 
	 * 4.Department為部級
	 * 5.預算表的dropped必須為false
	 * 6.編列單位未被裁撤且編列單位的裁撤日為null 或 預算的年度小於等於編列單位裁撤日的年度
	 * 7.預算表的年度=查詢年度
	 * 8.departmentLargeType in (1, 2, 3, 6)
	 * 
	 * @偉哲
	 * @param year
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readCouldUploadBudgetByYear(final BudgetYear budgetYear){
		String queryString ="SELECT budget FROM Budget budget																	"+
							"	join budget.department department								"+
							"	join budget.childrenBudgets childrenBudget						"+
							"	join budget.humanFees humanFee									"+
							"	join budget.monthBudgets monthBudget							"+
							"	join humanFee.budgetItem										"+
							"	join monthBudget.budgetItem									"+
							"WHERE  																			"+
							"	budget.dropped = false AND														"+
							"	budget.year=:year AND															"+
							"	budget.budgetStateType in (5, 6, 10, 12) AND									"+
//							"	(																				"+
//							"		(budget.department.dropped=0 AND budget.department.droppedDate IS null) OR "+
//							"		(budget.year<= SUBSTRING(department.droppedDate,1,4))								"+
//							"	)AND 																			"+ 
							"	budget.department.departmentType.id = 1 AND										"+
							"	budget.department.approveUpload = true AND										"+
							"	budget.department.departmentLargeType.id in (1, 2, 3, 6)							"+
							"ORDER BY budget.department.id														"; 
		
		Map<String, Object> params= new HashMap<String, Object>();
		 params.put("year", budgetYear.getYear());
		
		return findByNamedParams(queryString.toString(), params);	
		
		
	}
	
	
	/**
	 * Fun5.1 
	 * 1.依年度查詢出不可上傳的預算 
	 * 2.Department的是否上傳大電腦為false 
	 * 3.Department為部級
	 * 4.預算表的dropped必須為false
	 * 5.編列單位未被裁撤且編列單位的裁撤日為null 或 預算的年度小於等於編列單位裁撤日的年度
	 * 6.預算表的年度=查詢年度
	 * 
	 * @偉哲
	 * @param year
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readCouldNotUploadBudgetByYear(final BudgetYear budgetYear){



		String queryString ="SELECT budget FROM Budget budget																	"+
							"	join budget.department department								"+
							"WHERE  																			"+
							"	budget.dropped = false AND														"+
							"	budget.year=:year AND															"+
							//"	budget.budgetStateType not in (5, 6, 10, 12) AND								"+
//							"	(																				"+
//							"		(budget.department.dropped=0 AND budget.department.droppedDate IS null) OR "+
//							"		(budget.year<= SUBSTRING(department.droppedDate,1,4))								"+
//							"	)AND 																			"+ 
							"	budget.department.departmentType.id = 1 AND										"+
							"	budget.department.approveUpload = false 										"+
		
							"ORDER BY budget.department.id														";		
		
		
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear.getYear());
		
		return findByNamedParams(queryString.toString(), params);	
	}
	
	/**
	 * FUN 6.2 依據 年度查詢全部單位預算追加減 2008.4.29新增 BY文珊
	 *  1.已考量編列單位裁 
	 *  2.包含子層編列單位
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetChangeSummartByYear(final BudgetYear budgetYear){
		String queryString = "SELECT  																			"+																																									
		"	BC.ID, BC.CODE,BC.UPLOAD_FILE_ADDRESS, DEPARTMENT.NAME												"+																																				
		"		FROM    TBBFM_BUDGET_CHANGES BC     															"+																																						
		"		        INNER JOIN TBBFM_PROJECT_BUDGETS PB ON BC.TBBFM_PROJECT_BUDGETS_ID = PB.ID  			"+																																						
		"		        INNER JOIN TBBFM_BUDGETS BUDGET ON PB.TBBFM_BUDGETS_ID = BUDGET.ID  					"+																																						
		"			    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT ON BUDGET.TBBFM_DEPARTMENTS_ID = DEPARTMENT.ID  "+			    
		"		WHERE  																							"+																																					
		"		BUDGET.YEAR = :year AND 																		"+																																				
		"		( 																								"+																																					
		"		         DEPARTMENT.DROPPED = 0 AND (DEPARTMENT.DROPPED_DATE IS NULL)OR  						"+																																						
		"		         BUDGET.YEAR <= EXTRACT (YEAR FROM DEPARTMENT.DROPPED_DATE) 							"+																																						
		"		) AND 																							"+																																					
		"		BUDGET.DROPPED=0 		                                                                		"+        		  
		"		ORDER BY BC.ID																					";	
		
		Map<String, Object> params = new HashMap<String, Object>();
    	
		params.put("year", Integer.toString(budgetYear.getYear()));    	
    	
		List result  = findByNativeSQL(StringUtils.queryStringAssembler(
				queryString.toString(), params));

    	return result;
	}
	
	/**
	 * FUN 6.2 依據 年度、單位名稱 查詢預算追加減
	 *  1.已考量編列單位裁 
	 *  2.包含子層編列單位
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetChangeSummartByDepAndYear(final BfmDepartment department, final BudgetYear budgetYear){
		String queryString = "SELECT  																			"+																																									
		"	BC.ID, BC.CODE,BC.UPLOAD_FILE_ADDRESS, DEPARTMENT.NAME 																"+																																				
		"		FROM    TBBFM_BUDGET_CHANGES BC     															"+																																						
		"		        INNER JOIN TBBFM_PROJECT_BUDGETS PB ON BC.TBBFM_PROJECT_BUDGETS_ID = PB.ID  			"+																																						
		"		        INNER JOIN TBBFM_BUDGETS BUDGET ON PB.TBBFM_BUDGETS_ID = BUDGET.ID  					"+																																						
		"			    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT ON BUDGET.TBBFM_DEPARTMENTS_ID = DEPARTMENT.ID  "+			    
		"		WHERE  																							"+																																					
		"		BUDGET.YEAR = :year AND 																		"+																																				
		"		( 																								"+																																					
		"		         DEPARTMENT.DROPPED = 0 AND (DEPARTMENT.DROPPED_DATE IS NULL)OR  						"+																																						
		"		         BUDGET.YEAR <= EXTRACT (YEAR FROM DEPARTMENT.DROPPED_DATE) 							"+																																						
		"		) AND 																							"+																																					
		"		         BUDGET.DROPPED=0 AND                                                                   "+        
		"		(                                                                                               "+
		"				DEPARTMENT.ID = :departmentId OR                                                        "+   
		"				DEPARTMENT.TBBFM_DEPARTMENTS_ID =  :departmentId                                        "+  
		"		) ORDER BY BC.ID																				";
		
		Map<String, Object> params = new HashMap<String, Object>();
    	
		params.put("year",  Integer.toString(budgetYear.getYear()));
		params.put("departmentId", department.getId());
    	
		List result  = findByNativeSQL(StringUtils.queryStringAssembler(
				queryString.toString(), params));

    	return result;
	}
	
	
	/**
	 * FUN 7.3 依據 預算項目、年度、單位名稱 查詢單位預算
	 *  1.包含預算追加減、人件費
	 *  2.已考量編列單位裁撤與重編製 
	 *  3.包含子層編列單位
	 *  4.2008.5.7以部門為單位檢視各預算項目的金額 BY.文珊
	 * @param budgetItem 
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetItemSummaryByBudgetItemAndDepAndYear(final BfmBudgetItem budgetItem, final BfmDepartment department, final BudgetYear budgetYear){
		String queryString = 
			"SELECT                                                                                               					     "+																																																						
			"SB.NAME AS BUDGET_ITEM_NAME, 				                                                                                 "+
			"SUM(SB.AMOUNT) AS AMOUNT                                                                                                    "+		
			"FROM 																														 "+																																	
			"( 																															 "+																																	
			" ( 																														 "+																																	
			"	SELECT  																												 "+																																
			"		BI1.NAME, PBI1.AMOUNT																	"+																																						
			"	FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																				"+																																
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  			"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  	"+	
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI1 ON PBI1.TBBFM_BUDGET_ITEMS_ID = BI1.ID 					"+
			"	WHERE  																													"+																															
			"			                    BUDGET1.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET1.DROPPED=0 AND 																	    "+
			"								(                                                                                           "+
			"								 	DEPARTMENT1.ID = :departmentId OR                                                       "+   
			"									DEPARTMENT1.TBBFM_DEPARTMENTS_ID =  :departmentId                                      	"+
			"								) AND                                                                                       "+
			"								PBI1.TBBFM_BUDGET_ITEMS_ID = :budgetItemId			                                        "+																																						                      
			" ) 																														"+																																	
			" UNION ALL 																												"+																																	
			" (                    																										"+																																
			"	SELECT  																												"+																																
			"		BI2.NAME, BCI2.AUDIT_AMOUNT AS AMOUNT 													"+																																							
			"	FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																				"+																																
			"			                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  				"+																																						
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  				"+																																					
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  	"+		
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI2 ON BCI2.TBBFM_BUDGET_ITEMS_ID = BI2.ID					"+
			"	WHERE  																													"+																															
			"			                    BUDGET2.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET2.DROPPED=0 AND                                                                       "+
			"								(                                                                                           "+
			"								 	DEPARTMENT2.ID = :departmentId OR                                                       "+
			"									DEPARTMENT2.TBBFM_DEPARTMENTS_ID =  :departmentId                                       "+
			"								) AND                                                                                       "+
			"								BCI2.TBBFM_BUDGET_ITEMS_ID = :budgetItemId		                                   		    "+																																		
			" ) 																                                          		        "+																																													
			" UNION ALL                    																								"+																																
			" (          																												"+																																			              
			"			                SELECT  																						"+																																						
			"			                    BI3.NAME, HF3.FEE AS AMOUNT									"+																																													
			"			                FROM    TBBFM_HUMAN_FEES HF3  																	"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET3 ON HF3.TBBFM_BUDGETS_ID = BUDGET3.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT3 ON BUDGET3.TBBFM_DEPARTMENTS_ID = DEPARTMENT3.ID  	"+
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI3 ON HF3.TBBFM_BUDGET_ITEMS_ID = BI3.ID						"+
			"			                WHERE  																							"+																																					
			"			                    BUDGET3.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT3.DROPPED = 0 AND (DEPARTMENT3.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET3.YEAR <= EXTRACT (YEAR FROM DEPARTMENT3.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET3.DROPPED=0 AND                                                                       "+
			"								(                                                                                           "+
			"								    DEPARTMENT3.ID = :departmentId OR                                                       "+
			"									DEPARTMENT3.TBBFM_DEPARTMENTS_ID =  :departmentId                                       "+
			"								) AND                                                                                       "+
			"								HF3.TBBFM_BUDGET_ITEMS_ID = :budgetItemId													"+																						
			" )     																													"+																																	
			")SB 																														"+																																	
			"GROUP BY SB.NAME 	                                                                            				            ";
			
		
    	Map<String, Object> params = new HashMap<String, Object>();
    	
    	params.put("year",Integer.toString(budgetYear.getYear()));
    	params.put("departmentId", department.getId());
    	params.put("budgetItemId", budgetItem.getId());
    	
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
	
	}
	
	/**
	 * FUN 7.3 依據 預算項目、年度 查詢預算
	 *  1.包含預算追加減、人件費
	 *  2.已考量編列單位裁撤與重編製 
	 *  3.包含子層編列單位
	 *  4.2008.5.7以部門為單位檢視各預算項目的金額 BY.文珊
	 *  5.order by department.id
	 * @param budgetItem 
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetItemSummaryByBudgetItemAndYear(final BfmBudgetItem budgetItem, final BudgetYear budgetYear){
		String queryString = 
			"SELECT                                                                                               					     "+																																																						
			"SB.NAME AS BUDGET_ITEM_NAME, 				                                                                                 "+
			"SB.AMOUNT AS AMOUNT," +
			"SB.DEPARTMENT_NAME                                                                                                    "+		
			"FROM 																														 "+																																	
			"( 																															 "+																																	
			" ( 																														 "+																																	
			"	SELECT  																												 "+																																
			"		BI1.NAME, PBI1.AMOUNT,DEPARTMENT1.NAME AS DEPARTMENT_NAME,DEPARTMENT1.ID AS DEPARTMENT_ID							"+																																						
			"	FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																				"+																																
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  			"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  	"+	
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI1 ON PBI1.TBBFM_BUDGET_ITEMS_ID = BI1.ID 					"+
			"	WHERE  																													"+																															
			"			                    BUDGET1.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET1.DROPPED=0 AND 																	    "+
			"								PBI1.TBBFM_BUDGET_ITEMS_ID = :budgetItemId			                                        "+																																						                      
			" ) 																														"+																																	
			" UNION ALL 																												"+																																	
			" (                    																										"+																																
			"	SELECT  																												"+																																
			"		BI2.NAME, BCI2.AUDIT_AMOUNT AS AMOUNT,DEPARTMENT2.NAME AS DEPARTMENT_NAME,DEPARTMENT2.ID AS DEPARTMENT_ID																				"+																																							
			"	FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																				"+																																
			"			                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  				"+																																						
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  				"+																																					
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  	"+		
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI2 ON BCI2.TBBFM_BUDGET_ITEMS_ID = BI2.ID					"+
			"	WHERE  																													"+																															
			"			                    BUDGET2.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET2.DROPPED=0 AND                                                                       "+
			"								BCI2.TBBFM_BUDGET_ITEMS_ID = :budgetItemId		                                   		    "+																																		
			" ) 																                                          		        "+																																													
			" UNION ALL                    																								"+																																
			" (          																												"+																																			              
			"		SELECT  																											"+																																						
			"		BI3.NAME, HF3.FEE AS AMOUNT,DEPARTMENT3.NAME AS DEPARTMENT_NAME,DEPARTMENT3.ID AS DEPARTMENT_ID						"+																																													
			"			                FROM    TBBFM_HUMAN_FEES HF3  																	"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET3 ON HF3.TBBFM_BUDGETS_ID = BUDGET3.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT3 ON BUDGET3.TBBFM_DEPARTMENTS_ID = DEPARTMENT3.ID  	"+
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI3 ON HF3.TBBFM_BUDGET_ITEMS_ID = BI3.ID						"+
			"			                WHERE  																							"+																																					
			"			                    BUDGET3.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT3.DROPPED = 0 AND (DEPARTMENT3.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET3.YEAR <= EXTRACT (YEAR FROM DEPARTMENT3.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET3.DROPPED=0 AND                                                                       "+
			"								HF3.TBBFM_BUDGET_ITEMS_ID = :budgetItemId													"+																						
			" )     																													"+																																	
			")SB 																														"+																																	
			"ORDER BY SB.DEPARTMENT_ID 	                                                                            				";
			
		
    	Map<String, Object> params = new HashMap<String, Object>();
    	
    	params.put("year", Integer.toString(budgetYear.getYear()));    	
    	params.put("budgetItemId", budgetItem.getId());
    	
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
	}
	
	/*public List readBudgetItemSummaryByBudgetItemAndYear(final BudgetItem budgetItem, final BudgetYear budgetYear){
		String queryString = 
			"SELECT                                                                                               					     "+																																																						
			"SB.NAME AS BUDGET_ITEM_NAME, 				                                                                                 "+
			"SUM(SB.AMOUNT) AS AMOUNT                                                                                                    "+		
			"FROM 																														 "+																																	
			"( 																															 "+																																	
			" ( 																														 "+																																	
			"	SELECT  																												 "+																																
			"		BI1.NAME, PBI1.AMOUNT																	"+																																						
			"	FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																				"+																																
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  			"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  	"+	
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI1 ON PBI1.TBBFM_BUDGET_ITEMS_ID = BI1.ID 					"+
			"	WHERE  																													"+																															
			"			                    BUDGET1.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET1.DROPPED=0 AND 																	    "+
			"								PBI1.TBBFM_BUDGET_ITEMS_ID = :budgetItemId			                                        "+																																						                      
			" ) 																														"+																																	
			" UNION ALL 																												"+																																	
			" (                    																										"+																																
			"	SELECT  																												"+																																
			"		BI2.NAME, BCI2.AUDIT_AMOUNT AS AMOUNT													"+																																							
			"	FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																				"+																																
			"			                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  				"+																																						
			"			                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  				"+																																					
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  	"+		
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI2 ON BCI2.TBBFM_BUDGET_ITEMS_ID = BI2.ID					"+
			"	WHERE  																													"+																															
			"			                    BUDGET2.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET2.DROPPED=0 AND                                                                       "+
			"								BCI2.TBBFM_BUDGET_ITEMS_ID = :budgetItemId		                                   		    "+																																		
			" ) 																                                          		        "+																																													
			" UNION ALL                    																								"+																																
			" (          																												"+																																			              
			"			                SELECT  																						"+																																						
			"			                    BI3.NAME, HF3.FEE AS AMOUNT													"+																																													
			"			                FROM    TBBFM_HUMAN_FEES HF3  																	"+																																						
			"			                    INNER JOIN TBBFM_BUDGETS BUDGET3 ON HF3.TBBFM_BUDGETS_ID = BUDGET3.ID  						"+																																					
			"			                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT3 ON BUDGET3.TBBFM_DEPARTMENTS_ID = DEPARTMENT3.ID  	"+
			"								INNER JOIN TBBFM_BUDGET_ITEMS BI3 ON HF3.TBBFM_BUDGET_ITEMS_ID = BI3.ID						"+
			"			                WHERE  																							"+																																					
			"			                    BUDGET3.YEAR = :year AND 																	"+																																						
			"			                    ( 																							"+																																						
			"			                        DEPARTMENT3.DROPPED = 0 AND (DEPARTMENT3.DROPPED_DATE IS NULL)OR  						"+																																						
			"			                        BUDGET3.YEAR <= EXTRACT (YEAR FROM DEPARTMENT3.DROPPED_DATE) 							"+																																						
			"			                    ) AND 																						"+																																						
			"			                    BUDGET3.DROPPED=0 AND                                                                       "+
			"								HF3.TBBFM_BUDGET_ITEMS_ID = :budgetItemId													"+																						
			" )     																													"+																																	
			")SB 																														"+																																	
			"GROUP BY SB.NAME 	                                                                            				            ";
			
		
    	Map<String, Object> params = new HashMap<String, Object>();
    	
    	params.put("year", budgetYear.getYear());    	
    	params.put("budgetItemId", budgetItem.getId());
    	
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
	
	}*/
	
	/**
	 * FUN 7.4 依據 專案、年度、單位名稱 查詢專案預算項目
	 *  1.包含預算追加減
	 *  2.已考量編列單位裁撤與重編製 
	 *  3.包含子層編列單位
	 * @param projectBudget 
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public List readProjectBudgetSummaryByProjectBudgetAndDepAndYear(final ProjectBudget projectBudget, final BfmDepartment department, final BudgetYear budgetYear){
		String queryString =
			"SELECT                                                                                               					     			"+																																																			
			"		SB.NAME AS BUDGET_ITEM_NAME, 				                                                                                 	"+
			"		SUM(SB.AMOUNT) AS AMOUNT                                                                                                    	"+	
			"		FROM 																														 	"+																																
			"		( 																															 	"+																																
			"		 ( 																														 		"+																															
			"			SELECT  																												 	"+																															
			"				BI1.NAME, PBI1.AMOUNT																									"+																														
			"			FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																					"+																															
			"					                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  				"+																																					
			"					                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  							"+																																				
			"					                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  		"+
			"										INNER JOIN TBBFM_BUDGET_ITEMS BI1 ON PBI1.TBBFM_BUDGET_ITEMS_ID = BI1.ID 					    "+
			"			WHERE  																														"+																														
			"					                    BUDGET1.YEAR = :year AND 																		"+																																					
			"					                    ( 																								"+																																					
			"					                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  							"+																																					
			"					                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 								"+																																					
			"					                    ) AND 																							"+																																					
			"					                    BUDGET1.DROPPED=0 AND 																	        "+
			"										(                                                                                               "+  
			"										 	DEPARTMENT1.ID = :departmentId OR                                                                      "+ 
			"											DEPARTMENT1.TBBFM_DEPARTMENTS_ID =  :departmentId                                      	            "+
			"										) AND                                                                                           "+
			"										PB1.ID = :projectBudgetId			                                        								"+																														                      
			"		 ) 																																"+																															
			"		 UNION ALL 																														"+																															
			"		 (                    																											"+																															
			"			SELECT  																													"+																															
			"				BI2.NAME, BCI2.AUDIT_AMOUNT AS AMOUNT 																					"+																															
			"			FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																					"+																															
			"					                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  					"+																																					
			"					                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  					"+																																				
			"					                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  							"+																																				
			"					                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  		"+	
			"										INNER JOIN TBBFM_BUDGET_ITEMS BI2 ON BCI2.TBBFM_BUDGET_ITEMS_ID = BI2.ID					    "+
			"			WHERE  																														"+																														
			"					                    BUDGET2.YEAR = :year AND 																		"+																																					
			"					                    ( 																								"+																																					
			"					                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  							"+																																					
			"					                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 								"+																																					
			"					                    ) AND 																							"+																																					
			"					                    BUDGET2.DROPPED=0 AND                                                                           "+
			"										(                                                                                               "+
			"										 	DEPARTMENT2.ID = :departmentId OR                                                                      "+ 
			"											DEPARTMENT2.TBBFM_DEPARTMENTS_ID =  :departmentId                                                      "+
			"										) AND                                                                                           "+
			"										BC2.TBBFM_PROJECT_BUDGETS_ID = :projectBudgetId		                                   		    			"+																															
			"		 ) 																                                          		        		"+																																																																																																									
			"		)SB 																															"+																																
			"		GROUP BY SB.NAME 	                                                                                                            ";
		
		
    	Map<String, Object> params = new HashMap<String, Object>();
    	
    	params.put("year", Integer.toString(budgetYear.getYear()));    	
    	params.put("projectBudgetId", projectBudget.getId());
    	params.put("departmentId", department.getId());
    	
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
	}
	
	/**
	 * FUN 7.5 依據編列單位ID、年份依據年份查詢編列單位預算
	 * 1.包含預算追加減、人件費
	 * 2.已考量編列單位裁撤與重編製 
	 * 3.包含子層編列單位
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetSummaryByDepartmentAndYearForSearchBudget(final BfmDepartment department, final BudgetYear budgetYear) {
		String queryString =
			"SELECT														 																																																			"+
			"    BI4.ID, BI4.NAME, SEARCH_BUDGET.LAST_YEAR_BUDGET, SEARCH_BUDGET.LAST_YEAR_ACTUAL_FEE, SEARCH_BUDGET.THIS_YEAR_BUDGET, 																																				"+
			"    SEARCH_BUDGET.UNTIL_NOW_BUDGET, SEARCH_BUDGET.UNTIL_NOW_ACTUAL_FEE, SEARCH_BUDGET.NEXT_YEAR_BUDGET, BIT4.ID AS BUDGET_ITEM_TYPE_ID, BIT4.NAME AS BUDGET_ITEM_TYPE_NAME,																							"+
			"    BILT4.ID AS BUDGET_ITEM_LARGE_TYPE_ID, BILT4.NAME AS BUDGET_ITEM_LARGE_TYPE_NAME																																													"+
			"FROM																																																																	"+
			"(																																																																		"+
			"    SELECT 																																																															"+
			"         CASE 																																																															"+
			"            WHEN LAST_YEAR.BUDGET_ITEM_ID IS NOT NULL THEN LAST_YEAR.BUDGET_ITEM_ID																																													"+
			"            WHEN NEXT_YEAR.BUDGET_ITEM_ID IS NOT NULL THEN NEXT_YEAR.BUDGET_ITEM_ID																																													"+
			"         END AS BUDGET_ITEM_ID,																																																										"+
			"         LAST_YEAR.LAST_YEAR_BUDGET, LAST_YEAR.LAST_YEAR_ACTUAL_FEE, LAST_YEAR.THIS_YEAR_BUDGET, LAST_YEAR.UNTIL_NOW_BUDGET, LAST_YEAR.UNTIL_NOW_ACTUAL_FEE,																											"+
			"         NEXT_YEAR.AMOUNT AS NEXT_YEAR_BUDGET																																																							"+
			"    FROM																																																																"+
			"    (																																																																	"+
			"        SELECT 																																																														"+
			"            CLYB5.TBBFM_BUDGET_ITEMS_ID AS BUDGET_ITEM_ID, SUM(CLYB5.LAST_YEAR_BUDGET) AS LAST_YEAR_BUDGET, SUM(CLYB5.LAST_YEAR_ACTUAL_FEE) AS LAST_YEAR_ACTUAL_FEE, SUM(CLYB5.THIS_YEAR_BUDGET) AS THIS_YEAR_BUDGET, SUM(CLYB5.UNTIL_NOW_BUDGET) AS UNTIL_NOW_BUDGET,	"+
			"            SUM(CLYB5.UNTIL_NOW_ACTUAL_FEE) AS UNTIL_NOW_ACTUAL_FEE																																																	"+
			"        FROM TBBFM_C_LAST_YEAR_BUDGETS CLYB5																																																							"+
			"            INNER JOIN TBBFM_BUDGETS BUDGET5 ON BUDGET5.ID = CLYB5.TBBFM_BUDGETS_ID																																													"+
			"            INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT5 ON BUDGET5.TBBFM_DEPARTMENTS_ID = DEPARTMENT5.ID																																									"+
			"        WHERE 																																																															"+
			"            BUDGET5.YEAR = :year AND																																																									"+
			"            (	 																																																														"+
			"                DEPARTMENT5.DROPPED = 0 AND (DEPARTMENT5.DROPPED_DATE IS NULL)OR  																																														"+
			"                BUDGET5.YEAR <= EXTRACT (YEAR FROM DEPARTMENT5.DROPPED_DATE) 																																															"+
			"            ) AND 																																																														"+
			"            BUDGET5.DROPPED=0  																																																										"+
			"            AND ( DEPARTMENT5.ID = :departmentId OR DEPARTMENT5.TBBFM_DEPARTMENTS_ID =  :departmentId 	)																																								"+			              
			"        GROUP BY  CLYB5.TBBFM_BUDGET_ITEMS_ID 																																																							"+
			"    ) LAST_YEAR 																																																														"+
			"    FULL OUTER JOIN 																																																													"+
			"    ( 																																																																	"+
			"       SELECT  																																																														"+
			"            SB.TBBFM_BUDGET_ITEMS_ID AS BUDGET_ITEM_ID, SUM(SB.AMOUNT) AS AMOUNT 																																														"+
			"        FROM 																																																															"+
			"        ( 																																																																"+
			"            ( 																																																															"+
			"                SELECT  																																																												"+
			"                    PBI1.TBBFM_BUDGET_ITEMS_ID, PBI1.AMOUNT 																																																			"+
			"                FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																																																				"+
			"                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  																																									"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  																																						"+
			"                WHERE  																																																												"+
			"                    BUDGET1.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET1.DROPPED=0  																																																								"+
			"                    AND ( DEPARTMENT1.ID = :departmentId OR DEPARTMENT1.TBBFM_DEPARTMENTS_ID =  :departmentId 	)																																						"+			                      
			"            ) 																																																															"+
			"            UNION ALL 																																																													"+
			"            (                    																																																										"+
			"                SELECT  																																																												"+
			"                    BCI2.TBBFM_BUDGET_ITEMS_ID, BCI2.AUDIT_AMOUNT AS AMOUNT 																																															"+
			"                FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																																																				"+
			"                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  																																										"+
			"                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  																																									"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  																																						"+               
			"                WHERE  																																																												"+
			"                    BUDGET2.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET2.DROPPED=0  																																																								"+
			"                    AND ( DEPARTMENT2.ID = :departmentId OR DEPARTMENT2.TBBFM_DEPARTMENTS_ID =  :departmentId 	)																																						"+
			"            ) 																																																															"+
			"            UNION ALL                    																																																								"+
			"            (          																																																												"+			              
			"                SELECT  																																																												"+
			"                    HF3.TBBFM_BUDGET_ITEMS_ID, HF3.FEE AS AMOUNT 																																																		"+
			"                FROM    TBBFM_HUMAN_FEES HF3  																																																							"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET3 ON HF3.TBBFM_BUDGETS_ID = BUDGET3.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT3 ON BUDGET3.TBBFM_DEPARTMENTS_ID = DEPARTMENT3.ID  																																						"+
			"                WHERE  																																																												"+
			"                    BUDGET3.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT3.DROPPED = 0 AND (DEPARTMENT3.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET3.YEAR <= EXTRACT (YEAR FROM DEPARTMENT3.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET3.DROPPED=0  																																																								"+
			"                    AND (DEPARTMENT3.ID = :departmentId OR DEPARTMENT3.TBBFM_DEPARTMENTS_ID =  :departmentId )																																							"+
			"            )     																																																														"+
			"        ) SB 																																																															"+
			"        GROUP BY SB.TBBFM_BUDGET_ITEMS_ID 																																																								"+
			"    ) NEXT_YEAR ON NEXT_YEAR.BUDGET_ITEM_ID = LAST_YEAR.BUDGET_ITEM_ID 																																																"+
			") SEARCH_BUDGET     																																																													"+
			"INNER JOIN TBBFM_BUDGET_ITEMS BI4 ON BI4.ID = SEARCH_BUDGET.BUDGET_ITEM_ID 																																															"+
			"INNER JOIN TBBFM_BUDGET_ITEM_TYPES BIT4 ON BIT4.ID = BI4.TBBFM_BUDGET_ITEM_TYPES_ID 																																													"+
			"INNER JOIN TBBFM_BUDGET_ITEM_LARGE_TYPES BILT4 ON BILT4.ID = BIT4.TBBFM_BUDGET_ITEM_L_TYPES_ID 																																										"+
			"ORDER BY BILT4.ID DESC, BI4.ID ASC																																										 																";
    


    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	
    	params.put("year", Integer.toString(budgetYear.getYear()));
    	params.put("departmentId", department.getId());
    	
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result ;
    	
	}

	/** 
	 * FUN 7.5 依據年份查詢公司所有編列單位預算
	 * 1.包含預算追加減、人件費
	 * 2.已考量編列單位裁撤與重編製
	 * @param budgetYear
	 * @return
	 */
	public List readBudgetSummaryByYearForSearchBudget(final BudgetYear budgetYear) {
		String queryString =
			"SELECT														 																																																			"+
			"    BI4.ID, BI4.NAME, SEARCH_BUDGET.LAST_YEAR_BUDGET, SEARCH_BUDGET.LAST_YEAR_ACTUAL_FEE, SEARCH_BUDGET.THIS_YEAR_BUDGET, 																																				"+
			"    SEARCH_BUDGET.UNTIL_NOW_BUDGET, SEARCH_BUDGET.UNTIL_NOW_ACTUAL_FEE, SEARCH_BUDGET.NEXT_YEAR_BUDGET, BIT4.ID AS BUDGET_ITEM_TYPE_ID, BIT4.NAME AS BUDGET_ITEM_TYPE_NAME,																							"+
			"    BILT4.ID AS BUDGET_ITEM_LARGE_TYPE_ID, BILT4.NAME AS BUDGET_ITEM_LARGE_TYPE_NAME																																													"+
			"FROM																																																																	"+
			"(																																																																		"+
			"    SELECT 																																																															"+
			"         CASE 																																																															"+
			"            WHEN LAST_YEAR.BUDGET_ITEM_ID IS NOT NULL THEN LAST_YEAR.BUDGET_ITEM_ID																																													"+
			"            WHEN NEXT_YEAR.BUDGET_ITEM_ID IS NOT NULL THEN NEXT_YEAR.BUDGET_ITEM_ID																																													"+
			"         END AS BUDGET_ITEM_ID,																																																										"+
			"         LAST_YEAR.LAST_YEAR_BUDGET, LAST_YEAR.LAST_YEAR_ACTUAL_FEE, LAST_YEAR.THIS_YEAR_BUDGET, LAST_YEAR.UNTIL_NOW_BUDGET, LAST_YEAR.UNTIL_NOW_ACTUAL_FEE,																											"+
			"         NEXT_YEAR.AMOUNT AS NEXT_YEAR_BUDGET																																																							"+
			"    FROM																																																																"+
			"    (																																																																	"+
			"        SELECT 																																																														"+
			"            CLYB5.TBBFM_BUDGET_ITEMS_ID AS BUDGET_ITEM_ID, SUM(CLYB5.LAST_YEAR_BUDGET) AS LAST_YEAR_BUDGET, SUM(CLYB5.LAST_YEAR_ACTUAL_FEE) AS LAST_YEAR_ACTUAL_FEE, SUM(CLYB5.THIS_YEAR_BUDGET) AS THIS_YEAR_BUDGET, SUM(CLYB5.UNTIL_NOW_BUDGET) AS UNTIL_NOW_BUDGET,	"+
			"            SUM(CLYB5.UNTIL_NOW_ACTUAL_FEE) AS UNTIL_NOW_ACTUAL_FEE																																																	"+
			"        FROM TBBFM_C_LAST_YEAR_BUDGETS CLYB5																																																							"+
			"            INNER JOIN TBBFM_BUDGETS BUDGET5 ON BUDGET5.ID = CLYB5.TBBFM_BUDGETS_ID																																													"+
			"            INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT5 ON BUDGET5.TBBFM_DEPARTMENTS_ID = DEPARTMENT5.ID																																									"+
			"        WHERE 																																																															"+
			"            BUDGET5.YEAR = :year AND																																																									"+
			"            (	 																																																														"+
			"                DEPARTMENT5.DROPPED = 0 AND (DEPARTMENT5.DROPPED_DATE IS NULL)OR  																																														"+
			"                BUDGET5.YEAR <= EXTRACT (YEAR FROM DEPARTMENT5.DROPPED_DATE) 																																															"+
			"            ) AND 																																																														"+
			"            BUDGET5.DROPPED=0  																																																										"+
//			"            AND DEPARTMENT5.ID = :departmentId OR DEPARTMENT5.TBBFM_DEPARTMENTS_ID =  :departmentId 																																									"+			              
			"        GROUP BY  CLYB5.TBBFM_BUDGET_ITEMS_ID 																																																							"+
			"    ) LAST_YEAR 																																																														"+
			"    FULL OUTER JOIN 																																																													"+
			"    ( 																																																																	"+
			"       SELECT  																																																														"+
			"            SB.TBBFM_BUDGET_ITEMS_ID AS BUDGET_ITEM_ID, SUM(SB.AMOUNT) AS AMOUNT 																																														"+
			"        FROM 																																																															"+
			"        ( 																																																																"+
			"            ( 																																																															"+
			"                SELECT  																																																												"+
			"                    PBI1.TBBFM_BUDGET_ITEMS_ID, PBI1.AMOUNT 																																																			"+
			"                FROM    TBBFM_PROJECT_BUDGET_ITEMS PBI1  																																																				"+
			"                    INNER JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI1.TBBFM_PROJECT_BUDGETS_ID = PB1.ID  																																									"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET1 ON PB1.TBBFM_BUDGETS_ID = BUDGET1.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT1 ON BUDGET1.TBBFM_DEPARTMENTS_ID = DEPARTMENT1.ID  																																						"+
			"                WHERE  																																																												"+
			"                    BUDGET1.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT1.DROPPED = 0 AND (DEPARTMENT1.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET1.YEAR <= EXTRACT (YEAR FROM DEPARTMENT1.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET1.DROPPED=0  																																																								"+
//			"                    AND DEPARTMENT1.ID = :departmentId OR DEPARTMENT1.TBBFM_DEPARTMENTS_ID =  :departmentId 																																							"+			                      
			"            ) 																																																															"+
			"            UNION ALL 																																																													"+
			"            (                    																																																										"+
			"                SELECT  																																																												"+
			"                    BCI2.TBBFM_BUDGET_ITEMS_ID, BCI2.AUDIT_AMOUNT AS AMOUNT 																																															"+
			"                FROM    TBBFM_BUDGET_CHANGE_ITEMS BCI2  																																																				"+
			"                    INNER JOIN TBBFM_BUDGET_CHANGES BC2 ON BCI2.TBBFM_BUDGET_CHANGES_ID = BC2.ID  																																										"+
			"                    INNER JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC2.TBBFM_PROJECT_BUDGETS_ID = PB2.ID  																																									"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET2 ON PB2.TBBFM_BUDGETS_ID = BUDGET2.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT2 ON BUDGET2.TBBFM_DEPARTMENTS_ID = DEPARTMENT2.ID  																																						"+               
			"                WHERE  																																																												"+
			"                    BUDGET2.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT2.DROPPED = 0 AND (DEPARTMENT2.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET2.YEAR <= EXTRACT (YEAR FROM DEPARTMENT2.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET2.DROPPED=0  																																																								"+
//			"                    AND DEPARTMENT2.ID = :departmentId OR DEPARTMENT2.TBBFM_DEPARTMENTS_ID =  :departmentId 																																							"+
			"            ) 																																																															"+
			"            UNION ALL                    																																																								"+
			"            (          																																																												"+			              
			"                SELECT  																																																												"+
			"                    HF3.TBBFM_BUDGET_ITEMS_ID, HF3.FEE AS AMOUNT 																																																		"+
			"                FROM    TBBFM_HUMAN_FEES HF3  																																																							"+
			"                    INNER JOIN TBBFM_BUDGETS BUDGET3 ON HF3.TBBFM_BUDGETS_ID = BUDGET3.ID  																																											"+
			"                    INNER JOIN TBBFM_DEPARTMENTS DEPARTMENT3 ON BUDGET3.TBBFM_DEPARTMENTS_ID = DEPARTMENT3.ID  																																						"+
			"                WHERE  																																																												"+
			"                    BUDGET3.YEAR = :year AND 																																																							"+
			"                    ( 																																																													"+
			"                        DEPARTMENT3.DROPPED = 0 AND (DEPARTMENT3.DROPPED_DATE IS NULL)OR  																																												"+
			"                        BUDGET3.YEAR <= EXTRACT (YEAR FROM DEPARTMENT3.DROPPED_DATE) 																																													"+
			"                    ) AND 																																																												"+
			"                    BUDGET3.DROPPED=0  																																																								"+
//			"                    AND DEPARTMENT3.ID = :departmentId OR DEPARTMENT3.TBBFM_DEPARTMENTS_ID =  :departmentId 																																							"+
			"            )     																																																														"+
			"        ) SB 																																																															"+
			"        GROUP BY SB.TBBFM_BUDGET_ITEMS_ID 																																																								"+
			"    ) NEXT_YEAR ON NEXT_YEAR.BUDGET_ITEM_ID = LAST_YEAR.BUDGET_ITEM_ID 																																																"+
			") SEARCH_BUDGET     																																																													"+
			"INNER JOIN TBBFM_BUDGET_ITEMS BI4 ON BI4.ID = SEARCH_BUDGET.BUDGET_ITEM_ID 																																															"+
			"INNER JOIN TBBFM_BUDGET_ITEM_TYPES BIT4 ON BIT4.ID = BI4.TBBFM_BUDGET_ITEM_TYPES_ID 																																													"+
			"INNER JOIN TBBFM_BUDGET_ITEM_LARGE_TYPES BILT4 ON BILT4.ID = BIT4.TBBFM_BUDGET_ITEM_L_TYPES_ID 																																										"+
			"ORDER BY BILT4.ID DESC, BI4.ID ASC																																										 																";
    


    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("year", Integer.toString(budgetYear.getYear()));
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
	}
	
	
	/**
	 * Fun7.7.0 依據年度查詢未丟棄的預算 BudgetStateType=(6、10、12) dropped=false
	 * method要load cLastYearBudgets, monthBudgets,
	 * humanAllocations(包含其下的humanAllocationItems), 和
	 * projectBudgets(包含其下的projectBudgetItems, budgetChanges和budgetChangeItems)
	 * 
	 * @param budgetYear
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "unchecked", "unchecked" })
	public List<Budget> readBudgetByDepartmentAndYearForRebudget(BudgetYear budgetYear) {

		String queryString ="SELECT DISTINCT budget 								" +
							"FROM Budget budget										" +
							"	join budget.department department	" +
							"WHERE			 										" +
							"	department.departmentType.id=1 AND					" +
							"	department.dropped=0 AND						" +
							"	department.name <>:name	AND							" +
							"	budget.year=:year AND								" + 
							"	budget.dropped=false AND							" +
							"	budget.budgetStateType.id IN ( '6','10','12' ) 			" ;
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear.getYear());
		params.put("name", "經管會");
				
		return findByNamedParams(queryString.toString(), params);
	}
	
    /**
     * Fun7.8 事業費用(檢核金額)輸入
     * @author 芷筠
     * 依據年份, 傳回該年份尚未達到5預算狀態的預算表的數目
     * @param year
     * @return
     */
    @SuppressWarnings("unchecked")
	public int readCountByBudgetYearForOperatingData(final int year){
		String queryString ="SELECT COUNT(DISTINCT budget) 			"+			
		"FROM Budget budget											"+
		"	LEFT OUTER JOIN budget.department department			"+
		"WHERE														"+ 
		"	budget.year=:year AND 									"+ 
		"	budget.dropped=false AND								"+
		"   budget.budgetStateType.id<:state 					";
//		"	( 														"+
//		"		(													"+
//		"			department.dropped=0 AND 					"+ 
//		"			department.droppedDate IS null					"+
//		"		) OR												"+ 
//		"		(													"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 		"+
//		"		)													"+
//		"	)														";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", year);
		params.put("state", BudgetStateType.FINISH_MONTH_BUDGET.getId());
		List list = new ArrayList();
		list = super.getJpaTemplate().findByNamedParams(queryString.toString(),params);
		if (!CollectionUtils.isEmpty(list)&& list.get(0) instanceof Number) {
			Iterator it = (Iterator) list.iterator();
			int result = 0;
			if (it.hasNext()) {			
				result = ((Number) it.next()).intValue();			
			}
			return result;	
		}
		return 0;

    }
    
    /**
     * Fun7.8 事業費用(檢核金額)輸入
     * @author 芷筠
     * 依據年份, 傳回所有預算表用以進行更新預算狀態為6
     * @param year
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<Budget> readByBudgetYearForOperatingData(final int year){
		String queryString ="SELECT budget FROM Budget budget							"+
		"WHERE															"+ 
		"	budget.year=:year AND 										"+ 
		"	budget.dropped=false 									";
//		"	( 															"+
//		"		(														"+
//		"			budget.department.dropped=0 AND 				"+ 
//		"			budget.department.droppedDate IS null					"+
//		"		) OR													"+ 
//		"		(														"+
//		"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4)	"+
//		"		)														"+
//		"	)															";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", year);
		return findByNamedParams(queryString.toString(), params);
    }
	
	
	/**
	 * Fun7.9.2 編列單位加班費用查詢
	 * 條件:(Department.dropped = false and department.droppedDate = null) or(budget.year <= SUBSTRING(department.droppedDate,1,4))
	 * @author 文珊
	 * @param budgetYear
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public List<Budget> readOverTimeForSectionDepartment(final int budgetYear){
		String queryString="SELECT DISTINCT budget			 			"+	
		"FROM Budget budget												"+
		"	join budget.department department			"+
		"WHERE															"+ 
		"	budget.year=:year AND 										"+ 
		"	budget.dropped=false AND	 								"+
		"	budget.parentBudget IS null 								";
//		"( 																"+
//		"		(														"+
//		"			budget.department.dropped=0 AND 				"+ 
//		"			budget.department.droppedDate IS null					"+
//		"		) OR													"+ 
//		"		(														"+
//		"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4) 	"+
//		"		)														"+
//		"	)  								";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		
		return findByNamedParams(queryString.toString(), params);
	}
	
	/**
	 * Fun7.9.2 編列單位加班費用查詢
	 * 條件:(Department.dropped = false and department.droppedDate = null) or(budget.year <= SUBSTRING(department.droppedDate,1,4))
	 * @author 文珊
	 * @param budgetYear
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public int readCountByOverTimeBudget(final int budgetYear){
		String queryString ="SELECT COUNT(DISTINCT budget) 				"+	
		"FROM Budget budget												"+
		"	LEFT OUTER JOIN budget.department department				"+
		"WHERE															"+ 
		"	budget.year=:year AND 										"+ 
		"	budget.dropped=false AND 									"+
		"	budget.parentBudget IS NULL									";
//		"( 																"+
//		"		(														"+
//		"			budget.department.dropped=0 AND 				"+ 
//		"			budget.department.droppedDate IS null					"+
//		"		) OR													"+ 
//		"		(														"+
//		"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4) 	"+
//		"		)														"+
//		"	)															";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		List list = new ArrayList();
		list = super.getJpaTemplate().findByNamedParams(queryString.toString(),
				params);
		if (!CollectionUtils.isEmpty(list)&& list.get(0) instanceof Number) {
			Iterator it = (Iterator) list.iterator();
			int result = 0;
			if (it.hasNext()) {
				result = ((Number) it.next()).intValue();
			}
			return result;
		}
		return 0;

	}
    
    /**
     * Fun7.12 彙總查詢
     * @author 芷筠
     * 依據年份, 回傳所有部級單位名稱, 辦公費, 人件費
     * @param year
     * @return
     */
    public List readByBudgetYearForBudgetSummary(final int year){
    	String queryString ="SELECT TD.NAME, TOF.OFFICE_FEE, THF.HUMAN_FEE FROM TBBFM_DEPARTMENTS TD	"+
    	"INNER JOIN TBBFM_BUDGETS TB ON TD.ID = TB.TBBFM_DEPARTMENTS_ID									"+
    	"LEFT JOIN(																						"+
    	" SELECT SUM(OFFICE_FEE) AS OFFICE_FEE, FID FROM(												"+
    	"  (																							"+
    	"  SELECT HF.FEE AS OFFICE_FEE, TD1.FID AS FID FROM TBBFM_HUMAN_FEES HF							"+
    	"  RIGHT JOIN TBBFM_BUDGET_ITEMS TBI ON HF.TBBFM_BUDGET_ITEMS_ID = TBI.ID						"+ 
    	"  RIGHT JOIN TBBFM_BUDGETS TB1 ON HF.TBBFM_BUDGETS_ID = TB1.ID									"+
    	"  RIGHT JOIN (																					"+ 
        "	SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 					"+
        "		CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID													"+
        "			WHEN 6 THEN																			"+
        "        		TBBFM_DEPARTMENTS.ID    														"+
        "        	ELSE    																			"+
        "        		DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID) 	"+
        "		END  AS  FID     																		"+	
        "	FROM TBBFM_DEPARTMENTS																		"+
        "	INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID "+
        "	) TD1 ON TB1.TBBFM_DEPARTMENTS_ID = TD1.ID  												"+
    	"  WHERE																						"+
    	"	 TB1.YEAR=:year AND 																		"+
    	"	 TBI.CODE NOT IN ('61110000') AND															"+         
    	"	 TB1.DROPPED = 0 AND																		"+ 
    	"	 ((TD1.DROPPED = 1 AND (TD1.DROPPED_DATE IS NOT NULL) AND									"+                                         
        "    TB1.YEAR <= EXTRACT(YEAR FROM TD1.DROPPED_DATE)) OR TD1.DROPPED=0)     					"+						 
    	"  )																							"+
    	"  UNION ALL																					"+
    	"  (																							"+
    	"  SELECT PBI.AMOUNT AS OFFICE_FEE, TD2.FID AS FID FROM TBBFM_PROJECT_BUDGET_ITEMS PBI  		"+
    	"  RIGHT JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI.TBBFM_PROJECT_BUDGETS_ID = PB1.ID 				"+
    	"  RIGHT JOIN TBBFM_BUDGETS TB2 ON PB1.TBBFM_BUDGETS_ID = TB2.ID								"+
    	"  RIGHT JOIN (																					"+
        "    SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 					"+
        "        CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID													"+
        "            WHEN 6 THEN																		"+
        "                TBBFM_DEPARTMENTS.ID    														"+
        "            ELSE    																			"+
        "                DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID) "+
        "        END  AS  FID																			"+
        "    FROM TBBFM_DEPARTMENTS																		"+
        "    INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID "+
        "    ) TD2 ON TB2.TBBFM_DEPARTMENTS_ID = TD2.ID													"+
    	"  WHERE 																						"+
    	"	 TB2.YEAR=:year AND																			"+
    	"    TB2.DROPPED=0 AND																			"+
    	"	 ((TD2.DROPPED = 1 AND TD2.DROPPED_DATE IS NOT NULL AND                                     "+    
        "    TB2.YEAR <= EXTRACT(YEAR FROM TD2.DROPPED_DATE)) OR TD2.DROPPED=0)   						"+
    	"  )																							"+
    	"  UNION ALL																					"+
    	"  (																							"+
    	"  SELECT BCI.AUDIT_AMOUNT AS OFFICE_FEE, TD3.FID AS FID FROM TBBFM_BUDGET_CHANGE_ITEMS BCI  	"+
    	"  RIGHT JOIN TBBFM_BUDGET_CHANGES BC ON BCI.TBBFM_BUDGET_CHANGES_ID = BC.ID					"+
    	"  RIGHT JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC.TBBFM_PROJECT_BUDGETS_ID = PB2.ID 				"+
    	"  RIGHT JOIN TBBFM_BUDGETS TB3 ON PB2.TBBFM_BUDGETS_ID = TB3.ID								"+
    	"  RIGHT JOIN (																					"+
        "        SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE,  			"+
        "            CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID												"+
        "                WHEN 6 THEN																	"+
        "                    TBBFM_DEPARTMENTS.ID    													"+
        "                ELSE    																		"+
        "                    DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID) "+ 
        "            END  AS  FID     																	"+
        "        FROM TBBFM_DEPARTMENTS																	"+
        "        INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID	"+
        "    ) TD3 ON TB3.TBBFM_DEPARTMENTS_ID = TD3.ID													"+
    	"  WHERE 																						"+
    	"	 TB3.YEAR=:year AND																			"+
    	"    TB3.DROPPED=0 AND																			"+
    	"    ((TD3.DROPPED = 1 AND TD3.DROPPED_DATE IS NOT NULL AND										"+
        "    TB3.YEAR <= EXTRACT(YEAR FROM TD3.DROPPED_DATE)) OR TD3.DROPPED=0)							"+
    	"  )																							"+
    	" )																								"+
    	" GROUP BY FID) TOF ON TD.ID = TOF.FID															"+
    	"LEFT JOIN(																						"+
    	"  SELECT SUM(HF2.FEE) AS HUMAN_FEE, TD4.FID FROM TBBFM_HUMAN_FEES HF2							"+
    	"  RIGHT JOIN TBBFM_BUDGET_ITEMS TBI2 ON HF2.TBBFM_BUDGET_ITEMS_ID = TBI2.ID					"+    	
    	"  RIGHT JOIN TBBFM_BUDGETS TB4 ON HF2.TBBFM_BUDGETS_ID = TB4.ID								"+
    	"  RIGHT JOIN (																					"+
        "        SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 				"+
        "            CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID												"+
        "                WHEN 6 THEN																	"+
        "                    TBBFM_DEPARTMENTS.ID    													"+
        "                ELSE    																		"+
        "                    DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID)	"+ 
        "            END  AS  FID      																	"+
        "        FROM TBBFM_DEPARTMENTS																	"+
        "        INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID "+
        "    ) TD4 ON TB4.TBBFM_DEPARTMENTS_ID = TD4.ID 												"+
    	"  WHERE																						"+
    	"	 TB4.YEAR=:year AND 																		"+
    	"	 TBI2.CODE IN ('61110000') AND																"+    	
    	"    TB4.DROPPED=0 AND 																			"+
    	"    ((TD4.DROPPED = 1 AND TD4.DROPPED_DATE IS NOT NULL AND										"+
        "    TB4.YEAR <= EXTRACT(YEAR FROM TD4.DROPPED_DATE)) OR TD4.DROPPED=0)							"+
    	"  GROUP BY TD4.FID																				"+
    	") THF ON TD.ID = THF.FID																		"+
    	"WHERE TB.YEAR=:year AND																		"+
    	"      TD.TBBFM_DEPARTMENT_TYPES_ID = 1 AND														"+
    	"      ((TD.DROPPED = 1 AND TD.DROPPED_DATE IS NOT NULL AND										"+
        "      TB.YEAR <= EXTRACT(YEAR FROM TD.DROPPED_DATE))  OR TD.DROPPED = 0)						"+	
    	"ORDER BY TD.ID																					"; 
    	    	
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("year", Integer.toString(year));
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
    }
    
    /**
     * Fun7.12 彙總查詢
     * @author 芷筠
     * 依據年份,部門, 回傳該單位之單位名稱, 辦公費, 人件費
     * @param year
     * @return
     */
    public List readByBudgetYearAndDepartmentForBudgetSummary(final int year, final BfmDepartment department){
    	String queryString ="SELECT TD.NAME, TOF.OFFICE_FEE, THF.HUMAN_FEE FROM TBBFM_DEPARTMENTS TD	"+
    	"INNER JOIN TBBFM_BUDGETS TB ON TD.ID = TB.TBBFM_DEPARTMENTS_ID									"+
    	"LEFT JOIN(																						"+
    	" SELECT SUM(OFFICE_FEE) AS OFFICE_FEE, FID FROM(												"+	
    	"  (																							"+
    	"  SELECT HF.FEE AS OFFICE_FEE, TD1.FID AS FID FROM TBBFM_HUMAN_FEES HF							"+
    	"  RIGHT JOIN TBBFM_BUDGET_ITEMS TBI ON HF.TBBFM_BUDGET_ITEMS_ID = TBI.ID						"+ 
    	"  RIGHT JOIN TBBFM_BUDGETS TB1 ON HF.TBBFM_BUDGETS_ID = TB1.ID									"+
    	"  RIGHT JOIN (																					"+
        "        SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 				"+
        "            CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID												"+
        "                WHEN 6 THEN																	"+
        "                    TBBFM_DEPARTMENTS.ID    													"+
        "                ELSE    																		"+
        "                    DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID)	"+ 
        "            END  AS  FID     																	"+
        "        FROM TBBFM_DEPARTMENTS																	"+
        "        INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID "+
        "     ) TD1 ON TB1.TBBFM_DEPARTMENTS_ID = TD1.ID												"+ 
    	"  WHERE																						"+
    	"	 TB1.YEAR=:year AND																			"+
    	"	 TBI.CODE NOT IN ('61110000') AND															"+         
    	"	 TB1.DROPPED = 0 AND																		"+ 
        "	 ((TD1.DROPPED = 1 AND (TD1.DROPPED_DATE IS NOT NULL) AND									"+                                         
        "    TB1.YEAR <= EXTRACT(YEAR FROM TD1.DROPPED_DATE)) OR TD1.DROPPED=0)     					"+	 
    	"  )																							"+
    	"  UNION ALL																					"+    	
    	"  (																							"+
    	"  SELECT PBI.AMOUNT AS OFFICE_FEE, TD2.FID AS FID FROM TBBFM_PROJECT_BUDGET_ITEMS PBI  		"+
    	"  RIGHT JOIN TBBFM_PROJECT_BUDGETS PB1 ON PBI.TBBFM_PROJECT_BUDGETS_ID = PB1.ID 				"+
    	"  RIGHT JOIN TBBFM_BUDGETS TB2 ON PB1.TBBFM_BUDGETS_ID = TB2.ID								"+
    	"  RIGHT JOIN (																					"+
        "    SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 					"+
        "        CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID													"+
        "            WHEN 6 THEN																		"+
        "                TBBFM_DEPARTMENTS.ID    														"+
        "            ELSE    																			"+
        "                DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID) "+
        "        END  AS  FID																			"+
        "    FROM TBBFM_DEPARTMENTS																		"+
        "    INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID "+
        "    ) TD2 ON TB2.TBBFM_DEPARTMENTS_ID = TD2.ID													"+
    	"  WHERE 																						"+
    	"	 TB2.YEAR=:year AND																			"+
    	"    TB2.DROPPED = 0 AND																		"+
    	"	 ((TD2.DROPPED = 1 AND TD2.DROPPED_DATE IS NOT NULL AND                                     "+    
        "     TB2.YEAR <= EXTRACT(YEAR FROM TD2.DROPPED_DATE)) OR TD2.DROPPED=0)   						"+
    	"  )																							"+
    	"  UNION ALL																					"+
    	"  (																							"+
    	"  SELECT BCI.AUDIT_AMOUNT AS OFFICE_FEE, TD3.FID AS FID FROM TBBFM_BUDGET_CHANGE_ITEMS BCI  	"+
    	"  RIGHT JOIN TBBFM_BUDGET_CHANGES BC ON BCI.TBBFM_BUDGET_CHANGES_ID = BC.ID					"+
    	"  RIGHT JOIN TBBFM_PROJECT_BUDGETS PB2 ON BC.TBBFM_PROJECT_BUDGETS_ID = PB2.ID 				"+
    	"  RIGHT JOIN TBBFM_BUDGETS TB3 ON PB2.TBBFM_BUDGETS_ID = TB3.ID								"+
    	"  RIGHT JOIN (																					"+
        "        SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 				"+
        "            CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID												"+
        "                WHEN 6 THEN																	"+
        "                    TBBFM_DEPARTMENTS.ID    													"+
        "                ELSE    																		"+
        "                    DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID)	"+ 
        "            END  AS  FID     																	"+
        "        FROM TBBFM_DEPARTMENTS																	"+
        "        INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID	"+
        "    ) TD3 ON TB3.TBBFM_DEPARTMENTS_ID = TD3.ID													"+
    	"  WHERE 																						"+
    	"	 TB3.YEAR=:year AND 																		"+
    	"    TB3.DROPPED = 0 AND																		"+
    	"    ((TD3.DROPPED = 1 AND TD3.DROPPED_DATE IS NOT NULL AND										"+
        "    TB3.YEAR <= EXTRACT(YEAR FROM TD3.DROPPED_DATE)) OR TD3.DROPPED=0)							"+
    	"  )																							"+
    	" )																								"+
    	" GROUP BY FID) TOF ON TD.ID = TOF.FID															"+
    	"LEFT JOIN(																						"+
    	"  SELECT SUM(HF2.FEE) AS HUMAN_FEE, TD4.FID FROM TBBFM_HUMAN_FEES HF2							"+
    	"  RIGHT JOIN TBBFM_BUDGET_ITEMS TBI2 ON HF2.TBBFM_BUDGET_ITEMS_ID = TBI2.ID					"+   	
    	"  RIGHT JOIN TBBFM_BUDGETS TB4 ON HF2.TBBFM_BUDGETS_ID = TB4.ID								"+
    	"  RIGHT JOIN (																					"+
        "        SELECT TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID, DROPPED, DROPPED_DATE, 				"+
        "            CASE TBBFM_DEPARTMENT_LARGE_TYPES.ID												"+
        "                WHEN 6 THEN																	"+
        "                    TBBFM_DEPARTMENTS.ID    													"+
        "                ELSE    																		"+
        "                    DECODE(TBBFM_DEPARTMENTS_ID, NULL, TBBFM_DEPARTMENTS.ID, TBBFM_DEPARTMENTS_ID)	"+ 
        "            END  AS  FID      																	"+
        "        FROM TBBFM_DEPARTMENTS																	"+
        "        INNER JOIN TBBFM_DEPARTMENT_LARGE_TYPES ON TBBFM_DEPARTMENTS.TBBFM_DEPARTMENT_L_TYPES_ID=TBBFM_DEPARTMENT_LARGE_TYPES.ID	"+
        "    ) TD4 ON TB4.TBBFM_DEPARTMENTS_ID = TD4.ID 							"+
    	"  WHERE																						"+
    	"	 TB4.YEAR=:year AND 																		"+
    	"	 TBI2.CODE IN ('61110000') AND																"+    	
    	"    TB4.DROPPED = 0 AND 																		"+
    	"    ((TD4.DROPPED = 1 AND TD4.DROPPED_DATE IS NOT NULL AND										"+
        "    TB4.YEAR <= EXTRACT(YEAR FROM TD4.DROPPED_DATE)) OR TD4.DROPPED=0)							"+
    	"  GROUP BY TD4.FID																				"+
    	") THF ON TD.ID = THF.FID																		"+
    	"WHERE TB.YEAR=:year AND																		"+
    	"      TD.ID=:departmentId AND																	"+
    	"      TD.TBBFM_DEPARTMENT_TYPES_ID = 1 AND														"+
    	"      ((TD.DROPPED = 1 AND TD.DROPPED_DATE IS NOT NULL AND										"+
        "      TB.YEAR <= EXTRACT(YEAR FROM TD.DROPPED_DATE))  OR TD.DROPPED = 0)						"+
    	"ORDER BY TD.ID																					"; 

    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("year",  String.valueOf(year));    	
    	params.put("departmentId", department.getId());
    	List result  = findByNativeSQL(StringUtils.queryStringAssembler(queryString.toString(), params));
    	return result;
    	
    }		
	
	/**
	 * Fun7.13 狀態查詢 
	 * 傳入預算年度, 回傳budget
	 * @author 芷筠
	 * @param firstResult
	 * @param maxResults
	 * @param budgetYear
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readByBudgetYearForBudgetState(final int firstResult, final int maxResults,
			final int budgetYear){
		String queryString = "SELECT budget FROM Budget budget								"+
		"	join budget.budgetStateType budgetStateType		"+
		"	join budget.department department 					"+
		"WHERE																	"+ 
		"	budget.year=:year AND 												"+ 
		"	budget.dropped=false AND											"+
//		"	( 																	"+
//		"		(																"+
//		"			department.dropped=0 AND 								"+ 
//		"			department.droppedDate IS null								"+
//		"		) OR															"+ 
//		"		(																"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 					"+
//		"		)																"+
//		"	) AND																"+
		"	department.departmentType.id = 1									"+
		"ORDER BY department.id													";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		
		return findByNamedParams(queryString.toString(), params);		
		
	}

	/**
	 * Fun7.13 狀態查詢 
	 * 傳入預算年度, 回傳budget數目
	 * @author 芷筠
	 * @param firstResult
	 * @param maxResults
	 * @param budgetYear
	 */
	@SuppressWarnings("unchecked")
	public int readCountByBudgetYearForBudgetState(final int budgetYear){
		String queryString = "SELECT COUNT(DISTINCT budget)			"+
		"FROM Budget budget											"+
		"	LEFT OUTER JOIN budget.department department 			"+
		"WHERE														"+ 
		"	budget.year=:year AND 									"+ 
		"	budget.dropped=false AND								"+
//		"	( 														"+
//		"		(													"+
//		"			department.dropped=0 AND 					"+ 
//		"			department.droppedDate IS null					"+
//		"		) OR												"+ 
//		"		(													"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 		"+
//		"		)													"+
//		"	) AND													"+
		"	department.departmentType.id = 1						";		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		
		List list = new ArrayList();
		list = super.getJpaTemplate().findByNamedParams(queryString.toString(),
				params);
		if (!CollectionUtils.isEmpty(list)&& list.get(0) instanceof Number) {
			Iterator it = (Iterator) list.iterator();
			int result = 0;
			if (it.hasNext()) {
				result = ((Number) it.next()).intValue();
			}
			return result;
		}
		return 0;
	}	
	
	/**
	 * Fun7.13 狀態查詢 
	 * 傳入預算年度和預算狀態Id, 回傳budget
	 * @author 芷筠
	 * @param firstResult
	 * @param maxResults
	 * @param budgetYear
	 * @param budgetStateTypeId
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readByBudgetYearAndBudgetStateTypeForBudgetState(final int firstResult, final int maxResults,
			final int budgetYear, final int budgetStateTypeId){
		String queryString = "SELECT budget FROM Budget budget								"+
		"	join budget.budgetStateType budgetStateType		"+
		"	join budget.department department 					"+
		"WHERE																	"+ 
		"	budget.year=:year AND 												"+ 
		"	budget.dropped=false AND											"+
//		"	( 																	"+
//		"		(																"+
//		"			department.dropped=0 AND 								"+ 
//		"			department.droppedDate IS null								"+
//		"		) OR															"+ 
//		"		(																"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 					"+
//		"		)																"+
//		"	) AND																"+
		"	budgetStateType.id=:stateId	AND										"+
		"	department.departmentType.id = 1									"+		
		"ORDER BY department.id													";
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		params.put("stateId", Integer.toString(budgetStateTypeId));
		return findByNamedParams(queryString.toString(), params);		
		
	}

	/**
	 * Fun7.13 狀態查詢 
	 * 傳入預算年度和預算狀態Id, 回傳budget數目
	 * @author 芷筠
	 * @param firstResult
	 * @param maxResults
	 * @param budgetYear
	 * @param budgetStateTypeId
	 */
	@SuppressWarnings("unchecked")
	public int readCountByBudgetYearAndBudgetStateTypeForBudgetState(final int budgetYear, final int budgetStateTypeId){
		String queryString = "SELECT COUNT(DISTINCT budget)			"+
		"FROM Budget budget											"+
		"	LEFT OUTER JOIN budget.department department 			"+
		"	LEFT OUTER JOIN budget.budgetStateType budgetStateType	"+
		"WHERE														"+ 
		"	budget.year=:year AND 									"+ 
		"	budget.dropped=false AND								"+
//		"	( 														"+
//		"		(													"+
//		"			department.dropped=0 AND 					"+ 
//		"			department.droppedDate IS null					"+
//		"		) OR												"+ 
//		"		(													"+
//		"			budget.year<= SUBSTRING(department.droppedDate,1,4) 		"+
//		"		)													"+
//		"	) AND													"+
		"	budgetStateType.id=:stateId AND							"+
		"	department.departmentType.id = 1						";	
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear);
		params.put("stateId", Integer.toString(budgetStateTypeId));
		List list = new ArrayList();
		list = super.getJpaTemplate().findByNamedParams(queryString.toString(),
				params);
		if (!CollectionUtils.isEmpty(list)&& list.get(0) instanceof Number) {
			Iterator it = (Iterator) list.iterator();
			int result = 0;
			if (it.hasNext()) {
				result = ((Number) it.next()).intValue();
			}
			return result;
		}
		return 0;

	}
	
	/**
	 * Fun7.14 建立年度預算表
	 * 依據部門與年度查詢出預算表資料，Fetch ProjectBudget,ProjectBudgetItem
	 * @author 偉哲
	 * @param department
	 * @param budgetYear
	 * @return
	 */
	public Budget readBudgetByDepartmentAndBudgetYear(BfmDepartment department, BudgetYear budgetYear){
		
		String queryString = "SELECT budget FROM Budget budget										"+
		"	left join budget.department department									"+
		"	left join budget.projectBudgets projectBudget					"+
		"	left join projectBudget.projectBudgetItems	projectBudgetItems		"+
		"	left join budget.childrenBudgets childrenBudget					"+
		"	left join childrenBudget.projectBudgets childrenProjectBudget	"+
		"	left join childrenProjectBudget.projectBudgetItems childrenprojectBudgetItems	"+
		"WHERE																			"+ 
		"	budget.year=:year AND 														"+ 
		"	budget.department.id=:id AND												"+
		"	budget.dropped=false														";

		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", budgetYear.getYear());
		params.put("id", department.getId());
		
		return findByNamedParamsUnique(queryString.toString(), params);	
	}

	
	
    /**
     * Fun 4.1 編列月預算
     * @author 芷筠
     * 依據年份, 部門, 回傳該單位的budget
     * @param year
     * @param department
     * @return
     */	
	@SuppressWarnings("unchecked")
	public List<Budget> readByBudgetYearAndDepartmentForMonthBudget(final int year, final BfmDepartment department){

		String queryString = "SELECT budget FROM Budget budget								"+
		"	join budget.budgetStateType budgetStateType		"+
		"WHERE																	"+ 
		"	budget.department.id=:departmentId AND								"+
		"	budget.year=:year AND 												"+ 
		"	budget.dropped=false 											";
//		"	( 																	"+
//		"		(																"+
//		"			budget.department.dropped=0 AND 						"+ 
//		"			budget.department.droppedDate IS null							"+
//		"		) OR															"+ 
//		"		(																"+
//		"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4)			"+
//		"		)																"+
//		"	)																	";
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("departmentId", department.getId());
		params.put("year", year);
		
		return findByNamedParams(queryString.toString(), params);		
		
		
	}
	
	
	/**
	 * Fun7.2.2 單位裁撤
	 * @author 芷筠
	 * 依據部門與年度, 回傳預算
	 * dropped=false
	 * @param department
	 * @param year
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Budget> readByDepartmentAndYearForDepartmentChange(
			final BfmDepartment department, final int year) {
		String queryString = "SELECT budget FROM Budget budget								"+
		"LEFT OUTER JOIN budget.budgetStateType budgetStateType						"+
		"WHERE																	"+ 
		"	budget.department.id=:departmentId AND								"+
		"	budget.year=:year AND 												"+ 
		"	budget.dropped=false 												";
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("departmentId", department.getId());
		params.put("year", Integer.valueOf(year));
		
		
		return findByNamedParams(queryString.toString(), params);
	}
	
	
	 /**
	  * Fun7.2.2 單位裁撤
	  * @author 芷筠
	  * 依據部門與年度, 回傳預算
	  * method包括父子層的projectBudget, projectBudgetItem, budgetChange, budgetChangeItem, 
	  * dropped=false
	  * @param department
	  * @param year
	  * @return
	  */
	 @SuppressWarnings("unchecked")
	public List<Budget> readByDepartmentAndYearForCopyForDepartmentChange(final BfmDepartment department, final int year){
			String queryString = "SELECT budget FROM Budget budget								"+
			"	left join budget.childrenBudgets childrenBudget			"+
			"	left join budget.projectBudgets projectBudget			"+
			"	left join projectBudget.projectBudgetItems projectBudgetItems				"+
			"	left join projectBudget.budgetChanges budgetChange		"+
			"	left join budgetChange.budgetChangeItems	budgetChangeItems			"+
			"	left join childrenBudget.projectBudgets childrenProjectBudget		"+
			"	left join childrenProjectBudget.projectBudgetItems childprojectBudgetItems					"+
			"	left join childrenProjectBudget.budgetChanges childrenBudgetChange	"+
			"	left join childrenBudgetChange.budgetChangeItems childbudgetChangeItems		"+
			"WHERE																	"+ 
			"	budget.department.id=:departmentId AND								"+
			"	budget.year=:year AND 												"+ 
			"	budget.dropped=false 											";
//			"	( 																	"+
//			"		(																"+
//			"			budget.department.dropped=0 AND 						"+ 
//			"			budget.department.droppedDate IS null							"+
//			"		) OR															"+ 
//			"		(																"+
//			"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4)			"+
//			"		)																"+
//			"	)																	";
			
			Map<String, Object> params= new HashMap<String, Object>();
			params.put("departmentId", department.getId());
			params.put("year", year);
			
			return findByNamedParams(queryString.toString(), params);		 
	 }
	 
		

	/**
	 * Fun3.3 or 需要子項預算的功能 當子項預算之前未被讀取出來時，則使用此函式初始化讀取子項預算 p.s會遞迴讀取所有子項預算
	 * 
	 * @author 偉哲
	 * @param parentBudget
	 */
	public Budget readInitializeChildrenBudgets(Budget parentBudget) {
		
//		session.lock(parentBudget, LockMode.NONE);
//		Hibernate.initialize(parentBudget.getChildrenBudgets());
//		this.releaseSession(session);

		if (parentBudget.getChildrenBudgets().size() > 0) {
			for (Budget childrenBudget : parentBudget.getChildrenBudgets()) {
				childrenBudget = this
						.readInitializeChildrenBudgets(childrenBudget);
			}
		}

		return parentBudget;
	} 
		
	/**
	 * 需要預算狀態的功能 當預算狀態之前未被讀取出來時，則使用此函式初始化讀取預算狀態
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeBudgetStateType(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getBudgetStateType());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要一般行政或專案的功能 當預一般行政或專案之前未被讀取出來時，則使用此函式初始化讀取一般行政或專案
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeProjectBudgets(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getProjectBudgets());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要編列單位的功能 當預編列單位之前未被讀取出來時，則使用此函式初始化讀取編列單位
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeDepartment(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getDepartment());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要編列單位的功能 當預編列單位之前未被讀取出來時，則使用此函式初始化讀取編列單位
	 * 
	 * @author 芷筠
	 * @param budget
	 */
	public Budget readInitializeParentBudget(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getParentBudget());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要初始化上一年度預算實支的功能 當需要初始化上一年度預算實支的功能未被讀取出來時，則使用此函式初始化上一年度預算實支的功能
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeConfirmLastYearBudgets(Budget budget) {

		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getConfirmLastYearBudgets());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要初始化月預算的功能 當需要初始化月預算的功能未被讀取出來時，則使用此函式初始化月預算的功能
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeMonthBudgets(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getMonthBudgets());
//		this.releaseSession(session);

		return budget;

	}

	/**
	 * 需要初始化人件費的功能 當需要初始化人件費的功能未被讀取出來時，則使用此函式初始化人件費的功能
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeHumanFees(Budget budget) {
		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getHumanFees());
//		this.releaseSession(session);

		return budget;
	}

	/**
	 * 需要初始化人力配置的功能 當需要初始化人力配置的功能未被讀取出來時，則使用此函式初始化人力配置的功能
	 * 
	 * @author 偉哲
	 * @param budget
	 */
	public Budget readInitializeHumanAllocations(Budget budget) {

		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getHumanAllocations());
//		this.releaseSession(session);

		return budget;

	}

	/**
	 * 需要初始化重編制新預算表的功能 當需要初始化人力配置的功能未被讀取出來時，則使用此函式初始化人力配置的功能
	 * 
	 * @author 文珊
	 * @param budget
	 */
	public Budget readInitializeNewRebudgetChanges(Budget budget) {

		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getNewRebudgetChanges());
//		this.releaseSession(session);

		return budget;

	}

	/**
	 * 需要初始化重編制舊預算表的功能 當需要初始化人力配置的功能未被讀取出來時，則使用此函式初始化人力配置的功能
	 * 
	 * @author 文珊
	 * @param budget
	 */
	public Budget readInitializeOldRebudgetChanges(Budget budget) {

		
//		session.lock(budget, LockMode.NONE);
//		Hibernate.initialize(budget.getOldRebudgetChanges());
//		this.releaseSession(session);

		return budget;

	}

	// RE201301619_新增各部室年度預算下載功能 modify by michael in 2014/02/24 start
	public Budget readByYearAndDepartment(final int year, final BfmDepartment department) {

		String queryString = "SELECT budget FROM Budget budget								"+
		"	join budget.budgetStateType budgetStateType		"+
		"WHERE																	"+ 
		"	budget.department.id=:departmentId AND								"+
		"	budget.year=:year AND 												"+ 
		"	budget.dropped=false 											";
//		"	( 																	"+
//		"		(																"+
//		"			budget.department.dropped=0 AND 						"+ 
//		"			budget.department.droppedDate IS null							"+
//		"		) OR															"+ 
//		"		(																"+
//		"			budget.year<= SUBSTRING(budget.department.droppedDate,1,4)			"+
//		"		)																"+
//		"	)																	";
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("departmentId", department.getId());
		params.put("year", year);
		
		return findByNamedParamsUnique(queryString.toString(), params);
		
		
	}
	// RE201301619_新增各部室年度預算下載功能 modify by michael in 2014/02/24 end
	
	@SuppressWarnings("unchecked")
	private List findByNativeSQL(final String sqlString) {
		return getJpaTemplate().executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createNativeQuery(sqlString);
				return queryObject.getResultList();
			}
		});
	}
	
	//defect4593_無寫入create(update) user,date問題 CU3178 2017/8/21 START
	/**
	 * 依據傳入年度刪除預算資料表
	 * @param year
	 * @return
	 */
	public int deleteBudgetByYear(int year) {
		String queryString = "DELETE FROM Budget budget  WHERE budget.year =:year   ";

		Map<String, Object> params= new HashMap<String, Object>();
		params.put("year", year);
		return executeUpdateORDeleteBySQL(queryString.toString(), params);
	}
	//defect4593_無寫入create(update) user,date問題 CU3178 2017/8/21 END
}
