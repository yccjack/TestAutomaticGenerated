package com.huawei.it.cspm.auto;

import com.alibaba.fastjson.JSON;
import com.huawei.it.cspm.auto.model.MapperModel;
import com.huawei.it.cspm.hlrcworkorder.requireorder.api.dto.RequireOrderDto;

/**
 * @author yccjack
 * @since 2022/1/26
 */
public final class Predict {

    private static final Predict predict = new Predict();

    public static Predict getPredict() {
        return predict;
    }

    private Predict() {

    }

    public String predictQuoteParam(String paramName,String paramType,String methodName) {
        boolean create = methodName.contains("create") || methodName.contains("new");
        if (paramType.contains(".")) {
            paramType = paramType.substring(paramType.lastIndexOf(".") + 1);
        }
        MapperModel mapperModel = new MapperModel(paramType);
        return mapperModel.getHitData(paramName,paramType,create);
    }
}
