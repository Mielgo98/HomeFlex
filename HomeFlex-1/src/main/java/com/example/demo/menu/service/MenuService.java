package com.example.demo.menu.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.*;
import org.springframework.stereotype.Service;
import com.example.demo.menu.model.MenuItemVO;
import com.example.demo.menu.repository.MenuItemRepository;

@Service
public class MenuService {
    
    @Autowired
    private MenuItemRepository repo;
    
    public List<MenuItemVO> obtenerMenuPorRoles(Set<String> roles) {
        return repo.findByRoles(roles);
    }
    
    public void cargarSubmenus(MenuItemVO item, Set<String> roles) {
        List<MenuItemVO> subs = repo.findByPadreIdAndActivoTrueOrderByOrden(item.getId());
        subs.removeIf(sub ->
            sub.getRoles().stream().map(r -> r.getNombre()).noneMatch(roles::contains)
        );
        item.setSubitems(new LinkedHashSet<>(subs));
        subs.forEach(sub -> cargarSubmenus(sub, roles));
    }
    
    public List<MenuItemVO> obtenerMenuUsuario(Authentication auth) {
        if (auth == null) return List.of();
        Set<String> roles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(r -> r.replace("ROLE_", ""))
            .collect(Collectors.toSet());
        List<MenuItemVO> raiz = obtenerMenuPorRoles(roles);
        raiz.forEach(item -> cargarSubmenus(item, roles));
        return raiz;
    }
}
