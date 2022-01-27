package ;

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
import model.Model;

/**
 * 自动生成对应的mock测试基础类
 *
 * @author yccjack
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
        testClassMockAuto.generateHasNoSpringBoot(...class);
    }

   
     /**
     * [创建目标文件] --> [生成类的package+import] --> [生成类的注入属性] -->
     * [生成public方法的测试类  --> {获取目标的参数
     * if
     * 1. 基本类型 从基本类型的基础数据中取【根据自己需求自定义】
     * 2. 包装类型 从包装类型的数据集中取
     * 3. 引用类型， 根据业务的不同自行实现  demo--> {@link OneHandler} {@link MapperModel}
     * <p>
     * } --> 生成目标方法的测试方法；
     * <p>
     * ]
     *
     * @param clazz
     * @throws IOException
     */
    public void generateHasNoSpringBoot(Class<?> clazz) throws IOException {
        String name = clazz.getName();
        simpleName = clazz.getSimpleName();
        String fileName = name.substring(19);
        String packageStr = fileName.substring(0, fileName.lastIndexOf("."));
        //获取需要创建的文件
        File file = getFile(fileName);
        if (file == null) {
            return;
        }
        handlerTestUtilsImport(packageStr);

        Field[] fields = clazz.getDeclaredFields();
        handlerFieldImport(fields);
        handlerClassAnnotation();
        getClassName(simpleName);
        firstClassLower();
        classSb.append("private ").append(simpleName).append(" ").append(lowerFirstName).append(";").append(doubleLine);
        handlerPrivateField(fields);
        handlerTestMethodCreate(clazz, lowerFirstName);
        handlerAround(simpleName, lowerFirstName);
        System.out.println(javaFile);
        createTestFile(file);
    }

    /**
     * 将首字母变成小写
     */
    protected void firstClassLower() {
        String firstChar = simpleName.substring(0, 1);
        //首字母大写
        boolean matches = firstChar.matches(regex);
        lowerFirstName = simpleName;
        if (matches) {
            lowerFirstName = simpleName.replaceFirst(firstChar, firstChar.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * 获取需要生成的java文件
     *
     * @param fileName 文件名
     * @return 最终路径的文件
     */
    protected File getFile(String fileName) {
        String[] split = fileName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append("\\").append(s);
        }
        sb.append("Test.java");
        String filePath = rootPath + sb;
        File file = new File(filePath);
        if (file.exists()) {
            return null;
        }
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            boolean mkdirs = parentDir.mkdirs();
            if (!mkdirs) {
                throw new RuntimeException("创建目录失败，请检查系统权限");
            }
        }
        return file;
    }

    /**
     * 生成文件
     *
     * @param file 生成的文件
     * @throws IOException 文件异常
     */
    protected void createTestFile(File file) throws IOException {
        boolean newFile = file.createNewFile();
        if (!newFile) {
            throw new RuntimeException("创建文件失败，请检查权限、文件深度、目录权限");
        }
        FileOutputStream fis = new FileOutputStream(file);
        fis.write(javaFile.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成测试类对应目标类的public方法
     *
     * @param clazz 目标类
     * @param lowerFirstName 目标类首字母小写名称
     */
    protected void handlerTestMethodCreate(Class<?> clazz, String lowerFirstName) {
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String simpleName = clazz.getSimpleName();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            List<Object> paramList = new ArrayList<>();
            Class<?> declaringClass = method.getDeclaringClass();
            //剔除继承非覆写的方法
            if (!declaringClass.getSimpleName().equalsIgnoreCase(simpleName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Parameter[] parameters = method.getParameters();
            String[] params = u.getParameterNames(method);
            String methodName = method.getName();
            //剔除从Object继承的clone方法
            if (methodName.contains("Clone")) {
                continue;
            }
            int length = parameterTypes.length;
            methodBuild.append("@Test").append(line);
            methodBuild.append("public void ").append(methodName).append("Test(){").append(line);
            for (int i = 0; i < length; i++) {
                //未检测到方法有参数
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

    /**
     * 训练的文件中未发现此类型此名称使用过的参数，重新生成新的参数
     *
     * @param paramName 参数名称
     * @param paramTypeName 参数类型
     * @param methodName 方法名称
     * @return 根据需求实现获取的新参数
     */
    protected String createParam(String paramName, String paramTypeName, String methodName) {
        return Model.createParam(paramName, paramTypeName, methodName);

    }

    /**
     * 从训练文件中获取已经使用过的参数
     *
     * @param simpleName 简单类型
     * @param name 参数名称
     * @return 已使用过的参数
     */
    protected String getParamFromStr(String simpleName, String name) {
        Set<String> objects = usedParamMap.get(simpleName);
        if (objects == null) {
            return null;
        }
        return objects.parallelStream().filter(p -> p.contains(name)).findFirst().orElse("NA");
    }

    /**
     * 初始化训练文件
     */
    protected static void exerciseStr() {
        File file = new File(rootPath + "\\exercise\\autoTestModel.xl");
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

    /**
     * 解析xl文件
     *
     * @param modelSb xl文件读出来的集合
     * @return 模型
     */
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

    /**
     * 获取生成的测试类的类名。此处无继承实现，如需继承实现需要在子类中覆写
     *
     * @param simpleName 简单类名
     */
    protected void getClassName(String simpleName) {
        classSb.append("public class ").append(simpleName).append("Test{").append(line);
    }

    /**
     * 拼接整体的文件得到最终文件集合
     *
     * @param simpleName 目标简单类名
     * @param lowerFirstName 目标简单类名首字母小写
     */
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

    /**
     * 生成测试类中拼接出目标类的非静态的注入属性
     *
     * @param fields 目标类属性集
     */
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

    /**
     * 生成的测试类的类头注解。此处为默认实现，需要进行业务的覆写[子类覆写]
     */
    protected void handlerClassAnnotation() {
        classSb.append(line).append(line);
        classSb.append("@RunWith(PowerMockRunner.class)")
            .append(line)
            .append("@PrepareForTest({RandomUtil.class})")
            .append(line);
    }

    /**
     * 生成目标测试类中注入的其他类的导入信息
     *
     * @param fields 目标类的属性集
     */
    protected void handlerFieldImport(Field[] fields) {
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getType().getName();
            importSb.add("import " + fieldName + ";" + line);
        }
    }

    /**
     * 初始化package+测试类需要导入的包
     *
     * @param packageStr 截取的工程目录到生成测试类的包路径
     */
    protected void handlerTestUtilsImport(String packageStr) {
        javaFile.append("package com.huawei.it.cspm.").append(packageStr).append(";").append(line).append(line);
        importSb.add("import org.powermock.modules.junit4.PowerMockRunner;" + line);
        importSb.add("import org.junit.runner.RunWith;" + line);
        importSb.add("import org.powermock.core.classloader.annotations.PrepareForTest;" + line);
        importSb.add("import org.junit.Before;" + line);
        importSb.add("import org.powermock.api.mockito.PowerMockito;" + line);
        importSb.add("import org.springframework.test.util.ReflectionTestUtils;" + line);
    }

}
