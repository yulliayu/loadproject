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
		boolean falg=false;
		if (col==0){
			falg=false;
		} else {
			falg=true;
		}
		return falg;
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
	
	public int findColumn(String colName) {
		//System.out.println("find="+colName +":"+columnName.indexOf((Object)colName));
		int col = -1;
		for (int i=0; i<columnName.size(); i++){
			String name = (String)columnName.elementAt(i);
			//System.out.println("name = "+name);
			if (name.equals(colName.toUpperCase())){
				col=i;
				break;
			}			
		}
		return col;
	}

}
