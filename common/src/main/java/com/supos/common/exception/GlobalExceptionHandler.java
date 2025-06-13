package com.supos.common.exception;

import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Optional;
import java.util.Set;

/**
 * 统一异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BuzException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleError(BuzException e) {
        String msg = I18nUtils.getMessage(e.getMsg(), e.getParams());
        return ResultVO.fail(msg);
    }

    /**
     * valid注解验证报错信息
     * @param e
     * @return
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleError(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        ConstraintViolation<?> violation = constraintViolations.iterator().next();
        String msg = I18nUtils.getMessage(violation.getMessage());
        return ResultVO.fail(msg);
    }

    /**
     * valid注解验证报错信息
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleError(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = e.getFieldError();
        ObjectError objectError = Optional.ofNullable(bindingResult.getGlobalError()).orElse(bindingResult.getFieldError());
        String msg = I18nUtils.getMessage(objectError.getDefaultMessage());
        return ResultVO.fail(msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("upload error", e);
        String msg = I18nUtils.getMessage("uns.attachment.max.size");
        return ResultVO.fail(msg);
    }

    /**
     * method not support
     * @param e
     * @return
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public ResultVO handleError(HttpRequestMethodNotSupportedException e) {
        log.warn("method not support:{} {}", e.getBody(), e.getMessage());
        return ResultVO.fail("method not support");
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleError(Throwable e) {
        log.error("system error", e);
        return ResultVO.fail(I18nUtils.getMessage("system.error"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultVO handleError(HttpMessageNotReadableException e) {
        log.error("JSON parse error", e);
        return ResultVO.fail("JSON parse error");
    }
}
