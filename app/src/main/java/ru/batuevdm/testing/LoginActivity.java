package ru.batuevdm.testing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    protected EditText lognLogin;
    protected EditText lognPassword;
    protected Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        lognLogin = findViewById(R.id.login_login);
        lognPassword = findViewById(R.id.login_password);

        setTitle("Вход");

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> {
            Tools.hideSoftKeyboard(LoginActivity.this, v);
            login(lognLogin.getText().toString(), lognPassword.getText().toString());
        });

        Button registerButton = findViewById(R.id.login_register);
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    public void login(String login, String password) {
        login = login.trim();

        boolean error = false;
        if (login.length() < 3) {
            lognLogin.setError("Минимальная длина - 3 символа");
            error = true;
        }

        if (password.length() < 6) {
            lognPassword.setError("Минимальная длина - 6 символов");
            error = true;
        }

        if (error) return;

        ProgressBar progressBar = findViewById(R.id.login_progress);
        Api api = new Api(getApplicationContext());

        api.loading(true, progressBar);
        loginButton.setVisibility(Button.INVISIBLE);
        String finalLogin = login;

        api.auth(login, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    loginButton.setVisibility(Button.VISIBLE);

                    api.loadError(findViewById(android.R.id.content), v -> {
                        login(finalLogin, password);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    loginButton.setVisibility(Button.VISIBLE);

                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        if (status.equals("success")) {
                            String token = result.getString("token");
                            Tools.setToken(token);

                            Intent intent = new Intent(LoginActivity.this, CabinetActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                            builder.setTitle("Ошибка")
                                    .setMessage("Неверный логин или пароль")
                                    .setCancelable(true)
                                    .setNegativeButton("OK",
                                            (dialog, id) -> dialog.cancel());
                            AlertDialog alert = builder.create();
                            alert.show();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            login(finalLogin, password);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });

    }
}
