package ;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.baomidou.mybatisplus.annotation.TableName;
import auto.MysqlConnect;

/**
 * 从此处开始跟系统业务关联比较大，如果想迁移，改动这里的代码。以及策略类
 *
 * @author yccjack
 * @since 2022/1/26
 */
public class MapperModel {

    protected static ThreadLocal<List<MapperModel>> next = ThreadLocal.withInitial(ArrayList::new);

    protected AtomicInteger nextIndex = new AtomicInteger(0);

    public static String mapperRootPath = Model.mapperRootPath;

    protected final static String workOrderMapperPath = mapperRootPath + "..\\dao";

    protected final static String requireOrderMapperPath = mapperRootPath + "..\\dao";

    protected final static String returnOrderMapperPath = mapperRootPath + "..\\dao";

    protected static Connection connection = MysqlConnect.getMysqlConnect();

    protected static String preparedSql = "select * from %s limit 1";

    /**
     * 大写字母
     */
    protected static final String regexLower = "^[a-z]+$";

    /**
     * 同包子类需要加载的po类，打包后此map无法通过继承更改，需要更改的话增加[protected]修饰符
     */
    static ThreadLocal<Map<String, Class<?>>> poMap = ThreadLocal.withInitial(HashMap::new);

    static ThreadLocal<Map<String, String>> poTableMap = ThreadLocal.withInitial(HashMap::new);

    protected MapperModel() {

    }

    public MapperModel(String typeName) {
        if (typeName.toLowerCase(Locale.ROOT).contains("one")) {
            next.get().add(new OneHandler());
        } else if (typeName.toLowerCase(Locale.ROOT).contains("two")) {
            next.get().add(new TwoHandler());
        } else if (typeName.toLowerCase(Locale.ROOT).contains("three")) {
            next.get().add(new ThreeHandler());
        } else {
            next.get().add(new OtherHandler());
        }
    }

    public final String getHitData(String paramName,String typeName, boolean create) {
        List<MapperModel> mapperModels = next.get();
        if (mapperModels.size() > 0) {
            return mapperModels.get(nextIndex.getAndIncrement()).handler(paramName,typeName, create);
        } else {
            return handler(paramName,typeName, create);
        }

    }

    protected String handler(String paramName,String typeName) {
        throw new IllegalStateException("未找到策略，请增加[缺省]策略");
    }

    protected String handler(String paramName,String typeName, boolean create) {
        throw new IllegalStateException("未找到策略，请增加[缺省]策略");
    }

    protected static void parse(String classWholePathPre, String name) {

        try {
            Class<?> aClass = Class.forName(classWholePathPre + name);
            Annotation[] annotations = aClass.getAnnotations();
            if (annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof TableName) {
                        String hasNoPostClassName = name;
                        if (name.contains("po")) {
                            hasNoPostClassName = name.substring(0, name.indexOf("po"));
                        }
                        TableName tableNameAn = (TableName) annotation;
                        String tableName = tableNameAn.value();
                        poTableMap.get().put(hasNoPostClassName, tableName);
                        poMap.get().put(hasNoPostClassName, aClass);
                        break;
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
