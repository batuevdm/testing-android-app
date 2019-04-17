package ru.batuevdm.testing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TestQuestionActivity extends AppCompatActivity {

    LinearLayout content;
    ScrollView allContent;
    TextView questionQuestion;
    TextView questionTitle;
    Button answerButton;
    ProgressBar progressBar;
    Api api;

    // TEMP FIELDS
    EditText tempEditText;
    RadioGroup tempRadioGroup;
    RadioButton[] tempRadioButtons;
    CheckBox[] tempCheckboxes;

    int qTime = 0;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_question);

        content = findViewById(R.id.question_content);
        questionQuestion = findViewById(R.id.question_question);
        questionTitle = findViewById(R.id.question_title);
        answerButton = findViewById(R.id.question_answer_button);
        progressBar = findViewById(R.id.question_progress);
        allContent = findViewById(R.id.question_scroll);

        int userTestID = getIntent().getIntExtra("user_test_id", -1);
        api = new Api(getApplicationContext());

        if (userTestID > 0) {
            loadQuestion(userTestID);
        } else {
            invalidTest();
        }

    }

    @Override
    public void onBackPressed() {
    }

    public void loadQuestion(int userTestID) {
        api.loading(true, progressBar);
        allContent.setVisibility(ScrollView.INVISIBLE);

        try {
            timer.cancel();
        } catch (Exception ignored) {
        }

        api.getQuestion(Tools.getToken(), userTestID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    allContent.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        loadQuestion(userTestID);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    allContent.setVisibility(ScrollView.VISIBLE);
                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        if (status.equals("success")) {
                            JSONObject question = result.getJSONObject("Question");
                            int userQuestionID = question.getInt("ID");
                            setTitle("Вопрос");

                            questionTitle.setText(question.getString("Title"));
                            questionQuestion.setText(question.getString("Question"));
                            String questionType = question.getString("Type");
                            String timeType = question.getString("TimeType");
                            int time = question.getInt("Time");

                            if (timeType.toLowerCase().equals("timer")) {
                                qTime = time;
                                timer = new Timer();
                                TextView timeText = findViewById(R.id.question_time);

                                timer.scheduleAtFixedRate(new TimerTask() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(() -> {
                                            qTime--;
                                            timeText.setText(Tools.secondsToTime(qTime));

                                            if (qTime <= 0) {
                                                timer.cancel();
                                                loadQuestion(userTestID);
                                            }
                                        });
                                    }
                                }, 1000, 1000);
                            }

                            JSONArray answers;
                            content.removeAllViewsInLayout();
                            switch (questionType) {
                                case "QTypeEdit":
                                    tempEditText = new EditText(TestQuestionActivity.this);
                                    tempEditText.setHint("Ответ");
                                    tempEditText.setId(View.generateViewId());
                                    content.addView(tempEditText);

                                    answerButton.setOnClickListener(v -> {
                                        String answer = tempEditText.getText().toString().trim();

                                        if (!answer.equals("")) {
                                            Tools.hideSoftKeyboard(TestQuestionActivity.this, v);
                                            setAnswer(userTestID, userQuestionID, answer);
                                        } else {
                                            Snackbar snackbar = Snackbar.make(v, "Введите ответ", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        }
                                    });

                                    break;

                                case "QTypeRadio":
                                    answers = question.getJSONArray("Answers");

                                    tempRadioGroup = new RadioGroup(TestQuestionActivity.this);
                                    tempRadioButtons = new RadioButton[answers.length()];

                                    for (int i = 0; i < answers.length(); i++) {
                                        JSONObject answer = answers.getJSONObject(i);

                                        tempRadioButtons[i] = new RadioButton(TestQuestionActivity.this);
                                        tempRadioButtons[i].setText(answer.getString("Answer"));
                                        tempRadioButtons[i].setId(answer.getInt("ID"));

                                        tempRadioGroup.addView(tempRadioButtons[i]);
                                    }
                                    content.addView(tempRadioGroup);

                                    answerButton.setOnClickListener(v -> {
                                        int answer = tempRadioGroup.getCheckedRadioButtonId();

                                        if (answer > 0) {
                                            setAnswer(userTestID, userQuestionID, answer);
                                        } else {
                                            Snackbar snackbar = Snackbar.make(v, "Необходимо выбрать ответ", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        }

                                    });

                                    break;

                                case "QTypeCheckbox":
                                    answers = question.getJSONArray("Answers");

                                    tempCheckboxes = new CheckBox[answers.length()];

                                    for (int i = 0; i < answers.length(); i++) {
                                        JSONObject answer = answers.getJSONObject(i);

                                        tempCheckboxes[i] = new CheckBox(TestQuestionActivity.this);
                                        tempCheckboxes[i].setText(answer.getString("Answer"));
                                        tempCheckboxes[i].setId(answer.getInt("ID"));

                                        content.addView(tempCheckboxes[i]);
                                    }

                                    answerButton.setOnClickListener(v -> {
                                        ArrayList<Integer> userAnswers = new ArrayList<>();
                                        for (CheckBox checkBox : tempCheckboxes) {
                                            if (checkBox.isChecked())
                                                userAnswers.add(checkBox.getId());
                                        }

                                        int[] userAnswersInt = new int[userAnswers.size()];
                                        for (int i = 0; i < userAnswersInt.length; i++)
                                            userAnswersInt[i] = userAnswers.get(i);

                                        if (userAnswersInt.length > 0) {
                                            setAnswer(userTestID, userQuestionID, userAnswersInt);
                                        } else {
                                            Snackbar snackbar = Snackbar.make(v, "Необходимо выбрать хотя бы один вариант", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        }
                                    });

                                    break;
                            }

                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();

                            AlertDialog.Builder builder = new AlertDialog.Builder(TestQuestionActivity.this);

                            if (result.getString("error_type").equals("end")) {
                                builder.setTitle("Конец теста")
                                        .setMessage("Тест завершен. С результатами можно ознакомиться на вкладке История")
                                        .setCancelable(false)
                                        .setNegativeButton("OK", (dialog, id) -> {
                                            Intent intent = new Intent(TestQuestionActivity.this, CabinetActivity.class);
                                            startActivity(intent);
                                            finish();

                                            dialog.cancel();
                                        });
                            } else {
                                builder.setTitle("Ошибка")
                                        .setMessage(result.getString("message"))
                                        .setCancelable(false)
                                        .setNegativeButton("OK", (dialog, id) -> dialog.cancel());
                            }
                            AlertDialog alert = builder.create();
                            alert.show();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            loadQuestion(userTestID);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });
    }

    public void setAnswer(int userTestID, int userQuestionID, String answer) {
        api.loading(true, progressBar);
        allContent.setVisibility(ScrollView.INVISIBLE);

        api.setAnswer(Tools.getToken(), userTestID, userQuestionID, answer, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(true, progressBar);
                    allContent.setVisibility(ScrollView.INVISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        setAnswer(userTestID, userQuestionID, answer);
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    api.loading(true, progressBar);
                    allContent.setVisibility(LinearLayout.INVISIBLE);

                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        if (status.equals("success")) {
                            loadQuestion(userTestID);
                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                            AlertDialog.Builder builder = new AlertDialog.Builder(TestQuestionActivity.this);
                            builder.setTitle("Ошибка")
                                    .setMessage(result.getString("message"))
                                    .setCancelable(false)
                                    .setNegativeButton("OK", (dialog, id) -> dialog.cancel());
                            AlertDialog alert = builder.create();
                            alert.show();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            setAnswer(userTestID, userQuestionID, answer);
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });
    }

    public void setAnswer(int userTestID, int userQuestionID, int answer) {
        setAnswer(userTestID, userQuestionID, String.valueOf(answer));
    }

    public void setAnswer(int userTestID, int userQuestionID, int[] answers) {
        StringBuilder answerString = new StringBuilder();
        for (int i = 0; i < answers.length; i++) {
            if (i != 0)
                answerString.append(":");
            answerString.append(answers[i]);
        }
        setAnswer(userTestID, userQuestionID, answerString.toString());
    }

    public void invalidTest() {
        Intent intent = new Intent(this, CabinetActivity.class);
        startActivity(intent);

        finish();
    }

    private void invalidToken() {
        Tools.clearToken();
        Intent intent = new Intent(TestQuestionActivity.this, LoginActivity.class);
        startActivity(intent);

        finish();
    }
}
