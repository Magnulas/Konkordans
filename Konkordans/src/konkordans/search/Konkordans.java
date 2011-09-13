package konkordans.search;

//run from console: java -cp bin konkordans.search.Konkordans FILE WORD

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;

import konkordans.StringData;
import konkordans.database.HashFileWriter;
import util.StringParser;
import util.Timer;

public class Konkordans {

	private static final String SYS_MULTIPLE_OCCURENCES_START = "There exists ";
	private static final String SYS_MULTIPLE_OCCURENCES_MIDDLE = " occurences of the word ";
	private static final String SYS_MULTIPLE_OCCURENCES_END = ". Do you wish to print all of them? (y/n)";
	
	private static final String SYS_SEARCH_START = "Searching ";
	private static final String SYS_SEARCH_MIDDLE = " for the word ";
	private static final String SYS_SEARCH_END = ".";
			
	private static final String ERR_ARG_COUNT = "You need to pass a path to the file you wish to search and a word to search for. Ex. 'myfile word'";
	
	private static final int DISPLAYSIZE = 40;
	
	public static void main(String args[]) throws IOException{
		
		if(args.length!=2){
			System.out.println(ERR_ARG_COUNT);
			return;
		}
		
		Timer timer = new Timer();
		
		timer.start();
		
		String filePath = args[0];
		String fileName = StringParser.parseFileName(filePath);
		String searchWord = args[1].toLowerCase(); 
		
		System.out.println(SYS_SEARCH_START + filePath + SYS_SEARCH_MIDDLE + searchWord + SYS_SEARCH_END);
		
		String hashFilePath = StringData.VAR_DATA_PATH + fileName + StringData.VAR_HASH_SUFFIX;
		
		int ind = HashFileWriter.hash(searchWord);
		long filePointerStart = HashFileWriter.getFromFile(hashFilePath,ind);
		long filePointerEnd;
		
		File indexFile = new File(StringData.VAR_DATA_PATH + fileName + StringData.VAR_INDEX_SUFFIX);
		
		ind = ind+1;
		
		if(ind>=HashFileWriter.HASHSIZE){
			filePointerEnd = indexFile.length();
		} else{
			filePointerEnd = HashFileWriter.getFromFile(hashFilePath,ind);
		}
		
		long[] filePointers = searchIndexFile(filePointerStart,filePointerEnd,searchWord,indexFile);
		
		String[] foundStrings = searchFile(filePath, filePointers);
		
		timer.stop();
		print(searchWord, foundStrings);
		timer.cont();
		
		System.out.println("It took " + timer.getElapsedTime() + " milliseconds to search");
	}

	private static void print(String searchWord, String[] foundStrings) throws IOException {
		
		if(foundStrings==null){
			System.out.println(SYS_MULTIPLE_OCCURENCES_START + 0 + SYS_MULTIPLE_OCCURENCES_MIDDLE + searchWord + ".");
			return;
		}
		
		int length = foundStrings.length;
		boolean print = true;
		
		if(length>25){
			System.out.println(SYS_MULTIPLE_OCCURENCES_START + length + SYS_MULTIPLE_OCCURENCES_MIDDLE + searchWord + SYS_MULTIPLE_OCCURENCES_END);
			int byteRead = System.in.read();
			if(byteRead!='y'){
				print = false;
			}
		}
		
		if(print){
			for(int i = 0;i<foundStrings.length;i++){
				
				System.out.println(replaceChar((i+1) + ". " + foundStrings[i],'\n',' '));
			}
		}
	}
	
	private static String replaceChar(String elem, char replace, char replaceWith) {
		
		byte[] chars = elem.getBytes();
		for(int i = 0;i<chars.length;i++){
			if(chars[i]==(byte)replace){
				chars[i] = (byte)replaceWith;
			}
		}
		
		return new String(chars);
	}

