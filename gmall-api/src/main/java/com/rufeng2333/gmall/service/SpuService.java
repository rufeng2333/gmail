package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.PmsProductInfo;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);
}
