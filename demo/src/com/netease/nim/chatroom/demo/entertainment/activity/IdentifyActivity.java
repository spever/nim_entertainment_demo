package com.netease.nim.chatroom.demo.entertainment.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.netease.nim.chatroom.demo.DemoCache;
import com.netease.nim.chatroom.demo.R;
import com.netease.nim.chatroom.demo.base.ui.TActivity;
import com.netease.nim.chatroom.demo.entertainment.http.ChatRoomHttpClient;
import com.netease.nim.chatroom.demo.im.business.LogoutHelper;
import com.netease.nim.chatroom.demo.permission.MPermission;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionDenied;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionGranted;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;

/**
 * Created by hzxuwen on 2016/3/2.
 */
public class IdentifyActivity extends TActivity implements View.OnClickListener{
    private final int BASIC_PERMISSION_REQUEST_CODE = 100;
    private Button masterBtn;
    private Button audienceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.identify_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.drawable.actionbar_logo_white);
        setSupportActionBar(toolbar);

        findViews();
        registerObservers(true);
        requestBasicPermission(); // 申请APP基本权限
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        registerObservers(false);
    }

    private void registerObservers(boolean register) {
        NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(userStatusObserver, register);
    }

    Observer<StatusCode> userStatusObserver = new Observer<StatusCode>() {
        @Override
        public void onEvent(StatusCode statusCode) {
            if (statusCode.wontAutoLogin()) {
                LogoutHelper.logout(IdentifyActivity.this, true);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entertainment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_logout:
                LogoutHelper.logout(IdentifyActivity.this, false);
                break;
            case R.id.action_about:
                startActivity(new Intent(IdentifyActivity.this, AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void findViews() {
        masterBtn = (Button) findViewById(R.id.master_btn);
        audienceBtn = (Button) findViewById(R.id.audience_btn);

        masterBtn.setOnClickListener(this);
        audienceBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.master_btn:
//                UserPreferences.setTeacherIdentify(true);
                masterEnterRoom();
                break;
            case R.id.audience_btn:
                startActivity(new Intent(IdentifyActivity.this, EnterRoomActivity.class));
                break;
            default:
                break;
        }
    }

    private void masterEnterRoom() {
        ChatRoomHttpClient.getInstance().masterEnterRoom(DemoCache.getAccount(), "直播间",
                new ChatRoomHttpClient.ChatRoomHttpCallback<ChatRoomHttpClient.EnterRoomParam>() {
                    @Override
                    public void onSuccess(ChatRoomHttpClient.EnterRoomParam enterRoomParam) {
                        Toast.makeText(IdentifyActivity.this, "登录直播间：" + enterRoomParam.getRoomId(), Toast.LENGTH_SHORT).show();
                        LiveActivity.start(IdentifyActivity.this, enterRoomParam.getRoomId(), enterRoomParam.getUrl());
                    }

                    @Override
                    public void onFailed(int code, String errorMsg) {
                        Toast.makeText(IdentifyActivity.this, "创建直播间失败，code:" + code, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 基本权限管理
     */
    private void requestBasicPermission() {
        MPermission.with(IdentifyActivity.this)
                .addRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess(){
        Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed(){
        Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
    }
}
