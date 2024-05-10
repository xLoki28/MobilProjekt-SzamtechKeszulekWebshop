package com.example.mobil_webshop;

public class User {
    private String username;
    private String email;
    private String phoneNumber;
    private String phoneType;
    private String address;
    private String role;

    public User() {
    }

    public User(String username, String email, String phoneNumber, String phoneType, String address, String role) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.phoneType = phoneType;
        this.address = address;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public String getAddress() {
        return address;
    }

    public String getRole() {
        return role;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
