package com.netease.nim.chatroom.demo.thirdparty.live;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.netease.LSMediaCapture.lsMediaCapture;
import com.netease.LSMediaCapture.lsMessageHandler;
import com.netease.nim.chatroom.demo.base.util.log.LogUtil;

/**
 * Created by huangjun on 2016/3/28.
 */
public class LivePlayer implements lsMessageHandler {

    private final String TAG = "NELivePlayer";

    public interface ActivityProxy {
        Activity getActivity();
    }

    private ActivityProxy activityProxy;
    private LiveSurfaceView liveView;
    private Intent mAlertServiceIntent;
    private boolean live = false; // 是否已经开始推流（断网重连用），推流没有暂停

    // 视频采集器
    private lsMediaCapture mLSMediaCapture; // 直播实例
    private lsMediaCapture.LSLiveStreamingParaCtx mLSLiveStreamingParaCtx;

    // 基本配置
    private String mliveStreamingURL; // 推流地址
    private int mVideoEncodeWidth, mVideoEncodeHeight; // 推流分辨率

    // 音视频
    public static final int HAVE_AUDIO = 0;
    public static final int HAVE_VIDEO = 1;
    public static final int HAVE_AV = 2;

    // 协议
    public static final int FLV = 0;
    public static final int RTMP = 1;

    // 前后置摄像头
    public static final int CAMERA_POSITION_BACK = 0;
    public static final int CAMERA_POSITION_FRONT = 1;

    // 横竖屏
    public static final int CAMERA_ORIENTATION_PORTRAIT = 0;
    public static final int CAMERA_ORIENTATION_LANDSCAPE = 1;

    // 语音编码
    public static final int LS_AUDIO_CODEC_AAC = 0;
    public static final int LS_AUDIO_CODEC_SPEEX = 1;
    public static final int LS_AUDIO_CODEC_MP3 = 2;
    public static final int LS_AUDIO_CODEC_G711A = 3;
    public static final int LS_AUDIO_CODEC_G711U = 4;

    // 视频编码
    public static final int LS_VIDEO_CODEC_AVC = 0;
    public static final int LS_VIDEO_CODEC_VP9 = 1;
    public static final int LS_VIDEO_CODEC_H265 = 2;

    // 状态控制
    private boolean m_liveStreamingInitFinished = false;
    private boolean m_liveStreamingOn = false;
    private boolean m_liveStreamingPause = false;
    private boolean m_tryToStopLiveStreaming = false;

    public LivePlayer(LiveSurfaceView liveView, String url, ActivityProxy proxy) {
        this.liveView = liveView;
        this.mliveStreamingURL = url;
        this.activityProxy = proxy;
    }

    /**
     * ******************************** Activity 生命周期直播状态控制 ********************************
     */
    public void onActivityResume() {
        if (mLSMediaCapture != null) {
            //关闭推流固定图像
            mLSMediaCapture.stopVideoEncode();

            //关闭推流静音帧
            //mLSMediaCapture.stopAudioEncode();
        }
    }

    public void onActivityPause() {
        if (mLSMediaCapture != null) {
            //关闭视频Preview
            mLSMediaCapture.stopVideoPreview();

            if (m_tryToStopLiveStreaming) {
                m_liveStreamingOn = false;
            } else {
                //继续视频推流，推固定图像
                mLSMediaCapture.resumeVideoEncode();

                //释放音频采集资源
                //mLSMediaCapture.stopAudioRecord();
            }
        }
    }

    /**
     * ******************************** 初始化 ********************************
     */

    // 设置推流参数
    private void initLiveParam() {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //应用运行时，保持屏幕高亮，不锁屏

        m_liveStreamingOn = false;
        m_liveStreamingPause = false;
        m_tryToStopLivestreaming = false;

        // 推流URL和分辨率信息
        mVideoEncodeWidth = 320;
        mVideoEncodeHeight = 240;

        //创建直播实例
        mLSMediaCapture = new lsMediaCapture(this, getActivity(), mVideoEncodeWidth, mVideoEncodeHeight);

        liveView.setPreviewSize(mVideoEncodeWidth, mVideoEncodeHeight);

        //创建参数实例
        mLSLiveStreamingParaCtx = mLSMediaCapture.new LSLiveStreamingParaCtx();
        mLSLiveStreamingParaCtx.eHaraWareEncType = mLSLiveStreamingParaCtx.new HardWareEncEnable();
        mLSLiveStreamingParaCtx.eOutFormatType = mLSLiveStreamingParaCtx.new OutputFormatType();
        mLSLiveStreamingParaCtx.eOutStreamType = mLSLiveStreamingParaCtx.new OutputStreamType();
        mLSLiveStreamingParaCtx.sLSAudioParaCtx = mLSLiveStreamingParaCtx.new LSAudioParaCtx();
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.codec = mLSLiveStreamingParaCtx.sLSAudioParaCtx.new LSAudioCodecType();
        mLSLiveStreamingParaCtx.sLSVideoParaCtx = mLSLiveStreamingParaCtx.new LSVideoParaCtx();
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.codec = mLSLiveStreamingParaCtx.sLSVideoParaCtx.new LSVideoCodecType();
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.cameraPosition = mLSLiveStreamingParaCtx.sLSVideoParaCtx.new CameraPosition();
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.interfaceOrientation = mLSLiveStreamingParaCtx.sLSVideoParaCtx.new CameraOrientation();

        //配置音视频和camera参数
        configLiveStream();
    }

