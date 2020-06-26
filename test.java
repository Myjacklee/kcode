package com.kuaishou.kcode;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class test {
   public static void main(String args[]){
       String time="2020-06-01 09:42";
       Class cls=KcodeRpcMonitorImpl.class;
       try {
           Method timeFuc=cls.getMethod("timeTrans",String.class);
           Constructor<KcodeRpcMonitorImpl> cons=cls.getConstructor();
           try {
               KcodeRpcMonitorImpl kpm=cons.newInstance();
               String timeStamp=(String)timeFuc.invoke(kpm,time);
               System.out.println("原始时间："+time+" 时间戳："+timeStamp);
           } catch (InstantiationException e) {
               e.printStackTrace();
           } catch (IllegalAccessException e) {
               e.printStackTrace();
           } catch (InvocationTargetException e) {
               e.printStackTrace();
           }
       } catch (NoSuchMethodException e) {
           e.printStackTrace();
       }
   }
}