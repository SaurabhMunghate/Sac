package com.shatam.test;

import org.apache.commons.codec.language.Metaphone;

import com.shatam.util.U;

public class TestSoundex
{
    public static void main(String[] ar)
    {
        //U.log(StrUtil.containsNum("kkkkkk"));
        Metaphone SNDX = new Metaphone();
        String s = SNDX.encode("Khobragade");
        U.log(s);
    }
}
