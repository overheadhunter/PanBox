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

import java.io.EOFException;
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

import org.apache.log4j.Logger;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public abstract class PAKCoreHandlerProtocol {

	protected static final Logger logger = Logger.getLogger("org.panbox");

    public static final BigInteger g = new BigInteger("13", 10); //base
    public static final BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16); //very large prime for mod operation (2048bit)
    protected String idA; //Requester of protocol = idA
    protected String idB; //Handler of protocol = idB (this)
    private final String password; //Protocol pairing password
    protected ObjectOutputStream dataOutputStream; //output stream
    protected ObjectInputStream dataInputStream; //output stream

    public PAKCoreHandlerProtocol(String password) {
        this.password = password;
    }

    public abstract void initCommunication() throws Exception;

    public abstract void runOperation(Cipher cipher, SecretKeySpec spec) throws Exception;

    public void runProtocol() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, Exception {
        initCommunication();
    	System.out.println("IDA: " + idA + ", IDB: " + idB + ", PW: " + password);

        BigInteger Rb = new BigInteger(1024, new SecureRandom());
        BigInteger gRb = g.modPow(Rb, p);
        BigInteger gRa;
        BigInteger gRaRb;

        //-- Protocol Step 1 (handle request A->B) --
        //1. Receive idA and X
        String recIdA = dataInputStream.readUTF();
        BigInteger recX = (BigInteger) dataInputStream.readObject();
        //2. Check if the received idA is equal to the set idA
        if (!idA.equals(recIdA)) {
            throw new ProtocolException("The incoming connection has IdA=" + recIdA + ", while connection was setup for IdA=" + idA);
        }
        //3. Check if received X is zero
        if (recX.equals(BigInteger.ZERO)) {
            throw new ProtocolException("The received message X was zero, which should never happen!");
        }
        //4. Calculate g^Ra from X
        String pstr = idA + idB + password;
        BigInteger P = new BigInteger(1, HashHelper.hash(1, pstr));
        gRa = recX.divide(P);

        //-- Protocol Step 2 (send response B->A) --
        //5. Calculate g^RaRb, Y and S1
        P = new BigInteger(1, HashHelper.hash(2, pstr));
        gRaRb = gRa.multiply(gRb);
        BigInteger Y = P.multiply(gRb);
        String s1str = idA + idB + password + gRa + gRb + gRaRb;
        BigInteger S1 = new BigInteger(1, HashHelper.hash(3, s1str));

        //6. Send Y and S1 to the requester
        dataOutputStream.writeObject(Y);
        dataOutputStream.writeObject(S1);
        dataOutputStream.flush();

        //-- Protocol Step 3 (handle request A->B) --
        //7. Receives S2
        BigInteger S2 = null;
        try {
             S2 = (BigInteger) dataInputStream.readObject();
        } catch (EOFException ex) {
        	throw new ProtocolException("S2 could not be received. Other side exited after sending S1.");
        }

        //8. Check if S2 is equal S2'
        String s2str = idA + idB + password + gRa + gRb + gRaRb;
        BigInteger S2apos = new BigInteger(1, HashHelper.hash(4, s2str));
        if (!S2apos.equals(S2)) {
            throw new ProtocolException("The received message S2=" + S2 + " is not equal to S2'=" + S2apos + ".");
        }

        //9. AUTH-OK for A to B

        //10. Calculate session key K.
        String kstr = idA + idB + password + gRa + gRb + gRaRb;
        byte[] key = HashHelper.hash(5, kstr);
        SecretKeySpec spec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");

        runOperation(cipher, spec);

        Thread.sleep(100);
    }
}
