package iooojik.app.klass.tests;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import iooojik.app.klass.Api;
import iooojik.app.klass.AppСonstants;
import iooojik.app.klass.Database;
import iooojik.app.klass.R;
import iooojik.app.klass.models.PostResult;
import iooojik.app.klass.models.ServerResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static iooojik.app.klass.AppСonstants.testDivider;


public class Questions extends Fragment implements View.OnClickListener{

    public Questions() {}

    private View view;
    private Database mDBHelper;
    private SQLiteDatabase mDb;
    private Cursor userCursor;

    private List<String> questions;
    private List<String> answers;
    private List<String> isTrue;
    private HashMap<Integer, Bitmap> images;
    private int userScore = 0;
    private int totalScore = 0;
    private Api api;
    private SharedPreferences preferences;
    private final Handler chrono = new Handler();;
    private boolean running = true;
    private int seconds;
    private int scorePerAnswer = 1;
    private NavController navHostFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_questions, container, false);
        mDBHelper = new Database(getContext());
        mDBHelper.openDataBase();

        navHostFragment = NavHostFragment.findNavController(this);

        questions = new ArrayList<>();
        answers = new ArrayList<>();
        isTrue = new ArrayList<>();
        images= new HashMap<>();
        preferences = getActivity().getSharedPreferences(AppСonstants.APP_PREFERENCES, Context.MODE_PRIVATE);

        getQuestions();
        getAnswers();
        getImages();
        setTest();
        setTimer();
        getScorePerAnswer();
        totalScore = questions.size() * scorePerAnswer;
        ImageView btn = view.findViewById(R.id.send_answers);
        btn.setOnClickListener(this);

        return view;
    }

    private void getScorePerAnswer() {
        mDb = mDBHelper.getReadableDatabase();

        userCursor =  mDb.rawQuery("Select * from " + AppСonstants.TABLE_TESTS +
                " WHERE _id=?", new String[]{String.valueOf(getTestID())});

        userCursor.moveToFirst();

        scorePerAnswer = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_SCORE_QUEST));

    }

    private void getImages() {
        mDb = mDBHelper.getReadableDatabase();
        userCursor =  mDb.rawQuery("Select * from picturesToQuestions WHERE test_id=?", new String[]{String.valueOf(getTestID())});
        userCursor.moveToFirst();
        Bitmap bitmap;
        while (!userCursor.isAfterLast()){
            byte[] bytesImg = userCursor.getBlob(userCursor.getColumnIndex("image"));
            bitmap = BitmapFactory.decodeByteArray(bytesImg, 0, bytesImg.length);
            images.put(userCursor.getInt(userCursor.getColumnIndex("num_question")), bitmap);
            userCursor.moveToNext();
        }

    }

    private void setTest(){
        List<String> temp = answers;

        for (int i = 0; i < questions.size(); i++) {

            View view1 = getLayoutInflater().inflate(R.layout.recycler_view_item_question, null, false);
            //вопрос
            TextView nameNote = view1.findViewById(R.id.task);
            nameNote.setText(questions.get(i));

            for (Map.Entry entry : images.entrySet()){
                if ((int) entry.getKey() - 1 == i){
                    ImageView img = view1.findViewById(R.id.image);
                    img.setVisibility(View.VISIBLE);

                    img.setImageBitmap((Bitmap) entry.getValue());
                    break;
                }
            }

            RadioButton radioButton1 = view1.findViewById(R.id.radioButton1);
            radioButton1.setText(temp.get(0));

            radioButton1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked && isTrue.contains(radioButton1.getText().toString())){
                    userScore+=scorePerAnswer;

                }else if (!isChecked && isTrue.contains(radioButton1.getText().toString())){
                    userScore-=scorePerAnswer;
                }
            });

            RadioButton radioButton2 = view1.findViewById(R.id.radioButton2);
            radioButton2.setText(temp.get(1));

            radioButton2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked && isTrue.contains(radioButton2.getText().toString())){
                    userScore+=scorePerAnswer;

                }else if (!isChecked && isTrue.contains(radioButton2.getText().toString())){
                    userScore-=scorePerAnswer;
                }
            });

            RadioButton radioButton3 = view1.findViewById(R.id.radioButton3);
            radioButton3.setText(temp.get(2));

            radioButton3.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked && isTrue.contains(radioButton3.getText().toString())){
                    userScore+=scorePerAnswer;

                }else if (!isChecked && isTrue.contains(radioButton3.getText().toString())){
                    userScore-=scorePerAnswer;
                }
            });

            RadioButton radioButton4 = view1.findViewById(R.id.radioButton4);
            radioButton4.setText(temp.get(3));

            radioButton4.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked && isTrue.contains(radioButton4.getText().toString())){
                    userScore+=scorePerAnswer;

                }else if (!isChecked && isTrue.contains(radioButton4.getText().toString())){
                    userScore-=scorePerAnswer;
                }
            });

            temp.subList(0, 4).clear();

            //установка на активити
            LinearLayout linearLayout = view.findViewById(R.id.scrollQuestThemes);
            linearLayout.addView(view1);
        }
        temp.clear();
    }

    private int getTestID(){
        Bundle arguments = this.getArguments();
        assert arguments != null;
        return arguments.getInt("test id");
    }

    private void getAnswers(){
        mDb = mDBHelper.getReadableDatabase();

        String TEMPansws = userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_TEXT_ANSWERS));

        answers.addAll(Arrays.asList(TEMPansws.split(Pattern.quote(testDivider))));

        TEMPansws = userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_ANSWERS));

        isTrue.addAll(Arrays.asList(TEMPansws.split(Pattern.quote(testDivider))));
        System.out.println(answers + " " + answers.size());
    }

    private void getQuestions() {
        mDb = mDBHelper.getReadableDatabase();

        userCursor =  mDb.rawQuery("Select * from " + AppСonstants.TABLE_TESTS +
                " WHERE _id=?", new String[]{String.valueOf(getTestID())});

        userCursor.moveToFirst();

        String TEMP_quests = userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_QUESTIONS));
        String[] quests = TEMP_quests.split(Pattern.quote(testDivider));
        questions.addAll(Arrays.asList(quests));
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.send_answers){
            endTest(false);
        }
    }

    private void endTest(boolean isTime){

        if (isTime){

            sendAnswers();
            navHostFragment.navigate(R.id.nav_tests);

        } else {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view.getContext());
            final LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);

            View view1 = getLayoutInflater().inflate(R.layout.text_view, null, false);
            TextView textView = view1.findViewById(R.id.tv);

            textView.setText("Вы действительно хотите завершить выполнение теста?");

            layout.addView(view1);
            builder.setView(layout);

            builder.setPositiveButton("Да", (dialog, which) -> {
                sendAnswers();
                navHostFragment.navigate(R.id.nav_tests);
            });

            builder.setNegativeButton("Нет", (dialog, which) -> dialog.cancel());

            builder.create().show();
        }
    }

    private void doRetrofit(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppСonstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(Api.class);
    }

    private void sendAnswers(){
        mDb = mDBHelper.getWritableDatabase();

        userCursor =  mDb.rawQuery("Select * from " + AppСonstants.TABLE_TESTS + " WHERE _id=?",
                new String[]{String.valueOf(getTestID())});
        userCursor.moveToFirst();

        ContentValues contentValues = new ContentValues();
        contentValues.put(AppСonstants.TABLE_TESTS_USER_SCORE, userScore);
        contentValues.put(AppСonstants.TABLE_TESTS_IS_PASSED, 1);
        mDb.update(AppСonstants.TABLE_TESTS, contentValues, "_id =" + (getTestID()), null);
        FrameLayout frameLayout = Tests.VIEW.findViewById(R.id.test_frame);
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);

        TestTheme testTheme = Tests.TEST_ITEMS.get(getTestID() - 1);
        testTheme.setUserScore(userScore);
        testTheme.setPassed(true);
        Tests.TEST_ADAPTER.notifyDataSetChanged();
        //отправка результатов

        doRetrofit();
        HashMap<String, String> map = new HashMap<>();
        map.put("user_email", preferences.getString(AppСonstants.USER_EMAIL, ""));
        map.put("group_id", String.valueOf(userCursor.getInt(
                userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_GROUP_ID))));

        map.put("result",  String.valueOf((userScore / totalScore) * 100.0f));



        Call<ServerResponse<PostResult>> updateInfo = api.addResult(
                AppСonstants.X_API_KEY, preferences.getString(AppСonstants.AUTH_SAVED_TOKEN, ""), map);

        updateInfo.enqueue(new Callback<ServerResponse<PostResult>>() {
            @Override
            public void onResponse(Call<ServerResponse<PostResult>> call, Response<ServerResponse<PostResult>> response) {
                if (response.code() != 200) Log.e("SENDING RESULT", String.valueOf(response.raw()));
            }

            @Override
            public void onFailure(Call<ServerResponse<PostResult>> call, Throwable t) {
                Log.e("SENDING RESULT", String.valueOf(t));
            }
        });

        //изменяем значения койнов
        //если тест выполнен на 0-30%, то добавляем 1 койн, если на 31 - 60 %, то 3 койна,
        // если на 61 - 75 %, то 4 койна, если 76 - 100%, то 5 койнов


        HashMap<String, String> log = new HashMap<>();
        log.put("user_email", preferences.getString(AppСonstants.USER_EMAIL, ""));

        int percent = (userScore / totalScore) * 100;

        int coins = preferences.getInt(AppСonstants.USER_COINS, 0);

        if (percent <= 30) AppСonstants.USER_COINS += 1;
        else if (percent >= 31 && percent <= 60) {
            coins += 3;
            log.put("achievement_change", "add " + 3 + " coins");
        }
        else if (percent >= 61 && percent <= 75) {
            coins += 4;
            log.put("achievement_change", "add " + 4 + " coins");
        }
        else if (percent >= 76 && percent <= 100) {
            coins += 5;
            log.put("achievement_change", "add " + 5 + " coins");
        }

        preferences.edit().putInt(AppСonstants.USER_COINS, coins).apply();

        //получаем дату
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateText = dateFormat.format(currentDate);
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeText = timeFormat.format(currentDate);

        String full_date = dateText + " " + timeText;

        log.put("date_change", full_date);

        //логируем койны
        Call<ServerResponse<PostResult>> call = api.logAchievement(AppСonstants.X_API_KEY,
                AppСonstants.AUTH_SAVED_TOKEN, log);

        call.enqueue(new Callback<ServerResponse<PostResult>>() {
            @Override
            public void onResponse(Call<ServerResponse<PostResult>> call, Response<ServerResponse<PostResult>> response) {
                if (response.code() != 200) Log.e("LOG ACHIEVEMENT", String.valueOf(response.raw()));
            }

            @Override
            public void onFailure(Call<ServerResponse<PostResult>> call, Throwable t) {
                Log.e("LOG ACHIEVEMENT", String.valueOf(t));
            }
        });

        //добавляем койны
        HashMap<String, String> changes = new HashMap<>();
        changes.put("user_email", preferences.getString(AppСonstants.USER_EMAIL, ""));
        changes.put("_id", String.valueOf(preferences.getInt(AppСonstants.ACHIEVEMENTS_ID, -1)));
        changes.put("coins", String.valueOf(preferences.getInt(AppСonstants.USER_COINS, 0)));

        Call<ServerResponse<PostResult>> call2 = api.updateAchievement(AppСonstants.X_API_KEY,
                preferences.getString(AppСonstants.STANDART_TOKEN, ""), changes);

        call2.enqueue(new Callback<ServerResponse<PostResult>>() {
            @Override
            public void onResponse(Call<ServerResponse<PostResult>> call, Response<ServerResponse<PostResult>> response) {
                if (response.code() != 200) Log.e("ADD ACHIEVEMENT", String.valueOf(response.raw()));
            }

            @Override
            public void onFailure(Call<ServerResponse<PostResult>> call, Throwable t) {
                Log.e("ADD ACHIEVEMENT", String.valueOf(t));
            }
        });

    }

    @SuppressLint("DefaultLocale")
    private void setTimer(){
        userCursor =  mDb.rawQuery("Select * from " + AppСonstants.TABLE_TESTS + " WHERE _id=?",
                new String[]{String.valueOf(getTestID())});
        userCursor.moveToFirst();
        int time = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_TIME));
        TextView time_process = view.findViewById(R.id.timer);
        time_process.setVisibility(View.VISIBLE);
        running = true;
        seconds = time * 60;

        chrono.post(new Runnable() {
            @Override
            public void run() {
                if(running && seconds != 0) {
                    int minutes = (seconds % 3600) / 60;
                    int secon = seconds % 60;
                    String time = String.format("%02d:%02d", minutes, secon);
                    time_process.setText(time);
                    seconds--;
                    chrono.postDelayed(this, 1000);
                } else {
                    running = false;
                    endTest(true);
                    navHostFragment.navigate(R.id.nav_tests);
                }
            }
        });
    }
}
