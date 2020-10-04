package org.bcos.browser.service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.bcos.browser.base.ConstantCode;
import org.bcos.browser.base.Constants;
import org.bcos.browser.base.exception.BaseException;
import org.bcos.browser.entity.base.BasePageResponse;
import org.bcos.browser.entity.base.BaseResponse;
import org.bcos.browser.entity.dto.BlockFromChain;
import org.bcos.browser.entity.dto.ReceiptFromChain;
import org.bcos.browser.entity.dto.Transaction;
import org.bcos.browser.entity.dto.TransactionAndReceipt;
import org.bcos.browser.entity.dto.TransactionFromChain;
import org.bcos.browser.entity.req.ReqGetCode;
import org.bcos.browser.entity.req.ReqTransaction;
import org.bcos.browser.entity.rsp.RspGetTransaction;
import org.bcos.browser.entity.rsp.RspGetTxnLatelyDays;
import org.bcos.browser.mapper.NodeMapper;
import org.bcos.browser.mapper.TransactionMapper;
import org.bcos.browser.util.CommonUtils;
import org.bcos.browser.util.DateTimeUtils;
import org.bcos.browser.util.Web3jRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {
    @Autowired
    TransactionMapper transactionMapper;
    @Autowired
    Web3jRpc web3jRpc;
    @Autowired
    NodeMapper nodeMapper;

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
    public BasePageResponse getTransInfoByPage(int groupId, int pageNumber,
            int pageSize, String transHash, String blockNumber) {
        log.info("getTransInfoByPage groupId:{} pageNumber:{} pageSize:{} transHash:{} blockNumber:{}",
                groupId, pageNumber, pageSize, transHash, blockNumber);
        int start =
                Optional.ofNullable(pageNumber).map(page -> (page - 1) * pageSize).orElse(null);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        map.put("transHash", CommonUtils.trimSpaces(transHash));
        map.put("number", CommonUtils.trimSpaces(blockNumber));
        map.put("start", start);
        map.put("pageSize", pageSize);

        Instant startTime = Instant.now();
        int total = transactionMapper.getAllTransactionCount(map);
        log.info("getTransInfoByPage getCount useTime:{}",
                Duration.between(startTime, Instant.now()).toMillis());

        List<RspGetTransaction> list = new ArrayList<>();
        // get from db
        if (total > 0) {
            Instant startTime1 = Instant.now();
            List<Transaction> listTbTransactionDto = transactionMapper.getTbTransactionByPage(map);
            log.info("getTransInfoByPage getTbTransaction useTime:{}",
                    Duration.between(startTime1, Instant.now()).toMillis());
            if (listTbTransactionDto != null) {
                for (Transaction tbTransactionDto : listTbTransactionDto) {
                    RspGetTransaction rspEntity = new RspGetTransaction();
                    rspEntity.setTransHash(tbTransactionDto.getTransHash());
                    rspEntity.setBlockHash(tbTransactionDto.getBlockHash());
                    rspEntity.setBlockNumber(tbTransactionDto.getBlockNumber());
                    rspEntity.setBlockTimesStr(DateTimeUtils.timestamp2String(
                            tbTransactionDto.getBlockTime(), Constants.DEFAULT_DATA_TIME_FORMAT));
                    rspEntity.setFrom(tbTransactionDto.getTransFrom());
                    rspEntity.setTo(tbTransactionDto.getTransTo());
                    rspEntity.setTransIndex(tbTransactionDto.getTransIndex());
                    rspEntity.setMethod(tbTransactionDto.getMethod());
                    list.add(rspEntity);
                }
            }
        } else { // get from chain
            if (CommonUtils.trimSpaces(transHash) != null) {
                log.info("getTransInfoByPage transHash:{} get from chain", transHash);
                TransactionFromChain transInfo = web3jRpc.getTransByHash(groupId, transHash);
                if (transInfo != null) {
                    BlockFromChain blockInfo = web3jRpc.getBlockByNumber(groupId, CommonUtils.parseHexStr2Int(transInfo.getBlockNumber()));
                    RspGetTransaction rspEntity = getTransactionFromChain(transInfo, blockInfo.getTimestamp());
                    list.add(rspEntity);
                }
                total = list.size();
            } else if (CommonUtils.trimSpaces(blockNumber) != null) {
                log.info("getTransInfoByPage blockNumber:{} get from chain", blockNumber);
                BlockFromChain blockInfo = web3jRpc.getBlockByNumber(groupId, Integer.valueOf(blockNumber));
                for (TransactionFromChain transInfo : blockInfo.getTransactions()) {
                    RspGetTransaction rspEntity = getTransactionFromChain(transInfo, blockInfo.getTimestamp());
                    list.add(rspEntity);
                }
                total = list.size();
            }
        }
        BasePageResponse response = new BasePageResponse(ConstantCode.SUCCESS);
        response.setTotalCount(total);
        response.setData(list);
        return response;
    }
    
    /**
     * getTransactionFromChain.
     * 
     * @param transInfo info
     * @param timeStr time
     * @return
     */
    private RspGetTransaction getTransactionFromChain(TransactionFromChain transInfo, String timeStr) {
        RspGetTransaction rspEntity = new RspGetTransaction();
        rspEntity.setTransHash(transInfo.getHash());
        rspEntity.setBlockHash(transInfo.getBlockHash());
        rspEntity.setBlockNumber(CommonUtils.parseHexStr2Int(transInfo.getBlockNumber()));
        rspEntity.setBlockTimesStr(DateTimeUtils.timestamp2String(
                new Timestamp(Long.parseLong(timeStr.substring(2), 16)), 
                Constants.DEFAULT_DATA_TIME_FORMAT));
        rspEntity.setFrom(transInfo.getFrom());
        rspEntity.setTo(transInfo.getTo());
        rspEntity.setTransIndex(CommonUtils.parseHexStr2Int(transInfo.getTransactionIndex()));
        return rspEntity;
    }

    /**
     * analyzeData.
     * 
     * @param reqTransaction info
     * @return
     */
    public BaseResponse analyzeData(ReqTransaction reqTransaction) {
        log.info("analyzeData reqTransaction:{}", reqTransaction);
        List<Transaction> transHashList = reqTransaction.getData();
        int groupId = reqTransaction.getGroupId();
        List<TransactionAndReceipt> data = new ArrayList<>();
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        map.put("type", 0);
        map.put("status", 0);
        int total = nodeMapper.getNodeCnts(map);
        // check for manually added nodes
        if (total > 0) {
            for (int i = 0; i < transHashList.size(); i++) {
                TransactionAndReceipt result = new TransactionAndReceipt();
                TransactionFromChain transInfo = web3jRpc.getTransByHash(groupId, 
                        transHashList.get(i).getTransHash());
                ReceiptFromChain receiptInfo = web3jRpc.getReceiptByHash(groupId, 
                        transHashList.get(i).getTransHash());
                result.setTransactionFromChain(transInfo);
                result.setReceiptFromChain(receiptInfo);
                data.add(result);
            }
        }
        
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        response.setData(data);
        log.debug("###analyzeData response:{}###", response);
        return response;
    }

    /**
     * updateMethod.
     * 
     * @param reqTransaction info
     * @return
     */
    public BaseResponse updateMethod(ReqTransaction reqTransaction) {
        log.info("updateTransInfo reqTransaction:{}", reqTransaction);
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        for (Transaction transaction : reqTransaction.getData()) {
            transaction.setGroupId(reqTransaction.getGroupId());
            transactionMapper.updateMethod(transaction);
        }
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
    public BaseResponse getTransactionByHash(int groupId, String transHash) throws BaseException {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        TransactionFromChain info = web3jRpc.getTransByHash(groupId, transHash);
        if (info != null) {
        	response.setData(info);
        } else {
        	throw new BaseException(ConstantCode.NODE_ABNORMAL);
        }
        log.debug("###getTransactionByHash response:{}###", response);
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
    public BaseResponse getReceiptByHash(int groupId, String transHash) throws BaseException {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        ReceiptFromChain info = web3jRpc.getReceiptByHash(groupId, transHash);
        if (info != null) {
        	response.setData(info);
        } else {
        	throw new BaseException(ConstantCode.NODE_ABNORMAL);
        }
        log.debug("###getTransactionReceiptByHash response:{}###", response);
        return response;
    }

    /**
     * getCode.
     * 
     * @param reqGetCode info
     * @return
     */
    public BaseResponse getCode(ReqGetCode reqGetCode) {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        List<String> data = new ArrayList<>();
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", reqGetCode.getGroupId());
        map.put("type", 0);
        map.put("status", 0);
        int total = nodeMapper.getNodeCnts(map);
        if (total > 0) {
        	for (int i = 0; i < reqGetCode.getData().size(); i++) {
        		String code = web3jRpc.getCode(reqGetCode.getGroupId(), 
        				reqGetCode.getData().get(i));
        		data.add(code);
        	}
        }
        
        response.setData(data);
        log.debug("###getCode response:{}###", response);
        return response;
    }

    /**
     * getDataAmount.
     *
     * @param groupId groupId
     * @return
     */
    //智能合约调用CreateData次数统计
    public BaseResponse getDataAmount(int groupId)  {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method = "0xff8fe78b";
        map.put("method",method);
        int total = transactionMapper.getAmount(map);
        response.setData(total);
        return response;
    }
    /**
     * getCert.
     *
     * @param groupId groupId
     * @return
     */
    //献血证次数统计
    public BaseResponse getCert(int groupId)  {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method = "0x8606cf3c";
        map.put("method",method);
        int total = transactionMapper.getAmount(map);
        response.setData(total);
        return response;
    }

    /**
     * getTrade.
     *
     * @param groupId groupId
     * @return
     */
    //智能合约调用Trade和TradeByTxid次数统计
    public BaseResponse getTrade(int groupId)  {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method = "0x2f566cea";
        map.put("method",method);
        int total = transactionMapper.getAmount(map);
        Map<String, Object> map1 = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method1 = "0x03eb5390";
        map.put("method",method1);
        int total1 = transactionMapper.getAmount(map);
        response.setData(total+total1);
        return response;
    }

    /**
     * getCopy.
     *
     * @param groupId groupId
     * @return
     */
    //智能合约调用Confirm和ConfirmByTxid次数统计
    public BaseResponse getCopy(int groupId)  {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method = "0x80b11bca";
        map.put("method",method);
        int total = transactionMapper.getAmount(map);
        Map<String, Object> map1 = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method1 = "0x4935f7f4";
        map.put("method",method1);
        int total1 = transactionMapper.getAmount(map);
        response.setData(total+total1);
        return response;
    }

    /**
     * getAuth.
     *
     * @param groupId groupId
     * @return
     */
    //智能合约调用AuthRight和AuthRightByTxid次数统计
    public BaseResponse getAuth(int groupId)  {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method = "0x9651338e";
        map.put("method",method);
        int total = transactionMapper.getAmount(map);
        Map<String, Object> map1 = new HashMap<String, Object>();
        map.put("groupId", groupId);
        String method1 = "0x0b255b9d";
        map.put("method",method1);
        int total1 = transactionMapper.getAmount(map);
        response.setData(total+total1);
        return response;
    }

    /**
     * get data count lately
     *
     * @param groupId groupId
     * @return
     */
    public BaseResponse getDataCountDay(int groupId,String dateTimeBegin,String dateTimeEnd) throws ParseException {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);

        List<RspGetTxnLatelyDays> listTxn = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(dateTimeBegin);
        Date parse2 = sdf.parse(dateTimeEnd);
        Long spi = parse2.getTime()-parse.getTime();
        Long step = spi/(1000*60*60*24);


        for(int i = 0;i<=step;i++){
            Date date0 = new Date(parse.getTime()+i*1000*60*60*24);
            String date = sdf.format(date0);
            RspGetTxnLatelyDays rspEntity = new RspGetTxnLatelyDays();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("groupId", groupId);
            map.put("date",date);
            map.put("method","0x9651338e");
            int total = transactionMapper.getDataCountDay(map);
            Map<String, Object> map1 = new HashMap<String, Object>();
            map1.put("groupId", groupId);
            map1.put("date",date);
            map1.put("method","0x0b255b9d");
            total += transactionMapper.getDataCountDay(map1);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date0);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            String dayStr = month + "/" + day;
            rspEntity.setDateStr(dayStr);
            rspEntity.setTxn(total);
            listTxn.add(rspEntity);
        }
        response.setData(listTxn);
        return response;
    }

    /**
     * get trade count lately
     *
     * @param groupId groupId
     * @return
     */
    public BaseResponse getTradeCountDay(int groupId,String dateTimeBegin,String dateTimeEnd) throws ParseException {
        BaseResponse response = new BaseResponse(ConstantCode.SUCCESS);

        List<RspGetTxnLatelyDays> listTxn = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(dateTimeBegin);
        Date parse2 = sdf.parse(dateTimeEnd);
        Long spi = parse2.getTime()-parse.getTime();
        Long step = spi/(1000*60*60*24);


        for(int i = 0;i<=step;i++){
            Date date0 = new Date(parse.getTime()+i*1000*60*60*24);
            String date = sdf.format(date0);
            RspGetTxnLatelyDays rspEntity = new RspGetTxnLatelyDays();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("groupId", groupId);
            map.put("date",date);
            map.put("method","0x2f566cea");
            int total = transactionMapper.getDataCountDay(map);
            Map<String, Object> map1 = new HashMap<String, Object>();
            map1.put("groupId", groupId);
            map1.put("date",date);
            map1.put("method","0x03eb5390");
            total += transactionMapper.getDataCountDay(map1);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date0);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            String dayStr = month + "/" + day;
            rspEntity.setDateStr(dayStr);
            rspEntity.setTxn(total);
            listTxn.add(rspEntity);
        }
        response.setData(listTxn);
        return response;
    }

}
