package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {
    String checkTradeCode(String memberId,String tradeCode);

    String genTradeCode(String memberId);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);
}
