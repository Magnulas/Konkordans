package konkordans.database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.TreeMap;

public class Tokenizer {

	private final static int READLENGTH = 1024;
	
	/**
	 * Tokenize a file and get the words as keys and a list with indexes 
	 * of all the occurences of that word.
	 * @param file File to tokenize
	 * @return A treemap with the words in the file and where they occure in tyhe file.
	 * @throws IOException
	 */
	public static TreeMap<String, ArrayList<Long>> tokenize(File file) throws IOException{
		
		RandomAccessFile stream = new RandomAccessFile(file, "r");
		TreeMap<String, ArrayList<Long>> retValue = new TreeMap<String, ArrayList<Long>>();;
		byte[] words = new byte[READLENGTH];
		int bytesRead = 0;
		long newFilePointer = 0;
		
		while(true){
						
			bytesRead = stream.read(words);
			if(bytesRead == -1){
				break;
			}
								
			newFilePointer = insertToTree(retValue,words, bytesRead, stream.getFilePointer()-bytesRead);
			stream.seek(newFilePointer);

		}
		
		stream.close();
		
		return retValue;
	}
	
	/**
	 * Proccesses the read data and inserts found words in the tree together with the filepointers
	 * 
	 * @param wordsAndIndex The tree to insert word and filepointers to
	 * @param words The read data
	 * @param length Length of read data, could be shorter than the array words
	 * @param filePointer A filepointer in the file that marks where the array of read data started
	 * @return A filepointer that indicates how many of the bytes that has been processed. Not all data is proccessed if the last sequence of characterts can't be determined to be a word. The returned filepointer indicates where in the file you are and should be set as the readers new filepointer unless you want to risk missing words.
	 */
	private static long insertToTree(TreeMap<String, ArrayList<Long>> wordsAndIndex,byte[] words, int length, long filePointer) {
		
		StringBuilder word = new StringBuilder();
		short wordOffset = 0;
		char cha = 0;
		
		for(int i = 0;i<length;i++){
				
			if((words[i]>='a'&&words[i]<='z')||(words[i]>='A'&&words[i]<='Z')|| //a-zA-Z
					(words[i]>=-32&&words[i]<=-17)||(words[i]>=-15&&words[i]<=-10)||(words[i]>=-8&&words[i]<=-3)||(words[i]==-1)|| //Special chars for proccessSpecialChar, lower case 
					(words[i]>=-64&&words[i]<=-49)||(words[i]>=-47&&words[i]<=-42)||(words[i]>=-40&&words[i]<=-35)){ //Special chars for proccessSpecialChar, upper case 
				
				if(words[i]<0){
					cha = proccessSpecialChar(words[i]);
				} else{
					cha = (char)words[i];
				}
				
				word.append(cha);
				
			} else{
				String mapWord = word.toString();
				
				if(mapWord.length()>0){
					
				mapWord = mapWord.toLowerCase();
					
				if(!wordsAndIndex.containsKey(mapWord)){
					wordsAndIndex.put(mapWord, new ArrayList<Long>(2));
				}
				wordsAndIndex.get(mapWord).add(filePointer);
				wordOffset = (short)word.length();
				filePointer = filePointer + wordOffset;
				word.delete(0,wordOffset);
				}
				
				filePointer++;
			}
		}
		
		return filePointer;
	}

	private static char proccessSpecialChar(byte b) {
		
		char ch = (char) (0x000000FF & b);
		
		if(ch>=224&&ch<=227||ch>=192&&ch<=195){
			return 'a';
		}
		
		if(ch>=232&&ch<=235||ch>=200&&ch<=203){
			return 'e';
		}
		
		if(ch>=236&&ch<=239||ch>=204&&ch<=207){
			return 'i';
		}
		
		if(ch>=242&&ch<=245||ch>=210&&ch<=213){
			return 'o';
		}
		
		if(ch>=249&&ch<=252||ch>=217&&ch<=220){
			return 'u';
		}
		
		if(ch==230||ch==198){
			return 'ä';
		}
		
		if(ch==231||ch==199){
			return 'c';
		}
		
		if(ch==241||ch==209){
			return 'ä';
		}
		
		if(ch==248||ch==216){
			return 'ö';
		}
		
		if(ch==253||ch==255||ch==221){
			return 'y';
		}
		
		return ch;
	}
}
