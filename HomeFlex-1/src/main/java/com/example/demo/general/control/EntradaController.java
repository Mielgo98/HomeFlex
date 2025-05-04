package com.example.demo.general.control;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;

@Controller
public class EntradaController {
  
  @Autowired
  private PropiedadService propiedadService;
  
  @GetMapping({"/", "/index"})
  public String index(Model model) {
	  
	  if (!model.containsAttribute("propiedad")) {      // para no
          model.addAttribute("propiedad", new PropiedadVO()); // pisar redirecciones
      }
	  
    List<PropiedadDTO> propiedadesDestacadas = propiedadService.obtenerPropiedadesDestacadas();
    model.addAttribute("propiedadesDestacadas", propiedadesDestacadas);
    
    // Obtener ciudades populares para sugerencias de búsqueda
    List<String> ciudadesPopulares = propiedadService.obtenerCiudadesPopulares();
    model.addAttribute("ciudadesPopulares", ciudadesPopulares);
    
    return "index";
  }
}