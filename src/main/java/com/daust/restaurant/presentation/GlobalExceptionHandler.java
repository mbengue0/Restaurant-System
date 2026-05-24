package com.daust.restaurant.presentation;

import com.daust.restaurant.application.BillAlreadyGeneratedException;
import com.daust.restaurant.application.BillAlreadyPaidException;
import com.daust.restaurant.application.BillNotFoundException;
import com.daust.restaurant.application.CategoryHasActiveItemsException;
import com.daust.restaurant.application.CategoryNotFoundException;
import com.daust.restaurant.application.LastActiveAdminException;
import com.daust.restaurant.application.MenuItemNotFoundException;
import com.daust.restaurant.application.OrderNotFoundException;
import com.daust.restaurant.application.PaymentMethodNotAcceptedException;
import com.daust.restaurant.application.TableAlreadyOccupiedException;
import com.daust.restaurant.application.TableNotFoundException;
import com.daust.restaurant.application.TableNotOccupiedException;
import com.daust.restaurant.application.UserNotFoundException;
import com.daust.restaurant.application.UsernameTakenException;
import com.daust.restaurant.domain.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        TableAlreadyOccupiedException.class,
        TableNotOccupiedException.class,
        TableNotFoundException.class,
        OrderNotFoundException.class,
        BillNotFoundException.class,
        BillAlreadyGeneratedException.class,
        BillAlreadyPaidException.class,
        MenuItemNotFoundException.class,
        CategoryNotFoundException.class,
        CategoryHasActiveItemsException.class,
        UserNotFoundException.class,
        UsernameTakenException.class,
        LastActiveAdminException.class,
        PaymentMethodNotAcceptedException.class
    })
    public String handleApplicationException(
            RuntimeException ex, RedirectAttributes flash, HttpServletRequest request) {
        log.warn("Application exception on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public String handleDomainException(
            RuntimeException ex, RedirectAttributes flash, HttpServletRequest request) {
        log.warn("Domain exception on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(
            UnauthorizedException ex, RedirectAttributes flash, HttpServletRequest request) {
        log.warn("Unauthorized on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleUnexpected(
            RuntimeException ex, RedirectAttributes flash, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        flash.addFlashAttribute("error",
                "Unexpected error: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        return "redirect:" + safeReferer(request);
    }

    private String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        return referer;
    }
}
