package remix.myplayer.activities;


import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;


import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.adapters.SlideMenuRecycleAdpater;
import remix.myplayer.broadcastreceivers.ExitReceiver;
import remix.myplayer.broadcastreceivers.LineCtlReceiver;
import remix.myplayer.broadcastreceivers.NotifyReceiver;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.SharedPrefsUtil;
import remix.myplayer.utils.Utility;
import remix.myplayer.utils.XmlUtil;

public class MainActivity extends AppCompatActivity implements MusicService.Callback{
    public static MainActivity mInstance = null;
    private MusicService mService;
    private BottomActionBarFragment mActionbar;
    private LoaderManager mManager;
    private Utility mUtlity;
    private RecyclerView mMenuRecycle;
    private SlideMenuRecycleAdpater mMenuAdapter;
    public NotifyReceiver mNotifyReceiver;
    private LineCtlReceiver mLineCtlReceiver;
    private MusicService.PlayerReceiver mMusicReceiver;
    private ExitReceiver mExitReceiver;
    //判断NotifyReceiver是否注册过
    private static boolean mNotifyFlag = false;
    private ServiceConnection mConnecting = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.PlayerBinder)service).getService();
            mService.addCallback(MainActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        unregisterReceiver(mLineCtlReceiver);
//        unbindService(mConnecting);
//        unregisterReceiver(mNotifyReceiver);
//        unregisterReceiver(mMusicReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XmlUtil.setContext(getApplicationContext());

        Fresco.initialize(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.content_main);
        mUtlity = new Utility(getApplicationContext());
        loadsongs();

//        //获取音频服务
//        AudioManager audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
//        //注册接收的Receiver
//        ComponentName  mbCN = new ComponentName(this,LineCtlReceiver.class);
//        //注册MediaButton
//        audioManager.registerMediaButtonEventReceiver(mbCN);
//        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
//                AudioManager.AUDIOFOCUS_GAIN);
//        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
//            Toast.makeText(this,"could not get audio focus",Toast.LENGTH_SHORT).show();


        //播放的service
        MusicService.addCallback(MainActivity.this);
        startService(new Intent(this,MusicService.class));
        try {
            mNotifyFlag = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            filter.addAction(Utility.NOTIFY);
            mNotifyReceiver = new NotifyReceiver();
            registerReceiver(mNotifyReceiver, filter);
        }catch (Exception e){
            e.printStackTrace();
        }
        //加载主页fragment
        initMainFragment();
        //初始化侧滑菜单
        initSlideMenu();
        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
            return;
        //如果是第一次启动软件
        boolean mFir = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mFirst",true);
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        SharedPrefsUtil.putValue(getApplicationContext(),"setting","mFirst",false);

        if(mFir || mPos < 0)
            mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayingList.get(0)),false);
        else
            mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayingList.get(mPos)), false);

//        mActionbar.UpdateBottomStatus(MusicService.getCurrentMP3() == null ?
//                Utility.getMP3InfoById(Utility.mPlayingList.get(0)) :
//                MusicService.getCurrentMP3(),
//                false);

        //注册Musicreceiver
//        MusicService service = new MusicService(getApplicationContext());
//        mMusicReceiver = service.new PlayerReceiver();
//        IntentFilter musicfilter = new IntentFilter(Utility.CTL_ACTION);
//        registerReceiver(mMusicReceiver, musicfilter);


    }

    private void initSlideMenu()
    {
//        mMenuRecycle = (RecyclerView)findViewById(R.id.slide_menu_recyclelist);
//        mMenuRecycle.setLayoutManager(new LinearLayoutManager(this));
//        mMenuAdapter = new SlideMenuRecycleAdpater(getLayoutInflater());
//        mMenuRecycle.setAdapter(mMenuAdapter);
//        mMenuRecycle.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }
    public RecyclerView getRecycleMenu()
    {
        return mMenuRecycle;
    }
    private void initMainFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, new MainFragment(), "MainFragment").addToBackStack(null).commit();
    }

    public FragmentManager getFM()
    {
        return getSupportFragmentManager();
    }


    //读取sd卡歌曲信息
    public static void loadsongs()
    {
        //读取所有歌曲信息
        FutureTask<ArrayList<Long>> task = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return Utility.getAllSongsId();
            }
        });
        //开启一条线程来读取歌曲信息
        new Thread(task, "getInfo").start();
        try {
            Utility.mAllSongList = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //读取正在播放列表信息
        FutureTask<ArrayList<Long>> task1 = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return XmlUtil.getPlayingList();
            }
        });
        //开启一条线程来读取歌曲信息
        new Thread(task1, "getPlayingList").start();
        try {
            Utility.mPlayingList = task1.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //后退返回桌面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            NotifyReceiver.misFromActivity = true;
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            sendBroadcast(new Intent(Utility.NOTIFY));
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        MP3Info temp = MP3info;
        mActionbar.UpdateBottomStatus(MP3info, isplay);
    }

    @Override
    public int getType() {
        return 0;
    }

    public  MusicService getService()
    {
        return mService;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

}

