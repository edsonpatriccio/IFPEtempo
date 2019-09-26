package br.edu.ifpe.ifpetempo.models;

import android.os.Parcel;
import android.os.Parcelable;

public class WeatherData implements Parcelable {

    private String userName;
    private String location;
    private float latitude, longitude;
    private String weather;
    private String date;

    public WeatherData(String userName, String location, float latitude, float longitude, String weather, String date) {
        this.userName = userName;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.weather = weather;
        this.date = date;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "userName='" + userName + '\'' +
                ", location='" + location + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", weather='" + weather + '\'' +
                ", date=" + date +
                '}';
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userName);
        dest.writeString(this.location);
        dest.writeFloat(this.latitude);
        dest.writeFloat(this.longitude);
        dest.writeString(this.weather);
        dest.writeString(this.date);
    }

    protected WeatherData(Parcel in) {
        this.userName = in.readString();
        this.location = in.readString();
        this.latitude = in.readFloat();
        this.longitude = in.readFloat();
        this.weather = in.readString();
        this.date = in.readString();
    }

    public static final Creator<WeatherData> CREATOR = new Creator<WeatherData>() {
        public WeatherData createFromParcel(Parcel source) {
            return new WeatherData(source);
        }

        public WeatherData[] newArray(int size) {
            return new WeatherData[size];
        }
    };
}
