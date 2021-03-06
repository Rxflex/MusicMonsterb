/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public abstract class MusicCommand extends Command 
{
    protected final Bot bot;
    protected boolean bePlaying;
    protected boolean beListening;
    
    public MusicCommand(Bot bot)
    {
        this.bot = bot;
        this.guildOnly = true;
        this.category = new Category("Music");
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        TextChannel tchannel = settings.getTextChannel(event.getGuild());
        if(tchannel!=null && !event.getTextChannel().equals(tchannel))
        {
            try 
            {
                event.getMessage().delete().queue();
            } catch(PermissionException ignore){}
            event.replyInDm(event.getClient().getError()+" Вы можете использовать эту команду только в "+tchannel.getAsMention()+"!");
            return;
        }
        bot.getPlayerManager().setUpHandler(event.getGuild()); // no point constantly checking for this later
        if(bePlaying && !((AudioHandler)event.getGuild().getAudioManager().getSendingHandler()).isMusicPlaying(event.getJDA()))
        {
            event.reply(event.getClient().getError()+" Должна играть музыка чтобы использовать это!");
            return;
        }
        if(beListening)
        {
            VoiceChannel current = event.getGuild().getSelfMember().getVoiceState().getChannel();
            if(current==null)
                current = settings.getVoiceChannel(event.getGuild());
            GuildVoiceState userState = event.getMember().getVoiceState();
            if(!userState.inVoiceChannel() || userState.isDeafened() || (current!=null && !userState.getChannel().equals(current)))
            {
                event.replyError("Вы должны быть в голосовом канале "+(current==null ? "" : "**"+current.getName()+"**")+" чтобы использовать это!");
                return;
            }

            VoiceChannel afkChannel = userState.getGuild().getAfkChannel();
            if(afkChannel != null && afkChannel.equals(userState.getChannel()))
            {
                event.replyError("Вы не можете использовать команды находясь в AFK канале!");
                return;
            }

            if(!event.getGuild().getSelfMember().getVoiceState().inVoiceChannel())
            {
                try 
                {
                    event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());
                }
                catch(PermissionException ex) 
                {
                    event.reply(event.getClient().getError()+" Я не могу подключиться к **"+userState.getChannel().getName()+"**!");
                    return;
                }
            }
        }
        
        doCommand(event);
    }
    
    public abstract void doCommand(CommandEvent event);
}
