package com.cyberon.a802_exam;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.cyberon.dspotterutility.DSpotterRecog;
import com.cyberon.dspotterutility.DSpotterStatus;
import com.cyberon.a802_exam.DSpotterApplication;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class RecordVideo extends AppCompatActivity implements SurfaceHolder.Callback {
    private String[] voice = {"開始錄影","結束操作","離開操作"};
    private MediaRecorder mediarecorder;// 錄製視頻的類
    private Boolean isRecording = false;
    private SurfaceView surfaceview;// 顯示視頻的控制項
    private SurfaceHolder surfaceHolder;
    private AlertDialog dialog = null;
    private Camera mCamera;
    File viodFile;

    private TextView title;
    private String Study_name;
    private String FormID;
    private String Agent_SYSID;

    private String start_end = "";

    private TextView mMinutePrefix;
    private TextView mMinuteText;
    private TextView mSecondPrefix;
    private TextView mSecondText;
    private Handler mHandler = new Handler();
    private int total_sec = 0;

    private static final int MSG_INITIALIZE_SUCCESS = 2000;
    private DSpotterRecog m_oDSpotterRecog = null;
    //form createLogResultFile function

    private ArrayAdapter<String> m_oCommandBinAdapter = null;

    // For status callback
    private DSpotterRecog.DSpotterRecogStatusListener m_oRecogStatusListener = new DSpotterRecog.DSpotterRecogStatusListener() {

        @Override
        public void onDSpotterRecogStatusChanged(int nStatus) {
            m_oHandler.sendMessage(m_oHandler.obtainMessage(nStatus, 0, 0));
        }
    };


    private final Handler m_oHandler = new DSpotterDemoHandler(this);

    @SuppressLint("HandlerLeak")
    private class DSpotterDemoHandler extends Handler {


        public DSpotterDemoHandler(RecordVideo mainActivity) {

        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INITIALIZE_SUCCESS:
                    System.out.println("MSG_INITIALIZE_SUCCESS");
                    // Set recognize button enable
                    // Show success message
                    // showToast("Initialize success!");
                    break;
                case DSpotterStatus.STATUS_RECORDER_INITIALIZE_FAIL:
                    System.out.println("DSpotterStatus.STATUS_RECORDER_INITIALIZE_FAIL");
                    m_oDSpotterRecog.stop();

                    // Show message
                    // Show message
//                    showToast("Fail to initialize recorder!");
                    break;
                case DSpotterStatus.STATUS_RECOGNITION_START:
                    System.out.println("DSpotterStatus.STATUS_RECOGNITION_START");
                    break;

                case DSpotterStatus.STATUS_RECOGNITION_OK:
                    System.out.println("DSpotterStatus.STATUS_RECOGNITION_OK");

                    String[] straResult = new String[1];
                    m_oDSpotterRecog.getResult(null, straResult, null, null, null, null, null, null);

                    System.out.println(straResult[0]);
                    String Recog_text = straResult[0].replaceAll("\\s+","");
                    if(Arrays.asList(voice).contains(Recog_text)){
                        showToast(Recog_text);
                    }
                    switch (Recog_text){
                        case "開始錄影":
                            if(isRecording == false){
                                try {
                                    if(mediarecorder != null){
                                        start_end = "start";
                                        isRecording = true;
                                        mediarecorder.prepare();
                                        mediarecorder.start();
                                        mHandler.postDelayed(mTimestampRunnable, 1000);
                                        dialog.dismiss();
                                        TextView textView = findViewById(R.id.textview);
                                        textView.setText("*說「結束操作」結束錄影");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            break;
                        case "結束操作":
                            if(isRecording){
                                end_media();
                                isRecording = false;
                            }
                            break;
                        case "離開操作":
                            if(isRecording == false){
                                m_oDSpotterRecog.stop();
                                mediarecorder.reset();
                                mediarecorder.release();
                                dialog.dismiss();
                                Intent intent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putString("Agent_SYSID",Agent_SYSID);
                                intent.setClass(RecordVideo.this,MainActivity.class);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                            break;
                    }
                    break;
                case DSpotterStatus.STATUS_RECOGNITION_FAIL:
                    System.out.println("DSpotterStatus.STATUS_RECOGNITION_FAIL");
                    m_oDSpotterRecog.stop();
                    break;
                case DSpotterStatus.STATUS_RECOGNITION_ABORT:
                    System.out.println("DSpotterStatus.STATUS_RECOGNITION_ABORT");
                    break;
                case DSpotterStatus.STATUS_RECORD_FAIL:
                    System.out.println("DSpotterStatus.STATUS_RECORD_FAIL");
                    m_oDSpotterRecog.stop();
                    break;
                case DSpotterStatus.STATUS_RECORD_FINISH:
                    System.out.println("DSpotterStatus.STATUS_RECORD_FINISH");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        surfaceview = this.findViewById(R.id.surfaceview);
        surfaceHolder = surfaceview.getHolder();// 取得holder
        surfaceview.getHolder().setFormat(PixelFormat.TRANSPARENT);
        surfaceHolder.addCallback(this); // holder加入回檔介面
        // setType必須設置，要不出錯.
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mMinutePrefix = findViewById(R.id.timestamp_minute_prefix);
        mMinuteText = findViewById(R.id.timestamp_minute_text);
        mSecondPrefix = findViewById(R.id.timestamp_second_prefix);
        mSecondText = findViewById(R.id.timestamp_second_text);

        SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
        Study_name =  sharedPreferences.getString("Study_name","");
        FormID =  sharedPreferences.getString("FormID","");
        Agent_SYSID = sharedPreferences.getString("Agent_SYSID","");
        System.out.println(Agent_SYSID);
        title = findViewById(R.id.title);
        title.setText("教案名稱:"+Study_name);

        iniDSpotter();
        if(m_oDSpotterRecog.start(false) == 0){
            System.out.println("success");
        }else {
//            showToast("語音辨識開啟失敗，請重新開啟或聯絡管理員。");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("1. 請說「開始錄影」"+"\n"+"(請注意系統僅保留最後三筆上傳影片, 此模擬考APP完成後到web後臺選擇最終送出影片)"+"\n"+"2.或說「離開操作」返回掃描教案");
        dialog = builder.create();
        dialog.show();

        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() throws IOException {
        mCamera = Camera.open(0);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        parameters.set("cam_mode", 1);
//        parameters.set("cam-mode", 1);
        mCamera.setParameters(parameters);
        mCamera.stopPreview();
        mCamera.unlock();

        mediarecorder = new MediaRecorder();
        mediarecorder.setCamera(mCamera);
//        mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

        mediarecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediarecorder.setVideoEncodingBitRate(3*1024*1024);
        mediarecorder.setVideoSize(1280, 720);
        mediarecorder.setVideoFrameRate(30);
//        mediarecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());

        viodFile = new File(Environment.getExternalStorageDirectory()
                .getPath()+"/Movies/"+FormID+"_"+Agent_SYSID+".mp4");

        viodFile.createNewFile();
        mediarecorder.setOutputFile(viodFile.getAbsolutePath());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // 將holder，這個holder為開始在oncreat裡面取得的holder，將它賦給surfaceHolder
        surfaceHolder = holder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 將holder，這個holder為開始在oncreat裡面取得的holder，將它賦給surfaceHolder
        surfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // surfaceDestroyed的時候同時物件設置為null
        surfaceview = null;
        surfaceHolder = null;
        mediarecorder = null;
    }

    private Runnable mTimestampRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimestamp();
            mHandler.postDelayed(this, 1000);
        }
    };

    private void updateTimestamp() {
        total_sec +=1;
        int second = Integer.parseInt(mSecondText.getText().toString());
        int minute = Integer.parseInt(mMinuteText.getText().toString());
        if(total_sec == 1200){
            isRecording = false;
            end_media();
        }
        else {
            second++;

            if (second < 10) {
                mSecondText.setText(String.valueOf(second));
            } else if (second >= 10 && second < 60) {
                mSecondPrefix.setVisibility(View.GONE);
                mSecondText.setText(String.valueOf(second));
            } else if (second >= 60) {
                mSecondPrefix.setVisibility(View.VISIBLE);
                mSecondText.setText("0");

                minute++;
                mMinuteText.setText(String.valueOf(minute));
            } else if (minute >= 10 && minute < 60) {
                mMinutePrefix.setVisibility(View.GONE);
                mMinuteText.setText(String.valueOf(minute));
            }

        }
    }

    public void end_media(){
        if(start_end.equals("start")){
            if(mediarecorder != null){
                mediarecorder.stop();
                mediarecorder.reset();
                mediarecorder.release();

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            m_oDSpotterRecog.stop();
            mMinutePrefix.setVisibility(View.VISIBLE);
            mMinuteText.setText("0");
            mSecondPrefix.setVisibility(View.VISIBLE);
            mSecondText.setText("0");

            SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
            String data1 =  sharedPreferences.getString("image1","");
            System.out.println("data1:"+data1);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("image1",viodFile.getAbsolutePath());
            editor.apply();

            Intent intent = new Intent();
            intent.setClass(RecordVideo.this,VideoPlayer.class);
            startActivity(intent);
        }
    }

    //下面全都是語音辨識用
    private void initCmdBinSpinner() {
        File oFile = new File(DSpotterApplication.m_sCmdFileDirectoryPath);
        String[] strBinFileArray = oFile.list(new CmdBinFilter());

        if (strBinFileArray == null || strBinFileArray.length == 0) {
            showToast("Found no command file.");
            return;
        }

        for (int i = 0; i < strBinFileArray.length; i++)
            strBinFileArray[i] = strBinFileArray[i].substring(0,
                    strBinFileArray[i].length() - 4); // skip .bin

        Arrays.sort(strBinFileArray);
        m_oCommandBinAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, strBinFileArray);
        m_oCommandBinAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private class CmdBinFilter implements FilenameFilter {

        @SuppressLint("DefaultLocale")
        private boolean isBinFile(String file) {
            return file.endsWith(".bin");
        }

        @Override
        public boolean accept(File dir, String filename) {
            if (filename.equals("DSpotter_CMS.bin"))
                return false;
            return isBinFile(filename);
        }
    }

    public void iniDSpotter(){

        if (m_oDSpotterRecog == null)
            m_oDSpotterRecog = new DSpotterRecog();

        int nRet;
        int[] naErr = new int[1];

        String strCommandFile;
        if (DSpotterApplication.m_sCmdFilePath == null) {
//            strCommandFile = DSpotterApplication.m_sCmdFileDirectoryPath + "/"
//                    + m_oCommandBinAdapter
//                    .getItem(DSpotterApplication.m_nCmdBinListIndex)
//                    + ".bin";
            strCommandFile = DSpotterApplication.m_sCmdFileDirectoryPath + "/" + "glasses_802_pack_withTxt" + ".bin";
        }
        else {
            strCommandFile = DSpotterApplication.m_sCmdFilePath;
            String strCommandFileName = new File(strCommandFile).getName();
            DSpotterApplication.m_sCmdFilePath = null;
        }

        nRet = m_oDSpotterRecog.initWithFiles(this,strCommandFile,DSpotterApplication.m_sLicenseFile,DSpotterApplication.m_sServerFile,naErr);
        if (nRet != DSpotterRecog.DSPOTTER_RECOG_SUCCESS) {
            Toast.makeText(this,"Fail to initialize DSpotter, " + naErr[0],Toast.LENGTH_LONG).show();
            return;
        }

        m_oDSpotterRecog.setListener(m_oRecogStatusListener);

        m_oDSpotterRecog.getTriggerWord();

        m_oHandler.sendMessage(m_oHandler.obtainMessage(MSG_INITIALIZE_SUCCESS,
                0, 0));
    }

    public void showToast(String text){
        Toast.makeText(this,text,Toast.LENGTH_LONG).show();
    }
}
