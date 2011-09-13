package konkordans.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.TreeMap;

import util.Compressor;

public class HashFileWriter{

	private static final char[] ALPHABET = {' ','a','b','c','d','e',
											'f','g','h','i','j','k',
											'l','m','n','o','p','q',
											'r','s','t','u','v','w',
											'x','y','z','å','ä','ö'};
	public static final int HASHSIZE = 27000;
	
	private long[] hashIndex = new long[HASHSIZE];
	private String filePath;	
	
	public HashFileWriter(String filePath){
		this.filePath = filePath;
	}
		
	public void fillHashIndex(TreeMap<String, Long> words, long endOfFile){

		char[] chars = new char[3];
		int index = 0;
		int x2;
		int x;
		
		int len = ALPHABET.length;
		int lenP2 = (int) Math.pow(len,2);
		String key = null;
		
		for(int loop0 = 0;loop0<ALPHABET.length;loop0++){
			chars[0] = ALPHABET[loop0];
			x2 = loop0*lenP2;
			for(int loop1 = 0;loop1<ALPHABET.length;loop1++){
				chars[1] = ALPHABET[loop1];
				x = loop1*len;
				for(int loop2 = 0;loop2<ALPHABET.length;loop2++){
					chars[2] = ALPHABET[loop2];
					index = x2+x+loop2;

					key = new String(chars);
					
					if(words.containsKey(key)){
						hashIndex[index] = words.remove(key);
					}else {
						if(!words.isEmpty()){
							hashIndex[index] = words.get(words.firstKey());
						} else{
							hashIndex[index] = endOfFile;
						}
					}
				}
			}
		}
	}

	public void writeHashFile(){
		
		FileOutputStream stream = null;
		byte[] bytes = new byte[hashIndex.length*8];
		byte[] eightBytes;
		
		try{
			File f = new File(filePath);
			f.delete();
			f.createNewFile();
			stream = new FileOutputStream(f);
			
			for(int i = 0;i<hashIndex.length;i++){
				eightBytes = Compressor.compressLong(hashIndex[i]);
				for(int j=0;j<eightBytes.length;j++){
					bytes[8*i+j] = eightBytes[j];
				}
			}
			
			stream.write(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  finally{
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return filePath;
	}
	
	public static long getFromFile(String filePath, int index){
		
		RandomAccessFile reader = null;
		long l = -1;
		
		try{
			File f = new File(filePath);
			reader = new RandomAccessFile(f,"r");
			reader.seek(8*index);
			l = reader.readLong();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(reader!=null){
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return l;
	}

	public static int hash(String valueToHash){
		
		valueToHash = valueToHash.toLowerCase();
		int hashValue = 0;
		int pow = (int) Math.pow(ALPHABET.length,2);
		char c;
		int length = valueToHash.length()-1;
		
		for(int i = 0;i<3;i++){
			
			if(i>length){

				break;
			}
			
			c = valueToHash.charAt(i);
	
			switch(c){
				case ' ':
					break;
				case 'å':
					hashValue += 27*pow;
					break;
				case 'ä':
					hashValue += 28*pow;
					break;
				case 'ö':
					hashValue += 29*pow;
					break;
				default:
					if(c>=97&&c<=122){
						hashValue += (c-96)*pow;
					}else{
						throw new IllegalArgumentException("The characters in the string are outside of the used alphabet. Alphabet is: " + String.valueOf(ALPHABET));
					}
			}
			pow = pow / ALPHABET.length;
		}
		
		return hashValue;
	}
}
