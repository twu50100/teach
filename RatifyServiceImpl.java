package tw.com.skl.exp.kernel.model6.logic.impl;


import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tw.com.skl.common.model6.logic.impl.BaseServiceImpl;
import tw.com.skl.common.model6.web.jsf.utils.Messages;
import tw.com.skl.exp.kernel.model6.bo.ApplState;
import tw.com.skl.exp.kernel.model6.bo.ApplState.ApplStateCode;
import tw.com.skl.exp.kernel.model6.bo.Department;
import tw.com.skl.exp.kernel.model6.bo.ExpapplB;
import tw.com.skl.exp.kernel.model6.bo.Group;
import tw.com.skl.exp.kernel.model6.bo.Group.GroupCode;
import tw.com.skl.exp.kernel.model6.bo.KindType;
import tw.com.skl.exp.kernel.model6.bo.KindType.KindTypeCode;
import tw.com.skl.exp.kernel.model6.bo.MiddleType;
import tw.com.skl.exp.kernel.model6.bo.MiddleType.MiddleTypeCode;
import tw.com.skl.exp.kernel.model6.bo.Ratify;
import tw.com.skl.exp.kernel.model6.bo.RatifyState;
import tw.com.skl.exp.kernel.model6.bo.RatifyState.RatifyStateCode;
import tw.com.skl.exp.kernel.model6.bo.User;
import tw.com.skl.exp.kernel.model6.common.ErrorCode;
import tw.com.skl.exp.kernel.model6.common.exception.ExpRuntimeException;
import tw.com.skl.exp.kernel.model6.common.util.StringUtils;
import tw.com.skl.exp.kernel.model6.common.util.time.DateUtils;
import tw.com.skl.exp.kernel.model6.dao.RatifyDao;
import tw.com.skl.exp.kernel.model6.dto.ExportTurnOverDto;
import tw.com.skl.exp.kernel.model6.dto.OfficeExpDto;
import tw.com.skl.exp.kernel.model6.dto.PettyCashStatusDto;
import tw.com.skl.exp.kernel.model6.dto.RatifyCountsDto;
import tw.com.skl.exp.kernel.model6.dto.RatifyExportDto;
import tw.com.skl.exp.kernel.model6.dto.RatifyStatusDetailDto;
import tw.com.skl.exp.kernel.model6.dto.RatifyStatusDto;
import tw.com.skl.exp.kernel.model6.dto.ReturnAmtDto;
import tw.com.skl.exp.kernel.model6.dto.TurnOverDto;
import tw.com.skl.exp.kernel.model6.dto.UnprocessedRatifyDto;
import tw.com.skl.exp.kernel.model6.dto.WorkAreaDto;
import tw.com.skl.exp.kernel.model6.facade.RatifyFacade;
import tw.com.skl.exp.kernel.model6.logic.RatifyService;
import tw.com.skl.exp.kernel.model6.logic.RatifyStateService;

/**
 * 有核定表的 Service 類別。
 * 
 * <pre>
 * Revision History
 * 2010/12/16 IISI 2010-12-16 updated for 如果沒有費用申請單則須要將本期之周轉金累計到次期,
 * 2011/3/15, Sunkist Wang, update for 過濾併入次期金額等於零，不產生有核定表。
 * </pre>
 * 
 * @author sunkist Wang
 * @version 1.0, 2009/04/21
 */
public class RatifyServiceImpl extends BaseServiceImpl<Ratify, String, RatifyDao> implements RatifyService {

    protected final Log Logger = LogFactory.getLog(RatifyServiceImpl.class);

    private RatifyFacade facade;

    public void appendTurnOver(String settleYM) {
        // B 5.4 將所有主任組長的該工作月未還清之周轉金併入次期工作月，成為次期的累計周轉金。

        // 未輸入必填欄位。
        if (settleYM == null) {
            String[] paramStrs = { Messages.getString("applicationResources",
                    "auto_bind_readTurnOverDataMaintain_tw_com_skl_exp_bo_Ratify_balanceWkYymm", null) };
            throw new ExpRuntimeException(ErrorCode.A10001, paramStrs);
        }

        if ("11".equals(settleYM.substring(4, 6)) || "12".equals(settleYM.substring(4, 6))) {
            // 11和12業績年月，不需執行併入次期業績年月
            throw new ExpRuntimeException(ErrorCode.B10074);
        }

        // 檢核是否執行過BUC5.1功能（有核定表月結算日必須有值）。
        String queryString = "select r from Ratify r where r.middleType.code in ('510', '610') and r.wkYymm = :wkYymm and r.mmFinalDate is null";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("wkYymm", settleYM);
        // 找出是否有主任組長的該業績年月中月結算日沒有值的有核定表。
        List<Ratify> list = getDao().findByNamedParams(queryString, params);
        // 若不是空的，表示未執行過BUC 5.1。
        if (!list.isEmpty()) {
            throw new ExpRuntimeException(ErrorCode.B10050);
        }

        Calendar sysDate = Calendar.getInstance();
        // 檢核是否有次期資料。
        // 計算出次業績年月。
        String nextWkYymm = CalcNextWkYymm(settleYM);
        queryString = "select r from Ratify r where  r.middleType.code in ('510', '610') and r.wkYymm = :settleYM ";
        params = new HashMap<String, Object>();
        params.put("settleYM", settleYM);
        list = getDao().findByNamedParams(queryString, params);
        if (!list.isEmpty()) {
            List<TurnOverDto> dtoList = getDao().findNextWkYymmTurnOver(settleYM);
            Map<String, BigDecimal> nextAmtMap = new TreeMap<String, BigDecimal>();
            for (TurnOverDto dto : dtoList) {
                // 次業績年月之前期週轉金額
                BigDecimal nextWkYymmEarlyAdvpayAmt = dto.getEarlyAdvpayAmt().add(dto.getAdvpayAmt())
                        .subtract(dto.getPaymentAmt());
                nextAmtMap.put(dto.getId(), nextWkYymmEarlyAdvpayAmt);
            }
            for (Ratify ratify : list) {
            	//RE201501754_週轉金累計未核銷餘額併入次次期核定表 CU3178 2015/8/25 START
                //List<Ratify> nextRatifyList = findExistRatify(ratify.getUser().getCode(), nextWkYymm, ratify
                //        .getMiddleType().getCode(), ratify.getUnitCode3());
            	List<Ratify> nextRatifyList = findExistRatifyBy510_610(ratify.getUser().getCode(), nextWkYymm);
            	//RE201501754_週轉金累計未核銷餘額併入次次期核定表 CU3178 2015/8/25 END
                if (!nextRatifyList.isEmpty() && nextRatifyList.size() > 0) {
                    // 更新TABLE方式處理
                    Ratify entity = nextRatifyList.get(0);
                    // 是否系統已併入次期
                    if (ratify.isMmFinalFlag()) {
                        throw new ExpRuntimeException(ErrorCode.B10051);
                    }
                    // TO-DO 申請截止日，轉入核銷日期 ，依據7.1.1轉進來的核銷日期為主。
                    if (nextAmtMap != null) {
                        BigDecimal temp = BigDecimal.ZERO;
                        // IISI 2010-12-16 updated for 如果沒有費用申請單則須要將本期之周轉金累計到次期,
                        // 如果有則計算申請單之累計周轉金
                        temp = ratify.getEarlyAdvpayAmt();
                        if (nextAmtMap.get(ratify.getId()) != null) {
                            temp = nextAmtMap.get(ratify.getId());
                        }
                        entity.setEarlyAdvpayAmt(temp);
                    }
                    entity.setMmFinalFlag(false);
                    entity.setUpdateDate(sysDate);
                    entity = update(entity);
                } else {
                    // IISI 2011-03-15 updated for 過濾併入次期金額等於零，不產生有核定表。
                    if (nextAmtMap != null) {
                        BigDecimal temp = BigDecimal.ZERO;
                        // IISI 2010-12-16 updated for 如果沒有費用申請單則須要將本期之周轉金累計到次期,
                        // 如果有則計算申請單之累計周轉金
                        temp = ratify.getEarlyAdvpayAmt();
                        if (nextAmtMap.get(ratify.getId()) != null) {
                            temp = nextAmtMap.get(ratify.getId());
                        }
                        if (temp.compareTo(BigDecimal.ZERO) != 0) {
                            // 新增TABLE方式處理
                            // IISI 2011-03-15
                            // 本期週轉金不等於零再併入次期，不可產生核定金額為0且上期周轉金為0的核定表。
                            Ratify entity = new Ratify();
                            //RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
                            //RE201601260_週轉金累計未核銷餘額併入次次期 CU3178 2016/5/8 START
                            //entity.setMiddleType(ratify.getMiddleType());
                            //改為依據當前核定表的使用者之職等代號判定”核銷代碼”
//                            if(ratify.getUser()==null || ratify.getUser().getRank()==null){
//                            	//查無資料，顯示《xxx職等代號不存在》訊息
//                            	throw new ExpRuntimeException(ErrorCode.A30005, new String[] { ratify.getUser().getCode()+
//                            			Messages.getString("applicationResources",
//                                        "tw_com_skl_exp_kernel_model6_bo_Rank" , null)});                           
                            //RE201603080_業推(發)_組發_區經理展業費匯款專案 　CU3178 2016/10/06 START       
                            //610轉入條件新增:K8(代理組長)、KA(實習組長)	
                            if(ratify.getUser().getRank().getCode().equals("KB")
                            //RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 end	
                            		||ratify.getUser().getRank().getCode().equals("KM")
                            		||ratify.getUser().getRank().getCode().equals("KD")
                            		||ratify.getUser().getRank().getCode().equals("KE")
                            		||ratify.getUser().getRank().getCode().equals("KG")
                            		||ratify.getUser().getRank().getCode().equals("KH")
                            		||ratify.getUser().getRank().getCode().equals("K8")
                            		||ratify.getUser().getRank().getCode().equals("KA")){
                            	//核銷代碼"610"
                            	MiddleType middleType = facade.getMiddleTypeService().findByCode("610");
                            	entity.setMiddleType(middleType);
                            //RE201603080_業推(發)_組發_區經理展業費匯款專案 　CU3178 2016/10/06 END
                            }else if(ratify.getUser().getRank().getCode().equals("Y2")
                            		||ratify.getUser().getRank().getCode().equals("F1")
                            		||ratify.getUser().getRank().getCode().equals("F2")
                            		||ratify.getUser().getRank().getCode().equals("H1")
                            		||ratify.getUser().getRank().getCode().equals("H2")
                            		||ratify.getUser().getRank().getCode().equals("H3")
                            		||ratify.getUser().getRank().getCode().equals("H4")
                            		||ratify.getUser().getRank().getCode().equals("H5")
                            		||ratify.getUser().getRank().getCode().equals("HA")
                            		||ratify.getUser().getRank().getCode().equals("HB")
                            		||ratify.getUser().getRank().getCode().equals("HC")
                            		||ratify.getUser().getRank().getCode().equals("HD")
                            		||ratify.getUser().getRank().getCode().equals("HE")
                            		||ratify.getUser().getRank().getCode().equals("HF")){
                            	//核銷代碼"510"
                            	MiddleType middleType = facade.getMiddleTypeService().findByCode("510");
                            	entity.setMiddleType(middleType);
                            }else{
                            //RE201800605_傳票不計入107預算實支 ec0416 2018/3/12 start
                            	//查無資料，顯示《xxx職等代號不存在》訊息
//                            	throw new ExpRuntimeException(ErrorCode.A30005, new String[] { 
//                            			ratify.getUser().getCode()+Messages.getString("applicationResources",
//                                        "tw_com_skl_exp_kernel_model6_bo_Rank" , null)});
                            	
                            	//使用者職等代號為空或是不同於現行規則時，改成抓取原核定表內的中分類寫入新的核定表
                            	MiddleType middleType = facade.getMiddleTypeService().findByCode(ratify.getMiddleType().getCode());
                            	entity.setMiddleType(middleType);
                            	
                            }
                            //RE201601260_週轉金累計未核銷餘額併入次次期 CU3178 2016/5/8 END                            
                            entity.setTransDate(sysDate);
                            entity.setWkYymm(nextWkYymm);
                            entity.setWkUyymm(settleYM);
                            entity.setCWk(BigDecimal.ZERO);
                            entity.setRatifyAmt(BigDecimal.ZERO);
                            // TO-DO 申請截止日，轉入核銷日期
                            // ，依照B7.1.1是否有該核銷代號，及次期業績年月為條件，查出任一筆(get(0))的核銷日期。
                            entity.setCancelDate(findNextCancelDate(nextWkYymm, ratify.getMiddleType().getCode()));
                            entity.setEarlyAdvpayAmt(temp);
                            entity.setUser(ratify.getUser());
                            entity.setIdentityId(ratify.getIdentityId());
                            entity.setSalesId(ratify.getSalesId());
                            entity.setSalesName(ratify.getSalesName());
                            entity.setPositionCode(ratify.getPositionCode());
                            entity.setUnitCode1(ratify.getUnitCode1());
                            entity.setUnitName1(ratify.getUnitName1());
                            entity.setUnitCode2(ratify.getUnitCode2());
                            entity.setUnitName2(ratify.getUnitName2());
                            entity.setUnitCode3(ratify.getUnitCode3());
                            entity.setUnitName3(ratify.getUnitName3());
                            entity.setMmFinalFlag(false);
                            entity.setYyFinalFlag(false);
                            // 尚未申請。
                            entity.setRatifyState(findRatifyState(RatifyStateCode.OFFICE_0.getCode()).get(0));
                            entity.setCreateDate(sysDate);
                            create(entity);
                        }

                    }
                }
                // 更新原本當期Ratify。
                ratify.setMmFinalFlag(true);
                ratify.setUpdateDate(sysDate);
                ratify = update(ratify);
            }
        }
    }

