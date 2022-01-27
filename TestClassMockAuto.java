package com.huawei.it.cspm.auto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import com.alibaba.fastjson.JSON;
import com.huawei.it.cspm.auto.model.Model;
import com.huawei.it.cspm.hlrcworkorder.requireorder.service.impl.RepairRequireOrderService;
import com.huawei.it.jalor5.core.util.CollectionUtil;

/**
 * 自动生成对应的mock测试基础类
 *
 * @author yWX1117504
 * @since 2022/1/20
 */
public abstract class TestClassMockAuto {

    /**
     * 换行符
     */
    protected static final String line = System.getProperty("line.separator", "/n");

    /**
     * 大写字母
     */
    protected static final String regex = "^[A-Z]+$";

    /**
     * 双换行
     */
    protected static final String doubleLine = line + line;

    /**
     * 格式化，暂时不用，生成文件后直接手动格式化就行
     */
    protected static final String fourSp = System.getProperty("    ");

    /**
     * 生成文件的文件夹前缀
     */
    public static String rootPath = Model.rootPath;

    public static String mapperPath = Model.mapperRootPath;

    /**
     * 生成的Java文件字符串
     */
    protected StringBuilder javaFile = new StringBuilder(128);

    /**
     * 生成的Java的import部分
     */
    protected Set<String> importSb = new HashSet<>(512);

    /**
     * 生成的Java class信息的部分
     */
    protected StringBuilder classSb = new StringBuilder(512);

    /**
     * 遍历时候提前记录的拼接字符串
     */
    protected StringBuilder javaFieldBuild = new StringBuilder(1024);

    /**
     * 测试方法拼接字符串
     */
    protected StringBuilder methodBuild = new StringBuilder(8 * 1024);

    /**
     * 类型判断-基本类型集合
     */
    protected static Map<String, Object> primitiveMap = new HashMap<>(8);

    /**
     * 类型判断-基本类型包装类集合
     */
    protected static Map<String, Object> packagingMap = new HashMap<>(8);

    /**
     * 已使用过参数集合，经过多次使用此集合会越来越大，内部数据会越来越完整
     */
    public static Map<String, Set<String>> usedParamMap = Model.usedParamMap;

    /**
     * 已经被使用过的参数名称集合
     */
    public static Map<String, List<String>> usedParamMapForParamName = Model.usedParamMapForParamName;

    /**
     * 当前处理的类的首字母小写名称。
     */
    protected String lowerFirstName;

    /**
     * 当前处理的类的简单类名；可能大写可能小写
     */
    protected String simpleName;

    static {
        primitiveMap.put("int", 1);
        primitiveMap.put("float", 1.0f);
        primitiveMap.put("boolean", true);
        primitiveMap.put("double", 1.0);
        primitiveMap.put("short", (short) 1);
        primitiveMap.put("long", 1.0);

        packagingMap.put("Integer", 1);
        packagingMap.put("Float", 1.0f);
        packagingMap.put("Boolean", true);
        packagingMap.put("Double", 1.0);
        packagingMap.put("Short", (short) 1);
        packagingMap.put("Long", 1.0);
        exerciseStr();
    }

    public static void main(String[] args) throws IOException {

        TestClassMockAuto testClassMockAuto = new WithSpringboot();
        testClassMockAuto.generateHasNoSpringBoot(RepairRequireOrderService.class);
    }

