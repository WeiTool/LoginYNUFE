package com.srun.campuslogin;

import android.app.Activity;
import android.os.Bundle;
import com.srun.campuslogin.utils.VersionChecker;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 触发版本检查
        VersionChecker.checkNewVersion(this);
    }
}