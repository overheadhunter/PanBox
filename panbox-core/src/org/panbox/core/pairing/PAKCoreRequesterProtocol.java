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
package org.panbox.core.pairing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public abstract class PAKCoreRequesterProtocol {

	protected static final Logger logger = Logger.getLogger("org.panbox");
	
	static {
		logger.addAppender(new ConsoleAppender());
	}

	public static final BigInteger g = new BigInteger("13");
	public static final BigInteger p = new BigInteger(
			"FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF",
			16);
	protected String idA;
	protected String idB;
	private final String password;
	protected ObjectOutputStream dataOutputStream;
	protected ObjectInputStream dataInputStream;

	public PAKCoreRequesterProtocol(String password) {
		this.password = password;
	}

	public abstract void initCommunication() throws Exception;

	public abstract void runOperation(Cipher cipher, SecretKeySpec spec)
			throws Exception;

	public void runProtocol() throws IOException, ClassNotFoundException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, Exception {
		initCommunication();
		logger.debug("IDA: " + idA + ", IDB: " + idB + ", PW: " + password);

		BigInteger Ra = new BigInteger(1024, new SecureRandom());
		BigInteger gRa = g.modPow(Ra, p);
		BigInteger gRb;
		BigInteger gRaRb;

		// -- Protocol Step 1 (send request A->B) --
		// 1. calculate X
		String pstr = idA + idB + password;
		BigInteger P = new BigInteger(1, HashHelper.hash(1, pstr));
		BigInteger X = P.multiply(gRa);

		// 2. send idA and X
		dataOutputStream.writeUTF(idA);
		dataOutputStream.writeObject(X);
		dataOutputStream.flush();

		// -- Protocol Step 2 (handle response B->A) --
		// 3. receive Y and S1
		BigInteger Y = (BigInteger) dataInputStream.readObject();
		BigInteger recS1 = (BigInteger) dataInputStream.readObject();

		// 4. check if Y is zero
		if (Y.equals(BigInteger.ZERO)) {
			throw new ProtocolException(
					"The received message Y was zero, which should never happen!");
		}

		// 5. calculate gRb and S1'
		P = new BigInteger(1, HashHelper.hash(2, pstr));
		gRb = Y.divide(P);
		gRaRb = gRb.multiply(gRa);
		String s1str = idA + idB + password + gRa + gRb + gRaRb;
		BigInteger S1 = new BigInteger(1, HashHelper.hash(3, s1str));

		// 6. check if the received S1 is really S1
		if (!S1.equals(recS1)) {
			throw new ProtocolException("The received S1=" + recS1
					+ " is not equal the calculated S1'=" + S1);
		}

		// 7. AUTH-OK for A to B
		// Authentication done

		// 8. calculate S2
		String s2str = idA + idB + password + gRa + gRb + gRaRb;
		BigInteger S2 = new BigInteger(1, HashHelper.hash(4, s2str));

		// 9. send S2
		dataOutputStream.writeObject(S2);
		dataOutputStream.flush();

		// 10. Calculate session key K.
		String kstr = idA + idB + password + gRa + gRb + gRaRb;
		byte[] key = HashHelper.hash(5, kstr);

		// -- Protocol Step 3 (handle key and cert B->A) --
		// 11. Init encryption
		SecretKeySpec spec = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES");

		runOperation(cipher, spec);

		Thread.sleep(100);
	}
}
