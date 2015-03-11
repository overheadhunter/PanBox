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
package org.panbox.core.crypto.randomness;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.exception.RandomDataGenerationException;

/**
 * @author palige
 * 
 *         Class implements a wrapper for instances of {@link SecureRandom},
 *         adding additional checks and methods regarding entropy / reseeding.
 */
public class SecureRandomWrapper {

	/**
	 * stores the actual {@link SecureRandom}-instance. use default
	 * PRNG-implementation.
	 */
	private final SecureRandom wrappedRandomnessGenerator;

	/**
	 * 512 kbytes
	 */
	protected final static long RESEEDING_LIMIT = 1024 * 512;

	private static SecureRandomWrapper instance;

	/**
	 * stores the number of random bytes that have been generated so far
	 */
	protected long nBytesGenerated;

	/**
	 * @throws RandomDataGenerationException
	 * @throws NoSuchAlgorithmException
	 * 
	 */
	private SecureRandomWrapper() throws RandomDataGenerationException {
		try {
			this.wrappedRandomnessGenerator = SecureRandom
					.getInstance(KeyConstants.CSPRNG_ALGO);
			// upon initialization force prng to seed itself..
			byte[] tmp = new byte[128];
			this.wrappedRandomnessGenerator.nextBytes(tmp);
		} catch (NoSuchAlgorithmException e) {
			throw new RandomDataGenerationException(
					"Unable to initialize instance!", e);
		}
	}

	/**
	 * performs 5 different randomness health tests for the given
	 * {@link SecureRandomWrapper}-instance as defined in by BSI in AIS20/AIS31
	 * and in ISO18031:2005
	 * 
	 * @param secureRandom
	 * @return <code>true</code> if all tests have been passed,
	 *         <code>false</code> otherwise
	 * @throws RandomDataGenerationException
	 */
	protected boolean startupHealthTests() throws RandomDataGenerationException {
		byte[] b = new byte[2500];
		wrappedRandomnessGenerator.nextBytes(b);
		nBytesGenerated += b.length;

		IRandomnessQualityRanges ranges = new AIS31Borders();
		// IRandomnessQualityRanges ranges = new ISO18031Borders();
		RandomnessQualityCheck rQuality = new RandomnessQualityCheck();

		// do randomness startup tests
		rQuality.setBorders(ranges);

		// returns true if all tests are passed
		return (rQuality.monobitTest(b) && rQuality.pokerTest(b)
				&& rQuality.runTest(b) && rQuality.longrunTest(b) && rQuality
					.autocorrelationTest(b));
	}

	private final static int NUM_HEALTH_TESTS = 5;

	/**
	 * creates and returns a {@link SecureRandomWrapper} instance. Upon
	 * instantiation, randomness health tests are performed
	 * 
	 * @return the instance
	 */
	public synchronized static SecureRandomWrapper getInstance()
			throws RandomDataGenerationException {
		if (instance == null) {
			instance = new SecureRandomWrapper();
			for (int i = 0; i <= NUM_HEALTH_TESTS; i++) {
				if (instance.startupHealthTests()) {
					break;
				} else {
					instance.reseed();
					// if health tests fail after max number of
					// iterations, throw exception
					if (i == NUM_HEALTH_TESTS) {
						throw new RandomDataGenerationException(
								"PRNG did not pass initial health tests!");
					}
				}
			}
		}
		return instance;
	}

	/**
	 * encapsulates {@link SecureRandom#nextBytes(byte[])} with additional
	 * checks for entropy freshness
	 * 
	 * @param buffer
	 * @throws RandomDataGenerationException
	 *             to be thrown if no additional entropy has been introduced
	 */
	public synchronized void nextBytes(byte[] buffer)
			throws RandomDataGenerationException {
		if ((buffer == null) || (buffer.length == 0))
			throw new RandomDataGenerationException("buffer is empty");
		reseed();
		wrappedRandomnessGenerator.nextBytes(buffer);
		nBytesGenerated += buffer.length;
	}

	/**
	 * method periodically introduces some new seed material into the prngs
	 * internal sate. just to be sure
	 */
	private void reseed() throws RandomDataGenerationException {
		if (nBytesGenerated < RESEEDING_LIMIT) {
			return;
		} else {
			try {
				setSeed(wrappedRandomnessGenerator.generateSeed(32));
			} catch (Exception e) {
				throw new RandomDataGenerationException("reseeding failed");
			}
		}
	}

	final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;
	final int SIZEOF_DOUBLE = Double.SIZE / Byte.SIZE;
	final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;
	final int SIZEOF_BOOLEAN = Byte.SIZE / Byte.SIZE;
	final int SIZEOF_FLOAT = Float.SIZE / Byte.SIZE;
	final int SIZEOF_GAUSSIAN = Double.SIZE / Byte.SIZE;

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextInt()
	 */
	public int nextInt() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_INT;
		return wrappedRandomnessGenerator.nextInt();
	}

	/**
	 * @param n
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextInt(int)
	 */
	public int nextInt(int n) throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_INT;
		return wrappedRandomnessGenerator.nextInt(n);
	}

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextLong()
	 */
	public long nextLong() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_LONG;
		return wrappedRandomnessGenerator.nextLong();
	}

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextBoolean()
	 */
	public boolean nextBoolean() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_BOOLEAN;
		return wrappedRandomnessGenerator.nextBoolean();
	}

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextFloat()
	 */
	public float nextFloat() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_FLOAT;
		return wrappedRandomnessGenerator.nextFloat();
	}

	/**
	 * @param seed
	 * @see java.security.SecureRandom#setSeed(byte[])
	 */
	public void setSeed(byte[] seed) {
		wrappedRandomnessGenerator.setSeed(seed);
		nBytesGenerated = 0;
	}

	/**
	 * @param seed
	 * @see java.security.SecureRandom#setSeed(long)
	 */
	public void setSeed(long seed) {
		wrappedRandomnessGenerator.setSeed(seed);
		nBytesGenerated = 0;
	}

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextDouble()
	 */
	public double nextDouble() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_DOUBLE;
		return wrappedRandomnessGenerator.nextDouble();
	}

	/**
	 * @return
	 * @throws RandomDataGenerationException
	 * @see java.util.Random#nextGaussian()
	 */
	public double nextGaussian() throws RandomDataGenerationException {
		reseed();
		nBytesGenerated += SIZEOF_GAUSSIAN;
		return wrappedRandomnessGenerator.nextGaussian();
	}

}
