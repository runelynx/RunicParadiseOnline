package io.github.runelynx.runicparadise;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;

public class DiscordSRVHook {

    private static final DiscordSRVHook instance = new DiscordSRVHook();

    private DiscordSRVHook() {
     }

     @Subscribe
     public void onMessageReceived(DiscordGuildMessageReceivedEvent event) {


     }

    public static void sendMessage(String channel, String message) {
        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

        if (textChannel != null) {
            textChannel.sendMessage(message).complete();
        }

    }

     public static void register() {
         DiscordSRV.api.subscribe(instance);
     }

    public static void unregister() {
        DiscordSRV.api.unsubscribe(instance);
    }

}
