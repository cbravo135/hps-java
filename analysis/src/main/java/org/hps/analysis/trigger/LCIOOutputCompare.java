package org.hps.analysis.trigger;

import java.io.File;
import java.io.IOException;

import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOReader;

public class LCIOOutputCompare {
    public static void main(String[] args) throws IOException {
        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);
        
        //File oldFile = new File("C:\\cygwin64\\home\\Kyle\\tritrigOldReadout.slcio");
        //File newFile = new File("C:\\cygwin64\\home\\Kyle\\tritrigReadout.slcio");
        
        LCIOReader oldReader = new LCIOReader(oldFile);
        LCIOReader newReader = new LCIOReader(newFile);
        
        
        for(int i = 0; i < 50; i++) {
            // FIXME: Crashes here!
            EventHeader oldEvent = oldReader.read();
        }
        
        
        oldReader.close();
        newReader.close();
    }
}