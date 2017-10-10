package hive;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


public class HiveJDBCDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		
		String sql = "select * from student4";
		try {
			//获取连接
			conn = JDBCUtils.getConnection();
			System.out.println("1");
			//创建运行环境
			st = conn.createStatement();
			System.out.println("2");
			//运行HQL
			rs = st.executeQuery(sql);
			System.out.println("3");
			//处理数据
			while(rs.next()){
				int id = rs.getInt("id");
				String name = rs.getString("name");
				int age = rs.getInt("age");
				System.out.println(id+"\t"+name);
			}
			System.out.println("4");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			//JDBCUtils.release(conn, st, rs);
		}
	}

}








