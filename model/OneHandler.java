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
public class OnerHandler extends AbstractMapperHandler {
    
    protected static final String functionPre="one";

    //实体路径[对应mybatis-plus的实体]
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

    protected static void init() {
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
    protected String dealWithTypeString(String paramName)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        FunctionHandler functionHandler = new FunctionHandler(functionPre,paramName);
        return functionHandler.execute();
    }
}
