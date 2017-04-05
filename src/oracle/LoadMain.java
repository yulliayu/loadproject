package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

public class LoadMain extends JFrame implements ActionListener{
	JPanel  p_north;
	JButton  bt_open, bt_load, bt_del, bt_excel;
	JTable   table;
	JScrollPane  scroll;
	JTextField  t_path;
	JFileChooser  chooser;
	FileReader reader=null;
	BufferedReader  buffr=null;
	
	// 윈도우 창이 열리면 이미 접속을 확보해 놓자.
	Connection con;
	DBManager  manager=DBManager.getInstance();
	
	public LoadMain() {
		p_north = new JPanel();
		
		t_path = new JTextField(25);
		bt_open = new JButton("파일 열기");
		bt_load = new JButton("로드하기");
		bt_excel = new JButton("엑셀 로드");
		bt_del = new JButton("삭제하기");
		
		table = new JTable();
		scroll = new JScrollPane(table);
		
		chooser = new JFileChooser("C:/animal/");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		// 리스너 연결
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_del.addActionListener(this);
		bt_excel.addActionListener(this);

		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		// 윈도우와 리스너 연결
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// 데이터베이스 자원 해제
				manager.disConnection(con);
				
				// 프로세스 종료
				System.exit(0);
			}
		});
		
		setVisible(true);
		setSize(800, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		
		init();
		
	}
	
	public void init(){
		// Connection 얻기
		con = manager.getConnection();
	}
	
	// 파일 탐색기 띄우기
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		// 열기를 누르면 목적파일에 스트림을 생성하자.
		
		if (result == JFileChooser.APPROVE_OPTION){
			// 유저가 선택한 파일
			File file = chooser.getSelectedFile();			
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader=new FileReader(file);
				buffr = new BufferedReader(reader);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	// CSV --> Oracle 로 데이터 이전(migration)하기
	public void load(){
		// 버퍼스트림을 이용하여 csv 의 데이터를 한줄씩 읽어 들여 insert 시키자, 
		// 레코드가 없을때까지
		// while 문으로 돌리면 너무 빠르므로, 네트워크가 감당할 수 없기 때문에 일부러 지연시키면서...
		String data;
		StringBuffer  sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data = buffr.readLine();
				if(data==null) break;
				String[] value = data.split(","); /* , 는 특수문자로 인식하지 않아 \\ 두개 안 붙여도 된다.  */
				// seq 줄을 제외하고 insert 하겠다.
				if (!value[0].equals("seq")){
					//System.out.println(data);
			
					sb.append("insert into hospital (seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values ("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]
							        +"', '"+value[4]+"', "+value[5]+", '"+value[6]+"') ");
					System.out.println(sb.toString());
					
					pstmt = con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate();
					// 기존에 누적된 StringBuffer 데이터를 모두 지우기
					sb.delete(0, sb.length());
					
				} else {
					System.out.println("난 제외");
				}				
			}			
			JOptionPane.showMessageDialog(this, "로드 완료");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(pstmt !=null)
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}
	
	// 엑셀 파일 읽어서 db에 마이그레이션 하기.
	// javaSE 엑셀제어 라이브러리 있다? 없다.
	// open Source : 공개 소프트웨어
	// copyright (소프트웨어는 유료) <--> copyleft (소프트웨어는 무료화 되어야 한다.-아파치 단체)
	// POI 라이브러리! http://apache.org
	/*
	 * HSSWorkBook : 엑셀파일
	 * HSSFSheet : sheet
	 * HSSFRow : row
	 * HSSFCell : cell
	 */
	public void loadExcel(){
		
		int result = chooser.showOpenDialog(this);
		
		if (result == JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			FileInputStream fis=null;
			
			try {
				fis=new FileInputStream(file);
				
				HSSFWorkbook book=null;
				book = new HSSFWorkbook(fis);
				
				HSSFSheet sheet=null;
				sheet = book.getSheet("sheet1");				
				
				int total = sheet.getLastRowNum();
				DataFormatter  df=new DataFormatter();
				
				for (int a=1; a<=total;a++){
					
					HSSFRow  row= sheet.getRow(a);
					int columnCount = row.getLastCellNum();
					
					for (int i=0; i<columnCount; i++){
						HSSFCell  cell=row.getCell(i);
						// 자료형에 국한되지 않고 모두 String 처리
						String value=df.formatCellValue(cell);
						System.out.print(value);

					}
					System.out.println("");
				}
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	// 선택한 레코드 삭제
	public void delete(){
		
	}
	
	public void actionPerformed(ActionEvent e) {
		Object obj=e.getSource();
		if (obj==bt_open){
			open();
		} else if (obj==bt_load){
			load();
		} else if (obj==bt_excel){
			loadExcel();
		} else if (obj==bt_del){
			delete();
		}
	}

	public static void main(String[] args) {
		new LoadMain();

	}

}
