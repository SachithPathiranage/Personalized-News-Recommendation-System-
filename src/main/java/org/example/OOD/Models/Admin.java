package org.example.OOD.Models;

import org.example.OOD.Database_Handler.DatabaseHandler;

public class Admin {
    private String name;
    private String email;
    private String password;
    private static Admin currentAdmin;

    // Constructor
    public Admin(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public static Admin setCurrentAdmin(Admin admin) {
        currentAdmin = admin;
        return currentAdmin;
    }

    // Static handleLogin method
    public static Admin handleLogin(String email, String password) {
        String query = "SELECT Name FROM admin WHERE Email = ? AND Password = ?";
        Admin admin = DatabaseHandler.handleLogin_Admin(email, password, query);
        if (admin != null) {
            return Admin.setCurrentAdmin(admin);
        }
        return null;
    }
}