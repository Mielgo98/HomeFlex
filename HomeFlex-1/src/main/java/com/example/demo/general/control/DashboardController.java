package com.example.demo.general.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @GetMapping("/dashboard")
    public String dashboard() {
        System.out.println("Accediendo al dashboard");
        return "dashboard";
    }
}