package com.lingfeng.biz.downloader.config;


import com.lingfeng.biz.downloader.model.resp.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2021/10/21 15:44
 * @Description:
 */
@Slf4j
@Component
@RestController
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 应用到所有@RequestMapping注解方法，在其执行之前初始化数据绑定器
     *
     * @param binder
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
    }

    /**
     * 把值绑定到Model中，使全局@RequestMapping可以获取到该值
     *
     * @param model
     */
    @ModelAttribute
    public void addAttributes(Model model) {
        //model.addAttribute("author", "Magical Sam");
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    //@ResponseBody//为了返回数据
    public R<?> defaultExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error(e.getMessage(), e);
        return R.fail(e.getMessage());
    }

    /**
     * 视图跳转
     */
    /*@ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView MvErrorHandler(Exception e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        modelAndView.addObject("msg", e.getMessage());
        return modelAndView;
    }*/

    //请求参数校验错误拦截反馈器
    @ExceptionHandler({BindException.class})//WebExchangeBindException
    public R<?> handle(BindException e) {
        //获取参数校验错误集合
        List<FieldError> fieldErrors = e.getFieldErrors();
        //格式化以提供友好的错误提示
        String data = String.format("参数校验错误（%s）：%s", fieldErrors.size(),
                fieldErrors.stream()
                        .map(fieldError -> fieldError.getField() + fieldError.getDefaultMessage())
                        .collect(Collectors.joining(";")));
        //参数校验失败响应失败个数及原因
        return R.fail(400, data);
    }
}
