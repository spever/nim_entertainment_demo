package com.netease.nim.chatroom.demo.entertainment.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.nim.chatroom.demo.DemoCache;
import com.netease.nim.chatroom.demo.R;
import com.netease.nim.chatroom.demo.entertainment.adapter.GiftAdapter;
import com.netease.nim.chatroom.demo.entertainment.constant.GiftType;
import com.netease.nim.chatroom.demo.entertainment.helper.ChatRoomMemberCache;
import com.netease.nim.chatroom.demo.entertainment.helper.SimpleCallback;
import com.netease.nim.chatroom.demo.entertainment.module.GiftAttachment;
import com.netease.nim.chatroom.demo.entertainment.module.LikeAttachment;
import com.netease.nim.chatroom.demo.im.config.UserPreferences;
import com.netease.nim.chatroom.demo.im.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.chatroom.demo.permission.MPermission;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionDenied;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionGranted;
import com.netease.nim.chatroom.demo.thirdparty.video.NEVideoView;
import com.netease.nim.chatroom.demo.thirdparty.video.VideoPlayer;
import com.netease.nim.chatroom.demo.thirdparty.video.constant.VideoConstant;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.chatroom.ChatRoomMessageBuilder;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMember;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * 观众端
 * Created by hzxuwen on 2016/3/18.
 */
public class AudienceActivity extends LivePlayerBaseActivity implements VideoPlayer.VideoPlayerProxy {
    private static final String TAG = AudienceActivity.class.getSimpleName();
    private final int BASIC_PERMISSION_REQUEST_CODE = 110;
    private final static String EXTRA_ROOM_ID = "ROOM_ID";
    private final static String EXTRA_URL = "EXTRA_URL";

    // view
    private View closeBtn;
    private ImageButton shareBtn;
    private ImageButton likeBtn;
    private ViewGroup liveFinishLayout;
    private View liveFinishBtn;
    private TextView finishTipText;
    private TextView finishNameText;
    private TextView preparedText;
    private Button sendGiftBtn;

    // 播放器
    private VideoPlayer videoPlayer;
    // 发送礼物频率控制使用
    private long lastClickTime = 0;
    // 选中的礼物
    private int giftPosition = -1;

    // state
    private boolean isStartLive = false; // 推流是否开始

