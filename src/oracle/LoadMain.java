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
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

public class LoadMain extends JFrame implements ActionListener, TableModelListener{
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
	
	public LoadMain() {
		p_north = new JPanel();
		
		t_path = new JTextField(25);
		bt_open = new JButton("���� ����");
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
					System.out.println(sb.toString());
					
					pstmt = con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate();
					// ������ ������ StringBuffer �����͸� ��� �����
					sb.delete(0, sb.length());
					
				} else {
					System.out.println("�� ����");
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

			StringBuffer  sb = new StringBuffer();
			PreparedStatement pstmt=null;
			
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
					
					sb.append(" insert into hospital (seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values(");

					for (int i=0; i<columnCount; i++){
						HSSFCell  cell=row.getCell(i);
						int cellType = cell.getCellType();
						// �ڷ����� ���ѵ��� �ʰ� ��� String ó��
						String value=df.formatCellValue(cell);
						//System.out.print(value);
						if (cellType==HSSFCell.CELL_TYPE_NUMERIC){
							sb.append(value);
						} else {
							sb.append("'"+value+"'" );
						}
						if (i != (columnCount-1)){
							sb.append(", ");
						}

					}
					sb.append(") ");
					
					pstmt = con.prepareStatement(sb.toString());
					int pstmtResult = pstmt.executeUpdate();
					// ������ ������ StringBuffer �����͸� ��� �����
					System.out.println(sb.toString());
					sb.delete(0, sb.length());
					
					//System.out.println("");
				}
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
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
		Object obj = e.getSource();
		TableModel model = table.getModel();
		
		// e �� ���� �� row, col �� �����ߴ��� system out �Ѹ���, 
		//System.out.println("source="+e.getSource());
		if (obj == model){
			System.out.println("�� �ٲ��?");
			System.out.println("row="+table.getSelectedRow());
			System.out.println("column="+table.getSelectedColumn());
			int row = table.getSelectedRow();
			int col = table.getSelectedColumn();
			model.getValueAt(row, col);
			Object o=table.getValueAt(row, col);
			System.out.println(o);
			System.out.println("column_name="+table.getColumnName(col));
			
			
		}
		
		// update �ϱ�.
		//update hospital set �÷���=�� where seq = 
	}

	public static void main(String[] args) {
		new LoadMain();

	}

}
