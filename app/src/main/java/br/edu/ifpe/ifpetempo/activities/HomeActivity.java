package br.edu.ifpe.ifpetempo.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import br.edu.ifpe.ifpetempo.R;
import br.edu.ifpe.ifpetempo.database.WeatherSQLiteOpenHelper;
import br.edu.ifpe.ifpetempo.utilities.AlertDialogUtilities;
import br.edu.ifpe.ifpetempo.utilities.DateUtilities;
import br.edu.ifpe.ifpetempo.utilities.HTTPWeatherFetch;
import br.edu.ifpe.ifpetempo.utilities.WeatherUtilities;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static br.edu.ifpe.ifpetempo.R.drawable.clear_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.cloudy_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.drizzle_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.foggy_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.rainy_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.snowy_weather_anim;
import static br.edu.ifpe.ifpetempo.R.drawable.thunderstorm_weather_anim;

//Classe Home do usuario

public class HomeActivity extends AppCompatActivity implements LocationListener {

    @Bind(R.id.loginUsernameEditText)
    protected EditText userNameText;
    @Bind(R.id.loginLetsGoButton)
    protected Button letsGoBtn;
    @Bind(R.id.loginYourLocationTextView)
    protected TextView yourLocationTextView;
    @Bind(R.id.loginLoadingIndicator)
    protected ImageView loadingIndicator;
    @Bind(R.id.loginCurrentWeatherImage)
    protected ImageView currentWeatherImage;

    private LocationManager mLocationManager;
    private String mProvider;

    private SharedPreferences mDefaultSharedPreferences;

    private static Handler mMainThreadHandler;

    private JSONObject mLastJsonWeatherData;
    private Location mLastLocationData;

    private WeatherSQLiteOpenHelper mWeatherSQLiteOpenHelper;

    private boolean mExternalSendIntentReceived = false;

    private static final int ONE_SECOND = 1000;

    private static final int REQUEST_LOCATION_PERMISSION = 11;

