package com.rufeng2333.gmall.payment.mq;

import com.rufeng2333.gmall.bean.PaymentInfo;
import com.rufeng2333.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE" ,containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {

        String out_trade_no = mapMessage.getString("out_trade_no");
        Integer count = 0;
        if(mapMessage.getString("count")!=null) {
            count = Integer.parseInt("" + mapMessage.getString("count"));
        }

        Map<String,Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);

        if(resultMap!=null && !resultMap.isEmpty()){
            String trade_status = (String) resultMap.get("trade_status");
            if (trade_status.equals("TRADE_SUCCESS")){

                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));
                paymentInfo.setCallbackContent((String) resultMap.get("call_back_content"));
                paymentInfo.setCallbackTime(new Date());

                System.out.println("支付成功，调用支付服务");
                paymentService.updatePayment(paymentInfo);
                return;
            }
        }

        if(count>0) {
            System.out.println("没有支付成功，重新发送消息队列");
            count--;
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
        }else {
            System.out.println("检查次数用尽");
        }

    }

}
