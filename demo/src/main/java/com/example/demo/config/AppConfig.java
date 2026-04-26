package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Upload upload = new Upload();
    private Python python = new Python();
    private Results results = new Results();
    
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Python getPython() { return python; }
    public void setPython(Python python) { this.python = python; }
    public Results getResults() { return results; }
    public void setResults(Results results) { this.results = results; }
    
    public static class Upload {
        private String dir = "../uploads/tiff";
        private long maxSize = 500 * 1024 * 1024;
        
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
    }
    
    public static class Python {
        private String executable = "python";
        private String scriptPath = "";
        
        public String getExecutable() { return executable; }
        public void setExecutable(String executable) { this.executable = executable; }
        public String getScriptPath() { return scriptPath; }
        public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    }
    
    public static class Results {
        private String dir = "../uploads/results";
        
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
    }
}
