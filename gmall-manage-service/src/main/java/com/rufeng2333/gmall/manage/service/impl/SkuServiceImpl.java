package com.rufeng2333.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.rufeng2333.gmall.bean.*;
import com.rufeng2333.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuImageMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuInfoMapper;
import com.rufeng2333.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.rufeng2333.gmall.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
}
