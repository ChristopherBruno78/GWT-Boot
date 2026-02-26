package ${package}.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;


/**
 * Ensures the embedded server respects GWT .cache and .nocahe files
 *
 */

@Configuration
public class CacheConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Cache *.cache.js files for 1 year
        // These files are unique per build (MD5 hashed), so they are safe to cache forever.
        registry.addResourceHandler("/**/*.cache.js", "/**/*.cache.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());

        // 2. Disable caching for *.nocache.js
        // The bootstrapper must always be fresh to point to the latest .cache.js files.
        registry.addResourceHandler("/**/*.nocache.js")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
    }
}
