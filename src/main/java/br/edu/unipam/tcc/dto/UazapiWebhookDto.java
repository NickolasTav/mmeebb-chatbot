package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para recepção e deserialização de eventos de webhook da Uazapi (WhatsApp).
 *
 * @param event      Tipo de evento disparado (ex: "messages.upsert", "messages", "connection.update").
 * @param remoteJid  JID completo do remetente (ex: "5534999999999@s.whatsapp.net").
 * @param fromMe     Indica se a mensagem foi enviada pelo próprio número/bot.
 * @param text       Conteúdo textual da mensagem recebida.
 * @param pushName   Nome exibido/perfil do remetente no WhatsApp.
 * @param instance   Identificador ou nome da instância da Uazapi.
 * @param messageId  Identificador único da mensagem.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UazapiWebhookDto(
        @JsonProperty("event") String event,
        @JsonProperty("remoteJid") String remoteJid,
        @JsonProperty("fromMe") Boolean fromMe,
        @JsonProperty("text") String text,
        @JsonProperty("pushName") String pushName,
        @JsonProperty("instance") String instance,
        @JsonProperty("messageId") String messageId
) {

    /**
     * Construtor de conveniência para retrocompatibilidade sem event/pushName.
     */
    public UazapiWebhookDto(String remoteJid, Boolean fromMe, String text, String instance, String messageId) {
        this(null, remoteJid, fromMe, text, null, instance, messageId);
    }

    /**
     * Extrai apenas os dígitos numéricos do número de telefone (E.164 nacional).
     * Remove sufixos como @s.whatsapp.net, @c.us e símbolos de formatação (+, -, parênteses).
     *
     * @return Telefone normalizado com apenas números, ou string vazia se nulo/em branco.
     */
    public String getCleanPhoneNumber() {
        if (remoteJid == null || remoteJid.isBlank()) {
            return "";
        }
        // Se contiver ':', remove identificadores de dispositivo (ex: 5534999998888:1@s.whatsapp.net)
        String cleaned = remoteJid;
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex > 0) {
            int atIndex = cleaned.indexOf('@');
            if (atIndex > colonIndex) {
                cleaned = cleaned.substring(0, colonIndex) + cleaned.substring(atIndex);
            }
        }
        return cleaned.replaceAll("@.*$", "").replaceAll("\\D", "");
    }
}
