package com.cyberon.a802_exam;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.cyberon.dspotterutility.DSpotterRecog;
import com.cyberon.dspotterutility.DSpotterStatus;
import com.cyberon.a802_exam.DSpotterApplication;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class VideoPlayer extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private String[] voice = {"播放影片","離開操作","掃描教案","上傳影片","退出軟體","結束操作"};
    private TextView title;

    private String Status = "";

    private String IP;
    private String FormID;
    private String Agent_SYSID;

    private String Study_name;
    private String finish = "";
    private String Path;

    private ImageView image1 = null;
    private AlertDialog dialog = null;
    private AlertDialog dialog2 = null;
    private AlertDialog dialog3 = null;

    private TextView mMinutePrefix;
    private TextView mMinuteText;
    private TextView mSecondPrefix;
    private TextView mSecondText;
    private Handler mHandler = new Handler();

    private VideoView mVv;
    private Boolean isPlay = false;
    private static String tmpPath;
    private int SplitCount;
    String dir = Environment.getExternalStorageDirectory()
            .getPath() + "/Movies/";

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


    private final Handler m_oHandler = new VideoPlayer.DSpotterDemoHandler(this);

    @SuppressLint("HandlerLeak")
    private class DSpotterDemoHandler extends Handler {


        public DSpotterDemoHandler(VideoPlayer mainActivity) {

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
                        case "播放影片":
                            if(isPlay == false){
                                play_video();
                                if(dialog.isShowing()){
                                    dialog3.dismiss();
                                    dialog.dismiss();
                                }
                            }
                            break;
                        case "離開操作":
                            if(isPlay == false){
                                m_oDSpotterRecog.stop();
                                if(dialog.isShowing()){
                                    File file = new File(dir);
                                    deleteAll(file);
                                    dialog.dismiss();
                                }else {
                                    dialog2.dismiss();
                                }
                                Intent intent = new Intent();
                                intent.setClass(VideoPlayer.this,RecordVideo.class);
                                startActivity(intent);
                            }
                            break;
                        case "掃描教案":
                            if(mVv == null){
                                m_oDSpotterRecog.stop();
                                dialog2.dismiss();
                                Intent intent2 = new Intent();
                                Bundle bundle2 = new Bundle();
                                bundle2.putString("Agent_SYSID",Agent_SYSID);
                                intent2.setClass(VideoPlayer.this,MainActivity.class);
                                intent2.putExtras(bundle2);
                                startActivity(intent2);
                            }
                            break;
                        case "上傳影片":
                            try {
                                if(dialog.isShowing()){
                                    dialog.dismiss();
                                    Thread thread = new Thread(runnable2);
                                    thread.start();
                                    thread.join();
                                    File file = new File(dir);
                                    deleteAll(file);
                                    if(Status.equals("count")){
                                        showToast("已上傳三部影片，請至後台篩選。");
                                    }
                                    else if(Status.equals("success")){
                                        showToast("上傳成功。");
                                    }
                                    else {
                                        showToast("伺服器發生錯誤，請重新嘗試或聯絡管理員。");
                                    }
                                    finish = "finish";
                                    dialog3.dismiss();
                                    dialog2.show();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "退出軟體":
                            if(isPlay == false){
                                m_oDSpotterRecog.stop();
                                SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                finishAffinity();
                                System.exit(0);
                            }
                            break;
                        case "結束操作":
                            if(isPlay == true){
                                setContentView(R.layout.videolist);
                                image1 = findViewById(R.id.image1);

                                image1.setImageResource(R.drawable.video1);

                                dialog3.show();
                                dialog.show();

                                isPlay = false;
                                mVv = null;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videolist);
        setFinishOnTouchOutside(false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
        Study_name =  sharedPreferences.getString("Study_name","");
        Path = sharedPreferences.getString("image1","");
        IP = sharedPreferences.getString("IP","");
        FormID = sharedPreferences.getString("FormID","");
        Agent_SYSID = sharedPreferences.getString("Agent_SYSID","");
        init();

        iniDSpotter();
        if(m_oDSpotterRecog.start(false) == 0){
            System.out.println("success");
        }else {
            showToast("語音辨識開啟失敗，請重新開啟或聯絡管理員。");
        }
    }

    public void init(){
        title = findViewById(R.id.title);
        title.setText("教案名稱:"+Study_name);

        image1 = findViewById(R.id.image1);
        image1.setImageResource(R.drawable.video1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("1.若要上傳此影片, 請說「上傳影片」"+"\n"+"2.若要重新錄製, 請說「離開操作」"+"\n"+"(系統僅保留最後三筆上傳影片)"+"\n"+"3.若要再回看, 請說「播放影片」");
        dialog = builder.create();

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setMessage("1. 若要重新錄製, 請說「離開操作」,\n2.若要掃描其他教案請說「掃描教案」\n3. 若要退出APP請說「退出軟體」");
        dialog2 = builder2.create();

        AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder3.setView(inflater.inflate(R.layout.loding,null));
        dialog3 = builder3.create();
    }

    public void view_list(){
        setContentView(R.layout.videolist);
        image1 = findViewById(R.id.image1);

        image1.setImageResource(R.drawable.video1);

        title = findViewById(R.id.title);
        title.setText("教案名稱:"+Study_name);
    }

    public void play_video(){
        setContentView(R.layout.videoview);
        isPlay = true;
        mVv = findViewById(R.id.vv);

        mMinutePrefix = findViewById(R.id.minute_prefix);
        mMinuteText = findViewById(R.id.timestamp_minute_text);
        mSecondPrefix = findViewById(R.id.timestamp_second_prefix);
        mSecondText = findViewById(R.id.timestamp_second_text);

        mVv.setOnPreparedListener(this);
        mVv.setOnErrorListener(this);
        mVv.setOnCompletionListener(this);

//        mVv.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test));
        mVv.setVideoURI(Uri.parse(Path));
        mVv.setMediaController(new MediaController(this));
        mVv.start();

        mHandler.postDelayed(mTimestampRunnable, 1000);
    }

    private Runnable mTimestampRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimestamp();
            mHandler.postDelayed(this, 1000);
        }
    };

    private void updateTimestamp() {
        int second = Integer.parseInt(mSecondText.getText().toString());
        int minute = Integer.parseInt(mMinuteText.getText().toString());

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
        } else if (minute >= 60) {
            mMinutePrefix.setVisibility(View.GONE);
        }
    }

    public void VideoEnd(){
        mMinutePrefix.setVisibility(View.VISIBLE);
        mMinuteText.setText("0");
        mSecondPrefix.setVisibility(View.VISIBLE);
        mSecondText.setText("0");

        setContentView(R.layout.videolist);
        image1 = findViewById(R.id.image1);

        image1.setImageResource(R.drawable.video1);

        dialog3.show();
        dialog.show();

        isPlay = false;
        mVv = null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
//        // TODO
        //影片結束後
        VideoEnd();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "資源有問題", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
//        Toast.makeText(getApplicationContext(), "準備好了", Toast.LENGTH_LONG).show();
    }

    public void getSplitFile() throws FileNotFoundException {
        FileInputStream fis = null;
        File file = new File(Path);
        fis = new FileInputStream(file);
        RandomAccessFile raf = null;
        String srcFilePath = Path;
        String dstFilePath = Environment.getExternalStorageDirectory()
                .getPath()+"/Movies/";

        try {
            //获取目标文件 预分配文件所占的空间 在磁盘中创建一个指定大小的文件   r 是只读
            raf = new RandomAccessFile(new File(srcFilePath), "r");
            int size = fis.available();
            int cutsize = 2 * 1024 * 1024;
            SplitCount = (int) Math.ceil(size / cutsize);

            long length = raf.length();//文件的总长度
            long maxSize = length / SplitCount;//文件切片后的长度
            long offSet = 0L;//初始化偏移量
            for (int i = 0; i < SplitCount - 1; i++) { //最后一片单独处理
                long begin = offSet;
                long end = (i + 1) * maxSize;
//                offSet = writeFile(file, begin, end, i);
                offSet = getWrite(srcFilePath, dstFilePath, i, begin, end);
                Thread thread = new Thread(runnable);
                thread.start();
                thread.join();
            }
            if (length - offSet > 0) {
                getWrite(srcFilePath, dstFilePath, SplitCount - 1, offSet, length);
                Thread thread = new Thread(runnable);
                thread.start();
                thread.join();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (raf != null)
                    raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static long getWrite(String srcFilePath, String dstFilePath, int index, long begin,
                                long end) {
        File srcFile = new File(srcFilePath);
        long endPointer = 0L;
        try {
            //申明文件切割后的文件磁盘
            RandomAccessFile in = new RandomAccessFile(new File(srcFilePath), "r");
            tmpPath = dstFilePath + srcFile.getName()
                    .split("\\.")[0]
                    + "_" + index + ".tmp";
            //定义一个可读，可写的文件并且后缀名为.tmp的二进制文件
            RandomAccessFile out = new RandomAccessFile(new File(tmpPath), "rw");

            //申明具体每一文件的字节数组
            byte[] b = new byte[1024];
            int n = 0;
            //从指定位置读取文件字节流
            in.seek(begin);
            //判断文件流读取的边界
            while (in.getFilePointer() <= end && (n = in.read(b)) != -1) {
                //从指定每一份文件的范围，写入不同的文件
                out.write(b, 0, n);
            }
            //定义当前读取文件的指针
            endPointer = in.getFilePointer();
            //关闭输入流
            in.close();
            //关闭输出流
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endPointer;
    }

    public void deleteAll(File path) {
        if (!path.exists()) {
            return;
        }
        if (path.isFile()) {
            path.delete();
            return;
        }
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            deleteAll(files[i]);
        }
    }

    Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            try {
                getSplitFile();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                File file = new File(tmpPath);
                String fileName = file.getName();
                byte[] bytesArray = new byte[(int) file.length()];

                FileInputStream fis = new FileInputStream(file);
                fis.read(bytesArray); //read file into bytes[]
                fis.close();

                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(IP+"/WebServiceUpload.asmx/UploadFile");
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addBinaryBody("file", bytesArray, ContentType.DEFAULT_BINARY, fileName);
                builder.addTextBody("project","exam");
                builder.addTextBody("count",String.valueOf(SplitCount));
                HttpEntity reqEntity = builder.build();
                httpPost.setEntity(reqEntity);
                try(CloseableHttpResponse response = httpclient.execute(httpPost)){
                    System.out.println(response.getCode() + " " + response.getReasonPhrase());
                    HttpEntity entity = response.getEntity();
                    String res_data  = EntityUtils.toString(entity); //response
                    int location = res_data.indexOf("success");
                    int location2 = res_data.indexOf("count");
                    if(location>0){
                        Status = "success";
                    }
                    if(location2>0){
                        Status = "count";
                    }
                    if(location<0 && location2<0){
                        Status = "fail";
                    }
                    System.out.println("res_data:"+Status);

                }catch (Exception e){
                    System.out.println(e);
                }
            } catch (Exception e) {
                System.out.println("IOException"+e.getMessage());
                e.printStackTrace();
            }
        }

    };


//    Runnable runnable = new Runnable() {
//        @Override
//        public void run() {
//            String fileName = FormID+"_"+Agent_SYSID;
//            try {
//                File file = new File(Path);
//
//                byte[] bytesArray = new byte[(int) file.length()];
//
//                FileInputStream fis = new FileInputStream(file);
//                fis.read(bytesArray); //read file into bytes[]
//                fis.close();
//
//                CloseableHttpClient httpclient = HttpClients.createDefault();
//                HttpPost httpPost = new HttpPost(IP+"/WebServiceUpload.asmx/UploadVideoExam");
//                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//                builder.addBinaryBody("file", bytesArray, ContentType.DEFAULT_BINARY, fileName);
//                HttpEntity reqEntity = builder.build();
//                httpPost.setEntity(reqEntity);
//                try(CloseableHttpResponse response = httpclient.execute(httpPost)){
//                    System.out.println(response.getCode() + " " + response.getReasonPhrase());
//                    HttpEntity entity = response.getEntity();
//                    String res_data  = EntityUtils.toString(entity); //response
//                    int location = res_data.indexOf("success");
//                    int location2 = res_data.indexOf("count");
//                    if(location>0){
//                        Status = "success";
//                    }
//                    if(location2>0){
//                        Status = "count";
//                    }
//                    if(location<0 && location2<0){
//                        Status = "fail";
//                    }
//                    System.out.println("res_data:"+Status);
//
//                }catch (Exception e){
//                    System.out.println(e);
//                }
//            } catch (Exception e) {
//                System.out.println("IOException"+e.getMessage());
//                e.printStackTrace();
//            }
//        }
//
//    };

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

        nRet = m_oDSpotterRecog.initWithFiles(this,strCommandFile, DSpotterApplication.m_sLicenseFile, DSpotterApplication.m_sServerFile,naErr);
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
