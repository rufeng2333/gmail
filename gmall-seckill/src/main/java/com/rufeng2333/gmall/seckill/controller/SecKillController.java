package com.rufeng2333.gmall.seckill.controller;

import com.rufeng2333.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    //先到先得
    @RequestMapping("secKill")
    @ResponseBody
    public String SecKill() {

        Jedis jedis = redisUtil.getJedis();
        RSemaphore semaphore = redissonClient.getSemaphore("106");
        boolean b = semaphore.tryAcquire();
        Integer stock = Integer.parseInt(jedis.get("106"));

        if (b) {
            System.out.println("剩余" + stock + "成功");
            //发消息队列给订单服务
        } else {
            System.out.println("剩余" + stock + "失败");
        }

        jedis.close();
        return "1";
    }

    //拼运气
    @RequestMapping("kill")
    @ResponseBody
    public String kill() {


        Jedis jedis = redisUtil.getJedis();

        jedis.watch("106");

        Integer stock = Integer.parseInt(jedis.get("106"));

        if (stock > 0) {
            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0) {
                System.out.println("剩余" + stock + "成功");
                //发消息队列给订单服务
            } else {
                System.out.println("剩余" + stock + "失败");
            }
        }

        jedis.close();

        return "1";
    }

}
