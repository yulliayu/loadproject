/*
 * ���ϰ� ���õ� �۾��� �����ִ� ���뼺�� �ִ� Ŭ������ �����Ѵ�.
 */
package util.file;

public class FileUtil {
	
	/* �Ѱ� ���� ��ο��� Ȯ���� ���ϱ� */
	public static String getExt(String path){
		int last = path.lastIndexOf(".");
		
		return path.substring(last+1, path.length());
	}

}
