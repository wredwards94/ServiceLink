package com.wesleyedwards.ServiceLink.Controllers.Advice;

import com.wesleyedwards.ServiceLink.Dtos.ErrorDto;
import com.wesleyedwards.ServiceLink.Exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.Exceptions.NotAuthorizedException;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseBody
@ControllerAdvice(basePackages = {"com.wesleyedwards.ServiceLink.Controllers"})
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
}
