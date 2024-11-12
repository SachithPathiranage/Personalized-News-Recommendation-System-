package org.example.ood;

public class User {
    private String id;
    private String unique_code;
    private String name;
    private String email;
    private String password;

    public User(String id, String unique_code, String name, String email, String password) {
        this.id = id;
        this.unique_code = unique_code;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters and setters for each field
    

    public String getUnique_code() {
        return unique_code;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

}
