package com.example.demo.usuario.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

@Service
public class UsuarioDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        UsuarioVO usuarioVO = this.usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("El nombre de usuario no existe"));
        
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        usuarioVO.getRoles().forEach(role -> 
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.getNombre()))
        );
        
        return new User(
                usuarioVO.getUsername(),
                usuarioVO.getPassword(),
                usuarioVO.getIsEnabled(),           // habilitado
                usuarioVO.getAccountNonExpired(),   // cuenta no expirada
                usuarioVO.getCredentialsNonExpired(), // credenciales no expirada
                usuarioVO.getAccountNonLocked(),    // cuenta no bloqueada
                authorityList
        );
    }
}