    /**
     * 依照B7.1.1是否有該核銷代號，及次期業績年月為條件，查出任一筆有核定檔(get(0))，並取得核銷日期。
     * 
     * @param nextWkYymm
     *            次期業績年月
     * @param reimbursementId
     *            核銷代號
     * @return
     */
    private Calendar findNextCancelDate(String nextWkYymm, String reimbursementId) {
        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("wkYymm", nextWkYymm);
        criteriaMap.put("middleType.code", reimbursementId);
        List<Ratify> list = findByCriteriaMap(criteriaMap);
        if (list.isEmpty() || list.size() < 1) {
            throw new ExpRuntimeException(ErrorCode.B10065);
        }
        return list.get(0).getCancelDate();
    }

    public BigDecimal calcAccumulatedValueForTurnOver(Ratify ratify) {
        BigDecimal advpayAmt = BigDecimal.ZERO;
        BigDecimal paymentAmt = BigDecimal.ZERO;
        List<ExpapplB> expapplBs = ratify.getExpapplBs();
        if (expapplBs != null) {
            for (ExpapplB expapplB : expapplBs) {
                if (expapplB.getSubpoena() != null) { // 排除掉最新一筆資料。
                    if (!ApplStateCode.DELETED.getCode().equals(expapplB.getApplState().getCode())) { // 排除掉被刪件的申請單
                        advpayAmt = advpayAmt.add(expapplB.getAdvpayAmt() == null ? BigDecimal.ZERO : expapplB
                                .getAdvpayAmt());
                        paymentAmt = paymentAmt.add(expapplB.getPaymentAmt() == null ? BigDecimal.ZERO : expapplB
                                .getPaymentAmt());
                    }
                }
            }
        }
        return ratify.getEarlyAdvpayAmt().add(advpayAmt.subtract(paymentAmt));
    }

    public BigDecimal calcPayAmount(Ratify ratify, BigDecimal advpayAmt, BigDecimal proofTotal) {
        if (this.calcAccumulatedValueForTurnOver(ratify).compareTo(BigDecimal.ZERO) == 0
                || proofTotal.compareTo(this.calcMayAmt(ratify)) < 0) {
            return proofTotal.add(advpayAmt);
        } else if (proofTotal.compareTo(this.calcMayAmt(ratify)) > 0
                && proofTotal.compareTo(this.calcMayAmt(ratify).add(this.calcAccumulatedValueForTurnOver(ratify))) <= 0) {
            return this.calcMayAmt(ratify);
        } else if (proofTotal.compareTo(this.calcMayAmt(ratify).add(calcAccumulatedValueForTurnOver(ratify))) > 0) {
            return this.calcMayAmt(ratify);
        } else {
            return advpayAmt.add(proofTotal);
        }
    }

    private List<ExpapplB> getSortedAndUndeletedAppl(Ratify ratify) {
        List<ExpapplB> expapplBs = new ArrayList<ExpapplB>();
        expapplBs.addAll(ratify.getExpapplBs());
        List<ExpapplB> deleted = new ArrayList<ExpapplB>();
        for (ExpapplB expapplB : expapplBs) { // 排除尚未寫入資料庫或已刪件的申請單
            if (expapplB.getId() == null || ApplStateCode.DELETED.getCode().equals(expapplB.getApplState().getCode())) {
                deleted.add(expapplB);
            }
        }
        expapplBs.removeAll(deleted);
        Collections.sort(expapplBs);
        return expapplBs;
    }

    public BigDecimal calcMayAmt(Ratify ratify) {
        // 若核定表的核銷次數為零次，則可領金額之數值 = 核定表之核定金額；
        // 若核定表的核銷次數為一次，則可領金額之數值 = 核定表之核定金額減去已申請之金額。
        // 已申請金額 = 前期已申請金額 + 前期實支

        List<ExpapplB> expapplBs = this.getSortedAndUndeletedAppl(ratify);

        if (expapplBs.size() == 0) {
            return ratify.getRatifyAmt();
        }

        // 計算已申請金額
        BigDecimal appliedAmt = BigDecimal.ZERO;
        for (ExpapplB expapplB : expapplBs) {
            if (expapplB.getSubpoena() != null) { // 排除掉最新一筆資料
                appliedAmt = appliedAmt.add(expapplB.getPayAmt());
            }
        }

        return ratify.getRatifyAmt().subtract(appliedAmt);
    }

