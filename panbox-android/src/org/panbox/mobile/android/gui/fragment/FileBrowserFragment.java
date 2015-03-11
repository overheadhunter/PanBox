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
package org.panbox.mobile.android.gui.fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.panbox.PanboxConstants;
import org.panbox.core.Utils;
import org.panbox.core.crypto.AbstractObfuscatorFactory;
import org.panbox.core.crypto.AndroidObfuscatorFactory;
import org.panbox.core.crypto.CryptCore;
import org.panbox.core.crypto.KeyConstants;
import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.crypto.io.AESGCMRandomAccessFileCompat;
import org.panbox.core.crypto.io.EncRandomAccessInputStream;
import org.panbox.core.exception.FileEncryptionException;
import org.panbox.core.exception.FileIntegrityException;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.core.exception.SymmetricKeyDecryptionException;
import org.panbox.core.exception.SymmetricKeyNotFoundException;
import org.panbox.core.identitymgmt.AbstractIdentity;
import org.panbox.core.identitymgmt.IPerson;
import org.panbox.core.identitymgmt.PanboxContact;
import org.panbox.core.keymgmt.AndroidJDBCHelperNonRevokeable;
import org.panbox.core.keymgmt.EncryptedShareKey;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.keymgmt.Volume;
import org.panbox.core.sharemgmt.ShareManagerException;
import org.panbox.mobile.android.R;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualFile;
import org.panbox.mobile.android.gui.activity.FileBrowserActivity;
import org.panbox.mobile.android.gui.activity.ShareManagerActivity;
import org.panbox.mobile.android.gui.adapter.FileItemAdapter;
import org.panbox.mobile.android.gui.data.FileItem;
import org.panbox.mobile.android.gui.data.PanboxManager;
import org.panbox.mobile.android.utils.AndroidSettings;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileBrowserFragment extends Fragment implements
		FileBrowserActivity.TaskListener, OnItemClickListener {

	private final String TAG_CLASS = "FileBrowserFragment:";
	private final String TAG_GET_FILE = "GetFile:";
	private final String TAG_SYNC_SHARE_CONTENT = "SyncShareContent:";

	private final int ERROR_NOT_OWNER = 0x1;
	private final int ERROR_COULD_NOT_EXTRACT_KEYS = 0x2;
	private final int ERROR_COULD_NOT_FIND_FILE_ON_DISK = 0x3;
	private final int ERROR_DECRYPTING = 0x4;
	private final int ERROR_DEOBFUSCATING = 0x5;

	private int accessStatus = 0;

	private ProgressDialog progressDialog;
	private boolean isTaskRunning = false;
	private boolean isGetFileTaskRunning = false;
	private SyncShareContent shareContentTask;
	private GetFile getFileTask;

	protected Bundle bundle;
	private PanboxManager panbox;
	private AbstractIdentity identity;

	private View fragmentLayout;
	private LinearLayout infoBarLine1;
	private LinearLayout infoBarLine2;
	private LinearLayout infoBarContainer;
	private ListView mainLv = null;
	private LinearLayout updateButton;
	private FileItemAdapter adapter = null;
	private ArrayList<FileItem> shareContent;

	private ArrayList<DropboxVirtualFile> dbList;

	private Obfuscator obfuscator;
	private SecretKey obfuscationKey;
	private ShareKey shareKey;

	private String deviceName;
	private String shareName;
	protected boolean isItemClicked = false;

	private OnTouchListener onUpdateButtonListener;

	private String root;
	private String viewPath;
	private String path;

	private Context context;
	private AndroidSettings settings;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		onUpdateButtonListener = (OnTouchListener) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v("FileBrowserFragment:", "in onCreate()");
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		settings = AndroidSettings.getInstance(); // no need to check for
													// exceptions here. It is
													// already done in the
													// parent activity
		// we get bundle and generate volume only once and only when fragment is
		// created,
		// therefore they are initialized as long as the fragment lives
		// if fragment is destroyed as well as the activity while activity was
		// stopped, then need to conduct pairing again
		bundle = ((FileBrowserActivity) getActivity()).getBundleFromIntent();
		if (bundle != null) {
			Log.v("FileBrowserFragment:onCreate():",
					"shareName is: " + bundle.getString("chosenShare"));
			shareName = bundle.getString("chosenShare");
			root = File.separator + shareName;
			viewPath = bundle.getString("viewPath");
			path = bundle.getString("path");

			// mDBCon = ((FileBrowserActivity) getActivity()).getmDBCon();
			// volume = ((FileBrowserActivity) getActivity()).getVolume();
		} else {
			// // if bundle is zero, then the FileBrowserActivity was accessed
			// in other way than clicking a share item
			// // so we need to redirect the user to the ShareManagerActivity
			Log.v("FileBrowserFragment:onCreate()",
					"undocumented access path => redirect user to the ShareManagerActivity");
		}

	}

	public LinearLayout getUpdateButton() {
		return updateButton;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.v("FileBrowserFragment:", "in onCreateView()");

		// On orientation change the activity is destroyed and created again,
		// however the fragment is not destroyed and during activity creation
		// fragment's onCreateView is called
		if (!isGetFileTaskRunning) { // if this task is running, than per
										// definition the fragment is there as
										// well as listview

			context = getActivity();

			panbox = PanboxManager.getInstance(context);

			identity = panbox.getIdentity();

			deviceName = settings.getDeviceName();

			fragmentLayout = inflater.inflate(R.layout.pb_list_view, container,
					false);
			infoBarContainer = (LinearLayout) fragmentLayout
					.findViewById(R.id.pb_infobar_container);

			updateButton = (LinearLayout) fragmentLayout
					.findViewById(R.id.pb_update_container);
			this.updateButton.setClickable(true);
			this.updateButton.setOnTouchListener(this.onUpdateButtonListener);

			infoBarLine1 = (LinearLayout) inflater.inflate(
					R.layout.pb_infobar_line, container, false);
			infoBarLine2 = (LinearLayout) inflater.inflate(
					R.layout.pb_infobar_line, container, false);

			infoBarContainer.addView(infoBarLine1);
			infoBarContainer.addView(infoBarLine2);

			mainLv = (ListView) fragmentLayout.findViewById(R.id.pb_listview);
		}

		mainLv.setOnItemClickListener(this);
		return fragmentLayout;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v("FileBrowserFragment:", "in onActivityCreated()");
		super.onActivityCreated(savedInstanceState);

		if (bundle != null
				|| (bundle = ((FileBrowserActivity) getActivity())
						.getBundleFromIntent()) != null) {

			setInfoBarView();

			// If we are returning here from a screen orientation
			// and the AsyncTask is still working, re-create and display the
			// progress dialog.
			if (isTaskRunning) {
				progressDialog = ProgressDialog.show(getActivity(),
						getString(R.string.pb_loading),
						getString(R.string.pb_please_wait));
			} else {

				if (shareContent != null) {
					populateListView(shareContent);
				} else {
					shareContentTask = new SyncShareContent(this);
					shareContentTask.execute();
				}
			}
		} else {
			// if bundle is zero, then the FileBrowserActivity was accessed in
			// other way than clicking a share item
			// so we need to redirect the user to the ShareManagerActivity
			Log.v("FileBrowserFragment:onActivityCreated()",
					"undocumented access path => redirect user to the ShareManagerActivity");
		}
	}

	@Override
	public void onPreExecute() {
		Log.v("FileBrowserFragment:", "in onPreExecute()");
		isTaskRunning = true;
		progressDialog = ProgressDialog.show(getActivity(),
				getString(R.string.pb_loading),
				getString(R.string.pb_please_wait));
	}

	@Override
	public void onPostExecute() {
		Log.v("FileBrowserFragment:", "in onPostExecute()");
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		isTaskRunning = false;
	}

	@Override
	public void onDetach() {
		// All dialogs should be closed before leaving the activity in order to
		// avoid
		// the: Activity has leaked window com.android.internal.policy...
		// exception
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		super.onDetach();
	}

	public void populateListView(ArrayList<FileItem> shareContent) {
		Log.v("FileBrowserFragment:populateListView()",
				"setting adapter for listview...");
		adapter = new FileItemAdapter(getActivity(), R.layout.pb_list_item,
				shareContent); // at this step the objects to be mapped to views
								// are instantiated, so we can use our adapter
								// to convert them to views
		mainLv.setAdapter(adapter);
	}

	public void onItemClick(AdapterView<?> parentAdapter,
			android.view.View view, int position, long id) {

		String parent = path.substring(0, path.lastIndexOf("/"));
		String parentViewPath = File.separator;
		if (!viewPath.equals(File.separator)) {
			parentViewPath = viewPath.substring(0, viewPath.length() - 1);
			parentViewPath = parentViewPath.substring(0,
					parentViewPath.lastIndexOf("/") + 1);
		}
		Log.v("parent: ", parent);
		Log.v("parentViewPath: ", parentViewPath);

		if (position == 0) {
			if (path.equals(root)) {
				Toast.makeText(getActivity(),
						getString(R.string.pb_already_in_root_text),
						Toast.LENGTH_LONG).show();
			} else {
				path = parent;
				viewPath = parentViewPath; // path to the parent directory

				setInfoBarView();

				shareContentTask = new SyncShareContent(this);
				shareContentTask.execute();

			}
		} else { // position !=0

			DropboxVirtualFile file = dbList.get((int) (id - 1)); // id - 1 is
																	// needed
																	// because
																	// the
																	// "UptoParent"
																	// listview
																	// entry as
																	// the first
																	// entry in
																	// the
																	// listview
																	// is added

			String decName = shareContent.get((int) id).getName();

			String encName = file.getFileName();

			if (file.isDirectory()) { // go one level deeper

				path = path + File.separator + encName;
				viewPath += decName + File.separator;

				Log.v("position", String.valueOf(position));
				Log.v("path", path);
				Log.v("viewPath", viewPath);

				setInfoBarView();

				shareContentTask = new SyncShareContent(this);
				shareContentTask.execute();

			} else {
				// path points to the obfuscated file in the dropbox (it was
				// already deobfuscated here locally => decName),
				// now the obfuscated file needs to ne downloaded from the
				// dropbox and decrypted
				getFileTask = new GetFile(path, encName, decName, this);
				getFileTask.execute();
			}
		}
	}

	private void setInfoBarView() {
		addInfoBarLine(R.id.pb_infobar_container, getString(R.string.pb_share)
				+ ":\t\t\t", shareName, infoBarLine1);
		addInfoBarLine(R.id.pb_infobar_container, getString(R.string.pb_path)
				+ ":\t\t\t", viewPath, infoBarLine2);
	}

	/**
	 * 
	 * @param id
	 *            - id of the infobar container to inflate
	 * @param name
	 *            - name of the line
	 * @param value
	 *            - value of the line
	 * @param view
	 *            - LinearLayout - layout to which name and value textviews are
	 *            added
	 */
	public void addInfoBarLine(int id, String name, String value,
			LinearLayout infoBarLine) {

		TextView nameView = (TextView) infoBarLine
				.findViewById(R.id.pb_infobar_line_name);
		TextView valueView = (TextView) infoBarLine
				.findViewById(R.id.pb_infobar_value);
		nameView.setText(name);
		valueView.setText(value);
	}

	public ArrayList<FileItem> getShareContent() {
		return shareContent;
	}

	public void setShareContent(ArrayList<FileItem> shareContent) {
		this.shareContent = shareContent;
	}

	public String getShareName() {
		return shareName;
	}

	public void setShareName(String shareName) {
		this.shareName = shareName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	public String getViewPath() {
		return viewPath;
	}

	public void setViewPath(String viewPath) {
		this.viewPath = viewPath;
	}
	
	@Override
	public void onStart() {
		Log.v("FileBrowserFragment:", "in onStart()");
		super.onStart();
		if (bundle == null
				&& (bundle = ((FileBrowserActivity) getActivity())
						.getBundleFromIntent()) == null) { // need to retrieve
															// bundle also here,
															// because it is the
															// only method that
															// is called when
															// activity's
															// onNewIntent() is
															// called
			Intent shareManager = new Intent(getActivity(),
					ShareManagerActivity.class);
			startActivity(shareManager);
		}
	}

	@Override
	public void onPause() {
		Log.v("FileBrowserFragment:", "in onPause()");
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.v("FileBrowserFragment:", "in onStop()");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.v("FileBrowserFragment:", "in onDestroy()");
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private class GetFile extends AsyncTask<Void, Void, Boolean> {

		private String path;
		private String encName;
		private String decName;
		private FileBrowserActivity.TaskListener listener;

		private GetFile(String path, String encName, String decName,
				FileBrowserActivity.TaskListener listener) {
			this.path = path;
			this.encName = encName;
			this.decName = decName;
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.v(TAG_CLASS + TAG_GET_FILE, " in onPreExecute()");
			listener.onPreExecute();
			isGetFileTaskRunning = true;
		}

		@Override
		public void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			Log.v(TAG_CLASS + TAG_GET_FILE, " in onPostExecute()");
			listener.onPostExecute();
			isGetFileTaskRunning = false;

			if (accessStatus == ERROR_COULD_NOT_FIND_FILE_ON_DISK)
				Toast.makeText(context,
						getString(R.string.pb_could_not_find_file_on_disk),
						Toast.LENGTH_LONG).show();
			if (accessStatus == ERROR_DECRYPTING)
				Toast.makeText(context,
						getString(R.string.pb_could_not_decrypt),
						Toast.LENGTH_LONG).show();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			Log.v(TAG_CLASS + TAG_GET_FILE, "in doInBackground()");
			String baseDir;

			String state = Environment.getExternalStorageState();

			if (!state.equals(Environment.MEDIA_MOUNTED)) {

				Log.v(TAG_CLASS + TAG_GET_FILE,
						"No external storage available, store files in the internal memory");

				baseDir = settings.getConfDir();

			} else
				baseDir = Environment.getExternalStorageDirectory().getPath();

			File encryptedFile = new File(baseDir + path + File.separator
					+ encName);

			encryptedFile.getParentFile().mkdirs();

			if (encryptedFile.exists()) {
				encryptedFile.delete();
			}
			try {
				encryptedFile.createNewFile();
			} catch (IOException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE,
						"Error while creating a new file ");
				e.printStackTrace();
			}

			Log.v(TAG_CLASS + TAG_GET_FILE, "File successfully created:");

			panbox.getMyDBCon().downloadFile(path + File.separator + encName,
					encryptedFile.getPath());

			Log.v(TAG_CLASS + TAG_GET_FILE, "File downloaded:");

			try {

				AESGCMRandomAccessFileCompat rafc = AESGCMRandomAccessFileCompat
						.getInstance(encryptedFile, true); // rafc is then an
															// instance of
															// EncRandomAccessFile
				rafc.open();
				if ((shareKey = panbox.getCachedShareKey()) == null)
					throw new FileEncryptionException("The share key is not set");
				
				rafc.initWithShareKey(shareKey.key);
				EncRandomAccessInputStream encryptedInputStream = new EncRandomAccessInputStream(
						rafc); // creates an inputsream of encrypted file

				File realDecrypted = new File(baseDir + path + File.separator
						+ decName);

				realDecrypted.getParentFile().mkdirs();
				if (realDecrypted.exists()) {
					realDecrypted.delete();
				}

				try {
					realDecrypted.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}

				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(realDecrypted));

				byte[] decBytes = new byte[1024];

				while (encryptedInputStream.read(decBytes) != -1) {
					bos.write(decBytes);
					bos.flush();
				}
				bos.close();
				encryptedInputStream.close();
				Intent i = new Intent();
				i.setAction(Intent.ACTION_VIEW);
				String extension = MimeTypeMap.getFileExtensionFromUrl(Uri
						.fromFile(realDecrypted).toString());
				String mimetype = MimeTypeMap.getSingleton()
						.getMimeTypeFromExtension(extension);
				try {
					i.setDataAndType(Uri.fromFile(realDecrypted), mimetype);

					startActivity(i);
				} catch (NullPointerException e) {
					accessStatus = ERROR_COULD_NOT_FIND_FILE_ON_DISK;
					e.printStackTrace();
				} catch (ActivityNotFoundException e) {
					i.setDataAndType(Uri.fromFile(realDecrypted), "text/*");
					startActivity(i);
					e.printStackTrace();
				}
			} catch (FileIntegrityException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DECRYPTING;
				e.printStackTrace();
			} catch (FileEncryptionException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DECRYPTING;
				e.printStackTrace();
			} catch (IOException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DECRYPTING;
				e.printStackTrace();
			}

			return false;
		}
	}

	private class SyncShareContent extends AsyncTask<Void, Void, Boolean> {

		private FileBrowserActivity.TaskListener listener;

		public SyncShareContent(FileBrowserActivity.TaskListener listener) {
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, " in onPreExecute()");
			listener.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {

			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "in doInBackground()");
			System.err.println("Before filelist");
			long timeBefore = System.currentTimeMillis();

			ArrayList<DropboxVirtualFile> fileNameList = panbox.getMyDBCon()
					.listFiles(path, panbox.getVolume());

			AbstractObfuscatorFactory aof = null;
			try {
				aof = AbstractObfuscatorFactory
						.getFactory(AndroidObfuscatorFactory.class);
			} catch (ClassNotFoundException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DEOBFUSCATING;
				e.printStackTrace();
			} catch (InstantiationException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DEOBFUSCATING;
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DEOBFUSCATING;
				e.printStackTrace();
			} catch (Exception e) {
				Log.v(TAG_CLASS + TAG_GET_FILE, e.getMessage());
				accessStatus = ERROR_DEOBFUSCATING;
				e.printStackTrace();
			}

			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "path: " + path);
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "shareName: " + shareName);
			if (path.equals(File.separator + shareName)) { // if we are not in
															// root directory
															// any more, then if
															// share folder was
															// clicked, in this
															// case parent
															// points to root
															// dir

				IPerson owner = checkOwnership();
				if (owner != null) { // OK, user is an owner of the share, start
										// extracting obfuscation and share keys
					Log.v("FileBrowserFragment:SyncShareContent:doInBackground()",
							"user is the owner of the share");
					extractKeys(owner);
					// extractKeys(); // a mockup function. use if only keys are
					// needed without extracting them from the pbmeta.db

				} else { // this user is not an owner of the share

					// TODO if this identity is not the owner of the share, then
					// this identity should be in possession of necessary
					// parameters to initialize the VolumeParams object
					// which in turn is used to instantiate the sharemetadata
					// object
					// accessStatus = ERROR_NOT_OWNER;
					Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT
							+ "doInBackground()",
							"user is not the owner of the share");
					accessStatus = ERROR_NOT_OWNER;

					return false;
				}

				deobfuscateFiles(fileNameList, aof);

			} else { // we have clicked a folder in the share

				deobfuscateFiles(fileNameList, aof);

				long timeAfter = System.currentTimeMillis();

				System.err.println("Files in dir: " + fileNameList.size()
						+ " time needed for deobfuscation: "
						+ (timeAfter - timeBefore));
			}

			return true;
		}

		private IPerson checkOwnership() {
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "in checkOnwership()");
			IPerson owner = null;

			try {

				byte[] ownerFp; // digest of this identity
				String metaDataDir = path + File.separator
						+ PanboxConstants.PANBOX_SHARE_METADATA_DIRECTORY
						+ File.separator;
				MessageDigest md;
				try {
					md = MessageDigest.getInstance(
							// throws no such algo exception
							KeyConstants.PUBKEY_FINGERPRINT_DIGEST,
							KeyConstants.PROV_BC);

				} catch (NoSuchProviderException e1) {
					// TODO Auto-generated catch block
					throw new ShareManagerException(
							"Error initializing message-digest!", e1);
				}

				ownerFp = new byte[md.getDigestLength()]; // this will hold the
															// owner's digest
															// read from the
															// owner.pbox file

				BufferedInputStream bis = new BufferedInputStream(
						panbox.getMyDBCon()
								.downloadFileStream(
										metaDataDir
												+ PanboxConstants.PANBOX_SHARE_OWNER_FILE));

				bis.read(ownerFp);

				bis.close();

				byte[] c = null;
				if (identity.getCertSign() != null)
					c = md.digest(identity.getCertSign().getPublicKey()
							.getEncoded());

				if (Arrays.equals(ownerFp, c)) {
					owner = identity;
				} else {
					for (PanboxContact contact : identity.getAddressbook()
							.getContacts()) {
						if (contact.getCertSign() != null)
							c = md.digest(contact.getCertSign().getPublicKey()
									.getEncoded());
						md.reset();
						if (Arrays.equals(ownerFp, c)) {
							owner = contact;

							break;
						}

					}
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} catch (ShareManagerException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return owner;
		}

		private void extractKeys(IPerson owner) {
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "in extractKeys()");
			try {

				PublicKey publicKeyForDevice = identity
						.getPublicKeyForDevice(deviceName);

				PrivateKey privateKeyForDevice = identity
						.getPrivateKeyForDevice(deviceName);

				String destination = settings.getConfDir() + File.separator
						+ Volume.SPL_FILE;
				String source = path + File.separator + ".panbox"
						+ File.separator + Volume.SPL_FILE;

				MessageDigest md = null;
				String fingerprint = null;
				try {
					md = MessageDigest.getInstance("SHA-256");
					fingerprint = Utils.bytesToHex(md.digest(identity
							.getPublicKeySign().getEncoded()));
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e);
				}

				String destinationDL = settings.getConfDir() + File.separator
						+ fingerprint + ".db";
				String sourceDL = path + File.separator + ".panbox"
						+ File.separator + fingerprint + ".db";

				Log.v(Volume.SPL_FILE + " download destination:", destination);

				boolean success = false;

				if (panbox.getMyDBCon().downloadFile(source, destination)) {
					Log.v(Volume.SPL_FILE,
							"downloaded, size: "
									+ String.valueOf((new File(destination))
											.getTotalSpace()));
					if (panbox.getMyDBCon().downloadFile(sourceDL,
							destinationDL)) {
						Log.v(fingerprint,
								"downloaded, size: "
										+ String.valueOf((new File(
												destinationDL)).getTotalSpace()));
						success = true;
					} else {
						Log.v(fingerprint, "fail to download");
					}
				} else {
					Log.v(Volume.SPL_FILE, "fail to download");
				}

				if (!success) {
					accessStatus = ERROR_COULD_NOT_EXTRACT_KEYS;
					return;
				}

				Volume vol = new Volume(
						new AndroidJDBCHelperNonRevokeable(destination,
								destinationDL, identity.getPublicKeySign()));

				vol.loadShareMetaData((owner != null ? owner.getCertSign()
						.getPublicKey() : null));

				obfuscationKey = CryptCore.decryptSymmertricKey(
						vol.getEncryptedObfuscationKey(publicKeyForDevice),
						privateKeyForDevice);
				panbox.setCachedObfuscationKey(obfuscationKey);

				EncryptedShareKey encryptedShareKey = vol
						.getLatestEncryptedShareKey(publicKeyForDevice);

				SecretKey secretKey = CryptCore.decryptSymmertricKey(
						encryptedShareKey.encryptedKey, privateKeyForDevice);

				shareKey = new ShareKey(secretKey, encryptedShareKey.version);
				panbox.setCachedShareKey(shareKey);

			} catch (UnrecoverableKeyException e) {
				Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, e.getMessage());
				accessStatus = ERROR_COULD_NOT_EXTRACT_KEYS;
				e.printStackTrace();
			} catch (SymmetricKeyDecryptionException e) {
				Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, e.getMessage());
				accessStatus = ERROR_COULD_NOT_EXTRACT_KEYS;
				e.printStackTrace();
			} catch (SymmetricKeyNotFoundException e) {
				Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, e.getMessage());
				accessStatus = ERROR_COULD_NOT_EXTRACT_KEYS;
				e.printStackTrace();

			} catch (ShareMetaDataException e) {
				Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, e.getMessage());
				accessStatus = ERROR_COULD_NOT_EXTRACT_KEYS;
				e.printStackTrace();
			}

		}

		/**
		 * This method is used to deobfuscate filenames using the previously
		 * obtained obfuscationKey
		 * 
		 * @param fileNameList
		 * @param aof
		 */
		private void deobfuscateFiles(
				ArrayList<DropboxVirtualFile> fileNameList,
				AbstractObfuscatorFactory aof) {
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, "in deobfuscateFiles()");
			String deobfuscated;
			String modified;

			if (accessStatus != ERROR_COULD_NOT_EXTRACT_KEYS
					&& accessStatus != ERROR_NOT_OWNER) {
				try {
					dbList = new ArrayList<DropboxVirtualFile>();
					obfuscator = ((AndroidObfuscatorFactory) aof).getInstance(
							path, shareName, panbox.getMyDBCon(), context);
					shareContent = new ArrayList<FileItem>();

				} catch (ObfuscationException e) {
					Log.e("FileBrowserFragment",
							"Failed to get AndroidObfuscatorFactory.");
					return;
				}

				for (DropboxVirtualFile dbf : fileNameList) {
					try {

						if (!dbf.getFileName().endsWith("~")
								&& !dbf.getFileName().equals(".panbox")
								&& !dbf.getFileName().equals(".directory")
								&& !dbf.getFileName().equals(".dropbox")) {

							deobfuscated = obfuscator.deObfuscate(
									dbf.getFileName(), obfuscationKey);
							modified = panbox.getMyDBCon().getFileInfo(
									dbf.getPath()).modified;

							shareContent.add(new FileItem(deobfuscated, dbf
									.getPath(), modified.substring(0,
									modified.lastIndexOf("+")),
									panbox.getMyDBCon().getFileInfo(
											dbf.getPath()).size, dbf
											.isDirectory() ? String
											.valueOf((new DropboxVirtualFile(
													dbf.getPath(), panbox
															.getVolume()))
													.list().length) : "", dbf
											.isDirectory()));
							dbList.add(dbf);
						}

					} catch (ObfuscationException e) {
						Log.v("FileBrowserFragment",
								"Could not deobfuscate file. Will ignore this one: "
										+ dbf.getFileName());
					}
				}
				if (path.equals(root)) {
					shareContent.add(0, new FileItem("/", path, "", "", "0",
							true)); // no way up, because already
									// in the root
				}

				else {
					shareContent.add(0, new FileItem("..", path, "", "", "0",
							true)); // still can go at least
									// one level up
				}
			}
		}

		@Override
		public void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			Log.v(TAG_CLASS + TAG_SYNC_SHARE_CONTENT, " in onPostExecute()");
			panbox = PanboxManager.getInstance(context); // TODO:Here need to
															// make sure that
															// the context is
															// available

			if (accessStatus == ERROR_NOT_OWNER) {
				Toast.makeText(context,
						getString(R.string.pb_not_share_onwer_text),
						Toast.LENGTH_LONG).show();
				Intent sharesActivity = new Intent(context,
						ShareManagerActivity.class);
				startActivity(sharesActivity);
			} else if (accessStatus == ERROR_COULD_NOT_EXTRACT_KEYS) {
				Toast.makeText(context,
						getString(R.string.pb_obtain_credentials),
						Toast.LENGTH_LONG).show();
				Intent sharesActivity = new Intent(context,
						ShareManagerActivity.class);
				startActivity(sharesActivity);
			} else if (accessStatus == ERROR_DEOBFUSCATING) {
				Toast.makeText(context,
						getString(R.string.pb_could_not_deobfuscate),
						Toast.LENGTH_LONG).show();
				// Intent sharesActivity = new Intent(context,
				// ShareManagerActivity.class);
				// startActivity(sharesActivity);
			} else if (shareContent != null)
				populateListView(shareContent);

			listener.onPostExecute();

		}
	}
}
