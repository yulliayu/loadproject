package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;

public class LoadMain extends JFrame implements ActionListener, TableModelListener, Runnable {
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
	Vector<Vector> list;
	Vector  columnName;
	Thread  thread; // 엑셀 등록시 사용될 쓰레드. 왜? 데이터량이 너무 많을 경우, 네트워크 상태가 좋지 않을 경우
	                         // insert 가 while 문 속도를 못따락 나다. 따라서 안정성을 위해 일부러 시간지연을 일으켜 insert 시도할 거임.
	// 엑셀파일에 의해 생성된 쿼리 문을 쓰레드가 사용할 수 있는 상태로 저장해 놓자.
	StringBuffer  insertSql = new StringBuffer();
	String seq;
	
	public LoadMain() {
		p_north = new JPanel();
		
		t_path = new JTextField(25);
		bt_open = new JButton("CSV 파일 열기");
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
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t = (JTable)e.getSource();
				int row = t.getSelectedRow();
				int col = 0; // seq 는 첫번째 컬럼
				seq = (String)t.getValueAt(row, col);
				
			}
		});

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
			String ext = FileUtil.getExt(file.getName());
			if (!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV 만 넣어주세요.");
				return; // 더이상의 진행을 막는다.
			}
			/*
			if (file.getName().indexOf(".cvs")==-1){
				JOptionPane.showMessageDialog(this, "csv 파일을 선택하세요.");
				return;
			}
			*/
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
					//System.out.println(sb.toString());
					
					pstmt = con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate();
					// 기존에 누적된 StringBuffer 데이터를 모두 지우기
					sb.delete(0, sb.length());
					
				} else {
					//System.out.println("난 제외");
				}				
			}			
			JOptionPane.showMessageDialog(this, "마이그레이션 완료");
			
			// JTable 나오게 처리.
			getList();
			
			table.setModel(new MyModel(list, columnName));
			// 테이블 모델과 리스너와의 연결. table 이 아니라 model 에 줘야 한다.
			table.getModel().addTableModelListener(this);
			
			table.updateUI();
			
			
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
		System.out.println("load : "+table.getRowCount());
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
			StringBuffer   cols = new StringBuffer();
			StringBuffer   data = new StringBuffer();

			//StringBuffer  sb = new StringBuffer();
			PreparedStatement pstmt=null;
			
			try {
				fis=new FileInputStream(file);
				
				HSSFWorkbook book=null;
				book = new HSSFWorkbook(fis);
				
				HSSFSheet sheet=null;
				sheet = book.getSheet("sheet1");				
				DataFormatter  df=new DataFormatter();
				
				int total = sheet.getLastRowNum();
				/*-----------------------------------------------------
				 * 첫번째 row는 데이터가 아닌, 컬럼 정보이므로, 
				 * 이 정보들을 추출하여 insert into table(****) 에 넣자.
				 */
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());
				//System.out.println("이 파일의 첫번째 row 번호는 ="+firstRow);
				
				// Row 를 얻었으니, 컬럼을 분석하자.
				cols.delete(0, cols.length());
				for (int i=0; i<firstRow.getLastCellNum();i++){
					HSSFCell  cell=firstRow.getCell(i);
					// 첫번째 줄에 컬럼명이 들어 있어서, getStringCellValue 로 받아 온다.
					if (i < firstRow.getLastCellNum()-1){
						cols.append(cell.getStringCellValue()+",");
						//System.out.print(cell.getStringCellValue()+",");
					} else {
						cols.append(cell.getStringCellValue());
						//System.out.print(cell.getStringCellValue());
					}
				}				
				
				for (int a=1; a<=total;a++){
					
					HSSFRow  row= sheet.getRow(a);
					int columnCount = row.getLastCellNum();
					
					data.delete(0, data.length());
					for (int i=0; i<columnCount; i++){
						HSSFCell  cell=row.getCell(i);
						
						// 자료형에 국한되지 않고 모두 String 처리
						String value=df.formatCellValue(cell);
						
						if (cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							value = "'"+value + "'";
						}
						
						if (i<columnCount-1){
							data.append(value+",");
						} else {
							data.append(value);
						}		
						
					}
					
					//System.out.println("insert into hospital ("+cols.toString()+") values ("+data.toString()+") ");
					insertSql.append("insert into hospital ("+cols.toString()+") values ("+data.toString()+"); ");
					
					//pstmt = con.prepareStatement(sb.toString());
					//int pstmtResult = pstmt.executeUpdate();
					// 기존에 누적된 StringBuffer 데이터를 모두 지우기
					//System.out.println(sb.toString());
					//sb.delete(0, sb.length());
					
					//System.out.println("");
				}
				
				// 모든게 끝났으니, 편안하게 쓰레드에게 일 시키자
				// Runnable 인터페이스를 인수로 넣으면, Thread 의 즉 아래의 run 을 수행하는 것이 아니라
				// Runnable 인터페이스를 구현한자의 run() 수행하게 됨. 따라서 우리꺼 수행
				thread = new Thread(this);
				thread.start();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			//} catch (SQLException e) {
			//	e.printStackTrace();
			}
		}
		
	}
	
	// 모든 레코드 가져오기
	public void getList(){
		String sql = "select * from hospital order by seq asc ";
		
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			// column name 담아 놓기
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			columnName = new Vector();
			for (int i=0; i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector(); // 2차원 Vector
			
			while (rs.next()){ // 커서 한칸 내리기
				Vector vec = new Vector(); // 레코드 1 건 담기
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				
				list.add(vec);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if (pstmt!=null)
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		
	}
	
	// 선택한 레코드 삭제
	public void delete(){
		int result = JOptionPane.showConfirmDialog(LoadMain.this, seq + " 삭제할래요?");
		if (result == JOptionPane.OK_OPTION){
			int ans;
			String sql = "delete from hospital where seq="+seq;
			System.out.println(sql);
			PreparedStatement pstmt=null;
			try {
				pstmt = con.prepareStatement(sql);
				ans = pstmt.executeUpdate();
				if (ans !=0){
					System.out.println("갱신 전 : "+table.getRowCount() + ", row = "+table.getSelectedRow());
					JOptionPane.showMessageDialog(this, "삭제완료");
					//getList();
					table.updateUI();
					System.out.println("갱신 : "+table.getRowCount());
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (pstmt!=null)
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
			}
			
		}
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

	// 테이블 모델의 데이터값의 변경이 발생하면, 그 찰나를 감지하는 리스너
	public void tableChanged(TableModelEvent e) {
		int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		
		String column = (String)columnName.elementAt(col);
		// 반환한 값
		String value = (String)table.getValueAt(row, col);
		//System.out.println(row);
		String seq = (String)table.getValueAt(0, col);
		String sql = "update hospital set "+column+"="+value
				        + " where seq = "+seq;
		System.out.println(sql);

		PreparedStatement  pstmt=null;
		try {
			pstmt = con.prepareStatement(sql);
			int result = pstmt.executeUpdate();
			if (result != 0){
				JOptionPane.showMessageDialog(this, "Update 완료");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			if (pstmt!=null)
				try {
					pstmt.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
		}
		
		
		/*
		Object obj = e.getSource();
		TableModel model = table.getModel();
		
		// e 를 통해 몇 row, col 을 수정했는지 system out 뿌리고, 
		//System.out.println("source="+e.getSource());
		StringBuffer sb=new StringBuffer();
		
		if (obj == model){
			//System.out.println("나 바꿨어?");
			//System.out.println("row="+table.getSelectedRow());
			//System.out.println("column="+table.getSelectedColumn());

			int row = table.getSelectedRow();
			int col = table.getSelectedColumn();
			
			
			
			String colName = table.getColumnName(col);
			
			String value=(String)table.getValueAt(row, col);
			
			System.out.println(value);
			System.out.println("column_name="+table.getColumnName(col));
			
			sb.append(" update hospital set "+colName +"='" + value+"' ");
			sb.append(" where seq = "+(String)table.getValueAt(row, 0));
			//int i=model.findColumn(colName);
		   
		   
			System.out.println(sb.toString());

		}
		
		// update 하기.
		//update hospital set 컬럼명=값 where seq = 

		 */
		//int colnum=((AbstractTableModel)(table.getModel())).findColumn(columnName);
		//int seq1=((MyModel)(table.getModel())).findColumn("seq");
		//System.out.println("seq1="+seq1);
	}

	public void run() {
		// insertSql 에 insert 문이 몇개인지 알아보자
		String[] str=insertSql.toString().split(";");
		System.out.println("insert 문 수 = "+str.length);
		PreparedStatement  pstmt=null;
		for (int i=0; i<str.length-1; i++){
			System.out.println("i = " + i + " : "+str[i]);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				pstmt = con.prepareStatement(str[i]);
				int result = pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		// 기존에 사용했던 StringBuffer 비우기
		insertSql.delete(0, insertSql.length());
		if (pstmt!=null)
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
		// 모든 insert 가 종료되면 JTable UI 갱신
		JOptionPane.showMessageDialog(this, "Insert 완료");
	}

	public static void main(String[] args) {
		new LoadMain();

	}

}
