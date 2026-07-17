package com.demo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that uses the Lombok-generated Greeting class.
 * If Lombok's annotation processor is not on the processor path, this servlet
 * will fail to compile because Greeting would be missing its constructor and getter.
 */
@WebServlet(urlPatterns = "/greeting")
public class GreetingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Greeting greeting = new Greeting("Hello from Liberty dev mode");
        response.getWriter().append(greeting.getMessage());
    }
}
