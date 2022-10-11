package com.cyberon.a802_exam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.cyberon.dspotterutility.DSpotterRecog;
import com.cyberon.dspotterutility.DSpotterStatus;
import com.cyberon.engine.LoadLibrary;
import com.google.zxing.Result;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private String[] voice = {"退出軟體"};
    private static final int MSG_INITIALIZE_SUCCESS = 2000;
    private DSpotterRecog m_oDSpotterRecog = null;

    private ZXingScannerView mScannerView;
    private Toast toast = null;
    private String toast_show = "";
    private static long oneTime = 0;
    private static long twoTime = 0;

    private String IP = "http://192.168.2.143:8080";
//    private String IP = "http://210.68.227.123:8029";

    private String Post;
    private String Agent_ID = "";
    private String UserID = "";
    private String sub_str;
    private String Study_name;
    private String Message = "";
    private String Agent_Name = "";

    private String FormID;
    private String Agent_SYSID = "";

    private String[]  QR_data_array;

    private DSpotterRecog.DSpotterRecogStatusListener m_oRecogStatusListener = new DSpotterRecog.DSpotterRecogStatusListener() {

        @Override
        public void onDSpotterRecogStatusChanged(int nStatus) {
            m_oHandler.sendMessage(m_oHandler.obtainMessage(nStatus, 0, 0));
        }
    };


    private final Handler m_oHandler = new MainActivity.DSpotterDemoHandler(this);

    @SuppressLint("HandlerLeak")
    private class DSpotterDemoHandler extends Handler {


        public DSpotterDemoHandler(MainActivity mainActivity) {

        }
        @Override
        public void handleMessage(android.os.Message msg) {
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
                        case "退出軟體":
                            m_oDSpotterRecog.stop();
                            SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.clear();
                            finishAffinity();
                            System.exit(0);
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
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mScannerView = new ZXingScannerView(this);
        //找到介面
        mScannerView = findViewById(R.id.QRCode);
        mScannerView.setAspectTolerance(0.5f);

        SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            if(bundle.containsKey("Agent_SYSID")){
                String bundleAgent_SYSID = bundle.getString("Agent_SYSID");
                Agent_SYSID = bundleAgent_SYSID;
                TextView textView = findViewById(R.id.textView_Result);
                textView.setText("請掃描教案QRCode");
            }else {
                editor.clear();
            }
        }else {
            CheckLicenseFile();
            LoadLibrary.loadLibrary(getApplicationContext());
        }

        iniDSpotter();
        if(m_oDSpotterRecog.start(false) == 0){
            System.out.println("success");
        }else {
            showToast("語音辨識開啟失敗，請重新開啟或聯絡管理員。");
        }
    }

    public void CheckLicenseFile(){
        //            String fileNames[] = getAssets().list("");
//            System.out.println(Arrays.toString(fileNames));
        InputStream in = null;
        OutputStream out = null;
        File outFile = null;
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            String address = info.getMacAddress().replace(':','_');
            System.out.println("address:"+address);

            String filestr = "CybLicense_" + address + ".bin";
            in = getAssets().open(filestr);
            String outDir = Environment.getExternalStorageDirectory()
                    .getPath() + "/DCIM/";
            outFile = new File(outDir, "CybLicense.bin");
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            outFile = null;
//            in = getAssets().open("CybLicense.bin");
//            outFile = new File(outDir, "CybLicense.bin");
//            out = new FileOutputStream(outFile);
//            copyFile(in, out);
//            in.close();
//            in = null;
//            out.flush();
//            out.close();
//            out = null;

            in = getAssets().open("glasses_802_pack_withTxt.bin");
            outFile = new File(outDir, "glasses_802_pack_withTxt.bin");
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void Check(){
        m_oDSpotterRecog.stop();
        SharedPreferences sharedPreferences = getSharedPreferences("Data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        editor.putString("IP",IP);
        editor.putString("Study_name",Study_name);
        editor.putString("Agent_ID",Agent_ID);
        editor.putString("Agent_SYSID",Agent_SYSID);
        editor.putString("FormID",FormID);
        editor.apply();

        Intent intent = new Intent();
        intent.setClass(MainActivity.this,RecordVideo.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
        mScannerView.setAutoFocus(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    Runnable runnable = new Runnable(){
        @Override
        public void run() {
            try {
                CloseableHttpClient httpclient = HttpClients.createDefault();
//                Agent_ID = "000000";
//                UserID = "users";
//                System.out.println(Agent_ID);
//                System.out.println(UserID);
                switch (Post){
                    case "login":

                        HttpPost httpPost = new HttpPost(IP+"/WebServiceLogin.asmx/GlassLogin");
                        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                        nvps.add(new BasicNameValuePair("Agent_ID", Agent_ID));
                        nvps.add(new BasicNameValuePair("UserID", UserID));
                        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                        try(CloseableHttpResponse response = httpclient.execute(httpPost)) {
                            System.out.println(response.getCode() + " " + response.getReasonPhrase());
                            HttpEntity entity = response.getEntity();
                            String res_data  = EntityUtils.toString(entity); //response
                            System.out.println(res_data);
                            int index = res_data.indexOf("{");
                            int index2 = res_data.lastIndexOf("}") +1; //要取到}要+1
                            sub_str = res_data.substring(index,index2);

                            JSONObject jsonObject = new JSONObject(sub_str);
                            Agent_Name = jsonObject.getString("Agent_Name");
                            Agent_SYSID = jsonObject.getString("SYSID");
                            if(jsonObject.has("MSG")){
                                Message = "NoAgent";
                                return;
                            }
                            if(res_data.equals("請確認身分是否正確")){
                                Message = "NoAgent";
                                return;
                            }
                            Message = "success";
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "study":
                        HttpPost httpPost2 = new HttpPost(IP+"/WebServiceFormID.asmx/GetFormData");
                        List<NameValuePair> nvps2 = new ArrayList<NameValuePair>();
                        nvps2.add(new BasicNameValuePair("FormID", FormID));
                        httpPost2.setEntity(new UrlEncodedFormEntity(nvps2));
                        try(CloseableHttpResponse response = httpclient.execute(httpPost2)) {
                            System.out.println(response.getCode() + " " + response.getReasonPhrase());
                            HttpEntity entity = response.getEntity();
                            String res_data  = EntityUtils.toString(entity); //response
                            System.out.println(res_data);
                            int index = res_data.indexOf("{");
                            int index2 = res_data.lastIndexOf("}") +1; //要取到}要+1
                            sub_str = res_data.substring(index,index2);

                            JSONObject jsonObject = new JSONObject(sub_str);
                            if(jsonObject.has("MSG")){
                                Message = "NoData";
                                return;
                            }
                            Message = "success";
                        }  catch (ParseException e) {
                            Message = "fail";
                            e.printStackTrace();
                        } catch (JSONException e) {
                            Message = "fail";
                            e.printStackTrace();
                        }
                        break;
                }

            } catch(MalformedURLException e){
                Message = "fail";
                e.printStackTrace();
            } catch (IOException e) {
                Message = "fail";
                e.printStackTrace();
            }
        }
    };

    @Override
    public void handleResult(Result rawResult) {
        System.out.println("qrcode:"+rawResult.getText());
        String QR_data = rawResult.getText();
        Boolean comma = QR_data.contains(",");
        if(comma){
            QR_data_array = QR_data.split(",");
            String data2 = QR_data_array[1];
            int data2_len = data2.length();
            // 判斷陣列1 是否是中文
            if(data2.getBytes().length == data2_len) {
                Post = "login";
                Agent_ID = QR_data_array[0];
                UserID = QR_data_array[1];

                if(Agent_SYSID.equals("")){
                    try {
                        Thread thread = new Thread(runnable);
                        thread.start();
                        thread.join();
                        if(Message.equals("success")){
                            Message = "";
                            showShortMsg_Time("登入成功，登入人員:"+Agent_Name+"");
                            mScannerView.resumeCameraPreview(this);

                            TextView textView = findViewById(R.id.textView_Result);
                            textView.setText("請掃描教案QRCode");
                        }
                        else if(Message.equals("NoAgent")){
                            Message = "";
                            showShortMsg_Time("登入失敗，請確認QRCode。");
                            mScannerView.resumeCameraPreview(this);
                        }
                        else if(Message.equals("fail")){
                            Message = "";
                            showShortMsg_Time("網路異常，請稍後嘗試。");
                            mScannerView.resumeCameraPreview(this);
                        }
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    showShortMsg_Time("已經登入成功，請掃描教案QRCode");
                    mScannerView.resumeCameraPreview(this);
                }
            }
            else {
                if(Agent_SYSID.equals("")){
                    showShortMsg_Time("請先登入。");
                    mScannerView.resumeCameraPreview(this);
                }
                else {
                    Post = "study";
                    FormID = QR_data_array[0];
                    Study_name = QR_data_array[1];
                    try {
                        Thread thread2 = new Thread(runnable);
                        thread2.start();
                        thread2.join();
                        mScannerView.stopCamera();
                        if (Message.equals("fail")) {
                            Message = "";
                            showShortMsg_Time("網路異常，請稍後嘗試。");
                            mScannerView.resumeCameraPreview(this);
                        }
                        else if (Message.equals("fail")) {
                            Message = "";
                            showShortMsg_Time("網路異常，請稍後嘗試。");
                            mScannerView.resumeCameraPreview(this);
                        }
                        else {
                            mScannerView.resumeCameraPreview(this);
                            Check();
                        }
                    } catch (InterruptedException e) {
                        mScannerView.resumeCameraPreview(this);
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            showShortMsg_Time("QRCode錯誤, 請重新掃描。");
            mScannerView.resumeCameraPreview(this);
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

    private void showShortMsg_Time(String msg) {
        if(toast_show.equals("")){
            toast =  Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
            oneTime = System.currentTimeMillis();
            toast_show = "show";
        }else {
            twoTime = System.currentTimeMillis();
            if(twoTime - oneTime > 3000){
                toast_show = "";
            }
        }
    }
}