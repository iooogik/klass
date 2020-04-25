package iooojik.app.klass.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;

import iooojik.app.klass.Api;
import iooojik.app.klass.AppСonstants;
import iooojik.app.klass.R;
import iooojik.app.klass.models.ServerResponse;
import iooojik.app.klass.models.achievements.AchievementsData;
import iooojik.app.klass.models.achievements.AchievementsToUser;
import iooojik.app.klass.models.userData.Data;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignIn extends Fragment implements View.OnClickListener {

    public SignIn() {}

    private View view;
    private SharedPreferences preferences;
    private NavController navController;
    private Api api;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        navController = NavHostFragment.findNavController(this);
        preferences = getActivity().getSharedPreferences(AppСonstants.APP_PREFERENCES, Context.MODE_PRIVATE);
        //ещё одна проверка на авторизацию
        String token = preferences.getString(AppСonstants.AUTH_SAVED_TOKEN, "");
        if (!(token.isEmpty())) navController.navigate(R.id.nav_profile);

        view = inflater.inflate(R.layout.fragment_sign_in, container, false);
        //кнопка входа
        Button signIn = view.findViewById(R.id.login);
        signIn.setOnClickListener(this);
        //кнопка перехода на регистрационную форму
        Button reg = view.findViewById(R.id.registr);
        reg.setOnClickListener(this);
        //скрываем fab
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.hide();
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login:
                /**
                 * Обработка нажатия кнопки "Войти"
                 * 1. проверяем, не пустые ли поля с email и password
                 * 2. выполняем авторизацию, если она не удалась, то вызываем Snackbar c сообщением "Что-то пошло не так. Попробуйте снова."
                 * если авторизация прошла успешно, то переходим на главный фрагмент, показываем нижний toolbar и разблокируем шторку
                 */
                EditText email = view.findViewById(R.id.email);
                EditText password = view.findViewById(R.id.password);


                String uEmail = email.getText().toString().trim();
                String uPassword = password.getText().toString().trim();
                //проверка пароля и email
                if (!(uEmail.isEmpty() && uPassword.isEmpty())) {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(AppСonstants.BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    api = retrofit.create(Api.class);
                    //данные пользователя
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

                                //сохраняем пользовательский токен
                                preferences.edit().putString(AppСonstants.AUTH_SAVED_TOKEN, dataAuth.getToken()).apply();
                                preferences.edit().putString(AppСonstants.USER_ID, result.getId()).apply();
                                preferences.edit().putString(AppСonstants.USER_PASSWORD, password.getText().toString()).apply();
                                preferences.edit().putString(AppСonstants.USER_LOGIN, result.getUsername()).apply();


                                //получение достижений и монеток пользователя
                                getUserAchievements(uEmail);

                                DrawerLayout mDrawerLayout = getActivity().findViewById(R.id.drawer_layout);
                                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                navController.navigate(R.id.nav_profile);
                                Log.i("Sign In", String.valueOf(dataAuth.getToken()));

                                MaterialToolbar materialToolbar = getActivity().findViewById(R.id.bar);
                                materialToolbar.setVisibility(View.VISIBLE);

                            }
                            else {
                                Log.e("Sign In", String.valueOf(response.raw()));
                                Snackbar.make(getView(), "Что-то пошло не так. Попробуйте снова.", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ServerResponse<Data>> call, Throwable t) {
                            Log.e("Sign In", String.valueOf(t));
                            Snackbar.make(getView(), "Что-то пошло не так. Попробуйте снова.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                break;
            case R.id.registr:
                //переход на регистрационную форму
                NavController navController2 = NavHostFragment.findNavController(this);
                navController2.navigate(R.id.nav_signUp);
                break;
        }
    }

    private void getUserAchievements(String userEmail) {
        Call<ServerResponse<AchievementsData>> call = api.getAchievements(AppСonstants.X_API_KEY,
                "user_email", userEmail);
        call.enqueue(new Callback<ServerResponse<AchievementsData>>() {
            @Override
            public void onResponse(Call<ServerResponse<AchievementsData>> call, Response<ServerResponse<AchievementsData>> response) {
                if (response.code() == 200){
                    AchievementsData data = response.body().getData();
                    AchievementsToUser achievements = data.getAchievementsToUsers().get(0);
                    preferences.edit().putInt(AppСonstants.USER_COINS, Integer.parseInt(achievements.getCoins())).apply();
                    preferences.edit().putInt(AppСonstants.ACHIEVEMENTS_ID, Integer.parseInt(achievements.getId())).apply();
                }
                else Log.e("GET ACHIEVEMENTS", String.valueOf(response.raw()));
            }

            @Override
            public void onFailure(Call<ServerResponse<AchievementsData>> call, Throwable t) {
                Log.e("GET ACHIEVEMENTS", String.valueOf(t));
            }
        });
    }
}
