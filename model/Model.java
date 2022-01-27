package ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Predict;
import TestClassMockAuto;

import lombok.Data;

/**
 * 模型类
 *
 * @author yccjack
 * @since 2022/1/25
 */
@Data
public class Model {

    public static String userPath = System.getProperty("user.dir");

    public static Map<String, List<Model>> modelMap = new HashMap<>(1024);

    public static Map<String, String> paths = new HashMap<>(1024);

    public static Map<String, Set<String>> usedParamMap = new HashMap<>(1024);

    public static Map<String, List<String>> usedParamMapForParamName = new HashMap<>(8 * 1024);

    public static String rootPath = userPath;


    public static String mapperRootPath = userPath;


    public static Predict predict = Predict.getPredict();

    /**
     * 类型
     */
    private String simpleName;

    /**
     * 参数
     */
    private List<Param> paramTypeName;

    /**
     * 在xl文件#维度的位置
     */
    private int index;

    /**
     * 参数构造的路径
     */
    private String paramFilePath;

    public static String createParam(String paramName, String paramTypeName, String methodName) {
        return predict.predictQuoteParam(paramName,paramTypeName,methodName);
    }

    public void setParamFilePath(String paramFilePath) {
        this.paramFilePath = paramFilePath + ".xl";
    }

    public final void createMode(String classDatum, String paramData, int index) {
        String[] split = classDatum.split("\\.");
        String className = split[0];
        this.setSimpleName(className);
        String methodName = split[1];
        String path = split[3];
        this.setParamFilePath(path);
        String[] paramSp = split[2].split(",");
        String[] paramDataSp = paramData.split(",");
        Set<String> objects = usedParamMap.computeIfAbsent(simpleName, k -> new HashSet<>());

        List<Param> params = new ArrayList<>();
        this.setParamTypeName(params);
        for (int i = 0; i < paramSp.length; i++) {
            Param param = new Param();
            String paramIndex = paramSp[i];
            String[] paramDataArray = paramDataSp[i].split("-");
            String usedParam = handlerUsedParam(path);
            String[] usedArray = usedParam.split(",");
            if (paramIndex.contains("Array")) {
                String paramName = paramDataArray[1];
                param.setParamTypeName(paramIndex);
                param.setInnerParamType(paramDataArray[0]);
                param.setParamName(paramName);
                int usedParamIndex = Integer.parseInt(paramDataArray[2]);
                param.setIndex(usedParamIndex);
                String used = usedArray[usedParamIndex - 1];
                param.setUsedParam(used);
                objects.add(paramName);
                List<String> strings = usedParamMapForParamName.computeIfAbsent(paramName, k -> new ArrayList<>());
                strings.add(used);
            } else {
                String paramName = paramDataArray[0];
                param.setParamTypeName(paramIndex);
                param.setInnerParamType(null);
                param.setParamName(paramName);
                int usedParamIndex = Integer.parseInt(paramDataArray[1]);
                param.setIndex(usedParamIndex);
                String used = usedArray[usedParamIndex - 1];
                param.setUsedParam(used);
                objects.add(paramName);
                List<String> strings = usedParamMapForParamName.computeIfAbsent(paramName, k -> new ArrayList<>());
                strings.add(used);
            }
            param.setMethodName(methodName);
            params.add(param);
        }
        List<Model> models = modelMap.get(split[0]);
        if (models != null) {
            models.add(this);
        } else {
            models = new ArrayList<>();
            models.add(this);
            modelMap.put(split[0], models);
        }
        paths.put(simpleName, path);

    }

    private String handlerUsedParam(String path) {
        File file = new File(rootPath + "\\auto\\exercise\\param\\" + this.getParamFilePath());
        StringBuilder sb = new StringBuilder(1024);
        try (BufferedReader fis = new BufferedReader(new FileReader(file))) {
            String s = fis.readLine();
            while (s != null) {
                sb.append(s);
                s = fis.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();

    }

    /**
     * 参数类
     */
    @Data
    public static class Param {
        /**
         * 参数类型
         */
        private String paramTypeName;

        /**
         * 方法名
         */
        private String methodName;

        /**
         * 泛型，只存在于集合类型中
         */
        private String innerParamType;

        /**
         * 参数名称，此字段为学习字段。
         */
        private String paramName;

        /**
         * 参数在参数构造路径文件的位置
         */
        private int index;

        private String usedParam;
    }

}
