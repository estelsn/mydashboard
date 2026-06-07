package com.aifomo.dashboard.service;

public class CollectionRunNotFoundException extends RuntimeException {

    public CollectionRunNotFoundException(Long id) {
        super("CollectionRun %d not found".formatted(id));
    }
}
