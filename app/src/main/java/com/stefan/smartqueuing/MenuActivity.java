package com.stefan.smartqueuing;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import cn.com.newland.nle_sdk.responseEntity.DeviceInfo;
import cn.com.newland.nle_sdk.responseEntity.SensorInfo;
import cn.com.newland.nle_sdk.responseEntity.User;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NCallBack;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {
    private EditText EquipmentID;  //设备ID
    private TextView Pnumber; //排队人数
    private TextView Waittime;// 预计等待时间
    private Button ConfirmDevice; //确定设备
    private Button GetData; //最新进展
    private Button GetPastdata; //历史数据
    private int num;
    private int time;
    private int onewait = 3; //每人预计等待时间
    private NetWorkBusiness netWorkBusiness;
    private String accessToken;
    private String deviceID = "";
    private int dstatus = 1;
    private int FLAG_MSG = 0x001; //定义发送的消息代码
    private Message message; //声明消息对象
    private Boolean flag = true; //是否获取数据标志
    private Boolean flag1 = true; //是否再次提醒
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        init();

        ConfirmDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceInfo();
            }
        });

        refreshData(); //轮询

        GetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPeopleNumber();
            }
        });

        GetPastdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dstatus == 0) {
                    Intent intent = new Intent(MenuActivity.this, PastDataActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("accessToken", accessToken);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    //finish();
                } else {
                    Toast.makeText(MenuActivity.this, "设备输入不正确或未确定设备", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    void init() {
        EquipmentID = findViewById(R.id.equimentID); //设备id
        Pnumber = findViewById(R.id.pnumber);//排队人数
        Waittime = findViewById(R.id.waittime); //等待时间
        ConfirmDevice = findViewById(R.id.confirmDevice); //确认设备
        GetData = findViewById(R.id.getData); //最新数据
        GetPastdata = findViewById(R.id.getPastdata);//历史数据
        Bundle bundle = getIntent().getExtras();
        accessToken = bundle.getString("accessToken"); //获得传输密钥
        netWorkBusiness = new NetWorkBusiness(accessToken, "http://api.nlecloud.com:80/");//进行登录连接
    }

    /**
     * 获取设备信息
     */
    public void getDeviceInfo() {
        final Gson gson = new Gson();
        deviceID = EquipmentID.getText().toString();//获取输入设备id
        if (deviceID.equals("")) {
            Toast.makeText(this, "设备不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        netWorkBusiness.getDeviceInfo(deviceID, new NCallBack<BaseResponseEntity<DeviceInfo>>() {
            @Override
            protected void onResponse(BaseResponseEntity<DeviceInfo> response) {

            }

            public void onResponse(final Call<BaseResponseEntity<DeviceInfo>> call, final Response<BaseResponseEntity<DeviceInfo>> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                if (baseResponseEntity != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                        dstatus = (int) jsonObject.get("Status");
                        if (dstatus == 0) {
                            Toast.makeText(MenuActivity.this, "欢迎使用设备", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MenuActivity.this, "设备不存在,请确认", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void refreshData() {
        message = Message.obtain();//从消息池获取空消息对象
        message.what = FLAG_MSG;//标识信息，以便用不同的方式处理Message
        handler.sendMessage(message);//立刻发送消息
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == FLAG_MSG) {
                getPeopleNumber();
            }
            message = handler.obtainMessage(FLAG_MSG);//从消息池获取空消息对象，标识为FLAG_MSG
            handler.sendMessageDelayed(message, 5000); // 延时5秒发送
        }
    };


    /**
     * 获取排队人数
     */
    private void getPeopleNumber() {
        netWorkBusiness.getSensor(deviceID, "number_up", new NCallBack<BaseResponseEntity<SensorInfo>>() {
            @Override
            protected void onResponse(BaseResponseEntity<SensorInfo> response) {

            }

            public void onResponse(final Call<BaseResponseEntity<SensorInfo>> call, final Response<BaseResponseEntity<SensorInfo>> response) {
                if (flag) {
                    BaseResponseEntity baseResponseEntity = response.body();
                    if (baseResponseEntity != null) {
                        //获取内容，使用json解析数据
                        final Gson gson = new Gson();
                        JSONObject jsonObject = null;
                        String msg = gson.toJson(baseResponseEntity);
                        try {
                            jsonObject = new JSONObject(msg); //解析数据
                            JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                            String value = resultObj.getString("Value");
                            num = Integer.parseInt(value);
                            if (num <= 4 && flag1) {
                                dialog();
                            }
                            time = num * onewait;
                            Pnumber.setText(num + "人");
                            Waittime.setText(time + "min");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (!flag) {
                    num = 0;
                    time = 0;
                    Pnumber.setText(num + "人");
                    Waittime.setText(time + "min");
                }
            }

            public void onFailure(Call<BaseResponseEntity<SensorInfo>> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "人数获取失败", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 减少排队人数
     *
     * @param id     设备id
     * @param apiTag 标识符
     * @param value  值
     */
    public void reducePeopleNumber(String id, String apiTag, Object value) {
        netWorkBusiness.control(id, apiTag, value, new Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(Call<BaseResponseEntity> call, Response<BaseResponseEntity> response) {
                BaseResponseEntity<User> baseResponseEntity = response.body(); //获得返回体
                if (baseResponseEntity == null) {
                    Toast.makeText(MenuActivity.this, "请求内容为空", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponseEntity> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "请求失败" + t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }


    /**
     * 即将排完队时，给出弹窗提醒
     */
    public void dialog() {
        //标题居中
        TextView title = new TextView(this);
        title.setText("提示");
        title.setPadding(0, 25, 0, 0);
        title.setGravity(Gravity.CENTER);
        //创建对话框对象
        AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
        alertDialog.setIcon(R.drawable.advise); //设置对话框的图标
        alertDialog.setCustomTitle(title);//设置标题
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        assert vibrator != null;
        vibrator.vibrate(new long[]{300, 500}, 0);
        //设置要显示的内容
        alertDialog.setMessage("前面仅剩3人,请立即到现场等候办理业务");
        //添加取消提醒按钮
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消提醒", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrator.cancel();
                flag1 = false;
            }
        });
        //添加放弃排队按钮
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "放弃排队", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //放弃排队，人数减一
                num = num - 1;
                reducePeopleNumber(deviceID, "number_down", num);
                vibrator.cancel();
                //放弃排队后，按键置灰，不更新数据
                flag = false;
                ConfirmDevice.setEnabled(false);
                GetData.setEnabled(false);
                GetPastdata.setEnabled(false);
            }
        });
        alertDialog.show();//显示对话框
        //按钮剧中设置
        Button mNegativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button mPositiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        LinearLayout.LayoutParams mNegativeButtonLL = (LinearLayout.LayoutParams) mNegativeButton.getLayoutParams();
        mNegativeButtonLL.weight = 1;
        mNegativeButton.setLayoutParams(mNegativeButtonLL);

        LinearLayout.LayoutParams mPositiveButtonLL = (LinearLayout.LayoutParams) mPositiveButton.getLayoutParams();
        mPositiveButtonLL.weight = 1;
        mPositiveButton.setLayoutParams(mPositiveButtonLL);
    }
}