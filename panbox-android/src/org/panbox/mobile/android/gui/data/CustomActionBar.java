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

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.activity.CustomActionBarActivity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CustomActionBar extends LinearLayout{
	
	public static int count = 0; 
	
	private LayoutInflater myLayoutInflater;
	
	private LinearLayout icContainer;	// Custom Actionbar Icon Container

	public CustomActionBar() {
		super(null);
	}
	
	/**
	 * Use this constructor to place the custom actionbar as a custom view
	 * @param context
	 */
	public CustomActionBar(Context context) {
		
		super(context);
		
		count++;
				
		myLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		icContainer = (LinearLayout)myLayoutInflater.inflate(R.layout.pb_actionbar, this, true);
	
		Log.v("CustomActionBar:", "cActionbar instantiated:" + count + " times");
	}

	/**
	 * Use this constructor when instantiating from the menu
	 * @param context
	 */

	public CustomActionBar(Context context, LinearLayout icContainer) {
		
		super(context);
				
		myLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		this.icContainer = icContainer;
		
		Log.v("custom actionbar", "instantiated");
		
	}
	/**
	 * This method is used to add icon items to the actionbar container
	 * If you want to add a control such as progress bar, then use overloaded method, that takes only a layout of the control
	 * 
	 * Parameters of the method:
	 * 
	 * @param iconResourceID	- id of the icon
	 * @param layout			- an xml layout of an item to be added to the container
	 * @param labelID			- id of the label that has to be shown together with the icon. Use 0 to point that no text must be shown
	 */
	public View addActionItem(int iconResourceID, int layout, int labelID, OnTouchListener listener, CustomActionBarActivity.ITEMS id){
		
		//Log.v("tag111", String.valueOf(iconNumber));	
		
		View item = myLayoutInflater.inflate(layout, null);	// inflate xml layout for item to place the inflated view-object in container
		
		ImageView itemIcon= (ImageView)item.findViewById(R.id.pb_item_icon);	// obtain handle to the icon of item
				
		itemIcon.setImageResource(iconResourceID);	// iconResourceID is the ID of the image
		
		if(labelID != 0){
					
			TextView itemText = (TextView)item.findViewById(R.id.pb_item_text);
				
			itemText.setText(labelID);
		}
		
		item.setOnTouchListener(listener);
		
		item.setId(id.getNumVal());	// use this id in onTouch to differ what icon was clicked
	
		icContainer.addView(item);	// add item to container
		
		return item;
		
	}

		public LinearLayout getIcContainer() {
		return icContainer;
	}
	public void setIcContainer(LinearLayout actbarIcContainer) {
		this.icContainer = actbarIcContainer;
	}
}
