package com.smartstudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home controller for basic navigation
 */
@Controller
public class HomeController {
    
    /**
     * Redirect root to test page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/test.html";
    }
    
    /**
     * Redirect /api to test page
     */
    @GetMapping("/api")
    public String apiHome() {
        return "redirect:/test.html";
    }
}

