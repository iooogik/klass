package iooojik.app.klass.notes;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import iooojik.app.klass.AppСonstants;
import iooojik.app.klass.Database;
import iooojik.app.klass.R;

import static iooojik.app.klass.AppСonstants.APP_PREFERENCES_SHOW_BOOK_MATERIALS;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<Note> notes;
    //Переменные для работы с БД
    private Database mDBHelper;
    private SQLiteDatabase mDb;
    private Bundle bundle = new Bundle();
    private Fragment fragment;
    private Context context;
    private Cursor userCursor;

    NotesAdapter(Context context, List<Note> notes, Fragment fragment){
        this.notes = notes;
        this.inflater = LayoutInflater.from(context);
        this.fragment = fragment;
        this.context = context;
    }


    @NonNull
    @Override
    public NotesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view_item_note, parent, false);//поиск элемента списка
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesAdapter.ViewHolder holder, int position) {
        mDBHelper = new Database(context);
        mDBHelper.openDataBase();
        //получение и установка данных в элемент
        Note note = notes.get(position);
        SharedPreferences settings = context.getSharedPreferences(AppСonstants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int val = settings.getInt(APP_PREFERENCES_SHOW_BOOK_MATERIALS, 0);

        NavController navHostFragment = NavHostFragment.findNavController(fragment);

        if (!(note.getName().equals("Математические формулы") && val == 1)) {
            holder.name.setText(note.getName());
            holder.desc.setText(note.getDescription());

            Bitmap bitmap = note.getImage();

            if (bitmap != null) {
                ImageView img = holder.imageView;
                img.setMinimumHeight(150);
                img.setMinimumWidth(150);
                img.setImageBitmap(bitmap);

                img.setOnLongClickListener(view -> {

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    TextView textView = new TextView(builder.getContext());
                    textView.setText("Вы действительно хотите удалить  QR-код?");
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.addView(textView);
                    builder.setView(layout);

                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mDb = mDBHelper.getReadableDatabase();

                            userCursor =  mDb.rawQuery("Select * from Notes WHERE _id=?",
                                    new String[]{String.valueOf(note.getId())});
                            userCursor.moveToFirst();

                            if(userCursor.getBlob(userCursor.getColumnIndex("image")) != null){
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("image", (byte[]) null);
                                mDb.update("Notes", contentValues, "_id="+ position, null);
                            }
                        }
                    });

                    builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    builder.create().show();


                    return false;
                });

            }

            if (note.getType().equals("shop")){
                mDb = mDBHelper.getReadableDatabase();

                userCursor =  mDb.rawQuery("Select * from Notes WHERE _id=?",
                        new String[]{String.valueOf(note.getId())});
                userCursor.moveToFirst();

                if(userCursor.getInt(userCursor.getColumnIndex("isCompleted")) == 1){
                    holder.completed.setVisibility(View.VISIBLE);
                }else if(userCursor.getInt(userCursor.getColumnIndex("isCompleted")) == 0){
                    holder.completed.setVisibility(View.GONE);
                }
            }

            mDb = mDBHelper.getReadableDatabase();

            userCursor =  mDb.rawQuery("Select * from Notes WHERE _id=?",
                    new String[]{String.valueOf(note.getId())});
            userCursor.moveToFirst();

            if(userCursor.getInt(userCursor.getColumnIndex("isNotifSet")) == 1){
                holder.isNotifSet.setVisibility(View.VISIBLE);
            }else if(userCursor.getInt(userCursor.getColumnIndex("isNotifSet")) == 0){
                holder.isNotifSet.setVisibility(View.GONE);
            }

            //слушатель для открытия фрагмента с заметкой
            holder.frameLayout.setOnClickListener(v -> {
                bundle.putString("button name", note.getName());
                bundle.putInt("button ID", note.getId());

                switch (note.getType()) {
                    case "shop":
                        navHostFragment.navigate(R.id.nav_checkList, bundle);
                        break;
                    case "standart":
                        navHostFragment.navigate(R.id.nav_standart_note, bundle);
                        break;
                    case "book":
                        navHostFragment.navigate(R.id.nav_book, bundle);
                        break;
                    default:
                        Snackbar.make(holder.itemView, "Error", Snackbar.LENGTH_LONG).show();
                        break;
                }

            });

            holder.frameLayout.setOnLongClickListener(v -> {

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(v.getContext());

                builder.setTitle("Важное сообщение!")
                        .setMessage("Вы действительно хотите удалить заметку?")
                        .setPositiveButton("Удалить", (dialog, id) -> {

                            mDb = mDBHelper.getWritableDatabase();

                            mDb.delete("Notes", "_id=" + (note.getId()), null);

                            Notes.ITEMS.remove(note);
                            notifyItemRemoved(position);


                        })
                        .setNegativeButton("Нет", (dialog, which) -> dialog.cancel())
                        .show();
                return true;
            });
        } else {
            holder.frameLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        final ImageView imageView, completed, isNotifSet;
        final TextView name, desc;
        final LinearLayout back;
        final FrameLayout frameLayout;
        ViewHolder(View view){
            super(view);
            frameLayout = view.findViewById(R.id.frame);
            imageView = view.findViewById(R.id.subImg);
            name = view.findViewById(R.id.name);
            desc = view.findViewById(R.id.description);
            back = view.findViewById(R.id.background);
            isNotifSet = view.findViewById(R.id.notif);
            completed = view.findViewById(R.id.completed);
        }
    }
}