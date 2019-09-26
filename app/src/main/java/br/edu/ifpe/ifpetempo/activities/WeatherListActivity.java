package br.edu.ifpe.ifpetempo.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import br.edu.ifpe.ifpetempo.R;
import br.edu.ifpe.ifpetempo.database.WeatherSQLiteOpenHelper;
import br.edu.ifpe.ifpetempo.listadapters.WeatherListItemAdapter;
import br.edu.ifpe.ifpetempo.models.WeatherData;
import br.edu.ifpe.ifpetempo.utilities.AlertDialogUtilities;
import br.edu.ifpe.ifpetempo.utilities.DateUtilities;
import br.edu.ifpe.ifpetempo.utilities.HTTPWeatherFetch;
import br.edu.ifpe.ifpetempo.utilities.WeatherUtilities;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;

//Classe que exibe a lista do tempo apos clicar no botao (GO) na tela do usuario

public class WeatherListActivity extends AppCompatActivity implements LocationListener {

    @Bind(R.id.weatherList)
    protected ListView weatherList;

    private WeatherSQLiteOpenHelper mWeatherDB;

    private LocationManager mLocationManager;
    private String mProvider;

    private static Handler mMainThreadHandler;

    private JSONObject mLastJsonWeatherData;
    private Location mLastLocationData;

    private SharedPreferences mDefaultSharedPreferences;

    private WeatherListItemAdapter mWeatherListItemAdapter;
    private Vector<WeatherData> mWeatherDataVector;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private static final int ONE_SECOND = 1000;

    //ciclo de vida Atividade região.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("WeatherListActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weatherlist);

        // injeção Butterknife.
        ButterKnife.bind(this);

