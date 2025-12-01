package com.tata.test1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to the JSON response received after generating the webhook.
 */
public record WebhookResponse(@JsonProperty("webhook") String webhookUrl, String accessToken) {}

