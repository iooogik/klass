package iooojik.app.klass.notes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import iooojik.app.klass.AppСonstants;
import iooojik.app.klass.Database;
import iooojik.app.klass.R;
import iooojik.app.klass.api.Api;
import iooojik.app.klass.models.PostResult;
import iooojik.app.klass.models.ServerResponse;
import iooojik.app.klass.models.notesData.NotesData;
import iooojik.app.klass.models.notesData.OnlineNote;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Notes extends Fragment {

    //Переменные для работы с БД
    private Database mDBHelper;
    private SQLiteDatabase mDb;

    private View view;
    private Context context;
    private NotesAdapter NOTES_ADAPTER;

    private Api api;
    private SharedPreferences preferences;

    private FloatingActionButton fab;
    private Cursor userCursor;

    private List<NoteObject> ITEMS = new ArrayList<>();

    public Notes() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_notes, container, false);
        preferences = getActivity().getSharedPreferences(AppСonstants.APP_PREFERENCES, Context.MODE_PRIVATE);
        context = view.getContext();
        fab = getActivity().findViewById(R.id.fab);
        fab.setImageResource(R.drawable.round_keyboard_arrow_up_24);
        setHasOptionsMenu(true);
        return view;
    }

    @SuppressLint("InflateParams")
    private void enableBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity());
        View bottomSheet = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_notes, null);

        bottomSheetDialog.setContentView(bottomSheet);

        Button add = bottomSheet.findViewById(R.id.add);
        add.setOnClickListener(v -> {
            addNote();
            bottomSheetDialog.hide();
            fab.show();
        });

        Button sync = bottomSheet.findViewById(R.id.sync);
        sync.setOnClickListener(v -> {
            uploadNotes();
            bottomSheetDialog.hide();
            fab.show();
        });


        Button download = bottomSheet.findViewById(R.id.download);
        download.setOnClickListener(v -> {
            downloadNotes();
            bottomSheetDialog.hide();
            fab.show();
        });

        bottomSheetDialog.setOnCancelListener(dialog -> fab.show());


        fab.setOnClickListener(v -> {
            bottomSheetDialog.show();
            fab.hide();
        });
    }

    private void startProcedures() {
        mDBHelper = new Database(getContext());
        mDBHelper.openDataBase();
        mDBHelper.updateDataBase();

        //необходимо очистить содержимое, чтобы при старте активити не было повторяющихся элементов
        try {
            ITEMS.clear();
        } catch (Exception e) {
            Log.i(AppСonstants.TABLE_NOTES, String.valueOf(e));
        }
        updateNotes();

    }

    //обновление проектов на активити
    private void updateNotes() {
        //добавление новых проектов
        mDb = mDBHelper.getReadableDatabase();
        userCursor = mDb.rawQuery("Select * from " + AppСonstants.TABLE_NOTES, null);
        userCursor.moveToFirst();

        String name, desc, type;
        Bitmap bitmap = null;
        while (!userCursor.isAfterLast()) {

            name = String.valueOf(userCursor.getString(1)); //колонки считаются с 0

            type = userCursor.getString(8);

            desc = String.valueOf(userCursor.getString(2));

            byte[] bytesImg = userCursor.getBlob(userCursor.getColumnIndex(AppСonstants.TABLE_IMAGE));

            if (bytesImg != null) {
                bitmap = BitmapFactory.decodeByteArray(bytesImg, 0, bytesImg.length);
            }

            if (name != null || type != null)
                ITEMS.add(new NoteObject(name, desc, bitmap, type,
                        userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_ID)), -1));

            userCursor.moveToNext();

            bitmap = null;
        }


        userCursor.close();
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        NOTES_ADAPTER = new NotesAdapter(getContext(), ITEMS, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(NOTES_ADAPTER);
    }

    @SuppressLint("InflateParams")
    private void addNote() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

        final LinearLayout layout1 = new LinearLayout(getContext());
        layout1.setOrientation(LinearLayout.VERTICAL);
        //ввод названия заметки
        View view1 = getLayoutInflater().inflate(R.layout.edit_text, null, false);
        TextInputLayout textInputLayout = view1.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Введите название заметки");
        EditText nameNote = textInputLayout.getEditText();

        layout1.addView(view1);

        final String standartTextNote = "Стандартная заметка";
        final String marckedList = "Маркированный список";

        final String[] types = new String[]{standartTextNote, marckedList};
        //выбор типа
        View view2 = getLayoutInflater().inflate(R.layout.spinner_item, null, false);
        TextInputLayout textInputLayout2 = view2.findViewById(R.id.spinner_layout);

        AutoCompleteTextView spinner = textInputLayout2.findViewById(R.id.filled_exposed_dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.list_item, types);
        spinner.setAdapter(adapter);
        spinner.setText(types[0], false);
        layout1.addView(view2);
        builder.setView(layout1);


        int id = 0;
        if (ITEMS.size() != 0) {
            NoteObject noteObject = ITEMS.get(ITEMS.size() - 1);
            id = noteObject.getId() + 1;
        }

        int finalId = id;
        builder.setPositiveButton("Добавить",
                (dialog, which) -> {
                    if (!nameNote.getText().toString().isEmpty() &&
                            !spinner.getText().toString().isEmpty()) {
                        String name = nameNote.getText().toString();
                        String text = "Новая заметка";
                        String type = "";
                        mDb = mDBHelper.getWritableDatabase();

                        if (spinner.getText().toString().equals(standartTextNote)) {
                            type = AppСonstants.TABLE_DB_TYPE_STNDRT;
                        } else if (spinner.getText().toString().equals(marckedList)) {
                            type = AppСonstants.TABLE_DB_TYPE_SHOP;
                        }

                        //добавление в бд и обновление адаптера
                        ContentValues cv = new ContentValues();
                        cv.put(AppСonstants.TABLE_ID, finalId);
                        cv.put(AppСonstants.TABLE_NAME, name);
                        cv.put(AppСonstants.TABLE_TEXT, text);
                        cv.put(AppСonstants.TABLE_TYPE, type);
                        cv.put(AppСonstants.TABLE_IS_NOTIF_SET, 0);
                        cv.put(AppСonstants.TABLE_PERM_TO_SYNC, 1);
                        //получение даты
                        Date currentDate = new Date();
                        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy",
                                Locale.getDefault());
                        String dateText = dateFormat.format(currentDate);
                        cv.put(AppСonstants.DATE_FIELD, dateText);
                        //запись
                        mDb.insert(AppСonstants.TABLE_NOTES, null, cv);

                        ITEMS.add(new NoteObject(name, null, null, type, finalId, -1));
                        NOTES_ADAPTER.notifyDataSetChanged();
                    } else {
                        Snackbar.make(view, "Что-то пошло не так. Проверьте, пожалуйста, название и выбранный тип.",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
        builder.create().show();
        fab.show();
    }

    private void doRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppСonstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(Api.class);
    }

    @SuppressLint("Recycle")
    private void uploadNotes() {
        //получаем id каждой заметки из списка ITEMS и узнаём, можно ли добавлять картинку в базу
        List<NoteObject> uploadNoteObjects = new ArrayList<>();
        mDb = mDBHelper.getReadableDatabase();

        Cursor userCursor;
        for (NoteObject noteObject : ITEMS) {
            int id = noteObject.getId();

            userCursor = mDb.rawQuery("Select * from " + AppСonstants.TABLE_NOTES + " WHERE _id=?", new String[]{String.valueOf(id)});
            userCursor.moveToFirst();

            if (userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_PERM_TO_SYNC)) == 1) {
                uploadNoteObjects.add(noteObject);
            }
        }

        doRetrofit();
        //получаем все пользовательские заметки
        //удаляем все пользовательские заметки
        if (uploadNoteObjects.size() > 0) clearNotes();

        for (NoteObject noteObject : uploadNoteObjects) {

            //заносим каждую заметку в базу
            HashMap<String, String> map = new HashMap<>();
            userCursor = mDb.rawQuery("Select * from "+ AppСonstants.TABLE_NOTES +" WHERE _id=?",
                    new String[]{String.valueOf(noteObject.getId())});

            userCursor.moveToFirst();
            String name = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_NAME)));
            if (name.trim().isEmpty()) name = "null";
            //собираем данные
            String shortName = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_SHORT_NAME)));
            if (shortName.trim().isEmpty()) shortName = "null";
            String text = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TEXT)));
            if (text.trim().isEmpty()) text = "null";
            String date = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.DATE_FIELD)));
            String type = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TYPE)));
            String isNotifSet = String.valueOf(userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_IS_NOTIF_SET)));
            String permToSync = String.valueOf(userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_PERM_TO_SYNC)));
            String isChecked = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_IS_CHECKED)));
            String points = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_POINTS)));
            String isCompleted = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_IS_COMPLETED)));
            String decodeQR = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_DECODE_QR)));
            String typeface = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_TYPEFACE)));
            String fontSize = String.valueOf(userCursor.getString(userCursor.getColumnIndex(AppСonstants.TABLE_FONT_SIZE)));
            String image = "null";

            //добавляем данные в map
            map.put(AppСonstants.USER_ID_FIELD, String.valueOf(getUserID()));
            map.put(AppСonstants.TABLE_NAME, name);
            map.put(AppСonstants.TABLE_SHORT_NAME, shortName);
            map.put(AppСonstants.TABLE_TEXT, text);
            map.put(AppСonstants.DATE_FIELD, date);
            map.put(AppСonstants.TABLE_TYPE, type);
            map.put(AppСonstants.TABLE_IS_NOTIF_SET, isNotifSet);
            map.put(AppСonstants.TABLE_PERM_TO_SYNC, permToSync);
            map.put(AppСonstants.TABLE_IS_CHECKED, isChecked);
            map.put(AppСonstants.TABLE_POINTS, points);
            map.put(AppСonstants.TABLE_IS_COMPLETED, isCompleted);
            map.put(AppСonstants.TABLE_DECODE_QR, decodeQR);
            map.put(AppСonstants.TABLE_IMAGE, image);
            map.put(AppСonstants.TABLE_TYPEFACE, typeface);
            map.put(AppСonstants.TABLE_FONT_SIZE, fontSize);
            //отправляем данные
            Call<ServerResponse<PostResult>> call = api.uploadNotes(AppСonstants.X_API_KEY,
                    preferences.getString(AppСonstants.AUTH_SAVED_TOKEN, ""), map);

            call.enqueue(new Callback<ServerResponse<PostResult>>() {
                @Override
                public void onResponse(Call<ServerResponse<PostResult>> call, Response<ServerResponse<PostResult>> response) {
                    if (response.code() != 200) {
                        Log.e("UPLOAD NOTES", String.valueOf(response.raw()));
                    }
                }

                @Override
                public void onFailure(Call<ServerResponse<PostResult>> call, Throwable t) {
                    Log.e("UPLOAD NOTES", String.valueOf(t));
                }
            });
        }
        Snackbar.make(view, "Ваши заметки успешно загружены на сервер!", Snackbar.LENGTH_LONG).show();
    }

    private void clearNotes() {
        doRetrofit();
        Call<ServerResponse<NotesData>> call = api.getNotes(AppСonstants.X_API_KEY, AppСonstants.USER_ID_FIELD, String.valueOf(getUserID()));
        call.enqueue(new Callback<ServerResponse<NotesData>>() {
            @Override
            public void onResponse(Call<ServerResponse<NotesData>> call, Response<ServerResponse<NotesData>> response) {
                if (response.code() == 200) {
                    List<OnlineNote> onlineNotes = response.body().getData().getOnlineNotes();
                    if (response.body().getData().getOnlineNotes() != null) removeNotes(onlineNotes);
                }
            }

            @Override
            public void onFailure(Call<ServerResponse<NotesData>> call, Throwable t) {

            }
        });
    }

    private void removeNotes(List<OnlineNote> onlineNotes) {
        for (OnlineNote onlineNote : onlineNotes) {
            Call<ServerResponse<PostResult>> call = api.removeNotes(AppСonstants.X_API_KEY, String.valueOf(onlineNote.getId()));
            call.enqueue(new Callback<ServerResponse<PostResult>>() {
                @Override
                public void onResponse(Call<ServerResponse<PostResult>> call, Response<ServerResponse<PostResult>> response) {
                    if (response.code() != 200)
                        Log.e("REMOVE NOTE", response.raw() + " " + onlineNote.getId());
                }

                @Override
                public void onFailure(Call<ServerResponse<PostResult>> call, Throwable t) {
                    Log.e("REMOVE NOTE", String.valueOf(t));
                }
            });
        }
    }

    private int getUserID() {
        int userId = -1;
        userId = Integer.valueOf(preferences.getString(AppСonstants.USER_ID, ""));
        return userId;
    }

    private void downloadNotes() {
        doRetrofit();
        Call<ServerResponse<NotesData>> call = api.getNotes(AppСonstants.X_API_KEY, AppСonstants.USER_ID_FIELD, String.valueOf(getUserID()));
        call.enqueue(new Callback<ServerResponse<NotesData>>() {
            @Override
            public void onResponse(Call<ServerResponse<NotesData>> call, Response<ServerResponse<NotesData>> response) {
                if (response.code() == 200) {
                    List<OnlineNote> onlineNotes = response.body().getData().getOnlineNotes();

                    if (onlineNotes != null) {

                        for (OnlineNote onlineNote : onlineNotes) {
                            boolean result = false;
                            for (NoteObject noteObject2 : ITEMS) {
                                if (noteObject2.getName().equals(onlineNote.getName())) {
                                    result = false;
                                    break;
                                } else result = true;
                            }
                            int id;
                            if (ITEMS.size() == 0) {
                                result = true;
                                id = 1;
                            } else {
                                id = ITEMS.get(0).getId() + 1;
                            }
                            if (result) {

                                String name = onlineNote.getName();
                                //собираем данные
                                String shortName = onlineNote.getShortName();
                                String text = onlineNote.getText();
                                String type = onlineNote.getType();
                                String points = onlineNote.getPoints();
                                String isCompleted = onlineNote.getIsCompleted();
                                String isChecked = onlineNote.getIsChecked();

                                String decodeQR;
                                ContentValues cv = new ContentValues();
                                if (!onlineNote.getDecodeQR().equals("null")) {
                                    decodeQR = onlineNote.getDecodeQR();
                                    cv.put(AppСonstants.TABLE_DECODE_QR, decodeQR);
                                }

                                mDb = mDBHelper.getWritableDatabase();


                                cv.put(AppСonstants.TABLE_NAME, name);
                                cv.put(AppСonstants.TABLE_SHORT_NAME, shortName);
                                cv.put(AppСonstants.TABLE_TEXT, text);
                                cv.put(AppСonstants.TABLE_TYPE, type);
                                cv.put(AppСonstants.TABLE_IS_NOTIF_SET, 0);
                                cv.put(AppСonstants.TABLE_PERM_TO_SYNC, 1);
                                cv.put(AppСonstants.TABLE_POINTS, points);
                                cv.put(AppСonstants.TABLE_IS_COMPLETED, isCompleted);
                                cv.put(AppСonstants.TABLE_IS_CHECKED, isChecked);

                                //получение даты
                                Date currentDate = new Date();
                                DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy",
                                        Locale.getDefault());
                                String dateText = dateFormat.format(currentDate);
                                cv.put(AppСonstants.DATE_FIELD, dateText);
                                //запись
                                mDb.insert(AppСonstants.TABLE_NOTES, null, cv);

                                mDb = mDBHelper.getWritableDatabase();

                                userCursor = mDb.rawQuery("Select * from " + AppСonstants.TABLE_NOTES, null);
                                userCursor.moveToLast();
                                int ident = userCursor.getInt(userCursor.getColumnIndex(AppСonstants.TABLE_ID));
                                ITEMS.add(new NoteObject(name, shortName, null, type, id, ident));
                                NOTES_ADAPTER.notifyDataSetChanged();

                            }
                            Snackbar.make(view, "Вы успешно загрузили заметки!", Snackbar.LENGTH_SHORT).show();
                            fab.show();
                        }
                    }

                    ITEMS.clear();
                    updateNotes();
                } else Log.e("GET NOTES", String.valueOf(response.raw()));
            }

            @Override
            public void onFailure(Call<ServerResponse<NotesData>> call, Throwable t) {

            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        fab.show();
        // Скрываем клавиатуру при открытии Navigation Drawer
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                    getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(requireActivity().getCurrentFocus().
                        getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.i(AppСonstants.TABLE_NOTES, String.valueOf(e));
        }

        getActivity().runOnUiThread(() -> {
            startProcedures();
            enableBottomSheet();
        });

        synchronized (NOTES_ADAPTER) {
            NOTES_ADAPTER.notify();
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                    getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(requireActivity().getCurrentFocus().
                        getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.i(AppСonstants.TABLE_NOTES, String.valueOf(e));
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.menu_notes, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                NOTES_ADAPTER.getFilter().filter(newText);
                return false;
            }
        });
    }
}