    public static void start(Context context, String roomId, String url) {
        Intent intent = new Intent();
        intent.setClass(context, AudienceActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_URL, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getActivityLayout() {
        return R.layout.audience_activity;
    }

    @Override
    protected int getLayoutId() {
        return R.id.audience_layout;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 恢复播放
        if(videoPlayer != null) {
            videoPlayer.onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 暂停播放
        if(videoPlayer != null) {
            videoPlayer.onActivityPause();
        }
    }

    @Override
    protected void onDestroy() {
        // 释放资源
        if (videoPlayer != null) {
            videoPlayer.resetVideo();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
       super.onBackPressed();
        finishLive();
    }

    // 离开聊天室
    private void logoutChatRoom() {
        EasyAlertDialogHelper.createOkCancelDiolag(this, null, getString(R.string.finish_confirm),
                getString(R.string.confirm), getString(R.string.cancel), true,
                new EasyAlertDialogHelper.OnDialogActionListener() {
                    @Override
                    public void doCancelAction() {

                    }

                    @Override
                    public void doOkAction() {
                        NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                        clearChatRoom();
                    }
                }).show();
    }

    private void clearChatRoom() {
        ChatRoomMemberCache.getInstance().clearRoomCache(roomId);
        finish();
    }

    @Override
    protected void onConnected() {

    }

    @Override
    protected void onDisconnected() {

    }

    /******************************** 初始化 *******************************/

    // 初始化播放器参数
    protected void initParam() {
        requestBasicPermission(); // 申请APP基本权限
    }

    private void initAudienceParam() {
        NEVideoView videoView = findView(R.id.video_view);

        videoPlayer = new VideoPlayer(AudienceActivity.this, videoView, null, url,
                UserPreferences.getPlayerStrategy(), this, VideoConstant.VIDEO_SCALING_MODE_FILL_SCALE);

        videoPlayer.openVideo();
    }

    protected void findViews() {
        super.findViews();

        closeBtn = findView(R.id.close_btn);
        controlLayout = findView(R.id.control_layout);
        shareBtn = findView(R.id.share_btn);
        shareBtn.setVisibility(View.GONE);
        likeBtn = findView(R.id.like_btn);

        closeBtn.setOnClickListener(buttonClickListener);
        shareBtn.setOnClickListener(buttonClickListener);
        giftBtn.setOnClickListener(buttonClickListener);
        likeBtn.setOnClickListener(buttonClickListener);

        // 直播结束布局
        liveFinishLayout = findView(R.id.live_finish_layout);
        liveFinishBtn = findView(R.id.finish_close_btn);
        finishTipText = findView(R.id.finish_tip_text);
        finishNameText = findView(R.id.finish_master_name);
        finishTipText.setText(R.string.loading);

        liveFinishBtn.setOnClickListener(buttonClickListener);

        preparedText = findView(R.id.prepared_text);
    }

    // 初始化礼物布局
    protected void findGiftLayout() {
        super.findGiftLayout();
        sendGiftBtn = findView(R.id.send_gift_btn);
        sendGiftBtn.setOnClickListener(buttonClickListener);

        adapter = new GiftAdapter(this);
        giftView.setAdapter(adapter);

        giftLayout.setOnClickListener(buttonClickListener);
        giftView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                giftPosition = position;
            }
        });

    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.close_btn:
                    finishLive();
                    break;
                case R.id.finish_close_btn:
                    NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                    clearChatRoom();
                    break;
                case R.id.share_btn:
                    break;
                case R.id.gift_btn:
                    showGiftLayout();
                    break;
                case R.id.like_btn:
                    periscopeLayout.addHeart();
                    sendLike();
                    break;
                case R.id.gift_layout:
                    giftLayout.setVisibility(View.GONE);
                    giftPosition = -1;
                    break;
                case R.id.send_gift_btn:
                    sendGift();
                    break;
            }
        }
    };

    private void finishLive() {
        if (isStartLive) {
            logoutChatRoom();
        } else {
            NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
            clearChatRoom();
        }
    }

    protected void updateUI() {
        super.updateUI();
        ChatRoomMemberCache.getInstance().fetchMember(roomId, roomInfo.getCreator(), new SimpleCallback<ChatRoomMember>() {
            @Override
            public void onResult(boolean success, ChatRoomMember result) {
                if (success) {
                    masterNick = result.getNick();
                    String nick = TextUtils.isEmpty(masterNick) ? result.getAccount() : masterNick;
                    masterNameText.setText(nick);
                    finishNameText.setText(nick);
                }
            }
        });
    }

    /**
     * 基本权限管理
     */
    private void requestBasicPermission() {
        MPermission.with(AudienceActivity.this)
                .addRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(
                        Manifest.permission.READ_PHONE_STATE)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess(){
        initAudienceParam();
        Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed(){
        finish();
        Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    /************************* 点赞爱心 ********************************/

    // 发送点赞爱心
    private void sendLike() {
        if (!isFastClick()) {
            LikeAttachment attachment = new LikeAttachment();
            ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
            setMemberType(message);
            NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
        }
    }

    // 发送爱心频率控制
    private boolean isFastClick() {
        long currentTime = System.currentTimeMillis();
        long time = currentTime - lastClickTime;
        if (time > 0 && time < 1000) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }

    /*********************** 收发礼物 ******************************/

    // 显示礼物列表
    private void showGiftLayout() {
        giftLayout.setVisibility(View.VISIBLE);
        inputPanel.collapse(true);
    }

    // 发送礼物
    private void sendGift() {
        if (giftPosition == -1) {
            Toast.makeText(AudienceActivity.this, "请选择礼物", Toast.LENGTH_SHORT).show();
            return;
        }
        giftLayout.setVisibility(View.GONE);
        GiftAttachment attachment = new GiftAttachment(GiftType.typeOfValue(giftPosition), 1);
        ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
        setMemberType(message);
        NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
        giftAnimation.showGiftAnimation(message);
        giftPosition = -1; // 发送完毕，置空
    }

    private void setMemberType(ChatRoomMessage message) {
        Map<String, Object> ext = new HashMap<>();
        ChatRoomMember chatRoomMember = ChatRoomMemberCache.getInstance().getChatRoomMember(roomId, DemoCache.getAccount());
        if (chatRoomMember != null && chatRoomMember.getMemberType() != null) {
            ext.put("type", chatRoomMember.getMemberType().getValue());
            message.setRemoteExtension(ext);
        }
    }

    @Override
    public boolean isDisconnected() {
        return false;
    }

    /**************************** 播放器状态回调 *****************************/

    @Override
    public void onError() {
        preparedText.setVisibility(View.VISIBLE);
        showFinishLayout();
    }

    @Override
    public void onCompletion() {
        isStartLive = false;
        showFinishLayout();
        TextView masterNickText = findView(R.id.finish_master_name);
        masterNickText.setText(TextUtils.isEmpty(masterNick) ? roomInfo.getCreator() : masterNick);
    }

    @Override
    public void onPrepared() {
        isStartLive = true;
        preparedText.setVisibility(View.GONE);
        liveFinishLayout.setVisibility(View.GONE);
    }

    // 显示直播已结束布局
    private void showFinishLayout() {
        liveFinishLayout.setVisibility(View.VISIBLE);
        finishTipText.setText(R.string.live_finish);
        inputPanel.collapse(true);
    }
}
