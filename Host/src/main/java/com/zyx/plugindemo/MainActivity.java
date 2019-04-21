package com.zyx.plugindemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态替换方式
        Button btn1 = findViewById(R.id.btn_1);
        btn1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 正常去启动插件中的Activity
                Intent intent = new Intent();
                String activityName = "com.zyx.plugin.PlugInActivity1";
                intent.setClassName(MainActivity.this, activityName);
                startActivity(intent);
            }
        });

        // 静态代理方式
        Button btn2 = findViewById(R.id.btn_2);
        btn2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 要启动插件中的Activity，就要先启动宿主中的ProxyActivity
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ProxyActivity.class);
                intent.putExtra(ProxyActivity.TRAGET_ACTIVITY_CLASS_NAME, "com.zyx.plugin.PlugInActivity2");
                startActivity(intent);
            }
        });
    }
}
