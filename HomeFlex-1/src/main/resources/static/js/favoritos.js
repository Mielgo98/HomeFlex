/**
 * Script para gestionar favoritos en la vista de listado
 * y sincronización con el modal de perfil
 */

class FavoritosManager {
    constructor() {
        this.favoritos = new Set();
        this.initEventListeners();
        this.loadFavoritos();
    }

    /**
     * Inicializa los event listeners
     */
    initEventListeners() {
        // Delegar eventos en el documento para botones dinámicos
        document.addEventListener('click', async (e) => {
            const target = e.target.closest('.btn-favorito');
            if (!target) return;

            e.preventDefault();
            const propiedadId = parseInt(target.getAttribute('data-propiedad-id'));
            await this.toggleFavorito(propiedadId, target);
        });
    }

    /**
     * Carga los favoritos actuales del usuario
     */
    async loadFavoritos() {
        try {
            const response = await fetch('/api/usuario/propiedades-favoritas');
            if (!response.ok) throw new Error('Error al cargar favoritos');
            
            const favoritos = await response.json();
            this.favoritos = new Set(favoritos.map(p => p.id));
            this.updateButtonsState();
        } catch (error) {
            console.error('Error al cargar favoritos:', error);
        }
    }

    /**
     * Alterna el estado de favorito de una propiedad
     */
    async toggleFavorito(propiedadId, buttonElement) {
        const isFavorite = this.favoritos.has(propiedadId);
        const icon = buttonElement.querySelector('i');
        
        try {
            // Deshabilitar el botón durante la petición
            buttonElement.disabled = true;
            
            const response = await fetch(`/api/usuario/${isFavorite ? 'eliminar' : 'agregar'}-favorito/${propiedadId}`, {
                method: isFavorite ? 'DELETE' : 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) throw new Error('Error al actualizar favorito');

            const data = await response.json();
            if (data.success) {
                // Actualizar el estado local
                if (isFavorite) {
                    this.favoritos.delete(propiedadId);
                } else {
                    this.favoritos.add(propiedadId);
                }
                
                // Actualizar visualmente el botón
                this.updateButtonVisual(buttonElement, !isFavorite);
                
                // Emitir evento personalizado para sincronización
                this.emitFavoritoChange(propiedadId, !isFavorite);
            }
        } catch (error) {
            console.error('Error:', error);
            this.showNotification('Error al actualizar favorito', 'error');
        } finally {
            buttonElement.disabled = false;
        }
    }

    /**
     * Actualiza el estado visual de un botón de favorito
     */
    updateButtonVisual(button, isFavorite) {
        const icon = button.querySelector('i');
        
        if (isFavorite) {
            icon.classList.remove('bi-heart');
            icon.classList.add('bi-heart-fill');
            button.classList.remove('btn-outline-danger');
            button.classList.add('btn-danger');
        } else {
            icon.classList.remove('bi-heart-fill');
            icon.classList.add('bi-heart');
            button.classList.remove('btn-danger');
            button.classList.add('btn-outline-danger');
        }
    }

    /**
     * Actualiza el estado de todos los botones de favoritos
     */
    updateButtonsState() {
        document.querySelectorAll('.btn-favorito').forEach(button => {
            const propiedadId = parseInt(button.getAttribute('data-propiedad-id'));
            const isFavorite = this.favoritos.has(propiedadId);
            this.updateButtonVisual(button, isFavorite);
        });
    }

    /**
     * Emite un evento personalizado cuando cambia el estado de un favorito
     */
    emitFavoritoChange(propiedadId, isFavorite) {
        const event = new CustomEvent('favoritoChange', {
            detail: { propiedadId, isFavorite }
        });
        document.dispatchEvent(event);
    }

    /**
     * Muestra una notificación al usuario
     */
    showNotification(message, type = 'info') {
        // Crear elemento de notificación
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        // Añadir estilos inline
        notification.style.position = 'fixed';
        notification.style.bottom = '80px';
        notification.style.right = '20px';
        notification.style.padding = '12px 24px';
        notification.style.borderRadius = '4px';
        notification.style.color = 'white';
        notification.style.zIndex = '9999';
        notification.style.opacity = '0';
        notification.style.transition = 'opacity 0.3s ease';
        
        if (type === 'error') {
            notification.style.backgroundColor = '#dc3545';
        } else {
            notification.style.backgroundColor = '#28a745';
        }
        
        document.body.appendChild(notification);
        
        // Animar entrada
        setTimeout(() => {
            notification.style.opacity = '1';
        }, 10);
        
        // Animar salida
        setTimeout(() => {
            notification.style.opacity = '0';
            setTimeout(() => {
                document.body.removeChild(notification);
            }, 300);
        }, 3000);
    }
}

// Inicializar el gestor de favoritos cuando se carga la página
document.addEventListener('DOMContentLoaded', () => {
    if (document.querySelector('.btn-favorito')) {
        window.favoritosManager = new FavoritosManager();
    }
});