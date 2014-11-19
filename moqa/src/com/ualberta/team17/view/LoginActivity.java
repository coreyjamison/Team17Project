package com.ualberta.team17.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Browser;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ualberta.team17.R;
import com.ualberta.team17.controller.QAController;
import com.ualberta.team17.datamanager.UserContext;

public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		TextView tv = (TextView) findViewById(R.id.loginWarning);
		if (tv != null) {
			tv.setVisibility(View.INVISIBLE);
		}
		
		Button loginButton = (Button) findViewById(R.id.signInButton);
		if (loginButton != null) {
			loginButton.setOnClickListener( new OnClickListener() {

	            @Override
	            public void onClick(View v) {
	                LoginActivity.this.login();
	            }
	        });
		}
		
		Button browseButton = (Button) findViewById(R.id.browseButton);
		if (browseButton != null) {
			browseButton.setOnClickListener( new OnClickListener() {

	            @Override
	            public void onClick(View v) {
	                browse();
	            }
	        });
		}
		
		Button askButton = (Button) findViewById(R.id.askButton);
		if (askButton != null) {
			askButton.setOnClickListener( new OnClickListener() {

	            @Override
	            public void onClick(View v) {
	                ask();
	            }
	        });
		}
	}
	
	/**
	 * This function creates the user context, logging the user in in the process. 
	 * The user is passed to the TaxonomyActivity.
	 * 
	 * @author Jared
	 */
	private void login() {
		String username;
		
		
		EditText usernameET = (EditText) findViewById(R.id.usernameText);
		if (usernameET != null) {
			username = usernameET.getText().toString();

			if (username.length() >= 4 && username.length() <= 20 && !username.contains(" ")) {
				// Create the user context.
				QAController.getInstance().login(new UserContext(username));	
				Button bButton = (Button) findViewById(R.id.browseButton);
				Button aButton = (Button) findViewById(R.id.askButton);
				EditText et = (EditText) findViewById(R.id.usernameText);
				Button lButton = (Button)findViewById(R.id.signInButton);
				
				et.setVisibility(View.INVISIBLE);
				lButton.setVisibility(View.INVISIBLE);
				bButton.setVisibility(View.VISIBLE);
				aButton.setVisibility(View.VISIBLE);
				
			}
			else {
				TextView tv = (TextView) findViewById(R.id.loginWarning);
				if (tv != null) {
					tv.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	private void browse() {
		Intent intent = new Intent(LoginActivity.this, QuestionListActivity.class);
		startActivity(intent);
	}
	
	private void ask() {
		Intent intent = new Intent(LoginActivity.this, QuestionViewActivity.class);		
		startActivity(intent);
	}
}
