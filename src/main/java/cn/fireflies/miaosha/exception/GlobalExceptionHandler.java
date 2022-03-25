package cn.fireflies.miaosha.exception;

import cn.fireflies.miaosha.result.CodeMsg;
import cn.fireflies.miaosha.result.Result;
import org.apache.ibatis.executor.ReuseExecutor;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)//拦截所有异常
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e){
        if (e instanceof GlobalException){
          GlobalException ex = (GlobalException) e;
          return Result.error(ex.getCm());
        } else if(e instanceof org.springframework.validation.BindException){
            BindException ex = (BindException) e;
            List<ObjectError> errors =  ex.getAllErrors();
            ObjectError error = errors.get(0);//取第一个
            String msg = error.getDefaultMessage();
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        }else {
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }

}
