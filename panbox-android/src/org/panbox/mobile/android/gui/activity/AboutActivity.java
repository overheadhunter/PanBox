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
package org.panbox.mobile.android.gui.activity;

import org.panbox.mobile.android.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
public class AboutActivity extends CustomActionBarActivity{
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("AboutActivity:", "in onCreate()");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.pb_about);
		
		updateActionbarBehaviour();
	}
	
	public void openBrowser(View v){
		String url = v.getTag().toString();
		Intent browser = new Intent(Intent.ACTION_VIEW);
		browser.setData(Uri.parse(url));
		startActivity(browser);
	}

	@Override
	public void updateActionbarBehaviour() {

		Log.v("CustomActionBarActivity:AboutActivity:","in updateActionbarBehaviour()");
		
		if(settings.isDropboxAuthTokenSet()){
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.SHAREMANAGER.getNumVal()).setEnabled(true);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal()).setEnabled(true);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.NFC.getNumVal()).setEnabled(true);
		}
		else{
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.SHAREMANAGER.getNumVal()).setEnabled(false);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.FILEBROWSER.getNumVal()).setEnabled(false);
			customActionbar.getIcContainer().getChildAt(CustomActionBarActivity.ITEMS.NFC.getNumVal()).setEnabled(false);
		}
	}
}
