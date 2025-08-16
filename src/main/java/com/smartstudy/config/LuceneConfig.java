package com.smartstudy.config;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for Apache Lucene search engine
 */
@Configuration
public class LuceneConfig {
    
    @Value("${lucene.index.directory}")
    private String indexDirectoryPath;
    
    /**
     * Create Lucene analyzer bean
     */
    @Bean
    public StandardAnalyzer standardAnalyzer() {
        return new StandardAnalyzer();
    }
    
    /**
     * Create Lucene index directory bean
     */
    @Bean
    public FSDirectory indexDirectory() throws IOException {
        Path indexPath = Paths.get(indexDirectoryPath);
        // Create directory if it doesn't exist
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }
        System.out.println("🔍 Using Lucene index directory: " + indexPath.toAbsolutePath());
        return FSDirectory.open(indexPath);
    }
    
    /**
     * Create Lucene IndexWriter bean
     */
    @Bean
    public IndexWriter indexWriter(FSDirectory directory, StandardAnalyzer analyzer) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        
        return new IndexWriter(directory, config);
    }
}
