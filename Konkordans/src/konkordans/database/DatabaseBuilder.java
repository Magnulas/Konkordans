package konkordans.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import konkordans.StringData;
import util.StringParser;
import util.Timer;

/**
 * A runnable class that takes one argument in the form
 * of a file path and builds one index file and one hash file
 * to make the the file passed as a argument searchable by the 
 * runnable class Konkordans
 * 
 * @author Magnus
 *
 */
public class DatabaseBuilder {
	
	/**
	 * System messages. passes information about the process
	 */
	private static final String SYS_BUILDNING_INDEX = "Building index for file: ";
	private static final String SYS_READING = "Reading ";
	private static final String SYS_READING_DONE = "Reading took: ";
	private static final String TXT_MILLISECONDS = " milliseconds.";
	private static final String SYS_WRITING = "Writing ";
	private static final String SYS_WRITING_DONE = "Writing took: ";
	
	/**
	 * Error mesages, informs the user of erros
	 */
	private static final String ERR_ARG_COUNT = "You need to pass a path to the file you wish to index.";
		
	/**
	 * The only visible and runnable function
	 * 
	 * @param args A file path
	 * @throws IOException If the file does not exist etc.
	 */
	public static void main(String args[]) throws IOException{
				
		String filePath = getArgs(args);
		String fileName = StringParser.parseFileName(filePath);
					
		System.out.println(SYS_BUILDNING_INDEX + filePath);		
				
		TreeMap<String, ArrayList<Long>> indexFileData = readSource(filePath);
		
		IndexFileWriter indexFileWriter = new IndexFileWriter(StringData.VAR_DATA_PATH + fileName + StringData.VAR_INDEX_SUFFIX, indexFileData);
		
		TreeMap<String, Long> words = writeIndex(indexFileWriter);	
		
		HashFileWriter hashFileWriter = new HashFileWriter(StringData.VAR_DATA_PATH + fileName + StringData.VAR_HASH_SUFFIX);
				
		hashFileWriter.fillHashIndex(words, indexFileWriter.length());
		
		writeHashFile(hashFileWriter);
		
		System.out.println("Done");
	}

	/**
	 * Writes the given hashfile to a file given by the hashfile.
	 * Prints time for the operation of writing it to file.
	 * 
	 * @param hashFileWriter The data to write
	 */
	private static void writeHashFile(HashFileWriter hashFileWriter) {
		Timer timer = new Timer();
		
		System.out.println(SYS_WRITING + hashFileWriter.getPath());
		
		timer.start();
		
		hashFileWriter.writeHashFile();
		
		long time = timer.getElapsedTime();
		System.out.println(SYS_WRITING_DONE + time + TXT_MILLISECONDS);		
	}

	/**
	 * Reads the file passed as an argument to the main method and
	 * creates an index of it in the primary memory.
	 * 
	 * @param source The file to read from
	 * @param indexFile The file to write to
	 * @param stream
	 * @throws IOException
	 */
	private static TreeMap<String, ArrayList<Long>> readSource(String source) throws IOException {
		Timer timer = new Timer();
		
		System.out.println(SYS_READING + source);
		
		timer.start();
		
		TreeMap<String, ArrayList<Long>> wordsAndFilepointers = Tokenizer.tokenize(new File(source));
				
		long time = timer.getElapsedTime();
		System.out.println(SYS_READING_DONE + time + TXT_MILLISECONDS);
		
		return wordsAndFilepointers;
	}

	/**
	 * Writes the indexfile to disk and prints time for the
	 * operation.
	 * 
	 * @param indexFileWriter The data to write
	 * @return A map over the first three characters in each word as key and a filepointer as the value that indicates where in the written indexfile the three word combination occures for the first time
	 * @throws IOException
	 */
	private static TreeMap<String, Long> writeIndex(IndexFileWriter indexFileWriter) throws IOException {
		
		Timer timer = new Timer();
		
		System.out.println(SYS_WRITING + indexFileWriter.getPath());
		
		timer.start();
		
		TreeMap<String, Long> hashIndex = indexFileWriter.writeIndex();
		
		long time = timer.getElapsedTime();
		System.out.println(SYS_WRITING_DONE + time + TXT_MILLISECONDS);
		
		return hashIndex;
	}

	/**
	 * The main method only has one argument so we intemperate all arguments as one
	 * 
	 * @param args The arguments to combine into one argument
	 * @return One argument that has been passed to the file
	 */
	private static String getArgs(String[] args) {
		if(args.length<1){
			System.out.println(ERR_ARG_COUNT);
			System.exit(0);
		}
				
		StringBuilder argMerge = new StringBuilder(args[0]);
		String filePath;
		
		for(int i = 1;i<args.length;i++){
			argMerge.append(" " + args[i]);
		}
			
		filePath = argMerge.toString();
		return filePath;
	}
}
