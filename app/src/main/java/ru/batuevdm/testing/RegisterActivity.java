package ru.batuevdm.testing;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    protected EditText registerFirstName;
    protected EditText registerLastName;
    protected EditText registerLogin;
    protected EditText registerPassword;
    protected EditText registerPasswordRepeat;
    protected Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        registerFirstName = findViewById(R.id.register_fn);
        registerLastName = findViewById(R.id.register_ln);
        registerLogin = findViewById(R.id.register_login);
        registerPassword = findViewById(R.id.register_password);
        registerPasswordRepeat = findViewById(R.id.register_password_repeat);

        registerButton = findViewById(R.id.register_button);

        setTitle("Регистрация");

        registerButton.setOnClickListener(v -> {
            Tools.hideSoftKeyboard(RegisterActivity.this, v);
            register(
                    registerLogin.getText().toString(),
                    registerPassword.getText().toString(),
                    registerPasswordRepeat.getText().toString(),
                    registerFirstName.getText().toString(),
                    registerLastName.getText().toString()
            );
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void register(String login, String password, String passwordRepeat, String fn, String ln) {
        login = login.trim();
        fn = fn.trim();
        ln = ln.trim();

        boolean error = false;
        if (fn.length() < 1) {
            registerFirstName.setError("Введите имя");
            error = true;
        }

        if (ln.length() < 1) {
            registerLastName.setError("Введите фамилию");
            error = true;
        }

        if (login.length() < 3) {
            registerLogin.setError("Минимальная длина - 3 символа");
            error = true;
        }

        if (password.length() < 6) {
            registerPassword.setError("Минимальная длина - 6 символов");
            error = true;
        }

        if (!password.equals(passwordRepeat)) {
            registerPasswordRepeat.setError("Пароли не совпадают");
            error = true;
        }

        if (error) return;

        ProgressBar progressBar = findViewById(R.id.register_progress);
        Api api = new Api(getApplicationContext());

        api.loading(true, progressBar);
        registerButton.setVisibility(Button.INVISIBLE);

        String finalFn = fn;
        String finalLn = ln;
        String finalLogin = login;
        api.register(fn, ln, login, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    registerButton.setVisibility(Button.VISIBLE);

                    api.loadError(findViewById(android.R.id.content), v -> {
                        register(finalLogin, password, passwordRepeat, finalFn, finalLn);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    registerButton.setVisibility(Button.VISIBLE);

                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                        if (status.equals("success")) {
                            builder.setTitle("Успешно")
                                    .setMessage("Регистрация завершена. Теперь нужно войти")
                                    .setCancelable(false)
                                    .setNegativeButton("OK", (dialog, id) -> {
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        dialog.cancel();
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            if (result.getString("error_type").equals("validate")) {
                                JSONObject errors = result.getJSONObject("errors");

                                Iterator<String> keys = errors.keys();
                                StringBuilder errorsString = new StringBuilder();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    String error = errors.getJSONArray(key).getString(0);
                                    errorsString.append(error)
                                            .append("\n");
                                }

                                builder.setTitle("Ошибка")
                                        .setMessage(errorsString.toString())
                                        .setCancelable(true)
                                        .setNegativeButton("OK",
                                                (dialog, id) -> dialog.cancel());
                            } else {
                                builder.setTitle("Ошибка")
                                        .setMessage("Ошибка регистрации")
                                        .setCancelable(true)
                                        .setNegativeButton("OK",
                                                (dialog, id) -> dialog.cancel());
                            }
                            AlertDialog alert = builder.create();
                            alert.show();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            register(finalLogin, password, passwordRepeat, finalFn, finalLn);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });

    }
}
