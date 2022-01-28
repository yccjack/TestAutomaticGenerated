package ;

/**
 * @author yccjack
 * @since 2022/1/26
 */
public class OtherHandler extends MapperModel {
    protected static final String functionPre="other";
    @Override
    protected String dealWithTypeString(String paramName)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        FunctionHandler functionHandler = new FunctionHandler(functionPre,paramName);
        return functionHandler.execute();
    }
}
