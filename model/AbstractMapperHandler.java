package ;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.platform.commons.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * @author yccjack
 * @since 2022/1/28
 */
public class AbstractMapperHandler extends MapperModel {

    protected static String preparedSql = "select * from %s limit 1";

    public static final Map<String, Map<String, List<String>>> functionCList = new HashMap<>(1024);

    public static Map<String, Map<String, String>> functionTList = new HashMap<>(1024);

    static {
        initFunction();
    }

    private static void initFunction() {
        String fileContent = Model.getFileContent(rootPath + "auto\\exercise\\function.xl");
        String[] fileList = fileContent.split("@@");
        for (String s : fileList) {
            String[] classInfo = s.split(":");
            String classSimpleName = classInfo[0];
            String stringInfo = classInfo[1];
            String[] stringList = stringInfo.split(";");
            for (String stringFunctionInfo : stringList) {
                String[] functionInfo = stringFunctionInfo.split("\\[");
                String name = functionInfo[0];
                String function = functionInfo[1];
                String[] step = function.split(",");
                String action = step[0];
                if (action.equals("C")) {
                    Map<String, List<String>> stringListMap = functionCList.get(classSimpleName);
                    if (stringListMap == null) {
                        stringListMap = new HashMap<>();
                        functionCList.put(classSimpleName, stringListMap);
                    }
                    List<String> strings = stringListMap.get(name);
                    if (strings == null) {
                        strings = new ArrayList<>();
                        stringListMap.put(name, strings);
                    }
                    for (int i = 1; i < step.length; i++) {
                        strings.add(step[i]);
                    }
                } else if (action.equals("T")) {
                    Map<String, String> stringListMap = functionTList.get(classSimpleName);
                    if (stringListMap == null) {
                        stringListMap = new HashMap<>();
                        functionTList.put(classSimpleName, stringListMap);
                    }
                    stringListMap.put(name, step[1]);

                }
            }
        }
    }

    @Override
    protected String handler(String paramName, String typeName, boolean create)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return handler(paramName, typeName);
    }

    @Override
    protected String handler(String paramName, String typeName)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (typeName.contains("String")) {
            return dealWithTypeString(paramName);
        } else if (typeName.contains("Page")) {
            return dealWithTypePage();
        }
        Map<String, String> stringStringMap = poTableMap.get();
        String key = stringStringMap.keySet().parallelStream().filter(p -> {
            if (!p.contains("Po")) {
                return false;
            }
            String po = p.substring(0, p.indexOf("Po"));
            if (typeName.contains(po)) {
                return true;
            } else {
                return false;
            }
        }).findFirst().orElse("NA");
        if ("NA".equals(key)) {

        } else {
            String s = stringStringMap.get(key);
            try (PreparedStatement pstmt = connection.prepareStatement(String.format(preparedSql, s))) {
                ResultSet result = pstmt.executeQuery();
                boolean next = result.next();
                if (next) {
                    return parseToJson(key, result);
                } else {
                    return null;
                }

            } catch (SQLException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    protected String dealWithTypePage() {
        return JSON.toJSONString(new PageVO());
    }

    protected String dealWithTypeString(ExecuteFunction<String> function) {
        throw new RuntimeException("请实现 [dealWithTypeString] 方法进行字符串预测");
    }

    protected String dealWithTypeString(String paramName)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        throw new RuntimeException("请实现 [dealWithTypeString] 方法进行字符串预测");
    }

    static SimpleDateFormat sb = new SimpleDateFormat("yyyy-MM-dd");

    protected String parseToJson(String key, ResultSet result)
        throws SQLException, InstantiationException, IllegalAccessException {
        Class<?> aClass = poMap.get().get(key);
        Object obj = aClass.newInstance();
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            TableField annotation = field.getAnnotation(TableField.class);
            Class<?> type = field.getType();
            String typeSimpleName = type.getSimpleName();
            if (annotation != null) {
                String value = annotation.value();
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                Object object = result.getObject(value);
                if (object != null) {
                    field.setAccessible(true);
                    if (object instanceof Float) {
                        BigDecimal bigDecimal = new BigDecimal(String.valueOf(object));
                        double v = bigDecimal.doubleValue();
                        field.set(obj, v);
                    } else if (object instanceof Date) {
                        if (typeSimpleName.contains("String")) {
                            String format = sb.format(object);
                            field.set(obj, format);
                        } else {
                            Date date = (Date) object;
                            field.set(obj, date.toLocalDate());
                        }

                    } else if (object instanceof java.sql.Timestamp) {
                        if (typeSimpleName.contains("String")) {
                            String format = sb.format(object);
                            field.set(obj, format);
                        } else {
                            java.sql.Timestamp date = (java.sql.Timestamp) object;
                            field.set(obj, date.toLocalDateTime());
                        }
                    } else {
                        if (typeSimpleName.contains("Boolean")) {
                            if (object instanceof Integer) {
                                Integer bo = (Integer) object;
                                field.set(obj, bo > 0);
                            }
                        } else {
                            if (object instanceof Boolean) {
                                Boolean bo = (Boolean) object;
                                field.set(obj, bo?1:0);
                            }else {
                                field.set(obj, object);
                            }

                        }

                    }

                }
            }
        } return JSON.toJSONString(obj);
    }
}
