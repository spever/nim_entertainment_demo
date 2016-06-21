package com.netease.nim.chatroom.demo.entertainment.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.nim.chatroom.demo.R;
import com.netease.nim.chatroom.demo.base.util.log.LogUtil;
import com.netease.nim.chatroom.demo.entertainment.adapter.GiftAdapter;
import com.netease.nim.chatroom.demo.entertainment.constant.GiftConstant;
import com.netease.nim.chatroom.demo.entertainment.constant.GiftType;
import com.netease.nim.chatroom.demo.entertainment.helper.ChatRoomMemberCache;
import com.netease.nim.chatroom.demo.entertainment.helper.GiftCache;
import com.netease.nim.chatroom.demo.entertainment.model.Gift;
import com.netease.nim.chatroom.demo.im.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.chatroom.demo.permission.MPermission;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionDenied;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionGranted;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nim.chatroom.demo.permission.util.MPermissionUtil;
import com.netease.nim.chatroom.demo.thirdparty.live.LivePlayer;
import com.netease.nim.chatroom.demo.thirdparty.live.LiveSurfaceView;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMember;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LiveActivity extends LivePlayerBaseActivity implements LivePlayer.ActivityProxy {
    private static final String TAG = "LiveActivity";
    private final static String EXTRA_ROOM_ID = "ROOM_ID";
    private final static String EXTRA_URL = "EXTRA_URL";
    private final int LIVE_PERMISSION_REQUEST_CODE = 100;

    // view
    private LiveSurfaceView liveView;
    private View backBtn;
    private View startLayout;
    private Button startBtn;
    private ImageButton switchBtn;
    private TextView noGiftText;
    private ViewGroup liveFinishLayout;
    private Button liveFinishBtn;

    // state
    private boolean disconnected = false; // 是否断网（断网重连用）
    private boolean isStartLive = false; // 是否开始直播推流

    // 推流参数
    private LivePlayer livePlayer;

    // data
    private List<Gift> giftList = new ArrayList<>(); // 礼物列表数据

    public static void start(Context context, String roomId, String url) {
        Intent intent = new Intent();
        intent.setClass(context, LiveActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_URL, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadGift();
        setListener();
    }

    @Override
    protected int getActivityLayout() {
        return R.layout.live_player_activity;
    }

    @Override
    protected int getLayoutId() {
        return R.id.live_layout;
    }

    @Override
    public Activity getActivity() {
        return LiveActivity.this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 恢复直播
        if(livePlayer != null) {
            livePlayer.onActivityResume();
        }
    }

    protected void onPause() {
        super.onPause();
        // 暂停直播
        if(livePlayer != null) {
            livePlayer.onActivityPause();
        }
    }

    @Override
    protected void onDestroy() {
        // 释放资源
        if (livePlayer != null) {
            livePlayer.resetLive();
        }

        giftList.clear();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(livePlayer != null) {
            livePlayer.tryStop();
        }

        if (isStartLive) {
            logoutChatRoom();
        } else {
            NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
            clearChatRoom();
        }
    }

    // 退出聊天室
    private void logoutChatRoom() {
        EasyAlertDialogHelper.createOkCancelDiolag(this, null, getString(R.string.finish_confirm),
                getString(R.string.confirm), getString(R.string.cancel), true,
                new EasyAlertDialogHelper.OnDialogActionListener() {
                    @Override
                    public void doCancelAction() {

                    }

                    @Override
                    public void doOkAction() {
                        isStartLive = false;
                        liveFinishLayout.setVisibility(View.VISIBLE);
                        TextView masterNickText = findView(R.id.finish_master_name);
                        masterNickText.setText(TextUtils.isEmpty(masterNick) ? roomInfo.getCreator() : masterNick);

                        // 释放资源
                        if(livePlayer != null) {
                            livePlayer.resetLive();
                        }
                    }
                }).show();
    }

    // 清空聊天室缓存
    private void clearChatRoom() {
        ChatRoomMemberCache.getInstance().clearRoomCache(roomId);
        finish();
    }

    /***************************** 初始化 *****************************/

    protected void initParam() {
        // 初始化推流
        livePlayer = new LivePlayer(liveView, url, this);
    }

    protected void findViews() {
        super.findViews();
        liveView = (LiveSurfaceView) findViewById(R.id.live_view);
        backBtn = findView(R.id.BackBtn);
        startLayout = findViewById(R.id.start_layout);
        startBtn = (Button) findViewById(R.id.start_live_btn);
        switchBtn = (ImageButton) findViewById(R.id.switch_btn);
        noGiftText = findView(R.id.no_gift_tip);
        controlLayout = findView(R.id.control_layout);

        // 直播结束
        liveFinishLayout = findView(R.id.live_finish_layout);
        liveFinishBtn = findView(R.id.finish_btn);
        liveFinishBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                clearChatRoom();
            }
        });
    }

    // 初始化礼物布局
    protected void findGiftLayout() {
        super.findGiftLayout();
        adapter = new GiftAdapter(giftList, this);
        giftView.setAdapter(adapter);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        ChatRoomMember roomMember = ChatRoomMemberCache.getInstance().getChatRoomMember(roomId, roomInfo.getCreator());
        if (roomMember != null) {
            masterNick = roomMember.getNick();
        }
        masterNameText.setText(TextUtils.isEmpty(masterNick) ? roomInfo.getCreator() : masterNick);
    }

    // 取出缓存的礼物
    private void loadGift() {
        Map gifts = GiftCache.getInstance().getGift(roomId);
        if (gifts == null) {
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> it = gifts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            int type = entry.getKey();
            int count = entry.getValue();
            giftList.add(new Gift(GiftType.typeOfValue(type), GiftConstant.titles[type], count, GiftConstant.images[type]));
        }
    }

    private void setListener() {
        startBtn.setOnClickListener(buttonClickListener);
        backBtn.setOnClickListener(buttonClickListener);
        switchBtn.setOnClickListener(buttonClickListener);
        giftBtn.setOnClickListener(buttonClickListener);
        giftLayout.setOnClickListener(buttonClickListener);
    }

    OnClickListener buttonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start_live_btn:
                    if (disconnected) {
                        // 如果网络不通
                        Toast.makeText(getActivity(), R.string.net_broken, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startBtn.setText("准备中...");
                    requestLivePermission(); // 请求权限
                    break;
                case R.id.BackBtn:
                    if(isStartLive && livePlayer != null) {
                        livePlayer.tryStop();
                        logoutChatRoom();
                    } else {
                        NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                        clearChatRoom();
                    }

                    break;
                case R.id.switch_btn:
                    if (livePlayer != null) {
                        livePlayer.switchCamera();
                    }
                    break;
                case R.id.gift_btn:
                    showGiftLayout();
                    break;
                case R.id.gift_layout:
                    giftLayout.setVisibility(View.GONE);
                    break;
            }
        }
    };

    // 显示礼物布局
    private void showGiftLayout() {
        inputPanel.collapse(true);// 收起软键盘
        giftLayout.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        if (adapter.getCount() == 0) {
            // 暂无礼物
            noGiftText.setVisibility(View.VISIBLE);
        } else {
            noGiftText.setVisibility(View.GONE);
        }
    }

    protected void updateGiftList(GiftType type) {
        if (!updateGiftCount(type)) {
            giftList.add(new Gift(type, GiftConstant.titles[type.getValue()], 1, GiftConstant.images[type.getValue()]));
        }
        GiftCache.getInstance().saveGift(roomId, type.getValue());
    }

    // 更新收到礼物的数量
    private boolean updateGiftCount(GiftType type) {
        for (Gift gift : giftList) {
            if (type == gift.getGiftType()) {
                gift.setCount(gift.getCount() + 1);
                return true;
            }
        }
        return false;
    }

    /**
     * ********************************** 断网重连处理 **********************************
     */

    // 网络连接成功
    protected void onConnected() {
        if (disconnected == false) {
            return;
        }

        LogUtil.i(TAG, "live on connected");
        if (livePlayer != null) {
            livePlayer.restartLive();
        }

        disconnected = false;
    }

    // 网络断开
    protected void onDisconnected() {
        LogUtil.i(TAG, "live on disconnected");

        if (livePlayer != null) {
            livePlayer.stopLive();
        }
        disconnected = true;
    }


    /*********************** 录音摄像头权限申请 *******************************/

    // 权限控制
    private static final String[] LIVE_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE};

    private void requestLivePermission() {
        MPermission.with(this)
                .addRequestCode(LIVE_PERMISSION_REQUEST_CODE)
                .permissions(LIVE_PERMISSIONS)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionGranted() {
        Toast.makeText(getActivity(), "授权成功", Toast.LENGTH_SHORT).show();
        if(livePlayer.startStopLive()) {
            isStartLive = true;
            startLayout.setVisibility(View.GONE);
        }
    }

    @OnMPermissionDenied(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDenied() {
        List<String> deniedPermissions = MPermission.getDeniedPermissions(this, LIVE_PERMISSIONS);
        String tip = "您拒绝了权限" + MPermissionUtil.toString(deniedPermissions) + "，无法开启直播";
        Toast.makeText(getActivity(), tip, Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionNeverAskAgain(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDeniedAsNeverAskAgain() {
        List<String> deniedPermissions = MPermission.getDeniedPermissionsWithoutNeverAskAgain(this, LIVE_PERMISSIONS);
        List<String> neverAskAgainPermission = MPermission.getNeverAskAgainPermissions(this, LIVE_PERMISSIONS);
        StringBuilder sb = new StringBuilder();
        sb.append("无法开启直播，请到系统设置页面开启权限");
        sb.append(MPermissionUtil.toString(neverAskAgainPermission));
        if(deniedPermissions != null && !deniedPermissions.isEmpty()) {
            sb.append(",下次询问请授予权限");
            sb.append(MPermissionUtil.toString(deniedPermissions));
        }

        Toast.makeText(getActivity(), sb.toString(), Toast.LENGTH_LONG).show();
    }
}
