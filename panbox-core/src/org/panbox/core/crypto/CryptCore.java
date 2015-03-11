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
package org.panbox.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.panbox.PanboxConstants;
import org.panbox.core.Utils;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyEncryptionException;
import org.panbox.core.identitymgmt.IPerson;
import org.panbox.core.keymgmt.ShareKeyDBEntry;

public class CryptCore {

	private final static Cipher ASYMM_CIPHER;

	private static final Logger logger = Logger.getLogger("org.panbox.core");

	static {
		// add bouncy castle
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		try {
			ASYMM_CIPHER = Cipher
					.getInstance(KeyConstants.ASYMMETRIC_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			logger.fatal("Alogrithm " + KeyConstants.ASYMMETRIC_ALGORITHM
					+ " not found!", e);
			throw new RuntimeException(e);
		} catch (NoSuchPaddingException e) {
			logger.fatal("No Padding for " + KeyConstants.ASYMMETRIC_ALGORITHM
					+ " found!", e);
			throw new RuntimeException(e);
		}
	}

	private CryptCore() {
		super();
	}

	/**
	 * Method calculates a fingerprint for the given public key instance
	 * 
	 * @param pubKey
	 * @return secure hash {@link KeyConstants#PUBKEY_FINGERPRINT_DIGEST} of the
	 *         encoded public key, or null if there was an error
	 */
	public static byte[] getPublicKeyfingerprint(PublicKey pubKey) {
		try {
			MessageDigest mdFingerprint = MessageDigest.getInstance(
					KeyConstants.PUBKEY_FINGERPRINT_DIGEST,
					KeyConstants.PROV_BC);
			byte[] enc = pubKey.getEncoded();
			if (enc != null && enc.length != 0) {
				return mdFingerprint.digest(enc);
			} else {
				logger.error(CryptCore.class.getName()
						+ "::getPublicKeyFingerprint: Invalid public key!");
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			logger.error(
					CryptCore.class.getName()
							+ "::getPublicKeyFingerprint: Could not init digest for fingerprinting!",
					e);
		}
		return null;
	}

	public static KeyStore createUnprotectedKeyStore() {
		KeyStore store = null;
		try {
			store = KeyStore.getInstance(KeyConstants.KEYSTORE_TYPE);
			store.load(null, KeyConstants.OPEN_KEYSTORE_PASSWORD);
		} catch (NoSuchAlgorithmException | CertificateException | IOException
				| KeyStoreException e) {
			logger.error(
					"Could not create a keystore with out default password", e);
		}
		return store;
	}

	public static boolean verifySignature(Signable s, byte[] signature,
			PublicKey key) throws SignatureException {
		try {
			return SignatureHelper.verify(s, signature, key);
		} catch (Exception e) {
			throw new SignatureException("Could not verify signature", e);
		}
	}

	public static SecretKey generateSymmetricKey() {
		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance(
					KeyConstants.SYMMETRIC_ALGORITHM, KeyConstants.PROV_BC);
			generator.init(KeyConstants.SYMMETRIC_KEY_SIZE);
			return generator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			logger.error("Error during symmetric key generation: " + e);
		} catch (NoSuchProviderException e) {
			logger.error("Error during symmetric key generation: " + e);
		}
		return null;
	}

	public static byte[] encryptSymmetricKey(byte[] symKey, PublicKey pKey)
			throws SymmetricKeyEncryptionException {
		try {
			ASYMM_CIPHER.init(Cipher.ENCRYPT_MODE, pKey);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			CipherOutputStream cos = new CipherOutputStream(bos, ASYMM_CIPHER);
			cos.write(symKey);
			cos.flush();
			cos.close();
			byte[] byteArray = bos.toByteArray();
			return byteArray;
		} catch (Exception e) {
			throw new SymmetricKeyEncryptionException(e);
		}
	}

	public static SecretKey decryptSymmertricKey(byte[] symKey, PrivateKey pKey)
			throws SymmetricKeyDecryptionException {
		try {
			byte[] byteArray = _asymmetricDecrypt(symKey, pKey);

			SecretKey k = new SecretKeySpec(byteArray,
					KeyConstants.SYMMETRIC_ALGORITHM);

			return k;
		} catch (Exception e) {
			throw new SymmetricKeyDecryptionException(e);
		}
	}

	public static byte[] _asymmetricDecrypt(byte[] symKey, PrivateKey pKey)
			throws InvalidKeyException, IOException {
		ASYMM_CIPHER.init(Cipher.DECRYPT_MODE, pKey);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CipherOutputStream cos = new CipherOutputStream(bos, ASYMM_CIPHER);
		cos.write(symKey);
		cos.flush();
		cos.close();
		byte[] byteArray = bos.toByteArray();
		return byteArray;
	}

	public static SecretKey decryptShareKey(ShareKeyDBEntry entry,
			PublicKey pubKey, PrivateKey privKey) {
		SecretKey result = null;
		if (entry != null) {
			byte[] encSK = entry.getEncryptedKey(pubKey);
			byte[] sk = new byte[KeyConstants.SYMMETRIC_BLOCK_SIZE];
			try {
				ASYMM_CIPHER.init(Cipher.DECRYPT_MODE, privKey);
				ByteArrayInputStream bis = new ByteArrayInputStream(encSK);
				CipherInputStream cis = new CipherInputStream(bis, ASYMM_CIPHER);
				cis.read(sk);
				cis.close();
				bis.close();
				result = new SecretKeySpec(sk, entry.getAlgorithm());
			} catch (InvalidKeyException e) {
				logger.warn("Exception caught in CryptCore.decryptShareKey", e);
			} catch (IOException e) {
				logger.warn("Exception caught in CryptCore.decryptShareKey", e);
			}
		}
		return result;
	}

	public static KeyPair generateKeypair() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(
					KeyConstants.KEY_FACTORY, KeyConstants.PROV_BC);
			kpg.initialize(KeyConstants.ASYMMETRIC_KEYSIZE);
			KeyPair kp = kpg.generateKeyPair();
			return kp;
		} catch (NoSuchAlgorithmException e) {
			logger.error("Error during asymmetric key pair generation: " + e);
		} catch (NoSuchProviderException e) {
			logger.error("Error during asymmetric key pair generation: " + e);
		}
		return null;
	}

	public static byte[] sign(Signable s, PrivateKey key)
			throws SignatureException {
		try {
			return SignatureHelper.sign(s, key);
		} catch (Exception e) {
			throw new SignatureException("Could not sign...", e);
		}
	}

	public static void encryptStream(InputStream bis, OutputStream fos,
			SecretKey shareKey) {

		Cipher aesCipher;
		try {
			aesCipher = Cipher.getInstance(KeyConstants.SYMMETRIC_ALGORITHM);

			aesCipher.init(Cipher.ENCRYPT_MODE, shareKey);

			CipherOutputStream os = new CipherOutputStream(fos, aesCipher);

			copy(bis, os);

			os.close();

		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IOException e) {
			logger.warn("Exception caught in CryptCore.encryptStream", e);
		}

	}

	public static void decryptStream(InputStream bis, OutputStream fos,
			SecretKey shareKey) {

		Cipher aesCipher = null;

		try {
			aesCipher = Cipher.getInstance(KeyConstants.SYMMETRIC_ALGORITHM);
			aesCipher.init(Cipher.DECRYPT_MODE, shareKey);

			CipherInputStream is = new CipherInputStream(bis, aesCipher);

			copy(is, fos);

			is.close();

		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IOException e) {
			logger.warn("Exception caught in CryptCore.decryptStream", e);
		}

	}

	public static ByteBuffer decryptFile(ByteBuffer buf, SecretKey key) {
		buf.clear();
		byte[] bytes = new byte[buf.capacity()];
		buf.get(bytes, 0, bytes.length);

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		CryptCore.decryptStream(bais, baos, key);

		try {
			bais.close();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ByteBuffer.wrap(baos.toByteArray());
	}

	private static void copy(InputStream is, OutputStream os)
			throws IOException {
		int i;
		byte[] b = new byte[1024];
		while ((i = is.read(b)) != -1) {
			os.write(b, 0, i);
		}
	}

	public static X509Certificate createSelfSignedX509Certificate(
			PrivateKey privKey, PublicKey pubKey, IPerson person) {
		return createSelfSignedX509Certificate(privKey, pubKey,
				person.getEmail(),
				person.getFirstName() + " " + person.getName());
	}

	// public static X509Certificate createSelfSignedX509Certificate(
	// PrivateKey privKey, PublicKey pubKey) {
	// return createSelfSignedX509Certificate(privKey, pubKey, null, null);
	// }

	/**
	 * Creates a self signed certificate valid for 10 years (necessary to store
	 * public keys in keystore)
	 * 
	 * @param privKey
	 * @param pubKey
	 * @param eMail
	 * @param name
	 * @return the certificate or NULL if there is an error
	 */
	private static X509Certificate createSelfSignedX509Certificate(
			PrivateKey privKey, PublicKey pubKey, String eMail, String name) {
		// Generate self-signed certificate
		X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
		builder.addRDN(BCStyle.OU, "Panbox");
		builder.addRDN(BCStyle.O, "Panbox");
		builder.addRDN(BCStyle.CN, "localhost");

		if (eMail != null) {
			builder.addRDN(BCStyle.EmailAddress, eMail);
		}

		if (name != null) {
			builder.addRDN(BCStyle.NAME, name);
		}

		Calendar cal = Calendar.getInstance();
		Date notBefore = cal.getTime();

		cal.add(Calendar.YEAR, PanboxConstants.CERTIFICATE_LIFETIME_YEARS);
		Date notAfter = cal.getTime();

		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

		X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
				builder.build(), serial, notBefore, notAfter, builder.build(),
				pubKey);

		X509Certificate cert = null;
		try {
			ContentSigner sigGen = new JcaContentSignerBuilder(
					"SHA256WithRSAEncryption")
					.setProvider(KeyConstants.PROV_BC).build(privKey);

			cert = new JcaX509CertificateConverter().setProvider(
					KeyConstants.PROV_BC).getCertificate(certGen.build(sigGen));

			cert.checkValidity(new Date());

			cert.verify(cert.getPublicKey());

		} catch (NoSuchAlgorithmException | InvalidKeyException
				| OperatorCreationException | CertificateException
				| NoSuchProviderException | SignatureException e) {
			logger.warn(
					"Exception caught in CryptCore.createSelfSignedX509Certificate, returning null",
					e);
		}

		return cert;
	}

	/**
	 * Creates a public key from a byte[]
	 * 
	 * @param keyType
	 *            type for example "RSA"
	 * @param keyBytes
	 *            the byte[] with the key
	 * @return the public key or null on error
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static PublicKey createPublicKeyFromBytes(byte[] keyBytes) {
		PublicKey pk = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(
					KeyConstants.KEY_FACTORY, new BouncyCastleProvider());
			pk = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			logger.warn("Exception caught in CryptCore."
					+ "createPublicKeyFromBytes, returning null", e);
		}
		return pk;
	}

	/**
	 * Creates an X509 Certificate for a given byte array
	 * 
	 * @param certBytes
	 * @return
	 */
	public static X509Certificate createCertificateFromBytes(byte[] certBytes) {
		X509Certificate cert = null;
		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance("X.509");

			InputStream in = new ByteArrayInputStream(certBytes);
			cert = (X509Certificate) certFactory.generateCertificate(in);

		} catch (CertificateException e) {
			logger.warn("Excpetion caught in CryptCore."
					+ "createCertificateFromBytes, returning null", e);
		}

		return cert;
	}

	/**
	 * Converts a given RSA PrivateKey instance to a KeyPair instance. The
	 * PublicKey will be extracted from PrivateKey instance.
	 * 
	 * @param pKey
	 *            The RSA PrivateKey used to generate the KeyPair
	 * @return KeyPair instance including the private and public key.
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static KeyPair privateKeyToKeyPair(PrivateKey pKey)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance(
				KeyConstants.KEY_FACTORY, new BouncyCastleProvider());
		RSAPrivateCrtKey rsaPKey = (RSAPrivateCrtKey) pKey;
		RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(
				rsaPKey.getModulus(), rsaPKey.getPublicExponent());
		return new KeyPair(keyFactory.generatePublic(publicKeySpec), pKey);
	}

	public static byte[] genChecksum(File f) throws NoSuchAlgorithmException,
			NoSuchProviderException, FileNotFoundException, IOException {
		MessageDigest md = MessageDigest.getInstance(KeyConstants.DEFAULT_HASH,
				KeyConstants.PROV_BC);
		byte[] data = IOUtils.toByteArray(new FileInputStream(f));
		return md.digest(data);
	}

	public static boolean checkChecksum(File f, byte[] tocheck)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			FileNotFoundException, IOException {
		byte[] ref = genChecksum(f);
		return Arrays.equals(ref, tocheck);
	}

	public static char[] deriveKeystorePass(char[] userpass, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec ks = new PBEKeySpec(userpass, salt, 4096, 512);
		byte[] res = f.generateSecret(ks).getEncoded();
		Utils.eraseChars(userpass);
		return Utils.toChars(res);
	}

}
