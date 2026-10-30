package com.yzc.bookmall.model;

public class ReceiverInfo {
    private final String name; private final String phone; private final String address;
    public ReceiverInfo(String name, String phone, String address) {
        this.name=name; this.phone=phone; this.address=address;
    }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}