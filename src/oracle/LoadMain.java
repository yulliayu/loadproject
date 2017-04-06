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
	
	// ������ â�� ������ �̹� ������ Ȯ���� ����.
	Connection con;
	DBManager  manager=DBManager.getInstance();
	Vector<Vector> list;
	Vector  columnName;
	Thread  thread; // ���� ��Ͻ� ���� ������. ��? �����ͷ��� �ʹ� ���� ���, ��Ʈ��ũ ���°� ���� ���� ���
	                         // insert �� while �� �ӵ��� ������ ����. ���� �������� ���� �Ϻη� �ð������� ������ insert �õ��� ����.
	// �������Ͽ� ���� ������ ���� ���� �����尡 ����� �� �ִ� ���·� ������ ����.
	StringBuffer  insertSql = new StringBuffer();
	String seq;
	
	public LoadMain() {
		p_north = new JPanel();
		
		t_path = new JTextField(25);
		bt_open = new JButton("CSV ���� ����");
		bt_load = new JButton("�ε��ϱ�");
		bt_excel = new JButton("���� �ε�");
		bt_del = new JButton("�����ϱ�");
		
		table = new JTable();
		scroll = new JScrollPane(table);
		
		chooser = new JFileChooser("C:/animal/");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		// ������ ����
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_del.addActionListener(this);
		bt_excel.addActionListener(this);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t = (JTable)e.getSource();
				int row = t.getSelectedRow();
				int col = 0; // seq �� ù��° �÷�
				seq = (String)t.getValueAt(row, col);
				
			}
		});

		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		// ������� ������ ����
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// �����ͺ��̽� �ڿ� ����
				manager.disConnection(con);
				
				// ���μ��� ����
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
		// Connection ���
		con = manager.getConnection();
	}
	
	// ���� Ž���� ����
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		// ���⸦ ������ �������Ͽ� ��Ʈ���� ��������.
		
		if (result == JFileChooser.APPROVE_OPTION){
			// ������ ������ ����
			File file = chooser.getSelectedFile();		
			String ext = FileUtil.getExt(file.getName());
			if (!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV �� �־��ּ���.");
				return; // ���̻��� ������ ���´�.
			}
			/*
			if (file.getName().indexOf(".cvs")==-1){
				JOptionPane.showMessageDialog(this, "csv ������ �����ϼ���.");
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
	
	// CSV --> Oracle �� ������ ����(migration)�ϱ�
	public void load(){
		// ���۽�Ʈ���� �̿��Ͽ� csv �� �����͸� ���پ� �о� �鿩 insert ��Ű��, 
		// ���ڵ尡 ����������
		// while ������ ������ �ʹ� �����Ƿ�, ��Ʈ��ũ�� ������ �� ���� ������ �Ϻη� ������Ű�鼭...
		String data;
		StringBuffer  sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data = buffr.readLine();
				if(data==null) break;
				String[] value = data.split(","); /* , �� Ư�����ڷ� �ν����� �ʾ� \\ �ΰ� �� �ٿ��� �ȴ�.  */
				// seq ���� �����ϰ� insert �ϰڴ�.
				if (!value[0].equals("seq")){
					//System.out.println(data);
			
					sb.append("insert into hospital (seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values ("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]
							        +"', '"+value[4]+"', "+value[5]+", '"+value[6]+"') ");
					//System.out.println(sb.toString());
					
					pstmt = con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate();
					// ������ ������ StringBuffer �����͸� ��� �����
					sb.delete(0, sb.length());
					
				} else {
					//System.out.println("�� ����");
				}				
			}			
			JOptionPane.showMessageDialog(this, "���̱׷��̼� �Ϸ�");
			
			// JTable ������ ó��.
			getList();
			
			table.setModel(new MyModel(list, columnName));
			// ���̺� �𵨰� �����ʿ��� ����. table �� �ƴ϶� model �� ��� �Ѵ�.
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
	
	// ���� ���� �о db�� ���̱׷��̼� �ϱ�.
	// javaSE �������� ���̺귯�� �ִ�? ����.
	// open Source : ���� ����Ʈ����
	// copyright (����Ʈ����� ����) <--> copyleft (����Ʈ����� ����ȭ �Ǿ�� �Ѵ�.-����ġ ��ü)
	// POI ���̺귯��! http://apache.org
	/*
	 * HSSWorkBook : ��������
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
				 * ù��° row�� �����Ͱ� �ƴ�, �÷� �����̹Ƿ�, 
				 * �� �������� �����Ͽ� insert into table(****) �� ����.
				 */
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());
				//System.out.println("�� ������ ù��° row ��ȣ�� ="+firstRow);
				
				// Row �� �������, �÷��� �м�����.
				cols.delete(0, cols.length());
				for (int i=0; i<firstRow.getLastCellNum();i++){
					HSSFCell  cell=firstRow.getCell(i);
					// ù��° �ٿ� �÷����� ��� �־, getStringCellValue �� �޾� �´�.
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
						
						// �ڷ����� ���ѵ��� �ʰ� ��� String ó��
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
					// ������ ������ StringBuffer �����͸� ��� �����
					//System.out.println(sb.toString());
					//sb.delete(0, sb.length());
					
					//System.out.println("");
				}
				
				// ���� ��������, ����ϰ� �����忡�� �� ��Ű��
				// Runnable �������̽��� �μ��� ������, Thread �� �� �Ʒ��� run �� �����ϴ� ���� �ƴ϶�
				// Runnable �������̽��� ���������� run() �����ϰ� ��. ���� �츮�� ����
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
	
	// ��� ���ڵ� ��������
	public void getList(){
		String sql = "select * from hospital order by seq asc ";
		
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			// column name ��� ����
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			columnName = new Vector();
			for (int i=0; i<count;i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list = new Vector(); // 2���� Vector
			
			while (rs.next()){ // Ŀ�� ��ĭ ������
				Vector vec = new Vector(); // ���ڵ� 1 �� ���
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
	
	// ������ ���ڵ� ����
	public void delete(){
		int result = JOptionPane.showConfirmDialog(LoadMain.this, seq + " �����ҷ���?");
		if (result == JOptionPane.OK_OPTION){
			int ans;
			String sql = "delete from hospital where seq="+seq;
			System.out.println(sql);
			PreparedStatement pstmt=null;
			try {
				pstmt = con.prepareStatement(sql);
				ans = pstmt.executeUpdate();
				if (ans !=0){
					System.out.println("���� �� : "+table.getRowCount() + ", row = "+table.getSelectedRow());
					JOptionPane.showMessageDialog(this, "�����Ϸ�");
					//getList();
					table.updateUI();
					System.out.println("���� : "+table.getRowCount());
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

	// ���̺� ���� �����Ͱ��� ������ �߻��ϸ�, �� ������ �����ϴ� ������
	public void tableChanged(TableModelEvent e) {
		int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		
		String column = (String)columnName.elementAt(col);
		// ��ȯ�� ��
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
				JOptionPane.showMessageDialog(this, "Update �Ϸ�");
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
		
		// e �� ���� �� row, col �� �����ߴ��� system out �Ѹ���, 
		//System.out.println("source="+e.getSource());
		StringBuffer sb=new StringBuffer();
		
		if (obj == model){
			//System.out.println("�� �ٲ��?");
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
		
		// update �ϱ�.
		//update hospital set �÷���=�� where seq = 

		 */
		//int colnum=((AbstractTableModel)(table.getModel())).findColumn(columnName);
		//int seq1=((MyModel)(table.getModel())).findColumn("seq");
		//System.out.println("seq1="+seq1);
	}

	public void run() {
		// insertSql �� insert ���� ����� �˾ƺ���
		String[] str=insertSql.toString().split(";");
		System.out.println("insert �� �� = "+str.length);
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
		
		// ������ ����ߴ� StringBuffer ����
		insertSql.delete(0, insertSql.length());
		if (pstmt!=null)
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
		// ��� insert �� ����Ǹ� JTable UI ����
		JOptionPane.showMessageDialog(this, "Insert �Ϸ�");
	}

	public static void main(String[] args) {
		new LoadMain();

	}

}
