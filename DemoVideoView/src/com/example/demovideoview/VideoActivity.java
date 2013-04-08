package com.example.demovideoview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

	// String SrcPath =
	// "rtsp://202.146.92.42/ANIMAXVOD/Lunar_Legend_Ep2_4_114.3gp"; //?
	// String SrcPath = "http://192.168.1.95/stream/first/fileSequence0.ts"; //
	// 1 files ok
	// String SrcPath = "http://192.168.1.95/stream/second/prog_index.m3u8"; //
	// local ok
	// String SrcPath =
	// "http://192.168.1.152/testhls/rakutenfiles/C01MPM0001-C01S01N00000461-0400.m3u8";
	// //rakuten ok

	String SrcPath = "http://192.168.1.152/testhls/pachinko/C02S01N00000004-0400.m3u8"; // pachinko
																						// ok
	// hami																					// for
																						// local

	private static final String TAG = "VideoViewDemo";

	protected int mDuration;
	protected VideoView mVideoView;
	protected FrameLayout mFrameLayout;
	protected ImageButton mPlay;
	// private ImageButton mPause;
	protected ImageButton mReset;
	protected ImageButton mStop;
	protected String mCurrent;
	private ImageView mLoadingScreenImageView;
	private LinearLayout mControlsLinearLayout;
	protected ProgressBar mProgressBar;
	protected boolean mIsIntroVideo;
	private Timer mTimer;
	private TimerTask mTimerTask;
	protected SeekBar mSeekBar;
	protected boolean mDoesNextActivityStarted;

	private TimerTask mSeekbarTimerTask;
	private Timer mSeekBarTimer;
	private boolean mIsPrefetched;

	private TextView mCurrentDuration;
	private TextView mTotalDuration;
	private long mLastPOosition = -1;
	private long mCurrentPosition = -1;
	private int mTestSecondaryProgress = 0;
	private int mLagCount = 0;
	
	private int mCurrentState = STATE_IDLE;
	
    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.video);
		mVideoView = (VideoView) findViewById(R.id.video_view);
		mDuration = -1;

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar_video);
		// mLoadingScreenImageView =
		// (ImageView)findViewById(R.id.iv_loading_screen);
		mControlsLinearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
		mFrameLayout = (FrameLayout) findViewById(R.id.videoframelayout);
		mCurrentDuration = (TextView) findViewById(R.id.tvCurrentDuration);
		mTotalDuration = (TextView) findViewById(R.id.tvTotalDuration);

		// loadingScreenImageView.setVisibility(View.GONE);
		// mProgressBar.setVisibility(View.VISIBLE);
		mProgressBar.setVisibility(View.GONE);
		mCurrentDuration.setText("00:00");
		mTotalDuration.setText("00:00");
		mTotalDuration.setTextColor(Color.RED);

		mControlsLinearLayout.setAnimation(AnimationUtils.loadAnimation(this, R.anim.alpha));

		mPlay = (ImageButton) findViewById(R.id.play);
		mReset = (ImageButton) findViewById(R.id.reset);
		mStop = (ImageButton) findViewById(R.id.stop);

		final Drawable playDrawable = getResources().getDrawable(R.drawable.play);
		final Drawable pauseDrawable = getResources().getDrawable(R.drawable.pause);

		mPlay.setImageDrawable(playDrawable);
		mVideoView.setOnPreparedListener(new OnPreparedListener() {

			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.d("onPrepared", "mp.getDuration() : " + mp.getDuration());
				mIsPrefetched = true;
				// loadingScreenImageView.setVisibility(View.GONE);
			}
		});
		
		mVideoView.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				mCurrentState = STATE_PLAYBACK_COMPLETED;
			}
		});

		// mVideoView.setO
		mPlay.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {

				if (mVideoView != null && mVideoView.isPlaying()) {
					mPlay.setImageDrawable(playDrawable);
					mVideoView.pause();
					mCurrentState = STATE_PAUSED;
				} else {
					mPlay.setImageDrawable(pauseDrawable);
					playVideo();
				}

			}
		});

		mReset.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (mVideoView != null && mVideoView.isPlaying()) {
					mVideoView.seekTo(0);
				} else {
					mCurrent = null;
					playVideo();
				}
			}
		});

		mStop.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (mVideoView != null) {
					mCurrent = null;
					//mVideoView.stopPlayback();
					onSeekForward(mSeekBar, 10000);
				}
			}
		});
		mSeekBar = (SeekBar) findViewById(R.id.seekBar1);
		mSeekBar.setOnSeekBarChangeListener(this);

		mTimer = new Timer();
		mTimerTask = new TimerTask() {

			public void run() {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {

					public void run() {
						// TODO Auto-generated method stub
						if (mVideoView != null) {
							mDuration = mVideoView.getDuration();

							int percent = mVideoView.getBufferPercentage();

//							Log.d("hami2000", "getCurrentPosition(): " + mVideoView.getCurrentPosition());

							// if(Constants.LOG)Log.d(" mVideoView.getBufferPercentage() : ",
							// ""+mVideoView.getBufferPercentage());

							if (mSeekBar.getSecondaryProgress() != 100 && percent != 100 && mDuration != -1) {
								mSeekBar.setSecondaryProgress(percent + 10);
								// mSeekBar.setSecondaryProgress(mTestSecondaryProgress++);
							}
						}
						// if(Constants.LOG)Log.d(" Intimer task before ifisTouchedOnceAgain : ",
						// ""+isTouchedOnceAgain);
						if (!mIsTouchedOnceAgain) {

							// setVisibilityOfControls(View.GONE);

						} else {

							mIsTouchedOnceAgain = false;
						}
					}
				});
			}
		};
		mTimer.scheduleAtFixedRate(mTimerTask, 2000, 3000);

		mSeekBarTimer = new Timer();
		mSeekbarTimerTask = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (mVideoView != null) {
							// if(Constants.LOG)Log.d(" mVideoView.getDuration() : "+
							// mVideoView.getDuration(),
							// "  mVideoView.getBufferPercentage() : "+mVideoView.getBufferPercentage()+" mVideoView.getCurrentPosition() : "+mVideoView.getCurrentPosition());
							mDuration = mVideoView.getDuration();
							int percent = mVideoView.getBufferPercentage();

//							Log.d("hami", "getCurrentPosition(): " + mVideoView.getCurrentPosition());

							/* hami */
							// show loading when stay at current position
							/*
							 * mCurrentPosition =
							 * mVideoView.getCurrentPosition();
							 * mCurrentDuration.
							 * setText(TimeFormat.milisecondToHMS
							 * (mCurrentPosition));
							 * 
							 * if(mLastPOosition == mCurrentPosition){ if
							 * (mProgressBar != null &&
							 * mProgressBar.getVisibility() == View.GONE){
							 * mProgressBar.setVisibility(View.VISIBLE); } }
							 * else { mLastPOosition = mCurrentPosition; if
							 * (mProgressBar != null &&
							 * mProgressBar.getVisibility() == View.VISIBLE){
							 * mProgressBar.setVisibility(View.GONE); } }
							 */
							/* #hami */

							if (mVideoView.getCurrentPosition() != -1 && mDuration != -1) {

								mSeekBar.setSecondaryProgress(percent);
								// mSeekBar.setSecondaryProgress(mTestSecondaryProgress++);
								mSeekBar.setProgress((int) (((float) mVideoView.getCurrentPosition() / mDuration) * 100));
							}
						}
					}
				});
			}
		};
		mSeekBarTimer.scheduleAtFixedRate(mSeekbarTimerTask, 1000, 1000);

		Timer timerForLoading = new Timer();
		TimerTask timerTaskForLoading = new TimerTask() {

			@Override
			public void run() {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						
						if(!(mCurrentState == STATE_PLAYBACK_COMPLETED || mCurrentState == STATE_PAUSED || mCurrentState == STATE_IDLE)) {
							// show loading when stay at current position
							mCurrentPosition = mVideoView.getCurrentPosition();
							mCurrentDuration.setText(TimeFormat.milisecondToHMS(mCurrentPosition));
	
							if (mLastPOosition == mCurrentPosition) {
								if (mProgressBar != null && mProgressBar.getVisibility() == View.GONE) {
									mProgressBar.setVisibility(View.VISIBLE);
									mTotalDuration.setText("Lag: " + mLagCount);
									Log.e("lag", "Lag at :" + TimeFormat.milisecondToHMS(mCurrentPosition) + " " + mLagCount++ + " times");
								}
								mCurrentDuration.setTextColor(Color.RED);
								
							} else {
								mLastPOosition = mCurrentPosition;
								if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE) {
									mProgressBar.setVisibility(View.GONE);
								}
								mCurrentDuration.setTextColor(Color.GREEN);
								
							}
						}
					}
				});

			}
		};
		
		timerForLoading.scheduleAtFixedRate(timerTaskForLoading, 500, 1000);

	}

	private void playVideo() {
		try {
			final String path = SrcPath;
			Log.d(TAG, "path: " + path);
			if (path == null || path.length() == 0) {
				Toast.makeText(VideoActivity.this, "Empty URL", Toast.LENGTH_LONG).show();

			} else {
				// loading
				mProgressBar.setVisibility(View.VISIBLE);

				// If the path has not changed, just start the media player
				if (path.equals(mCurrent) && mVideoView != null) {
					mVideoView.start();
					mVideoView.requestFocus();

					mCurrentState = STATE_PLAYING;
					// hide loading
					mProgressBar.setVisibility(View.GONE);

					return;
				}
				mCurrent = path;
				mVideoView.setVideoURI(Uri.parse(path));

				// mVideoView.setVideoPath(getDataSource(path));
				mVideoView.start();
				mVideoView.requestFocus();
				mCurrentState = STATE_PLAYING;
				mDuration = mVideoView.getDuration();

				Log.d("duration", "" + mDuration);
				// hide loading
				mProgressBar.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			Log.d(TAG, "error: " + e.getMessage(), e);
			if (mVideoView != null) {
				mVideoView.stopPlayback();
				mCurrentState = STATE_IDLE;
			}

		}
	}

	private String getDataSource(String path) {
		try {
			if (!URLUtil.isNetworkUrl(path)) {
				return path;
			} else {
				URL url = new URL(path);
				URLConnection cn = url.openConnection();
				cn.connect();
				InputStream stream = cn.getInputStream();
				if (stream == null)
					throw new RuntimeException("stream is null");
				File temp = File.createTempFile("mediaplayertmp", "dat");
				temp.deleteOnExit();
				String tempPath = temp.getAbsolutePath();
				FileOutputStream out = new FileOutputStream(temp);
				byte buf[] = new byte[128];
				do {
					int numread = stream.read(buf);
					if (numread <= 0)
						break;
					out.write(buf, 0, numread);
				} while (true);
				try {
					stream.close();
				} catch (IOException ex) {
					Log.e(TAG, "error: " + ex.getMessage(), ex);
				}
				return tempPath;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
	}

	boolean mIsTouchedOnceAgain = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		// if(Constants.LOG)Log.d("In Activity touch listenr"," controlsLinearLayout.getVisibility() : "+controlsLinearLayout.getVisibility()
		// );

		if (mControlsLinearLayout.getVisibility() == View.GONE) {

			setVisibilityOfControls(View.VISIBLE);

		}
		mIsTouchedOnceAgain = true;

		if (mIsPrefetched) {

			return super.onTouchEvent(event);
		} else {

			return false;
		}
	}

	private void setVisibilityOfControls(int visibility) {
		// TODO Auto-generated method stub
		mSeekBar.setVisibility(visibility);
		mControlsLinearLayout.setVisibility(visibility);
		mPlay.setVisibility(visibility);
		mReset.setVisibility(visibility);
		mStop.setVisibility(visibility);

	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		// TODO Auto-generated method stub
		/*
		 * if(mVideoView.getCurrentPosition()!=-1){ if(Constants.LOG)Log.d(
		 * "onProgressChanged : seekbar position changing to ",
		 * ""+((float)mVideoView.getCurrentPosition()/duration)*100);
		 * seekBar.setProgress
		 * ((int)(((float)mVideoView.getCurrentPosition()/duration)*100)); }
		 */
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		int progress = seekBar.getProgress();
		if (mDuration != 0 && mDuration != -1) {
			if (mProgressBar != null && mProgressBar.getVisibility() == View.GONE) {
				mProgressBar.setVisibility(View.VISIBLE);
			}
			
			Log.d("onStopTrackingTouch : Seeking to :", "" + (int) (mDuration * ((float) progress) / 100));
			mVideoView.seekTo((int) (mDuration * ((float) progress) / 100));
		}

	}
	
	public void onSeekForward(SeekBar seekBar, int addTime){
		if(mVideoView.canSeekForward()) {
			if (mDuration != 0 && mDuration != -1) {
				seekBar.setProgress(seekBar.getProgress() + 5); // 5 = tmp percent
				int progress = seekBar.getProgress();
				
				Log.d("onSeekForward : Seeking to :", "" + mDuration);
				mVideoView.seekTo((int) (mDuration * ((float) progress) / 100));
			}
		}
	}
	
	public void onSeekBackward(){
		if(mVideoView.canSeekBackward()) {
			if (mDuration != 0 && mDuration != -1) {
				Log.d("onSeekBackward : Seeking to :", "" + mDuration  + 10000);
				mVideoView.seekTo(mDuration  - 10000);
			}
		}
	}

	/*
	 * protected void startPlaying(String URL) { // TODO Auto-generated method
	 * stub SrcPath = URL;
	 * 
	 * runOnUiThread(new Runnable() { public void run() { playVideo();
	 * 
	 * }
	 * 
	 * });
	 * 
	 * }
	 */

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}

}
