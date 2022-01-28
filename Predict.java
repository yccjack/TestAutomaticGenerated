package ;

import com.alibaba.fastjson.JSON;
import auto.model.MapperModel;

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

    public String predictQuoteParam(String paramName, String paramType, String methodName) {
        boolean create = methodName.contains("create") || methodName.contains("new");
        if (paramType.contains(".")) {
            paramType = paramType.substring(paramType.lastIndexOf(".") + 1);
        }
        MapperModel mapperModel = new MapperModel(paramType);
        try {
            String hitData = mapperModel.getHitData(paramName, paramType, create);
            return hitData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
