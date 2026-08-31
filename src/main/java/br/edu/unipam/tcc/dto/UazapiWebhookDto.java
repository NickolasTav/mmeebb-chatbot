package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

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
        @JsonProperty("messageId") String messageId,
        @JsonProperty("BaseUrl") String baseUrl,
        @JsonProperty("EventType") String eventType,
        @JsonProperty("instanceName") String instanceName,
        @JsonProperty("owner") String owner,
        @JsonProperty("token") String token,
        @JsonProperty("message") UazapiNestedMessage message,
        @JsonProperty("chat") UazapiNestedChat chat
) {

    /**
     * Construtor de conveniência com 7 parâmetros.
     */
    public UazapiWebhookDto(String event, String remoteJid, Boolean fromMe, String text, String pushName, String instance, String messageId) {
        this(event, remoteJid, fromMe, text, pushName, instance, messageId, null, null, null, null, null, null, null);
    }

    /**
     * Construtor de conveniência para retrocompatibilidade com 5 parâmetros.
     */
    public UazapiWebhookDto(String remoteJid, Boolean fromMe, String text, String instance, String messageId) {
        this(null, remoteJid, fromMe, text, null, instance, messageId, null, null, null, null, null, null, null);
    }

    @Override
    public String event() {
        if (event != null && !event.isBlank()) return event;
        return eventType != null ? eventType : "";
    }

    @Override
    public String remoteJid() {
        if (remoteJid != null && !remoteJid.isBlank()) return remoteJid;
        if (message != null) {
            if (message.chatid() != null && !message.chatid().isBlank()) return message.chatid();
            if (message.sender_pn() != null && !message.sender_pn().isBlank()) return message.sender_pn();
            if (message.sender() != null && !message.sender().isBlank()) return message.sender();
        }
        if (chat != null) {
            if (chat.wa_chatid() != null && !chat.wa_chatid().isBlank()) return chat.wa_chatid();
            if (chat.phone() != null && !chat.phone().isBlank()) return chat.phone() + "@s.whatsapp.net";
        }
        return "";
    }

    @Override
    public Boolean fromMe() {
        if (fromMe != null) return fromMe;
        if (message != null && message.fromMe() != null) return message.fromMe();
        return false;
    }

    @Override
    public String text() {
        if (text != null && !text.isBlank()) return text;
        if (message != null) {
            if (message.text() != null && !message.text().isBlank()) {
                return message.text();
            }
            if (message.content() != null) {
                if (message.content().isTextual()) {
                    return message.content().asText();
                }
                if (message.content().hasNonNull("text")) {
                    return message.content().get("text").asText();
                }
                if (message.content().hasNonNull("body")) {
                    return message.content().get("body").asText();
                }
                if (message.content().hasNonNull("conversation")) {
                    return message.content().get("conversation").asText();
                }
                if (message.content().path("extendedTextMessage").hasNonNull("text")) {
                    return message.content().path("extendedTextMessage").get("text").asText();
                }
            }
        }
        return "";
    }

    @Override
    public String pushName() {
        if (pushName != null && !pushName.isBlank()) return pushName;
        if (message != null && message.senderName() != null && !message.senderName().isBlank()) return message.senderName();
        if (chat != null && chat.wa_name() != null && !chat.wa_name().isBlank()) return chat.wa_name();
        return "";
    }

    @Override
    public String instance() {
        if (instance != null && !instance.isBlank()) return instance;
        return instanceName != null ? instanceName : "";
    }

    @Override
    public String messageId() {
        if (messageId != null && !messageId.isBlank()) return messageId;
        if (message != null) {
            if (message.messageid() != null && !message.messageid().isBlank()) return message.messageid();
            if (message.id() != null && !message.id().isBlank()) return message.id();
        }
        return "";
    }

    /**
     * Verifica se a mensagem é proveniente de um grupo do WhatsApp.
     */
    public boolean isGroupMessage() {
        String jid = remoteJid();
        if (jid != null && jid.contains("@g.us")) return true;
        if (message != null && Boolean.TRUE.equals(message.isGroup())) return true;
        if (chat != null && Boolean.TRUE.equals(chat.wa_isGroup())) return true;
        return false;
    }

    /**
     * Extrai apenas os dígitos numéricos do número de telefone (E.164 nacional).
     * Remove sufixos como @s.whatsapp.net, @c.us e símbolos de formatação (+, -, parênteses).
     *
     * @return Telefone normalizado com apenas números, ou string vazia se nulo/em branco.
     */
    public String getCleanPhoneNumber() {
        String effectiveJid = remoteJid();
        if (effectiveJid == null || effectiveJid.isBlank()) {
            if (chat != null && chat.phone() != null && !chat.phone().isBlank()) {
                return chat.phone().replaceAll("\\D", "");
            }
            return "";
        }
        // Se contiver ':', remove identificadores de dispositivo (ex: 5534999998888:1@s.whatsapp.net)
        String cleaned = effectiveJid;
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex > 0) {
            int atIndex = cleaned.indexOf('@');
            if (atIndex > colonIndex) {
                cleaned = cleaned.substring(0, colonIndex) + cleaned.substring(atIndex);
            }
        }
        return cleaned.replaceAll("@.*$", "").replaceAll("\\D", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UazapiNestedMessage(
            @JsonProperty("chatid") String chatid,
            @JsonProperty("chatlid") String chatlid,
            @JsonProperty("content") JsonNode content,
            @JsonProperty("fromMe") Boolean fromMe,
            @JsonProperty("id") String id,
            @JsonProperty("isGroup") Boolean isGroup,
            @JsonProperty("mediaType") String mediaType,
            @JsonProperty("messageTimestamp") Long messageTimestamp,
            @JsonProperty("messageType") String messageType,
            @JsonProperty("messageid") String messageid,
            @JsonProperty("owner") String owner,
            @JsonProperty("sender") String sender,
            @JsonProperty("senderName") String senderName,
            @JsonProperty("sender_lid") String sender_lid,
            @JsonProperty("sender_pn") String sender_pn,
            @JsonProperty("source") String source,
            @JsonProperty("text") String text,
            @JsonProperty("type") String type,
            @JsonProperty("wasSentByApi") Boolean wasSentByApi
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UazapiNestedChat(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("owner") String owner,
            @JsonProperty("phone") String phone,
            @JsonProperty("wa_chatid") String wa_chatid,
            @JsonProperty("wa_chatlid") String wa_chatlid,
            @JsonProperty("wa_isGroup") Boolean wa_isGroup,
            @JsonProperty("wa_name") String wa_name
    ) {}
}

