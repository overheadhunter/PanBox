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
package org.panbox.core.identitymgmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.panbox.PanboxConstants;
import org.panbox.core.Utils;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.randomness.SecureRandomWrapper;
import org.panbox.core.exception.RandomDataGenerationException;

public class VCardProtector {

//	private static final String VCARD_PROTECTION_SALTNAME = "salt";

	private final static Logger logger = Logger.getLogger(VCardProtector.class);

	private static SecureRandomWrapper srWrapper;
	private static Mac vcfMac;

	static {
		try {
			vcfMac = Mac.getInstance(KeyConstants.VCARD_HMAC,
					KeyConstants.PROV_BC);
			srWrapper = SecureRandomWrapper.getInstance();
		} catch (RandomDataGenerationException e) {
			logger.error("Unable to initialize SecureRandom wrapper class!", e);
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			logger.error("Unable to initialize VCard messagedigest!", e);
		}
	}

	public static void protectVCF(File targetFile, File vCardFile,
			char[] password) throws Exception {
		ZipArchiveOutputStream out = null;
		try {
			out = new ZipArchiveOutputStream(new FileOutputStream(targetFile));

			byte[] vCardData = IOUtils.toByteArray(new FileInputStream(
					vCardFile));
			byte[] passwordBytes = Utils.toBytes(password);

			vcfMac.init(new SecretKeySpec(passwordBytes,
					KeyConstants.VCARD_HMAC));
			byte[] hmac = vcfMac.doFinal(vCardData);

			String fileName = Utils.bytesToHex(hmac);

			// first entry is the vcard itself
			ZipArchiveEntry entry = new ZipArchiveEntry(vCardFile.getName());
			entry.setSize(vCardData.length);
			out.putArchiveEntry(entry);
			out.write(vCardData);
			out.flush();
			out.closeArchiveEntry();

			// second entry is the hmac value
			entry = new ZipArchiveEntry(fileName);
			entry.setSize(fileName.length());
			out.putArchiveEntry(entry);
			out.closeArchiveEntry();
			out.flush();
		} catch (IOException | InvalidKeyException e) {
			logger.error("Could not create protected VCF export file!", e);
			throw e;
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}

	// 32 byte default salt size
//	final static int SALT_SIZE = 64;
//
//	private static byte[] generateSalt() throws RandomDataGenerationException {
//		byte[] ret = new byte[SALT_SIZE];
//		srWrapper.nextBytes(ret);
//		return ret;
//	}

	/**
	 * Method extracts the VCF file stored within the zipped import file to the
	 * given destination file. It further returns the corresponding hmac stored
	 * within the archive.
	 * 
	 * @param sourceFile
	 *            import archive
	 * @param tmpFile
	 *            temporary file to extract the CVF to
	 * @return byte[] array containing the hmac of the VCF
	 * @throws Exception
	 */
	public static byte[] unwrapVCF(File sourceFile, File tmpFile)
			throws FileNotFoundException, IOException {

		ZipArchiveInputStream in = null;
		FileOutputStream fos = null;
		String hmacString = null;
		try {

			in = new ZipArchiveInputStream(new FileInputStream(sourceFile));
			ArchiveEntry entry;
			// ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// ENTRY 1: vcard contents
			in.getNextEntry();
			fos = new FileOutputStream(tmpFile);
			IOUtils.copy(in, fos);

			// ENTRY 2: sha-256 hmac
			entry = in.getNextEntry();
			hmacString = entry.getName();

			return Utils.hexToBytes(hmacString);
		} catch (StringIndexOutOfBoundsException e) {
			logger.error("Error parsing hmac: " + hmacString
					+ " is no valid hex String", e);
			throw e;
		} catch (Exception e) {
			logger.error("Error unwrapping VCF file", e);
			throw e;
		} finally {
			if (fos != null) {
				fos.flush();
				fos.close();
			}
			if (in != null) {
				in.close();
			}
		}
	}

	public static boolean verifyVCFIntegrity(byte[] vcf, byte[] hmacToCheck,
			char[] password) {
		try {
			byte[] passwordBytes = Utils.toBytes(password);
			vcfMac.init(new SecretKeySpec(passwordBytes,
					KeyConstants.VCARD_HMAC));
			byte[] ret = vcfMac.doFinal(vcf);
			return Arrays.equals(ret, hmacToCheck);
		} catch (Exception e) {
			logger.error("Could not verify VCF integrity!", e);
			return false;
		}
	}

	/**
	 * reads an extracted vcf and returns its contents as a byte array
	 * 
	 * @param vcfFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static byte[] loadVCFBytes(File vcfFile)
			throws FileNotFoundException, IOException {
		return IOUtils.toByteArray(new FileInputStream(vcfFile));
	}

	/**
	 * verifies the given vcf file w.r.t. the given reference hmac value and
	 * password
	 * 
	 * @param vcfLoc
	 * @param hmacToCheck
	 * @param password
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static boolean verifyVCFIntegrity(File vcfLoc, byte[] hmacToCheck,
			char[] password) throws FileNotFoundException, IOException {
		byte[] vCardBytes = IOUtils.toByteArray(new FileInputStream(vcfLoc));
		return verifyVCFIntegrity(vCardBytes, hmacToCheck, password);
	}

	/**
	 * Generates a password with size of
	 * {@link PanboxConstants#DEFAULT_EXPORT_PIN_LENGHT} containing a-z, A-Z and
	 * 0-9
	 * 
	 * @return password in form of char[]
	 * @throws RandomDataGenerationException
	 */
	public static char[] generatePassword()
			throws RandomDataGenerationException {

		char[] symbols = PanboxConstants.DEFAULT_EXPORT_PIN_CHARSET;
		char[] buf = new char[PanboxConstants.DEFAULT_EXPORT_PIN_LENGHT];

		for (int i = 0; i < buf.length; i++) {
			buf[i] = symbols[srWrapper.nextInt(symbols.length)];
		}

		return buf;
	}
}
