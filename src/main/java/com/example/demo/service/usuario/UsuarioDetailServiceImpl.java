package com.example.demo.service.usuario;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.demo.model.usuario.UsuarioVO;
import com.example.demo.repository.usuario.UsuarioRepository;

@Service
public class UsuarioDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        UsuarioVO usuarioVO = this.usuarioRepository.findUserEntityByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("El nombre de usuario no existe"));
        
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        usuarioVO.getRoles().forEach(role -> 
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.getNombre()))
        );
        
        return new User(
                usuarioVO.getEmail(),
                usuarioVO.getPassword(),
                usuarioVO.getIsEnabled(),           // habilitado
                usuarioVO.getAccountNonExpired(),   // cuenta no expirada
                usuarioVO.getCredentialsNonExpired(), // credenciales no expirada
                usuarioVO.getAccountNonLocked(),    // cuenta no bloqueada
                authorityList
        );
    }
}
