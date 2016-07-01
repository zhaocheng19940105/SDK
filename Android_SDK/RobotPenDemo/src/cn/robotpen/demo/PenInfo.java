package cn.robotpen.demo;

import java.util.Random;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.robotpen.model.interfaces.Listeners.OnConnectStateListener;
import cn.robotpen.model.interfaces.Listeners.OnPointChangeListener;
import cn.robotpen.model.interfaces.Listeners.OnTrailsClientChangeListener;
import cn.robotpen.model.FrameSizeObject;
import cn.robotpen.model.PointObject;
import cn.robotpen.model.TrailObject;
import cn.robotpen.core.module.TrailsManageModule.OnTrailsListener;
import cn.robotpen.core.services.PenService;
import cn.robotpen.core.services.SmartPenService;
import cn.robotpen.core.services.UsbPenService;
import cn.robotpen.model.symbol.BatteryState;
import cn.robotpen.model.symbol.ConnectState;
import cn.robotpen.model.symbol.Keys;
import cn.robotpen.model.symbol.SceneType;
import cn.robotpen.core.utils.SystemUtil;
import cn.robotpen.core.views.MultipleCanvasView;
import cn.robotpen.core.views.MultipleCanvasView.CanvasManageInterface;
import cn.robotpen.core.views.MultipleCanvasView.PenModel;

/**
 * 笔信息显示
 * @author Xiaoz
 * @date 2015年6月12日 下午3:34:58
 *
 * Description
 */
public class PenInfo extends Activity implements CanvasManageInterface{
	public static final String TAG = PenInfo.class.getSimpleName();
	public static final int REQUEST_SETTING_SIZE = 1000;
	
	/**笔服务广播处理**/
	private PenServiceReceiver mPenServiceReceiver;
	private PenService mPenService;
	private ProgressDialog mProgressDialog;
	private int mShowType = 0;
	private Button mXYBut;
	private Button mLineBut;

	private LinearLayout mXYFrame;
	private RelativeLayout mLineFrame;
	private FrameLayout mLineWindow;
	
	//笔的实际坐标
	private TextView mOriginalX;
	private TextView mOriginalY;
	
	//是否是写入状态
	private TextView mIsRoute;
	private TextView mIsSw1;
	private TextView mPenPressure;
	
	//纸张尺寸
	private TextView mSceneWidth;
	private TextView mSceneHeight;
	private TextView mSceneType;
	
	//纸张坐标
	private TextView mSceneX;
	private TextView mSceneY;
	
	//屏幕坐标
	private TextView mWindowX;
	private TextView mWindowY;

	private Button mStartP2PBut;
	private Button mStartGroupBut;
	private Button mStopLiveBut;
	
	/**虚拟用户ID**/
	private String mUserId;
	
	/** 笔画布 **/
	private MultipleCanvasView mPenCanvasView;
	
	/**
     * 画布尺寸对象
     **/
    private FrameSizeObject mCanvasSizeObject = new FrameSizeObject();
	
	//笔视图
	private PenView mPenView;
	
	//当前设备屏幕宽度
	private int mDisplayWidth;
	//屏幕高度
	private int mDisplayHeight;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//控制屏幕常亮
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		DisplayMetrics metric = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(metric);
	    mDisplayWidth = metric.widthPixels;  // 屏幕宽度（像素）
	    mDisplayHeight = metric.heightPixels;  // 屏幕高度（像素）
	    
	    mPenService = RobotPenApplication.getInstance().getPenService();
	    mPenService.setSceneType(SceneType.INCH_101);
	    
	    //状态栏+ActionBar+菜单按钮高
		Rect frame = new Rect();  
		getWindow().getDecorView().getWindowVisibleDisplayFrame(frame); 
		int menuHeight = SystemUtil.dip2px(PenInfo.this,120) + frame.top;
		
		Log.v(TAG, "menuHeight:"+menuHeight);
		
		//设置显示区域宽和高
		mCanvasSizeObject.frameWidth = mDisplayWidth;
		mCanvasSizeObject.frameHeight = mDisplayHeight - menuHeight;
		
		//设置纸张宽和高
        mCanvasSizeObject.sceneType = mPenService.getSceneType();
		mCanvasSizeObject.sceneWidth = mPenService.getSceneWidth();
		mCanvasSizeObject.sceneHeight = mPenService.getSceneHeight();
		
		//将纸张宽高尺寸适应到显示区域
		mCanvasSizeObject.initWindowSize();
		
		//随机一个用户ID，正式环境上替换为用户ID
		Random rd = new Random();
        mUserId = "u"+ String.valueOf(rd.nextInt(10000));
		mPenService.setUserId(mUserId);
	    