    private void configLiveStream() {
        //输出格式：视频、音频和音视频
        mLSLiveStreamingParaCtx.eOutStreamType.outputStreamType = HAVE_AV;

        //输出封装格式
        mLSLiveStreamingParaCtx.eOutFormatType.outputFormatType = RTMP;

        //摄像头参数配置
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.cameraPosition.cameraPosition = CAMERA_POSITION_BACK;//后置摄像头
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.interfaceOrientation.interfaceOrientation = CAMERA_ORIENTATION_PORTRAIT;//竖屏

        //音频编码参数配置
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.samplerate = 44100;
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.bitrate = 64000;
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.frameSize = 2048;
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.channelConfig = AudioFormat.CHANNEL_IN_MONO;
        mLSLiveStreamingParaCtx.sLSAudioParaCtx.codec.audioCODECType = LS_AUDIO_CODEC_AAC;

        //硬件编码参数设置
        mLSLiveStreamingParaCtx.eHaraWareEncType.hardWareEncEnable = false;

        //视频编码参数配置
//        if(mVideoResolution.equals("HD")) {
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.fps = 20;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.bitrate = 800000;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.codec.videoCODECType = LS_VIDEO_CODEC_AVC;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.width = 800;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.height = 600;
//        }
//        else if(mVideoResolution.equals("SD")) {
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.fps = 20;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.bitrate = 600000;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.codec.videoCODECType = LS_VIDEO_CODEC_AVC;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.width = 640;
//            mLSLiveStreamingParaCtx.sLSVideoParaCtx.height = 480;
//        }
//        else {
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.fps = 15;
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.bitrate = 250000;
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.codec.videoCODECType = LS_VIDEO_CODEC_AVC;
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.width = 320;
        mLSLiveStreamingParaCtx.sLSVideoParaCtx.height = 240;
//        }

        if (mLSMediaCapture != null) {
            //开始本地视频预览
            mLSMediaCapture.startVideoPreview(liveView, mLSLiveStreamingParaCtx.sLSVideoParaCtx.cameraPosition.cameraPosition);

            //初始化直播推流
            m_liveStreamingInitFinished = mLSMediaCapture.initLiveStream(mliveStreamingURL, mLSLiveStreamingParaCtx);
            m_liveStreamingInit = true;

            //开启统计日志上传机制，预计下个版本发布
            //mLSMediaCapture.setStaticsLog(false, "2.6");
        }
    }

    /**
     * ******************************** 直播控制 ********************************
     */

    /**
     * 返回是否成功开启
     * @return
     */
    public boolean startStopLive() {
        if (!m_liveStreamingOn) {
            if (!m_liveStreamingPause) {
                if (mliveStreamingURL.isEmpty()) {
                    return false;
                }

                initLiveParam();
                startAV();
                live = true;
                return true;
            }
        }

        return true;
    }


    //开始直播
    private void startAV() {
        if (mLSMediaCapture != null) {
            mLSMediaCapture.startLiveStreaming();
            m_liveStreamingOn = true;
            m_liveStreamingPause = false;
        }
    }

    //切换前后摄像头
    public void switchCamera() {
        if (mLSMediaCapture != null) {
            mLSMediaCapture.switchCamera();
        }
    }

    /**
     * 重启直播（例如：断网重连）
     * @return 是否开始重启
     */
    public boolean restartLive() {
        if (live) {
            // 必须是已经开始推流 才需要处理断网重新开始直播
            LogUtil.i(TAG, "restart live on connected");
            if (mLSMediaCapture != null) {
                mLSMediaCapture.resumeVideoPreview();
                mLSMediaCapture.initLiveStream(mliveStreamingURL, mLSLiveStreamingParaCtx);
                mLSMediaCapture.startLiveStreaming();
                m_tryToStopLivestreaming = false;
                return true;
            }
        }

        return false;
    }

