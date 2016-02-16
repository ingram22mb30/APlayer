package remix.myplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.R;
import remix.myplayer.services.TimerService;
import remix.myplayer.utils.Utility;

/**
 * Created by taeja on 16-1-15.
 */
public class TimerPopupWindow extends Activity {
    //正在计时
    public static boolean misTime = false;
    //正在运行
    public static boolean misRun = false;
    private TextView mText;
    private CircleSeekBar mSeekbar;
    private Button mToggle;
    private Button mCancel;
    private static long mTime;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mText.setText(msg.obj.toString());
            mSeekbar.setProgress(msg.arg1);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_timer);
        //居中显示
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (metrics.heightPixels * 0.6);
        lp.width = (int) (metrics.widthPixels * 0.7);
        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);

        mText = (TextView)findViewById(R.id.close_time);
        mSeekbar = (CircleSeekBar) findViewById(R.id.close_seekbar);
        if(misTime) {
            int remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
            mSeekbar.setProgress(remain / 60);
            mSeekbar.setStart(true);
        }

        mSeekbar.setOnSeekBarChangeListener(new CircleSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser) {
                if(progress > 0) {
                    String text = (progress < 10 ? "0" + progress : "" + progress )+ ":00min";
                    mText.setText(text);
                    mTime = progress;
                }
            }
            @Override
            public void onStartTrackingTouch(CircleSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(CircleSeekBar seekBar) {
            }
        });

        Intent startIntent = new Intent(TimerPopupWindow.this, TimerService.class);
        startService(startIntent);

        mToggle = (Button)findViewById(R.id.close_toggle);
        mToggle.setText(misTime == true ? "取消计时" : "开始计时");
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = misTime == true ? "取消定时关闭" : "将在" + mTime + "分钟后关闭";
                Toast.makeText(TimerPopupWindow.this,msg,Toast.LENGTH_SHORT).show();
                misTime = !misTime;
                mSeekbar.setStart(misTime);
                Intent intent = new Intent(Utility.CONTROL_TIMER);
                intent.putExtra("Time",mTime);
                intent.putExtra("Run", misTime);
                sendBroadcast(intent);
                finish();
            }
        });
        mCancel = (Button)findViewById(R.id.close_stop);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                misRun = false;
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        misRun = true;
        if(misTime) {
            TimeThread thread = new TimeThread();
            thread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        misRun = false;
    }

    class TimeThread extends Thread{
        int min,sec,remain;
        @Override
        public void run(){
            while (misRun){
                remain = (int)mTime * 60 - (int)(System.currentTimeMillis() - TimerService.mStartTime) / 1000;
                min = remain / 60;
                sec = remain % 60;
                String str_min = min < 10 ? "0" + min : "" + min;
                String str_sec = sec < 10 ? "0" + sec : "" + sec;
                String text = str_min + ":" + str_sec + "min";
                Message msg = new Message();
                msg.obj = text;
                msg.arg1 = min;
                mHandler.sendMessage(msg);
                Log.d("Timer","SendMsg");
                try {
                    sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