	private static String[] searchFile(String filePath, long[] filePointers) throws IOException {
		
		if(filePointers==null){
			return null;
		}
		
		File f = new File(filePath);
		long startPointer;
		RandomAccessFile reader = new RandomAccessFile(f, "r");
		
		String[] foundData = new String[filePointers.length];
		long endPointer;
		long fileLength = f.length()-1;
		byte[] readData;
		
		int twoTimesDisplaySize = DISPLAYSIZE*2;
		
		if(fileLength>twoTimesDisplaySize){
			readData = new byte[twoTimesDisplaySize];
		}else{
			//for small files
			readData = new byte[(int) (fileLength+1)];
		}
		
		for(int i = 0;i<filePointers.length;i++){
			startPointer = filePointers[i] - DISPLAYSIZE;
			endPointer = startPointer+twoTimesDisplaySize;
			
			if(endPointer>fileLength){
				startPointer = startPointer - (endPointer-fileLength);
			}
			if(startPointer < 0){
				 startPointer = 0;
			}
			
			reader.seek(startPointer);
			reader.read(readData);
			foundData[i] = new String(readData, "ISO-8859-1");
		}
		
		return foundData;
	}

	/**
	 * 
	 * @param filePointerStart
	 * @param filePointerEnd
	 * @param searchWord
	 * @return Return a list of file pointers to the indexed file
	 * @throws IOException 
	 */
	private static long[] searchIndexFile(long filePointerStart, long filePointerEnd, String searchWord, File file) throws IOException {
		
		int length = searchWord.length();
		long[] returnValue = null;
		
		String subString;
		
		if(length>3){

			subString = searchWord.substring(3,length);
		} else {
			subString = "";
		}
				
		returnValue = binarySearchIndex(filePointerStart,filePointerEnd,subString,file);
		
		return returnValue;
		
	}

	private static long[] binarySearchIndex(long filePointerStart, long filePointerEnd, String matchTo, File file) throws IOException {
		
		RandomAccessFile reader = new RandomAccessFile(file,"r");
		long[] returnValue = null;
		long middleFP;
		char c;
		String wordLongInt;
		String[] wordLongIntArray;
		int compareValue;
		
			
		middleFP = (filePointerStart + filePointerEnd)/2;
		reader.seek(middleFP);

		//BINÄRSÖKNING
		while(filePointerEnd-filePointerStart>1000){
								
			c = (char)reader.readByte();
			
			if(c=='\n'){
					
				wordLongInt = reader.readLine();
				wordLongIntArray = wordLongInt.split(" "); //A array of length 3 with a word, a long and a int
				compareValue = matchTo.compareTo(wordLongIntArray[0]);
					
				if(compareValue==0){ //Found the word
					returnValue = readFilePointers(Long.valueOf(wordLongIntArray[1]),Integer.valueOf(wordLongIntArray[2]), file);
					reader.close();
					return returnValue;
				}
					
				if(compareValue<0){  //Word is in first half of interval
					filePointerEnd = reader.getFilePointer();
				} else{ //Word is in second half of interval
					filePointerStart = reader.getFilePointer();	
				}
					
				middleFP = (filePointerStart + filePointerEnd)/2;
				reader.seek(middleFP);
			}
		}
		
		reader.seek(filePointerStart);
		
		while(true){
			wordLongInt = reader.readLine();
			
			if(wordLongInt == null){
				return null;
			}
			wordLongIntArray = wordLongInt.split(" "); //A array of length 3 with a word, a long and a int
			
			if(wordLongIntArray[0].equals(matchTo)){
				returnValue = readFilePointers(Long.valueOf(wordLongIntArray[1]),Integer.valueOf(wordLongIntArray[2]), file);
				reader.close();
				return returnValue;
			}
			if(reader.getFilePointer()>=filePointerEnd){
				//no such word existed
				reader.close();
				return null;
			}
		}
	}

	private static long[] readFilePointers(long filePointer, int numberOfLongstoRead, File file) throws IOException {
		
		long[] returnValue = new long[numberOfLongstoRead];
		
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		dis.skip(filePointer);
		for(int i = 0;i<numberOfLongstoRead;i++){
			returnValue[i] = dis.readLong();
		}
		
		dis.close();
		
		return returnValue;
		
	}

	public static HashFileWriter readHashFile(String  filePath){
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try{
			fis = new FileInputStream(filePath);
			in = new ObjectInputStream(fis);
			return (HashFileWriter)in.readObject();
		}catch(IOException ex){
			ex.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		} finally{
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