    /**
     * 停止直播（例如：断网了）
     */
    public void stopLive() {
        m_tryToStopLivestreaming = true;
        if (mLSMediaCapture != null) {
            mLSMediaCapture.stopLiveStreaming();
            mLSMediaCapture.stopVideoPreview();
        }
    }

    public void tryStop() {
        m_tryToStopLiveStreaming = true;
    }

    public void resetLive() {
        if (mLSMediaCapture != null && m_liveStreamingInitFinished) {
            mLSMediaCapture.stopLiveStreaming();
            mLSMediaCapture.stopVideoPreview();
            mLSMediaCapture.destroyVideoPreview();
            mLSMediaCapture = null;

            m_liveStreamingInitFinished = false;
            m_liveStreamingOn = false;
            m_liveStreamingPause = false;
            m_tryToStopLiveStreaming = false;
        } else if (!m_liveStreamingInitFinished) {
        }

        if (m_liveStreamingInit) {
            m_liveStreamingInit = false;
        }

        if (mAlertServiceOn) {
            mAlertServiceIntent = new Intent(getActivity(), AlertService.class);
            getActivity().stopService(mAlertServiceIntent);
            mAlertServiceOn = false;
        }
    }

    private void onStopStartLive(boolean isRestart) {
        if (isRestart) {
            stopLive();
        } else {
            restartLive();
        }
    }

    /**
     * ******************************** lsMessageHandler ********************************
     */

    private boolean m_liveStreamingInit = false;
    private boolean m_tryToStopLivestreaming = false;
    private boolean mAlertServiceOn = false;
    private long mLastVideoProcessErrorAlertTime = 0;
    private long mLastAudioProcessErrorAlertTime = 0;

