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

import java.util.Locale;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class StartActivity extends Activity {

	// Splash screen timer
    private static int SPLASH_TIME_OUT = 3000;
    private PanboxManager panbox; 
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		AndroidSettings settings = AndroidSettings.initSettings(getPreferences(MODE_PRIVATE));
        
        setContentView(R.layout.pb_splash_screen);
        context = getApplicationContext();

		Locale locale = settings.getLocale();
		Locale.setDefault(locale);
		Configuration config = context.getApplicationContext().getResources().getConfiguration();
		config.locale = locale;
		context.getApplicationContext().getResources().updateConfiguration(config, null);
        
        panbox = PanboxManager.getInstance(context);
    	if (panbox.getIdentity() == null){
    
    		new Handler().postDelayed(new Runnable() {
    		 	
                @Override
                public void run() {
                    // This method will be executed once the timer is over
                    // Start the appropriate activity
                    Intent i = new Intent(StartActivity.this, AssistentActivity.class);
                    startActivity(i);
     
                    // close this activity
                    finish();
                }
            }, SPLASH_TIME_OUT);
     
    	}
    	else if (AndroidSettings.getInstance().getDropboxAuthToken().equals("")){
    		Log.v("StartActivity:", "the accessToken is not set -> load SettingsActivity");
    		new Handler().postDelayed(new Runnable() {
    		 	
                @Override
                public void run() {
                    // This method will be executed once the timer is over
                    // Start the appropriate activity
                    Intent i = new Intent(StartActivity.this, SettingsActivity.class);
                    startActivity(i);
     
                    // close this activity
                    finish();
                }
            }, SPLASH_TIME_OUT);
    	}else {
    		
    		new Handler().postDelayed(new Runnable() {
    		 	
                @Override
                public void run() {
                    // This method will be executed once the timer is over
                    // Start the appropriate activity
                    Intent i = new Intent(StartActivity.this, ShareManagerActivity.class);
                    startActivity(i);
     
                    // close this activity
                    finish();
                }
            }, SPLASH_TIME_OUT);
    	}
        
    }
	

}
