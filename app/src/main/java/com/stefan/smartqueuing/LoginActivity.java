package com.stefan.smartqueuing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.com.newland.nle_sdk.requestEntity.SignIn;
import cn.com.newland.nle_sdk.responseEntity.User;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText username;
    private EditText password;
    private Button login;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String _username = "";
    private String _password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("nlecloud", MODE_PRIVATE);
        editor = sp.edit();

        login = findViewById(R.id.login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        if (sp.getString("username", _username) != null && sp.getString("passwod", _password) != null) {
            if (!sp.getString("username", _username).equals("") && !sp.getString("password", _password).equals("")) {
                username.setText(sp.getString("username", "1")); //判断SharedPreferences文件中，用户名、密码是否存在
                password.setText(sp.getString("password", "2")); //第二个参数是该值如果获取不到的默认值
            }
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }


    private void signIn() {
        String platformAddress = "http://api.nlecloud.com:80/"; //新大陆网址
        _username = username.getText().toString(); //获取用户名和密码
        _password = password.getText().toString();
        //判断用户名和密码是否已输入
        if (_username.equals("") || _password.equals("")) {
            Toast.makeText(this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        final NetWorkBusiness netWorkBusiness = new NetWorkBusiness("", platformAddress);
        netWorkBusiness.signIn(new SignIn(_username, _password), new Callback<BaseResponseEntity<User>>() {
            @Override
            public void onResponse(Call<BaseResponseEntity<User>> call, Response<BaseResponseEntity<User>> response) {
                BaseResponseEntity<User> baseResponseEntity = response.body(); //获得响应体
                if (baseResponseEntity != null) {
                    if (baseResponseEntity.getStatus() == 0) {
                        editor.putString("username", _username);
                        editor.putString("password", _password);
                        editor.apply();
                        //传输密钥
                        String accessToken = baseResponseEntity.getResultObj().getAccessToken();
                        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        //界面跳转
                        Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("accessToken", accessToken);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, baseResponseEntity.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponseEntity<User>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "登录失败" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}