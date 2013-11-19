package me.timos.busyboxonrails;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;

public abstract class FragmentAsyncTask<Params, Progress, Result> extends
		Fragment {

	private static class InternalAsyncTask<X, Y, Z> extends AsyncTask<X, Y, Z> {

		private FragmentAsyncTask<X, Y, Z> mFragment;

		public InternalAsyncTask(FragmentAsyncTask<X, Y, Z> f) {
			mFragment = f;
		}

		@Override
		protected Z doInBackground(X... params) {
			return mFragment.doInBackground(params);
		}

		@Override
		protected void onPostExecute(Z result) {
			mFragment.mResult = result;
			mFragment.mIsDone = true;
			if (!mFragment.mIsPaused) {
				mFragment.doPostExecute();
			}
		}

		@Override
		protected void onPreExecute() {
			mFragment.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Y... values) {
			mFragment.onProgressUpdate(values);
		}

		public void publishStatus(Y... values) {
			publishProgress(values);
		}

	}

	private Params[] mParams;
	private InternalAsyncTask<Params, Progress, Result> mTask;
	Result mResult;
	private String mTag;
	boolean mIsPaused;
	boolean mIsDone;

	public abstract Result doInBackground(Params... params);

	public void doPostExecute() {
		Fragment f = getFragmentManager().findFragmentByTag(mTag);
		if (f != null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.remove(f);
			ft.commit();
			onPostExecute(mResult);
		}
	}

	public void execute(FragmentManager fm, String tag, Params... params) {
		setRetainInstance(true);
		mParams = params;
		mTag = tag;
		if (mTag == null) {
			mTag = getClass().getName();
		}
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(this, mTag);
		ft.commit();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (mTask == null) {
			mTask = new InternalAsyncTask<Params, Progress, Result>(this);
			mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mParams);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mIsPaused = true;
	}

	public void onPostExecute(Result result) {
	}

	public void onPreExecute() {
	}

	public void onProgressUpdate(Progress... values) {
	}

	@Override
	public void onResume() {
		super.onResume();
		mIsPaused = false;
		if (mIsDone) {
			doPostExecute();
		}
	}

	public void publishProgress(Progress... values) {
		if (mTask == null) {
			throw new IllegalStateException(
					"FragmentAsyncTask must be executed before calling this method");
		}
		mTask.publishStatus(values);
	}

}
