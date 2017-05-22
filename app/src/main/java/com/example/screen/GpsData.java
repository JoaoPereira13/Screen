package com.example.screen;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class GpsData {

    private String id;
    private String lon;

    private String lat;
    private String speed;
    private String course;

    GpsData(){
        id = "-1";
        lat = "-1";
        lon = "-1";
        speed = "-1";
        course = "-1";
    }

    GpsData(String ID, String Lon, String Lat, String Speed, String Course){
        id = ID;
        lon = Lon;
        lat = Lat;
        speed = Speed;
        course = Course;
    }

    public void setGpsData(String ID, String Lon, String Lat, String Speed, String Course){
        id = ID;
        lon = Lon;
        lat = Lat;
        speed = Speed;
        course = Course;
    }

    public String getId(){
        return id;
    }

    public String getLat(){
        return lat;
    }
    public Double getLatDouble(){
        return Double.parseDouble(lat);
    }

    public String getLon(){
        return lon;
    }
    public Double getLonDouble(){
        return Double.parseDouble(lon);
    }

    public String getSpeed(){
        return speed;
    }
    public Double getSpeedDouble(){
        return Double.parseDouble(speed);
    }

    public String getCourse(){
        return course;
    }
    public Double getCourseDouble(){
        return Double.parseDouble(course);
    }

    public GpsData getGpsData(){
        return this;
    }

    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    @Override
    public boolean equals(final Object obj)
    {
        if ( obj == null || obj == this || !(obj instanceof GpsData) )
            return false;

        GpsData gpsData = (GpsData) obj;

        if (!gpsData.id.equals(this.id))            return false;
        if (!gpsData.lon.equals(this.lon))          return false;
        if (!gpsData.lat.equals(this.lat))          return false;
        if (!gpsData.speed.equals(this.speed))      return false;
        if (!gpsData.course.equals(this.course))    return false;

        return true;
    }
}