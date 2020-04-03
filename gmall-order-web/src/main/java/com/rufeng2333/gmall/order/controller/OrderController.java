package com.rufeng2333.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.rufeng2333.gmall.annotations.LoginRequired;
import com.rufeng2333.gmall.bean.OmsCartItem;
import com.rufeng2333.gmall.bean.OmsOrder;
import com.rufeng2333.gmall.bean.OmsOrderItem;
import com.rufeng2333.gmall.bean.UmsMemberReceiveAddress;
import com.rufeng2333.gmall.service.CartService;
import com.rufeng2333.gmall.service.OrderService;
import com.rufeng2333.gmall.service.SkuService;
import com.rufeng2333.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;


    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        String success = orderService.checkTradeCode(memberId,tradeCode);
        if(success.equals("success")){

            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("速速发货");
            //生成外部订单号
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());
            omsOrder.setOrderSn(outTradeNo);
            omsOrder.setPayAmount(totalAmount);
            omsOrder.setOrderType(1);

            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            //java的时间加减工具类      当前日期加一天
            Calendar instance = Calendar.getInstance();
            instance.add(Calendar.DATE,1);
            Date time = instance.getTime();
            omsOrder.setReceiveTime(time);
            //这两个字段没封装上，数据库是int这里是BigDecimal
            omsOrder.setSourceType(0);
            omsOrder.setStatus(0);

            omsOrder.setTotalAmount(totalAmount);

            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            for (OmsCartItem omsCartItem : omsCartItems) {
                if(omsCartItem.getIsChecked().equals("1")){
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if(b==false){
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }

                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setOrderSn(outTradeNo);
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("111");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编号");//在仓库中的skuId

                    omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);
            //将订单和订单详情写入数据库同时删除购物车对应商品
            orderService.saveOrder(omsOrder);

            ModelAndView mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            //这里为了方便直接传值，实际上不应该传值而应该是根据拦截器返回的参数查询
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",totalAmount);
            return mv;

        }else {
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }
    }


    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        List<UmsMemberReceiveAddress> receiveAddressByMemberId = userService.getReceiveAddressByMemberId(memberId);

        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {

            if(omsCartItem.getIsChecked().equals("1")) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItems.add(omsOrderItem);
            }
        }
        modelMap.put("userAddressList",receiveAddressByMemberId);
        modelMap.put("omsOrderItems",omsOrderItems);
        modelMap.put("totalAmount",getTotalAmount(omsCartItems));

        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.put("tradeCode",tradeCode);

        return "trade";

    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            if(omsCartItem.getIsChecked().equals("1")) {
                BigDecimal totalPrice = omsCartItem.getTotalPrice();
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

}
