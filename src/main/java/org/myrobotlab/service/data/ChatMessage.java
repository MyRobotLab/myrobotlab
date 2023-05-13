package org.myrobotlab.service.data;

import java.util.Objects;

/**
 * Represents one "turn" of a conversation.
 * In a conversation the participants take turns
 * speaking, while one is speaking the others
 * <i>should</i> be listening. In most conversations,
 * a participant can speak for as long as they like,
 * but in all cases respondents must know who was speaking.
 * Thus, this class contains the name of the speaker
 * and what they said during their turn.
 * Since one cannot unsay what has been said,
 * this class is immutable. It is meant as a data object
 * to pass records of the conversation around.
 *
 * @author AutonomicPerfectionist
 */
public class ChatMessage {

    /**
     * When an AI / Chatbot is speaking during
     * the conversation, its speakerName is the value
     * of this constant. This allows a chatbot to be
     * renamed without forgetting everything.
     */
    public static final String AI = "AI";

    /**
     * The person who was speaking during this turn.
     * If a chatbot was speaking, then this field should have
     * the value of {@link #AI}.
     */
    public final String speaker;

    /**
     * What the {@link #speaker} said during
     * their turn.
     */
    public final String message;

    /**
     * The ID of the conversation this message was a part of.
     * This ID can be generated through a number of ways, a simple
     * way would be to add the hashcodes of the participants' names.
     */
    public final long conversationId;

    public ChatMessage(String speaker, String message, long conversationId) {
        Objects.requireNonNull(speaker, "Speaker may not be null");
        Objects.requireNonNull(message, "Turn contents may not be null");
        this.speaker = speaker;
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getMessage() {
        return message;
    }

    public long getConversationId() {
        return conversationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChatMessage that = (ChatMessage) o;

        if (!speaker.equals(that.speaker)) return false;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = speaker.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "speaker='" + speaker + '\'' +
                ", turnContents='" + message + '\'' +
                ", conversationId=" + conversationId +
                '}';
    }
}
