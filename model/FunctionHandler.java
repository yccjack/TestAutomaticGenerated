package ;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

/**
 * @author yccjack
 * @since 2022/1/28
 */
public class FunctionHandler extends AbstractMapperHandler {

    private List<String> handler = new ArrayList<>();

    private List<String> containsParamList = new ArrayList<>();

    private String table;

    private String param;

    public FunctionHandler(String functionPre, String param) {
        Map<String, List<String>> stringListMap = functionCList.get(functionPre);
        if (stringListMap != null) {
            List<String> paramList = stringListMap.get(param);
            if (CollectionUtils.isNotEmpty(paramList)) {
                handler.add("containsFunction");
                containsParamList.addAll(containsParamList);
            }
        }
        Map<String, String> tableFunctions = functionTList.get(functionPre);
        if (tableFunctions != null) {
            String tableName = tableFunctions.get(param);
            if (tableName != null) {
                handler.add("tableFunction");
                table = tableName;
            }
        }
        this.param = param;
    }

    public String execute() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (this.handler.size() == 0) {
            return null;
        }
        Iterator<String> iterator = handler.iterator();
        String result = null;
        while (iterator.hasNext() && result == null) {
            String next = iterator.next();
            Class<? extends FunctionHandler> aClass = this.getClass();
            Method method = aClass.getDeclaredMethod(next, String.class);
            Object invoke = method.invoke(this, this.param);
            if (invoke != null) {
                result = (String) invoke;
            }
        }
        return result;

    }

    private String tableFunction(String param) {
        try (PreparedStatement pstmt = connection.prepareStatement(String.format(preparedSql, table))) {
            ResultSet result = pstmt.executeQuery();
            boolean next = result.next();
            if (next) {
                StringBuilder tableParam = new StringBuilder();
                char[] chars = param.toCharArray();
                for (char aChar : chars) {
                    String s = aChar + "";
                    if (s.matches("^[A-Z]+$")) {
                        s = "_" + s.toLowerCase(Locale.ROOT);
                    }
                    tableParam.append(s);
                }

                Object object1 = result.getObject(tableParam.toString());
                return (String) object1;
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return param;
    }

    private String containsFunction(String param) {
        if (containsParamList.size() > 0) {
            return handler();
        } else {
            return null;
        }
    }

    private String randomFunction(String param) {
        return null;
    }

    private String handler() {
        int randomAction = (int) (Math.random() * (containsParamList.size() - 1) + 1);
        return containsParamList.get(randomAction);
    }
}
