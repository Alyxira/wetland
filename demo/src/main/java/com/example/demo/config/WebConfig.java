package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:${user.dir}/../uploads/tiff}")
    private String uploadDir;

    @Value("${app.results.dir:${user.dir}/../uploads/results}")
    private String resultsDir;

    @Value("${app.local.images.dir:${user.dir}/../uploads/local-images}")
    private String localImagesDir;

    @Value("${app.tiles.cache.dir:${user.dir}/../uploads/tiles-cache}")
    private String tilesCacheDir;

    @Value("${app.distribution.maps.dir:${user.dir}/frontend/public/distribution-maps}")
    private String distributionMapsDir;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(resolveUploadLocations());
        registry.addResourceHandler("/results/**")
            .addResourceLocations(toResourceLocation(resultsDir));
        registry.addResourceHandler("/distribution-maps/**")
            .addResourceLocations(toResourceLocation(distributionMapsDir));
    }

    private String[] resolveUploadLocations() {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Paths.get("src", "uploads").toAbsolutePath().normalize());
        candidates.add(Paths.get("demo", "src", "uploads").toAbsolutePath().normalize());
        candidates.add(Paths.get("..", "uploads").toAbsolutePath().normalize());

        addParentDirectory(candidates, uploadDir);
        addParentDirectory(candidates, localImagesDir);
        addParentDirectory(candidates, resultsDir);
        addParentDirectory(candidates, tilesCacheDir);

        List<String> locations = new ArrayList<>();
        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate)) {
                locations.add(candidate.toUri().toString());
            }
        }

        if (locations.isEmpty()) {
            locations.add(Paths.get("src", "uploads").toAbsolutePath().normalize().toUri().toString());
        }

        return locations.toArray(String[]::new);
    }

    private void addParentDirectory(Set<Path> candidates, String configuredPath) {
        Path absolutePath = Paths.get(configuredPath);
        if (!absolutePath.isAbsolute()) {
            absolutePath = Paths.get(System.getProperty("user.dir")).resolve(absolutePath);
        }
        Path parent = absolutePath.normalize().toAbsolutePath().getParent();
        if (parent != null) {
            candidates.add(parent);
        }
    }

    private String toResourceLocation(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize().toAbsolutePath().toUri().toString();
    }
}
