package com.sitemate.fabo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.InputStream;
import java.util.HashMap;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;

public class MainActivity extends AppCompatActivity {
    private static final int MAX_COUNT = 30;
    protected DanmakuContext mDanmakuContext = null;
    protected IDanmakuView mDanmakuView ;
    protected BaseDanmakuParser mParser = null;
    private EditText mEtDanmaku;
    private TextView tv_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        mDanmakuView = (IDanmakuView) findViewById(R.id.danmaku_view);

        initDanmaku();
    }

    //初始化弹幕层
    public void initDanmaku() {
        mDanmakuContext = DanmakuContext.create();
        HashMap<Integer, Integer> maxLine = new HashMap<>();
        maxLine.put(BaseDanmaku.TYPE_SCROLL_RL, 3);// 滚动弹幕最大显示3行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)//设置描边样式
                .setDuplicateMergingEnabled(false)//是否启用合并重复弹幕
                .setScrollSpeedFactor(1.2f) //设置弹幕滚动速度系数,只对滚动弹幕有效
                .setScaleTextSize(1.2f)//设置字体缩放
                .setMaximumLines(maxLine)//设置最大显示行数
                .preventOverlapping(overlappingEnablePair);//设置防弹幕重叠
        if (mDanmakuView != null) {
            mDanmakuView.setCallback(new DrawHandler.Callback() {
                @Override
                public void prepared() {
                    //开始播放弹幕
                    mDanmakuView.start();
                }

                @Override
                public void updateTimer(DanmakuTimer timer) {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {

                }

                @Override
                public void drawingFinished() {

                }
            });

            //弹幕解析器，如不想使用使用xml格式的可以以自己定义或默认
//            mParser = new BaseDanmakuParser() {
//                @Override
//                protected IDanmakus parse() {
//                    return new Danmakus();
//                }
//            };

            mParser = createParser(this.getResources().openRawResource(R.raw.comments));
            mDanmakuView.showFPS(true);//显示fps
            mDanmakuView.enableDanmakuDrawingCache(true);//显示弹幕绘制缓冲
            mDanmakuView.prepare(mParser, mDanmakuContext);
        }
    }

    //创建解析器对象，解析输入流
    private BaseDanmakuParser createParser(InputStream stream) {

        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }

        //DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI) xml解析
        //DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN) json文件格式解析

        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }

    /**
     * 添加弹幕
     */
    public void addDanmaku(boolean islive, String msg, boolean isUs) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        danmaku.text = msg;//弹幕内容
        danmaku.padding = 5;
        danmaku.priority = 1;//0 表示可能会被各种过滤器过滤并隐藏显示 1 表示一定会显示, 一般用于本机发送的弹幕
        danmaku.isLive = islive; //是否是直播弹幕
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200); //显示时间
        danmaku.textSize = 18f * (mParser.getDisplayer().getDensity() - 0.6f); //字体大小
        danmaku.textColor = Color.WHITE;
        danmaku.textShadowColor = Color.parseColor("#333333");// 阴影颜色，可防止白色字体在白色背景下不可见
        if (isUs)
            danmaku.borderColor = Color.YELLOW; //对于自己发送的弹幕可以加框显示,0表示无边框
        mDanmakuView.addDanmaku(danmaku);
    }

    //弹幕发送框
    public void SendDanmaku(View AtLocationView) {
        View view = View.inflate(getApplicationContext(), R.layout.danmaku_layout, null);
        mEtDanmaku = (EditText) view.findViewById(R.id.et_danmaku);
        tv_count = (TextView) view.findViewById(R.id.tv_waring);
        Button mBtnSend = (Button) view.findViewById(R.id.btn_send_danmaku);
        mEtDanmaku.addTextChangedListener(mTextWatcher);
        mEtDanmaku.setSelection(mEtDanmaku.length());
        setLeftCount();
        final PopupWindow popupWindow = new PopupWindow();
        popupWindow.setContentView(view);
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.update();
        ColorDrawable dw = new ColorDrawable(0000000000);
        popupWindow.setBackgroundDrawable(dw);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String trim = mEtDanmaku.getText().toString().trim();
                if (TextUtils.isEmpty(trim))
                    return;
                addDanmaku(false,trim,true);
                mEtDanmaku.setText("");
                popupWindow.dismiss();
            }
        });
        if (!popupWindow.isShowing()) {
            popupWindow.showAtLocation(AtLocationView, Gravity.TOP, 0, 0);//居中显示
            mEtDanmaku.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(1000, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    //自定义textwatcher 限制输入字符数
    private TextWatcher mTextWatcher = new TextWatcher() {
        private int editStart;
        private int editEnd;

        public void afterTextChanged(Editable s) {
            editStart = mEtDanmaku.getSelectionStart();
            editEnd = mEtDanmaku.getSelectionEnd();
            mEtDanmaku.removeTextChangedListener(mTextWatcher);
            while (calculateLength(s.toString()) > MAX_COUNT) { // 当输入字符个数超过限制的大小时，进行截断操作
                s.delete(editStart - 1, editEnd);
                editStart--;
                editEnd--;
            }
            mEtDanmaku.addTextChangedListener(mTextWatcher);
            setLeftCount();
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    };

    //计算输入长度
    private long calculateLength(CharSequence c) {
        double len = 0;
        for (int i = 0; i < c.length(); i++) {
            int tmp = (int) c.charAt(i);
            if (tmp > 0 && tmp < 127) {
                len += 0.5;
            } else {
                len++;
            }
        }
        return Math.round(len);
    }

    //设置显示长度
    private void setLeftCount() {
        long l = MAX_COUNT - getInputCount();
        if (l <= 0) {
            tv_count.setTextColor(Color.RED);
            tv_count.setText(String.valueOf((MAX_COUNT - getInputCount())));
        } else {
            tv_count.setText(String.valueOf((MAX_COUNT - getInputCount())));
        }
    }

    //获取输入长度
    private long getInputCount() {
        return calculateLength(mEtDanmaku.getText().toString());
    }

    //显示弹幕
    public void ShowDanmaku(View view) {
        if (mDanmakuView != null && mDanmakuView.isPrepared())
            mDanmakuView.show();
    }

    //隐藏弹幕
    public void HideDanmaku(View view) {
        if (mDanmakuView != null && mDanmakuView.isPrepared())
            mDanmakuView.hide();
    }

    //暂停弹幕
    public void PauseDanmaku(View view) {
        if (mDanmakuView != null && mDanmakuView.isPrepared())
            mDanmakuView.pause();
    }

    //恢复弹幕
    public void ResumeDanmaku(View view) {
        if (mDanmakuView != null && mDanmakuView.isPrepared())
            mDanmakuView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDanmakuView != null) {
            // 释放资源
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDanmakuView != null) {
            // 释放资源
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }
}