		setContentView(R.layout.activity_info);

        ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
	    
		mXYBut = (Button) findViewById(R.id.xyBut);
		mLineBut = (Button) findViewById(R.id.lineBut);
		
		mXYFrame = (LinearLayout) findViewById(R.id.xyFrame);
		mLineFrame = (RelativeLayout) findViewById(R.id.lineFrame);
		mLineWindow = (FrameLayout) findViewById(R.id.lineWindow);
		mPenCanvasView = (MultipleCanvasView) findViewById(R.id.penCanvasView);
		
		mOriginalX = (TextView) findViewById(R.id.originalX);
		mOriginalY = (TextView) findViewById(R.id.originalY);
		mIsRoute = (TextView) findViewById(R.id.isRoute);
		mIsSw1 = (TextView) findViewById(R.id.isSw1);
		mPenPressure = (TextView) findViewById(R.id.penPressure);
		mSceneWidth = (TextView) findViewById(R.id.sceneWidth);
		mSceneHeight = (TextView) findViewById(R.id.sceneHeight);
		mSceneType = (TextView) findViewById(R.id.sceneType);
		mSceneX = (TextView) findViewById(R.id.sceneX);
		mSceneY = (TextView) findViewById(R.id.sceneY);
		mWindowX = (TextView) findViewById(R.id.windowX);
		mWindowY = (TextView) findViewById(R.id.windowY);
		
		mStartP2PBut = (Button) findViewById(R.id.startP2PBut);
		mStartP2PBut.setOnClickListener(startP2P_click);
		
		mStartGroupBut = (Button) findViewById(R.id.startGroupBut);
		mStartGroupBut.setOnClickListener(startGroup_click);
		
		mStopLiveBut = (Button) findViewById(R.id.stopLiveBut);
		mStopLiveBut.setOnClickListener(stopLive_click);

		//添加笔跟随图标
		mPenView = new PenView(this);
		mLineWindow.addView(mPenView);
		
