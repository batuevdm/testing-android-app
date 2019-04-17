package ru.batuevdm.testing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LoadingActivity extends AppCompatActivity {
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);

        mContentView = findViewById(R.id.fullscreen_content);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        String token = Tools.getToken();
        if (token.length() > 0) {
            checkToken(token);
        } else {
            tokenInvalid();
        }

    }

    void checkToken(String token) {
        Api api = new Api(getApplicationContext());

        api.userInfo(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    api.loadError(findViewById(android.R.id.content), v -> {
                        checkToken(token);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);

                        String status = result.getString("status");
                        if (status.equals("success")) {
                            tokenValid();
                        } else {
                            tokenInvalid();
                        }
                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            checkToken(token);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });
    }

    void tokenValid() {
        Intent intent = new Intent(LoadingActivity.this, CabinetActivity.class);
        startActivity(intent);
        finish();
    }

    void tokenInvalid() {
        Tools.clearToken();
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 1000);
    }

}
