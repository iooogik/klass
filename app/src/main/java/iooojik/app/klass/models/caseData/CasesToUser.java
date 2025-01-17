package iooojik.app.klass.models.caseData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CasesToUser {
    @SerializedName("_id")
    @Expose
    private String id;
    @SerializedName("user_email")
    @Expose
    private String userEmail;
    @SerializedName("latitude")
    @Expose
    private String latitude;
    @SerializedName("longitude")
    @Expose
    private String longitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

}
