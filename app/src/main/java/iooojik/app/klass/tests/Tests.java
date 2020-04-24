package iooojik.app.klass.tests;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import iooojik.app.klass.AppСonstants;
import iooojik.app.klass.Database;
import iooojik.app.klass.MainActivity;
import iooojik.app.klass.R;

import static iooojik.app.klass.AppСonstants.TABLE_ID;
import static iooojik.app.klass.AppСonstants.TABLE_TESTS;

public class Tests extends Fragment implements View.OnClickListener{

    static View VIEW;

    //Переменная для работы с БД
    private Database mDBHelper;

    private Cursor userCursor;
    static TestsAdapter TEST_ADAPTER;
    static List<TestTheme> TEST_ITEMS;


    public Tests() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        VIEW = inflater.inflate(R.layout.fragment_test, container, false);

        FloatingActionButton floatingActionButton = getActivity().findViewById(R.id.fab);
        floatingActionButton.hide();

        TEST_ITEMS = new ArrayList<>();
        mDBHelper = new Database(getContext());
        mDBHelper.openDataBase();
        mDBHelper.updateDataBase();

        loadAndSetThemes();
        return VIEW;
    }

    private void loadAndSetThemes(){
        SQLiteDatabase mDb = mDBHelper.getReadableDatabase();
        userCursor = mDb.rawQuery("Select * from " + TABLE_TESTS, null);
        userCursor.moveToFirst();
        String name, desc;

        //необходимо очистить содержимое, чтобы при старте активити не было повторяющихся элементов
        try {
            TEST_ITEMS.clear();
        } catch (Exception e){
            Log.i(TABLE_TESTS, String.valueOf(e));
        }

        while (!userCursor.isAfterLast()) {
            //получение имени
            name = String.valueOf(userCursor.getString(1));
            //получение описания
            desc = String.valueOf(userCursor.getString(2));
            //получение количества правильных и неправильных ответов
            int userScore = 0, totalScore = 0;

            userScore = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_USER_SCORE));
            totalScore = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_TOTAL_SCORE));

            int isPassedDB = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_TESTS_IS_PASSED));
            boolean isPassed = false;
            isPassed = isPassedDB == 1;

            int id = userCursor.getInt(userCursor.getColumnIndex(TABLE_ID));
            //добавляем тест в recyclerView
            TEST_ITEMS.add(new TestTheme(name, desc, userScore, totalScore, isPassed, id));
            userCursor.moveToNext();
        }
        userCursor.close();
        RecyclerView recyclerView = VIEW.findViewById(R.id.test_items);
        TEST_ADAPTER = new TestsAdapter(getContext(), TEST_ITEMS);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(TEST_ADAPTER);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.back){
            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
        }
    }
}
