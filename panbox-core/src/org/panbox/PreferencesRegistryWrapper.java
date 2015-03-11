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
package org.panbox;

import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;


public class PreferencesRegistryWrapper
	extends Preferences
{
	
	private static final Logger loggercommon = Logger.getLogger( "org.panbox.desktop" );
	
	private static final String PANBOX_REGISTRY = "SOFTWARE\\Panbox.org\\Panbox";
	
	public PreferencesRegistryWrapper()
	{
		try {
			WinRegistry.createKey( WinRegistry.HKEY_CURRENT_USER, PANBOX_REGISTRY );
		} catch ( Exception e ) {
			loggercommon.error( "Could not create key for Panbox usage in registry!" );
		}
	}
	
	@Override
	public void put( String key, String value )
	{
		try {
			WinRegistry.writeStringValue( WinRegistry.HKEY_CURRENT_USER, PANBOX_REGISTRY, key, value );
		} catch ( Exception e ) {
			loggercommon.error( "An error while writing Panbox settings registry key occurred!" );
		}
	}
	
	
	@Override
	public String get( String key, String def )
	{
		try {
			String read = WinRegistry.readString( WinRegistry.HKEY_CURRENT_USER, PANBOX_REGISTRY, key );
			if( read != null ) {
				return read;
			}
		} catch ( Exception e ) {
			loggercommon.error( "An error while writing Panbox settings registry key occurred!" );
		}
		return def;
	}
	
	
	@Override
	public void remove( String key )
	{
		throw new UnsupportedOperationException( "Method 'remove' has not been implemented yet." );
	}
	
	
	@Override
	public void clear()
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'clear' has not been implemented yet." );
	}
	
	
	@Override
	public void putInt( String key, int value )
	{
		throw new UnsupportedOperationException( "Method 'putInt' has not been implemented yet." );
	}
	
	
	@Override
	public int getInt( String key, int def )
	{
		throw new UnsupportedOperationException( "Method 'getInt' has not been implemented yet." );
	}
	
	
	@Override
	public void putLong( String key, long value )
	{
		throw new UnsupportedOperationException( "Method 'putLong' has not been implemented yet." );
	}
	
	
	@Override
	public long getLong( String key, long def )
	{
		throw new UnsupportedOperationException( "Method 'getLong' has not been implemented yet." );
	}
	
	
	@Override
	public void putBoolean( String key, boolean value )
	{
		throw new UnsupportedOperationException( "Method 'putBoolean' has not been implemented yet." );
	}
	
	
	@Override
	public boolean getBoolean( String key, boolean def )
	{
		throw new UnsupportedOperationException( "Method 'getBoolean' has not been implemented yet." );
	}
	
	
	@Override
	public void putFloat( String key, float value )
	{
		throw new UnsupportedOperationException( "Method 'putFloat' has not been implemented yet." );
	}
	
	
	@Override
	public float getFloat( String key, float def )
	{
		throw new UnsupportedOperationException( "Method 'getFloat' has not been implemented yet." );
	}
	
	
	@Override
	public void putDouble( String key, double value )
	{
		throw new UnsupportedOperationException( "Method 'putDouble' has not been implemented yet." );
	}
	
	
	@Override
	public double getDouble( String key, double def )
	{
		throw new UnsupportedOperationException( "Method 'getDouble' has not been implemented yet." );
	}
	
	
	@Override
	public void putByteArray( String key, byte[] value )
	{
		throw new UnsupportedOperationException( "Method 'putByteArray' has not been implemented yet." );
	}
	
	
	@Override
	public byte[] getByteArray( String key, byte[] def )
	{
		throw new UnsupportedOperationException( "Method 'getByteArray' has not been implemented yet." );
	}
	
	
	@Override
	public String[] keys()
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'keys' has not been implemented yet." );
	}
	
	
	@Override
	public String[] childrenNames()
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'childrenNames' has not been implemented yet." );
	}
	
	
	@Override
	public Preferences parent()
	{
		throw new UnsupportedOperationException( "Method 'parent' has not been implemented yet." );
	}
	
	
	@Override
	public Preferences node( String pathName )
	{
		throw new UnsupportedOperationException( "Method 'node' has not been implemented yet." );
	}
	
	
	@Override
	public boolean nodeExists( String pathName )
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'nodeExists' has not been implemented yet." );
	}
	
	
	@Override
	public void removeNode()
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'removeNode' has not been implemented yet." );
	}
	
	
	@Override
	public String name()
	{
		throw new UnsupportedOperationException( "Method 'name' has not been implemented yet." );
	}
	
	
	@Override
	public String absolutePath()
	{
		throw new UnsupportedOperationException( "Method 'absolutePath' has not been implemented yet." );
	}
	
	
	@Override
	public boolean isUserNode()
	{
		throw new UnsupportedOperationException( "Method 'isUserNode' has not been implemented yet." );
	}
	
	
	@Override
	public String toString()
	{
		throw new UnsupportedOperationException( "Method 'toString' has not been implemented yet." );
	}
	
	
	@Override
	public void flush()
		throws BackingStoreException
	{
		//there is no need to flush anything on Windows!
	}
	
	
	@Override
	public void sync()
		throws BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'sync' has not been implemented yet." );
	}
	
	
	@Override
	public void addPreferenceChangeListener( PreferenceChangeListener pcl )
	{
		throw new UnsupportedOperationException( "Method 'addPreferenceChangeListener' has not been implemented yet." );
	}
	
	
	@Override
	public void removePreferenceChangeListener( PreferenceChangeListener pcl )
	{
		throw new UnsupportedOperationException( "Method 'removePreferenceChangeListener' has not been implemented yet." );
	}
	
	
	@Override
	public void addNodeChangeListener( NodeChangeListener ncl )
	{
		throw new UnsupportedOperationException( "Method 'addNodeChangeListener' has not been implemented yet." );
	}
	
	
	@Override
	public void removeNodeChangeListener( NodeChangeListener ncl )
	{
		throw new UnsupportedOperationException( "Method 'removeNodeChangeListener' has not been implemented yet." );
	}
	
	
	@Override
	public void exportNode( OutputStream os )
		throws IOException, BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'exportNode' has not been implemented yet." );
	}
	
	
	@Override
	public void exportSubtree( OutputStream os )
		throws IOException, BackingStoreException
	{
		throw new UnsupportedOperationException( "Method 'exportSubtree' has not been implemented yet." );
	}
	
}
