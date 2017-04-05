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
	
	Connection con;  // 접속 후, 그 정보 담는 객체

	// new 막기 위함.
	/*
	 * 1. 드리이버 로드
	 * 2. 접속
	 * 3. 쿼리 실행
	 * 4. 반납
	 */
	private DBManager() {
		try {
			// 1. 드리이버 로드
			Class.forName(driver);
			
			// 2. 접속
			con = DriverManager.getConnection(url, user, password);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Instance 생성
	static public DBManager getInstance(){
		if (instance==null){
			instance = new DBManager();
		}
		return instance;
	}
	
	// 접속객체 반환
	public Connection getConnection(){
		return con;
	}
	
	// 접속 해제 4. 반납
	public void disConnection(Connection con){
		if (con!=null)
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	

}
