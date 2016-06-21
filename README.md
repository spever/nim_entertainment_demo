# 网易云信在线教学 导读

网易云信在线教学工程集成网易云信 SDK、网易视频云 SDK，展示组合了直播(推拉流)与直播间功能的应用场景，为教育行业提供直播解决方案。

## <span id="源码导读"> 源码导读</span>

### 工程结构说明

源码主要分成四个 package ：base、im、thirdparty 和 education。
- base：封装一些 UI 基类，工具类等。
- im：包含登录页面及登录、注册业务逻辑（ activity / business / config 子包下）、基础 UI 组件( ui 子包下)、 会话页面相关组件( session 子包下)。
- thirdparty：包含网易视频云直播(推流)、网易视频云播放器(拉流)相关的核心组件。
- education：在线教育相关的页面和业务逻辑。

下面具体介绍 education 包下的子包结构：
- activity：所有 Activity。
- fragment：所有Activity中嵌套的 Fragment。其中包括 tab 子包，定义了直播间页面框架中的 Tab Fragment。
- adapter：直播间 Tab 页面、聊天页面数据源适配器等。
- viewholder：界面相关 ViewHolder。
- module：直播间收发消息模块、直播间自定义消息、网易云信直播间 Demo Http Client(与云信Demo应用服务器通信)。
- helper：直播间成员缓存，通知类消息处理等帮助类。

### 重点类说明

- ChatRoomActivity：直播间界面。封装了 ChatRoomFragment。包括直播间的进入/离开的操作，监听直播间在线状态和监听直播间被踢出状态。
- ChatRoomTabFragment：直播间内 tab fragment 的基类。
- ChatRoomFragment：直播间 Activity 包含的顶层 Fragment。包括界面上方界面和下方的 viewpager。viewpager 包含2个 tab fragment，分别是：MessageTabFragment （讨论区 tab）， OnlinePeopleTabFragment （在线成员 tab）。
- MessageTabFragment：讨论区基类 fragment。内嵌 ChatRoomMessageFragment （讨论区 fragment）。。
- OnlinePeopleTabFragment：在线成员基类 fragment。内嵌 OnlinePeopleFragment （在线成员 fragment）
- ChatRoomMessageFragment：讨论区fragment。包括消息的收发相关操作。
- OnlinePeopleFragment：直播间在线人数 fragment。
- ChatRoomMsgViewHolderFactory:  直播间消息项展示ViewHolder工厂类。包括消息展示 ViewHolder 的注册操作。
- IdentifyActivity： 选择老师或学生身份界面
- StudentActivity： 学生身份填写老师创建的房间id，进入直播间


/************************ Education *****************************/

# 网易云信娱乐直播 导读

网易云信娱乐直播工程集成网易云信 SDK、网易视频云 SDK，展示组合了直播(推拉流)与直播间功能的应用场景，为娱乐行业提供直播解决方案。

## <span id="源码导读"> 源码导读</span>

### 工程结构说明

源码主要分成四个 package ：base、im、thirdparty 和 entertainment。
- base：封装一些 UI 基类，工具类等。
- im：包含登录页面及登录、注册业务逻辑（ activity / business / config 子包下）、基础 UI 组件( ui 子包下)、 会话页面相关组件( session 子包下)。
- thirdparty：包含网易视频云直播(推流)、网易视频云播放器(拉流)相关的核心组件。
- entertainment：娱乐直播相关的页面和业务逻辑。

下面具体介绍 entertainment 包下的子包结构：
- activity：所有 Activity。
- adapter：聊天页面数据源适配器等。
- constant: 聊天页面数据常量。
- helper：直播间成员缓存，主播收到礼物缓存，礼物动画等帮助类。
- http: 网易云信直播间 Demo Http Client(与云信 Demo 应用服务器通信)
- model: 聊天界面数据实体。
- module：直播间收发消息模块、直播间自定义消息。
- ui: 直播间界面 ui 控件。
- viewholder：界面相关 ViewHolder。

### 重点类说明

- LivePlayerBaseActivity : 直播间基类。包括直播间的进入/离开的操作，监听直播间在线状态和监听直播间被踢出状态。
- LiveActivity：主播端 Activity。包含主播相关操作。
- AudienceActivity： 观众端 Activity。包含观众香港操作。
- IdentifyActivity： 选择主播或观众身份界面
- EnterRoomActivity： 观众身份填写主播创建的房间id，进入直播间
- ChatRoomMsgViewHolderFactory:  直播间消息项展示ViewHolder工厂类。包括消息展示 ViewHolder 的注册操作。


/************************ Meeting *****************************/

# 网易云信多人会议 导读

网易云信多人会议工程集成网易云信 SDK，展示组合了多人会议语音视频通话功能的应用场景。

## <span id="源码导读"> 源码导读</span>

### 工程结构说明

源码主要分成四个 package ：base、im、thirdparty 和 meeting。
- base：封装一些 UI 基类，工具类等。
- im：包含登录页面及登录、注册业务逻辑（ activity / business / config 子包下）、基础 UI 组件( ui 子包下)、 会话页面相关组件( session 子包下)。
- meeting：多人会议相关的页面和业务逻辑。

下面具体介绍 meeting 包下的子包结构：
- activity：所有 Activity。
- adapter：聊天页面数据源适配器等。
- helper：聊天室成员缓存，控制消息发送，聊天室通知消息构造等帮助类。
- module：收发消息模块、自定义消息、网易云信多人会议 Demo Http Client(与云信 Demo 应用服务器通信)。
- viewholder：界面相关 ViewHolder。

### 重点类说明

- ChatRoomActivity：多人会议界面。封装了 ChatRoomFragment。包括聊天室的进入/离开的操作，监听聊天室在线状态和监听聊天室被踢出状态，。
- ChatRoomTabFragment：多人会议内 tab fragment 的基类。
- ChatRoomFragment：多人会议 Activity 包含的顶层 Fragment。包括加入/离开音视频频道的操作，音视频图像的显示与移除，监听音视频状态。
包括界面上方界面和下方的 viewpager。viewpager 包含3个 tab fragment，分别是：MessageTabFragment （讨论区 tab）， OnlinePeopleTabFragment （在线成员 tab），ApplyForSpeechTabFragment (举手发言 tab，这个tab只在观众端显示）。
- MessageTabFragment：讨论区基类 fragment。内嵌 ChatRoomMessageFragment （讨论区 fragment）。。
- OnlinePeopleTabFragment：在线成员基类 fragment。内嵌 OnlinePeopleFragment （在线成员 fragment）
- ApplyForSpeechTabFragmet：举手发言基类 fragment。内嵌ApplyForSpeechFragment (举手发言 fragment)
- ChatRoomMessageFragment：讨论区fragment。包括消息的收发相关操作。
- OnlinePeopleFragment：聊天室在线人数 fragment。包括开启音视频的控制。
- ApplyForSpeechFragment：举手发言 fragment。包括成员举手，正在发言的相关操作
- ChatRoomMsgViewHolderFactory:  聊天室消息项展示ViewHolder工厂类。包括消息展示 ViewHolder 的注册操作。
- IdentifyActivity： 选择主持人或观众身份界面
- EnterRoomActivity： 主持人创建聊天室和音视频频道进入房间。观众搜索聊天室id进入房间。
