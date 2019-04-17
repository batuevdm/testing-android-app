package ru.batuevdm.testing;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TestActivity extends AppCompatActivity {

    Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        int testID = getIntent().getIntExtra("test_id", -1);
        api = new Api(getApplicationContext());

        if (testID > 0) {
            loadTest(testID);
        } else {
            invalidTest();
        }
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

    public void loadTest(int testID) {
        ProgressBar progressBar = findViewById(R.id.test_progress);

        ScrollView content = findViewById(R.id.test_scroll);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);
        api.testGet(Tools.getToken(), testID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        loadTest(testID);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        if (status.equals("success")) {
                            JSONObject test = result.getJSONObject("test");
                            String testName = test.getString("Name");
                            JSONArray questions = test.getJSONArray("Questions");

                            setTitle("Информация о тесте");

                            StringBuilder testInfo = new StringBuilder();

                            testInfo.append(testName)
                                    .append("\n")
                                    .append("\n");

                            testInfo.append("Количество вопросов: ")
                                    .append(questions.length())
                                    .append("\n")
                                    .append("\n")
                                    .append("Вопросы: ")
                                    .append("\n");

                            for (int i = 0; i < questions.length(); i++) {
                                JSONObject question = questions.getJSONObject(i);
                                testInfo.append(question.getInt("Number"))
                                        .append(". ")
                                        .append(question.getString("Question"))
                                        .append(" (");
                                String time = question.getInt("Time") == 0 ? "Неограничено по времени" : Tools.secondsToTime(question.getInt("Time"));
                                testInfo.append(time)
                                        .append(")")
                                        .append("\n");
                            }

                            TextView testInfoText = findViewById(R.id.test_information_text);
                            testInfoText.setText(testInfo.toString());

                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                            invalidTest();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            loadTest(testID);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });

        Button startButton = findViewById(R.id.test_start);
        startButton.setOnClickListener(v -> startTest(testID));
    }

    public void invalidTest() {
        Intent intent = new Intent(this, CabinetActivity.class);
        startActivity(intent);

        finish();
    }

    private void invalidToken() {
        Tools.clearToken();
        Intent intent = new Intent(TestActivity.this, LoginActivity.class);
        startActivity(intent);

        finish();
    }

    public void startTest(int testID) {
        ProgressBar progressBar = findViewById(R.id.test_progress);

        ScrollView content = findViewById(R.id.test_scroll);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);
        api.newTest(Tools.getToken(), testID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        startTest(testID);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        if (status.equals("success")) {
                            int userTestID = result.getInt("UserTestID");

                            Intent intent = new Intent(TestActivity.this, TestQuestionActivity.class);
                            intent.putExtra("user_test_id", userTestID);
                            startActivity(intent);
                            finish();
                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                            AlertDialog.Builder builder = new AlertDialog.Builder(TestActivity.this);
                            builder.setTitle("Ошибка")
                                    .setMessage(result.getString("message"))
                                    .setCancelable(false)
                                    .setNegativeButton("OK", (dialog, id) -> {
                                        dialog.cancel();
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();

                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            startTest(testID);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });
    }
}
