package com.vxtindia.androidvideolibrary.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;

//using http://www.brightec.co.uk/blog/custom-android-media-controller
public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        VideoControllerView.MediaPlayerControl{

    private static final String TAG = "VideoPlayerActivity" ;
    public static final String KEY_VIDEO_TITLE = "keyVideoLabel";
    public static final String KEY_URL_STRING = "keyUrlString";

    public static final String KEY_SCREEN_ORIENTATION = "keyScreenOrientation";

    public static final int ORIENTATION_DEFAULT = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    private int screenOrientation=ORIENTATION_DEFAULT;

    private String url = "";
    private String videoTitle = "";


    private SurfaceView videoSurface;
    private MediaPlayer player;
    private VideoControllerView controller;

    private LinearLayout layoutProgressBar;

    private int bufferPosition;
    private int mCurrentPosition=-1;

    private boolean fullScreen = false;

    private boolean retry = false;

    private ActionBarHider actionBarHider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        setupActionBar();

        actionBarHider = new ActionBarHider(this);

        layoutProgressBar = (LinearLayout) findViewById(R.id.layoutPregressBar);

        videoTitle = getIntent().getStringExtra(KEY_VIDEO_TITLE);
        url = getIntent().getStringExtra(KEY_URL_STRING);

        setTitle(videoTitle);

        screenOrientation = getIntent().getIntExtra(KEY_SCREEN_ORIENTATION, ORIENTATION_DEFAULT);

        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);

        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        controller = new VideoControllerView(this,false);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        fullScreen = getScreenOrientation();

        if(player == null){

            setupScreenOrientation();

            player = new MediaPlayer();
            layoutProgressBar.setVisibility(View.VISIBLE);
            prepareVideo();
        }
        else{
            showActionBarAndController();
        }

    }

    private void prepareVideo(){
        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnErrorListener(this);
            player.setDataSource(this, Uri.parse(url));
            player.setOnPreparedListener(this);
            player.setOnBufferingUpdateListener(this);
            player.setOnCompletionListener(this);

            player.prepareAsync();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startVideo(){
        player.setScreenOnWhilePlaying(true);

        if(mCurrentPosition == -1)
            player.start();
        else{
            player.seekTo(mCurrentPosition);
        }
        showActionBarAndController();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCurrentPosition = player.getCurrentPosition();
        player.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(player != null){
            player.release();
            player = null;
        }
        controller.setMediaPlayer(null);
    }

    private void setupScreenOrientation(){
        if(screenOrientation == ORIENTATION_LANDSCAPE){
            fullScreen = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else if(screenOrientation == ORIENTATION_PORTRAIT){
            fullScreen = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        fullScreen = getScreenOrientation();
        setScreenSize();
        controller.updateFullScreen();
        showActionBarAndController();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(controller.isShowing()){
                controller.hide();
            }else{
                showActionBarAndController();
            }
        }
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus){
            showActionBarAndController(0);
        }else{
            showActionBarAndController();
        }
    }

    // Implement SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        player.setDisplay(holder);
        startVideo();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(player != null){
            mCurrentPosition = player.getCurrentPosition();
            player.pause();
        }
    }
    // End SurfaceHolder.Callback

    // Implement MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        layoutProgressBar.setVisibility(View.GONE);
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
        setScreenSize();

        if(retry){
            startVideo();
            retry = false;
        }
    }
    // End MediaPlayer.OnPreparedListener

    public boolean getScreenOrientation(){
        int orientation = getResources().getConfiguration().orientation;

        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            return true;
        }else if(orientation == Configuration.ORIENTATION_PORTRAIT){
            return false;
        }

        return false;
    }

    public void setScreenSize(){
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        ViewGroup.LayoutParams lp = videoSurface.getLayoutParams();

        int videoWidth = player.getVideoWidth();
        int videoHeight = player.getVideoHeight();
         
        lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) screenWidth);

        videoSurface.setLayoutParams(lp);

    }

    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public void start() {
        player.start();
    }
    @Override
    public void pause() {
        player.pause();
    }
    @Override
    public int getDuration() {
        return player.getDuration();
    }
    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }
    @Override
    public void seekTo(int i) {
        player.seekTo(i);
    }
    @Override
    public boolean isPlaying() {
            return player.isPlaying();
    }
    @Override
    public int getBufferPercentage() {
        return bufferPosition;
    }
    @Override
    public boolean canPause() {
        return true;
    }
    @Override
    public boolean isFullScreen() {
        return fullScreen;
    }
    @Override
    public void toggleFullScreen() {
        fullScreen = !fullScreen;

        controller.updateFullScreen();

        if(fullScreen){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
    @Override
    public void notifyHidden() {
        hideActionBar();
    }
    //End VideoControllerView.MediaPlayerControl


    //Implement MediaPlayer.OnBufferingUpdateListener
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        bufferPosition=i;
    }
    //End MediaPlayer.OnBufferingUpdateListener

    protected void setBufferPosition(int progress) {
        bufferPosition = progress;
    }

    //Implement MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.seekTo(0);
        showActionBarAndController();
    }

    //Implement MediaPlayer.OnErrorListener
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int framework_err, int impl_err) {
        if(layoutProgressBar != null && layoutProgressBar.getVisibility() == View.VISIBLE){

            layoutProgressBar.setVisibility(View.GONE);
        }

        releaseMediaPlayerListeners();

        showErrorAlertDialog();
        return true;
    }
    //End MediaPlayer.OnErrorListener

    private void releaseMediaPlayerListeners(){
        player.setOnErrorListener(null);
        player.setOnBufferingUpdateListener(null);
        player.setOnCompletionListener(null);
        player.setOnPreparedListener(null);
    }

    public void showErrorAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.error_dialog_title)
                .setCancelable(false)
                .setMessage(R.string.error_dialog_message)
                .setPositiveButton(R.string.error_dialog_retry_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        retryVideo();
                    }
                })
                .setNegativeButton(R.string.error_dialog_cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        VideoPlayerActivity.this.finish();
                    }
                });
        AlertDialog alert=alertDialogBuilder.create();
        alert.show();
    }

    private void retryVideo(){
        retry = true;
        layoutProgressBar.setVisibility(View.VISIBLE);
        mCurrentPosition = -1;
        player.reset();
        prepareVideo();
    }

    private void showActionBarAndController(){
        actionBarHider.show();
        controller.show();
    }

    private void showActionBarAndController(int timeout){
        actionBarHider.show();
        controller.show(timeout);
    }

    private void hideActionBar(){
        actionBarHider.hide();
    }
}