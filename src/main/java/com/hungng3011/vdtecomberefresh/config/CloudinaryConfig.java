package com.hungng3011.vdtecomberefresh.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(
                //System.getenv("CLOUDINARY_URL")
                "cloudinary://285633991747994:WNwV7cYwbo-3kHjef_GPfBuETdM@ddbrhoqrn"
        );
    }
}
