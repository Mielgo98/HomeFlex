package com.example.demo.general.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class EntradaController {
  @GetMapping({"/", "/index"})
  public String index() {
    return "index";
  }
}
