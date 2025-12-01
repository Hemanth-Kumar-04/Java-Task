    package com.tata.test1.dto;

    /**
    * Represents the JSON body for the initial POST request to generate a webhook.
    */
    public record WebhookRequest(String name, String regNo, String email) {}
