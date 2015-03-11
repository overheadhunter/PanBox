/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.core.deprecated;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.panbox.PanboxConstants;
import org.panbox.core.crypto.EncodingHelper;
import org.panbox.core.crypto.EncodingType;

public class MainObfuscationTest {

	public static void main(String[] args) {

//		String str = "Hello there. How are you? Have a nice day.";
//		String str = "HelloThisIs_a_Fancy.Filename That is realy long-and contains-Some_special characters so hopefully it suits for a name on a cloud storage provider.test12345-BlaBlubbMaximumLeng";
		
		String str = "DiesisteinSehrLanger DateiName zum TestderObfuskierung1234.txt";
		
//		String str = "DateinameÄmitÖSonderzeichen";
		
		try {
			// Generate key
//			KeyGenerator kgen = KeyGenerator.getInstance("AES");
//			kgen.init(128);
//			SecretKey aesKey = kgen.generateKey();
			
//			byte[] key = aesKey.getEncoded();			
//			System.out.print("Key:\t\t");
//			printByteArray(key);
//			System.out.println();
			
			byte[] fixedAesKey = new byte[16];
			
			for(int i=0; i<fixedAesKey.length; i++)
			{
				fixedAesKey[i] = (byte)i;
			}
			
			System.out.print("Key:\t\t");
			printByteArray(fixedAesKey);
			System.out.println();
			
			//use key as key and iv
			String encryptedFileName = encryptFileName(str, fixedAesKey, fixedAesKey, false);			
			decryptFileName(encryptedFileName, fixedAesKey, fixedAesKey, false);
			
			//test
//			System.out.println("Test: ");
//			decryptFileName("9WNVAnQyCgjfqYcFxO53hab1PmdtjkjQgvRFnRI2szUvYB0JSEjYSjdtB272IpnF", fixedAesKey, fixedAesKey);
			
			System.out.println("\n\n");
			
			//clean test directories
			File dir = new File("testdirOb");
			for (File f : dir.listFiles())
			{
				f.delete();
			}
			
			dir = new File("testdirResult");
			for (File f : dir.listFiles())
			{
				f.delete();
			}
			
			obfuscateFileNamesInDir("testdir", fixedAesKey, fixedAesKey, false);			
			deobfuscateFileNamesInDir("testdirOb", fixedAesKey, fixedAesKey, false);

		}
		catch(Exception e)
		{
			System.out.println(e);
			e.printStackTrace();
		}


	}
	
	public static void deobfuscateFileNamesInDir(String dir, byte[] key, byte[] iv, boolean zip) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException, DataFormatException
	{
		File directory = new File(dir);
		
		for (File f : directory.listFiles())
		{
			if(f.getName().equals(".dropbox"))
			{
				continue;
			}
			else if(f.getName().equals("desktop.ini"))
			{
				continue;
			}
			
			System.out.println("Deobfuscate: " + f.getName());
			
			String origFileName = decryptFileName(f.getName(), key, iv, zip);
			
			System.out.println("orig Name: " + origFileName);
			
			File dest = new File("testdirResult/" + origFileName);
			
			copyFileUsingStream(f, dest);
			System.out.println();
		}
	}
	
	public static String decryptFileName(String filename, byte[] key, byte[] iv, boolean zip) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, DataFormatException, IOException
	{
		byte[] encFileName = decodeUrlSafeBase64(filename);
		
		Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		SecretKeySpec aesKeySpec = new SecretKeySpec(key, "AES");

		decryptCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, ivParameterSpec);

		byte[] decryptedBytes = decryptCipher.doFinal(encFileName);

		String decoded;
		
		if(zip)
		{
			decoded = unzipFileName(decryptedBytes);
		}
		else
		{
			decoded = new String(decryptedBytes, PanboxConstants.STANDARD_CHARSET);
		}
		
		System.out.println("Decrypted String:\t" + decoded);
		
