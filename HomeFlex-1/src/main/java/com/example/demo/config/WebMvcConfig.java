package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {
	 @Value("${homeflex.upload-dir}")
	    private String uploadDir;    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Manejar recursos estáticos: CSS, JavaScript, Imágenes
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/media/**")
        .addResourceLocations("file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/"))
        .setCachePeriod(3600)                     // opcional (1 h de caché)
        .resourceChain(true)
        .addResolver(new PathResourceResolver());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Mapeo simple para redireccionar la raíz a la página de inicio
        registry.addViewController("/").setViewName("forward:/index");
    }
}