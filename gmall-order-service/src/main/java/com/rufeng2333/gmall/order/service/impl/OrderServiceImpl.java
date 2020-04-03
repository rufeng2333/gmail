package com.rufeng2333.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.rufeng2333.gmall.bean.OmsOrder;
import com.rufeng2333.gmall.bean.OmsOrderItem;
import com.rufeng2333.gmall.mq.ActiveMQUtil;
import com.rufeng2333.gmall.order.mapper.OmsOrderItemMapper;
import com.rufeng2333.gmall.order.mapper.OmsOrderMapper;
import com.rufeng2333.gmall.service.CartService;
import com.rufeng2333.gmall.service.OrderService;
import com.rufeng2333.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";
            String tradeCodeFromCache = jedis.get(tradeKey);//使用lua脚本防止并发攻击
            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));
            if (eval != null && eval != 0) {
                return "success";
            } else {
                return "tradeFail";
            }
        } finally {
            jedis.close();
        }
    }

    @Override
    public String genTradeCode(String memberId) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();

            String tradeKey = "user:" + memberId + ":tradeCode";

            String tradeCode = UUID.randomUUID().toString();

            jedis.setex(tradeKey, 60 * 15, tradeCode);

            return tradeCode;
        } finally {
            jedis.close();
        }
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {

        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        //把id传进去
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除购物车数据
            //这里暂时先不写，因为要重复测试   CartService已经注入进来了
        }


    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {

        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);

        return omsOrder1;
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {

        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus(1);

        Session session = null;
        Connection connection = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true,Session.SESSION_TRANSACTED);

            Queue payment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);
            TextMessage textMessage = new ActiveMQTextMessage();
            //查询订单对象，存入消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);

            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderSn(omsOrderParam.getOrderSn());
            List<OmsOrderItem> select = omsOrderItemMapper.select(omsOrderItemParam);
            omsOrderResponse.setOmsOrderItems(select);

            textMessage.setText(JSON.toJSONString(omsOrderResponse));

            omsOrderMapper.updateByExampleSelective(omsOrderUpdate,e);
            producer.send(textMessage);

            session.commit();
        }catch (Exception e1){
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }
    }
}
