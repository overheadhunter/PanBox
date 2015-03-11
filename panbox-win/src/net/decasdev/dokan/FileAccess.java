/*
 * JDokan : Java library for Dokan Copyright (C) 2008 Yu Kobayashi http://yukoba.accelart.jp/ 2009 Caleido AG
 * http://www.wuala.com/ This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package net.decasdev.dokan;

public interface FileAccess {

    public final static int GENERIC_READ = 0x80;
    // these are the observed values that are passed to onCreateFile
    public final static int GENERIC_WRITE = 0x40;
    
	// // access mask rights derived from WinNT.h, cf.
	// //
	// http://blogs.msdn.com/b/openspecification/archive/2010/04/01/about-the-access-mask-structure.aspx
	// public final static long GENERIC_READ = 0x80000000L;
	//
	// public final static long GENERIC_WRITE = 0x40000000L;
	//
	// public final static long GENERIC_EXECUTE = 0x20000000L;
	//
	// public final static long GENERIC_ALL = 0x10000000L;
	//
	// public final static long ACCESS_SYSTEM_SECURITY = 0x01000000L;
	//
	// public final static long DELETE = 0x00010000L;
	//
	// public final static long READ_CONTROL = 0x00020000L;
	//
	// public final static long WRITE_DAC = 0x00040000L;
	//
	// public final static long WRITE_OWNER = 0x00080000L;
	//
	// public final static long SYNCHRONIZE = 0x00100000L;
	//
	// public final static long STANDARD_RIGHTS_REQUIRED = 0x000F0000L;
	//
	// public final static long STANDARD_RIGHTS_READ = READ_CONTROL;
	//
	// public final static long STANDARD_RIGHTS_WRITE = READ_CONTROL;
	//
	// public final static long STANDARD_RIGHTS_EXECUTE = READ_CONTROL;
	//
	// public final static long STANDARD_RIGHTS_ALL = 0x001F0000L;
}
