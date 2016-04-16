package me.mrkirby153.uhc.bot.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public interface NetworkCommand {


    void process(ByteArrayDataInput input, ByteArrayDataOutput response);
}
