package com.aifomo.dashboard.service;

public class SourceNotFoundException extends RuntimeException {

    public SourceNotFoundException(Long id) {
        super("Source not found: " + id);
    }
}
