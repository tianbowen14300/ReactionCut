package com.tbw.cut.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.tbw.cut.utils")
public class FFmpegConfig {
    // FFmpeg configuration class
    // This ensures that FFmpegUtil is properly scanned and configured
}