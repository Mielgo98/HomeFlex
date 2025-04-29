/**
 * HomeFlex - Script para el Asistente Virtual (Chatbot)
 * 
 * Este script maneja:
 * 1. Envío de mensajes al API del chatbot
 * 2. Visualización de mensajes en la interfaz
 * 3. Sugerencias de preguntas
 * 4. Formateo de respuestas
 */

document.addEventListener('DOMContentLoaded', function() {
    // Referencias a elementos del DOM
    const chatForm = document.getElementById('chatForm');
    const userInput = document.getElementById('userInput');
    const chatMessages = document.getElementById('chatMessages');
    const conversationHistory = document.getElementById('conversationHistory');
    const suggestionButtons = document.querySelectorAll('.suggestion-btn');
    
    // Estado del chat
    let isWaitingForResponse = false;
    
    // Inicializar el chat
    initChat();
    
    /**
     * Inicializa el componente de chat
     */
    function initChat() {
        // Eventos del formulario
        chatForm.addEventListener('submit', handleSubmit);
        
        // Eventos para botones de sugerencias
        suggestionButtons.forEach(button => {
            button.addEventListener('click', function() {
                const question = this.dataset.question;
                userInput.value = question;
                handleSubmit(new Event('submit'));
            });
        });
        
        // Enfocar el campo de entrada
        userInput.focus();
        
        // Scroll al final de los mensajes
        scrollToBottom();
    }
    
    /**
     * Maneja el envío de mensajes
     */
    function handleSubmit(e) {
        e.preventDefault();
        
        // Obtener mensaje del usuario
        const message = userInput.value.trim();
        
        // Validar que no esté vacío
        if (!message || isWaitingForResponse) return;
        
        // Mostrar mensaje del usuario
        addUserMessage(message);
        
        // Limpiar campo de entrada
        userInput.value = '';
        
        // Enviar mensaje al servicio de chatbot
        sendMessage(message);
    }
    
    /**
     * Envía el mensaje al API del chatbot
     */
    function sendMessage(message) {
        // Mostrar indicador de carga
        showTypingIndicator();
        isWaitingForResponse = true;
        
        // Preparar datos para la solicitud
        const data = {
            question: message,
            entityType: "PROPERTY" // Por defecto consultamos sobre propiedades
        };
        
        // Llamada al API
        fetch('/api/chatbot/ask', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la comunicación con el chatbot');
            }
            return response.json();
        })
        .then(data => {
            // Quitar indicador de carga
            removeTypingIndicator();
            
            // Mostrar respuesta
            addBotMessage(data.answer);
            isWaitingForResponse = false;
        })
        .catch(error => {
            console.error('Error:', error);
            
            // Quitar indicador de carga
            removeTypingIndicator();
            
            // Mostrar mensaje de error
            addBotMessage('Lo siento, ha ocurrido un error al procesar tu consulta. Por favor, inténtalo de nuevo más tarde.');
            isWaitingForResponse = false;
        });
    }
    
    /**
     * Agrega un mensaje del usuario a la conversación
     */
    function addUserMessage(text) {
        const time = getCurrentTime();
        
        // Crear elemento de mensaje
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user-message';
        messageDiv.innerHTML = `
            <div class="message-content">
                <i class="bi bi-person-fill message-avatar"></i>
                <div class="message-bubble">
                    <p>${escapeHtml(text)}</p>
                </div>
            </div>
            <span class="message-time">${time}</span>
        `;
        
        // Añadir a la conversación
        conversationHistory.appendChild(messageDiv);
        
        // Scroll al último mensaje
        scrollToBottom();
    }
    
    /**
     * Agrega un mensaje del bot a la conversación
     */
    function addBotMessage(text) {
        const time = getCurrentTime();
        
        // Mejorar formato del mensaje (convertir URLs, etc.)
        const formattedText = formatMessage(text);
        
        // Crear elemento de mensaje
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message';
        messageDiv.innerHTML = `
            <div class="message-content">
                <i class="bi bi-robot message-avatar"></i>
                <div class="message-bubble">
                    ${formattedText}
                </div>
            </div>
            <span class="message-time">${time}</span>
        `;
        
        // Añadir a la conversación
        conversationHistory.appendChild(messageDiv);
        
        // Scroll al último mensaje
        scrollToBottom();
    }
    
    /**
     * Muestra el indicador de escritura del bot
     */
    function showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message bot-message typing-indicator';
        typingDiv.innerHTML = `
            <div class="message-content">
                <i class="bi bi-robot message-avatar"></i>
                <div class="message-bubble typing">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                </div>
            </div>
        `;
        
        // Añadir a la conversación
        conversationHistory.appendChild(typingDiv);
        
        // Scroll al indicador
        scrollToBottom();
    }
    
    /**
     * Quita el indicador de escritura
     */
    function removeTypingIndicator() {
        const typingIndicator = document.querySelector('.typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }
    
    /**
     * Hace scroll al final de la conversación
     */
    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
    
    /**
     * Obtiene la hora actual formateada
     */
    function getCurrentTime() {
        const now = new Date();
        return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    
    /**
     * Formatea el mensaje para mejorar la visualización
     */
    function formatMessage(text) {
        // Dividir el texto en párrafos
        const paragraphs = text.split('\n').filter(p => p.trim() !== '');
        
        // Convertir cada párrafo en un elemento <p>
        return paragraphs.map(p => `<p>${formatLinks(escapeHtml(p))}</p>`).join('');
    }
    
    /**
     * Convierte enlaces de texto en elementos <a>
     */
    function formatLinks(text) {
        // Expresión regular para URLs
        const urlRegex = /(https?:\/\/[^\s]+)/g;
        
        // Reemplazar URLs con elementos <a>
        return text.replace(urlRegex, url => `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);
    }
    
    /**
     * Escapa caracteres HTML para prevenir XSS
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});