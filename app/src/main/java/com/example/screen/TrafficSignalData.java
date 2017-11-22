package com.example.screen;


import com.mapbox.mapboxsdk.geometry.LatLng;

public class TrafficSignalData {

    private String id;
    private String lon;
    private String lat;
    private String course;
    private String type;
    private int ttl;

    TrafficSignalData(){
        id = "-1";
        lat = "-1";
        lon = "-1";
        course = "-1";
        type = "-1";
        ttl = 0;
    }

    TrafficSignalData(String ID, String Lon, String Lat, String Course, String Type){
        id = ID;
        lon = Lon;
        lat = Lat;
        course = Course;
        type = Type;
    }

    public void setTrafficSignalData(String ID, String Lon, String Lat, String Course, String Type){
        id = ID;
        lon = Lon;
        lat = Lat;
        course = Course;
        type = Type;
    }

    public String getId(){
        return id;
    }
    public String getType(){
        return type;
    }

    public int getTTL(){
        return ttl;
    }
    public void setTTL(int t){
        ttl = t;
    }

    public void increaseTTl(int t)
    {
        ttl = ttl + t;
    }
    public void decreaseTTL(int t)
    {
        if(ttl !=0)
        ttl = ttl - t;
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

    public String getCourse(){
        return course;
    }
    public Double getCourseDouble(){
        return Double.parseDouble(course);
    }

    public TrafficSignalData getTrafficSignalData(){
        return this;
    }
    public String getAllData(){
        return id+" "+lat+" "+lon+" "+course;
    }

    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    @Override
    public boolean equals(final Object obj)
    {
        if ( obj == null || obj == this || !(obj instanceof TrafficSignalData) )
            return false;

        TrafficSignalData trafficSignalData = (TrafficSignalData) obj;


        if (!trafficSignalData.id.equals(this.id))            return false;
        if (!trafficSignalData.lon.equals(this.lon))          return false;
        if (!trafficSignalData.lat.equals(this.lat))          return false;
        if (!trafficSignalData.course.equals(this.course))    return false;

        return true;
    }

}
