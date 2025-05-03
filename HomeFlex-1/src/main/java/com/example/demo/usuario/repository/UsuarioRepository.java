package com.example.demo.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioVO, Long> {
    Optional<UsuarioVO> findUserEntityByEmail(String email);
    Optional<UsuarioVO> findByUsername(String username);
    Optional<UsuarioVO> findByTokenVerificacion(String token);
    List<UsuarioVO> findAllByRolesNombre(String rolNombre);

    /**
     * Busca usuarios por estado de verificación y que tengan un token de verificación
     * @param verificado estado de verificación (falso para los que aún no han activado)
     * @return lista de usuarios no verificados y con token presente
     */
    List<UsuarioVO> findByVerificadoAndTokenVerificacionIsNotNull(boolean verificado);

    /**
     * Cuenta los usuarios agrupados por nombre de rol.
     * @return Lista de Object[] donde [0]=rolNombre (String), [1]=cantidad (Long)
     */
    @Query("""
        SELECT r.nombre, COUNT(u)
        FROM UsuarioVO u
        JOIN u.roles r
        GROUP BY r.nombre
        """)
    List<Object[]> contarUsuariosPorRol();
    

    /**
     * Busca todos los Usuarios que tengan rol PROPIETARIO o INQUILINO.
     * DISTINCT evita que un mismo Usuario salga repetido si tiene ambos roles.
     */
    @Query("SELECT DISTINCT u FROM UsuarioVO u JOIN u.roles r WHERE r.nombre IN :roles")
    List<UsuarioVO> findDistinctByRolNombreIn(@Param("roles") List<String> roles);
}
