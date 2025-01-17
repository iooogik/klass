package iooojik.app.klass.subjects.astronomy;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import iooojik.app.klass.Database;
import iooojik.app.klass.R;

public class ScrollingArticle extends Fragment{

    private Cursor userCursor;

    public ScrollingArticle(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_scrolling_article, container, false);

        Bundle args = this.getArguments();
        assert args != null;
        int id = args.getInt("_id");
        Database mDBHelper = new Database(getContext());
        mDBHelper.openDataBase();

        mDBHelper.updateDataBase();

        SQLiteDatabase mDb = mDBHelper.getReadableDatabase();
        userCursor =  mDb.rawQuery("Select * from Planets", null);

        userCursor.moveToPosition(id);

        TextView textView = view.findViewById(R.id.article_text);

        textView.setText(Html.fromHtml(getEditedText()));
        ImageView imageView = view.findViewById(R.id.sc_back);

        Bitmap bitmap;

        byte[] bytesImg = userCursor.getBlob(userCursor.getColumnIndex("images"));
        bitmap =  BitmapFactory.decodeByteArray(bytesImg, 0, bytesImg.length);

        imageView.setImageBitmap(bitmap);
        setHasOptionsMenu(true);

        return view;
    }

    private String getEditedText(){
        String tempText = userCursor.getString(userCursor.getColumnIndex("article"));
        StringBuilder stringBuilder = new StringBuilder();
        try {
            for (int i = 0; i < tempText.length(); i++) {
                stringBuilder.append(tempText.charAt(i));
            }
        }catch (Exception e){
            Log.i("scrollingArticle", String.valueOf(e));
        }


        return stringBuilder.toString();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }
}
