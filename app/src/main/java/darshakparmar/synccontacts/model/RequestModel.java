package darshakparmar.synccontacts.model;

import java.util.ArrayList;

public class RequestModel {
    private String user_name;
    private ArrayList<String> user_mobile;
    private String user_email;

    public RequestModel() {
    }

    public RequestModel(String user_name, ArrayList<String> user_mobile, String user_email) {
        this.user_name = user_name;
        this.user_mobile = user_mobile;
        this.user_email = user_email;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public ArrayList<String> getUser_mobile() {
        return user_mobile;
    }

    public void setUser_mobile(ArrayList<String> user_mobile) {
        this.user_mobile = user_mobile;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }
}
