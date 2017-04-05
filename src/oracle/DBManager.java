package oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.management.InstanceAlreadyExistsException;

public class DBManager {
	
	static private DBManager instance;
	
	private String driver="oracle.jdbc.driver.OracleDriver";
	private String url="jdbc:oracle:thin:@localhost:1521:XE";
	private String user="batman";
	private String password="1234";
	
	Connection con;  // ���� ��, �� ���� ��� ��ü

	// new ���� ����.
	/*
	 * 1. �帮�̹� �ε�
	 * 2. ����
	 * 3. ���� ����
	 * 4. �ݳ�
	 */
	private DBManager() {
		try {
			// 1. �帮�̹� �ε�
			Class.forName(driver);
			
			// 2. ����
			con = DriverManager.getConnection(url, user, password);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Instance ����
	static public DBManager getInstance(){
		if (instance==null){
			instance = new DBManager();
		}
		return instance;
	}
	
	// ���Ӱ�ü ��ȯ
	public Connection getConnection(){
		return con;
	}
	
	// ���� ���� 4. �ݳ�
	public void disConnection(Connection con){
		if (con!=null)
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	

}
