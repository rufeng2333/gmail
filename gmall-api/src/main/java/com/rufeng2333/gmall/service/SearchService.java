package com.rufeng2333.gmall.service;

import com.rufeng2333.gmall.bean.PmsSearchParam;
import com.rufeng2333.gmall.bean.PmsSearchSkuInfo;

import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);
}
