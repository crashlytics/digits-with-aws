package digits.digitsapp;

/**
 * Created by gjones on 7/6/15.
 */

public class PhoneInfo {

    private String phoneNumber;
    private long id;
    private String accessToken;
    private String accessTokenSecret;
    private String userName;
    private long userId;

    public PhoneInfo() {
    }

    public PhoneInfo(String phoneNumber, long id, String accessToken, String accessTokenSecret, String userName, long userId) {
        this.phoneNumber = phoneNumber;
        this.id = id;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
        this.userName = userName;
        this.userId = userId;
    }

    // phoneNumber
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // accessToken
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    // accessTokenSecret
    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

}
