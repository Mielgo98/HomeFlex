/**
 * HomeFlex - Script para el Asistente Virtual (Chatbot Widget)
 * 
 * Este script maneja:
 * 1. Apertura/cierre del widget
 * 2. Envío de mensajes al API del chatbot
 * 3. Visualización de mensajes en la interfaz
 * 4. Formateo de respuestas
 */

document.addEventListener('DOMContentLoaded', function() {
    initChatbotWidget();
});

/**
 * Inicializa el widget de chatbot
 */
function initChatbotWidget() {
    const chatbotToggle = document.getElementById('chatbotToggle');
    const chatbotContainer = document.getElementById('chatbotContainer');
    const closeChatbot = document.getElementById('closeChatbot');
    const chatForm = document.getElementById('chatForm');
    const userInput = document.getElementById('userInput');
    const chatMessages = document.getElementById('chatMessages');
    const conversationHistory = document.getElementById('conversationHistory');
    
    // Estado del widget
    let isWaitingForResponse = false;
    
    // Eventos del widget
    chatbotToggle.addEventListener('click', openChatbot);
    closeChatbot.addEventListener('click', closeChatbotWidget);
    chatForm.addEventListener('submit', handleSubmit);
    
    // Cerrar con Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && chatbotContainer.style.display !== 'none') {
            closeChatbotWidget();
        }
    });
    
    /**
     * Abre el widget de chatbot
     */
    function openChatbot() {
        chatbotContainer.style.display = 'flex';
        userInput.focus();
        scrollToBottom();
    }
    
    /**
     * Cierra el widget de chatbot
     */
    function closeChatbotWidget() {
        chatbotContainer.style.display = 'none';
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
            entityType: "PROPERTY"
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
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user-message';
        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-bubble">
                    <p>${escapeHtml(text)}</p>
                </div>
                <i class="bi bi-person-fill message-avatar"></i>
            </div>
        `;
        
        conversationHistory.appendChild(messageDiv);
        scrollToBottom();
    }
    
    /**
     * Agrega un mensaje del bot a la conversación
     */
    function addBotMessage(text) {
        const formattedText = formatMessage(text);
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message';
        messageDiv.innerHTML = `
            <div class="message-content">
                <i class="bi bi-robot message-avatar"></i>
                <div class="message-bubble">
                    ${formattedText}
                </div>
            </div>
        `;
        
        conversationHistory.appendChild(messageDiv);
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
        
        conversationHistory.appendChild(typingDiv);
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
        const urlRegex = /(https?:\/\/[^\s]+)/g;
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
}