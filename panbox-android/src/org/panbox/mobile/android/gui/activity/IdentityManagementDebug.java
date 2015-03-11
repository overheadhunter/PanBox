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
import org.panbox.mobile.android.identitymgmt.IdentityDebugApp;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class IdentityManagementDebug extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
//	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.panbox_contact_test);

		final IdentityDebugApp debug = new IdentityDebugApp(this,
				getContentResolver());

		Button createAccount = (Button) findViewById(R.id.createAccountBtn);
		Button deleteAccount = (Button) findViewById(R.id.deleteAccountBtn);

		createAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.createPanboxAccount();
			}
		});

		deleteAccount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.deletePanboxAccount();
			}
		});

		Button addContact = (Button) findViewById(R.id.addContactBtn);
		Button delContact = (Button) findViewById(R.id.delContactBtn);

		addContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.addContactTest();
			}
		});

		delContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.deleteContactsTest();
			}
		});

		Button createIdentity = (Button) findViewById(R.id.createIdentityBtn);
		Button delIdentity = (Button) findViewById(R.id.deleteIdentityBtn);

		createIdentity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.createIdentity();
			}
		});

		delIdentity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.deleteIdentity();
			}
		});

		Button loadIdentity = (Button) findViewById(R.id.loadIdentityBtn);
		
		loadIdentity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.loadIdentityTest();
			}
		});
		
		Button exportContacts = (Button) findViewById(R.id.exportContactsButton);
		
		exportContacts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.exportAddressbook();
			}
		});
		
		Button importContacts = (Button) findViewById(R.id.importContactsBtn);
		
		importContacts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.importContacts();
			}
		});

		// old panbox client for dropbox tests
		// setContentView(R.layout.activity_panbox_main);
		//
		// // Create the adapter that will return a fragment for each of the
		// three
		// // primary sections of the app.
		// mSectionsPagerAdapter = new SectionsPagerAdapter(
		// getSupportFragmentManager());
		//
		// // Set up the ViewPager with the sections adapter.
		// mViewPager = (ViewPager) findViewById(R.id.pager);
		// mViewPager.setAdapter(mSectionsPagerAdapter);

	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.panbox_main, menu);
//		return true;
//	}
//
//	/**
//	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
//	 * one of the sections/tabs/pages.
//	 */
//	public class SectionsPagerAdapter extends FragmentPagerAdapter {
//
//		public SectionsPagerAdapter(FragmentManager fm) {
//			super(fm);
//		}
//
//		@Override
//		public Fragment getItem(int position) {
//			// getItem is called to instantiate the fragment for the given page.
//			// Return a DummySectionFragment (defined as a static inner class
//			// below) with the page number as its lone argument.
//			Fragment fragment = new DummySectionFragment();
//			Bundle args = new Bundle();
//			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
//			fragment.setArguments(args);
//			return fragment;
//		}
//
//		@Override
//		public int getCount() {
//			// Show 3 total pages.
//			return 3;
//		}
//
//		@Override
//		public CharSequence getPageTitle(int position) {
//			Locale l = Locale.getDefault();
//			switch (position) {
//			case 0:
//				return getString(R.string.title_section1).toUpperCase(l);
//			case 1:
//				return getString(R.string.title_section2).toUpperCase(l);
//			case 2:
//				return getString(R.string.title_section3).toUpperCase(l);
//			}
//			return null;
//		}
//	}
//
//	/**
//	 * A dummy fragment representing a section of the app, but that simply
//	 * displays dummy text.
//	 */
//	public static class DummySectionFragment extends Fragment {
//		/**
//		 * The fragment argument representing the section number for this
//		 * fragment.
//		 */
//		public static final String ARG_SECTION_NUMBER = "section_number";
//
//		public DummySectionFragment() {
//		}
//
//		@Override
//		public View onCreateView(LayoutInflater inflater, ViewGroup container,
//				Bundle savedInstanceState) {
//			View rootView = inflater.inflate(
//					R.layout.fragment_panbox_main_dummy, container, false);
//			TextView dummyTextView = (TextView) rootView
//					.findViewById(R.id.section_label);
//			dummyTextView.setText(Integer.toString(getArguments().getInt(
//					ARG_SECTION_NUMBER)));
//			return rootView;
//		}
//	}

}
