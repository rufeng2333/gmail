package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.PmsBaseAttrInfo;
import com.rufeng2333.gmall.bean.PmsBaseAttrValue;
import com.rufeng2333.gmall.bean.PmsBaseSaleAttr;

import java.util.List;

public interface AttrService {

    List<PmsBaseAttrInfo> attrInfoList(String catalog3Id);


    String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);

    List<PmsBaseAttrValue> getAttrValueList(String attrId);

    List<PmsBaseSaleAttr> baseSaleAttrList();
}
