package org.usac.bots.jbot.messagehandlers;

import com.ciscospark.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.usac.bots.jbot.WebexTeamsController;
import org.usac.bots.jbot.entities.ChatMessage;
import org.usac.bots.jbot.entities.HandlerResult;
import org.usac.bots.jbot.scheduler.Scheduler;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageHandlers {

    private List<LogGrootMessageHandler> handlers;
    private Map<String, LogGrootMessageHandler> convoHandlers;
    private Map<String, String> convoThemes;

    private Scheduler scheduler;
    private WebexTeamsController webexTeamsController;

    @Autowired
    public MessageHandlers(List<LogGrootMessageHandler> handlers, WebexTeamsController webexTeamsController) {
        this.handlers = handlers;
        this.webexTeamsController = webexTeamsController;

        this.convoHandlers = new HashMap<>();
        this.convoThemes = new HashMap<>();

        for (LogGrootMessageHandler handler : handlers) {
            handler.init(this);
        }
    }

    public List<LogGrootMessageHandler> getHandlers() {
        return handlers;
    }

    public LogGrootMessageHandler getHandler(String id) {
        for (LogGrootMessageHandler handler : handlers) {
            if (handler.getId().equals(id)) {
                return handler;
            }
        }
        return null;
    }

    public WebexTeamsController getWebexTeamsController() {
        return webexTeamsController;
    }

    public void setWebexTeamsController(WebexTeamsController webexTeamsController) {
        this.webexTeamsController = webexTeamsController;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }


    public void setConversation(ChatMessage message, LogGrootMessageHandler handler, String theme) {
        convoThemes.put(message.getRoomId(), theme);
        convoHandlers.put(message.getRoomId(), handler);
    }

    public String getConversationTheme(ChatMessage message) {
        return convoThemes.get(message.getRoomId());
    }

    public LogGrootMessageHandler getConversationHandler(ChatMessage message) {
        return convoHandlers.get(message.getRoomId());
    }


    public HandlerResult handleChatMessage(ChatMessage message, LogGrootMessageHandler triggeredBy) {
        return handleChatMessage(message, triggeredBy, true);
    }

    public HandlerResult handleChatMessageNoConversation(ChatMessage message, LogGrootMessageHandler triggeredBy, boolean renderReply) {

        LogGrootMessageHandler handler = getBestMessageHandler(message);
        HandlerResult result = safeHandleChatMessage(handler, message, triggeredBy, renderReply, null);
        return result;
    }

    public HandlerResult handleChatMessage(ChatMessage message, LogGrootMessageHandler triggeredBy, boolean renderReply) {

        // Redirect to same handler if this is conversation
        boolean hasConvoHandler = convoHandlers.containsKey(message.getRoomId());

        LogGrootMessageHandler handler = hasConvoHandler
                ? convoHandlers.get(message.getRoomId())
                : getBestMessageHandler(message);

        String theme = hasConvoHandler
                ? convoThemes.get(message.getRoomId())
                : null;

        HandlerResult result = safeHandleChatMessage(handler, message, triggeredBy, renderReply, theme);

        // Manage the convo list
        if (result.getConversationTheme() != null) {
            convoHandlers.put(message.getRoomId(), handler);
            convoThemes.put(message.getRoomId(), result.getConversationTheme());

        } else if (convoHandlers.containsKey(message.getRoomId())) {
            convoHandlers.remove(message.getRoomId());
            convoThemes.remove(message.getRoomId());
        }


        return result;
    }

    public HandlerResult safeHandleChatMessage(LogGrootMessageHandler handler, ChatMessage message, LogGrootMessageHandler triggeredBy, boolean renderReply, String theme) {
        HandlerResult result = null;
        try {
            result = handler.handleChatMessage(message, triggeredBy, theme);
        } catch (InvocationTargetException e) {
            Room exceptionsRoom = webexTeamsController.createRoom("LogGroot Exceptions", Arrays.asList("Stephan.Mitchev@usac.org"));
            webexTeamsController.replyTo(exceptionsRoom, "I heard `" + message.getText() + "` and an invocation target exception occurred:  \n```  \n" + e.getTargetException().getMessage() + "  \n```");
        } catch (IllegalAccessException e) {
            Room exceptionsRoom = webexTeamsController.createRoom("LogGroot Exceptions", Arrays.asList("Stephan.Mitchev@usac.org"));
            webexTeamsController.replyTo(exceptionsRoom, "I heard `" + message.getText() + "` and this is what happened:  \n```  \n" + e.getMessage() + "  \n```");
        }

        if (result != null) {
            if (renderReply && (result.getReply() != null || result.getAttachment() != null)) {
                webexTeamsController.replyToWithAttachment(message, result.getReply(), result.getAttachment(), true);
            }
        } else {
            result = HandlerResult.failure().reply("Congrats! You just discovered a bug! I notified the support team with all relevant data.");
        }

        return result;
    }

    /**
     * Determines the best matching {@link LogGrootMessageHandler} for the current message based on
     * its {@link LogGrootMessageHandler#getActivationScore(ChatMessage)}  method}
     *
     * @param message The already decrypted message
     * @return The {@link LogGrootMessageHandler} with the highest score fo this message
     * @see LogGrootMessageHandler
     */
    public LogGrootMessageHandler getBestMessageHandler(ChatMessage message) {

        LogGrootMessageHandler bestHandler = null;
        float bestScore = 0;

        for (LogGrootMessageHandler currentHandler : handlers) {
            float currentScore = currentHandler.isEnabled() ? currentHandler.getActivationScore(message) : 0;

            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestHandler = currentHandler;
            }
        }

        return bestHandler;
    }

}