		//看坐标
		mXYBut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowType = 0;
				initPage();
			}
		});
		
		//看实际画线
		mLineBut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowType = 1;
				initPage();
			}
		});
		
		String address = getIntent().getStringExtra(Keys.KEY_DEVICE_ADDRESS);
		if(address != null && !address.isEmpty()){
			connectDevice(address);
		}else{
			String isUsbSvr = getIntent().getStringExtra(Keys.KEY_VALUE);
			if(isUsbSvr != null && !isUsbSvr.isEmpty() && isUsbSvr.equals(Keys.APP_USB_SERVICE_NAME)){
				((UsbPenService)mPenService).checkDeviceConnect();
				initPage();
			}else{
				alertError("IP address error.");
			}
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pen_info, menu);
        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case android.R.id.home:
            PenInfo.this.finish();
            break;
    	case R.id.action_settings:
    		initPage();
    		break;
    	}
    	return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_OK){
    		if(requestCode == REQUEST_SETTING_SIZE){
    			Log.v(TAG, "onActivityResult:"+REQUEST_SETTING_SIZE);
    			
    			initPage();
    		}
    	}
	}
    
    @Override
	public void onResume() {
		super.onResume();
		
		if(mPenService != null){
			//设置笔坐标监听
			mPenService.setOnPointChangeListener(onPointChangeListener);
		}else{
			//注册笔服务通过广播方式发送的笔迹坐标信息
			mPenServiceReceiver = new PenServiceReceiver();
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Keys.ACTION_SERVICE_SEND_POINT);
			registerReceiver(mPenServiceReceiver, intentFilter);
		}
	}
	
	@Override
	public void onPause(){
		if(mPenService != null){
			mPenService.setOnPointChangeListener(null);
		}else{
			unregisterReceiver(mPenServiceReceiver);
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		//断开设备
		if(mPenService != null){
			mPenService.disconnectDevice();
		}
		
		super.onDestroy();
	}
	
	private void initPage(){
		if(mShowType == 0){
			mXYBut.setEnabled(false);
			mLineBut.setEnabled(true);
			mXYFrame.setVisibility(View.VISIBLE);
			mLineFrame.setVisibility(View.GONE);
		}else{
			mXYBut.setEnabled(true);
			mLineBut.setEnabled(false);
			
			mXYFrame.setVisibility(View.GONE);
			mLineFrame.setVisibility(View.VISIBLE);
		}

		Log.v(TAG, "sceneWidth:"+mCanvasSizeObject.sceneWidth+",sceneHeight:"+mCanvasSizeObject.sceneHeight);
		Log.v(TAG, "DisplayWidth:"+mDisplayWidth+",DisplayHeight:"+mDisplayHeight);
		Log.v(TAG, "windowWidth:"+mCanvasSizeObject.windowWidth+",windowHeight:"+mCanvasSizeObject.windowHeight);
		Log.v(TAG, "windowLeft:"+mCanvasSizeObject.windowLeft+",windowTop:"+mCanvasSizeObject.windowTop);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCanvasSizeObject.windowWidth, mCanvasSizeObject.windowHeight);
		params.setMargins(mCanvasSizeObject.windowLeft, mCanvasSizeObject.windowTop, 0, 0);
		mLineWindow.setLayoutParams(params);
	}
	
	private void alertError(String msg){
		Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Warning");
        alert.setMessage(msg);
        alert.setPositiveButton("OK",new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PenInfo.this.finish();
			}
        });
        alert.show();
	}
	
	/**释放progressDialog**/
	protected void dismissProgressDialog(){
		if(mProgressDialog != null){
			if(mProgressDialog.isShowing())
				mProgressDialog.dismiss();
			
			mProgressDialog = null;
		}
	}
	
	private void connectDevice(String address){
		PenService service = RobotPenApplication.getInstance().getPenService();
		if(service != null){
			ConnectState state = ((SmartPenService)service).connectDevice(onConnectStateListener, address);
			if(state != ConnectState.CONNECTING){
				alertError("The pen service connection failure.");
			}else{
				mProgressDialog = ProgressDialog.show(PenInfo.this, "", getString(R.string.connecting), true);
			}
		}
	}
	
	//处理笔服务通过广播方式发送的笔迹坐标信息
	//示例仅用作演示有这个功能，没有特殊需求可删除以下代码
	private class PenServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Keys.ACTION_SERVICE_SEND_POINT)){
				//广播的形式接收笔迹信息
				String pointJson = intent.getStringExtra(Keys.KEY_PEN_POINT);
				if(pointJson != null && !pointJson.isEmpty()){

					Toast.makeText(PenInfo.this, pointJson, Toast.LENGTH_SHORT).show();
					//Log.v(TAG, "pointJson:"+pointJson);
					
					//更新笔坐标信息
					//如果注册了service.setOnPointChangeListener监听，那么请注释掉下面的代码，否则信息会冲突
					//反之如果需要使用Receiver，那么就不要使用setOnPointChangeListener
					//PointObject item = new PointObject(pointJson);
					//onPointChangeListener.change(item);
				}
				return;
			}
		}
	}
	
	private OnClickListener startGroup_click = new OnClickListener(){
		@Override
		public void onClick(View v) {
			View view = LayoutInflater.from(PenInfo.this).inflate(R.layout.alert_live_group, null);
            final EditText groupIdText = (EditText) view.findViewById(R.id.groupIdText);
            
            AlertDialog.Builder alert = new AlertDialog.Builder(PenInfo.this);
            alert.setTitle("请输入组ID");
            alert.setView(view);
            alert.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(mPenService != null && groupIdText.getText().length() > 0){
						String groupId = groupIdText.getText().toString();
						if(groupId != null && !groupId.isEmpty()){
							mPenService.setLiveTargetId(null);
							//设置组ID，需要在startLive之前设置
							mPenService.setLiveGroupId(groupId);
							mPenService.startLive();
							//设置轨迹接收监听,要在startLive之后设置
					        mPenService.setOnTrailsClientChangeListener(onTrailsClientChangeListener);
						}
					}
				}
            });
            alert.setNegativeButton(R.string.canceled,null);
            alert.show();
		}
	};
	
	private OnClickListener startP2P_click = new OnClickListener(){
		@Override
		public void onClick(View v) {
			View view = LayoutInflater.from(PenInfo.this).inflate(R.layout.alert_live_p2p, null);
            TextView myIdText = (TextView) view.findViewById(R.id.myIdText);
            final EditText targetIdText = (EditText) view.findViewById(R.id.targetIdText);
            
            myIdText.setText(mUserId);
            
            AlertDialog.Builder alert = new AlertDialog.Builder(PenInfo.this);
            alert.setTitle("请输入对方ID");
            alert.setView(view);
            alert.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(mPenService != null && targetIdText.getText().length() > 0){
						String targetId = targetIdText.getText().toString();
						if(targetId != null && !targetId.isEmpty()){
							//设置目标ID，需要在startLive之前设置
							mPenService.setLiveTargetId(targetId);
							mPenService.setLiveGroupId(null);
							mPenService.startLive();
							//设置轨迹接收监听,要在startLive之后设置
					        mPenService.setOnTrailsClientChangeListener(onTrailsClientChangeListener);
						}
					}
				}
            });
            alert.setNegativeButton(R.string.canceled,null);
            alert.show();
		}
	};

	private OnClickListener stopLive_click = new OnClickListener(){
		@Override
		public void onClick(View v) {
			if(mPenService != null){
		        mPenService.setOnTrailsClientChangeListener(null);
				mPenService.stopLive();
			}
		}
	};

    private OnTrailsClientChangeListener onTrailsClientChangeListener = new OnTrailsClientChangeListener() {
        @Override
        public void receiveTrail(TrailObject trail) {
            //TrailObject转换为PointObject
            PointObject point = new PointObject(trail);

            //设置当前场景类型
            point.setSceneType(mPenService.getSceneType());

            //设置窗口尺寸
            point.showUIWidth = mCanvasSizeObject.windowWidth;
            point.showUIHeight = mCanvasSizeObject.windowHeight;

            //绘制笔迹
            mPenCanvasView.drawLine(point);
        }

        @Override
        public void cmdClearScreen(int page) {
        	mPenCanvasView.cleanAll();
        }

        @Override
        public void receiveAudio(byte[] data) {

        }
    };
    
    
    
	
	private OnConnectStateListener onConnectStateListener = new OnConnectStateListener(){
		@Override
		public void stateChange(String address,ConnectState state) {
			switch(state){
			case PEN_READY:
				
				break;
			case PEN_INIT_COMPLETE:
				dismissProgressDialog();
				Toast.makeText(PenInfo.this, R.string.connected, Toast.LENGTH_SHORT).show();
				initPage();
				break;
			case CONNECTED:
				
				break;
			case SERVICES_FAIL:
				dismissProgressDialog();
				alertError("The pen service discovery failed.");
				break;
			case CONNECT_FAIL:
				dismissProgressDialog();
				alertError("The pen service connection failure.");
				break;
			case DISCONNECTED:
				dismissProgressDialog();
				Toast.makeText(PenInfo.this, R.string.disconnected, Toast.LENGTH_SHORT).show();
				
				mXYBut.setEnabled(false);
				mLineBut.setEnabled(false);
				break;
			default:
				
				break;
			}
		}
	};
	
	private OnPointChangeListener onPointChangeListener = new OnPointChangeListener(){
		
		@Override
		public void change(PointObject point) {
			
			//设置看坐标中的各个字段
			mOriginalX.setText(String.valueOf(point.originalX));
			mOriginalY.setText(String.valueOf(point.originalY));
			mIsRoute.setText(String.valueOf(point.isRoute));
			mIsSw1.setText(String.valueOf(point.isSw1));
			mPenPressure.setText(String.valueOf(point.pressure) +"("+String.valueOf(point.pressureValue)+")");
			
			mSceneWidth.setText(String.valueOf(point.getWidth()));
			mSceneHeight.setText(String.valueOf(point.getHeight()));
			
			mSceneX.setText(String.valueOf(point.getSceneX()));
			mSceneY.setText(String.valueOf(point.getSceneY()));
			
			if(point.battery == BatteryState.LOW){
				Toast.makeText(PenInfo.this, R.string.battery_low, Toast.LENGTH_LONG).show();
			}
			
			//设置窗口尺寸
            point.showUIWidth = mCanvasSizeObject.windowWidth;
            point.showUIHeight = mCanvasSizeObject.windowHeight;
			
			mWindowX.setText(String.valueOf(point.getSceneX()));
			mWindowY.setText(String.valueOf(point.getSceneY()));
			
			mSceneType.setText(String.valueOf(point.getSceneType()));
			
			if(mShowType != 1)return;
			
			//笔跟随图标
			mPenView.bitmapX = point.getSceneX();
			mPenView.bitmapY = point.getSceneY();
			mPenView.isRoute = point.isRoute;
			mPenView.invalidate();
			
			//绘制笔迹
			mPenCanvasView.drawLine(point);
			mPenCanvasView.addTrail(point);
		}
	};


	@Override
	public int getBgColor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBgResId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCurrUserId() {
		return mUserId;
	}

	@Override
	public FrameSizeObject getFrameSize() {
		return mCanvasSizeObject;
	}

	@Override
	public boolean getIsRubber() {
		return false;
	}

	@Override
	public int getPenColor() {
		return 0xFF000000;
	}

	@Override
	public PenModel getPenModel() {
		return PenModel.WaterPen;
	}

	@Override
	public float getPenWeight() {
		return 2;
	}

	@Override
	public OnTrailsListener getTrailsListener() {
		return mPenService;
	}

	@Override
	public void penRouteStatus(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Bitmap getBgBitmap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScaleType getBgScaleType() {
		// TODO Auto-generated method stub
		return null;
	}
}
	 
