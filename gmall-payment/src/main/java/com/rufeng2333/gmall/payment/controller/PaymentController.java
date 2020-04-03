package com.rufeng2333.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.rufeng2333.gmall.annotations.LoginRequired;
import com.rufeng2333.gmall.bean.OmsOrder;
import com.rufeng2333.gmall.bean.PaymentInfo;
import com.rufeng2333.gmall.payment.config.AlipayConfig;
import com.rufeng2333.gmall.service.OrderService;
import com.rufeng2333.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    //这里把Controller层和Service层写在一起了,所以是@Autowired
    @Autowired
    PaymentService paymentService;

    @Reference
    OrderService orderService;

    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String alipayCallBackReturn(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();


        //2.0版本的接口将同步验签的参数去掉了,所以这里不能验签了,这里还是用同步模拟一下异步验签
        if(StringUtils.isNotBlank(sign)){

            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setCallbackContent(call_back_content);
            paymentInfo.setCallbackTime(new Date());

            paymentService.updatePayment(paymentInfo);
        }

        //支付成功后引起的系统服务->订单服务更新->库存服务->物流服务

        return "finish";

    }

    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){
        return null;
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);
        map.put("subject","商品标题");

        String param = JSON.toJSONString(map);
        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("商品标题");
        paymentInfo.setTotalAmount(totalAmount);

        paymentService.savePaymentInfo(paymentInfo);

        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);

        return form;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        modelMap.put("nickname",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);

        return "index";
    }

}
