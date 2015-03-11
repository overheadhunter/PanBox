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
package org.panbox.mobile.android.gui.adapter;

import java.util.ArrayList;
import java.util.List;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.FileItem;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class FileItemAdapter extends ArrayAdapter<FileItem>{
	
	static class ViewHolder {	// use this static class to cache the view when getView method is called
		
		ImageView icon;
		TextView name;
		TextView desc;
	}
	
	public Context context;
	
	public int layoutResourceId;
	
	public ArrayList<FileItem> listItems= null;
	
	public FileItemAdapter(Context context, int resource, List<FileItem> objects) {
	
		super(context, resource, objects);
		
		this.listItems = (ArrayList<FileItem>) objects;
		
		this.layoutResourceId = resource;
		
		this.context = context;
		
	}
	/**
	 * this method is called implicitly to correctly display a view item in the listview
	 * We use ViewHolder object to reuse list items once displayed
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View row = convertView;
		
		ViewHolder holder = null;
		
		FileItem fileItem = listItems.get(position);
		
		if (row == null){	// if the ListView is empty, then we have to inflate the layout, otherwise use the existing lisview object
		
			row = ((Activity)context).getLayoutInflater().inflate(layoutResourceId, parent,false);
			
			holder = new ViewHolder();
			
			holder.icon = (ImageView)row.findViewById(R.id.pb_list_item_icon);
			
			holder.name = (TextView)row.findViewById(R.id.pb_list_item_name);
			
			holder.desc = (TextView)row.findViewById(R.id.pb_list_item_desc);
			
			row.setTag(holder);
		}
		
		else {
			
			holder = (ViewHolder)row.getTag();
		}
	
		holder.icon.setImageResource(fileItem.getItemTypeId());		// set an image to the file item
		
		holder.name.setText(fileItem.getName());
		if (position == 0  && fileItem.getItemTypeId() == R.drawable.ic_arrow_up){		// at the position 0 is always an navigation arrow, which is either enabled (in case non-root directory) or disabled (if we are in root)
				holder.desc.setText(context.getString(R.string.pb_to_parent_directory));
		}		
		else if (position == 0  && fileItem.getItemTypeId() == R.drawable.ic_arrow_up_disabled){
				holder.desc.setText(context.getString(R.string.pb_root_directory));
		}
		else if(fileItem.isDirectory()){
			
			String unit = "item";
			
			if( Integer.valueOf(fileItem.getNumbOfDirItems()) > 1 || Integer.valueOf(fileItem.getNumbOfDirItems()) == 0)
				
				unit += "s";
			
			holder.desc.setText(fileItem.getLastModified() + "     " + fileItem.getNumbOfDirItems() + " " + unit);
		
		}
		
		else
			
			holder.desc.setText(fileItem.getLastModified() + "     " + fileItem.getSize());
		
		return row;
	}
	

}