    //ciclo de vida Atividade região.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("LoginActivity", "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // injeção Butterknife.
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            mExternalSendIntentReceived = savedInstanceState.getBoolean("externalSendIntentReceived");
            Log.d("onCreate", "savedInstanceState != null -> " + mExternalSendIntentReceived);
        }


        // Obter preferências de preference fragment.
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        String possibleUserName = myPrefs.getString(getString(R.string.share_prefs_user_logged), "");

        // inicialização Thread.
        mMainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };

        // Obter a localização de serviço referencia para a primeira localização.
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // É o usuário já  => Redireciono.
        if (!possibleUserName.equals("")) {

            //Redirecionar para a atividade lista.
            Intent weatherListActivityIntent = new Intent(this, WeatherListActivity.class);
            startActivity(weatherListActivityIntent);

            //Desativar voltar a esta atividade.
            finish();

            return;
        }

        // Começar a carregar animação indicador.
        Animation loadingIndicatorAnimation;
        loadingIndicatorAnimation = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        loadingIndicatorAnimation.setRepeatCount(Animation.INFINITE);
        loadingIndicatorAnimation.setRepeatMode(Animation.INFINITE);
        loadingIndicatorAnimation.setDuration(1000);
        loadingIndicatorAnimation.setInterpolator(new LinearInterpolator());
        loadingIndicator.startAnimation(loadingIndicatorAnimation);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        Log.d("HomeActivity", "onStart");
        super.onStart();

        //Checa se o servico de localizao ta habilitado e se esta com rede
        // Check if any location service is enabled
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            AlertDialogUtilities.showNoLocationSettingsEnabledAlert(this);

        }

        //Permissão garantida.
        Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        criteria.setAltitudeRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        mProvider = mLocationManager.getBestProvider(criteria, false);

        Log.d("HomeActivity", "Provider: " + mProvider);

        //Metodo para registar o previsao na Activity lista que sera chama ao clicar em (GO)
        registerLocationListener();

        Location lastLocation = new Location(mProvider);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Tem permissão.
            lastLocation = mLocationManager.getLastKnownLocation(mProvider);
        } else {
            // Não tem permissão (Android 6 e superior).
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }


        if (lastLocation != null) {

            //Exibe a localizacao atual do usuario na actvity home
            mLastLocationData = lastLocation;

            String yourLocation = getString(R.string.your_location) + ": " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude();
            yourLocationTextView.setText(yourLocation);
            String city = getLocationName(lastLocation);
            yourLocationTextView.append("(" + city + ")");

            // Armazenar a atual cidade nas preferências.
            storeCity(city);
            showWeatherData(city);

            // Pausa animação girador se qualquer local foi encontrado.
            loadingIndicator.clearAnimation();
            loadingIndicator.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    protected void onResume() {
        Log.d("HomeActivity", "onResume");
        super.onResume();
    }

    // -------------------------------------------------------------------> Atividade em execução.

    @Override
    protected void onPause() {
        Log.d("HomeActivity", "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("HomeActivity", "onStop");

        // Deixar de requerer atualizações de conexão.
        unregisterLocationListener();
        mWeatherSQLiteOpenHelper = null;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("HomeActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("HomeActivity", "onSaveInstanceState");

        outState.putBoolean("externalSendIntentReceived", mExternalSendIntentReceived);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("HomeActivity", "onRestoreInstanceState");
    }

    //metodo para locitacao e permissao da localizacao do usuario
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permissão garantida.
                    Criteria criteria = new Criteria();
                    criteria.setCostAllowed(false);
                    criteria.setAltitudeRequired(false);
                    criteria.setAccuracy(Criteria.ACCURACY_LOW);
                    criteria.setPowerRequirement(Criteria.POWER_LOW);
                    mProvider = mLocationManager.getBestProvider(criteria, false);

                    registerLocationListener();

                    Location lastLocation = new Location(mProvider);

                    // É redundante, já que este código é chamado quando o usuário dá permissão, mas estúdio android reclama ...
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        lastLocation = mLocationManager.getLastKnownLocation(mProvider);
                    }

                    if (lastLocation != null) {

                        mLastLocationData = lastLocation;

                        String yourLocation = getString(R.string.your_location) + ": " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude();
                        yourLocationTextView.setText(yourLocation);
                        String city = getLocationName(lastLocation);
                        yourLocationTextView.append("(" + city + ")");

                        // Armazenar a atual cidade nas preferências.
                        storeCity(city);
                        showWeatherData(city);

                        // Pausa animação girador se qualquer local foi encontrado.
                        loadingIndicator.clearAnimation();
                        loadingIndicator.setVisibility(View.INVISIBLE);

                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("HomeActivity", "onActivityResult -> Request:" + requestCode + " - Result:" + resultCode);

    }

    //Menu de região.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar o menu; isto acrescenta itens à barra de ação se ela estiver presente.
        getMenuInflater().inflate(R.menu.menu_home_activity, menu);
        return true;
    }

    //CLASSE REPONSAVEL PELO MENU BAR
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Item de ação Handle bar detalhar previsao. A barra de ação vai Tratar automaticamente os cliques no botão Início
        int id = item.getItemId();

        switch (id) {
            case R.id.action_share_current_weather: //OPCAO DE COMPARTILHAMENTO
                shareCurrentWeather();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareCurrentWeather() {
        if (mLastJsonWeatherData == null) {
            Toast.makeText(this, getString(R.string.no_weather_data_to_share), Toast.LENGTH_SHORT).show();
            return;
        }

        //Tentar obter dados JSON a partir da última vez.
        StringBuilder weatherString = new StringBuilder();
        try {
            JSONObject currentWeather = mLastJsonWeatherData.getJSONArray("weather").getJSONObject(0);
            weatherString.append(currentWeather.getString("description"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (weatherString.length() > 0) {

            weatherString.append(" ").append(getString(R.string.in)).append(" ").append(getStoredCity());

            Intent sendWeatherIntent = new Intent();
            sendWeatherIntent.setAction(Intent.ACTION_SEND);
            sendWeatherIntent.putExtra(Intent.EXTRA_TEXT, weatherString.toString());
            sendWeatherIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendWeatherIntent, getString(R.string.share_with)));
        }
    }

    //Trata o evento do botao detalhar previsao.
    @SuppressWarnings("unused")
    @OnClick(R.id.loginLetsGoButton)
    public void LetsGoBtnClicked(Button target) {

        //Checa a localizacao antes de enviar a consulta.
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            //Chama o evento alerta caso o GPS nao localize
            AlertDialogUtilities.showNoLocationSettingsEnabledAlert(this);
            return;
        }

        //Checa se foi digitado algum nome na busca, se nao, exibe um dialogo
        if (userNameText.length() != 0) { //(userNameText.length() == 0)
            Toast.makeText(this, getString(R.string.cannot_empty_name), Toast.LENGTH_SHORT).show();
        } else {

            //Verifica se ha dados meteorologico para autorizar a busca, se nao tiver, exibe um dialogo.
            if (mLastJsonWeatherData == null) {
                Toast.makeText(this, getString(R.string.sorry_no_weather_data_available), Toast.LENGTH_SHORT).show();
                return;
            }

            String userName = userNameText.getText().toString();

            SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = myPrefs.edit();
            prefEditor.putString(getString(R.string.share_prefs_user_logged), userName); //nao preciso de nome
            prefEditor.apply();

            Toast.makeText(this, getString(R.string.welcome) + " " + userName, Toast.LENGTH_SHORT).show(); //nao preciso exibir nome na proxima activity

            // Insira sua primeira entrada no banco de dados.
            WeatherUtilities.WeatherType weatherType = WeatherUtilities.WeatherType.CLEAR;
            try {
                JSONObject currentWeather = mLastJsonWeatherData.getJSONArray("weather").getJSONObject(0);
                weatherType = WeatherUtilities.getWeatherType(currentWeather.getInt("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (mWeatherSQLiteOpenHelper == null) {

                // Criar ajudante de banco de dados.
                mWeatherSQLiteOpenHelper = new WeatherSQLiteOpenHelper(getApplicationContext());
            }

            ContentValues values = new ContentValues();
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_USER, userName); //nao preciso guardar no banco
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LOCATION, getLocationName(mLastLocationData));
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LAT, mLastLocationData.getLatitude());
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LON, mLastLocationData.getLongitude());
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_WEATHER, weatherType.toString());
            values.put(WeatherSQLiteOpenHelper.FIELD_ROW_DATE, DateUtilities.milisToDate(System.currentTimeMillis()));

            mWeatherSQLiteOpenHelper.insert(values);

            // Mudar de atividade.
            Intent weatherListActivityIntent = new Intent(this, WeatherListActivity.class);
            startActivity(weatherListActivityIntent);

            //Desativar voltar a esta atividade.
            finish();
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.loginCurrentWeatherImage)
    public void weatherAnimImageClicked(ImageView imgv) {

        //Chama a animacao de acordo com os dados do tempo
        AnimationDrawable anim = (AnimationDrawable) imgv.getBackground();
        if (anim.isRunning()) {
            anim.stop();
        } else {
            anim.start();
        }
    }

    //Os metodos singIn e singOut atualiza a lista e insere
    @SuppressWarnings("unused")
    //@OnClick(R.id.signInButton)
    public void signInButtonClicked(Button btn) {
        Log.d("LoginActivity", "signInButtonClicked");

    }

    @SuppressWarnings("unused")
    //@OnClick(R.id.signOutButton)
    public void signOutButtonClicked(Button btn) {
        Log.d("LoginActivity", "signOutButtonClicked");

    }


    //region LocationListener
    //Metodo para exibira a localizacao na home do usuario
    @Override
    public void onLocationChanged(Location location) {

        Log.d("HomeActivity", "onLocationChanged -> " + location.toString());

        // Store last location data
        mLastLocationData = location;

        //Nao preciso mostrar as coordenadas geograficas na home
        String yourLocation = getString(R.string.your_location) + ": ";
        yourLocationTextView.setText(yourLocation);
        String city = getLocationName(location);
        yourLocationTextView.append(city);

        //Passa a e a previsao para a activity list que vai exibir
        storeCity(city);
        showWeatherData(city);

        // Parar a animação se algum for encontrado.
        loadingIndicator.clearAnimation();
        loadingIndicator.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("HomeActivity", "onStatusChanged -> " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("HomeActivity", "onProviderEnabled -> " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("HomeActivity", "onProviderDisabled -> " + provider);
    }

    //Metodo que atualiza a previsao a cada 0 segundo, e com uma distancia de 10 km entre as cidades
    private void registerLocationListener() {
        String updateFrequencyStr = mDefaultSharedPreferences.getString(getString(R.string.share_prefs_update_freq), "0");
        String updateMetersStr = mDefaultSharedPreferences.getString(getString(R.string.share_prefs_update_meters), "10");

        int updateFrequencyInt = Integer.parseInt(updateFrequencyStr);
        int updateMetersInt = Integer.parseInt(updateMetersStr);

        Log.d("HomeActivity", "registerLocationListener -> " + updateFrequencyInt);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(mProvider, updateFrequencyInt * ONE_SECOND, updateMetersInt, this);

            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateFrequencyInt * ONE_SECOND, updateMetersInt, this);
            }

            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateFrequencyInt * ONE_SECOND, updateMetersInt, this);
            }
        }
    }

    private void unregisterLocationListener() {
        Log.d("HomeActivity", "unregisterLocationListener");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(this);
        }
    }

    //região Geocoder.
    private String getLocationName(Location location) {

        String result = "";

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                result = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
    //endregion Geocoder


    //região localização e tempo.
    private void storeCity(String city) {
        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = myPrefs.edit();
        prefEditor.putString(getString(R.string.share_prefs_current_city), city);
        prefEditor.apply();
    }

    private String getStoredCity() {
        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        return myPrefs.getString(getString(R.string.share_prefs_current_city), "");
    }


    //Metodo para exibicao as informacoes da cidade de acordo com a localizacao
    private void showWeatherData(final String city) {
        new Thread() {
            @Override
            public void run() {
                final JSONObject json = HTTPWeatherFetch.getJSON(getApplicationContext(), city);
                if (json == null) {
                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.weather_data_not_found),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showWeatherData(json);
                        }
                    });
                }
            }
        }.start();
    }


    //Mostrar uma GIF de acordo com a previsao advinda da base WEB
    private void showWeatherData(JSONObject json) {
        try {

            mLastJsonWeatherData = json;
            JSONObject currentWeather = json.getJSONArray("weather").getJSONObject(0);
            WeatherUtilities.WeatherType weatherType = WeatherUtilities.getWeatherType(currentWeather.getInt("id"));

            //String description = currentWeather.getString("description");
            String description = "";
            AnimationDrawable spriteAnim = null;

            switch (weatherType) {
                case CLEAR:
                    currentWeatherImage.setBackgroundResource(clear_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_clear);
                    scheduleAlarm();
                    break;
                case CLOUDY:
                    currentWeatherImage.setBackgroundResource(cloudy_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_cloudy);
                    scheduleAlarm();
                    break;
                case DRIZZLE:
                    currentWeatherImage.setBackgroundResource(drizzle_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_drizzle);
                    scheduleAlarm();
                    break;
                case FOGGY:
                    currentWeatherImage.setBackgroundResource(foggy_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_foggy);
                    scheduleAlarm();
                    break;
                case RAINY:
                    currentWeatherImage.setBackgroundResource(rainy_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_rainy);
                    scheduleAlarm();
                    break;
                case SNOWY:
                    currentWeatherImage.setBackgroundResource(snowy_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_snowy);
                    scheduleAlarm();
                    break;
                case THUNDERSTORM:
                    currentWeatherImage.setBackgroundResource(thunderstorm_weather_anim);
                    spriteAnim = (AnimationDrawable) currentWeatherImage.getBackground();
                    description = getString(R.string.weather_thunderstorm);
                    scheduleAlarm();
                    break;
                default:
                    break;
            }

            if (spriteAnim != null) {
                spriteAnim.start();
            }

            currentWeatherImage.setVisibility(View.VISIBLE);

            Toast.makeText(this, description, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Metedo Alarme
    public void scheduleAlarm() {

        // Tempo atual mais 1 segundos (10 * 1000 milissegundos)
        Long time = new GregorianCalendar().getTimeInMillis() + 10 * 10;

        // Cria Intent do alarm, que será recebido pela classe AlarmReceiver
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);

        // Cria um PendingIntent, usado para fazer o broadcast do alarme
        PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(this, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

        //Obtem o gerenciador de alarmes do sistema
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        //Configura um alarme lançará o intent em 10 segundos
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingAlarmIntent);

        Toast.makeText(this, "Alarme agendado.", Toast.LENGTH_LONG).show();

        // Finaliza a atividade
        this.finish();
    }

}
