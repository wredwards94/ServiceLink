package com.wesleyedwards.ServiceLink.controllers.advice;

import com.wesleyedwards.ServiceLink.dtos.ErrorDto;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.NotAuthorizedException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@ResponseBody
@ControllerAdvice(basePackages = {"com.wesleyedwards.ServiceLink.controllers"})
public class ServiceLinkControllerAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ErrorDto handleBadRequestException(BadRequestException badRequest) {
        return new ErrorDto(badRequest.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotAuthorizedException.class)
    public ErrorDto handleNotAuthorizedException(NotAuthorizedException notAuthorized) {
        return new ErrorDto(notAuthorized.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorDto handleNotFoundException(NotFoundException notFound) {
        return new ErrorDto(notFound.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorDto handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorDto(message);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    public ErrorDto handleBadCredentialsException(BadCredentialsException badCredentials) {return new ErrorDto(badCredentials.getMessage());}

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(DisabledException.class)
    public ErrorDto handleDisabledException(DisabledException disabled) {return new ErrorDto(disabled.getMessage());}
}
