package ;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.platform.commons.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * @author yccjack
 * @since 2022/1/26
 */
public class RequireOrderHandler extends MapperModel {

    protected final static String requireOrderPoPath = mapperRootPath + "..\\po";

    /**
     *    Po类的全路径名前缀，用于加载类使用
     */
    protected final static String classWholePathPre
        = "...po.";

    protected static List<String> poClassName = new ArrayList<>(64);

    static {
        init();
    }

    private static void init() {
        File file = new File(requireOrderPoPath);
        if (file.isDirectory()) {
            String[] fileList = file.list();
            if (fileList == null) {
                System.out.println("无po类，请检查路径：" + requireOrderPoPath);
                return;
            }
            for (String filePath : fileList) {
                File readFile = new File(requireOrderPoPath + "\\" + filePath);
                String name = readFile.getName();
                String className = name.substring(0, name.lastIndexOf("."));
                poClassName.add(className);
                parse(classWholePathPre, className);
            }

        }
    }

    @Override
    protected String handler(String paramName, String typeName, boolean create) {
        return handler(paramName, typeName);
    }

    @Override
    protected String handler(String paramName, String typeName) {
        if (typeName.contains("String")) {
            return dealWithTypeString(paramName);
        } else if (typeName.contains("Page")) {
            return dealWithTypePage();
        }
        Map<String, String> stringStringMap = poTableMap.get();
        String key = stringStringMap.keySet().parallelStream().filter(p -> {
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
                result.next();
                return parseToJson(key, result);
            } catch (SQLException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    private String dealWithTypePage() {
        return JSON.toJSONString(new PageVO());
    }

    static class ActionHandler {
    
        public static final String OPERATOR_MODIFY = "modify";

       
        public static final String OPERATOR_CLOSE = "close";

        
        public static final String OPERATOR_DELETE = "delete";

        public static final String OPERATOR_CANCEL = "cancel";

        static List<String> actionList = new ArrayList<>(6);

        static {
            actionList.add(OPERATOR_MODIFY);
            actionList.add(OPERATOR_CLOSE);
            actionList.add(OPERATOR_DELETE);
            actionList.add(OPERATOR_CANCEL);
        }

        static String handler() {
            int randomAction = (int) (Math.random() * 4 + 1);
            return actionList.get(randomAction);
        }
    }

    private String dealWithTypeString(String paramName) {
        if (paramName.toLowerCase(Locale.ROOT).contains("action")) {
            return ActionHandler.handler();
        } else {
            return null;
        }
    }

    SimpleDateFormat sb = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private String parseToJson(String key, ResultSet result)
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
                        String format = sb.format(object);
                        field.set(obj, format);
                    } else {
                        field.set(obj, object);
                    }

                }
            }
        }
        return JSON.toJSONString(obj);
    }

    public static void main(String[] args) {

        System.out.println(JSON.toJSONString(poMap.get()));
        System.out.println(JSON.toJSONString(poTableMap.get()));
    }
}