        // Obter preferências de preference fragment.
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);

        // inicialização Thread.
        mMainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onStart() {
        Log.d("WeatherListActivity", "onStart");
        super.onStart();

        Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        criteria.setAltitudeRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mProvider = mLocationManager.getBestProvider(criteria, false);

        registerLocationListener();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLastLocationData = mLocationManager.getLastKnownLocation(mProvider);
        }

        // Verifique se qualquer serviço de localização está activado.
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            AlertDialogUtilities.showNoLocationSettingsEnabledAlert(this);
        }
    }

    @Override
    protected void onResume() {
        Log.d("WeatherListActivity", "onResume");
        super.onResume();

        if (mWeatherDB == null) {
            mWeatherDB = new WeatherSQLiteOpenHelper(this);
        }

        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        String currentUser = myPrefs.getString(getString(R.string.share_prefs_user_logged), "");
        mWeatherDataVector = mWeatherDB.getUserWeatherDataVector(currentUser);

        // Criar o adaptador para a lista e atualizá-lo.
        mWeatherListItemAdapter = new WeatherListItemAdapter(this, mWeatherDataVector);
        weatherList.setAdapter(mWeatherListItemAdapter);

        Log.d("WeatherListActivity", "onResume -> showing data for this user -> " + currentUser + ", entries " + mWeatherDataVector.size());

        // Inserir no banco de dados a cada X minutos.
        String insertFrequencyMinutesStr = mDefaultSharedPreferences.getString(getString(R.string.share_prefs_insert_freq), "60");
        int insertFrequencyMinutesInt = Integer.parseInt(insertFrequencyMinutesStr);
        insertFrequencyMinutesInt *= 60;    // Translate to minutes

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("WeatherListActivity", "onResume -> Timer schedule executing");
                if (mLastJsonWeatherData != null) {
                    mMainThreadHandler.post(new Runnable() {
                        //Funcao Inicial RUN, apos iniciar ela exibe e insere a previsao na listView ao inicar o sistema ***
                        @Override
                        public void run() {
                            insertIntoDB();
                        }
                    });
                }
            }
        };
        Log.d("WeatherListActivity", "onResume -> scheculing timer every " + (insertFrequencyMinutesInt / 60) + " minutes");
        mTimer.scheduleAtFixedRate(mTimerTask, insertFrequencyMinutesInt * 1000, insertFrequencyMinutesInt * 1000); // Delay X minutes since activity onResume
    }

    // -------------------------------------------------------------------> Atividade em execução.

    @Override
    protected void onPause() {
        Log.d("WeatherListActivity", "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("WeatherListActivity", "onStop");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("WeatherListActivity", "onDestroy");
        super.onDestroy();
    }

    //rMenu de legião.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar o menu; isto acrescenta itens à barra de ação se ela estiver presente ..
        getMenuInflater().inflate(R.menu.menu_weather_list_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Item de ação Handle bar detalhar previsao. A barra de ação vai Tratar automaticamente os cliques no botão Início
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                showPreferences();
                return true;
            case R.id.action_change_user:
                changeUser();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    @OnItemClick(R.id.weatherList)
    public void itemListClick(int position) {
        Log.d("WeatherListActivity", "itemListClick at" + position);

        WeatherData weatherRowData = (WeatherData) mWeatherListItemAdapter.getItem(position);
        Log.d("WeatherListActivity", "itemListClick -> " + weatherRowData);

        Intent detailActivity = new Intent(this, WeatherDetailActivity.class);
        detailActivity.putExtra("weather_data", weatherRowData);
        startActivity(detailActivity);
    }

    @SuppressWarnings("unused")
    @OnItemLongClick(R.id.weatherList)
    public boolean itemListLongClick(int position) {
        Log.d("WeatherListActivity", "itemListLongClick at" + position);

        WeatherData weatherRowData = (WeatherData) mWeatherListItemAdapter.getItem(position);
        Log.d("WeatherListActivity", "itemListLongClick -> " + weatherRowData);

        deleteEntryFromDB(weatherRowData);
        return true;
    }
    //eventos endregion.

    //métodos privados da regiao.
    private void changeUser() {

        // Apagar nome de usuário das preferências.
        logOutUser();

        // Pare TimerTask -> Pára inserir no db.
        mTimerTask.cancel();
        mTimer.cancel();
        mTimer.purge();

        // Volte para acessar a atividade.
        Intent loginActivityIntent = new Intent(this, HomeActivity.class);
        startActivity(loginActivityIntent);

    }

    private void logOutUser() {

        // Substituir o valor nome de usuário.
        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = myPrefs.edit();
        prefEditor.putString(getString(R.string.share_prefs_user_logged), "");
        prefEditor.apply();
    }

    private void showPreferences() {
    }

    //região LocationListener.
    @Override
    public void onLocationChanged(Location location) {
        Log.d("WeatherListActivity", "onLocationChanged");

        //Armazenar últimos dados de localização.
        mLastLocationData = location;

        String yourLocation = getString(R.string.your_location) + ": " + location.getLatitude() + ", " + location.getLongitude();
        String city = getLocationName(location);

        // Armazenar a atual cidade nas preferências.
        storeCity(city);
        storeWeatherData(city);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("WeatherListActivity", "onStatusChanged " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("WeatherListActivity", "onProviderEnabled " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("WeatherListActivity", "onProviderDisabled " + provider);
    }

    private void registerLocationListener() {
        String updateFrequencyStr = mDefaultSharedPreferences.getString(getString(R.string.share_prefs_update_freq), "0");
        String updateMetersStr = mDefaultSharedPreferences.getString(getString(R.string.share_prefs_update_meters), "10");

        int updateFrequencyInt = Integer.parseInt(updateFrequencyStr);
        int updateMetersInt = Integer.parseInt(updateMetersStr);

        Log.d("WeatherListActivity", "registerLocationListener " + updateFrequencyInt + " --- " + updateMetersInt);

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
        Log.d("WeatherListActivity", "unregisterLocationListener");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(this);
        }
    }


    //metodos da região localização e tempo.
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

    private void storeWeatherData(final String city) {
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
                            mLastJsonWeatherData = json;
                            Log.d("WeatherListActivity", "showWeatherData weather data found");
                        }
                    });
                }
            }
        }.start();
    }

    //Regiao Geocoder
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


    //regiao DB //Metodo para inserir mais uma previsao no BD
    private void insertIntoDB() {
        Log.d("WeatherListActivity", "insertIntoDB");

        WeatherUtilities.WeatherType weatherType = WeatherUtilities.WeatherType.CLEAR;
        try {
            JSONObject currentWeather = mLastJsonWeatherData.getJSONArray("weather").getJSONObject(0);
            weatherType = WeatherUtilities.getWeatherType(currentWeather.getInt("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mWeatherDB == null) { //**

            // Criar o banco de dados auxiliar.
            mWeatherDB = new WeatherSQLiteOpenHelper(getApplicationContext());
        }

        // Inserir no banco de dados.
        SharedPreferences myPrefs = getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
        String currentUser = myPrefs.getString(getString(R.string.share_prefs_user_logged), "");

        ContentValues values = new ContentValues();
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_USER, currentUser);
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LOCATION, getLocationName(mLastLocationData));
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LOCATION, "Cocentaina");
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LAT, mLastLocationData.getLatitude());
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_LON, mLastLocationData.getLongitude());
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_WEATHER, weatherType.toString());
        values.put(WeatherSQLiteOpenHelper.FIELD_ROW_DATE, DateUtilities.milisToDate(System.currentTimeMillis()));

        Log.d("WeatherListActivity", "insertIntoDB inserting " + values.toString());
        mWeatherDB.insert(values);

        // Notificar o adaptador para alterações de dados.
        mWeatherDataVector = mWeatherDB.getUserWeatherDataVector(currentUser);
        mWeatherListItemAdapter.updateItems(mWeatherDataVector);
    }

    //Metodo para deletar um item da lista
    private void deleteEntryFromDB(final WeatherData wd) {
        Log.d("WeatherListActivity", "deleteEntryFromDB: " + wd);

        Runnable delete = new Runnable() {
            @Override
            public void run() {

                mWeatherDB.deleteEntry(wd);
                mWeatherDataVector = mWeatherDB.getUserWeatherDataVector(wd.getUserName());
                mWeatherListItemAdapter.updateItems(mWeatherDataVector);
            }
        };

        AlertDialogUtilities.showDeleteFromDatabaseAlert(this, delete);
    }
}

