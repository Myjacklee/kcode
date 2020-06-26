package com.kuaishou.kcode;

import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author kcode
 * Created on 2020-06-01
 * 实际提交时请维持包名和类名不变
 */

public class KcodeRpcMonitorImpl implements KcodeRpcMonitor {
    //输入数据暂存区
    //主调服务名 被调服务名 IP1 IP2 执行状态
    //主调服务名 被调服务名 IP1 IP2 执行时间
    public HashMap<String,HashMap<String,HashMap<String,HashMap<String,Vector<String>>>>> connState;
    public HashMap<String,HashMap<String,HashMap<String,HashMap<String,Vector<Integer>>>>> connDuration;

    //调用成功率 P99 存储区
    //时间戳 被调服务名 主调服务名 成功率P99
    public HashMap<String,HashMap<String,HashMap<String,ArrayList<String>>>> successRateP99;

    // 不要修改访问级别
    public KcodeRpcMonitorImpl() {
        successRateP99=new HashMap<>();
        connState=new HashMap<>();
        connDuration=new HashMap<>();
    }

    public void prepare(String path) {
        try {
            InputStream inputStream=new FileInputStream(path);
            InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
            BufferedReader reader=new BufferedReader(inputStreamReader);
            String line;
            Boolean begin=false;
            String beforeTimeStamp="";
            while((line=reader.readLine())!=null){
//                System.out.println(line);
                String[] split=line.split(",");
                Long timeStampLong=Long.parseLong(split[6]);
                timeStampLong-=timeStampLong%(60*1000);
                String timeStamp=String.valueOf(timeStampLong);
                if(!begin){
                    begin=true;
                    beforeTimeStamp=timeStamp;
                }
                if(beforeTimeStamp.equals(timeStamp)){
                    putConnState(split[0],split[2],split[1],split[3],split[4]);
                    putConnDuration(split[0],split[2],split[1],split[3],Integer.parseInt(split[5]));
                }else{
                    beforeTimeStamp=timeStamp;
                    getRateP99(timeStamp);
                    connState.clear();
                    connDuration.clear();
                    putConnState(split[0],split[2],split[1],split[3],split[4]);
                    putConnDuration(split[0],split[2],split[1],split[3],Integer.parseInt(split[5]));
                }
            }
            getRateP99(beforeTimeStamp);
            connState.clear();
            connDuration.clear();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getRateP99(String timeStamp) {
        String commentService="";
        String userService="";
        String IP1="";
        String IP2="";
        int P99;
        String rate;
        String result="";
        for(Map.Entry<String,HashMap<String,HashMap<String,HashMap<String,Vector<String>>>>> entry: connState.entrySet()){
            commentService=entry.getKey();
            for(Map.Entry<String,HashMap<String,HashMap<String,Vector<String>>>> entry1:connState.get(commentService).entrySet()){
                userService=entry1.getKey();
                for(Map.Entry<String,HashMap<String,Vector<String>>> entry2:connState.get(commentService).get(userService).entrySet()){
                    IP1=entry2.getKey();
                    for(Map.Entry<String,Vector<String>> entry3:connState.get(commentService).get(userService).get(IP1).entrySet()){
                        IP2=entry3.getKey();
                        int index=(int)Math.ceil((double)entry3.getValue().size()*99/100);
                        P99=getP99(connDuration.get(commentService).get(userService).get(IP1).get(IP2),index,0);
                        rate=getRate(connState.get(commentService).get(userService).get(IP1).get(IP2));
                        result=IP1+","+IP2+","+rate+"%,"+String.valueOf(P99);
                        System.out.println(result);
                        if(!successRateP99.containsKey(timeStamp)){
                            HashMap<String,HashMap<String,ArrayList<String>>> temp=new HashMap<>();
                            successRateP99.put(timeStamp,temp);
                        }
                        if(!successRateP99.get(timeStamp).containsKey(userService)){
                            HashMap<String,ArrayList<String>> temp=new HashMap<>();
                            successRateP99.get(timeStamp).put(userService,temp);
                        }
                        if(!successRateP99.get(timeStamp).get(userService).containsKey(commentService)){
                            ArrayList<String> temp=new ArrayList<>();
                            successRateP99.get(timeStamp).get(userService).put(commentService,temp);
                        }
                        successRateP99.get(timeStamp).get(userService).get(commentService).add(result);
                    }
                }
            }
        }
    }
    public String getRate(Vector<String> successState){
        int size=successState.size();
        double successNum=0;
        for(String state:successState){
            if(state.equals("true")){
                successNum=successNum+1;
            }
        }
//        System.out.println("successNum:"+successNum+" "+"size:"+size);
        double rate=successNum/size;
        String rateString=String.valueOf(rate);
        if((int)successNum==0){
            return ".00";
        }
        int rateInt=(int) rate;
        if(rateInt==1){
            return "100.00";
        }
        rateString=rateString.substring(rateString.indexOf(".")+1)+"00000";
        rateString=rateString.substring(0,4);
        if(rateString.equals("0000")){
            return ".00";
        }
        rateString=rateString.substring(0,2)+"."+rateString.substring(2);
        if(rateString.charAt(0)=='0'){
            rateString=rateString.substring(1);
        }
        return rateString;
    }
    public int getP99(Vector<Integer> allTime,int index,int begin){
        Vector<Integer> left=new Vector<>();
        Vector<Integer> right=new Vector<>();
        int centerNum=allTime.get(0);
        for(int i=1;i<allTime.size();i++){
            Integer tempNum=allTime.get(i);
            if(centerNum>tempNum){
                left.add(tempNum);
            }else{
                right.add(tempNum);
            }
        }
        if(left.size()+1+begin==index){
            return centerNum;
        }else if(left.size()+1+begin<index){
            return getP99(right,index,left.size()+1+begin);
        }else{
            return getP99(left,index,begin);
        }
    }
    private void putConnDuration(String CommentService,String userService,String IP1,String IP2,Integer duration) {
        if(!connDuration.containsKey(CommentService)){
            HashMap<String,HashMap<String,HashMap<String,Vector<Integer>>>> tempMap=new HashMap<>();
            connDuration.put(CommentService,tempMap);
        }
        if(!connDuration.get(CommentService).containsKey(userService)){
            HashMap<String,HashMap<String,Vector<Integer>>> tempMap=new HashMap();
            connDuration.get(CommentService).put(userService,tempMap);
        }
        if(!connDuration.get(CommentService).get(userService).containsKey(IP1)){
            HashMap<String,Vector<Integer>> tempMap=new HashMap<>();
            connDuration.get(CommentService).get(userService).put(IP1,tempMap);
        }
        if(!connDuration.get(CommentService).get(userService).get(IP1).containsKey(IP2)){
            Vector<Integer> temp=new Vector<>();
            connDuration.get(CommentService).get(userService).get(IP1).put(IP2,temp);
        }
        connDuration.get(CommentService).get(userService).get(IP1).get(IP2).add(duration);
    }

    private void putConnState(String CommentService,String userService,String IP1,String IP2,String state) {
        if(!connState.containsKey(CommentService)){
            HashMap<String,HashMap<String,HashMap<String,Vector<String>>>> tempMap=new HashMap<>();
            connState.put(CommentService,tempMap);
        }
        if(!connState.get(CommentService).containsKey(userService)){
            HashMap<String,HashMap<String,Vector<String>>> tempMap=new HashMap();
            connState.get(CommentService).put(userService,tempMap);
        }
        if(!connState.get(CommentService).get(userService).containsKey(IP1)){
            HashMap<String,Vector<String>> tempMap=new HashMap<>();
            connState.get(CommentService).get(userService).put(IP1,tempMap);
        }
        if(!connState.get(CommentService).get(userService).get(IP1).containsKey(IP2)){
            Vector<String> temp=new Vector<>();
            connState.get(CommentService).get(userService).get(IP1).put(IP2,temp);
        }
        connState.get(CommentService).get(userService).get(IP1).get(IP2).add(state);
    }
    public String timeTrans(String time){
        Long realTime=(new SimpleDateFormat("yyyy-MM-dd HH:mm")).parse(time,new ParsePosition(0)).getTime();
        realTime-=realTime%(60*1000);
        String realTimeString=String.valueOf(realTime);
        return realTimeString;
    }
    public List<String> checkPair(String caller, String responder, String time) {
        String realTimeString=timeTrans(time);
        if(successRateP99.containsKey(realTimeString)&&successRateP99.get(realTimeString).containsKey(responder)&&successRateP99.get(realTimeString).get(responder).containsKey(caller)){
            return successRateP99.get(realTimeString).get(responder).get(caller);
        }else{
            return new ArrayList<String>();
        }
    }

    public String checkResponder(String responder, String start, String end) {
        String startTimeString=timeTrans(start);
        String endTimeString=timeTrans(end);
        Long startTime=Long.parseLong(startTimeString);
        Long endTime=Long.parseLong(endTimeString);
        int count=0;
        double allRate=0;
        for(Long s=startTime;s<=endTime;s=s+60*1000){
            if(successRateP99.containsKey(s)&&successRateP99.get(s).containsKey(responder)){
                for(Map.Entry<String,ArrayList<String>> entry:successRateP99.get(s).get(responder).entrySet()){
                    for(String v:entry.getValue()){
                        String[] split=v.split(",");
                        String rate=split[2].substring(0,split[2].indexOf("%"));
                        allRate=allRate+Double.parseDouble(rate);
                        count++;
                    }
                }
            }else{
                continue;
            }
        }
        if(count==0){
            return "-1.00%";
        }else{
            double output=allRate/count;
            String outputString=String.valueOf(output)+"0000";
            outputString=outputString.substring(0,outputString.indexOf(".")+3);
        }
        return "0.00%";
    }
}
