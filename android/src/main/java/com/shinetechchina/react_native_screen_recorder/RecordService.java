package com.shinetechchina.react_native_screen_recorder;


import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.app.Activity;
import android.os.IBinder;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class RecordService extends Service {

    private MediaProjection mediaProjection;
    //private MediaRecorder mediaRecorder;
    private VideoCapture mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private String filePath;

    private boolean running;
    private int maxDuration = 10000;
    private int width = 720;
    private int height = 1080;
    private int dpi;


    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        // mediaRecorder = new MediaRecorder();
        mediaRecorder = new VideoCapture();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }

        mediaRecorder.initRecorder();
        createVirtualDisplay();
        mediaRecorder.start();
        running = true;
        return true;
    }

    public String stopRecord() {
        if (!running)
        {
            return filePath;
        }

        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();

        return filePath;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    public String getsaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/";

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

            Toast.makeText(getApplicationContext(), rootDir, Toast.LENGTH_SHORT).show();

            return rootDir;
        } else {
            return null;
        }
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

    // Subclass of the MediaRecorder which implements the max duration of what will happen
    // when the timout is reached. This will stop and save the video.
    public class VideoCapture extends Activity implements MediaRecorder.OnInfoListener {

        private MediaRecorder recorder = new MediaRecorder();

        public Surface getSurface() {
            return recorder.getSurface();
        }

        public void start() {
            recorder.start();
        }

        public void stop() {
            recorder.stop();
        }

        public void reset() {
            recorder.reset();
        }

        public void initRecorder() {
            filePath = getsaveDirectory() + System.currentTimeMillis() + ".mp4";
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(filePath);
            recorder.setVideoSize(width, height);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            recorder.setVideoFrameRate(30);
            recorder.setMaxDuration(maxDuration); // 10 seconds
            recorder.setOnInfoListener(this);
            try {
                recorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void onInfo(MediaRecorder mr, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                stopRecord();
            }
        }
    }
  }