    public BigDecimal calcPaymentAmt(Ratify ratify, BigDecimal proofTotal, BigDecimal rejectAmt) {
        if (this.calcAccumulatedValueForTurnOver(ratify).compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        } else if (proofTotal.compareTo(this.calcMayAmt(ratify)) < 0) {
            return BigDecimal.ZERO;
        } else if (proofTotal.compareTo(this.calcMayAmt(ratify)) > 0
                && proofTotal.compareTo(this.calcMayAmt(ratify).add(this.calcAccumulatedValueForTurnOver(ratify))) <= 0) {
            return proofTotal.subtract(this.calcMayAmt(ratify)).subtract(rejectAmt);
        } else if (proofTotal.compareTo(this.calcMayAmt(ratify).add(this.calcAccumulatedValueForTurnOver(ratify))) > 0) {
            return this.calcAccumulatedValueForTurnOver(ratify);
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    public List<String> exportCancelData(KindType kindType, String settleYM, Calendar settleYMD, Calendar startDate,
            Calendar endDate) {

        List<String> dataList = new ArrayList<String>();

        if (KindTypeCode.DIRECTOR.getCode().equals(kindType.getCode())) { // 主任組長核銷檔。

            // 選擇主任組長核銷檔時，應檢核結算業績年月所關連之申請單無在途之申請單，若有在途申請單，
            // 則系統顯示警示訊息：「尚有費用申請單未完成核銷，請先確認此業績年月所有費用申請單皆完成日結，再進行此功能!」。
            List<ExpapplB> wayExpapplB = getFacade().getExpapplBService().findWayExpapplBByWkYymm(settleYM);
            if (!wayExpapplB.isEmpty()) {

                throw new ExpRuntimeException(ErrorCode.B10010);
            }

            Map<String, Object> criteriaMap = new HashMap<String, Object>();
            criteriaMap.put("wkYymm", settleYM);
            StringBuffer queryString = new StringBuffer();
            queryString.append(" select e.ratify from ExpapplB e");
            queryString
                    .append(" where e.ratify.id in (select x.id from Ratify x where x.middleType.code in ('510', '610') and x.wkYymm = :wkYymm)");
            queryString
                    .append(" and e.applState.id in (select y.id from ApplState y where y.sysType.code = 'B' and y.code < '99')");
            List<Ratify> resultList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            queryString = new StringBuffer();
            queryString.append(" select r from Ratify r");
            queryString
                    .append(" where r.id in (select x.id from Ratify x where x.middleType.code in ('510', '610') and x.wkYymm = :wkYymm)");
            List<Ratify> allRatifyList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            List<Ratify> allList = (List<Ratify>) CollectionUtils.union(allRatifyList, resultList);

            // 回押該結算【業績年月】之核銷記錄中的【結算年月日】。
            // Calendar updateDateTime = Calendar.getInstance();
            Set<Ratify> ratifySet = new HashSet<Ratify>();
            for (Ratify ratify : allList) {
                if (!ratifySet.contains(ratify)) {
                    ratifySet.add(ratify);
                }
            }
            for (Ratify ratify : ratifySet) {
                Ratify obj = findByPK(ratify.getId());
                // 也就是月結算日欄位。
                obj.setMmFinalDate(settleYMD);
                // obj.setUpdateDate(updateDateTime);
                obj = super.update(obj);
            }

            for (Ratify ratify : ratifySet) {
                // 加入檔案下載工具
                dataList.add(assembleRatify(ratify, ""));
            }

        } else if (KindTypeCode.YCC.getCode().equals(kindType.getCode())) { // 獎勵YCC核銷檔。
            List<ExpapplB> expapplBList = new ArrayList<ExpapplB>();

            // 依據輸入作帳起迄日檢核有核定表是否有在途申請單
            expapplBList = getFacade().getExpapplBService().findWay3ExpapplBBySubpoenaDate(startDate, endDate);
            if (!expapplBList.isEmpty()) {

                throw new ExpRuntimeException(ErrorCode.B10016);
            }

            expapplBList = getFacade().getExpapplBService().findExpapplBByKindType(startDate, endDate,
                    kindType.getCode());
            for (ExpapplB expapplB : expapplBList) {
                // 加入檔案下載工具
                dataList.add(assembleRatify(expapplB.getRatify(), expapplB.getExpApplNo()));
            }

            if (dataList != null && dataList.size() > 0) {
                dataList = combineResult(dataList);
            }

        } else if (KindTypeCode.UNIT_AWARD.getCode().equals(kindType.getCode())) { // 駐區業獎核銷檔。
            List<ExpapplB> expapplBList = new ArrayList<ExpapplB>();

            // 依據輸入作帳起迄日檢核有核定表是否有在途申請單
            expapplBList = getFacade().getExpapplBService().findWay4ExpapplBBySubpoenaDate(startDate, endDate);
            if (!expapplBList.isEmpty()) {

                throw new ExpRuntimeException(ErrorCode.B10016);
            }

            expapplBList = getFacade().getExpapplBService().findExpapplBByKindType(startDate, endDate,
                    kindType.getCode());
            for (ExpapplB expapplB : expapplBList) {
                // 加入檔案下載工具
                dataList.add(assembleRatify(expapplB.getRatify(), expapplB.getExpApplNo()));
            }

            if (dataList != null && dataList.size() > 0) {
                dataList = combineResult(dataList);
            }
        }
        else if (KindTypeCode.ORG_DEVELOP.getCode().equals(kindType.getCode())) { // 組織發展費核銷檔。
            // 組織發展費核銷檔與主任組長核銷檔規則相同, 但週轉金欄位空白
            // 選擇主任組長核銷檔時，應檢核結算業績年月所關連之申請單無在途之申請單，若有在途申請單，
            // 則系統顯示警示訊息：「尚有費用申請單未完成核銷，請先確認此業績年月所有費用申請單皆完成日結，再進行此功能!」。
            List<ExpapplB> wayExpapplB = getFacade().getExpapplBService().find810WayExpapplBByWkYymm(settleYM);
            if (!wayExpapplB.isEmpty()) {

                throw new ExpRuntimeException(ErrorCode.B10010);
            }

            Map<String, Object> criteriaMap = new HashMap<String, Object>();
            criteriaMap.put("wkYymm", settleYM);
            StringBuffer queryString = new StringBuffer();
            queryString.append(" select e.ratify from ExpapplB e");
            queryString
                    .append(" where e.ratify.id in (select x.id from Ratify x where x.middleType.code in ('810') and x.wkYymm = :wkYymm)");
            queryString
                    .append(" and e.applState.id in (select y.id from ApplState y where y.sysType.code = 'B' and y.code < '99')");
            List<Ratify> resultList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            queryString = new StringBuffer();
            queryString.append(" select r from Ratify r");
            queryString
                    .append(" where r.id in (select x.id from Ratify x where x.middleType.code in ('810') and x.wkYymm = :wkYymm)");
            List<Ratify> allRatifyList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            List<Ratify> allList = (List<Ratify>) CollectionUtils.union(allRatifyList, resultList);

            // 回押該結算【業績年月】之核銷記錄中的【結算年月日】。
            // Calendar updateDateTime = Calendar.getInstance();
            Set<Ratify> ratifySet = new HashSet<Ratify>();
            for (Ratify ratify : allList) {
                if (!ratifySet.contains(ratify)) {
                    ratifySet.add(ratify);
                }
            }
            for (Ratify ratify : ratifySet) {
                Ratify obj = findByPK(ratify.getId());
                // 也就是月結算日欄位。
                obj.setMmFinalDate(settleYMD);
                // obj.setUpdateDate(updateDateTime);
                obj = super.update(obj);
            }

            for (Ratify ratify : ratifySet) {
                // 加入檔案下載工具
                dataList.add(assembleRatify(ratify, ""));
            }
        }
        //RE201701090_行銷推廣費 CU3178 2017/4/10 START
        else if (KindTypeCode.CHIEF_MARKING.getCode().equals(kindType.getCode())) { // 專銷制行銷推廣費。
            // 專銷制行銷推廣費與組織發展費核銷檔
            // 選擇專銷制行銷推廣費時，應檢核結算業績年月所關連之申請單無在途之申請單，若有在途申請單，
            // 則系統顯示警示訊息：「尚有費用申請單未完成核銷，請先確認此業績年月所有費用申請單皆完成日結，再進行此功能!」。
            List<ExpapplB> wayExpapplB = getFacade().getExpapplBService().find270WayExpapplBByWkYymm(settleYM);
            if (!wayExpapplB.isEmpty()) {

                throw new ExpRuntimeException(ErrorCode.B10010);
            }

            Map<String, Object> criteriaMap = new HashMap<String, Object>();
            criteriaMap.put("wkYymm", settleYM);
            StringBuffer queryString = new StringBuffer();
            queryString.append(" select e.ratify from ExpapplB e");
            queryString
                    .append(" where e.ratify.id in (select x.id from Ratify x where x.middleType.code in ('270') and x.wkYymm = :wkYymm)");
            queryString
                    .append(" and e.applState.id in (select y.id from ApplState y where y.sysType.code = 'B' and y.code < '99')");
            List<Ratify> resultList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            queryString = new StringBuffer();
            queryString.append(" select r from Ratify r");
            queryString
                    .append(" where r.id in (select x.id from Ratify x where x.middleType.code in ('270') and x.wkYymm = :wkYymm)");
            List<Ratify> allRatifyList = super.getDao().findByNamedParams(queryString.toString(), criteriaMap);

            List<Ratify> allList = (List<Ratify>) CollectionUtils.union(allRatifyList, resultList);

            // 回押該結算【業績年月】之核銷記錄中的【結算年月日】。
            // Calendar updateDateTime = Calendar.getInstance();
            Set<Ratify> ratifySet = new HashSet<Ratify>();
            for (Ratify ratify : allList) {
                if (!ratifySet.contains(ratify)) {
                    ratifySet.add(ratify);
                }
            }
            for (Ratify ratify : ratifySet) {
                Ratify obj = findByPK(ratify.getId());
                // 也就是月結算日欄位。
                obj.setMmFinalDate(settleYMD);
                // obj.setUpdateDate(updateDateTime);
                obj = super.update(obj);
            }

            for (Ratify ratify : ratifySet) {
                // 加入檔案下載工具
                dataList.add(assembleRatify(ratify, ""));
            }
        }
        //RE201701090_行銷推廣費 CU3178 2017/4/10 END

        return dataList;
    }

    /**
     * 依有核定檔格式組成字串。
     * 
     * @param ratify
     *            有核定檔
     * @return
     */
    private String assembleRatify(Ratify ratify, String expApplNo) {

        String blank = "";
        StringBuffer strBuff = new StringBuffer();
        if (ratify.getSalesId() != null) {
            strBuff.append(ratify.getSalesId());
        }
        strBuff.append(",");
        strBuff.append(ratify.getUser().getCode());
        strBuff.append(",");
        if (ratify.getSalesName() != null) {
            strBuff.append(ratify.getSalesName());
        }
        strBuff.append(",");
        if (ratify.getIdentityId() != null) {
            strBuff.append(ratify.getIdentityId());
        }
        strBuff.append(",");
        if (ratify.getPositionCode() != null) {
            strBuff.append(ratify.getPositionCode());
        }
        strBuff.append(",");
        strBuff.append(ratify.getUnitCode3());
        strBuff.append(",");
        if (ratify.getUnitName3() != null) {
            strBuff.append(ratify.getUnitName3());
        }
        strBuff.append(",");
        strBuff.append(ratify.getUnitCode2());
        strBuff.append(",");
        if (ratify.getUnitName2() != null) {
            strBuff.append(ratify.getUnitName2());
        }
        strBuff.append(",");
        strBuff.append(ratify.getUnitCode1());
        strBuff.append(",");
        if (ratify.getUnitName1() != null) {
            strBuff.append(ratify.getUnitName1());
        }
        strBuff.append(",");

        strBuff.append(GetRocWkYymm(ratify.getWkYymm()));
        strBuff.append(",");
        strBuff.append(ratify.getMiddleType().getCode());
        strBuff.append(",");
        if (ratify.getCWk() != null) {
            strBuff.append(ratify.getCWk());
        }
        strBuff.append(",");
        if (ratify.getCancelDate() != null) {
            strBuff.append(DateUtils.getRocDateStrByCalendar(ratify.getCancelDate()));
        }
        strBuff.append(",");
        if (ratify.getRatifyAmt() != null) {
            strBuff.append(ratify.getRatifyAmt());
        }
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");
        strBuff.append(blank);
        strBuff.append(",");

        // 第一次核銷日期。
        Calendar firstSubpoenaDate = null;
        // 第二次核銷日期。
        Calendar secondSubpoenaDate = null;
        // 第一次核銷金額。
        BigDecimal firstPayAmt = BigDecimal.ZERO;
        // 第二次核銷金額。
        BigDecimal secondPayAmt = BigDecimal.ZERO;

        // 找出第一次核銷日期。(剔除純還款的申請單後，按照傳票日期排序)
        List<ExpapplB> cancelExpapplBList = getFacade().getExpapplBService().findCancelExpapplBByRatify(ratify);
        if (!cancelExpapplBList.isEmpty()) {
            if (StringUtils.isNotBlank(expApplNo)) {
                for (ExpapplB entity : cancelExpapplBList) {
                    if (expApplNo.equals(entity.getExpApplNo())) {
                        firstSubpoenaDate = entity.getSubpoena().getSubpoenaDate();
                        firstPayAmt = entity.getPayAmt();
                    }
                }
            } else {
                firstSubpoenaDate = cancelExpapplBList.get(0).getSubpoena().getSubpoenaDate();
                // 找出第一次核銷金額。
                ExpapplB expapplB = cancelExpapplBList.get(0);
                firstPayAmt = expapplB.getPayAmt();
            }
        }

        // 第一次核銷日期。
        if (firstSubpoenaDate == null) {
            strBuff.append(blank);
        } else {
            strBuff.append(DateUtils.getRocDateStrByCalendar(firstSubpoenaDate));
        }
        strBuff.append(",");

        // 第一次核銷金額。(實支金額)
        if (firstPayAmt == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(String.valueOf(firstPayAmt));
        }
        strBuff.append(",");

        //RE201701090_行銷推廣費 CU3178 2017/4/10 START
        // 核銷檔案為主任組長及組織發展費時，找出第二次核銷日期。(剔除純還款的申請單後，按照傳票日期排序)
        if (MiddleTypeCode.CODE_510.getCode().equals(ratify.getMiddleType().getCode())
                || MiddleTypeCode.CODE_610.getCode().equals(ratify.getMiddleType().getCode())
                || MiddleTypeCode.CODE_810.getCode().equals(ratify.getMiddleType().getCode())
                || MiddleTypeCode.CODE_270.getCode().equals(ratify.getMiddleType().getCode())) {
            if (cancelExpapplBList.size() >= 2) {
                secondSubpoenaDate = cancelExpapplBList.get(1).getSubpoena().getSubpoenaDate();
                // 找出第二次核銷金額。
                ExpapplB expapplB = cancelExpapplBList.get(1);
                secondPayAmt = expapplB.getPayAmt();
            }
        }
        //RE201701090_行銷推廣費 CU3178 2017/4/10 END
        // 第二次核銷日期。
        if (secondSubpoenaDate == null) {
            strBuff.append(blank);
        } else {
            strBuff.append(DateUtils.getRocDateStrByCalendar(secondSubpoenaDate));
        }
        strBuff.append(",");

        // 第二次核銷金額。(實支金額)
        if (secondPayAmt == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(String.valueOf(secondPayAmt));
        }
        strBuff.append(",");

        BigDecimal amount = BigDecimal.ZERO;
        // 核銷檔案為主任組長時，找出該核定檔所有申請單中的借支金額。
        if (MiddleTypeCode.CODE_510.getCode().equals(ratify.getMiddleType().getCode())
                || MiddleTypeCode.CODE_610.getCode().equals(ratify.getMiddleType().getCode())) {
            Map<String, Object> criteriaMap = new HashMap<String, Object>();
            criteriaMap.put("ratify", ratify);
            List<ExpapplB> allExpapplBList = getFacade().getExpapplBService().findByCriteriaMap(criteriaMap);
            for (ExpapplB expapplB : allExpapplBList) {
                BigDecimal temp = BigDecimal.ZERO;
                if (expapplB.getAdvpayAmt() != null
                        && ApplStateCode.DAILY_CLOSED.getCode().equals(expapplB.getApplState().getCode())) {
                    temp = expapplB.getAdvpayAmt();
                }
                amount = amount.add(temp);
            }
        }
        strBuff.append(String.valueOf(amount));

        strBuff.append(",");

        BigDecimal accmulatedValueForTurnOver = BigDecimal.ZERO;
        // 核銷檔案為主任組長時，算出週轉金餘額。
        if (MiddleTypeCode.CODE_510.getCode().equals(ratify.getMiddleType().getCode())
                || MiddleTypeCode.CODE_610.getCode().equals(ratify.getMiddleType().getCode())) {
            accmulatedValueForTurnOver = calcAccumulatedValueForTurnOver(ratify);
        }
        strBuff.append(String.valueOf(accmulatedValueForTurnOver));

        return strBuff.toString();
    }

    /**
     * 將[獎勵YCC核銷檔]及[駐區業獎核銷檔]以駐區代號及費用中分類(核銷代號)加總
     * 需求單:RE201000872
     * 
     * @param dataList
     *            來源資料

     * @return
     */
    @SuppressWarnings("unused")
    public List<String> combineResult(List<String> dataList)
    {
        int intUnitCode2Index = 7;       //區部代號位置
        int intCancelCodeIndex = 12;     //核銷碼位置
        int intCancelDateIndex = 46;     //核銷日位置
        int intCancelAmtIndex = 47;      //核銷金額位置
        int intGroupCancelAmtIndex = 17; //加總的核銷金額位置
        int intArraycopyCount = 13;
        int intNewColumnCount = 22;      //新檔案的欄位數
        int intInitArrayBlankInd1 = 0; 
        int intInitArrayBlankInd2 = 18;
        int intInitArrayZeroInd1 = 49;
        int intInitArrayZeroInd2 = 52; //the index of the last element (exclusive) to be filled with the specified value.
        String strBlank = "";
        String strZero = "0";
        
        List<String> newDataList = new ArrayList<String>();
        String[] split = dataList.get(0).split(",");
        String[] buffer = new String[split.length]; //放加總資料
        double dblCancelAmt = 0;        
        java.text.DecimalFormat myformat = new java.text.DecimalFormat("#"); //設定格式, 不要小數點
        String strTemp = "";        
        Calendar firstSubpoenaDate = Calendar.getInstance(); // 第一次核銷日期,設為今天。
        
        java.util.Arrays.fill(buffer, strBlank); //初始陣列, 填空字串
        java.util.Arrays.fill(buffer, intInitArrayZeroInd1, intInitArrayZeroInd2, strZero); //初始陣列, 最後三欄都放0
        java.lang.System.arraycopy(split, 0, buffer, 0, intArraycopyCount); //複製前intArraycopyCount個元素
        for (int i = 0; i < dataList.size(); i++)
        {
            split = dataList.get(i).split(","); //以逗號分開
            
            if (buffer[intUnitCode2Index].equals(split[intUnitCode2Index]) && buffer[intCancelCodeIndex].equals(split[intCancelCodeIndex])) //若駐區代號與核銷代號相同
            {
                try
                {
                    dblCancelAmt += java.lang.Double.parseDouble(split[intCancelAmtIndex]); //加起來
                }
                catch(Exception ex)
                {
                    dblCancelAmt += 0;
                }
            }
            else //遇到不同的
            {   
                buffer[intCancelDateIndex] = DateUtils.getRocDateStrByCalendar(firstSubpoenaDate); //核銷日期
                buffer[intCancelAmtIndex] = myformat.format(dblCancelAmt); //核銷金額,進行格式化
                for(int j =0 ; j < buffer.length; j++) //陣列轉逗號分隔字串
                {
                    strTemp += buffer[j] + ",";
                }
                strTemp = strTemp.substring(0, strTemp.length() - 1); //去掉結尾的 ','                
                newDataList.add(strTemp); //加總結果加入新的 list裡                
                
                strTemp = ""; //清空暫存字串
                buffer = new String[split.length]; //建新的buffer陣列
                java.util.Arrays.fill(buffer, strBlank); //初始陣列, 填空字串
                java.util.Arrays.fill(buffer, intInitArrayZeroInd1, intInitArrayZeroInd2, strZero); //初始陣列, 最後三欄都放0
                java.lang.System.arraycopy(split, 0, buffer, 0, intArraycopyCount); //複製前intArraycopyCount個元素
                
                try
                {
                    dblCancelAmt = java.lang.Double.parseDouble(split[intCancelAmtIndex]); //
                }
                catch(Exception ex)
                {
                    dblCancelAmt = 0;
                } 
            }
        } //for 
        
        buffer[intCancelDateIndex] = DateUtils.getRocDateStrByCalendar(firstSubpoenaDate); //核銷日期
        buffer[intCancelAmtIndex] = myformat.format(dblCancelAmt); //進行格式化
        for(int j =0 ; j < buffer.length; j++) //陣列轉逗號分隔字串
        {
            strTemp += buffer[j] + ",";
        }
        strTemp = strTemp.substring(0, strTemp.length() - 1); //去掉結尾的 ','
        
        newDataList.add(strTemp); //加入新的 list裡      
        
        return newDataList;
    }

    @SuppressWarnings("unchecked")
    public List<String> exportTurnOverData(Calendar startDate, Calendar endDate) {

        List<String> dataList = new ArrayList<String>();
        List<ExpapplB> expapplBList = new ArrayList<ExpapplB>();
        // 依據輸入作帳起迄日檢核有核定表是否有在途申請單
        expapplBList = getFacade().getExpapplBService().findWay2ExpapplBBySubpoenaDate(startDate, endDate);
        if (!expapplBList.isEmpty()) {

            throw new ExpRuntimeException(ErrorCode.B10016);
        }

        // SK-marked 改以sql取得下載資料。
        // expapplBList =
        // getFacade().getExpapplBService().findTurnOverExpapplB(startDate,
        // endDate);
        List daoList = new ArrayList();
        daoList = getDao().exportTurnOverData(startDate, endDate);

        if (!CollectionUtils.isEmpty(daoList)) {
            for (Object obj : daoList) {

                Object[] record = (Object[]) obj;
                String unitCode2 = (String) record[0];
                String unitCode3 = (String) record[1];
                String userCode = (String) record[2];
                String wkyymm = (String) record[3];
                String indate = (String) record[4];
                BigDecimal advpayAmt = (BigDecimal) record[5];
                BigDecimal ultAmt = (BigDecimal) record[6];
                BigDecimal solAmt1 = (BigDecimal) record[7];
                BigDecimal solAmt2 = (BigDecimal) record[8];

                Timestamp ts = (Timestamp) record[9];
                Calendar solDate1 = null;
                if (ts != null) {
                    solDate1 = Calendar.getInstance();
                    solDate1.setTimeInMillis(ts.getTime());
                }

                ts = (Timestamp) record[10];
                Calendar solDate2 = null;
                if (ts != null) {
                    solDate2 = Calendar.getInstance();
                    solDate2.setTimeInMillis(ts.getTime());
                }
                BigDecimal pileAmt = (BigDecimal) record[11];

                ts = (Timestamp) record[12];
                Calendar accDate = null;
                if (ts != null) {
                    accDate = Calendar.getInstance();
                    accDate.setTimeInMillis(ts.getTime());
                }

                ExportTurnOverDto exportTurnOverDto = new ExportTurnOverDto();
                exportTurnOverDto.setUnitCode2(unitCode2);
                exportTurnOverDto.setUnitCode3(unitCode3);
                exportTurnOverDto.setUserCode(userCode);
                exportTurnOverDto.setWkyymm(wkyymm);
                exportTurnOverDto.setIndate(indate);
                exportTurnOverDto.setAdvpayAmt(advpayAmt);
                exportTurnOverDto.setUltAmt(ultAmt);
                exportTurnOverDto.setSolAmt1(solAmt1);
                exportTurnOverDto.setSolAmt2(solAmt2);
                exportTurnOverDto.setSolDate1(solDate1);
                exportTurnOverDto.setSolDate2(solDate2);
                exportTurnOverDto.setPileAmt(pileAmt);
                exportTurnOverDto.setAccDate(accDate);
                dataList.add(assembleTurnOverData(exportTurnOverDto));
            }
        }

        return dataList;
    }

    /**
     * 組成周轉金資料。
     * 
     * @param ratify
     * @return
     */
    private String assembleTurnOverData(ExportTurnOverDto dto) {

        String blank = "";
        StringBuffer strBuff = new StringBuffer();
        strBuff.append(dto.getUnitCode2());
        strBuff.append(",");
        strBuff.append(dto.getUnitCode3());
        strBuff.append(",");
        strBuff.append(dto.getUserCode());
        strBuff.append(",");
        strBuff.append(GetRocWkYymm(dto.getWkyymm()));
        strBuff.append(",");
        strBuff.append(NextMonth(dto.getWkyymm()));
        strBuff.append(",");

        // INSAMT：當期借支
        if (dto.getAdvpayAmt() == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(dto.getAdvpayAmt());
        }
        strBuff.append(",");

        // ULTAMT：前期累計
        if (dto.getUltAmt() == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(dto.getUltAmt());
        }
        strBuff.append(",");

        // SOLAMT1：第一次還款金額
        if (dto.getSolAmt1() == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(String.valueOf(dto.getSolAmt1()));
        }
        strBuff.append(",");

        // SOLAMT2：第二次還款金額
        if (dto.getSolAmt2() == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(String.valueOf(dto.getSolAmt2()));
        }
        strBuff.append(",");

        // SOLDATE1：第一次還款年月日
        if (dto.getSolDate1() == null) {
            strBuff.append(blank);
        } else {
            strBuff.append(DateUtils.getRocDateStrByCalendar(dto.getSolDate1()));
        }
        strBuff.append(",");

        // SOLDATE2：第二次還款年月日
        if (dto.getSolDate2() == null) {
            strBuff.append(blank);
        } else {
            strBuff.append(DateUtils.getRocDateStrByCalendar(dto.getSolDate2()));
        }
        strBuff.append(",");

        // PILEAMT：周轉金累計
        // 當期借支+前期累計-第一次還款-第二次還款
        if (dto.getPileAmt() == null) {
            strBuff.append(BigDecimal.ZERO);
        } else {
            strBuff.append(dto.getPileAmt());
        }
        strBuff.append(",");

        // ACCMMDD：作帳年月日
        // 當期借支的作帳年月日
        if (dto.getAccDate() == null) {
            strBuff.append(blank);
        } else {
            strBuff.append(DateUtils.getRocDateStrByCalendar(dto.getAccDate()));
        }

        return strBuff.toString();
    }

    @SuppressWarnings("unchecked")
    public List<UnprocessedRatifyDto> findRatifyForUser(User user, boolean showAll) {
        List<UnprocessedRatifyDto> result = new ArrayList<UnprocessedRatifyDto>();
        List list = new ArrayList();
        List<User> users = facade.getUserService().findUserInGeneralMgrGroup();
        StringBuffer userListStr = new StringBuffer();
        if (users != null && users.contains(user)) {
            // 使用者為審計初審經辦、稅務和總帳經辦群組的使用者。

        	//RE201400156-各項業務費用核銷進度統計 CU3178 2014/5/20 START
            /*Set<User> authUsers = facade.getUserService().findAuthUserForRatifyQuery(user);
            if (!CollectionUtils.isEmpty(authUsers)) {
                // IISI-20110325 修正 ORA-01795，每 500 筆拆出一個 IN STATEMENT
                int i = 0;
                for (User authUser : authUsers) {
                    if (i != 0) {
                        userListStr.append(", ");
                    }
                    userListStr.append("'").append(authUser.getCode()).append("'");
                    i++;
                    if (i > 500) {
                        userListStr.append(") OR U.CODE IN (");
                        i = 0;
                    }
                }
            }*/          
        	List<Department> ownDepartments =user.getOwnDepartments();  //如果是會計部群組改為以轄屬單位尋找有核定表
        	if (!CollectionUtils.isEmpty(ownDepartments)) {
        	int i=0;
        	for(Department department :ownDepartments){
        		if(i!=0){
        			userListStr.append(", ");
        		}
        		 userListStr.append("'").append(department.getCode()).append("'");
                 i++;
                 if (i > 500) {
                     userListStr.append(") OR RATIFY.UNIT_CODE3 IN (");
                     i = 0;     
                 	}
        		}        
        	}else{
        		 userListStr.append("''");
        	}
        	//RE201400156-各項業務費用核銷進度統計 CU3178 2014/5/20 END
            list = getDao().findRatifyForAuditUser(userListStr.toString(), showAll);
        } else {
            if (GroupCode.INTERIOR.getCode().equals(user.getGroup().getCode())
                    || GroupCode.BIZ_DIVISION.getCode().equals(user.getGroup().getCode())
                    || GroupCode.BIZ_SECTION.getCode().equals(user.getGroup().getCode())
                    || GroupCode.BIZ_UNIT.getCode().equals(user.getGroup().getCode())) {
                // 內務、業務部室主管、業務區部主管、業務單位主管/區經理
                Set<User> authUsers = facade.getUserService().findAuthUserForRatifyQuery(null);
                if (!CollectionUtils.isEmpty(authUsers)) {
                    // IISI-20110325 修正 ORA-01795，每 500 筆拆出一個 IN STATEMENT
                    int i = 0;
                    for (User authUser : authUsers) {
                        if (i != 0) {
                            userListStr.append(", ");
                        }
                        userListStr.append("'").append(authUser.getCode()).append("'");
                        i++;
                        if (i > 500) {
                            userListStr.append(") OR U.CODE IN (");
                            i = 0;
                        }
                    }
                }
            } else {
                // 主任/組長
                userListStr.append("'").append(user.getCode()).append("'");
            }
            list = getDao().findRatifyForOutUser(userListStr.toString(), showAll);
        }
        if (!CollectionUtils.isEmpty(list)) {
            for (Object obj : list) {
                Object[] record = (Object[]) obj;
                String wkYyymm = (String) record[0];
                String unitName = (String) record[1];
                String userCode = (String) record[2];
                String midCode = (String) record[3];
                BigDecimal mayAmt = (BigDecimal) record[4];
                BigDecimal advPayAmt = (BigDecimal) record[5];
                String unitCode = (String) record[6];
                UnprocessedRatifyDto unprocessedRatifyDto = new UnprocessedRatifyDto();
                unprocessedRatifyDto.setWkYymm(wkYyymm);
                unprocessedRatifyDto.setUnitName(unitName);
                unprocessedRatifyDto.setUserCode(userCode);
                unprocessedRatifyDto.setMidCode(midCode);
                unprocessedRatifyDto.setMayAmt(mayAmt);
                unprocessedRatifyDto.setAdvpayAmt(advPayAmt);
                unprocessedRatifyDto.setUnitCode(unitCode);
                result.add(unprocessedRatifyDto);
            }
        }
        return result;
    }

    public List<Ratify> findExistRatify(String code, String wkYymm, String reimbursementId, String unitCode3) {

        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("code", code);
        criteriaMap.put("wkYymm", wkYymm);
        criteriaMap.put("mid_code", reimbursementId);
        String queryString = "";
        if (unitCode3 != null) {
            criteriaMap.put("unitCode3", unitCode3);
            queryString = "select r from Ratify r where r.user.code = :code and r.wkYymm = :wkYymm and r.middleType.code = :mid_code and r.unitCode3 = :unitCode3 ";
        } else {
            queryString = "select r from Ratify r where r.user.code = :code and r.wkYymm = :wkYymm and r.middleType.code = :mid_code ";
        }
        return super.getDao().findByNamedParams(queryString, criteriaMap);
    }

    public boolean doCheckIsNotApply(Ratify ratify) {
        RatifyState ratifyState = ratify.getRatifyState();
        // 只是要去取得Code (0)，不理會Office KindType
        if (!RatifyStateCode.OFFICE_0.getCode().equals(ratifyState.getCode())) {
            return true;
        }
        return false;
    }

    public List<RatifyState> findRatifyState(String code) {
        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("code", code);
        return getFacade().getRatifyStateService().findByCriteriaMap(criteriaMap);
    }

    public List<User> findUserForAddRatify(String code) {
        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("code", code);
        return getFacade().getUserService().findByCriteriaMap(criteriaMap);
    }

    public List<MiddleType> findMiddleTypeForAddRatify(String reimbursementId) {
        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("code", reimbursementId);
        return getFacade().getMiddleTypeService().findByCriteriaMap(criteriaMap);
    }

    public void create(final List<Ratify> objList) {
        for (Ratify obj : objList) {
            super.create(obj);
        }
    }

    public void update(final List<Ratify> objList) {

        // Calendar updateDateTime = Calendar.getInstance();
        for (Ratify obj : objList) {
            Ratify r = findExistRatify(obj.getUser().getCode(), obj.getWkYymm(), obj.getMiddleType().getCode(),
                    obj.getUnitCode3()).get(0);

            // 轉入存值，與費用系統無關。
            r.setSalesId(obj.getSalesId());
            r.setSalesName(obj.getSalesName());
            r.setIdentityId(obj.getIdentityId());
            r.setPositionCode(obj.getPositionCode());
            r.setUnitName1(obj.getUnitName1());
            r.setUnitName2(obj.getUnitName2());
            r.setUnitName3(obj.getUnitName3());
            r.setCWk(obj.getCWk());

            r.setCancelDate(obj.getCancelDate());
            r.setRatifyAmt(obj.getRatifyAmt());
            r.setUnitCode1(obj.getUnitCode1());
            r.setUnitCode2(obj.getUnitCode2());
            r.setUnitCode3(obj.getUnitCode3());
            // 因為 BO Ratify 中 implement SystemDateEntityListener的時候在整批更新會有問題。
            // r.setUpdateDate(updateDateTime);
            // RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/22 start
            r.setSalaryCode1(obj.getSalaryCode1()); 
            r.setAllowanceItem1(obj.getAllowanceItem1()); 
            r.setAmount1(obj.getAmount1());
            // RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/22 end
            super.update(r);
        }
    }

    public RatifyFacade getFacade() {
        return facade;
    }

    public void setFacade(RatifyFacade facade) {
        this.facade = facade;
    }

    public Map<String, Object> doImportRatify(String fileName, InputStream is, Map<String, Object> params) {
        Map<String, Object> result = null;
        try {
            result = getFacade().getFileImportService().fileRead(fileName, is, params);
        } catch (ExpRuntimeException e) {
            throw e;
        }
        return result;
    }

    /**
     * 將西元年(YYYY)業績年月轉成民國年(YYY)業績年月。
     * 
     * @param wkYymm
     *            業績年月(西元年)
     * @return
     */
    private static String GetRocWkYymm(String wkYymm) {
        StringBuffer date = new StringBuffer();

        if (StringUtils.isNotBlank(wkYymm)) {

            if (wkYymm.length() == 6) {

                // 將西元年(YYYY)轉成民國年(YYY)
                String sYear = String.valueOf(Integer.parseInt(wkYymm.substring(0, 4)) - 1911);
                // 月份(MM)
                String sMonth = String.valueOf(Integer.parseInt(wkYymm.substring(4, 6)));
                sYear = StringUtils.leftPad(sYear, 3, "0");
                sMonth = StringUtils.leftPad(sMonth, 2, "0");
                date.append(sYear).append(sMonth);
            }
        }
        return date.toString();
    }

    /**
     * INDATE 核銷年月：固定為周轉金發生的業績年月之次月。 格式：YYYMM。
     * 
     * @param wkYymm
     *            業績年月(西元年)
     * @return
     */
    private static String NextMonth(String wkYymm) {
        StringBuffer date = new StringBuffer();

        if (StringUtils.isNotBlank(wkYymm)) {
            if (wkYymm.length() == 6) {

                // 將西元年(YYYY)轉成民國年(YYY)
                String sYear = String.valueOf(Integer.parseInt(wkYymm.substring(0, 4)) - 1911);
                // 月份(MM)
                int month = Integer.parseInt(wkYymm.substring(4, 6));
                if (month == 13) {

                    sYear = StringUtils.leftPad(String.valueOf(Integer.parseInt(sYear) + 1), 3, "0");
                    String sMonth = "01";
                    date.append(sYear).append(sMonth);
                    return date.toString();
                }
                sYear = StringUtils.leftPad(sYear, 3, "0");
                // 次月。
                String sMonth = String.valueOf(month + 1);
                sMonth = StringUtils.leftPad(sMonth, 2, "0");
                date.append(sYear).append(sMonth);
            }
        }
        return date.toString();
    }

    /**
     * 計算次業績年月。
     * 
     * @param wkYymm
     *            業績年月(西元年)
     * @return 次業績年月(西元年)
     */
    private static String CalcNextWkYymm(String wkYymm) {
        String y = wkYymm.substring(0, 4);
        String m = wkYymm.substring(4, 6);

        if ("13".equals(m)) {
            // 13月要變成下一年的2月。
            String temp = String.valueOf(Integer.valueOf(y) + 1);
            y = StringUtils.leftPad(temp, 4, '0');

            return y + "02";

        } else if ("12".equals(m)) {
            String temp = String.valueOf(Integer.valueOf(y) + 1);
            y = StringUtils.leftPad(temp, 4, '0');

            return y + "02";

        } else if ("11".equals(m)) {
            // 11月要變成下一年的1月。
            String temp = String.valueOf(Integer.valueOf(y) + 1);
            y = StringUtils.leftPad(temp, 4, '0');

            return y + "01";

        } else if ("01".equals(m)) {
            return y + "03";

        } else if ("02".equals(m)) {
            return y + "04";

        } else if ("03".equals(m)) {
            return y + "05";

        } else if ("04".equals(m)) {
            return y + "06";

        } else if ("05".equals(m)) {
            return y + "07";

        } else if ("06".equals(m)) {
            return y + "08";

        } else if ("07".equals(m)) {
            return y + "09";

        } else if ("08".equals(m)) {
            return y + "10";

        } else if ("09".equals(m)) {
            return y + "11";

        } else if ("10".equals(m)) {
            return y + "12";

        }

        return null;
    }

    public BigDecimal calcTurnAmount(boolean advpayType, Ratify ratify, BigDecimal proofTotal, BigDecimal advpayAmt) {
        if (!advpayType || this.calcMayAmt(ratify).subtract(proofTotal).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        } else if (advpayAmt.compareTo(this.calcMayAmt(ratify).subtract(proofTotal)) <= 0) {
            return advpayAmt;
        } else if (advpayAmt.compareTo(this.calcMayAmt(ratify).subtract(proofTotal)) > 0) {
            return this.calcMayAmt(ratify).subtract(proofTotal);
        } else {
            return BigDecimal.ZERO;
        }
    }

    public void changeRatifyStateAccordingToApplState(Ratify ratify, ApplState applState) {
        RatifyStateService ratifyStateService = this.facade.getRatifyStateService();

        // CR #228 尚未切件的申請單要改成切件審核中
        RatifyState ycc0 = ratifyStateService.findByCode(RatifyStateCode.YCC_0);
        RatifyState ycc1 = ratifyStateService.findByCode(RatifyStateCode.YCC_1);
        RatifyState ycc3 = ratifyStateService.findByCode(RatifyStateCode.YCC_3);
        RatifyState ycc4 = ratifyStateService.findByCode(RatifyStateCode.YCC_4);
        RatifyState office0 = ratifyStateService.findByCode(RatifyStateCode.OFFICE_0);
        RatifyState office1 = ratifyStateService.findByCode(RatifyStateCode.OFFICE_1);
        RatifyState office2 = ratifyStateService.findByCode(RatifyStateCode.OFFICE_2);
        RatifyState office3 = ratifyStateService.findByCode(RatifyStateCode.OFFICE_3);
        RatifyState office4 = ratifyStateService.findByCode(RatifyStateCode.OFFICE_4);
        RatifyState director0 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_0);
        RatifyState director1 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_1);
        RatifyState director2 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_2);
        RatifyState director3 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_3);
        RatifyState director4 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_4);
        /* 有核定表-核銷問題 靜怡 [Start]  20130107 新增 */
        RatifyState director5 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_5);
        RatifyState director6 = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_6);
        /* 有核定表-核銷問題 靜怡 [End] 新增 */
        RatifyState directorA = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_A);
        RatifyState directorB = ratifyStateService.findByCode(RatifyStateCode.DIRECTOR_B);

        KindType kindType = ratify.getMiddleType().getKindType();
        if (KindTypeCode.YCC.getCode().equals(kindType.getCode())) {
            if (ApplStateCode.APPLIED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.YCC_0.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(ycc1);
                } else if (RatifyStateCode.YCC_3.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(ycc4);
                }
            } else if (ApplStateCode.FIRST_VERIFICATION_SEND.getCode().equals(applState.getCode())) {

            } else if (ApplStateCode.FIRST_VERIFIED.getCode().equals(applState.getCode())) {

            } else if (ApplStateCode.DELETED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.YCC_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(ycc0);
                } else if (RatifyStateCode.YCC_4.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(ycc3);
                }
            }
        } else if (KindTypeCode.OFFICE.getCode().equals(kindType.getCode())) {
            if (ApplStateCode.APPLIED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.OFFICE_0.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office1);
                } else if (RatifyStateCode.OFFICE_3.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office4);
                }
            } else if (ApplStateCode.FIRST_VERIFICATION_SEND.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.OFFICE_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office2);
                }
            } else if (ApplStateCode.FIRST_VERIFIED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.OFFICE_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office2);
                }
            } else if (ApplStateCode.DELETED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.OFFICE_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office0);
                } else if (RatifyStateCode.OFFICE_4.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(office3);
                }
            }
        } else if (KindTypeCode.DIRECTOR.getCode().equals(kindType.getCode())) {
            if (ApplStateCode.APPLIED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.DIRECTOR_0.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director1);
                } else if (RatifyStateCode.DIRECTOR_3.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director4);
                /* 有核定表-核銷問題 靜怡 [Start] 20130107 新增 */
                } else if (RatifyStateCode.DIRECTOR_5.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director6);    
                /* 有核定表-核銷問題 靜怡 [End] 新增 */
                } else if (RatifyStateCode.DIRECTOR_A.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(directorB);
                }
            } else if (ApplStateCode.FIRST_VERIFICATION_SEND.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.DIRECTOR_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director2);
                } else if (RatifyStateCode.DIRECTOR_A.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(directorB);
                }
            } else if (ApplStateCode.FIRST_VERIFIED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.DIRECTOR_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director2);
                } else if (RatifyStateCode.DIRECTOR_A.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(directorB);
                }
            } else if (ApplStateCode.DELETED.getCode().equals(applState.getCode())) {
                if (RatifyStateCode.DIRECTOR_1.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director0);
                } else if (RatifyStateCode.DIRECTOR_4.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director3);
                /* 有核定表-核銷問題 靜怡 [Start] 20130107 新增 */
                } else if (RatifyStateCode.DIRECTOR_6.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(director5);    
                /* 有核定表-核銷問題 靜怡 [End] 新增 */    
                } else if (RatifyStateCode.DIRECTOR_B.getCode().equals(ratify.getRatifyState().getCode())) {
                    ratify.setRatifyState(directorA);
                }
            }
        } else {

        }
    }

    public long countApplForThisQuarter(Ratify ratify) {
        return super.getDao().countApplForThisQuarter(ratify);
    }

    public boolean isReachedApplyLimitThisQuarter(Ratify ratify) {
        long appliedTimes = this.countApplForThisQuarter(ratify);
        int seasonNum = ratify.getMiddleType().getbCheckDetail().getSeasonNum();
        if (appliedTimes >= seasonNum) {
            return true;
        }
        return false;
    }

    public boolean isOverApplyLimitThisQuarter(Ratify ratify) {
        long appliedTimes = this.countApplForThisQuarter(ratify);
        int seasonNum = ratify.getMiddleType().getbCheckDetail().getSeasonNum();
        if (appliedTimes > seasonNum) {
            return true;
        }
        return false;
    }

    public long countBorrow(Ratify ratify) {
        return super.getDao().countBorrow(ratify);
    }

    public long countCancel(Ratify ratify) {
        return super.getDao().countCancel(ratify);
    }

    public long countPayment(Ratify ratify) {
        return super.getDao().countPayment(ratify);
    }

    public void checkRatifyCancelDate() {
        StringBuffer queryString = new StringBuffer();
        queryString.append("select ratify from Ratify ratify ");
        queryString.append("where ratify.middleType.kindType.code in ('1', '2') ");
        queryString.append("and ratify.ratifyState.code in ('0', '3', '5', 'A') ");
        List<Ratify> list = getDao().findByNamedParams(queryString.toString(), null);
        // 辦公費核銷截止
        RatifyState stateForOffice = facade.getRatifyStateService().findByCode(RatifyStateCode.OFFICE_8);
        // 主任組長併入次期
        RatifyState stateForDirector1 = facade.getRatifyStateService().findByCode(RatifyStateCode.DIRECTOR_8);
        // 主任組長尚未切件
        RatifyState stateForDirector2 = facade.getRatifyStateService().findByCode(RatifyStateCode.DIRECTOR_A);
        if (!CollectionUtils.isEmpty(list)) {
            // IISI 20100709 將需更新的 Ratify 先放到 updateList 中，最後再一併更新。
            List<Ratify> updateList = new ArrayList<Ratify>();
            for (Ratify ratify : list) {
                Calendar cancelDate = ratify.getCancelDate();
                cancelDate.set(Calendar.HOUR, 17);
                cancelDate.set(Calendar.MINUTE, 30);
                if (cancelDate.before(Calendar.getInstance())) {
                    if (KindTypeCode.OFFICE.getCode().equals(ratify.getMiddleType().getKindType().getCode())) {
                        ratify.setRatifyState(stateForOffice);
                        updateList.add(ratify);
                    } else if (KindTypeCode.DIRECTOR.getCode().equals(ratify.getMiddleType().getKindType().getCode())) {
                        if (ratify.getMmFinalDate() == null) {
                            // 未月結：尚未切件
                            ratify.setRatifyState(stateForDirector2);
                        } else {
                            // 已月結：併入次期
                            ratify.setRatifyState(stateForDirector1);
                        }
                        updateList.add(ratify);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(updateList)) {
                for (Ratify ratify : updateList) {
                    super.update(ratify);
                }
            }
        }
    }

    public boolean isReachCancelDate(Ratify ratify) {
        Calendar cancelDate = ratify.getCancelDate();
        cancelDate.set(Calendar.HOUR, 17);
        cancelDate.set(Calendar.MINUTE, 30);
        if (cancelDate.before(Calendar.getInstance())) {
            return true;
        } else {
            return false;
        }
    }

    //RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/20 start
    @SuppressWarnings("unchecked")
    public List<RatifyStatusDto> findRatifyStatus(String departmentCode, String userCode, String wkYymm,
            BigDecimal ratifyAmt, boolean ratifyStatus) {
        // B 1.6 核定表狀態查詢，依登入的使用者，找出核定表。
    	// List<RatifyStatusDto> resultList = new ArrayList<RatifyStatusDto>();

        List<RatifyStatusDto> result = getDao().findRatifyStatus(departmentCode, userCode, wkYymm, ratifyAmt, ratifyStatus,
                StringUtils.isNotBlank(userCode) ? doCheckUserGroup(userCode) : "",
                StringUtils.isNotBlank(departmentCode) ? doCheckDepartmentCode(departmentCode) : "");

        return result;
        /*        
        if (!CollectionUtils.isEmpty(daoList)) {
            for (Object obj : daoList) {

                Object[] record = (Object[]) obj;
                String unitCode3 = (String) record[0];
                String userId = (String) record[1];
                String userName = (String) record[2];
                String midCode = (String) record[3];
                String wkyymm = (String) record[4];
                BigDecimal expCount = (BigDecimal) record[5];
                BigDecimal remitAmt = (BigDecimal) record[6];
                BigDecimal taxAmt = (BigDecimal) record[7];
                BigDecimal stampAmt = (BigDecimal) record[8];
                BigDecimal paymentAmt = (BigDecimal) record[9];
                BigDecimal cumulationAmt = (BigDecimal) record[10];
                String rsname = (String) record[11];
                String ratifyId = (String) record[12];

                RatifyStatusDto ratifyStatusDto = new RatifyStatusDto();
                ratifyStatusDto.setUnitCode3(unitCode3);
                ratifyStatusDto.setUserId(userId);
                ratifyStatusDto.setMidCode(midCode);
                ratifyStatusDto.setWkyymm(wkyymm);
                ratifyStatusDto.setUserName(userName);
                ratifyStatusDto.setExpCount(expCount);
                ratifyStatusDto.setRemitAmt(remitAmt);
                ratifyStatusDto.setTaxAmt(taxAmt);
                ratifyStatusDto.setStampAmt(stampAmt);
                ratifyStatusDto.setPaymentAmt(paymentAmt);
                ratifyStatusDto.setCumulationAmt(cumulationAmt);
                ratifyStatusDto.setRsname(rsname);
                ratifyStatusDto.setRatifyId(ratifyId);
                

                ratifyStatusDto.setRatifyAmt((BigDecimal) record[13]);
                ratifyStatusDto.setHasExpappl(((BigDecimal) record[14]).intValue() == 0 ? false : true);
                Ratify other = findByPK(ratifyId);
                if (other != null) {
                    if (CollectionUtils.isNotEmpty(other.getExpapplBs())) {
                        ratifyStatusDto.setHasExpappl(true);
                    }
                }
                
             
                resultList.add(ratifyStatusDto);
            }
        }
        return resultList;
         */
    }
    //RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/20 end   
    /**
     * B 1.6 操作人員可依身份之不同，看到符合自身身份之資料。
     * 
     * @param userId
     */
    @SuppressWarnings("unchecked")
    private String doCheckUserGroup(String userId) {

        User checkUser = (User) getFacade().getUserService().findByCode(userId);
        if (checkUser == null) {
            String[] paramStrs = { Messages.getString("applicationResources",
                    "tw_com_skl_exp_kernel_model6_bo_User_code", null) };
            throw new ExpRuntimeException(ErrorCode.A10007, paramStrs);
        }
        StringBuffer strBuff = new StringBuffer();
        // 找出操作人員有權使用的使用者。
        Set<User> authUsers = getFacade().getUserService().findAuthUserForRejectedAndPaid();
        if (authUsers == null) {
            return strBuff.toString();
        }

        // IISI-20110325 修正 ORA-01795，每 500 筆拆出一個 IN STATEMENT
        int i = 0;
        Iterator it = authUsers.iterator();
        while (it.hasNext()) {
            
            strBuff.append("'");
            strBuff.append(((User) it.next()).getCode());
            strBuff.append("'");
            i++;
            if (it.hasNext()) {
                if( i > 500) { 
                    i = 0;
                    strBuff.append(") OR U.CODE IN (");
                } else {
                    strBuff.append(", ");
                }
            }
        }

        if (StringUtils.isBlank(userId)) {
            return strBuff.toString();
        }

        if (authUsers != null) {
            if (!authUsers.contains(checkUser)) {
                // 找不到
                throw new ExpRuntimeException(ErrorCode.B10054);
            }
        }

        return strBuff.toString();
    }

    /**
     * B 1.6 操作人員可依身份之不同，看到符合自身身份之資料。
     * 
     * @param departmentCode
     * @return
     */
    @SuppressWarnings("unchecked")
    private String doCheckDepartmentCode(String departmentCode) {

        Department checkDepartment = (Department) getFacade().getDepartmentService().findByCode(departmentCode);
        if (checkDepartment == null) {
            String[] paramStrs = { Messages.getString("applicationResources",
                    "tw_com_skl_exp_kernel_model6_bo_Department_code", null) };
            throw new ExpRuntimeException(ErrorCode.A10007, paramStrs);
        }
        StringBuffer strBuff = new StringBuffer();
        Set<Department> authDepartments = getFacade().getDepartmentService().findAuthDepartmentForAEExpInfo();
        Iterator i = authDepartments.iterator();
        while (i.hasNext()) {

            strBuff.append("'");
            strBuff.append(((Department) i.next()).getCode());
            strBuff.append("'");
            if (i.hasNext()) {
                strBuff.append(", ");
            }
        }
        if (StringUtils.isBlank(departmentCode)) {
            return strBuff.toString();
        }

        if (authDepartments != null) {
            if (!authDepartments.contains(checkDepartment)) {
                // 找不到
                throw new ExpRuntimeException(ErrorCode.B10054);
            }
        }

        return strBuff.toString();
    }

    @SuppressWarnings("unchecked")
    public List<RatifyStatusDetailDto> findExpapplBByRatifyId(String ratifyId) {
        // B 1.6 核定表狀態查欠，依核定表ID，找出有核定申請單。
        List<RatifyStatusDetailDto> resultList = new ArrayList<RatifyStatusDetailDto>();
        List daoList = getDao().findExpapplBByRatifyId(ratifyId);

        if (!CollectionUtils.isEmpty(daoList)) {
            for (Object obj : daoList) {

                Object[] record = (Object[]) obj;
                String expApplNo = (String) record[0];
                BigDecimal remitAmt = (BigDecimal) record[1];
                BigDecimal taxAmt = (BigDecimal) record[2];
                BigDecimal stampAmt = (BigDecimal) record[3];
                BigDecimal advpayAmt = (BigDecimal) record[4];
                BigDecimal paymentAmt = (BigDecimal) record[5];
                String stateName = (String) record[6];

                RatifyStatusDetailDto dto = new RatifyStatusDetailDto();
                dto.setExpApplNo(expApplNo);
                dto.setRemitAmt(remitAmt);
                dto.setTaxAmt(taxAmt);
                dto.setStampAmt(stampAmt);
                dto.setAdvpayAmt(advpayAmt);
                dto.setPaymentAmt(paymentAmt);
                dto.setStateName(stateName);
                resultList.add(dto);
            }
        }
        return resultList;
    }

    public boolean isReachedCancelLimit(Ratify ratify) {
        long canceledTimes = this.countCancel(ratify);
        int seasonNum = ratify.getMiddleType().getbCheckDetail().getCancelNum();
        if (canceledTimes >= seasonNum) {
            return true;
        }
        return false;
    }

    public boolean isReachedPaymentLimit(Ratify ratify) {
        long paymentTimes = this.countPayment(ratify);
        int seasonNum = ratify.getMiddleType().getbCheckDetail().getPaymentNum();
        if (paymentTimes >= seasonNum) {
            return true;
        }
        return false;
    }

    public Ratify findForDISB(ExpapplB expapplB) {
        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("expapplBs", expapplB);
        return getDao().findByCriteriaMapReturnUnique(criteriaMap);
    }

    public List<OfficeExpDto> findOfficeExpForAnnuallyClose(String yyy, String[] months) {
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        return getDao().findOfficeExpForAnnuallyClose(yyyy, months);
    }
    
    /**
     * CM9539
     * for D10.3 組織發展費
     * @param yyy
     * @param months
     * @return
     */
    public List<OfficeExpDto> findOfficeExpForAnnuallyCloseForOrganizeDevelope(String yyy, String[] months) {
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        return getDao().findOfficeExpForAnnuallyCloseForOrganizeDevelope(yyyy, months);
    }
    
    
    public boolean isMonthlyClosed(String yyy, String[] months) {
        List<String> wkYymm = new ArrayList<String>();
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        for (String month : months) {
            wkYymm.add(yyyy + month);
        }
        long count = getDao().countRatifyWithoutMmFinalDate(wkYymm);
        return count == 0;
    }

    /**
     * CM9539
     * for D10.3 組織發展費
     * @param yyy
     * @param months
     * @return
     */
    public boolean isMonthlyClosedForOrganizeDevelope(String yyy, String[] months) {
        List<String> wkYymm = new ArrayList<String>();
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        for (String month : months) {
            wkYymm.add(yyyy + month);
        }
        long count = getDao().countRatifyWithoutMmFinalDateForOrganizeDevelope(wkYymm);
        return count == 0;
    }
    

    
    public List<OfficeExpDto> findOfficeExpForAnnuallyCloseEntry(String yyy, String[] months) {
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        return getDao().findOfficeExpForAnnuallyCloseEntry(yyyy, months);
    }
    
    /**
     * CM9539
     * for D10.3 組織發展費
     * @param yyy
     * @param months
     * @return
     */
    public List<OfficeExpDto> findOfficeExpForAnnuallyCloseEntryForOrganizeDevelope(String yyy, String[] months) {
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        return getDao().findOfficeExpForAnnuallyCloseEntryForOrganizeDevelope(yyyy, months);
    }


    public void refresh(Ratify entity) {
        getDao().refresh(entity);
    }
    
 // RE201301890_併薪作業 modify by michael in 2013/09/11 start
 	public boolean checkFinalYearClose(String yyyy) {

 		String[] months = new String[] { "11", "12" };

// 		是否還有需年結的未核銷金額
 		if (getDao().findOfficeExpForAnnuallyClose(yyyy, months).size() > 0) {
 			return false;
 		}
 		
 		if (getDao().findOfficeExpForAnnuallyCloseForOrganizeDevelope(yyyy, months).size() > 0) {
 			return false;
 		}
 		
 		return true;
 	}
 	//RE201301890_併薪作業 modify by michael in 2013/09/11 end
 	
 	

 	//RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/02 start
    public List<RatifyExportDto> findRatifyByRange(String kindTypeCode, String startYyyymm, String endYyyymm) {
    	
    	return getDao().findRatifyByRange(kindTypeCode, startYyyymm, endYyyymm);
    }
    //RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/02 end
    
    //RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/12 start
    
    /**
     * 產生登入者的駐區
     * 
     * @return
     */
    public List<WorkAreaDto> findWorkAreaByUser(User user) {
    	
    	return getDao().findWorkAreaByUser(user);
    }
    
	public List<PettyCashStatusDto> findPettyCashStatusDtos(User user, String workAreaCode, String wkYymmStart, String wkYymmEnd, String userCode) {
		
		return getDao().findPettyCashStatusDtos(user, workAreaCode, wkYymmStart, wkYymmEnd, userCode);
	}
	
	public List<ReturnAmtDto> findReturnAmtDtos(String userCode, Calendar subpoenaDateS, Calendar subpoenaDateE) {
		
		return getDao().findReturnAmtDtos(userCode, subpoenaDateS, subpoenaDateE);
	}
    
	//RE201400156_各項業務費用核銷進度統計 modify by michael in 2014/05/12 end
	
	//RE201503859_週轉金全額借支及核定表狀態 CU3178 2015/11/11 START
	 //RE201400156-各項業務費用核銷進度統計 CU3178 2014/5/15 START
   /** 
    * B1.8有核定表狀態統計查詢
    * @param
    **/
   //public List<RatifyCountsDto> findRatifyCounts(String areacode,	String unitcode, String whyymmstart, String wkyymmend, boolean dept){
	public List<RatifyCountsDto> findRatifyCounts(String areacode,	String unitcode, String whyymmstart, String wkyymmend, boolean dept,String middleType){
   	List<RatifyCountsDto> resultList = new ArrayList<RatifyCountsDto>();
   	List totalcelunit = new ArrayList();
   	if(areacode!=null){
   		if(dept){
   			totalcelunit = getDao().findRatifyDeptAreaCounts(areacode, unitcode, whyymmstart, wkyymmend ,middleType);	//部級查詢
   		}
   		else{
   			totalcelunit = getDao().findRatifyUnitCounts(areacode, unitcode,	whyymmstart, wkyymmend ,middleType) ;	//統計全部及已核銷
   		}
   			if (!CollectionUtils.isEmpty(totalcelunit)) {    				 
   				for (Object obj : totalcelunit) {
   	                	Object[] recordunit = (Object[]) obj;
   	                	String workunitcode = (String)recordunit[0];	//區部代號
   	                	String workunitname = (String)recordunit[1];	//區部名稱
   	                	String newunitcode = (String)recordunit[2];		//單位代號
   	                	String newunitname = (String)recordunit[3];		//單位名稱
   	                	BigDecimal notcelitem = (BigDecimal) recordunit[4];   	//未核銷件數               	
   	                	BigDecimal totalamt = (BigDecimal) recordunit[5];	//總金額
   	                	BigDecimal celitem = (BigDecimal) recordunit[6];   	  //已核銷件數          	
   	                	BigDecimal celamt = (BigDecimal) recordunit[7];			//已核銷金額
   	                	RatifyCountsDto dto = new RatifyCountsDto();
   	                	dto.setAreaCode2(workunitcode);
   	                	dto.setAreaName2(workunitname);
   	                	dto.setUnitCode3(newunitcode); 
   	                	dto.setUnitName3(newunitname);
   	                	
   	                	if(celitem == null){	//若件數為null則給0
   	                		celitem = BigDecimal.ZERO;
   	                	}
   	                	if(celamt == null){	//若金額為null則給0
   	                		celamt = BigDecimal.ZERO;
   	                	}
   	                	if(notcelitem == null){	//若件數為null則給0
   	                		celitem = BigDecimal.ZERO;
   	                	}
   	                	if(totalamt == null){	//若金額為null則給0
   	                		celamt = BigDecimal.ZERO;
   	                	}
   	                	dto.setUnitCancelItem(celitem);
   	                	dto.setUnitCancelAmt(celamt);
   	                	dto.setUnitnotCancelItem(notcelitem);
   	                	dto.setUnitnotCancelAmt(totalamt.subtract(celamt));
   	                
   	                	resultList.add(dto);
   				
   			}
   		}
   	}
   	return resultList;
	}
   
   //public List findRatifyAreaCounts(String areacode ,String whyymmstart, String wkyymmend, boolean dept){
	public List findRatifyAreaCounts(String areacode ,String whyymmstart, String wkyymmend, boolean dept,String middleType){
   	if(dept){
   		return getDao().findRatifyDeptCounts(areacode, whyymmstart, wkyymmend ,middleType);
   	}
   	else{
   		return getDao().findRatifyAreaCounts(areacode, whyymmstart, wkyymmend ,middleType);
   	}
   }
	//RE201400156-各項業務費用核銷進度統計 CU3178 2014/5/15 END
   //RE201503859_週轉金全額借支及核定表狀態 CU3178 2015/11/11 START
    
    //RE201400844_查詢核定表效能 modify by michael in 2014/07/04 start    
	public boolean checkRatifyAuth(User loginUser, Ratify ratify) {
		
		boolean result = false;
		
//		本人
		if (loginUser.getCode().equals(ratify.getUser().getCode())) {
			return true;
		}
		
		
        Group group = loginUser.getGroup();

        if (group != null) {
            if (GroupCode.INTERIOR.getCode().equals(group.getCode())) {
                
                User ratifyUser = ratify.getUser();
                
                if (ratifyUser.getGroup() != null
                            && (GroupCode.BIZ_DIVISION.getCode().equals(ratifyUser.getGroup().getCode())
                                    || GroupCode.BIZ_SECTION.getCode().equals(ratifyUser.getGroup().getCode())
                                    || GroupCode.BIZ_UNIT.getCode().equals(ratifyUser.getGroup().getCode()) || GroupCode.SECTION_MANAGER
                                    .getCode().equals(ratifyUser.getGroup().getCode()))) {
                        
                	result = getDao().checkUserDepartmentExist(loginUser, ratify);
                }
            } else if (GroupCode.AUDITOR_FIRST_VERIFY.getCode().equals(group.getCode())
                    || GroupCode.AUDITOR_REVIEW.getCode().equals(group.getCode())
                    || GroupCode.AUDITOR_TAX.getCode().equals(group.getCode())
                    || GroupCode.AUDITOR_GENERAL.getCode().equals(group.getCode())) {
                
            	User ratifyUser = ratify.getUser();
            	
            	if (ratifyUser.getGroup() != null
            			&& (GroupCode.BIZ_DIVISION.getCode().equals(ratifyUser.getGroup().getCode())
            					|| GroupCode.BIZ_SECTION.getCode().equals(ratifyUser.getGroup().getCode())
                                || GroupCode.BIZ_UNIT.getCode().equals(ratifyUser.getGroup().getCode())
                                || GroupCode.DIRECTOR.getCode().equals(ratifyUser.getGroup().getCode()) || GroupCode.SECTION_MANAGER
                                	.getCode().equals(ratifyUser.getGroup().getCode()))) {
                        
            		result = getDao().checkUserDepartmentExist(loginUser, ratify);
            	}
            }
        }
        
        return result;
    }
	//RE201400844_查詢核定表效能 modify by michael in 2014/07/04 end
	
	//RE201501754_週轉金累計未核銷餘額併入次次期核定表 CU3178 2015/8/25 START
    public List<Ratify> findExistRatifyBy510_610(String code, String wkYymm) {

        Map<String, Object> criteriaMap = new HashMap<String, Object>();
        criteriaMap = new HashMap<String, Object>();
        criteriaMap.put("code", code);
        criteriaMap.put("wkYymm", wkYymm);
        String queryString = "";

        queryString = "select r from Ratify r where r.user.code = :code and r.wkYymm = :wkYymm and r.middleType.code in ('510','610') ";
        return super.getDao().findByNamedParams(queryString, criteriaMap);
    }
	//RE201501754_週轉金累計未核銷餘額併入次次期核定表 CU3178 2015/8/25 END
    
    //RE201703210_辦公費核定表提列應付費用 CU3178 2017/11/15 START
    /**
     * D10.6查詢推廣費應付資料
     * @param yyy
     * @param months
     * @return
     */
    public boolean isMonthlyClosedForMarkingPromExp(String yyy, String[] months) {
        List<String> wkYymm = new ArrayList<String>();
        String yyyy = String.valueOf(Integer.parseInt(yyy) + 1911);
        for (String month : months) {
            wkYymm.add(yyyy + month);
        }
        long count = getDao().countRatifyWithoutMmFinalDateForMarkingPromExp(wkYymm);
        return count == 0;
    }
    
    /**
     * D10.6查詢推廣費應付資料
     * @param year
     * @param months
     * @return
     */
    public List<OfficeExpDto> findOfficeExpForAnnuallyCloseForMarkingPromExp(String yyy, String[] months){
    	String year = String.valueOf(Integer.parseInt(yyy) + 1911);
    	return getDao().findOfficeExpForAnnuallyCloseForMarkingPromExp(year, months);
    }
    
    /**
     * D10.06年度結算行銷推廣費
     * @param year
     * @param months
     * @return
     */
    public List<OfficeExpDto> findOfficeExpForAnnuallyCloseEntryForMarkingPromExp(String year, String[] months) {
    	return getDao().findOfficeExpForAnnuallyCloseEntryForMarkingPromExp(year, months);
    }
    //RE201703210_辦公費核定表提列應付費用 CU3178 2017/11/15 END
    
}
