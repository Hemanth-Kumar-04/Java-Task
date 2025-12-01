package com.tata.test1.service;

import com.tata.test1.dto.SolutionRequest;
import com.tata.test1.dto.WebhookRequest;
import com.tata.test1.dto.WebhookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service class that contains the core logic for the hiring challenge.
 * Implements CommandLineRunner to execute the logic on application startup.
 */
@Service
public class ChallengeService implements CommandLineRunner {

    private final RestTemplate restTemplate;

    @Value("${user.name}")
    private String name;

    @Value("${user.regNo}")
    private String regNo;

    @Value("${user.email}")
    private String email;

    @Value("${api.url.generate-webhook}")
    private String generateWebhookUrl;

    public ChallengeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting HealthRx Hiring Challenge process...");
        try {
            WebhookResponse webhookResponse = generateWebhook();
            System.out.println("Successfully generated webhook: " + webhookResponse.webhookUrl());

            String sqlQuery = getSqlQuery();
            System.out.println("Generated SQL Query:\n" + sqlQuery);


            submitSolution(webhookResponse.webhookUrl(), webhookResponse.accessToken(), sqlQuery);
            System.out.println("Successfully submitted the solution!");

        } catch (RestClientException e) {
            System.err.println("An error occurred during the API communication: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    private WebhookResponse generateWebhook() {
        WebhookRequest requestBody = new WebhookRequest(name, regNo, email);
        HttpEntity<WebhookRequest> requestEntity = new HttpEntity<>(requestBody);

        System.out.println("Sending request to generate webhook at: " + generateWebhookUrl);
        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                generateWebhookUrl,
                requestEntity,
                WebhookResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to generate webhook. Status: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private void submitSolution(String webhookUrl, String accessToken, String sqlQuery) {
        
        SolutionRequest requestBody = new SolutionRequest(sqlQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        HttpEntity<SolutionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        System.out.println("Submitting solution to: " + webhookUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl,
                requestEntity,
                String.class
        );

        System.out.println("Submission response status: " + response.getStatusCode());
        System.out.println("Submission response body: " + response.getBody());
    }

        private String getSqlQuery() {

        return """
                WITH FILTERED_PAYMENTS AS (
                    SELECT 
                        p.EMP_ID,
                        p.AMOUNT,
                        p.PAYMENT_TIME
                    FROM PAYMENTS p
                    WHERE EXTRACT(DAY FROM p.PAYMENT_TIME) != 1
                ),
                TOTAL_SALARY AS (
                    SELECT
                        e.EMP_ID,
                        e.FIRST_NAME,
                        e.LAST_NAME,
                        e.DOB,
                        e.DEPARTMENT,
                        SUM(fp.AMOUNT) AS TOTAL_SALARY
                    FROM EMPLOYEE e
                    JOIN FILTERED_PAYMENTS fp ON e.EMP_ID = fp.EMP_ID
                    GROUP BY e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, e.DOB, e.DEPARTMENT
                ),
                MAX_SALARY_PER_DEPT AS (
                    SELECT
                        DEPARTMENT,
                        MAX(TOTAL_SALARY) AS MAX_SALARY
                    FROM TOTAL_SALARY
                    GROUP BY DEPARTMENT
                )
                SELECT
                    d.DEPARTMENT_NAME,
                    ts.TOTAL_SALARY AS SALARY,
                    CONCAT(ts.FIRST_NAME, ' ', ts.LAST_NAME) AS EMPLOYEE_NAME,
                    FLOOR(DATEDIFF(CURDATE(), ts.DOB) / 365) AS AGE
                FROM TOTAL_SALARY ts
                JOIN MAX_SALARY_PER_DEPT m
                    ON ts.DEPARTMENT = m.DEPARTMENT
                AND ts.TOTAL_SALARY = m.MAX_SALARY
                JOIN DEPARTMENT d
                    ON ts.DEPARTMENT = d.DEPARTMENT_ID
                ORDER BY d.DEPARTMENT_NAME;
            """;
    }

}