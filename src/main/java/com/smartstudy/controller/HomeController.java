package com.smartstudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home controller for basic navigation
 */
@Controller
public class HomeController {
    
    /**
     * Redirect root to index page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }
    
    /**
     * Redirect /api to index page
     */
    @GetMapping("/api")
    public String apiHome() {
        return "redirect:/index.html";
    }
}

