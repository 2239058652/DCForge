package com.forge.dc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DcForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DcForgeApplication.class, args);

        System.out.println("""
                
                ╔═════════════════════════════════════════════════════════════╗
                ║                                                             ║
                ║   🏥 DC-Forge Started Successfully! 🏥       ║
                ║                                                             ║
                ║   🌐 Server: http://localhost:5273                          ║
                ║   📚 Swagger: http://localhost:5273/swagger-ui/index.html#/ ║
                ║                                                             ║
                ╚═════════════════════════════════════════════════════════════╝
                """);
    }

}
