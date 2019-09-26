package br.edu.ifpe.ifpetempo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Vector;

import br.edu.ifpe.ifpetempo.models.WeatherData;

public class WeatherSQLiteOpenHelper extends SQLiteOpenHelper {

    //região variáveis publicas.
    public static final String FIELD_ROW_ID = "_id";
    public static final String FIELD_ROW_USER = "user";
    public static final String FIELD_ROW_LOCATION = "location";
    public static final String FIELD_ROW_LAT = "lat";
    public static final String FIELD_ROW_LON = "lon";
    public static final String FIELD_ROW_WEATHER = "weather";
    public static final String FIELD_ROW_DATE = "date";

    //região variáveis privadas.
    private static final String DB_NAME = "dataweather";
    private static final int VERSION = 1;
    private static final String WEATHER_TABLE = "weather";
    private static final String WEATHER_TABLE_SQL_CREATE =
            "CREATE TABLE " + WEATHER_TABLE + " ( " +
                    FIELD_ROW_ID + " integer primary key autoincrement, " +
                    FIELD_ROW_USER + " text, " +
                    FIELD_ROW_LOCATION + " text, " +
                    FIELD_ROW_LAT + " double, " +
                    FIELD_ROW_LON + " double, " +
                    FIELD_ROW_WEATHER + " text, " +
                    FIELD_ROW_DATE + " text" +
                    " ) ";

    private SQLiteDatabase mDB;

    //Constructor da classe
    public WeatherSQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        mDB = getWritableDatabase();
    }

    //regiao SQLiteOpenHelper
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WEATHER_TABLE_SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //Metodos da regiao no BD

    /**
     * Insterts row in the weather table
     *
     * @param contentValues
     * @return row id
     */
    public long insert(ContentValues contentValues) {
        long rowID = mDB.insert(WEATHER_TABLE, null, contentValues);
        Log.d("WEATHER DB", "INSERT -> " + contentValues.toString());
        return rowID;
    }

    /**
     * Deletes all entries in the weather table
     *
     * @return deleted rows count
     */
    public int del() {
        int cnt = mDB.delete(WEATHER_TABLE, null, null);
        return cnt;
    }

    public int deleteEntry(WeatherData wd) {
        String whereClause = FIELD_ROW_USER + " = ? and " + FIELD_ROW_LOCATION + " = ? and " + FIELD_ROW_DATE + " = ?";
        String[] whereArgs = new String[]{wd.getUserName(), wd.getLocation(), wd.getDate()};

        return mDB.delete(WEATHER_TABLE, whereClause, whereArgs);
    }

    /**
     * Obtém todas as linhas da tabela clima.
     *
     * @return
     */
    public Cursor getAllWeatherData() {
        String[] projection = {FIELD_ROW_ID, FIELD_ROW_USER, FIELD_ROW_LOCATION, FIELD_ROW_LAT, FIELD_ROW_LON, FIELD_ROW_WEATHER, FIELD_ROW_DATE};
        return mDB.query(WEATHER_TABLE, projection, null, null, null, null, null);
    }

    public Cursor getUserWeatherData(String user) {

        String[] projection = {FIELD_ROW_ID, FIELD_ROW_LOCATION, FIELD_ROW_LAT, FIELD_ROW_LON, FIELD_ROW_WEATHER, FIELD_ROW_DATE};
        String order = FIELD_ROW_ID + " DESC";
        Cursor c = mDB.query(WEATHER_TABLE, projection, FIELD_ROW_USER + "=?", new String[]{user}, null, null, order);

        return c;
    }

    public Vector<WeatherData> getUserWeatherDataVector(String user) {

        Log.d("WeatherSQLiteOpenHelper", "getUserWeatherDataVector 1");
        Cursor cursor = getUserWeatherData(user);

        Vector<WeatherData> result = new Vector<>();

        String location;
        float latitude = 0;
        float longitude = 0;
        String weather;
        String date;

        Log.d("WeatherSQLiteOpenHelper", "getUserWeatherDataVector 2");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            location = cursor.getString(cursor.getColumnIndex(WeatherSQLiteOpenHelper.FIELD_ROW_LOCATION));
            latitude = cursor.getFloat(cursor.getColumnIndex(WeatherSQLiteOpenHelper.FIELD_ROW_LAT));
            longitude = cursor.getFloat(cursor.getColumnIndex(WeatherSQLiteOpenHelper.FIELD_ROW_LON));
            weather = cursor.getString(cursor.getColumnIndex(WeatherSQLiteOpenHelper.FIELD_ROW_WEATHER));
            date = cursor.getString(cursor.getColumnIndex(WeatherSQLiteOpenHelper.FIELD_ROW_DATE));

            WeatherData newData = new WeatherData(user, location, latitude, longitude, weather, date);
            result.add(newData);

            cursor.moveToNext();

            Log.d("WeatherSQLiteOpenHelper", "getUserWeatherDataVector Iterating");
        }

        cursor.close();

        return result;
    }
}
