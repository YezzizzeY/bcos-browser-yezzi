package org.bcos.browser.controller;

import javax.validation.Valid;
import org.bcos.browser.base.BaseController;
import org.bcos.browser.base.exception.BaseException;
import org.bcos.browser.entity.base.BasePageResponse;
import org.bcos.browser.entity.base.BaseResponse;
import org.bcos.browser.entity.req.ReqGetCode;
import org.bcos.browser.entity.req.ReqTransaction;
import org.bcos.browser.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping(value = "transaction")
public class TransactionController extends BaseController {

    @Autowired
    TransactionService transactionService;

    /**
     * getTransInfoByPage.
     * 
     * @param groupId groupId
     * @param pageNumber pageNumber
     * @param pageSize pageSize
     * @param transHash transHash
     * @param blockNumber blockNumber
     * @return
     */
    @GetMapping("/transactionList/{groupId}/{pageNumber}/{pageSize}")
    public BasePageResponse getTransInfoByPage(@PathVariable("groupId") int groupId,
            @PathVariable("pageNumber") int pageNumber,
            @PathVariable("pageSize") int pageSize,
            @RequestParam(value = "transHash", required = false) String transHash,
            @RequestParam(value = "blockNumber", required = false) String blockNumber) {
        BasePageResponse response = transactionService.getTransInfoByPage(groupId, pageNumber,
                pageSize, transHash, blockNumber);
        return response;
    }
    
    /**
     * analyzeData.
     * 
     * @param reqTransaction info 
     * @param result checkResult
     * @return
     */
    @PostMapping("/analyzeData")
    public BaseResponse analyzeData(@Valid @RequestBody ReqTransaction reqTransaction,
            BindingResult result) throws BaseException {
        checkParamResult(result);
        BaseResponse response = transactionService.analyzeData(reqTransaction);
        return response;
    }
    
    /**
     * updateMethod.
     * 
     * @param reqTransaction info 
     * @param result checkResult
     * @return
     */
    @PutMapping("/updateMethod")
    public BaseResponse updateMethod(@Valid @RequestBody ReqTransaction reqTransaction,
            BindingResult result) throws BaseException {
        checkParamResult(result);
        BaseResponse response = transactionService.updateMethod(reqTransaction);
        return response;
    }

    /**
     * getTransactionByHash.
     * 
     * @param groupId groupId
     * @param transHash transHash
     * @return
     * @throws BaseException 
     */
    @GetMapping("/transactionByHash/{groupId}/{transHash}")
    public BaseResponse getTransactionByHash(@PathVariable("groupId") int groupId,
            @PathVariable("transHash") String transHash) throws BaseException {
        BaseResponse response = transactionService.getTransactionByHash(groupId, transHash);
        return response;
    }

    /**
     * getTransactionReceiptByHash.
     * 
     * @param groupId groupId
     * @param transHash transHash
     * @return
     * @throws BaseException 
     */
    @GetMapping("/receiptByHash/{groupId}/{transHash}")
    public BaseResponse getReceiptByHash(@PathVariable("groupId") int groupId,
            @PathVariable("transHash") String transHash) throws BaseException {
        BaseResponse response = transactionService.getReceiptByHash(groupId, transHash);
        return response;
    }
    
    /**
     * getCode.
     * 
     * @param reqGetCode info
     * @param result checkResult
     * @return
     */
    @PostMapping("/code")
    public BaseResponse getCode(@Valid @RequestBody ReqGetCode reqGetCode,
            BindingResult result) throws BaseException {
        BaseResponse response = transactionService.getCode(reqGetCode);
        return response;
    }

    /**
     * getDataAmount.
     *
     * @param groupId groupId
     * @return
     */
    //上链素材统计
    @GetMapping("/getDataAmount/{groupId}")
    public BaseResponse getDataAmount(@PathVariable("groupId") int groupId) {
        BaseResponse response = transactionService.getDataAmount(groupId);
        return response;
    }
    /**
     * getCert.
     *
     * @param groupId groupId
     * @return
     */
    //电子献血证书
    @GetMapping("/getCert/{groupId}")
    public BaseResponse getCert(@PathVariable("groupId") int groupId) {
        BaseResponse response = transactionService.getCert(groupId);
        return response;
    }

    /**
     * getTrade.
     *
     * @param groupId groupId
     * @return
     */
    //交易统计
    @GetMapping("/getTrade/{groupId}")
    public BaseResponse getTrade(@PathVariable("groupId") int groupId) {
        BaseResponse response = transactionService.getTrade(groupId);
        return response;
    }
    /**
     * getCopy.
     *
     * @param groupId groupId
     * @return
     */
    //侵权数量统计
    @GetMapping("/getCopy/{groupId}")
    public BaseResponse getCopy(@PathVariable("groupId") int groupId) {
        BaseResponse response = transactionService.getCopy(groupId);
        return response;
    }
    /**
     * getAuth.
     *
     * @param groupId groupId
     * @return
     */
    //确权数量统计
    @GetMapping("/getAuth/{groupId}")
    public BaseResponse getAuth(@PathVariable("groupId") int groupId) {
        BaseResponse response = transactionService.getAuth(groupId);
        return response;
    }

    /**
     * get lately data counts.
     *
     * @param groupId groupId
     * @param dateTimeBegin start time
     * @param dateTimeEnd end time
     * @return
     */
    //获得近期上链素材数量走势
    @GetMapping("/dataLately/{groupId}/{dateTimeBegin}/{dateTimeEnd}")
    public BaseResponse getDataCountDay(@PathVariable("groupId") int groupId,
                                         @PathVariable("dateTimeBegin") String dateTimeBegin,
                                         @PathVariable("dateTimeEnd") String dateTimeEnd) throws ParseException {
        BaseResponse response =
                transactionService.getDataCountDay(groupId,dateTimeBegin,dateTimeEnd);
        return response;
    }

    /**
     * get lately trade counts.
     *
     * @param groupId groupId
     * @param dateTimeBegin start time
     * @param dateTimeEnd end time
     * @return
     */
    //获得近期交易数量走势
    @GetMapping("/tradeLately/{groupId}/{dateTimeBegin}/{dateTimeEnd}")
    public BaseResponse getTradeCountDay(@PathVariable("groupId") int groupId,
                                        @PathVariable("dateTimeBegin") String dateTimeBegin,
                                        @PathVariable("dateTimeEnd") String dateTimeEnd) throws ParseException {
        BaseResponse response =
                transactionService.getTradeCountDay(groupId,dateTimeBegin,dateTimeEnd);
        return response;
    }
}
