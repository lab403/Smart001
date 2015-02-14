package com.example.admin.phonemyapplication;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import android.os.Handler;
import android.os.HandlerThread;
import java.util.logging.LogRecord;


public class PhoneMainActivity extends Activity {

    private static Button butConnect;
    private static Button btnAutoConnect;
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static DataOutputStream dout = null;

    // thread1 - UI 主畫面
    private Handler mUIHandler=new Handler();
    // thread2
    private Handler mThreadHandler;
    private HandlerThread mThread;


    // thread3
    private Handler mThreadReciverHandler;
    private HandlerThread mThreadReciver;


    // MRCode常數區段
    public final String MRCODE_TRUN_OFF = "MRCode_CC_02";
    public final String MRCODE_RESET = "MRCode_CC_01";
    public final String MRCODE_SLEEP = "MRCode_CC_00";
    public final String MRCODE_CONNECT = "TESTTEST123123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_main);

        butConnect=(Button)findViewById(R.id.but1);
        butConnect.setOnClickListener(ChkIP);

        btnAutoConnect=(Button)findViewById(R.id.button);
        btnAutoConnect.setOnClickListener(autoConnect);

        //editInput=(EditText)findViewById(R.id.editText);

    }

//    public void ChkIP(View v) {// IP手動設定
//
//        EditText serverIP = (EditText) findViewById(R.id.editText);
//        //EditText serverPORT = (EditText) findViewById(R.id.ServerPORT);
//
//        int Error = 0;
//        String IP4[] = serverIP.getText().toString().split("\\.");
//
//        if(serverIP.getText().toString().isEmpty()){
//            //請輸入ip
//            Error = 1;
//        }else if(IP4.length!=4){
//            //錯誤的ip格式
//            Error = 2;
//            //}else if(Integer.parseInt(serverPORT.getText().toString()) <= 0 || Integer.parseInt(serverPORT.getText().toString()) > 65535){
//            //	//錯誤的port
//            //	Error = 3;
//        }else
//            for (String IP : IP4)
//                if(Integer.parseInt(IP) < 0 || Integer.parseInt(IP) > 255)
//                    Error = 2;
//
//        String MES = null;
//        switch(Error){
//            case 0:
//                MES = "設定成功";
//
//                break;
//            case 1:
//                MES = "請輸入IP";
//                break;
//            case 2:
//                MES ="不正確的IP格式";
//                break;
//            //case 3:
//                //MES = getString(R.string.port_error);
//              //  break;
//        }
//        //Toast.makeText(getApplication(),MES, Toast.LENGTH_LONG).show(); // 列印異常資訊
//        Toast.makeText(getApplication(),MES,Toast.LENGTH_LONG).show();
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_phone_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private Button.OnClickListener autoConnect=new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            // 宣告handler
            mThread=new HandlerThread("autoConnect");
            mThread.start();
            // 找到handler
            mThreadHandler=new Handler(mThread.getLooper());
            // 啟動輸出字串
            mThreadHandler.post(autoConnectTask);
        }
    };



    // Thread工作
    private Runnable autoConnectTask=new Runnable() {
        @Override
        public void run() {
            // try-catch區段
            try {
                byte[] msg = new String("connection successfully!!!").getBytes();
                InetAddress addr = InetAddress.getByName("255.255.255.255");
                DatagramSocket client = new DatagramSocket();
                DatagramPacket sendPack =
                        new DatagramPacket(msg, msg.length, addr,8888);
                client.send(sendPack);
                Toast.makeText(getBaseContext(),"自動搜尋",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                Toast.makeText(getBaseContext(),e.toString(),Toast.LENGTH_LONG).show();
            }
        }
    };


    private Button.OnClickListener ChkIP=new Button.OnClickListener(){
        @Override
        public void onClick(View v) {


            EditText serverIP = (EditText) findViewById(R.id.editText);
            //EditText serverPORT = (EditText) findViewById(R.id.ServerPORT);


            int Error = 0;
            String IP4[] = serverIP.getText().toString().split("\\.");


            if(serverIP.getText().toString().isEmpty()){
                //請輸入ip
                Error = 1;
            }else if(IP4.length!=4){
                //錯誤的ip格式
                Error = 2;
                //}else if(Integer.parseInt(serverPORT.getText().toString()) <= 0 || Integer.parseInt(serverPORT.getText().toString()) > 65535){
                //	//錯誤的port
                //	Error = 3;
            }else
                for (String IP : IP4)
                    if(Integer.parseInt(IP) < 0 || Integer.parseInt(IP) > 255)
                        Error = 2;

            //String MES = null;

            switch(Error){
                case 0:
                    // MES = "設定成功";
                    SERVER_IP= serverIP.getText().toString();
                    SERVER_PORT = 3579;

                    // 宣告handler
                    mThread=new HandlerThread("writer");
                    mThread.start();
                    // 找到handler
                    mThreadHandler=new Handler(mThread.getLooper());
                    // 啟動輸出字串
                    mThreadHandler.post(socketWrite);


                    break;
//                    case 1:
//                        MES = "請輸入IP";
//                        break;
//                    case 2:
//                        MES ="不正確的IP格式";
//                        break;
//                    //case 3:
                //MES = getString(R.string.port_error);
                //  break;
            }
            //Toast.makeText(getApplication(),MES, Toast.LENGTH_LONG).show(); // 列印異常資訊
            // Toast.makeText(getApplication(),MES,Toast.LENGTH_LONG).show();
        }



    };

    // Thread工作
    private Runnable socketWrite=new Runnable() {
        @Override
            public void run() {
            // try-catch區段

            String in;
            Socket s = null;
            DataInputStream din = null;
            DataOutputStream dout = null;
            try {
                s = new Socket(SERVER_IP, SERVER_PORT);// 連接伺服器(IP依您電腦位址來修改)
                dout = new DataOutputStream(s.getOutputStream());// 得到輸出串流
                dout.writeUTF(MRCODE_CONNECT);// 向伺服器發送訊息

                din = new DataInputStream(s.getInputStream());// 得到輸出串流
                in =din.readUTF();// 向伺服器發送訊息

                if(in.equalsIgnoreCase("Connected")) {
                    Toast.makeText(getApplication(),"Connected!!!!!!!!!!!!!",Toast.LENGTH_LONG).show();
                }else{

                    Toast.makeText(getApplication(),in,Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                Toast.makeText(getApplication(), e.toString(), Toast.LENGTH_LONG).show(); // 列印異常資訊
            } finally {// 用finally語句塊確保動作執行
                try {
                    if (dout != null)  dout.close();// 關閉輸入串流
                    if (s != null) 	s.close();// 關閉Socket連接

                } catch (Exception e) {
                    Toast.makeText(getApplication(), e.toString(),Toast.LENGTH_LONG).show();
                }
            }
        }
    };


}