    public void generateHasNoSpringBoot(Class<?> clazz) throws IOException {
        String name = clazz.getName();
        simpleName = clazz.getSimpleName();
        String fileName = name.substring(19);
        String[] split = fileName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append("\\").append(s);
        }
        sb.append("Test.java");
        String filePath = rootPath + sb;
        File file = new File(filePath);
        if (file.exists()) {
            return;
        }
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            boolean mkdirs = parentDir.mkdirs();
            if (!mkdirs) {
                throw new RuntimeException("创建目录失败，请检查系统权限");
            }
        }
        handlerTestUtilsImport(fileName);
        Field[] fields = clazz.getDeclaredFields();
        handlerFieldImport(fields);
        handlerClassAnnotation();
        getClassName(simpleName);
        String firstChar = simpleName.substring(0, 1);
        //首字母大写
        boolean matches = firstChar.matches(regex);
        lowerFirstName = simpleName;
        if (matches) {
            lowerFirstName = simpleName.replaceFirst(firstChar, firstChar.toLowerCase(Locale.ROOT));
        }
        classSb.append("private ").append(simpleName).append(" ").append(lowerFirstName).append(";").append(doubleLine);
        handlerPrivateField(fields);
        handlerTestMethodCreate(clazz, lowerFirstName);
        handlerAround(simpleName, lowerFirstName);
        System.out.println(javaFile);
        createTestFile(file);
    }

    private void createTestFile(File file) throws IOException {
        boolean newFile = file.createNewFile();
        if (!newFile) {
            throw new RuntimeException("创建文件失败，请检查权限、文件深度、目录权限");
        }
        FileOutputStream fis = new FileOutputStream(file);
        fis.write(javaFile.toString().getBytes(StandardCharsets.UTF_8));
    }

    protected void handlerTestMethodCreate(Class<?> clazz, String lowerFirstName) {
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String simpleName = clazz.getSimpleName();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            List<Object> paramList = new ArrayList<>();
            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getSimpleName().equalsIgnoreCase(simpleName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Parameter[] parameters = method.getParameters();
            String[] params = u.getParameterNames(method);
            String methodName = method.getName();
            if (methodName.contains("Clone")) {
                continue;
            }
            int length = parameterTypes.length;
            methodBuild.append("@Test").append(line);
            methodBuild.append("public void ").append(methodName).append("Test(){").append(line);

            for (int i = 0; i < length; i++) {
                if (params == null) {
                    continue;
                }
                Class<?> type = parameterTypes[i];
                String typeName = type.getName();
                String typeSimpleName = type.getSimpleName();
                String name = params[i];
                String paramFromStr = getParamFromStr(simpleName, name);
                if (type.isPrimitive()) {
                    Object o = handlerPrimitive(typeName);
                    paramList.add(o);
                } else {
                    if (paramFromStr == null || "null".equalsIgnoreCase(paramFromStr)) {
                        List<String> strings = usedParamMapForParamName.get(name);
                        if (CollectionUtil.isNullOrEmpty(strings)) {
                            if (packagingMap.containsKey(typeSimpleName)) {
                                Object packageParam = packagingMap.get(typeSimpleName);
                                paramList.add(packageParam);
                            } else {
                                String param = createParam(name, typeName, methodName);
                                if (param != null) {
                                    String s = param.replaceAll("\"", "'");
                                    methodBuild.append(typeSimpleName)
                                        .append(" ")
                                        .append(typeSimpleName)
                                        .append("= JSON.parseObject(\"")
                                        .append(s)
                                        .append("\",")
                                        .append(typeSimpleName)
                                        .append(".class);")
                                        .append(line);
                                }
                                paramList.add(typeSimpleName);
                            }
                            importSb.add("import " + typeName + ";" + line);

                        } else {
                            if (typeName.contains("String")) {
                                paramList.add("\"" + strings.get(0) + "\"");
                            } else if (packagingMap.containsKey(typeSimpleName)) {
                                Object o = packagingMap.get(typeSimpleName);
                                paramList.add(o);
                            } else {
                                paramList.add(strings.get(0));

                            }
                        }

                    } else {
                        List<String> strings = usedParamMapForParamName.get(paramFromStr);
                        if (typeName.contains("String")) {
                            paramList.add("\"" + strings.get(0) + "\"");
                        } else {
                            paramList.add(strings.get(0));
                        }
                    }
                }
            }
            methodBuild.append(lowerFirstName).append(".").append(methodName).append("(");
            for (Object o : paramList) {
                methodBuild.append(o).append(",");
            }
            methodBuild.deleteCharAt(methodBuild.length() - 1);
            methodBuild.append(");").append(line).append("}");
        }
    }

    protected Object handlerPrimitive(String typeName) {
        return primitiveMap.get(typeName);
    }

    protected String createParam(String paramName, String paramTypeName, String methodName) {
        return Model.createParam(paramName, paramTypeName, methodName);

    }

    protected String getParamFromStr(String simpleName, String name) {
        Set<String> objects = usedParamMap.get(simpleName);
        if (objects == null) {
            return null;
        }
        return objects.parallelStream().filter(p -> p.contains(name)).findFirst().orElse("NA");
    }

    protected static void exerciseStr() {
        File file = new File(rootPath + "\\auto\\exercise\\autoTestModel.xl");
        StringBuilder modelSb = new StringBuilder(2048);
        try (BufferedReader fis = new BufferedReader(new FileReader(file))) {
            String s = fis.readLine();
            while (s != null) {
                modelSb.append(s);
                s = fis.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Model model = parseExerciseFile(modelSb);
        JSON.toJSONString(model);
    }

    protected static Model parseExerciseFile(StringBuilder modelSb) {
        String s = modelSb.toString();
        if (StringUtils.isNotBlank(s)) {
            String[] zero = s.split("#");
            if (zero.length < 1) {
                return null;
            }
            String first = zero[1];
            String second = zero[2];
            if (StringUtils.isBlank(first) || StringUtils.isBlank(second)) {
                return null;
            }
            String[] classData = first.split("@@");
            Model model = new Model();
            int length = classData.length;
            String[] paramData = second.split("@@");
            for (int i = 0; i < length; i++) {
                String classDatum = classData[i];
                String paramDatum = paramData[i];
                model.createMode(classDatum, paramDatum, i);
            }
            return model;
        }
        return null;

    }

    protected void getClassName(String simpleName) {
        classSb.append("public class ").append(simpleName).append("Test{").append(line);
    }

    protected void handlerAround(String simpleName, String lowerFirstName) {
        for (String s : importSb) {
            javaFile.append(s);
        }
        javaFile.append(doubleLine);
        classSb.append(importSb).append(doubleLine);
        classSb.append("@Before")
            .append(line)
            .append("public void before() {")
            .append(line)
            .append(simpleName)
            .append(" ")
            .append(lowerFirstName)
            .append(" = PowerMockito.spy(")
            .append("new ")
            .append(simpleName)
            .append("());")
            .append(line);
        if (javaFieldBuild.length() > 0) {
            classSb.append(javaFieldBuild);
        }
        classSb.append(line).append("}").append(line);
        if (methodBuild.length() > 0) {
            classSb.append(methodBuild);
        }
        classSb.append("}");
        javaFile.append(classSb);
    }

    protected void handlerPrivateField(Field[] fields) {
        for (Field field : fields) {
            boolean matches;
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers())) {
                String loweName = "";
                String fieldName = field.getType().getSimpleName();
                String firstStr = fieldName.substring(0, 1);
                matches = firstStr.matches(regex);
                if (matches) {
                    loweName = fieldName.replaceFirst(firstStr, firstStr.toLowerCase(Locale.ROOT));
                }
                classSb.append("private ")
                    .append(fieldName)
                    .append(" ")
                    .append(loweName)
                    .append(";")
                    .append(doubleLine);
                handlerBeforeMock(loweName, fieldName);
            }
        }
    }

    protected void handlerBeforeMock(String loweName, String fieldName) {
        javaFieldBuild.append(loweName)
            .append(" = ")
            .append("PowerMockito.mock(")
            .append(fieldName)
            .append(".class")
            .append(");")
            .append(line)
            .append("ReflectionTestUtils.setField(")
            .append(lowerFirstName)
            .append(",")
            .append("\"")
            .append(loweName)
            .append("\"")
            .append(",")
            .append(loweName)
            .append(");")
            .append(line);
    }

    protected void handlerClassAnnotation() {
        classSb.append(line).append(line);
        classSb.append("@RunWith(PowerMockRunner.class)")
            .append(line)
            .append("@PrepareForTest({RandomUtil.class})")
            .append(line);
    }

    protected void handlerFieldImport(Field[] fields) {
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getType().getName();
            importSb.add("import " + fieldName + ";" + line);
        }
    }

    protected void handlerTestUtilsImport(String fileName) {
        String getPackageStr = fileName.substring(0, fileName.lastIndexOf("."));
        javaFile.append("package com.huawei.it.cspm.").append(getPackageStr).append(";").append(line).append(line);
        importSb.add("import org.powermock.modules.junit4.PowerMockRunner;" + line);
        importSb.add("import org.junit.runner.RunWith;" + line);
        importSb.add("import org.powermock.core.classloader.annotations.PrepareForTest;" + line);
        importSb.add("import com.huawei.it.jalor5.core.util.RandomUtil;" + line);
        importSb.add("import org.junit.Before;" + line);
        importSb.add("import org.powermock.api.mockito.PowerMockito;" + line);
        importSb.add("import org.springframework.test.util.ReflectionTestUtils;" + line);
    }

}
