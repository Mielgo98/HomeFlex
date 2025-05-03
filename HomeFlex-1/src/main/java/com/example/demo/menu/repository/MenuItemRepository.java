package com.example.demo.menu.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.menu.model.MenuItemVO;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItemVO, Long> {
    
    List<MenuItemVO> findByPadreIsNullAndActivoTrueOrderByOrden();
    
    List<MenuItemVO> findByPadreIdAndActivoTrueOrderByOrden(Long padreId);
    
    @Query("SELECT DISTINCT m FROM MenuItemVO m JOIN m.roles r "
         + "WHERE r.nombre IN :roles AND m.padre IS NULL AND m.activo = true "
         + "ORDER BY m.orden")
    List<MenuItemVO> findByRoles(@Param("roles") Set<String> roles);
}
