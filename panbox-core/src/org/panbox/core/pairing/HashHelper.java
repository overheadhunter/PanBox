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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 * @author Clemens A. Schulz <c.schulz@sirrix.com>
 */
public class HashHelper {

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static byte[] last128bit(byte[] hash) {
        return Arrays.copyOfRange(hash, hash.length - 16, hash.length);
    }

    /**
     * This helper method outputs the correct hash for the PAK protocol.
     *
     * @param idx H_i index (1-5) of the hash that should be created
     * @param val The value that should be hashed (pure value only)
     */
    public static byte[] hash(int idx, String val) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");

        byte[] retVal = {};

        switch (idx) {
            case 1: //1152bit output (9 calls)
            case 2: //1152bit output (9 calls)
                String val1 = null;
                for (int i = 0; i < 9; ++i) {
                    val1 = Integer.toString(idx) + Integer.toString(i + 1) + val;
                    sha.update(val1.getBytes());
                    retVal = concat(retVal, last128bit(sha.digest()));
                }
                break;
            case 3: //128bit output
            case 4: //128bit output
            case 5: //128bit output
                String val2 = Integer.toString(idx) + Integer.toString(val.length()) + val + val;
                sha.update(val2.getBytes());
                retVal = last128bit(sha.digest());
                break;
            default:
                throw new IllegalArgumentException("The hash index " + idx + " does not exist!");
        }

        return retVal;
    }
}
