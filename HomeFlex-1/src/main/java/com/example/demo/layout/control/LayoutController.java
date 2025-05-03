package com.example.demo.layout.control;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.demo.menu.model.MenuItemVO;
import com.example.demo.menu.service.MenuService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

@ControllerAdvice
public class LayoutController {
    
    @Autowired private MenuService menuService;
    @Autowired private UsuarioService usuarioService;
    
    @ModelAttribute("menuPrincipal")
    public List<MenuItemVO> menuPrincipal(Authentication auth) {
        return menuService.obtenerMenuUsuario(auth);
    }
    
    @ModelAttribute("usuarioActual")
    public UsuarioVO usuarioActual(Authentication auth) {
        if (auth != null) {
            return usuarioService.buscarPorUsername(auth.getName());
        }
        return null;
    }
}
