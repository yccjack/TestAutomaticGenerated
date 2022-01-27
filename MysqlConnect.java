package ;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author yccjack
 * @since 2022/1/26
 */
public class MysqlConnect {

    private static MysqlConnect mysqlConnect = new MysqlConnect();

    private static Connection connection;

    private MysqlConnect() {
        connection = getConnection();
    }

    public static Connection getMysqlConnect() {
        return connection;
    }

    public Connection getConnection() {
        Connection con = null;
        try {
            //驱动程序名
            String driver = "com.mysql.jdbc.Driver";
            //URL指向要访问的数据库名mydata
            String url
                = "";
            //MySQL配置时的用户名
            String user = "";
            //MySQL配置时的密码
            String password = "";
            //加载驱动程序
            Class.forName(driver);
            //1.getConnection()方法，连接MySQL数据库！！
            con = DriverManager.getConnection(url, user, password);
            if (!con.isClosed()) {
                System.out.println("Succeeded connecting to the Database!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return con;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!connection.isClosed()) {
            System.out.println("关闭连接");
            connection.close();
        }
        super.finalize();
    }
}
