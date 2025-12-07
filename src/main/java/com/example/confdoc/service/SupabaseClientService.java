package com.example.confdoc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;

@Service
public class SupabaseClientService {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.key}")
    private  String apiKey;

    private final HttpClient http;

    public SupabaseClientService() {
        this.http = HttpClient.newHttpClient();
    }

    // ----------------------
    // Helper : construire la requête
    // ----------------------
    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + path))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey);
    }

    // ----------------------
    // Upload fichier binaire
    // ----------------------
    public String upload(String bucket, String path, byte[] content) throws Exception {
        HttpRequest request = baseRequest("/storage/v1/object/" + bucket + "/" + path)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400)
            throw new RuntimeException("Upload error: " + response.body());

        return response.body();
    }

    // ----------------------
    // Télécharger fichier
    // ----------------------
    public byte[] download(String bucket, String path) throws Exception {
        HttpRequest request = baseRequest("/storage/v1/object/" + bucket + "/" + path)
                .GET()
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() >= 400)
            throw new RuntimeException("Download error: HTTP " + response.statusCode());

        return response.body();
    }

    // ----------------------
    // Supprimer
    // ----------------------
    public void delete(String bucket, String path) throws Exception {
        HttpRequest request = baseRequest("/storage/v1/object/" + bucket + "/" + path)
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400)
            throw new RuntimeException("Delete error: " + resp.body());
    }

    // ----------------------
    // Liste objets
    // ----------------------
    public String list(String bucket, String folder) throws Exception {
        HttpRequest request = baseRequest("/storage/v1/object/list/" + bucket)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"prefix\":\"" + folder + "\"}"))
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400)
            throw new RuntimeException("List error: " + resp.body());

        return resp.body();
    }

    // ----------------------
    // Récupérer l'URL publique
    // ----------------------
    public String publicUrl(String bucket, String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
