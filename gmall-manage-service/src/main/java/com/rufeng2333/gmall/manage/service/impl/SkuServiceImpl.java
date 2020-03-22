package com.rufeng2333.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.rufeng2333.gmall.bean.*;
import com.rufeng2333.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuImageMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuInfoMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.rufeng2333.gmall.service.SkuService;
import com.rufeng2333.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);

        String skuId = pmsSkuInfo.getId();

        List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuInfo.getSkuAttrValueList();
        for(PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValues){
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues = pmsSkuInfo.getSkuSaleAttrValueList();
        for(PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValues){
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        List<PmsSkuImage> pmsSkuImages = pmsSkuInfo.getSkuImageList();
        for(PmsSkuImage pmsSkuImage : pmsSkuImages){
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId){
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImages);

        return skuInfo;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId) {

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        Jedis jedis = redisUtil.getJedis();

        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);
        if(StringUtils.isNotBlank(skuJson)) {
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else{
            String token = UUID.randomUUID().toString();
            String ok = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10*1000);
            if(StringUtils.isNotBlank(ok) && ok.equals("OK")){
                pmsSkuInfo = getSkuByIdFromDb(skuId);
                if(pmsSkuInfo != null){
                    jedis.set("sku:"+skuId+":info", JSON.toJSONString(pmsSkuInfo));
                }else{
                    jedis.setex("sku:"+skuId+":info",60*3, JSON.toJSONString(""));
                }
                String lockToken = jedis.get("sku:" + skuId + ":info");
                if(StringUtils.isNotBlank(lockToken) && lockToken.equals(token));//用token来确认是否是自己的锁
                {
                    /*当刚好代码执行到这里时，自己的token过期了，然后在if里面又把下一个用户的token删除了，怎么解决？
                      可以用lua脚本，在查询到key的同时删除该key，防止高并发下的意外发生
                      将：String newToken = jedis.get("sku:" + skuId + ":info");
                         if(newToken!=null&&!newToken.equals("")&&newToken.equals(token)){
                             jedis.del("sku:" + skuId + ":lock");
                         }改为下面的代码：lua脚本，在查询到key的同时删除该key
                       String script = "if redis.call('get' , KEYS[1]==ARGV[1] then return redis.call('del',KEYS[1]))
                                        else return o end";
                       jedis.eval(script,Collections.singletonList("lock"),Conllections.singletonList(token));
                */
                    jedis.del("sku:" + skuId + ":lock");
                }
            }else {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById(skuId);
            }

        }

        jedis.close();

        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);

        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(select);
        }

        return pmsSkuInfos;
    }
}
