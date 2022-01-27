package com.huawei.it.cspm.auto;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

/**
 * @author yWX1117504
 * @since 2022/1/25
 */
public class WithSpringboot extends TestClassMockAuto {
    protected void handlerAround(String simpleName, String lowerFirstName) {
        for (String s : importSb) {
            javaFile.append(s);
        }
        javaFile.append(doubleLine);
        classSb.append("@BeforeEach").append(line).append("public void before() {}").append(line);
        if (methodBuild.length() > 0) {
            classSb.append(methodBuild);
        }
        classSb.append("}");
        javaFile.append(classSb);
    }

    protected void getClassName(String simpleName) {
        classSb.append("public class ")
            .append(simpleName)
            .append("Test")
            .append(" extends InitDataTest{ ")
            .append(line);
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
                classSb.append("@Inject")
                    .append(line)
                    .append("private ")
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

    }

    protected void handlerClassAnnotation() {
        classSb.append(line).append(line);
        classSb.append("@Rollback")
            .append(line)
            .append("@Log4j")
            .append(line)
            .append("@AutoConfigureMockMvc")
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
        importSb.add("import org.junit.Assert;" + line);
        importSb.add("import org.junit.jupiter.api.Test;" + line);
        importSb.add("import org.springframework.beans.BeanUtils;" + line);
        importSb.add("import javax.inject.Inject;" + line);
        importSb.add("import org.junit.jupiter.api.BeforeEach;" + line);
        importSb.add("import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;" + line);
        importSb.add("import org.springframework.http.MediaType;" + line);
        importSb.add("import org.springframework.mock.web.MockHttpServletResponse;" + line);
        importSb.add("import org.springframework.test.annotation.Rollback;" + line);
        importSb.add("import org.springframework.test.web.servlet.MockMvc;" + line);
        importSb.add("import org.springframework.test.web.servlet.MvcResult;" + line);
        importSb.add("import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;" + line);
        importSb.add("import com.alibaba.fastjson.JSON;" + line);
        importSb.add("import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;" + line);
        importSb.add("import com.huawei.it.cspm.hlrc.provider.InitDataTest;" + line);
        importSb.add("import lombok.extern.log4j.Log4j;" + line);
    }
}
