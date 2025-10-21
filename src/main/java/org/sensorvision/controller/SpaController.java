package org.sensorvision.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle Single Page Application (SPA) routes
 * Serves index.html for 404s on non-API routes to support client-side routing
 */
@Controller
public class SpaController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // For 404 errors on non-API routes, serve the React app
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

                // Don't serve index.html for API requests
                if (uri != null && !uri.startsWith("/api/") && !uri.startsWith("/actuator/")) {
                    return "forward:/index.html";
                }
            }
        }

        // For other errors or API 404s, return the default error page
        return "error";
    }
}
