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
import java.net.ServerSocket;
import java.net.Socket;
import android.os.Handler;
import android.os.HandlerThread;


public class PhoneMainActivity extends Activity implements Button.OnClickListener  {

    private static Button btnConnect;
    private static Button btnAutoConnect;
    private static EditText serverIP;

    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static DataOutputStream dout = null;

    // thread1 - UI 主畫面
    private Handler mUIHandler=new Handler();

    // thread2 - 廣播本機IP
    private Handler mThreadHandler;
    private HandlerThread mThread;

    // thread3 - 接收serverIP
    private Handler mThreadReciverHandler;
    private HandlerThread mThreadReciver;

    private boolean tThreadStop=false;

    // MRCode常數區段
    public final String MRCODE_TRUN_OFF = "MRCode_CC_02";
    public final String MRCODE_RESET = "MRCode_CC_01";
    public final String MRCODE_SLEEP = "MRCode_CC_00";
    public final String MRCODE_CONNECT = "TESTTEST123123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_main);

        // 連線
        btnConnect=(Button)findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);

        // 廣播本機IP
        btnAutoConnect=(Button)findViewById(R.id.btnAutoConnect);
        btnAutoConnect.setOnClickListener(this);

        // 取得字串
        serverIP = (EditText) findViewById(R.id.editText);
        //EditText serverPORT = (EditText) findViewById(R.id.ServerPORT);
    }

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnConnect:
                mCheckIP();
                break;
            case R.id.btnAutoConnect:
                mAutoConnection();
                break;
        }
    }

    // 方法:判斷ip
    private void mCheckIP(){
        int Error = 0;
        String IP4[] = serverIP.getText().toString().split("\\.");
        if(serverIP.getText().toString().isEmpty()){
            //請輸入ip
            Error = 1;
        }else if(IP4.length!=4){
            //錯誤的ip格式
            Error = 2;
            /*
                暫時去除檢查PORT功能,因為目前port都固定
             */
            //}else if(Integer.parseInt(serverPORT.getText().toString()) <= 0 ||
            //        Integer.parseInt(serverPORT.getText().toString()) > 65535){
            //    //錯誤的port
            //    Error = 3;
        }else
            for (String IP : IP4)
                if(Integer.parseInt(IP) < 0 || Integer.parseInt(IP) > 255)
                    Error = 2;
        String MES = null;
        switch(Error){
            case 0:
                mConnectServer();
                break;
            case 1:
                MES = "請輸入IP";
                break;
            case 2:
                MES ="不正確的IP格式";
                break;
            case 3:
                MES = "port錯誤";
                break;
        }
        Toast.makeText(getApplication(),MES, Toast.LENGTH_LONG).show(); // 列印異常資訊
        Toast.makeText(getApplication(),MES,Toast.LENGTH_LONG).show();
    }

    // 方法:建立連線
    private void mConnectServer(){
        SERVER_IP= serverIP.getText().toString();
        SERVER_PORT = 3579;

        // 如thread存在則移除它
        if(! mThread.isInterrupted()) {
            try{
                mThread.interrupt();
            }catch(Exception e){
                Toast.makeText(getBaseContext(),e.toString(),Toast.LENGTH_LONG).show();
            }
        }

        // 建立與SERVER連線
        mThread=new HandlerThread("writer");
        mThread.start();
        mThreadHandler=new Handler(mThread.getLooper());
        mThreadHandler.post(tSocketClient);
    }

    // 方法:自動搜尋
    private void mAutoConnection(){
        // 如thread存在則移除它
        if(mThread!=null) mThread.interrupt();

        // 宣告handler
        mThread = new HandlerThread("autoConnect");
        mThread.start();
        mThreadHandler = new Handler(mThread.getLooper());
        mThreadHandler.post(tSendBrocast);
    }

    // 執行緒:廣播本機IP
    private Runnable tSendBrocast=new Runnable() {
        @Override
        public void run() {
            String myIp;
            try {
                // 調用自訂之外部方法取得本地IP
                myIp = utils.getIPAddress(getApplication());
                byte[] msg = new String(myIp).getBytes();
                InetAddress addr = InetAddress.getByName("255.255.255.255");
                DatagramSocket client = new DatagramSocket();
                DatagramPacket sendPack =
                        new DatagramPacket(msg, msg.length, addr, 8899);
                client.send(sendPack);
                Toast.makeText(getBaseContext(), "自動搜尋", Toast.LENGTH_SHORT).show();

                // 如thread存在則移除它
                if(mThreadReciver!=null) mThreadReciver.interrupt();
                // 啟動另一個thread接收SERVER IP
                mThreadReciver = new HandlerThread("recver");
                mThreadReciver.start();
                mThreadReciverHandler = new Handler(mThreadReciver.getLooper());
                mThreadReciverHandler.post(tReciveFromPc);

            } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    };

    // 執行緒:把IP填入EDITTEXT並啟動連線
    private Runnable tAutoConnectServer=new Runnable() {
        @Override
        public void run() {
            // 把IP填入EDITTEXT
            EditText serverIP = (EditText) findViewById(R.id.editText);
            serverIP.setText(SERVER_IP);

            Toast.makeText(getBaseContext(), " 把IP填入EDITTEXT", Toast.LENGTH_LONG).show();

            // 呼叫連線方法
            mConnectServer();
        }
    };

    // 執行緒:接收serverIP
    private Runnable tReciveFromPc=new Runnable() {
        @Override
        public void run() {
            String in;
            ServerSocket ss=null;
            Socket cs=null;
            DataInputStream din=null;
            try {
                //
                ss = new ServerSocket(3578);
                cs = ss.accept();
                din = new DataInputStream(cs.getInputStream());// 得到輸出串流
                in = din.readUTF();// 向伺服器發送訊息

                if (in.isEmpty()) {
                    Toast.makeText(getApplication(), "Empty", Toast.LENGTH_LONG).show();
                } else {
                    SERVER_IP = in;
                    Toast.makeText(getApplication(), SERVER_IP, Toast.LENGTH_LONG).show();
                    // 把IP填入EDITTEXT並啟動連線
                    mUIHandler.post(tAutoConnectServer);
                }

            } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
            }finally {
                try {
                    if (cs!=null) cs.close();
                    if (ss!=null) ss.close();
                    if (din!=null) din.close();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    // 執行緒:作為客戶端連線
    private Runnable tSocketClient=new Runnable() {
        @Override
        public void run() {
            String in;
            Socket cs = null;
            DataInputStream din;
            try {
                cs = new Socket(SERVER_IP, SERVER_PORT);// 連接伺服器(IP依您電腦位址來修改)
                dout = new DataOutputStream(cs.getOutputStream());// 得到輸出串流
                dout.writeUTF(MRCODE_CONNECT);// 向伺服器發送訊息

                din = new DataInputStream(cs.getInputStream());// 得到輸出串流
                in =din.readUTF();// 向伺服器發送訊息

                if(in.equalsIgnoreCase("Connected")) {
                    Toast.makeText(getApplication(),"Connected",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplication(),in,Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                Toast.makeText(getApplication(), e.toString(), Toast.LENGTH_LONG).show(); // 列印異常資訊
            } finally {// 用finally語句塊確保動作執行
                try {
                    if (dout != null) dout.close();// 關閉輸入串流
                    if (cs != null) cs.close();// 關閉Socket連接
                } catch (Exception e) {
                    Toast.makeText(getApplication(), e.toString(),Toast.LENGTH_LONG).show();
                }
            }
        }
    };
}