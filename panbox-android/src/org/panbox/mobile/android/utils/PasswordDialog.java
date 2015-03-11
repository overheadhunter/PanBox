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
package org.panbox.mobile.android.utils;

import org.panbox.mobile.android.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
public class PasswordDialog extends DialogFragment{

	public static interface OnCompleteListener {
	    public abstract void onComplete(Bundle b);
	}
		private OnCompleteListener listener;
		private String password;
		
		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.listener = (OnCompleteListener)activity;
		}
		private EditText pwEditText;
		private LinearLayout dialogView;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
		}
	    @SuppressLint("InflateParams")
		@Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        // Get the layout inflater
	        
	        
	        // Inflate and set the layout for the dialog
	        // Pass null as the parent view because its going in the dialog layout
	    	LayoutInflater inflater = getActivity().getLayoutInflater();
	    	dialogView = (LinearLayout)inflater.inflate(R.layout.pb_pairing_password_dialog,null);
			pwEditText = (EditText)dialogView.findViewById(R.id.pairing_pass_edittext);
	       builder.setView(dialogView);
	       builder.setTitle(getActivity().getString(R.string.password_hint_text));
	        // Add action buttons
	       builder.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (which == DialogInterface.BUTTON_POSITIVE) {
										password = pwEditText.getText().toString();
										if (!password.equals("")) {
											Bundle b = new Bundle();
											b.putString("password", password);
											listener.onComplete(b); 
										} else {
											Toast.makeText(
													getActivity(),
													getString(R.string.pb_password_required),
													Toast.LENGTH_LONG).show();
										}
									dialog.dismiss();
									}
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();

								}
							});
	        return builder.create();

	    }
}
