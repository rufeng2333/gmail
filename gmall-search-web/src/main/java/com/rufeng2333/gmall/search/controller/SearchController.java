package com.rufeng2333.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.rufeng2333.gmall.annotations.LoginRequired;
import com.rufeng2333.gmall.bean.*;
import com.rufeng2333.gmall.service.AttrService;
import com.rufeng2333.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map) {

        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);

        map.put("skuLsInfoList", pmsSearchSkuInfos);

        Set<String> valueIdSet = new HashSet<>();

        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        map.put("attrList", pmsBaseAttrInfos);

        //迭代器删除相同属性+面包屑
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            //for循环要放在迭代器前面，迭代器只能用一次
            for (String delValueId : delValueIds) {
            Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();

                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));

                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (valueId.equals(delValueId)) {
                            //给面包屑的属性名称赋值
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除相同属性
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            map.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        String urlParam = getUrlParam(pmsSearchParam);
        map.put("urlParam", urlParam);

        return "list";
    }

    public String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            urlParam += "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + catalog3Id;

        }
        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                if (!pmsSkuAttrValue.equals(delValueId)) {
                    urlParam += "&valueId=" + pmsSkuAttrValue;
                }
            }
        }
        return urlParam;
    }

    public String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            urlParam += "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + catalog3Id;

        }
        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                urlParam += "&valueId=" + pmsSkuAttrValue;
            }
        }
        return urlParam;
    }


    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index() {
        return "index";
    }

}