		return decoded;
	}
	
	public static void obfuscateFileNamesInDir(String dir, byte[] key, byte[] iv, boolean zip) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException
	{
		File directory = new File(dir);
		
		for (File f : directory.listFiles())
		{
			System.out.println("Obfuscate: " + f.getName());
			
			String newFileName = encryptFileName(f.getName(), key, iv, zip);
			
			System.out.println("Obfuscated name: " + newFileName);
			
			File dest = new File("testdirOb/" + newFileName);
			
			copyFileUsingStream(f, dest);
			
			System.out.println();
		}
	}
	
	public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
        	if (is != null)
        		is.close();
        	if (os != null)
        		os.close();
        }
    }
	
	public static String encryptFileName(String filename, byte[] key, byte[] iv, boolean zip) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException
	{
		System.out.println("Original String ("+filename.length()+" chars):\t" + filename);
		SecretKeySpec aesKeySpec = new SecretKeySpec(key, "AES");
		
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		encryptCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, ivParameterSpec);

		byte[] encryptedBytes;
		if(zip)
		{
			//test zip filename
			byte[] zippedFileName = zipFileName(filename);		
			
			System.out.println("Zip ("+((float)zippedFileName.length/(float)filename.getBytes().length)*100 +"%) OrigName bytes: " + filename.getBytes().length + " Zipped bytes: " + zippedFileName.length);
			encryptedBytes = encryptCipher.doFinal(zippedFileName);
		}
		else
		{
			encryptedBytes = encryptCipher.doFinal(filename.getBytes());
		}
		
		String base64Text = encodeUrlSafeBase64(encryptedBytes);
		System.out.println("Base64-Url String ("+base64Text.length()+" chars):\t" + base64Text);
		
		return base64Text;
	}
	
	public static void printByteArray(byte[] bytes)
	{
		System.out.print("{");
		for(int i=0; i<bytes.length; i++)
		{
			if(i == bytes.length-1)
				System.out.print(bytes[i]+ "");
			else
				System.out.print(bytes[i]+ ", ");
		}
		System.out.println("}");
	}
	
	public static String unzipFileName(byte[] zippedFileName) throws DataFormatException, IOException
	{
		Inflater inflater = new Inflater();
		inflater.setInput(zippedFileName);
				
		byte[] buffer = new byte[1024];
        inflater.inflate(buffer);
        inflater.end();
		
		int count =0;
		for(int i = 0; i<buffer.length; i++)
		{
			if(buffer[i] != 0)
				count++;
		}
		
		byte[] result = new byte[count];
		for(int i = 0; i<result.length; i++)
		{
			result[i] = buffer[i];
		}
		
		return new String(result, PanboxConstants.STANDARD_CHARSET);
		
	}
	
	public static byte[] zipFileName(String fileName) throws IOException
	{
		Deflater deflater = new Deflater();

		byte[] bytes = fileName.getBytes();
		deflater.setInput(bytes);

		deflater.finish();

		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		byte[] buffer = new byte[1024];

		while(!deflater.finished())
		{
			int bytesCompressed = deflater.deflate(buffer);
			bos.write(buffer,0,bytesCompressed);
		}
		bos.close();

		int count =0;
		for(int i = 0; i<buffer.length; i++)
		{
			if(buffer[i] != 0)
				count++;
		}
		
		byte[] result = new byte[count];
		for(int i = 0; i<result.length; i++)
		{
			result[i] = buffer[i];
		}
		
		return result;
	}
	
	
	public static String encodeUrlSafeBase64(byte[] data)
	{		
//		BASE64Encoder b64enc = new BASE64Encoder();
//		String b64Str = b64enc.encode(data);
		//b64enc.
		
//		char[] chars = b64Str.toCharArray();
//		
//		for(int i=0; i < chars.length; i++)
//		{
//			if(chars[i] == '+')
//			{
//				chars[i] = '-';
//			}
//			else if (chars[i] == '/') {
//				chars[i] = '_';
//			}
//		}
//		return String.valueOf(chars);
		
		return EncodingHelper.encodeByte(data, EncodingType.BASE64);
	}
	
	public static byte[] decodeUrlSafeBase64(String str)
	{		
//		char[] chars = str.toCharArray();
//		
//		for(int i=0; i < chars.length; i++)
//		{
//			if(chars[i] == '-')
//			{
//				chars[i] = '+';
//			}
//			else if (chars[i] == '_') {
//				chars[i] = '/';
//			}
//		}
//		String b64Str = String.valueOf(chars);
//		
//		BASE64Decoder b64dec = new BASE64Decoder();
//		
//		byte[] b64bytes = null;
//		try {
//			b64bytes = b64dec.decodeBuffer(b64Str);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return EncodingHelper.decodeString(str, EncodingType.BASE64);
	}

}
