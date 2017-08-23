package com.example.screen;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class VehicularData {

    private String id;
    private String lon;

    private String lat;
    private String speed;
    private String course;

    VehicularData(){
        id = "-1";
        lat = "-1";
        lon = "-1";
        speed = "-1";
        course = "-1";
    }

    VehicularData(String ID, String Lon, String Lat, String Speed, String Course){
        id = ID;
        lon = Lon;
        lat = Lat;
        speed = Speed;
        course = Course;
    }

    public void setVehicularData(String ID, String Lon, String Lat, String Speed, String Course){
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

    public VehicularData getVehicularData(){
        return this;
    }
    public String getAllData(){
        return id+" "+lat+" "+lon+" "+speed+" "+course;
    }

    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    @Override
    public boolean equals(final Object obj)
    {
        if ( obj == null || obj == this || !(obj instanceof VehicularData) )
            return false;

        VehicularData vehicularData = (VehicularData) obj;

        if (!vehicularData.id.equals(this.id))            return false;
        if (!vehicularData.lon.equals(this.lon))          return false;
        if (!vehicularData.lat.equals(this.lat))          return false;
        if (!vehicularData.speed.equals(this.speed))      return false;
        if (!vehicularData.course.equals(this.course))    return false;

        return true;
    }
}