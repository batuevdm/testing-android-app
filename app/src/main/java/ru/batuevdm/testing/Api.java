package ru.batuevdm.testing;
// Подключение библиотек

import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

// Класс для работы с API сайта
class Api {
    static String site = "https://rctrl.ru/"; // Адрес сайта
    private Context context; // Контекст приложения для показа уведомлений
    private Snackbar bar; // Переменная Snackbar (Сообщение внизу экрана)

    /**
     * Конструктор класса
     *
     * @param context Контекст приложения
     */
    Api(Context context) {
        this.context = context;
    }

    /**
     * GET-запрос к сайту
     *
     * @param address  Адрес сайта
     * @param callback Callback функция, которая выполнится при завершении запроса
     */
    private void get(String address, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(site + address)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    /**
     * GET-запрос к сайту
     *
     * @param address  Адрес сайта
     * @param params   Параметры запроса
     * @param callback Callback функция, которая выполнится при завершении запроса
     */
    private void post(String address, Map<String, String> params, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder requestBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            requestBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        RequestBody requestBody = requestBuilder.build();

        Request request = new Request.Builder()
                .post(requestBody)
                .url(site + address)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    /**
     * Вывод сообщения на экран
     *
     * @param text Текст сообщения
     */
    void toast(String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Показ или скрытие прогресс бара
     *
     * @param status      Показать (true) или скрыть (false) прогресс-бар
     * @param progressBar Прогресс-бар
     */
    void loading(boolean status, ProgressBar progressBar) {
        if (status) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    /**
     * Вывод сообщения при ошибке загрузки с кнопкой повтора соединения
     *
     * @param view            Форма, на которой покажется сообщение
     * @param onClickListener Обработчик клика на кнопку повтора соединения
     */
    void loadError(View view, View.OnClickListener onClickListener) {
        bar = Snackbar.make(view, "Ошибка загрузки. Проверьте подключение к интернету.", Snackbar.LENGTH_INDEFINITE);
        bar.setAction("Повторить", onClickListener);
        bar.show();
    }

    /**
     * Скрытие ошибки загрузки
     */
    void dismissLoadError() {
        if (bar != null && bar.isShown())
            bar.dismiss();
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * Вход
     *
     * @param login    User login
     * @param password User password
     * @param callback Функция, которая выполнится при завершении запроса
     */
    void auth(String login, String password, Callback callback) {

        String varsion = Build.VERSION.RELEASE;

        Map<String, String> params = new HashMap<>();
        params.put("login", login);
        params.put("password", password);
        params.put("device", getDeviceName() + " (Android " + varsion + ")");

        post("user/auth", params, callback);
    }

    /**
     * Регистрация
     *
     * @param firstName Имя
     * @param lastName  Фамилия
     * @param login     Логин
     * @param password  Пароль
     * @param callback  Функция, которая выполнится при завершении запроса
     */
    void register(String firstName, String lastName, String login, String password, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("first_name", firstName);
        params.put("last_name", lastName);
        params.put("login", login);
        params.put("password", password);

        post("user/register", params, callback);
    }

    /**
     * Оформление нового заказа
     *
     * @param token    User Token
     * @param callback Функция, которая выполнится при завершении запроса
     */
    void userInfo(String token, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);

        post("user/info", params, callback);
    }

    void userEdit(String token, String firstName, String lastName, String login, Callback callback) {
        userEdit(token, firstName, lastName, login, null, null, callback);
    }

    void userEdit(String token, String firstName, String lastName, String login, String oldPassword, String password, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("first_name", firstName);
        params.put("last_name", lastName);
        params.put("login", login);
        if (oldPassword != null && password != null) {
            params.put("password", password);
            params.put("old_password", oldPassword);
        }

        post("user/edit", params, callback);
    }

    /**
     * Получение списка тестов
     *
     * @param token    User Token
     * @param callback Функция, которая выполнится при завершении запроса
     */
    void testsList(String token, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);

        post("tests/get_list", params, callback);
    }

    void testGet(String token, int testID, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("test_id", String.valueOf(testID));

        post("tests/get", params, callback);
    }

    void newTest(String token, int testID, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("test_id", String.valueOf(testID));

        post("user/new_test", params, callback);
    }

    void getQuestion(String token, int userTestID, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("user_test_id", String.valueOf(userTestID));

        post("user/get_question", params, callback);
    }

    void setAnswer(String token, int userTestID, int userQuestionID, String answer, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("user_test_id", String.valueOf(userTestID));
        params.put("user_question_id", String.valueOf(userQuestionID));
        params.put("answer", answer);

        post("user/set_answer", params, callback);
    }

    void userTests(String token, Callback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("token", token);

        post("user/tests", params, callback);
    }

    void generateCert(int userTestID, Callback callback) {
        get("test/generate_cert?user_test_id=" + userTestID, callback);
    }
}