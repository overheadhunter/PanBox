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
package org.panbox.mobile.android.gui.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.panbox.mobile.android.R;

public class FileItem {
	
	private String name;
	private String lastModified;
	private String fullPath;
	private String numbOfDirItems;
	private String size;
	private boolean isDirectory = false;

	/**
	 * 
	 * @param name - name of the item 
	 * @param fullPath	- full path to the item
	 * @param desc	- description of the item (if directory then amount of items in it)
	 * @param size	- size of a file in bytes
	 * @param lastModified	- date of last modification
	 * @param isDirectory	- is this file item a directory
	 */
	
	public FileItem(String name, 
					String fullPath, 
					String lastModified, 
					String size,
					String numbOfDirItems,
					boolean isDirectory)
	{
		this.name = name;
		this.fullPath = fullPath;
		this.lastModified = lastModified;
		this.isDirectory = isDirectory;
		this.numbOfDirItems = numbOfDirItems;
		this.size = size;	
	}
	
	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastUpdate) {
		this.lastModified = lastUpdate;
	}
	public void setCurrentDate(){
				
		this.lastModified = (new SimpleDateFormat("dd.MM.yy , hh:mm:ss")).format(new Date()); 
	}
	public String getExtension(){
		String regex = "(.*)\\.(.*)";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(this.getName());
		if(m.find())
			return m.group(2);
		return "";
	}
	public int getItemTypeId() {
		if (this.isDirectory){
			
			if(this.name.equals(".."))
				return R.drawable.ic_arrow_up;	// the first entry in the list is to go a level up in the file system
			else if (this.name.equals("/"))
				return R.drawable.ic_arrow_up_disabled;	// the first entry in the list, has this icon if we are in the root of the filesystem
			else
				return R.drawable.ic_directory_icon;
		}
		
		
		String ext;
		// TODO: Hier sollten wir uns noch was ueberlegen, ob man nicht irgendwo her einen ganzen Satz items bekommen koennte
		if(!(ext=this.getExtension()).equals("")){
			if(ext.equals("pdf"))
				return R.drawable.ic_pdf;
			if(ext.equals("zip"))
				return R.drawable.ic_zip;
			if(ext.equals("txt"))
				return R.drawable.ic_txt;
			if(ext.equals("mp3"))
				return R.drawable.ic_launcher;
		}
		
		return R.drawable.ic_unknown;
	}
	public String getFullPath() {
		return fullPath;
	}
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	public String getNumbOfDirItems() {
		return numbOfDirItems;
	}
	public void setNumbOfDirItems(String numbOfDirItems) {
		this.numbOfDirItems = numbOfDirItems;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
}
