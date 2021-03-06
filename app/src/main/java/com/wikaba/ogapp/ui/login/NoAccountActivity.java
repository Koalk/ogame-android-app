/*
	Copyright 2015 Kevin Le Perf

	This file is part of Ogame on Android.

	Ogame on Android is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Ogame on Android is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Ogame on Android.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wikaba.ogapp.ui.login;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityManagerCompat;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.wikaba.ogapp.AgentActions;
import com.wikaba.ogapp.AgentService;
import com.wikaba.ogapp.R;
import com.wikaba.ogapp.database.AccountsManager;
import com.wikaba.ogapp.events.OnAgentUpdateEvent;
import com.wikaba.ogapp.ui.main.HomeActivity;
import com.wikaba.ogapp.utils.AccountCredentials;
import com.wikaba.ogapp.utils.FragmentStackManager;
import com.wikaba.ogapp.utils.SystemFittableActivity;
import com.wikaba.ogapp.views.ButtonRectangle;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * Created by kevinleperf on 03/07/15.
 */
public class NoAccountActivity extends SystemFittableActivity {

	private MaterialDialog _login_progress;

	@Bind(R.id.uniSelect)
	protected Spinner uniSpinner;

	@Bind(R.id.username)
	protected EditText usernameField;

	@Bind(R.id.password)
	protected EditText passwdField;

	@Bind(R.id.lang)
	protected EditText langField;

	@Bind(R.id.login)
	protected ButtonRectangle loginButton;

	@Bind(R.id.pw_checkbox)
	protected CheckBox pwCheckBox;

	@Bind(R.id.existingAccList)
	protected Spinner existingAccs;

	@Bind(R.id.parent)
	protected FrameLayout parentFrame;

	private ArrayList<AccountCredentials> existingAccountsCredentials;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		unsetInsets();
		//Does bind need to be called here if we're calling it in the parent class?
		ButterKnife.bind(this);

		String[] uniNames = getResources().getStringArray(R.array.universe_names);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, uniNames);
		uniSpinner.setAdapter(adapter);

		String account_spinner = getString(R.string.account_spinner);
		existingAccountsCredentials = AccountsManager.getInstance().getAllAccountCredentials();
		String[] accountNames = new String[existingAccountsCredentials.size() + 1];
		accountNames[0] = getString(R.string.select_an_account);
		int i = 1;
		for (AccountCredentials cred : existingAccountsCredentials) {
			accountNames[i] = String.format(account_spinner, cred.getUsername(), cred.getUniverse());
			i++;
		}
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountNames);
		existingAccs.setAdapter(adapter);
		existingAccs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				position -= 1;

				if (position >= 0) {
					AccountCredentials cred = existingAccountsCredentials.get(position);
					//TODO ?
					loginToAccount(cred);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		boolean isLowRam = ActivityManagerCompat.isLowRamDevice(am);
		if(!isLowRam) {
			//Load the background image
			ImageView img = new ImageView(this);
			img.setScaleType(ImageView.ScaleType.CENTER_CROP);
			ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
			);
			img.setLayoutParams(layoutParams);
			img.setImageResource(R.drawable.nasa_hubble);
			parentFrame.addView(img, 0);
		}
	}

	@Override
	public int getContentView() {
		return R.layout.activity_no_account;
	}

	@Override
	protected FragmentStackManager getFragmentStackManager() {
		return new FragmentStackManager(this, 0) {
			@Override
			public void pop() {

			}

			@Override
			public void push(int new_index, Bundle arguments) {

			}

			@Override
			public boolean isMainView() {
				return true;
			}

			@Override
			public boolean navigationBackEnabled() {
				return false;
			}

			@Override
			public boolean isNavigationDrawerEnabled() {
				return false;
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		EventBus.getDefault().register(this);

		Intent service = new Intent(this, AgentService.class);
		startService(service);
	}

	@Override
	public void onPause() {
		EventBus.getDefault().unregister(this);
		dismissLogin();
		super.onPause();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.remove) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int rowPosition = info.position;
			AccountCredentials creds = existingAccountsCredentials.get(rowPosition);

			AccountsManager dbmanager = AccountsManager.getInstance();
			dbmanager.removeAccount(creds.getUniverse(), creds.getUsername());
			existingAccountsCredentials.remove(rowPosition);

			//TODO NOTIFY SPINNER CHANGE
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@OnClick(R.id.login)
	public void onLoginClicked() {
		String username = usernameField.getText().toString().trim();
		String passwd = passwdField.getText().toString().trim();
		String lang = langField.getText().toString().trim();
		if (lang == null || lang.length() == 0) {
			lang = "en";
		}
		View selectedView = uniSpinner.getSelectedView();
		if (selectedView == null || username.length() == 0 || passwd.length() == 0) {
			//TODO SHOW SNACKBAR
			return;
		}

		TextView selectedText = (TextView) selectedView;
		String universe = selectedText.getText().toString();

		AccountCredentials acc = new AccountCredentials();
		acc.setUniverse(universe);
		acc.setUsername(username);
		acc.setPasswd(passwd);
		acc.setLang(lang);
		//TODO: Replace screen with ProgressBar
		loginToAccount(acc);
	}

	private void loginToAccount(AccountCredentials credentials) {
		Intent i = new Intent(this, AgentService.class);
		i.putExtra(AgentActions.ACCOUNT_CREDENTIAL_KEY, credentials);
		i.putExtra(AgentActions.OGAME_AGENT_KEY, credentials.getId());
		i.putExtra(AgentActions.AGENT_ACTION_KEY, AgentActions.LOGIN);
		startService(i);
	}

	@OnClick(R.id.pw_checkbox)
	public void onPasswordCheckBoxClicked() {
		int inputType = (pwCheckBox.isChecked()) ?
				(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
				: (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passwdField.setInputType(inputType);
	}

	@Subscribe(threadMode = ThreadMode.MainThread, sticky = true)
	public void onAgentUpdated(OnAgentUpdateEvent event) {
		EventBus.getDefault().removeStickyEvent(event);
		boolean loggedIn = event.getWasSuccessful();
		if(!loggedIn) {
			//TODO: Return state of the activity to initial state (login fields ready)
			//TODO: Send toast saying we were unable to log in.
			return;
		}

		long agentKey = event.getAgentManagerKey();
		Intent i = new Intent(this, HomeActivity.class);
		i.putExtra(HomeActivity.ACCOUNT_KEY, agentKey);
		i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(i);
	}

	private void dismissLogin() {
		if (_login_progress != null && _login_progress.isShowing()) {
			_login_progress.dismiss();
		}
		_login_progress = null;
	}
}
