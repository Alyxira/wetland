package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PythonService {

    @Value("${app.python.executable:python}")
    private String pythonExecutable;

    @Value("${app.python.script.path:}")
    private String scriptBasePath;

    @Value("${app.python.user.algorithm.path:}")
    private String userAlgorithmPath;

    @Value("${app.python.command-timeout-ms:30000}")
    private long pythonCommandTimeoutMs;

    @Value("${app.python.service.enabled:false}")
    private boolean pythonServiceEnabled;

    @Value("${app.python.service.base-url:http://127.0.0.1:8000}")
    private String pythonServiceBaseUrl;

    @Value("${app.python.service.connect-timeout-ms:5000}")
    private long pythonServiceConnectTimeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String executeWaterQualityInversion(String requestJson) throws IOException {
        return executeUserWaterQualityInversion(requestJson);
    }

    public String executeUserWaterQualityInversion(String requestJson) throws IOException {
        if (pythonServiceEnabled) {
            return executeRemoteInversion(requestJson);
        }
        String scriptPath = getScriptPath("user_water_quality_inversion.py");
        Map<String, String> env = new HashMap<>(System.getenv());
        if (userAlgorithmPath != null && !userAlgorithmPath.isBlank()) {
            env.put("USER_ALGORITHM_PATH", userAlgorithmPath);
        }
        return executePythonScript(scriptPath, env, requestJson);
    }

    public String getTiffMetadata(String tiffPath) throws IOException {
        String scriptPath = getScriptPath("tiff_metadata.py");
        return executePythonScript(scriptPath, null, tiffPath);
    }

    public byte[] renderTilePng(String imagePath, int z, int x, int y, int band, String colorRamp) throws IOException {
        String scriptPath = getScriptPath("tiff_tile.py");
        String result = executePythonScript(
            scriptPath,
            null,
            imagePath,
            String.valueOf(z),
            String.valueOf(x),
            String.valueOf(y),
            String.valueOf(band),
            colorRamp
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(result, Map.class);
        Object error = payload.get("error");
        if (error != null) {
            throw new IOException(String.valueOf(error));
        }
        Object pngBase64 = payload.get("pngBase64");
        if (pngBase64 == null) {
            throw new IOException("tile render failed: missing pngBase64");
        }
        return Base64.getDecoder().decode(String.valueOf(pngBase64));
    }

    public String convertMaskTiffToGeoJson(String tiffPath) throws IOException {
        String scriptPath = getScriptPath("mask_tiff_to_geojson.py");
        String result = executePythonScript(scriptPath, null, tiffPath);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(result, Map.class);
        Object error = payload.get("error");
        if (error != null) {
            throw new IOException(String.valueOf(error));
        }
        return result;
    }

    private String executeRemoteInversion(String requestJson) throws IOException {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(pythonServiceConnectTimeoutMs))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(pythonServiceBaseUrl) + "/api/v1/inversion/run"))
                .timeout(Duration.ofMillis(pythonCommandTimeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String detail = extractRemoteErrorDetail(response.body());
                if (detail == null || detail.isBlank()) {
                    throw new IOException("Python service request failed, code=" + response.statusCode() + ", body=" + response.body());
                }
                throw new IOException("Python service request failed, code=" + response.statusCode() + ", detail=" + detail);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Python service request interrupted", e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Python service base URL: " + pythonServiceBaseUrl, e);
        }
    }

    private String executePythonScript(String scriptPath, Map<String, String> env, String... args) throws IOException {
        CommandLine cmdLine = CommandLine.parse(pythonExecutable);
        cmdLine.addArgument(scriptPath, true);
        for (String arg : args) {
            cmdLine.addArgument(arg, true);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(pythonCommandTimeoutMs);
        executor.setWatchdog(watchdog);

        int exitValue;
        if (env == null) {
            exitValue = executor.execute(cmdLine);
        } else {
            exitValue = executor.execute(cmdLine, env);
        }

        String stdout = outputStream.toString(StandardCharsets.UTF_8);
        if (watchdog.killedProcess()) {
            throw new IOException("Python script timeout after " + pythonCommandTimeoutMs + " ms");
        }
        if (exitValue != 0) {
            String stderr = errorStream.toString(StandardCharsets.UTF_8);
            throw new IOException("Python script execution failed, code=" + exitValue + ", error=" + stderr);
        }
        return stdout;
    }

    private String getScriptPath(String scriptName) {
        if (scriptBasePath != null && !scriptBasePath.isBlank()) {
            return scriptBasePath + "/" + scriptName;
        }
        try {
            java.net.URL resource = getClass().getResource("/python/" + scriptName);
            if (resource == null) {
                throw new RuntimeException("Python script not found: " + scriptName);
            }
            return new java.io.File(resource.toURI()).getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve Python script: " + scriptName, e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private String extractRemoteErrorDetail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(body, Map.class);
            Object detail = parsed.get("detail");
            if (detail == null) {
                detail = parsed.get("message");
            }
            if (detail == null) {
                detail = parsed.get("error");
            }
            return detail == null ? "" : String.valueOf(detail);
        } catch (Exception ignored) {
            return body;
        }
    }
}
