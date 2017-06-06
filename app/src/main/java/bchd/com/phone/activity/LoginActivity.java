package bchd.com.phone.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import bchd.com.phone.R;
import bchd.com.phone.utils.Mutils;

public class LoginActivity extends AppCompatActivity {
    private EditText accountText;
    private EditText passwordEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        accountText = (EditText) findViewById(R.id.account_edittext);
        passwordEditText = (EditText) findViewById(R.id.password_edittext);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login(){
//        if (TextUtils.isEmpty(accountText.getText())){
//            Toast.makeText(this,"请填写账号",Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (TextUtils.isEmpty(passwordEditText.getText())){
//            Toast.makeText(this,"请填写密码",Toast.LENGTH_SHORT).show();
//            return;
//        }

        startActivity(Mutils.createSingleTaskIntent(LoginActivity.this,PhoneActivity.class));
        // TODO: 2017/6/2 登录

    }
}
