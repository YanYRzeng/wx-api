package com.ydkj.wx.controller;

import com.thoughtworks.xstream.XStream;
import com.ydkj.wx.message.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/wx")
@Slf4j
public class WXController {

    @Value("${wx.appId}")
    private String appId;

    @Value("${wx.appSecret}")
    private String appSecret;

    @Value("${wx.token}")
    private String token;

    @GetMapping
    public String wx(String signature, String timestamp, String nonce, String echostr) {
        log.info("signature:" + signature + " " + "timestamp:" + timestamp + " " + "nonce:" + nonce + " " + "echostr:" + echostr);
        //将 token ,timestamp,nonce 将三个字符出进行字典排序
        List<String> list = Arrays.asList(token, timestamp, nonce);
        //排序
        Collections.sort(list);

        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            stringBuilder.append(s);
        }
        //进行加密
        try {
            MessageDigest digest = MessageDigest.getInstance("sha1");
            //使用sha1 进行加密  获得byte数组
            byte[] bytes = digest.digest(stringBuilder.toString().getBytes());
            StringBuilder sum = new StringBuilder();
            for (byte b : bytes) {
                sum.append(Integer.toHexString(b >> 4 & 15));
                sum.append(Integer.toHexString(b & 15));
                  //加密后的字符川与signature 进行比较
                if (!StringUtils.isEmpty(signature) && signature.equals(sum.toString())) {
                    return echostr;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    @PostMapping
    public String receiveMsg(HttpServletRequest request) throws IOException {
        ServletInputStream inputStream = request.getInputStream();
//        byte [] b = new byte[1024];
//        int len=0;
//        while ((len = inputStream.read(b)) !=-1)
//        {
//            log.info( "微信发送的值："+new String(b,0,len));
//        }
        Map<String,String> map = new HashMap<>();
        SAXReader reader = new SAXReader();
        try {
            //通过inputStream 获取Document队像
            Document document =reader.read(inputStream);
            //获取根节点
            Element root = document.getRootElement();
            //获取子节点
            List<Element> elements = root.elements();
            elements.forEach(element -> map.put(element.getName(),element.getStringValue()));
            System.out.println("获取微信用户发送的信息详情: "+map);
           // 被动回复消息
            return getReplayMsg(map);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 被动回复消息
     * @param map
     * @return
     */
    private String getReplayMsg(Map<String, String> map) {
        TextMessage textMessage = new TextMessage();
        textMessage.setToUserName(map.get("FromUserName"));
        textMessage.setFromUserName(map.get("ToUserName"));
        textMessage.setMsgType("text");
        textMessage.setContent("欢迎关注本公众号！！！！");
        textMessage.setCreateTime(System.currentTimeMillis()/1000);

        //Xsteam 将java 对象转化为xml 字符串
        XStream xStream = new XStream();
        xStream.processAnnotations(TextMessage.class);
        String xml = xStream.toXML(textMessage);
        return xml;
    }
}