    //处理SDK抛上来的异常和事件
    @Override
    public void handleMessage(int msg, Object object) {
        final Context context = getActivity();
        switch (msg) {
            case MSG_INIT_LIVESTREAMING_OUTFILE_ERROR:
            case MSG_INIT_LIVESTREAMING_VIDEO_ERROR:
            case MSG_INIT_LIVESTREAMING_AUDIO_ERROR: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingInit) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_INIT_LIVESTREAMING_ERROR");
                    Intent intent = new Intent(context, AlertService.class);
                    intent.putExtras(bundle);
                    context.startService(intent);
                    mAlertServiceOn = true;
                }
            }
            case MSG_START_LIVESTREAMING_ERROR:
            case MSG_AUDIO_PROCESS_ERROR: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingOn && System.currentTimeMillis() - mLastAudioProcessErrorAlertTime >= 10000) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_AUDIO_PROCESS_ERROR");
                    Intent intent = new Intent(context, AlertService.class);
                    intent.putExtras(bundle);
                    context.startService(intent);
                    mAlertServiceOn = true;
                    mLastAudioProcessErrorAlertTime = System.currentTimeMillis();
                }

            }
            case MSG_VIDEO_PROCESS_ERROR: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingOn && System.currentTimeMillis() - mLastVideoProcessErrorAlertTime >= 10000) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_VIDEO_PROCESS_ERROR");
                    Intent intent = new Intent(context, AlertService.class);
                    intent.putExtras(bundle);
                    context.startService(intent);
                    mAlertServiceOn = true;
                    mLastVideoProcessErrorAlertTime = System.currentTimeMillis();
                }
            }
            case MSG_SEND_STATICS_LOG_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_SEND_STATICS_LOG_ERROR");
            }
            case MSG_AUDIO_SAMPLE_RATE_NOT_SUPPORT_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_AUDIO_SAMPLE_RATE_NOT_SUPPORT_ERROR");
            }
            case MSG_AUDIO_PARAMETER_NOT_SUPPORT_BY_HARDWARE_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_AUDIO_PARAMETER_NOT_SUPPORT_BY_HARDWARE_ERROR");
            }
            case MSG_NEW_AUDIORECORD_INSTANCE_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_NEW_AUDIORECORD_INSTANCE_ERROR");
            }
            case MSG_AUDIO_START_RECORDING_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_AUDIO_START_RECORDING_ERROR");
            }
            case MSG_OTHER_AUDIO_PROCESS_ERROR: {
                //Log.i(TAG, "test: in handleMessage, MSG_OTHER_AUDIO_PROCESS_ERROR");
                Toast.makeText(context, "初始化出错，正在重试", Toast.LENGTH_LONG).show();
                LogUtil.i(TAG, "live init error, restart");
                onStopStartLive(true);
                break;
            }
            case MSG_STOP_LIVESTREAMING_ERROR: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingOn) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_STOP_LIVESTREAMING_ERROR");
                    Intent intent = new Intent(context, AlertService.class);
                    intent.putExtras(bundle);
                    context.startService(intent);
                    mAlertServiceOn = true;
                }
                break;
            }
            case MSG_RTMP_URL_ERROR: {
                  /*
                  if(m_liveStreamingOn && System.currentTimeMillis() - mLastRtmpUrlErrorAlertTime >= 10000)
		    	  {
	      		      Bundle bundle = new Bundle();
	                  bundle.putString("alert", "MSG_RTMP_URL_ERROR");
	          	      Intent intent = new Intent(MediaPreviewActivity.this, AlertService.class);
	          	      intent.putExtras(bundle);
	      		      startService(intent);
	      		      mAlertServiceOn = true;
	      		      mLastRtmpUrlErrorAlertTime = System.currentTimeMillis();
		    	  }
		    	  */
                //Log.i(TAG, "test: in handleMessage, MSG_RTMP_URL_ERROR");
                break;
            }
            case MSG_URL_NOT_AUTH: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingInit) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_URL_NOT_AUTH");
                    Intent intent = new Intent(getActivity(), AlertService.class);
                    intent.putExtras(bundle);
                    getActivity().startService(intent);
                    mAlertServiceOn = true;
                }
                break;
            }
            case MSG_QOS_TO_STOP_LIVESTREAMING: {
                  /*
                  if(m_liveStreamingOn && System.currentTimeMillis() - mLastQosToStopLivestreamingAlertTime >= 10000)
		    	  {
		    	      Bundle bundle = new Bundle();
	                  bundle.putString("alert", "MSG_QOS_TO_STOP_LIVESTREAMING");
	          	      Intent intent = new Intent(MediaPreviewActivity.this, AlertService.class);
	          	      intent.putExtras(bundle);
	      		      startService(intent);
	      		      mAlertServiceOn = true;
	      		      mLastQosToStopLivestreamingAlertTime = System.currentTimeMillis();
		    	  }
		    	  */
                break;
            }
            case MSG_HW_VIDEO_PACKET_ERROR: {
                if (context == null) {
                    return;
                }
                if (m_liveStreamingOn) {
                    Bundle bundle = new Bundle();
                    bundle.putString("alert", "MSG_HW_VIDEO_PACKET_ERROR");
                    Intent intent = new Intent(context, AlertService.class);
                    intent.putExtras(bundle);
                    context.startService(intent);
                    mAlertServiceOn = true;
                }
                break;
            }
            case MSG_START_PREVIEW_FINISHED: {
                break;
            }
            case MSG_START_LIVESTREAMING_FINISHED: {
                break;
            }
            case MSG_STOP_LIVESTREAMING_FINISHED: {
                onStopStartLive(false);
                break;
            }
            case MSG_STOP_VIDEO_CAPTURE_FINISHED: {
                //Log.i(TAG, "test: in handleMessage: MSG_STOP_VIDEO_CAPTURE_FINISHED");
                if (!m_tryToStopLivestreaming && mLSMediaCapture != null) {
                    //继续视频推流，推最后一帧图像
                    mLSMediaCapture.resumeVideoEncode();
                }
                break;
            }
            case MSG_STOP_RESUME_VIDEO_CAPTURE_FINISHED: {
                //Log.i(TAG, "test: in handleMessage: MSG_STOP_RESUME_VIDEO_CAPTURE_FINISHED");
                if (mLSMediaCapture != null) {
                    mLSMediaCapture.resumeVideoPreview();
                    m_liveStreamingOn = true;
                    //开启视频推流，推正常帧
                    mLSMediaCapture.startVideoLiveStream();
                }
                break;
            }
            case MSG_STOP_AUDIO_CAPTURE_FINISHED: {
                //Log.i(TAG, "test: in handleMessage: MSG_STOP_AUDIO_CAPTURE_FINISHED");
                if (!m_tryToStopLivestreaming && mLSMediaCapture != null) {
                    //继续音频推流，推静音帧
                    mLSMediaCapture.resumeAudioEncode();
                }
                break;
            }
            case MSG_STOP_RESUME_AUDIO_CAPTURE_FINISHED: {
                //Log.i(TAG, "test: in handleMessage: MSG_STOP_RESUME_AUDIO_CAPTURE_FINISHED");
                //开启音频推流，推正常帧
                if (mLSMediaCapture != null) {
                    mLSMediaCapture.startAudioLiveStream();
                }
                break;
            }
            case MSG_SWITCH_CAMERA_FINISHED: {
                int cameraId = (Integer) object;//切换之后的camera id
                break;
            }
            case MSG_SEND_STATICS_LOG_FINISHED: {
                //Log.i(TAG, "test: in handleMessage, MSG_SEND_STATICS_LOG_FINISHED");
                break;
            }
        }
    }

    private Activity getActivity() {
        return activityProxy.getActivity();
    }
}
