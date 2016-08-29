package com.shatam.main;

import com.shatam.io.AbstractIndexType;

public class WorkerThread implements Runnable {
     
    private int start;
    private int end;
  
    ThreadedSAC threadedSAC = new ThreadedSAC();
    public WorkerThread(int s,int e){
        this.start=s;
        this.end=e;
        
    }
 
    @Override
    public void run() {
         System.out.println(Thread.currentThread().getName()+" Start. Command = "+end);
        //threadedSAC.threadCalling(start,end);
       	//CustomAddressCorrector.extractCorrectAddress(start,end,it);
        //  processCommand();
        System.out.println(Thread.currentThread().getName()+" End.");
    }
 
    private void processCommand() 
    {
        try
        {
            Thread.sleep(5000);
        } 
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
 
    @Override
    public String toString(){
        return "end";
    }
}