package br.edu.ifpe.ifpetempo.utilities;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import br.edu.ifpe.ifpetempo.R;

public class HTTPWeatherFetch {

    //private static final String OPEN_WEATHER_MAP_URL = "http://api.openweathermap.org/data/2.5/weather?q=Recife&units=metric&appid=767a351f1feff0d437b16c2284a43247";
    private static final String OPEN_WEATHER_MAP_URL = "http://api.openweathermap.org/data/2.5/weather?q=Recife&units=metric&appid=767a351f1feff0d437b16c2284a43247";
   // private static final String OPEN_WEATHER_MAP_URL = "http://api.openweathermap.org/data/2.5/forecast?id=524901&APPID=";
   // private static final String OPEN_WEATHER_MAP_URL = "http://api.openweathermap.org/data/2.5/weather?id=524901&APPID=";
    
    public static JSONObject getJSON(Context context, String city) {

        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_URL, city));
            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();

           connection.addRequestProperty("x-api-key",
                   context.getString(R.string.OPEN_WEATHER_MAP_API_KEY));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // Este valor será 404 se o pedido não for bem sucedido
            if(data.getInt("cod") != 200){
                return null;
            }

            return data;
        }catch(Exception e){
            return null;
        }
    }
}
