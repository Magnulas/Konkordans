package konkordans.search;

//run from console: java -cp bin konkordans.search.Konkordans FILE WORD

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import konkordans.StringData;
import konkordans.database.DatabaseBuilder;
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
	private static final String SYS_SEARCH_TIMER_START = "It took ";
	private static final String SYS_SEARCH_TIMER_END = " milliseconds to search";
			
	private static final String ERR_ARG_COUNT = "You need to pass a path to the file you wish to search and a word to search for. Ex. 'myfile word'";
	private static final String ERR_INDEX_FILE_START = "Could not find index file for given file ";
	private static final String ERR_INDEX_FILE_END = ". Would you like to build the index file? (y/n)";
	
	private static final int DISPLAYSIZE = 40;

	/**
	 * The only public visible function. Takes two arguments, a path to a file 
	 * and a word to search for in this file. This requires that you have built
	 * an index of the file you're searching with help of DatabaseBuilder.
	 * 
	 * @param args First argument is the file to search, second argument is sword to search for.
	 * @throws IOException 
	 */
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
		File indexFile = new File(StringData.VAR_DATA_PATH + fileName + StringData.VAR_INDEX_SUFFIX);
		
		timer.stop();
		validateFile(filePath,indexFile);
		timer.cont();
		
		int ind = HashFileWriter.hash(searchWord);
		long filePointerStart = HashFileWriter.getFromFile(hashFilePath,ind);
		long filePointerEnd;
		
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
		
		System.out.println(SYS_SEARCH_TIMER_START + timer.getElapsedTime() + SYS_SEARCH_TIMER_END);
	}

	private static void validateFile(String filePath,File indexFile) throws IOException {
		if(!indexFile.exists()){
			System.out.print(ERR_INDEX_FILE_START + filePath + ERR_INDEX_FILE_END);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			String line = br.readLine();
			
			if(line.charAt(0) == 'y'){
				String[] args = new String[1];
				args[0] = filePath;
				DatabaseBuilder.main(args);
			} else{
				System.exit(0);
			}
		}
	}

	private static void print(String searchWord, String[] foundStrings) throws IOException{
		
		if(foundStrings==null){
			System.out.println(SYS_MULTIPLE_OCCURENCES_START + 0 + SYS_MULTIPLE_OCCURENCES_MIDDLE + searchWord + ".");
			return;
		}
		
		int length = foundStrings.length;
		boolean print = true;
		
		if(length>25){
			System.out.println(SYS_MULTIPLE_OCCURENCES_START + length + SYS_MULTIPLE_OCCURENCES_MIDDLE + searchWord + SYS_MULTIPLE_OCCURENCES_END);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = br.readLine();
			
			if(line.charAt(0)!='y'){
				print = false;
			}
		}
		
		if(print){
			for(int i = 0;i<foundStrings.length;i++){
				System.out.println((i+1) + ". " + foundStrings[i].replace('\n',' '));
			}
		}
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

	private static long[] searchIndexFile(long filePointerStart, long filePointerEnd, String searchWord, File file) throws IOException {
		
		int length = searchWord.length();
		
		String matchTo;
		
		if(length>3){
			matchTo = searchWord.substring(3,length);
		} else {
			matchTo = "";
		}
		
		RandomAccessFile reader = new RandomAccessFile(file,"r");

		filePointerStart = searchIndexFileHelperBinary(filePointerStart,filePointerEnd,reader,matchTo);
		
		String[] wordLongIntArray = searchIndexFileHelperRowByRow(filePointerStart,filePointerEnd,reader,matchTo);
		reader.close();
		
		if(wordLongIntArray == null){
			return null;
		} else{
			return readFilePointers(Long.valueOf(wordLongIntArray[1]),Integer.valueOf(wordLongIntArray[2]), file);
		}
	}

	private static String[] searchIndexFileHelperRowByRow(long filePointerStart, long filePointerEnd, RandomAccessFile reader, String matchTo) throws IOException {
		
		reader.seek(filePointerStart);
		
		String wordLongInt;
		String[] wordLongIntArray;
		
		//Row-by-row search
		while(true){
			wordLongInt = reader.readLine();
			
			if(wordLongInt == null){
				reader.close();
				return null;
			}
			wordLongIntArray = wordLongInt.split(" "); //A array of length 3 with a word, a long and a int
			
			if(wordLongIntArray[0].equals(matchTo)){
				return wordLongIntArray;
			}
			if(reader.getFilePointer()>=filePointerEnd){
				//no such word existed
				return null;
			}
		}
	}

	private static long searchIndexFileHelperBinary(long filePointerStart, long filePointerEnd, RandomAccessFile reader, String matchTo) throws IOException {
		
		long middleFP;
		char c;
		String wordLongInt;
		String[] wordLongIntArray;
		int compareValue;
		
		middleFP = (filePointerStart + filePointerEnd)/2;
		reader.seek(middleFP);

		while(filePointerEnd-filePointerStart>1000){
								
			c = (char)reader.readByte();
			
			if(c=='\n'){
					
				middleFP = reader.getFilePointer(); //In case we found the word on the row we're reading we can return this file pointer
				wordLongInt = reader.readLine();
				wordLongIntArray = wordLongInt.split(" "); //A array of length 3 with a word, a long and a int
				compareValue = matchTo.compareTo(wordLongIntArray[0]);
					
				if(compareValue==0){ //Found the word
//					returnValue = readFilePointers(Long.valueOf(wordLongIntArray[1]),Integer.valueOf(wordLongIntArray[2]), file);
//					reader.close();
					return middleFP; //Return to the start of the row since it has the word we were looking for
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
		
		return filePointerStart;
		
	}

	private static long[] readFilePointers(long filePointer, int numberOfLongstoRead, File file) throws IOException {
				
		if(file == null){
			return null;
		}
		
		long[] returnValue = new long[numberOfLongstoRead];
		
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		dis.skip(filePointer);
		for(int i = 0;i<numberOfLongstoRead;i++){
			returnValue[i] = dis.readLong();
		}
		
		dis.close();
		
		return returnValue;
		
	}
}
