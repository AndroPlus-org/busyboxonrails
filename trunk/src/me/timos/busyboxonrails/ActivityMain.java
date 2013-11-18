package me.timos.busyboxonrails;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import me.timos.busyboxonrails.R;

public class ActivityMain extends Activity {

	public void OnCancel(View v) {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void OnNormalInstall(View v) {
		startService(new Intent(this, ServiceNormalInstall.class));
		finish();
	}

	public void OnRecoveryInstall(View v) {
		startService(new Intent(this, ServiceRecoveryInstall.class));
		finish();
	}

}
