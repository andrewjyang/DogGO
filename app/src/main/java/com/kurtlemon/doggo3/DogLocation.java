/**
 * DogLocation is a wrapper class that defines the id, latitude, and longitude of a dog
 * CPSC 312-02, Fall 2017
 * Programming Assignment Final Project
 *
 * @author Kurt Lamon, Andrew Yang
 * @version v1.0 12/8/17
 */
package com.kurtlemon.doggo3;

public class DogLocation implements Comparable<DogLocation>{
    // Location information
    private double latitude;
    private double longitude;

    // The id that ties the location to the correct user.
    private String id;

    /** Default Value Constructor -- not used.
     *
     */
    public DogLocation() {
        latitude = 0;
        longitude = 0;
        id = "";
    }

    /** Explicit Value Constructor.
     *
     * @param latitude
     * @param longitude
     * @param id
     */
    public DogLocation(double latitude, double longitude, String id) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = id;
    }

    /** Returns the latitude value.
     *
     * @return
     */
    public double getLatitude() {
        return latitude;
    }

    /** Sets the latitude value.
     *
     * @param latitude
     */
    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    /** Gets the longitude value.
     *
     * @return
     */
    public double getLongitude() {
        return longitude;
    }

    /** Sets the longitude value.
     *
     * @param longitude
     */
    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    /** Returns the ID value.
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /** Sets the ID value.
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /** Implementation for the comparable interface.
     *
     *  Returns 0 if two locations have the same ID. Else returns 1.
     *
     * @param other
     * @return
     */
    public int compareTo(DogLocation other){
        if(this.id.equals(other.getId())){
            return 0;
        }
        return 1;
    }
}
