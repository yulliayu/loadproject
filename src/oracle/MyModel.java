/*
 * JTable 이 수시로 정보를 얻어가는 컨트롤러
 * 
 */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	
	Vector  columnName; // 컬럼의 제목을 담을 백터
	Vector<Vector>  list; // 레코드를 담을 이차원 백터
	
	public MyModel(Vector list, Vector columnName) {
		this.list = list;
		this.columnName = columnName;
	}
	
	public String getColumnName(int col) {
		return (String)columnName.elementAt(col);
	}

	public int getColumnCount() {
		return columnName.size();
	}

	public int getRowCount() {
		return list.size();
	}
	
	// row, col 에 위치한 cell 을 편집 가능하게 한다.
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	public void setValueAt(Object value, int row, int col) {
		// 층, 호수를 변경한다.
		Vector vec = list.get(row);
		vec.set(col, value);
		
		// 이것을 해줘야 테이블이 변경여부를 체크하게 된다.
		this.fireTableDataChanged();
		//this.fireTableCellUpdated(row, col);
	}	

	public Object getValueAt(int row, int col) {
		Vector vec=list.get(row);
		// 이창원 Vector
		return vec.elementAt(col);
	}

}
