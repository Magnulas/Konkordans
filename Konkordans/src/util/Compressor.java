package util;

public class Compressor {
	
	
	public static byte[] compressLong(long elem) {
			
		byte[] chars = new byte[8];
		 
		for(int i = 0;i<chars.length;i++){
				chars[i] = (byte)(0xff & (elem >> (7-i)*8));
		}
		 
		return chars;
	}
	
	public static long uncompressLong(byte[] elem) {
			
		if(elem.length!=8){
			throw new IllegalArgumentException("Byte array has to be 8 in length");
		}
		
		
		long uncompressed = 0;
		 
		for(int i = 0;i<elem.length;i++){
			uncompressed = uncompressed + ((long)(0xff & elem[i]) << ((7-i)*8));
		}
		 
		return uncompressed;
	}
}
