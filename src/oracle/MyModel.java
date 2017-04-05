/*
 * JTable �� ���÷� ������ ���� ��Ʈ�ѷ�
 * 
 */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	
	Vector  columnName; // �÷��� ������ ���� ����
	Vector<Vector>  list; // ���ڵ带 ���� ������ ����
	
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
	
	// row, col �� ��ġ�� cell �� ���� �����ϰ� �Ѵ�.
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	public void setValueAt(Object value, int row, int col) {
		// ��, ȣ���� �����Ѵ�.
		Vector vec = list.get(row);
		vec.set(col, value);
		
		// �̰��� ����� ���̺��� ���濩�θ� üũ�ϰ� �ȴ�.
		this.fireTableDataChanged();
		//this.fireTableCellUpdated(row, col);
	}	

	public Object getValueAt(int row, int col) {
		Vector vec=list.get(row);
		// ��â�� Vector
		return vec.elementAt(col);
	}

}
