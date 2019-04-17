package ru.batuevdm.testing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CabinetActivity extends AppCompatActivity {

    Api api;

    EditText editFirstName;
    EditText editLastName;
    EditText editLogin;
    EditText editPasswordOld;
    EditText editPassword;
    EditText editPasswordRepeat;
    Button editButton;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {

        switch (item.getItemId()) {
            case R.id.navigation_home:

                loadTests();
                return true;
            case R.id.navigation_history:

                loadHistory();
                return true;
            case R.id.navigation_user_edit:

                loadUserEdit();
                return true;
        }
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cabinet);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        api = new Api(getApplicationContext());

        loadTests();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cabinet_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                invalidToken();

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    protected void loadLayout(int id, boolean scroll) {
        ScrollView cabinetScroll = findViewById(R.id.cabinet_scroll);
        LinearLayout cabinetLayout = findViewById(R.id.cabinet_not_scroll);

        cabinetScroll.removeAllViewsInLayout();
        cabinetLayout.removeAllViewsInLayout();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(id, null);
        if (scroll) {
            cabinetScroll.addView(rowView, cabinetScroll.getChildCount());
        } else {
            cabinetScroll.addView(rowView, cabinetLayout.getChildCount());
        }
    }

    protected void loadLayout(int id) {
        loadLayout(id, true);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        KeyValueArrayAdapter listAdapter = (KeyValueArrayAdapter) listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount())) + 100;
        listView.setLayoutParams(params);
    }

    public void loadTests() {
        setTitle("Тесты");
        loadLayout(R.layout.cabinet_tests_layout, false);

        ListView testList = findViewById(R.id.test_listview);
        ProgressBar progressBar = findViewById(R.id.cabinet_progress);
        ScrollView content = findViewById(R.id.cabinet_scroll);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);
        api.testsList(Tools.getToken(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        loadTests();
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
                            JSONArray tests = result.getJSONArray("tests");
                            String[] names = new String[tests.length()];
                            String[] ids = new String[tests.length()];

                            for (int i = 0; i < tests.length(); i++) {
                                JSONObject test = tests.getJSONObject(i);
                                names[i] = test.getString("Name");
                                ids[i] = String.valueOf(test.getInt("ID"));
                            }

                            KeyValueArrayAdapter adapter = new KeyValueArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1);

                            adapter.setEntries(names);
                            adapter.setEntryValues(ids);

                            testList.setAdapter(adapter);
//                            getTotalHeightofListView(testList);
                            setListViewHeightBasedOnChildren(testList);


                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            loadTests();
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });

        testList.setOnItemClickListener((parent, v, pos, id) -> {
            if (pos != -1) {
                KeyValueArrayAdapter adapter = (KeyValueArrayAdapter) testList.getAdapter();

                int testID = Integer.parseInt(adapter.getEntryValue(pos));

                Intent intent = new Intent(CabinetActivity.this, TestActivity.class);
                intent.putExtra("test_id", testID);

                startActivity(intent);
            }
        });
    }

    public void loadHistory() {
        setTitle("История прохождения тестов");
        loadLayout(R.layout.cabinet_history_layout);

        ProgressBar progressBar = findViewById(R.id.cabinet_progress);
        ScrollView content = findViewById(R.id.cabinet_scroll);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);
        api.userTests(Tools.getToken(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        loadHistory();
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

                        TextView textView = findViewById(R.id.textViewHistory);
                        LinearLayout historyItems = findViewById(R.id.history_items);
                        historyItems.removeAllViewsInLayout();

                        if (status.equals("success")) {
                            JSONArray tests = result.getJSONArray("tests");

                            if (tests.length() > 0) {
                                for (int i = 0; i < tests.length(); i++) {
                                    JSONObject test = tests.getJSONObject(i);
                                    newHistoryItem(historyItems, test);
                                }
                            } else {
                                textView.setText("Здесь пока ничего нет");
                            }
                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            loadHistory();
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });
    }

    public void loadUserEdit() {
        setTitle("Редактирование информации");
        loadLayout(R.layout.cabinet_user_edit_layout);

        ProgressBar progressBar = findViewById(R.id.cabinet_progress);
        ScrollView content = findViewById(R.id.cabinet_scroll);

        editFirstName = findViewById(R.id.edit_fn);
        editLastName = findViewById(R.id.edit_ln);
        editLogin = findViewById(R.id.edit_login);
        editPasswordOld = findViewById(R.id.edit_password_old);
        editPassword = findViewById(R.id.edit_password);
        editPasswordRepeat = findViewById(R.id.edit_password_repeat);

        editButton = findViewById(R.id.edit_button);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);
        api.userInfo(Tools.getToken(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        loadUserEdit();
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
                            JSONObject user = result.getJSONObject("user");

                            editFirstName.setText(user.getString("FirstName"));
                            editLastName.setText(user.getString("LastName"));
                            editLogin.setText(user.getString("Login"));
                        } else {
                            if (result.getString("error_type").equals("token"))
                                invalidToken();
                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            loadUserEdit();
                            api.dismissLoadError();
                        });
                    }
                });
            }
        });

        editButton.setOnClickListener(v -> save(
                editLogin.getText().toString(),
                editPasswordOld.getText().toString(),
                editPassword.getText().toString(),
                editPasswordRepeat.getText().toString(),
                editFirstName.getText().toString(),
                editLastName.getText().toString()
        ));

    }

    private void invalidToken() {
        Tools.clearToken();
        Intent intent = new Intent(CabinetActivity.this, LoginActivity.class);
        startActivity(intent);

        finish();
    }

    public void save(String login, String passwordOld, String password, String passwordRepeat, String fn, String ln) {
        login = login.trim();
        fn = fn.trim();
        ln = ln.trim();

        boolean error = false;
        if (fn.length() < 1) {
            editFirstName.setError("Введите имя");
            error = true;
        }

        if (ln.length() < 1) {
            editLastName.setError("Введите фамилию");
            error = true;
        }

        if (login.length() < 3) {
            editLogin.setError("Минимальная длина - 3 символа");
            error = true;
        }

        boolean isPasswordEdited = false;

        if (!passwordOld.equals("") || !password.equals("") || !passwordRepeat.equals("")) {
            isPasswordEdited = true;

            if (password.length() < 6) {
                editPassword.setError("Минимальная длина - 6 символов");
                error = true;
            }

            if (!password.equals(passwordRepeat)) {
                editPasswordRepeat.setError("Пароли не совпадают");
                error = true;
            }

            if (passwordOld.equals("")) {
                editPasswordOld.setError("Введите текущий пароль");
                error = true;
            }
        }

        if (error) return;

        ProgressBar progressBar = findViewById(R.id.edit_progress);

        api.loading(true, progressBar);
        editButton.setVisibility(Button.INVISIBLE);

        if (isPasswordEdited) {
            api.userEdit(Tools.getToken(), fn, ln, login, passwordOld, password, callback());
        } else {
            api.userEdit(Tools.getToken(), fn, ln, login, callback());
        }
    }

    private Callback callback() {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    ProgressBar progressBar = findViewById(R.id.edit_progress);
                    api.loading(false, progressBar);
                    editButton.setVisibility(Button.VISIBLE);

                    api.loadError(findViewById(android.R.id.content), v -> {
                        save(
                                editLogin.getText().toString(),
                                editPasswordOld.getText().toString(),
                                editPassword.getText().toString(),
                                editPasswordRepeat.getText().toString(),
                                editFirstName.getText().toString(),
                                editLastName.getText().toString()
                        );
                        api.dismissLoadError();
                    });
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    ProgressBar progressBar = findViewById(R.id.edit_progress);
                    api.loading(false, progressBar);
                    editButton.setVisibility(Button.VISIBLE);

                    try {
                        String res = response.body() != null ? response.body().string() : "{}";
                        JSONObject result = new JSONObject(res);
                        String status = result.getString("status");

                        AlertDialog.Builder builder = new AlertDialog.Builder(CabinetActivity.this);
                        if (status.equals("success")) {
                            builder.setTitle("Успешно")
                                    .setMessage("Новые данные сохранены")
                                    .setCancelable(false)
                                    .setNegativeButton("OK", (dialog, id) -> {
                                        loadUserEdit();
                                        dialog.cancel();
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {

                            switch (result.getString("error_type")) {
                                case "validate":
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
                                    break;

                                case "token":
                                    invalidToken();
                                    break;

                                default:
                                    builder.setTitle("Ошибка")
                                            .setMessage(result.getString("message"))
                                            .setCancelable(true)
                                            .setNegativeButton("OK",
                                                    (dialog, id) -> dialog.cancel());
                                    break;
                            }

                            AlertDialog alert = builder.create();
                            alert.show();

                        }

                    } catch (Exception e) {
                        api.loadError(findViewById(android.R.id.content), v -> {
                            save(
                                    editLogin.getText().toString(),
                                    editPasswordOld.getText().toString(),
                                    editPassword.getText().toString(),
                                    editPasswordRepeat.getText().toString(),
                                    editFirstName.getText().toString(),
                                    editLastName.getText().toString()
                            );
                            api.dismissLoadError();
                        });
                    }
                });
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private void newHistoryItem(LinearLayout layout, JSONObject item) throws JSONException {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.history_item, null);
        layout.addView(rowView, layout.getChildCount());

        TextView name = rowView.findViewById(R.id.history_item_name);
        TextView date = rowView.findViewById(R.id.history_item_date);
        TextView information = rowView.findViewById(R.id.history_item_information);
        Button button = rowView.findViewById(R.id.history_item_button);
        ToggleButton questionsButton = rowView.findViewById(R.id.history_item_questions_button);
        LinearLayout questions = rowView.findViewById(R.id.history_item_questions);

        Date _date = new Date();
        _date.setTime((long) item.getInt("StartTime") * 1000);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

        name.setText(item.getString("Name"));
        date.setText(formatter.format(_date));

        StringBuilder textInformation = new StringBuilder();
        int testID = item.getInt("ID");

        if (item.isNull("EndTime")) {
            textInformation.append("Не завершен")
                    .append("\n");
            button.setText("Продолжить");
            questionsButton.setVisibility(Button.GONE);

            button.setOnClickListener(v -> {
                Intent intent = new Intent(CabinetActivity.this, TestQuestionActivity.class);
                intent.putExtra("user_test_id", testID);
                startActivity(intent);

                finish();
            });
        } else {
            textInformation.append("Правильных ответов: ")
                    .append(item.getInt("MarkPercent"))
                    .append("%");
            if (item.getInt("MarkPercent") >= item.getInt("MinMarkPercent")) {
                button.setText("Сертификат");

                button.setOnClickListener(v -> generateCert(testID));
            } else {
                button.setVisibility(Button.INVISIBLE);
                textInformation.append("\n")
                        .append("Для получения сертификата необходимо: ")
                        .append(item.getInt("MinMarkPercent"))
                        .append("%");
            }

            JSONArray _questions = item.getJSONArray("UserTestQuestions");

            StringBuilder questionsStr = new StringBuilder();
            int size = 200;
            for (int i = 0; i < _questions.length(); i++) {
                JSONObject _question = _questions.getJSONObject(i);
                String isTrue = _question.getInt("IsTrue") == 1 ? "(Верно)" : "(Неверно)";
                questionsStr.append(_question.getString("Question"))
                        .append(" ")
                        .append(isTrue)
                        .append("\n")
                        .append("\n");

            }

            questionsButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CabinetActivity.this);
                    builder.setTitle("Вопросы")
                            .setMessage(questionsStr.toString())
                            .setCancelable(true)
                            .setNegativeButton("OK",
                                    (dialog, id) -> {
                                        questionsButton.toggle();
                                        dialog.cancel();
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

        }


        information.setText(textInformation.toString());
    }

    public void generateCert(int userTestID) {
        ProgressBar progressBar = findViewById(R.id.cabinet_progress);
        ScrollView content = findViewById(R.id.cabinet_scroll);

        api.loading(true, progressBar);
        content.setVisibility(ScrollView.INVISIBLE);

        api.generateCert(userTestID, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    api.loading(false, progressBar);
                    content.setVisibility(ScrollView.VISIBLE);
                    api.loadError(findViewById(android.R.id.content), v -> {
                        generateCert(userTestID);
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

                        if (result.getString("error_type").equals("token"))
                            invalidToken();

                        AlertDialog.Builder builder = new AlertDialog.Builder(CabinetActivity.this);
                        builder.setTitle("Ошибка")
                                .setMessage(result.getString("message"))
                                .setCancelable(true)
                                .setNegativeButton("OK",
                                        (dialog, id) -> dialog.cancel());
                        AlertDialog alert = builder.create();
                        alert.show();

                    } catch (IOException e) {
                        generateCert(userTestID);
                    } catch (JSONException e) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Api.site + "tests/generate_cert?user_test_id=" + userTestID));
                        startActivity(intent);
                    }
                });
            }
        });
    }

}
