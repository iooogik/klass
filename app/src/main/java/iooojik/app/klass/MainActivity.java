package iooojik.app.klass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;

import iooojik.app.klass.models.ServerResponse;
import iooojik.app.klass.models.getToken.DataToken;
import iooojik.app.klass.models.userData.Data;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static iooojik.app.klass.AppСonstants.APP_PREFERENCES;
import static iooojik.app.klass.AppСonstants.APP_PREFERENCES_THEME;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    public MaterialToolbar materialToolbar;

    // переменная с настройками приложения
    public SharedPreferences preferences;
    //контроллер
    private NavController navController;
    //packageInfo, чтобы получать текущую версию приложения
    private PackageInfo packageInfo;
    private Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // получение настроек
        preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        // изменение темы
        switch (preferences.getInt(APP_PREFERENCES_THEME, 0)) {
            case 0:
                setTheme(R.style.AppThemeLight); // Стандартная
                break;
            case 1:
                setTheme(R.style.AppThemeDark); // Тёмная
                break;
        }
        getAdminToken();

        setContentView(R.layout.activity_main);
        materialToolbar = findViewById(R.id.bar);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //метод проверки на аутентификацию пользователя
        //проверка акутальной версии приложения
        //needUpdate();
        isUserAuth();
        createToolbar();

    }

    private void doBase(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppСonstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(Api.class);
    }

    private void getAdminToken() {
        doBase();

        HashMap<String, String> map = new HashMap<>();
        map.put("username", AppСonstants.adminEmail);
        map.put("password", AppСonstants.adminPassword);

        SharedPreferences preferences = getSharedPreferences(AppСonstants.APP_PREFERENCES, Context.MODE_PRIVATE);

        Call<DataToken> authResponse = api.request_token(AppСonstants.X_API_KEY, map);

        authResponse.enqueue(new Callback<DataToken>() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onResponse(Call<DataToken> call, Response<DataToken> response) {
                if (response.code() == 200) {
                    //получаем данные с сервера
                    DataToken dataToken = response.body();
                    //сохраняем админский токен
                    preferences.edit().putString(AppСonstants.STANDART_TOKEN, dataToken.getToken().getToken()).apply();
                }
                else {
                    Log.e("GET TOKEN", String.valueOf(response.raw()));
                }
            }

            @Override
            public void onFailure(Call<DataToken> call, Throwable t) {
                Log.e("GET TOKEN", String.valueOf(t));
            }
        });
    }

    private void isUserAuth(){

        //получаем токен пользователя
        String token = preferences.getString(AppСonstants.AUTH_SAVED_TOKEN, "");
        if(token.isEmpty() || preferences.getString(AppСonstants.USER_EMAIL, "").isEmpty()){
            navController.navigate(R.id.nav_signIn);
            materialToolbar.setVisibility(View.GONE);
            //убираем шторку
            DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {

            signIN(preferences.getString(AppСonstants.USER_EMAIL, ""),
                    preferences.getString(AppСonstants.USER_PASSWORD, ""));

            navController.navigate(R.id.nav_profile);
        }
    }

    private void createToolbar(){

        materialToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_settings){
                    navController.navigate(R.id.nav_settings);
                }
                return false;
            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // определение "домашнего" фрагмента
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_profile).setDrawerLayout(drawer).build();
        // получение nav-контроллера
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        NavigationUI.setupWithNavController(materialToolbar, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    private void needUpdate(){
        //проверяем текущую версию приложения, получив из бд актуальную и сравнив с установленной
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //текущая версия
        String currentVersion = packageInfo.versionName;

    }

    private void showUpdateDialog(String currVersion) {
        //метод показывание всплывающего окна с просьбой обновить приложение
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        View view = getLayoutInflater().inflate(R.layout.req_update_dialog, null, false);

        TextView installedVersion = view.findViewById(R.id.installedVersion);
        installedVersion.setText(String.format("%s%s", installedVersion.getText(), packageInfo.versionName));

        TextView currentVersion = view.findViewById(R.id.currentVersion);
        currentVersion.setText(String.format("%s%s", currentVersion.getText(), currVersion));




        builder.setPositiveButton("Обновить", (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(AppСonstants.url));
            startActivity(browserIntent);
        });

        builder.setNegativeButton("Обновить позже", (dialog, which) -> dialog.cancel());

        builder.setView(view);
        builder.create().show();

    }

    private void signIN(String uEmail, String uPassword){

        doBase();

        HashMap<String, String> uCredi = new HashMap<>();
        uCredi.put("username", uEmail);
        uCredi.put("password", uPassword);

        Call<ServerResponse<Data>> authResponse = api.UserLogin(uCredi);

        authResponse.enqueue(new Callback<ServerResponse<Data>>() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onResponse(Call<ServerResponse<Data>> call, Response<ServerResponse<Data>> response) {
                if (response.code() == 200) {

                    //получаем данные с сервера
                    ServerResponse<Data> dataAuth = response.body();
                    Data result = dataAuth.getData();
                    preferences.edit().putString(AppСonstants.USER_ID, result.getId()).apply();

                    //сохраняем пользовательский токен
                    preferences.edit().putString(AppСonstants.AUTH_SAVED_TOKEN, dataAuth.getToken()).apply();

                    preferences.edit().putString(AppСonstants.USER_PASSWORD, uPassword).apply();
                    preferences.edit().putString(AppСonstants.USER_EMAIL, result.getEmail()).apply();

                }
                else {
                    Log.e("Sign In", String.valueOf(response.raw()));
                }
            }

            @Override
            public void onFailure(Call<ServerResponse<Data>> call, Throwable t) {
                Log.e("Sign In", String.valueOf(t));
            }
        });

    }



}
