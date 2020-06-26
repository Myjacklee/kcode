package com.kuaishou.kcode;

import java.io.*;

public class test3 {
    //代码用于测试数据集是否是按分钟递增的
    public static void main(String args[]){
        try{
            String path="H:\\data\\input\\kcodeRpcMonitor\\2kcodeRpcMonitor.data";
            InputStream inputStream=new FileInputStream(path);
            InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
            BufferedReader reader=new BufferedReader(inputStreamReader);
            String line;
            Long beforeTime=0L;
            System.out.println("递增测试中...");
            while((line=reader.readLine())!=null){
                String[] split=line.split(",");
                Long timeStampLong=Long.parseLong(split[6]);
                timeStampLong-=timeStampLong%(60*1000);
                if(timeStampLong<beforeTime){
                    System.out.println("数据不是按分钟递增的");
                    break;
                }
                beforeTime=timeStampLong;
            }
            System.out.println("数据是按分钟递增的");
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
