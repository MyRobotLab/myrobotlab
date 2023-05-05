package org.myrobotlab.service.interfaces;


import org.myrobotlab.service.data.ConversationTurn;

import java.util.List;

/**
 * Provides a form of memory for chatbots.
 * The idea is to store information in an
 * index of some kind during the conversation,
 * and then recall specific information using
 * the current input request. Usually
 * this would be implemented through a vector store.
 *
 * @author AutonomicPerfectionist
 */
public interface ChatbotMemory {

    /**
     * Commit a piece of the conversation to memory.
     * Once memorized, the memory can be recalled if a request
     * has high enough similarity to the memory.
     *
     * @param memory The turn to be remembered.
     */
    void memorize(ConversationTurn memory);

    /**
     * Recall a number of memorized conversation turns
     * that have similarity to the request. The maximum number
     * of memories recalled is set via {@link #setMaxNumMemoriesRecalled(int)}.
     * This usually corresponds to the {@code top_k} parameter in vector stores.
     * @param request The most recent conversation turn that is used to recall memories.
     * @return Recalled memories
     */
    List<ConversationTurn> recallMemories(ConversationTurn request);

    /**
     * Upon recalling memories, they are published through this method.
     * Services that are interested in recalled memories should subscribe to this method.
     * @param memories The memories that have been recalled.
     * @return The recalled memories.
     */
    List<ConversationTurn> publishMemories(List<ConversationTurn> memories);

    /**
     * Sets the maximum number of memories to be recalled
     * via {@link #recallMemories(ConversationTurn)}.
     * @param number The maximum number of memories that can be recalled at once
     */
    void setMaxNumMemoriesRecalled(int number);

    /**
     * Gets the maximum number of memories to be recalled
     * via {@link #recallMemories(ConversationTurn)}.
     * @return The maximum number of memories that can be recalled at once.
     */
    int getMaxNumMemoriesRecalled();
}
