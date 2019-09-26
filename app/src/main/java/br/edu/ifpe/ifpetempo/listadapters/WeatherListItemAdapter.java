package br.edu.ifpe.ifpetempo.listadapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import br.edu.ifpe.ifpetempo.R;
import br.edu.ifpe.ifpetempo.models.WeatherData;
import br.edu.ifpe.ifpetempo.utilities.WeatherUtilities;

public class WeatherListItemAdapter extends BaseAdapter {

    private final Activity mActivity;
    private Vector<WeatherData> mOriginalWeatherDataList;

    public WeatherListItemAdapter(Activity activity, Vector<WeatherData> dataList) {
        super();
        this.mActivity = activity;
        this.mOriginalWeatherDataList = dataList;
    }

    //região BaseAdapter.
    @Override
    public int getCount() {
        return mOriginalWeatherDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mOriginalWeatherDataList.elementAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_weatherlist_item, null, true);

        TextView cityTextView = (TextView) view.findViewById(R.id.weatherListItemCityText);
        TextView dateTextView = (TextView) view.findViewById(R.id.weatherListItemDateText);
        TextView latLngTextView = (TextView) view.findViewById(R.id.weatherListItemLatLng);
        TextView weatherTextView = (TextView) view.findViewById(R.id.weatherListItemWeatherText);
        ImageView weatherImageView = (ImageView) view.findViewById(R.id.weatherListItemWeatherIcon);

        WeatherData rowData = mOriginalWeatherDataList.elementAt(position);
        StringBuilder latLng = new StringBuilder();
        latLng.append(rowData.getLatitude()).append(", ").append(rowData.getLongitude());

        cityTextView.setText(rowData.getLocation());
        dateTextView.setText(rowData.getDate());
        latLngTextView.setText(latLng.toString());
        weatherTextView.setText(rowData.getWeather().toLowerCase());

        //Converter se string para enum.
        WeatherUtilities.WeatherType weather = WeatherUtilities.WeatherType.valueOf(rowData.getWeather());
        switch (weather) {
            case CLEAR:
                weatherImageView.setImageResource(R.drawable.clear);
                break;
            case THUNDERSTORM:
                weatherImageView.setImageResource(R.drawable.thunderstorm);
                break;
            case DRIZZLE:
                weatherImageView.setImageResource(R.drawable.drizzle);
                break;
            case FOGGY:
                weatherImageView.setImageResource(R.drawable.foggy);
                break;
            case CLOUDY:
                weatherImageView.setImageResource(R.drawable.cloudy);
                break;
            case SNOWY:
                weatherImageView.setImageResource(R.drawable.snowy);
                break;
            case RAINY:
                weatherImageView.setImageResource(R.drawable.rainy);
                break;
            default:
                weatherImageView.setImageResource(R.drawable.clear);
                break;
        }

        Log.d("WeatherListItemAdapter", "getView " + position + " (" + rowData + ") with weather " + weather);

        return view;
    }

    public void updateItems(Vector<WeatherData> newItems) {
        mOriginalWeatherDataList = newItems;
        notifyDataSetChanged();
    }

    public void sort(final String field) {
        Collections.sort(mOriginalWeatherDataList, new Comparator<WeatherData>() {
            @Override
            public int compare(WeatherData lhs, WeatherData rhs) {
                if (field.equals("location")) {
                    return lhs.getLocation().compareTo(rhs.getLocation());
                } else if (field.equals("date")) {
                    return lhs.getDate().compareTo(rhs.getDate());
                }
                return 0;
            }
        });
        notifyDataSetChanged();
    }
}

