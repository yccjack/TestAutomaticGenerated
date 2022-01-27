package ;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

/**
 * @author yccjack
 * @since 2022/1/25
 */
public class WithoutSpringboot extends TestClassMockAuto {

    protected void handlerAround(String simpleName,  String lowerFirstName) {
        javaFile.append("@Before")
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
            javaFile.append(javaFieldBuild);
        }
        javaFile.append(line).append("}").append(line).append("}");
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
                javaFile.append("private ")
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
        javaFile.append(line).append(line);
        javaFile.append("@RunWith(PowerMockRunner.class)")
            .append(line)
            .append("@PrepareForTest({RandomUtil.class})")
            .append(line);
    }

    protected void handlerFieldImport(Field[] fields) {
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getType().getName();
            javaFile.append("import ").append(fieldName).append(";").append(line);
        }
    }

    protected void handlerTestUtilsImport(String fileName, StringBuilder javaFile) {
        String getPackageStr = fileName.substring(0, fileName.lastIndexOf("."));
        javaFile.append("package ").append(getPackageStr).append(";").append(line).append(line);
        javaFile.append("import org.powermock.modules.junit4.PowerMockRunner;").append(line);
        javaFile.append("import org.junit.runner.RunWith;").append(line);
        javaFile.append("import org.powermock.core.classloader.annotations.PrepareForTest;").append(line);
        javaFile.append("import org.junit.Before;").append(line);
        javaFile.append("import org.powermock.api.mockito.PowerMockito;").append(line);
        javaFile.append("import org.springframework.test.util.ReflectionTestUtils;").append(line);
    }
}
