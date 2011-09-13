package konkordans.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import util.Compressor;

public class IndexFileWriter {
	
//	private static final int READLENGTH = 1024;
	
	private TreeMap<String, ArrayList<Long>> wordsAndIndex;
	private String filePath;
	
	public IndexFileWriter(String filePath, TreeMap<String, ArrayList<Long>> wordsAndIndex){
		
		this.filePath = filePath;
		this.wordsAndIndex = wordsAndIndex;
	}
	
	/*
	public void fillIndex(RandomAccessFile stream) throws IOException{
	
		byte[] words = new byte[READLENGTH];
		int bytesRead = 0;
		long newFilePointer = 0;
		
		while(true){
						
			bytesRead = stream.read(words);
			if(bytesRead == -1){
				break;
			}
								
			newFilePointer = insertToTree(words, bytesRead, stream.getFilePointer()-bytesRead);
			stream.seek(newFilePointer);

		}		
	}
	
	private long insertToTree(byte[] words, int length, long filePointer) {
			
		StringBuilder word = new StringBuilder();
		short wordOffset = 0;
		char cha = 0;
		
		for(int i = 0;i<length;i++){
				
		if(!(words[i]>='a'&&words[i]<='z')&&!(words[i]>='A'&&words[i]<='Z')&&
			!(words[i]>=-32&&words[i]<=-17)&&!(words[i]>=-15&&words[i]<=-10)&&!(words[i]>=-8&&words[i]<=-3)&&!(words[i]==-1)&&
			!(words[i]>=-64&&words[i]<=-49)&&!(words[i]>=-47&&words[i]<=-42)&&!(words[i]>=-40&&words[i]<=-35)){ //TODO GÖR DET LITE SNYGGT VA!?
				
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
				
			} else{
				
				if(words[i]<0){
					cha = proccessSpecialChar(words[i]);
				} else{
					cha = (char)words[i];
				}
				
				word.append(cha);
			}
		}
		
		return filePointer;
	}
	
	private char proccessSpecialChar(byte b) {
		
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

	*/
	public TreeMap<String, Long> writeIndex(){
		
		Set<String> keySet = wordsAndIndex.keySet();
		TreeMap<String, Long> hashIndex = new TreeMap<String, Long>();
		
		OutputStreamWriter writer = null;
		FileOutputStream stream = null;
		int length = 0;
		char[] chars = new char[3];
		String key;
		int k;
		long listFilePointer = 0;
		long textFilePointer = 0;
		byte[] bytes;
		byte[] eigthBytes;
		
		try{
			
			File f = new File(filePath);
			f.delete();
			f.createNewFile();
			
			stream = new FileOutputStream(f);
			writer = new OutputStreamWriter(stream,"ISO-8859-1");
			Iterator<String> iter = keySet.iterator();
			String elem;
			ArrayList<Long> list;
			String subElem;
			String listFilePointerString;
			String listSizeString;
	
			while(iter.hasNext()){
				elem = iter.next();
				list = wordsAndIndex.get(elem);
				bytes = new byte[list.size()*8];
				for(int i = 0;i<list.size();i++){

					eigthBytes = Compressor.compressLong(list.get(i));
					for(int j = 0;j<8;j++){
						bytes[8*i+j] = eigthBytes[j];
					}
					textFilePointer += 8;
				}
				
				stream.write(bytes);
			}
						
			iter = keySet.iterator();
				
			while(iter.hasNext()){
				k = 0;
				elem = iter.next();
				list = wordsAndIndex.get(elem);			
				
				length = elem.length();
				
				for(int i = 0;i<chars.length;i++){
					if(length>i){
						chars[i] = elem.charAt(i);
						k++;
					} else{
						chars[i] = ' ';
					}
				}
				
				key = new String(chars);
				
				if(!hashIndex.containsKey(key)){
					hashIndex.put(key, textFilePointer);
				}
				
				subElem = elem.substring(k);
				listFilePointerString = String.valueOf(listFilePointer);
				listSizeString = String.valueOf(list.size());
				
				writer.write(subElem);
				writer.write(' ');
				writer.write(listFilePointerString);
				writer.write(' ');
				writer.write(listSizeString);
				writer.write('\n');;
				
				listFilePointer += list.size()*8;
				textFilePointer += 3 + 
					subElem.length() + 
					listFilePointerString.length() + 
					listSizeString.length();
			}
			
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			if(writer!=null){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return hashIndex;
	}
	
	public String getPath(){
		return filePath;
	}
	
	public long length(){
		File f = new File(filePath);
		return f.length();
	}
}