package org.usac.bots.jbot.messagehandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usac.bots.jbot.WebexTeamsController;
import org.usac.bots.jbot.annotations.*;
import org.usac.bots.jbot.entities.ChatMessage;
import org.usac.bots.jbot.entities.HandlerResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public abstract class LogGrootMessageHandler {

    protected Logger log = null;
    private MessageHandlers messageHandlers;
    private List<Method> methods;
    private boolean enabled = true;
    private String id;

    public void init(MessageHandlers messageHandlers) {
        this.setMessageHandlers(messageHandlers);
        log = LoggerFactory.getLogger(this.getClass());

        MessageHandler messageHandlerAnnotation = this.getClass().getAnnotation(MessageHandler.class);
        id = messageHandlerAnnotation.id();

        methods = Arrays.asList(this.getClass().getMethods());
        methods.sort(Comparator.comparingInt((Method a) -> a.getAnnotation(OrderedMethod.class) != null ? a.getAnnotation(OrderedMethod.class).order() : 0));

        validateAnnotations();

        setEnabled(dependeciesEnabled());
    }

    public abstract boolean dependeciesEnabled();

    private void validateAnnotations() {
        for (Method m : methods) {

            StartsWithPhrase startsWithPhraseAnnotation = m.getAnnotation(StartsWithPhrase.class);
            ContainsPhrase containsPhraseAnnotation = m.getAnnotation(ContainsPhrase.class);
            ContainsDateReference containsDateReferenceAnnotation = m.getAnnotation(ContainsDateReference.class);
            MatchesRegex matchesRegexAnnotation = m.getAnnotation(MatchesRegex.class);
            ExcludesPhrase excludesPhraseAnnotation = m.getAnnotation(ExcludesPhrase.class);

            if (startsWithPhraseAnnotation != null &&
                    (m.getParameterTypes().length != 2
                            || m.getParameterTypes()[0] != ChatMessage.class
                            || m.getParameterTypes()[1] != LogGrootMessageHandler.class)
            ) {
                throw new IllegalArgumentException("Parameters of " + m.getName() + " do not match requirements of "
                        + StartsWithPhrase.class.getSimpleName() + " annotation");
            }

            if (containsPhraseAnnotation != null &&
                    (m.getParameterTypes().length != 2
                            || m.getParameterTypes()[0] != ChatMessage.class
                            || m.getParameterTypes()[1] != LogGrootMessageHandler.class)
            ) {
                throw new IllegalArgumentException("Parameters of " + m.getName() + " do not match requirements of "
                        + ContainsPhrase.class.getSimpleName() + " annotation");
            }

            if (containsDateReferenceAnnotation != null &&
                    (m.getParameterTypes().length != 3
                            || m.getParameterTypes()[0] != ChatMessage.class
                            || m.getParameterTypes()[1] != LogGrootMessageHandler.class
                            || m.getParameterTypes()[2] != List.class)
            ) {
                throw new IllegalArgumentException("Parameters of " + m.getName() + " do not match requirements of "
                        + ContainsDateReference.class.getSimpleName() + " annotation");
            }

            if (matchesRegexAnnotation != null &&
                    (m.getParameterTypes().length != 4
                            || m.getParameterTypes()[0] != ChatMessage.class
                            || m.getParameterTypes()[1] != LogGrootMessageHandler.class
                            || m.getParameterTypes()[2] != Pattern.class
                            || m.getParameterTypes()[3] != List.class)
            ) {
                throw new IllegalArgumentException("Parameters of " + m.getName() + " do not match requirements of "
                        + MatchesRegex.class.getSimpleName() + " annotation");
            }

            if (excludesPhraseAnnotation != null
                    && startsWithPhraseAnnotation == null
                    && containsPhraseAnnotation == null
            ) {
                throw new IllegalArgumentException(ExcludesPhrase.class.getSimpleName() + " annotation can be used only along with "
                        + ContainsPhrase.class.getSimpleName() + " and " + StartsWithPhrase.class.getSimpleName() + " annotations");
            }
        }

    }


    /**
     * Provides user with getHelp interacting with this message handler
     *
     * @return The help message or null.
     */
    public String getHelp() {
        List<HelpContentCapability> generalCapabilities = new ArrayList<>();
        Map<ConversationTheme, List<HelpContentCapability>> conversationCapabilities = new HashMap<>();

        StringBuilder result = new StringBuilder();

        for (Method m : methods) {
            ConversationTheme conversation = m.getAnnotation(ConversationTheme.class);
            HelpContentCapability helpContentCapability = m.getAnnotation(HelpContentCapability.class);

            if (helpContentCapability != null) {
                List<HelpContentCapability> contentCapabilities = generalCapabilities;

                if (conversation != null) {
                    if (!conversationCapabilities.containsKey(conversation)) {
                        conversationCapabilities.put(conversation, new ArrayList<>());
                    }

                    contentCapabilities = conversationCapabilities.get(conversation);
                }

                contentCapabilities.add(helpContentCapability);
            }
        }

        HelpContentHandler helpContentHandler = this.getClass().getAnnotation(HelpContentHandler.class);
        if (helpContentHandler != null) {
            result.append(helpContentHandler.prefix()).append("  \n\n");
        }

        generalCapabilities.sort(Comparator.comparingInt(HelpContentCapability::order));
        for (HelpContentCapability capability : generalCapabilities) {
            result.append("* ").append(capability.content()).append("  \n");
        }

        for (ConversationTheme conversation : conversationCapabilities.keySet()) {
            result.append("While engaged in a " + conversation.theme() + " you could say the following:  \n\n");
            conversationCapabilities.get(conversation).sort(Comparator.comparingInt(HelpContentCapability::order));
            for (HelpContentCapability capability : conversationCapabilities.get(conversation)) {
                result.append("* ").append(capability.content()).append("  \n");
            }
        }

        if (helpContentHandler != null) {
            result.append("\n").append(helpContentHandler.postfix());
        }

        return result.toString();
    }


    /**
     * Returns {@link HandlerResult} and publishes the response. T
     *
     * @param message     The already decrypted message
     * @param triggeredBy If this message is triggered by a different handler (e.g. scheduler) set it here. Otherwise, null
     * @return HandlerResult that contains the results statuses.
     */
    public HandlerResult handleChatMessage(ChatMessage message, LogGrootMessageHandler triggeredBy, String conversationTheme) throws InvocationTargetException, IllegalAccessException {

        HandlerResult result = null;

        for (int i = 0; result == null && i < methods.size(); i++) {

            Method m = methods.get(i);

            StartsWithPhrase startsWithPhraseAnnotation = m.getAnnotation(StartsWithPhrase.class);
            ContainsPhrase containsPhraseAnnotation = m.getAnnotation(ContainsPhrase.class);
            ContainsDateReference containsDateReferenceAnnotation = m.getAnnotation(ContainsDateReference.class);
            MatchesRegex matchesRegexAnnotation = m.getAnnotation(MatchesRegex.class);
            ExcludesPhrase excludesPhraseAnnotation = m.getAnnotation(ExcludesPhrase.class);
            ConversationTheme conversationAnnotation = m.getAnnotation(ConversationTheme.class);

            // Execute only annotated methods
            if (startsWithPhraseAnnotation == null && matchesRegexAnnotation == null
                    && containsPhraseAnnotation == null && containsDateReferenceAnnotation == null) {
                continue;
            }

            // Skip if within a conversation and not annotated or theme does not match
            if (conversationTheme != null && (conversationAnnotation == null || !conversationAnnotation.theme().equalsIgnoreCase(conversationTheme) && !conversationAnnotation.theme().equalsIgnoreCase("all"))
                    || conversationTheme == null && conversationAnnotation != null) {
                continue;
            }


            if (startsWithPhraseAnnotation != null
                    && WebexTeamsController.beginsWith(message.getText(), startsWithPhraseAnnotation.phrase())
                    && (excludesPhraseAnnotation == null || !WebexTeamsController.heard(message, excludesPhraseAnnotation.phrase()))
            ) {
                String text = WebexTeamsController.removeTermsFromStart(message.getText(), startsWithPhraseAnnotation.phrase());
                message.setProcessedText(text);
                result = (HandlerResult) m.invoke(this, message, triggeredBy);
            }
            // Process containsDateReference
            if (containsDateReferenceAnnotation != null
                    && WebexTeamsController.getDateGroups(message).size() > 0
            ) {
                message.setProcessedText(message.getText());
                result = (HandlerResult) m.invoke(this, message, triggeredBy, WebexTeamsController.getDateGroups(message));
                continue;
            }
            // Process contains
            if (containsPhraseAnnotation != null
                    && WebexTeamsController.heard(message, containsPhraseAnnotation.phrase())
                    && (excludesPhraseAnnotation == null || !WebexTeamsController.heard(message, excludesPhraseAnnotation.phrase()))
            ) {
                message.setProcessedText(message.getText());
                result = (HandlerResult) m.invoke(this, message, triggeredBy);
                continue;
            }
            // Process matchesRegex
            if (matchesRegexAnnotation != null && WebexTeamsController.heardRegex(message, Pattern.compile(matchesRegexAnnotation.regex(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL))) {
                Pattern pattern = Pattern.compile(matchesRegexAnnotation.regex(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                List<String> regexGroups = WebexTeamsController.getRegexGroups(message, pattern);

                result = (HandlerResult) m.invoke(this, message, triggeredBy, pattern, regexGroups);
                continue;
            }

        }

        if (result == null) {
            result = catchAll(message, triggeredBy, conversationTheme);
        }

        result.setHandler(this);
        return result;
    }

    public HandlerResult catchAll(ChatMessage message, LogGrootMessageHandler triggeredBy, String conversationTheme) {
        return HandlerResult.failure().reply(messageHandlers.getWebexTeamsController().getRandomReply("idk"));
    }

    /**
     * Computes and returns a fitness score for handling the current message. Higher scores give higher probability
     * of this handler being selected for handling the message.
     *
     * @param message The already decrypted message
     * @return A fitness score for handling the current message
     */
    Float getActivationScore(ChatMessage message) {

        MessageHandler aPrecendece = this.getClass().getDeclaredAnnotation(MessageHandler.class);
        if (aPrecendece == null) {
            return 0f;
        }


        for (Method m : methods) {

            StartsWithPhrase startsWithPhraseAnnotation = m.getAnnotation(StartsWithPhrase.class);
            ContainsPhrase containsPhraseAnnotation = m.getAnnotation(ContainsPhrase.class);
            ContainsDateReference containsDateReferenceAnnotation = m.getAnnotation(ContainsDateReference.class);
            MatchesRegex matchesRegexAnnotation = m.getAnnotation(MatchesRegex.class);
            ExcludesPhrase excludesPhraseAnnotation = m.getAnnotation(ExcludesPhrase.class);

            if ((startsWithPhraseAnnotation != null && WebexTeamsController.beginsWith(message.getText(), startsWithPhraseAnnotation.phrase()) && (excludesPhraseAnnotation == null || !WebexTeamsController.heard(message, excludesPhraseAnnotation.phrase())))
                    ||
                    (containsPhraseAnnotation != null && WebexTeamsController.heard(message, containsPhraseAnnotation.phrase()) && (excludesPhraseAnnotation == null || !WebexTeamsController.heard(message, excludesPhraseAnnotation.phrase())))
                    ||
                    (matchesRegexAnnotation != null && WebexTeamsController.heardRegex(message, Pattern.compile(matchesRegexAnnotation.regex(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL)))
                    ||
                    (containsDateReferenceAnnotation != null && WebexTeamsController.getDateGroups(message).size() > 0)
            ) {
                return aPrecendece.activationScore();
            }
        }

        return aPrecendece.residualScore();
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MessageHandlers getMessageHandlers() {
        return messageHandlers;
    }

    private void setMessageHandlers(MessageHandlers messageHandlers) {
        this.messageHandlers = messageHandlers;
    }
}
