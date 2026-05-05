package com.aifomo.dashboard.service;

public class InfoItemNotFoundException extends RuntimeException {

    public InfoItemNotFoundException(Long id) {
        super("InfoItem not found: " + id);
    }
}
