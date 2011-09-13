package util;

public final class StringParser {

	public static String parseFileName(String filePath) {
		int index;
		String fileName;
		index = filePath.lastIndexOf('/')+1;
			
		if(index==0){
			index = filePath.lastIndexOf('\\') +1;
		}
						
		fileName = filePath.substring(index, filePath.length());
		return fileName;
	}
}
