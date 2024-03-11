package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    //这里为什么接受GET和POST方式的请求和下面的方法有关
    @RequestMapping(value = "data", method = {RequestMethod.GET,RequestMethod.POST})
    public String jumpToDataPage() {
        return "/site/admin/data";
    }

    /**
     * 该方法就是获取一段时间范围内的UV的控制器
     * 这里因为是前段提交表单过来的数据，所以是post请求方式
     * 并且前段提交过来的是字符串，我们如何将其转换为Date类型的对象参数，就用到了DateTimeFormat注解
     * 该注解可以指定日期的字符串格式，接收到该格式的字符串，就转换为Date对象
     * @param start
     * @param end
     * @return
     */
    @RequestMapping(value = "data/uv",method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd")Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        Long uv = dataService.getUV(start, end);
        model.addAttribute("uvCount", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        //这里因为获取统计数据完毕，就也要重新跳转到数据页面进行数据的显示了
        //这里不直接返回数据页面所在的html路径，而是通过请求转发给跳转到请求页面的控制器
        //这样的好处是方便以后跳转data控制器添加了一些逻辑，此时我们的data/uv控制器通过请求转发，可以复用器逻辑！
        return "forward:/data";
    }
    @RequestMapping(value = "data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                         Model model) {
        Long dau = dataService.getDAU(start, end);
        model.addAttribute("dauCount", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        //这里因为获取统计数据完毕，就也要重新跳转到数据页面进行数据的显示了
        //这里不直接返回数据页面所在的html路径，而是通过请求转发给跳转到请求页面的控制器
        //这样的好处是方便以后跳转data控制器添加了一些逻辑，此时我们的data/uv控制器通过请求转发，可以复用器逻辑！
        return "forward:/data";

    }
}
